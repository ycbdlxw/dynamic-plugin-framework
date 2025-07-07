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

-- 更新用户的组织机构ID
UPDATE sys_user SET org_id = 1 WHERE username = 'admin'; 