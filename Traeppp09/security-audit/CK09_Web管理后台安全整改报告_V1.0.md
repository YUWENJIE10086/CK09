# CK09 Web 管理后台安全整改报告 V1.0

- 基线：`main`
- 整改分支：`security/web-audit-fix`
- 原则：最小改动、保留业务能力、只增加安全边界。

## 整改闭环

| 编号 | 整改措施 | 状态 |
|---|---|---|
| 3.10 | 健康/寿命算法参数白名单，非法值 HTTP 400 | 已整改 |
| 3.11 | 单点 CORS、显式来源、限制方法/头、关闭 credentials | 已整改 |
| 3.12 | 移除 AI Query `permitAll`，JWT/API Key 认证 | 已整改 |
| 3.13 | Path normalize、上传根目录边界、扩展名白名单，保留原图片接口公开访问能力 | 已整改 |
| 3.14 | updatePwd 使用 SecurityContext 当前用户；管理操作 ADMIN | 已整改 |
| 3.15 | SysUserDTO 字段白名单；add/edit ADMIN | 已整改 |
| 3.16 | JWT role、数据库 userType 映射、17 个管理 Controller ADMIN | 已整改 |
| 3.17 | 删除 SQL 中真实默认哈希，改部署模板；弱口令/失败锁定 | 已整改 |

## 生产配置
必须配置 `CORS_ALLOWED_ORIGINS`、`AI_QUERY_API_KEY`、`JWT_SECRET`。初始账户使用 `Traeppp09/sql/barn_user_seed.template.sql` 生成不同 BCrypt 哈希，不得把真实密码或固定哈希提交到仓库。

## 复测
代码整改完成不等于第三方审计必然通过。正式送审前应执行 Maven test/package、数据库初始化、前端回归、Dify 调用、浏览器 CORS 验证，以及 Fortify/CODE SEC/人工渗透复测。

## V2.1 追加整改

2026-09-27 已继续完成代码级加固并合并至 `main`：

- 移除 `/upload/**` 匿名 `permitAll`，直接上传文件访问进入认证链路。
- JWT 认证增加用户状态校验，禁用账号不再因持有旧 JWT 建立认证上下文。
- CI 安全基线扫描拆分 DEBUG/私钥检查，降低脚本误判风险。
- PR #2 已合并，merge commit：`f2bf98578305cbd9ab72fc7414cb86178e1e40e9`。

### 当前未闭环事项

3.1–3.9 原审计指向的小程序/Flask 生产服务目前未在本仓库发现对应独立源码，因此不能标记为已修复。尤其 3.1 USERID/IDOR、3.2 Flask DEBUG、3.3 硬编码凭据、3.4 小程序侧 CORS、3.5 异常信息、3.6 高德 Key、3.7 上传内容校验、3.8 Token 比较仍需以实际生产服务/发布包复核。

另外，3.10–3.17 虽已完成源码整改，仍需通过实际构建、数据库初始化、浏览器、Dify 以及第三方 SAST/人工复测形成最终证据链。
