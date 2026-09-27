package com.barn.barn.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.barn.barn.entity.BarnComponent;
import com.barn.barn.entity.BarnProject;
import com.barn.barn.entity.RepairRecord;
import com.barn.barn.entity.ReservationRecord;
import com.barn.barn.mapper.BarnComponentMapper;
import com.barn.barn.mapper.BarnProjectMapper;
import com.barn.barn.mapper.RepairRecordMapper;
import com.barn.barn.mapper.ReservationRecordMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.*;
import java.util.stream.Collectors;

/**
 * AI算法Service - 实现健康评分、预测、推荐、资金分配等智能算法
 */
@Service
public class AIService {

    @Autowired
    private BarnProjectMapper barnProjectMapper;

    @Autowired
    private BarnComponentMapper barnComponentMapper;

    @Autowired
    private RepairRecordMapper repairRecordMapper;

    @Autowired
    private ReservationRecordMapper reservationRecordMapper;

    // ==================== 算法1：自动健康评分 ====================

    /**
     * 自动健康评分
     * 优先使用部件加权评分，无部件数据时使用多因素模型
     *
     * @param ovenId 烤房ID
     * @return 评分详情（各因子得分、权重、最终分数和等级）
     */
    public Map<String, Object> calculateHealthScore(String ovenId) {
        Map<String, Object> result = new HashMap<>();
        result.put("ovenId", ovenId);

        // 1. 查询烤房信息
        BarnProject barn = barnProjectMapper.selectById(ovenId);
        if (barn == null) {
            result.put("error", "烤房不存在");
            return result;
        }

        // 2. 查询部件列表
        List<BarnComponent> components = barnComponentMapper.selectList(
                new LambdaQueryWrapper<BarnComponent>().eq(BarnComponent::getOvenId, ovenId)
        );

        double healthScore;

        if (components != null && !components.isEmpty()) {
            // ====== 有部件数据：部件评分加权求和 ======
            double totalWeightedScore = 0;
            double totalWeight = 0;
            List<Map<String, Object>> componentDetails = new ArrayList<>();

            for (BarnComponent comp : components) {
                double score = comp.getScore() != null ? comp.getScore().doubleValue() : 0;
                double weight = comp.getWeight() != null ? comp.getWeight().doubleValue() : 1;
                totalWeightedScore += score * weight;
                totalWeight += weight;

                Map<String, Object> detail = new HashMap<>();
                detail.put("componentName", comp.getComponentName());
                detail.put("componentType", comp.getComponentKey());
                detail.put("score", score);
                detail.put("weight", weight);
                componentDetails.add(detail);
            }

            healthScore = totalWeight > 0 ? totalWeightedScore / totalWeight : 0;
            result.put("method", "部件加权评分");
            result.put("componentDetails", componentDetails);
            result.put("totalWeightedScore", round2(totalWeightedScore));
            result.put("totalWeight", round2(totalWeight));

        } else {
            // ====== 无部件数据：多因素模型 ======
            // a. 使用年限因子
            int currentYear = Year.now().getValue();
            int buildYear = barn.getCompleteDate() != null ? barn.getCompleteDate().getYear() : currentYear;
            int age = currentYear - buildYear;
            double score1 = Math.max(0, 100 - age * 3);

            // b. 使用状态因子
            double score2;
            String useStatus = barn.getUseStatus();
            if ("在用".equals(useStatus)) {
                score2 = 100;
            } else if ("闲置".equals(useStatus)) {
                int idleYears = barn.getIdleYears() != null ? barn.getIdleYears() : 0;
                score2 = Math.max(0, 100 - idleYears * 10);
            } else if ("转用".equals(useStatus)) {
                score2 = 60;
            } else if ("损毁".equals(useStatus)) {
                score2 = 20;
            } else {
                score2 = 50; // 默认
            }

            // c. 设备完整度：5项各20分
            double score3 = 0;
            score3 += (barn.getHasHeating() != null && barn.getHasHeating() == 1) ? 20 : 0;
            score3 += (barn.getHasRadiator() != null && barn.getHasRadiator() == 1) ? 20 : 0;
            score3 += (barn.getHasAutocontrol() != null && barn.getHasAutocontrol() == 1) ? 20 : 0;
            score3 += (barn.getHasMain() != null && barn.getHasMain() == 1) ? 20 : 0;
            score3 += (barn.getHasAncillary() != null && barn.getHasAncillary() == 1) ? 20 : 0;

            // d. 综合评分 = score1*0.3 + score2*0.3 + score3*0.4
            healthScore = score1 * 0.3 + score2 * 0.3 + score3 * 0.4;

            result.put("method", "多因素模型");
            result.put("ageFactor", round2(score1));
            result.put("ageFactorWeight", 0.3);
            result.put("age", age);
            result.put("statusFactor", round2(score2));
            result.put("statusFactorWeight", 0.3);
            result.put("useStatus", useStatus);
            result.put("equipmentFactor", round2(score3));
            result.put("equipmentFactorWeight", 0.4);
            
            // 添加 factors 数组供前端雷达图使用
            List<Map<String, Object>> factors = new ArrayList<>();
            Map<String, Object> f1 = new HashMap<>();
            f1.put("name", "使用年限");
            f1.put("score", round2(score1));
            f1.put("weight", 0.3);
            f1.put("weightedScore", round2(score1 * 0.3));
            factors.add(f1);
            
            Map<String, Object> f2 = new HashMap<>();
            f2.put("name", "使用状态");
            f2.put("score", round2(score2));
            f2.put("weight", 0.3);
            f2.put("weightedScore", round2(score2 * 0.3));
            factors.add(f2);
            
            Map<String, Object> f3 = new HashMap<>();
            f3.put("name", "设备完整度");
            f3.put("score", round2(score3));
            f3.put("weight", 0.4);
            f3.put("weightedScore", round2(score3 * 0.4));
            factors.add(f3);
            
            result.put("factors", factors);
        }

        healthScore = round2(healthScore);

        // 3. 确定等级
        String healthLevel;
        if (healthScore >= 85) {
            healthLevel = "优良";
        } else if (healthScore >= 60) {
            healthLevel = "需维护";
        } else if (healthScore >= 40) {
            healthLevel = "急需修复";
        } else {
            healthLevel = "退出";
        }

        // 4. 更新烤房的评分和等级
        barn.setHealthScore((int) healthScore);
        barn.setHealthLevel(healthLevel);
        barn.setUpdatedAt(LocalDateTime.now());
        barnProjectMapper.updateById(barn);

        // 5. 返回评分详情
        result.put("healthScore", healthScore);
        result.put("healthLevel", healthLevel);
        return result;
    }

    // ==================== 算法2：健康状态预测 ====================

    /**
     * 健康状态预测
     * 基于历史数据预测退化速率、剩余寿命、最优维修时间等
     *
     * @param ovenId 烤房ID
     * @return 预测结果（退化速率、剩余寿命、最优维修时间、预计失灵时间）
     */
    public Map<String, Object> predictHealth(String ovenId) {
        Map<String, Object> result = new HashMap<>();
        result.put("ovenId", ovenId);

        // 1. 查询烤房信息
        BarnProject barn = barnProjectMapper.selectById(ovenId);
        if (barn == null) {
            result.put("error", "烤房不存在");
            return result;
        }

        // 查询维修记录
        List<RepairRecord> repairRecords = repairRecordMapper.selectList(
                new LambdaQueryWrapper<RepairRecord>().eq(RepairRecord::getOvenId, ovenId)
        );

        // 当前健康评分
        double currentHealthScore = barn.getHealthScore() != null ? barn.getHealthScore().doubleValue() : 50;

        // 计算使用年限
        int currentYear = Year.now().getValue();
        int buildYear = barn.getCompleteDate() != null ? barn.getCompleteDate().getYear() : currentYear;
        int useYears = Math.max(1, currentYear - buildYear); // 至少1年

        // 2. 计算退化速率
        double degradationRate;
        if (repairRecords != null && !repairRecords.isEmpty()) {
            // 有维修记录：按使用年限计算年均退化速率
            degradationRate = (100.0 - currentHealthScore) / useYears;
        } else {
            // 无维修记录：使用默认速率 3分/年
            degradationRate = 3.0;
        }
        degradationRate = Math.max(0.1, degradationRate); // 避免除零

        // 3. 预测剩余寿命（到急需修复阈值40分）
        double remainingLife = Math.max(0, (currentHealthScore - 40) / degradationRate);

        // 4. 预测最优维修时间点（评分降到70分时）
        double optimalRepairTime = Math.max(0, (currentHealthScore - 70) / degradationRate);

        // 5. 预测失灵时间（评分降到40分时）
        double failureTime = Math.max(0, (currentHealthScore - 40) / degradationRate);

        // 构建返回结果
        result.put("currentHealthScore", currentHealthScore);
        result.put("healthLevel", barn.getHealthLevel());
        result.put("useYears", useYears);
        result.put("degradationRate", round2(degradationRate));
        result.put("degradationRateUnit", "分/年");
        result.put("remainingLife", round2(remainingLife));
        result.put("remainingLifeUnit", "年");
        result.put("optimalRepairTime", round2(optimalRepairTime));
        result.put("optimalRepairTimeUnit", "年后");
        result.put("failureTime", round2(failureTime));
        result.put("failureTimeUnit", "年后");
        result.put("repairRecordCount", repairRecords != null ? repairRecords.size() : 0);

        // 建议
        List<String> suggestions = new ArrayList<>();
        if (optimalRepairTime <= 0.5) {
            suggestions.add("建议立即安排维修，已错过最优维修时间点");
        } else if (optimalRepairTime <= 1) {
            suggestions.add("建议在一年内安排维修");
        } else if (optimalRepairTime <= 3) {
            suggestions.add("建议关注烤房状态，适时安排预防性维修");
        } else {
            suggestions.add("烤房状态良好，暂无需维修");
        }
        if (remainingLife <= 2) {
            suggestions.add("烤房剩余寿命较短，建议考虑更新换代");
        }
        result.put("suggestions", suggestions);

        return result;
    }

    // ==================== 算法3：智能烤房推荐 ====================

    /**
     * 智能烤房推荐
     * 根据用户需求（县区、预约日期、烟叶重量）推荐最匹配的烤房
     *
     * @param params 推荐请求参数（userId, countyCode, reserveDate, leafWeight）
     * @return 推荐结果列表（前10个推荐烤房）
     */
    public Map<String, Object> recommendBarns(Map<String, Object> params) {
        Map<String, Object> result = new HashMap<>();

        // 解析参数
        String countyCode = params.get("countyCode") != null ? params.get("countyCode").toString() : null;
        LocalDate reserveDate = null;
        if (params.get("reserveDate") != null) {
            reserveDate = LocalDate.parse(params.get("reserveDate").toString());
        }
        double leafWeight = params.get("leafWeight") != null ? Double.parseDouble(params.get("leafWeight").toString()) : 0;

        // 1. 查询所有"在用"状态的烤房
        LambdaQueryWrapper<BarnProject> wrapper = new LambdaQueryWrapper<BarnProject>()
                .eq(BarnProject::getUseStatus, "在用");
        if (countyCode != null && !countyCode.isEmpty()) {
            wrapper.eq(BarnProject::getCountyCode, countyCode);
        }
        List<BarnProject> barns = barnProjectMapper.selectList(wrapper);

        // 2. 计算每个烤房的最大体积（用于容量归一化）
        double maxVolume = 1; // 避免除零
        for (BarnProject b : barns) {
            double vol = calcVolume(b);
            if (vol > maxVolume) maxVolume = vol;
        }

        // 3. 查询预约日期的预约记录（批量查询）
        Set<String> reservedOvenIds = new HashSet<>();
        if (reserveDate != null) {
            List<ReservationRecord> reservations = reservationRecordMapper.selectList(
                    new LambdaQueryWrapper<ReservationRecord>()
                            .like(ReservationRecord::getPlanStartTime, reserveDate.toString())
            );
            for (ReservationRecord r : reservations) {
                reservedOvenIds.add(r.getOvenId());
            }
        }

        // 4. 查询近一年有维修记录的烤房ID集合
        LocalDateTime oneYearAgo = LocalDateTime.now().minusYears(1);
        List<RepairRecord> recentRepairs = repairRecordMapper.selectList(
                new LambdaQueryWrapper<RepairRecord>()
                        .ge(RepairRecord::getCreatedAt, oneYearAgo)
        );
        Set<String> repairedOvenIds = recentRepairs.stream()
                .map(RepairRecord::getOvenId)
                .collect(Collectors.toSet());

        // 5. 对每个候选烤房计算匹配度评分
        List<Map<String, Object>> recommendations = new ArrayList<>();
        for (BarnProject barn : barns) {
            // 过滤条件：健康评分 >= 60
            double healthScore = barn.getHealthScore() != null ? barn.getHealthScore().doubleValue() : 0;
            if (healthScore < 60) continue;

            // a. 健康分因子(30%)
            double healthFactor = healthScore / 100.0 * 30;

            // b. 空闲度因子(25%)：无预约=25分，有预约=10分
            double idleFactor = reservedOvenIds.contains(barn.getId()) ? 10 : 25;

            // c. 容量因子(20%)：根据体积归一化评分
            double volume = calcVolume(barn);
            double capacityFactor = (volume / maxVolume) * 20;

            // d. 设备完整度因子(15%)：3项设备
            int equipCount = 0;
            if (barn.getHasHeating() != null && barn.getHasHeating() == 1) equipCount++;
            if (barn.getHasRadiator() != null && barn.getHasRadiator() == 1) equipCount++;
            if (barn.getHasAutocontrol() != null && barn.getHasAutocontrol() == 1) equipCount++;
            double equipmentFactor = (equipCount / 3.0) * 15;

            // e. 维修历史因子(10%)：近一年无维修=10分，有维修=5分
            double repairFactor = repairedOvenIds.contains(barn.getId()) ? 5 : 10;

            // 总匹配度
            double totalScore = healthFactor + idleFactor + capacityFactor + equipmentFactor + repairFactor;

            // 生成推荐理由
            List<String> reasons = new ArrayList<>();
            if (healthScore >= 85) reasons.add("烤房状态优良");
            else reasons.add("烤房状态良好");
            if (!reservedOvenIds.contains(barn.getId())) reasons.add("预约日期空闲");
            if (equipCount == 3) reasons.add("设备配置完整");
            if (volume >= maxVolume * 0.8) reasons.add("容量充足");
            if (!repairedOvenIds.contains(barn.getId())) reasons.add("近一年无维修记录");

            Map<String, Object> rec = new HashMap<>();
            rec.put("ovenId", barn.getId());
            rec.put("id", barn.getId());
            rec.put("barnName", barn.getBarnName());
            rec.put("address", barn.getAddress());
            rec.put("countyName", barn.getCounty());
            rec.put("townName", barn.getTownship());
            rec.put("healthScore", healthScore);
            rec.put("matchScore", round2(totalScore));
            rec.put("healthFactor", round2(healthFactor));
            rec.put("idleFactor", round2(idleFactor));
            rec.put("capacityFactor", round2(capacityFactor));
            rec.put("equipmentFactor", round2(equipmentFactor));
            rec.put("repairFactor", round2(repairFactor));
            rec.put("reasons", reasons);
            rec.put("volume", round2(volume));
            recommendations.add(rec);
        }

        // 6. 按匹配度降序排列，取前10个
        recommendations.sort((a, b) -> Double.compare(
                (double) b.get("matchScore"), (double) a.get("matchScore")));
        if (recommendations.size() > 10) {
            recommendations = recommendations.subList(0, 10);
        }

        result.put("totalCandidates", barns.size());
        result.put("recommendCount", recommendations.size());
        result.put("recommendations", recommendations);
        return result;
    }

    // ==================== 算法4：维修资金分配 ====================

    /**
     * 维修资金分配
     * 根据维修优先级智能分配预算资金
     *
     * @param params 分配请求参数（budget: 总预算, countyCode: 可选县区筛选）
     * @return 分配方案（总预算、分配列表、预算利用率）
     */
    public Map<String, Object> allocateFund(Map<String, Object> params) {
        Map<String, Object> result = new HashMap<>();

        // 解析参数
        double budget = Double.parseDouble(params.get("budget").toString());
        String countyCode = params.get("countyCode") != null ? params.get("countyCode").toString() : null;

        // 1. 查询所有需要维修的烤房（健康评分<85 或 有待处理维修记录）
        LambdaQueryWrapper<BarnProject> wrapper = new LambdaQueryWrapper<BarnProject>()
                .lt(BarnProject::getHealthScore, 85)
                .or(w -> w.isNull(BarnProject::getHealthScore));
        if (countyCode != null && !countyCode.isEmpty()) {
            wrapper.and(w -> w.eq(BarnProject::getCountyCode, countyCode));
        }
        List<BarnProject> barns = barnProjectMapper.selectList(wrapper);

        // 同时查询有待处理维修记录的烤房
        List<RepairRecord> pendingRepairs = repairRecordMapper.selectList(
                new LambdaQueryWrapper<RepairRecord>()
                        .ne(RepairRecord::getRepairStatus, "已完成")
        );
        Set<String> pendingOvenIds = pendingRepairs.stream()
                .map(RepairRecord::getOvenId)
                .collect(Collectors.toSet());

        // 合并去重
        Set<String> allOvenIds = barns.stream().map(BarnProject::getId).collect(Collectors.toSet());
        allOvenIds.addAll(pendingOvenIds);

        // 重新查询完整的烤房列表
        if (allOvenIds.isEmpty()) {
            result.put("totalBudget", budget);
            result.put("allocations", new ArrayList<>());
            result.put("budgetUtilization", 0);
            result.put("message", "没有需要维修的烤房");
            return result;
        }

        List<BarnProject> allBarns = barnProjectMapper.selectBatchIds(allOvenIds);

        // 查询每个烤房的维修记录（用于ROI计算）
        List<RepairRecord> allRepairs = repairRecordMapper.selectList(
                new LambdaQueryWrapper<RepairRecord>().in(RepairRecord::getOvenId, allOvenIds)
        );
        // 按ovenId分组
        Map<String, List<RepairRecord>> repairsByBarn = allRepairs.stream()
                .collect(Collectors.groupingBy(RepairRecord::getOvenId));

        // 2. 为每个烤房计算维修优先级评分
        List<Map<String, Object>> allocationList = new ArrayList<>();
        double totalPriorityScore = 0;

        for (BarnProject barn : allBarns) {
            double healthScore = barn.getHealthScore() != null ? barn.getHealthScore().doubleValue() : 50;
            List<RepairRecord> barnRepairs = repairsByBarn.getOrDefault(barn.getId(), new ArrayList<>());

            // a. 紧迫性(35%)
            double urgencyScore;
            // 检查是否有紧急标签的待处理维修记录
            boolean hasUrgentRepair = barnRepairs.stream()
                    .anyMatch(r -> "紧急建议".equals(r.getUrgency()) && !"已完成".equals(r.getRepairStatus()));
            boolean hasDeferredRepair = barnRepairs.stream()
                    .anyMatch(r -> "暂缓".equals(r.getUrgency()) && !"已完成".equals(r.getRepairStatus()));

            if (hasUrgentRepair) {
                urgencyScore = 100;
            } else if (hasDeferredRepair) {
                urgencyScore = 50;
            } else {
                // 基于healthScore反推：越低越紧急
                urgencyScore = Math.max(0, (100 - healthScore));
            }
            double urgencyFactor = urgencyScore / 100.0 * 35;

            // b. 健康分影响(25%)：越低越紧急
            double healthFactor = (100 - healthScore) / 100.0 * 25;

            // c. ROI预期(20%)：基于历史维修记录的roi_score平均值
            double roiScore = 10; // 默认值
            List<RepairRecord> completedRepairs = barnRepairs.stream()
                    .filter(r -> r.getRoiScore() != null)
                    .collect(Collectors.toList());
            if (!completedRepairs.isEmpty()) {
                double avgRoi = completedRepairs.stream()
                        .mapToDouble(r -> r.getRoiScore().doubleValue())
                        .average()
                        .orElse(10);
                roiScore = Math.min(20, avgRoi); // 上限20分
            }
            double roiFactor = roiScore;

            // d. 使用价值(20%)
            double usageFactor;
            String useStatus = barn.getUseStatus();
            if ("在用".equals(useStatus)) {
                usageFactor = 20;
            } else if ("闲置".equals(useStatus)) {
                usageFactor = 10;
            } else {
                usageFactor = 5;
            }

            // 总优先级评分
            double priorityScore = urgencyFactor + healthFactor + roiFactor + usageFactor;
            totalPriorityScore += priorityScore;

            // 获取预估维修成本
            double estimatedCost = 5000; // 默认最低维修成本
            for (RepairRecord r : barnRepairs) {
                if (r.getEstimatedCost() != null && !"已完成".equals(r.getRepairStatus())) {
                    estimatedCost = Math.max(estimatedCost, r.getEstimatedCost().doubleValue());
                }
            }

            Map<String, Object> item = new HashMap<>();
            item.put("ovenId", barn.getId());
            item.put("id", barn.getId());
            item.put("barnName", barn.getBarnName());
            item.put("countyName", barn.getCounty());
            item.put("healthScore", healthScore);
            item.put("priorityScore", round2(priorityScore));
            item.put("urgencyFactor", round2(urgencyFactor));
            item.put("healthFactor", round2(healthFactor));
            item.put("roiFactor", round2(roiFactor));
            item.put("usageFactor", round2(usageFactor));
            item.put("estimatedCost", estimatedCost);
            item.put("useStatus", useStatus);
            allocationList.add(item);
        }

        // 3. 按优先级排序
        allocationList.sort((a, b) -> Double.compare(
                (double) b.get("priorityScore"), (double) a.get("priorityScore")));

        // 4. 资金分配
        double totalAllocated = 0;
        int fundedCount = 0;
        int insufficientCount = 0;
        double totalRoi = 0;

        for (Map<String, Object> item : allocationList) {
            double priorityScore = (double) item.get("priorityScore");
            double estimatedCost = (double) item.get("estimatedCost");

            // 计算需求权重
            double needWeight = totalPriorityScore > 0 ? priorityScore / totalPriorityScore : 0;
            // 初始分配
            double initialAlloc = budget * needWeight;

            if (initialAlloc < estimatedCost) {
                // 预算不足
                item.put("allocatedAmount", 0);
                item.put("status", "预算不足");
                item.put("shortfall", round2(estimatedCost - initialAlloc));
                insufficientCount++;
            } else {
                // 分配实际金额
                item.put("allocatedAmount", round2(estimatedCost));
                item.put("status", "已分配");
                item.put("shortfall", 0);
                totalAllocated += estimatedCost;
                fundedCount++;
            }

            // ROI贡献
            double roiFactor = (double) item.get("roiFactor");
            totalRoi += roiFactor;
        }

        // 5. 计算整体ROI预期
        double overallRoi = fundedCount > 0 ? totalRoi / fundedCount : 0;
        double budgetUtilization = budget > 0 ? (totalAllocated / budget * 100) : 0;

        // 6. 返回结果
        result.put("totalBudget", budget);
        result.put("totalAllocated", round2(totalAllocated));
        result.put("budgetUtilization", round2(budgetUtilization) + "%");
        result.put("fundedCount", fundedCount);
        result.put("insufficientCount", insufficientCount);
        result.put("overallRoi", round2(overallRoi));
        result.put("allocations", allocationList);

        return result;
    }

    // ==================== 工具方法 ====================

    /**
     * 计算烤房体积（长×宽×高）
     */
    private double calcVolume(BarnProject barn) {
        double l = barn.getLengthM() != null ? barn.getLengthM().doubleValue() : 0;
        double w = barn.getWidthM() != null ? barn.getWidthM().doubleValue() : 0;
        double h = barn.getHeightM() != null ? barn.getHeightM().doubleValue() : 0;
        return l * w * h;
    }

    /**
     * 保留两位小数
     */
    private double round2(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}
