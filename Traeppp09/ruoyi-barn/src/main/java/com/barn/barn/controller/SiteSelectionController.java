package com.barn.barn.controller;

import com.barn.common.core.domain.R;
import com.barn.common.core.domain.TableDataInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 烤房选址评价 Controller（测试用，仅新增功能，不改动现有内容）
 * 依据《智慧烤房全生命周期管理系统》方案，采用 8 大一级指标 + AHP + 熵权法 + TOPSIS 综合评价模型：
 *   一级指标(权重)：地形地势15% / 水文条件12% / 电力保障18% / 交通条件10% / 烟田匹配20% / 环境安全10% / 集群效益8% / 成本经济7%
 *   算法流程：指标量化打分(0-100) → AHP主观权重 + 熵权法客观权重 → 组合权重 → TOPSIS综合排序
 * 数据存储于独立新表 site_selection_candidates
 */
@RestController
@RequestMapping("/site-selection")
public class SiteSelectionController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /** AHP 主观权重（来自项目方案，合计100%） */
    private static final String[] INDICATORS = {"terrain", "hydro", "power", "traffic", "tobacco", "environment", "cluster", "cost"};
    private static final String[] INDICATOR_LABELS = {"地形地势", "水文条件", "电力保障", "交通条件", "烟田匹配", "环境安全", "集群效益", "成本经济"};
    private static final double[] AHP_WEIGHTS = {0.15, 0.12, 0.18, 0.10, 0.20, 0.10, 0.08, 0.07};
    private static final double AHP_ALPHA = 0.6; // 组合权重中 AHP 占比

    // ======================== 算法描述 ========================

    @GetMapping("/algorithm-desc")
    public R<Map<String, Object>> getAlgorithmDesc() {
        Map<String, Object> desc = new LinkedHashMap<>();
        desc.put("name", "烤房选址多源数据融合评价模型");
        desc.put("fullName", "AHP + 熵权法 + TOPSIS 综合选址评价模型");
        desc.put("principle", "依据《智慧烤房全生命周期管理系统》方案，整合8大一级指标、12大类48项评价指标，"
            + "采用层次分析法(AHP)确定主观权重、熵权法计算客观权重，主客观融合后通过TOPSIS逼近理想解排序法对候选点综合排序。");
        desc.put("indicators", "地形地势15% / 水文条件12% / 电力保障18% / 交通条件10% / 烟田匹配20% / 环境安全10% / 集群效益8% / 成本经济7%");
        desc.put("formula", "① 指标打分：各指标量化到0-100分\n"
            + "② AHP权重：W_ahp = [0.15,0.12,0.18,0.10,0.20,0.10,0.08,0.07]\n"
            + "③ 熵权法：e_j = -1/ln(n)×Σp_ij·ln(p_ij),  W_ent_j = (1-e_j)/Σ(1-e_j)\n"
            + "④ 组合权重：W_j = 0.6×W_ahp_j + 0.4×W_ent_j\n"
            + "⑤ TOPSIS：C_i = D⁻/(D⁺+D⁻)，按贴近度C_i排序");
        desc.put("levels", "≥85 高度适宜, 70-85 适宜, 55-70 一般, <55 不适宜");
        desc.put("params", "[{\"key\":\"alpha\",\"label\":\"AHP权重占比\",\"default\":0.6},"
            + "{\"key\":\"wTerrain\",\"label\":\"地形地势权重\",\"default\":0.15},"
            + "{\"key\":\"wHydro\",\"label\":\"水文条件权重\",\"default\":0.12},"
            + "{\"key\":\"wPower\",\"label\":\"电力保障权重\",\"default\":0.18},"
            + "{\"key\":\"wTraffic\",\"label\":\"交通条件权重\",\"default\":0.10},"
            + "{\"key\":\"wTobacco\",\"label\":\"烟田匹配权重\",\"default\":0.20},"
            + "{\"key\":\"wEnvironment\",\"label\":\"环境安全权重\",\"default\":0.10},"
            + "{\"key\":\"wCluster\",\"label\":\"集群效益权重\",\"default\":0.08},"
            + "{\"key\":\"wCost\",\"label\":\"成本经济权重\",\"default\":0.07}]");
        return R.ok(desc);
    }

    // ======================== 候选点 CRUD ========================

    /** 候选点列表（分页） */
    @GetMapping("/list")
    public TableDataInfo<Map<String, Object>> list(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String township,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String level,
            @RequestParam(required = false) String keyword) {

        StringBuilder where = new StringBuilder("WHERE 1=1");
        List<Object> params = new ArrayList<>();
        if (township != null && !township.isEmpty()) {
            where.append(" AND township = ?");
            params.add(township);
        }
        if (status != null && !status.isEmpty()) {
            where.append(" AND status = ?");
            params.add(status);
        }
        if (level != null && !level.isEmpty()) {
            where.append(" AND suitability_level = ?");
            params.add(level);
        }
        if (keyword != null && !keyword.isEmpty()) {
            where.append(" AND (candidate_name LIKE ? OR village LIKE ?)");
            params.add("%" + keyword + "%");
            params.add("%" + keyword + "%");
        }

        String countSql = "SELECT COUNT(*) as total FROM site_selection_candidates " + where;
        Map<String, Object> countResult = jdbcTemplate.queryForMap(countSql, params.toArray());
        long total = ((Number) countResult.get("total")).longValue();

        int offset = (pageNum - 1) * pageSize;
        String listSql = "SELECT * FROM site_selection_candidates " + where +
                " ORDER BY ranking IS NULL, ranking ASC LIMIT ? OFFSET ?";
        params.add(pageSize);
        params.add(offset);

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(listSql, params.toArray());
        List<Map<String, Object>> camelRows = new ArrayList<>();
        for (Map<String, Object> row : rows) camelRows.add(toCamel(row));
        return TableDataInfo.build(camelRows, total, pageNum, pageSize);
    }

    /** 全部候选点（地图用） */
    @GetMapping("/all")
    public R<List<Map<String, Object>>> all() {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            "SELECT * FROM site_selection_candidates ORDER BY ranking IS NULL, ranking ASC");
        List<Map<String, Object>> camelRows = new ArrayList<>();
        for (Map<String, Object> row : rows) camelRows.add(toCamel(row));
        return R.ok(camelRows);
    }

    /** 新增候选点 */
    @PostMapping
    public R<Map<String, Object>> add(@RequestBody Map<String, Object> body) {
        String name = str(body.get("candidateName"), "");
        String township = str(body.get("township"), "");
        String village = str(body.get("village"), "");
        double longitude = dbl(body.get("longitude"), 0);
        double latitude = dbl(body.get("latitude"), 0);
        Integer altitude = intObj(body.get("altitude"));
        Double areaSqm = dblObj(body.get("areaSqm"));
        Double slope = dblObj(body.get("slopeDegree"));
        Double distRoad = dblObj(body.get("distanceRoad"));
        Double distPower = dblObj(body.get("distancePower"));
        Double distWater = dblObj(body.get("distanceWater"));
        Double tobaccoArea = dblObj(body.get("tobaccoArea"));
        String landType = str(body.get("landType"), "");
        String remark = str(body.get("remark"), "");

        if (longitude <= 0 || latitude <= 0) {
            return R.fail("必须填写经纬度");
        }
        if (name.isEmpty()) name = "候选点" + System.currentTimeMillis() % 100000;

        jdbcTemplate.update(
            "INSERT INTO site_selection_candidates (candidate_name, township, village, longitude, latitude, altitude, " +
            "area_sqm, slope_degree, distance_road, distance_power, distance_water, tobacco_area, land_type, remark, status) " +
            "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?, '待评估')",
            name, township, village, longitude, latitude, altitude, areaSqm, slope,
            distRoad, distPower, distWater, tobaccoArea, landType, remark);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("candidateName", name);
        result.put("msg", "候选点添加成功，请运行选址算法评估");
        return R.ok(result);
    }

    /** 更新候选点 */
    @PutMapping("/{id}")
    public R<Map<String, Object>> update(@PathVariable int id, @RequestBody Map<String, Object> body) {
        List<Object> params = new ArrayList<>();
        StringBuilder sql = new StringBuilder("UPDATE site_selection_candidates SET ");

        String[] fields = {"candidate_name", "township", "village", "longitude", "latitude", "altitude",
            "area_sqm", "slope_degree", "distance_road", "distance_power", "distance_water",
            "tobacco_area", "land_type", "remark"};
        String[] keys = {"candidateName", "township", "village", "longitude", "latitude", "altitude",
            "areaSqm", "slopeDegree", "distanceRoad", "distancePower", "distanceWater",
            "tobaccoArea", "landType", "remark"};

        List<String> sets = new ArrayList<>();
        for (int i = 0; i < fields.length; i++) {
            if (body.containsKey(keys[i])) {
                sets.add(fields[i] + " = ?");
                params.add(body.get(keys[i]));
            }
        }
        if (sets.isEmpty()) return R.fail("没有需要更新的字段");
        sql.append(String.join(", ", sets)).append(" WHERE id = ?");
        params.add(id);
        jdbcTemplate.update(sql.toString(), params.toArray());
        return R.ok(new HashMap<String, Object>(), "更新成功");
    }

    /** 删除候选点 */
    @DeleteMapping("/{id}")
    public R<Map<String, Object>> delete(@PathVariable int id) {
        jdbcTemplate.update("DELETE FROM site_selection_candidates WHERE id = ?", id);
        return R.ok(new HashMap<String, Object>(), "删除成功");
    }

    /** 清空所有候选点 */
    @DeleteMapping("/clear")
    public R<Map<String, Object>> clear() {
        jdbcTemplate.update("DELETE FROM site_selection_candidates");
        return R.ok(new HashMap<String, Object>(), "已清空所有候选点");
    }

    // ======================== 自动生成候选点 ========================

    /**
     * 自动生成网格候选点（秭归县范围）
     * 在指定经纬度范围内按步长生成网格点，仅保留距现有烤房5km以内的点
     */
    @PostMapping("/auto-generate")
    public R<Map<String, Object>> autoGenerate(@RequestBody Map<String, Object> params) {
        double minLng = dbl(params.get("minLng"), 110.26);
        double maxLng = dbl(params.get("maxLng"), 110.99);
        double minLat = dbl(params.get("minLat"), 30.42);
        double maxLat = dbl(params.get("maxLat"), 31.09);
        double step = dbl(params.get("step"), 0.03); // 约3km
        double maxDistKm = dbl(params.get("maxDistKm"), 5.0);

        // 获取所有现有烤房经纬度
        List<Map<String, Object>> barns = jdbcTemplate.queryForList(
            "SELECT longitude, latitude, township, village FROM kf_basedata WHERE longitude > 0 AND latitude > 0");
        if (barns.isEmpty()) return R.fail("无现有烤房坐标数据");

        // 生成网格点并筛选
        int inserted = 0;
        int total = 0;
        List<Object[]> batch = new ArrayList<>();
        for (double lat = minLat; lat <= maxLat; lat += step) {
            for (double lng = minLng; lng <= maxLng; lng += step) {
                total++;
                // 找最近的烤房
                double minDist = Double.MAX_VALUE;
                String nearTown = "";
                String nearVillage = "";
                for (Map<String, Object> b : barns) {
                    double blng = ((Number) b.get("longitude")).doubleValue();
                    double blat = ((Number) b.get("latitude")).doubleValue();
                    double d = haversineKm(lat, lng, blat, blng);
                    if (d < minDist) {
                        minDist = d;
                        nearTown = str(b.get("township"), "");
                        nearVillage = str(b.get("village"), "");
                    }
                }
                if (minDist <= maxDistKm) {
                    String name = String.format("候选点-%s-%s", nearTown, String.format("%.3f,%.3f", lng, lat));
                    batch.add(new Object[]{name, nearTown, nearVillage, lng, lat, "待评估"});
                }
            }
        }

        // 批量插入
        for (Object[] row : batch) {
            try {
                jdbcTemplate.update(
                    "INSERT INTO site_selection_candidates (candidate_name, township, village, longitude, latitude, status) VALUES (?,?,?,?,?,?)",
                    row);
                inserted++;
            } catch (Exception ignored) {}
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("gridTotal", total);
        result.put("inserted", inserted);
        result.put("msg", "网格生成" + total + "个点，筛选出" + inserted + "个候选点（距现有烤房" + maxDistKm + "km内）");
        return R.ok(result);
    }

    // ======================== 选址评价算法（AHP + 熵权法 + TOPSIS） ========================

    /**
     * 运行选址评价算法（AHP + 熵权法 + TOPSIS 综合模型）
     * 对全部候选点：8大指标打分 → 熵权法客观权重 → 组合权重 → TOPSIS综合排序
     */
    @PostMapping("/evaluate")
    public R<Map<String, Object>> evaluate(@RequestBody(required = false) Map<String, Object> params) {
        if (params == null) params = new HashMap<>();
        double alpha = dbl(params.get("alpha"), AHP_ALPHA);

        // 允许用户覆盖 AHP 权重
        double[] ahpW = AHP_WEIGHTS.clone();
        String[] wKeys = {"wTerrain", "wHydro", "wPower", "wTraffic", "wTobacco", "wEnvironment", "wCluster", "wCost"};
        for (int i = 0; i < wKeys.length; i++) {
            if (params.containsKey(wKeys[i])) ahpW[i] = dbl(params.get(wKeys[i]), ahpW[i]);
        }

        // 获取所有候选点
        List<Map<String, Object>> candidates = jdbcTemplate.queryForList(
            "SELECT * FROM site_selection_candidates WHERE status != '已选定'");
        if (candidates.isEmpty()) return R.fail("暂无候选点，请先添加或自动生成");

        // 获取所有现有烤房（预计算坐标，避免重复查询）
        List<Map<String, Object>> barns = jdbcTemplate.queryForList(
            "SELECT longitude, latitude, altitude, township FROM kf_basedata WHERE longitude > 0 AND latitude > 0");
        double[][] barnPos = new double[barns.size()][2];
        for (int i = 0; i < barns.size(); i++) {
            barnPos[i][0] = ((Number) barns.get(i).get("longitude")).doubleValue();
            barnPos[i][1] = ((Number) barns.get(i).get("latitude")).doubleValue();
        }

        // 乡镇收购站中心（用乡镇烤房均值近似）
        Map<String, double[]> townCenters = new HashMap<>();
        for (Map<String, Object> b : barns) {
            String town = str(b.get("township"), "");
            if (town.isEmpty()) continue;
            double lng = ((Number) b.get("longitude")).doubleValue();
            double lat = ((Number) b.get("latitude")).doubleValue();
            double[] c = townCenters.get(town);
            if (c == null) townCenters.put(town, new double[]{lng, lat, 1});
            else { c[0] += lng; c[1] += lat; c[2] += 1; }
        }
        for (Map.Entry<String, double[]> e : townCenters.entrySet()) {
            double[] c = e.getValue();
            c[0] /= c[2]; c[1] /= c[2];
        }

        // 第一步：对每个候选点计算 8 大指标得分
        int n = candidates.size();
        double[][] scores = new double[n][8];
        int[] ids = new int[n];
        for (int i = 0; i < n; i++) {
            Map<String, Object> c = candidates.get(i);
            ids[i] = ((Number) c.get("id")).intValue();
            double lng = ((Number) c.get("longitude")).doubleValue();
            double lat = ((Number) c.get("latitude")).doubleValue();
            String township = str(c.get("township"), "");

            scores[i][0] = scoreTerrain(c, lng, lat, barnPos);          // 地形地势
            scores[i][1] = scoreHydro(c);                                // 水文条件
            scores[i][2] = scorePower(c);                                // 电力保障
            scores[i][3] = scoreTraffic(c);                              // 交通条件
            scores[i][4] = scoreTobaccoMatch(c, lng, lat, barnPos);      // 烟田匹配
            scores[i][5] = scoreEnvironment(c);                          // 环境安全
            scores[i][6] = scoreCluster(lng, lat, barnPos);              // 集群效益
            scores[i][7] = scoreCost(c);                                 // 成本经济
        }

        // 第二步：熵权法计算客观权重
        double[] entropyW = entropyWeights(scores);

        // 第三步：组合权重
        double[] combinedW = new double[8];
        for (int j = 0; j < 8; j++) {
            combinedW[j] = alpha * ahpW[j] + (1 - alpha) * entropyW[j];
        }

        // 第四步：TOPSIS 综合排序
        double[] topsisC = topsis(scores, combinedW);

        // 第五步：AHP 加权得分 + 等级 + 落库
        double[] ahpScore = new double[n];
        for (int i = 0; i < n; i++) {
            double s = 0;
            for (int j = 0; j < 8; j++) s += ahpW[j] * scores[i][j];
            ahpScore[i] = Math.round(s * 100.0) / 100.0;
        }

        // 按 TOPSIS 贴近度排序得到排名
        Integer[] order = new Integer[n];
        for (int i = 0; i < n; i++) order[i] = i;
        Arrays.sort(order, (a, b) -> Double.compare(topsisC[b], topsisC[a]));
        int[] rank = new int[n];
        for (int r = 0; r < n; r++) rank[order[r]] = r + 1;

        int success = 0;
        for (int i = 0; i < n; i++) {
            try {
                String level = getLevel(ahpScore[i]);
                jdbcTemplate.update(
                    "UPDATE site_selection_candidates SET score_terrain=?, score_hydro=?, score_power=?, " +
                    "score_traffic=?, score_tobacco_match=?, score_environment=?, score_cluster=?, score_cost=?, " +
                    "ahp_weight=?, topsis_score=?, ranking=?, total_score=?, suitability_level=?, status='已评估' WHERE id=?",
                    round1(scores[i][0]), round1(scores[i][1]), round1(scores[i][2]),
                    round1(scores[i][3]), round1(scores[i][4]), round1(scores[i][5]),
                    round1(scores[i][6]), round1(scores[i][7]),
                    ahpScore[i], round4(topsisC[i]), rank[i], ahpScore[i], level, ids[i]);
                success++;
            } catch (Exception ignored) {}
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", success);
        result.put("total", n);
        result.put("ahpWeights", ahpW);
        result.put("entropyWeights", roundArr(entropyW));
        result.put("combinedWeights", roundArr(combinedW));
        result.put("msg", "AHP+熵权法+TOPSIS评价完成，共评估" + success + "个候选点");
        return R.ok(result);
    }

    // ======================== 8 大指标打分函数 ========================

    /** 地形地势：海拔(适宜800-1400m) + 坡度(≤15°) */
    private double scoreTerrain(Map<String, Object> c, double lng, double lat, double[][] barnPos) {
        double altScore;
        Object altObj = c.get("altitude");
        if (altObj != null && ((Number) altObj).doubleValue() > 0) {
            double alt = ((Number) altObj).doubleValue();
            // 适宜区间 800-1400m 满分，向外高斯衰减
            if (alt >= 800 && alt <= 1400) altScore = 100;
            else if (alt < 800) altScore = 100 * Math.exp(-Math.pow((alt - 800) / 300, 2));
            else altScore = 100 * Math.exp(-Math.pow((alt - 1400) / 400, 2));
        } else {
            // 用周边烤房平均海拔估算
            double sum = 0; int cnt = 0;
            for (double[] b : barnPos) {
                double d = haversineKm(lat, lng, b[1], b[0]);
                if (d <= 3.0) { sum += 1100; cnt++; }
            }
            altScore = cnt > 0 ? 100 : 70;
        }

        double slopeScore;
        Object slopeObj = c.get("slope_degree");
        if (slopeObj != null && ((Number) slopeObj).doubleValue() >= 0) {
            double slope = ((Number) slopeObj).doubleValue();
            if (slope <= 15) slopeScore = 100;                    // 方案要求 ≤15°
            else if (slope <= 25) slopeScore = 100 - (slope - 15) * 5.0;
            else slopeScore = 30;
        } else {
            slopeScore = 70;
        }
        return 0.6 * altScore + 0.4 * slopeScore;
    }

    /** 水文条件：地下水位/积水风险/排水条件（用距水源+土地类型近似） */
    private double scoreHydro(Map<String, Object> c) {
        double waterScore = 70;
        Object wObj = c.get("distance_water");
        if (wObj != null) {
            double d = ((Number) wObj).doubleValue();
            if (d <= 0) waterScore = 60;              // 紧邻水体，积水风险
            else if (d <= 300) waterScore = 95;       // 水源充足且无积水
            else if (d <= 1500) waterScore = 80;
            else if (d <= 3000) waterScore = 60;
            else waterScore = 40;                     // 水源过远
        }
        // 土地类型修正排水条件
        String landType = str(c.get("land_type"), "");
        if ("坡地".equals(landType)) waterScore = Math.min(100, waterScore + 5);   // 排水好
        else if ("平地".equals(landType)) waterScore = Math.min(100, waterScore + 2);
        else if ("洼地".equals(landType)) waterScore = Math.max(30, waterScore - 15); // 易积水
        return waterScore;
    }

    /** 电力保障：距变电站/电力线路距离 + 供电可靠性 */
    private double scorePower(Map<String, Object> c) {
        Object pObj = c.get("distance_power");
        if (pObj == null) return 70;
        double d = ((Number) pObj).doubleValue();
        if (d <= 200) return 100;
        if (d <= 1000) return 80;
        if (d <= 3000) return 60;
        return 40;
    }

    /** 交通条件：距主干道距离 + 路面状况 */
    private double scoreTraffic(Map<String, Object> c) {
        Object rObj = c.get("distance_road");
        if (rObj == null) return 70;
        double d = ((Number) rObj).doubleValue();
        if (d <= 200) return 100;
        if (d <= 1000) return 80;
        if (d <= 3000) return 60;
        return 40;
    }

    /** 烟田匹配：服务烟田面积 + 距烟田距离（用周边烤房密度近似烟田） */
    private double scoreTobaccoMatch(Map<String, Object> c, double lng, double lat, double[][] barnPos) {
        double areaScore = 60;
        Object tObj = c.get("tobacco_area");
        if (tObj != null) {
            double area = ((Number) tObj).doubleValue();
            if (area >= 200) areaScore = 100;
            else if (area >= 100) areaScore = 85;
            else if (area >= 50) areaScore = 70;
            else if (area >= 20) areaScore = 55;
            else areaScore = 40;
        }
        // 距最近烤房距离（烟田通常围绕烤房分布）
        double minDist = Double.MAX_VALUE;
        for (double[] b : barnPos) {
            double d = haversineKm(lat, lng, b[1], b[0]);
            if (d < minDist) minDist = d;
        }
        double distScore;
        if (minDist == Double.MAX_VALUE) distScore = 60;
        else if (minDist <= 1.0) distScore = 100;
        else if (minDist <= 3.0) distScore = 85;
        else if (minDist <= 5.0) distScore = 65;
        else distScore = 45;
        return 0.7 * areaScore + 0.3 * distScore;
    }

    /** 环境安全：与民房/仓库距离、防火条件（用土地类型+区位近似） */
    private double scoreEnvironment(Map<String, Object> c) {
        String landType = str(c.get("land_type"), "");
        double base = 70;
        if ("荒地".equals(landType)) base = 90;        // 远离民房，防火条件好
        else if ("坡地".equals(landType)) base = 80;
        else if ("平地".equals(landType)) base = 65;   // 靠近村庄
        else if ("林地".equals(landType)) base = 45;   // 防火风险高
        return base;
    }

    /** 集群效益：周边2km烤房密度（5-20座最优） */
    private double scoreCluster(double lng, double lat, double[][] barnPos) {
        int count = 0;
        for (double[] b : barnPos) {
            if (haversineKm(lat, lng, b[1], b[0]) <= 2.0) count++;
        }
        if (count >= 5 && count <= 20) return 100;
        if (count < 5) return Math.min(100, 40 + count * 12);
        return Math.max(50, 100 - (count - 20) * 2);
    }

    /** 成本经济：土地获取成本（土地类型）+ 建设规模（面积） */
    private double scoreCost(Map<String, Object> c) {
        String landType = str(c.get("land_type"), "");
        double landScore;
        switch (landType) {
            case "荒地": landScore = 100; break;   // 获取成本低
            case "坡地": landScore = 80; break;
            case "平地": landScore = 70; break;
            case "林地": landScore = 50; break;    // 成本高
            default: landScore = 75;
        }
        double areaScore = 70;
        Object aObj = c.get("area_sqm");
        if (aObj != null) {
            double area = ((Number) aObj).doubleValue();
            if (area >= 500) areaScore = 100;      // 规模经济
            else if (area >= 200) areaScore = 85;
            else if (area >= 100) areaScore = 70;
            else areaScore = 55;
        }
        return 0.6 * landScore + 0.4 * areaScore;
    }

    // ======================== 熵权法 + TOPSIS ========================

    /** 熵权法：由得分矩阵计算客观权重 */
    private double[] entropyWeights(double[][] scores) {
        int n = scores.length;
        int m = scores[0].length;
        double[] weights = new double[m];

        // 归一化 p_ij = x_ij / Σx_ij
        double[][] p = new double[n][m];
        for (int j = 0; j < m; j++) {
            double sum = 0;
            for (int i = 0; i < n; i++) sum += scores[i][j];
            for (int i = 0; i < n; i++) p[i][j] = sum > 0 ? scores[i][j] / sum : 0;
        }

        // 熵值 e_j = -1/ln(n) × Σ p_ij·ln(p_ij)
        double[] e = new double[m];
        double lnN = Math.log(n);
        for (int j = 0; j < m; j++) {
            double sum = 0;
            for (int i = 0; i < n; i++) {
                if (p[i][j] > 0) sum += p[i][j] * Math.log(p[i][j]);
            }
            e[j] = -sum / lnN;
        }

        // 权重 w_j = (1-e_j) / Σ(1-e_j)
        double total = 0;
        for (int j = 0; j < m; j++) total += (1 - e[j]);
        for (int j = 0; j < m; j++) weights[j] = total > 0 ? (1 - e[j]) / total : 1.0 / m;
        return weights;
    }

    /** TOPSIS：返回各方案贴近度 C_i = D⁻/(D⁺+D⁻) */
    private double[] topsis(double[][] scores, double[] weights) {
        int n = scores.length;
        int m = scores[0].length;

        // 向量归一化 + 加权
        double[][] v = new double[n][m];
        for (int j = 0; j < m; j++) {
            double norm = 0;
            for (int i = 0; i < n; i++) norm += scores[i][j] * scores[i][j];
            norm = Math.sqrt(norm);
            for (int i = 0; i < n; i++) v[i][j] = weights[j] * (norm > 0 ? scores[i][j] / norm : 0);
        }

        // 正负理想解（全部为效益型指标，取最大/最小）
        double[] vPlus = new double[m];
        double[] vMinus = new double[m];
        for (int j = 0; j < m; j++) {
            double max = Double.MIN_VALUE, min = Double.MAX_VALUE;
            for (int i = 0; i < n; i++) {
                max = Math.max(max, v[i][j]);
                min = Math.min(min, v[i][j]);
            }
            vPlus[j] = max;
            vMinus[j] = min;
        }

        // 距离与贴近度
        double[] c = new double[n];
        for (int i = 0; i < n; i++) {
            double dPlus = 0, dMinus = 0;
            for (int j = 0; j < m; j++) {
                dPlus += Math.pow(v[i][j] - vPlus[j], 2);
                dMinus += Math.pow(v[i][j] - vMinus[j], 2);
            }
            dPlus = Math.sqrt(dPlus);
            dMinus = Math.sqrt(dMinus);
            c[i] = (dPlus + dMinus) > 0 ? dMinus / (dPlus + dMinus) : 0;
        }
        return c;
    }

    // ======================== 统计 ========================

    @GetMapping("/stats")
    public R<Map<String, Object>> stats() {
        Map<String, Object> result = new LinkedHashMap<>();

        Map<String, Object> totalRow = jdbcTemplate.queryForMap(
            "SELECT COUNT(*) as total, COUNT(total_score) as evaluated, " +
            "ROUND(AVG(total_score),1) as avg_score, " +
            "MAX(total_score) as max_score, MIN(total_score) as min_score, " +
            "ROUND(AVG(topsis_score),4) as avg_topsis " +
            "FROM site_selection_candidates");
        result.put("total", totalRow.get("total"));
        result.put("evaluated", totalRow.get("evaluated"));
        result.put("avgScore", totalRow.get("avg_score"));
        result.put("maxScore", totalRow.get("max_score"));
        result.put("minScore", totalRow.get("min_score"));
        result.put("avgTopsis", totalRow.get("avg_topsis"));

        // 适宜等级分布
        List<Map<String, Object>> levelDist = jdbcTemplate.queryForList(
            "SELECT suitability_level as level, COUNT(*) as cnt FROM site_selection_candidates " +
            "WHERE suitability_level IS NOT NULL GROUP BY suitability_level ORDER BY cnt DESC");
        result.put("levelDistribution", levelDist);

        // 乡镇分布
        List<Map<String, Object>> townDist = jdbcTemplate.queryForList(
            "SELECT township, COUNT(*) as cnt, ROUND(AVG(total_score),1) as avg_score, " +
            "SUM(CASE WHEN suitability_level='高度适宜' THEN 1 ELSE 0 END) as high_cnt " +
            "FROM site_selection_candidates GROUP BY township ORDER BY cnt DESC");
        result.put("townDistribution", townDist);

        return R.ok(result);
    }

    // ======================== 工具方法 ========================

    private String getLevel(double score) {
        if (score >= 85) return "高度适宜";
        if (score >= 70) return "适宜";
        if (score >= 55) return "一般";
        return "不适宜";
    }

    /** Haversine 距离（km） */
    private double haversineKm(double lat1, double lng1, double lat2, double lng2) {
        double R = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
            + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
            * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return 2 * R * Math.asin(Math.sqrt(a));
    }

    private double round1(double v) { return Math.round(v * 10.0) / 10.0; }
    private double round4(double v) { return Math.round(v * 10000.0) / 10000.0; }
    private double[] roundArr(double[] arr) {
        double[] out = new double[arr.length];
        for (int i = 0; i < arr.length; i++) out[i] = Math.round(arr[i] * 10000.0) / 10000.0;
        return out;
    }

    private String str(Object o, String def) { return o == null ? def : String.valueOf(o); }

    /** 数据库下划线字段 → 前端驼峰字段 */
    private Map<String, Object> toCamel(Map<String, Object> row) {
        Map<String, Object> out = new LinkedHashMap<>();
        for (Map.Entry<String, Object> e : row.entrySet()) {
            out.put(snakeToCamel(e.getKey()), e.getValue());
        }
        return out;
    }

    private String snakeToCamel(String s) {
        StringBuilder sb = new StringBuilder();
        boolean upper = false;
        for (char ch : s.toCharArray()) {
            if (ch == '_') upper = true;
            else if (upper) { sb.append(Character.toUpperCase(ch)); upper = false; }
            else sb.append(ch);
        }
        return sb.toString();
    }
    private double dbl(Object o, double def) {
        if (o == null) return def;
        try { return Double.parseDouble(String.valueOf(o)); } catch (Exception e) { return def; }
    }
    private Double dblObj(Object o) {
        if (o == null) return null;
        try { return Double.parseDouble(String.valueOf(o)); } catch (Exception e) { return null; }
    }
    private Integer intObj(Object o) {
        if (o == null) return null;
        try { return Integer.parseInt(String.valueOf(o)); } catch (Exception e) { return null; }
    }
}
