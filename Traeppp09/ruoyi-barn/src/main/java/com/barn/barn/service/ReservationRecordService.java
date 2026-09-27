package com.barn.barn.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.barn.barn.entity.ReservationRecord;
import com.barn.barn.mapper.ReservationRecordMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ReservationRecordService {

    @Autowired
    private ReservationRecordMapper reservationRecordMapper;

    public IPage<ReservationRecord> pageList(int pageNum, int pageSize, String status, String ovenId) {
        Page<ReservationRecord> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<ReservationRecord> wrapper = new LambdaQueryWrapper<>();
        if (status != null && !status.isEmpty()) {
            wrapper.eq(ReservationRecord::getStatus, status);
        }
        if (ovenId != null) {
            wrapper.eq(ReservationRecord::getOvenId, ovenId);
        }
        wrapper.orderByDesc(ReservationRecord::getPlanStartTime);
        return reservationRecordMapper.selectPage(page, wrapper);
    }

    public int save(ReservationRecord record) {
        return reservationRecordMapper.insert(record);
    }

    public int update(ReservationRecord record) {
        return reservationRecordMapper.updateById(record);
    }

    public int delete(Long id) {
        return reservationRecordMapper.deleteById(id);
    }
}
