-- =====================================================================
-- 烟农画像与培育 —— 数据库表
-- 说明：
--   1. farmer_question  烟农问卷答题明细（一题一行，支持 35 题 → 五维拆解）
--   2. farmer_profile   烟农画像结果（五维知识分/画像分/分级/最弱维/缺口优先级）
-- 运行前请先连接 web_kaoFa 数据库，脚本可重复执行（幂等）。
-- =====================================================================

-- ===== 表1：烟农问卷答题明细 =====
CREATE TABLE IF NOT EXISTS farmer_question (
  id            BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
  paper_id      VARCHAR(32)   DEFAULT '2026'   COMMENT '问卷批次(年度/场次)',
  farmer_name   VARCHAR(64)   DEFAULT ''       COMMENT '烟农姓名',
  farmer_phone  VARCHAR(20)   DEFAULT ''       COMMENT '烟农手机号',
  county        VARCHAR(32)   DEFAULT ''       COMMENT '县(市)',
  township      VARCHAR(32)   DEFAULT ''       COMMENT '乡镇/站',
  village       VARCHAR(32)   DEFAULT ''       COMMENT '村',
  pound_group   VARCHAR(32)   DEFAULT ''       COMMENT '磅组/片区',
  dim           VARCHAR(10)   DEFAULT ''       COMMENT '维度: 栽培/植保/采烤/烘烤/综合',
  question_no   INT           DEFAULT 0        COMMENT '题号',
  question      VARCHAR(255)  DEFAULT ''       COMMENT '题干',
  answer        VARCHAR(255)  DEFAULT ''       COMMENT '烟农作答',
  key_ind       VARCHAR(255)  DEFAULT ''       COMMENT '标准答案',
  correct       TINYINT       DEFAULT 0        COMMENT '是否答对 0/1',
  survey_date   DATE          DEFAULT NULL     COMMENT '答卷日期',
  created_at    DATETIME      DEFAULT CURRENT_TIMESTAMP,
  KEY idx_q_region (county, township, village),
  KEY idx_q_dim    (dim),
  KEY idx_q_farmer (farmer_phone),
  KEY idx_q_paper  (paper_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='烟农问卷答题明细';

-- ===== 表2：烟农画像结果 =====
CREATE TABLE IF NOT EXISTS farmer_profile (
  id           BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
  paper_id     VARCHAR(32)   DEFAULT '2026'   COMMENT '问卷批次',
  farmer_name  VARCHAR(64)   DEFAULT ''       COMMENT '烟农姓名',
  farmer_phone VARCHAR(20)   DEFAULT ''       COMMENT '烟农手机号',
  county       VARCHAR(32)   DEFAULT ''       COMMENT '县(市)',
  township     VARCHAR(32)   DEFAULT ''       COMMENT '乡镇/站',
  village      VARCHAR(32)   DEFAULT ''       COMMENT '村',
  pound_group  VARCHAR(32)   DEFAULT ''       COMMENT '磅组/片区',
  k_cult       DOUBLE        DEFAULT 0        COMMENT '栽培知识分0-100',
  k_pp         DOUBLE        DEFAULT 0        COMMENT '植保知识分0-100',
  k_hv         DOUBLE        DEFAULT 0        COMMENT '采烤知识分0-100',
  k_cur        DOUBLE        DEFAULT 0        COMMENT '烘烤知识分0-100',
  k_syn        DOUBLE        DEFAULT 0        COMMENT '综合知识分0-100',
  p_score      DOUBLE        DEFAULT 0        COMMENT '画像分0-100',
  level        VARCHAR(10)   DEFAULT '普通'    COMMENT '职业/普通/新手',
  weak_dim     VARCHAR(10)   DEFAULT ''       COMMENT '最弱维度',
  priority     DOUBLE        DEFAULT 0        COMMENT '培育优先级',
  survey_date  DATE          DEFAULT NULL     COMMENT '问卷年度',
  updated_at   DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_farmer_paper (farmer_phone, paper_id),
  KEY idx_p_region (county, township, village)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='烟农画像结果';

-- 校验：表结构
SELECT table_name, table_comment
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = DATABASE()
  AND table_name IN ('farmer_question', 'farmer_profile');