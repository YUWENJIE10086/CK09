# 数据库合并方案分析

> 小程序数据库 `KaoF` vs 后台管理系统数据库 `barn_management`
> 日期: 2026-07-11 | 仅分析方案，不修改任何数据

---

## 一、整体概览

| 维度 | 小程序 (KaoF) | 后台系统 (barn_management) |
|------|--------------|--------------------------|
| 表数量 | 12 张 | 20 张 + 3 视图 |
| 核心烤房数据 | 6 条 (ovens) | 2642 条 (barn_project) |
| ID 体系 | VARCHAR 主键 (如 `YN001`, `10-420527KF00001`) | BIGINT 自增主键 |
| 数据来源 | Excel 导入 + 手动初始化 | Excel 导入 (2642条烤房) |
| 定位 | 烟农端小程序 | 后台管理端 |

### 小程序 12 张表

| # | 表名 | 说明 | 数据量 |
|---|------|------|--------|
| 0 | `kf_basedata` | 烤房项目基础信息（Excel权威源） | - |
| 1 | `pound_groups` | 磅组表 | 3 |
| 2 | `users` | 烟农用户表 | 1 |
| 3 | `bakers` | 烘烤师表 | 3 |
| 4 | `ovens` | 烤房表 | 6 |
| 5 | `oven_components` | 设备部件表 | 24 |
| 6 | `user_favorites` | 用户收藏表 | 2 |
| 7 | `planting_records` | 种植档案表 | 3 |
| 8 | `oven_assignments` | 烤房分配表 | 4 |
| 8b | `reservations` | 预约记录（已废弃） | 0 |
| 9 | `baking_records` | 烘烤记录表 | 2 |
| 10 | `evaluations` | 评价反馈表 | 3 |

### 后台系统 20 张表

| # | 表名 | 说明 | 数据量 | 状态 |
|---|------|------|--------|------|
| 1 | `barn_project` | 烤房项目表 | 2642 | **主力表** |
| 2 | `barn_component` | 烤房部件表 | 0 | 空表 |
| 3 | `barn_assignment` | 烤房分配表 | 0 | 空表 |
| 4 | `evaluation_record` | 评价记录表 | 17 | 有数据 |
| 5 | `repair_record` | 维修记录表 | 60 | 有数据 |
| 6 | `fund_management` | 资金管理表 | 0 | 空表 |
| 7 | `maintenance_team` | 管护队伍表 | 17 | 有数据 |
| 8 | `sys_user` | 系统用户表 | 4 | 管理员 |
| 9 | `sys_city` | 市表 | 1 | 字典 |
| 10 | `sys_county` | 县区表 | 5 | 字典 |
| 11 | `sys_township` | 乡镇表 | 19 | 字典 |
| 12 | `sys_village` | 村表 | 98 | 字典 |
| 13 | `sys_project_type` | 项目类型表 | 3 | 字典 |
| 14 | `barn_burner` | 烤房燃烧器表 | 1109 | 有数据 |
| 15 | `t_barn_info` | 旧烤房表 | 5 | **废弃** |
| 16 | `t_barn_component` | 旧部件表 | 0 | **废弃** |
| 17 | `t_evaluation` | 旧评价表 | 0 | **废弃** |
| 18 | `t_fund_management` | 旧资金表 | 0 | **废弃** |
| 19 | `t_maintenance_team` | 旧队伍表 | 4 | **废弃** |
| 20 | `t_region` | 旧区域表 | 8 | **废弃** |
| 21 | `t_repair_record` | 旧维修表 | 0 | **废弃** |
| 22 | `t_reservation` | 旧预约表 | 0 | **废弃** |
| 23 | `t_user_ext` | 旧用户扩展 | 0 | **废弃** |

---

## 二、逐表对比分析

### 2.1 烤房核心表

#### 小程序: `kf_basedata` + `ovens` → 后台: `barn_project`

小程序将烤房分为两张表：`kf_basedata`（Excel 基础数据）和 `ovens`（业务数据）。
后台合并为一张 `barn_project` 表，已包含基础字段 + 业务字段。

**字段映射关系:**

| 小程序 kf_basedata / ovens 字段 | 后台 barn_project 字段 | 差异说明 |
|-------------------------------|----------------------|---------|
| `kf_basedata.project_id` | `project_code` | 小程序用 project_id 做 PK，后台用自增 barn_id |
| `ovens.id` (VARCHAR) | `project_code` | 小程序 ovens.id = kf_basedata.project_id |
| `ovens.base_id` | — | 后台已合并，无需关联 |
| `kf_basedata.use_status` | `use_status` | ✅ 一致 |
| `kf_basedata.idle_years` | `idle_years` | ✅ 一致 |
| `kf_basedata.diversion_year` | `transfer_year` | 字段名不同，语义相同 |
| `kf_basedata.diversion_purpose` | `transfer_use` | 字段名不同，语义相同 |
| `kf_basedata.damage_year` | `damage_year` | ✅ 一致 |
| `kf_basedata.damage_reason` | `damage_reason` | ✅ 一致 |
| `kf_basedata.build_method` | `build_mode` | 字段名不同 |
| `kf_basedata.project_type` | `project_type` | ✅ 一致 |
| `kf_basedata.oven_length/width/height` | `length_m/width_m/height_m` | 字段名不同 |
| `kf_basedata.project_cost` | `project_cost` | ✅ 一致 |
| `kf_basedata.natl_subsidy` | `subsidy_national` | 字段名不同 |
| `kf_basedata.local_subsidy` | `subsidy_region` | 字段名不同 |
| `kf_basedata.total_subsidy` | `subsidy_total` | 字段名不同 |
| `kf_basedata.start_date` | `start_date` | ✅ 一致 |
| `kf_basedata.finish_date` | `complete_date` | 字段名不同 |
| `kf_basedata.city` | `city_name` | ✅ 语义一致 |
| `kf_basedata.county` | `county_name` | ✅ 语义一致 |
| `kf_basedata.township` | `town_name` | ✅ 语义一致 |
| `kf_basedata.village` | `village_name` | ✅ 语义一致 |
| `kf_basedata.detail_address` | `address` | 字段名不同 |
| `kf_basedata.project_owner` | `project_owner` | ✅ 一致 |
| `kf_basedata.build_company` | `construction_unit` | 字段名不同 |
| `kf_basedata.altitude` | `elevation` | 字段名不同 |
| `kf_basedata.longitude` | `longitude` | ✅ 一致 |
| `kf_basedata.latitude` | `latitude` | ✅ 一致 |
| `kf_basedata.technician_name` | — | ⚠️ 后台缺失 |
| `kf_basedata.heater_filled` | `has_heating` (tinyint) | 类型不同：VARCHAR→TINYINT |
| `kf_basedata.radiator_filled` | `has_radiator` (tinyint) | 类型不同 |
| `kf_basedata.controller_filled` | `has_autocontrol` (tinyint) | 类型不同 |
| `kf_basedata.oven_body_filled` | `has_main` (tinyint) | 类型不同 |
| `kf_basedata.annex_filled` | `has_ancillary` (tinyint) | 类型不同 |
| `kf_basedata.city_code` | `city_code` | ✅ 一致 |
| `kf_basedata.county_code` | `county_code` | ✅ 一致 |
| `kf_basedata.township_code` | `town_code` | ✅ 一致 |
| `kf_basedata.village_code` | `village_code` | ✅ 一致 |
| `kf_basedata.rel_station` | `station_name` | 字段名不同 |
| `ovens.status` (ENUM) | `use_status` (VARCHAR) | 小程序用 ENUM(idle/baking等)，后台用中文(在用/闲置等) |
| `ovens.health_score` | `health_score` | ✅ 一致 |
| `ovens.temperature` | — | ⚠️ 后台缺失 |
| `ovens.humidity` | — | ⚠️ 后台缺失 |
| `ovens.baker_id` | — | ⚠️ 后台缺失 |
| `ovens.usage_count` | — | ⚠️ 后台缺失 |
| `ovens.distance` | — | ⚠️ 后台缺失（小程序特有） |
| — | `health_level` | 后台多出健康等级字段 |
| — | `predicted_life_years` | 后台多出预测寿命字段 |
| — | `is_idle` | 后台多出空闲标志 |
| — | `status` | 后台多出审核状态 |

**结论:** 后台 `barn_project` 已经是 `kf_basedata + ovens` 的合并增强版，2642 条数据远超小程序 6 条。需要补齐小程序特有字段（temperature, humidity, baker_id, usage_count, technician_name）。

---

#### 2.2 设备部件表

| 小程序 `oven_components` | 后台 `barn_component` | 差异 |
|------------------------|---------------------|------|
| `id` INT AUTO | `component_id` BIGINT AUTO | 类型不同 |
| `oven_id` VARCHAR(30) | `barn_id` BIGINT | 类型不同（VARCHAR→BIGINT） |
| `component_key` (heater/fan/controller/sensor) | `component_type` | 字段名不同 |
| `component_name` | `component_name` | ✅ 一致 |
| `status` ENUM(normal/warning/fault/missing) | `component_status` VARCHAR(正常等) | 类型+值不同 |
| `value` (当前值描述) | — | ⚠️ 后台缺失 |
| `last_check_at` | — | ⚠️ 后台缺失 |
| — | `damage_level` | 后台多出损坏程度 |
| — | `score` | 后台多出评分 |
| — | `weight` | 后台多出权重 |
| — | `repair_year` | 后台多出维修年份 |

**数据量:** 小程序 24 条 vs 后台 0 条

**结论:** 后台部件表为空，需要从小程序同步部件数据。后台多了 score/weight 等健康评估字段，小程序多了 value/last_check_at 等运行状态字段。需要互相补齐。

---

#### 2.3 烤房分配表

| 小程序 `oven_assignments` | 后台 `barn_assignment` | 差异 |
|-------------------------|---------------------|------|
| `id` VARCHAR(30) (ASG2026001) | `assignment_id` BIGINT AUTO | 类型不同 |
| `user_id` VARCHAR(20) | `farmer_id` BIGINT | 字段名+类型不同 |
| `oven_id` VARCHAR(30) | `barn_id` BIGINT | 字段名+类型不同 |
| `baker_id` VARCHAR(20) | — | ⚠️ 后台缺失 |
| `season_year` SMALLINT | — | ⚠️ 后台缺失 |
| `season_name` VARCHAR(30) | — | ⚠️ 后台缺失 |
| `status` ENUM(assigned/active/completed/cancelled) | `assign_status` VARCHAR(已分配等) | 字段名+类型不同 |
| `assigned_by` VARCHAR(50) | `assign_by` VARCHAR(64) | 字段名不同 |
| `assigned_at` DATETIME | `assign_time` DATETIME | 字段名不同 |
| `remark` | `assign_remark` | 字段名不同 |
| — | `farmer_name` | 后台多出冗余字段 |
| — | `farmer_phone` | 后台多出冗余字段 |
| — | `barn_name` | 后台多出冗余字段 |
| — | `project_code` | 后台多出冗余字段 |
| — | `return_date` | 后台多出归还日期 |

**数据量:** 小程序 4 条 vs 后台 0 条

**结论:** 后台分配表为空，需要同步小程序数据。但后台缺少烘烤师关联(baker_id)和烘烤季(season_year/season_name)字段。

---

#### 2.4 评价表

| 小程序 `evaluations` | 后台 `evaluation_record` | 差异 |
|---------------------|------------------------|------|
| `id` INT AUTO | `evaluation_id` BIGINT AUTO | 类型不同 |
| `baking_record_id` INT | — | ⚠️ 后台缺失 |
| `reservation_id` VARCHAR | `reservation_id` BIGINT | 类型不同 |
| `assignment_id` VARCHAR | — | ⚠️ 后台缺失 |
| `user_id` VARCHAR(20) | `evaluator_id` BIGINT | 字段名+类型不同 |
| `oven_id` VARCHAR(30) | `barn_id` BIGINT | 字段名+类型不同 |
| `baker_id` VARCHAR(20) | `baker_id` BIGINT | 类型不同 |
| `overall_rating` TINYINT (1-5) | — | ⚠️ 后台缺失总体评分 |
| `baker_rating` TINYINT | `baker_score` INT | 字段名不同 |
| `temp_control_rating` TINYINT | — | ⚠️ 后台缺失温控评分 |
| `equipment_rating` TINYINT | `barn_score` INT | 语义近似 |
| `comment` TEXT | `barn_comment` VARCHAR(500) | 字段名不同 |
| `baker_comment` TEXT | `baker_comment` VARCHAR(500) | ✅ 一致 |
| `images` JSON | — | ⚠️ 后台缺失 |
| `evaluate_time` DATETIME | `evaluate_time` DATETIME | ✅ 一致 |
| — | `evaluator_name` | 后台多出冗余 |
| — | `baker_name` | 后台多出冗余 |
| — | `project_code` | 后台多出冗余 |
| — | `barn_name` | 后台多出冗余 |

**数据量:** 小程序 3 条 vs 后台 17 条

**结论:** 后台已有 17 条评价数据。小程序有更细粒度的评分维度（总体/烘烤师/温控/设备），后台只有烤房分+烘烤师分。需要补齐 overall_rating, temp_control_rating, equipment_rating, images 字段。

---

#### 2.5 用户表

| 小程序 `users` (烟农) | 后台 `sys_user` (管理员) | 差异 |
|----------------------|------------------------|------|
| `id` VARCHAR(20) (YN001) | `user_id` BIGINT AUTO | 完全不同 |
| `name` | `nick_name` | 字段名不同 |
| `phone` | `phone` | ✅ 一致 |
| `password_hash` | `password` | 字段名不同 |
| `credit_level` CHAR(1) A/B/C/D | — | ⚠️ 后台缺失 |
| `credit_score` SMALLINT | — | ⚠️ 后台缺失 |
| `area` | — | ⚠️ 后台缺失 |
| `pound_group_id` INT | — | ⚠️ 后台缺失 |
| `planting_area` DECIMAL | — | ⚠️ 后台缺失 |
| `planting_years` TINYINT | — | ⚠️ 后台缺失 |
| `total_bakes` INT | — | ⚠️ 后台缺失 |
| `avatar_url` | `avatar` | 字段名不同 |
| `openid` | — | ⚠️ 后台缺失 |
| `unionid` | — | ⚠️ 后台缺失 |

**结论:** 语义完全不同。`sys_user` 是后台管理员，小程序 `users` 是烟农。**需要新建烟农表**。

---

#### 2.6 烘烤师表

小程序有 `bakers` 表（3条数据），**后台完全没有此表**。

| 字段 | 说明 |
|------|------|
| `id` VARCHAR(20) | BK001 |
| `name` | 姓名 |
| `phone` | 电话 |
| `experience` TINYINT | 从业年限 |
| `rating` DECIMAL(2,1) | 综合评分 0-5 |
| `total_orders` INT | 服务总单数 |
| `pound_group_id` INT | 所属磅组 |
| `status` TINYINT | 1在岗 0离职 |

**结论:** 需要新建烘烤师表。

---

### 2.7 小程序独有表（后台完全缺失）

| 小程序表 | 数据量 | 说明 | 后台对应 |
|---------|--------|------|---------|
| `pound_groups` | 3 | 磅组表 | 无，需新建 |
| `bakers` | 3 | 烘烤师表 | 无，需新建 |
| `user_favorites` | 2 | 用户收藏表 | 无，需新建 |
| `planting_records` | 3 | 种植档案表 | 无，需新建 |
| `baking_records` | 2 | 烘烤记录表 | 无，需新建 |
| `reservations` | 0 | 预约表（已废弃） | `t_reservation`(空)，可跳过 |

### 2.8 后台独有表（小程序没有，保留）

| 后台表 | 数据量 | 说明 | 保留策略 |
|-------|--------|------|---------|
| `repair_record` | 60 | 维修记录 | ✅ 保留，小程序无此功能 |
| `fund_management` | 0 | 资金管理 | ✅ 保留 |
| `maintenance_team` | 17 | 管护队伍 | ✅ 保留 |
| `sys_city/county/township/village` | 1/5/19/98 | 行政区划字典 | ✅ 保留，优于小程序内联地址 |
| `sys_project_type` | 3 | 项目类型字典 | ✅ 保留 |
| `barn_burner` | 1109 | 烤房燃烧器 | ✅ 保留 |
| `sys_user` | 4 | 管理员 | ✅ 保留 |

### 2.9 后台废弃表（t_ 前缀，建议清理）

| 废弃表 | 数据量 | 对应的新表 |
|-------|--------|----------|
| `t_barn_info` | 5 | → `barn_project` |
| `t_barn_component` | 0 | → `barn_component` |
| `t_evaluation` | 0 | → `evaluation_record` |
| `t_fund_management` | 0 | → `fund_management` |
| `t_maintenance_team` | 4 | → `maintenance_team` |
| `t_region` | 8 | → `sys_county/township/village` |
| `t_repair_record` | 0 | → `repair_record` |
| `t_reservation` | 0 | 已废弃 |
| `t_user_ext` | 0 | 将被新烟农表取代 |

---

## 三、核心差异总结

### 3.1 ID 体系冲突（最关键）

| 维度 | 小程序 | 后台系统 |
|------|--------|---------|
| 烤房ID | VARCHAR `10-420527KF00001` | BIGINT 自增 |
| 用户ID | VARCHAR `YN001` | BIGINT 自增 |
| 烘烤师ID | VARCHAR `BK001` | 无 |
| 分配ID | VARCHAR `ASG2026001` | BIGINT 自增 |

**合并策略:** 以 `project_code`（VARCHAR）作为烤房的业务唯一标识，两套系统通过 project_code 关联。`barn_id`（BIGINT）保留为后台内部主键，对外通过 project_code 交互。

### 3.2 状态枚举体系冲突

| 业务 | 小程序 | 后台系统 |
|------|--------|---------|
| 烤房状态 | ENUM: idle/waiting/baking/maintenance/reserved | VARCHAR: 在用/闲置/转用/损毁 |
| 部件状态 | ENUM: normal/warning/fault/missing | VARCHAR: 正常/异常等 |
| 分配状态 | ENUM: assigned/active/completed/cancelled | VARCHAR: 已分配/使用中/已归还/已撤销 |

**合并策略:** 后台保留中文 VARCHAR 风格（适合管理端展示），在接口层做中英映射。小程序端使用英文 ENUM，通过 API 转换。

### 3.3 数据归属

| 数据 | 权威来源 | 说明 |
|------|---------|------|
| 烤房基础信息 | **后台 barn_project** (2642条) | 远超小程序 6 条，以后台为准 |
| 行政区划 | **后台 sys_county/township/village** | 结构化字典，优于小程序内联 |
| 烟农信息 | **小程序 users** | 后台无此数据，需同步 |
| 烘烤师信息 | **小程序 bakers** | 后台无此数据，需同步 |
| 磅组信息 | **小程序 pound_groups** | 后台无此数据，需同步 |
| 部件运行状态 | **小程序 oven_components** | 后台部件表为空，需同步 |
| 分配记录 | **小程序 oven_assignments** | 后台分配表为空，需同步 |
| 烘烤记录 | **小程序 baking_records** | 后台无此表，需新建 |
| 种植档案 | **小程序 planting_records** | 后台无此表，需新建 |
| 收藏 | **小程序 user_favorites** | 后台无此表，需新建 |
| 评价 | **双向同步** | 后台 17 条 + 小程序 3 条 |
| 维修/资金/队伍 | **后台独有** | 小程序无此功能 |

---

## 四、合并方案

### 方案原则

1. **以后台 `barn_project` 为烤房数据权威源**（2642条 vs 6条）
2. **以小程序表结构为业务模型标准**，后台补齐缺失表
3. **ID 体系不强行统一**，通过 `project_code` 做业务关联
4. **废弃 t_ 前缀旧表**，统一使用新表
5. **保留后台独有功能表**（维修/资金/队伍/字典）

### 步骤一: 新建缺失表（6张）

以下表在小程序有、后台没有，需要按小程序结构新建：

```
1. pound_groups        — 磅组表
2. bakers              — 烘烤师表
3. barn_farmer         — 烟农表（对应小程序 users，改名避免与 sys_user 冲突）
4. user_favorites      — 收藏表
5. planting_records    — 种植档案表
6. baking_records      — 烘烤记录表
```

**ID 策略调整:** 烟农表用 BIGINT 自增主键 + `farmer_code`(VARCHAR) 业务编号（对应小程序的 YN001），烘烤师表同理用 `baker_code`。

### 步骤二: 补齐现有表字段

#### 2a. `barn_project` 补齐字段

```sql
-- 小程序 ovens 有、后台 barn_project 缺失的字段
ALTER TABLE barn_project ADD COLUMN temperature DECIMAL(5,1) DEFAULT 25.0 COMMENT '当前温度(°C)';
ALTER TABLE barn_project ADD COLUMN humidity DECIMAL(5,1) DEFAULT 60.0 COMMENT '当前湿度(%)';
ALTER TABLE barn_project ADD COLUMN baker_id BIGINT DEFAULT NULL COMMENT '当前负责烘烤师ID';
ALTER TABLE barn_project ADD COLUMN usage_count INT DEFAULT 0 COMMENT '累计使用次数';
ALTER TABLE barn_project ADD COLUMN technician_name VARCHAR(50) DEFAULT NULL COMMENT '技术员姓名';
ALTER TABLE barn_project ADD COLUMN pound_group_id INT DEFAULT NULL COMMENT '所属磅组ID';
```

#### 2b. `barn_component` 补齐字段

```sql
-- 小程序 oven_components 有、后台 barn_component 缺失的字段
ALTER TABLE barn_component ADD COLUMN component_value VARCHAR(100) DEFAULT NULL COMMENT '当前值/状态描述';
ALTER TABLE barn_component ADD COLUMN last_check_at DATETIME DEFAULT NULL COMMENT '最后检测时间';
```

#### 2c. `barn_assignment` 补齐字段

```sql
-- 小程序 oven_assignments 有、后台 barn_assignment 缺失的字段
ALTER TABLE barn_assignment ADD COLUMN baker_id BIGINT DEFAULT NULL COMMENT '烘烤师ID';
ALTER TABLE barn_assignment ADD COLUMN season_year SMALLINT DEFAULT NULL COMMENT '分配年度';
ALTER TABLE barn_assignment ADD COLUMN season_name VARCHAR(30) DEFAULT NULL COMMENT '烘烤季名称';
```

#### 2d. `evaluation_record` 补齐字段

```sql
-- 小程序 evaluations 有、后台 evaluation_record 缺失的字段
ALTER TABLE evaluation_record ADD COLUMN overall_rating TINYINT DEFAULT NULL COMMENT '总体评分1-5星';
ALTER TABLE evaluation_record ADD COLUMN temp_control_rating TINYINT DEFAULT NULL COMMENT '温控评分1-5星';
ALTER TABLE evaluation_record ADD COLUMN equipment_rating TINYINT DEFAULT NULL COMMENT '设备完好度评分1-5星';
ALTER TABLE evaluation_record ADD COLUMN baking_record_id BIGINT DEFAULT NULL COMMENT '关联烘烤记录ID';
ALTER TABLE evaluation_record ADD COLUMN assignment_id BIGINT DEFAULT NULL COMMENT '关联分配单号';
ALTER TABLE evaluation_record ADD COLUMN images JSON DEFAULT NULL COMMENT '图片URL列表';
```

### 步骤三: 数据同步

#### 3a. 磅组数据同步（3条）

```
小程序 pound_groups → 后台新 pound_groups
直接导入：磨坪磅组、梅家河磅组、柏家坪磅组
```

#### 3b. 烘烤师数据同步（3条）

```
小程序 bakers → 后台新 bakers
BK001 李师傅 → baker_code=BK001
BK002 王师傅 → baker_code=BK002
BK003 张师傅 → baker_code=BK003
```

#### 3c. 烟农数据同步（1条）

```
小程序 users → 后台新 barn_farmer
YN001 张三 → farmer_code=YN001
```

#### 3d. 部件数据同步（24条）

```
小程序 oven_components → 后台 barn_component
通过 project_code 关联 barn_id
component_key → component_type 映射:
  heater → 加热设备
  fan → 风机
  controller → 自控仪
  sensor → 温湿度传感器
status 映射:
  normal → 正常
  warning → 预警
  fault → 故障
  missing → 缺失
```

#### 3e. 分配数据同步（4条）

```
小程序 oven_assignments → 后台 barn_assignment
通过 oven_id(project_code) 关联 barn_id
通过 user_id(farmer_code) 关联 farmer_id
通过 baker_id(baker_code) 关联 baker_id
status 映射:
  assigned → 已分配
  active → 使用中
  completed → 已归还
  cancelled → 已撤销
```

#### 3f. 烘烤记录同步（2条）

```
小程序 baking_records → 后台新 baking_records
通过 oven_id(project_code) 关联 barn_id
```

#### 3g. 种植档案同步（3条）

```
小程序 planting_records → 后台新 planting_records
通过 user_id(farmer_code) 关联 farmer_id
```

#### 3h. 收藏同步（2条）

```
小程序 user_favorites → 后台新 user_favorites
通过 user_id(farmer_code) 关联 farmer_id
通过 oven_id(project_code) 关联 barn_id
```

#### 3i. 评价数据合并

```
后台已有 17 条 evaluation_record（保留不动）
小程序 3 条 evaluations → 通过 project_code 关联 barn_id 后导入
若存在重复（同 barn_id + 同 evaluate_time），跳过
```

### 步骤四: 废弃旧表清理

```sql
-- 确认无引用后删除 t_ 前缀旧表
DROP TABLE IF EXISTS t_barn_info;
DROP TABLE IF EXISTS t_barn_component;
DROP TABLE IF EXISTS t_evaluation;
DROP TABLE IF EXISTS t_fund_management;
DROP TABLE IF EXISTS t_maintenance_team;
DROP TABLE IF EXISTS t_region;
DROP TABLE IF EXISTS t_repair_record;
DROP TABLE IF EXISTS t_reservation;
DROP TABLE IF EXISTS t_user_ext;
```

### 步骤五: 视图更新

更新 3 个视图使其引用新表结构：
- `v_barn_health_stats`
- `v_barn_region_stats`
- `v_repair_stats`

---

## 五、合并后表结构总览

合并后后台数据库共 **20 张表 + 3 视图**：

| 分类 | 表名 | 来源 | 数据量 |
|------|------|------|--------|
| **烤房核心** | `barn_project` | 后台（补齐字段） | 2642 |
| | `barn_component` | 后台（补齐字段+同步数据） | 24 |
| | `barn_burner` | 后台原有 | 1109 |
| **用户体系** | `sys_user` | 后台原有（管理员） | 4 |
| | `barn_farmer` 🆕 | 新建（同步小程序） | 1 |
| | `bakers` 🆕 | 新建（同步小程序） | 3 |
| **业务流转** | `barn_assignment` | 后台（补齐字段+同步数据） | 4 |
| | `baking_records` 🆕 | 新建（同步小程序） | 2 |
| | `planting_records` 🆕 | 新建（同步小程序） | 3 |
| | `user_favorites` 🆕 | 新建（同步小程序） | 2 |
| **评价反馈** | `evaluation_record` | 后台（补齐字段+合并数据） | 20 |
| **运维管理** | `repair_record` | 后台原有 | 60 |
| | `fund_management` | 后台原有 | 0 |
| | `maintenance_team` | 后台原有 | 17 |
| **字典** | `sys_city` | 后台原有 | 1 |
| | `sys_county` | 后台原有 | 5 |
| | `sys_township` | 后台原有 | 19 |
| | `sys_village` | 后台原有 | 98 |
| | `sys_project_type` | 后台原有 | 3 |
| | `pound_groups` 🆕 | 新建（同步小程序） | 3 |
| **视图** | `v_barn_health_stats` | 更新 | - |
| | `v_barn_region_stats` | 更新 | - |
| | `v_repair_stats` | 更新 | - |

---

## 六、字段映射对照（关键字段）

### 6.1 烤房状态映射

| 小程序 ENUM | 后台 VARCHAR | 中文含义 |
|------------|-------------|---------|
| `idle` | `闲置` | 空闲 |
| `waiting` | — | 待机（后台无对应） |
| `baking` | `在用` | 烘烤中 |
| `maintenance` | — | 维修中（后台用 repair_record 表管理） |
| `reserved` | `转用` | 已分配/转用 |

### 6.2 分配状态映射

| 小程序 ENUM | 后台 VARCHAR | 中文含义 |
|------------|-------------|---------|
| `assigned` | `已分配` | 已分配 |
| `active` | `使用中` | 使用中 |
| `completed` | `已归还` | 已完成 |
| `cancelled` | `已撤销` | 已取消 |

### 6.3 部件状态映射

| 小程序 ENUM | 后台 VARCHAR | 中文含义 |
|------------|-------------|---------|
| `normal` | `正常` | 正常 |
| `warning` | `预警` | 预警 |
| `fault` | `故障` | 故障 |
| `missing` | `缺失` | 缺失 |

### 6.4 部件类型映射

| 小程序 component_key | 后台 component_type | 中文 |
|---------------------|-------------------|------|
| `heater` | 加热设备 | 加热设备 |
| `fan` | 风机 | 风机 |
| `controller` | 自控仪 | 自控仪 |
| `sensor` | 温湿度传感器 | 传感器 |

---

## 七、风险与注意事项

1. **ID 映射表**: 同步数据时需要建立 `project_code → barn_id`、`farmer_code → farmer_id`、`baker_code → baker_id` 的映射关系，确保外键引用正确
2. **project_code 冲突**: 小程序 ovens 有 `09-420527KF00104` 这个编号在 barn_project 中需确认是否存在，不存在则需新建
3. **评价去重**: 合并评价数据时需检查 barn_id + evaluate_time 是否重复
4. **后端代码适配**: 新增表后需要对应的 Entity/Mapper/Controller/Service 代码
5. **前端适配**: 烘烤记录、种植档案、收藏等新功能需要前端页面
6. **API 对齐**: 小程序端 API 需要与后台保持一致，特别是 ID 传递方式（project_code vs barn_id）
7. **备份**: 执行任何合并操作前，必须完整备份 `barn_management` 数据库

---

## 八、执行顺序建议

```
1. 备份 barn_management 数据库
2. 新建 6 张缺失表（pound_groups, bakers, barn_farmer, user_favorites, planting_records, baking_records）
3. 补齐 4 张现有表字段（barn_project, barn_component, barn_assignment, evaluation_record）
4. 同步小程序基础数据（磅组→烘烤师→烟农→部件→分配→烘烤记录→种植档案→收藏→评价）
5. 验证数据完整性（外键引用、ID映射）
6. 清理 t_ 前缀废弃表
7. 更新视图
8. 后端代码适配（新建 Entity/Mapper/Controller）
9. 前端页面适配
10. 联调测试
```
