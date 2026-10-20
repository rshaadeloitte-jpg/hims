-- ============================================================================
-- V2 — M35 IAM workflow (GAP-REGISTER WP-22 HIGH priority)
-- Ported from legacy:
--  - Module/permission master (ModulePermissionMaster, RoleBasedPermission)
--  - Role<->permission grant (t_role_enabledpermission, enabledModulePermissionIdsJson)
--  - OTP login (LDAP/OTP flow, SendNotification, OTP verification)
--  - User unit/store/ward mapping (UserDetail unit/store/ward, Role Ward mapping)
--  - Notification templates (EmailTemplate T_EMAILTEMPLATE)
--
-- ponytail: single migration adds all M35 columns; per-table splitting is YAGNI
-- ============================================================================

SET search_path TO ihms_iam, public;

-- ---------------------------------------------------------------------------
-- t_module_permission_master — clean target of ModulePermissionMaster
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ihms_iam.t_module_permission_master (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(64) NOT NULL,
    module VARCHAR(100) NOT NULL,
    description VARCHAR(255),
    status BOOLEAN NOT NULL DEFAULT TRUE,
    added_by BIGINT,
    added_datetime TIMESTAMP(3),
    updated_by BIGINT,
    updated_datetime TIMESTAMP(3),
    CONSTRAINT uq_module_permission_code UNIQUE (code)
);
CREATE INDEX IF NOT EXISTS ix_module_permission_status ON ihms_iam.t_module_permission_master (status);
CREATE INDEX IF NOT EXISTS ix_module_permission_module ON ihms_iam.t_module_permission_master (module);
COMMENT ON TABLE ihms_iam.t_module_permission_master IS 'Module/menu permission catalogue (ported from ModulePermissionMaster). Code is the permission key used in enabledPermissionIds.';

-- ---------------------------------------------------------------------------
-- t_role_enabledpermission — clean target of t_role_enabledpermission (MBase)
-- One row per role OR per user override (role_id XOR user_id, per legacy RoleBasedPermission)
-- enabled_permission_ids JSON set stored as TEXT (legacy JSON string)
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ihms_iam.t_role_enabledpermission (
    id BIGSERIAL PRIMARY KEY,
    role_id BIGINT REFERENCES ihms_iam.t_userrolemaster (id),
    user_id BIGINT REFERENCES ihms_iam.t_userdetails (id),
    enabled_permission_ids TEXT, -- JSON array string e.g. "[1,2,3]"
    status BOOLEAN NOT NULL DEFAULT TRUE,
    added_by BIGINT,
    added_datetime TIMESTAMP(3),
    updated_by BIGINT,
    updated_datetime TIMESTAMP(3),
    CONSTRAINT chk_role_or_user CHECK ( (role_id IS NOT NULL) OR (user_id IS NOT NULL) ),
    CONSTRAINT uq_role_permission_role UNIQUE (role_id),
    CONSTRAINT uq_role_permission_user UNIQUE (user_id)
);
CREATE INDEX IF NOT EXISTS ix_role_enabledpermission_role ON ihms_iam.t_role_enabledpermission (role_id);
CREATE INDEX IF NOT EXISTS ix_role_enabledpermission_user ON ihms_iam.t_role_enabledpermission (user_id);
COMMENT ON TABLE ihms_iam.t_role_enabledpermission IS 'Role/user -> permission grants. Legacy RoleBasedPermission.enabledModulePermissionIdsJson. One row per role (role_id) OR per user override (user_id).';

-- ---------------------------------------------------------------------------
-- t_otp_store — OTP challenges for phone/email login (feature-flagged)
-- pony: in-PG with 5-min expiry; Redis alternative is later if needed
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ihms_iam.t_otp_store (
    id BIGSERIAL PRIMARY KEY,
    identifier VARCHAR(100) NOT NULL, -- username / phone / email
    otp_hash VARCHAR(255) NOT NULL, -- BCrypt hash of 6-digit OTP
    channel VARCHAR(16) NOT NULL DEFAULT 'SMS', -- SMS / EMAIL / WHATSAPP
    attempts INT NOT NULL DEFAULT 0,
    max_attempts INT NOT NULL DEFAULT 3,
    verified BOOLEAN NOT NULL DEFAULT FALSE,
    expires_at TIMESTAMP(3) NOT NULL,
    created_at TIMESTAMP(3) NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS ix_otp_identifier ON ihms_iam.t_otp_store (identifier);
CREATE INDEX IF NOT EXISTS ix_otp_expires ON ihms_iam.t_otp_store (expires_at);
COMMENT ON TABLE ihms_iam.t_otp_store IS 'OTP challenges: generate stores BCrypt hash, verify checks expiry+attempts. TTL 5 min.';

-- ---------------------------------------------------------------------------
-- t_notification_template — clean target of T_EMAILTEMPLATE
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ihms_iam.t_notification_template (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(64) NOT NULL,
    channel VARCHAR(16) NOT NULL DEFAULT 'EMAIL', -- EMAIL / SMS / WHATSAPP
    subject VARCHAR(255),
    body TEXT NOT NULL,
    sms_text VARCHAR(500),
    template_id VARCHAR(64),
    is_variable BOOLEAN NOT NULL DEFAULT FALSE,
    status BOOLEAN NOT NULL DEFAULT TRUE,
    added_by BIGINT,
    added_datetime TIMESTAMP(3),
    updated_by BIGINT,
    updated_datetime TIMESTAMP(3),
    CONSTRAINT uq_notification_template_code UNIQUE (code)
);
CREATE INDEX IF NOT EXISTS ix_notification_template_status ON ihms_iam.t_notification_template (status);
COMMENT ON TABLE ihms_iam.t_notification_template IS 'Notification templates (ported from T_EMAILTEMPLATE). Channel EMAIL/SMS/WHATSAPP.';

-- ---------------------------------------------------------------------------
-- t_userdetails columns for unit/store/ward mapping (M35 user-store/ward/unit)
-- ---------------------------------------------------------------------------
ALTER TABLE ihms_iam.t_userdetails
    ADD COLUMN IF NOT EXISTS store_id BIGINT,
    ADD COLUMN IF NOT EXISTS ward_id BIGINT,
    ADD COLUMN IF NOT EXISTS department_id BIGINT;

CREATE INDEX IF NOT EXISTS ix_userdetails_store ON ihms_iam.t_userdetails (store_id);
CREATE INDEX IF NOT EXISTS ix_userdetails_ward ON ihms_iam.t_userdetails (ward_id);

-- ---------------------------------------------------------------------------
-- Seed baseline permissions (module-level, mirrors legacy ModulePermissionMaster defaults)
-- ---------------------------------------------------------------------------
INSERT INTO ihms_iam.t_module_permission_master (code, module, description, status, added_by, added_datetime)
VALUES
    ('PATIENT_READ', 'patient', 'Patient read/search', TRUE, 1, NOW()),
    ('PATIENT_WRITE', 'patient', 'Patient register/update', TRUE, 1, NOW()),
    ('BILLING_READ', 'billing', 'Billing read', TRUE, 1, NOW()),
    ('BILLING_WRITE', 'billing', 'Billing write/settle', TRUE, 1, NOW()),
    ('LAB_READ', 'lab', 'Lab worklist/results', TRUE, 1, NOW()),
    ('LAB_WRITE', 'lab', 'Lab result entry/verify', TRUE, 1, NOW()),
    ('PHARMACY_READ', 'pharmacy', 'Pharmacy read', TRUE, 1, NOW()),
    ('PHARMACY_WRITE', 'pharmacy', 'Pharmacy dispense', TRUE, 1, NOW()),
    ('NURSING_READ', 'nursing', 'Nursing worklist', TRUE, 1, NOW()),
    ('NURSING_WRITE', 'nursing', 'Nursing notes/vitals', TRUE, 1, NOW()),
    ('ADMIN', 'admin', 'Admin operations', TRUE, 1, NOW())
ON CONFLICT (code) DO NOTHING;

INSERT INTO ihms_iam.t_notification_template (code, channel, subject, body, status, added_by, added_datetime)
VALUES
    ('OTP_LOGIN', 'SMS', 'Your OTP', 'Your OTP is {{otp}} valid for 5 minutes', TRUE, 1, NOW()),
    ('CRITICAL_ALERT', 'SMS', 'Critical lab alert', 'Critical value {{parameter}}={{value}} for patient {{patient}}', TRUE, 1, NOW())
ON CONFLICT (code) DO NOTHING;
