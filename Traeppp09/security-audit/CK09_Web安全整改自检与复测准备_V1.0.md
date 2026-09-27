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
\n\n## V2.1.1 自检补充（2026-09-27）\n\n- PR #3 已合并：`112d1cb6212db93e8d59111fcebfbcfed19f7cf9`。\n- CI 已新增前端 `npm ci`、`npm run check`、`npm run build`。\n- 当前 workflow 查询尚未返回该 commit 的实际运行记录，因此不把 CI 标记为“通过”。\n- 前端工程已确认是 Vue 3/Vite；原审计 3.1–3.9 所对应的小程序/Flask 服务仍待定位。\n\n### 下一轮必须补齐\n\n13. 获取 GitHub Actions 实际成功日志，保存为复测证据。\n14. 获取实际部署包/生产 API，逐项复测 3.1–3.9。\n15. 对 /upload/**、禁用 JWT、RBAC、CORS、AI Query 执行黑盒验证并保存响应证据。\n16. 完成 Fortify/CODE SEC/人工渗透复测。\n\n\n## V2.1.2 自检补充（2026-09-27）\n\n- PR #4 已合并，新增上传目录符号链接逃逸防护。\n- 登录用户名/密码增加 128 字符上限，在 BCrypt 前拒绝超长输入。\n- 建议新增黑盒用例：上传目录内创建指向外部文件的符号链接，请求 `/api/image/thumb` 应拒绝；发送超长用户名/密码应在密码哈希计算前返回 401。\n- 3.1–3.9 仍需真实小程序/Flask 服务或发布包。\n

## V2.1.3 自检补充（2026-09-27）

- PR #5 已合并，修复前端下载 `responseType` 配置错误。
- LoginAttemptService 已采用 `Locale.ROOT` 做用户名规范化。
- 建议构建回归后增加下载接口二进制响应测试，并保留登录锁定测试结果。
- 仍不得将源码整改等同于第三方扫描通过。


## V2.1.4 自检补充（2026-09-27）

- PR #7 已合并，修复 3 个 Controller 的异常信息直接回传。
- 建议黑盒测试触发数据库/参数异常时，响应中不应出现 SQL、表名、文件路径、Java 异常类等内部信息。
- 第三方 SAST/人工复测仍待执行。


## V2.1.5 自检补充（2026-09-27）

- JWT_SECRET：要求至少 64 字符；部署验证时必须检查环境变量。
- MyBatis StdOutImpl：已移除。
- 注意：源码修改完成不等同于运行环境已满足 JWT_SECRET 要求，需在实际部署环境启动验证。


## V2.1.6 复测记录（2026-09-27）
- [x] JJWT 依赖升级完成
- [x] JwtUtil 新 API 编译通过
- [x] Maven test 通过
- [x] Maven package 通过
- [x] Security baseline scan 通过
- [ ] 前端 TypeScript check 全量通过（存在既有 12 项错误）
- [ ] npm audit 依赖漏洞完成逐项处置
- [ ] 生产环境 JWT_SECRET 实际值与启动校验验证


## V2.1.7 复测记录（2026-09-27）
- [x] 前端 12 项 TypeScript 错误全部修复
- [x] frontend npm run check 通过
- [x] frontend npm run build 通过
- [x] backend Maven test/package 通过
- [x] security baseline 通过
- [x] npm audit 明细已进入 CI 日志
- [ ] Vite/PostCSS/ECharts 等直接依赖升级
- [ ] xlsx 实际使用面核查及替代方案评估
- [ ] PptxGenJS/image-size 依赖链升级
