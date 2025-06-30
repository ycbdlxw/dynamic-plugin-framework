-- 添加初始用户（密码：ycbd1234）
INSERT INTO sys_user (username, password, real_name, email, status) VALUES
('admin', '$2a$10$D5H.EIucZ9tiKDho4n5W9.0lJLW6L1OwNB6Ua.NPxOB3XX/0CcQye', '管理员', 'admin@example.com', 1);

-- 表属性配置
INSERT INTO table_attribute (dbtable, tableName, mainKey, sort) VALUES
('sys_user', '系统用户', 'id', 'created_at DESC');

-- 字段属性配置
INSERT INTO column_attribute (dbTableName, name, pagename, IsShowInList, searchFlag, editFlag, IsRequired, OrderNo) VALUES
('sys_user', 'id', 'ID', TRUE, FALSE, FALSE, FALSE, 1),
('sys_user', 'username', '用户名', TRUE, TRUE, TRUE, TRUE, 2),
('sys_user', 'password', '密码', FALSE, FALSE, TRUE, TRUE, 3),
('sys_user', 'real_name', '真实姓名', TRUE, TRUE, TRUE, FALSE, 4),
('sys_user', 'email', '邮箱', TRUE, TRUE, TRUE, FALSE, 5),
('sys_user', 'status', '状态', TRUE, TRUE, TRUE, FALSE, 6),
('sys_user', 'created_at', '创建时间', TRUE, FALSE, FALSE, FALSE, 7);

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