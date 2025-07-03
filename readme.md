# 动态插件框架（Dynamic Plugin Framework）

## 项目简介

该项目是一个基于Spring Boot构建的高度动态化、配置驱动的企业级应用框架。它完美融合了"配置驱动一切"的数据处理引擎和"热插拔"的动态插件引擎，旨在实现最大程度的灵活性、可扩展性和代码复用。

## 核心特点

- **双引擎驱动**:

  - **配置驱动的数据引擎**: 无需编写Java代码，仅通过修改数据库配置表即可实现任何业务表的通用CRUD、数据校验和API暴露。
  - **动态插件引擎**: 支持在**不重启服务**的情况下，动态地热加载、启用、禁用和卸载功能模块。
- **全面的安全机制**:

  - 内置基于Hutool-JWT和AOP的统一认证授权。
  - 安全策略（如API白名单）完全由数据库动态配置，实时生效。
  - 通过 `UserContext`提供可靠、线程隔离的用户信息上下文。
- **企业级开发规范**:

  - 严格遵循**三层架构** (Controller -> Service -> Mapper) 和 **"三不三少"** 开发原则。
  - 实现了统一的API响应格式、全局异常处理和参数校验服务。
  - 集成了**SpringDoc**，自动生成交互式API文档。
  - 具备完善的单元测试和日志体系。

## 架构设计

1. **双引擎架构**:
   - **数据引擎**: 由 `CommonService`, `BaseService`, `SystemMapper`及相关配置表构成，负责所有通用数据操作。
   - **插件引擎**: 由 `PluginEngine`, `IPlugin`接口及 `plugin_config`表构成，负责动态功能的生命周期管理。
2. **三层架构**: 严格遵守 **表现层(Controller)** -> **业务层(Service)** -> **数据访问层(Mapper)** 的分层结构。
3. **安全层**: `AuthenticationAspect`作为安全切面，对所有进入Controller的请求进行统一的JWT校验和用户上下文设置。
4. **测试服务**: 通过内置的测试服务应用，完成针对curl测试脚本的运行，并生成相应的测试报告。

## 技术栈

- **后端**: Spring Boot, Spring AOP, MyBatis
- **数据库**: H2 (演示), MySQL (生产)
- **安全**: Hutool-JWT
- **工具**: Hutool, SpringDoc OpenAPI

## 快速开始

1. **环境准备**:

   - JDK 1.8 或更高版本
   - Maven 3.6 或更高版本
2. **克隆或下载项目**
3. **运行项目**:

   ```bash
   mvn spring-boot:run
   ```
4. **服务访问**:

   - **API服务**: `http://localhost:8080`
   - **H2数据库控制台**: `http://localhost:8080/h2-console`
     - JDBC URL: `jdbc:h2:mem:demodb`
     - User Name: `sa`
     - Password: (空)
   - **API文档 (Swagger UI)**: `http://localhost:8080/swagger-ui.html`

## 核心功能

### 1. 用户认证与授权

- **注册**: `POST /api/core/register` (白名单接口)
- **登录**: `POST /api/core/login` (白名单接口)

### 2. 通用数据接口 (需携带Token)

- **通用保存/更新**: `POST /api/common/save?targetTable=<表名>`
- **通用列表查询**: `GET /api/common/list?targetTable=<表名>&<查询参数>`
- **通用删除**: `POST /api/common/delete?targetTable=<表名>&id=<记录ID>`

### 3. 动态插件系统

项目内置多种可动态加载的插件：

1. **命令执行器插件 (CommandExecutor)**:

   - 提供跨平台命令行执行能力，支持Windows/Linux/macOS
   - 接口：`POST /api/command/execute`
2. **测试服务插件 (TestService)**:

   - 提供自动化接口测试框架，支持通过脚本文件批量执行HTTP接口测试
   - 接口：`POST /api/test/execute`
   - 支持的测试脚本：`test_file_pipeline.curl`, `test_command_executor.curl`, `test_user_crud.curl`等

### 4. 插件热部署

1. **准备插件**: 将插件的 `.class`文件放入项目根目录下的 `plugins/`目录。
2. **配置数据库**: 在 `plugin_config`表中添加插件配置。
3. **触发热加载**: `POST /api/system/plugins/load?pluginName=<插件名>`

## 开发规范

### API设计

- **RESTful风格**: 使用标准HTTP方法 (GET, POST, PUT, DELETE)
- **统一响应**: 所有Controller接口必须返回 `ApiResponse`对象，确保前端接收到统一的 `{code, message, data}`格式
- **统一入口**: 通用CRUD操作通过 `/api/common`接口，插件功能通过独立命名空间

### 安全与认证

- **JWT认证**: 非白名单接口须携带 `Authorization: Bearer <token>`
- **用户上下文**: 业务逻辑中通过 `UserContext`获取用户信息，禁止从前端参数获取
- **白名单配置**: API白名单由 `security_config`表动态管理

### 插件开发流程

1. **创建主类**: 实现 `IPlugin`接口
2. **封装组件**: 将插件组件放在独立包内
3. **生命周期管理**:
   - `initialize()`: 动态注册Bean和Controller
   - `shutdown()`: 注销Bean并清理资源
4. **打包部署**: 将编译后的 `.class`文件（保持包结构）放入 `plugins/`目录

## 测试服务使用

测试服务提供了一个自动化的接口测试框架，支持执行.curl脚本文件：

```bash
# 基本调用
curl -X POST 'http://localhost:8080/api/test/execute?scriptPath=src/main/resources/test/test_user_crud.curl'

# 指定结果目录
curl -X POST 'http://localhost:8080/api/test/execute?scriptPath=src/main/resources/test/test_file_pipeline.curl&resultDir=/tmp/test_results'
```

### 测试脚本生成

项目提供了测试脚本生成工具，可以快速生成测试文件和执行脚本：

```bash
# 进入测试目录
cd src/main/resources/test

# 生成测试脚本
./generate_test_script.sh common api

# 生成的文件:
# - test_common_api.curl (测试用例文件)
# - runs/run_common_api_test.sh (执行脚本)
```

## 核心哲学与开发原则

1. **数据驱动一切**: 业务逻辑、校验规则、API行为应优先通过配置表驱动
2. **配置优于编码**: 能通过配置实现的功能不硬编码
3. **插件化一切**: 独立业务功能应实现为可热插拔的插件
4. **"三不三少"原则**:
   - **三不**: 不过度开发、不过度复杂、不破坏现有功能
   - **三少**: 少文件、少代码、少调用
5. **测试驱动开发**: 所有功能优化、调整或新增都必须有相应的测试用例
6. **文档同步更新**: 代码变更必须同步更新相关文档

## 项目文档

### 用户文档

- [入门指南](docs/user/getting-started.md) - 系统使用入门指南
- [测试服务使用指南](docs/user/test-service-usage.md) - 如何使用测试服务

### API文档

- [通用控制器接口](docs/api/common-controller.md) - CommonController API接口文档

### 开发文档

- [系统架构](docs/dev/architecture.md) - 系统架构设计文档
- [开发指南](docs/dev/开发文档.md) - 详细开发规范和指南
- [数据库驱动示例](docs/dev/db_driver_demo.md) - 数据库驱动示例
- [操作日志](docs/dev/operation-log.md) - 操作日志设计
- [调度中心设计](docs/dev/调度中心与业务逻辑完全分离.md) - 调度中心与业务逻辑分离设计

### 插件文档

- [命令执行器插件](docs/plugins/command-executor-plugin.md) - CommandExecutor插件使用指南
- [测试服务插件](docs/plugins/test-service.md) - TestService插件详细说明

### 规范文档

- [测试规范](docs/测试规范.md) - 测试编写和执行规范
- [测试规划](docs/test-plan.md) - 测试规划和脚本生成规范
- [文档规范](docs/文档规范.md) - 文档编写和维护规范

## 许可证

[MIT License](LICENSE)
