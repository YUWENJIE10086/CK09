package com.barn.barn.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 烤房分配记录实体
 */
@Data
@TableName("oven_assignments")
public class BarnAssignment implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.INPUT)
    private String id;

    private String ovenId;
    private String ovenName;

    private String userId;
    private String farmerName;
    private String farmerPhone;

    private String status;
    private String returnDate;

    private String assignedBy;
    private LocalDateTime assignedAt;

    /** 分配开始时间 */
    private LocalDateTime startTime;

    /** 分配结束时间（到期自动释放） */
    private LocalDateTime endTime;

    private String bakerId;
    private Integer seasonYear;
    private String seasonName;

    private String createdBy;
    private LocalDateTime createdAt;
    private String updatedBy;
    private LocalDateTime updatedAt;
    private String remark;
}
