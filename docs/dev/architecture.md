# 系统架构文档

## 1. 系统概述

动态插件框架是一个基于Java Spring Boot的可扩展系统，通过插件化架构实现业务功能的动态加载和卸载。系统遵循"数据驱动一切"和"配置优于编码"的核心理念，大部分业务逻辑通过数据库配置实现，而非硬编码。

## 2. 系统架构

系统采用分层架构，主要包括以下几层：

1. **控制层（Controller）**：处理HTTP请求，进行权限验证和参数校验
2. **服务层（Service）**：实现业务逻辑，处理数据
3. **数据访问层（Mapper）**：与数据库交互，执行SQL语句
4. **插件层（Plugin）**：提供可动态加载卸载的业务功能扩展

### 2.1 系统架构图

```mermaid
flowchart TB
    subgraph "核心引擎"
        C[控制层] --> S[服务层]
        S --> M[数据访问层]
        M --> DB[(数据库)]
    end
    
    subgraph "插件系统"
        PE[插件引擎] --> P1[插件1]
        PE --> P2[插件2]
        PE --> P3[插件3]
    end
    
    C <--> PE
    
    subgraph "安全层"
        AA[认证切面]
    end
    
    AA -.-> C
```

## 3. 核心组件

### 3.1 核心引擎

核心引擎负责系统的基础功能，包括通用CRUD操作、权限验证、数据校验等。

#### 3.1.1 CommonController

提供通用的数据操作接口，包括查询、保存和删除功能。

#### 3.1.2 BaseService

提供基础的服务功能，包括数据库操作、缓存管理等。

#### 3.1.3 SystemMapper

提供系统级的数据库操作，包括配置表的读取、系统参数的获取等。

### 3.2 插件系统

插件系统负责管理和加载插件，实现业务功能的动态扩展。

#### 3.2.1 PluginEngine

负责插件的加载、卸载和生命周期管理。

```mermaid
sequenceDiagram
    participant A as 应用程序
    participant PE as 插件引擎
    participant P as 插件
    
    A->>PE: 初始化插件引擎
    PE->>PE: 扫描插件目录
    loop 每个插件
        PE->>P: 加载插件
        P-->>PE: 注册插件信息
    end
    A->>PE: 获取插件列表
    PE-->>A: 返回插件列表
    A->>PE: 调用插件方法
    PE->>P: 转发调用
    P-->>PE: 返回结果
    PE-->>A: 返回结果
```

#### 3.2.2 IPlugin接口

所有插件必须实现的接口，定义了插件的基本行为。

```mermaid
classDiagram
    class IPlugin {
        +String getName()
        +String getVersion()
        +void initialize()
        +void destroy()
        +boolean isEnabled()
    }
    
    class TestServicePlugin {
        -String name
        -String version
        +String getName()
        +String getVersion()
        +void initialize()
        +void destroy()
        +boolean isEnabled()
        +executeCommand(String command)
    }
    
    class CommandExecutorPlugin {
        -String name
        -String version
        +String getName()
        +String getVersion()
        +void initialize()
        +void destroy()
        +boolean isEnabled()
        +executeCommand(String command)
    }
    
    IPlugin <|-- TestServicePlugin
    IPlugin <|-- CommandExecutorPlugin
```

### 3.3 安全层

安全层负责系统的安全控制，包括认证、授权和数据权限控制。

#### 3.3.1 AuthenticationAspect

负责请求的认证和授权，验证用户身份和权限。

```mermaid
flowchart TD
    A[请求开始] --> B{是否白名单路径?}
    B -->|是| C[允许访问]
    B -->|否| D{是否有Token?}
    D -->|是| E[解析Token]
    D -->|否| F[返回401未授权]
    E --> G{Token有效?}
    G -->|是| H[设置用户上下文]
    G -->|否| F
    H --> I[继续处理请求]
    C --> I
    I --> J[请求结束]
```

#### 3.3.2 UserContext

存储当前用户的上下文信息，包括用户ID、用户名、权限等。

## 4. 数据模型

系统的核心数据模型包括：

### 4.1 配置表

```mermaid
erDiagram
    table_attribute {
        int id PK
        string table_name
        string description
        boolean is_enabled
    }
    
    column_attribute {
        int id PK
        int table_id FK
        string column_name
        string data_type
        boolean is_required
        string description
    }
    
    column_check_property {
        int id PK
        int column_id FK
        string check_type
        string check_value
        string error_message
    }
    
    table_attribute ||--o{ column_attribute : has
    column_attribute ||--o{ column_check_property : has
```

### 4.2 用户权限表

```mermaid
erDiagram
    sys_user {
        int id PK
        string username
        string password
        string real_name
        string email
        int status
    }
    
    sys_role {
        int id PK
        string role_name
        string description
        int status
    }
    
    sys_permission {
        int id PK
        string permission_code
        string permission_name
        string resource_type
        string resource_path
    }
    
    sys_user_role {
        int user_id FK
        int role_id FK
    }
    
    sys_role_permission {
        int role_id FK
        int permission_id FK
    }
    
    sys_user ||--o{ sys_user_role : has
    sys_role ||--o{ sys_user_role : has
    sys_role ||--o{ sys_role_permission : has
    sys_permission ||--o{ sys_role_permission : has
```

## 5. 关键流程

### 5.1 请求处理流程

```mermaid
flowchart TD
    A[HTTP请求] --> B[认证切面]
    B --> C{认证通过?}
    C -->|是| D[控制器处理]
    C -->|否| E[返回401/403]
    D --> F{是否通用请求?}
    F -->|是| G[CommonController]
    F -->|否| H[其他Controller]
    G --> I[BaseService]
    H --> I
    I --> J[SystemMapper]
    J --> K[数据库]
    K --> L[返回结果]
```

### 5.2 插件加载流程

```mermaid
flowchart TD
    A[系统启动] --> B[初始化插件引擎]
    B --> C[扫描插件目录]
    C --> D{发现插件?}
    D -->|是| E[加载插件类]
    D -->|否| F[完成初始化]
    E --> G[实例化插件]
    G --> H[调用插件initialize方法]
    H --> I[注册插件到系统]
    I --> J{还有更多插件?}
    J -->|是| C
    J -->|否| F
```

## 6. 设计决策

### 6.1 为什么选择插件化架构

插件化架构允许系统在不重启的情况下动态加载和卸载功能模块，提高了系统的灵活性和可扩展性。这种架构特别适合需要频繁添加新功能的系统。

### 6.2 为什么采用数据驱动

数据驱动设计将业务规则和配置存储在数据库中，而不是硬编码在代码中，这使得系统更加灵活，能够在不修改代码的情况下调整业务规则。

## 7. 注意事项和限制

1. 插件开发必须遵循IPlugin接口规范
2. 数据库配置修改需要谨慎，可能影响系统行为
3. 白名单路径必须从数据库获取，不允许硬编码
4. 用户信息必须通过UserContext获取，不允许从其他途径获取 