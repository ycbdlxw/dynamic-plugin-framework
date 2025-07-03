# 数据库设计指南

## 配置表设计

### 1. 表属性配置(table_attribute)
管理表级属性,包括:
- 表名和显示名称
- 排序规则
- 功能模块分类
- 其他表级配置

完整结构:
```sql
CREATE TABLE IF NOT EXISTS `table_attribute` (
  `tableName` varchar(100) COMMENT '数据库中文表名',
  `dbtable` varchar(100) NOT NULL COMMENT '数据库表名',
  `sort` varchar(200) COMMENT '排序内容',
  `functions` varchar(100) COMMENT '功能模块',
  `groupby` varchar(200) COMMENT '分组排序',
  `isLoading` tinyint(1) DEFAULT 0 COMMENT '是否显示进度,0-否,1-是',
  `isAllSelect` tinyint(1) DEFAULT 0 COMMENT '是否全选,0-否,1-是',
  `isRowOpertionFlag` tinyint(1) DEFAULT 0 COMMENT '是否行操作,0-否,1-是',
  `isOpertionFlag` tinyint(1) DEFAULT 0 COMMENT '是否操作,0-否,1-是',
  `tableProcedure` varchar(100) COMMENT '存储过程名',
  `subTables` varchar(200) COMMENT '子表名称',
  `tableKey` varchar(100) COMMENT '主从表关联字段',
  `ParameterType` tinyint(1) COMMENT '参数类型',
  `alias` varchar(100) COMMENT '其它名称',
  `mainKey` varchar(100) COMMENT '表关键字段名称',
  `pageTitle` varchar(100) COMMENT '页面标题',
  `roleFlag` tinyint(1) DEFAULT 0 COMMENT '是否角色控制,0-否,1-是',
  `joinStr` varchar(500) COMMENT '关联',
  `definColumns` varchar(500) COMMENT '自定义字段',
  PRIMARY KEY (`dbtable`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='表属性配置表';
```

### 2. 字段属性配置(column_attribute)
管理字段级属性,包括:
- 字段名和显示名称
- 显示类型
- 验证规则
- 其他字段级配置

完整结构:
```sql
CREATE TABLE IF NOT EXISTS `column_attribute` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `dbTableName` varchar(100) NOT NULL COMMENT '数据库表名',
  `tableName` varchar(100) COMMENT '数据库中文表名',
  `name` varchar(100) NOT NULL COMMENT '字段名称',
  `pagename` varchar(100) COMMENT '名称',
  `IsShowInList` tinyint(1) DEFAULT 0 COMMENT '列表显示,0-否,1-是',
  `searchFlag` tinyint(1) DEFAULT 0 COMMENT '查询,0-否,1-是',
  `editFlag` tinyint(1) DEFAULT 0 COMMENT '编辑,0-否,1-是',
  `options` varchar(50) COMMENT '选择数据,0-字典,1-autoselect,2-本身',
  `showType` varchar(50) COMMENT '控件类型',
  `queryType` varchar(50) COMMENT '匹配方式',
  `checkMode` varchar(50) COMMENT '校验方式',
  `IsRequired` tinyint(1) DEFAULT 0 COMMENT '必填,0-否,1-是',
  `autoSelectId` int(11) COMMENT '下拉框属性',
  `OrderNo` int(11) COMMENT '字段排序',
  `searchOrderNo` int(11) COMMENT '搜索排序',
  `editOrderNo` int(11) COMMENT '编辑排序',
  `defaultValue` varchar(200) COMMENT '默认值',
  `len` int(11) COMMENT '字段长度',
  `fieldType` varchar(50) COMMENT '字段类型',
  `type` int(11) COMMENT '字段类型',
  `classcode` varchar(100) COMMENT '字典代码',
  `Other` varchar(200) COMMENT '其它',
  `isRead` tinyint(1) DEFAULT 0 COMMENT '只读,0-否,1-是',
  `unionTable` varchar(100) COMMENT '外部关联表名',
  `IsPri` tinyint(1) DEFAULT 0 COMMENT '主键,0-否,1-是',
  `IsForeignKey` tinyint(1) DEFAULT 0 COMMENT '其它主键,0-否,1-是',
  `attrType` varchar(50) COMMENT '属性类型',
  `attrName` varchar(100) COMMENT '属性名称',
  `whereSql` varchar(500) COMMENT '自订义条件',
  `templetResId` int(11) COMMENT '模板ID',
  `selectColumns` varchar(500) COMMENT '外联表字段列',
  `isExport` tinyint(1) DEFAULT 0 COMMENT '导出,0-否,1-是',
  `showWidth` int(11) COMMENT '显示宽度',
  `contentLen` int(11) COMMENT '内容长度',
  `editType` varchar(50) COMMENT '编辑属性',
  `roles` varchar(200) COMMENT '角色名称',
  `importRequired` tinyint(1) DEFAULT 0 COMMENT '导出必填',
  `searchWidth` int(11) COMMENT '搜索宽度',
  `listWidth` int(11) COMMENT '列表宽度',
  `isBatch` tinyint(1) DEFAULT 0 COMMENT '是否批量,0-否，1-是',
  `isMobile` tinyint(1) DEFAULT 0 COMMENT '是否手机端',
  PRIMARY KEY (`id`),
  KEY `idx_dbtable_name` (`dbTableName`,`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='字段属性配置表';
```

### 3. 字段校验配置(column_check_property)
管理字段校验规则,包括:
- 唯一性校验
- 外键关联校验
- 范围校验
- 自定义校验规则

完整结构:
```sql
CREATE TABLE IF NOT EXISTS `column_check_property` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `check_table` VARCHAR(50) NOT NULL COMMENT '检查表名',
  `target_table` VARCHAR(50) NOT NULL COMMENT '目标表名',
  `check_column` VARCHAR(50) NOT NULL COMMENT '列名',
  `check_mode` VARCHAR(50) NOT NULL COMMENT '检查模式',
  `check_order` INT NOT NULL DEFAULT 0 COMMENT '校验顺序',
  `status` TINYINT(4) NOT NULL DEFAULT '1' COMMENT '状态,1-有效 0-无效',
  `errorMsg` VARCHAR(255) NOT NULL COMMENT '错误信息',
  `whereStr` VARCHAR(255) DEFAULT NULL COMMENT '附加条件',
  `params` JSON DEFAULT NULL COMMENT '多参数',
  PRIMARY KEY (`id`),
  KEY `idx_check_table` (`check_table`),
  KEY `idx_check_column` (`check_column`),
  KEY `idx_check_order` (`check_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='列检查属性表';
```

## SQL编写规范

### 1. 查询语句标准化
```sql
SELECT [字段列表]
FROM [主表]
[LEFT/RIGHT/INNER] JOIN [关联表] ON [关联条件]
WHERE [筛选条件]
```

### 2. 数据操作规范
- INSERT语句使用参数化查询
- UPDATE语句必须带WHERE条件
- DELETE语句必须带WHERE条件

### 3. 索引设计原则
- 主键索引
- 外键索引
- 查询条件索引
- 组合索引原则

## 校验模式说明

### 1. isNotExit模式
确保值在目标表中不存在,用于唯一性校验:
```sql
INSERT INTO column_check_property 
(check_table, target_table, check_column, check_mode, errorMsg, whereStr)
VALUES 
('sys_user', 'sys_user', 'username', 'isNotExit', '用户名已存在', 'status = 1 and username=%s');
```

### 2. isExit模式
确保值在目标表中存在,用于外键关联校验:
```sql
INSERT INTO column_check_property 
(check_table, target_table, check_column, check_mode, errorMsg, whereStr)
VALUES 
('sys_user', 'org', 'org_id', 'isExit', '所选组织机构不存在', 'status = 1 and id=%s');
```

### 3. isRang模式
确保数值在指定范围内:
```sql
INSERT INTO column_check_property 
(check_table, target_table, check_column, check_mode, errorMsg, params)
VALUES 
('sys_user', 'sys_user', 'age', 'isRang', '年龄必须在18到60之间', '{"min": 18, "max": 60}');
```

### 4. MutiReapeat模式
确保多个字段组合不重复:
```sql
INSERT INTO column_check_property 
(check_table, target_table, check_column, check_mode, errorMsg, whereStr)
VALUES 
('sys_user', 'sys_user', 'username,email', 'MutiReapeat', '用户名和邮箱组合已存在', 'status = 1');
``` 