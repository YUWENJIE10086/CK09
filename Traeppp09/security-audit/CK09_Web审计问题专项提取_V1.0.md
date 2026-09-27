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
