-- Seed a default super-admin user (username: superadmin, password: SuperAdmin@123)
-- with admin/staff/patient roles so the system is usable out of the box.
SET search_path TO ihms_iam, public;

INSERT INTO ihms_iam.t_userdetails (username, password_hash, email, full_name, status, added_by, added_datetime)
VALUES ('superadmin', '$2a$10$/WMHkg7rp.n0vdsvKsrpGeb2crnoASHWPlK6HW/CLaUdAHpzGiAR.', 'superadmin@hims.local', 'Super Admin', TRUE, 1, NOW())
ON CONFLICT (username) DO NOTHING;

-- grant admin role (id 1) to superadmin if not already granted
INSERT INTO ihms_iam.t_userroles (user_id, role_id, added_by, added_datetime)
SELECT u.id, r.id, 1, NOW()
FROM ihms_iam.t_userdetails u
CROSS JOIN ihms_iam.t_userrolemaster r
WHERE u.username = 'superadmin' AND r.code IN ('admin','staff','patient')
  AND NOT EXISTS (SELECT 1 FROM ihms_iam.t_userroles ur WHERE ur.user_id = u.id AND ur.role_id = r.id);
