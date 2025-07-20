-- 关闭H2的兼容模式，使用标准SQL
SET MODE MySQL;

-- 用户表
DROP TABLE IF EXISTS sys_user;
CREATE TABLE sys_user (
  id INT AUTO_INCREMENT PRIMARY KEY,
  username VARCHAR(50) NOT NULL UNIQUE,
  password VARCHAR(100) NOT NULL,
  real_name VARCHAR(50),
  email VARCHAR(100),
  status TINYINT DEFAULT 1,
  role_id INT NOT NULL,
  org_id INT NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 角色表
DROP TABLE IF EXISTS sys_role;
CREATE TABLE sys_role (
  id INT AUTO_INCREMENT PRIMARY KEY,
  role_name VARCHAR(50) NOT NULL,
  role_code VARCHAR(50) NOT NULL UNIQUE,
  description VARCHAR(200),
  status TINYINT DEFAULT 1,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 用户角色关联表
DROP TABLE IF EXISTS sys_user_role;
CREATE TABLE sys_user_role (
  id INT AUTO_INCREMENT PRIMARY KEY,
  user_id INT NOT NULL,
  role_id INT NOT NULL,
  UNIQUE KEY uk_user_role (user_id, role_id),
  FOREIGN KEY (user_id) REFERENCES sys_user(id),
  FOREIGN KEY (role_id) REFERENCES sys_role(id)
);

-- 组织机构表
DROP TABLE IF EXISTS sys_org;
CREATE TABLE sys_org (
  id INT AUTO_INCREMENT PRIMARY KEY,
  org_name VARCHAR(100) NOT NULL,
  org_code VARCHAR(50) NOT NULL UNIQUE,
  parent_id INT,
  status TINYINT DEFAULT 1,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ========= 元数据表 =========
DROP TABLE IF EXISTS table_attribute;
CREATE TABLE table_attribute (
  id INT AUTO_INCREMENT PRIMARY KEY,
  table_name VARCHAR(100) COMMENT '数据库中文表名',
  db_table   VARCHAR(100) NOT NULL COMMENT '数据库表名',
  sort      VARCHAR(200) COMMENT '排序内容',
  module VARCHAR(100) COMMENT '功能模块',
  group_by VARCHAR(200) COMMENT '分组排序',
  is_loading TINYINT DEFAULT 0 COMMENT '是否显示进度',
  is_all_select TINYINT DEFAULT 0 COMMENT '是否全选',
  is_row_operation_flag TINYINT DEFAULT 0 COMMENT '是否行操作',
  is_operation_flag TINYINT DEFAULT 0 COMMENT '是否操作按钮',
  table_procedure VARCHAR(100) COMMENT '存储过程名',
  sub_tables VARCHAR(200) COMMENT '子表名称',
  table_key VARCHAR(100) COMMENT '主从表关联字段',
  parameter_type TINYINT COMMENT '参数类型',
  alias_name VARCHAR(100) COMMENT '其它名称',
  main_key VARCHAR(100) COMMENT '表关键字段名称',
  page_title VARCHAR(100) COMMENT '页面标题',
  role_flag TINYINT DEFAULT 0 COMMENT '是否角色控制',
  join_str VARCHAR(500) COMMENT '默认 JOIN',
  defin_columns VARCHAR(500) COMMENT '自定义字段',
  UNIQUE KEY idx_db_table (db_table)
);

-- 字段属性配置
DROP TABLE IF EXISTS column_attribute;
CREATE TABLE column_attribute (
  id INT AUTO_INCREMENT PRIMARY KEY,
  db_table_name VARCHAR(100) NOT NULL COMMENT '数据库表名',
  table_name  VARCHAR(100) COMMENT '中文表名',
  column_name VARCHAR(100) NOT NULL COMMENT '字段名称',
  page_name VARCHAR(100) COMMENT '显示名称',
  is_show_in_list TINYINT DEFAULT 0 COMMENT '列表显示',
  search_flag TINYINT DEFAULT 0 COMMENT '可搜索',
  edit_flag TINYINT DEFAULT 1 COMMENT '可编辑',
  option_source VARCHAR(50) COMMENT '选择数据来源',
  show_type VARCHAR(50) COMMENT '控件类型',
  query_type VARCHAR(50) COMMENT '匹配方式',
  check_mode VARCHAR(50) COMMENT '校验方式',
  is_required TINYINT DEFAULT 0 COMMENT '必填',
  auto_select_id INT COMMENT '下拉关联',
  order_no INT COMMENT '字段排序',
  search_order_no INT COMMENT '搜索排序',
  edit_order_no INT COMMENT '编辑排序',
  default_value VARCHAR(200) COMMENT '默认值',
  len INT COMMENT '长度',
  field_type VARCHAR(50) COMMENT '字段类型',
  field_num_type INT COMMENT '字段类型数值',
  class_code VARCHAR(100) COMMENT '字典代码',
  other_info VARCHAR(200) COMMENT '其它',
  is_read TINYINT DEFAULT 0 COMMENT '只读',
  union_table VARCHAR(100) COMMENT '外联表',
  is_pri TINYINT DEFAULT 0 COMMENT '主键',
  is_foreign_key TINYINT DEFAULT 0 COMMENT '外键',
  attr_type VARCHAR(50) COMMENT '属性类型',
  attr_name VARCHAR(100) COMMENT '属性名称',
  where_sql VARCHAR(500) COMMENT '自定义条件',
  templet_res_id INT COMMENT '模板ID',
  select_columns VARCHAR(500) COMMENT '外联字段',
  is_export TINYINT DEFAULT 0 COMMENT '可导出',
  show_width INT COMMENT '显示宽度',
  content_len INT COMMENT '内容长度',
  edit_type VARCHAR(50) COMMENT '编辑属性',
  role_codes VARCHAR(200) COMMENT '角色',
  import_required TINYINT DEFAULT 0 COMMENT '导入必填',
  search_width INT COMMENT '搜索宽度',
  list_width INT COMMENT '列表宽度',
  is_batch TINYINT DEFAULT 0 COMMENT '支持批量',
  is_mobile TINYINT DEFAULT 0 COMMENT '移动端显示',
  KEY idx_db_table_name (db_table_name, column_name)
);

-- 安全配置表
DROP TABLE IF EXISTS security_config;
CREATE TABLE security_config (
  id INT AUTO_INCREMENT PRIMARY KEY,
  type VARCHAR(20) NOT NULL,
  pattern VARCHAR(255) NOT NULL,
  is_active TINYINT(1) DEFAULT 1 COMMENT '是否激活，1=是，0=否'
);

-- 插件配置表
DROP TABLE IF EXISTS plugin_config;
CREATE TABLE plugin_config (
  id INT AUTO_INCREMENT PRIMARY KEY,
  plugin_name VARCHAR(100) NOT NULL UNIQUE,
  class_name VARCHAR(255) NOT NULL,
  description VARCHAR(255),
  is_active TINYINT(1) DEFAULT 1 COMMENT '是否激活，1=是，0=否'
);

-- AI 处理器配置表
DROP TABLE IF EXISTS ai_processor_config;
CREATE TABLE ai_processor_config (
  id INT AUTO_INCREMENT PRIMARY KEY,
  processor_name VARCHAR(100) NOT NULL UNIQUE,
  class_name VARCHAR(255) NOT NULL,
  description VARCHAR(255),
  is_active TINYINT(1) DEFAULT 1 COMMENT '是否激活，1=是，0=否'
);

-- 列校验规则表
DROP TABLE IF EXISTS column_check_property;
CREATE TABLE column_check_property (
  id INT AUTO_INCREMENT PRIMARY KEY,
  check_table VARCHAR(100) NOT NULL,
  target_table VARCHAR(100) NOT NULL,
  check_column VARCHAR(255) NOT NULL,
  check_mode VARCHAR(50) NOT NULL,
  check_order INT DEFAULT 1,
  status INT DEFAULT 1,
  errorMsg VARCHAR(255)
);

-- 系统字典分类表
DROP TABLE IF EXISTS sys_dict_category;
CREATE TABLE sys_dict_category (
  code VARCHAR(50) PRIMARY KEY COMMENT '分类编码',
  name VARCHAR(100) NOT NULL COMMENT '分类名称'
);

-- 系统字典项表
DROP TABLE IF EXISTS sys_dict_item;
CREATE TABLE sys_dict_item (
  id INT AUTO_INCREMENT PRIMARY KEY,
  category_code VARCHAR(50) NOT NULL COMMENT '分类编码',
  item_key VARCHAR(50) NOT NULL COMMENT '字典项键',
  item_value VARCHAR(100) NOT NULL COMMENT '字典项值',
  sort_no INT DEFAULT 0 COMMENT '排序号',
  FOREIGN KEY (category_code) REFERENCES sys_dict_category(code)
);

-- 测试预处理器的表
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

-- 系统日志表
DROP TABLE IF EXISTS system_log;
CREATE TABLE system_log (
  id INT AUTO_INCREMENT PRIMARY KEY,
  user_id INT COMMENT '用户ID',
  username VARCHAR(50) COMMENT '用户名',
  method VARCHAR(10) COMMENT '请求方法',
  request_uri VARCHAR(255) COMMENT '请求URI',
  params TEXT COMMENT '请求参数',
  client_ip VARCHAR(50) COMMENT '客户端IP',
  status INT COMMENT '状态码',
  response_body TEXT COMMENT '响应内容',
  duration_ms BIGINT COMMENT '执行时间(毫秒)',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
);

