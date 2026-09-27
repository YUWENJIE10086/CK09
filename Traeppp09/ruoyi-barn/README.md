# 烤房全周期信息化管理平台 - 后端

基于 RuoYi（若依）Spring Boot 框架，提供 RESTful 接口供 Vue3 前端调用。

## 一、技术栈

- Spring Boot 2.7.18
- MyBatis-Plus 3.5.5
- Spring Security + JWT
- MySQL 5.7+ / 8.0
- Lombok + Hutool

## 二、环境要求

| 组件       | 版本         | 说明                           |
|----------|------------|------------------------------|
| JDK      | 17         | 后端编译和运行环境                |
| Maven    | 3.6+       | 用于构建/运行                      |
| MySQL    | 5.7+ / 8.0 | 字符集 utf8mb4                 |

## 三、数据库准备

```bash
# 1. 登录MySQL（密码由安全提示输入）
mysql -u "$DB_USER" -p -h "$DB_HOST" -P "${DB_PORT:-3306}"

# 2. 创建数据库
CREATE DATABASE IF NOT EXISTS merge_sql DEFAULT CHARACTER SET utf8mb4;
USE merge_sql;

# 3. 导入表结构 + 真实数据
#    （在项目根目录执行 sql/import_all_data_v2.py）
```

## 四、构建与启动

```bash
cd ruoyi-barn

# 方式1：Maven 启动
mvn spring-boot:run

# 方式2：打包后启动
mvn clean package -DskipTests
java -jar target/ruoyi-barn.jar
```

启动成功后控制台会输出：

```
========== 烤房管理系统启动成功 ==========
接口地址: http://localhost:8080
```

## 五、账号安全

项目不提供默认明文密码。初始管理员应由部署人员使用独立强密码创建，密码以 BCrypt 哈希存储。

## 六、接口约定（RuoYi 规范）

### 6.1 统一响应

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": { ... }
}
```

### 6.2 分页响应

```json
{
  "total": 100,
  "rows": [ ... ],
  "pageNum": 1,
  "pageSize": 10
}
```

### 6.3 鉴权

所有 `/barn/**` 接口需在 Header 携带：

```
Authorization: Bearer <token>
```

登录后从 `data.token` 获取。

## 七、主要接口清单

| 模块        | 方法   | 路径                            | 说明            |
|-----------|------|-------------------------------|---------------|
| 认证        | POST | `/api/login`                  | 登录，返回token   |
| 认证        | POST | `/api/logout`                 | 退出            |
| 认证        | POST | `/api/getInfo`                | 当前用户信息        |
| 烤房项目      | GET  | `/barn/project/list`          | 分页列表          |
| 烤房项目      | GET  | `/barn/project/{id}`          | 详情            |
| 烤房项目      | POST | `/barn/project`               | 新增            |
| 烤房项目      | PUT  | `/barn/project`               | 修改            |
| 烤房项目      | DEL  | `/barn/project/{id}`          | 删除            |
| 烤房项目      | GET  | `/barn/project/stats/health`  | 健康等级统计        |
| 烤房项目      | GET  | `/barn/project/stats/county`  | 县区分布统计        |
| 烤房项目      | GET  | `/barn/project/stats/projectType` | 项目类型分布       |
| 烤房项目      | GET  | `/barn/project/stats/buildMode` | 建设方式分布       |
| 烤房部件      | GET  | `/barn/component/list`        | 部件列表          |
| 维修记录      | GET  | `/barn/repair/list`           | 维修分页          |
| 维修记录      | GET  | `/barn/repair/stats`          | 维修统计          |
| 预约记录      | GET  | `/barn/reservation/list`      | 预约分页          |
| 评价记录      | GET  | `/barn/evaluation/list`       | 评价分页          |
| 评价记录      | GET  | `/barn/evaluation/stats`      | 评价统计          |
| 管护队伍      | GET  | `/barn/team/list`             | 队伍列表          |
| 资金管理      | GET  | `/barn/fund/list`             | 资金列表          |
| 字典        | GET  | `/barn/dict/county`           | 县区字典          |
| 字典        | GET  | `/barn/dict/township`         | 乡镇字典          |
| 字典        | GET  | `/barn/dict/village`          | 村字典           |
| 字典        | GET  | `/barn/dict/projectType`      | 项目类型字典        |

## 八、与前端对接

前端 `vite.config.ts` 已配置代理：

```ts
server: {
  port: 5174,
  proxy: {
    '/dev-api': {
      target: 'http://localhost:8080',
      changeOrigin: true,
      rewrite: (p) => p.replace(/^\/dev-api/, ''),
    },
  },
}
```

前端 `src/api/*` 已全部改为真实接口，不再使用 mock。
