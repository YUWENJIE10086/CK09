-- 烤房管理后台初始账号模板
-- 部署前由运维系统将占位符替换为随机生成的 BCrypt 哈希。
-- 禁止将真实密码或固定可破解哈希提交到源码仓库。

INSERT INTO sys_user (user_id, dept_id, user_name, nick_name, user_type, password, status, remark) VALUES
(1, 1, 'admin', '系统管理员', '00', '{{ADMIN_PASSWORD_HASH}}', '0', '首次部署后必须修改密码'),
(2, 2, 'coop01', '合作社管理员', '01', '{{COOP_PASSWORD_HASH}}', '0', '首次部署后必须修改密码'),
(3, 3, 'tech01', '技术员', '02', '{{TECH_PASSWORD_HASH}}', '0', '首次部署后必须修改密码'),
(4, 4, 'farmer01', '烟农01', '03', '{{FARMER_PASSWORD_HASH}}', '0', '首次部署后必须修改密码');
