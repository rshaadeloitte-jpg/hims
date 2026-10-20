-- ============================================================================
-- V2 — M27 department-specific queues (rad/patho) + cabin board
-- Ported from legacy QueueManagmentServiceImpl department queues:
--   getPatientListForQueue (dept), departmentCabinRoomMapping, cabin queue boards
-- Counter.department_id already exists (V1); queue_token.department_id already D-1.
-- This migration adds indexes and seeds for RADIOLOGY/PATHOLOGY dept queues.
-- ponytail: no new table; reuse counter + queue_token with dept filter.
-- ============================================================================

SET search_path TO ihms_queue, public;

-- dept-specific indexes (rad/patho dashboard scans)
CREATE INDEX IF NOT EXISTS ix_queue_token_dept_date_status
    ON ihms_queue.queue_token (department_id, queue_date, queue_status)
    WHERE status = TRUE;
CREATE INDEX IF NOT EXISTS ix_queue_counter_dept_status
    ON ihms_queue.counter (department_id, status);

-- seed example counters for department-specific queues (idempotent)
-- dept 9001 = RADIOLOGY, 9002 = PATHOLOGY (mirrors legacy departmentMaster ids)
INSERT INTO ihms_queue.counter (id, code, name, department_id, status, added_by, added_datetime, synchronized)
VALUES
    (9001, 'RAD-01', 'Radiology Counter 1', 9001, TRUE, 1, NOW(), FALSE),
    (9002, 'PATH-01', 'Pathology Counter 1', 9002, TRUE, 1, NOW(), FALSE)
ON CONFLICT (id) DO NOTHING;

-- ensure counter_seq advances past seeded ids
SELECT setval('ihms_queue."counter_seq"', GREATEST((SELECT COALESCE(MAX(id),0) FROM ihms_queue.counter), 9002) + 1, false);
