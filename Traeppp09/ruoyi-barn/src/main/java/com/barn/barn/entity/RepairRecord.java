package com.barn.barn.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 维修记录实体
 */
@Data
@TableName("repair_records")
public class RepairRecord implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String ovenId;
    private String ovenName;
    private String repairType;
    private String urgency;
    private String applicant;
    private LocalDateTime applyTime;
    private String applyDesc;
    private BigDecimal estimatedCost;
    private BigDecimal actualCost;
    private String repairStatus;
    private String auditor;
    private LocalDateTime auditTime;
    private String auditOpinion;
    private String implementTeam;
    private LocalDate startDate;
    private LocalDate endDate;
    private String acceptor;
    private LocalDateTime acceptTime;
    private String acceptResult;
    private String acceptOpinion;
    private BigDecimal roiScore;
    private String createdBy;
    private LocalDateTime createdAt;
    private String updatedBy;
    private LocalDateTime updatedAt;
    private String remark;
}
