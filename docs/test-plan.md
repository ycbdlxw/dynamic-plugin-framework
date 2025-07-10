# 动态插件框架测试计划

## 测试目标

验证动态插件框架的核心功能，确保系统能够按照预期工作，包括但不限于：

1. 用户认证与授权
2. 数据预处理机制
3. 过滤条件自动填充
4. 通用CRUD接口
5. 插件加载与卸载

## 测试环境

- 操作系统：macOS 24.5.0
- JDK版本：17+
- 数据库：H2内存数据库
- 应用服务器：Spring Boot内嵌Tomcat
- 端口：8081

## 测试API端点

### 重要说明

测试脚本执行应使用 `/api/test/execute` 端点，而非 `/api/test/run` 端点。`/api/test/run` 端点已被废弃，仅为向后兼容而保留，并会自动重定向到 `/api/test/execute` 端点。

- 正确的API调用方式：
  ```
  curl -X POST "http://localhost:8080/api/test/execute?scriptPath=/path/to/script.curl&resultDir=/path/to/results&useCurrentDir=true"
  ```

## 测试流程

### 1. 环境准备

1. 启动应用服务器：
   ```bash
   mvn spring-boot:run
   ```

2. 初始化测试环境：
   ```bash
   cd src/main/resources/test
   ./init_test_env.sh
   ```

### 2. 功能测试

#### 2.1 用户认证测试

执行以下命令测试用户认证功能：
```bash
./test_login.curl
```

测试内容：
- 正确的用户名和密码登录
- 错误的用户名登录
- 错误的密码登录
- 缺少参数登录

#### 2.2 新用户注册和登录测试

执行以下命令测试新用户注册和登录功能：
```bash
./test_new_user.curl
```

测试内容：
- 注册新用户
- 新用户登录
- 为新用户分配角色
- 使用新用户Token查询用户列表

#### 2.3 用户查询测试

执行以下命令测试用户查询功能：
```bash
./test_user_query.curl
```

测试内容：
- 查询所有用户
- 按用户名查询
- 分页查询
- 按状态查询

#### 2.4 数据预处理器测试

执行以下命令测试数据预处理器功能：
```bash
./test_data_preprocessor.curl
```

测试内容：
- 自动填充默认值
- 必填字段校验
- 审计字段自动填充
- 查询结果验证

#### 2.5 过滤规则测试

执行以下命令测试过滤规则功能：
```bash
./test_filter_rule.curl
```

测试内容：
- 创建测试数据
- 创建第二个用户
- 使用第二个用户创建数据
- 管理员查询所有数据
- 用户2查询数据（应该只看到自己的）

#### 2.6 通用API测试

执行以下命令测试通用API接口：
```bash
./test_common_api.curl
```

测试内容：
- 健康检查测试
- 获取列表测试
- 保存数据测试
- 更新数据测试
- 删除数据测试
- 未授权测试

### 3. 一键执行所有测试

执行以下命令一键运行所有测试：
```bash
./run_tests.sh
```

测试结果将保存在`test_results`目录下。

## 预期结果

1. 所有API接口应返回正确的状态码和数据结构
2. 用户认证应正确验证用户身份并返回有效的Token
3. 数据预处理应正确处理默认值、必填校验和审计字段
4. 过滤条件应根据用户上下文正确过滤数据
5. 通用CRUD接口应正确处理数据的增删改查操作

## 测试报告

测试完成后，查看`docs/test/test-report.md`获取详细的测试报告。 