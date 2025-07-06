-- 添加初始用户（密码：ycbd1234）
INSERT INTO sys_user (username, password, real_name, email, status) VALUES
('admin', '$2a$10$D5H.EIucZ9tiKDho4n5W9.0lJLW6L1OwNB6Ua.NPxOB3XX/0CcQye', '管理员', 'admin@example.com', 1);

-- 表属性配置
INSERT INTO table_attribute (db_table, table_name, main_key, sort, module, group_by, is_loading, is_all_select, is_row_operation_flag, is_operation_flag, table_procedure, sub_tables, table_key, parameter_type, alias_name, page_title, role_flag, join_str, defin_columns) VALUES
('sys_user', '系统用户', 'id', 'created_at DESC', 'system', NULL, 0, 0, 0, 0, NULL, NULL, NULL, NULL, NULL, NULL, 0, NULL, NULL);

-- 字段属性配置
INSERT INTO column_attribute (db_table_name, column_name, page_name, is_show_in_list, search_flag, edit_flag, is_required, order_no) VALUES
('sys_user', 'id', 'ID', TRUE, FALSE, FALSE, FALSE, 1),
('sys_user', 'username', '用户名', TRUE, TRUE, TRUE, TRUE, 2),
('sys_user', 'password', '密码', FALSE, FALSE, TRUE, TRUE, 3),
('sys_user', 'real_name', '真实姓名', TRUE, TRUE, TRUE, FALSE, 4),
('sys_user', 'email', '邮箱', TRUE, TRUE, TRUE, FALSE, 5),
('sys_user', 'status', '状态', TRUE, TRUE, TRUE, FALSE, 6),
('sys_user', 'created_at', '创建时间', TRUE, FALSE, FALSE, FALSE, 7);

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
('WHITELIST', '/api-docs/**', TRUE);

-- 命令执行器插件配置
INSERT INTO plugin_config (plugin_name, class_name, description, is_active) VALUES
('CommandExecutor', 'com.ycbd.demo.plugin.commandexecutor.CommandExecutorPlugin', '跨平台命令行执行插件', TRUE);

-- 添加 system_log 表的表属性配置
INSERT INTO table_attribute (db_table, table_name, main_key, sort, module, group_by, is_loading, is_all_select, is_row_operation_flag, is_operation_flag, table_procedure, sub_tables, table_key, parameter_type, alias_name, page_title, role_flag, join_str, defin_columns) VALUES
('system_log', '系统日志', 'id', 'created_at DESC', 'system', NULL, 0, 0, 0, 0, NULL, NULL, NULL, NULL, NULL, NULL, 0, NULL, NULL);

-- 添加 system_log 表的字段属性配置
INSERT INTO column_attribute (db_table_name, column_name, page_name, is_show_in_list, search_flag, edit_flag, is_required, order_no) VALUES
('system_log', 'id', 'ID', FALSE, FALSE, FALSE, FALSE, 1),
('system_log', 'user_id', '用户ID', TRUE, TRUE, FALSE, FALSE, 2),
('system_log', 'username', '用户名', TRUE, TRUE, FALSE, FALSE, 3),
('system_log', 'method', '请求方法', TRUE, TRUE, FALSE, FALSE, 4),
('system_log', 'request_uri', '请求URI', TRUE, TRUE, FALSE, FALSE, 5),
('system_log', 'params', '请求参数', FALSE, FALSE, FALSE, FALSE, 6),
('system_log', 'client_ip', '客户端IP', TRUE, TRUE, FALSE, FALSE, 7),
('system_log', 'status', '状态码', TRUE, TRUE, FALSE, FALSE, 8),
('system_log', 'response_body', '响应内容', FALSE, FALSE, FALSE, FALSE, 9),
('system_log', 'duration_ms', '执行时间(毫秒)', TRUE, TRUE, FALSE, FALSE, 10),
('system_log', 'created_at', '创建时间', TRUE, FALSE, FALSE, FALSE, 11); 