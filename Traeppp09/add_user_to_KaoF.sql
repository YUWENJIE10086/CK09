-- ============================================================
-- KaoF数据库用户添加脚本
-- ============================================================
-- 说明：
-- 1. 小程序登录方式：微信手机号快速授权，不需要密码
-- 2. 手机号必须在数据库中唯一
-- 3. 添加用户后，小程序通过微信授权获取手机号后自动匹配
-- ============================================================

-- 查看当前用户
SELECT id, name, phone, status, credit_score FROM users;

-- 插入新用户
INSERT INTO users (
    id,
    name,
    phone,
    password_hash,
    credit_level,
    credit_score,
    area,
    pound_group_id,
    planting_area,
    planting_years,
    total_bakes,
    avatar_url,
    openid,
    unionid,
    status,
    last_login_at,
    created_at,
    updated_at
) VALUES (
    'YN003',                           -- 用户ID（唯一）
    '测试用户',                         -- 姓名
    '15872515073',                     -- 手机号（唯一）
    '',                                -- 密码哈希（小程序登录不需要）
    'A',                               -- 信用等级：A/B/C/D
    95,                                -- 信用分数：0-100
    '宜昌市秭归县',                     -- 所属区域
    1,                                 -- 所属磅组ID
    12.50,                             -- 种植面积（亩）
    8,                                 -- 种植年限（年）
    0,                                 -- 累计烘烤次数
    NULL,                              -- 头像URL
    NULL,                              -- 微信OpenID
    NULL,                              -- 微信UnionID
    1,                                 -- 状态：1=正常 0=禁用
    NULL,                              -- 最后登录时间
    NOW(),                             -- 创建时间
    NOW()                              -- 更新时间
);

-- 查看所有用户（包括新添加的用户）
SELECT id, name, phone, status, credit_score, created_at FROM users;
