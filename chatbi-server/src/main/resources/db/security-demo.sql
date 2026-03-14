-- Security demo seed data (idempotent, no fixed primary keys)

INSERT INTO sys_user (username, password, email, nick_name, status, is_admin, created_at, updated_at)
SELECT 'admin', 'placeholder', 'admin@chatbi.com', '超级管理员', 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM sys_user WHERE username = 'admin' AND deleted_at IS NULL
);

INSERT INTO sys_role (role_code, role_name, description, data_scope, status, sort_order, created_at, updated_at)
SELECT 'ADMIN', '系统管理员', '企业级管理员角色', 1, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM sys_role WHERE role_code = 'ADMIN' AND deleted_at IS NULL
);

INSERT INTO sys_role (role_code, role_name, description, data_scope, status, sort_order, created_at, updated_at)
SELECT 'ANALYST', '业务分析师', '分析与查看业务数据', 1, 1, 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM sys_role WHERE role_code = 'ANALYST' AND deleted_at IS NULL
);

INSERT INTO sys_permission (perm_code, perm_name, resource_type, resource_path, parent_id, sort_order, icon, status, created_at)
SELECT p.perm_code, p.perm_name, 'API', p.resource_path, 0, p.sort_order, p.icon, 1, CURRENT_TIMESTAMP
FROM (
    SELECT 'admin' AS perm_code, '管理审计权限' AS perm_name, '/api/audit-logs' AS resource_path, 1 AS sort_order, 'Lock' AS icon
    UNION ALL SELECT 'datasource:query', '查询数据源', '/api/datasources', 2, 'DataBoard'
    UNION ALL SELECT 'datasource:add', '新增数据源', '/api/datasources', 3, 'Plus'
    UNION ALL SELECT 'datasource:update', '修改数据源', '/api/datasources/*', 4, 'Edit'
    UNION ALL SELECT 'datasource:delete', '删除数据源', '/api/datasources/*', 5, 'Delete'
    UNION ALL SELECT 'datasource:test', '测试数据源', '/api/datasources/test', 6, 'Connection'
) p
WHERE NOT EXISTS (
    SELECT 1 FROM sys_permission sp WHERE sp.perm_code = p.perm_code AND sp.deleted_at IS NULL
);

INSERT INTO sys_user_role (user_id, role_id, created_at)
SELECT u.id, r.id, CURRENT_TIMESTAMP
FROM sys_user u
JOIN sys_role r ON r.role_code = 'ADMIN' AND r.deleted_at IS NULL
WHERE u.username = 'admin' AND u.deleted_at IS NULL
  AND NOT EXISTS (
    SELECT 1 FROM sys_user_role ur WHERE ur.user_id = u.id AND ur.role_id = r.id
  );

INSERT INTO sys_role_permission (role_id, permission_id, created_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP
FROM sys_role r
JOIN sys_permission p ON p.deleted_at IS NULL
WHERE r.role_code = 'ADMIN' AND r.deleted_at IS NULL
  AND p.perm_code IN ('admin', 'datasource:query', 'datasource:add', 'datasource:update', 'datasource:delete', 'datasource:test')
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_permission rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
