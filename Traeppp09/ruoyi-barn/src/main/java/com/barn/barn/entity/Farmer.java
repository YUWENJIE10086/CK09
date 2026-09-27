package com.barn.barn.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 烟农实体 - 对应users表
 */
@Data
@TableName("users")
public class Farmer implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.INPUT)
    private String id;

    /** 姓名 */
    private String name;

    /** 手机号 */
    private String phone;

    /** 信用等级(A/B/C/D) */
    private String creditLevel;

    /** 信用分数 */
    private Integer creditScore;

    /** 所属区域 */
    private String area;

    /** 磅组ID */
    private Integer poundGroupId;

    /** 种植面积(亩) */
    private BigDecimal plantingArea;

    /** 种植年限 */
    private Integer plantingYears;

    /** 总烘烤次数 */
    private Integer totalBakes;

    /** 头像URL */
    private String avatarUrl;

    /** 状态(1=启用, 0=禁用) */
    private Integer status;

    /** 最后登录时间 */
    private LocalDateTime lastLoginAt;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}
