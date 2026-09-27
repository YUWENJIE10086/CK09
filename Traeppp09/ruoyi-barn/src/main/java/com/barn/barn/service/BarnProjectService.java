package com.barn.barn.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.barn.barn.entity.BarnProject;
import com.barn.barn.mapper.BarnProjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 烤房项目Service
 */
@Service
public class BarnProjectService {

    @Autowired
    private BarnProjectMapper barnProjectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * 分页查询
     * @param useStatus 使用状态(英文值: baking/idle/reserved/damaged)，对应ovens表的status字段
     * @param healthLevel 健康等级(英文值: excellent/maintenance/urgent/retired)，根据health_score计算
     */
    public IPage<BarnProject> pageList(int pageNum, int pageSize, String countyCode,
                                         String townCode, String useStatus, String healthLevel,
                                         String projectCode, String barnName) {
        Page<BarnProject> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<BarnProject> wrapper = new LambdaQueryWrapper<>();
        
        if (countyCode != null && !countyCode.isEmpty()) {
            wrapper.eq(BarnProject::getCountyCode, countyCode);
        }
        if (townCode != null && !townCode.isEmpty()) {
            wrapper.eq(BarnProject::getTownCode, townCode);
        }
        // 使用状态筛选 - 使用status字段（存储英文值: baking/idle/reserved/damaged）
        if (useStatus != null && !useStatus.isEmpty()) {
            wrapper.eq(BarnProject::getStatus, useStatus);
        }
        // 健康等级筛选 - 根据health_score范围筛选
        if (healthLevel != null && !healthLevel.isEmpty()) {
            switch (healthLevel) {
                case "excellent":   // 优良: >= 90
                    wrapper.ge(BarnProject::getHealthScore, 90);
                    break;
                case "maintenance": // 需维护: 75-89
                    wrapper.ge(BarnProject::getHealthScore, 75)
                           .lt(BarnProject::getHealthScore, 90);
                    break;
                case "urgent":      // 急需修复: 60-74
                    wrapper.ge(BarnProject::getHealthScore, 60)
                           .lt(BarnProject::getHealthScore, 75);
                    break;
                case "retired":     // 退出: < 60
                    wrapper.lt(BarnProject::getHealthScore, 60);
                    break;
                default:
                    break;
            }
        }
        if (projectCode != null && !projectCode.isEmpty()) {
            wrapper.like(BarnProject::getId, projectCode);
        }
        if (barnName != null && !barnName.isEmpty()) {
            wrapper.like(BarnProject::getBarnName, barnName);
        }
        wrapper.orderByDesc(BarnProject::getCreatedAt);
        return barnProjectMapper.selectPage(page, wrapper);
    }

    /**
     * 根据ID查询
     */
    public BarnProject getById(String id) {
        return barnProjectMapper.selectById(id);
    }

    /**
     * 新增
     */
    public int save(BarnProject barn) {
        return barnProjectMapper.insert(barn);
    }

    /**
     * 更新
     */
    public int update(BarnProject barn) {
        return barnProjectMapper.updateById(barn);
    }

    /**
     * 删除
     */
    public int delete(String id) {
        return barnProjectMapper.deleteById(id);
    }

    /**
     * 烤房下拉选项 - 返回前端需要的字段（含经纬度、设施现状、预测寿命）
     */
    public List<Map<String, Object>> listOptions() {
        List<BarnProject> all = barnProjectMapper.selectList(null);

        // 批量查询 kf_basedata 表的 facility_status
        Map<String, String> facilityStatusMap = new HashMap<>();
        try {
            List<Map<String, Object>> baseRows = jdbcTemplate.queryForList(
                "SELECT project_id, facility_status FROM kf_basedata WHERE facility_status IS NOT NULL");
            for (Map<String, Object> row : baseRows) {
                String projectId = (String) row.get("project_id");
                String facilityStatus = (String) row.get("facility_status");
                if (projectId != null && facilityStatus != null) {
                    facilityStatusMap.put(projectId, facilityStatus);
                }
            }
        } catch (Exception e) {
            // 查询失败时不影响主流程，facilityStatus 默认为"正常"
        }

        List<Map<String, Object>> result = new java.util.ArrayList<>();
        for (BarnProject b : all) {
            Map<String, Object> m = new HashMap<>();
            m.put("barnId", b.getId());
            m.put("projectCode", b.getId());
            m.put("id", b.getId());
            m.put("barnCode", b.getId());
            m.put("barnName", b.getBarnName());
            m.put("address", b.getAddress());
            m.put("gpsAddress", b.getAddress());
            m.put("countyCode", b.getCountyCode());
            m.put("countyName", b.getCounty());
            m.put("county", b.getCounty());
            m.put("townCode", b.getTownCode());
            m.put("townName", b.getTownship());
            m.put("township", b.getTownship());
            m.put("cityCode", b.getCityCode());
            m.put("cityName", b.getCity());
            m.put("city", b.getCity());
            m.put("longitude", b.getLongitude());
            m.put("latitude", b.getLatitude());
            m.put("healthLevel", b.getHealthLevel());
            m.put("healthScore", b.getHealthScore());
            m.put("isIdle", b.getIsIdle());
            m.put("useStatus", b.getUseStatus());
            m.put("projectType", b.getProjectType());
            m.put("buildMode", b.getBuildMethod());
            m.put("status", b.getAuditStatus());
            m.put("predictedLifeYears", b.getPredictedLifeYears());
            m.put("facilityStatus", facilityStatusMap.getOrDefault(b.getId(), "正常"));
            result.add(m);
        }
        return result;
    }

    /**
     * 统计各健康等级数量
     */
    public Map<String, Object> healthStats() {
        List<BarnProject> all = barnProjectMapper.selectList(null);
        Map<String, Object> result = new HashMap<>();
        int excellent = 0, maintain = 0, urgent = 0, retired = 0;
        BigDecimal totalScore = BigDecimal.ZERO;
        for (BarnProject b : all) {
            String level = b.getHealthLevel();
            if (level != null) {
                if ("优良".equals(level)) excellent++;
                else if ("需维护".equals(level)) maintain++;
                else if ("急需修复".equals(level)) urgent++;
                else if ("退出".equals(level)) retired++;
            }
            if (b.getHealthScore() != null) {
                totalScore = totalScore.add(BigDecimal.valueOf(b.getHealthScore()));
            }
        }
        result.put("total", all.size());
        result.put("excellent", excellent);
        result.put("maintain", maintain);
        result.put("urgent", urgent);
        result.put("retired", retired);
        result.put("avgScore", all.isEmpty() ? 0 : totalScore.doubleValue() / all.size());
        return result;
    }

    /**
     * 县区分布统计
     */
    public List<Map<String, Object>> countyStats() {
        List<BarnProject> all = barnProjectMapper.selectList(null);
        Map<String, int[]> grouped = new HashMap<>();
        Map<String, Double> scoreSum = new HashMap<>();
        for (BarnProject b : all) {
            String key = b.getCounty() != null ? b.getCounty() : "未知";
            int[] arr = grouped.computeIfAbsent(key, k -> new int[3]);
            arr[0]++;
            if ("在用".equals(b.getUseStatus())) arr[1]++;
            else if ("闲置".equals(b.getUseStatus())) arr[2]++;
            scoreSum.merge(key, b.getHealthScore() != null ? b.getHealthScore().doubleValue() : 0.0, Double::sum);
        }
        return grouped.entrySet().stream().map(e -> {
            Map<String, Object> m = new HashMap<>();
            m.put("countyName", e.getKey());
            m.put("total", e.getValue()[0]);
            m.put("inUse", e.getValue()[1]);
            m.put("idle", e.getValue()[2]);
            m.put("avgScore", e.getValue()[0] > 0 ? scoreSum.get(e.getKey()) / e.getValue()[0] : 0);
            return m;
        }).collect(java.util.stream.Collectors.toList());
    }

    /**
     * 项目类型分布
     */
    public List<Map<String, Object>> projectTypeStats() {
        List<BarnProject> all = barnProjectMapper.selectList(null);
        Map<String, Integer> grouped = new HashMap<>();
        for (BarnProject b : all) {
            String key = b.getProjectType() != null ? b.getProjectType() : "其他";
            grouped.merge(key, 1, Integer::sum);
        }
        return grouped.entrySet().stream().map(e -> {
            Map<String, Object> m = new HashMap<>();
            m.put("projectType", e.getKey());
            m.put("count", e.getValue());
            return m;
        }).collect(java.util.stream.Collectors.toList());
    }

    /**
     * 建设方式分布
     */
    public List<Map<String, Object>> buildModeStats() {
        List<BarnProject> all = barnProjectMapper.selectList(null);
        Map<String, Integer> grouped = new HashMap<>();
        for (BarnProject b : all) {
            String key = b.getBuildMethod() != null ? b.getBuildMethod() : "其他";
            grouped.merge(key, 1, Integer::sum);
        }
        return grouped.entrySet().stream().map(e -> {
            Map<String, Object> m = new HashMap<>();
            m.put("buildMethod", e.getKey());
            m.put("count", e.getValue());
            return m;
        }).collect(java.util.stream.Collectors.toList());
    }
}
