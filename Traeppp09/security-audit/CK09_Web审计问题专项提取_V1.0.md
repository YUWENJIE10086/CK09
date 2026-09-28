# CK09 管理后台审计问题专项提取 V1.0

## 范围
本专项仅针对 Java 管理后台 `Traeppp09/ruoyi-barn` 及其直接相关 SQL 初始化脚本。微信小程序相关问题不纳入本次整改。

## 直接对应的审计项

| 编号 | 风险 | 问题 |
|---|---|---|
| 3.10 | 高 | SQL 注入：HealthAlgoController `algo` 拼接 SQL 列名 |
| 3.11 | 高 | CORS：通配符来源 + 允许凭证 |
| 3.12 | 高 | AiQueryController 全部接口 `permitAll` |
| 3.13 | 高 | ImageController `resolveOriginalFile` 路径穿越 |
| 3.14 | 高 | SysUserController 密码修改 IDOR/越权 |
| 3.15 | 高 | SysUserController 直接绑定 SysUser 导致批量赋值 |
| 3.16 | 高 | 缺少 RBAC，统一 `ROLE_USER` |
| 3.17 | 高 | SQL 初始化脚本硬编码默认凭据 |

## 排除项
报告 3.1–3.9 中以 `server/app.py`、`utils/api.js` 等为主要代码位置的问题不属于本次 Java 管理后台整改；其中 3.6 明确属于小程序前端高德 Key。根目录/历史压缩包等归档材料需在正式送审前单独确认是否纳入扫描范围。

## V2.1 复核更新

### 当前仓库新增确认

当前 `main` 为 Vue 3 + Spring Boot 架构；未发现与原报告 3.1–3.9 所指 `server/app.py` 等位置对应的独立 Flask/Python Web 服务。该事实只说明“当前仓库未发现”，不代表生产环境不存在，因此 3.1–3.9 保持“待定位/待复测”。

同时复核 Java 管理后台全部 Controller：管理类 Controller 均已采用 `@PreAuthorize("hasRole('ADMIN')")`；AI Query 单独允许 ADMIN/AI_QUERY；SecurityConfig 默认要求认证，未保留业务接口的匿名 `permitAll`。

### 继续整改

PR #2 已进一步关闭 `/upload/**` 匿名静态文件访问，并增加禁用账号 JWT 即时失效校验。3.13 因而从“路径穿越防护”进一步收紧到“路径防护 + 默认认证访问”，但仍建议生产环境建设签名/授权下载机制。
\n\n## V2.1.1 继续复核（2026-09-27）\n\nPR #3 已合并到 `main`，新增前端 CI 门禁：`npm ci`、TypeScript 检查和 Vite 生产构建。当前仓库的 `package.json` 明确为 Vue 3/Vite 工程。\n\n因此对 3.6（高德 Key）、3.7（上传内容校验）等原小程序前端问题，当前仍只能认定为“未找到对应审计对象”，不能因为当前前端构建成功/存在就推断漏洞已修复。正式复测仍需取得原小程序源码、生产包或实际 API 服务。\n\n\n## V2.1.2 复核更新（2026-09-27）\n\nPR #4 已合并至 `main`。3.13 现为“路径规范化 + 上传根目录边界 + 实际 real path 边界”三层控制；登录入口增加输入长度限制。\n\n该更新仍不改变专项范围：3.1–3.9 对应的小程序/Flask 服务不在当前 Java 管理后台范围内，必须取得实际对象后复测。\n

## V2.1.3 复核更新（2026-09-27）

PR #5 已合并。当前前端请求封装的文件下载方法已修正 Axios 配置参数错误；登录失败计数的用户名规范化已改为 `Locale.ROOT`。本轮未改变 3.10–3.17 的既有权限边界，也未将 3.1–3.9 改判为已修复。


## V2.1.4 复核更新（2026-09-27）

PR #7 已合并。针对 Controller 层发现的异常信息直接回传问题，已完成第一批实际修复：Baker、BarnProject、Farmer 三个 Controller 不再把异常文本直接拼接到 API 错误响应中。


## V2.1.5 复核更新（2026-09-27）

PR #9 已合并。JWT 配置现在具备启动期密钥长度和有效期校验；MyBatis 不再使用标准输出 SQL 日志实现。两项均为实际代码变更。


## V2.1.6 新增专项发现（2026-09-27）

### 依赖生命周期/安全维护状态
- JJWT 0.9.1 已不属于官方支持版本范围，存在继续使用旧依赖的维护与安全风险。
- 已在 PR #11 升级至 JJWT 0.13.0 并完成 API 迁移。
- 后续需继续梳理 npm audit 报告中的 17 个依赖漏洞，并核对实际可利用性与升级兼容性。


## V2.1.7 新增依赖专项（2026-09-27）
- Vite 当前锁定 5.4.21，audit 命中高危及传递 esbuild 风险；需升级至受支持主版本并同步 @vitejs/plugin-vue。
- PostCSS 当前锁定 8.5.15，audit 命中高危路径/源码映射相关问题；应升级至当前修复版本。
- ECharts 当前 5.6.0，audit 命中 XSS；当前 npm 最新 6.1.0，应在兼容性验证后升级。
- xlsx 0.18.5 存在 Prototype Pollution/ReDoS，audit 无自动修复版本；需核查实际使用并评估替换。
- PptxGenJS 当前 4.0.1 的 image-size 依赖命中高危；应升级/覆盖 image-size 到已修复版本并做 PPT 生成回归。


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


## V2.1.10 Web 专项边界与新增发现（2026-09-28）

本版本进一步明确：当前专项只审 Web 管理后台，不把微信小程序/独立 Flask 服务作为整改对象或 Web 阻断项。

### 新增实际发现与处置

- FarmerAlgoController 导入异常路径存在 System.out.println("导入答题失败: " + e.getMessage())，本轮已删除异常消息输出。
- FarmerAlgoController 分页查询现使用参数化 LIMIT ? OFFSET ?，并限制 pageNum/pageSize 边界。
- ImageController 增加文件大小、图片尺寸及像素总量限制，降低图片解码资源耗尽风险。
- SecurityConfig 增加 X-Frame-Options: DENY。
- ApiKeyAuthenticationFilter 增加 API Key 最低 32 UTF-8 字节配置门禁。

### 后续专项

继续进行全项目 SQL 注入、异常泄露、文件上传/下载、API 对象级授权、前端 XSS/Secret、Swagger/Actuator 暴露以及依赖漏洞复核。所有“已修复”结论以实际代码或可复核 CI/黑盒证据为准。
