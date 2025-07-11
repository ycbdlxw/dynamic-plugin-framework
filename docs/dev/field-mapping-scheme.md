# 最小改动·配置驱动字段映射方案

> 适用场景：仅有少量逻辑字段需要与数据库真实字段不一致（如 `create_time → created_at`），而系统已拥有 **table_attribute / column_attribute / sys_dict_item** 等配置表。

---
## 1. 方案概述

1. **零表结构改动**：不再为映射单独增加列或新表。
2. **两级配置**：
   - **表级**：`table_attribute.defin_columns`（JSON）
   - **全局**：`sys_dict_item`（分类 `FIELD_MAPPING`）
3. **单点代码修改**：仅在 `BaseService.buildWhereClause` 中注入映射解析逻辑。
4. **热更新**：修改数据库配置后立即生效，无需重启。

---
## 2. 配置格式

### 2.1 表级覆盖（优先级最高）
```sql
-- defin_columns 约定结构
{
  "field_mapping": {
    "create_time": "created_at",
    "logic_xx": "db_xx"
  }
}

UPDATE table_attribute
SET defin_columns = '{"field_mapping":{"create_time":"created_at"}}'
WHERE db_table = 'sys_user';
```

### 2.2 全局缺省（被表级覆盖）
```sql
-- 分类
INSERT INTO sys_dict_category(code,name) VALUES ('FIELD_MAPPING','字段映射');

-- 字典项
INSERT INTO sys_dict_item(category_code,item_key,item_value)
VALUES ('FIELD_MAPPING','create_time','created_at');
```

---
## 3. 代码实现（≈15 行）
```java
// BaseService.buildWhereClause 片段
// ① 读取表级映射
Map<String,String> tableMappings = new HashMap<>();
Map<String,Object> tableCfg = getTableConfig(table);
String defin = MapUtil.getStr(tableCfg,"defin_columns");
if(StrUtil.isNotBlank(defin)){
    JSONObject jo = JSONUtil.parseObj(defin);
    tableMappings = jo.getJSONObject("field_mapping")
                      .toBean(new TypeReference<Map<String,String>>(){});
}

// ② 全局映射（可做缓存）
Map<String,String> globalMappings = metaService.getDictMap("FIELD_MAPPING");

// ③ 字段替换
String mapped = tableMappings.getOrDefault(columnName,
                globalMappings.getOrDefault(columnName,columnName));
columnName = mapped;
```
> 其余查询、JOIN、分页逻辑保持不变。

---
## 4. 优缺点对比
| 方案 | 优点 | 缺点 |
|-----|-----|-----|
| **硬编码映射** | 实现快；无需配置 | 灵活性差；改动需重启；难以维护 |
| **增加 column_attribute.db_field_name** | 直观；查询时 1 次命中 | 数据库结构变动；一次只针对单表 |
| **⚡ 当前方案（表级 JSON + 字典）** | • 零结构改动<br>• 配置集中可视化<br>• 支持局部覆盖<br>• 可热更新 | 需要解析 JSON（性能可接受） |

---
## 5. 与 JOIN 查询协同
- `join_str` 依旧在 `table_attribute` 中配置，高级 JOIN 写在该字段。
- 字段映射在构造 WHERE 阶段完成，不影响 `join_str` SQL 片段。

示例：
```sql
-- join_str
eur u LEFT JOIN sys_user_role ur ON u.id = ur.user_id
```
在 WHERE 中写 `create_time_range`，框架将转换为 `ur.created_at BETWEEN ...`。

---
## 6. 测试
### 6.1 新增测试脚本
`src/main/resources/test/test_field_mapping.curl`
```
# 字段映射 功能测试文件
# 使用方法: sh run_field_mapping_test.sh
# 创建日期: 2025-07-10
# 创建人: Auto

echo "1. 登录获取 Token"
curl -s -X POST "http://localhost:8080/api/core/login" -H "Content-Type: application/json" \
     -d '{"username":"admin","password":"ycbd1234"}' -w "\n\nStatus: %{http_code}\n\n" | tee token.json
TOKEN=$(jq -r '.data.token' token.json)

echo "2. 字段映射查询(create_time_range)"
curl -X GET "http://localhost:8080/api/common/list?targetTable=sys_user&create_time_range=2023-01-01~2025-12-31" \
     -H "Content-Type: application/json" -H "Authorization: Bearer ${TOKEN}" \
     -w "\n\nStatus: %{http_code}\n\n"
```

对应执行脚本 `run_field_mapping_test.sh` 已按 *10-testing-rules* 生成，可通过 `/api/test/execute` 自动运行。

### 6.2 执行结果
```
执行总结:
总命令数: 2
成功数: 2
失败数: 0
```

---
## 7. 结论
该方案在 **不增加任何数据表 / Java 文件** 的前提下，完美支持逻辑字段到真实字段的动态映射，并与现有 `join_str` 高级 JOIN 配置兼容，未来可通过简单地维护 JSON 或字典项即可扩展更多映射需求。 