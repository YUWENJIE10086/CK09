# 烤房全周期信息化管理平台

项目由 Vue 3 + TypeScript 前端和 Spring Boot 后端组成，使用 MySQL 存储业务数据。

## 目录

- `src/`：前端源码
- `public/`：前端运行资源
- `ruoyi-barn/src/`：后端源码及数据库脚本
- `sql/`：数据库建表和数据导入工具
- `deploy-ubuntu.sh`：Ubuntu 构建和部署脚本

## 环境要求

- Node.js 18+
- JDK 17
- Maven 3.6+
- MySQL 5.7+

## 必需配置

启动后端或执行部署前，必须通过受保护的环境注入以下变量：

- `DB_HOST`
- `DB_NAME`
- `DB_USER`
- `DB_PASS`
- `JWT_SECRET`（建议使用至少64个随机字符）

可选变量：`DB_PORT`、`BACKEND_PORT`、`FRONTEND_PORT`、`UPLOAD_PATH`、`DIFY_API_URL`。项目不包含默认明文密码或 API Key。

## 构建

```bash
npm ci
npm run build
mvn -f ruoyi-barn/pom.xml clean package -DskipTests
```

## 本地启动

配置好环境变量并完成构建后：

```bash
./start.sh
```

停止和重启：

```bash
./stop.sh
./restart.sh
```

## Ubuntu 部署

`deploy-ubuntu.sh` 会安装依赖、构建前后端、配置 Nginx 和 systemd。先从密钥管理工具或受保护的环境注入必需变量，再执行：

```bash
sudo --preserve-env=DB_HOST,DB_PORT,DB_NAME,DB_USER,DB_PASS,JWT_SECRET,DIFY_API_URL bash deploy-ubuntu.sh all
```

部署生成的后端环境文件权限为 `600`。

## 账号安全

- 新增用户时必须显式设置8–64位初始密码。
- 管理员重置密码时必须提供新密码。
- 密码仅以 BCrypt 哈希存储，不在源码或文档中提供默认凭据。
