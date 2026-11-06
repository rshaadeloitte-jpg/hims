-- ============================================================================
-- V2 — complaint tables (Equipment Management service, S18) — MIG-023 Phase 2
-- Clean target of the legacy equipment-complaint cluster. Single-writer
-- ownership: equipment-service is the sole writer of ihms_equipment.
--
-- Legacy sources (evidence authority, /mnt/d/ihms read-only):
--   T_EqpComplaint                      -> equipment_complaint                (EqpComplaint.java)
--   T_eqpComplaintDetails               -> equipment_complaint_details        (EqpComplaintDetails.java)
--   t_eqpcomplaintresolution            -> equipment_complaint_resolution     (EqpComplaintResolution.java)
--   t_eqpcomplaintresolutionagentdetail -> equipment_complaint_resolution_details
--                                                                               (EqpComplaintResolutionDetails.java)
--
-- Complaint lifecycle dictionary (seeded in V1, ids ARE the API codes used by
-- the ComplaintStatus constants, Task 7): 1=OPEN 2=IN_PROGRESS 3=RESOLVED 4=CLOSED.
--
-- Complaint number rule (grievance-service pattern, legacy addGrievenceTicket):
--   IHMS/EQ/{yyyy}/{%05d seq} from ihms_equipment.equipment_complaint_no_seq.
--
-- Deferred long-tail (cross-domain or derivable, documented not dropped):
--   - ISTYPECHANGED / UPDATEDTYPEID / PREVIOUSTYPEID type-change audit trail
--     (complaint re-typing history — follow-up slice if ops needs it);
--   - COMPLAINTRESOLUTIONSTATUS (derivable from complaint_status);
--   - legacy ADDEDON/UPDATEDON free-text strings (redundant with the audit
--     timestamps in the base block).
-- ============================================================================

-- ---------------------------------------------------------------------------
-- equipment_complaint — header record per reported equipment problem
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ihms_equipment.equipment_complaint (
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
    complaint_no VARCHAR(64),
    category_id BIGINT,
    complaint_type_id BIGINT,
    severity_id BIGINT,
    description TEXT,
    complaint_date TIMESTAMP(3),
    reported_by BIGINT,
    reported_date TIMESTAMP(3),
    complaint_status BIGINT NOT NULL DEFAULT 1 CHECK (complaint_status BETWEEN 1 AND 4),
    department_id BIGINT,
    store_id BIGINT,
    accession_no VARCHAR(64),
    room_no VARCHAR(64),
    extension_no VARCHAR(32),
    assigned_to BIGINT,
    expected_resolution_date TIMESTAMP(3),
    resolved_date TIMESTAMP(3),
    visit_date TIMESTAMP(3),
    CONSTRAINT pk_equipment_complaint PRIMARY KEY (id)
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_equipment_complaint_no
    ON ihms_equipment.equipment_complaint (complaint_no) WHERE complaint_no IS NOT NULL;
CREATE INDEX IF NOT EXISTS ix_equipment_complaint_equipment ON ihms_equipment.equipment_complaint (equipment_id);
CREATE INDEX IF NOT EXISTS ix_equipment_complaint_unit ON ihms_equipment.equipment_complaint (unit_id, complaint_status);
CREATE INDEX IF NOT EXISTS ix_equipment_complaint_status ON ihms_equipment.equipment_complaint (complaint_status);
CREATE INDEX IF NOT EXISTS ix_equipment_complaint_date ON ihms_equipment.equipment_complaint (complaint_date);
COMMENT ON TABLE ihms_equipment.equipment_complaint IS 'Clean target of T_EqpComplaint. complaint_no IHMS/EQ/yyyy/%05d (uq partial index); complaint_status -> complaint_status_master id (1=OPEN 2=IN_PROGRESS 3=RESOLVED 4=CLOSED); category/type/severity -> master ids; description maps legacy COMPLAINTDETAILS TEXT.';

-- ---------------------------------------------------------------------------
-- equipment_complaint_details — line items of a complaint (asset snapshot +
-- reporting/priority context captured per line; legacy denormalised detail row)
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ihms_equipment.equipment_complaint_details (
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
    complaint_id BIGINT NOT NULL REFERENCES ihms_equipment.equipment_complaint (id),
    item_id BIGINT,
    serial_no VARCHAR(128),
    model_no VARCHAR(128),
    problem_date TIMESTAMP(3),
    problem_time TIMESTAMP(3),
    reported_date TIMESTAMP(3),
    reported_time TIMESTAMP(3),
    reported_by BIGINT,
    service_agent_id BIGINT,
    priority_id BIGINT,
    expected_resolution_date TIMESTAMP(3),
    expected_resolution_time TIMESTAMP(3),
    eqp_machine_status BIGINT,
    supplier_id BIGINT,
    store_id BIGINT,
    warranty_type_id BIGINT,
    description TEXT,
    CONSTRAINT pk_equipment_complaint_details PRIMARY KEY (id)
);
CREATE INDEX IF NOT EXISTS ix_equipment_complaint_details_complaint ON ihms_equipment.equipment_complaint_details (complaint_id);
COMMENT ON TABLE ihms_equipment.equipment_complaint_details IS 'Clean target of T_eqpComplaintDetails. Per-line asset snapshot (item/serial/model) + reporting context; eqp_machine_status holds an equipment_working_status_master id at report time.';

-- ---------------------------------------------------------------------------
-- equipment_complaint_resolution — resolution record applied to a complaint
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ihms_equipment.equipment_complaint_resolution (
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
    complaint_id BIGINT NOT NULL REFERENCES ihms_equipment.equipment_complaint (id),
    resolve_no VARCHAR(64),
    resolution_date TIMESTAMP(3),
    resolved_by BIGINT,
    resolution_notes TEXT,
    parts_replaced TEXT,
    service_agent_id BIGINT,
    assigned_to BIGINT,
    assigned_by_id BIGINT,
    resolved_status BIGINT CHECK (resolved_status BETWEEN 1 AND 4),
    CONSTRAINT pk_equipment_complaint_resolution PRIMARY KEY (id)
);
CREATE INDEX IF NOT EXISTS ix_equipment_complaint_resolution_complaint ON ihms_equipment.equipment_complaint_resolution (complaint_id);
CREATE INDEX IF NOT EXISTS ix_equipment_complaint_resolution_date ON ihms_equipment.equipment_complaint_resolution (resolution_date);
COMMENT ON TABLE ihms_equipment.equipment_complaint_resolution IS 'Clean target of t_eqpcomplaintresolution. resolution_notes maps legacy REMARKS; parts_replaced is a free-text summary (structured part lines live in equipment_complaint_resolution_details); resolved_status -> complaint_status_master id the complaint moves to.';

-- ---------------------------------------------------------------------------
-- equipment_complaint_resolution_details — resolution line items (replaced
-- parts / engineer visits; legacy agent-detail row)
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ihms_equipment.equipment_complaint_resolution_details (
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
    resolution_id BIGINT NOT NULL REFERENCES ihms_equipment.equipment_complaint_resolution (id),
    engineer_id BIGINT,
    item_id BIGINT,
    batch_id BIGINT,
    batch_code VARCHAR(64),
    expiry_date TIMESTAMP(3),
    quantity NUMERIC(19, 2),
    cost NUMERIC(19, 2),
    total_amount NUMERIC(19, 2),
    store_id BIGINT,
    CONSTRAINT pk_equipment_complaint_resolution_details PRIMARY KEY (id)
);
CREATE INDEX IF NOT EXISTS ix_equipment_complaint_res_details_resolution ON ihms_equipment.equipment_complaint_resolution_details (resolution_id);
COMMENT ON TABLE ihms_equipment.equipment_complaint_resolution_details IS 'Clean target of t_eqpcomplaintresolutionagentdetail. Replaced-part lines: item + batch/expiry traceability, quantity/cost/total_amount NUMERIC(19,2).';

-- -------------------------------------------------------------- sequences --
-- Explicit sequences per migrated table (IMP-DEC-003; no BIGSERIAL).
-- equipment_complaint_no_seq drives the IHMS/EQ/{yyyy}/{%05d} complaint number
-- (same split PK-seq vs number-seq pattern as grievance-service).

CREATE SEQUENCE IF NOT EXISTS ihms_equipment.equipment_complaint_seq                   START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS ihms_equipment.equipment_complaint_no_seq                START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS ihms_equipment.equipment_complaint_details_seq           START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS ihms_equipment.equipment_complaint_resolution_seq        START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS ihms_equipment.equipment_complaint_resolution_details_seq START WITH 1 INCREMENT BY 1;
