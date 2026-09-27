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

## V2.1 自检补充

### 已追加验证的代码事实

- `/upload/**` 已不再配置匿名 `permitAll`。
- JWT 过滤器会读取数据库用户状态；禁用用户不会建立认证上下文。
- 全部当前 Java Controller 已复核：业务管理 Controller 使用 ADMIN 方法级权限；AI Query 使用 ADMIN/AI_QUERY。
- CI 已加入 Maven test/package 以及安全基线扫描流程；当前仓库仍不能把 GitHub Actions 未实际返回成功状态等同于本地构建成功。

### 建议新增复测

9. 已登录用户访问允许的图片资源应按业务权限正常工作；未登录直接访问 `/upload/**` 应返回 401。
10. 已签发 JWT 后将用户状态改为禁用，再请求受保护接口，应返回 401。
11. 普通用户遍历当前管理 Controller 路径，应统一返回 403。
12. 恢复业务所需的匿名图片访问时，不得重新开放整个 `/upload/**`，应改用受控资源接口。

### 3.1–3.9 专项待办

需补齐实际小程序源码、Flask/Python 服务或生产发布包后，再逐项做动态验证；在证据缺失前保持“未确认”，不把“仓库不存在”写成“漏洞已修复”。
