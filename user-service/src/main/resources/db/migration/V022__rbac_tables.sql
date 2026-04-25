-- =============================================================
-- V022: RBAC — admin roles and permissions
-- Applied to tenant schemas for per-tenant admin management
-- =============================================================

-- ─── Add role column to users ─────────────────────────────────
ALTER TABLE users ADD COLUMN IF NOT EXISTS role VARCHAR(30) NOT NULL DEFAULT 'TENANT_ADMIN';

-- ─── admin_permissions ─────────────────────────────────────────
CREATE TABLE IF NOT EXISTS admin_permissions (
    id              BIGINT          PRIMARY KEY,
    code            VARCHAR(50)     NOT NULL UNIQUE,   -- e.g. 'tenant.read', 'user.write'
    name            VARCHAR(100)    NOT NULL,           -- e.g. '查看租户'
    module          VARCHAR(50)     NOT NULL,           -- e.g. 'tenant', 'user', 'order', 'billing'
    action          VARCHAR(20)     NOT NULL,           -- 'read' | 'write' | 'delete'

    status          INTEGER         NOT NULL DEFAULT 1,
    create_by       BIGINT,
    create_time     TIMESTAMPTZ,
    update_by       BIGINT,
    update_time     TIMESTAMPTZ,
    is_valid        BOOLEAN         NOT NULL DEFAULT TRUE
);

-- ─── admin_role_permissions ─────────────────────────────────────
CREATE TABLE IF NOT EXISTS admin_role_permissions (
    id              BIGINT          PRIMARY KEY,
    role            VARCHAR(30)     NOT NULL,
    permission_id   BIGINT          NOT NULL REFERENCES admin_permissions(id),

    status          INTEGER         NOT NULL DEFAULT 1,
    create_by       BIGINT,
    create_time     TIMESTAMPTZ,
    update_by       BIGINT,
    update_time     TIMESTAMPTZ,
    is_valid        BOOLEAN         NOT NULL DEFAULT TRUE,

    CONSTRAINT admin_role_permissions_unique UNIQUE (role, permission_id)
);

CREATE INDEX IF NOT EXISTS idx_arp_role ON admin_role_permissions (role);

-- ─── Seed permissions ──────────────────────────────────────────
INSERT INTO admin_permissions (id, code, name, module, action) VALUES
    (300001, 'dashboard.read',  '查看仪表盘', 'dashboard', 'read'),
    (300002, 'tenant.read',     '查看租户',   'tenant',    'read'),
    (300003, 'tenant.write',    '管理租户',   'tenant',    'write'),
    (300004, 'tenant.delete',   '删除租户',   'tenant',    'delete'),
    (300005, 'user.read',       '查看用户',   'user',      'read'),
    (300006, 'user.write',      '管理用户',   'user',      'write'),
    (300007, 'user.delete',     '删除用户',   'user',      'delete'),
    (300008, 'order.read',      '查看订单',   'order',     'read'),
    (300009, 'order.write',     '管理订单',   'order',     'write'),
    (300010, 'billing.read',    '查看账单',   'billing',   'read'),
    (300011, 'billing.write',   '管理账单',   'billing',   'write'),
    (300012, 'settings.read',   '查看设置',   'settings',  'read'),
    (300013, 'settings.write',  '修改设置',   'settings',  'write'),
    (300014, 'role.read',       '查看角色',   'role',      'read'),
    (300015, 'role.write',      '管理角色',   'role',      'write')
ON CONFLICT (code) DO NOTHING;

-- ─── SUPER_ADMIN gets all permissions ──────────────────────────
INSERT INTO admin_role_permissions (id, role, permission_id)
SELECT 310001 + row_number() OVER (), 'SUPER_ADMIN', id FROM admin_permissions
ON CONFLICT (role, permission_id) DO NOTHING;

-- ─── TENANT_ADMIN gets most permissions (except role management) ─
INSERT INTO admin_role_permissions (id, role, permission_id)
SELECT 320001 + row_number() OVER (), 'TENANT_ADMIN', id FROM admin_permissions
WHERE code NOT IN ('role.read', 'role.write', 'tenant.delete')
ON CONFLICT (role, permission_id) DO NOTHING;

-- ─── SUPPORT gets read + user write ────────────────────────────
INSERT INTO admin_role_permissions (id, role, permission_id)
SELECT 330001 + row_number() OVER (), 'SUPPORT', id FROM admin_permissions
WHERE code IN ('dashboard.read', 'tenant.read', 'user.read', 'user.write', 'order.read', 'order.write')
ON CONFLICT (role, permission_id) DO NOTHING;

-- ─── BILLING gets billing + dashboard read ─────────────────────
INSERT INTO admin_role_permissions (id, role, permission_id)
SELECT 340001 + row_number() OVER (), 'BILLING', id FROM admin_permissions
WHERE code IN ('dashboard.read', 'billing.read', 'billing.write', 'tenant.read')
ON CONFLICT (role, permission_id) DO NOTHING;

-- ─── Set existing admin user as SUPER_ADMIN ────────────────────
UPDATE users SET role = 'SUPER_ADMIN' WHERE email = 'admin@iemodo.com';
