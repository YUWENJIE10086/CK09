-- ============================================================
-- KaoF 烟草智能烤房服务系统 - 数据库建表脚本
-- 数据库: KaoF   字符集: utf8mb4
-- 更新: 2025-06-26 新增 kf_basedata（烤房基础信息表，来源Excel）
-- ============================================================

CREATE DATABASE IF NOT EXISTS `KaoF` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `KaoF`;

-- ----------------------------------------------------------
-- 0. 烤房项目基础信息表（kf_basedata）
--    来源: files/烤房-项目基础信息表.xlsx
--    权威数据源，ovens 表的基础字段从本表同步
-- ----------------------------------------------------------
DROP TABLE IF EXISTS `kf_basedata`;
CREATE TABLE `kf_basedata` (
  `project_id`        VARCHAR(30) NOT NULL COMMENT '项目编号（即烤房编号）',
  `use_status`        VARCHAR(20) DEFAULT NULL COMMENT '使用状态',
  `idle_years`        DECIMAL(4,1) DEFAULT NULL COMMENT '闲置年份数（单位：年）',
  `diversion_year`    VARCHAR(10) DEFAULT NULL COMMENT '转用年度',
  `diversion_purpose` VARCHAR(100) DEFAULT NULL COMMENT '转用用途',
  `damage_year`       VARCHAR(10) DEFAULT NULL COMMENT '损毁年度',
  `damage_reason`     VARCHAR(200) DEFAULT NULL COMMENT '损毁原因',
  `build_method`      VARCHAR(20) DEFAULT NULL COMMENT '建设方式',
  `project_type`      VARCHAR(50) DEFAULT NULL COMMENT '项目类型',
  `oven_length`       DECIMAL(6,2) DEFAULT NULL COMMENT '长（米）',
  `oven_width`        DECIMAL(6,2) DEFAULT NULL COMMENT '宽（米）',
  `oven_height`       DECIMAL(6,2) DEFAULT NULL COMMENT '高（米）',
  `project_cost`      DECIMAL(12,2) DEFAULT NULL COMMENT '工程造价（元）',
  `natl_subsidy`      DECIMAL(12,2) DEFAULT NULL COMMENT '国家局补贴（元）',
  `local_subsidy`     DECIMAL(12,2) DEFAULT NULL COMMENT '产区补贴（元）',
  `total_subsidy`     DECIMAL(12,2) DEFAULT NULL COMMENT '补贴总额（元）',
  `start_date`        DATE DEFAULT NULL COMMENT '开工时间',
  `finish_date`       DATE DEFAULT NULL COMMENT '竣工时间',
  `city`              VARCHAR(50) DEFAULT NULL COMMENT '市',
  `county`            VARCHAR(50) DEFAULT NULL COMMENT '县（市、区）',
  `township`          VARCHAR(50) DEFAULT NULL COMMENT '乡（镇）',
  `village`           VARCHAR(50) DEFAULT NULL COMMENT '村',
  `detail_address`    VARCHAR(200) DEFAULT NULL COMMENT '详细地址',
  `project_owner`     VARCHAR(100) DEFAULT NULL COMMENT '项目业主',
  `build_company`     VARCHAR(100) DEFAULT NULL COMMENT '施工单位',
  `altitude`          INT DEFAULT NULL COMMENT '海拔（米）',
  `longitude`         DECIMAL(12,9) DEFAULT NULL COMMENT '经度',
  `latitude`          DECIMAL(12,9) DEFAULT NULL COMMENT '纬度',
  `technician_name`   VARCHAR(50) DEFAULT NULL COMMENT '技术员姓名',
  `heater_filled`     VARCHAR(10) DEFAULT NULL COMMENT '加热设备是否填写',
  `radiator_filled`   VARCHAR(10) DEFAULT NULL COMMENT '散热器是否填写',
  `controller_filled` VARCHAR(10) DEFAULT NULL COMMENT '自控设备是否填写',
  `oven_body_filled`  VARCHAR(10) DEFAULT NULL COMMENT '烤房主体是否填写',
  `annex_filled`      VARCHAR(10) DEFAULT NULL COMMENT '附属设施是否填写',
  `city_code`         VARCHAR(20) DEFAULT NULL COMMENT '市编码',
  `county_code`       VARCHAR(20) DEFAULT NULL COMMENT '县市编码',
  `township_code`     VARCHAR(20) DEFAULT NULL COMMENT '乡镇编码',
  `village_code`      VARCHAR(20) DEFAULT NULL COMMENT '村编码',
  `rel_station`       VARCHAR(200) DEFAULT NULL COMMENT '关联收购站',
  `rel_county_dept`   VARCHAR(200) DEFAULT NULL COMMENT '关联县级部门',
  `rel_city_dept`     VARCHAR(200) DEFAULT NULL COMMENT '关联市级部门',
  `created_at`        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`project_id`),
  KEY `idx_use_status` (`use_status`),
  KEY `idx_city_county` (`city`, `county`),
  KEY `idx_township` (`township`),
  KEY `idx_village` (`village`),
  KEY `idx_geo` (`longitude`, `latitude`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='烤房项目基础信息表（来源Excel）';

-- ----------------------------------------------------------
-- 1. 磅组表
-- ----------------------------------------------------------
DROP TABLE IF EXISTS `pound_groups`;
CREATE TABLE `pound_groups` (
  `id`          INT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '磅组ID',
  `name`        VARCHAR(50) NOT NULL COMMENT '磅组名称',
  `description` VARCHAR(200) DEFAULT NULL COMMENT '描述',
  `sort_order`  TINYINT UNSIGNED DEFAULT 0 COMMENT '排序序号',
  `status`      TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 1启用 0禁用',
  `created_at`  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='磅组表';

-- ----------------------------------------------------------
-- 2. 烟农用户表
-- ----------------------------------------------------------
DROP TABLE IF EXISTS `users`;
CREATE TABLE `users` (
  `id`              VARCHAR(20) NOT NULL COMMENT '用户ID, 如YN001',
  `name`            VARCHAR(50) NOT NULL COMMENT '姓名',
  `phone`           VARCHAR(20) NOT NULL COMMENT '手机号',
  `password_hash`   VARCHAR(255) NOT NULL DEFAULT '' COMMENT '密码哈希',
  `credit_level`    CHAR(1) NOT NULL DEFAULT 'D' COMMENT '信用等级: A/B/C/D',
  `credit_score`    SMALLINT UNSIGNED NOT NULL DEFAULT 60 COMMENT '信用积分 0-100',
  `area`            VARCHAR(100) DEFAULT NULL COMMENT '所属区域',
  `pound_group_id`  INT UNSIGNED DEFAULT NULL COMMENT '所属磅组ID',
  `planting_area`   DECIMAL(6,2) NOT NULL DEFAULT 0.00 COMMENT '种植面积(亩)',
  `planting_years`  TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '种植年限(年)',
  `total_bakes`     INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '累计烘烤次数',
  `avatar_url`      VARCHAR(255) DEFAULT NULL COMMENT '头像URL',
  `openid`          VARCHAR(100) DEFAULT NULL COMMENT '微信OpenID',
  `unionid`         VARCHAR(100) DEFAULT NULL COMMENT '微信UnionID',
  `status`          TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 1正常 0禁用',
  `last_login_at`   DATETIME DEFAULT NULL COMMENT '最后登录时间',
  `created_at`      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_phone` (`phone`),
  UNIQUE KEY `uk_openid` (`openid`),
  KEY `idx_pound_group` (`pound_group_id`),
  KEY `idx_credit_level` (`credit_level`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='烟农用户表';

-- ----------------------------------------------------------
-- 3. 烘烤师表
-- ----------------------------------------------------------
DROP TABLE IF EXISTS `bakers`;
CREATE TABLE `bakers` (
  `id`             VARCHAR(20) NOT NULL COMMENT '烘烤师ID, 如BK001',
  `name`           VARCHAR(50) NOT NULL COMMENT '姓名',
  `phone`          VARCHAR(20) DEFAULT NULL COMMENT '联系电话',
  `experience`     TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '从业年限(年)',
  `rating`         DECIMAL(2,1) NOT NULL DEFAULT 5.0 COMMENT '综合评分 0-5',
  `total_orders`   INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '服务总单数',
  `pound_group_id` INT UNSIGNED DEFAULT NULL COMMENT '所属磅组ID',
  `status`         TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 1在岗 0离职',
  `created_at`     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_pound_group` (`pound_group_id`),
  KEY `idx_rating` (`rating`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='烘烤师表';

-- ----------------------------------------------------------
-- 4. 烤房表
--    基础字段（地址、经纬度等）来源: kf_basedata 表
--    业务字段（状态、健康度等）为小程序自有
-- ----------------------------------------------------------
DROP TABLE IF EXISTS `ovens`;
CREATE TABLE `ovens` (
  `id`              VARCHAR(30) NOT NULL COMMENT '烤房编号, 如10-420527KF00001',
  `base_id`         VARCHAR(30) DEFAULT NULL COMMENT '关联kf_basedata编号',
  `pound_group_id`  INT UNSIGNED DEFAULT NULL COMMENT '所属磅组ID',
  `city`            VARCHAR(50) DEFAULT NULL COMMENT '市',
  `county`          VARCHAR(50) DEFAULT NULL COMMENT '县',
  `township`        VARCHAR(50) DEFAULT NULL COMMENT '乡（镇）',
  `village`         VARCHAR(50) DEFAULT NULL COMMENT '村',
  `project_type`    VARCHAR(50) DEFAULT NULL COMMENT '项目类型',
  `build_method`    VARCHAR(20) DEFAULT NULL COMMENT '建设方式',
  `address`         VARCHAR(200) NOT NULL COMMENT '完整地址',
  `latitude`        DECIMAL(10,7) DEFAULT NULL COMMENT '纬度',
  `longitude`       DECIMAL(10,7) DEFAULT NULL COMMENT '经度',
  `altitude`        INT DEFAULT NULL COMMENT '海拔（米）',
  `distance`        INT UNSIGNED DEFAULT 0 COMMENT '距离用户距离(米)',
  `status`          ENUM('idle','waiting','baking','maintenance','reserved') NOT NULL DEFAULT 'idle' COMMENT '状态: 空闲/待机/烘烤中/维修/已分配',
  `health_score`    TINYINT UNSIGNED NOT NULL DEFAULT 85 COMMENT '健康度评分 0-100',
  `temperature`     DECIMAL(5,1) NOT NULL DEFAULT 25.0 COMMENT '当前温度(°C)',
  `humidity`        DECIMAL(5,1) NOT NULL DEFAULT 60.0 COMMENT '当前湿度(%)',
  `baker_id`        VARCHAR(20) DEFAULT NULL COMMENT '当前负责烘烤师ID',
  `technician`      VARCHAR(50) DEFAULT NULL COMMENT '技术员姓名',
  `project_cost`    DECIMAL(12,2) DEFAULT NULL COMMENT '工程造价（元）',
  `usage_count`     INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '累计使用次数',
  `description`     VARCHAR(500) DEFAULT NULL COMMENT '备注描述',
  `created_at`      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_base_id` (`base_id`),
  KEY `idx_pound_group` (`pound_group_id`),
  KEY `idx_city_county` (`city`, `county`),
  KEY `idx_township` (`township`),
  KEY `idx_status` (`status`),
  KEY `idx_health` (`health_score`),
  KEY `idx_baker` (`baker_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='烤房表';

-- ----------------------------------------------------------
-- 5. 烤房设备部件表
-- ----------------------------------------------------------
DROP TABLE IF EXISTS `oven_components`;
CREATE TABLE `oven_components` (
  `id`              INT UNSIGNED NOT NULL AUTO_INCREMENT,
  `oven_id`         VARCHAR(30) NOT NULL COMMENT '所属烤房编号',
  `component_key`   VARCHAR(30) NOT NULL COMMENT '部件标识: heater/fan/controller/sensor',
  `component_name`  VARCHAR(50) NOT NULL COMMENT '部件名称',
  `status`          ENUM('normal','warning','fault','missing') NOT NULL DEFAULT 'normal' COMMENT '状态',
  `value`           VARCHAR(100) DEFAULT NULL COMMENT '当前值/状态描述',
  `last_check_at`   DATETIME DEFAULT NULL COMMENT '最后检测时间',
  `created_at`      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_oven_component` (`oven_id`, `component_key`),
  KEY `idx_oven` (`oven_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='烤房设备部件表';

-- ----------------------------------------------------------
-- 6. 用户收藏表
-- ----------------------------------------------------------
DROP TABLE IF EXISTS `user_favorites`;
CREATE TABLE `user_favorites` (
  `id`          INT UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_id`     VARCHAR(20) NOT NULL COMMENT '用户ID',
  `oven_id`     VARCHAR(30) NOT NULL COMMENT '烤房编号',
  `created_at`  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_oven` (`user_id`, `oven_id`),
  KEY `idx_user` (`user_id`),
  KEY `idx_oven` (`oven_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户收藏表';

-- ----------------------------------------------------------
-- 7. 种植档案表
-- ----------------------------------------------------------
DROP TABLE IF EXISTS `planting_records`;
CREATE TABLE `planting_records` (
  `id`          INT UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_id`     VARCHAR(20) NOT NULL COMMENT '烟农用户ID',
  `year`        SMALLINT UNSIGNED NOT NULL COMMENT '年份',
  `season`      ENUM('春季','秋季') NOT NULL COMMENT '季节',
  `area`        DECIMAL(6,2) NOT NULL DEFAULT 0.00 COMMENT '种植面积(亩)',
  `yield`       INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '产量(斤)',
  `quality`     ENUM('优良','良好','一般','较差') NOT NULL DEFAULT '一般' COMMENT '品质等级',
  `remark`      VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `created_at`  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user` (`user_id`),
  KEY `idx_year_season` (`year`, `season`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='种植档案表';

-- ----------------------------------------------------------
-- 8. 烤房分配表（后台管理程序维护）
-- ----------------------------------------------------------
DROP TABLE IF EXISTS `oven_assignments`;
CREATE TABLE `oven_assignments` (
  `id`              VARCHAR(30) NOT NULL COMMENT '分配单号',
  `user_id`         VARCHAR(20) NOT NULL COMMENT '烟农用户ID',
  `oven_id`         VARCHAR(30) NOT NULL COMMENT '烤房编号',
  `baker_id`        VARCHAR(20) DEFAULT NULL COMMENT '烘烤师ID',
  `season_year`     SMALLINT UNSIGNED NOT NULL COMMENT '分配年度/烘烤季',
  `season_name`     VARCHAR(30) NOT NULL COMMENT '烘烤季名称',
  `status`          ENUM('assigned','active','completed','cancelled') NOT NULL DEFAULT 'active' COMMENT '状态',
  `assigned_by`     VARCHAR(50) DEFAULT NULL COMMENT '后台分配人',
  `assigned_at`     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '分配时间',
  `remark`          VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `created_at`      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user` (`user_id`),
  KEY `idx_oven` (`oven_id`),
  KEY `idx_baker` (`baker_id`),
  KEY `idx_season` (`season_year`),
  KEY `idx_status` (`status`),
  UNIQUE KEY `uk_user_oven_season` (`user_id`, `oven_id`, `season_year`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='烤房分配表';

-- ----------------------------------------------------------
-- 8b. 预约记录表（历史兼容，不再由烟农端创建）
-- ----------------------------------------------------------
DROP TABLE IF EXISTS `reservations`;
CREATE TABLE `reservations` (
  `id`              VARCHAR(30) NOT NULL COMMENT '预约单号',
  `user_id`         VARCHAR(20) NOT NULL COMMENT '烟农用户ID',
  `oven_id`         VARCHAR(30) NOT NULL COMMENT '烤房编号',
  `baker_id`        VARCHAR(20) DEFAULT NULL COMMENT '烘烤师ID',
  `status`          ENUM('pending','confirmed','baking','completed','cancelled') NOT NULL DEFAULT 'pending' COMMENT '状态',
  `plan_start_time` DATETIME NOT NULL COMMENT '计划开始时间',
  `duration_days`   SMALLINT UNSIGNED NOT NULL DEFAULT 365 COMMENT '计划烘烤天数',
  `season_year`     SMALLINT UNSIGNED NOT NULL COMMENT '预约年度/烘烤季',
  `season_name`     VARCHAR(30) NOT NULL COMMENT '预约季名称',
  `tobacco_weight`  DECIMAL(8,2) DEFAULT NULL COMMENT '烟叶重量(kg)',
  `tobacco_type`    VARCHAR(50) DEFAULT NULL COMMENT '烟叶品种',
  `remark`          VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `confirm_time`    DATETIME DEFAULT NULL COMMENT '确认时间',
  `actual_start`    DATETIME DEFAULT NULL COMMENT '实际开始时间',
  `actual_end`      DATETIME DEFAULT NULL COMMENT '实际结束时间',
  `cancel_reason`   VARCHAR(200) DEFAULT NULL COMMENT '取消原因',
  `created_at`      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user` (`user_id`),
  KEY `idx_oven` (`oven_id`),
  KEY `idx_status` (`status`),
  KEY `idx_plan_start` (`plan_start_time`),
  KEY `idx_season` (`season_year`),
  UNIQUE KEY `uk_user_oven_season` (`user_id`, `oven_id`, `season_year`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='预约记录表';

-- ----------------------------------------------------------
-- 9. 烘烤记录表 (无冗余oven_name，通过oven_id关联)
-- ----------------------------------------------------------
DROP TABLE IF EXISTS `baking_records`;
CREATE TABLE `baking_records` (
  `id`              INT UNSIGNED NOT NULL AUTO_INCREMENT,
  `reservation_id`  VARCHAR(30) DEFAULT NULL COMMENT '关联预约单号',
  `user_id`         VARCHAR(20) NOT NULL COMMENT '烟农用户ID',
  `oven_id`         VARCHAR(30) NOT NULL COMMENT '烤房编号',
  `baker_id`        VARCHAR(20) DEFAULT NULL COMMENT '烘烤师ID',
  `season_year`     SMALLINT UNSIGNED NOT NULL COMMENT '烘烤年度/烘烤季',
  `season_name`     VARCHAR(30) NOT NULL COMMENT '烘烤季名称',
  `start_time`      DATETIME NOT NULL COMMENT '开始时间',
  `end_time`        DATETIME DEFAULT NULL COMMENT '结束时间',
  `status`          ENUM('baking','completed','aborted') NOT NULL DEFAULT 'baking' COMMENT '状态',
  `quality`         ENUM('excellent','good','normal','poor') DEFAULT NULL COMMENT '品质评估',
  `created_at`      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_reservation` (`reservation_id`),
  KEY `idx_user` (`user_id`),
  KEY `idx_oven` (`oven_id`),
  KEY `idx_baker` (`baker_id`),
  KEY `idx_status` (`status`),
  KEY `idx_end_time` (`end_time`),
  UNIQUE KEY `uk_oven_season` (`oven_id`, `season_year`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='烘烤记录表';

-- ----------------------------------------------------------
-- 10. 评价反馈表 (无冗余oven_name)
-- ----------------------------------------------------------
DROP TABLE IF EXISTS `evaluations`;
CREATE TABLE `evaluations` (
  `id`                  INT UNSIGNED NOT NULL AUTO_INCREMENT,
  `baking_record_id`    INT UNSIGNED DEFAULT NULL COMMENT '关联烘烤记录ID',
  `reservation_id`      VARCHAR(30) DEFAULT NULL COMMENT '关联年度预约单号',
  `assignment_id`       VARCHAR(30) DEFAULT NULL COMMENT '关联烤房分配单号',
  `user_id`             VARCHAR(20) NOT NULL COMMENT '烟农用户ID',
  `oven_id`             VARCHAR(30) NOT NULL COMMENT '烤房编号',
  `baker_id`            VARCHAR(20) DEFAULT NULL COMMENT '烘烤师ID',
  `overall_rating`      TINYINT UNSIGNED NOT NULL COMMENT '总体评分 1-5星',
  `baker_rating`        TINYINT UNSIGNED DEFAULT NULL COMMENT '烘烤师评分 1-5星',
  `temp_control_rating` TINYINT UNSIGNED DEFAULT NULL COMMENT '温湿度控制评分 1-5星',
  `equipment_rating`    TINYINT UNSIGNED DEFAULT NULL COMMENT '设备完好度评分 1-5星',
  `comment`             TEXT DEFAULT NULL COMMENT '烤房评价内容',
  `baker_comment`       TEXT DEFAULT NULL COMMENT '烘烤师评价内容',
  `images`              JSON DEFAULT NULL COMMENT '图片URL列表',
  `evaluate_time`       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '评价时间',
  `created_at`          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_baking_record` (`baking_record_id`),
  KEY `idx_reservation` (`reservation_id`),
  KEY `idx_assignment` (`assignment_id`),
  KEY `idx_user` (`user_id`),
  KEY `idx_oven` (`oven_id`),
  KEY `idx_baker` (`baker_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评价反馈表';

-- ============================================================
-- 初始化数据
-- ============================================================

-- 磅组
INSERT INTO `pound_groups` (`id`, `name`, `sort_order`) VALUES
(1, '磨坪磅组', 1),
(2, '梅家河磅组', 2),
(3, '柏家坪磅组', 3);

-- 烟农用户
INSERT INTO `users` (`id`, `name`, `phone`, `credit_level`, `credit_score`, `area`, `pound_group_id`, `planting_area`, `planting_years`, `total_bakes`) VALUES
('YN001', '张三', '138****8888', 'A', 95, '宜昌市秭归县', 1, 12.5, 8, 156);

-- 烘烤师
INSERT INTO `bakers` (`id`, `name`, `experience`, `rating`, `total_orders`, `pound_group_id`) VALUES
('BK001', '李师傅', 10, 4.8, 156, 1),
('BK002', '王师傅', 8, 4.6, 132, 1),
('BK003', '张师傅', 6, 4.5, 89, 3);

-- 烤房 (无name字段，地址为"市-县-乡-村"格式)
INSERT INTO `ovens` (`id`, `pound_group_id`, `address`, `latitude`, `longitude`, `distance`, `status`, `health_score`, `temperature`, `humidity`, `baker_id`, `usage_count`) VALUES
('10-420527KF00001', 1, '宜昌市秭归县两河口香龙', 30.8295, 110.9750, 500, 'idle', 92, 25.0, 65.0, 'BK001', 156),
('10-420527KF00002', 1, '宜昌市秭归县两河口香龙', 30.8300, 110.9760, 800, 'waiting', 88, 23.0, 60.0, 'BK002', 132),
('10-420527KF00003', 2, '宜昌市兴山县南阳镇龙门村', 31.2310, 110.7670, 1200, 'baking', 78, 65.0, 45.0, 'BK001', 98),
('10-420527KF00004', 2, '宜昌市兴山县南阳镇龙门村', 31.2320, 110.7680, 1500, 'maintenance', 55, 20.0, 50.0, NULL, 67),
('10-420527KF00005', 3, '宜昌市长阳县渔峡口镇西坪村', 30.4830, 110.8690, 2000, 'reserved', 95, 22.0, 55.0, 'BK003', 89),
('10-420527KF00006', 3, '宜昌市长阳县渔峡口镇西坪村', 30.4840, 110.8700, 2500, 'idle', 85, 24.0, 62.0, 'BK002', 112);

-- 设备部件
INSERT INTO `oven_components` (`oven_id`, `component_key`, `component_name`, `status`, `value`) VALUES
('10-420527KF00001', 'heater', '加热设备', 'normal', '正常运行'),
('10-420527KF00001', 'fan', '风机', 'normal', '转速: 1200rpm'),
('10-420527KF00001', 'controller', '自控仪', 'normal', '版本: V3.2'),
('10-420527KF00001', 'sensor', '温湿度传感器', 'normal', '校准完成'),
('10-420527KF00002', 'heater', '加热设备', 'normal', '正常运行'),
('10-420527KF00002', 'fan', '风机', 'warning', '转速偏低'),
('10-420527KF00002', 'controller', '自控仪', 'normal', '版本: V3.0'),
('10-420527KF00002', 'sensor', '温湿度传感器', 'normal', '校准完成'),
('10-420527KF00003', 'heater', '加热设备', 'normal', '温度: 65°C'),
('10-420527KF00003', 'fan', '风机', 'normal', '转速: 1500rpm'),
('10-420527KF00003', 'controller', '自控仪', 'normal', '自动模式'),
('10-420527KF00003', 'sensor', '温湿度传感器', 'normal', '实时监控中'),
('10-420527KF00004', 'heater', '加热设备', 'fault', '待维修'),
('10-420527KF00004', 'fan', '风机', 'normal', '正常'),
('10-420527KF00004', 'controller', '自控仪', 'normal', '待机'),
('10-420527KF00004', 'sensor', '温湿度传感器', 'missing', '缺失'),
('10-420527KF00005', 'heater', '加热设备', 'normal', '待机状态'),
('10-420527KF00005', 'fan', '风机', 'normal', '待机'),
('10-420527KF00005', 'controller', '自控仪', 'normal', '版本: V3.3'),
('10-420527KF00005', 'sensor', '温湿度传感器', 'normal', '正常'),
('10-420527KF00006', 'heater', '加热设备', 'normal', '正常运行'),
('10-420527KF00006', 'fan', '风机', 'normal', '转速: 1300rpm'),
('10-420527KF00006', 'controller', '自控仪', 'normal', '版本: V3.1'),
('10-420527KF00006', 'sensor', '温湿度传感器', 'normal', '校准完成');

-- 种植档案
INSERT INTO `planting_records` (`user_id`, `year`, `season`, `area`, `yield`, `quality`) VALUES
('YN001', 2024, '春季', 12.5, 5800, '优良'),
('YN001', 2023, '春季', 10.0, 4800, '良好'),
('YN001', 2022, '秋季', 8.0, 4200, '良好');

-- 收藏
INSERT INTO `user_favorites` (`user_id`, `oven_id`) VALUES
('YN001', '10-420527KF00001'),
('YN001', '10-420527KF00002');

-- 后台分配给烟农的本季烤房
INSERT INTO `oven_assignments` (`id`, `user_id`, `oven_id`, `baker_id`, `season_year`, `season_name`, `status`, `assigned_by`, `remark`) VALUES
('ASG2026001', 'YN001', '10-420527KF00001', 'BK001', 2026, '2026年度烘烤季', 'active', '后台管理员', '后台分配样例'),
('ASG2026002', 'YN001', '10-420527KF00002', 'BK002', 2026, '2026年度烘烤季', 'active', '后台管理员', '后台分配样例'),
('ASG2026003', 'YN001', '09-420527KF00104', 'BK001', 2026, '2026年度烘烤季', 'active', '后台管理员', '后台分配样例'),
('ASG2025001', 'YN001', '10-420527KF00005', 'BK003', 2025, '2025年度烘烤季', 'completed', '后台管理员', '历史分配样例');

-- 烘烤记录：每个烤房每年度烘烤季一条记录
INSERT INTO `baking_records` (`id`, `user_id`, `oven_id`, `baker_id`, `season_year`, `season_name`, `start_time`, `end_time`, `status`, `quality`) VALUES
(1, 'YN001', '10-420527KF00001', 'BK001', 2025, '2025年度烘烤季', '2025-01-01 00:00:00', '2025-12-31 23:59:59', 'completed', 'excellent'),
(2, 'YN001', '10-420527KF00001', 'BK001', 2024, '2024年度烘烤季', '2024-01-01 00:00:00', '2024-12-31 23:59:59', 'completed', 'good');

-- 评价反馈 (无oven_name冗余字段)
INSERT INTO `evaluations` (`baking_record_id`, `user_id`, `oven_id`, `baker_id`, `overall_rating`, `baker_rating`, `temp_control_rating`, `equipment_rating`, `comment`, `baker_comment`, `evaluate_time`) VALUES
(1, 'YN001', '10-420527KF00001', 'BK001', 5, 5, 5, 5, '烤房设备很好，温湿度控制精准，李师傅服务态度也很好！', '技术精湛，操作熟练，非常满意！', '2025-06-15 15:00:00'),
(2, 'YN001', '10-420527KF00003', 'BK002', 4, 4, 4, 4, '整体不错，温湿度控制稍微有点波动', '服务周到，响应及时', '2025-06-10 10:00:00'),
(3, 'YN001', '10-420527KF00002', 'BK002', 5, 5, 5, 5, '设备完好度高，烘烤过程稳定', '操作规范，服务热情', '2025-06-22 17:00:00');
