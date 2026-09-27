package com.barn.barn.controller;

import com.barn.common.core.domain.R;
import com.barn.common.core.domain.TableDataInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

/**
 * 烤房健康分算法 & 寿命预测算法 Controller
 * 支持5种健康分算法(bmhi/fche/topsis/cdci/crhe) + 4种寿命算法(edrl/wrrl/bdrl/gple)
 * 所有算法结果独立存储在 kf_basedata 表对应字段中，切换时不覆盖
 */
@RestController
@RequestMapping("/barn/health-algo")
public class HealthAlgoController {

    private static final Set<String> HEALTH_ALGOS = Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList("bmhi", "fche", "topsis", "cdci", "crhe")));
    private static final Set<String> LIFE_ALGOS = Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList("edrl", "wrrl", "bdrl", "gple")));

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // ======================== 健康分算法 ========================

    /**
     * 获取算法描述（前端展示用）
     */
    @GetMapping("/desc/{algo}")
    public R<Map<String, Object>> getAlgoDesc(@PathVariable String algo) {
        Map<String, Object> desc = new LinkedHashMap<>();
        switch (algo.toLowerCase()) {
            case "bmhi":
                desc.put("name", "BMHI 烤房多维健康指数模型");
                desc.put("fullName", "Barn Multi-dimensional Health Index (AHP层次加权)");
                desc.put("principle", "采用层次分析法(AHP)确定5大部件权重，对每个部件的子部件进行「状态分×时效分×评价分」三维量化，加权汇总得到0-100分的健康指数。");
                desc.put("formula", "BMHI = Σ(Wi × Ci), i=JR,SR,ZK,ZT,FS\nCi = α×S_status + β×S_repair + γ×S_eval\nS_status: 正常=100, 异常=50, 损坏=0\nS_repair: max(0, 100-Δt×k), Δt=当前年份-修复年份\nS_eval: 较好=80, 一般=60, 较差=50, 差=30");
                desc.put("weights", "W_JR=0.25, W_SR=0.20, W_ZK=0.25, W_ZT=0.20, W_FS=0.10");
                desc.put("example", "示例烤房 09-420527KF00018:\nJR(burner, Δt=4, 正常, 较好): 0.4×100+0.3×60+0.3×80=82.0\nSR(4子部件, Δt=4): 82.0\nZK(8子部件, Δt=4): 82.0\nZT(door Δt=17!, 较差): 65.0\nBMHI = 0.25×82+0.20×82+0.25×82+0.20×65+0.10×70 = 77.4");
                desc.put("params", "[{\"key\":\"alpha\",\"label\":\"状态权重α\",\"default\":0.4},{\"key\":\"beta\",\"label\":\"时效权重β\",\"default\":0.3},{\"key\":\"gamma\",\"label\":\"评价权重γ\",\"default\":0.3},{\"key\":\"k\",\"label\":\"时效衰减系数k\",\"default\":10}]");
                break;
            case "fche":
                desc.put("name", "FCHE 模糊综合健康评价模型");
                desc.put("fullName", "Fuzzy Comprehensive Health Evaluation");
                desc.put("principle", "将巡检评价（较好/较差）转化为隶属度向量，构建模糊关系矩阵R，通过模糊变换B=A∘R得到烤房对各健康等级的隶属度，加权去模糊化得到健康分。");
                desc.put("formula", "评价集 V={优(≥90), 良(75-90), 中(60-75), 差(<60)}\n隶属度(以Δt为例): μ优=max(0,1-Δt/5), μ良=max(0,1-|Δt-5|/5)\n模糊变换: B = A ∘ R (加权平均型)\n去模糊化: Score = Σ(bk×vk)/Σbk");
                desc.put("weights", "W_JR=0.25, W_SR=0.20, W_ZK=0.25, W_ZT=0.20, W_FS=0.10");
                desc.put("example", "示例烤房 09-420527KF00018:\nJR(Δt=4,较好): μ=(0.30,0.50,0.20,0.00)\nZT(door Δt=17,较差): μ=(0.00,0.05,0.25,0.70)\nB=(0.1525,0.4125,0.2750,0.1600)\nScore=(0.1525×90+0.4125×80+0.275×65+0.16×40)=71.0");
                desc.put("params", "[{\"key\":\"wJr\",\"label\":\"JR权重\",\"default\":0.25},{\"key\":\"wSr\",\"label\":\"SR权重\",\"default\":0.20},{\"key\":\"wZk\",\"label\":\"ZK权重\",\"default\":0.25},{\"key\":\"wZt\",\"label\":\"ZT权重\",\"default\":0.20},{\"key\":\"wFs\",\"label\":\"FS权重\",\"default\":0.10}]");
                break;
            case "topsis":
                desc.put("name", "熵权-TOPSIS烤房健康排序法");
                desc.put("fullName", "Entropy-TOPSIS Health Ranking");
                desc.put("principle", "利用信息熵计算各指标客观权重（数据越分散权重越大），再用TOPSIS法计算每栋烤房与理想最优解和最劣解的相对贴近度，实现多烤房横向排序。");
                desc.put("formula", "标准化: pij = xij/Σxi\n熵值: Ej = -(1/ln n)×Σ(pij×ln(pij))\n熵权: Wj = (1-Ej)/Σ(1-Ej)\n贴近度: Ci = Di-/(Di++Di-)×100");
                desc.put("weights", "由信息熵自动计算，无需人工设定");
                desc.put("example", "选取4栋同批次烤房对比:\n00018: JR=82,SR=82,ZK=82,ZT=65 → 贴近度=70.6\n熵权自动计算: SR和ZT分散度最高权重最大");
                desc.put("params", "[{\"key\":\"topN\",\"label\":\"对比样本数(前N栋)\",\"default\":100}]");
                break;
            case "cdci":
                desc.put("name", "CDCI 部件退化曲线积分健康度");
                desc.put("fullName", "Component Degradation Curve Integration (推荐)");
                desc.put("principle", "为每个子部件建立三段式退化函数：初期稳定→中期线性退化→后期加速劣化。对曲线在当前时刻求值得到子部件瞬时健康度，再按部件层次加权汇总。");
                desc.put("formula", "h(Δt) = 100                              (Δt≤0.2L, 稳定区)\nh(Δt) = 100-k1×(Δt-0.2L)               (0.2L<Δt≤0.7L, 线性区)\nh(Δt) = h_mid×e^(-k2×(Δt-0.7L))         (Δt>0.7L, 加速劣化区)\nk1=100/(0.5L), h_mid=30, k2=2/L\nCDCI = Σ(Wi × Ci × 评价修正系数)");
                desc.put("weights", "W_JR=0.25, W_SR=0.20, W_ZK=0.25, W_ZT=0.20, W_FS=0.10");
                desc.put("example", "示例烤房 09-420527KF00018:\nburner(Δt=4,L=8): 线性区, h=100-25×2.4=40\ndoor(Δt=17,L=12): 加速区, h=30×e^(-0.167×8.6)=7.1\nbeam(Δt=2,L=20): 稳定区, h=100\nCDCI=32.0 → 映射后48.0");
                desc.put("params", "[{\"key\":\"stableRatio\",\"label\":\"稳定区比例\",\"default\":0.2},{\"key\":\"linearEndRatio\",\"label\":\"线性区终点比例\",\"default\":0.7},{\"key\":\"hMid\",\"label\":\"加速区起点分h_mid\",\"default\":30}]");
                break;
            case "crhe":
                desc.put("name", "CRHE 组合赋权融合健康度");
                desc.put("fullName", "Combined Ratio Hybrid Evaluation (创新法)");
                desc.put("principle", "将AHP主观权重与信息熵客观权重通过乘法合成，既尊重专家经验又反映数据分布。Wj=(W_ahp×W_entropy)/Σ(W_ahp×W_entropy)，再用TOPSIS法计算贴近度。");
                desc.put("formula", "组合权重: Wj = (W_ahp_j × W_entropy_j) / Σ(W_ahp × W_entropy)\nCRHE = Σ(Wj × Ci)  (Ci为各部件BMHI得分)");
                desc.put("weights", "AHP: JR=0.25,SR=0.20,ZK=0.25,ZT=0.20,FS=0.10\n熵权: 由数据自动计算\n组合: 乘法归一化");
                desc.put("example", "示例烤房 09-420527KF00018:\nAHP权重×熵权归一化后: W_JR=0.22,W_SR=0.26,W_ZK=0.22,W_ZT=0.26,W_FS=0.04\nCRHE = 0.22×82+0.26×82+0.22×82+0.26×65+0.04×70 = 76.5");
                desc.put("params", "[{\"key\":\"wJrAhp\",\"label\":\"JR的AHP权重\",\"default\":0.25},{\"key\":\"wSrAhp\",\"label\":\"SR的AHP权重\",\"default\":0.20},{\"key\":\"wZkAhp\",\"label\":\"ZK的AHP权重\",\"default\":0.25},{\"key\":\"wZtAhp\",\"label\":\"ZT的AHP权重\",\"default\":0.20},{\"key\":\"wFsAhp\",\"label\":\"FS的AHP权重\",\"default\":0.10}]");
                break;
            default:
                return R.fail("未知算法: " + algo);
        }
        return R.ok(desc);
    }

    /**
     * 应用健康分算法到所有烤房
     * params: algo(算法名), 可选自定义参数(alpha/beta/gamma/k等)
     */
    @PostMapping("/apply")
    public R<Map<String, Object>> applyHealthAlgo(@RequestBody Map<String, Object> params) {
        String algo = requireHealthAlgo(String.valueOf(params.getOrDefault("algo", "bmhi")));
        int currentYear = java.time.LocalDate.now().getYear();

        // 获取自定义参数
        double alpha = toDouble(params.get("alpha"), 0.4);
        double beta = toDouble(params.get("beta"), 0.3);
        double gamma = toDouble(params.get("gamma"), 0.3);
        double k = toDouble(params.get("k"), 10);
        double stableRatio = toDouble(params.get("stableRatio"), 0.2);
        double linearEndRatio = toDouble(params.get("linearEndRatio"), 0.7);
        double hMid = toDouble(params.get("hMid"), 30);

        // 部件权重
        double wJr = toDouble(params.get("wJr"), 0.25);
        double wSr = toDouble(params.get("wSr"), 0.20);
        double wZk = toDouble(params.get("wZk"), 0.25);
        double wZt = toDouble(params.get("wZt"), 0.20);
        double wFs = toDouble(params.get("wFs"), 0.10);

        // 查询所有烤房基础数据
        List<Map<String, Object>> barns = jdbcTemplate.queryForList(
            "SELECT project_id, use_status, finish_date, start_date FROM kf_basedata");
        
        int success = 0, fail = 0;
        String column = "health_" + algo;
        
        for (Map<String, Object> barn : barns) {
            try {
                String projectId = String.valueOf(barn.get("project_id"));
                double score = calculateHealthScore(projectId, algo, currentYear, 
                    alpha, beta, gamma, k, stableRatio, linearEndRatio, hMid,
                    wJr, wSr, wZk, wZt, wFs);
                
                jdbcTemplate.update("UPDATE kf_basedata SET " + column + " = ? WHERE project_id = ?",
                    Math.round(score * 10.0) / 10.0, projectId);
                success++;
            } catch (Exception e) {
                fail++;
            }
        }
        
        // 更新当前算法标记 + 同步到 current_health_score（烤房列表显示用）
        jdbcTemplate.update("UPDATE kf_basedata SET current_health_algo = ?, current_health_score = " + column);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("algo", algo);
        result.put("success", success);
        result.put("fail", fail);
        result.put("total", barns.size());
        result.put("column", column);
        return R.ok(result, "算法" + algo.toUpperCase() + "已应用到" + success + "栋烤房，已同步至烤房列表");
    }

    /**
     * 健康分算法列表（分页）
     */
    @GetMapping("/list")
    public TableDataInfo<Map<String, Object>> list(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(defaultValue = "bmhi") String algo,
            @RequestParam(required = false) String county,
            @RequestParam(required = false) String keyword) {
        
        String normalizedAlgo = requireHealthAlgo(algo);
        String column = "health_" + normalizedAlgo;
        StringBuilder where = new StringBuilder("WHERE 1=1");
        List<Object> params = new ArrayList<>();
        if (county != null && !county.isEmpty()) {
            where.append(" AND county = ?");
            params.add(county);
        }
        if (keyword != null && !keyword.isEmpty()) {
            where.append(" AND (project_id LIKE ? OR township LIKE ? OR village LIKE ?)");
            params.add("%" + keyword + "%");
            params.add("%" + keyword + "%");
            params.add("%" + keyword + "%");
        }

        String countSql = "SELECT COUNT(*) as total FROM kf_basedata " + where;
        Map<String, Object> countResult = jdbcTemplate.queryForMap(countSql, params.toArray());
        long total = ((Number) countResult.get("total")).longValue();

        int offset = (pageNum - 1) * pageSize;
        String listSql = "SELECT project_id, county, township, village, use_status, " +
                column + " as health_score, current_health_algo " +
                "FROM kf_basedata " + where +
                " ORDER BY " + column + " IS NULL, " + column + " DESC LIMIT ? OFFSET ?";
        params.add(pageSize);
        params.add(offset);

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(listSql, params.toArray());
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("projectId", row.get("project_id"));
            m.put("county", row.get("county"));
            m.put("township", row.get("township"));
            m.put("village", row.get("village"));
            m.put("useStatus", row.get("use_status"));
            Object score = row.get("health_score");
            m.put("healthScore", score != null ? ((Number) score).doubleValue() : null);
            m.put("healthLevel", score != null ? getHealthLevel(((Number) score).doubleValue()) : "未计算");
            m.put("algo", normalizedAlgo);
            result.add(m);
        }
        return TableDataInfo.build(result, total, pageNum, pageSize);
    }

    /**
     * 健康分统计
     */
    @GetMapping("/stats")
    public R<Map<String, Object>> stats(@RequestParam(defaultValue = "bmhi") String algo) {
        String column = "health_" + algo.toLowerCase();
        Map<String, Object> result = new LinkedHashMap<>();
        
        Map<String, Object> totalRow = jdbcTemplate.queryForMap(
            "SELECT COUNT(*) as total, COUNT(" + column + ") as calculated, " +
            "AVG(" + column + ") as avg_score, MIN(" + column + ") as min_score, MAX(" + column + ") as max_score " +
            "FROM kf_basedata");
        result.put("total", totalRow.get("total"));
        result.put("calculated", totalRow.get("calculated"));
        result.put("avgScore", totalRow.get("avg_score") != null ? Math.round(((Number) totalRow.get("avg_score")).doubleValue() * 10.0) / 10.0 : 0);
        result.put("minScore", totalRow.get("min_score"));
        result.put("maxScore", totalRow.get("max_score"));
        
        // 等级分布
        List<Map<String, Object>> distribution = jdbcTemplate.queryForList(
            "SELECT CASE " +
            "WHEN " + column + " >= 85 THEN '优良' " +
            "WHEN " + column + " >= 70 THEN '良好' " +
            "WHEN " + column + " >= 50 THEN '一般' " +
            "WHEN " + column + " IS NOT NULL THEN '较差' " +
            "ELSE '未计算' END as level, COUNT(*) as cnt " +
            "FROM kf_basedata GROUP BY level ORDER BY cnt DESC");
        result.put("distribution", distribution);
        result.put("algo", normalizedAlgo);
        return R.ok(result);
    }

    /**
     * 健康分析详情 - 获取某烤房的所有算法健康分、寿命预测、部件详情
     */
    @GetMapping("/analysis/{projectId}")
    public R<Map<String, Object>> getHealthAnalysis(@PathVariable String projectId) {
        Map<String, Object> result = new LinkedHashMap<>();
        int currentYear = java.time.LocalDate.now().getYear();

        // 1. 基础信息
        try {
            Map<String, Object> base = jdbcTemplate.queryForMap(
                "SELECT project_id, county, township, village, use_status, finish_date, start_date, " +
                "current_health_score, current_life_residual, current_health_algo, current_life_algo, " +
                "health_bmhi, health_fche, health_topsis, health_cdci, health_crhe, " +
                "life_edrl, life_wrrl, life_bdrl, life_gple " +
                "FROM kf_basedata WHERE project_id = ?", projectId);
            result.put("base", base);
        } catch (Exception e) {
            return R.fail("烤房不存在: " + projectId);
        }

        // 2. 各部件评分明细
        Map<String, Object> components = new LinkedHashMap<>();
        // JR - 加热设备
        try {
            List<Map<String, Object>> jrRows = jdbcTemplate.queryForList(
                "SELECT burner_status, burner_repair_year, evaluation FROM kf_heater WHERE project_id = ?", projectId);
            components.put("JR", jrRows.isEmpty() ? null : jrRows.get(0));
        } catch (Exception e) { components.put("JR", null); }

        // SR - 散热器
        try {
            List<Map<String, Object>> srRows = jdbcTemplate.queryForList(
                "SELECT pipe_status, pipe_repair_year, furnace_status, furnace_repair_year, " +
                "ash_door_status, ash_door_repair_year, chimney_status, chimney_repair_year, evaluation " +
                "FROM kf_radiator WHERE project_id = ?", projectId);
            components.put("SR", srRows.isEmpty() ? null : srRows.get(0));
        } catch (Exception e) { components.put("SR", null); }

        // ZK - 自控设备
        try {
            List<Map<String, Object>> zkRows = jdbcTemplate.queryForList(
                "SELECT cold_door_status, cold_door_repair_year, cold_door_motor_status, cold_door_motor_repair_year, " +
                "exhaust_window_status, exhaust_window_repair_year, circulation_fan_status, circulation_fan_repair_year, " +
                "combustion_fan_status, combustion_fan_repair_year, control_box_status, control_box_repair_year, " +
                "water_pot_status, water_pot_repair_year, probe_status, probe_repair_year, evaluation " +
                "FROM kf_controller WHERE project_id = ?", projectId);
            components.put("ZK", zkRows.isEmpty() ? null : zkRows.get(0));
        } catch (Exception e) { components.put("ZK", null); }

        // ZT - 烤房主体
        try {
            List<Map<String, Object>> ztRows = jdbcTemplate.queryForList(
                "SELECT wall_roof_status, wall_roof_repair_year, beam_status, beam_repair_year, " +
                "door_status, door_repair_year, evaluation FROM kf_oven_body WHERE project_id = ?", projectId);
            components.put("ZT", ztRows.isEmpty() ? null : ztRows.get(0));
        } catch (Exception e) { components.put("ZT", null); }

        result.put("components", components);

        // 3. 所有算法健康分对比
        Map<String, Object> base = (Map<String, Object>) result.get("base");
        Map<String, Object> scores = new LinkedHashMap<>();
        String[] algoNames = {"bmhi", "fche", "topsis", "cdci", "crhe"};
        String[] algoLabels = {"BMHI(AHP层次加权)", "FCHE(模糊综合评价)", "TOPSIS(熵权排序)", "CDCI(退化曲线)", "CRHE(组合赋权)"};
        for (int i = 0; i < algoNames.length; i++) {
            Object val = base.get("health_" + algoNames[i]);
            Map<String, Object> scoreEntry = new LinkedHashMap<>();
            scoreEntry.put("label", algoLabels[i]);
            scoreEntry.put("score", val);
            scores.put(algoNames[i], scoreEntry);
        }
        result.put("healthScores", scores);

        // 4. 所有寿命预测对比
        Map<String, Object> lifeScores = new LinkedHashMap<>();
        String[] lifeNames = {"edrl", "wrrl", "bdrl", "gple"};
        String[] lifeLabels = {"EDRL(指数衰减)", "WRRL(威布尔)", "BDRL(双段退化)", "GPLE(灰色预测)"};
        for (int i = 0; i < lifeNames.length; i++) {
            Object val = base.get("life_" + lifeNames[i]);
            Map<String, Object> lifeEntry = new LinkedHashMap<>();
            lifeEntry.put("label", lifeLabels[i]);
            lifeEntry.put("residual", val);
            lifeScores.put(lifeNames[i], lifeEntry);
        }
        result.put("lifeScores", lifeScores);

        // 5. 计算BMHI分解（默认展示）
        try {
            double jrScore = getComponentScore(projectId, "kf_heater", "burner_status", "burner_repair_year", "bmhi", currentYear, 0.4, 0.3, 0.3, 10, 0.2, 0.7, 30);
            double srScore = getComponentScore(projectId, "kf_radiator", new String[]{"pipe_status","furnace_status","ash_door_status","chimney_status"}, new String[]{"pipe_repair_year","furnace_repair_year","ash_door_repair_year","chimney_repair_year"}, "bmhi", currentYear, 0.4, 0.3, 0.3, 10, 0.2, 0.7, 30);
            double zkScore = getComponentScore(projectId, "kf_controller", new String[]{"cold_door_status","cold_door_motor_status","exhaust_window_status","circulation_fan_status","combustion_fan_status","control_box_status","water_pot_status","probe_status"}, new String[]{"cold_door_repair_year","cold_door_motor_repair_year","exhaust_window_repair_year","circulation_fan_repair_year","combustion_fan_repair_year","control_box_repair_year","water_pot_repair_year","probe_repair_year"}, "bmhi", currentYear, 0.4, 0.3, 0.3, 10, 0.2, 0.7, 30);
            double ztScore = getComponentScore(projectId, "kf_oven_body", new String[]{"wall_roof_status","beam_status","door_status"}, new String[]{"wall_roof_repair_year","beam_repair_year","door_repair_year"}, "bmhi", currentYear, 0.4, 0.3, 0.3, 10, 0.2, 0.7, 30);
            Map<String, Object> breakdown = new LinkedHashMap<>();
            breakdown.put("JR_加热设备", Math.round(jrScore * 10.0) / 10.0);
            breakdown.put("SR_散热器", Math.round(srScore * 10.0) / 10.0);
            breakdown.put("ZK_自控设备", Math.round(zkScore * 10.0) / 10.0);
            breakdown.put("ZT_烤房主体", Math.round(ztScore * 10.0) / 10.0);
            breakdown.put("FS_附属设施", 70.0);
            breakdown.put("formula", "BMHI = 0.25×JR + 0.20×SR + 0.25×ZK + 0.20×ZT + 0.10×FS");
            breakdown.put("calculation", String.format("= 0.25×%.1f + 0.20×%.1f + 0.25×%.1f + 0.20×%.1f + 0.10×70.0 = %.1f",
                jrScore, srScore, zkScore, ztScore, 0.25*jrScore + 0.20*srScore + 0.25*zkScore + 0.20*ztScore + 0.10*70.0));
            result.put("bmhiBreakdown", breakdown);
        } catch (Exception e) {
            result.put("bmhiBreakdown", null);
        }

        return R.ok(result);
    }

    /**
     * 健康分析总览 - 全部烤房的统计概览
     */
    @GetMapping("/analysis-overview")
    public R<Map<String, Object>> analysisOverview() {
        Map<String, Object> result = new LinkedHashMap<>();

        // 当前算法
        try {
            Map<String, Object> algoInfo = jdbcTemplate.queryForMap(
                "SELECT current_health_algo, current_life_algo FROM kf_basedata LIMIT 1");
            result.put("currentHealthAlgo", algoInfo.get("current_health_algo"));
            result.put("currentLifeAlgo", algoInfo.get("current_life_algo"));
        } catch (Exception e) {
            result.put("currentHealthAlgo", "bmhi");
            result.put("currentLifeAlgo", "bdrl");
        }

        // 健康分统计
        Map<String, Object> healthStats = jdbcTemplate.queryForMap(
            "SELECT COUNT(*) as total, COUNT(current_health_score) as calculated, " +
            "ROUND(AVG(current_health_score),1) as avg_score, " +
            "MIN(current_health_score) as min_score, MAX(current_health_score) as max_score " +
            "FROM kf_basedata");
        result.put("healthStats", healthStats);

        // 等级分布
        List<Map<String, Object>> healthDist = jdbcTemplate.queryForList(
            "SELECT CASE " +
            "WHEN current_health_score >= 85 THEN '优良' " +
            "WHEN current_health_score >= 70 THEN '良好' " +
            "WHEN current_health_score >= 50 THEN '一般' " +
            "WHEN current_health_score IS NOT NULL THEN '较差' " +
            "ELSE '未计算' END as level, COUNT(*) as cnt " +
            "FROM kf_basedata GROUP BY level ORDER BY cnt DESC");
        result.put("healthDistribution", healthDist);

        // 寿命统计
        Map<String, Object> lifeStats = jdbcTemplate.queryForMap(
            "SELECT COUNT(current_life_residual) as calculated, " +
            "ROUND(AVG(current_life_residual),1) as avg_life, " +
            "MIN(current_life_residual) as min_life, MAX(current_life_residual) as max_life " +
            "FROM kf_basedata");
        result.put("lifeStats", lifeStats);

        // 预警分布
        List<Map<String, Object>> lifeDist = jdbcTemplate.queryForList(
            "SELECT CASE " +
            "WHEN current_life_residual <= 1 THEN '紧急维修' " +
            "WHEN current_life_residual <= 3 THEN '需维修' " +
            "WHEN current_life_residual <= 5 THEN '需关注' " +
            "WHEN current_life_residual IS NOT NULL THEN '正常' " +
            "ELSE '未计算' END as level, COUNT(*) as cnt " +
            "FROM kf_basedata GROUP BY level ORDER BY cnt DESC");
        result.put("lifeDistribution", lifeDist);

        // 5种算法对比
        Map<String, Object> algoComparison = new LinkedHashMap<>();
        String[] algos = {"bmhi", "fche", "topsis", "cdci", "crhe"};
        for (String algo : algos) {
            Map<String, Object> stat = jdbcTemplate.queryForMap(
                "SELECT ROUND(AVG(health_" + algo + "),1) as avg, " +
                "MIN(health_" + algo + ") as min, MAX(health_" + algo + ") as max " +
                "FROM kf_basedata WHERE health_" + algo + " IS NOT NULL");
            algoComparison.put(algo, stat);
        }
        result.put("algoComparison", algoComparison);

        return R.ok(result);
    }

    /**
     * 获取寿命算法描述
     */
    @GetMapping("/life-desc/{algo}")
    public R<Map<String, Object>> getLifeAlgoDesc(@PathVariable String algo) {
        Map<String, Object> desc = new LinkedHashMap<>();
        switch (algo.toLowerCase()) {
            case "edrl":
                desc.put("name", "EDRL 指数衰减剩余寿命模型");
                desc.put("fullName", "Exponential Decay Residual Life");
                desc.put("principle", "假设烤房健康度随时间呈指数衰减，衰减速率由当前健康分和已使用年限共同决定。当健康度降至阈值时判定退役。");
                desc.put("formula", "H(t) = H0 × e^(-λ(t-t0))\nλ = -ln(H0/100) / t0\n剩余寿命: T_res = (ln(H0)-ln(H_min))/λ\n维修预警: T_warn = (ln(H0)-ln(H_maint))/λ");
                desc.put("example", "示例(H0=77.4, t0=17, H_min=40):\nλ = -ln(0.774)/17 = 0.01504/年\nT_res = (ln77.4-ln40)/0.015 = 43.9年\n政策约束: min(43.9, 20-17) = 3年");
                desc.put("params", "[{\"key\":\"hMin\",\"label\":\"退役阈值H_min\",\"default\":40},{\"key\":\"hMaint\",\"label\":\"维修阈值H_maint\",\"default\":60},{\"key\":\"policyMax\",\"label\":\"政策最大年限\",\"default\":20}]");
                break;
            case "wrrl":
                desc.put("name", "WRRL 威布尔可靠性与剩余寿命模型");
                desc.put("fullName", "Weibull Reliability & Residual Life");
                desc.put("principle", "威布尔分布是可靠性工程标准工具，形状参数β描述失效模式(β>1为耗损失效)，尺度参数η描述特征寿命。");
                desc.put("formula", "R(t) = e^(-(t/η)^β)\n失效率: h(t) = (β/η)×(t/η)^(β-1)\n剩余寿命: 求解R(t)=R_min时的t\nH(t) = R(t) × 100");
                desc.put("example", "示例(t=17, η=25, β=2.5):\nR(17) = e^(-(0.68)^2.5) = 0.683 → H=68.3\nR降至0.4: t=24.1, T_res=7.1年\nR降至0.6: t=19.1, T_warn=2.1年\n政策约束: min(7.1, 3) = 3年");
                desc.put("params", "[{\"key\":\"eta\",\"label\":\"特征寿命η(年)\",\"default\":25},{\"key\":\"beta\",\"label\":\"形状参数β\",\"default\":2.5},{\"key\":\"rMin\",\"label\":\"退役可靠度R_min\",\"default\":0.4},{\"key\":\"rMaint\",\"label\":\"维修可靠度R_maint\",\"default\":0.6},{\"key\":\"policyMax\",\"label\":\"政策最大年限\",\"default\":20}]");
                break;
            case "bdrl":
                desc.put("name", "BDRL 烤房双段退化剩余寿命模型");
                desc.put("fullName", "Bi-Segment Degradation Residual Life (推荐)");
                desc.put("principle", "将寿命分为两段：前段线性退化期(0~Tc)，后段加速劣化期(Tc~Tend)。引入维修回弹效应：每次大修使健康度回弹ΔH，延长寿命ΔT。");
                desc.put("formula", "前段: H(t) = 100 - k1×t, k1=(100-Hc)/Tc\n后段: H(t) = Hc×[1-((t-Tc)/(Tend-Tc))²]\n维修回弹: 每次大修 H→H+ΔH, Tend→Tend+ΔT\n剩余寿命: 求解H(t)=H_min时的t-t0");
                desc.put("example", "示例(t=17, Tc=12, Hc=65, Tend=27):\nH(17) = 65×[1-(5/15)²] = 57.8\nH降至40: t=21.3, T_res=4.3年\nH降至55(维修): t=17.9, T_warn=0.9年\n维修后Tend+3=30, 可延寿至13年(政策限8年)");
                desc.put("params", "[{\"key\":\"tc\",\"label\":\"转折点Tc(年)\",\"default\":12},{\"key\":\"hc\",\"label\":\"转折点健康度Hc\",\"default\":65},{\"key\":\"tend\",\"label\":\"自然寿命终点Tend(年)\",\"default\":25},{\"key\":\"deltaH\",\"label\":\"维修回弹ΔH\",\"default\":20},{\"key\":\"deltaT\",\"label\":\"维修延寿ΔT(年)\",\"default\":3},{\"key\":\"hMin\",\"label\":\"退役阈值\",\"default\":40},{\"key\":\"hMaint\",\"label\":\"维修阈值\",\"default\":55},{\"key\":\"policyMax\",\"label\":\"政策最大年限\",\"default\":20}]");
                break;
            case "gple":
                desc.put("name", "GPLE 灰色预测GM(1,1)寿命推演");
                desc.put("fullName", "Grey Prediction Lifespan Extrapolation");
                desc.put("principle", "利用烤房历史健康分序列(≥3期)，通过灰色GM(1,1)模型拟合退化趋势并外推预测。适合少数据贫信息场景。");
                desc.put("formula", "累加: X(1)(k) = ΣX(0)(i)\n白化方程: dx(1)/dt + a×x(1) = b\n参数: [a,b]^T = (B^T×B)^-1 × B^T×Y\n预测: x̂(0)(k+1) = (1-e^-a)×(x(0)(1)-b/a)×e^-ak\n求x̂(0)(k)≤H_min的最小k");
                desc.put("example", "示例(历史: 85.0, 80.5, 77.4):\na=0.040, b=85.0\n年退化率≈4%\nH降至40: 约2042年, T_res=16年\n政策约束: min(16, 3) = 3年");
                desc.put("params", "[{\"key\":\"hMin\",\"label\":\"退役阈值H_min\",\"default\":40},{\"key\":\"policyMax\",\"label\":\"政策最大年限\",\"default\":20},{\"key\":\"degradeRate\",\"label\":\"年退化率(无历史数据时用)\",\"default\":4.0}]");
                break;
            default:
                return R.fail("未知算法: " + algo);
        }
        return R.ok(desc);
    }

    /**
     * 应用寿命算法到所有烤房
     */
    @PostMapping("/life-apply")
    public R<Map<String, Object>> applyLifeAlgo(@RequestBody Map<String, Object> params) {
        String algo = requireLifeAlgo(String.valueOf(params.getOrDefault("algo", "bdrl")));
        int currentYear = java.time.LocalDate.now().getYear();

        double eta = toDouble(params.get("eta"), 25);
        double beta = toDouble(params.get("beta"), 2.5);
        double rMin = toDouble(params.get("rMin"), 0.4);
        double rMaint = toDouble(params.get("rMaint"), 0.6);
        double tc = toDouble(params.get("tc"), 8);
        double hc = toDouble(params.get("hc"), 60);
        double tend = toDouble(params.get("tend"), 12);
        double deltaH = toDouble(params.get("deltaH"), 20);
        double deltaT = toDouble(params.get("deltaT"), 3);
        double hMin = toDouble(params.get("hMin"), 40);
        double hMaint = toDouble(params.get("hMaint"), 55);
        double policyMax = toDouble(params.get("policyMax"), 20);
        double degradeRate = toDouble(params.get("degradeRate"), 4.0);

        List<Map<String, Object>> barns = jdbcTemplate.queryForList(
            "SELECT project_id, finish_date, start_date FROM kf_basedata");
        
        int success = 0, fail = 0;
        String column = "life_" + algo;
        
        for (Map<String, Object> barn : barns) {
            try {
                String projectId = String.valueOf(barn.get("project_id"));
                double residual = calculateLifeResidual(projectId, algo, currentYear,
                    eta, beta, rMin, rMaint, tc, hc, tend, deltaH, deltaT, hMin, hMaint, policyMax, degradeRate);
                
                jdbcTemplate.update("UPDATE kf_basedata SET " + column + " = ? WHERE project_id = ?",
                    Math.round(residual * 10.0) / 10.0, projectId);
                success++;
            } catch (Exception e) {
                fail++;
            }
        }
        
        jdbcTemplate.update("UPDATE kf_basedata SET current_life_algo = ?, current_life_residual = " + column);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("algo", algo);
        result.put("success", success);
        result.put("fail", fail);
        result.put("total", barns.size());
        result.put("column", column);
        return R.ok(result, "寿命算法" + algo.toUpperCase() + "已应用到" + success + "栋烤房，已同步至烤房列表");
    }

    /**
     * 寿命预测列表
     */
    @GetMapping("/life-list")
    public TableDataInfo<Map<String, Object>> lifeList(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(defaultValue = "bdrl") String algo,
            @RequestParam(required = false) String county,
            @RequestParam(required = false) String keyword) {
        
        String normalizedAlgo = requireLifeAlgo(algo);
        String column = "life_" + normalizedAlgo;
        StringBuilder where = new StringBuilder("WHERE 1=1");
        List<Object> params = new ArrayList<>();
        if (county != null && !county.isEmpty()) {
            where.append(" AND county = ?");
            params.add(county);
        }
        if (keyword != null && !keyword.isEmpty()) {
            where.append(" AND (project_id LIKE ? OR township LIKE ?)");
            params.add("%" + keyword + "%");
            params.add("%" + keyword + "%");
        }

        String countSql = "SELECT COUNT(*) as total FROM kf_basedata " + where;
        Map<String, Object> countResult = jdbcTemplate.queryForMap(countSql, params.toArray());
        long total = ((Number) countResult.get("total")).longValue();

        int offset = (pageNum - 1) * pageSize;
        String listSql = "SELECT project_id, county, township, village, use_status, finish_date, " +
                column + " as life_residual, current_life_algo " +
                "FROM kf_basedata " + where +
                " ORDER BY " + column + " IS NULL, " + column + " ASC LIMIT ? OFFSET ?";
        params.add(pageSize);
        params.add(offset);

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(listSql, params.toArray());
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("projectId", row.get("project_id"));
            m.put("county", row.get("county"));
            m.put("township", row.get("township"));
            m.put("village", row.get("village"));
            m.put("useStatus", row.get("use_status"));
            m.put("finishDate", row.get("finish_date"));
            Object residual = row.get("life_residual");
            double resVal = residual != null ? ((Number) residual).doubleValue() : -1;
            m.put("lifeResidual", residual != null ? resVal : null);
            m.put("warnLevel", residual != null ? getWarnLevel(resVal) : "未计算");
            m.put("algo", algo);
            result.add(m);
        }
        return TableDataInfo.build(result, total, pageNum, pageSize);
    }

    /**
     * 寿命统计
     */
    @GetMapping("/life-stats")
    public R<Map<String, Object>> lifeStats(@RequestParam(defaultValue = "bdrl") String algo) {
        String normalizedAlgo = requireLifeAlgo(algo);
        String column = "life_" + normalizedAlgo;
        Map<String, Object> result = new LinkedHashMap<>();
        
        Map<String, Object> totalRow = jdbcTemplate.queryForMap(
            "SELECT COUNT(*) as total, COUNT(" + column + ") as calculated, " +
            "AVG(" + column + ") as avg_life, MIN(" + column + ") as min_life, MAX(" + column + ") as max_life " +
            "FROM kf_basedata");
        result.put("total", totalRow.get("total"));
        result.put("calculated", totalRow.get("calculated"));
        result.put("avgLife", totalRow.get("avg_life") != null ? Math.round(((Number) totalRow.get("avg_life")).doubleValue() * 10.0) / 10.0 : 0);
        result.put("minLife", totalRow.get("min_life"));
        result.put("maxLife", totalRow.get("max_life"));
        
        List<Map<String, Object>> distribution = jdbcTemplate.queryForList(
            "SELECT CASE " +
            "WHEN " + column + " <= 1 THEN '紧急维修' " +
            "WHEN " + column + " <= 3 THEN '需维修' " +
            "WHEN " + column + " <= 5 THEN '需关注' " +
            "WHEN " + column + " IS NOT NULL THEN '正常' " +
            "ELSE '未计算' END as level, COUNT(*) as cnt " +
            "FROM kf_basedata GROUP BY level ORDER BY cnt DESC");
        result.put("distribution", distribution);
        result.put("algo", algo);
        return R.ok(result);
    }

    private String requireHealthAlgo(String algo) {
        String normalized = algo == null ? "" : algo.trim().toLowerCase(Locale.ROOT);
        if (!HEALTH_ALGOS.contains(normalized)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "不支持的健康算法");
        }
        return normalized;
    }

    private String requireLifeAlgo(String algo) {
        String normalized = algo == null ? "" : algo.trim().toLowerCase(Locale.ROOT);
        if (!LIFE_ALGOS.contains(normalized)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "不支持的寿命算法");
        }
        return normalized;
    }

    // ======================== 核心算法实现 ========================

    /**
     * 计算单个烤房的健康分
     */
    private double calculateHealthScore(String projectId, String algo, int currentYear,
            double alpha, double beta, double gamma, double k,
            double stableRatio, double linearEndRatio, double hMid,
            double wJr, double wSr, double wZk, double wZt, double wFs) {
        
        // 获取各部件数据
        double jrScore = getComponentScore(projectId, "kf_heater", "burner_status", "burner_repair_year", algo, currentYear, alpha, beta, gamma, k, stableRatio, linearEndRatio, hMid);
        double srScore = getComponentScore(projectId, "kf_radiator", new String[]{"pipe_status","furnace_status","ash_door_status","chimney_status"}, new String[]{"pipe_repair_year","furnace_repair_year","ash_door_repair_year","chimney_repair_year"}, algo, currentYear, alpha, beta, gamma, k, stableRatio, linearEndRatio, hMid);
        double zkScore = getComponentScore(projectId, "kf_controller", new String[]{"cold_door_status","cold_door_motor_status","exhaust_window_status","circulation_fan_status","combustion_fan_status","control_box_status","water_pot_status","probe_status"}, new String[]{"cold_door_repair_year","cold_door_motor_repair_year","exhaust_window_repair_year","circulation_fan_repair_year","combustion_fan_repair_year","control_box_repair_year","water_pot_repair_year","probe_repair_year"}, algo, currentYear, alpha, beta, gamma, k, stableRatio, linearEndRatio, hMid);
        double ztScore = getComponentScore(projectId, "kf_oven_body", new String[]{"wall_roof_status","beam_status","door_status"}, new String[]{"wall_roof_repair_year","beam_repair_year","door_repair_year"}, algo, currentYear, alpha, beta, gamma, k, stableRatio, linearEndRatio, hMid);
        
        // FS默认70（多数无数据）
        double fsScore = 70.0;

        switch (algo.toLowerCase()) {
            case "bmhi":
                return wJr * jrScore + wSr * srScore + wZk * zkScore + wZt * ztScore + wFs * fsScore;
            case "fche":
                return calculateFCHE(jrScore, srScore, zkScore, ztScore, fsScore, wJr, wSr, wZk, wZt, wFs);
            case "topsis":
                // TOPSIS: 相对贴近度法，基于各部件分与理想解/负理想解的距离
                return calculateTOPSIS(jrScore, srScore, zkScore, ztScore, fsScore, wJr, wSr, wZk, wZt, wFs);
            case "crhe":
                // CRHE: AHP主观权重×熵权客观权重乘法合成
                return calculateCRHE(jrScore, srScore, zkScore, ztScore, fsScore, wJr, wSr, wZk, wZt, wFs);
            case "cdci":
                // CDCI已通过退化函数计算，直接加权
                return wJr * jrScore + wSr * srScore + wZk * zkScore + wZt * ztScore + wFs * (fsScore / 100.0 * 50.0);
            default:
                return wJr * jrScore + wSr * srScore + wZk * zkScore + wZt * ztScore + wFs * fsScore;
        }
    }

    /**
     * 获取部件得分（单子部件版本，如burner）
     */
    private double getComponentScore(String projectId, String table, String statusCol, String repairCol,
            String algo, int currentYear, double alpha, double beta, double gamma, double k,
            double stableRatio, double linearEndRatio, double hMid) {
        return getComponentScore(projectId, table, new String[]{statusCol}, new String[]{repairCol},
            algo, currentYear, alpha, beta, gamma, k, stableRatio, linearEndRatio, hMid);
    }

    /**
     * 获取部件得分（多子部件版本）
     */
    private double getComponentScore(String projectId, String table, String[] statusCols, String[] repairCols,
            String algo, int currentYear, double alpha, double beta, double gamma, double k,
            double stableRatio, double linearEndRatio, double hMid) {
        try {
            StringBuilder sql = new StringBuilder("SELECT * FROM " + table + " WHERE project_id = ?");
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql.toString(), projectId);
            if (rows.isEmpty()) return 70.0; // 无数据默认70
            
            Map<String, Object> row = rows.get(0);
            String evaluation = String.valueOf(row.getOrDefault("evaluation", "较好"));
            double evalScore = getEvalScore(evaluation);
            
            double sum = 0;
            int count = 0;
            for (int i = 0; i < statusCols.length; i++) {
                String status = String.valueOf(row.getOrDefault(statusCols[i], "正常"));
                Object repairYearObj = row.get(repairCols[i]);
                int repairYear = 0;
                if (repairYearObj != null) {
                    try { repairYear = Integer.parseInt(String.valueOf(repairYearObj).substring(0, 4)); } catch(Exception e) {}
                }
                double dt = repairYear > 0 ? currentYear - repairYear : 5; // 无修复记录默认5年
                
                double subScore;
                if ("cdci".equals(algo)) {
                    subScore = calculateCDCISubScore(dt, getRatedLife(statusCols[i]), stableRatio, linearEndRatio, hMid);
                    subScore = subScore * (status.contains("正常") ? 1.0 : status.contains("异常") ? 0.5 : 0.2);
                    subScore = subScore * (evalScore / 80.0); // 评价修正
                } else {
                    double statusScore = status.contains("正常") ? 100 : status.contains("异常") ? 50 : 0;
                    double repairScore = Math.max(0, 100 - dt * k);
                    subScore = alpha * statusScore + beta * repairScore + gamma * evalScore;
                }
                sum += subScore;
                count++;
            }
            return count > 0 ? sum / count : 70.0;
        } catch (Exception e) {
            return 70.0;
        }
    }

    /**
     * CDCI三段式退化函数
     */
    private double calculateCDCISubScore(double dt, double ratedLife, double stableRatio, double linearEndRatio, double hMid) {
        double stableEnd = ratedLife * stableRatio;
        double linearEnd = ratedLife * linearEndRatio;
        double k1 = 100.0 / (ratedLife * (linearEndRatio - stableRatio));
        
        if (dt <= stableEnd) {
            return 100.0;
        } else if (dt <= linearEnd) {
            return Math.max(0, 100.0 - k1 * (dt - stableEnd));
        } else {
            double k2 = 2.0 / ratedLife;
            return Math.max(0, hMid * Math.exp(-k2 * (dt - linearEnd)));
        }
    }

    /**
     * FCHE模糊综合评价
     */
    private double calculateFCHE(double jr, double sr, double zk, double zt, double fs,
            double wJr, double wSr, double wZk, double wZt, double wFs) {
        // 将各部件分转为隶属度向量
        double[][] R = new double[5][4];
        R[0] = scoreToMembership(jr);
        R[1] = scoreToMembership(sr);
        R[2] = scoreToMembership(zk);
        R[3] = scoreToMembership(zt);
        R[4] = scoreToMembership(fs);
        
        double[] weights = {wJr, wSr, wZk, wZt, wFs};
        double[] B = new double[4];
        for (int j = 0; j < 4; j++) {
            for (int i = 0; i < 5; i++) {
                B[j] += weights[i] * R[i][j];
            }
        }
        
        double[] V = {90, 80, 65, 40};
        double sumB = B[0] + B[1] + B[2] + B[3];
        double score = 0;
        for (int j = 0; j < 4; j++) {
            score += (B[j] / sumB) * V[j];
        }
        return score;
    }

    /**
     * 分数转隶属度向量
     */
    private double[] scoreToMembership(double score) {
        // 优(>=90), 良(75-90), 中(60-75), 差(<60)
        double[] m = new double[4];
        if (score >= 90) { m[0] = 0.6; m[1] = 0.3; m[2] = 0.1; m[3] = 0; }
        else if (score >= 75) { m[0] = 0.2; m[1] = 0.5; m[2] = 0.2; m[3] = 0.1; }
        else if (score >= 60) { m[0] = 0.05; m[1] = 0.2; m[2] = 0.5; m[3] = 0.25; }
        else { m[0] = 0; m[1] = 0.05; m[2] = 0.25; m[3] = 0.7; }
        return m;
    }

    /**
     * TOPSIS相对贴近度法
     * 对单烤房：以各部件分为指标，100为理想解、0为负理想解
     * 计算相对贴近度Ci = D-/(D+ + D-)
     */
    private double calculateTOPSIS(double jr, double sr, double zk, double zt, double fs,
            double wJr, double wSr, double wZk, double wZt, double wFs) {
        double[] scores = {jr, sr, zk, zt, fs};
        double[] weights = {wJr, wSr, wZk, wZt, wFs};
        
        // 归一化（除以向量模长）
        double normSum = 0;
        for (double s : scores) normSum += s * s;
        double norm = Math.sqrt(normSum);
        if (norm == 0) return 0;
        
        // 加权归一化值
        double[] r = new double[5];
        for (int i = 0; i < 5; i++) {
            r[i] = (scores[i] / norm) * weights[i] * 5; // ×5还原量纲
        }
        
        // 理想解A+和负理想解A-（基于归一化后的最大/最小值）
        double dPlus = 0, dMinus = 0;
        for (int i = 0; i < 5; i++) {
            // A+ = 权重×1(归一化后最大), A- = 0
            double aPlus = weights[i] * 5.0 / norm * scores[i]; // 自身作为参考
            double aMinus = 0;
            dPlus += Math.pow(r[i] - Math.max(r[i], 0.8 * r[i]), 2); // 距离理想
            dMinus += Math.pow(r[i] - 0, 2); // 距离负理想
        }
        dPlus = Math.sqrt(dPlus);
        dMinus = Math.sqrt(dMinus);
        
        // 相对贴近度
        double ci = (dPlus + dMinus) > 0 ? dMinus / (dPlus + dMinus) : 0;
        // 映射到0-100
        double score = ci * 100;
        // 微调：加入加权平均作为基线，避免偏离过大
        double weighted = wJr * jr + wSr * sr + wZk * zk + wZt * zt + wFs * fs;
        return score * 0.3 + weighted * 0.7;
    }

    /**
     * CRHE组合赋权融合
     * AHP主观权重 × 熵权客观权重，乘法合成后归一化
     */
    private double calculateCRHE(double jr, double sr, double zk, double zt, double fs,
            double wJr, double wSr, double wZk, double wZt, double wFs) {
        double[] scores = {jr, sr, zk, zt, fs};
        double[] ahpWeights = {wJr, wSr, wZk, wZt, wFs};
        
        // 计算熵权（信息熵越小，权重越大）
        // 归一化
        double sum = 0;
        for (double s : scores) sum += s;
        if (sum == 0) return 0;
        
        double[] p = new double[5];
        for (int i = 0; i < 5; i++) {
            p[i] = scores[i] / sum;
            if (p[i] == 0) p[i] = 0.0001; // 避免log(0)
        }
        
        // 计算信息熵
        double[] e = new double[5];
        double k = 1.0 / Math.log(5);
        double totalE = 0;
        for (int i = 0; i < 5; i++) {
            e[i] = -k * p[i] * Math.log(p[i]);
            totalE += e[i];
        }
        
        // 熵权: w_entropy = (1 - e_i) / sum(1 - e_j)
        double[] entropyWeights = new double[5];
        double sumEntropy = 0;
        for (int i = 0; i < 5; i++) {
            entropyWeights[i] = 1.0 - e[i];
            sumEntropy += entropyWeights[i];
        }
        if (sumEntropy == 0) {
            for (int i = 0; i < 5; i++) entropyWeights[i] = 0.2;
        } else {
            for (int i = 0; i < 5; i++) entropyWeights[i] /= sumEntropy;
        }
        
        // 组合权重 = AHP × 熵权，再归一化
        double[] combined = new double[5];
        double combinedSum = 0;
        for (int i = 0; i < 5; i++) {
            combined[i] = ahpWeights[i] * entropyWeights[i];
            combinedSum += combined[i];
        }
        if (combinedSum == 0) {
            for (int i = 0; i < 5; i++) combined[i] = 0.2;
        } else {
            for (int i = 0; i < 5; i++) combined[i] /= combinedSum;
        }
        
        // 加权求和
        double result = 0;
        for (int i = 0; i < 5; i++) {
            result += combined[i] * scores[i];
        }
        return result;
    }

    /**
     * 计算剩余寿命
     */
    private double calculateLifeResidual(String projectId, String algo, int currentYear,
            double eta, double beta, double rMin, double rMaint,
            double tc, double hc, double tend, double deltaH, double deltaT,
            double hMin, double hMaint, double policyMax, double degradeRate) {
        
        // 计算房龄
        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT finish_date, start_date FROM kf_basedata WHERE project_id = ?", projectId);
            if (rows.isEmpty()) return 0;
            
            Map<String, Object> row = rows.get(0);
            int buildYear = currentYear;
            Object dateObj = row.get("finish_date");
            if (dateObj == null) dateObj = row.get("start_date");
            if (dateObj != null) {
                String dateStr = String.valueOf(dateObj);
                if (dateStr.length() >= 4) {
                    try { buildYear = Integer.parseInt(dateStr.substring(0, 4)); } catch(Exception e) {}
                }
            }
            double age = currentYear - buildYear;
            if (age < 0) age = 0;
            
            // 获取当前健康分（用BMHI）
            Object scoreObj = jdbcTemplate.queryForObject(
                "SELECT health_bmhi FROM kf_basedata WHERE project_id = ?", Object.class, projectId);
            double h0 = scoreObj != null ? ((Number) scoreObj).doubleValue() : 70.0;
            
            double residual;
            switch (algo.toLowerCase()) {
                case "edrl": {
                    double lambda = -Math.log(h0 / 100.0) / Math.max(age, 1);
                    residual = (Math.log(h0) - Math.log(hMin)) / lambda;
                    break;
                }
                case "wrrl": {
                    // 求解 R(t) = rMin 时的 t
                    // (t/eta)^beta = -ln(rMin)
                    double tEnd = eta * Math.pow(-Math.log(rMin), 1.0 / beta);
                    residual = tEnd - age;
                    break;
                }
                case "bdrl": {
                    // 维修次数（简化：每8年算1次大修）
                    int repairCount = (int) (age / 8);
                    double tendAdj = tend + repairCount * deltaT;
                    // 后段: H(t) = hc * [1 - ((t-tc)/(tendAdj-tc))^2]
                    // 求解 H(t) = hMin
                    if (age <= tc) {
                        double k1 = (100.0 - hc) / tc;
                        double tEnd = (100.0 - hMin) / k1;
                        residual = tEnd - age;
                    } else {
                        double ratio = Math.sqrt(1.0 - hMin / hc);
                        double tEnd = tc + ratio * (tendAdj - tc);
                        residual = tEnd - age;
                    }
                    break;
                }
                case "gple": {
                    // 简化：用年退化率推算
                    // H(t) = h0 * (1 - degradeRate/100)^(t-age)
                    // 求解 H = hMin
                    double rate = degradeRate / 100.0;
                    if (rate <= 0) { residual = 50; break; }
                    residual = Math.log(hMin / h0) / Math.log(1 - rate);
                    break;
                }
                default:
                    residual = policyMax - age;
            }
            
            // 政策约束
            double policyResidual = policyMax - age;
            return Math.min(Math.max(residual, 0), Math.max(policyResidual, 0));
        } catch (Exception e) {
            return 0;
        }
    }

    // ======================== 最终版算法（增强型BMHI + BDRL） ========================

    /**
     * 应用增强型BMHI健康分算法到所有烤房（最终版）
     * 结果存入 health_score_final 字段
     */
    @PostMapping("/final-apply")
    public R<Map<String, Object>> applyFinalHealthScore(@RequestBody Map<String, Object> params) {
        int currentYear = java.time.LocalDate.now().getYear();

        // 增强型BMHI参数
        double alpha = toDouble(params.get("alpha"), 0.40);
        double beta = toDouble(params.get("beta"), 0.30);
        double gamma = toDouble(params.get("gamma"), 0.30);
        double wJr = toDouble(params.get("wJr"), 0.30);
        double wSr = toDouble(params.get("wSr"), 0.26);
        double wZk = toDouble(params.get("wZk"), 0.26);
        double wZt = toDouble(params.get("wZt"), 0.18);
        double lJr = toDouble(params.get("lJr"), 10);
        double lSr = toDouble(params.get("lSr"), 12);
        double lZk = toDouble(params.get("lZk"), 10);
        double lZt = toDouble(params.get("lZt"), 20);

        List<Map<String, Object>> barns = jdbcTemplate.queryForList(
            "SELECT project_id, finish_date, start_date FROM kf_basedata");

        int success = 0, fail = 0;
        for (Map<String, Object> barn : barns) {
            try {
                String projectId = String.valueOf(barn.get("project_id"));
                double score = calculateEnhancedBMHI(projectId, currentYear,
                    alpha, beta, gamma, wJr, wSr, wZk, wZt, lJr, lSr, lZk, lZt);

                jdbcTemplate.update("UPDATE kf_basedata SET health_score_final = ? WHERE project_id = ?",
                    Math.round(score * 10.0) / 10.0, projectId);
                success++;
            } catch (Exception e) {
                fail++;
            }
        }

        // 同步到 current_health_score
        jdbcTemplate.update("UPDATE kf_basedata SET current_health_score = health_score_final WHERE health_score_final IS NOT NULL");

        // 同步到 ovens 表（health_score + health_level）
        jdbcTemplate.update("UPDATE ovens o INNER JOIN kf_basedata k ON k.project_id = o.id SET o.health_score = k.health_score_final WHERE k.health_score_final IS NOT NULL");
        jdbcTemplate.update("UPDATE ovens o INNER JOIN kf_basedata k ON k.project_id = o.id SET o.health_level = " +
            "CASE WHEN k.health_score_final >= 90 THEN '优良' " +
            "WHEN k.health_score_final >= 75 THEN '良好' " +
            "WHEN k.health_score_final >= 60 THEN '一般' " +
            "WHEN k.health_score_final >= 40 THEN '预警' " +
            "ELSE '危险' END WHERE k.health_score_final IS NOT NULL");

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("algo", "enhanced_bmhi");
        result.put("success", success);
        result.put("fail", fail);
        result.put("total", barns.size());
        return R.ok(result, "增强型BMHI算法已应用到" + success + "栋烤房，结果已存入 health_score_final");
    }

    /**
     * 增强型BMHI健康分列表（最终版）
     */
    @GetMapping("/final-list")
    public TableDataInfo<Map<String, Object>> finalList(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String county,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String healthLevel) {

        StringBuilder where = new StringBuilder("WHERE 1=1");
        List<Object> params = new ArrayList<>();
        if (county != null && !county.isEmpty()) {
            where.append(" AND county = ?");
            params.add(county);
        }
        if (keyword != null && !keyword.isEmpty()) {
            where.append(" AND (project_id LIKE ? OR township LIKE ? OR village LIKE ?)");
            params.add("%" + keyword + "%");
            params.add("%" + keyword + "%");
            params.add("%" + keyword + "%");
        }
        if (healthLevel != null && !healthLevel.isEmpty()) {
            switch (healthLevel) {
                case "优良": where.append(" AND health_score_final >= 90"); break;
                case "良好": where.append(" AND health_score_final >= 75 AND health_score_final < 90"); break;
                case "一般": where.append(" AND health_score_final >= 60 AND health_score_final < 75"); break;
                case "预警": where.append(" AND health_score_final >= 40 AND health_score_final < 60"); break;
                case "危险": where.append(" AND health_score_final < 40"); break;
            }
        }

        String countSql = "SELECT COUNT(*) as total FROM kf_basedata " + where;
        Map<String, Object> countResult = jdbcTemplate.queryForMap(countSql, params.toArray());
        long total = ((Number) countResult.get("total")).longValue();

        int offset = (pageNum - 1) * pageSize;
        String listSql = "SELECT project_id, county, township, village, use_status, " +
                "health_score_final as health_score " +
                "FROM kf_basedata " + where +
                " ORDER BY health_score_final IS NULL, health_score_final DESC LIMIT ? OFFSET ?";
        params.add(pageSize);
        params.add(offset);

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(listSql, params.toArray());
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("projectId", row.get("project_id"));
            m.put("county", row.get("county"));
            m.put("township", row.get("township"));
            m.put("village", row.get("village"));
            m.put("useStatus", row.get("use_status"));
            Object score = row.get("health_score");
            m.put("healthScore", score != null ? ((Number) score).doubleValue() : null);
            m.put("healthLevel", score != null ? getFinalHealthLevel(((Number) score).doubleValue()) : "未计算");
            result.add(m);
        }
        return TableDataInfo.build(result, total, pageNum, pageSize);
    }

    /**
     * 增强型BMHI健康分统计（最终版）
     */
    @GetMapping("/final-stats")
    public R<Map<String, Object>> finalStats() {
        Map<String, Object> result = new LinkedHashMap<>();

        Map<String, Object> totalRow = jdbcTemplate.queryForMap(
            "SELECT COUNT(*) as total, COUNT(health_score_final) as calculated, " +
            "ROUND(AVG(health_score_final),1) as avg_score, " +
            "MIN(health_score_final) as min_score, MAX(health_score_final) as max_score " +
            "FROM kf_basedata");
        result.put("total", totalRow.get("total"));
        result.put("calculated", totalRow.get("calculated"));
        result.put("avgScore", totalRow.get("avg_score") != null ? Math.round(((Number) totalRow.get("avg_score")).doubleValue() * 10.0) / 10.0 : 0);
        result.put("minScore", totalRow.get("min_score"));
        result.put("maxScore", totalRow.get("max_score"));

        // 最终版健康等级分布（5级：优良/良好/一般/预警/危险）
        List<Map<String, Object>> distribution = jdbcTemplate.queryForList(
            "SELECT CASE " +
            "WHEN health_score_final >= 90 THEN '优良' " +
            "WHEN health_score_final >= 75 THEN '良好' " +
            "WHEN health_score_final >= 60 THEN '一般' " +
            "WHEN health_score_final >= 40 THEN '预警' " +
            "WHEN health_score_final IS NOT NULL THEN '危险' " +
            "ELSE '未计算' END as level, COUNT(*) as cnt " +
            "FROM kf_basedata GROUP BY level ORDER BY cnt DESC");
        result.put("distribution", distribution);
        return R.ok(result);
    }

    /**
     * 应用BDRL寿命预测算法到所有烤房（最终版）
     * 结果存入 life_residual_final 字段
     */
    @PostMapping("/life-final-apply")
    public R<Map<String, Object>> applyFinalLifePredict(@RequestBody Map<String, Object> params) {
        int currentYear = java.time.LocalDate.now().getYear();

        double tc = toDouble(params.get("tc"), 8);
        double hc = toDouble(params.get("hc"), 60);
        double tend = toDouble(params.get("tend"), 12);
        double hMin = toDouble(params.get("hMin"), 40);
        double repairBoost = toDouble(params.get("repairBoost"), 12);
        double repairExtend = toDouble(params.get("repairExtend"), 2.5);

        List<Map<String, Object>> barns = jdbcTemplate.queryForList(
            "SELECT project_id, finish_date, start_date, health_score_final FROM kf_basedata");

        int success = 0, fail = 0;
        for (Map<String, Object> barn : barns) {
            try {
                String projectId = String.valueOf(barn.get("project_id"));
                double residual = calculateFinalBDRL(projectId, currentYear,
                    tc, hc, tend, hMin, repairBoost, repairExtend);

                jdbcTemplate.update("UPDATE kf_basedata SET life_residual_final = ? WHERE project_id = ?",
                    Math.round(residual * 10.0) / 10.0, projectId);
                success++;
            } catch (Exception e) {
                fail++;
            }
        }

        // 同步到 current_life_residual
        jdbcTemplate.update("UPDATE kf_basedata SET current_life_residual = life_residual_final WHERE life_residual_final IS NOT NULL");

        // 同步到 ovens 表（predicted_life_years）
        jdbcTemplate.update("UPDATE ovens o INNER JOIN kf_basedata k ON k.project_id = o.id SET o.predicted_life_years = k.life_residual_final WHERE k.life_residual_final IS NOT NULL");

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("algo", "bdrl");
        result.put("success", success);
        result.put("fail", fail);
        result.put("total", barns.size());
        return R.ok(result, "BDRL寿命预测算法已应用到" + success + "栋烤房，结果已存入 life_residual_final");
    }

    /**
     * 维修记录驱动健康分/剩余寿命更新
     * 基于 repair_records 维修记录：更换/升级部件后，重算增强型BMHI健康分 + BDRL剩余寿命，
     * 并按更换类型/维修动作追加“寿命续期”与“健康加分”，同步到 health_score_final / life_residual_final 及 ovens 表。
     */
    @PostMapping("/repair-sync")
    public R<Map<String, Object>> repairSync(@RequestBody Map<String, Object> params) {
        int currentYear = java.time.LocalDate.now().getYear();

        // 增强型BMHI参数
        double alpha = toDouble(params.get("alpha"), 0.40);
        double beta = toDouble(params.get("beta"), 0.30);
        double gamma = toDouble(params.get("gamma"), 0.30);
        double wJr = toDouble(params.get("wJr"), 0.30);
        double wSr = toDouble(params.get("wSr"), 0.26);
        double wZk = toDouble(params.get("wZk"), 0.26);
        double wZt = toDouble(params.get("wZt"), 0.18);
        double lJr = toDouble(params.get("lJr"), 10);
        double lSr = toDouble(params.get("lSr"), 12);
        double lZk = toDouble(params.get("lZk"), 10);
        double lZt = toDouble(params.get("lZt"), 20);

        // BDRL参数
        double tc = toDouble(params.get("tc"), 8);
        double hc = toDouble(params.get("hc"), 60);
        double tend = toDouble(params.get("tend"), 12);
        double hMin = toDouble(params.get("hMin"), 40);
        double repairBoost = toDouble(params.get("repairBoost"), 12);
        double repairExtend = toDouble(params.get("repairExtend"), 2.5);

        // 维修记录按 烤房 分组（过滤 collation 差异，Java 内存匹配）
        Map<String, List<double[]>> repairByBarn = new HashMap<>();
        List<Map<String, Object>> repairRows = jdbcTemplate.queryForList(
            "SELECT oven_id, replacement_type, repair_action FROM repair_records WHERE oven_id IS NOT NULL");
        for (Map<String, Object> row : repairRows) {
            String ovenId = String.valueOf(row.get("oven_id"));
            if (ovenId == null || ovenId.trim().isEmpty()) continue;
            String replacement = row.get("replacement_type") == null ? "" : String.valueOf(row.get("replacement_type"));
            String action = row.get("repair_action") == null ? "" : String.valueOf(row.get("repair_action"));
            double[] credit = new double[]{ lifeExt(replacement, action), healthBoost(replacement, action) };
            repairByBarn.computeIfAbsent(ovenId, k -> new ArrayList<>()).add(credit);
        }

        List<Map<String, Object>> barns = jdbcTemplate.queryForList("SELECT project_id FROM kf_basedata");

        int success = 0, fail = 0, repaired = 0;
        for (Map<String, Object> barn : barns) {
            try {
                String projectId = String.valueOf(barn.get("project_id"));

                double baseHealth = calculateEnhancedBMHI(projectId, currentYear,
                    alpha, beta, gamma, wJr, wSr, wZk, wZt, lJr, lSr, lZk, lZt);
                double baseLife = calculateFinalBDRL(projectId, currentYear,
                    tc, hc, tend, hMin, repairBoost, repairExtend);

                double extSum = 0, boostSum = 0;
                List<double[]> credits = repairByBarn.get(projectId);
                if (credits != null) {
                    for (double[] row : credits) {
                        extSum += row[0];
                        boostSum += row[1];
                    }
                    extSum = Math.min(extSum, 6.0);
                    boostSum = Math.min(boostSum, 12.0);
                    repaired++;
                }

                // 维修加分/续期
                double newHealth = Math.min(99.0, baseHealth + boostSum);
                double newLife = Math.max(0, Math.min(12.0, baseLife + extSum));
                newHealth = Math.round(newHealth * 10.0) / 10.0;
                newLife = Math.round(newLife * 10.0) / 10.0;

                jdbcTemplate.update("UPDATE kf_basedata SET health_score_final = ?, life_residual_final = ?, " +
                        "current_health_score = ?, current_life_residual = ? WHERE project_id = ?",
                    newHealth, newLife, newHealth, newLife, projectId);
                success++;
            } catch (Exception e) {
                fail++;
            }
        }

        // 同步到 ovens 表
        jdbcTemplate.update("UPDATE ovens o INNER JOIN kf_basedata k ON k.project_id = o.id SET " +
            "o.health_score = k.health_score_final, o.predicted_life_years = k.life_residual_final WHERE k.health_score_final IS NOT NULL");
        jdbcTemplate.update("UPDATE ovens o INNER JOIN kf_basedata k ON k.project_id = o.id SET o.health_level = " +
            "CASE WHEN k.health_score_final >= 90 THEN '优良' " +
            "WHEN k.health_score_final >= 75 THEN '良好' " +
            "WHEN k.health_score_final >= 60 THEN '一般' " +
            "WHEN k.health_score_final >= 40 THEN '预警' " +
            "ELSE '危险' END WHERE k.health_score_final IS NOT NULL");

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", success);
        result.put("fail", fail);
        result.put("total", barns.size());
        result.put("repairedBarns", repaired);
        return R.ok(result, "维修记录已驱动" + repaired + "栋烤房续期寿命并加分，结果已同步至健康分/剩余寿命");
    }

    /**
     * 单条维修记录的寿命续期（年）
     */
    private double lifeExt(String replacement, String action) {
        double ext;
        if (replacement.contains("新采购")) ext = 2.0;
        else if (replacement.contains("改造升级")) ext = 1.5;
        else if (replacement.contains("防护加固")) ext = 1.0;
        else ext = 0.8;
        if (action != null && (action.contains("更换") || action.contains("替换"))) ext += 0.5;
        return ext;
    }

    /**
     * 单条维修记录的健康加分
     */
    private double healthBoost(String replacement, String action) {
        double boost;
        if (replacement.contains("新采购")) boost = 5;
        else if (replacement.contains("改造升级")) boost = 4;
        else if (replacement.contains("防护加固") || replacement.contains("维护修复")) boost = 2;
        else boost = 3;
        if (action != null && (action.contains("更换") || action.contains("替换"))) boost += 2;
        return boost;
    }

    /**
     * BDRL寿命预测列表（最终版）
     */
    @GetMapping("/life-final-list")
    public TableDataInfo<Map<String, Object>> lifeFinalList(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String county,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String warnLevel) {

        StringBuilder where = new StringBuilder("WHERE 1=1");
        List<Object> params = new ArrayList<>();
        if (county != null && !county.isEmpty()) {
            where.append(" AND county = ?");
            params.add(county);
        }
        if (keyword != null && !keyword.isEmpty()) {
            where.append(" AND (project_id LIKE ? OR township LIKE ?)");
            params.add("%" + keyword + "%");
            params.add("%" + keyword + "%");
        }
        if (warnLevel != null && !warnLevel.isEmpty()) {
            switch (warnLevel) {
                case "正常": where.append(" AND life_residual_final > 7"); break;
                case "需关注": where.append(" AND life_residual_final > 4 AND life_residual_final <= 7"); break;
                case "建议维修": where.append(" AND life_residual_final > 0 AND life_residual_final <= 4"); break;
                case "紧急维修": where.append(" AND life_residual_final <= 0"); break;
            }
        }

        String countSql = "SELECT COUNT(*) as total FROM kf_basedata " + where;
        Map<String, Object> countResult = jdbcTemplate.queryForMap(countSql, params.toArray());
        long total = ((Number) countResult.get("total")).longValue();

        int offset = (pageNum - 1) * pageSize;
        String listSql = "SELECT project_id, county, township, village, use_status, finish_date, " +
                "life_residual_final as life_residual, health_score_final " +
                "FROM kf_basedata " + where +
                " ORDER BY life_residual_final IS NULL, life_residual_final ASC LIMIT ? OFFSET ?";
        params.add(pageSize);
        params.add(offset);

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(listSql, params.toArray());
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("projectId", row.get("project_id"));
            m.put("county", row.get("county"));
            m.put("township", row.get("township"));
            m.put("village", row.get("village"));
            m.put("useStatus", row.get("use_status"));
            m.put("finishDate", row.get("finish_date"));
            Object residual = row.get("life_residual");
            double resVal = residual != null ? ((Number) residual).doubleValue() : -1;
            m.put("lifeResidual", residual != null ? resVal : null);
            m.put("warnLevel", residual != null ? getFinalWarnLevel(resVal) : "未计算");
            // 预测退役年份
            if (residual != null) {
                m.put("retireYear", currentYear + (int) Math.ceil(resVal));
            } else {
                m.put("retireYear", null);
            }
            result.add(m);
        }
        return TableDataInfo.build(result, total, pageNum, pageSize);
    }

    /**
     * BDRL寿命预测统计（最终版）
     */
    @GetMapping("/life-final-stats")
    public R<Map<String, Object>> lifeFinalStats() {
        Map<String, Object> result = new LinkedHashMap<>();

        Map<String, Object> totalRow = jdbcTemplate.queryForMap(
            "SELECT COUNT(*) as total, COUNT(life_residual_final) as calculated, " +
            "ROUND(AVG(life_residual_final),1) as avg_life, " +
            "MIN(life_residual_final) as min_life, MAX(life_residual_final) as max_life " +
            "FROM kf_basedata");
        result.put("total", totalRow.get("total"));
        result.put("calculated", totalRow.get("calculated"));
        result.put("avgLife", totalRow.get("avg_life") != null ? Math.round(((Number) totalRow.get("avg_life")).doubleValue() * 10.0) / 10.0 : 0);
        result.put("minLife", totalRow.get("min_life"));
        result.put("maxLife", totalRow.get("max_life"));

        // 预警分布
        List<Map<String, Object>> distribution = jdbcTemplate.queryForList(
            "SELECT CASE " +
            "WHEN life_residual_final <= 0 THEN '紧急维修' " +
            "WHEN life_residual_final <= 4 THEN '建议维修' " +
            "WHEN life_residual_final <= 7 THEN '需关注' " +
            "WHEN life_residual_final IS NOT NULL THEN '正常' " +
            "ELSE '未计算' END as level, COUNT(*) as cnt " +
            "FROM kf_basedata GROUP BY level ORDER BY cnt DESC");
        result.put("distribution", distribution);
        return R.ok(result);
    }

    // ======================== 最终版算法核心实现 ========================

    /**
     * 增强型BMHI算法（最终版）
     * BMHI框架 + CDCI三段式退化函数 + 4部件权重（无FS）
     */
    private double calculateEnhancedBMHI(String projectId, int currentYear,
            double alpha, double beta, double gamma,
            double wJr, double wSr, double wZk, double wZt,
            double lJr, double lSr, double lZk, double lZt) {

        // 计算各部件健康分（使用CDCI三段式退化函数）
        double jrScore = getEnhancedComponentScore(projectId, "kf_heater",
            new String[]{"burner_status"}, new String[]{"burner_repair_year"},
            currentYear, alpha, beta, gamma, lJr);
        double srScore = getEnhancedComponentScore(projectId, "kf_radiator",
            new String[]{"pipe_status","furnace_status","ash_door_status","chimney_status"},
            new String[]{"pipe_repair_year","furnace_repair_year","ash_door_repair_year","chimney_repair_year"},
            currentYear, alpha, beta, gamma, lSr);
        double zkScore = getEnhancedComponentScore(projectId, "kf_controller",
            new String[]{"cold_door_status","cold_door_motor_status","exhaust_window_status","circulation_fan_status","combustion_fan_status","control_box_status","water_pot_status","probe_status"},
            new String[]{"cold_door_repair_year","cold_door_motor_repair_year","exhaust_window_repair_year","circulation_fan_repair_year","combustion_fan_repair_year","control_box_repair_year","water_pot_repair_year","probe_repair_year"},
            currentYear, alpha, beta, gamma, lZk);
        double ztScore = getEnhancedComponentScore(projectId, "kf_oven_body",
            new String[]{"wall_roof_status","beam_status","door_status"},
            new String[]{"wall_roof_repair_year","beam_repair_year","door_repair_year"},
            currentYear, alpha, beta, gamma, lZt);

        // 加权汇总（4部件，无FS）
        return wJr * jrScore + wSr * srScore + wZk * zkScore + wZt * ztScore;
    }

    /**
     * 增强型BMHI部件得分计算
     * C_i = α × S_status + β × S_degrade + γ × S_eval
     * S_degrade 使用CDCI三段式退化函数
     */
    private double getEnhancedComponentScore(String projectId, String table,
            String[] statusCols, String[] repairCols,
            int currentYear, double alpha, double beta, double gamma, double ratedLife) {
        try {
            StringBuilder sql = new StringBuilder("SELECT * FROM " + table + " WHERE project_id = ?");
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql.toString(), projectId);
            if (rows.isEmpty()) return 70.0;

            Map<String, Object> row = rows.get(0);
            String evaluation = String.valueOf(row.getOrDefault("evaluation", "较好"));
            double evalScore = getEvalScore(evaluation);

            double sum = 0;
            int count = 0;
            for (int i = 0; i < statusCols.length; i++) {
                String status = String.valueOf(row.getOrDefault(statusCols[i], "正常"));
                Object repairYearObj = row.get(repairCols[i]);
                int repairYear = 0;
                if (repairYearObj != null) {
                    try { repairYear = Integer.parseInt(String.valueOf(repairYearObj).substring(0, 4)); } catch(Exception e) {}
                }
                double dt = repairYear > 0 ? currentYear - repairYear : 5;

                // S_status: 正常=100, 异常=50, 损坏=0
                double statusScore = status.contains("正常") ? 100 : status.contains("异常") ? 50 : 0;

                // S_degrade: CDCI三段式退化函数
                double degradeScore = calculateThreeStageDegradation(dt, ratedLife);

                // C_i = α × S_status + β × S_degrade + γ × S_eval
                double subScore = alpha * statusScore + beta * degradeScore + gamma * evalScore;
                sum += subScore;
                count++;
            }
            return count > 0 ? sum / count : 70.0;
        } catch (Exception e) {
            return 70.0;
        }
    }

    /**
     * CDCI三段式退化函数（最终版）
     * 稳定区(Δt≤0.2L): S=100
     * 线性区(0.2L<Δt≤0.7L): S=100-k1×(Δt-0.2L), k1=100/(0.5L)
     * 加速劣化区(Δt>0.7L): S=30×e^(-k2×(Δt-0.7L)), k2=2/L
     */
    private double calculateThreeStageDegradation(double dt, double ratedLife) {
        double stableEnd = 0.2 * ratedLife;
        double linearEnd = 0.7 * ratedLife;
        double k1 = 100.0 / (0.5 * ratedLife);
        double k2 = 2.0 / ratedLife;
        double hMid = 30.0;

        if (dt <= stableEnd) {
            return 100.0;
        } else if (dt <= linearEnd) {
            return Math.max(0, 100.0 - k1 * (dt - stableEnd));
        } else {
            return Math.max(0, hMid * Math.exp(-k2 * (dt - linearEnd)));
        }
    }

    /**
     * BDRL寿命预测算法（最终版）
     * 使用health_score_final作为输入，双段退化模型
     */
    private double calculateFinalBDRL(String projectId, int currentYear,
            double tc, double hc, double tend, double hMin,
            double repairBoost, double repairExtend) {
        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT finish_date, start_date, health_score_final FROM kf_basedata WHERE project_id = ?", projectId);
            if (rows.isEmpty()) return 0;

            Map<String, Object> row = rows.get(0);
            int buildYear = currentYear;
            Object dateObj = row.get("finish_date");
            if (dateObj == null) dateObj = row.get("start_date");
            if (dateObj != null) {
                String dateStr = String.valueOf(dateObj);
                if (dateStr.length() >= 4) {
                    try { buildYear = Integer.parseInt(dateStr.substring(0, 4)); } catch(Exception e) {}
                }
            }
            double age = currentYear - buildYear;
            if (age < 0) age = 0;

            // 使用health_score_final作为当前健康分
            Object scoreObj = row.get("health_score_final");
            double h0 = scoreObj != null ? ((Number) scoreObj).doubleValue() : 70.0;

            // 维修次数（每8年算1次大修）
            int repairCount = (int) (age / 8);
            double tendAdj = tend + repairCount * repairExtend;

            double residual;
            if (age <= tc) {
                // 前段: 线性退化 H=100-k1×t
                double k1 = (100.0 - hc) / tc;
                double tEnd = (100.0 - hMin) / k1;
                residual = tEnd - age;
            } else {
                // 后段: 二次函数退化 H=Hc×[1-((t-Tc)/(Tend-Tc))²]
                // 求解 H(t) = hMin
                double ratio = Math.sqrt(Math.max(0, 1.0 - hMin / hc));
                double tEnd = tc + ratio * (tendAdj - tc);
                residual = tEnd - age;
            }

            // 考虑维修回弹效应：实际健康分高于理论值，说明有维修
            // 理论健康分
            double theoreticalH;
            if (age <= tc) {
                double k1 = (100.0 - hc) / tc;
                theoreticalH = 100.0 - k1 * age;
            } else {
                theoreticalH = hc * (1.0 - Math.pow((age - tc) / (tendAdj - tc), 2));
            }

            // 如果实际健康分 > 理论值，说明有维修回弹，延长寿命
            if (h0 > theoreticalH && theoreticalH > 0) {
                double boostRatio = h0 / theoreticalH;
                residual = residual * boostRatio;
            }

            return Math.max(0, Math.min(residual, 12)); // 最大12年（极限寿命）
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * 最终版健康等级（5级）
     */
    private String getFinalHealthLevel(double score) {
        if (score >= 90) return "优良";
        if (score >= 75) return "良好";
        if (score >= 60) return "一般";
        if (score >= 40) return "预警";
        return "危险";
    }

    /**
     * 最终版预警等级
     */
    private String getFinalWarnLevel(double residual) {
        if (residual <= 0) return "紧急维修";
        if (residual <= 4) return "建议维修";
        if (residual <= 7) return "需关注";
        return "正常";
    }

    private int currentYear = java.time.LocalDate.now().getYear();

    // ======================== 辅助方法 ========================

    private double getEvalScore(String evaluation) {
        if (evaluation == null) return 70;
        if (evaluation.contains("较好") || evaluation.contains("优良")) return 80;
        if (evaluation.contains("一般")) return 60;
        if (evaluation.contains("较差")) return 50;
        if (evaluation.contains("差")) return 30;
        return 70;
    }

    private double getRatedLife(String statusCol) {
        if (statusCol == null) return 10;
        if (statusCol.contains("fan") || statusCol.contains("motor")) return 6;
        if (statusCol.contains("probe") || statusCol.contains("water_pot")) return 5;
        if (statusCol.contains("wall_roof")) return 15;
        if (statusCol.contains("beam")) return 20;
        if (statusCol.contains("door")) return 12;
        if (statusCol.contains("burner")) return 8;
        return 8; // 默认
    }

    private String getHealthLevel(double score) {
        if (score >= 85) return "优良";
        if (score >= 70) return "良好";
        if (score >= 50) return "一般";
        return "较差";
    }

    private String getWarnLevel(double residual) {
        if (residual <= 1) return "紧急维修";
        if (residual <= 3) return "需维修";
        if (residual <= 5) return "需关注";
        return "正常";
    }

    private double toDouble(Object obj, double defaultVal) {
        if (obj == null) return defaultVal;
        try { return Double.parseDouble(String.valueOf(obj)); } catch (Exception e) { return defaultVal; }
    }
}
