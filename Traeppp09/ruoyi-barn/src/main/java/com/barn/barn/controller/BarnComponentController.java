package com.barn.barn.controller;

import com.barn.common.core.domain.R;
import com.barn.common.core.domain.TableDataInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 烤房部件Controller
 * 提供烤房5大部位（加热设备JR/散热器SR/自控设备ZK/烤房主体ZT/附属设施FS）的查询能力
 * 数据来源：kf_heater / kf_radiator / kf_controller / kf_oven_body / kf_annex
 * 基础信息来源：kf_basedata
 */
@RestController
@RequestMapping("/barn/components")
public class BarnComponentController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /** 部位代码 -> 物理表名 */
    private static final Map<String, String> CATEGORY_TABLE = new HashMap<>();
    /** 部位代码 -> 中文名称 */
    private static final Map<String, String> CATEGORY_NAME = new LinkedHashMap<>();
    /** 部位代码 -> 列表查询中的evaluation别名 */
    private static final Map<String, String> CATEGORY_EVAL_ALIAS = new HashMap<>();

    static {
        CATEGORY_TABLE.put("JR", "kf_heater");
        CATEGORY_TABLE.put("SR", "kf_radiator");
        CATEGORY_TABLE.put("ZK", "kf_controller");
        CATEGORY_TABLE.put("ZT", "kf_oven_body");
        CATEGORY_TABLE.put("FS", "kf_annex");

        CATEGORY_NAME.put("JR", "加热设备");
        CATEGORY_NAME.put("SR", "散热器");
        CATEGORY_NAME.put("ZK", "自控设备");
        CATEGORY_NAME.put("ZT", "烤房主体");
        CATEGORY_NAME.put("FS", "附属设施");

        CATEGORY_EVAL_ALIAS.put("JR", "h.evaluation");
        CATEGORY_EVAL_ALIAS.put("SR", "r.evaluation");
        CATEGORY_EVAL_ALIAS.put("ZK", "c.evaluation");
        CATEGORY_EVAL_ALIAS.put("ZT", "o.evaluation");
        CATEGORY_EVAL_ALIAS.put("FS", "a.evaluation");
    }

    // ======================== API 1: 部件列表查询（分页） ========================

    /**
     * 部件列表查询，支持按部位(category: JR/SR/ZK/ZT/FS)、项目编号、区县、乡镇、状态搜索
     * 当指定category时，以该部件表为主表JOIN kf_basedata，返回部件编码和各部件状态
     * 未指定category时，以kf_basedata为主表LEFT JOIN全部部件表，返回evaluation概览
     */
    @GetMapping("/list")
    public TableDataInfo<Map<String, Object>> list(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String projectId,
            @RequestParam(required = false) String county,
            @RequestParam(required = false) String township,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String facilityStatus) {

        String cat = (category != null && !category.trim().isEmpty()) ? category.toUpperCase().trim() : null;
        StringBuilder where = new StringBuilder("WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (projectId != null && !projectId.trim().isEmpty()) {
            where.append(" AND k.project_id LIKE ?");
            params.add("%" + projectId.trim() + "%");
        }
        if (county != null && !county.isEmpty()) {
            where.append(" AND k.county = ?");
            params.add(county);
        }
        if (township != null && !township.isEmpty()) {
            where.append(" AND k.township = ?");
            params.add(township);
        }
        // 设施现状筛选
        if (facilityStatus != null && !facilityStatus.isEmpty()) {
            where.append(" AND k.facility_status = ?");
            params.add(facilityStatus);
        }

        if (cat != null && CATEGORY_TABLE.containsKey(cat)) {
            // 按部位查询：以部件表为主表，INNER JOIN kf_basedata
            return listByCategory(cat, where.toString(), params, pageNum, pageSize, status);
        } else {
            // 全部概览：以kf_basedata为主表
            return listAllOverview(where.toString(), params, pageNum, pageSize, status);
        }
    }

    /** 按部位查询部件列表 */
    private TableDataInfo<Map<String, Object>> listByCategory(String cat, String where, List<Object> params, int pageNum, int pageSize, String status) {
        String table = CATEGORY_TABLE.get(cat);
        String alias = cat.toLowerCase();
        String codeCol = getCharCodeColumn(cat);

        if (status != null && !status.isEmpty()) {
            where += " AND " + alias + ".evaluation = ?";
            params.add(status);
        }

        // 只查有该部件数据的烤房
        where += " AND " + alias + ".project_id IS NOT NULL";

        String countSql = "SELECT COUNT(*) as total FROM " + table + " " + alias +
                " INNER JOIN kf_basedata k ON " + alias + ".project_id = k.project_id " + where;
        Map<String, Object> countResult = jdbcTemplate.queryForMap(countSql, params.toArray());
        long total = ((Number) countResult.get("total")).longValue();

        int offset = (pageNum - 1) * pageSize;
        String listSql = "SELECT k.project_id, k.county, k.township, k.village, " +
                "k.detail_address, k.use_status, k.facility_status, " +
                "k.current_health_score, k.current_life_residual, " +
                alias + "." + codeCol + " as component_code, " +
                alias + ".evaluation, " + alias + ".data_year " +
                "FROM " + table + " " + alias +
                " INNER JOIN kf_basedata k ON " + alias + ".project_id = k.project_id " +
                where + " ORDER BY k.project_id LIMIT ? OFFSET ?";
        params.add(pageSize);
        params.add(offset);

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(listSql, params.toArray());

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Map<String, Object> m = new HashMap<>();
            m.put("projectId", row.get("project_id"));
            m.put("county", row.get("county"));
            m.put("township", row.get("township"));
            m.put("village", row.get("village"));
            m.put("detailAddress", row.get("detail_address"));
            m.put("useStatus", row.get("use_status"));
            m.put("facilityStatus", row.get("facility_status") != null ? row.get("facility_status") : "正常");
            m.put("healthScore", row.get("current_health_score"));
            m.put("lifeResidual", row.get("current_life_residual"));
            m.put("componentCode", row.get("component_code"));
            m.put("evaluation", row.get("evaluation"));
            m.put("dataYear", row.get("data_year"));
            m.put("category", cat);
            m.put("categoryName", CATEGORY_NAME.get(cat));
            result.add(m);
        }
        return TableDataInfo.build(result, total, pageNum, pageSize);
    }

    /** 全部概览查询 */
    private TableDataInfo<Map<String, Object>> listAllOverview(String where, List<Object> params, int pageNum, int pageSize, String status) {
        if (status != null && !status.isEmpty()) {
            where += " AND (h.evaluation = ? OR r.evaluation = ? OR c.evaluation = ? OR o.evaluation = ? OR a.evaluation = ?)";
            for (int i = 0; i < 5; i++) params.add(status);
        }

        String countSql = "SELECT COUNT(*) as total FROM kf_basedata k " +
                "LEFT JOIN kf_heater h ON k.project_id = h.project_id " +
                "LEFT JOIN kf_radiator r ON k.project_id = r.project_id " +
                "LEFT JOIN kf_controller c ON k.project_id = c.project_id " +
                "LEFT JOIN kf_oven_body o ON k.project_id = o.project_id " +
                "LEFT JOIN kf_annex a ON k.project_id = a.project_id " + where;
        Map<String, Object> countResult = jdbcTemplate.queryForMap(countSql, params.toArray());
        long total = ((Number) countResult.get("total")).longValue();

        int offset = (pageNum - 1) * pageSize;
        String listSql = "SELECT k.project_id, k.city, k.county, k.township, k.village, " +
                "k.detail_address, k.use_status, k.facility_status, " +
                "k.current_health_score, k.current_life_residual, " +
                "h.heater_code as jr_code, h.evaluation as jr_evaluation, " +
                "r.radiator_code as sr_code, r.evaluation as sr_evaluation, " +
                "c.controller_code as zk_code, c.evaluation as zk_evaluation, " +
                "o.body_code as zt_code, o.evaluation as zt_evaluation, " +
                "a.annex_code as fs_code, a.evaluation as fs_evaluation " +
                "FROM kf_basedata k " +
                "LEFT JOIN kf_heater h ON k.project_id = h.project_id " +
                "LEFT JOIN kf_radiator r ON k.project_id = r.project_id " +
                "LEFT JOIN kf_controller c ON k.project_id = c.project_id " +
                "LEFT JOIN kf_oven_body o ON k.project_id = o.project_id " +
                "LEFT JOIN kf_annex a ON k.project_id = a.project_id " +
                where + " ORDER BY k.project_id LIMIT ? OFFSET ?";
        params.add(pageSize);
        params.add(offset);

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(listSql, params.toArray());

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Map<String, Object> m = new HashMap<>();
            m.put("projectId", row.get("project_id"));
            m.put("county", row.get("county"));
            m.put("township", row.get("township"));
            m.put("village", row.get("village"));
            m.put("detailAddress", row.get("detail_address"));
            m.put("useStatus", row.get("use_status"));
            m.put("facilityStatus", row.get("facility_status") != null ? row.get("facility_status") : "正常");
            m.put("healthScore", row.get("current_health_score"));
            m.put("lifeResidual", row.get("current_life_residual"));
            m.put("jrCode", row.get("jr_code"));
            m.put("srCode", row.get("sr_code"));
            m.put("zkCode", row.get("zk_code"));
            m.put("ztCode", row.get("zt_code"));
            m.put("fsCode", row.get("fs_code"));
            m.put("jrEvaluation", row.get("jr_evaluation"));
            m.put("srEvaluation", row.get("sr_evaluation"));
            m.put("zkEvaluation", row.get("zk_evaluation"));
            m.put("ztEvaluation", row.get("zt_evaluation"));
            m.put("fsEvaluation", row.get("fs_evaluation"));
            result.add(m);
        }
        return TableDataInfo.build(result, total, pageNum, pageSize);
    }

    /** 获取各部件表的编码列名 */
    private String getCharCodeColumn(String cat) {
        switch (cat) {
            case "JR": return "heater_code";
            case "SR": return "radiator_code";
            case "ZK": return "controller_code";
            case "ZT": return "body_code";
            case "FS": return "annex_code";
            default: return "heater_code";
        }
    }

    // ======================== API 2: 获取某烤房全部5大部位详情 ========================

    /**
     * 获取某烤房的全部5大部位部件详情
     * 返回 baseInfo + JR/SR/ZK/ZT/FS 各部位完整字段
     *
     * @param projectId 项目编号（即烤房编号）
     */
    @GetMapping("/{projectId}")
    public R<Map<String, Object>> detail(@PathVariable String projectId) {
        // 查询基础信息
        List<Map<String, Object>> baseRows = jdbcTemplate.queryForList(
                "SELECT * FROM kf_basedata WHERE project_id = ?", projectId);
        if (baseRows.isEmpty()) {
            return R.fail("项目不存在: " + projectId);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("baseInfo", baseRows.get(0));
        result.put("JR", queryCategoryDetail("kf_heater", projectId));
        result.put("SR", queryCategoryDetail("kf_radiator", projectId));
        result.put("ZK", queryCategoryDetail("kf_controller", projectId));
        result.put("ZT", queryCategoryDetail("kf_oven_body", projectId));
        result.put("FS", queryCategoryDetail("kf_annex", projectId));

        return R.ok(result);
    }

    // ======================== API 3: 按部位查看特定部位详情 ========================

    /**
     * 按部位查看某个烤房的特定部位
     * 返回 baseInfo + 指定部位的完整字段
     *
     * @param projectId 项目编号（即烤房编号）
     * @param category  部位代码 JR/SR/ZK/ZT/FS
     */
    @GetMapping("/{projectId}/{category}")
    public R<Map<String, Object>> categoryDetail(@PathVariable String projectId,
                                                  @PathVariable String category) {
        String cat = category.toUpperCase();
        String table = CATEGORY_TABLE.get(cat);
        if (table == null) {
            return R.fail("无效的部位代码: " + category + "，支持的部位: JR/SR/ZK/ZT/FS");
        }

        // 查询基础信息
        List<Map<String, Object>> baseRows = jdbcTemplate.queryForList(
                "SELECT * FROM kf_basedata WHERE project_id = ?", projectId);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("baseInfo", baseRows.isEmpty() ? null : baseRows.get(0));
        result.put("category", cat);
        result.put("categoryName", CATEGORY_NAME.get(cat));
        result.put("detail", queryCategoryDetail(table, projectId));

        return R.ok(result);
    }

    // ======================== 私有辅助方法 ========================

    /**
     * 查询单个部位表的详情（全部字段）
     *
     * @param table     物理表名（如 kf_heater）
     * @param projectId 项目编号
     * @return 该部位的完整记录，不存在则返回null
     */
    private Map<String, Object> queryCategoryDetail(String table, String projectId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT * FROM " + table + " WHERE project_id = ?", projectId);
        return rows.isEmpty() ? null : rows.get(0);
    }

    /**
     * 拼接非空字符串（与 BarnProjectController 保持一致的风格）
     */
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
