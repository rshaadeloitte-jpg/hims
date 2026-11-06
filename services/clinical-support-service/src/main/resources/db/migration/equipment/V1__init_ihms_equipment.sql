-- ============================================================================
-- V1 — ihms_equipment schema (Equipment Management service, S18) — MIG-023
-- Clean target of the legacy bio-medical equipment cluster. Single-writer
-- ownership: equipment-service is the sole writer of ihms_equipment
-- (same extraction pattern as ihms_item / ihms_grievance / ihms_waste /
-- ihms_transport / ihms_linen, IMP-DEC-026/032/033/034).
--
-- Legacy sources (evidence authority, /mnt/d/ihms read-only):
--   Masters
--     M_EQUIPCHECKLIST                 -> equipment_checklist               (Equipchecklist.java)
--     M_EQUIPMENTPREVENTIVECHECKLIST   -> equipment_preventive_checklist    (Equipmentpreventivechecklist.java)
--     M_EquipmentFunctionalCheckList   -> functional_checklist_master       (Functionalchecklistmaster.java)
--     M_EquipmentElecticalCheckList    -> electrical_checklist_master       (ElectricalChecklistmaster.java)
--     M_COMPLAINTCATEGORYMASTER        -> complaint_category_master         (Complaintcategorymaster.java)
--     M_MAINTENANCECOMPLAINTSTATUS     -> complaint_status_master           (Maintenancecomplaintstatus.java;
--                                                                             resolved/closed/reopened flags from
--                                                                             M_EquipmentStatus.ISRESOLVED/isClosed/isReOpened)
--     M_EqupComplaintType              -> complaint_type_master             (Complainttypemaster.java)
--     M_COMPLIANCEMASTER               -> compliance_master                 (Compliancemaster.java)
--     M_EQUIPMENTDETAILSAMC.MAINTAINCETYPE (free text) -> maintenance_type_master (new clean dictionary)
--     M_eqpworkingstatus               -> equipment_working_status_master   (Equipworkingstatus.java)
--     M_AMCAGENT                       -> amc_agent_master                  (Amcagent.java)
--     M_SERVICEAGENTDETAILS            -> service_agent_master              (Serviceagentdetails.java)
--     M_INSURANCEAGENT                 -> insurance_agent_master            (Insuranceagent.java)
--   Transactionals
--     t_equipmentdetails + M_EQUIPMENTDETAIL -> equipment                   (Equipment.java / Equipmentdetail.java)
--     T_EQUIPMENTDETAILDOCS            -> equipment_detail_docs             (EquipmentDetailDocs.java)
--     m_eqpwarranty                    -> equipment_warranty                (EquipmentWarranty.java)
--     T_EquipmentTransfer              -> equipment_transfer                (EquipmentTransfer.java)
--     usage tracking                   -> equipment_usage_details           (T_EQUIPMENTDATADETAILS readings shape)
--     T_EQUIPMENTOPENINGBALANCE        -> equipment_opening_balance         (EqiupOpeningBalance.java)
--
-- Status dictionaries (seeded below, ids are the API codes):
--   complaint_status:      1=OPEN 2=IN_PROGRESS 3=RESOLVED 4=CLOSED
--   working_status:        1=WORKING 2=NOT_WORKING 3=UNDER_REPAIR 4=CONDEMNED
--   maintenance_type:      1=PREVENTIVE 2=CORRECTIVE 3=CALIBRATION
--
-- Cross-domain references deferred (dependency-neutral): item_id -> item
-- domain (ihms_item), supplier_id / department_id / store_id / custodian ->
-- IAM + store domains — no FK until those integration slices.
-- REST-only slice: EquipmentComplaintRaised REJECTED phase 1
-- (KAFKA-EVENT-CATALOG) — no outbox table here.
-- ============================================================================

CREATE SCHEMA IF NOT EXISTS ihms_equipment;

-- ---------------------------------------------------------------- masters --

CREATE TABLE IF NOT EXISTS ihms_equipment.equipment_checklist (
    id BIGINT NOT NULL,
    unit_id BIGINT,
    status BOOLEAN NOT NULL DEFAULT TRUE,
    created_unit_id BIGINT,
    updated_unit_id BIGINT,
    added_by BIGINT,
    added_datetime TIMESTAMP(3),
    updated_by BIGINT,
    updated_datetime TIMESTAMP(3),
    synchronized BOOLEAN NOT NULL DEFAULT FALSE,
    item_id BIGINT,
    serial_no VARCHAR(128),
    description VARCHAR(500),
    part_no VARCHAR(64),
    part_sr_no VARCHAR(64),
    quantity BIGINT NOT NULL DEFAULT 1,
    functional_list_id BIGINT,
    equipment_id BIGINT,
    CONSTRAINT pk_equipment_checklist PRIMARY KEY (id)
);
CREATE INDEX IF NOT EXISTS ix_equipment_checklist_item ON ihms_equipment.equipment_checklist (item_id);

CREATE TABLE IF NOT EXISTS ihms_equipment.equipment_preventive_checklist (
    id BIGINT NOT NULL,
    unit_id BIGINT,
    status BOOLEAN NOT NULL DEFAULT TRUE,
    created_unit_id BIGINT,
    updated_unit_id BIGINT,
    added_by BIGINT,
    added_datetime TIMESTAMP(3),
    updated_by BIGINT,
    updated_datetime TIMESTAMP(3),
    synchronized BOOLEAN NOT NULL DEFAULT FALSE,
    code VARCHAR(64) NOT NULL,
    description VARCHAR(255) NOT NULL,
    CONSTRAINT pk_equipment_preventive_checklist PRIMARY KEY (id)
);
CREATE INDEX IF NOT EXISTS ix_equipment_preventive_checklist_code ON ihms_equipment.equipment_preventive_checklist (code);

CREATE TABLE IF NOT EXISTS ihms_equipment.functional_checklist_master (
    id BIGINT NOT NULL,
    unit_id BIGINT,
    status BOOLEAN NOT NULL DEFAULT TRUE,
    created_unit_id BIGINT,
    updated_unit_id BIGINT,
    added_by BIGINT,
    added_datetime TIMESTAMP(3),
    updated_by BIGINT,
    updated_datetime TIMESTAMP(3),
    synchronized BOOLEAN NOT NULL DEFAULT FALSE,
    code VARCHAR(64) NOT NULL,
    description VARCHAR(255) NOT NULL,
    CONSTRAINT pk_functional_checklist_master PRIMARY KEY (id)
);
CREATE INDEX IF NOT EXISTS ix_functional_checklist_master_code ON ihms_equipment.functional_checklist_master (code);

CREATE TABLE IF NOT EXISTS ihms_equipment.electrical_checklist_master (
    id BIGINT NOT NULL,
    unit_id BIGINT,
    status BOOLEAN NOT NULL DEFAULT TRUE,
    created_unit_id BIGINT,
    updated_unit_id BIGINT,
    added_by BIGINT,
    added_datetime TIMESTAMP(3),
    updated_by BIGINT,
    updated_datetime TIMESTAMP(3),
    synchronized BOOLEAN NOT NULL DEFAULT FALSE,
    code VARCHAR(64) NOT NULL,
    description VARCHAR(255) NOT NULL,
    CONSTRAINT pk_electrical_checklist_master PRIMARY KEY (id)
);
CREATE INDEX IF NOT EXISTS ix_electrical_checklist_master_code ON ihms_equipment.electrical_checklist_master (code);

CREATE TABLE IF NOT EXISTS ihms_equipment.complaint_category_master (
    id BIGINT NOT NULL,
    unit_id BIGINT,
    status BOOLEAN NOT NULL DEFAULT TRUE,
    created_unit_id BIGINT,
    updated_unit_id BIGINT,
    added_by BIGINT,
    added_datetime TIMESTAMP(3),
    updated_by BIGINT,
    updated_datetime TIMESTAMP(3),
    synchronized BOOLEAN NOT NULL DEFAULT FALSE,
    code VARCHAR(64) NOT NULL,
    description VARCHAR(255) NOT NULL,
    CONSTRAINT pk_complaint_category_master PRIMARY KEY (id)
);
CREATE INDEX IF NOT EXISTS ix_complaint_category_master_code ON ihms_equipment.complaint_category_master (code);

-- Complaint lifecycle dictionary. Ids ARE the status codes used by
-- ComplaintStatus constants (Task 7): 1=OPEN 2=IN_PROGRESS 3=RESOLVED 4=CLOSED.
CREATE TABLE IF NOT EXISTS ihms_equipment.complaint_status_master (
    id BIGINT NOT NULL,
    unit_id BIGINT,
    status BOOLEAN NOT NULL DEFAULT TRUE,
    created_unit_id BIGINT,
    updated_unit_id BIGINT,
    added_by BIGINT,
    added_datetime TIMESTAMP(3),
    updated_by BIGINT,
    updated_datetime TIMESTAMP(3),
    synchronized BOOLEAN NOT NULL DEFAULT FALSE,
    code VARCHAR(64) NOT NULL,
    description VARCHAR(255) NOT NULL,
    is_resolved BOOLEAN NOT NULL DEFAULT FALSE,
    is_closed BOOLEAN NOT NULL DEFAULT FALSE,
    is_reopened BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT pk_complaint_status_master PRIMARY KEY (id),
    CONSTRAINT uq_complaint_status_master_code UNIQUE (code)
);

CREATE TABLE IF NOT EXISTS ihms_equipment.complaint_type_master (
    id BIGINT NOT NULL,
    unit_id BIGINT,
    status BOOLEAN NOT NULL DEFAULT TRUE,
    created_unit_id BIGINT,
    updated_unit_id BIGINT,
    added_by BIGINT,
    added_datetime TIMESTAMP(3),
    updated_by BIGINT,
    updated_datetime TIMESTAMP(3),
    synchronized BOOLEAN NOT NULL DEFAULT FALSE,
    code VARCHAR(64) NOT NULL,
    description VARCHAR(255) NOT NULL,
    expected_resolution_hours BIGINT,
    period VARCHAR(32),
    CONSTRAINT pk_complaint_type_master PRIMARY KEY (id)
);
CREATE INDEX IF NOT EXISTS ix_complaint_type_master_code ON ihms_equipment.complaint_type_master (code);

CREATE TABLE IF NOT EXISTS ihms_equipment.compliance_master (
    id BIGINT NOT NULL,
    unit_id BIGINT,
    status BOOLEAN NOT NULL DEFAULT TRUE,
    created_unit_id BIGINT,
    updated_unit_id BIGINT,
    added_by BIGINT,
    added_datetime TIMESTAMP(3),
    updated_by BIGINT,
    updated_datetime TIMESTAMP(3),
    synchronized BOOLEAN NOT NULL DEFAULT FALSE,
    code VARCHAR(64) NOT NULL,
    description VARCHAR(255) NOT NULL,
    compliance_type_id BIGINT,
    CONSTRAINT pk_compliance_master PRIMARY KEY (id)
);
CREATE INDEX IF NOT EXISTS ix_compliance_master_code ON ihms_equipment.compliance_master (code);

-- Maintenance dictionary. Ids ARE the type codes: 1=PREVENTIVE 2=CORRECTIVE 3=CALIBRATION.
-- Replaces the free-text M_EQUIPMENTDETAILSAMC.MAINTAINCETYPE column.
CREATE TABLE IF NOT EXISTS ihms_equipment.maintenance_type_master (
    id BIGINT NOT NULL,
    unit_id BIGINT,
    status BOOLEAN NOT NULL DEFAULT TRUE,
    created_unit_id BIGINT,
    updated_unit_id BIGINT,
    added_by BIGINT,
    added_datetime TIMESTAMP(3),
    updated_by BIGINT,
    updated_datetime TIMESTAMP(3),
    synchronized BOOLEAN NOT NULL DEFAULT FALSE,
    code VARCHAR(64) NOT NULL,
    description VARCHAR(255) NOT NULL,
    CONSTRAINT pk_maintenance_type_master PRIMARY KEY (id),
    CONSTRAINT uq_maintenance_type_master_code UNIQUE (code)
);

-- Equipment operational-status dictionary. Ids ARE the codes used by
-- EquipmentWorkingStatus constants (Task 3): 1=WORKING 2=NOT_WORKING
-- 3=UNDER_REPAIR 4=CONDEMNED.
CREATE TABLE IF NOT EXISTS ihms_equipment.equipment_working_status_master (
    id BIGINT NOT NULL,
    unit_id BIGINT,
    status BOOLEAN NOT NULL DEFAULT TRUE,
    created_unit_id BIGINT,
    updated_unit_id BIGINT,
    added_by BIGINT,
    added_datetime TIMESTAMP(3),
    updated_by BIGINT,
    updated_datetime TIMESTAMP(3),
    synchronized BOOLEAN NOT NULL DEFAULT FALSE,
    code VARCHAR(64) NOT NULL,
    description VARCHAR(255) NOT NULL,
    CONSTRAINT pk_equipment_working_status_master PRIMARY KEY (id),
    CONSTRAINT uq_equipment_working_status_master_code UNIQUE (code)
);

CREATE TABLE IF NOT EXISTS ihms_equipment.amc_agent_master (
    id BIGINT NOT NULL,
    unit_id BIGINT,
    status BOOLEAN NOT NULL DEFAULT TRUE,
    created_unit_id BIGINT,
    updated_unit_id BIGINT,
    added_by BIGINT,
    added_datetime TIMESTAMP(3),
    updated_by BIGINT,
    updated_datetime TIMESTAMP(3),
    synchronized BOOLEAN NOT NULL DEFAULT FALSE,
    code VARCHAR(64) NOT NULL,
    name VARCHAR(255) NOT NULL,
    contact_person VARCHAR(255),
    contact_person_number VARCHAR(32),
    contact_person_email VARCHAR(255),
    contact_number VARCHAR(32),
    email_id VARCHAR(255),
    address VARCHAR(500),
    city VARCHAR(64),
    state VARCHAR(64),
    country VARCHAR(64),
    CONSTRAINT pk_amc_agent_master PRIMARY KEY (id)
);
CREATE INDEX IF NOT EXISTS ix_amc_agent_master_code ON ihms_equipment.amc_agent_master (code);

CREATE TABLE IF NOT EXISTS ihms_equipment.service_agent_master (
    id BIGINT NOT NULL,
    unit_id BIGINT,
    status BOOLEAN NOT NULL DEFAULT TRUE,
    created_unit_id BIGINT,
    updated_unit_id BIGINT,
    added_by BIGINT,
    added_datetime TIMESTAMP(3),
    updated_by BIGINT,
    updated_datetime TIMESTAMP(3),
    synchronized BOOLEAN NOT NULL DEFAULT FALSE,
    code VARCHAR(64) NOT NULL,
    name VARCHAR(255) NOT NULL,
    designation VARCHAR(128),
    contact_no VARCHAR(32),
    mobile_no VARCHAR(32),
    email_id VARCHAR(255),
    CONSTRAINT pk_service_agent_master PRIMARY KEY (id)
);
CREATE INDEX IF NOT EXISTS ix_service_agent_master_code ON ihms_equipment.service_agent_master (code);

CREATE TABLE IF NOT EXISTS ihms_equipment.insurance_agent_master (
    id BIGINT NOT NULL,
    unit_id BIGINT,
    status BOOLEAN NOT NULL DEFAULT TRUE,
    created_unit_id BIGINT,
    updated_unit_id BIGINT,
    added_by BIGINT,
    added_datetime TIMESTAMP(3),
    updated_by BIGINT,
    updated_datetime TIMESTAMP(3),
    synchronized BOOLEAN NOT NULL DEFAULT FALSE,
    code VARCHAR(64) NOT NULL,
    name VARCHAR(255) NOT NULL,
    contact_person VARCHAR(255),
    contact_person_number VARCHAR(32),
    contact_person_email VARCHAR(255),
    contact_number VARCHAR(32),
    email_id VARCHAR(255),
    address VARCHAR(500),
    city VARCHAR(64),
    state VARCHAR(64),
    country VARCHAR(64),
    CONSTRAINT pk_insurance_agent_master PRIMARY KEY (id)
);
CREATE INDEX IF NOT EXISTS ix_insurance_agent_master_code ON ihms_equipment.insurance_agent_master (code);

-- ----------------------------------------------------------- transactional --

-- equipment — core asset register. Clean merge of t_equipmentdetails
-- (unit-scoped stock row) + M_EQUIPMENTDETAIL (asset identity: accession no,
-- serial/model, commissioning, custodian). working_status holds a
-- equipment_working_status_master id (default 1 = WORKING).
CREATE TABLE IF NOT EXISTS ihms_equipment.equipment (
    id BIGINT NOT NULL,
    unit_id BIGINT,
    status BOOLEAN NOT NULL DEFAULT TRUE,
    created_unit_id BIGINT,
    updated_unit_id BIGINT,
    added_by BIGINT,
    added_datetime TIMESTAMP(3),
    updated_by BIGINT,
    updated_datetime TIMESTAMP(3),
    synchronized BOOLEAN NOT NULL DEFAULT FALSE,
    item_id BIGINT NOT NULL,
    accession_no VARCHAR(64),
    serial_no VARCHAR(128),
    model VARCHAR(128),
    make VARCHAR(128),
    purchase_date TIMESTAMP(3),
    installation_date TIMESTAMP(3),
    warranty_expiry TIMESTAMP(3),
    working_status BIGINT NOT NULL DEFAULT 1,
    department_id BIGINT,
    store_id BIGINT,
    location VARCHAR(255),
    cost NUMERIC(19, 2),
    supplier_id BIGINT,
    custodian_name VARCHAR(255),
    documents TEXT,
    CONSTRAINT pk_equipment PRIMARY KEY (id)
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_equipment_unit_accession
    ON ihms_equipment.equipment (unit_id, accession_no) WHERE accession_no IS NOT NULL;
CREATE INDEX IF NOT EXISTS ix_equipment_unit ON ihms_equipment.equipment (unit_id);
CREATE INDEX IF NOT EXISTS ix_equipment_item ON ihms_equipment.equipment (item_id);
CREATE INDEX IF NOT EXISTS ix_equipment_working_status ON ihms_equipment.equipment (working_status);
COMMENT ON TABLE ihms_equipment.equipment IS 'Clean target of t_equipmentdetails + M_EQUIPMENTDETAIL. Accession number unique per unit (partial index); working_status -> equipment_working_status_master id.';

CREATE TABLE IF NOT EXISTS ihms_equipment.equipment_detail_docs (
    id BIGINT NOT NULL,
    unit_id BIGINT,
    status BOOLEAN NOT NULL DEFAULT TRUE,
    created_unit_id BIGINT,
    updated_unit_id BIGINT,
    added_by BIGINT,
    added_datetime TIMESTAMP(3),
    updated_by BIGINT,
    updated_datetime TIMESTAMP(3),
    synchronized BOOLEAN NOT NULL DEFAULT FALSE,
    equipment_id BIGINT NOT NULL REFERENCES ihms_equipment.equipment (id),
    doc_type VARCHAR(64),
    file_name VARCHAR(255),
    file_path VARCHAR(500),
    description TEXT,
    CONSTRAINT pk_equipment_detail_docs PRIMARY KEY (id)
);
CREATE INDEX IF NOT EXISTS ix_equipment_detail_docs_equipment ON ihms_equipment.equipment_detail_docs (equipment_id);

-- Warranty / AMC records (warranty_type distinguishes WARRANTY vs AMC; Task 12
-- expiry alerting queries drive off end_date).
CREATE TABLE IF NOT EXISTS ihms_equipment.equipment_warranty (
    id BIGINT NOT NULL,
    unit_id BIGINT,
    status BOOLEAN NOT NULL DEFAULT TRUE,
    created_unit_id BIGINT,
    updated_unit_id BIGINT,
    added_by BIGINT,
    added_datetime TIMESTAMP(3),
    updated_by BIGINT,
    updated_datetime TIMESTAMP(3),
    synchronized BOOLEAN NOT NULL DEFAULT FALSE,
    equipment_id BIGINT NOT NULL REFERENCES ihms_equipment.equipment (id),
    warranty_type VARCHAR(64) NOT NULL DEFAULT 'WARRANTY',
    start_date TIMESTAMP(3),
    end_date TIMESTAMP(3),
    vendor VARCHAR(255),
    terms TEXT,
    CONSTRAINT pk_equipment_warranty PRIMARY KEY (id)
);
CREATE INDEX IF NOT EXISTS ix_equipment_warranty_equipment ON ihms_equipment.equipment_warranty (equipment_id);
CREATE INDEX IF NOT EXISTS ix_equipment_warranty_end_date ON ihms_equipment.equipment_warranty (end_date);

CREATE TABLE IF NOT EXISTS ihms_equipment.equipment_transfer (
    id BIGINT NOT NULL,
    unit_id BIGINT,
    status BOOLEAN NOT NULL DEFAULT TRUE,
    created_unit_id BIGINT,
    updated_unit_id BIGINT,
    added_by BIGINT,
    added_datetime TIMESTAMP(3),
    updated_by BIGINT,
    updated_datetime TIMESTAMP(3),
    synchronized BOOLEAN NOT NULL DEFAULT FALSE,
    equipment_id BIGINT NOT NULL REFERENCES ihms_equipment.equipment (id),
    from_department_id BIGINT,
    to_department_id BIGINT,
    transfer_date TIMESTAMP(3),
    reason TEXT,
    CONSTRAINT pk_equipment_transfer PRIMARY KEY (id)
);
CREATE INDEX IF NOT EXISTS ix_equipment_transfer_equipment ON ihms_equipment.equipment_transfer (equipment_id);
CREATE INDEX IF NOT EXISTS ix_equipment_transfer_date ON ihms_equipment.equipment_transfer (transfer_date);

CREATE TABLE IF NOT EXISTS ihms_equipment.equipment_usage_details (
    id BIGINT NOT NULL,
    unit_id BIGINT,
    status BOOLEAN NOT NULL DEFAULT TRUE,
    created_unit_id BIGINT,
    updated_unit_id BIGINT,
    added_by BIGINT,
    added_datetime TIMESTAMP(3),
    updated_by BIGINT,
    updated_datetime TIMESTAMP(3),
    synchronized BOOLEAN NOT NULL DEFAULT FALSE,
    equipment_id BIGINT NOT NULL REFERENCES ihms_equipment.equipment (id),
    usage_date TIMESTAMP(3),
    hours_used NUMERIC(8, 2),
    operator VARCHAR(255),
    remarks TEXT,
    CONSTRAINT pk_equipment_usage_details PRIMARY KEY (id)
);
CREATE INDEX IF NOT EXISTS ix_equipment_usage_equipment ON ihms_equipment.equipment_usage_details (equipment_id);
CREATE INDEX IF NOT EXISTS ix_equipment_usage_date ON ihms_equipment.equipment_usage_details (usage_date);

-- Opening balance — clean target of T_EQUIPMENTOPENINGBALANCE (item-level
-- quantity + valuation snapshot taken at cutover).
CREATE TABLE IF NOT EXISTS ihms_equipment.equipment_opening_balance (
    id BIGINT NOT NULL,
    unit_id BIGINT,
    status BOOLEAN NOT NULL DEFAULT TRUE,
    created_unit_id BIGINT,
    updated_unit_id BIGINT,
    added_by BIGINT,
    added_datetime TIMESTAMP(3),
    updated_by BIGINT,
    updated_datetime TIMESTAMP(3),
    synchronized BOOLEAN NOT NULL DEFAULT FALSE,
    item_id BIGINT NOT NULL,
    opening_balance BIGINT NOT NULL DEFAULT 0,
    rate NUMERIC(19, 2),
    amount NUMERIC(19, 2),
    accession_no VARCHAR(64),
    serial_no VARCHAR(128),
    model VARCHAR(128),
    store_id BIGINT,
    supplier_id BIGINT,
    remarks TEXT,
    CONSTRAINT pk_equipment_opening_balance PRIMARY KEY (id)
);
CREATE INDEX IF NOT EXISTS ix_equipment_opening_balance_unit_item ON ihms_equipment.equipment_opening_balance (unit_id, item_id);

-- -------------------------------------------------------------- sequences --
-- Explicit sequences per migrated table (IMP-DEC-003; no BIGSERIAL).

CREATE SEQUENCE IF NOT EXISTS ihms_equipment.equipment_checklist_seq                    START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS ihms_equipment.equipment_preventive_checklist_seq         START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS ihms_equipment.functional_checklist_master_seq            START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS ihms_equipment.electrical_checklist_master_seq            START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS ihms_equipment.complaint_category_master_seq              START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS ihms_equipment.complaint_status_master_seq                START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS ihms_equipment.complaint_type_master_seq                  START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS ihms_equipment.compliance_master_seq                      START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS ihms_equipment.maintenance_type_master_seq                START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS ihms_equipment.equipment_working_status_master_seq        START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS ihms_equipment.amc_agent_master_seq                       START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS ihms_equipment.service_agent_master_seq                   START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS ihms_equipment.insurance_agent_master_seq                 START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS ihms_equipment.equipment_seq                              START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS ihms_equipment.equipment_detail_docs_seq                  START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS ihms_equipment.equipment_warranty_seq                     START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS ihms_equipment.equipment_transfer_seq                     START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS ihms_equipment.equipment_usage_details_seq                START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS ihms_equipment.equipment_opening_balance_seq              START WITH 1 INCREMENT BY 1;

-- ------------------------------------------------------------ seed data ----
-- Status dictionaries only (ids = API codes used by the constants classes).
-- Idempotent: ON CONFLICT DO NOTHING + sequence aligned to MAX(id).

INSERT INTO ihms_equipment.complaint_status_master
    (id, code, description, is_resolved, is_closed, status, synchronized)
VALUES
    (1, 'OPEN',        'Open',                          FALSE, FALSE, TRUE, FALSE),
    (2, 'IN_PROGRESS', 'In progress',                   FALSE, FALSE, TRUE, FALSE),
    (3, 'RESOLVED',    'Resolved',                      TRUE,  FALSE, TRUE, FALSE),
    (4, 'CLOSED',      'Closed',                        TRUE,  TRUE,  TRUE, FALSE)
ON CONFLICT DO NOTHING;

INSERT INTO ihms_equipment.equipment_working_status_master
    (id, code, description, status, synchronized)
VALUES
    (1, 'WORKING',      'Working',                      TRUE, FALSE),
    (2, 'NOT_WORKING',  'Not working',                  TRUE, FALSE),
    (3, 'UNDER_REPAIR', 'Under repair',                 TRUE, FALSE),
    (4, 'CONDEMNED',    'Condemned',                    TRUE, FALSE)
ON CONFLICT DO NOTHING;

INSERT INTO ihms_equipment.maintenance_type_master
    (id, code, description, status, synchronized)
VALUES
    (1, 'PREVENTIVE',  'Preventive maintenance',        TRUE, FALSE),
    (2, 'CORRECTIVE',  'Corrective maintenance',        TRUE, FALSE),
    (3, 'CALIBRATION', 'Calibration',                   TRUE, FALSE)
ON CONFLICT DO NOTHING;

SELECT setval('ihms_equipment.complaint_status_master_seq',
              (SELECT COALESCE(MAX(id), 1) FROM ihms_equipment.complaint_status_master));
SELECT setval('ihms_equipment.equipment_working_status_master_seq',
              (SELECT COALESCE(MAX(id), 1) FROM ihms_equipment.equipment_working_status_master));
SELECT setval('ihms_equipment.maintenance_type_master_seq',
              (SELECT COALESCE(MAX(id), 1) FROM ihms_equipment.maintenance_type_master));
