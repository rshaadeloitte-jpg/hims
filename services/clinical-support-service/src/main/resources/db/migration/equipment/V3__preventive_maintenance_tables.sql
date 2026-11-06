-- ============================================================================
-- V3 — preventive maintenance tables (Equipment Management service, S18) —
-- MIG-023 Phase 3. Clean target of the legacy PM cluster. Single-writer
-- ownership: equipment-service is the sole writer of ihms_equipment.
--
-- Legacy sources (evidence authority, /mnt/d/ihms read-only):
--   T_EqpPMSchedule            -> eqp_pm_schedule             (EqpPMSchedule.java)
--   T_EqpPMScheduleDetails     -> eqp_pm_schedule_details     (EqpPMScheduleDetails.java)
--   T_EqpPreventiveMaintenance -> eqp_preventive_maintenance  (EqpPreventiveMaintenance.java)
--
-- Spec additions over legacy (Task 9 contract, documented not invented):
--   - schedule_code: human-facing schedule reference (uq partial index, same
--     pattern as uq_equipment_complaint_no);
--   - next_due_date / last_completed_date: due-listing + completion
--     bookkeeping (legacy derived these on the fly from PMDATE + FREQUENCY
--     + month flags);
--   - pm_type on the schedule and performed_by / remarks / pm_status on the
--     completion record; pm_type -> maintenance_type_master id (V1 seed,
--     ids ARE the codes: 1=PREVENTIVE 2=CORRECTIVE 3=CALIBRATION).
--
-- Deferred long-tail (documented not dropped):
--   - legacy PMSCHEDULEUNITID / PMSCHEDULEDETAILUNITID denormalised unit
--     copies (unit_id already sits on the base audit block of every row);
--   - legacy SERVICEAGENTID typed BigDecimal in T_EqpPreventiveMaintenance
--     (agent ids are BIGINT everywhere else — corrected here);
--   - T_EqpPreventiveMaintenance has no schedule FK in legacy (only
--     PMSCHEDULEDETAILID) — the clean table adds a nullable pm_schedule_id
--     so ad-hoc completions remain legal while history can join to the plan.
-- ============================================================================

-- ---------------------------------------------------------------------------
-- eqp_pm_schedule — recurring preventive-maintenance plan per asset.
-- Legacy T_EqpPMSchedule: PMDATE plan start, FREQUENCY = months between
-- services, JAN..DEC month-of-year flags pick which months a service falls
-- in; STOREID snapshot carried alongside the asset's current department.
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ihms_equipment.eqp_pm_schedule (
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
    schedule_code VARCHAR(64),
    frequency BIGINT,
    pm_date TIMESTAMP(3),
    jan BOOLEAN NOT NULL DEFAULT FALSE,
    feb BOOLEAN NOT NULL DEFAULT FALSE,
    mar BOOLEAN NOT NULL DEFAULT FALSE,
    apr BOOLEAN NOT NULL DEFAULT FALSE,
    may BOOLEAN NOT NULL DEFAULT FALSE,
    jun BOOLEAN NOT NULL DEFAULT FALSE,
    jul BOOLEAN NOT NULL DEFAULT FALSE,
    aug BOOLEAN NOT NULL DEFAULT FALSE,
    sep BOOLEAN NOT NULL DEFAULT FALSE,
    oct BOOLEAN NOT NULL DEFAULT FALSE,
    nov BOOLEAN NOT NULL DEFAULT FALSE,
    dec BOOLEAN NOT NULL DEFAULT FALSE,
    next_due_date TIMESTAMP(3),
    last_completed_date TIMESTAMP(3),
    pm_type BIGINT NOT NULL DEFAULT 1 CHECK (pm_type BETWEEN 1 AND 3),
    store_id BIGINT,
    CONSTRAINT pk_eqp_pm_schedule PRIMARY KEY (id)
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_eqp_pm_schedule_code
    ON ihms_equipment.eqp_pm_schedule (schedule_code) WHERE schedule_code IS NOT NULL;
CREATE INDEX IF NOT EXISTS ix_eqp_pm_schedule_equipment ON ihms_equipment.eqp_pm_schedule (equipment_id);
CREATE INDEX IF NOT EXISTS ix_eqp_pm_schedule_due ON ihms_equipment.eqp_pm_schedule (next_due_date);
CREATE INDEX IF NOT EXISTS ix_eqp_pm_schedule_unit ON ihms_equipment.eqp_pm_schedule (unit_id, pm_type);
COMMENT ON TABLE ihms_equipment.eqp_pm_schedule IS 'Clean target of T_EqpPMSchedule. frequency = months between services; jan..dec legacy month flags; pm_date legacy PMDATE start; next_due_date/last_completed_date are spec Task 9 additions for due-listing; pm_type -> maintenance_type_master id (V1 seed 1=PREVENTIVE 2=CORRECTIVE 3=CALIBRATION); schedule_code unique per partial index uq_eqp_pm_schedule_code.';

-- ---------------------------------------------------------------------------
-- eqp_pm_schedule_details — planned occurrence line items of a schedule.
-- Legacy T_EqpPMScheduleDetails: DATE planned occurrence and
-- ISPREVENTIVEMAINTENANCE flag (schedules may carry corrective visits too).
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ihms_equipment.eqp_pm_schedule_details (
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
    pm_schedule_id BIGINT NOT NULL REFERENCES ihms_equipment.eqp_pm_schedule (id),
    scheduled_date TIMESTAMP(3),
    is_preventive_maintenance BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT pk_eqp_pm_schedule_details PRIMARY KEY (id)
);
CREATE INDEX IF NOT EXISTS ix_eqp_pm_schedule_details_schedule ON ihms_equipment.eqp_pm_schedule_details (pm_schedule_id);
CREATE INDEX IF NOT EXISTS ix_eqp_pm_schedule_details_date ON ihms_equipment.eqp_pm_schedule_details (scheduled_date);
COMMENT ON TABLE ihms_equipment.eqp_pm_schedule_details IS 'Clean target of T_EqpPMScheduleDetails. One planned occurrence per line: scheduled_date maps legacy DATE; is_preventive_maintenance maps legacy ISPREVENTIVEMAINTENANCE.';

-- ---------------------------------------------------------------------------
-- eqp_preventive_maintenance — completed maintenance record (PM/corrective/
-- calibration execution). Legacy T_EqpPreventiveMaintenance carried the full
-- checklist-result block (functional / preventive / electrical-safety remarks
-- + calibration note), sign-offs and cost.
-- pm_status is the execution lifecycle: ids ARE the codes
-- 1=SCHEDULED 2=IN_PROGRESS 3=COMPLETED 4=CANCELLED (CHECK-enforced like the
-- complaint_status/resolved_status columns in V2).
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ihms_equipment.eqp_preventive_maintenance (
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
    pm_schedule_id BIGINT REFERENCES ihms_equipment.eqp_pm_schedule (id),
    pm_schedule_detail_id BIGINT,
    item_id BIGINT,
    maintenance_no VARCHAR(64),
    maintenance_date TIMESTAMP(3),
    performed_by BIGINT,
    functional_check_remark TEXT,
    preventive_maintenance_remark TEXT,
    electrical_safety_remark TEXT,
    calibration TEXT,
    further_actions TEXT,
    comments_on_use TEXT,
    remarks TEXT,
    dept_in_charge BIGINT,
    biomedical_engineer_id BIGINT,
    biomedical_in_charge_id BIGINT,
    verified_by BIGINT,
    amount_incurred NUMERIC(19, 2),
    service_agent_id BIGINT,
    pm_status BIGINT NOT NULL DEFAULT 1 CHECK (pm_status BETWEEN 1 AND 4),
    CONSTRAINT pk_eqp_preventive_maintenance PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS ix_eqp_preventive_maintenance_schedule ON ihms_equipment.eqp_preventive_maintenance (pm_schedule_id);
CREATE INDEX IF NOT EXISTS ix_eqp_preventive_maintenance_equipment ON ihms_equipment.eqp_preventive_maintenance (equipment_id);
CREATE INDEX IF NOT EXISTS ix_eqp_preventive_maintenance_date ON ihms_equipment.eqp_preventive_maintenance (maintenance_date);
COMMENT ON TABLE ihms_equipment.eqp_preventive_maintenance IS 'Clean target of T_EqpPreventiveMaintenance. Checklist results map legacy FUNCTIONALCHECKREMARK/PREVENTIVEMAINTENANCEREMARK/ELECTRICALSAFETYREMARK/CALIBRATION (all TEXT); remarks is the spec Task 9 generic notes column alongside legacy FURTHERACTIONS/COMMENTSONUSE; performed_by maps legacy BIOMEDICALENGINEERID; amount_incurred NUMERIC(19,2); service_agent_id corrected from legacy BigDecimal to BIGINT; pm_status CHECK 1..4 (1=SCHEDULED 2=IN_PROGRESS 3=COMPLETED 4=CANCELLED).';

-- -------------------------------------------------------------- sequences --
-- Explicit sequences per migrated table (IMP-DEC-003; no BIGSERIAL).

CREATE SEQUENCE IF NOT EXISTS ihms_equipment.eqp_pm_schedule_seq            START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS ihms_equipment.eqp_pm_schedule_details_seq    START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS ihms_equipment.eqp_preventive_maintenance_seq START WITH 1 INCREMENT BY 1;
