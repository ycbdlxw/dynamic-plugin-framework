# 动态插件框架

一个基于Spring Boot的动态插件框架，支持插件的热加载和卸载，以及基于数据库配置的动态业务逻辑。

## 核心功能

- **插件化架构**：支持插件的动态加载、卸载和版本管理
- **数据驱动**：基于数据库配置驱动业务逻辑，减少硬编码
- **数据预处理机制**：自动处理默认值、必填校验、审计字段等
- **过滤条件自动填充**：基于配置自动从用户上下文中获取过滤条件
- **通用CRUD接口**：提供标准化的数据操作接口

## 数据预处理机制

系统实现了一套完整的数据预处理机制（`DataPreprocessorService`），负责在数据持久化前对数据进行统一的预处理，包括：

1. **用户上下文自动填充**：根据列属性配置表中的`edit_flag=1`标记，自动从用户上下文中获取同名字段值
2. **默认值处理**：根据列属性配置表中的`default_value`配置，自动为空值字段填充默认值，支持静态值和动态表达式
3. **必填校验**：根据列属性配置表中的`is_required=1`标记，自动校验必填字段
4. **审计字段处理**：自动处理标准审计字段（创建人、创建时间、更新人、更新时间等）
5. **批量数据处理**：支持批量数据的预处理，并确保批量数据的字段一致性

详细文档请参考 [数据预处理机制文档](docs/dev/数据预处理机制.md)。

## 快速开始

### 环境要求

- JDK 17+
- Maven 3.6+
- MySQL 5.7+ 或 H2 Database

### 构建与运行

```bash
# 克隆项目
git clone https://github.com/yourusername/dynamic-plugin-framework.git
cd dynamic-plugin-framework

# 构建项目
mvn clean package

# 运行项目
java -jar target/dynamic-plugin-framework-1.0.0.jar
```

### 访问接口

启动成功后，可以通过以下地址访问系统：

- API接口: http://localhost:8080/api/
- H2控制台: http://localhost:8080/h2-console (仅开发环境)

## 文档

- [开发文档](docs/dev/开发文档.md)
- [用户手册](docs/user/getting-started.md)
- [API文档](docs/api/common-controller.md)
- [测试规范](docs/测试规范.md)

## 贡献

欢迎提交问题和功能需求。如果您想贡献代码，请遵循以下步骤：

1. Fork 项目
2. 创建您的特性分支 (`git checkout -b feature/amazing-feature`)
3. 提交您的更改 (`git commit -m "Add some amazing feature"`)
4. 推送到分支 (`git push origin feature/amazing-feature`)
5. 打开一个 Pull Request

## 许可证

本项目采用 MIT 许可证 - 详情请参见 [LICENSE](LICENSE) 文件。
