# 智汇人事管理系统

基于 **Spring Boot 4 + MyBatis-Plus + JWT** 后端与 **Vue Vben Admin (Ant Design Vue)** 前端的人事管理系统，支持组织架构、员工、考勤、请假、薪资与 RBAC 权限。

## 技术栈

| 层级 | 技术 |
|------|------|
| 后端 | Java 17、Spring Boot 4.1、Spring Security、JWT、MyBatis-Plus 3.5 |
| 前端 | Vue 3、Vite、Pinia、Ant Design Vue（Vben Admin） |
| 数据库 | MySQL 8.0+ |

## 项目结构

```
hr-management/                    # 后端（本仓库）
├── src/main/java/                # 业务代码
├── src/main/resources/
│   ├── application.yml           # 公共配置
│   ├── application-dev.yml       # 开发环境
│   ├── application-prod.yml      # 生产环境
│   └── db/
│       ├── schema.sql            # 全量建库 + 测试数据
│       └── seed-test-data.sql    # 已有库增量补数
└── deploy/
    ├── env.prod.example          # 生产环境变量示例
    └── nginx.conf.example        # Nginx 反向代理示例

vue-vben-admin-main/              # 前端（独立目录）
└── apps/web-antd/                # HR 前端应用
    ├── .env.development          # 开发：/api 代理到 localhost:8080
    └── .env.production           # 生产：/api 同域反代
```

## 功能模块

- **工作台**：个人考勤月历、待审批请假、任务待办/逾期、快捷入口（按角色过滤）
- **组织**：部门树、岗位管理
- **员工**：入职建档（自动创建登录账号）、数据权限（员工/经理/HR）、花名册 Excel 导出、试用期提醒
- **考勤**：自助打卡、人脸打卡、超管代打卡、HR 补录、考勤台账 Excel 导出
- **请假**：申请、审批、假期类型维护
- **任务**：上级下发、接收/进度/催办/关闭、看板视图、一层子任务与进度汇总、挂接项目、附件（OSS）、逾期定时提醒
- **项目**：建档/成员/挂任务、进度自动汇总或负责人锁定确认；工作台「项目」统计参与中的进行中项目
- **绩效考核**：月/季考核单、经理五级评分、任务表现快照、确认归档进员工档案
- **入转调离**：调岗/调薪/离职/入职完善，申请→审批→生效（调薪写入个人底薪；入职完善需合同类文档）
- **统计看板**（HR）：人力分布、考勤异常、发薪汇总、任务/项目完成率
- **薪资**：月薪生成与发放（底薪优先个人底薪，否则岗位字典+当月任务奖金，可手调扣款）；HR/超管
- **字典管理**（仅超级管理员）：薪资字典；用户权限字典（按角色勾选能力）；页面字典（按角色配置可见菜单）
- **文档管理**：员工合同/协议/薪资确认单等 PDF、Word 集中管理（按部门岗位筛选、到期标记、OSS 存储）
- **个人中心**：基本资料、安全设置（密保手机/邮箱/密保问题/MFA）、消息提醒偏好、改密、人脸录入
- **通知**：站内消息 + SSE 实时推送
- **AI 助手**：本地 Ollama 转发问答、任务草稿、待办任务卡片跳转详情
- **操作日志**：`@OperationLog` 落库，HR/超管分页查询

## 快速开始

### 1. 数据库

**全新安装：**

```bash
mysql -u root -p < src/main/resources/db/schema.sql
```

**已有库补测试数据：**

```bash
mysql -u root -p hr_management < src/main/resources/db/seed-test-data.sql
```

**已有库扩充组织/底薪字典/任务评分样例（V10）：** 启动后端时 Flyway 会自动执行 `V10__seed_realistic_org_salary.sql`；也可手动：

```bash
mysql -u root -p hr_management --default-character-set=utf8mb4 -e "source src/main/resources/db/migration/V10__seed_realistic_org_salary.sql"
```

### 2. 后端

修改 `application-dev.yml` 中的数据库账号密码（默认 `root` / `123456`）。

```bash
cd hr-management
mvn spring-boot:run
```

- API 根路径：`http://localhost:8080/api`
- Swagger（仅 dev）：`http://localhost:8080/api/swagger-ui.html`

### 3. 前端

需安装 [pnpm](https://pnpm.io/)（Vben  monorepo 要求）。

```bash
cd vue-vben-admin-main
pnpm install
pnpm dev:antd
```

- 前端地址：`http://localhost:5666`
- 开发环境通过 Vite 代理：`/api` → `http://localhost:8080`

### 4. 测试账号

| 账号 | 密码 | 角色 | 说明 |
|------|------|------|------|
| admin | Admin@123 | 超级管理员 | 无员工绑定，可选员工代打卡 |
| hr | Hr@2024 | HR 管理员 | 张人事 |
| manager | Mgr@2024 | 部门经理 | 李经理，技术部 |
| employee | Emp@2024 | 普通员工 | 王员工，自助打卡/请假 |

## Docker 一键启动（MySQL + 后端 + Nginx）

适合本地演示或快速验收，三个容器同域访问 `http://localhost`。

### 前置条件

- 已安装 [Docker Desktop](https://www.docker.com/products/docker-desktop/)
- 已安装 [pnpm](https://pnpm.io/)（用于构建前端）
- 前端项目路径正确（默认 `D:/vue/vue-vben-admin-main`）

### 步骤

```powershell
cd e:\java\hr-management\deploy

# 1. 复制环境变量并按需修改 FRONTEND_PATH
copy .env.example .env

# 2. 一键构建前端 + 启动容器
.\start.ps1
```

Linux / macOS：

```bash
cd hr-management/deploy
cp .env.example .env
chmod +x start.sh && ./start.sh
```

### 架构

```
浏览器 → Nginx:80
           ├─ /        → 前端静态文件 (frontend-dist)
           └─ /api/    → backend:8080 (Spring Boot)
                              ↓
                           mysql:3306
```

### 常用命令

```bash
cd deploy

docker compose ps          # 查看状态
docker compose logs -f     # 查看日志
docker compose down        # 停止
docker compose down -v     # 停止并清空数据库（重新初始化 schema）
```

启动后访问 **http://localhost**，使用测试账号 `employee` / `Emp@2024` 登录。

## 一键升级包（推荐）

不必每次手动打包再拷到服务器，可用跨平台脚本或 IDEA 插件生成升级包（含后端 jar、前端 dist、Windows/Linux 应用脚本）：

```powershell
# Windows
.\deploy\upgrade\pack-upgrade.ps1
```

```bash
# Linux / macOS
chmod +x deploy/upgrade/pack-upgrade.sh
./deploy/upgrade/pack-upgrade.sh
```

产物在 `dist/upgrades/`。服务器解压后执行 `bin/apply.sh` 或 `bin/apply.ps1`。

IDEA 插件：见 [`idea-plugin/README.md`](idea-plugin/README.md)（Tools → HR Upgrade → 生成升级包）。说明见 [`deploy/upgrade/README.md`](deploy/upgrade/README.md)。

## 生产部署

### 后端

1. 打包：

```bash
mvn -DskipTests package
# 产物: target/hr-management-0.0.1-SNAPSHOT.jar
```

2. 配置环境变量（参考 `deploy/env.prod.example`）：

| 变量 | 说明 |
|------|------|
| `SPRING_PROFILES_ACTIVE` | 固定为 `prod` |
| `SPRING_DATASOURCE_URL` | MySQL JDBC 连接串 |
| `SPRING_DATASOURCE_USERNAME` | 数据库用户名 |
| `SPRING_DATASOURCE_PASSWORD` | 数据库密码 |
| `JWT_SECRET` | JWT 密钥，**至少 32 字符**，务必更换；生产环境若使用内置默认值，应用将**拒绝启动** |
| `APP_CORS_ORIGIN` | 前端访问域名，如 `https://hr.example.com` |

**安全说明（v1.0.4+）：**

- 登录/改密接口请求体**不会**写入 info 日志。
- 站内消息 SSE 使用短时一次性 `ticket`（`POST /notifications/stream-ticket` 签发），**不再**在 URL 中传递 JWT。
- Docker 部署请复制 [`deploy/.env.example`](deploy/.env.example) 为 `deploy/.env` 并设置强随机 `JWT_SECRET`。

3. 启动示例：

```bash
export SPRING_PROFILES_ACTIVE=prod
export SPRING_DATASOURCE_URL="jdbc:mysql://127.0.0.1:3306/hr_management?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=true"
export SPRING_DATASOURCE_USERNAME=hr_app
export SPRING_DATASOURCE_PASSWORD=your-password
export JWT_SECRET=your-long-random-secret-at-least-32-chars
export APP_CORS_ORIGIN=https://your-domain.com

java -jar target/hr-management-0.0.1-SNAPSHOT.jar
```

生产 profile 下 **Swagger 默认关闭**，SQL 日志关闭，日志级别为 `info`。

### 前端

1. 确认 `apps/web-antd/.env.production`：

```env
VITE_GLOB_API_URL=/api
VITE_ROUTER_HISTORY=hash
```

- **同域部署**（推荐）：`VITE_GLOB_API_URL=/api`，由 Nginx 将 `/api` 转发到后端。
- **跨域部署**：改为后端完整地址，如 `https://api.example.com/api`，并确保后端 `APP_CORS_ORIGIN` 包含前端域名。

2. 构建：

```bash
cd vue-vben-admin-main
pnpm install
pnpm build:antd
# 产物: apps/web-antd/dist
```

3. 将 `dist` 目录部署到 Nginx 静态目录。

### Nginx（同域反代）

参考 `deploy/nginx.conf.example`：

- `/` → 前端静态文件
- `/api/` → `http://127.0.0.1:8080/api/`

## 开发说明

### API 约定

- 统一前缀：`/api`（`server.servlet.context-path`）
- 认证：`Authorization: Bearer <token>`
- 响应格式：`{ "code": 200, "data": ..., "message": "..." }`

### 主要接口

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/auth/login` | 登录 |
| GET | `/auth/userinfo` | 当前用户信息 |
| GET | `/dashboard/calendar?yearMonth=yyyy-MM` | 个人考勤月历 |
| POST | `/attendance/check-in` | 上班打卡 |
| GET | `/employees` | 员工列表（经理+） |
| POST | `/leave/requests` | 提交请假 |

### 环境切换

| 环境 | Profile | 配置文件 |
|------|---------|----------|
| 本地开发 | `dev`（默认） | `application-dev.yml` |
| 生产 | `prod` | `application-prod.yml` + 环境变量 |

本地覆盖敏感配置可新建 `application-local.yml`（已加入 `.gitignore`），例如：

```yaml
spring:
  datasource:
    password: your-local-password
```

## 常见问题

**Q: 登录提示用户名或密码错误？**  
执行 `seed-test-data.sql` 更新密码，或确认使用上表中的独立密码。

**Q: 工作台不显示姓名？**  
确认 `sys_user.employee_id` 已绑定 `hr_employee.id`，重新登录刷新 userinfo。

**Q: 刷新页面后 401？**  
检查前端 `localStorage` 中 token 是否有效；后端 JWT 默认 24 小时过期。

**Q: 生产环境前端仍请求 mock 地址？**  
确认使用 `pnpm build:antd` 构建，且 `.env.production` 中 `VITE_GLOB_API_URL=/api`。

## License

本项目仅供学习与实践使用。
