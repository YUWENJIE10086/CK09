-- =====================================================================
-- 烤房健康分三维融合算法 —— 数据库字段扩展
-- 说明：
--   1. manage_mode  管护制度（集中管护 / 分户管护），默认集中管护
--   2. manual_eval  人工评价（好 / 中 / 差），默认好
-- 运行前请先连接 web_kaoFa 数据库，脚本可重复执行（幂等）。
-- =====================================================================

-- 新增列：管护制度
SET @exist_mgmt := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'kf_basedata'
    AND COLUMN_NAME = 'manage_mode'
);
SET @sql_mgmt := IF(
  @exist_mgmt = 0,
  'ALTER TABLE kf_basedata ADD COLUMN manage_mode VARCHAR(20) DEFAULT ''集中管护'' COMMENT ''管护制度: 集中管护/分户管护''',
  'SELECT ''manage_mode 列已存在，跳过'''
);
PREPARE stmt_mgmt FROM @sql_mgmt;
EXECUTE stmt_mgmt;
DEALLOCATE PREPARE stmt_mgmt;

-- 新增列：人工评价
SET @exist_manual := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'kf_basedata'
    AND COLUMN_NAME = 'manual_eval'
);
SET @sql_manual := IF(
  @exist_manual = 0,
  'ALTER TABLE kf_basedata ADD COLUMN manual_eval VARCHAR(10) DEFAULT ''好'' COMMENT ''人工评价: 好/中/差''',
  'SELECT ''manual_eval 列已存在，跳过'''
);
PREPARE stmt_manual FROM @sql_manual;
EXECUTE stmt_manual;
DEALLOCATE PREPARE stmt_manual;

-- 更新既有/默认数据：未录入的按“集中管护 + 好”兜底
UPDATE kf_basedata SET manage_mode = '集中管护' WHERE manage_mode IS NULL OR manage_mode = '';
UPDATE kf_basedata SET manual_eval = '好' WHERE manual_eval IS NULL OR manual_eval = '';

-- 校验结果
SELECT project_id, manage_mode, manual_eval, health_score_final
FROM kf_basedata
ORDER BY project_id
LIMIT 20;