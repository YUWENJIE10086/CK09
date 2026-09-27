package com.barn.barn.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 预约记录实体
 */
@Data
@TableName("reservations")
public class ReservationRecord implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.INPUT)
    private String id;

    private String userId;
    private String ovenId;
    private String bakerId;
    private String status;
    private LocalDateTime planStartTime;
    private Integer durationDays;
    private Integer seasonYear;
    private String seasonName;
    private BigDecimal tobaccoWeight;
    private String tobaccoType;
    private String remark;
    private LocalDateTime confirmTime;
    private LocalDateTime actualStart;
    private LocalDateTime actualEnd;
    private String cancelReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String reviewer;
    private LocalDateTime reviewTime;
    private String reviewOpinion;
    private Integer isTimeout;
    private String createdBy;
    private String updatedBy;
}
