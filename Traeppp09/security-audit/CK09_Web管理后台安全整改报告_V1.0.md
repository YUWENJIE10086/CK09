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
| 3.13 | Path normalize、上传根目录边界、扩展名白名单、认证 | 已整改 |
| 3.14 | updatePwd 使用 SecurityContext 当前用户；管理操作 ADMIN | 已整改 |
| 3.15 | SysUserDTO 字段白名单；add/edit ADMIN | 已整改 |
| 3.16 | JWT role、数据库 userType 映射、17 个管理 Controller ADMIN | 已整改 |
| 3.17 | 删除 SQL 中真实默认哈希，改部署模板；弱口令/失败锁定 | 已整改 |

## 生产配置
必须配置 `CORS_ALLOWED_ORIGINS`、`AI_QUERY_API_KEY`、`JWT_SECRET`。初始账户使用 `Traeppp09/sql/barn_user_seed.template.sql` 生成不同 BCrypt 哈希，不得把真实密码或固定哈希提交到仓库。

## 复测
代码整改完成不等于第三方审计必然通过。正式送审前应执行 Maven test/package、数据库初始化、前端回归、Dify 调用、浏览器 CORS 验证，以及 Fortify/CODE SEC/人工渗透复测。