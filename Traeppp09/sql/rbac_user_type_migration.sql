-- ============================================================
-- RBAC user_type 数据迁移（3.16 交叉复审）
-- ============================================================
-- 角色编码：
--   00 = 系统管理员（ROLE_ADMIN）
--   01 = 合作社用户（ROLE_COOP）
--   02 = 技术员（ROLE_TECH）
--   03 = 烟农（ROLE_FARMER）
--
-- 重要：
-- 1. 旧初始化数据曾把 admin/coop01/tech01/farmer01 全部写成 00，
--    会导致四类账户都获得管理员权限。
-- 2. 执行前请备份 sys_user，并核对实际账号与业务身份。
-- 3. 本脚本只迁移仓库旧种子中已明确出现的四个账号，不猜测其他用户身份。

START TRANSACTION;

UPDATE sys_user SET user_type = '00' WHERE user_name = 'admin';
UPDATE sys_user SET user_type = '01' WHERE user_name = 'coop01';
UPDATE sys_user SET user_type = '02' WHERE user_name = 'tech01';
UPDATE sys_user SET user_type = '03' WHERE user_name = 'farmer01';

-- 执行后必须人工核对；确认无误再 COMMIT。
SELECT user_id, user_name, nick_name, user_type, status
FROM sys_user
WHERE user_name IN ('admin', 'coop01', 'tech01', 'farmer01')
ORDER BY user_id;

-- 核对正确后执行：
-- COMMIT;
-- 如发现身份映射不正确，执行：
-- ROLLBACK;
