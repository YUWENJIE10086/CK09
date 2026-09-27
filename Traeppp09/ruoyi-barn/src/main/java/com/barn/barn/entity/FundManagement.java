package com.barn.barn.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 资金管理实体
 */
@Data
@TableName("fund_managements")
public class FundManagement implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Integer fundYear;
    private String countyCode;
    private String countyName;
    private String fundSource;
    private BigDecimal totalAmount;
    private BigDecimal usedAmount;
    private BigDecimal allocatedAmount;
    private Integer repairCount;
    private BigDecimal avgRoi;
    private String createdBy;
    private LocalDateTime createdAt;
    private String updatedBy;
    private LocalDateTime updatedAt;
    private String remark;
}
