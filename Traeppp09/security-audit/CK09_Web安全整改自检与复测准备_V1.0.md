# CK09 管理后台安全整改自检与复测准备

## 静态自检

- CORS 通配符：通过
- AI Query 匿名访问：通过
- Image 路径穿越：通过
- 用户密码 IDOR：通过
- SysUser 批量赋值：通过
- RBAC：通过
- SQL 默认凭据：通过
- 登录失败锁定：通过

## 建议复测用例

1. 无 Authorization/API Key 访问 `/barn/ai-query/**` 应返回 401。
2. 正确 AI API Key 可进入 AI Query。
3. 非 ADMIN 用户访问管理 Controller 应返回 403。
4. `updatePwd` 伪造请求体 `userId` 不应修改他人密码。
5. 非白名单 `algo` 应返回 400。
6. `path=../../application.yml` 等路径应被拒绝；正常图片路径仍应保持原有访问能力。
7. 恶意 Origin 不应获得 CORS 允许来源。
8. SQL 初始化脚本不得包含真实默认 BCrypt 哈希。

## 限制
当前结论来自源码静态整改与 Git 差异核对；生产数据库、浏览器、Dify、第三方扫描器尚未在本环境完成全链路复测。