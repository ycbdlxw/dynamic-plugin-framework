-- 添加初始用户（密码：ycbd1234）
INSERT INTO sys_user (username, password, real_name, email, status) VALUES
('admin', '$2a$10$82G8gQiqZ1sOhmi1z.v0aOwVzP1A02GfUtcUCTObwKMEQYwn/kT/G', '管理员', 'admin@example.com', 1);

-- 初始化角色数据
INSERT INTO sys_role (role_name, role_code, description, status) VALUES
('管理员', 'ADMIN', '系统管理员，拥有所有权限', 1),
('普通用户', 'USER', '普通用户，拥有基本权限', 1),
('访客', 'GUEST', '访客，仅有查看权限', 1);

-- 为管理员用户分配角色
INSERT INTO sys_user_role (user_id, role_id) VALUES
(1, 1);  -- admin用户关联管理员角色

-- 初始化组织机构数据
INSERT INTO sys_org (org_name, org_code, parent_id, status) VALUES
('总公司', 'HQ', NULL, 1),
('技术部', 'TECH', 1, 1),
('市场部', 'MARKET', 1, 1),
('财务部', 'FINANCE', 1, 1);

-- 系统字典
INSERT INTO sys_dict_category (code, name) VALUES ('boolean_flag', '布尔标识');
INSERT INTO sys_dict_category (code, name) VALUES ('status_flag', '启用状态');

INSERT INTO sys_dict_item (category_code, item_key, item_value, sort_no) VALUES
('boolean_flag', '0', '否', 0),
('boolean_flag', '1', '是', 1),
('status_flag', '0', '禁用', 0),
('status_flag', '1', '启用', 1);

-- 安全白名单配置
INSERT INTO security_config (type, pattern, is_active) VALUES
('WHITELIST', '/api/core/register', TRUE),
('WHITELIST', '/api/core/login', TRUE),
('WHITELIST', '/h2-console/**', TRUE),
('WHITELIST', '/swagger-ui.html', TRUE),
('WHITELIST', '/swagger-ui/**', TRUE),
('WHITELIST', '/api/test/execute', TRUE),
('WHITELIST', '/api-docs/**', TRUE);

-- 插件配置表初始化
INSERT INTO plugin_config (plugin_name, class_name, description, is_active) VALUES
  ('TestService', 'com.ycbd.demo.plugin.TestServicePlugin', '测试服务插件', 1),
  ('CommandExecutor', 'com.ycbd.demo.plugin.commandexecutor.CommandExecutorPlugin', '命令执行插件', 1);

-- =====================================================================
-- 表属性配置 (table_attribute)
-- =====================================================================

-- sys_user 表属性配置
INSERT INTO table_attribute (db_table, table_name, main_key, sort, module, group_by, is_loading, is_all_select, is_row_operation_flag, is_operation_flag) VALUES
('sys_user', '系统用户', 'id', 'created_at DESC', 'system', NULL, 0, 0, 1, 1);

-- sys_role 表属性配置
INSERT INTO table_attribute (db_table, table_name, main_key, sort, module, group_by, is_loading, is_all_select, is_row_operation_flag, is_operation_flag) VALUES
('sys_role', '系统角色', 'id', 'created_at DESC', 'system', NULL, 0, 0, 1, 1);

-- sys_user_role 表属性配置
INSERT INTO table_attribute (db_table, table_name, main_key, sort, module, group_by, is_loading, is_all_select, is_row_operation_flag, is_operation_flag) VALUES
('sys_user_role', '用户角色关联', 'id', 'id DESC', 'system', NULL, 0, 0, 1, 1);

-- sys_org 表属性配置
INSERT INTO table_attribute (db_table, table_name, main_key, sort, module, group_by, is_loading, is_all_select, is_row_operation_flag, is_operation_flag) VALUES
('sys_org', '组织机构', 'id', 'created_at DESC', 'system', NULL, 0, 0, 1, 1);

-- table_attribute 表属性配置
INSERT INTO table_attribute (db_table, table_name, main_key, sort, module, group_by, is_loading, is_all_select, is_row_operation_flag, is_operation_flag) VALUES
('table_attribute', '表属性配置', 'id', 'id ASC', 'system', NULL, 0, 0, 1, 1);

-- column_attribute 表属性配置
INSERT INTO table_attribute (db_table, table_name, main_key, sort, module, group_by, is_loading, is_all_select, is_row_operation_flag, is_operation_flag) VALUES
('column_attribute', '字段属性配置', 'id', 'id ASC', 'system', NULL, 0, 0, 1, 1);

-- security_config 表属性配置
INSERT INTO table_attribute (db_table, table_name, main_key, sort, module, group_by, is_loading, is_all_select, is_row_operation_flag, is_operation_flag) VALUES
('security_config', '安全配置', 'id', 'id ASC', 'system', NULL, 0, 0, 1, 1);

-- plugin_config 表属性配置
INSERT INTO table_attribute (db_table, table_name, main_key, sort, module, group_by, is_loading, is_all_select, is_row_operation_flag, is_operation_flag) VALUES
('plugin_config', '插件配置', 'id', 'id ASC', 'system', NULL, 0, 0, 1, 1);

-- ai_processor_config 表属性配置
INSERT INTO table_attribute (db_table, table_name, main_key, sort, module, group_by, is_loading, is_all_select, is_row_operation_flag, is_operation_flag) VALUES
('ai_processor_config', 'AI处理器配置', 'id', 'id ASC', 'system', NULL, 0, 0, 1, 1);

-- column_check_property 表属性配置
INSERT INTO table_attribute (db_table, table_name, main_key, sort, module, group_by, is_loading, is_all_select, is_row_operation_flag, is_operation_flag) VALUES
('column_check_property', '列校验规则', 'id', 'id ASC', 'system', NULL, 0, 0, 1, 1);

-- sys_dict_category 表属性配置
INSERT INTO table_attribute (db_table, table_name, main_key, sort, module, group_by, is_loading, is_all_select, is_row_operation_flag, is_operation_flag) VALUES
('sys_dict_category', '字典分类', 'code', 'code ASC', 'system', NULL, 0, 0, 1, 1);

-- sys_dict_item 表属性配置
INSERT INTO table_attribute (db_table, table_name, main_key, sort, module, group_by, is_loading, is_all_select, is_row_operation_flag, is_operation_flag) VALUES
('sys_dict_item', '字典项', 'id', 'sort_no ASC', 'system', NULL, 0, 0, 1, 1);

-- test_preprocessor 表属性配置
INSERT INTO table_attribute (db_table, table_name, main_key, sort, module, group_by, is_loading, is_all_select, is_row_operation_flag, is_operation_flag) VALUES
('test_preprocessor', '测试预处理器', 'id', 'id ASC', 'test', NULL, 0, 0, 1, 1);

-- system_log 表属性配置
INSERT INTO table_attribute (db_table, table_name, main_key, sort, module, group_by, is_loading, is_all_select, is_row_operation_flag, is_operation_flag) VALUES
('system_log', '系统日志', 'id', 'created_at DESC', 'system', NULL, 0, 0, 0, 0);

-- =====================================================================
-- 字段属性配置 (column_attribute)
-- =====================================================================

-- sys_user 表字段属性配置
INSERT INTO column_attribute (db_table_name, column_name, page_name, is_show_in_list, search_flag, edit_flag, is_required, order_no, field_type, field_num_type, other_info) VALUES
('sys_user', 'id', 'ID', TRUE, FALSE, FALSE, FALSE, 1, 'int', 2, '{"isTokenField":true,"tokenName":"userId"}'),
('sys_user', 'username', '用户名', TRUE, TRUE, TRUE, TRUE, 2, 'varchar', 1, '{"isTokenField":true}'),
('sys_user', 'password', '密码', FALSE, FALSE, TRUE, TRUE, 3, 'varchar', 1, NULL),
('sys_user', 'real_name', '真实姓名', TRUE, TRUE, TRUE, FALSE, 4, 'varchar', 1, '{"isTokenField":true,"tokenName":"real_name"}'),
('sys_user', 'email', '邮箱', TRUE, TRUE, TRUE, FALSE, 5, 'varchar', 1, NULL),
('sys_user', 'status', '状态', TRUE, TRUE, TRUE, FALSE, 6, 'tinyint', 3, '{"class_code":"status_flag"}'),
('sys_user', 'created_at', '创建时间', TRUE, TRUE, FALSE, FALSE, 7, 'timestamp', 4, NULL);

-- sys_role 表字段属性配置
INSERT INTO column_attribute (db_table_name, column_name, page_name, is_show_in_list, search_flag, edit_flag, is_required, order_no, field_type, field_num_type) VALUES
('sys_role', 'id', 'ID', TRUE, FALSE, FALSE, FALSE, 1, 'int', 2),
('sys_role', 'role_name', '角色名称', TRUE, TRUE, TRUE, TRUE, 2, 'varchar', 1),
('sys_role', 'role_code', '角色编码', TRUE, TRUE, TRUE, TRUE, 3, 'varchar', 1),
('sys_role', 'description', '描述', TRUE, FALSE, TRUE, FALSE, 4, 'varchar', 1),
('sys_role', 'status', '状态', TRUE, TRUE, TRUE, FALSE, 5, 'tinyint', 3),
('sys_role', 'created_at', '创建时间', TRUE, TRUE, FALSE, FALSE, 6, 'timestamp', 4);

-- sys_user_role 表字段属性配置
INSERT INTO column_attribute (db_table_name, column_name, page_name, is_show_in_list, search_flag, edit_flag, is_required, order_no, field_type, field_num_type) VALUES
('sys_user_role', 'id', 'ID', TRUE, FALSE, FALSE, FALSE, 1, 'int', 2),
('sys_user_role', 'user_id', '用户ID', TRUE, TRUE, TRUE, TRUE, 2, 'int', 2),
('sys_user_role', 'role_id', '角色ID', TRUE, TRUE, TRUE, TRUE, 3, 'int', 2);

-- sys_org 表字段属性配置
INSERT INTO column_attribute (db_table_name, column_name, page_name, is_show_in_list, search_flag, edit_flag, is_required, order_no, field_type, field_num_type, other_info) VALUES
('sys_org', 'id', 'ID', TRUE, FALSE, FALSE, FALSE, 1, 'int', 2, '{"isTokenField":true,"tokenName":"orgId"}'),
('sys_org', 'org_name', '机构名称', TRUE, TRUE, TRUE, TRUE, 2, 'varchar', 1, '{"isTokenField":true,"tokenName":"orgName"}'),
('sys_org', 'org_code', '机构编码', TRUE, TRUE, TRUE, TRUE, 3, 'varchar', 1, NULL),
('sys_org', 'parent_id', '上级机构', TRUE, TRUE, TRUE, FALSE, 4, 'int', 2, NULL),
('sys_org', 'status', '状态', TRUE, TRUE, TRUE, FALSE, 5, 'tinyint', 3, '{"class_code":"status_flag"}'),
('sys_org', 'created_at', '创建时间', TRUE, TRUE, FALSE, FALSE, 6, 'timestamp', 4, NULL);

-- table_attribute 表字段属性配置
INSERT INTO column_attribute (db_table_name, column_name, page_name, is_show_in_list, search_flag, edit_flag, is_required, order_no, field_type, field_num_type) VALUES
('table_attribute', 'id', 'ID', FALSE, FALSE, FALSE, FALSE, 1, 'int', 2),
('table_attribute', 'db_table', '数据库表名', TRUE, TRUE, TRUE, TRUE, 2, 'varchar', 1),
('table_attribute', 'table_name', '中文表名', TRUE, TRUE, TRUE, TRUE, 3, 'varchar', 1),
('table_attribute', 'main_key', '主键', TRUE, FALSE, TRUE, TRUE, 4, 'varchar', 1),
('table_attribute', 'sort', '排序', TRUE, FALSE, TRUE, FALSE, 5, 'varchar', 1),
('table_attribute', 'module', '模块', TRUE, TRUE, TRUE, FALSE, 6, 'varchar', 1),
('table_attribute', 'is_loading', '显示进度', TRUE, TRUE, TRUE, FALSE, 7, 'tinyint', 3),
('table_attribute', 'is_all_select', '全选', TRUE, TRUE, TRUE, FALSE, 8, 'tinyint', 3),
('table_attribute', 'is_row_operation_flag', '行操作', TRUE, TRUE, TRUE, FALSE, 9, 'tinyint', 3),
('table_attribute', 'is_operation_flag', '操作按钮', TRUE, TRUE, TRUE, FALSE, 10, 'tinyint', 3);

-- column_attribute 表字段属性配置
INSERT INTO column_attribute (db_table_name, column_name, page_name, is_show_in_list, search_flag, edit_flag, is_required, order_no, field_type, field_num_type) VALUES
('column_attribute', 'id', 'ID', FALSE, FALSE, FALSE, FALSE, 1, 'int', 2),
('column_attribute', 'db_table_name', '表名', TRUE, TRUE, TRUE, TRUE, 2, 'varchar', 1),
('column_attribute', 'column_name', '字段名', TRUE, TRUE, TRUE, TRUE, 3, 'varchar', 1),
('column_attribute', 'page_name', '显示名称', TRUE, TRUE, TRUE, TRUE, 4, 'varchar', 1),
('column_attribute', 'is_show_in_list', '列表显示', TRUE, TRUE, TRUE, FALSE, 5, 'tinyint', 3),
('column_attribute', 'search_flag', '可搜索', TRUE, TRUE, TRUE, FALSE, 6, 'tinyint', 3),
('column_attribute', 'edit_flag', '可编辑', TRUE, TRUE, TRUE, FALSE, 7, 'tinyint', 3),
('column_attribute', 'is_required', '必填', TRUE, TRUE, TRUE, FALSE, 8, 'tinyint', 3),
('column_attribute', 'field_type', '字段类型', TRUE, TRUE, TRUE, FALSE, 9, 'varchar', 1),
('column_attribute', 'field_num_type', '字段类型数值', TRUE, FALSE, TRUE, FALSE, 10, 'int', 2),
('column_attribute', 'other_info', '其它信息', FALSE, FALSE, TRUE, FALSE, 11, 'varchar', 1);

-- security_config 表字段属性配置
INSERT INTO column_attribute (db_table_name, column_name, page_name, is_show_in_list, search_flag, edit_flag, is_required, order_no, field_type, field_num_type) VALUES
('security_config', 'id', 'ID', FALSE, FALSE, FALSE, FALSE, 1, 'int', 2),
('security_config', 'type', '类型', TRUE, TRUE, TRUE, TRUE, 2, 'varchar', 1),
('security_config', 'pattern', '匹配模式', TRUE, TRUE, TRUE, TRUE, 3, 'varchar', 1),
('security_config', 'is_active', '是否激活', TRUE, TRUE, TRUE, TRUE, 4, 'tinyint', 3);

-- plugin_config 表字段属性配置
INSERT INTO column_attribute (db_table_name, column_name, page_name, is_show_in_list, search_flag, edit_flag, is_required, order_no, field_type, field_num_type) VALUES
('plugin_config', 'id', 'ID', FALSE, FALSE, FALSE, FALSE, 1, 'int', 2),
('plugin_config', 'plugin_name', '插件名称', TRUE, TRUE, TRUE, TRUE, 2, 'varchar', 1),
('plugin_config', 'class_name', '类名', TRUE, TRUE, TRUE, TRUE, 3, 'varchar', 1),
('plugin_config', 'description', '描述', TRUE, TRUE, TRUE, FALSE, 4, 'varchar', 1),
('plugin_config', 'is_active', '是否激活', TRUE, TRUE, TRUE, TRUE, 5, 'tinyint', 3);

-- ai_processor_config 表字段属性配置
INSERT INTO column_attribute (db_table_name, column_name, page_name, is_show_in_list, search_flag, edit_flag, is_required, order_no, field_type, field_num_type) VALUES
('ai_processor_config', 'id', 'ID', FALSE, FALSE, FALSE, FALSE, 1, 'int', 2),
('ai_processor_config', 'processor_name', '处理器名称', TRUE, TRUE, TRUE, TRUE, 2, 'varchar', 1),
('ai_processor_config', 'class_name', '类名', TRUE, TRUE, TRUE, TRUE, 3, 'varchar', 1),
('ai_processor_config', 'description', '描述', TRUE, TRUE, TRUE, FALSE, 4, 'varchar', 1),
('ai_processor_config', 'is_active', '是否激活', TRUE, TRUE, TRUE, TRUE, 5, 'tinyint', 3);

-- column_check_property 表字段属性配置
INSERT INTO column_attribute (db_table_name, column_name, page_name, is_show_in_list, search_flag, edit_flag, is_required, order_no, field_type, field_num_type) VALUES
('column_check_property', 'id', 'ID', FALSE, FALSE, FALSE, FALSE, 1, 'int', 2),
('column_check_property', 'check_table', '检查表', TRUE, TRUE, TRUE, TRUE, 2, 'varchar', 1),
('column_check_property', 'target_table', '目标表', TRUE, TRUE, TRUE, TRUE, 3, 'varchar', 1),
('column_check_property', 'check_column', '检查字段', TRUE, TRUE, TRUE, TRUE, 4, 'varchar', 1),
('column_check_property', 'check_mode', '检查模式', TRUE, TRUE, TRUE, TRUE, 5, 'varchar', 1),
('column_check_property', 'check_order', '检查顺序', TRUE, TRUE, TRUE, FALSE, 6, 'int', 2),
('column_check_property', 'status', '状态', TRUE, TRUE, TRUE, TRUE, 7, 'int', 2),
('column_check_property', 'errorMsg', '错误消息', TRUE, TRUE, TRUE, FALSE, 8, 'varchar', 1);

-- sys_dict_category 表字段属性配置
INSERT INTO column_attribute (db_table_name, column_name, page_name, is_show_in_list, search_flag, edit_flag, is_required, order_no, field_type, field_num_type) VALUES
('sys_dict_category', 'code', '分类编码', TRUE, TRUE, TRUE, TRUE, 1, 'varchar', 1),
('sys_dict_category', 'name', '分类名称', TRUE, TRUE, TRUE, TRUE, 2, 'varchar', 1);

-- sys_dict_item 表字段属性配置
INSERT INTO column_attribute (db_table_name, column_name, page_name, is_show_in_list, search_flag, edit_flag, is_required, order_no, field_type, field_num_type) VALUES
('sys_dict_item', 'id', 'ID', FALSE, FALSE, FALSE, FALSE, 1, 'int', 2),
('sys_dict_item', 'category_code', '分类编码', TRUE, TRUE, TRUE, TRUE, 2, 'varchar', 1),
('sys_dict_item', 'item_key', '字典键', TRUE, TRUE, TRUE, TRUE, 3, 'varchar', 1),
('sys_dict_item', 'item_value', '字典值', TRUE, TRUE, TRUE, TRUE, 4, 'varchar', 1),
('sys_dict_item', 'sort_no', '排序号', TRUE, TRUE, TRUE, FALSE, 5, 'int', 2);

-- test_preprocessor 表字段属性配置
INSERT INTO column_attribute (db_table_name, column_name, page_name, is_show_in_list, search_flag, edit_flag, is_required, order_no, field_type, field_num_type) VALUES
('test_preprocessor', 'id', 'ID', TRUE, FALSE, FALSE, FALSE, 1, 'int', 2),
('test_preprocessor', 'title', '标题', TRUE, TRUE, TRUE, TRUE, 2, 'varchar', 1),
('test_preprocessor', 'user_id', '用户ID', TRUE, TRUE, TRUE, FALSE, 3, 'int', 2),
('test_preprocessor', 'org_id', '组织ID', TRUE, TRUE, TRUE, FALSE, 4, 'int', 2),
('test_preprocessor', 'status', '状态', TRUE, TRUE, TRUE, FALSE, 5, 'tinyint', 3),
('test_preprocessor', 'content', '内容', TRUE, FALSE, TRUE, FALSE, 6, 'varchar', 1),
('test_preprocessor', 'created_at', '创建时间', TRUE, TRUE, FALSE, FALSE, 7, 'timestamp', 4),
('test_preprocessor', 'updated_at', '更新时间', TRUE, TRUE, FALSE, FALSE, 8, 'timestamp', 4),
('test_preprocessor', 'create_by', '创建人', TRUE, TRUE, FALSE, FALSE, 9, 'int', 2),
('test_preprocessor', 'update_by', '更新人', TRUE, TRUE, FALSE, FALSE, 10, 'int', 2);

-- system_log 表字段属性配置
INSERT INTO column_attribute (db_table_name, column_name, page_name, is_show_in_list, search_flag, edit_flag, is_required, order_no, field_type, field_num_type) VALUES
('system_log', 'id', 'ID', FALSE, FALSE, FALSE, FALSE, 1, 'int', 2),
('system_log', 'user_id', '用户ID', TRUE, TRUE, FALSE, FALSE, 2, 'int', 2),
('system_log', 'username', '用户名', TRUE, TRUE, FALSE, FALSE, 3, 'varchar', 1),
('system_log', 'method', '请求方法', TRUE, TRUE, FALSE, FALSE, 4, 'varchar', 1),
('system_log', 'request_uri', '请求URI', TRUE, TRUE, FALSE, FALSE, 5, 'varchar', 1),
('system_log', 'params', '请求参数', FALSE, FALSE, FALSE, FALSE, 6, 'text', 1),
('system_log', 'client_ip', '客户端IP', TRUE, TRUE, FALSE, FALSE, 7, 'varchar', 1),
('system_log', 'status', '状态码', TRUE, TRUE, FALSE, FALSE, 8, 'int', 2),
('system_log', 'response_body', '响应内容', FALSE, FALSE, FALSE, FALSE, 9, 'text', 1),
('system_log', 'duration_ms', '执行时间(毫秒)', TRUE, TRUE, FALSE, FALSE, 10, 'bigint', 2),
('system_log', 'created_at', '创建时间', TRUE, TRUE, FALSE, FALSE, 11, 'timestamp', 4);

-- =====================================================================
-- 列校验规则配置 (column_check_property)
-- =====================================================================

-- sys_user 表字段校验规则
INSERT INTO column_check_property (check_table, target_table, check_column, check_mode, check_order, status, errorMsg) VALUES
('sys_user', 'sys_user', 'username', 'required', 1, 1, '用户名不能为空'),
('sys_user', 'sys_user', 'username', 'unique', 2, 1, '用户名已存在'),
('sys_user', 'sys_user', 'password', 'required', 3, 1, '密码不能为空'),
('sys_user', 'sys_user', 'email', 'email', 4, 1, '邮箱格式不正确');

-- sys_role 表字段校验规则
INSERT INTO column_check_property (check_table, target_table, check_column, check_mode, check_order, status, errorMsg) VALUES
('sys_role', 'sys_role', 'role_name', 'required', 1, 1, '角色名称不能为空'),
('sys_role', 'sys_role', 'role_code', 'required', 2, 1, '角色编码不能为空'),
('sys_role', 'sys_role', 'role_code', 'unique', 3, 1, '角色编码已存在');

-- sys_org 表字段校验规则
INSERT INTO column_check_property (check_table, target_table, check_column, check_mode, check_order, status, errorMsg) VALUES
('sys_org', 'sys_org', 'org_name', 'required', 1, 1, '机构名称不能为空'),
('sys_org', 'sys_org', 'org_code', 'required', 2, 1, '机构编码不能为空'),
('sys_org', 'sys_org', 'org_code', 'unique', 3, 1, '机构编码已存在');

-- table_attribute 表字段校验规则
INSERT INTO column_check_property (check_table, target_table, check_column, check_mode, check_order, status, errorMsg) VALUES
('table_attribute', 'table_attribute', 'db_table', 'required', 1, 1, '数据库表名不能为空'),
('table_attribute', 'table_attribute', 'db_table', 'unique', 2, 1, '数据库表名已存在'),
('table_attribute', 'table_attribute', 'table_name', 'required', 3, 1, '中文表名不能为空');

-- column_attribute 表字段校验规则
INSERT INTO column_check_property (check_table, target_table, check_column, check_mode, check_order, status, errorMsg) VALUES
('column_attribute', 'column_attribute', 'db_table_name', 'required', 1, 1, '表名不能为空'),
('column_attribute', 'column_attribute', 'column_name', 'required', 2, 1, '字段名不能为空');

-- plugin_config 表字段校验规则
INSERT INTO column_check_property (check_table, target_table, check_column, check_mode, check_order, status, errorMsg) VALUES
('plugin_config', 'plugin_config', 'plugin_name', 'required', 1, 1, '插件名称不能为空'),
('plugin_config', 'plugin_config', 'plugin_name', 'unique', 2, 1, '插件名称已存在'),
('plugin_config', 'plugin_config', 'class_name', 'required', 3, 1, '类名不能为空');

-- test_preprocessor 表字段校验规则
INSERT INTO column_check_property (check_table, target_table, check_column, check_mode, check_order, status, errorMsg) VALUES
('test_preprocessor', 'test_preprocessor', 'title', 'required', 1, 1, '标题不能为空');

