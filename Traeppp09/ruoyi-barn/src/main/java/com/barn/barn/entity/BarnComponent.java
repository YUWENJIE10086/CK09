package com.barn.barn.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 烤房部件实体
 */
@Data
@TableName("oven_components")
public class BarnComponent implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Integer id;

    private String ovenId;
    private String componentKey;
    private String componentName;
    private String status;
    private String damageLevel;
    private BigDecimal score;
    private BigDecimal weight;
    private Integer repairYear;
    private String value;
    private LocalDateTime lastCheckAt;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
