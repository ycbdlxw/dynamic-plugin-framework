# 自动过滤填充功能测试指南

本文档提供了测试系统自动过滤填充功能的方法和步骤。

## 测试目的

验证系统能够正确实现以下功能：
1. 只有`search_flag=1`的字段才能用于过滤条件
2. 自动从用户上下文中获取`search_flag=1`字段的值并填充到查询条件中

## 前置条件

1. 已配置测试数据表和相关字段
2. 已在`column_attribute`表中设置了`search_flag`标记
3. 确保测试用户具有相应的上下文信息

## 测试步骤

### 1. 基础功能测试

#### 测试用例1：自动填充用户ID

**准备工作**：
```sql
-- 确保测试表中有user_id字段
CREATE TABLE IF NOT EXISTS test_auto_filter (
  id INT AUTO_INCREMENT PRIMARY KEY,
  user_id INT,
  content VARCHAR(255)
);

-- 插入测试数据
INSERT INTO test_auto_filter (user_id, content) VALUES 
(1, '用户1的数据'),
(2, '用户2的数据'),
(1, '用户1的另一条数据');

-- 配置column_attribute
INSERT INTO column_attribute 
(db_table_name, column_name, page_name, search_flag, query_type) 
VALUES 
('test_auto_filter', 'user_id', '用户ID', 1, 'eq');
```

**测试步骤**：
1. 使用用户ID为1的账号登录系统
2. 调用通用查询API，不传入user_id参数：
```bash
curl -X GET "http://localhost:8080/api/common/list?tableName=test_auto_filter" \
  -H "Authorization: Bearer {token}"
```

**预期结果**：
- 返回的数据只包含user_id=1的记录
- 共返回2条数据

#### 测试用例2：显式参数优先级

**测试步骤**：
1. 使用用户ID为1的账号登录系统
2. 调用通用查询API，显式传入user_id=2参数：
```bash
curl -X GET "http://localhost:8080/api/common/list?tableName=test_auto_filter&user_id=2" \
  -H "Authorization: Bearer {token}"
```

**预期结果**：
- 返回的数据只包含user_id=2的记录
- 共返回1条数据
- 显式传入的参数优先于自动填充

### 2. 高级功能测试

#### 测试用例3：不同查询类型

**准备工作**：
```sql
-- 配置like查询类型
INSERT INTO column_attribute 
(db_table_name, column_name, page_name, search_flag, query_type) 
VALUES 
('test_auto_filter', 'content', '内容', 1, 'like');

-- 更新用户上下文中的content字段（模拟实现）
-- 实际测试中，需要确保UserContext中有content字段，值为"用户"
```

**测试步骤**：
1. 确保用户上下文中有content="用户"的字段
2. 调用通用查询API，不传入content参数：
```bash
curl -X GET "http://localhost:8080/api/common/list?tableName=test_auto_filter" \
  -H "Authorization: Bearer {token}"
```

**预期结果**：
- 返回的数据包含content字段包含"用户"的所有记录
- 查询条件应为`content LIKE '%用户%'`

### 3. search_flag限制测试

#### 测试用例4：非search_flag字段被忽略

**准备工作**：
```sql
-- 添加一个新字段但不设置search_flag
ALTER TABLE test_auto_filter ADD COLUMN status INT DEFAULT 1;

-- 更新数据
UPDATE test_auto_filter SET status = 0 WHERE id = 1;
UPDATE test_auto_filter SET status = 1 WHERE id > 1;

-- 配置column_attribute，故意设置search_flag=0
INSERT INTO column_attribute 
(db_table_name, column_name, page_name, search_flag, query_type) 
VALUES 
('test_auto_filter', 'status', '状态', 0, 'eq');
```

**测试步骤**：
1. 调用通用查询API，显式传入status=0参数：
```bash
curl -X GET "http://localhost:8080/api/common/list?tableName=test_auto_filter&status=0" \
  -H "Authorization: Bearer {token}"
```

**预期结果**：
- 返回所有记录，不仅仅是status=0的记录
- status参数应被完全忽略，因为其search_flag=0
- 系统日志应显示status参数未被纳入过滤条件

#### 测试用例5：search_flag与自动填充组合测试

**准备工作**：
```sql
-- 确保配置了多个search_flag=1的字段
INSERT INTO column_attribute 
(db_table_name, column_name, page_name, search_flag, query_type) 
VALUES 
('test_auto_filter', 'created_by', '创建人', 1, 'eq');

-- 添加字段并更新数据
ALTER TABLE test_auto_filter ADD COLUMN created_by INT;
UPDATE test_auto_filter SET created_by = 1;
```

**测试步骤**：
1. 使用用户ID为1的账号登录系统，确保用户上下文中有user_id=1和created_by=1
2. 调用通用查询API，传入status=0参数（search_flag=0的字段）：
```bash
curl -X GET "http://localhost:8080/api/common/list?tableName=test_auto_filter&status=0" \
  -H "Authorization: Bearer {token}"
```

**预期结果**：
- status参数被忽略（因为search_flag=0）
- user_id和created_by从用户上下文自动填充（因为search_flag=1）
- 最终查询条件应为`user_id=1 AND created_by=1`
- 返回的数据应符合这两个条件

### 4. 边界条件测试

#### 测试用例6：用户上下文中不存在的字段

**准备工作**：
```sql
-- 配置一个在用户上下文中不存在的字段
INSERT INTO column_attribute 
(db_table_name, column_name, page_name, search_flag, query_type) 
VALUES 
('test_auto_filter', 'non_existent_field', '不存在的字段', 1, 'eq');
```

**测试步骤**：
1. 调用通用查询API：
```bash
curl -X GET "http://localhost:8080/api/common/list?tableName=test_auto_filter" \
  -H "Authorization: Bearer {token}"
```

**预期结果**：
- 系统不应添加不存在的字段到查询条件中
- 查询应正常执行，不受不存在字段的影响

## 测试验证方法

1. 检查返回的数据条数和内容是否符合预期
2. 查看系统日志，确认查询条件是否正确构建
3. 可以通过开启SQL日志，查看实际执行的SQL语句
4. 验证search_flag=0的字段确实不会被用于过滤条件

## 故障排查

如果测试失败，请检查：

1. `column_attribute`表中的配置是否正确
2. 用户上下文中是否包含所需的字段
3. 字段名称是否完全匹配（大小写敏感）
4. 数据库中是否有符合条件的数据
5. 检查系统日志中的SQL语句，确认过滤条件是否正确构建

## 测试脚本

可以使用以下bash脚本自动执行测试：

```bash
#!/bin/bash

# 获取测试用户token
TOKEN=$(curl -s -X POST "http://localhost:8080/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","password":"password"}' | jq -r '.token')

# 测试用例1：自动填充
RESULT1=$(curl -s -X GET "http://localhost:8080/api/common/list?tableName=test_auto_filter" \
  -H "Authorization: Bearer $TOKEN")

# 测试用例2：显式参数优先
RESULT2=$(curl -s -X GET "http://localhost:8080/api/common/list?tableName=test_auto_filter&user_id=2" \
  -H "Authorization: Bearer $TOKEN")

# 测试用例4：非search_flag字段被忽略
RESULT4=$(curl -s -X GET "http://localhost:8080/api/common/list?tableName=test_auto_filter&status=0" \
  -H "Authorization: Bearer $TOKEN")

# 输出结果
echo "测试用例1结果（自动填充）："
echo $RESULT1 | jq
echo "测试用例2结果（显式参数优先）："
echo $RESULT2 | jq
echo "测试用例4结果（非search_flag字段被忽略）："
echo $RESULT4 | jq
``` 