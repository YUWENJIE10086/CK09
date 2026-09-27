-- 烤房新建数据表（独立于烤房选址）
-- 说明：
-- 1. site_new_build_candidates：新建点主表，承载地图新增、批量新增、算法评价结果
-- 2. site_new_build_scenarios：新建方案/沙盘推演主表，后续用于保存一次模拟
-- 3. site_new_build_village_stats：按村统计的容量/负载/均衡度结果，后续用于对比前后变化

CREATE TABLE IF NOT EXISTS site_new_build_candidates (
  id INT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  candidate_name VARCHAR(100) NOT NULL COMMENT '点名称',
  township VARCHAR(50) DEFAULT '' COMMENT '乡镇',
  village VARCHAR(50) DEFAULT '' COMMENT '村',
  longitude DECIMAL(10,6) NOT NULL COMMENT '经度',
  latitude DECIMAL(10,6) NOT NULL COMMENT '纬度',
  altitude INT DEFAULT NULL COMMENT '海拔(米)',
  area_sqm DECIMAL(10,2) DEFAULT NULL COMMENT '占地面积(平方米)',
  slope_degree DECIMAL(6,2) DEFAULT NULL COMMENT '坡度(度)',
  distance_road DECIMAL(10,2) DEFAULT NULL COMMENT '距公路距离(米)',
  distance_power DECIMAL(10,2) DEFAULT NULL COMMENT '距电力距离(米)',
  distance_water DECIMAL(10,2) DEFAULT NULL COMMENT '距水源距离(米)',
  tobacco_area DECIMAL(10,2) DEFAULT NULL COMMENT '服务烟田面积(亩)',
  land_type VARCHAR(20) DEFAULT '' COMMENT '土地类型',
  remark VARCHAR(500) DEFAULT '' COMMENT '备注',
  status VARCHAR(20) DEFAULT '待评估' COMMENT '状态',
  score_terrain DECIMAL(6,2) DEFAULT NULL COMMENT '地形地势得分',
  score_hydro DECIMAL(6,2) DEFAULT NULL COMMENT '水文条件得分',
  score_power DECIMAL(6,2) DEFAULT NULL COMMENT '电力保障得分',
  score_traffic DECIMAL(6,2) DEFAULT NULL COMMENT '交通条件得分',
  score_tobacco_match DECIMAL(6,2) DEFAULT NULL COMMENT '烟田匹配得分',
  score_environment DECIMAL(6,2) DEFAULT NULL COMMENT '环境安全得分',
  score_cluster DECIMAL(6,2) DEFAULT NULL COMMENT '集群效益得分',
  score_cost DECIMAL(6,2) DEFAULT NULL COMMENT '成本经济得分',
  ahp_weight DECIMAL(10,4) DEFAULT NULL COMMENT 'AHP加权分',
  topsis_score DECIMAL(10,4) DEFAULT NULL COMMENT 'TOPSIS贴近度',
  ranking INT DEFAULT NULL COMMENT '排名',
  total_score DECIMAL(10,2) DEFAULT NULL COMMENT '综合得分',
  suitability_level VARCHAR(20) DEFAULT NULL COMMENT '适宜等级',
  del_flag TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  KEY idx_new_build_town_village (township, village),
  KEY idx_new_build_status_rank (status, ranking),
  KEY idx_new_build_lng_lat (longitude, latitude)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='烤房新建候选点表';

CREATE TABLE IF NOT EXISTS site_new_build_scenarios (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  scenario_name VARCHAR(100) NOT NULL COMMENT '方案名称',
  target_year INT DEFAULT NULL COMMENT '目标年份',
  description VARCHAR(500) DEFAULT '' COMMENT '方案描述',
  total_village_count INT NOT NULL DEFAULT 0 COMMENT '涉及村数量',
  total_point_count INT NOT NULL DEFAULT 0 COMMENT '新增点数量',
  average_load_before DECIMAL(10,4) DEFAULT NULL COMMENT '新增前平均负载',
  average_load_after DECIMAL(10,4) DEFAULT NULL COMMENT '新增后平均负载',
  variance_before DECIMAL(10,4) DEFAULT NULL COMMENT '新增前方差',
  variance_after DECIMAL(10,4) DEFAULT NULL COMMENT '新增后方差',
  status VARCHAR(20) NOT NULL DEFAULT '草稿' COMMENT '状态',
  remark VARCHAR(500) DEFAULT '' COMMENT '备注',
  created_by VARCHAR(64) DEFAULT '' COMMENT '创建人',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  KEY idx_new_build_scenario_status (status, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='烤房新建推演方案表';

CREATE TABLE IF NOT EXISTS site_new_build_village_stats (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  scenario_id BIGINT NOT NULL COMMENT '方案ID',
  township VARCHAR(50) DEFAULT '' COMMENT '乡镇',
  village VARCHAR(50) NOT NULL COMMENT '村',
  existing_barn_count INT NOT NULL DEFAULT 0 COMMENT '现有烤房数',
  existing_capacity DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT '现有产能',
  existing_load DECIMAL(12,4) NOT NULL DEFAULT 0 COMMENT '现有负载',
  planned_new_count INT NOT NULL DEFAULT 0 COMMENT '新增烤房数',
  planned_new_capacity DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT '新增产能',
  planned_load DECIMAL(12,4) NOT NULL DEFAULT 0 COMMENT '新增后负载',
  avg_load_before DECIMAL(12,4) NOT NULL DEFAULT 0 COMMENT '新增前平均负载',
  avg_load_after DECIMAL(12,4) NOT NULL DEFAULT 0 COMMENT '新增后平均负载',
  load_delta DECIMAL(12,4) NOT NULL DEFAULT 0 COMMENT '平均负载变化',
  balance_score DECIMAL(12,4) DEFAULT NULL COMMENT '均衡度评分',
  note VARCHAR(500) DEFAULT '' COMMENT '备注',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  KEY idx_new_build_village_stats (scenario_id, township, village)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='烤房新建按村统计表';
