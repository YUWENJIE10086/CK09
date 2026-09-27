package com.barn.barn.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.barn.barn.entity.EvaluationRecord;
import com.barn.barn.mapper.EvaluationRecordMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class EvaluationRecordService {

    @Autowired
    private EvaluationRecordMapper evaluationRecordMapper;

    public IPage<EvaluationRecord> pageList(int pageNum, int pageSize, String ovenId) {
        Page<EvaluationRecord> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<EvaluationRecord> wrapper = new LambdaQueryWrapper<>();
        if (ovenId != null) {
            wrapper.eq(EvaluationRecord::getOvenId, ovenId);
        }
        wrapper.orderByDesc(EvaluationRecord::getEvaluateTime);
        return evaluationRecordMapper.selectPage(page, wrapper);
    }

    public Map<String, Object> statistics() {
        List<EvaluationRecord> all = evaluationRecordMapper.selectList(null);
        Map<String, Object> result = new HashMap<>();
        int totalBarn = 0, totalBaker = 0;
        for (EvaluationRecord e : all) {
            if (e.getEquipmentRating() != null) totalBarn += e.getEquipmentRating();
            if (e.getBakerRating() != null) totalBaker += e.getBakerRating();
        }
        int count = all.size();
        result.put("totalCount", count);
        result.put("avgBarnScore", count > 0 ? (double) totalBarn / count : 0);
        result.put("avgBakerScore", count > 0 ? (double) totalBaker / count : 0);
        return result;
    }

    public int save(EvaluationRecord record) {
        return evaluationRecordMapper.insert(record);
    }

    public int update(EvaluationRecord record) {
        return evaluationRecordMapper.updateById(record);
    }

    public int delete(Long id) {
        return evaluationRecordMapper.deleteById(id);
    }
}
