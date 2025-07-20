# CommonController 接口文档

## 概述

CommonController提供通用的数据操作接口，包括数据查询、保存和删除功能。这些接口支持对系统中任意数据表进行操作，通过参数指定目标表。

## 安全要求

除了健康检查接口外，所有接口都需要JWT令牌认证。请在请求头中添加`Authorization: Bearer {token}`。

## 接口列表

### 1. 健康检查

#### 接口描述

检查服务是否正常运行。

#### 请求方法

GET

#### 请求URL

/api/common/health

#### 请求参数

无

#### 响应参数

| 参数名 | 类型   | 描述     |
|-------|--------|---------|
| code  | Integer | 状态码   |
| msg   | String  | 状态信息 |
| data  | String  | 服务状态 |

#### 响应示例

```json
{
  "code": 200,
  "msg": "success",
  "data": "Service is running"
}
```

### 2. 获取数据列表

#### 接口描述

根据条件查询指定表的数据列表。

#### 请求方法

GET

#### 请求URL

/api/common/list

#### 请求参数

| 参数名      | 类型    | 必须 | 描述                  |
|------------|---------|-----|----------------------|
| targetTable | String  | 是  | 目标表名              |
| pageIndex   | Integer | 否  | 页码，默认为0          |
| pageSize    | Integer | 否  | 每页记录数，默认为100   |
| [其他字段]   | Any     | 否  | 作为查询条件的字段      |

#### 查询参数格式规则

系统支持多种查询参数格式，可通过参数名后缀指定查询类型：

1. **等值查询** - 无特殊后缀，如 `username=admin`
2. **模糊查询** - 添加 `_like` 后缀，如 `username_like=adm`（等同于 `username LIKE '%adm%'`）
3. **IN查询** - 添加 `_in` 后缀，值用逗号分隔，如 `status_in=1,2`（等同于 `status IN (1,2)`）
4. **范围查询** - 添加 `_range` 或 `_between` 后缀，值格式为 `start~end`，如 `create_time_range=2023-01-01~2023-12-31`

**特殊字段映射说明**：
- `create_time` 字段会自动映射到数据库中的 `created_at` 字段
- `sys_user` 表不支持 `is_deleted` 和 `roles` 字段的查询

#### 响应参数

| 参数名      | 类型     | 描述         |
|------------|----------|-------------|
| code       | Integer  | 状态码       |
| msg        | String   | 状态信息     |
| data       | Object   | 响应数据     |
| data.items | Array    | 数据列表     |
| data.total | Integer  | 总记录数     |

#### 响应示例

```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "items": [
      {
        "id": 1,
        "username": "admin",
        "real_name": "管理员",
        "email": "admin@example.com",
        "status": 1,
        "created_at": "2023-01-01 00:00:00"
      }
    ],
    "total": 1
  }
}
```

### 2.1 POST方式获取数据列表

#### 接口描述

使用POST方法查询指定表的数据列表，支持复杂查询参数。

#### 请求方法

POST

#### 请求URL

/api/common/list

#### 请求参数

| 参数名       | 类型    | 必须 | 描述                 |
|-------------|---------|-----|---------------------|
| targetTable | String  | 是  | 目标表名             |
| pageIndex   | Integer | 否  | 页码，默认为0         |
| pageSize    | Integer | 否  | 每页记录数，默认为10   |
| params      | Object  | 否  | 查询参数对象          |

#### 请求参数格式

POST方式查询支持更复杂的参数格式：

1. **简单值**：直接作为字符串或数字传递，如 `"username": "admin"`
2. **数组值**：对于范围查询，可传递包含两个元素的数组，如 `"create_time_range": ["2023-01-01", "2023-12-31"]`
3. **IN查询**：可传递数组，如 `"status_in": [1, 2, 3]`

#### 请求示例

```json
{
  "targetTable": "sys_user",
  "pageIndex": 0,
  "pageSize": 10,
  "params": {
    "username_like": "adm",
    "status": 1,
    "create_time_range": ["2023-01-01", "2023-12-31"]
  }
}
```

#### 响应格式

与GET方式查询相同。

### 3. 保存数据

#### 接口描述

保存或更新数据。如果数据包含id字段，则执行更新操作；否则执行新增操作。

#### 请求方法

POST

#### 请求URL

/api/common/save

#### 请求参数

| 参数名      | 类型    | 必须 | 描述                  |
|------------|---------|-----|----------------------|
| targetTable | String  | 是  | 目标表名              |
| [请求体]     | Object  | 是  | 要保存的数据对象       |

#### 请求示例

```json
{
  "username": "newuser",
  "password": "password123",
  "real_name": "新用户",
  "email": "new@example.com",
  "status": 1
}
```

#### 响应参数

| 参数名     | 类型     | 描述         |
|-----------|----------|-------------|
| code      | Integer  | 状态码       |
| msg       | String   | 状态信息     |
| data      | Object   | 响应数据     |
| data.id   | Integer  | 记录ID      |

#### 响应示例

```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "id": 2
  }
}
```

### 4. 删除数据

#### 接口描述

根据ID删除指定表中的数据。

#### 请求方法

POST

#### 请求URL

/api/common/delete

#### 请求参数

| 参数名      | 类型    | 必须 | 描述                  |
|------------|---------|-----|----------------------|
| targetTable | String  | 是  | 目标表名              |
| id          | Integer | 是  | 要删除的记录ID        |

#### 响应参数

| 参数名         | 类型     | 描述         |
|--------------|----------|-------------|
| code         | Integer  | 状态码       |
| msg          | String   | 状态信息     |
| data         | Object   | 响应数据     |
| data.affected | Integer  | 受影响的记录数 |

#### 响应示例

```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "affected": 1
  }
}
```

## 错误码

| 错误码 | 描述                |
|-------|-------------------|
| 200   | 成功               |
| 400   | 请求参数错误        |
| 401   | 未授权（无效令牌）   |
| 403   | 权限不足            |
| 500   | 服务器内部错误      |

## 注意事项

1. 所有接口（除健康检查外）都需要JWT令牌认证
2. 保存数据时，系统会自动添加`update_by`和`create_by`字段
3. 查询条件支持后缀匹配，如`name_like`表示模糊匹配，`age_in`表示IN查询
4. 列表查询默认最多返回100条记录，如需更多请使用分页参数 