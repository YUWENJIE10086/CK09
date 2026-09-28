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
\n\n## V2.1.1 继续整改（2026-09-27）\n\nPR #3 已合并至 `main`（merge commit：`112d1cb6212db93e8d59111fcebfbcfed19f7cf9`），继续补强前端交付验证：CI 新增 Node.js 20 环境下的 `npm ci`、`npm run check`、`npm run build`。当前 `Traeppp09/package.json` 为 Vue 3/Vite 前端。\n\n这项改动解决的是“后端有构建门禁、前端无构建门禁”的验证缺口，不等同于第三方安全扫描通过。GitHub Actions 当前尚未取得对应 commit 的可用运行结果，因此仍保留“待实际 CI 证据”。\n\n3.1–3.9 仍未闭环：当前仓库未发现原审计对应的独立 Flask/Python 服务或微信小程序工程，需继续定位实际生产服务/其他仓库/发布包。\n\n\n## V2.1.2 继续加固（2026-09-27）\n\nPR #4 已合并至 `main`（`5c7a49198de7be78f1a4680834ac3070c6e5df06`）。本轮增加两项实际代码加固：\n\n- ImageController 在词法路径规范化之后增加 `toRealPath()` 边界校验，阻断上传目录内符号链接指向外部目录的逃逸。\n- LoginController 对用户名/密码增加 128 字符长度上限，并在 BCrypt 校验前拒绝超长输入。\n\n上述属于代码层面加固，不替代黑盒及第三方复测。3.1–3.9 仍待实际小程序/Flask 服务或发布包定位。\n

## V2.1.3 继续整改记录（2026-09-27）

PR #5 已合并至 `main`（`9db9903d3b3d516ac129ecad3dd634b20816f03d`）。

- 修复前端 `download()` 对 Axios `responseType` 配置位置错误的问题，确保文件下载请求实际使用 `blob` 响应类型。
- 登录失败计数的用户名规范化改为 `Locale.ROOT`，保持跨运行环境一致。

这两项均属于低风险代码加固，不替代生产环境和第三方安全复测。


## V2.1.4 继续整改（2026-09-27）

PR #7 已合并至 `main`（`e4aa8e8c496434940c50f2c094a984aa7d9b6975`）。BakerController、BarnProjectController、FarmerController 不再直接向客户端返回 `Exception.getMessage()`，改为统一业务错误提示，降低后端实现细节泄露风险。


## V2.1.5 继续整改（2026-09-27）

PR #9 已合并至 `main`（`6b531ab0a26a54e6acbb47527b60c7647b7131d2`）。新增 JWT_SECRET 最低 64 字符启动门禁，并移除 MyBatis StdOut SQL 日志实现。部署环境需同步配置强 JWT_SECRET。


## V2.1.6 补充整改记录（2026-09-27）
- JJWT 0.9.1 → 0.13.0，完成新版 API 迁移。
- HS512 密钥强度改为 UTF-8 字节数校验，最低 64 字节。
- Maven test/package/security baseline 均已通过。
- 前端类型检查存在既有错误，另行纳入整改，不作为本轮后端安全修复失败依据。


## V2.1.7 补充记录（2026-09-27）
- 前端 12 项 TypeScript 错误已全部修复，CI frontend check/build 通过。
- 新增健康分三维更新接口，参数白名单 + 参数化 SQL。
- npm audit 当前仍为 17 项（7 moderate、10 high），进入下一轮专项依赖整改。


## V2.1.8 文档状态校正（2026-09-27）

本节为当前主线状态的最新记录，优先于本文前述历史版本中的“待执行/待验证”表述。

- 当前主线基线：main@6cbf40ee3fcdd128ec087f687f3191bccc7420c3。
- 代码整改状态：截至该基线，前端 12 项 TypeScript 类型错误已修复；frontend npm run check、frontend npm run build、backend Maven test/package、安全基线扫描均已有 CI 成功证据。
- npm 依赖状态：仍有 17 项漏洞（7 moderate、10 high、0 critical），尚未完成依赖专项处置；不得在本文中表述为“依赖漏洞已清零”。
- 仍未闭环的原审计项：3.1–3.9 对应的小程序/Flask 服务或生产发布包仍未在当前仓库中定位，因此继续保持“待定位/待复测”，不得改写为已修复。
- 仍需真实环境证据：数据库初始化、登录/RBAC/IDOR 黑盒测试、CORS 浏览器测试、AI Query JWT/API Key 实链路、图片路径攻击、生产 JWT_SECRET 启动验证，以及 Fortify/CODE SEC/人工渗透复测。
- 依赖专项下一步：Vite/PostCSS/ECharts、xlsx、PptxGenJS/image-size 等依赖需要在真实 npm 安装和 CI 回归基础上分组升级；不使用 npm audit fix --force，不手工伪造 lockfile integrity。


## V2.1.9 当前代码审计结论（2026-09-28）

### 审计结论：暂不能签发“通过”

本次基于当前 main 代码状态复核后，结论不是“整改失败”，而是“核心高风险代码问题已基本收敛，但尚未满足完整代码审计通过条件”。

- 已通过静态整改的核心项：原 3.10–3.17 对应的 Java 后台安全控制已进入 main；包括动态 SQL 字段白名单、认证与角色控制、IDOR/越权收敛、默认凭据清理、AI Query 鉴权、路径穿越/符号链接边界、异常信息收敛、JWT 密钥强度门禁、SQL 调试输出关闭等。
- 工程质量门禁：此前 CI 已取得 frontend TypeScript check/build、Maven test/package、安全基线扫描成功证据。
- 当前明确阻断项 1：依赖安全。npm audit 仍报告 17 项漏洞（7 moderate、10 high、0 critical），依赖专项尚未完成，因此不能宣称“依赖安全通过”。
- 当前明确阻断项 2：原审计 3.1–3.9。当前仓库仍未定位对应微信小程序/Flask 服务、生产发布包或其他实际运行代码，因此不能将这些项目标记为已修复。
- 当前明确阻断项 3：运行环境复测证据。尚缺数据库初始化、登录/RBAC/IDOR、CORS、AI Query JWT/API Key、图片路径攻击、生产 JWT_SECRET 启动等真实环境证据，以及独立 SAST/人工渗透复测报告。

### 当前评级

**代码整改成熟度：较高；完整代码审计状态：未通过/待最终复测。**

这里的“未通过”仅表示尚未满足完整审计的证据与风险闭环门槛，不代表已发现新的同等级代码漏洞。完成上述三个阻断项后，再进行最终复测并更新为正式审计结论。


## V2.1.10 Web 专项状态更新（2026-09-28）

本版本明确将整改范围限定为 Web 管理后台：Spring Boot 后端、Vue/Vite 前端、Web API、Web 配置及 Web 依赖。微信小程序/独立 Flask 服务不参与当前 Web 端结论。

### 本轮代码整改

- FarmerAlgoController 移除导入异常路径中的 System.out.println(... + e.getMessage())，不再把异常实现细节写入标准输出。
- SecurityConfig 增加 X-Frame-Options: DENY。
- ApiKeyAuthenticationFilter 对配置的 AI Query API Key 增加最低 32 UTF-8 字节启动门禁。
- ImageController 增加图片文件大小、尺寸、像素数量限制，并继续执行路径/real-path 边界校验。
- FarmerAlgoController 分页 LIMIT/OFFSET 使用参数化查询，并增加页码/页大小边界。

### 当前 Web 端未闭环事项

| 项目 | 状态 |
|---|---|
| 原 3.10–3.17 核心代码整改 | 基本完成 |
| 异常信息 stdout 泄露 | 本轮已修复 |
| SQL 全量二次扫描 | 待完成 |
| API 对象级授权/IDOR 黑盒 | 待完成 |
| npm 17 项依赖漏洞 | 待专项处置 |
| Swagger/Actuator 暴露 | 待检查 |
| 前端 XSS/Secret | 待专项检查 |
| 生产黑盒/第三方 SAST/人工渗透 | 待执行 |

**结论：当前属于“Web 代码整改成熟度较高、完整审计证据尚未闭环”，不宣称最终通过。**
