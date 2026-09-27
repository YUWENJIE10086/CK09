package com.barn.barn.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 管护队伍实体
 */
@Data
@TableName("maintenance_teams")
public class MaintenanceTeam implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String teamName;
    private String teamType;
    private String countyCode;
    private String countyName;
    private String leaderName;
    private String leaderPhone;
    private Integer memberCount;
    private String serviceArea;
    private Integer repairCount;
    private String status;
    private String createdBy;
    private LocalDateTime createdAt;
    private String updatedBy;
    private LocalDateTime updatedAt;
    private String remark;
}
