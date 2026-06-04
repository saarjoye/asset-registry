# 工作设备登记系统

内网 Web 系统，Vue 3 + Spring Boot + MyBatis-Plus，支持 PostgreSQL / H2 两种持久化模式，提供 Docker 一键部署。

## 技术栈

- 前端：Vue 3 + Vite + TypeScript
- 后端：Spring Boot 3 + Spring Security + MyBatis-Plus + Validation
- 数据库：PostgreSQL（生产）/ H2 file（本地开发）
- 部署：Docker + docker-compose

## 目录结构

```
backend/                 后端 Spring Boot 工程
  src/main/java/         业务代码（entity / mapper / service / controller）
  src/main/resources/db  schema-h2.sql（本地/测试） / schema-postgres.sql（PostgreSQL 部署）
  Dockerfile             多阶段构建（Maven build -> JRE 17 运行时）
frontend/                前端 Vue 工程
  src/api                fetch 客户端 + 字段适配
  Dockerfile             多阶段构建（Node build -> Nginx 静态服务）
  nginx.conf             /api 反向代理到 backend:8080
docker-compose.yml       backend + frontend + H2 一键测试启动（无需 .env）
docker-compose.pgsql.yml PostgreSQL 生产部署（使用公开预构建镜像）
```

## 数据库 schema 说明

后端按 Spring profile 选择不同的初始化脚本：

| Profile | 默认值 | 数据库 | 初始化脚本 | `continue-on-error` | 说明 |
|---|---|---|---|---|---|
| `dev` | 启用 | H2 file (`./data/work.mv.db`) | `db/schema-h2.sql` | `false` | 本机开发，所有 DDL 带 `IF NOT EXISTS` 严格幂等 |
| `prod` | PostgreSQL 部署 | PostgreSQL | `db/schema-postgres.sql` | `false` | `docker-compose.pgsql.yml` 使用，表和索引均使用 `IF NOT EXISTS`，重复启动保持幂等 |

切换 profile：通过 `SPRING_PROFILES_ACTIVE=dev|prod` 环境变量覆盖默认值。

## 本地开发模式（H2 内嵌）

无需额外数据库，H2 file 数据落在 `backend/data/work.mv.db`，重启后保留。

```powershell
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

另开终端：

```powershell
cd frontend
npm install
npm run dev
```

打开 http://127.0.0.1:5173/ 首次进入要求创建管理员账号。

H2 控制台：http://127.0.0.1:8080/h2-console
JDBC URL：`jdbc:h2:file:./data/work;MODE=MySQL`，用户 `sa`，无密码。这里的 `MODE=MySQL` 只是 H2 兼容模式，不代表生产环境使用 MySQL。

## Excel 批量导入

管理员和人事账号登录后，可在“批量导入”页面上传 Excel。文件只在本系统后端解析，不会上传到外部服务。

每个导入卡片都提供“下载模板”按钮，建议先下载模板再填写，避免 Sheet 名称或表头不一致导致导入失败。

部门岗位导入建议使用两个 Sheet：

| Sheet | 表头 |
|---|---|
| `部门` | `部门名称` |
| `岗位` | `职位名称`、`所属部门` |

人员档案导入使用 `人员` Sheet：

| 表头 | 说明 |
|---|---|
| `姓名` | 必填，同名允许存在，由系统人员编号区分 |
| `性别` | 必填，仅支持 `男` / `女` |
| `入职时间` | 必填，建议格式 `yyyy-MM-dd` |
| `用户名` | 必填，作为登录账号，重复用户名会跳过 |
| `职位` | 必填，按“部门 + 职位”匹配，不存在则自动创建 |
| `部门` | 必填，不存在则自动创建 |

导入人员默认 `年龄=0`、`状态=在职`、`角色=员工`、`初始密码=123456`。

## Docker 一键测试（无需 .env）

```powershell
docker compose up -d --build
```

启动后访问：

- 前端：http://127.0.0.1:5173/
- 后端：http://127.0.0.1:8080/

默认使用 H2 file 数据库，数据通过 named volume `backend-data` 持久化。停服不会丢数据；如需重置：

```powershell
docker compose down -v
```

首次进入前端时创建管理员账号；管理员账号属于系统业务数据，不需要通过 `.env` 配置。

## PostgreSQL 生产部署（宿主机变量）

数据库连接信息必须在应用启动前存在，不能放到管理后台里后置设置。正式部署时不要写 `.env` 文件，直接在 `docker-compose.pgsql.yml` 中替换 PostgreSQL 地址、账号和密码占位值。

`docker-compose.pgsql.yml` 使用公开预构建镜像，不需要在 NAS 上构建，也不需要拉取 Maven / Node 基础镜像：

```powershell
docker compose -p asset-registry -f docker-compose.pgsql.yml pull
docker compose -p asset-registry -f docker-compose.pgsql.yml up -d
```

如果 PostgreSQL 在宿主机本机，Docker Desktop 通常可用 `host.docker.internal` 作为 `DB_HOST`；NAS 上建议填 PostgreSQL 所在机器的局域网 IP、容器网络别名或反向代理域名。

Redis 当前项目尚未使用；如果后续要做登录会话、验证码、缓存或分布式锁，再接入 `spring-boot-starter-data-redis`。

## 环境变量

Compose 中需要替换以下占位值；不需要也不推荐提交 `.env` 文件：

| 配置项 | 示例值 | 说明 |
|---|---|---|
| `DB_HOST` | `192.168.1.10` | PostgreSQL 主机 |
| `DB_PORT` | `5432` | PostgreSQL 端口 |
| `DB_NAME` | `work_device_registry` | 业务库名 |
| `DB_USERNAME` | `asset` | PostgreSQL 业务账号 |
| `DB_PASSWORD` | 强密码 | PostgreSQL 业务账号密码 |

## 接口

所有接口位于 `/api/*`，主要资源：

- `POST /api/auth/setup` 创建管理员（首次部署时由前端调用）
- `POST /api/auth/login` 登录
- `GET /api/archive/employees|phones|devices|accounts|departments|positions` 拉取全表
- `POST /api/archive/employees[?includeAccount=true]` 增改员工
- `POST /api/archive/departments|positions` 增改部门 / 岗位
- `POST /api/archive/import/departments-positions` Excel 导入部门 / 岗位
- `POST /api/archive/import/employees` Excel 导入人员档案
- `GET /api/archive/import/templates/departments-positions` 下载部门岗位导入模板
- `GET /api/archive/import/templates/employees` 下载人员档案导入模板
- `POST /api/archive/accounts/open` 开通登录账号
- `POST /api/registry/users/{employeeId}/devices` 登记设备
- `POST /api/registry/users/{employeeId}/accounts` 登记渠道账号
- `GET /api/registry/users/{employeeId}/summary?scope=mine|department|all` 汇总
- `POST /api/handover/users/{employeeId}/resignation` 提交离职
- `POST /api/handover/allocate?allocatorId=...` 资产分配
- `POST /api/handover/approve?supervisorId=...` 主管审批
- `POST /api/handover/{taskId}/confirm?receiverId=...` 接收确认
- `POST /api/handover/reject?receiverId=...` 接收回退
- `POST /api/recycle/{deviceId}/confirm?supervisorId=...` 确认回收
- `POST /api/recycle/transfer` 设备移交
- `POST /api/recycle/{deviceId}/stock-in?supervisorId=...` 旧机入库

## 数据安全

- 系统不内置任何员工 / 手机号 / 设备 / 渠道账号 / 身份证测试数据。
- 渠道账号密码与身份证号后端加密（`account_password_cipher` / `id_card_cipher`），接口不返回原文，页面默认显示 `******` / 脱敏。
- 登录密码以 `{noop}<明文>` 形式入库（dev/演示用），正式上线前必须改为 BCrypt 等单向哈希方案。
- 同名人员通过系统自动生成的人员编号区分。
