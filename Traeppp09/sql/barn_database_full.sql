-- =============================================
-- 烤房管理系统 - 完整数据库初始化脚本
-- 基于基础数据Excel文件生成
-- 数据库: MySQL 8.0+
-- 创建时间: 2026-06-11
-- =============================================

USE `barn_management`;

-- =============================================
-- 【一、区域字典表】
-- =============================================

-- 1. 市表 (t_city) - 市级区域信息
DROP TABLE IF EXISTS `t_city`;
CREATE TABLE `t_city` (
  `city_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '市ID',
  `city_code` VARCHAR(10) NOT NULL COMMENT '市编码',
  `city_name` VARCHAR(50) NOT NULL COMMENT '市名称',
  `dept_name` VARCHAR(100) DEFAULT NULL COMMENT '关联市级部门',
  `status` VARCHAR(10) DEFAULT '启用' COMMENT '状态',
  `applicant` VARCHAR(50) DEFAULT NULL COMMENT '申请人',
  `apply_time` DATETIME DEFAULT NULL COMMENT '申请时间',
  `update_time` DATETIME DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`city_id`),
  UNIQUE KEY `uk_city_code` (`city_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='市表';

-- 2. 县区表 (t_county) - 县区信息
DROP TABLE IF EXISTS `t_county`;
CREATE TABLE `t_county` (
  `county_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '县区ID',
  `county_code` VARCHAR(10) NOT NULL COMMENT '县区编码',
  `county_name` VARCHAR(50) NOT NULL COMMENT '县区名称',
  `city_code` VARCHAR(10) NOT NULL COMMENT '所属市编码',
  `first_letter` VARCHAR(10) DEFAULT NULL COMMENT '首字母',
  `dept_name` VARCHAR(100) DEFAULT NULL COMMENT '关联部门',
  `status` VARCHAR(10) DEFAULT '启用' COMMENT '状态',
  `applicant` VARCHAR(50) DEFAULT NULL COMMENT '申请人',
  `apply_time` DATETIME DEFAULT NULL COMMENT '申请时间',
  `update_time` DATETIME DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`county_id`),
  UNIQUE KEY `uk_county_code` (`county_code`),
  KEY `idx_city_code` (`city_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='县区表';

-- 3. 乡镇表 (t_township) - 乡镇信息
DROP TABLE IF EXISTS `t_township`;
CREATE TABLE `t_township` (
  `township_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '乡镇ID',
  `township_code` VARCHAR(20) NOT NULL COMMENT '乡镇编码',
  `township_name` VARCHAR(50) NOT NULL COMMENT '乡镇名称',
  `county_code` VARCHAR(10) NOT NULL COMMENT '所属县区编码',
  `first_letter` VARCHAR(10) DEFAULT NULL COMMENT '首字母',
  `purchase_station` VARCHAR(100) DEFAULT NULL COMMENT '关联收购站',
  `status` VARCHAR(10) DEFAULT '启用' COMMENT '状态',
  `applicant` VARCHAR(50) DEFAULT NULL COMMENT '申请人',
  `apply_time` DATETIME DEFAULT NULL COMMENT '申请时间',
  `update_time` DATETIME DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`township_id`),
  UNIQUE KEY `uk_township_code` (`township_code`),
  KEY `idx_county_code` (`county_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='乡镇表';

-- 4. 村表 (t_village) - 村信息
DROP TABLE IF EXISTS `t_village`;
CREATE TABLE `t_village` (
  `village_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '村ID',
  `village_code` VARCHAR(20) NOT NULL COMMENT '村编码',
  `village_name` VARCHAR(50) NOT NULL COMMENT '村名称',
  `township_code` VARCHAR(20) NOT NULL COMMENT '所属乡镇编码',
  `first_letter` VARCHAR(10) DEFAULT NULL COMMENT '首字母',
  `status` VARCHAR(10) DEFAULT '启用' COMMENT '状态',
  `applicant` VARCHAR(50) DEFAULT NULL COMMENT '申请人',
  `apply_time` DATETIME DEFAULT NULL COMMENT '申请时间',
  `update_time` DATETIME DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`village_id`),
  UNIQUE KEY `uk_village_code` (`village_code`),
  KEY `idx_township_code` (`township_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='村表';

-- 5. 收购站表 (t_purchase_station) - 烟叶收购站信息
DROP TABLE IF EXISTS `t_purchase_station`;
CREATE TABLE `t_purchase_station` (
  `station_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '收购站ID',
  `station_code` VARCHAR(20) NOT NULL COMMENT '收购站编码',
  `station_name` VARCHAR(100) NOT NULL COMMENT '收购站名称',
  `township_code` VARCHAR(20) DEFAULT NULL COMMENT '所属乡镇编码',
  `county_code` VARCHAR(10) DEFAULT NULL COMMENT '所属县区编码',
  `status` VARCHAR(10) DEFAULT '启用' COMMENT '状态',
  `applicant` VARCHAR(50) DEFAULT NULL COMMENT '申请人',
  `apply_time` DATETIME DEFAULT NULL COMMENT '申请时间',
  `update_time` DATETIME DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`station_id`),
  UNIQUE KEY `uk_station_code` (`station_code`),
  KEY `idx_township_code` (`township_code`),
  KEY `idx_county_code` (`county_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='收购站表';

-- =============================================
-- 【二、烤房基础表】
-- =============================================

-- 6. 项目类型表 (t_project_type) - 烤房项目类型
DROP TABLE IF EXISTS `t_project_type`;
CREATE TABLE `t_project_type` (
  `type_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '类型ID',
  `type_name` VARCHAR(50) NOT NULL COMMENT '项目类型名称',
  `sort_order` VARCHAR(10) DEFAULT NULL COMMENT '排序号',
  `status` VARCHAR(10) DEFAULT '启用' COMMENT '状态',
  `applicant` VARCHAR(50) DEFAULT NULL COMMENT '申请人',
  `apply_time` DATETIME DEFAULT NULL COMMENT '申请时间',
  `update_time` DATETIME DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`type_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目类型表';

-- 7. 烤房项目基础信息表 (t_barn_project) - 烤房项目核心表
DROP TABLE IF EXISTS `t_barn_project`;
CREATE TABLE `t_barn_project` (
  `project_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '项目ID',
  `project_code` VARCHAR(50) NOT NULL COMMENT '项目编号',
  `use_status` VARCHAR(20) DEFAULT '在用' COMMENT '使用状态（在用/闲置/转用/损毁）',
  `idle_years` INT DEFAULT 0 COMMENT '闲置年份数',
  `transfer_year` INT DEFAULT NULL COMMENT '转用年度',
  `transfer_use` VARCHAR(100) DEFAULT NULL COMMENT '转用用途',
  `damage_year` INT DEFAULT NULL COMMENT '损毁年度',
  `damage_reason` VARCHAR(200) DEFAULT NULL COMMENT '损毁原因',
  `build_mode` VARCHAR(50) DEFAULT NULL COMMENT '建设方式（行业投入/政府投入/自建）',
  `project_type` VARCHAR(50) DEFAULT NULL COMMENT '项目类型',
  `length_m` DECIMAL(8,2) DEFAULT NULL COMMENT '长（米）',
  `width_m` DECIMAL(8,2) DEFAULT NULL COMMENT '宽（米）',
  `height_m` DECIMAL(8,2) DEFAULT NULL COMMENT '高（米）',
  `project_cost` DECIMAL(12,2) DEFAULT NULL COMMENT '工程造价（元）',
  `subsidy_national` DECIMAL(12,2) DEFAULT NULL COMMENT '国家局补贴（元）',
  `subsidy_region` DECIMAL(12,2) DEFAULT NULL COMMENT '产区补贴（元）',
  `subsidy_total` DECIMAL(12,2) DEFAULT NULL COMMENT '补贴总额（元）',
  `start_date` DATE DEFAULT NULL COMMENT '开工时间',
  `complete_date` DATE DEFAULT NULL COMMENT '竣工时间',
  `city_name` VARCHAR(50) DEFAULT NULL COMMENT '市名称',
  `city_code` VARCHAR(10) DEFAULT NULL COMMENT '市编码',
  `county_name` VARCHAR(50) DEFAULT NULL COMMENT '县区名称',
  `county_code` VARCHAR(10) DEFAULT NULL COMMENT '县区编码',
  `township_name` VARCHAR(50) DEFAULT NULL COMMENT '乡镇名称',
  `township_code` VARCHAR(20) DEFAULT NULL COMMENT '乡镇编码',
  `village_name` VARCHAR(50) DEFAULT NULL COMMENT '村名称',
  `village_code` VARCHAR(20) DEFAULT NULL COMMENT '村编码',
  `address` VARCHAR(255) DEFAULT NULL COMMENT '详细地址',
  `project_owner` VARCHAR(100) DEFAULT NULL COMMENT '项目业主',
  `construction_unit` VARCHAR(100) DEFAULT NULL COMMENT '施工单位',
  `elevation` DECIMAL(10,2) DEFAULT NULL COMMENT '海拔（米）',
  `longitude` DECIMAL(10,6) DEFAULT NULL COMMENT '经度',
  `latitude` DECIMAL(10,6) DEFAULT NULL COMMENT '纬度',
  `has_heating` TINYINT DEFAULT 0 COMMENT '加热设备是否填写',
  `has_radiator` TINYINT DEFAULT 0 COMMENT '散热器是否填写',
  `has_autocontrol` TINYINT DEFAULT 0 COMMENT '自控设备是否填写',
  `has_main` TINYINT DEFAULT 0 COMMENT '烤房主体是否填写',
  `has_ancillary` TINYINT DEFAULT 0 COMMENT '附属设施是否填写',
  `station_code` VARCHAR(20) DEFAULT NULL COMMENT '关联收购站编码',
  `health_score` DECIMAL(5,2) DEFAULT 100.00 COMMENT '健康评分（0-100）',
  `health_level` VARCHAR(20) DEFAULT '优良' COMMENT '健康等级',
  `is_idle` TINYINT DEFAULT 1 COMMENT '是否空闲（0否1是）',
  `status` VARCHAR(20) DEFAULT '待审核' COMMENT '流程状态',
  `applicant` VARCHAR(50) DEFAULT NULL COMMENT '申请人',
  `apply_time` DATETIME DEFAULT NULL COMMENT '申请时间',
  `update_time` DATETIME DEFAULT NULL COMMENT '更新时间',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`project_id`),
  UNIQUE KEY `uk_project_code` (`project_code`),
  KEY `idx_county_code` (`county_code`),
  KEY `idx_township_code` (`township_code`),
  KEY `idx_health_level` (`health_level`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='烤房项目基础信息表';

-- =============================================
-- 【三、烤房部件表】
-- =============================================

-- 8. 烤房主体表 (t_barn_main) - 烤房主体结构（墙体屋顶/挂烟梁柱/装烟室门）
DROP TABLE IF EXISTS `t_barn_main`;
CREATE TABLE `t_barn_main` (
  `main_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主体ID',
  `project_code` VARCHAR(50) NOT NULL COMMENT '关联项目编号',
  `main_code` VARCHAR(50) DEFAULT NULL COMMENT '烤房主体编码',
  -- 墙体屋顶
  `wall_status` VARCHAR(20) DEFAULT '正常' COMMENT '墙体屋顶-状态',
  `wall_repair_year` INT DEFAULT NULL COMMENT '墙体屋顶-最新修复改造年度',
  `wall_normal_photo` VARCHAR(500) DEFAULT NULL COMMENT '墙体屋顶-正常设备全景照片',
  `wall_damage_photo` VARCHAR(500) DEFAULT NULL COMMENT '墙体屋顶-损坏设备全景照片',
  `wall_damage_detail` VARCHAR(500) DEFAULT NULL COMMENT '墙体屋顶-损坏设备瑕疵细节图',
  `wall_missing_photo` VARCHAR(500) DEFAULT NULL COMMENT '墙体屋顶-缺失设备拍摄全景图',
  -- 挂烟梁柱
  `beam_status` VARCHAR(20) DEFAULT '正常' COMMENT '挂烟梁柱-状态',
  `beam_repair_year` INT DEFAULT NULL COMMENT '挂烟梁柱-最新修复改造年度',
  `beam_normal_photo` VARCHAR(500) DEFAULT NULL COMMENT '挂烟梁柱-正常设备全景照片',
  `beam_damage_photo` VARCHAR(500) DEFAULT NULL COMMENT '挂烟梁柱-损坏设备全景照片',
  `beam_damage_detail` VARCHAR(500) DEFAULT NULL COMMENT '挂烟梁柱-损坏设备瑕疵细节图',
  `beam_missing_photo` VARCHAR(500) DEFAULT NULL COMMENT '挂烟梁柱-缺失设备拍摄全景图',
  -- 装烟室门
  `door_status` VARCHAR(20) DEFAULT '正常' COMMENT '装烟室门-状态',
  `door_repair_year` INT DEFAULT NULL COMMENT '装烟室门-最新修复改造年度',
  `door_normal_photo` VARCHAR(500) DEFAULT NULL COMMENT '装烟室门-正常设备全景照片',
  `door_damage_photo` VARCHAR(500) DEFAULT NULL COMMENT '装烟室门-损坏设备全景照片',
  `door_damage_detail` VARCHAR(500) DEFAULT NULL COMMENT '装烟室门-损坏设备瑕疵细节图',
  `door_missing_photo` VARCHAR(500) DEFAULT NULL COMMENT '装烟室门-缺失设备拍摄全景图',
  -- 评价
  `eval_score` INT DEFAULT NULL COMMENT '评价选择',
  `has_linked_code` VARCHAR(10) DEFAULT '否' COMMENT '是否已存在关联编号',
  `status` VARCHAR(20) DEFAULT '待审核' COMMENT '流程状态',
  `applicant` VARCHAR(50) DEFAULT NULL COMMENT '申请人',
  `apply_time` DATETIME DEFAULT NULL COMMENT '申请时间',
  `update_time` DATETIME DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`main_id`),
  KEY `idx_project_code` (`project_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='烤房主体表';

-- 9. 加热设备表 (t_heating_equip) - 加热设备信息
DROP TABLE IF EXISTS `t_heating_equip`;
CREATE TABLE `t_heating_equip` (
  `heating_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '加热设备ID',
  `project_code` VARCHAR(50) NOT NULL COMMENT '关联项目编号',
  `heating_code` VARCHAR(50) DEFAULT NULL COMMENT '加热设备编码',
  `project_cost` DECIMAL(12,2) DEFAULT NULL COMMENT '工程造价（元）',
  `subsidy_total` DECIMAL(12,2) DEFAULT NULL COMMENT '补贴总额（元）',
  `subsidy_national` DECIMAL(12,2) DEFAULT NULL COMMENT '国家局补贴（元）',
  `subsidy_region` DECIMAL(12,2) DEFAULT NULL COMMENT '产区补贴（元）',
  `build_year` INT DEFAULT NULL COMMENT '年度',
  `construction_unit` VARCHAR(100) DEFAULT NULL COMMENT '施工（供应）单位',
  `has_burner_link` VARCHAR(10) DEFAULT '否' COMMENT '是否关联燃烧机编码',
  `burner_code` VARCHAR(50) DEFAULT NULL COMMENT '燃烧机编码',
  `heating_type` VARCHAR(50) DEFAULT NULL COMMENT '加热设备类型',
  `longitude` DECIMAL(10,6) DEFAULT NULL COMMENT '经度',
  `latitude` DECIMAL(10,6) DEFAULT NULL COMMENT '纬度',
  `status` VARCHAR(20) DEFAULT '待审核' COMMENT '流程状态',
  `applicant` VARCHAR(50) DEFAULT NULL COMMENT '申请人',
  `apply_time` DATETIME DEFAULT NULL COMMENT '申请时间',
  `update_time` DATETIME DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`heating_id`),
  KEY `idx_project_code` (`project_code`),
  KEY `idx_burner_code` (`burner_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='加热设备表';

-- 10. 散热器表 (t_radiator) - 散热器信息（散热管/炉堂/清灰门/烟囱）
DROP TABLE IF EXISTS `t_radiator`;
CREATE TABLE `t_radiator` (
  `radiator_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '散热器ID',
  `project_code` VARCHAR(50) NOT NULL COMMENT '关联项目编号',
  `radiator_code` VARCHAR(50) DEFAULT NULL COMMENT '散热器编码',
  -- 散热管
  `pipe_status` VARCHAR(20) DEFAULT '正常' COMMENT '散热管-状态',
  `pipe_repair_year` INT DEFAULT NULL COMMENT '散热管-最新修复改造年度',
  `pipe_normal_photo` VARCHAR(500) DEFAULT NULL COMMENT '散热管-正常设备全景照片',
  `pipe_damage_photo` VARCHAR(500) DEFAULT NULL COMMENT '散热管-损坏设备全景照片',
  `pipe_damage_detail` VARCHAR(500) DEFAULT NULL COMMENT '散热管-损坏设备瑕疵细节图',
  `pipe_missing_photo` VARCHAR(500) DEFAULT NULL COMMENT '散热管-缺失设备拍摄全景图',
  -- 炉堂
  `furnace_status` VARCHAR(20) DEFAULT '正常' COMMENT '炉堂-状态',
  `furnace_repair_year` INT DEFAULT NULL COMMENT '炉堂-最新修复改造年度',
  `furnace_normal_photo` VARCHAR(500) DEFAULT NULL COMMENT '炉堂-正常设备全景照片',
  `furnace_damage_photo` VARCHAR(500) DEFAULT NULL COMMENT '炉堂-损坏设备全景照片',
  `furnace_damage_detail` VARCHAR(500) DEFAULT NULL COMMENT '炉堂-损坏设备瑕疵细节图',
  `furnace_missing_photo` VARCHAR(500) DEFAULT NULL COMMENT '炉堂-缺失设备拍摄全景图',
  -- 清灰门
  `clean_door_status` VARCHAR(20) DEFAULT '正常' COMMENT '清灰门-状态',
  `clean_door_repair_year` INT DEFAULT NULL COMMENT '清灰门-最新修复改造年度',
  `clean_door_normal_photo` VARCHAR(500) DEFAULT NULL COMMENT '清灰门-正常设备全景照片',
  `clean_door_damage_photo` VARCHAR(500) DEFAULT NULL COMMENT '清灰门-损坏设备全景照片',
  `clean_door_damage_detail` VARCHAR(500) DEFAULT NULL COMMENT '清灰门-损坏设备瑕疵细节图',
  `clean_door_missing_photo` VARCHAR(500) DEFAULT NULL COMMENT '清灰门-缺失设备拍摄全景图',
  -- 烟囱
  `chimney_status` VARCHAR(20) DEFAULT '正常' COMMENT '烟囱-状态',
  `chimney_repair_year` INT DEFAULT NULL COMMENT '烟囱-最新修复改造年度',
  `chimney_normal_photo` VARCHAR(500) DEFAULT NULL COMMENT '烟囱-正常设备全景照片',
  `chimney_damage_photo` VARCHAR(500) DEFAULT NULL COMMENT '烟囱-损坏设备全景照片',
  `chimney_damage_detail` VARCHAR(500) DEFAULT NULL COMMENT '烟囱-损坏设备瑕疵细节图',
  `chimney_missing_photo` VARCHAR(500) DEFAULT NULL COMMENT '烟囱-缺失设备拍摄全景图',
  -- 评价
  `eval_score` INT DEFAULT NULL COMMENT '评价选择',
  `has_linked_code` VARCHAR(10) DEFAULT '否' COMMENT '是否已存在关联编号',
  `status` VARCHAR(20) DEFAULT '待审核' COMMENT '流程状态',
  `applicant` VARCHAR(50) DEFAULT NULL COMMENT '申请人',
  `apply_time` DATETIME DEFAULT NULL COMMENT '申请时间',
  `update_time` DATETIME DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`radiator_id`),
  KEY `idx_project_code` (`project_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='散热器表';

-- 11. 燃烧机信息表 (t_burner) - 燃烧机独立信息
DROP TABLE IF EXISTS `t_burner`;
CREATE TABLE `t_burner` (
  `burner_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '燃烧机ID',
  `burner_code` VARCHAR(50) NOT NULL COMMENT '燃烧机编码',
  `project_code` VARCHAR(50) DEFAULT NULL COMMENT '关联项目编码',
  `heating_type` VARCHAR(50) DEFAULT NULL COMMENT '加热设备类型',
  `project_cost` DECIMAL(12,2) DEFAULT NULL COMMENT '工程造价（元）',
  `subsidy_total` DECIMAL(12,2) DEFAULT NULL COMMENT '补贴总额（元）',
  `subsidy_national` DECIMAL(12,2) DEFAULT NULL COMMENT '国家局补贴（元）',
  `subsidy_region` DECIMAL(12,2) DEFAULT NULL COMMENT '产区补贴（元）',
  `build_year` INT DEFAULT NULL COMMENT '年度',
  `construction_unit` VARCHAR(100) DEFAULT NULL COMMENT '施工（供应）单位',
  `has_project_link` VARCHAR(10) DEFAULT '否' COMMENT '是否已关联项目编码',
  `status` VARCHAR(20) DEFAULT '待审核' COMMENT '流程状态',
  `applicant` VARCHAR(50) DEFAULT NULL COMMENT '申请人',
  `apply_time` DATETIME DEFAULT NULL COMMENT '申请时间',
  `update_time` DATETIME DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`burner_id`),
  UNIQUE KEY `uk_burner_code` (`burner_code`),
  KEY `idx_project_code` (`project_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='燃烧机信息表';

-- 12. 自控设备表 (t_auto_control) - 自控设备（冷风门/排湿窗/循环风机/减速电动机/助燃鼓风机/操作箱/水壶/探头）
DROP TABLE IF EXISTS `t_auto_control`;
CREATE TABLE `t_auto_control` (
  `autoid` BIGINT NOT NULL AUTO_INCREMENT COMMENT '自控设备ID',
  `project_code` VARCHAR(50) NOT NULL COMMENT '关联项目编号',
  `autocode` VARCHAR(50) DEFAULT NULL COMMENT '自控设备编码',
  -- 冷风门
  `airdoor_status` VARCHAR(20) DEFAULT '正常' COMMENT '冷风门-状态',
  `airdoor_repair_year` INT DEFAULT NULL COMMENT '冷风门-最新修复改造年度',
  `airdoor_normal_photo` VARCHAR(500) DEFAULT NULL COMMENT '冷风门-正常设备全景照片',
  `airdoor_damage_photo` VARCHAR(500) DEFAULT NULL COMMENT '冷风门-损坏设备全景照片',
  `airdoor_damage_detail` VARCHAR(500) DEFAULT NULL COMMENT '冷风门-损坏设备瑕疵细节图',
  `airdoor_missing_photo` VARCHAR(500) DEFAULT NULL COMMENT '冷风门-缺失设备拍摄全景图',
  -- 排湿窗
  `vent_status` VARCHAR(20) DEFAULT '正常' COMMENT '排湿窗-状态',
  `vent_repair_year` INT DEFAULT NULL COMMENT '排湿窗-最新修复改造年度',
  `vent_normal_photo` VARCHAR(500) DEFAULT NULL COMMENT '排湿窗-正常设备全景照片',
  `vent_damage_photo` VARCHAR(500) DEFAULT NULL COMMENT '排湿窗-损坏设备全景照片',
  `vent_damage_detail` VARCHAR(500) DEFAULT NULL COMMENT '排湿窗-损坏设备瑕疵细节图',
  `vent_missing_photo` VARCHAR(500) DEFAULT NULL COMMENT '排湿窗-缺失设备拍摄全景图',
  -- 循环风机
  `fan_status` VARCHAR(20) DEFAULT '正常' COMMENT '循环风机-状态',
  `fan_repair_year` INT DEFAULT NULL COMMENT '循环风机-最新修复改造年度',
  `fan_normal_photo` VARCHAR(500) DEFAULT NULL COMMENT '循环风机-正常设备全景照片',
  `fan_damage_photo` VARCHAR(500) DEFAULT NULL COMMENT '循环风机-损坏设备全景照片',
  `fan_damage_detail` VARCHAR(500) DEFAULT NULL COMMENT '循环风机-损坏设备瑕疵细节图',
  `fan_missing_photo` VARCHAR(500) DEFAULT NULL COMMENT '循环风机-缺失设备拍摄全景图',
  -- 减速电动机
  `motor_status` VARCHAR(20) DEFAULT '正常' COMMENT '减速电动机-状态',
  `motor_repair_year` INT DEFAULT NULL COMMENT '减速电动机-最新修复改造年度',
  `motor_normal_photo` VARCHAR(500) DEFAULT NULL COMMENT '减速电动机-正常设备全景照片',
  `motor_damage_photo` VARCHAR(500) DEFAULT NULL COMMENT '减速电动机-损坏设备全景照片',
  `motor_damage_detail` VARCHAR(500) DEFAULT NULL COMMENT '减速电动机-损坏设备瑕疵细节图',
  `motor_missing_photo` VARCHAR(500) DEFAULT NULL COMMENT '减速电动机-缺失设备拍摄全景图',
  -- 助燃鼓风机
  `blower_status` VARCHAR(20) DEFAULT '正常' COMMENT '助燃鼓风机-状态',
  `blower_repair_year` INT DEFAULT NULL COMMENT '助燃鼓风机-最新修复改造年度',
  `blower_normal_photo` VARCHAR(500) DEFAULT NULL COMMENT '助燃鼓风机-正常设备全景照片',
  `blower_damage_photo` VARCHAR(500) DEFAULT NULL COMMENT '助燃鼓风机-损坏设备全景照片',
  `blower_damage_detail` VARCHAR(500) DEFAULT NULL COMMENT '助燃鼓风机-损坏设备瑕疵细节图',
  `blower_missing_photo` VARCHAR(500) DEFAULT NULL COMMENT '助燃鼓风机-缺失设备拍摄全景图',
  -- 操作箱
  `controlbox_status` VARCHAR(20) DEFAULT '正常' COMMENT '操作箱-状态',
  `controlbox_repair_year` INT DEFAULT NULL COMMENT '操作箱-最新修复改造年度',
  `controlbox_normal_photo` VARCHAR(500) DEFAULT NULL COMMENT '操作箱-正常设备全景照片',
  `controlbox_damage_photo` VARCHAR(500) DEFAULT NULL COMMENT '操作箱-损坏设备全景照片',
  `controlbox_damage_detail` VARCHAR(500) DEFAULT NULL COMMENT '操作箱-损坏设备瑕疵细节图',
  `controlbox_missing_photo` VARCHAR(500) DEFAULT NULL COMMENT '操作箱-缺失设备拍摄全景图',
  -- 水壶
  `kettle_status` VARCHAR(20) DEFAULT '正常' COMMENT '水壶-状态',
  `kettle_repair_year` INT DEFAULT NULL COMMENT '水壶-最新修复改造年度',
  `kettle_normal_photo` VARCHAR(500) DEFAULT NULL COMMENT '水壶-正常设备全景照片',
  `kettle_damage_photo` VARCHAR(500) DEFAULT NULL COMMENT '水壶-损坏设备全景照片',
  `kettle_damage_detail` VARCHAR(500) DEFAULT NULL COMMENT '水壶-损坏设备瑕疵细节图',
  `kettle_missing_photo` VARCHAR(500) DEFAULT NULL COMMENT '水壶-缺失设备拍摄全景图',
  -- 探头
  `probe_status` VARCHAR(20) DEFAULT '正常' COMMENT '探头-状态',
  `probe_repair_year` INT DEFAULT NULL COMMENT '探头-最新修复改造年度',
  `probe_normal_photo` VARCHAR(500) DEFAULT NULL COMMENT '探头-正常设备全景照片',
  `probe_damage_photo` VARCHAR(500) DEFAULT NULL COMMENT '探头-损坏设备全景照片',
  `probe_damage_detail` VARCHAR(500) DEFAULT NULL COMMENT '探头-损坏设备瑕疵细节图',
  `probe_missing_photo` VARCHAR(500) DEFAULT NULL COMMENT '探头-缺失设备拍摄全景图',
  -- 评价
  `eval_score` INT DEFAULT NULL COMMENT '评价选择',
  `has_linked_code` VARCHAR(10) DEFAULT '否' COMMENT '是否已存在关联编号',
  `status` VARCHAR(20) DEFAULT '待审核' COMMENT '流程状态',
  `applicant` VARCHAR(50) DEFAULT NULL COMMENT '申请人',
  `apply_time` DATETIME DEFAULT NULL COMMENT '申请时间',
  `update_time` DATETIME DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`autoid`),
  KEY `idx_project_code` (`project_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='自控设备表';

-- 13. 附属设施表 (t_ancillary) - 附属设施（烟夹/烟棚/电路电器）
DROP TABLE IF EXISTS `t_ancillary`;
CREATE TABLE `t_ancillary` (
  `ancillary_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '附属设施ID',
  `project_code` VARCHAR(50) NOT NULL COMMENT '关联项目编号',
  `ancillary_code` VARCHAR(50) DEFAULT NULL COMMENT '烤房附属编码',
  -- 烟夹
  `clip_use` VARCHAR(10) DEFAULT '否' COMMENT '是否使用烟夹',
  `clip_status` VARCHAR(20) DEFAULT '正常' COMMENT '烟夹-状态',
  `clip_pending_count` INT DEFAULT 0 COMMENT '待补充数量',
  -- 烟棚
  `shed_status` VARCHAR(20) DEFAULT '正常' COMMENT '烟棚-状态',
  `shed_repair_year` INT DEFAULT NULL COMMENT '烟棚-最新修复改造年度',
  `shed_normal_photo` VARCHAR(500) DEFAULT NULL COMMENT '烟棚-正常设备全景照片',
  `shed_damage_photo` VARCHAR(500) DEFAULT NULL COMMENT '烟棚-损坏设备全景照片',
  `shed_damage_detail` VARCHAR(500) DEFAULT NULL COMMENT '烟棚-损坏设备瑕疵细节图',
  `shed_missing_photo` VARCHAR(500) DEFAULT NULL COMMENT '烟棚-缺失设备拍摄全景图',
  -- 电路电器
  `electric_status` VARCHAR(20) DEFAULT '正常' COMMENT '电路电器-状态',
  `electric_repair_year` INT DEFAULT NULL COMMENT '电路电器-最新修复改造年度',
  `electric_normal_photo` VARCHAR(500) DEFAULT NULL COMMENT '电路电器-正常设备全景照片',
  `electric_damage_photo` VARCHAR(500) DEFAULT NULL COMMENT '电路电器-损坏设备全景照片',
  `electric_damage_detail` VARCHAR(500) DEFAULT NULL COMMENT '电路电器-损坏设备瑕疵细节图',
  `electric_missing_photo` VARCHAR(500) DEFAULT NULL COMMENT '电路电器-缺失设备拍摄全景图',
  -- 评价
  `eval_score` INT DEFAULT NULL COMMENT '评价选择',
  `has_linked_code` VARCHAR(10) DEFAULT '否' COMMENT '是否已存在关联编号',
  `status` VARCHAR(20) DEFAULT '待审核' COMMENT '流程状态',
  `applicant` VARCHAR(50) DEFAULT NULL COMMENT '申请人',
  `apply_time` DATETIME DEFAULT NULL COMMENT '申请时间',
  `update_time` DATETIME DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`ancillary_id`),
  KEY `idx_project_code` (`project_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='附属设施表';

-- =============================================
-- 【四、业务表（原有）】
-- =============================================

-- 维修记录表
DROP TABLE IF EXISTS `t_repair_record`;
CREATE TABLE `t_repair_record` (
  `repair_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '维修ID',
  `project_code` VARCHAR(50) NOT NULL COMMENT '项目编号',
  `repair_type` VARCHAR(20) NOT NULL COMMENT '维修类型',
  `urgency` VARCHAR(20) DEFAULT '正常' COMMENT '紧迫性',
  `applicant_id` BIGINT DEFAULT NULL COMMENT '申请人ID',
  `apply_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
  `apply_desc` VARCHAR(1000) DEFAULT NULL COMMENT '申请描述',
  `apply_photos` VARCHAR(1000) DEFAULT NULL COMMENT '申请照片',
  `estimated_cost` DECIMAL(10,2) DEFAULT NULL COMMENT '预估费用',
  `actual_cost` DECIMAL(10,2) DEFAULT NULL COMMENT '实际费用',
  `status` VARCHAR(20) DEFAULT '待审核' COMMENT '状态',
  `auditor_id` BIGINT DEFAULT NULL COMMENT '审核人ID',
  `audit_time` DATETIME DEFAULT NULL COMMENT '审核时间',
  `audit_opinion` VARCHAR(500) DEFAULT NULL COMMENT '审核意见',
  `implement_team` VARCHAR(100) DEFAULT NULL COMMENT '实施队伍',
  `start_date` DATE DEFAULT NULL COMMENT '实施开始日期',
  `end_date` DATE DEFAULT NULL COMMENT '实施结束日期',
  `completion_photos` VARCHAR(1000) DEFAULT NULL COMMENT '完工照片',
  `acceptor_id` BIGINT DEFAULT NULL COMMENT '验收人ID',
  `accept_time` DATETIME DEFAULT NULL COMMENT '验收时间',
  `accept_result` VARCHAR(20) DEFAULT NULL COMMENT '验收结果',
  `accept_opinion` VARCHAR(500) DEFAULT NULL COMMENT '验收意见',
  `roi_score` DECIMAL(5,2) DEFAULT NULL COMMENT '投入产出比评分',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`repair_id`),
  KEY `idx_project_code` (`project_code`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='维修记录表';

-- 预约记录表
DROP TABLE IF EXISTS `t_reservation`;
CREATE TABLE `t_reservation` (
  `reservation_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '预约ID',
  `project_code` VARCHAR(50) NOT NULL COMMENT '项目编号',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `reserve_start` DATETIME NOT NULL COMMENT '预约开始时间',
  `reserve_end` DATETIME NOT NULL COMMENT '预约结束时间',
  `status` VARCHAR(20) DEFAULT '待审核' COMMENT '状态',
  `reviewer_id` BIGINT DEFAULT NULL COMMENT '审核人ID',
  `review_time` DATETIME DEFAULT NULL COMMENT '审核时间',
  `actual_start` DATETIME DEFAULT NULL COMMENT '实际开始时间',
  `actual_end` DATETIME DEFAULT NULL COMMENT '实际结束时间',
  `leaf_weight` DECIMAL(8,2) DEFAULT NULL COMMENT '烟叶重量（kg）',
  `is_timeout` TINYINT DEFAULT 0 COMMENT '是否超时',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`reservation_id`),
  KEY `idx_project_code` (`project_code`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='预约记录表';

-- 评价表
DROP TABLE IF EXISTS `t_evaluation`;
CREATE TABLE `t_evaluation` (
  `evaluation_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '评价ID',
  `reservation_id` BIGINT DEFAULT NULL COMMENT '预约ID',
  `project_code` VARCHAR(50) NOT NULL COMMENT '项目编号',
  `evaluator_id` BIGINT NOT NULL COMMENT '评价人ID',
  `baker_id` BIGINT DEFAULT NULL COMMENT '烘烤师ID',
  `barn_score` INT DEFAULT 5 COMMENT '烤房评分（1-5星）',
  `barn_comment` VARCHAR(500) DEFAULT NULL COMMENT '烤房评价内容',
  `baker_score` INT DEFAULT 5 COMMENT '烘烤师评分（1-5星）',
  `baker_comment` VARCHAR(500) DEFAULT NULL COMMENT '烘烤师评价内容',
  `evaluate_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '评价时间',
  PRIMARY KEY (`evaluation_id`),
  KEY `idx_project_code` (`project_code`),
  KEY `idx_reservation_id` (`reservation_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评价表';

-- 管护队伍表
DROP TABLE IF EXISTS `t_maintenance_team`;
CREATE TABLE `t_maintenance_team` (
  `team_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '队伍ID',
  `team_name` VARCHAR(100) NOT NULL COMMENT '队伍名称',
  `team_type` VARCHAR(50) NOT NULL COMMENT '队伍类型',
  `county_code` VARCHAR(10) DEFAULT NULL COMMENT '所属县区编码',
  `leader_name` VARCHAR(50) DEFAULT NULL COMMENT '负责人',
  `leader_phone` VARCHAR(20) DEFAULT NULL COMMENT '联系电话',
  `member_count` INT DEFAULT 0 COMMENT '人数',
  `service_area` VARCHAR(255) DEFAULT NULL COMMENT '服务区域',
  `status` TINYINT DEFAULT 0 COMMENT '状态（0正常1停用）',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`team_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='管护队伍表';

-- 资金管理表
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
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`fund_id`),
  KEY `idx_year` (`year`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='资金管理表';

-- 用户扩展信息表
DROP TABLE IF EXISTS `t_user_ext`;
CREATE TABLE `t_user_ext` (
  `user_ext_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '扩展信息ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `real_name` VARCHAR(50) DEFAULT NULL COMMENT '真实姓名',
  `phone` VARCHAR(20) DEFAULT NULL COMMENT '手机号',
  `role_type` VARCHAR(20) DEFAULT NULL COMMENT '角色类型',
  `county_code` VARCHAR(10) DEFAULT NULL COMMENT '所属县区',
  `township_code` VARCHAR(20) DEFAULT NULL COMMENT '所属乡镇',
  `credit_score` DECIMAL(5,2) DEFAULT 100.00 COMMENT '信用评分',
  `unique_id` VARCHAR(32) DEFAULT NULL COMMENT '唯一标识ID',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`user_ext_id`),
  UNIQUE KEY `uk_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户扩展信息表';

-- =============================================
-- 【五、视图】
-- =============================================

-- 烤房健康统计视图
CREATE OR REPLACE VIEW `v_barn_health_stats` AS
SELECT 
  county_name,
  county_code,
  COUNT(*) as total_count,
  SUM(CASE WHEN health_level = '优良' THEN 1 ELSE 0 END) as excellent_count,
  SUM(CASE WHEN health_level = '需维护' THEN 1 ELSE 0 END) as maintain_count,
  SUM(CASE WHEN health_level = '急需修复' THEN 1 ELSE 0 END) as urgent_count,
  SUM(CASE WHEN health_level = '退出' THEN 1 ELSE 0 END) as retired_count,
  AVG(health_score) as avg_score
FROM t_barn_project
GROUP BY county_name, county_code;

-- 烤房区域分布视图
CREATE OR REPLACE VIEW `v_barn_region_stats` AS
SELECT 
  city_name,
  county_name,
  township_name,
  COUNT(*) as barn_count,
  AVG(health_score) as avg_health_score
FROM t_barn_project
GROUP BY city_name, county_name, township_name;

SELECT '表结构创建完成' as message;