# 动态插件框架

动态插件框架是一个基于Spring Boot的可扩展应用框架，支持在运行时动态加载和卸载插件，实现功能的热插拔。

## 主要特性

- 动态插件加载/卸载
- 通用数据访问API
- 命令执行插件
- 测试服务插件
- 数据预处理机制
- 权限控制

## 快速开始

### 环境要求

- JDK 17+
- Maven 3.8+
- MySQL 8.0+

### 构建与运行

```bash
# 克隆项目
git clone https://github.com/ycbd/dynamic-plugin-framework.git
cd dynamic-plugin-framework

# 构建项目
mvn clean package

# 运行项目
java -jar target/dynamic-plugin-framework-1.0.0.jar
```

## 测试

本项目采用自动化测试方式，所有测试脚本位于 `src/main/resources/test` 目录下。

### 测试规则

所有测试必须遵循项目的测试规则，详见 [测试规则文档](docs/test/10-testing-rules.mdc)。

主要规则包括：

- 所有测试脚本必须符合规定的格式规范
- 所有测试必须通过测试服务的execute方法执行
- 测试文件必须包含完整的头部注释和测试步骤说明

### 执行测试

```bash
# 启动测试环境
cd src/main/resources/test/runs
sh start_test_env.sh

# 执行所有测试
sh run_all_tests.sh

# 执行单个测试
sh run_basic_test.sh
sh run_login_test.sh
sh run_common_api_test.sh
sh run_command_executor_test.sh
sh run_plugin_management_test.sh
```

### 创建新测试

使用测试脚本生成工具可以快速创建符合规范的测试文件：

```bash
cd src/main/resources/test
./generate_test_script.sh <模块名> <功能点>
```

例如：

```bash
./generate_test_script.sh user login
```

## 文档

- [架构设计](docs/dev/architecture.md)
- [API文档](docs/api/common-controller.md)
- [插件开发指南](docs/plugins/command-executor-plugin.md)
- [用户手册](docs/user/getting-started.md)
- [测试规范](docs/test/10-testing-rules.mdc)

## 插件列表

- CommandExecutor - 命令执行插件
- TestService - 测试服务插件

## 贡献

欢迎提交Pull Request或Issue。

## 许可证

MIT
