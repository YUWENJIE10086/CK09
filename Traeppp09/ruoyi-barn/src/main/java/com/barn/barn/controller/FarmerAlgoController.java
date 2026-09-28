package com.barn.barn.controller;

import com.barn.common.core.domain.R;
import com.barn.common.core.domain.TableDataInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.web.bind.annotation.*;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.*;

/**
 * 烟农画像与培育算法 Controller（仅新增功能，不动既有烟农/烤房逻辑）
 * 依据《烟农画像与培育算法详解-0901》：
 *   五维 = 栽培0.18 / 植保0.18 / 采烤0.19 / 烘烤0.25 / 综合0.20
 *   画像分 P = Σ w_d·K_d（缺失维度按权重再归一）
 *   分级：P≥85 职业 | 60≤P<85 普通 | P<60 新手
 *   薄弱定位：村×维度缺口 I=m_县 − K̄_村，K̄<60 或 I>15 → 标红
 * 数据存于独立新表 farmer_question / farmer_profile
 */
@RestController
@RequestMapping("/farmer-algo")
@PreAuthorize("hasRole('ADMIN')")
public class FarmerAlgoController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /** 五维顺序与基础权重 */
    private static final String[] DIMS = {"栽培", "植保", "采烤", "烘烤", "综合"};
    private static final String[] DIM_COLS = {"k_cult", "k_pp", "k_hv", "k_cur", "k_syn"};
    private static final double[] W = {0.18, 0.18, 0.19, 0.25, 0.20};

    /** 每维默认题数（23 道知识题拆分，综合维含 6 道管护类知识题） */
    private static final int[] DIM_QUESTIONS = {4, 4, 4, 5, 6};

    /** 薄弱维 → 培育主题 映射表 */
    private static final Map<String, String> THEME = new HashMap<>();
    static {
        THEME.put("栽培", "壮苗培育·大田移栽·水肥管护现场会");
        THEME.put("植保", "绿色防控·统防统治·病毒病防治培训");
        THEME.put("采烤", "成熟采收·编竿上炕·工艺课");
        THEME.put("烘烤", "烤房温湿度曲线·定色转火现场会");
        THEME.put("综合", "管护制度·调剂/抢修平台操作·政策宣贯");
    }

    public FarmerAlgoController() {}

    // ======================== 基础维护接口 ========================

    /** 导入答题明细（JSON 数组），replace=true 时先清空该批次再写入 */
    @PostMapping("/import")
    public R<Map<String, Object>> importAnswers(@RequestBody List<Map<String, Object>> rows,
                                                @RequestParam(defaultValue = "2026") String paperId,
                                                @RequestParam(defaultValue = "true") boolean replace) {
        Map<String, Object> result = new HashMap<>();
        if (rows == null || rows.isEmpty()) {
            result.put("success", 0);
            return R.ok(result);
        }
        if (replace) {
            jdbcTemplate.update("DELETE FROM farmer_question WHERE paper_id = ?", paperId);
        }
        int success = 0;
        for (Map<String, Object> r : rows) {
            try {
                jdbcTemplate.update(
                        "INSERT INTO farmer_question (paper_id, farmer_name, farmer_phone, county, township, village, pound_group, " +
                                "dim, question_no, question, answer, key_ind, correct, survey_date) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                        paperId, str(r.get("farmerName")), str(r.get("farmerPhone")), str(r.get("county")), str(r.get("township")),
                        str(r.get("village")), str(r.get("poundGroup")), str(r.get("dim")), intV(r.get("questionNo")),
                        str(r.get("question")), str(r.get("answer")), str(r.get("key")), intV(r.get("correct")),
                        dateOrNull(r.get("surveyDate"))
                );
                success++;
            } catch (Exception e) {
                // 不向标准输出或接口响应暴露数据库/实现细节；失败行仅计入失败数。
                // 生产环境应由统一日志组件按需记录异常堆栈并配合脱敏策略。
            }
        }
        result.put("success", success);
        result.put("paperId", paperId);
        return R.ok(result);
    }

    /** 生成演示问卷数据（便于界面即开即看，可随时清空） */
    @PostMapping("/demo")
    public R<Map<String, Object>> demo(@RequestParam(defaultValue = "2026") String paperId) {
        // [蒲公英前缀, 乡镇, 村, 栽培偏移概率, 烘烤偏移概率]（偏移>0 更弱于基准）
        Object[][] villages = {
                {"磨坪", "柏家坪村", 0.30, 0.05},
                {"磨坪", "三墩岩村", 0.08, 0.10},
                {"杨林桥", "杨林桥村", 0.05, 0.32},
                {"郭家坝", "桐树包村", 0.18, 0.12},
        };
        String[] families = {"陈", "王", "李", "张", "刘", "谭", "向", "郑", "周", "吴"};
        Random rnd = new Random(20260901L);

        jdbcTemplate.update("DELETE FROM farmer_question WHERE paper_id = ?", paperId);
        int total = 0;
        List<Object[]> batch = new ArrayList<>();
        int famIdx = 0;
        for (Object[] v : villages) {
            String township = (String) v[0];
            String village = (String) v[1];
            double cultWeak = (double) v[2];   // 栽培薄弱偏移
            double curWeak  = (double) v[3];   // 烘烤薄弱偏移
            int fams = 6 + rnd.nextInt(4);     // 每村 6-9 户
            for (int f = 0; f < fams; f++) {
                famIdx++;
                String name = families[famIdx % families.length] + (f + 1) + "烟农";
                String phone = "138" + String.format("%08d", 10000000 + famIdx);
                String pound = village.replace("村", "") + (f % 3 + 1) + "组";
                // 每户答该批次全部 23 道知识题（各维独立作答，偏移越大答对率越低）
                for (int d = 0; d < DIMS.length; d++) {
                    double base = 0.62;      // 基准答对率
                    double offset = 0.0;
                    if (DIMS[d].equals("栽培")) offset = cultWeak;
                    if (DIMS[d].equals("烘烤")) offset = curWeak;
                    if (DIMS[d].equals("综合")) offset = 0.15; // 综合略弱
                    offset += (rnd.nextDouble() - 0.5) * 0.10;  // 个体差异
                    int questions = DIM_QUESTIONS[d];
                    for (int q = 1; q <= questions; q++) {
                        int no = (d == 0 ? 0 : sum(DIM_QUESTIONS, d)) + q;
                        boolean correct = ((base - offset) * 100) >= rnd.nextInt(101);
                        String dim = DIMS[d];
                        batch.add(new Object[]{paperId, name, phone, "秭归县", township, village, pound,
                                dim, no, "[" + dim + "题] 调研知识题" + no, correct ? "正确" : "错误", "正确", correct ? 1 : 0,
                                java.sql.Date.valueOf(LocalDate.of(2026, 8, 20))});
                        total++;
                    }
                }
            }
        }
        List<Object[]> finalBatch = batch;
        jdbcTemplate.batchUpdate(
                "INSERT INTO farmer_question (paper_id, farmer_name, farmer_phone, county, township, village, pound_group, " +
                        "dim, question_no, question, answer, key_ind, correct, survey_date) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                new BatchPreparedStatementSetter() {
                    @Override public void setValues(PreparedStatement ps, int i) throws SQLException {
                        Object[] r = finalBatch.get(i);
                        for (int k = 0; k < r.length; k++) ps.setObject(k + 1, r[k]);
                    }
                    @Override public int getBatchSize() { return finalBatch.size(); }
                }
        );
        computeProfiles(paperId);
        Map<String, Object> result = new HashMap<>();
        result.put("success", total);
        result.put("message", "已生成演示答题数据 " + total + " 条，并完成画像重建");
        return R.ok(result);
    }

    /** 清空某批次答题与画像 */
    @PostMapping("/clear")
    public R<String> clear(@RequestParam(defaultValue = "2026") String paperId) {
        jdbcTemplate.update("DELETE FROM farmer_profile WHERE paper_id = ?", paperId);
        jdbcTemplate.update("DELETE FROM farmer_question WHERE paper_id = ?", paperId);
        return R.ok("已清空批次 " + paperId);
    }

    // ======================== 答题情况查询 ========================

    /** 答题明细分页 */
    @GetMapping("/answers")
    public TableDataInfo<Map<String, Object>> answers(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String county,
            @RequestParam(required = false) String township,
            @RequestParam(required = false) String village,
            @RequestParam(required = false) String dim,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "2026") String paperId) {
        StringBuilder where = new StringBuilder(" WHERE paper_id = ?");
        List<Object> params = new ArrayList<>();
        params.add(paperId);
        if (notBlank(county)) { where.append(" AND county = ?"); params.add(county); }
        if (notBlank(township)) { where.append(" AND township = ?"); params.add(township); }
        if (notBlank(village)) { where.append(" AND village = ?"); params.add(village); }
        if (notBlank(dim)) { where.append(" AND dim = ?"); params.add(dim); }
        if (notBlank(keyword)) { where.append(" AND (farmer_name LIKE ? OR question LIKE ?)"); params.add("%" + keyword + "%"); params.add("%" + keyword + "%"); }

        Long total = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM farmer_question" + where, Long.class, params.toArray());
        pageNum = safePageNum(pageNum);
        pageSize = safePageSize(pageSize);
        long offset = ((long) pageNum - 1L) * pageSize;
        List<Map<String, Object>> list = jdbcTemplate.queryForList(
                "SELECT id, paper_id AS paperId, farmer_name AS farmerName, farmer_phone AS farmerPhone, county, township, village," +
                        " pound_group AS poundGroup, dim, question_no AS questionNo, question, answer, key_ind AS keyInd, correct, survey_date AS surveyDate" +
                        " FROM farmer_question" + where + " ORDER BY village, farmer_name, question_no LIMIT ? OFFSET ?",
                appendPageParams(params, pageSize, offset).toArray());
        return TableDataInfo.build(list, total == null ? 0 : total, pageNum, pageSize);
    }

    /** 答题概况：覆盖户数/答条/总正确率 + 各维正确率 + 各地覆盖面 */
    @GetMapping("/answer-stats")
    public R<Map<String, Object>> answerStats(@RequestParam(defaultValue = "2026") String paperId) {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("paperId", paperId);
        r.put("farmers", jdbcTemplate.queryForObject("SELECT COUNT(DISTINCT farmer_phone) FROM farmer_question WHERE paper_id=?", Integer.class, paperId));
        r.put("rows", jdbcTemplate.queryForObject("SELECT COUNT(*) FROM farmer_question WHERE paper_id=?", Long.class, paperId));
        r.put("avgCorrect", round(oneDouble("SELECT AVG(correct)*100 FROM farmer_question WHERE paper_id=?", paperId)));
        // 各维正确率
        List<Map<String, Object>> dimStats = jdbcTemplate.queryForList(
                "SELECT dim, COUNT(*) AS cnt, ROUND(AVG(correct)*100,1) AS correct FROM farmer_question WHERE paper_id=? GROUP BY dim", paperId);
        Map<String, Double> base = new LinkedHashMap<>();
        for (String d : DIMS) base.put(d, null);
        Map<String, Object> byDim = new LinkedHashMap<>();
        for (String d : DIMS) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("dim", d);
            row.put("cnt", 0);
            row.put("correct", null);
            for (Map<String, Object> s : dimStats) {
                if (d.equals(s.get("dim"))) {
                    row.put("cnt", s.get("cnt"));
                    row.put("correct", s.get("correct"));
                }
            }
            byDim.put(d, row);
        }
        r.put("byDim", byDim);
        // 各地覆盖面（乡镇级）
        r.put("region", jdbcTemplate.queryForList(
                "SELECT township, village, COUNT(DISTINCT farmer_phone) AS farmers, COUNT(*) AS `rows`" +
                        " FROM farmer_question WHERE paper_id=? GROUP BY township, village ORDER BY township, village", paperId));
        // 可选维度下拉
        r.put("villages", jdbcTemplate.queryForList(
                "SELECT DISTINCT township, village FROM farmer_question WHERE paper_id=? AND village<>'' ORDER BY township, village", paperId));
        return R.ok(r);
    }

    // ======================== 画像计算与查询 ========================

    /** 重建烟农画像：由 farmer_question 汇总写 farmer_profile（先清空该批次） */
    @PostMapping("/compute")
    public R<Map<String, Object>> computeProfiles(@RequestParam(defaultValue = "2026") String paperId) {
        int n = rebuildProfiles(paperId);
        Map<String, Object> r = new HashMap<>();
        r.put("success", n);
        r.put("message", "已重建 " + n + " 份烟农画像");
        return R.ok(r);
    }

    private int rebuildProfiles(String paperId) {
        jdbcTemplate.update("DELETE FROM farmer_profile WHERE paper_id = ?", paperId);
        List<Map<String, Object>> farmers = jdbcTemplate.queryForList(
                "SELECT farmer_phone AS phone, MAX(farmer_name) AS name, MIN(county) AS county, MIN(township) AS township," +
                        " MIN(village) AS village, MAX(pound_group) AS pound " +
                        " FROM farmer_question WHERE paper_id=? AND farmer_phone<>'' GROUP BY farmer_phone", paperId);
        for (Map<String, Object> f : farmers) {
            String phone = (String) f.get("phone");
            Map<String, Map<String, Number>> agg = new LinkedHashMap<>();
            for (String d : DIMS) agg.put(d, new HashMap<>());
            List<Map<String, Object>> answers = jdbcTemplate.queryForList(
                    "SELECT dim, correct FROM farmer_question WHERE paper_id=? AND farmer_phone=? AND dim<>''", paperId, phone);
            for (Map<String, Object> a : answers) {
                String d = (String) a.get("dim");
                if (!agg.containsKey(d)) continue;
                int correct = ((Number) a.get("correct")).intValue();
                Map<String, Number> m = agg.get(d);
                m.put("cnt", m.containsKey("cnt") ? m.get("cnt").intValue() + 1 : 1);
                m.put("ok", m.containsKey("ok") ? m.get("ok").intValue() + correct : correct);
            }
            double[] k = new double[DIMS.length];
            double wSum = 0; double pNum = 0; boolean any = false;
            for (int d = 0; d < DIMS.length; d++) {
                Map<String, Number> m = agg.get(DIMS[d]);
                if (m != null && m.containsKey("cnt") && m.get("cnt").intValue() > 0) {
                    any = true;
                    k[d] = m.get("ok").doubleValue() / m.get("cnt").doubleValue() * 100.0;
                    pNum += W[d] * k[d];
                    wSum += W[d];
                }
            }
            if (!any) continue;
            double p = pNum / wSum;
            int minIdx = -1; double minV = 101;
            for (int d = 0; d < DIMS.length; d++) {
                if (agg.get(DIMS[d]) != null && agg.get(DIMS[d]).containsKey("cnt") && agg.get(DIMS[d]).get("cnt").intValue() > 0 && k[d] < minV) {
                    minV = k[d]; minIdx = d;
                }
            }
            String level = p >= 85 ? "职业" : (p >= 60 ? "普通" : "新手");
            double priority = minIdx >= 0 ? Math.round((0.5 * (100 - k[minIdx]) + 0.3 * 55 + 0.2 * 50) * 10) / 10.0 : 0;
            jdbcTemplate.update(
                    "INSERT INTO farmer_profile (paper_id, farmer_name, farmer_phone, county, township, village, pound_group," +
                            " k_cult, k_pp, k_hv, k_cur, k_syn, p_score, level, weak_dim, priority, survey_date) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                    paperId, str(f.get("name")), phone, str(f.get("county")), str(f.get("township")), str(f.get("village")), str(f.get("pound")),
                    k[0], k[1], k[2], k[3], k[4], p, level, minIdx >= 0 ? DIMS[minIdx] : "", priority,
                    java.sql.Date.valueOf(LocalDate.of(2026, 8, 20)));
        }
        return farmers.size();
    }

    /** Overview KPI + 分级分布 */
    @GetMapping("/overview")
    public R<Map<String, Object>> overview(@RequestParam(defaultValue = "2026") String paperId) {
        Map<String, Object> r = new LinkedHashMap<>();
        Integer fam = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM farmer_profile WHERE paper_id=?", Integer.class, paperId);
        fam = fam == null ? 0 : fam;
        r.put("profileCount", fam);
        r.put("avgP", round(oneDouble("SELECT AVG(p_score) FROM farmer_profile WHERE paper_id=?", paperId)));
        r.put("weakVillages", jdbcTemplate.queryForObject(
                "SELECT COUNT(DISTINCT village) FROM farmer_profile WHERE paper_id=? AND village<>'' " +
                        "AND (k_cult<60 OR k_pp<60 OR k_hv<60 OR k_cur<60 OR k_syn<60)", Integer.class, paperId));
        r.put("needVisit", jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM farmer_profile WHERE paper_id=? AND (k_cult<60 OR k_pp<60 OR k_hv<60 OR k_cur<60 OR k_syn<60)", Integer.class, paperId));
        r.put("levelDist", jdbcTemplate.queryForList(
                "SELECT level, COUNT(*) AS cnt FROM farmer_profile WHERE paper_id=? GROUP BY level", paperId));
        return R.ok(r);
    }

    /** 村×五维薄弱矩阵 + 县级基线 + 红黄绿状态 */
    @GetMapping("/village-gap")
    public R<Map<String, Object>> villageGap(@RequestParam(defaultValue = "2026") String paperId) {
        Map<String, Object> r = new LinkedHashMap<>();
        // 县级基线 m_d
        double[] m = new double[DIMS.length];
        for (int d = 0; d < DIMS.length; d++) {
            m[d] = round(oneDouble("SELECT AVG(" + DIM_COLS[d] + ") FROM farmer_profile WHERE paper_id=?", paperId));
        }
        r.put("baseline", buildBaseline(m));
        // 村聚合
        List<Map<String, Object>> villages = jdbcTemplate.queryForList(
                "SELECT township, village, COUNT(*) AS farmers, ROUND(AVG(p_score),1) AS avgP, ROUND(AVG(k_cult),1) AS k_cult," +
                        " ROUND(AVG(k_pp),1) AS k_pp, ROUND(AVG(k_hv),1) AS k_hv, ROUND(AVG(k_cur),1) AS k_cur, ROUND(AVG(k_syn),1) AS k_syn" +
                        " FROM farmer_profile WHERE paper_id=? AND village<>'' GROUP BY township, village ORDER BY township, village", paperId);
        List<Map<String, Object>> cells = new ArrayList<>();
        for (Map<String, Object> v : villages) {
            Map<String, Object> cell = new LinkedHashMap<>();
            cell.put("township", v.get("township"));
            cell.put("village", v.get("village"));
            cell.put("farmers", v.get("farmers"));
            cell.put("avgP", v.get("avgP"));
            List<Map<String, Object>> dims = new ArrayList<>();
            double weak = 101; String weakDim = "";
            for (int d = 0; d < DIMS.length; d++) {
                double val = num(v.get(DIM_COLS[d]));
                double gap = m[d] == 0 ? 0 : m[d] - val;
                String status;
                if (val <= 0 && val == 0 && v.get("farmers") != null && ((Number)v.get("farmers")).intValue() == 0) status = "gray";
                else if (val < 60 || gap > 15) status = "red";
                else if (val < m[d]) status = "yellow";
                else status = "green";
                if (val < weak) { weak = val; weakDim = DIMS[d]; }
                Map<String, Object> mm = new LinkedHashMap<>();
                mm.put("dim", DIMS[d]);
                mm.put("value", val);
                mm.put("gap", Math.round(gap * 10) / 10.0);
                mm.put("status", status);
                dims.add(mm);
            }
            cell.put("dims", dims);
            cell.put("weakDim", weakDim);
            cells.add(cell);
        }
        r.put("villages", cells);
        r.put("themes", THEME);
        return R.ok(r);
    }

    /** 个人画像列表（分级 + 最弱维 + 优先级） */
    @GetMapping("/profiles")
    public TableDataInfo<Map<String, Object>> profiles(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String township,
            @RequestParam(required = false) String village,
            @RequestParam(required = false) String level,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "2026") String paperId) {
        StringBuilder where = new StringBuilder(" WHERE paper_id = ?");
        List<Object> params = new ArrayList<>();
        params.add(paperId);
        if (notBlank(township)) { where.append(" AND township = ?"); params.add(township); }
        if (notBlank(village)) { where.append(" AND village = ?"); params.add(village); }
        if (notBlank(level)) { where.append(" AND level = ?"); params.add(level); }
        if (notBlank(keyword)) { where.append(" AND (farmer_name LIKE ? OR farmer_phone LIKE ?)"); params.add("%" + keyword + "%"); params.add("%" + keyword + "%"); }
        Long total = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM farmer_profile" + where, Long.class, params.toArray());
        pageNum = safePageNum(pageNum);
        pageSize = safePageSize(pageSize);
        long offset = ((long) pageNum - 1L) * pageSize;
        List<Map<String, Object>> list = jdbcTemplate.queryForList(
                "SELECT id, farmer_name AS farmerName, farmer_phone AS farmerPhone, township, village, pound_group AS poundGroup," +
                        " k_cult, k_pp, k_hv, k_cur, k_syn, p_score AS pScore, level, weak_dim AS weakDim, priority" +
                        " FROM farmer_profile" + where + " ORDER BY priority DESC LIMIT ? OFFSET ?", appendPageParams(params, pageSize, offset).toArray());
        return TableDataInfo.build(list, total == null ? 0 : total, pageNum, pageSize);
    }

    /** 培育计划：薄弱村×环节 → 主题/动作/优先级/上门名单 */
    @GetMapping("/train-plan")
    public R<Map<String, Object>> trainPlan(@RequestParam(defaultValue = "2026") String paperId) {
        Map<String, Object> r = new LinkedHashMap<>();
        // 薄弱红线：找出每个村最弱维低于阈值或缺口大的
        double[] m = new double[DIMS.length];
        for (int d = 0; d < DIMS.length; d++) m[d] = oneDouble("SELECT AVG(" + DIM_COLS[d] + ") FROM farmer_profile WHERE paper_id=?", paperId);
        List<Map<String, Object>> villages = jdbcTemplate.queryForList(
                "SELECT township, village, COUNT(*) AS farmers, ROUND(AVG(k_cult),1) AS k_cult, ROUND(AVG(k_pp),1) AS k_pp," +
                        " ROUND(AVG(k_hv),1) AS k_hv, ROUND(AVG(k_cur),1) AS k_cur, ROUND(AVG(k_syn),1) AS k_syn" +
                        " FROM farmer_profile WHERE paper_id=? AND village<>'' GROUP BY township, village", paperId);
        List<Map<String, Object>> plan = new ArrayList<>();
        for (Map<String, Object> v : villages) {
            for (int d = 0; d < DIMS.length; d++) {
                double val = num(v.get(DIM_COLS[d]));
                double gap = m[d] - val;
                if (val < 60 || gap > 15) {
                    Map<String, Object> p2 = new LinkedHashMap<>();
                    p2.put("township", v.get("township"));
                    p2.put("village", v.get("village"));
                    p2.put("dim", DIMS[d]);
                    p2.put("value", val);
                    p2.put("gap", Math.round(gap * 10) / 10.0);
                    p2.put("theme", THEME.getOrDefault(DIMS[d], DIMS[d] + "专题培训"));
                    p2.put("action", "现场会 + 上门辅导 + 复测");
                    double priority = Math.round((0.5 * (100 - val) + 0.3 * 55 + 0.2 * 50) * 10) / 10.0;
                    p2.put("priority", priority);
                    // 上门名单（该村该维得分低的前几个烟农）
                    List<Map<String, Object>> targets = jdbcTemplate.queryForList(
                            "SELECT farmer_name AS farmerName, " + DIM_COLS[d] + " AS score FROM farmer_profile" +
                                    " WHERE paper_id=? AND village=?" +
                                    (DIMS[d].equals("综合") ? " AND k_syn<60" : " AND " + DIM_COLS[d] + "<60") +
                                    " AND " + DIM_COLS[d] + ">0 ORDER BY " + DIM_COLS[d] + " ASC LIMIT 3", paperId, v.get("village"));
                    p2.put("targets", targets);
                    plan.add(p2);
                }
            }
        }
        plan.sort((a, b) -> Double.compare(-((Number) a.get("priority")).doubleValue(), -((Number) b.get("priority")).doubleValue()));
        r.put("plan", plan);
        r.put("themes", THEME);
        return R.ok(r);
    }

    /** 单烟农雷达（交付给画像页联动） */
    @GetMapping("/radar")
    public R<Map<String, Object>> radarTop(@RequestParam(defaultValue = "1") int limit,
                                           @RequestParam(defaultValue = "2026") String paperId) {
        limit = Math.min(Math.max(limit, 1), 10);
        List<Map<String, Object>> top = jdbcTemplate.queryForList(
                "SELECT farmer_name AS farmerName, township, village, k_cult, k_pp, k_hv, k_cur, k_syn, p_score AS pScore" +
                        " FROM farmer_profile WHERE paper_id=? ORDER BY p_score ASC LIMIT ?", paperId, limit);
        Map<String, Object> r = new HashMap<>();
        r.put("top", top);
        return R.ok(r);
    }

    // ======================== 工具方法 ========================

    private int safePageNum(int pageNum) {
        return Math.min(Math.max(pageNum, 1), 10000);
    }

    private int safePageSize(int pageSize) {
        return Math.min(Math.max(pageSize, 1), 100);
    }

    private List<Object> appendPageParams(List<Object> params, int pageSize, long offset) {
        List<Object> result = new ArrayList<>(params);
        result.add(pageSize);
        result.add(offset);
        return result;
    }

    private Map<String, Object> buildBaseline(double[] m) {
        Map<String, Object> b = new LinkedHashMap<>();
        for (int d = 0; d < DIMS.length; d++) b.put(DIMS[d], m[d]);
        return b;
    }

    private double oneDouble(String sql, Object... args) {
        Double v = jdbcTemplate.queryForObject(sql, Double.class, args);
        return v == null ? 0 : v;
    }

    private boolean notBlank(String s) { return s != null && !s.trim().isEmpty(); }

    private String str(Object o) { return o == null ? "" : String.valueOf(o); }

    private int intV(Object o) {
        if (o == null) return 0;
        if (o instanceof Number) return ((Number) o).intValue();
        try { return Integer.parseInt(str(o)); } catch (Exception e) { return 0; }
    }

    private double num(Object o) {
        if (o == null) return 0;
        if (o instanceof Number) return ((Number) o).doubleValue();
        return 0;
    }

    private int sum(int[] a, int uptoExclusive) { int s = 0; for (int i = 0; i < uptoExclusive; i++) s += a[i]; return s; }

    private double round(double v) { return Math.round(v * 10) / 10.0; }

    private java.sql.Date dateOrNull(Object o) {
        if (o == null) return null;
        try { return java.sql.Date.valueOf(str(o)); } catch (Exception e) { return null; }
    }
}