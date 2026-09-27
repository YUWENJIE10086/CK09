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
