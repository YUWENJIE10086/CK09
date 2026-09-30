-- =============================================
-- 烤房管理系统 - 核心表结构（RuoYi标准）
-- MySQL 5.7+
-- =============================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- 1. 用户表
CREATE TABLE sys_user (
  user_id BIGINT NOT NULL AUTO_INCREMENT,
  dept_id BIGINT DEFAULT NULL,
  user_name VARCHAR(30) NOT NULL,
  nick_name VARCHAR(30) NOT NULL,
  user_type VARCHAR(2) DEFAULT '00',
  email VARCHAR(50) DEFAULT '',
  phone VARCHAR(20) DEFAULT '',
  sex CHAR(1) DEFAULT '0',
  avatar VARCHAR(100) DEFAULT '',
  password VARCHAR(100) DEFAULT '',
  status CHAR(1) DEFAULT '0',
  del_flag CHAR(1) DEFAULT '0',
  login_ip VARCHAR(128) DEFAULT '',
  login_date DATETIME DEFAULT NULL,
  create_by VARCHAR(64) DEFAULT '',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_by VARCHAR(64) DEFAULT '',
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  remark VARCHAR(500) DEFAULT NULL,
  PRIMARY KEY (user_id),
  KEY idx_username (user_name)
) ENGINE=InnoDB AUTO_INCREMENT=100 DEFAULT CHARSET=utf8mb4;

-- 安全说明：不在源码中创建任何带固定默认密码的可登录账户。
-- 首次部署请使用 sql/sys_admin_secure_seed.template.sql，生成独立 BCrypt 哈希后再创建初始管理员。
-- 禁止在版本库中提交初始明文密码或其固定可复用哈希。

-- 2. 市表
CREATE TABLE sys_city (
  city_id BIGINT NOT NULL AUTO_INCREMENT,
  city_code VARCHAR(20) NOT NULL,
  city_name VARCHAR(50) NOT NULL,
  dept_name VARCHAR(100) DEFAULT NULL,
  status CHAR(1) DEFAULT '0',
  create_by VARCHAR(64) DEFAULT '',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_by VARCHAR(64) DEFAULT '',
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (city_id),
  UNIQUE KEY uk_city_code (city_code)
) ENGINE=InnoDB AUTO_INCREMENT=100 DEFAULT CHARSET=utf8mb4;

INSERT INTO sys_city (city_code, city_name, dept_name) VALUES
('420500', '宜昌市', '宜昌市烟叶分公司');

-- 3. 县（市、区）表
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
) ENGINE=InnoDB AUTO_INCREMENT=100 DEFAULT CHARSET=utf8mb4;

-- 4. 乡（镇）表
CREATE TABLE sys_township (
  town_id BIGINT NOT NULL AUTO_INCREMENT,
  town_code VARCHAR(20) NOT NULL,
  town_name VARCHAR(50) NOT NULL,
  county_code VARCHAR(20) NOT NULL,
  first_letter VARCHAR(10) DEFAULT NULL,
  purchase_station VARCHAR(100) DEFAULT NULL,
  status CHAR(1) DEFAULT '0',
  create_by VARCHAR(64) DEFAULT '',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_by VARCHAR(64) DEFAULT '',
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (town_id),
  UNIQUE KEY uk_town_code (town_code),
  KEY idx_county_code (county_code)
) ENGINE=InnoDB AUTO_INCREMENT=100 DEFAULT CHARSET=utf8mb4;

-- 5. 村表
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
) ENGINE=InnoDB AUTO_INCREMENT=1000 DEFAULT CHARSET=utf8mb4;

-- 6. 项目类型表
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
) ENGINE=InnoDB AUTO_INCREMENT=100 DEFAULT CHARSET=utf8mb4;

-- 7. 烤房项目基础信息表（核心业务表）
CREATE TABLE barn_project (
  barn_id BIGINT NOT NULL AUTO_INCREMENT,
  project_code VARCHAR(50) NOT NULL,
  barn_name VARCHAR(100) DEFAULT NULL,
  use_status VARCHAR(20) DEFAULT '在用',
  idle_years INT DEFAULT 0,
  transfer_year INT DEFAULT NULL,
  transfer_use VARCHAR(100) DEFAULT NULL,
  damage_year INT DEFAULT NULL,
  damage_reason VARCHAR(200) DEFAULT NULL,
  build_mode VARCHAR(50) DEFAULT NULL,
  project_type VARCHAR(50) DEFAULT NULL,
  length_m DECIMAL(8,2) DEFAULT NULL,
  width_m DECIMAL(8,2) DEFAULT NULL,
  height_m DECIMAL(8,2) DEFAULT NULL,
  project_cost DECIMAL(12,2) DEFAULT NULL,
  subsidy_national DECIMAL(12,2) DEFAULT NULL,
  subsidy_region DECIMAL(12,2) DEFAULT NULL,
  subsidy_total DECIMAL(12,2) DEFAULT NULL,
  start_date DATE DEFAULT NULL,
  complete_date DATE DEFAULT NULL,
  city_code VARCHAR(20) DEFAULT NULL,
  city_name VARCHAR(50) DEFAULT NULL,
  county_code VARCHAR(20) DEFAULT NULL,
  county_name VARCHAR(50) DEFAULT NULL,
  town_code VARCHAR(20) DEFAULT NULL,
  town_name VARCHAR(50) DEFAULT NULL,
  village_code VARCHAR(20) DEFAULT NULL,
  village_name VARCHAR(50) DEFAULT NULL,
  address VARCHAR(255) DEFAULT NULL,
  project_owner VARCHAR(100) DEFAULT NULL,
  construction_unit VARCHAR(100) DEFAULT NULL,
  elevation DECIMAL(10,2) DEFAULT NULL,
  longitude DECIMAL(10,6) DEFAULT NULL,
  latitude DECIMAL(10,6) DEFAULT NULL,
  has_heating TINYINT DEFAULT 0,
  has_radiator TINYINT DEFAULT 0,
  has_autocontrol TINYINT DEFAULT 0,
  has_main TINYINT DEFAULT 0,
  has_ancillary TINYINT DEFAULT 0,
  station_name VARCHAR(100) DEFAULT NULL,
  health_score DECIMAL(5,2) DEFAULT 100.00,
  health_level VARCHAR(20) DEFAULT '优良',
  predicted_life_years DECIMAL(4,1) DEFAULT 8.0,
  is_idle TINYINT DEFAULT 1,
  status VARCHAR(20) DEFAULT '已通过',
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
) ENGINE=InnoDB AUTO_INCREMENT=100000 DEFAULT CHARSET=utf8mb4;

-- 8. 燃烧机表
CREATE TABLE barn_burner (
  burner_id BIGINT NOT NULL AUTO_INCREMENT,
  burner_code VARCHAR(50) NOT NULL,
  project_code VARCHAR(50) DEFAULT NULL,
  heating_type VARCHAR(50) DEFAULT NULL,
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
) ENGINE=InnoDB AUTO_INCREMENT=100000 DEFAULT CHARSET=utf8mb4;

-- 9. 烤房部件评分表
CREATE TABLE barn_component (
  component_id BIGINT NOT NULL AUTO_INCREMENT,
  barn_id BIGINT NOT NULL,
  component_type VARCHAR(50) NOT NULL,
  component_name VARCHAR(100) NOT NULL,
  component_status VARCHAR(20) DEFAULT '正常',
  damage_level VARCHAR(20) DEFAULT '无',
  score DECIMAL(5,2) DEFAULT 100.00,
  weight DECIMAL(3,2) DEFAULT 0.10,
  repair_year INT DEFAULT NULL,
  remark VARCHAR(500) DEFAULT NULL,
  create_by VARCHAR(64) DEFAULT '',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_by VARCHAR(64) DEFAULT '',
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (component_id),
  KEY idx_barn_id (barn_id),
  KEY idx_component_type (component_type)
) ENGINE=InnoDB AUTO_INCREMENT=1000000 DEFAULT CHARSET=utf8mb4;

-- 10. 维修记录表
CREATE TABLE repair_record (
  repair_id BIGINT NOT NULL AUTO_INCREMENT,
  barn_id BIGINT NOT NULL,
  project_code VARCHAR(50) DEFAULT NULL,
  barn_name VARCHAR(100) DEFAULT NULL,
  repair_type VARCHAR(20) DEFAULT '常规维修',
  urgency VARCHAR(20) DEFAULT '正常',
  applicant VARCHAR(50) DEFAULT NULL,
  apply_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  apply_desc VARCHAR(1000) DEFAULT NULL,
  estimated_cost DECIMAL(10,2) DEFAULT NULL,
  actual_cost DECIMAL(10,2) DEFAULT NULL,
  repair_status VARCHAR(20) DEFAULT '待审核',
  auditor VARCHAR(50) DEFAULT NULL,
  audit_time DATETIME DEFAULT NULL,
  audit_opinion VARCHAR(500) DEFAULT NULL,
  implement_team VARCHAR(100) DEFAULT NULL,
  start_date DATE DEFAULT NULL,
  end_date DATE DEFAULT NULL,
  acceptor VARCHAR(50) DEFAULT NULL,
  accept_time DATETIME DEFAULT NULL,
  accept_result VARCHAR(20) DEFAULT NULL,
  accept_opinion VARCHAR(500) DEFAULT NULL,
  roi_score DECIMAL(5,2) DEFAULT NULL,
  create_by VARCHAR(64) DEFAULT '',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_by VARCHAR(64) DEFAULT '',
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  remark VARCHAR(500) DEFAULT NULL,
  PRIMARY KEY (repair_id),
  KEY idx_barn_id (barn_id),
  KEY idx_status (repair_status),
  KEY idx_urgency (urgency)
) ENGINE=InnoDB AUTO_INCREMENT=100000 DEFAULT CHARSET=utf8mb4;

-- 11. 预约记录表
CREATE TABLE reservation_record (
  reservation_id BIGINT NOT NULL AUTO_INCREMENT,
  barn_id BIGINT NOT NULL,
  project_code VARCHAR(50) DEFAULT NULL,
  barn_name VARCHAR(100) DEFAULT NULL,
  user_id BIGINT DEFAULT NULL,
  user_name VARCHAR(50) DEFAULT NULL,
  user_phone VARCHAR(20) DEFAULT NULL,
  reserve_date DATE NOT NULL,
  reserve_start DATETIME NOT NULL,
  reserve_end DATETIME NOT NULL,
  leaf_weight DECIMAL(8,2) DEFAULT NULL,
  reserve_status VARCHAR(20) DEFAULT '待审核',
  reviewer VARCHAR(50) DEFAULT NULL,
  review_time DATETIME DEFAULT NULL,
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
) ENGINE=InnoDB AUTO_INCREMENT=100000 DEFAULT CHARSET=utf8mb4;

-- 12. 评价表
CREATE TABLE evaluation_record (
  evaluation_id BIGINT NOT NULL AUTO_INCREMENT,
  reservation_id BIGINT DEFAULT NULL,
  barn_id BIGINT NOT NULL,
  project_code VARCHAR(50) DEFAULT NULL,
  barn_name VARCHAR(100) DEFAULT NULL,
  evaluator_id BIGINT DEFAULT NULL,
  evaluator_name VARCHAR(50) DEFAULT NULL,
  baker_id BIGINT DEFAULT NULL,
  baker_name VARCHAR(50) DEFAULT NULL,
  barn_score INT DEFAULT 5,
  baker_score INT DEFAULT 5,
  barn_comment VARCHAR(500) DEFAULT NULL,
  baker_comment VARCHAR(500) DEFAULT NULL,
  evaluate_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  create_by VARCHAR(64) DEFAULT '',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_by VARCHAR(64) DEFAULT '',
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (evaluation_id),
  KEY idx_barn_id (barn_id),
  KEY idx_reservation_id (reservation_id),
  KEY idx_evaluator (evaluator_id)
) ENGINE=InnoDB AUTO_INCREMENT=100000 DEFAULT CHARSET=utf8mb4;

-- 13. 管护队伍表
CREATE TABLE maintenance_team (
  team_id BIGINT NOT NULL AUTO_INCREMENT,
  team_name VARCHAR(100) NOT NULL,
  team_type VARCHAR(50) NOT NULL,
  county_code VARCHAR(20) DEFAULT NULL,
  county_name VARCHAR(50) DEFAULT NULL,
  leader_name VARCHAR(50) DEFAULT NULL,
  leader_phone VARCHAR(20) DEFAULT NULL,
  member_count INT DEFAULT 0,
  service_area VARCHAR(255) DEFAULT NULL,
  repair_count INT DEFAULT 0,
  status CHAR(1) DEFAULT '0',
  create_by VARCHAR(64) DEFAULT '',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_by VARCHAR(64) DEFAULT '',
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  remark VARCHAR(500) DEFAULT NULL,
  PRIMARY KEY (team_id)
) ENGINE=InnoDB AUTO_INCREMENT=1000 DEFAULT CHARSET=utf8mb4;

-- 14. 资金管理表
CREATE TABLE fund_management (
  fund_id BIGINT NOT NULL AUTO_INCREMENT,
  fund_year INT NOT NULL,
  county_code VARCHAR(20) DEFAULT NULL,
  county_name VARCHAR(50) DEFAULT NULL,
  fund_source VARCHAR(50) NOT NULL,
  total_amount DECIMAL(12,2) DEFAULT 0.00,
  used_amount DECIMAL(12,2) DEFAULT 0.00,
  allocated_amount DECIMAL(12,2) DEFAULT 0.00,
  repair_count INT DEFAULT 0,
  avg_roi DECIMAL(5,2) DEFAULT NULL,
  create_by VARCHAR(64) DEFAULT '',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_by VARCHAR(64) DEFAULT '',
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  remark VARCHAR(500) DEFAULT NULL,
  PRIMARY KEY (fund_id),
  KEY idx_year (fund_year),
  KEY idx_county (county_code)
) ENGINE=InnoDB AUTO_INCREMENT=1000 DEFAULT CHARSET=utf8mb4;

-- 视图：烤房健康统计
CREATE OR REPLACE VIEW v_barn_health_stats AS
SELECT
  county_code, county_name,
  COUNT(*) as total_count,
  SUM(CASE WHEN health_level = '优良' THEN 1 ELSE 0 END) as excellent_count,
  SUM(CASE WHEN health_level = '需维护' THEN 1 ELSE 0 END) as maintain_count,
  SUM(CASE WHEN health_level = '急需修复' THEN 1 ELSE 0 END) as urgent_count,
  SUM(CASE WHEN health_level = '退出' THEN 1 ELSE 0 END) as retired_count,
  ROUND(AVG(health_score), 2) as avg_score
FROM barn_project
GROUP BY county_code, county_name;

-- 视图：区域分布统计
CREATE OR REPLACE VIEW v_barn_region_stats AS
SELECT
  city_name, county_name, town_name,
  COUNT(*) as barn_count,
  ROUND(AVG(health_score), 2) as avg_health_score,
  SUM(CASE WHEN use_status = '在用' THEN 1 ELSE 0 END) as in_use_count,
  SUM(CASE WHEN use_status = '闲置' THEN 1 ELSE 0 END) as idle_count
FROM barn_project
GROUP BY city_name, county_name, town_name;

SET FOREIGN_KEY_CHECKS = 1;

SELECT '表结构创建完成' as result;
