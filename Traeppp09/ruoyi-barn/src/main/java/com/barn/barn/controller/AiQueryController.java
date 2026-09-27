package com.barn.barn.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * AI 助手查询 Controller
 * 供 Dify 工作流通过 HTTP 请求节点调用，返回烤房数据 JSON
 * 数据来源：kf_basedata LEFT JOIN ovens（使用状态来自 ovens.status）
 * 所有接口仅执行 SELECT，不执行任何写操作
 */
@RestController
@RequestMapping("/barn/ai-query")
public class AiQueryController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * 查询烤房列表
     * 使用状态来自 ovens.status（baking=在烤, idle=空闲）
     * 健康分来自 kf_basedata.health_score_final
     */
    @GetMapping("/barns")
    public Map<String, Object> queryBarns(
            @RequestParam(required = false) String county,
            @RequestParam(required = false) String useStatus,
            @RequestParam(required = false) String facilityStatus,
            @RequestParam(required = false) String healthLevel,
            @RequestParam(required = false) String lifeCondition,
            @RequestParam(defaultValue = "50") int limit) {

        StringBuilder where = new StringBuilder("WHERE 1=1");
        List<Object> params = new ArrayList<>();

        // 县名模糊匹配
        if (county != null && !county.isEmpty()) {
            where.append(" AND k.county LIKE ?");
            params.add("%" + county + "%");
        }

        // 使用状态筛选 — 来自 ovens.status
        if (useStatus != null && !useStatus.isEmpty()) {
            String statusVal = normalizeUseStatus(useStatus);
            if (statusVal != null) {
                where.append(" AND o.status = ?");
                params.add(statusVal);
            }
        }

        // 设施现状筛选
        if (facilityStatus != null && !facilityStatus.isEmpty()) {
            where.append(" AND k.facility_status = ?");
            params.add(facilityStatus);
        }

        // 健康等级筛选 — 来自 kf_basedata.health_score_final
        if (healthLevel != null && !healthLevel.isEmpty()) {
            switch (healthLevel) {
                case "优良":
                case "excellent":
                    where.append(" AND k.health_score_final >= 90");
                    break;
                case "良好":
                case "maintenance":
                    where.append(" AND k.health_score_final >= 75 AND k.health_score_final < 90");
                    break;
                case "一般":
                case "urgent":
                    where.append(" AND k.health_score_final >= 60 AND k.health_score_final < 75");
                    break;
                case "预警":
                    where.append(" AND k.health_score_final >= 40 AND k.health_score_final < 60");
                    break;
                case "危险":
                case "retired":
                    where.append(" AND k.health_score_final < 40");
                    break;
                case "低于60":
                    where.append(" AND k.health_score_final < 60");
                    break;
                default:
                    break;
            }
        }

        // 寿命条件筛选
        if (lifeCondition != null && !lifeCondition.isEmpty()) {
            if (lifeCondition.contains("不足5") || lifeCondition.contains("低于5") || lifeCondition.contains("小于5")) {
                where.append(" AND k.life_residual_final < 5 AND k.life_residual_final IS NOT NULL");
            } else if (lifeCondition.contains("不足10") || lifeCondition.contains("低于10") || lifeCondition.contains("小于10")) {
                where.append(" AND k.life_residual_final < 10 AND k.life_residual_final IS NOT NULL");
            } else if (lifeCondition.contains("不足3") || lifeCondition.contains("低于3") || lifeCondition.contains("小于3")) {
                where.append(" AND k.life_residual_final < 3 AND k.life_residual_final IS NOT NULL");
            }
        }

        if (limit > 100) limit = 100;

        String sql = "SELECT k.project_id, k.city, k.county, k.township, k.village, k.detail_address, " +
                "k.facility_status, k.health_score_final, k.life_residual_final, k.finish_date, " +
                "k.project_type, k.build_method, k.technician_name, " +
                "o.status, o.health_score as oven_health_score, o.temperature, o.humidity " +
                "FROM kf_basedata k LEFT JOIN ovens o ON k.project_id = o.id " +
                where.toString() +
                " ORDER BY k.health_score_final IS NULL, k.health_score_final ASC LIMIT ?";
        params.add(limit);

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, params.toArray());

        List<Map<String, Object>> formattedRows = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("project_id", row.get("project_id"));
            item.put("county", row.get("county"));
            item.put("township", row.get("township"));
            item.put("village", row.get("village"));
            item.put("detail_address", row.get("detail_address"));
            item.put("use_status", formatUseStatus(row.get("status")));
            item.put("facility_status", row.get("facility_status"));
            item.put("health_score", row.get("health_score_final"));
            item.put("life_residual", row.get("life_residual_final"));
            item.put("finish_date", row.get("finish_date"));
            formattedRows.add(item);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", formattedRows.size());
        result.put("data", formattedRows);
        return result;
    }

    /**
     * 按项目编号查询烤房全部信息
     */
    @GetMapping("/detail")
    public Map<String, Object> queryDetail(@RequestParam String projectId) {
        String sql = "SELECT k.project_id, k.city, k.county, k.township, k.village, k.detail_address, " +
                "k.project_type, k.build_method, k.technician_name, " +
                "k.latitude, k.longitude, k.altitude, " +
                "k.facility_status, k.use_status as kf_use_status, " +
                "k.health_score_final, k.life_residual_final, " +
                "k.current_health_score, k.current_life_residual, " +
                "k.current_health_algo, k.current_life_algo, " +
                "k.finish_date, k.start_date, " +
                "o.status as oven_status, o.health_score as oven_health_score, " +
                "o.temperature, o.humidity, o.baker_id, o.usage_count, " +
                "o.project_cost, o.description, " +
                "b.name as baker_name " +
                "FROM kf_basedata k " +
                "LEFT JOIN ovens o ON k.project_id = o.id " +
                "LEFT JOIN bakers b ON o.baker_id = b.id " +
                "WHERE k.project_id = ?";

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, projectId);

        if (rows.isEmpty()) {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("found", false);
            result.put("message", "未找到项目编号为 " + projectId + " 的烤房");
            return result;
        }

        Map<String, Object> row = rows.get(0);
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("project_id", row.get("project_id"));
        item.put("city", row.get("city"));
        item.put("county", row.get("county"));
        item.put("township", row.get("township"));
        item.put("village", row.get("village"));
        item.put("detail_address", row.get("detail_address"));
        item.put("use_status", formatUseStatus(row.get("oven_status")));
        item.put("facility_status", row.get("facility_status"));
        item.put("project_type", row.get("project_type"));
        item.put("build_method", row.get("build_method"));
        item.put("technician_name", row.get("technician_name"));
        item.put("latitude", row.get("latitude"));
        item.put("longitude", row.get("longitude"));
        item.put("altitude", row.get("altitude"));
        item.put("health_score", row.get("health_score_final"));
        item.put("life_residual", row.get("life_residual_final"));
        item.put("current_health_score", row.get("current_health_score"));
        item.put("current_life_residual", row.get("current_life_residual"));
        item.put("current_health_algo", row.get("current_health_algo"));
        item.put("current_life_algo", row.get("current_life_algo"));
        item.put("finish_date", row.get("finish_date"));
        item.put("start_date", row.get("start_date"));
        item.put("oven_health_score", row.get("oven_health_score"));
        item.put("temperature", row.get("temperature"));
        item.put("humidity", row.get("humidity"));
        item.put("baker_name", row.get("baker_name"));
        item.put("usage_count", row.get("usage_count"));
        item.put("project_cost", row.get("project_cost"));
        item.put("description", row.get("description"));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("found", true);
        result.put("data", item);
        return result;
    }

    /**
     * 按县统计烤房数量（使用状态来自 ovens.status）
     */
    @GetMapping("/stats")
    public Map<String, Object> queryStats(
            @RequestParam(required = false) String county) {

        StringBuilder where = new StringBuilder(
            "WHERE k.county IS NOT NULL");
        List<Object> params = new ArrayList<>();

        if (county != null && !county.isEmpty()) {
            where.append(" AND k.county LIKE ?");
            params.add("%" + county + "%");
        }

        String sql = "SELECT k.county, COUNT(*) as total, " +
                "SUM(CASE WHEN o.status = 'baking' THEN 1 ELSE 0 END) as baking, " +
                "SUM(CASE WHEN o.status = 'idle' THEN 1 ELSE 0 END) as idle, " +
                "SUM(CASE WHEN k.facility_status = '正常' THEN 1 ELSE 0 END) as facility_normal, " +
                "SUM(CASE WHEN k.facility_status = '损坏' THEN 1 ELSE 0 END) as facility_damaged, " +
                "ROUND(AVG(k.health_score_final), 1) as avg_health, " +
                "ROUND(AVG(k.life_residual_final), 1) as avg_life " +
                "FROM kf_basedata k LEFT JOIN ovens o ON k.project_id = o.id " +
                where.toString() + " GROUP BY k.county ORDER BY total DESC";

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, params.toArray());

        List<Map<String, Object>> formattedRows = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("county", row.get("county"));
            item.put("total", row.get("total"));
            item.put("baking", row.get("baking"));
            item.put("idle", row.get("idle"));
            item.put("facility_normal", row.get("facility_normal"));
            item.put("facility_damaged", row.get("facility_damaged"));
            item.put("avg_health", row.get("avg_health"));
            item.put("avg_life", row.get("avg_life"));
            formattedRows.add(item);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("counties", formattedRows);
        result.put("total_counties", formattedRows.size());

        int grandTotal = 0, grandBaking = 0, grandIdle = 0;
        for (Map<String, Object> row : formattedRows) {
            grandTotal += ((Number) row.get("total")).intValue();
            Object baking = row.get("baking");
            Object idle = row.get("idle");
            if (baking != null) grandBaking += ((Number) baking).intValue();
            if (idle != null) grandIdle += ((Number) idle).intValue();
        }
        result.put("grand_total", grandTotal);
        result.put("grand_baking", grandBaking);
        result.put("grand_idle", grandIdle);

        return result;
    }

    /**
     * 全局概览统计（使用状态来自 ovens.status）
     */
    @GetMapping("/overview")
    public Map<String, Object> overview() {
        Map<String, Object> result = new LinkedHashMap<>();

        Long total = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM kf_basedata", Long.class);
        result.put("total", total);

        // 使用状态分布 — 来自 ovens.status
        List<Map<String, Object>> useStatusStats = jdbcTemplate.queryForList(
            "SELECT o.status, COUNT(*) as cnt FROM kf_basedata k " +
            "LEFT JOIN ovens o ON k.project_id = o.id WHERE o.status IS NOT NULL GROUP BY o.status");
        Map<String, Object> useStatusMap = new LinkedHashMap<>();
        for (Map<String, Object> row : useStatusStats) {
            String status = formatUseStatus(row.get("status"));
            Number count = (Number) row.get("cnt");
            useStatusMap.put(status, count.intValue());
        }
        result.put("use_status_dist", useStatusMap);

        // 设施现状分布
        List<Map<String, Object>> facilityStats = jdbcTemplate.queryForList(
            "SELECT facility_status, COUNT(*) as cnt FROM kf_basedata WHERE facility_status IS NOT NULL GROUP BY facility_status");
        Map<String, Object> facilityMap = new LinkedHashMap<>();
        for (Map<String, Object> row : facilityStats) {
            String status = row.get("facility_status") != null ? row.get("facility_status").toString() : "未知";
            Number count = (Number) row.get("cnt");
            facilityMap.put(status, count.intValue());
        }
        result.put("facility_status_dist", facilityMap);

        // 健康等级分布
        Map<String, Object> healthDist = new LinkedHashMap<>();
        healthDist.put("优良", jdbcTemplate.queryForObject("SELECT COUNT(*) FROM kf_basedata WHERE health_score_final >= 90", Integer.class));
        healthDist.put("良好", jdbcTemplate.queryForObject("SELECT COUNT(*) FROM kf_basedata WHERE health_score_final >= 75 AND health_score_final < 90", Integer.class));
        healthDist.put("一般", jdbcTemplate.queryForObject("SELECT COUNT(*) FROM kf_basedata WHERE health_score_final >= 60 AND health_score_final < 75", Integer.class));
        healthDist.put("预警", jdbcTemplate.queryForObject("SELECT COUNT(*) FROM kf_basedata WHERE health_score_final >= 40 AND health_score_final < 60", Integer.class));
        healthDist.put("危险", jdbcTemplate.queryForObject("SELECT COUNT(*) FROM kf_basedata WHERE health_score_final < 40", Integer.class));
        result.put("health_level_dist", healthDist);

        try {
            Double avgHealth = jdbcTemplate.queryForObject("SELECT ROUND(AVG(health_score_final), 1) FROM kf_basedata WHERE health_score_final IS NOT NULL", Double.class);
            Double avgLife = jdbcTemplate.queryForObject("SELECT ROUND(AVG(life_residual_final), 1) FROM kf_basedata WHERE life_residual_final IS NOT NULL", Double.class);
            result.put("avg_health_score", avgHealth);
            result.put("avg_life_residual", avgLife);
        } catch (Exception e) {
            result.put("avg_health_score", 0);
            result.put("avg_life_residual", 0);
        }

        return result;
    }

    // ======================== 私有工具方法 ========================

    /**
     * 将中文使用状态转为数据库 ovens.status 存储的英文值
     */
    private String normalizeUseStatus(String input) {
        if (input == null) return null;
        if (input.contains("在烤") || input.contains("在用") || input.contains("baking")) return "baking";
        if (input.contains("空闲") || input.contains("闲置") || input.contains("idle")) return "idle";
        if (input.contains("转用") || input.contains("transfer")) return "transfer";
        if (input.contains("损毁") || input.contains("damaged")) return "damaged";
        return input;
    }

    /**
     * 将 ovens.status 英文值格式化为中文显示
     */
    private String formatUseStatus(Object raw) {
        if (raw == null) return "未知";
        String val = raw.toString().toLowerCase();
        switch (val) {
            case "baking": return "在烤";
            case "idle": return "空闲";
            case "transfer": return "转用";
            case "damaged": return "损毁";
            default: return raw.toString();
        }
    }
}
