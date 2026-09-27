package com.barn.barn.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.barn.barn.entity.BarnProject;
import com.barn.barn.service.BarnProjectService;
import com.barn.common.core.domain.R;
import com.barn.common.core.domain.TableDataInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 烤房项目Controller
 */
@RestController
@RequestMapping("/barn/project")
public class BarnProjectController {

    @Autowired
    private BarnProjectService barnProjectService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /** 分页列表 - 参考 assign-list 实现，从 kf_basedata 查询，JOIN ovens 获取状态 */
    @GetMapping("/list")
    public TableDataInfo<Map<String, Object>> list(@RequestParam(defaultValue = "1") int pageNum,
                                                   @RequestParam(defaultValue = "10") int pageSize,
                                                   @RequestParam(required = false) String countyCode,
                                                   @RequestParam(required = false) String townCode,
                                                   @RequestParam(required = false) String useStatus,
                                                   @RequestParam(required = false) String healthLevel,
                                                   @RequestParam(required = false) String projectCode,
                                                   @RequestParam(required = false) String barnName) {
        System.out.println("===== 开始查询烤房列表 =====");
        System.out.println("pageNum: " + pageNum + ", pageSize: " + pageSize);
        System.out.println("countyCode: " + countyCode + ", townCode: " + townCode);
        System.out.println("useStatus: " + useStatus + ", healthLevel: " + healthLevel);
        System.out.println("projectCode: " + projectCode + ", barnName: " + barnName);

        StringBuilder where = new StringBuilder("WHERE 1=1");
        List<Object> params = new ArrayList<>();

        // 县区筛选 - 前端传的是县名，直接匹配
        if (countyCode != null && !countyCode.isEmpty()) {
            where.append(" AND k.county = ?");
            params.add(countyCode);
        }
        // 乡镇筛选 - 前端传的是乡镇名，直接匹配
        if (townCode != null && !townCode.isEmpty()) {
            where.append(" AND k.township = ?");
            params.add(townCode);
        }
        // 使用状态筛选 - ovens.status 字段（英文值）
        if (useStatus != null && !useStatus.isEmpty()) {
            where.append(" AND o.status = ?");
            params.add(useStatus);
        }
        // 健康等级筛选 - 根据 ovens.health_score 范围筛选
        if (healthLevel != null && !healthLevel.isEmpty()) {
            switch (healthLevel) {
                case "excellent":
                    where.append(" AND o.health_score >= 90");
                    break;
                case "maintenance":
                    where.append(" AND o.health_score >= 75 AND o.health_score < 90");
                    break;
                case "urgent":
                    where.append(" AND o.health_score >= 60 AND o.health_score < 75");
                    break;
                case "retired":
                    where.append(" AND o.health_score < 60");
                    break;
                default:
                    break;
            }
        }
        // 烤房编号/名称筛选 - 和 assign-list 的 keyword 一致
        if (projectCode != null && !projectCode.trim().isEmpty()) {
            String kw = "%" + projectCode.trim() + "%";
            where.append(" AND (k.project_id LIKE ? OR k.detail_address LIKE ? OR k.township LIKE ? OR k.village LIKE ?)");
            params.add(kw);
            params.add(kw);
            params.add(kw);
            params.add(kw);
        }
        if (barnName != null && !barnName.trim().isEmpty()) {
            String kw = "%" + barnName.trim() + "%";
            where.append(" AND (k.project_id LIKE ? OR k.detail_address LIKE ?)");
            params.add(kw);
            params.add(kw);
        }

        // 查询总数 - 和 assign-list 一致，JOIN ovens 表
        String countSql = "SELECT COUNT(*) as total FROM kf_basedata k " +
                "LEFT JOIN ovens o ON k.project_id = o.id " +
                where.toString();
        System.out.println("===== COUNT SQL =====");
        System.out.println(countSql);
        System.out.println("===== COUNT Params =====");
        System.out.println(params);
        Map<String, Object> countResult = jdbcTemplate.queryForMap(countSql, params.toArray());
        long total = ((Number) countResult.get("total")).longValue();
        System.out.println("===== COUNT RESULT =====");
        System.out.println("Total: " + total);

        // 分页查询 - 和 assign-list 一致
        int offset = (pageNum - 1) * pageSize;
        String listSql = "SELECT k.project_id, k.city, k.county, k.township, k.village, " +
                "k.detail_address, k.project_type, k.build_method, k.technician_name, " +
                "k.latitude, k.longitude, k.altitude, " +
                "k.current_health_score, k.current_life_residual, k.current_health_algo, k.current_life_algo, " +
                "k.facility_status, k.use_status, " +
                "o.status, o.health_score, o.temperature, o.humidity, o.baker_id, o.usage_count, " +
                "o.project_cost, o.description, o.created_at, o.updated_at, " +
                "b.name as baker_name " +
                "FROM kf_basedata k " +
                "LEFT JOIN ovens o ON k.project_id = o.id " +
                "LEFT JOIN bakers b ON o.baker_id = b.id " +
                where.toString() +
                " ORDER BY k.project_id LIMIT ? OFFSET ?";
        params.add(pageSize);
        params.add(offset);

        System.out.println("===== LIST SQL =====");
        System.out.println(listSql);
        System.out.println("===== LIST Params =====");
        System.out.println(params);

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(listSql, params.toArray());
        System.out.println("===== LIST RESULT =====");
        System.out.println("Rows count: " + rows.size());

        // 格式化返回数据 - 和 assign-list 保持一致
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", row.get("project_id"));
            m.put("barnName", row.get("project_id"));
            m.put("projectId", row.get("project_id"));
            m.put("city", row.get("city"));
            m.put("county", row.get("county"));
            m.put("countyCode", row.get("county"));
            m.put("township", row.get("township"));
            m.put("townCode", row.get("township"));
            m.put("village", row.get("village"));
            m.put("detailAddress", row.get("detail_address"));
            m.put("address", joinNonEmpty(row.get("city"), row.get("county"),
                    row.get("township"), row.get("village"), row.get("detail_address")));
            m.put("projectType", row.get("project_type"));
            m.put("buildMethod", row.get("build_method"));
            m.put("technician", row.get("technician_name"));
            m.put("latitude", row.get("latitude"));
            m.put("longitude", row.get("longitude"));
            m.put("altitude", row.get("altitude"));
            m.put("status", row.get("status") != null ? row.get("status") : "idle");
            m.put("useStatus", row.get("use_status") != null ? row.get("use_status") : "在用");
            m.put("facilityStatus", row.get("facility_status") != null ? row.get("facility_status") : "正常");
            m.put("healthScore", row.get("current_health_score") != null ? row.get("current_health_score") : (row.get("health_score") != null ? row.get("health_score") : 85));
            m.put("currentHealthScore", row.get("current_health_score"));
            m.put("currentLifeResidual", row.get("current_life_residual"));
            m.put("currentHealthAlgo", row.get("current_health_algo"));
            m.put("currentLifeAlgo", row.get("current_life_algo"));
            m.put("temperature", row.get("temperature"));
            m.put("humidity", row.get("humidity"));
            m.put("bakerId", row.get("baker_id"));
            m.put("bakerName", row.get("baker_name"));
            m.put("projectCost", row.get("project_cost"));
            m.put("usageCount", row.get("usage_count") != null ? row.get("usage_count") : 0);
            m.put("description", row.get("description"));
            m.put("createdAt", row.get("created_at"));
            m.put("updatedAt", row.get("updated_at"));
            result.add(m);
        }

        System.out.println("===== 返回结果 =====");
        System.out.println("Result count: " + result.size());
        return TableDataInfo.build(result, total, pageNum, pageSize);
    }

    /** 烤房下拉选项 - 不分页，给前端选择器/日历用 */
    @GetMapping("/list-options")
    public R<List<Map<String, Object>>> listOptions() {
        return R.ok(barnProjectService.listOptions());
    }

    /** 详情 - 从 ovens 表查询，并补充 kf_basedata 表中的经纬度等字段 */
    @GetMapping("/{id}")
    public R<BarnProject> getInfo(@PathVariable String id) {
        BarnProject barn = barnProjectService.getById(id);
        if (barn == null) {
            return R.fail("烤房不存在: " + id);
        }
        // ovens 表可能缺少经纬度，从 kf_basedata 表补充
        if (barn.getLongitude() == null || barn.getLatitude() == null) {
            try {
                List<Map<String, Object>> baseRows = jdbcTemplate.queryForList(
                        "SELECT latitude, longitude, altitude, city, county, township, village, " +
                        "detail_address, project_type, build_method, technician_name " +
                        "FROM kf_basedata WHERE project_id = ?", id);
                if (!baseRows.isEmpty()) {
                    Map<String, Object> base = baseRows.get(0);
                    if (barn.getLongitude() == null && base.get("longitude") != null) {
                        barn.setLongitude(new java.math.BigDecimal(base.get("longitude").toString()));
                    }
                    if (barn.getLatitude() == null && base.get("latitude") != null) {
                        barn.setLatitude(new java.math.BigDecimal(base.get("latitude").toString()));
                    }
                    if (barn.getAltitude() == null && base.get("altitude") != null) {
                        barn.setAltitude(Integer.valueOf(base.get("altitude").toString()));
                    }
                }
            } catch (Exception e) {
                // 查询失败不影响主流程
            }
        }
        return R.ok(barn);
    }

    /** 新增 */
    @PostMapping
    public R<Void> add(@RequestBody BarnProject barn) {
        barnProjectService.save(barn);
        return R.ok();
    }

    /** 修改 */
    @PutMapping
    public R<Void> edit(@RequestBody BarnProject barn) {
        barnProjectService.update(barn);
        return R.ok();
    }

    /** 删除 */
    @DeleteMapping("/{id}")
    public R<Void> remove(@PathVariable String id) {
        barnProjectService.delete(id);
        return R.ok();
    }

    /** 烤房统计数据 - 从 kf_basedata 查询真实使用状态数据 */
    @GetMapping("/stats")
    public R<Map<String, Object>> stats() {
        Map<String, Object> result = new HashMap<>();
        // 总数 - 从 kf_basedata 查询
        Long total = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM kf_basedata", Long.class);
        result.put("total", total);
        // 按使用状态统计（在用/闲置/转用/损毁）- 从 kf_basedata.use_status 查询
        List<Map<String, Object>> useStatusStats = jdbcTemplate.queryForList(
            "SELECT use_status, COUNT(*) as cnt FROM kf_basedata WHERE use_status IS NOT NULL GROUP BY use_status");
        Map<String, Integer> useStatusMap = new HashMap<>();
        for (Map<String, Object> row : useStatusStats) {
            String status = (String) row.get("use_status");
            Number count = (Number) row.get("cnt");
            if (status != null) useStatusMap.put(status, count.intValue());
        }
        result.put("useStatus", useStatusMap);
        // 按县区统计 - 从 kf_basedata 查询
        List<Map<String, Object>> countyStats = jdbcTemplate.queryForList(
            "SELECT county, COUNT(*) as cnt FROM kf_basedata WHERE county IS NOT NULL GROUP BY county ORDER BY cnt DESC");
        result.put("countyStats", countyStats);
        // 按建设方式统计 - 从 kf_basedata.build_method 查询
        List<Map<String, Object>> buildMethodStats = jdbcTemplate.queryForList(
            "SELECT build_method, COUNT(*) as cnt FROM kf_basedata WHERE build_method IS NOT NULL GROUP BY build_method");
        result.put("buildMethodStats", buildMethodStats);
        // 按项目类型统计 - 从 kf_basedata.project_type 查询
        List<Map<String, Object>> projectTypeStats = jdbcTemplate.queryForList(
            "SELECT project_type, COUNT(*) as cnt FROM kf_basedata WHERE project_type IS NOT NULL GROUP BY project_type");
        result.put("projectTypeStats", projectTypeStats);
        // 健康等级统计 - 保持从 ovens 表查询（health_score 字段）
        result.put("healthExcellent", jdbcTemplate.queryForObject("SELECT COUNT(*) FROM ovens WHERE health_score >= 90", Integer.class));
        result.put("healthMaintenance", jdbcTemplate.queryForObject("SELECT COUNT(*) FROM ovens WHERE health_score >= 75 AND health_score < 90", Integer.class));
        result.put("healthUrgent", jdbcTemplate.queryForObject("SELECT COUNT(*) FROM ovens WHERE health_score >= 60 AND health_score < 75", Integer.class));
        result.put("healthRetired", jdbcTemplate.queryForObject("SELECT COUNT(*) FROM ovens WHERE health_score < 60", Integer.class));
        // 分配统计 - 保持从 oven_assignments 查询
        result.put("assignedCount", jdbcTemplate.queryForObject("SELECT COUNT(*) FROM oven_assignments WHERE status IN ('assigned','active')", Integer.class));
        // 评价统计 - 保持从 evaluations 查询
        result.put("evaluationCount", jdbcTemplate.queryForObject("SELECT COUNT(*) FROM evaluations", Integer.class));
        return R.ok(result);
    }

    /** 更新烤房信息 */
    @PutMapping("/edit")
    public R<String> edit(@RequestBody Map<String, Object> body) {
        String id = (String) body.get("id");
        if (id == null || id.isEmpty()) {
            return R.fail("烤房编号不能为空");
        }
        StringBuilder sql = new StringBuilder("UPDATE ovens SET updated_at = NOW()");
        List<Object> params = new ArrayList<>();

        String[] fields = {"barn_name", "project_type", "build_method", "use_status", "address",
            "latitude", "longitude", "altitude", "technician", "project_cost",
            "length_m", "width_m", "height_m", "health_score", "health_level",
            "project_owner", "construction_unit", "station_name", "description", "facility_status"};
        String[] bodyKeys = {"barnName", "projectType", "buildMethod", "useStatus", "address",
            "latitude", "longitude", "altitude", "technician", "projectCost",
            "lengthM", "widthM", "heightM", "healthScore", "healthLevel",
            "projectOwner", "constructionUnit", "stationName", "description", "facilityStatus"};

        for (int i = 0; i < fields.length; i++) {
            Object val = body.get(bodyKeys[i]);
            if (val != null) {
                sql.append(", ").append(fields[i]).append(" = ?");
                params.add(val);
            }
        }
        sql.append(" WHERE id = ?");
        params.add(id);

        try {
            int rows = jdbcTemplate.update(sql.toString(), params.toArray());
            // 同步更新 kf_basedata 表的 facility_status 和 use_status
            Object facilityVal = body.get("facilityStatus");
            Object useStatusVal = body.get("useStatus");
            if (facilityVal != null || useStatusVal != null) {
                try {
                    StringBuilder baseSql = new StringBuilder("UPDATE kf_basedata SET updated_at = NOW()");
                    List<Object> baseParams = new ArrayList<>();
                    if (facilityVal != null) {
                        baseSql.append(", facility_status = ?");
                        baseParams.add(facilityVal);
                    }
                    if (useStatusVal != null) {
                        // 前端可能传中文(在烤/空闲)或英文(baking/idle)，统一转英文存入kf_basedata
                        String useStatusStr = String.valueOf(useStatusVal);
                        if ("在烤".equals(useStatusStr)) useStatusStr = "baking";
                        else if ("空闲".equals(useStatusStr)) useStatusStr = "idle";
                        baseSql.append(", use_status = ?");
                        baseParams.add(useStatusStr);
                        // 同时更新 ovens 表的 status
                        try {
                            jdbcTemplate.update("UPDATE ovens SET status = ?, updated_at = NOW() WHERE id = ?", useStatusStr, id);
                        } catch (Exception ex2) { /* ignore */ }
                    }
                    baseSql.append(" WHERE project_id = ?");
                    baseParams.add(id);
                    jdbcTemplate.update(baseSql.toString(), baseParams.toArray());
                } catch (Exception ex) {
                    // kf_basedata 更新失败不影响主流程
                }
            }
            if (rows > 0) return R.ok("更新成功");
            return R.fail("更新失败，烤房不存在");
        } catch (Exception e) {
            return R.fail("更新失败: " + e.getMessage());
        }
    }

    /** 健康等级统计 */
    @GetMapping("/stats/health")
    public R<Map<String, Object>> healthStats() {
        return R.ok(barnProjectService.healthStats());
    }

    /** 县区分布统计 */
    @GetMapping("/stats/county")
    public R<List<Map<String, Object>>> countyStats() {
        return R.ok(barnProjectService.countyStats());
    }

    /** 项目类型分布 */
    @GetMapping("/stats/projectType")
    public R<List<Map<String, Object>>> projectTypeStats() {
        return R.ok(barnProjectService.projectTypeStats());
    }

    /** 建设方式分布 */
    @GetMapping("/stats/buildMode")
    public R<List<Map<String, Object>>> buildModeStats() {
        return R.ok(barnProjectService.buildModeStats());
    }

    /**
     * 烤房分配专用列表 - 从kf_basedata表获取烤房数据
     * 确保分配的烤房ID在小程序端能正确显示
     * 返回: projectId(烤房编码), barnName(名称), city, county, township, village, projectType, status, healthScore
     */
    @GetMapping("/assign-list")
    public TableDataInfo<Map<String, Object>> assignList(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String county,
            @RequestParam(required = false) String township,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String healthLevel,
            @RequestParam(required = false) String facilityStatus) {
        
        StringBuilder where = new StringBuilder("WHERE 1=1");
        List<Object> params = new ArrayList<>();
        
        if (county != null && !county.isEmpty()) {
            // 前端传的是县名，直接匹配
            where.append(" AND k.county = ?");
            params.add(county);
        }
        if (township != null && !township.isEmpty()) {
            where.append(" AND k.township = ?");
            params.add(township);
        }
        if (keyword != null && !keyword.trim().isEmpty()) {
            String kw = "%" + keyword.trim() + "%";
            where.append(" AND (k.project_id LIKE ? OR k.detail_address LIKE ? OR k.township LIKE ? OR k.village LIKE ?)");
            params.add(kw);
            params.add(kw);
            params.add(kw);
            params.add(kw);
        }
        if (status != null && !status.isEmpty()) {
            where.append(" AND o.status = ?");
            params.add(status);
        }
        // 设施现状筛选
        if (facilityStatus != null && !facilityStatus.isEmpty()) {
            where.append(" AND k.facility_status = ?");
            params.add(facilityStatus);
        }
        // 健康等级筛选 - 根据 ovens.health_score 范围筛选
        if (healthLevel != null && !healthLevel.isEmpty()) {
            switch (healthLevel) {
                case "excellent":
                    where.append(" AND o.health_score >= 90");
                    break;
                case "maintenance":
                    where.append(" AND o.health_score >= 75 AND o.health_score < 90");
                    break;
                case "urgent":
                    where.append(" AND o.health_score >= 60 AND o.health_score < 75");
                    break;
                case "retired":
                    where.append(" AND o.health_score < 60");
                    break;
                default:
                    break;
            }
        }
        
        // 查询总数 - 必须和list SQL一样JOIN所有表，否则where里的o.status会报错
        String countSql = "SELECT COUNT(*) as total FROM kf_basedata k " +
                "LEFT JOIN ovens o ON k.project_id = o.id " +
                where.toString();
        Map<String, Object> countResult = jdbcTemplate.queryForMap(countSql, params.toArray());
        long total = ((Number) countResult.get("total")).longValue();
        
        // 分页查询 - JOIN ovens表获取烤房状态
        // 排序：已分配的排在前面，然后按序号升序
        int offset = (pageNum - 1) * pageSize;
        String listSql = "SELECT k.project_id, k.city, k.county, k.township, k.village, " +
                "k.detail_address, k.project_type, k.build_method, k.technician_name, " +
                "k.latitude, k.longitude, k.altitude, " +
                "k.current_health_score, k.current_life_residual, k.current_health_algo, k.current_life_algo, " +
                "k.facility_status, k.use_status, " +
                "o.status, o.health_score, o.baker_id, o.usage_count, " +
                "b.name as baker_name, " +
                "CASE WHEN o.status = 'baking' THEN 0 ELSE 1 END as sort_priority " +
                "FROM kf_basedata k " +
                "LEFT JOIN ovens o ON k.project_id = o.id " +
                "LEFT JOIN bakers b ON o.baker_id = b.id " +
                where.toString() +
                " ORDER BY sort_priority ASC, k.project_id ASC LIMIT ? OFFSET ?";
        
        params.add(pageSize);
        params.add(offset);
        
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(listSql, params.toArray());
        
        // 格式化返回数据
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", row.get("project_id"));           // 烤房编码（kf_basedata.project_id）
            m.put("barnName", row.get("project_id"));    // 显示名也用编码
            m.put("projectId", row.get("project_id"));
            m.put("city", row.get("city"));
            m.put("county", row.get("county"));
            m.put("township", row.get("township"));
            m.put("village", row.get("village"));
            m.put("detailAddress", row.get("detail_address"));
            m.put("address", joinNonEmpty(row.get("city"), row.get("county"), 
                    row.get("township"), row.get("village"), row.get("detail_address")));
            m.put("projectType", row.get("project_type"));
            m.put("buildMethod", row.get("build_method"));
            m.put("technician", row.get("technician_name"));
            m.put("latitude", row.get("latitude"));
            m.put("longitude", row.get("longitude"));
            m.put("altitude", row.get("altitude"));
            m.put("status", row.get("status") != null ? row.get("status") : "idle");
            m.put("useStatus", row.get("use_status") != null ? row.get("use_status") : "在用");
            m.put("facilityStatus", row.get("facility_status") != null ? row.get("facility_status") : "正常");
            m.put("healthScore", row.get("current_health_score") != null ? row.get("current_health_score") : (row.get("health_score") != null ? row.get("health_score") : 85));
            m.put("currentHealthScore", row.get("current_health_score"));
            m.put("currentLifeResidual", row.get("current_life_residual"));
            m.put("currentHealthAlgo", row.get("current_health_algo"));
            m.put("currentLifeAlgo", row.get("current_life_algo"));
            m.put("bakerId", row.get("baker_id"));
            m.put("bakerName", row.get("baker_name"));
            m.put("usageCount", row.get("usage_count") != null ? row.get("usage_count") : 0);
            result.add(m);
        }
        
        return TableDataInfo.build(result, total, pageNum, pageSize);
    }
    
    /** 拼接非空字符串 */
    private String joinNonEmpty(Object... parts) {
        StringBuilder sb = new StringBuilder();
        for (Object p : parts) {
            if (p != null && !p.toString().isEmpty()) {
                sb.append(p);
            }
        }
        return sb.toString();
    }
}
