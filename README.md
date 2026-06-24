# official-website

官网后台与 Portal 接口后端服务，当前使用 Java 17、Spring Boot 3.3.6、MyBatis Plus 3.5.9、MySQL、Redis 客户端与 Maven。

## 文档入口

开发、审查或自动化 Agent 修改本仓库前，必须先阅读 [AGENTS.md](AGENTS.md)。

核心文档：

* [需求说明](docs/需求说明.md)
* [架构设计说明](docs/架构设计说明.md)
* [数据库设计说明](docs/数据库设计说明.md)
* [接口文档](docs/接口文档.md)
* [错误码说明](docs/错误码说明.md)

规则文档：

* [后端开发规则](rules/backend.md)
* [安全开发规则](rules/security.md)
* [测试规则](rules/testing.md)
* [代码 Review 规则](rules/review.md)

## 本地启动

本仓库默认使用 Docker 作为本地运行与验证环境。

### 启动依赖与应用

```bash
docker compose up --build -d
```

启动后默认暴露以下端口：

* `8080`：Spring Boot 应用
* `3306`：MySQL
* `6379`：Redis

默认会创建以下本地开发资源：

* MySQL 数据库：`official_website`
* MySQL 用户：`official_website`
* MySQL 密码：`official_website_local`
* MySQL root 密码：`official_website_root_local`

这些默认凭据仅用于本地 Docker 开发环境，不能复用于测试、预发或生产环境。

Flyway 会在应用启动时自动执行数据库迁移，并初始化后台管理员账号：

* 用户名：`admin`
* 密码：`Admin@123456`

媒体文件默认落在 Docker 卷 `media-data` 中，并通过 `/media/public/**` 对外提供访问。

### 执行测试

```bash
docker compose run --rm maven mvn -B test
```

该命令会在 Maven 容器中执行测试，复用仓库目录和 Maven 本地缓存卷。Docker 内的 Maven 下载默认通过阿里云 Maven Central 镜像加速，当前测试默认使用 H2，并通过 Compose 内的 Redis 服务完成 Redis 相关依赖注入。

### 停止环境

```bash
docker compose down
```

如果需要连同数据卷一起清理：

```bash
docker compose down -v
```

### 覆盖本地参数

如需覆盖数据库、Redis 或媒体目录参数，可在执行 `docker compose` 前通过环境变量覆盖，例如：

```bash
OW_DB_PASSWORD=my-local-password OW_DB_ROOT_PASSWORD=my-root-password docker compose up --build -d
```

应用的 Docker 专用配置位于 [application-docker.yml](src/main/resources/application-docker.yml)。Docker 构建和 `maven` 工具容器使用的 Maven 镜像配置位于 [.mvn/settings-docker.xml](.mvn/settings-docker.xml)。

### 非 Docker 备用方式

如果必须脱离 Docker 运行，可自行准备 MySQL、Redis 和 JDK 17/Maven 环境，再执行：

```bash
mvn test
mvn spring-boot:run
```

数据库、Redis 连接和本地开发示例参数可参考 [application-local.example.yml](src/main/resources/application-local.example.yml)。

## 当前状态

当前仓库已创建基础文档、规则文件、Maven 构建配置、后端包结构骨架和公共响应组件，并补充了 Docker 本地运行基线。MySQL/Redis 生产部署参数、数据库迁移工具最终策略、认证框架和文件存储方案仍待确认。

当前已预置统一响应 `ApiResponse`、数字错误码枚举 `ErrorCode`、业务异常 `BusinessException` 和全局异常处理器。新增接口时必须复用这些公共类，不得新增第二套统一返回结构或错误码定义。

新增、修改或删除接口时，必须同步更新 [docs/接口文档.md](docs/接口文档.md)，并标注 Controller 文件路径和方法名。
