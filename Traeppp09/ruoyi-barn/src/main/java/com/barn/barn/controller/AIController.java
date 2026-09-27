package com.barn.barn.controller;

import com.barn.barn.service.AIService;
import com.barn.common.core.domain.R;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * AI算法Controller - 提供健康评分、预测、推荐、资金分配等智能算法接口
 */
@RestController
@RequestMapping("/barn/ai")
public class AIController {

    @Autowired
    private AIService aiService;

    /**
     * 自动健康评分
     * 根据烤房部件数据或多因素模型计算健康评分
     */
    @PostMapping("/health-score")
    public R<Map<String, Object>> healthScore(@RequestBody Map<String, Object> params) {
        String ovenId = params.get("ovenId") != null ? params.get("ovenId").toString() : params.get("barnId").toString();
        return R.ok(aiService.calculateHealthScore(ovenId));
    }

    /**
     * 健康状态预测
     * 预测烤房退化速率、剩余寿命、最优维修时间等
     */
    @PostMapping("/predict")
    public R<Map<String, Object>> predict(@RequestBody Map<String, Object> params) {
        String ovenId = params.get("ovenId") != null ? params.get("ovenId").toString() : params.get("barnId").toString();
        return R.ok(aiService.predictHealth(ovenId));
    }

    /**
     * 智能烤房推荐
     * 根据用户需求推荐最匹配的烤房
     */
    @PostMapping("/recommend")
    public R<Map<String, Object>> recommend(@RequestBody Map<String, Object> params) {
        return R.ok(aiService.recommendBarns(params));
    }

    /**
     * 维修资金分配
     * 根据维修优先级智能分配预算资金
     */
    @PostMapping("/fund-allocate")
    public R<Map<String, Object>> fundAllocate(@RequestBody Map<String, Object> params) {
        return R.ok(aiService.allocateFund(params));
    }
}
