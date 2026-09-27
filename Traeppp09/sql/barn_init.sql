-- =============================================
-- 烤房全周期信息化管理平台 - 数据库初始化脚本
-- 数据库: MySQL 8.0+
-- 创建时间: 2026-06-10
-- =============================================

-- 创建数据库
CREATE DATABASE IF NOT EXISTS `barn_management` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

USE `barn_management`;

-- =============================================
-- 1. 烤房基础信息表
-- =============================================
DROP TABLE IF EXISTS `t_barn_info`;
CREATE TABLE `t_barn_info` (
  `barn_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '烤房唯一ID',
  `barn_code` VARCHAR(32) NOT NULL COMMENT '烤房编号',
  `barn_name` VARCHAR(100) NOT NULL COMMENT '烤房名称',
  `gps_longitude` DECIMAL(10,6) DEFAULT NULL COMMENT 'GPS经度',
  `gps_latitude` DECIMAL(10,6) DEFAULT NULL COMMENT 'GPS纬度',
  `gps_address` VARCHAR(255) DEFAULT NULL COMMENT '详细地址',
  `county_code` VARCHAR(10) DEFAULT NULL COMMENT '所属县区编码',
  `town_code` VARCHAR(10) DEFAULT NULL COMMENT '所属乡镇编码',
  `village_code` VARCHAR(10) DEFAULT NULL COMMENT '所属村编码',
  `station_id` BIGINT DEFAULT NULL COMMENT '所属烟站ID',
  `cooperative_id` BIGINT DEFAULT NULL COMMENT '所属合作社ID',
  `pound_group_id` BIGINT DEFAULT NULL COMMENT '所属磅组ID',
  `build_year` INT DEFAULT NULL COMMENT '建设年度',
  `build_project` VARCHAR(100) DEFAULT NULL COMMENT '建设项目名称',
  `build_cost` DECIMAL(12,2) DEFAULT NULL COMMENT '建设费用（元）',
  `barn_type` VARCHAR(20) DEFAULT '密集式' COMMENT '烤房类型',
  `capacity` INT DEFAULT NULL COMMENT '容量（杆数）',
  `benefit_area` DECIMAL(10,2) DEFAULT NULL COMMENT '受益烟田面积（亩）',
  `overall_status` VARCHAR(20) DEFAULT '正常' COMMENT '整体状态',
  `health_score` DECIMAL(5,2) DEFAULT 100.00 COMMENT '健康评分（0-100）',
  `health_level` VARCHAR(10) DEFAULT '优良' COMMENT '健康等级',
  `is_idle` TINYINT DEFAULT 1 COMMENT '是否空闲（0否1是）',
  `current_user_id` BIGINT DEFAULT NULL COMMENT '当前使用人ID',
  `predicted_life_years` DECIMAL(4,1) DEFAULT 10.0 COMMENT '预测剩余有效寿命（年）',
  `optimal_repair_date` DATE DEFAULT NULL COMMENT '最优维修时间点',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` TINYINT DEFAULT 0 COMMENT '删除标志（0正常1删除）',
  PRIMARY KEY (`barn_id`),
  UNIQUE KEY `uk_barn_code` (`barn_code`),
  KEY `idx_county` (`county_code`),
  KEY `idx_status` (`overall_status`),
  KEY `idx_health` (`health_level`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='烤房基础信息表';

-- =============================================
-- 2. 烤房部件表
-- =============================================
DROP TABLE IF EXISTS `t_barn_component`;
CREATE TABLE `t_barn_component` (
  `component_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '部件ID',
  `barn_id` BIGINT NOT NULL COMMENT '所属烤房ID',
  `part_type` VARCHAR(10) NOT NULL COMMENT '部位类型（JR/SR/ZK/ZT/FS）',
  `component_name` VARCHAR(50) NOT NULL COMMENT '部件名称',
  `component_code` VARCHAR(20) DEFAULT NULL COMMENT '部件编码',
  `status` VARCHAR(20) DEFAULT '正常' COMMENT '状态',
  `damage_level` VARCHAR(20) DEFAULT '无' COMMENT '损坏程度',
  `damage_photo` VARCHAR(500) DEFAULT NULL COMMENT '损坏照片URL',
  `score` DECIMAL(5,2) DEFAULT 100.00 COMMENT '部件评分（0-100）',
  `weight` DECIMAL(3,2) DEFAULT 0.10 COMMENT '部件权重',
  `last_check_time` DATETIME DEFAULT NULL COMMENT '最近检查时间',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`component_id`),
  KEY `idx_barn_id` (`barn_id`),
  KEY `idx_part_type` (`part_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='烤房部件表';

-- =============================================
-- 3. 维修记录表
-- =============================================
DROP TABLE IF EXISTS `t_repair_record`;
CREATE TABLE `t_repair_record` (
  `repair_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '维修ID',
  `barn_id` BIGINT NOT NULL COMMENT '烤房ID',
  `repair_type` VARCHAR(20) NOT NULL COMMENT '维修类型',
  `urgency` VARCHAR(20) DEFAULT '正常' COMMENT '紧迫性',
  `applicant_id` BIGINT DEFAULT NULL COMMENT '申请人ID',
  `apply_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
  `apply_desc` VARCHAR(1000) DEFAULT NULL COMMENT '申请描述',
  `apply_photos` VARCHAR(1000) DEFAULT NULL COMMENT '申请照片URL',
  `estimated_cost` DECIMAL(10,2) DEFAULT NULL COMMENT '预估费用',
  `actual_cost` DECIMAL(10,2) DEFAULT NULL COMMENT '实际费用',
  `status` VARCHAR(20) DEFAULT '待审核' COMMENT '状态',
  `auditor_id` BIGINT DEFAULT NULL COMMENT '审核人ID',
  `audit_time` DATETIME DEFAULT NULL COMMENT '审核时间',
  `audit_opinion` VARCHAR(500) DEFAULT NULL COMMENT '审核意见',
  `implement_team` VARCHAR(100) DEFAULT NULL COMMENT '实施队伍',
  `start_date` DATE DEFAULT NULL COMMENT '实施开始日期',
  `end_date` DATE DEFAULT NULL COMMENT '实施结束日期',
  `completion_photos` VARCHAR(1000) DEFAULT NULL COMMENT '完工照片URL',
  `acceptor_id` BIGINT DEFAULT NULL COMMENT '验收人ID',
  `accept_time` DATETIME DEFAULT NULL COMMENT '验收时间',
  `accept_result` VARCHAR(20) DEFAULT NULL COMMENT '验收结果',
  `accept_opinion` VARCHAR(500) DEFAULT NULL COMMENT '验收意见',
  `roi_score` DECIMAL(5,2) DEFAULT NULL COMMENT '投入产出比评分',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`repair_id`),
  KEY `idx_barn_id` (`barn_id`),
  KEY `idx_status` (`status`),
  KEY `idx_urgency` (`urgency`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='维修记录表';

-- =============================================
-- 4. 预约记录表
-- =============================================
DROP TABLE IF EXISTS `t_reservation`;
CREATE TABLE `t_reservation` (
  `reservation_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '预约ID',
  `barn_id` BIGINT NOT NULL COMMENT '烤房ID',
  `user_id` BIGINT NOT NULL COMMENT '烟农用户ID',
  `reserve_start` DATETIME NOT NULL COMMENT '预约开始时间',
  `reserve_end` DATETIME NOT NULL COMMENT '预约结束时间',
  `status` VARCHAR(20) DEFAULT '待审核' COMMENT '状态',
  `reviewer_id` BIGINT DEFAULT NULL COMMENT '审核人ID',
  `review_time` DATETIME DEFAULT NULL COMMENT '审核时间',
  `review_opinion` VARCHAR(500) DEFAULT NULL COMMENT '审核意见',
  `actual_start` DATETIME DEFAULT NULL COMMENT '实际开始时间',
  `actual_end` DATETIME DEFAULT NULL COMMENT '实际结束时间',
  `leaf_weight` DECIMAL(8,2) DEFAULT NULL COMMENT '烟叶重量（kg）',
  `is_timeout` TINYINT DEFAULT 0 COMMENT '是否超时',
  `timeout_hours` DECIMAL(6,1) DEFAULT NULL COMMENT '超时小时数',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`reservation_id`),
  KEY `idx_barn_id` (`barn_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_status` (`status`),
  KEY `idx_reserve_time` (`reserve_start`, `reserve_end`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='预约记录表';

-- =============================================
-- 5. 评价表
-- =============================================
DROP TABLE IF EXISTS `t_evaluation`;
CREATE TABLE `t_evaluation` (
  `evaluation_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '评价ID',
  `reservation_id` BIGINT DEFAULT NULL COMMENT '预约ID',
  `barn_id` BIGINT NOT NULL COMMENT '烤房ID',
  `evaluator_id` BIGINT NOT NULL COMMENT '评价人ID',
  `baker_id` BIGINT DEFAULT NULL COMMENT '烘烤师ID',
  `barn_score` INT DEFAULT 5 COMMENT '烤房评分（1-5星）',
  `barn_comment` VARCHAR(500) DEFAULT NULL COMMENT '烤房评价内容',
  `baker_score` INT DEFAULT 5 COMMENT '烘烤师评分（1-5星）',
  `baker_comment` VARCHAR(500) DEFAULT NULL COMMENT '烘烤师评价内容',
  `evaluate_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '评价时间',
  PRIMARY KEY (`evaluation_id`),
  KEY `idx_barn_id` (`barn_id`),
  KEY `idx_reservation_id` (`reservation_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评价表';

-- =============================================
-- 6. 管护队伍表
-- =============================================
DROP TABLE IF EXISTS `t_maintenance_team`;
CREATE TABLE `t_maintenance_team` (
  `team_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '队伍ID',
  `team_name` VARCHAR(100) NOT NULL COMMENT '队伍名称',
  `team_type` VARCHAR(50) NOT NULL COMMENT '队伍类型',
  `cooperative_id` BIGINT DEFAULT NULL COMMENT '所属合作社ID',
  `leader_name` VARCHAR(50) DEFAULT NULL COMMENT '负责人',
  `leader_phone` VARCHAR(20) DEFAULT NULL COMMENT '联系电话',
  `member_count` INT DEFAULT 0 COMMENT '人数',
  `service_area` VARCHAR(255) DEFAULT NULL COMMENT '服务区域',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `status` TINYINT DEFAULT 0 COMMENT '状态（0正常1停用）',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`team_id`),
  KEY `idx_team_type` (`team_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='管护队伍表';

-- =============================================
-- 7. 资金管理表
-- =============================================
DROP TABLE IF EXISTS `t_fund_management`;
CREATE TABLE `t_fund_management` (
  `fund_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '资金ID',
  `year` INT NOT NULL COMMENT '年度',
  `county_code` VARCHAR(10) DEFAULT NULL COMMENT '县区编码',
  `fund_source` VARCHAR(50) NOT NULL COMMENT '资金来源',
  `total_amount` DECIMAL(12,2) DEFAULT 0.00 COMMENT '总金额',
  `used_amount` DECIMAL(12,2) DEFAULT 0.00 COMMENT '已使用金额',
  `allocated_amount` DECIMAL(12,2) DEFAULT 0.00 COMMENT '已分配金额',
  `repair_count` INT DEFAULT 0 COMMENT '维修项目数',
  `roi_avg` DECIMAL(5,2) DEFAULT NULL COMMENT '平均投入产出比',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`fund_id`),
  KEY `idx_year` (`year`),
  KEY `idx_county` (`county_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='资金管理表';

-- =============================================
-- 8. 用户扩展信息表
-- =============================================
DROP TABLE IF EXISTS `t_user_ext`;
CREATE TABLE `t_user_ext` (
  `user_ext_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '扩展信息ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID（关联sys_user）',
  `real_name` VARCHAR(50) DEFAULT NULL COMMENT '真实姓名',
  `phone` VARCHAR(20) DEFAULT NULL COMMENT '手机号',
  `id_card` VARCHAR(20) DEFAULT NULL COMMENT '身份证号',
  `role_type` VARCHAR(20) DEFAULT NULL COMMENT '角色类型',
  `county_code` VARCHAR(10) DEFAULT NULL COMMENT '所属县区',
  `station_id` BIGINT DEFAULT NULL COMMENT '所属烟站',
  `cooperative_id` BIGINT DEFAULT NULL COMMENT '所属合作社',
  `pound_group_id` BIGINT DEFAULT NULL COMMENT '所属磅组',
  `credit_score` DECIMAL(5,2) DEFAULT 100.00 COMMENT '信用评分',
  `unique_id` VARCHAR(32) DEFAULT NULL COMMENT '唯一标识ID',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`user_ext_id`),
  UNIQUE KEY `uk_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户扩展信息表';

-- =============================================
-- 9. 区域字典表（县区/乡镇/村）
-- =============================================
DROP TABLE IF EXISTS `t_region`;
CREATE TABLE `t_region` (
  `region_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '区域ID',
  `region_code` VARCHAR(10) NOT NULL COMMENT '区域编码',
  `region_name` VARCHAR(50) NOT NULL COMMENT '区域名称',
  `parent_code` VARCHAR(10) DEFAULT NULL COMMENT '父级编码',
  `region_level` INT DEFAULT 1 COMMENT '层级（1县区2乡镇3村）',
  `sort_order` INT DEFAULT 0 COMMENT '排序',
  `status` TINYINT DEFAULT 0 COMMENT '状态（0正常1停用）',
  PRIMARY KEY (`region_id`),
  UNIQUE KEY `uk_region_code` (`region_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='区域字典表';

-- =============================================
-- 初始化数据 - 区域数据
-- =============================================
INSERT INTO `t_region` (`region_code`, `region_name`, `parent_code`, `region_level`, `sort_order`) VALUES
('530300', '曲靖市', NULL, 1, 1),
('530301', '麒麟区', '530300', 2, 1),
('530302', '宣威市', '530300', 2, 2),
('530303', '沾益区', '530300', 2, 3),
('530304', '陆良县', '530300', 2, 4),
('530305', '师宗县', '530300', 2, 5),
('530306', '罗平县', '530300', 2, 6),
('530307', '富源县', '530300', 2, 7);

-- =============================================
-- 初始化数据 - 管护队伍
-- =============================================
INSERT INTO `t_maintenance_team` (`team_name`, `team_type`, `leader_name`, `leader_phone`, `member_count`, `service_area`) VALUES
('麒麟区烘烤技术服务队', '烘烤技术服务队', '张明', '13800138001', 8, '麒麟区全域'),
('麒麟区简易维修服务队', '简易维修服务队', '李强', '13800138002', 6, '麒麟区全域'),
('麒麟区常规管护服务队', '常规管护服务队', '王华', '13800138003', 10, '麒麟区全域'),
('麒麟区综合管理服务队', '综合管理服务队', '赵刚', '13800138004', 5, '麒麟区全域');

-- =============================================
-- 初始化数据 - 资金管理
-- =============================================
INSERT INTO `t_fund_management` (`year`, `county_code`, `fund_source`, `total_amount`, `used_amount`, `allocated_amount`, `repair_count`) VALUES
(2026, '530301', '行业管护资金', 500000.00, 320000.00, 150000.00, 12),
(2026, '530301', '政府管护资金', 300000.00, 180000.00, 100000.00, 8),
(2026, '530301', '合作社专项', 200000.00, 120000.00, 50000.00, 5);

-- =============================================
-- 初始化数据 - 示例烤房
-- =============================================
INSERT INTO `t_barn_info` (`barn_code`, `barn_name`, `gps_longitude`, `gps_latitude`, `gps_address`, `county_code`, `town_code`, `overall_status`, `health_score`, `health_level`, `build_year`, `capacity`, `benefit_area`, `barn_type`) VALUES
('KF20260001', '麒麟区烤房001号', 103.797500, 25.490000, '麒麟区三宝镇张家村', '530301', '53030101', '正常', 95.00, '优良', 2020, 150, 20.00, '密集式'),
('KF20260002', '麒麟区烤房002号', 103.805000, 25.495000, '麒麟区三宝镇李家村', '530301', '53030101', '正常', 88.00, '需维护', 2018, 120, 15.00, '密集式'),
('KF20260003', '宣威市烤房001号', 104.095000, 26.220000, '宣威市龙场镇龙场村', '530302', '53030201', '正常', 92.00, '优良', 2019, 180, 25.00, '密集式'),
('KF20260004', '沾益区烤房001号', 103.820000, 25.600000, '沾益区白水镇白水村', '530303', '53030301', '急需修复', 45.00, '急需修复', 2015, 100, 12.00, '密集式'),
('KF20260005', '陆良县烤房001号', 103.650000, 25.030000, '陆良县板桥镇板桥村', '530304', '53030401', '正常', 78.00, '需维护', 2017, 130, 18.00, '密集式');

-- =============================================
-- 创建视图 - 烤房健康统计
-- =============================================
CREATE OR REPLACE VIEW `v_barn_health_stats` AS
SELECT 
  county_code,
  COUNT(*) as total_count,
  SUM(CASE WHEN health_level = '优良' THEN 1 ELSE 0 END) as excellent_count,
  SUM(CASE WHEN health_level = '需维护' THEN 1 ELSE 0 END) as maintain_count,
  SUM(CASE WHEN health_level = '急需修复' THEN 1 ELSE 0 END) as urgent_count,
  SUM(CASE WHEN health_level = '退出' THEN 1 ELSE 0 END) as retired_count,
  AVG(health_score) as avg_score
FROM t_barn_info
WHERE del_flag = 0
GROUP BY county_code;

-- =============================================
-- 创建视图 - 维修统计
-- =============================================
CREATE OR REPLACE VIEW `v_repair_stats` AS
SELECT 
  county_code,
  COUNT(*) as total_count,
  SUM(CASE WHEN status = '待审核' THEN 1 ELSE 0 END) as pending_count,
  SUM(CASE WHEN status = '实施中' THEN 1 ELSE 0 END) as ongoing_count,
  SUM(CASE WHEN status = '已验收' THEN 1 ELSE 0 END) as completed_count,
  SUM(estimated_cost) as total_estimated,
  SUM(actual_cost) as total_actual
FROM t_repair_record r
JOIN t_barn_info b ON r.barn_id = b.barn_id
GROUP BY county_code;

-- 完成提示
SELECT '数据库初始化完成！' as message;
SELECT COUNT(*) as barn_count FROM t_barn_info;
SELECT COUNT(*) as team_count FROM t_maintenance_team;
SELECT COUNT(*) as fund_count FROM t_fund_management;