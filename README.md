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

项目使用 Maven 构建。

```bash
mvn test
mvn spring-boot:run
```

数据库、Redis 连接统一在 [application.yml](src/main/resources/application.yml) 中配置：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/official_website?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
    username: official_website
    password: ""
  data:
    redis:
      host: localhost
      port: 6379
      password: ""
```

## 当前状态

当前仓库已创建基础文档、规则文件、Maven 构建配置、后端包结构骨架和公共响应组件。MySQL/Redis 部署参数、数据库迁移工具、认证框架和文件存储方案仍待确认。

当前已预置统一响应 `ApiResponse`、数字错误码枚举 `ErrorCode`、业务异常 `BusinessException` 和全局异常处理器。新增接口时必须复用这些公共类，不得新增第二套统一返回结构或错误码定义。

新增、修改或删除接口时，必须同步更新 [docs/接口文档.md](docs/接口文档.md)，并标注 Controller 文件路径和方法名。
