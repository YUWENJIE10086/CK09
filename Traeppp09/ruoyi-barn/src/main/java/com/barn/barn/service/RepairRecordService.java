package com.barn.barn.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.barn.barn.entity.RepairRecord;
import com.barn.barn.mapper.RepairRecordMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 维修Service
 */
@Service
public class RepairRecordService {

    @Autowired
    private RepairRecordMapper repairRecordMapper;

    public IPage<RepairRecord> pageList(int pageNum, int pageSize, String status, String urgency, String repairType) {
        Page<RepairRecord> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<RepairRecord> wrapper = new LambdaQueryWrapper<>();
        if (status != null && !status.isEmpty()) {
            wrapper.eq(RepairRecord::getRepairStatus, status);
        }
        if (urgency != null && !urgency.isEmpty()) {
            wrapper.eq(RepairRecord::getUrgency, urgency);
        }
        if (repairType != null && !repairType.isEmpty()) {
            wrapper.eq(RepairRecord::getRepairType, repairType);
        }
        wrapper.orderByDesc(RepairRecord::getApplyTime);
        return repairRecordMapper.selectPage(page, wrapper);
    }

    public RepairRecord getById(Long id) {
        return repairRecordMapper.selectById(id);
    }

    public int save(RepairRecord record) {
        return repairRecordMapper.insert(record);
    }

    public int update(RepairRecord record) {
        return repairRecordMapper.updateById(record);
    }

    public int delete(Long id) {
        return repairRecordMapper.deleteById(id);
    }
}
