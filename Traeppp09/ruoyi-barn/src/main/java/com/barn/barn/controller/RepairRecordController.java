package com.barn.barn.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.barn.barn.entity.RepairRecord;
import com.barn.barn.mapper.RepairRecordMapper;
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

@RestController
@RequestMapping("/barn/repair")
public class RepairRecordController {

    @Autowired
    private RepairRecordMapper repairRecordMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @GetMapping("/list")
    public TableDataInfo<Map<String, Object>> list(@RequestParam(defaultValue = "1") int pageNum,
                                                    @RequestParam(defaultValue = "10") int pageSize,
                                                    @RequestParam(required = false) String ovenId,
                                                    @RequestParam(required = false) String status,
                                                    @RequestParam(required = false) String repairType,
                                                    @RequestParam(required = false) String urgency,
                                                    @RequestParam(required = false) String repairYear,
                                                    @RequestParam(required = false) String replacementType,
                                                    @RequestParam(required = false) String componentName) {
        StringBuilder where = new StringBuilder("WHERE 1=1");
        List<Object> params = new ArrayList<>();

        // 烤房编号模糊搜索
        if (ovenId != null && !ovenId.trim().isEmpty()) {
            where.append(" AND oven_id LIKE ?");
            params.add("%" + ovenId.trim() + "%");
        }
        // 维修状态搜索
        if (status != null && !status.isEmpty()) {
            where.append(" AND repair_status = ?");
            params.add(status);
        }
        // 维修类型搜索
        if (repairType != null && !repairType.isEmpty()) {
            where.append(" AND repair_type = ?");
            params.add(repairType);
        }
        // 紧迫性搜索
        if (urgency != null && !urgency.isEmpty()) {
            where.append(" AND urgency = ?");
            params.add(urgency);
        }
        // 维修年度搜索
        if (repairYear != null && !repairYear.isEmpty()) {
            where.append(" AND repair_year = ?");
            params.add(repairYear);
        }
        // 更换类型搜索
        if (replacementType != null && !replacementType.isEmpty()) {
            where.append(" AND replacement_type = ?");
            params.add(replacementType);
        }
        // 部件名称搜索
        if (componentName != null && !componentName.isEmpty()) {
            where.append(" AND component_name LIKE ?");
            params.add("%" + componentName + "%");
        }

        // 查询总数
        String countSql = "SELECT COUNT(*) as total FROM repair_records " + where.toString();
        Map<String, Object> countResult = jdbcTemplate.queryForMap(countSql, params.toArray());
        long total = ((Number) countResult.get("total")).longValue();

        // 分页查询 - 包含所有原有字段及新增字段
        int offset = (pageNum - 1) * pageSize;
        String listSql = "SELECT id, oven_id, oven_name, repair_type, urgency, applicant, apply_time, apply_desc, " +
                "estimated_cost, actual_cost, repair_status, auditor, audit_time, audit_opinion, " +
                "implement_team, start_date, end_date, acceptor, accept_time, accept_result, " +
                "accept_opinion, roi_score, created_by, created_at, updated_by, updated_at, remark, " +
                "invest_year, build_spec, heater_type, repair_year, repair_code, industry_amount, " +
                "county, township, village, group_name, place_name, " +
                "component_name, component_raw, repair_action, component_quantity, component_unit, " +
                "replacement_type, repair_content " +
                "FROM repair_records " + where.toString() +
                " ORDER BY apply_time DESC LIMIT ? OFFSET ?";
        params.add(pageSize);
        params.add(offset);

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(listSql, params.toArray());

        // 格式化返回数据 - 字段名转驼峰
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", row.get("id"));
            m.put("ovenId", row.get("oven_id"));
            m.put("ovenName", row.get("oven_name"));
            m.put("repairType", row.get("repair_type"));
            m.put("urgency", row.get("urgency"));
            m.put("applicant", row.get("applicant"));
            m.put("applyTime", row.get("apply_time"));
            m.put("applyDesc", row.get("apply_desc"));
            m.put("estimatedCost", row.get("estimated_cost"));
            m.put("actualCost", row.get("actual_cost"));
            m.put("repairStatus", row.get("repair_status"));
            m.put("auditor", row.get("auditor"));
            m.put("auditTime", row.get("audit_time"));
            m.put("auditOpinion", row.get("audit_opinion"));
            m.put("implementTeam", row.get("implement_team"));
            m.put("startDate", row.get("start_date"));
            m.put("endDate", row.get("end_date"));
            m.put("acceptor", row.get("acceptor"));
            m.put("acceptTime", row.get("accept_time"));
            m.put("acceptResult", row.get("accept_result"));
            m.put("acceptOpinion", row.get("accept_opinion"));
            m.put("roiScore", row.get("roi_score"));
            m.put("createdBy", row.get("created_by"));
            m.put("createdAt", row.get("created_at"));
            m.put("updatedBy", row.get("updated_by"));
            m.put("updatedAt", row.get("updated_at"));
            m.put("remark", row.get("remark"));
            // 新增字段
            m.put("investYear", row.get("invest_year"));
            m.put("buildSpec", row.get("build_spec"));
            m.put("heaterType", row.get("heater_type"));
            m.put("repairYear", row.get("repair_year"));
            m.put("repairCode", row.get("repair_code"));
            m.put("industryAmount", row.get("industry_amount"));
            m.put("county", row.get("county"));
            m.put("township", row.get("township"));
            m.put("village", row.get("village"));
            m.put("groupName", row.get("group_name"));
            m.put("placeName", row.get("place_name"));
            // 部件相关新增字段
            m.put("componentName", row.get("component_name"));
            m.put("componentRaw", row.get("component_raw"));
            m.put("repairAction", row.get("repair_action"));
            m.put("componentQuantity", row.get("component_quantity"));
            m.put("componentUnit", row.get("component_unit"));
            m.put("replacementType", row.get("replacement_type"));
            m.put("repairContent", row.get("repair_content"));
            result.add(m);
        }

        return TableDataInfo.build(result, total, pageNum, pageSize);
    }

    @GetMapping("/{id}")
    public R<RepairRecord> getInfo(@PathVariable Long id) {
        return R.ok(repairRecordMapper.selectById(id));
    }

    @PostMapping
    public R<Void> add(@RequestBody RepairRecord record) {
        repairRecordMapper.insert(record);
        return R.ok();
    }

    @PutMapping
    public R<Void> edit(@RequestBody RepairRecord record) {
        repairRecordMapper.updateById(record);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    public R<Void> remove(@PathVariable Long id) {
        repairRecordMapper.deleteById(id);
        return R.ok();
    }

    @GetMapping("/stats")
    public R<Map<String, Object>> stats() {
        List<RepairRecord> all = repairRecordMapper.selectList(null);
        Map<String, Object> result = new java.util.HashMap<>();
        int pending = 0, processing = 0, completed = 0;
        java.math.BigDecimal totalCost = java.math.BigDecimal.ZERO;
        for (RepairRecord r : all) {
            if ("待审核".equals(r.getRepairStatus())) pending++;
            else if ("处理中".equals(r.getRepairStatus())) processing++;
            else if ("已完成".equals(r.getRepairStatus())) completed++;
            if (r.getActualCost() != null) totalCost = totalCost.add(r.getActualCost());
        }
        result.put("total", all.size());
        result.put("pending", pending);
        result.put("processing", processing);
        result.put("completed", completed);
        result.put("totalCost", totalCost);
        return R.ok(result);
    }

    /**
     * 维修分析 - 按年度、类型、部件统计
     */
    @GetMapping("/analysis")
    public R<Map<String, Object>> analysis() {
        Map<String, Object> result = new HashMap<>();

        // 1. 按更换类型统计
        String typeSql = "SELECT replacement_type, COUNT(*) as cnt, " +
                "COALESCE(SUM(estimated_cost),0) as total_cost, " +
                "COALESCE(SUM(industry_amount),0) as total_industry " +
                "FROM repair_records WHERE replacement_type IS NOT NULL " +
                "GROUP BY replacement_type ORDER BY cnt DESC";
        List<Map<String, Object>> typeStats = jdbcTemplate.queryForList(typeSql);
        result.put("typeStats", typeStats);

        // 2. 按年度统计
        String yearSql = "SELECT repair_year, COUNT(*) as cnt, " +
                "COALESCE(SUM(estimated_cost),0) as total_cost, " +
                "COALESCE(SUM(industry_amount),0) as total_industry " +
                "FROM repair_records WHERE repair_year IS NOT NULL AND repair_year != '' " +
                "GROUP BY repair_year ORDER BY repair_year";
        List<Map<String, Object>> yearStats = jdbcTemplate.queryForList(yearSql);
        result.put("yearStats", yearStats);

        // 3. 按部件统计
        String compSql = "SELECT component_name, COUNT(*) as cnt, " +
                "COALESCE(SUM(estimated_cost),0) as total_cost, " +
                "COALESCE(SUM(component_quantity),0) as total_quantity " +
                "FROM repair_records WHERE component_name IS NOT NULL AND component_name != '' " +
                "GROUP BY component_name ORDER BY cnt DESC LIMIT 15";
        List<Map<String, Object>> compStats = jdbcTemplate.queryForList(compSql);
        result.put("componentStats", compStats);

        // 4. 按县区统计
        String countySql = "SELECT county, COUNT(*) as cnt, " +
                "COALESCE(SUM(estimated_cost),0) as total_cost " +
                "FROM repair_records WHERE county IS NOT NULL AND county != '' " +
                "GROUP BY county ORDER BY cnt DESC";
        List<Map<String, Object>> countyStats = jdbcTemplate.queryForList(countySql);
        result.put("countyStats", countyStats);

        // 5. 年度×类型交叉统计
        String crossSql = "SELECT repair_year, replacement_type, " +
                "COUNT(*) as cnt, COALESCE(SUM(estimated_cost),0) as total_cost " +
                "FROM repair_records WHERE repair_year IS NOT NULL AND repair_year != '' " +
                "AND replacement_type IS NOT NULL " +
                "GROUP BY repair_year, replacement_type ORDER BY repair_year, replacement_type";
        List<Map<String, Object>> crossStats = jdbcTemplate.queryForList(crossSql);
        result.put("crossStats", crossStats);

        // 6. 总览数据
        String overviewSql = "SELECT COUNT(*) as total_records, " +
                "COUNT(DISTINCT oven_id) as total_barns, " +
                "COALESCE(SUM(estimated_cost),0) as total_cost, " +
                "COALESCE(SUM(industry_amount),0) as total_industry, " +
                "COUNT(DISTINCT repair_year) as total_years " +
                "FROM repair_records";
        Map<String, Object> overview = jdbcTemplate.queryForMap(overviewSql);
        result.put("overview", overview);

        // ---------- 智能分析：ROI / 维修优先级 / 寿命预警 / 维修方案 ----------
        // 烤房基础健康分/寿命（内存匹配，规避 collation 差异）
        Map<String, Map<String, Object>> kfById = new HashMap<>();
        List<Map<String, Object>> kfRows = jdbcTemplate.queryForList(
            "SELECT project_id, county, township, village, health_score_final, life_residual_final FROM kf_basedata");
        for (Map<String, Object> row : kfRows) {
            kfById.put(String.valueOf(row.get("project_id")), row);
        }

        // 维修记录按烤房聚合（含部件/投入）
        Map<String, List<Map<String, Object>>> repairByOven = new HashMap<>();
        List<Map<String, Object>> aggRows = jdbcTemplate.queryForList(
            "SELECT oven_id, replacement_type, repair_action, component_name, " +
            "COALESCE(SUM(estimated_cost),0) total_cost, COALESCE(SUM(industry_amount),0) total_industry, COUNT(*) n " +
            "FROM repair_records WHERE oven_id IS NOT NULL " +
            "GROUP BY oven_id, replacement_type, repair_action, component_name");
        for (Map<String, Object> row : aggRows) {
            String oven = String.valueOf(row.get("oven_id"));
            repairByOven.computeIfAbsent(oven, k -> new ArrayList<>()).add(row);
        }

        List<Map<String, Object>> roiList = new ArrayList<>();
        List<Map<String, Object>> priorityList = new ArrayList<>();

        for (Map.Entry<String, List<Map<String, Object>>> entry : repairByOven.entrySet()) {
            String oven = entry.getKey();
            List<Map<String, Object>> list = entry.getValue();

            double totalCost = 0, totalIndustry = 0, extSum = 0, boostSum = 0;
            Map<String, Double> extByComp = new HashMap<>();
            int times = 0;
            for (Map<String, Object> row : list) {
                String replacement = strVal(row.get("replacement_type"));
                String action = strVal(row.get("repair_action"));
                String comp = strVal(row.get("component_name"));
                totalCost += numVal(row.get("total_cost"));
                totalIndustry += numVal(row.get("total_industry"));
                double ext = lifeExt(replacement, action);
                double boost = healthBoost(replacement, action);
                extSum += ext;
                boostSum += boost;
                times += ((Number) row.get("n")).intValue();
                extByComp.merge(comp, ext, Double::sum);
            }
            extSum = Math.min(extSum, 6.0);
            boostSum = Math.min(boostSum, 12.0);

            Map<String, Object> kf = kfById.get(oven);
            double health = 0;
            double life = 0;
            String county = "", township = "", village = "";
            if (kf != null) {
                health = kf.get("health_score_final") == null ? 0 : ((Number) kf.get("health_score_final")).doubleValue();
                life = kf.get("life_residual_final") == null ? 0 : ((Number) kf.get("life_residual_final")).doubleValue();
                county = strVal(kf.get("county"));
                township = strVal(kf.get("township"));
                village = strVal(kf.get("village"));
            }

            // 主导维修部件（延寿贡献最大）
            String topComp = "";
            double topExt = 0;
            for (Map.Entry<String, Double> ce : extByComp.entrySet()) {
                if (!ce.getKey().isEmpty() && ce.getValue() > topExt) {
                    topExt = ce.getValue();
                    topComp = ce.getKey();
                }
            }

            // ROI：投入 = 工程造价+行业投入，收益 = 延寿年数×1.2万 + 健康加分×150
            double invest = totalCost + totalIndustry;
            double benefitValue = extSum * 12000 + boostSum * 150;
            double roi = invest > 0 ? Math.round(benefitValue / invest * 100.0) / 100.0 : 0;

            Map<String, Object> roiRow = new LinkedHashMap<>();
            roiRow.put("barnId", oven);
            roiRow.put("county", county);
            roiRow.put("township", township);
            roiRow.put("village", village);
            roiRow.put("component", topComp.isEmpty() ? "整体" : topComp);
            roiRow.put("repairTimes", times);
            roiRow.put("invest", invest);
            roiRow.put("lifeGain", Math.round(extSum * 10.0) / 10.0);
            roiRow.put("healthGain", Math.round(boostSum * 10.0) / 10.0);
            roiRow.put("roi", roi);
            roiRow.put("healthScore", health);
            roiRow.put("lifeResidual", life);
            roiList.add(roiRow);

            // 维修优先级：寿命低/健康低/维修次数多 → 更高优先级
            double riskScore = 0;
            riskScore += Math.max(0, 7 - life) * 8;                           // 剩余寿命低于7年
            riskScore += Math.max(0, 75 - health) / 75.0 * 20;                 // 健康分低于75
            riskScore += Math.min(6, times) * 2;                              // 维修次数
            String priority = riskScore >= 30 ? "高" : (riskScore >= 18 ? "中" : "低");
            String warning = life <= 4 ? "寿命预警" : (health < 60 ? "健康预警" : (life <= 7 ? "关注" : "正常"));

            Map<String, Object> pr = new LinkedHashMap<>();
            pr.put("barnId", oven);
            pr.put("county", county);
            pr.put("township", township);
            pr.put("village", village);
            pr.put("healthScore", health);
            pr.put("healthLevel", getAnalysisLevel(health));
            pr.put("lifeResidual", life);
            pr.put("warnLevel", getAnalysisWarn(life));
            pr.put("repairTimes", times);
            pr.put("priority", priority);
            pr.put("priorityScore", Math.round(riskScore * 10.0) / 10.0);
            pr.put("warning", warning);
            priorityList.add(pr);
        }

        // 按优先级分数倒序
        priorityList.sort((a, b) -> Double.compare(((Number) b.get("priorityScore")).doubleValue(),
                ((Number) a.get("priorityScore")).doubleValue()));

        // ROI排行（返回完整列表，前端做展示/筛选/分页）
        roiList.sort((a, b) -> Double.compare((Double) b.get("roi"), (Double) a.get("roi")));

        // 维修方案：对高优先级/高风险的烤房给出处置建议
        List<Map<String, Object>> maintenancePlan = new ArrayList<>();
        int planCount = 0;
        for (Map<String, Object> pr : priorityList) {
            if (planCount >= 10) break;
            String barnId = String.valueOf(pr.get("barnId"));
            double life = ((Number) pr.get("lifeResidual")).doubleValue();
            double health = ((Number) pr.get("healthScore")).doubleValue();
            // 找到该烤房主导维修部件
            String comp = "关键部件";
            double topExt = 0;
            List<Map<String, Object>> ovenRows = repairByOven.get(barnId);
            if (ovenRows != null) {
                for (Map<String, Object> row : ovenRows) {
                    String c = strVal(row.get("component_name"));
                    double e = lifeExt(strVal(row.get("replacement_type")), strVal(row.get("repair_action")));
                    if (!c.isEmpty() && e > topExt) {
                        topExt = e;
                        comp = c;
                    }
                }
            }
            Map<String, Object> plan = new LinkedHashMap<>(pr);
            plan.put("component", comp);
            plan.put("recommendation", buildRecommendAction(comp, life, health));
            maintenancePlan.add(plan);
            planCount++;
        }

        result.put("roiList", roiList);
        result.put("priorityList", priorityList);
        result.put("maintenancePlan", maintenancePlan);

        return R.ok(result);
    }

    /** 维修寿命续期（年） */
    private double lifeExt(String replacement, String action) {
        double ext;
        if (replacement.contains("新采购")) ext = 2.0;
        else if (replacement.contains("改造升级")) ext = 1.5;
        else if (replacement.contains("防护加固")) ext = 1.0;
        else ext = 0.8;
        if (action != null && (action.contains("更换") || action.contains("替换"))) ext += 0.5;
        return ext;
    }

    /** 维修健康加分 */
    private double healthBoost(String replacement, String action) {
        double boost;
        if (replacement.contains("新采购")) boost = 5;
        else if (replacement.contains("改造升级")) boost = 4;
        else if (replacement.contains("防护加固") || replacement.contains("维护修复")) boost = 2;
        else boost = 3;
        if (action != null && (action.contains("更换") || action.contains("替换"))) boost += 2;
        return boost;
    }

    /** 健康等级 */
    private String getAnalysisLevel(double score) {
        if (score >= 90) return "优良";
        if (score >= 75) return "良好";
        if (score >= 60) return "一般";
        if (score >= 40) return "预警";
        return "危险";
    }

    /** 寿命预警等级 */
    private String getAnalysisWarn(double residual) {
        if (residual <= 0) return "紧急维修";
        if (residual <= 4) return "建议维修";
        if (residual <= 7) return "需关注";
        return "正常";
    }

    /** 属性转字符串 */
    private String strVal(Object o) {
        return o == null ? "" : String.valueOf(o).trim();
    }

    /** 数值转换 */
    private double numVal(Object o) {
        if (o == null) return 0;
        try { return ((Number) o).doubleValue(); } catch (Exception e) { return 0; }
    }

    /** 生成维修方案建议 */
    private String buildRecommendAction(String comp, double life, double health) {
        String detail = "常规巡检";
        if (comp.contains("燃烧机") || comp.contains("发热体") || comp.contains("供热")) detail = "更换燃烧机/发热体";
        else if (comp.contains("自控") || comp.contains("电机")) detail = "检修并更换自控设备/电机";
        else if (comp.contains("风机")) detail = "更换风机及传动轴承";
        else if (comp.contains("挂烟梁")) detail = "加固并更换挂烟梁";
        else if (comp.contains("屋顶") || comp.contains("主体")) detail = "屋面防水与主体结构加固";
        else if (comp.contains("防水")) detail = "屋面防水层翻新施工";
        else if (comp.contains("操作棚") || comp.contains("附属")) detail = "附属设施维护更新";
        else detail = "对关键部件检修加固";

        if (life <= 0) return "【紧急】" + detail + "：寿命已耗尽，建议当年安排大修或整体更换";
        if (life <= 4) return "【建议】" + detail + "：剩余寿命有限，建议未来1-2年内完成专项维修";
        if (health < 60) return "【关注】" + detail + "：健康分偏低，建议加强巡检并择机维修";
        return "【维护】" + detail + "：状态稳定，按计划进行常规保养";
    }
}
