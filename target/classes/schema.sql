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
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 表属性配置
DROP TABLE IF EXISTS table_attribute;
CREATE TABLE table_attribute (
  tableName VARCHAR(100),
  dbtable VARCHAR(100) NOT NULL PRIMARY KEY,
  sort VARCHAR(200),
  mainKey VARCHAR(100) NOT NULL
);

-- 字段属性配置
DROP TABLE IF EXISTS column_attribute;
CREATE TABLE column_attribute (
  id INT AUTO_INCREMENT PRIMARY KEY,
  dbTableName VARCHAR(100) NOT NULL,
  name VARCHAR(100) NOT NULL,
  pagename VARCHAR(100),
  IsShowInList BOOLEAN DEFAULT TRUE,
  searchFlag BOOLEAN DEFAULT FALSE,
  editFlag BOOLEAN DEFAULT TRUE,
  IsRequired BOOLEAN DEFAULT FALSE,
  OrderNo INT,
  KEY idx_dbtable_name (dbTableName, name)
);

-- 安全配置表
DROP TABLE IF EXISTS security_config;
CREATE TABLE security_config (
  id INT AUTO_INCREMENT PRIMARY KEY,
  type VARCHAR(20) NOT NULL,
  pattern VARCHAR(255) NOT NULL,
  is_active BOOLEAN DEFAULT TRUE
);

-- 插件配置表
DROP TABLE IF EXISTS plugin_config;
CREATE TABLE plugin_config (
  id INT AUTO_INCREMENT PRIMARY KEY,
  plugin_name VARCHAR(100) NOT NULL UNIQUE,
  class_name VARCHAR(255) NOT NULL,
  description VARCHAR(255),
  is_active BOOLEAN DEFAULT TRUE
);

-- AI 处理器配置表
DROP TABLE IF EXISTS ai_processor_config;
CREATE TABLE ai_processor_config (
  id INT AUTO_INCREMENT PRIMARY KEY,
  processor_name VARCHAR(100) NOT NULL UNIQUE,
  class_name VARCHAR(255) NOT NULL,
  description VARCHAR(255),
  is_active BOOLEAN DEFAULT TRUE
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