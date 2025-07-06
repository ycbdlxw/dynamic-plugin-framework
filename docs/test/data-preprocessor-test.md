# 数据预处理机制测试指南

本文档提供了对数据预处理机制（`DataPreprocessorService`）进行测试的详细指南，包括测试环境准备、测试用例设计和执行方法。

## 测试环境准备

### 1. 创建测试表

测试前需要创建专用的测试表，以便进行各种场景的测试：

```sql
CREATE TABLE test_preprocessor (
  id INT AUTO_INCREMENT PRIMARY KEY,
  title VARCHAR(100),
  user_id INT,
  org_id INT,
  status TINYINT,
  content VARCHAR(255),
  created_at TIMESTAMP,
  updated_at TIMESTAMP,
  create_by INT,
  update_by INT
);
```

### 2. 配置测试数据

在`column_attribute`表中配置测试表的字段属性：

```sql
-- 清除可能存在的旧配置
DELETE FROM column_attribute WHERE db_table_name = 'test_preprocessor';

-- 添加新配置
INSERT INTO column_attribute 
(db_table_name, column_name, page_name, edit_flag, is_required, default_value) VALUES 
('test_preprocessor', 'title', '标题', 1, 1, NULL),
('test_preprocessor', 'user_id', '用户ID', 1, 0, 'userId()'),
('test_preprocessor', 'org_id', '组织ID', 1, 0, 'orgId()'),
('test_preprocessor', 'status', '状态', 1, 0, '1'),
('test_preprocessor', 'content', '内容', 1, 0, NULL);

-- 审计字段配置
INSERT INTO column_attribute 
(db_table_name, column_name, page_name, edit_flag) VALUES 
('test_preprocessor', 'create_by', '创建人', 0),
('test_preprocessor', 'update_by', '更新人', 0),
('test_preprocessor', 'created_at', '创建时间', 0),
('test_preprocessor', 'updated_at', '更新时间', 0);
```

### 3. 设置用户上下文

测试前需要设置模拟的用户上下文：

```java
Map<String, Object> userInfo = new HashMap<>();
userInfo.put("userId", 1);
userInfo.put("username", "testuser");
userInfo.put("orgId", 2);
userInfo.put("content", "用户上下文内容");
UserContext.setUser(userInfo);
```

## 测试用例设计

以下是针对`DataPreprocessorService`各项功能的测试用例设计：

### 1. 用户上下文自动填充测试

**测试目标**：验证`edit_flag=1`的字段能否正确从用户上下文中获取值

```java
@Test
public void testUserContextAutoFill() {
    // 准备测试数据
    Map<String, Object> data = new HashMap<>();
    data.put("title", "测试标题");

    // 执行预处理
    dataPreprocessorService.preprocessForSave("test_preprocessor", data);

    // 验证结果
    assertEquals(1, data.get("user_id"));
    assertEquals(2, data.get("org_id"));
    assertEquals("用户上下文内容", data.get("content"));
}
```

### 2. 显式值不被覆盖测试

**测试目标**：验证已有值不会被用户上下文覆盖

```java
@Test
public void testExplicitValueNotOverridden() {
    // 准备测试数据，显式设置user_id
    Map<String, Object> data = new HashMap<>();
    data.put("title", "测试标题");
    data.put("user_id", 99);

    // 执行预处理
    dataPreprocessorService.preprocessForSave("test_preprocessor", data);

    // 验证结果 - user_id不应被覆盖
    assertEquals(99, data.get("user_id"));
    assertEquals(2, data.get("org_id")); // org_id应该自动填充
}
```

### 3. 默认值处理测试

**测试目标**：验证默认值是否正确应用

```java
@Test
public void testDefaultValueProcessing() {
    // 准备测试数据，不设置status
    Map<String, Object> data = new HashMap<>();
    data.put("title", "测试标题");

    // 执行预处理
    dataPreprocessorService.preprocessForSave("test_preprocessor", data);

    // 验证结果 - status应使用默认值
    assertEquals(1, data.get("status"));
}
```

### 4. 必填字段校验测试

**测试目标**：验证必填字段校验是否正常工作

```java
@Test
public void testRequiredFieldValidation() {
    // 准备测试数据，不设置必填字段title
    Map<String, Object> data = new HashMap<>();

    // 执行预处理，应抛出异常
    Exception exception = assertThrows(IllegalArgumentException.class, () -> {
        dataPreprocessorService.preprocessForSave("test_preprocessor", data);
    });

    // 验证异常信息
    assertTrue(exception.getMessage().contains("必填项"));
}
```

### 5. 审计字段处理测试 - 插入操作

**测试目标**：验证插入操作时审计字段是否正确填充

```java
@Test
public void testAuditFieldsForInsert() {
    // 准备测试数据
    Map<String, Object> data = new HashMap<>();
    data.put("title", "测试标题");

    // 执行预处理
    dataPreprocessorService.preprocessForSave("test_preprocessor", data);

    // 验证结果 - 审计字段应自动填充
    assertTrue(data.containsKey("create_by"));
    assertTrue(data.containsKey("update_by"));
    assertTrue(data.containsKey("created_at"));
    assertTrue(data.containsKey("updated_at"));
}
```

### 6. 审计字段处理测试 - 更新操作

**测试目标**：验证更新操作时只有相关审计字段被填充

```java
@Test
public void testAuditFieldsForUpdate() {
    // 准备测试数据
    Map<String, Object> data = new HashMap<>();
    data.put("id", 1);
    data.put("title", "更新的标题");

    // 执行预处理
    dataPreprocessorService.preprocessForUpdate("test_preprocessor", data);

    // 验证结果 - 只有更新相关的审计字段应填充
    assertTrue(data.containsKey("update_by"));
    assertTrue(data.containsKey("updated_at"));
    assertNotNull(data.get("update_by"));
    assertNotNull(data.get("updated_at"));
}
```

### 7. 批量数据处理测试

**测试目标**：验证批量数据处理是否正常工作

```java
@Test
public void testBatchSavePreprocessing() {
    // 准备测试数据
    List<Map<String, Object>> dataList = new ArrayList<>();
    
    Map<String, Object> data1 = new HashMap<>();
    data1.put("title", "批量测试1");
    
    Map<String, Object> data2 = new HashMap<>();
    data2.put("title", "批量测试2");
    data2.put("status", 2); // 显式设置status
    
    dataList.add(data1);
    dataList.add(data2);

    // 执行批量预处理
    dataList = dataPreprocessorService.preprocessBatchSave("test_preprocessor", dataList);

    // 验证结果
    assertEquals(2, dataList.size());
    
    // 第一条记录
    assertEquals(1, dataList.get(0).get("user_id"));
    assertEquals(2, dataList.get(0).get("org_id"));
    assertEquals(1, dataList.get(0).get("status")); // 使用默认值
    
    // 第二条记录
    assertEquals(1, dataList.get(1).get("user_id"));
    assertEquals(2, dataList.get(1).get("org_id"));
    assertEquals(2, dataList.get(1).get("status")); // 保留显式设置的值
}
```

### 8. 动态表达式默认值测试

**测试目标**：验证动态表达式默认值是否正确解析

```java
@Test
public void testDynamicExpressionDefaultValues() {
    // 先删除已有的content字段配置
    jdbcTemplate.update("DELETE FROM column_attribute WHERE db_table_name = 'test_preprocessor' AND column_name = 'content'");
    
    // 配置动态表达式默认值
    jdbcTemplate.update("INSERT INTO column_attribute " +
            "(db_table_name, column_name, page_name, edit_flag, is_required, default_value) VALUES " +
            "('test_preprocessor', 'content', '内容', 0, 0, 'username()')");

    // 准备测试数据
    Map<String, Object> data = new HashMap<>();
    data.put("title", "测试标题");

    // 执行预处理
    dataPreprocessorService.preprocessForSave("test_preprocessor", data);

    // 验证结果 - content应使用username()表达式的值
    assertEquals("testuser", data.get("content"));
}
```

## 测试执行

### 通过JUnit执行测试

创建一个完整的JUnit测试类：

```java
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class DataPreprocessorTest {

    @Autowired
    private DataPreprocessorService dataPreprocessorService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    public void setup() {
        // 创建测试表
        jdbcTemplate.execute("DROP TABLE IF EXISTS test_preprocessor");
        jdbcTemplate.execute("CREATE TABLE test_preprocessor (" +
                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                "title VARCHAR(100), " +
                "user_id INT, " +
                "org_id INT, " +
                "status TINYINT, " +
                "content VARCHAR(255), " +
                "created_at TIMESTAMP, " +
                "updated_at TIMESTAMP, " +
                "create_by INT, " +
                "update_by INT)");

        // 配置column_attribute
        jdbcTemplate.update("DELETE FROM column_attribute WHERE db_table_name = 'test_preprocessor'");
        jdbcTemplate.update("INSERT INTO column_attribute " +
                "(db_table_name, column_name, page_name, edit_flag, is_required, default_value) VALUES " +
                "('test_preprocessor', 'title', '标题', 1, 1, NULL)");
        // ... 其他配置 ...

        // 设置用户上下文
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("userId", 1);
        userInfo.put("username", "testuser");
        userInfo.put("orgId", 2);
        userInfo.put("content", "用户上下文内容");
        UserContext.setUser(userInfo);
    }

    // 测试方法...

    @AfterEach
    public void cleanup() {
        UserContext.clear();
    }
}
```

### 通过命令行执行测试

使用Maven命令执行特定的测试类：

```bash
mvn test -Dtest=DataPreprocessorTest
```

执行特定的测试方法：

```bash
mvn test -Dtest=DataPreprocessorTest#testUserContextAutoFill
```

## 测试结果分析

### 常见问题及解决方案

1. **用户上下文字段未填充**
   - 检查用户上下文是否正确设置
   - 检查字段名是否与用户上下文中的键名完全匹配
   - 确认`edit_flag`设置为1

2. **默认值未应用**
   - 检查`default_value`配置是否正确
   - 确认字段值为null或空字符串

3. **必填校验未触发**
   - 确认`is_required`设置为1
   - 检查是否有默认值或用户上下文值导致字段不为空

4. **审计字段未填充**
   - 确认表中存在标准审计字段
   - 检查用户上下文中是否有必要的用户信息

## 注意事项

1. 测试应在事务中执行，以避免测试数据污染数据库
2. 每次测试前清理和重建测试环境
3. 测试完成后清理用户上下文
4. 确保测试环境与生产环境配置一致

## 总结

通过全面的测试用例，可以验证`DataPreprocessorService`的各项功能是否正常工作。测试应覆盖所有可能的使用场景，包括正常流程、边界条件和异常情况。完善的测试可以确保数据预处理机制在实际应用中的可靠性和稳定性。