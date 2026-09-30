# Web端代码审计整改最终交叉复审报告（3.9—3.17）

## 一、复审范围
本报告针对原审计报告中本次 Web/Java 管理后台保留范围 3.9—3.17，对 CK09 当前 main 进行整改后反向核验。

复审方法包括：
1. 逐项回读 3.9—3.17 单项整改记录；
2. 回读当前 main 的 SecurityConfig、JwtAuthenticationFilter、JwtUtil、LoginController、SysUserController、SysUserRequest、ImageController、HealthAlgoController、application.yml 及相关 SQL；
3. 检查整改之间的交叉影响；
4. 对关键残留模式进行再次搜索；
5. 使用 GitHub Actions 在 JDK 17 下执行 Maven `mvn -B test`；
6. 对只能由部署/运行环境确认的事项单独标记，不把静态检查冒充动态验证。

## 二、逐项最终状态

| 编号 | 原问题 | 当前源码状态 | 仍需运行/部署确认 |
|---|---|---|---|
| 3.9 | docker-compose 默认数据库弱口令 | 当前 main 无报告引用 docker-compose.yml，无法对不存在文件伪造整改 | 是：如实际部署包存在 compose 文件，必须复核并移除默认弱口令/3306 暴露 |
| 3.10 | HealthAlgoController 动态列名 SQL 注入 | 已整改；外部 algo 只能映射到固定列名白名单 | 建议动态恶意参数复测 |
| 3.11 | CORS 通配符源 + credentials | 已整改；单一 Security CORS、显式 Origin 白名单 | 是：生产必须设置真实 CORS_ALLOWED_ORIGINS |
| 3.12 | AiQueryController permitAll | 已整改；取消匿名放行，进入 authenticated | 是：Dify 必须配置有效认证凭据 |
| 3.13 | ImageController 路径穿越 | 已整改；Path normalize + 根目录约束 + 图片扩展名白名单 | 建议 ../、反斜杠、绝对路径动态复测 |
| 3.14 | updatePwd 请求体 userId IDOR | 已整改；目标用户绑定 Authentication principal.id | 建议双账户越权动态复测 |
| 3.15 | SysUser 直接实体绑定 | 已整改；SysUserRequest DTO + setter 白名单 | 建议附加敏感字段动态复测 |
| 3.16 | 所有用户统一 ROLE_USER / 缺少 RBAC | 源码已整改并经交叉返修；00/01/02/03 显式角色映射，用户管理 ADMIN-only，自助改密 authenticated | 是：旧数据库必须迁移 user_type，并用不同角色账户动态验证 |
| 3.17 | SQL 初始化固定默认凭据 | 当前初始化源码已移除固定账户/固定哈希，新增安全 seed 模板 | 是：已部署旧账户必须轮换密码或禁用 |

## 三、交叉复审发现并追加修复的问题

### 1. 3.16 旧数据会导致 RBAC 失效
首次整改后发现旧 SQL 曾将 admin、coop01、tech01、farmer01 的 user_type 全部设置为 00。如果运行数据库沿用旧数据，四类账户都会成为管理员。

已追加：
- 00 = ROLE_ADMIN
- 01 = ROLE_COOP
- 02 = ROLE_TECH
- 03 = ROLE_FARMER
- 新增 `Traeppp09/sql/rbac_user_type_migration.sql`

迁移脚本不自动 COMMIT，部署人员必须按真实身份核对后提交。

### 2. 3.10 仍有固定数组形成的动态 SQL 字符串
交叉复审在 `analysisOverview()` 发现算法名虽然来自代码内固定数组，但仍存在：
`health_ + algo`
参与 SQL 标识符构造。

该处当前并非外部用户可控注入点，但为了让动态 SQL 标识符治理彻底一致，并防止后续维护把固定数组改成外部输入后重新引入漏洞，已追加改为：
`String column = requireHealthAlgoColumn(algo)`
后再构造 SQL。

追加提交：
`6f49686676dd7673153c2a8401fa19ad10d0f2d0`

## 四、编译与测试验证
已新增：
`.github/workflows/web-audit-verify.yml`

验证环境：
- Ubuntu GitHub Actions
- JDK 17
- Maven
- 工作目录：`Traeppp09/ruoyi-barn`
- 命令：`mvn -B test`

首轮完整整改代码验证 Run：
`36681918364`
结果：SUCCESS。
其中 Compile and test：SUCCESS。

3.10 交叉补丁提交后会由同一 workflow 再次触发验证；最终验收应以最新 main 对应 run 为 SUCCESS 为准。

## 五、不能被源码整改替代的部署事项
以下事项不能通过 GitHub 源码本身证明已经完成：
1. 3.9 实际部署包是否仍存在报告中的 docker-compose.yml；
2. 生产 CORS_ALLOWED_ORIGINS 是否设置为真实可信域名；
3. Dify 是否已经携带有效认证凭据；
4. 旧数据库 user_type 是否已经执行并核对 RBAC 迁移；
5. 旧 admin/coop01/tech01/farmer01 是否已轮换密码/禁用；
6. Git 历史中的旧固定哈希是否需要历史清理。

这些项目在没有部署证据前必须标记“待部署/动态验证”，不能写成已经完成。

## 六、当前验收判断
### 源码层面
3.10—3.17 对应原审计问题已完成针对性整改；3.16 经交叉复审发现旧数据角色问题后已返修；3.10 经交叉复审进一步统一动态 SQL 标识符白名单。

3.9 当前 GitHub main 不存在原报告引用的整改对象，因此状态保持“待部署文件复核”，而不是虚假关闭。

### 是否可以直接宣称“全部通过代码审计”
目前不建议写“全部问题已 100% 通过”。

更准确的验收表述是：
“本次 Web/Java 源码范围内可直接整改的 3.10—3.17 已完成代码修复和交叉静态复核，并已建立 GitHub Actions Maven 编译/测试验证；3.9 及涉及生产 CORS、Dify 凭据、旧数据库角色迁移、旧默认账户密码轮换的事项仍需部署环境证据和动态复测后最终关闭。”

## 七、建议提交审计复测的证据
1. 本目录 3.9—3.17 单项整改记录；
2. 本最终交叉复审报告；
3. GitHub 代码提交记录；
4. Web Audit Verification 成功运行记录；
5. RBAC 数据迁移执行截图/查询结果；
6. 管理员与普通角色访问 /system/user/** 的 200/403 对照；
7. updatePwd 双账户越权测试；
8. CORS 白名单/恶意 Origin 对照；
9. ImageController 路径穿越攻击用例；
10. 旧默认账户密码轮换/禁用记录。
