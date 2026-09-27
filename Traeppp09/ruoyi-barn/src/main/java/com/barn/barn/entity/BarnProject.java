package com.barn.barn.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 烤房项目实体 - 对应ovens表
 */
@Data
@TableName("ovens")
public class BarnProject implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.INPUT)
    private String id;

    /** 烤房名称 */
    private String barnName;

    /** 使用状态(在用/闲置/转用/损毁) */
    private String useStatus;

    /** 闲置年数 */
    private Integer idleYears;

    private Integer transferYear;
    private String transferUse;
    private Integer damageYear;
    private String damageReason;

    /** 建设方式 */
    private String buildMethod;

    /** 项目类型 */
    private String projectType;

    private BigDecimal lengthM;
    private BigDecimal widthM;
    private BigDecimal heightM;
    private BigDecimal projectCost;
    private BigDecimal subsidyNational;
    private BigDecimal subsidyRegion;
    private BigDecimal subsidyTotal;

    private LocalDate startDate;
    private LocalDate completeDate;

    private String cityCode;
    private String city;
    private String countyCode;
    private String county;
    private String townCode;
    private String township;
    private String villageCode;
    private String village;
    private String address;
    private String projectOwner;
    private String constructionUnit;
    private Integer altitude;
    private BigDecimal longitude;
    private BigDecimal latitude;

    private Integer hasHeating;
    private Integer hasRadiator;
    private Integer hasAutocontrol;
    private Integer hasMain;
    private Integer hasAncillary;
    private String stationName;

    /** 健康评分 */
    private Integer healthScore;

    /** 健康等级 */
    private String healthLevel;

    private BigDecimal predictedLifeYears;
    private Integer isIdle;
    private String auditStatus;
    private String status;
    /** 设施现状(正常/闲置/损坏/另作他用) — 来自kf_basedata表，非ovens表字段 */
    @TableField(exist = false)
    private String facilityStatus;
    private String baseId;
    private Integer poundGroupId;
    private BigDecimal temperature;
    private BigDecimal humidity;
    private String bakerId;
    private String technician;
    private Integer usageCount;
    private Integer distance;
    private String description;
    private String createdBy;
    private LocalDateTime createdAt;
    private String updatedBy;
    private LocalDateTime updatedAt;
    private String remark;
}
