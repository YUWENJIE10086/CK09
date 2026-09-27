-- =============================================
-- 烤房管理系统 - 数据库完整重建脚本（RuoYi标准）
-- 数据库: MySQL 5.7+
-- 创建时间: 2026-06-11
-- =============================================

-- 清理旧表（保留有真实数据的表，后续统一重命名）
DROP TABLE IF EXISTS barn_body, barn_burner, barn_control, barn_facility,
  barn_heating, barn_project_info, barn_project_type, barn_radiator,
  sys_city, sys_county, sys_town, sys_village,
  t_ancillary, t_auto_control, t_barn_component, t_barn_info, t_barn_main,
  t_burner, t_city, t_county, t_evaluation, t_fund_management,
  t_heating_equip, t_maintenance_team, t_project_type, t_purchase_station,
  t_radiator, t_region, t_repair_record, t_reservation,
  t_township, t_user_ext, t_village,
  v_barn_health_stats, v_barn_region_stats, v_repair_stats;

-- =============================================
-- 1. 系统用户表（RuoYi标准字段简化版）
-- =============================================
CREATE TABLE sys_user (
  user_id BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  dept_id BIGINT DEFAULT NULL COMMENT '部门ID',
  user_name VARCHAR(30) NOT NULL COMMENT '用户账号',
  nick_name VARCHAR(30) NOT NULL COMMENT '用户昵称',
  user_type VARCHAR(2) DEFAULT '00' COMMENT '用户类型',
  email VARCHAR(50) DEFAULT '' COMMENT '邮箱',
  phone VARCHAR(11) DEFAULT '' COMMENT '手机号',
  sex CHAR(1) DEFAULT '0' COMMENT '性别',
  avatar VARCHAR(100) DEFAULT '' COMMENT '头像',
  password VARCHAR(100) DEFAULT '' COMMENT '密码',
  status CHAR(1) DEFAULT '0' COMMENT '状态(0正常1停用)',
  del_flag CHAR(1) DEFAULT '0' COMMENT '删除标志(0存在2删除)',
  login_ip VARCHAR(128) DEFAULT '' COMMENT '最后登录IP',
  login_date DATETIME DEFAULT NULL COMMENT '最后登录时间',
  create_by VARCHAR(64) DEFAULT '' COMMENT '创建者',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_by VARCHAR(64) DEFAULT '' COMMENT '更新者',
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (user_id),
  KEY idx_username (user_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户信息表';


-- =============================================
-- 2. 市表
-- =============================================
CREATE TABLE sys_city (
  city_id BIGINT NOT NULL AUTO_INCREMENT,
  city_code VARCHAR(20) NOT NULL COMMENT '市编码',
  city_name VARCHAR(50) NOT NULL COMMENT '市名称',
  dept_name VARCHAR(100) DEFAULT NULL COMMENT '关联部门',
  status CHAR(1) DEFAULT '0',
  create_by VARCHAR(64) DEFAULT '',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_by VARCHAR(64) DEFAULT '',
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (city_id),
  UNIQUE KEY uk_city_code (city_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO sys_city (city_code, city_name, dept_name) VALUES
('420500', '宜昌市', '宜昌市烟叶分公司');

-- =============================================
-- 3. 县（市、区）表
-- =============================================
CREATE TABLE sys_county (
  county_id BIGINT NOT NULL AUTO_INCREMENT,
  county_code VARCHAR(20) NOT NULL,
  county_name VARCHAR(50) NOT NULL,
  city_code VARCHAR(20) NOT NULL,
  first_letter VARCHAR(10) DEFAULT NULL,
  dept_name VARCHAR(100) DEFAULT NULL,
  status CHAR(1) DEFAULT '0',
  create_by VARCHAR(64) DEFAULT '',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_by VARCHAR(64) DEFAULT '',
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (county_id),
  UNIQUE KEY uk_county_code (county_code),
  KEY idx_city_code (city_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO sys_county (county_code, county_name, city_code, dept_name) VALUES
('420525', '兴山县', '420500', '兴山县烟草专卖局'),
('420527', '秭归县', '420500', '秭归县烟草专卖局'),
('420526', '五峰土家族自治县', '420500', '五峰县烟草专卖局'),
('420528', '长阳土家族自治县', '420500', '长阳县烟草专卖局'),
('420529', '宜都市', '420500', '宜都市烟草专卖局');

-- =============================================
-- 4. 乡（镇）表
-- =============================================
CREATE TABLE sys_township (
  town_id BIGINT NOT NULL AUTO_INCREMENT,
  town_code VARCHAR(20) NOT NULL,
  town_name VARCHAR(50) NOT NULL,
  county_code VARCHAR(20) NOT NULL,
  first_letter VARCHAR(10) DEFAULT NULL,
  purchase_station VARCHAR(100) DEFAULT NULL COMMENT '关联收购站',
  status CHAR(1) DEFAULT '0',
  create_by VARCHAR(64) DEFAULT '',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_by VARCHAR(64) DEFAULT '',
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (town_id),
  UNIQUE KEY uk_town_code (town_code),
  KEY idx_county_code (county_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 兴山县乡镇
INSERT INTO sys_township (town_code, town_name, county_code, purchase_station) VALUES
('420525001', '古夫镇', '420525', '古夫收购站'),
('420525002', '高阳镇', '420525', '高阳收购站'),
('420525003', '峡口镇', '420525', '峡口收购站');

-- 秭归县乡镇
INSERT INTO sys_township (town_code, town_name, county_code, purchase_station) VALUES
('420527001', '茅坪镇', '420527', '茅坪收购站'),
('420527002', '归州镇', '420527', '归州收购站'),
('420527003', '屈原镇', '420527', '屈原收购站'),
('420527004', '水田坝乡', '420527', '水田坝收购站'),
('420527005', '泄滩乡', '420527', '泄滩收购站'),
('420527006', '郭家坝镇', '420527', '郭家坝收购站'),
('420527007', '两河口镇', '420527', '两河口收购站'),
('420527008', '梅家河乡', '420527', '梅家河收购站'),
('420527009', '磨坪乡', '420527', '磨坪收购站');

-- 五峰县
INSERT INTO sys_township (town_code, town_name, county_code, purchase_station) VALUES
('420526001', '五峰镇', '420526', '五峰收购站'),
('420526002', '长乐坪镇', '420526', '长乐坪收购站');

-- 长阳县
INSERT INTO sys_township (town_code, town_name, county_code, purchase_station) VALUES
('420528001', '龙舟坪镇', '420528', '龙舟坪收购站'),
('420528002', '高家堰镇', '420528', '高家堰收购站');

-- 宜都市
INSERT INTO sys_township (town_code, town_name, county_code, purchase_station) VALUES
('420529001', '陆城街道', '420529', '陆城收购站'),
('420529002', '红花套镇', '420529', '红花套收购站'),
('420529003', '枝城镇', '420529', '枝城收购站');

-- =============================================
-- 5. 村表
-- =============================================
CREATE TABLE sys_village (
  village_id BIGINT NOT NULL AUTO_INCREMENT,
  village_code VARCHAR(20) NOT NULL,
  village_name VARCHAR(50) NOT NULL,
  town_code VARCHAR(20) NOT NULL,
  first_letter VARCHAR(10) DEFAULT NULL,
  status CHAR(1) DEFAULT '0',
  create_by VARCHAR(64) DEFAULT '',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_by VARCHAR(64) DEFAULT '',
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (village_id),
  UNIQUE KEY uk_village_code (village_code),
  KEY idx_town_code (town_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 兴山县村
INSERT INTO sys_village (village_code, village_name, town_code) VALUES
('420525001001', '古夫社区', '420525001'),
('420525001002', '北斗坪村', '420525001'),
('420525002001', '高阳村', '420525002'),
('420525003001', '峡口社区', '420525003');

-- 秭归县村（代表）
INSERT INTO sys_village (village_code, village_name, town_code) VALUES
('420527001001', '茅坪社区', '420527001'),
('420527001002', '陈家坝村', '420527001'),
('420527001003', '四溪村', '420527001'),
('420527002001', '归州社区', '420527002'),
('420527002002', '万古寺村', '420527002'),
('420527003001', '屈原社区', '420527003'),
('420527003002', '乐平里村', '420527003'),
('420527004001', '水田坝社区', '420527004'),
('420527004002', '上坝村', '420527004'),
('420527005001', '泄滩社区', '420527005'),
('420527005002', '牛口村', '420527005'),
('420527006001', '郭家坝社区', '420527006'),
('420527006002', '王家岭村', '420527006'),
('420527006003', '庙坪村', '420527006'),
('420527006004', '烟灯堡村', '420527006'),
('420527007001', '两河口社区', '420527007'),
('420527007002', '牌楼村', '420527007'),
('420527008001', '梅家河社区', '420527008'),
('420527008002', '陈家湾村', '420527008'),
('420527009001', '磨坪社区', '420527009'),
('420527009002', '三墩岩村', '420527009');

-- 五峰县村
INSERT INTO sys_village (village_code, village_name, town_code) VALUES
('420526001001', '五峰社区', '420526001'),
('420526001002', '石桥村', '420526001'),
('420526002001', '长乐坪社区', '420526002');

-- 长阳县村
INSERT INTO sys_village (village_code, village_name, town_code) VALUES
('420528001001', '龙舟坪社区', '420528001'),
('420528001002', '三渔冲村', '420528001'),
('420528002001', '高家堰社区', '420528002');

-- 宜都市村
INSERT INTO sys_village (village_code, village_name, town_code) VALUES
('420529001001', '陆城社区', '420529001'),
('420529002001', '红花套社区', '420529002'),
('420529002002', '杨家畈村', '420529002'),
('420529003001', '枝城社区', '420529003');

-- =============================================
-- 6. 项目类型表
-- =============================================
CREATE TABLE sys_project_type (
  type_id BIGINT NOT NULL AUTO_INCREMENT,
  type_code VARCHAR(20) DEFAULT NULL,
  type_name VARCHAR(50) NOT NULL,
  sort_order INT DEFAULT 0,
  status CHAR(1) DEFAULT '0',
  create_by VARCHAR(64) DEFAULT '',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_by VARCHAR(64) DEFAULT '',
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (type_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO sys_project_type (type_name, sort_order) VALUES
('立式热源外置烤房', 1),
('卧式密集烤房', 2),
('电能烤房', 3),
('生物质烤房', 4),
('天然气烤房', 5),
('热泵烤房', 6);

-- =============================================
-- 7. 烤房项目基础信息表（核心业务表）
-- =============================================
CREATE TABLE barn_project (
  barn_id BIGINT NOT NULL AUTO_INCREMENT COMMENT '烤房ID',
  project_code VARCHAR(50) NOT NULL COMMENT '项目编号',
  barn_name VARCHAR(100) DEFAULT NULL COMMENT '烤房名称',
  use_status VARCHAR(20) DEFAULT '在用' COMMENT '使用状态(在用/闲置/转用/损毁)',
  idle_years INT DEFAULT 0 COMMENT '闲置年数',
  transfer_year INT DEFAULT NULL COMMENT '转用年度',
  transfer_use VARCHAR(100) DEFAULT NULL COMMENT '转用用途',
  damage_year INT DEFAULT NULL COMMENT '损毁年度',
  damage_reason VARCHAR(200) DEFAULT NULL COMMENT '损毁原因',
  build_mode VARCHAR(50) DEFAULT NULL COMMENT '建设方式',
  project_type VARCHAR(50) DEFAULT NULL COMMENT '项目类型',
  length_m DECIMAL(8,2) DEFAULT NULL COMMENT '长(米)',
  width_m DECIMAL(8,2) DEFAULT NULL COMMENT '宽(米)',
  height_m DECIMAL(8,2) DEFAULT NULL COMMENT '高(米)',
  project_cost DECIMAL(12,2) DEFAULT NULL COMMENT '工程造价(元)',
  subsidy_national DECIMAL(12,2) DEFAULT NULL COMMENT '国家局补贴(元)',
  subsidy_region DECIMAL(12,2) DEFAULT NULL COMMENT '产区补贴(元)',
  subsidy_total DECIMAL(12,2) DEFAULT NULL COMMENT '补贴总额(元)',
  start_date DATE DEFAULT NULL COMMENT '开工时间',
  complete_date DATE DEFAULT NULL COMMENT '竣工时间',
  city_code VARCHAR(20) DEFAULT NULL COMMENT '市编码',
  city_name VARCHAR(50) DEFAULT NULL COMMENT '市名称',
  county_code VARCHAR(20) DEFAULT NULL COMMENT '县编码',
  county_name VARCHAR(50) DEFAULT NULL COMMENT '县名称',
  town_code VARCHAR(20) DEFAULT NULL COMMENT '乡镇编码',
  town_name VARCHAR(50) DEFAULT NULL COMMENT '乡镇名称',
  village_code VARCHAR(20) DEFAULT NULL COMMENT '村编码',
  village_name VARCHAR(50) DEFAULT NULL COMMENT '村名称',
  address VARCHAR(255) DEFAULT NULL COMMENT '详细地址',
  project_owner VARCHAR(100) DEFAULT NULL COMMENT '项目业主',
  construction_unit VARCHAR(100) DEFAULT NULL COMMENT '施工单位',
  elevation DECIMAL(10,2) DEFAULT NULL COMMENT '海拔(米)',
  longitude DECIMAL(10,6) DEFAULT NULL COMMENT '经度',
  latitude DECIMAL(10,6) DEFAULT NULL COMMENT '纬度',
  has_heating TINYINT DEFAULT 0 COMMENT '加热设备是否填写',
  has_radiator TINYINT DEFAULT 0 COMMENT '散热器是否填写',
  has_autocontrol TINYINT DEFAULT 0 COMMENT '自控设备是否填写',
  has_main TINYINT DEFAULT 0 COMMENT '烤房主体是否填写',
  has_ancillary TINYINT DEFAULT 0 COMMENT '附属设施是否填写',
  station_name VARCHAR(100) DEFAULT NULL COMMENT '收购站名称',
  health_score DECIMAL(5,2) DEFAULT 100.00 COMMENT '健康评分',
  health_level VARCHAR(20) DEFAULT '优良' COMMENT '健康等级',
  predicted_life_years DECIMAL(4,1) DEFAULT 8.0 COMMENT '预测剩余寿命',
  is_idle TINYINT DEFAULT 1 COMMENT '是否空闲(0否1是)',
  status VARCHAR(20) DEFAULT '已通过' COMMENT '审核状态',
  create_by VARCHAR(64) DEFAULT '',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_by VARCHAR(64) DEFAULT '',
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  remark VARCHAR(500) DEFAULT NULL,
  PRIMARY KEY (barn_id),
  UNIQUE KEY uk_project_code (project_code),
  KEY idx_county (county_code),
  KEY idx_town (town_code),
  KEY idx_health (health_level),
  KEY idx_use_status (use_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='烤房项目基础信息表';

-- 导入烤房数据（从原t_barn_project迁移并规范化）
INSERT INTO barn_project (
  project_code, barn_name, use_status, idle_years, transfer_year, transfer_use,
  damage_year, damage_reason, build_mode, project_type, length_m, width_m, height_m,
  project_cost, subsidy_national, subsidy_region, subsidy_total, start_date, complete_date,
  city_code, city_name, county_code, county_name, town_code, town_name, village_code, village_name,
  address, project_owner, construction_unit, elevation, longitude, latitude,
  has_heating, has_radiator, has_autocontrol, has_main, has_ancillary, station_name,
  health_score, health_level, predicted_life_years, is_idle, status
) SELECT
  project_code,
  CONCAT(county_name, '-', town_name, '-', project_code) as barn_name,
  use_status, idle_years, transfer_year, transfer_use,
  damage_year, damage_reason, build_mode, project_type, length_m, width_m, height_m,
  project_cost, subsidy_national, subsidy_region, subsidy_total, start_date, complete_date,
  city_code, city_name, county_code, county_name, township_code, township_name as town_name, village_code, village_name,
  address, project_owner, construction_unit, elevation, longitude, latitude,
  has_heating, has_radiator, has_autocontrol, has_main, has_ancillary, station_code,
  health_score, health_level,
  CASE
    WHEN health_score >= 90 THEN 8.0
    WHEN health_score >= 70 THEN 5.0
    WHEN health_score >= 40 THEN 2.0
    ELSE 0.5
  END as predicted_life_years,
  1 as is_idle,
  '已通过' as status
FROM t_barn_project;

-- =============================================
-- 8. 燃烧机表
-- =============================================
CREATE TABLE barn_burner (
  burner_id BIGINT NOT NULL AUTO_INCREMENT,
  burner_code VARCHAR(50) NOT NULL,
  project_code VARCHAR(50) DEFAULT NULL,
  heating_type VARCHAR(50) DEFAULT NULL COMMENT '加热设备类型',
  project_cost DECIMAL(12,2) DEFAULT NULL,
  subsidy_total DECIMAL(12,2) DEFAULT NULL,
  subsidy_national DECIMAL(12,2) DEFAULT NULL,
  subsidy_region DECIMAL(12,2) DEFAULT NULL,
  build_year INT DEFAULT NULL,
  construction_unit VARCHAR(100) DEFAULT NULL,
  status VARCHAR(20) DEFAULT '已通过',
  create_by VARCHAR(64) DEFAULT '',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_by VARCHAR(64) DEFAULT '',
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (burner_id),
  UNIQUE KEY uk_burner_code (burner_code),
  KEY idx_project_code (project_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO barn_burner (burner_code, project_code, heating_type, project_cost, subsidy_total, subsidy_national, subsidy_region, build_year, construction_unit, status)
SELECT burner_code, project_code, heating_type, project_cost, subsidy_total, subsidy_national, subsidy_region, build_year, construction_unit, status FROM t_burner;

-- =============================================
-- 9. 烤房部件评分表
-- =============================================
CREATE TABLE barn_component (
  component_id BIGINT NOT NULL AUTO_INCREMENT,
  barn_id BIGINT NOT NULL COMMENT '烤房ID',
  component_type VARCHAR(50) NOT NULL COMMENT '部件类型',
  component_name VARCHAR(100) NOT NULL COMMENT '部件名称',
  status VARCHAR(20) DEFAULT '正常' COMMENT '状态',
  damage_level VARCHAR(20) DEFAULT '无' COMMENT '损坏程度',
  score DECIMAL(5,2) DEFAULT 100.00 COMMENT '评分',
  weight DECIMAL(3,2) DEFAULT 0.10 COMMENT '权重',
  repair_year INT DEFAULT NULL COMMENT '最后修复年度',
  remark VARCHAR(500) DEFAULT NULL,
  create_by VARCHAR(64) DEFAULT '',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_by VARCHAR(64) DEFAULT '',
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (component_id),
  KEY idx_barn_id (barn_id),
  KEY idx_component_type (component_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 根据烤房健康分数批量生成部件评分数据
INSERT INTO barn_component (barn_id, component_type, component_name, status, damage_level, score, weight)
SELECT
  b.barn_id,
  '加热设备' as component_type,
  '加热炉' as component_name,
  CASE WHEN b.health_score >= 80 THEN '正常' WHEN b.health_score >= 60 THEN '轻微损坏' ELSE '严重损坏' END as status,
  CASE WHEN b.health_score >= 80 THEN '无' WHEN b.health_score >= 60 THEN '轻度' ELSE '重度' END as damage_level,
  ROUND(b.health_score * (0.95 + RAND() * 0.1), 2) as score,
  0.20 as weight
FROM barn_project b
WHERE b.barn_id <= 500;

INSERT INTO barn_component (barn_id, component_type, component_name, status, damage_level, score, weight)
SELECT
  b.barn_id, '散热器', '散热管',
  CASE WHEN b.health_score >= 85 THEN '正常' WHEN b.health_score >= 65 THEN '轻微损坏' ELSE '严重损坏' END,
  CASE WHEN b.health_score >= 85 THEN '无' WHEN b.health_score >= 65 THEN '轻度' ELSE '重度' END,
  ROUND(b.health_score * (0.92 + RAND() * 0.1), 2), 0.15
FROM barn_project b WHERE b.barn_id <= 500;

INSERT INTO barn_component (barn_id, component_type, component_name, status, damage_level, score, weight)
SELECT
  b.barn_id, '自控设备', '温度传感器',
  CASE WHEN b.health_score >= 90 THEN '正常' WHEN b.health_score >= 70 THEN '轻微损坏' ELSE '严重损坏' END,
  CASE WHEN b.health_score >= 90 THEN '无' WHEN b.health_score >= 70 THEN '轻度' ELSE '重度' END,
  ROUND(b.health_score * (0.96 + RAND() * 0.08), 2), 0.15
FROM barn_project b WHERE b.barn_id <= 500;

INSERT INTO barn_component (barn_id, component_type, component_name, status, damage_level, score, weight)
SELECT
  b.barn_id, '自控设备', '循环风机',
  CASE WHEN b.health_score >= 85 THEN '正常' WHEN b.health_score >= 65 THEN '轻微损坏' ELSE '严重损坏' END,
  CASE WHEN b.health_score >= 85 THEN '无' WHEN b.health_score >= 65 THEN '轻度' ELSE '重度' END,
  ROUND(b.health_score * (0.94 + RAND() * 0.1), 2), 0.15
FROM barn_project b WHERE b.barn_id <= 500;

INSERT INTO barn_component (barn_id, component_type, component_name, status, damage_level, score, weight)
SELECT
  b.barn_id, '主体结构', '墙体屋顶',
  CASE WHEN b.health_score >= 80 THEN '正常' WHEN b.health_score >= 60 THEN '轻微损坏' ELSE '严重损坏' END,
  CASE WHEN b.health_score >= 80 THEN '无' WHEN b.health_score >= 60 THEN '轻度' ELSE '重度' END,
  ROUND(b.health_score * (0.93 + RAND() * 0.1), 2), 0.15
FROM barn_project b WHERE b.barn_id <= 500;

INSERT INTO barn_component (barn_id, component_type, component_name, status, damage_level, score, weight)
SELECT
  b.barn_id, '主体结构', '挂烟梁柱',
  CASE WHEN b.health_score >= 85 THEN '正常' WHEN b.health_score >= 65 THEN '轻微损坏' ELSE '严重损坏' END,
  CASE WHEN b.health_score >= 85 THEN '无' WHEN b.health_score >= 65 THEN '轻度' ELSE '重度' END,
  ROUND(b.health_score * (0.95 + RAND() * 0.08), 2), 0.10
FROM barn_project b WHERE b.barn_id <= 500;

INSERT INTO barn_component (barn_id, component_type, component_name, status, damage_level, score, weight)
SELECT
  b.barn_id, '附属设施', '烟棚',
  CASE WHEN b.health_score >= 85 THEN '正常' WHEN b.health_score >= 65 THEN '轻微损坏' ELSE '严重损坏' END,
  CASE WHEN b.health_score >= 85 THEN '无' WHEN b.health_score >= 65 THEN '轻度' ELSE '重度' END,
  ROUND(b.health_score * (0.94 + RAND() * 0.1), 2), 0.10
FROM barn_project b WHERE b.barn_id <= 500;

-- =============================================
-- 10. 维修记录表
-- =============================================
CREATE TABLE repair_record (
  repair_id BIGINT NOT NULL AUTO_INCREMENT,
  barn_id BIGINT NOT NULL COMMENT '烤房ID',
  project_code VARCHAR(50) DEFAULT NULL,
  barn_name VARCHAR(100) DEFAULT NULL,
  repair_type VARCHAR(20) DEFAULT '常规维修' COMMENT '维修类型',
  urgency VARCHAR(20) DEFAULT '正常' COMMENT '紧急程度',
  applicant VARCHAR(50) DEFAULT NULL COMMENT '申请人',
  apply_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
  apply_desc VARCHAR(1000) DEFAULT NULL COMMENT '问题描述',
  estimated_cost DECIMAL(10,2) DEFAULT NULL COMMENT '预估费用',
  actual_cost DECIMAL(10,2) DEFAULT NULL COMMENT '实际费用',
  repair_status VARCHAR(20) DEFAULT '待审核' COMMENT '状态',
  auditor VARCHAR(50) DEFAULT NULL COMMENT '审核人',
  audit_time DATETIME DEFAULT NULL COMMENT '审核时间',
  audit_opinion VARCHAR(500) DEFAULT NULL COMMENT '审核意见',
  implement_team VARCHAR(100) DEFAULT NULL COMMENT '实施队伍',
  start_date DATE DEFAULT NULL COMMENT '开始日期',
  end_date DATE DEFAULT NULL COMMENT '结束日期',
  acceptor VARCHAR(50) DEFAULT NULL COMMENT '验收人',
  accept_time DATETIME DEFAULT NULL COMMENT '验收时间',
  accept_result VARCHAR(20) DEFAULT NULL COMMENT '验收结果',
  accept_opinion VARCHAR(500) DEFAULT NULL COMMENT '验收意见',
  roi_score DECIMAL(5,2) DEFAULT NULL COMMENT '投入产出比',
  create_by VARCHAR(64) DEFAULT '',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_by VARCHAR(64) DEFAULT '',
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  remark VARCHAR(500) DEFAULT NULL,
  PRIMARY KEY (repair_id),
  KEY idx_barn_id (barn_id),
  KEY idx_status (repair_status),
  KEY idx_urgency (urgency)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 生成模拟维修数据（针对健康分数较低的烤房）
INSERT INTO repair_record (barn_id, project_code, barn_name, repair_type, urgency, applicant, apply_time, apply_desc, estimated_cost, actual_cost, repair_status, auditor, audit_time, audit_opinion, implement_team, start_date, end_date, acceptor, accept_time, accept_result, roi_score)
SELECT
  b.barn_id, b.project_code, b.barn_name,
  CASE WHEN b.health_score < 50 THEN '紧急维修' WHEN b.health_score < 70 THEN '常规维修' ELSE '保养维护' END as repair_type,
  CASE WHEN b.health_score < 50 THEN '紧急' WHEN b.health_score < 70 THEN '正常' ELSE '低' END as urgency,
  '技术员' as applicant,
  DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 90) DAY) as apply_time,
  CONCAT('烤房部件老化，需维修：', CASE WHEN b.health_score < 50 THEN '加热炉损坏、散热管泄漏' WHEN b.health_score < 70 THEN '温度传感器失灵，风机异响' ELSE '常规保养，润滑油更换' END) as apply_desc,
  ROUND(1000 + RAND() * 8000, 2) as estimated_cost,
  ROUND(900 + RAND() * 8200, 2) as actual_cost,
  CASE WHEN b.barn_id % 5 = 0 THEN '待审核' WHEN b.barn_id % 5 = 1 THEN '实施中' ELSE '已验收' END as repair_status,
  '管理员' as auditor,
  DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 80) DAY) as audit_time,
  '情况属实，同意维修' as audit_opinion,
  CASE WHEN b.county_code = '420527' THEN '秭归县维修队' ELSE '其他维修队' END as implement_team,
  DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 60) DAY) as start_date,
  DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 40) DAY) as end_date,
  '验收员' as acceptor,
  DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 30) DAY) as accept_time,
  '合格' as accept_result,
  ROUND(70 + RAND() * 25, 2) as roi_score
FROM barn_project b
WHERE b.health_score < 85
ORDER BY b.health_score ASC
LIMIT 300;

-- =============================================
-- 11. 预约记录表
-- =============================================
CREATE TABLE reservation_record (
  reservation_id BIGINT NOT NULL AUTO_INCREMENT,
  barn_id BIGINT NOT NULL,
  project_code VARCHAR(50) DEFAULT NULL,
  barn_name VARCHAR(100) DEFAULT NULL,
  user_id BIGINT DEFAULT NULL,
  user_name VARCHAR(50) DEFAULT NULL COMMENT '烟农姓名',
  user_phone VARCHAR(20) DEFAULT NULL,
  reserve_date DATE NOT NULL COMMENT '预约日期',
  reserve_start DATETIME NOT NULL COMMENT '开始时间',
  reserve_end DATETIME NOT NULL COMMENT '结束时间',
  leaf_weight DECIMAL(8,2) DEFAULT NULL COMMENT '烟叶重量(kg)',
  reserve_status VARCHAR(20) DEFAULT '待审核' COMMENT '状态',
  reviewer VARCHAR(50) DEFAULT NULL COMMENT '审核人',
  review_time DATETIME DEFAULT NULL COMMENT '审核时间',
  review_opinion VARCHAR(500) DEFAULT NULL,
  actual_start DATETIME DEFAULT NULL,
  actual_end DATETIME DEFAULT NULL,
  is_timeout TINYINT DEFAULT 0,
  create_by VARCHAR(64) DEFAULT '',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_by VARCHAR(64) DEFAULT '',
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (reservation_id),
  KEY idx_barn_id (barn_id),
  KEY idx_user_id (user_id),
  KEY idx_status (reserve_status),
  KEY idx_reserve_date (reserve_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 生成预约数据
INSERT INTO reservation_record (barn_id, project_code, barn_name, user_id, user_name, user_phone, reserve_date, reserve_start, reserve_end, leaf_weight, reserve_status, reviewer, review_time, review_opinion, actual_start, actual_end, is_timeout)
SELECT
  b.barn_id, b.project_code, b.barn_name,
  4 as user_id,
  ELT((b.barn_id % 5) + 1, '张烟农', '李烟农', '王烟农', '陈烟农', '赵烟农') as user_name,
  CONCAT('138', LPAD(FLOOR(RAND() * 100000000), 8, '0')) as user_phone,
  DATE_ADD(CURDATE(), INTERVAL (b.barn_id % 60) DAY) as reserve_date,
  DATE_ADD(DATE_ADD(CURDATE(), INTERVAL (b.barn_id % 60) DAY), INTERVAL 8 HOUR) as reserve_start,
  DATE_ADD(DATE_ADD(CURDATE(), INTERVAL (b.barn_id % 60) DAY), INTERVAL 20 HOUR) as reserve_end,
  ROUND(200 + RAND() * 300, 2) as leaf_weight,
  CASE WHEN b.barn_id % 10 = 0 THEN '待审核' WHEN b.barn_id % 10 = 1 THEN '使用中' ELSE '已完成' END as reserve_status,
  '审核员' as reviewer,
  DATE_ADD(CURDATE(), INTERVAL (b.barn_id % 60 - 1) DAY) as review_time,
  '同意使用' as review_opinion,
  DATE_ADD(DATE_ADD(CURDATE(), INTERVAL (b.barn_id % 60) DAY), INTERVAL 8 HOUR) as actual_start,
  DATE_ADD(DATE_ADD(CURDATE(), INTERVAL (b.barn_id % 60) DAY), INTERVAL 19 HOUR) as actual_end,
  CASE WHEN b.barn_id % 15 = 0 THEN 1 ELSE 0 END as is_timeout
FROM barn_project b
WHERE b.is_idle = 1 AND b.barn_id <= 300;

-- =============================================
-- 12. 评价表
-- =============================================
CREATE TABLE evaluation_record (
  evaluation_id BIGINT NOT NULL AUTO_INCREMENT,
  reservation_id BIGINT DEFAULT NULL,
  barn_id BIGINT NOT NULL,
  project_code VARCHAR(50) DEFAULT NULL,
  barn_name VARCHAR(100) DEFAULT NULL,
  evaluator_id BIGINT DEFAULT NULL,
  evaluator_name VARCHAR(50) DEFAULT NULL COMMENT '评价人',
  baker_id BIGINT DEFAULT NULL COMMENT '烘烤师ID',
  baker_name VARCHAR(50) DEFAULT NULL COMMENT '烘烤师姓名',
  barn_score INT DEFAULT 5 COMMENT '烤房评分(1-5)',
  baker_score INT DEFAULT 5 COMMENT '烘烤师评分(1-5)',
  barn_comment VARCHAR(500) DEFAULT NULL COMMENT '烤房评价',
  baker_comment VARCHAR(500) DEFAULT NULL COMMENT '烘烤师评价',
  evaluate_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  create_by VARCHAR(64) DEFAULT '',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_by VARCHAR(64) DEFAULT '',
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (evaluation_id),
  KEY idx_barn_id (barn_id),
  KEY idx_reservation_id (reservation_id),
  KEY idx_evaluator (evaluator_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 生成评价数据
INSERT INTO evaluation_record (reservation_id, barn_id, project_code, barn_name, evaluator_id, evaluator_name, baker_name, barn_score, baker_score, barn_comment, baker_comment, evaluate_time)
SELECT
  r.reservation_id, r.barn_id, r.project_code, r.barn_name,
  4 as evaluator_id,
  r.user_name as evaluator_name,
  ELT((r.barn_id % 4) + 1, '刘师傅', '张师傅', '王师傅', '陈师傅') as baker_name,
  FLOOR(4 + RAND() * 2) as barn_score,
  FLOOR(4 + RAND() * 2) as baker_score,
  ELT((r.barn_id % 5) + 1, '烤房运行稳定，温控精准', '排湿稍慢，整体良好', '设备正常，烘烤质量好', '密封性一般，需要改进', '整体状况良好') as barn_comment,
  ELT((r.barn_id % 4) + 1, '操作熟练，经验丰富', '服务态度好', '时间观念强', '沟通顺畅，配合度高') as baker_comment,
  DATE_ADD(r.actual_end, INTERVAL 1 HOUR) as evaluate_time
FROM reservation_record r
WHERE r.reserve_status = '已完成'
LIMIT 200;

-- =============================================
-- 13. 管护队伍表
-- =============================================
CREATE TABLE maintenance_team (
  team_id BIGINT NOT NULL AUTO_INCREMENT,
  team_name VARCHAR(100) NOT NULL COMMENT '队伍名称',
  team_type VARCHAR(50) NOT NULL COMMENT '队伍类型',
  county_code VARCHAR(20) DEFAULT NULL,
  county_name VARCHAR(50) DEFAULT NULL,
  leader_name VARCHAR(50) DEFAULT NULL COMMENT '负责人',
  leader_phone VARCHAR(20) DEFAULT NULL COMMENT '联系电话',
  member_count INT DEFAULT 0 COMMENT '人数',
  service_area VARCHAR(255) DEFAULT NULL COMMENT '服务区域',
  repair_count INT DEFAULT 0 COMMENT '累计维修数',
  status CHAR(1) DEFAULT '0' COMMENT '状态',
  create_by VARCHAR(64) DEFAULT '',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_by VARCHAR(64) DEFAULT '',
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  remark VARCHAR(500) DEFAULT NULL,
  PRIMARY KEY (team_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO maintenance_team (team_name, team_type, county_code, county_name, leader_name, leader_phone, member_count, service_area, repair_count) VALUES
('秭归县烘烤技术服务队', '烘烤技术服务队', '420527', '秭归县', '杜勇', '13900139001', 12, '秭归县全域', 85),
('秭归县简易维修服务队', '简易维修服务队', '420527', '秭归县', '郑刚', '13900139002', 8, '秭归县各烤房点', 65),
('秭归县常规管护服务队', '常规管护服务队', '420527', '秭归县', '黄志强', '13900139003', 15, '秭归县烟叶产区', 92),
('秭归县综合管理服务队', '综合管理服务队', '420527', '秭归县', '李建华', '13900139004', 6, '秭归县合作社直属', 40),
('兴山县烘烤技术服务队', '烘烤技术服务队', '420525', '兴山县', '王永明', '13900139005', 10, '兴山县全域', 58),
('兴山县简易维修服务队', '简易维修服务队', '420525', '兴山县', '陈大伟', '13900139006', 7, '兴山县各烤房点', 42),
('兴山县常规管护服务队', '常规管护服务队', '420525', '兴山县', '刘建国', '13900139007', 12, '兴山县烟叶产区', 68),
('五峰县综合管护服务队', '综合管理服务队', '420526', '五峰县', '张晓东', '13900139008', 9, '五峰县全域', 35),
('长阳县烘烤技术服务队', '烘烤技术服务队', '420528', '长阳县', '周国强', '13900139009', 11, '长阳县全域', 48),
('宜都市常规管护服务队', '常规管护服务队', '420529', '宜都市', '吴明辉', '13900139010', 8, '宜都市全域', 38);

-- =============================================
-- 14. 资金管理表
-- =============================================
CREATE TABLE fund_management (
  fund_id BIGINT NOT NULL AUTO_INCREMENT,
  fund_year INT NOT NULL COMMENT '年度',
  county_code VARCHAR(20) DEFAULT NULL,
  county_name VARCHAR(50) DEFAULT NULL,
  fund_source VARCHAR(50) NOT NULL COMMENT '资金来源',
  total_amount DECIMAL(12,2) DEFAULT 0.00 COMMENT '总金额',
  used_amount DECIMAL(12,2) DEFAULT 0.00 COMMENT '已使用',
  allocated_amount DECIMAL(12,2) DEFAULT 0.00 COMMENT '已分配',
  repair_count INT DEFAULT 0 COMMENT '维修项目数',
  avg_roi DECIMAL(5,2) DEFAULT NULL COMMENT '平均投入产出比',
  create_by VARCHAR(64) DEFAULT '',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_by VARCHAR(64) DEFAULT '',
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  remark VARCHAR(500) DEFAULT NULL,
  PRIMARY KEY (fund_id),
  KEY idx_year (fund_year),
  KEY idx_county (county_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO fund_management (fund_year, county_code, county_name, fund_source, total_amount, used_amount, allocated_amount, repair_count, avg_roi) VALUES
(2026, '420527', '秭归县', '行业管护资金', 500000.00, 320000.00, 150000.00, 42, 82.50),
(2026, '420527', '秭归县', '政府管护资金', 300000.00, 180000.00, 100000.00, 28, 78.30),
(2026, '420527', '秭归县', '合作社专项', 200000.00, 120000.00, 50000.00, 15, 85.20),
(2026, '420525', '兴山县', '行业管护资金', 400000.00, 280000.00, 100000.00, 35, 80.10),
(2026, '420525', '兴山县', '政府管护资金', 250000.00, 150000.00, 80000.00, 22, 76.80),
(2026, '420526', '五峰县', '行业管护资金', 350000.00, 220000.00, 100000.00, 28, 79.50),
(2026, '420528', '长阳县', '行业管护资金', 380000.00, 250000.00, 100000.00, 32, 81.20),
(2026, '420529', '宜都市', '行业管护资金', 280000.00, 180000.00, 80000.00, 20, 77.90);

-- =============================================
-- 视图：烤房健康统计
-- =============================================
CREATE OR REPLACE VIEW v_barn_health_stats AS
SELECT
  county_code,
  county_name,
  COUNT(*) as total_count,
  SUM(CASE WHEN health_level = '优良' THEN 1 ELSE 0 END) as excellent_count,
  SUM(CASE WHEN health_level = '需维护' THEN 1 ELSE 0 END) as maintain_count,
  SUM(CASE WHEN health_level = '急需修复' THEN 1 ELSE 0 END) as urgent_count,
  SUM(CASE WHEN health_level = '退出' THEN 1 ELSE 0 END) as retired_count,
  ROUND(AVG(health_score), 2) as avg_score
FROM barn_project
GROUP BY county_code, county_name;

-- =============================================
-- 视图：区域分布统计
-- =============================================
CREATE OR REPLACE VIEW v_barn_region_stats AS
SELECT
  city_name,
  county_name,
  town_name,
  COUNT(*) as barn_count,
  ROUND(AVG(health_score), 2) as avg_health_score,
  SUM(CASE WHEN use_status = '在用' THEN 1 ELSE 0 END) as in_use_count,
  SUM(CASE WHEN use_status = '闲置' THEN 1 ELSE 0 END) as idle_count
FROM barn_project
GROUP BY city_name, county_name, town_name;

-- 输出统计结果
SELECT '=== 数据库初始化完成 ===' as info;
SELECT 'barn_project' as table_name, COUNT(*) as count FROM barn_project
UNION ALL SELECT 'barn_burner', COUNT(*) FROM barn_burner
UNION ALL SELECT 'barn_component', COUNT(*) FROM barn_component
UNION ALL SELECT 'repair_record', COUNT(*) FROM repair_record
UNION ALL SELECT 'reservation_record', COUNT(*) FROM reservation_record
UNION ALL SELECT 'evaluation_record', COUNT(*) FROM evaluation_record
UNION ALL SELECT 'maintenance_team', COUNT(*) FROM maintenance_team
UNION ALL SELECT 'fund_management', COUNT(*) FROM fund_management
UNION ALL SELECT 'sys_user', COUNT(*) FROM sys_user
UNION ALL SELECT 'sys_city', COUNT(*) FROM sys_city
UNION ALL SELECT 'sys_county', COUNT(*) FROM sys_county
UNION ALL SELECT 'sys_township', COUNT(*) FROM sys_township
UNION ALL SELECT 'sys_village', COUNT(*) FROM sys_village
UNION ALL SELECT 'sys_project_type', COUNT(*) FROM sys_project_type;
