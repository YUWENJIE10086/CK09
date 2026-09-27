package com.barn.barn.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.barn.barn.entity.ReservationRecord;
import com.barn.barn.mapper.ReservationRecordMapper;
import com.barn.common.core.domain.R;
import com.barn.common.core.domain.TableDataInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/barn/reservation")
@PreAuthorize("hasRole('ADMIN')")
public class ReservationRecordController {

    @Autowired
    private ReservationRecordMapper reservationRecordMapper;

    @GetMapping("/list")
    public TableDataInfo<ReservationRecord> list(@RequestParam(defaultValue = "1") int pageNum,
                                                 @RequestParam(defaultValue = "10") int pageSize,
                                                 @RequestParam(required = false) String ovenId,
                                                 @RequestParam(required = false) String status,
                                                 @RequestParam(required = false) String userId) {
        Page<ReservationRecord> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<ReservationRecord> wrapper = new LambdaQueryWrapper<>();
        if (ovenId != null && !ovenId.isEmpty()) wrapper.eq(ReservationRecord::getOvenId, ovenId);
        if (status != null && !status.isEmpty()) wrapper.eq(ReservationRecord::getStatus, status);
        if (userId != null && !userId.isEmpty()) wrapper.eq(ReservationRecord::getUserId, userId);
        wrapper.orderByDesc(ReservationRecord::getCreatedAt);
        IPage<ReservationRecord> result = reservationRecordMapper.selectPage(page, wrapper);
        return TableDataInfo.build(result.getRecords(), result.getTotal(), pageNum, pageSize);
    }

    @GetMapping("/{id}")
    public R<ReservationRecord> getInfo(@PathVariable String id) {
        return R.ok(reservationRecordMapper.selectById(id));
    }

    @PostMapping
    public R<Void> add(@RequestBody ReservationRecord record) {
        reservationRecordMapper.insert(record);
        return R.ok();
    }

    @PutMapping
    public R<Void> edit(@RequestBody ReservationRecord record) {
        reservationRecordMapper.updateById(record);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    public R<Void> remove(@PathVariable String id) {
        reservationRecordMapper.deleteById(id);
        return R.ok();
    }

    /**
     * 审核预约：通过/驳回
     * 入参：{ reservationId, reserveStatus: 已确认/rejected, reviewOpinion }
     */
    @PostMapping("/approve")
    public R<Void> approve(@RequestBody Map<String, Object> body) {
        Object idObj = body.get("reservationId");
        if (idObj == null) return R.fail("reservationId不能为空");
        String id = idObj.toString();
        ReservationRecord record = reservationRecordMapper.selectById(id);
        if (record == null) return R.fail("预约记录不存在");
        Object status = body.get("reserveStatus");
        if (status != null) record.setStatus(status.toString());
        Object opinion = body.get("reviewOpinion");
        if (opinion != null) record.setReviewOpinion(opinion.toString());
        record.setReviewer("admin");
        record.setReviewTime(java.time.LocalDateTime.now());
        reservationRecordMapper.updateById(record);
        return R.ok();
    }
}
