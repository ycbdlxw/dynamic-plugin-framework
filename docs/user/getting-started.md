# 动态插件框架入门指南

## 1. 系统概述

动态插件框架是一个基于Spring Boot构建的高度动态化、配置驱动的企业级应用框架。它完美融合了"配置驱动一切"的数据处理引擎和"热插拔"的动态插件引擎，旨在实现最大程度的灵活性、可扩展性和代码复用。

## 2. 环境准备

在开始使用动态插件框架前，请确保您的环境满足以下要求：

- JDK 1.8 或更高版本
- Maven 3.6 或更高版本
- MySQL 5.7+ 或 H2 数据库

## 3. 快速开始

### 3.1 安装与启动

1. 克隆或下载项目代码
   ```bash
   git clone https://github.com/yourusername/dynamic-plugin-framework.git
   cd dynamic-plugin-framework
   ```

2. 编译项目
   ```bash
   mvn clean package
   ```

3. 运行项目
   ```bash
   java -jar target/dynamic-plugin-framework-1.0.0.jar
   ```

4. 访问服务
   - API服务: http://localhost:8080
   - H2数据库控制台: http://localhost:8080/h2-console
     - JDBC URL: jdbc:h2:mem:demodb
     - User Name: sa
     - Password: (空)
   - API文档 (Swagger UI): http://localhost:8080/swagger-ui.html

### 3.2 用户认证

1. 注册新用户
   ```bash
   curl -X POST "http://localhost:8080/api/core/register" \
     -H "Content-Type: application/json" \
     -d '{"username":"testuser","password":"password123","real_name":"Test User","email":"test@example.com"}'
   ```

2. 用户登录
   ```bash
   curl -X POST "http://localhost:8080/api/core/login" \
     -H "Content-Type: application/json" \
     -d '{"username":"testuser","password":"password123"}'
   ```

3. 使用返回的Token进行后续API调用
   ```bash
   curl -X GET "http://localhost:8080/api/common/list?targetTable=sys_user" \
     -H "Authorization: Bearer YOUR_TOKEN_HERE"
   ```

## 4. 核心功能

### 4.1 通用数据接口

动态插件框架提供了一组通用的数据操作接口，可以对系统中的任何数据表进行CRUD操作：

1. 查询数据列表
   ```bash
   curl -X GET "http://localhost:8080/api/common/list?targetTable=sys_user&pageIndex=0&pageSize=10" \
     -H "Authorization: Bearer YOUR_TOKEN_HERE"
   ```

2. 保存/更新数据
   ```bash
   curl -X POST "http://localhost:8080/api/common/save?targetTable=sys_user" \
     -H "Content-Type: application/json" \
     -H "Authorization: Bearer YOUR_TOKEN_HERE" \
     -d '{"id":1,"email":"updated@example.com"}'
   ```

3. 删除数据
   ```bash
   curl -X POST "http://localhost:8080/api/common/delete?targetTable=sys_user&id=1" \
     -H "Authorization: Bearer YOUR_TOKEN_HERE"
   ```

### 4.2 使用插件功能

系统内置了多个功能插件，可以通过相应的API接口使用：

1. 命令执行器插件
   ```bash
   curl -X POST "http://localhost:8080/api/command/execute" \
     -H "Content-Type: application/json" \
     -H "Authorization: Bearer YOUR_TOKEN_HERE" \
     -d '{"command":"echo Hello World"}'
   ```

2. 测试服务插件
   ```bash
   curl -X POST "http://localhost:8080/api/test/execute?scriptPath=src/main/resources/test/test_user_crud.curl" \
     -H "Authorization: Bearer YOUR_TOKEN_HERE"
   ```

## 5. 常见问题

### 5.1 认证失败

如果遇到"401 Unauthorized"错误，请检查：
- Token是否已过期
- Token格式是否正确（应为"Bearer YOUR_TOKEN_HERE"）
- 请求的API是否需要认证（非白名单API）

### 5.2 权限不足

如果遇到"403 Forbidden"错误，请检查：
- 当前用户是否有权限访问该资源
- 用户角色是否正确配置

### 5.3 数据操作失败

如果通用数据接口返回错误，请检查：
- 目标表名是否正确
- 提交的数据格式是否符合表结构要求
- 是否违反了数据库约束（如唯一键约束）

## 6. 更多资源

- [API接口文档](/docs/api/common-controller.md) - 详细的API接口说明
- [测试服务使用指南](/docs/user/test-service-usage.md) - 如何使用测试服务
- [插件开发指南](/docs/dev/architecture.md) - 如何开发自定义插件 