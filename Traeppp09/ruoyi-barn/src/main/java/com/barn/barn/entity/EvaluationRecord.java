package com.barn.barn.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 评价记录实体
 */
@Data
@TableName("evaluations")
public class EvaluationRecord implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Integer id;

    private String reservationId;
    private String ovenId;
    private String ovenName;
    private String userId;
    private String evaluatorName;
    private String bakerId;
    private String bakerName;
    private Integer equipmentRating;
    private Integer bakerRating;
    private String comment;
    private String bakerComment;
    private LocalDateTime evaluateTime;
    private Integer bakingRecordId;
    private String assignmentId;
    private Integer overallRating;
    private Integer tempControlRating;
    private String ovenTags;
    private String images;
    private String createdBy;
    private LocalDateTime createdAt;
    private String updatedBy;
    private LocalDateTime updatedAt;
}
