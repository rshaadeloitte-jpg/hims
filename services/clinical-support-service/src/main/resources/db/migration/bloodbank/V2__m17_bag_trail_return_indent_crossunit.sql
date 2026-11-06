-- ============================================================================
-- V2 — M17 blood bank gap (legacy BloodBankServiceImpl):
--   * bag trail (audit log per bag — collected, crossmatched, issued, returned,
--     discarded, separated)
--   * bag return (return-to-supplier / quarantine flow)
--   * bag discard (write-off for unsalvageable bags)
--   * blood-bank indent (inter-store component request)
--   * cross-unit orders (inter-blood-bank transfer)
-- Status dictionaries locked at this version:
--   t_bloodbag_trail.event_type: 1=COLLECTED 2=CROSSMATCHED 3=ISSUED
--                                4=RETURNED 5=DISCARDED 6=SEPARATED
--                                7=INDENT_REQUESTED 8=INDENT_APPROVED
--                                9=INDENT_ISSUED 10=INDENT_CANCELLED
--                                11=CROSS_UNIT_REQUESTED 12=CROSS_UNIT_RECEIVED
--   t_bloodbag_return.doc_status:  1=PENDING 2=RETURNED 3=REJECTED
--   t_bloodbag_discard.doc_status: 1=PENDING 2=DISCARDED 3=REJECTED
--   t_bloodbank_indent.doc_status: 1=NEW 2=APPROVED 3=ISSUED 4=CANCELLED
--   t_bloodbank_cross_unit.doc_status: 1=REQUESTED 2=ACKNOWLEDGED 3=RECEIVED 4=CANCELLED
-- All schema is idempotent and uses explicit sequences (IMP-DEC-003).
-- ============================================================================
SET search_path TO ihms_bloodbank, public;

-- ---------------------------------------------------------------------------
-- t_bloodbag_trail — append-only audit log per bag.
-- bag_id is NOT a hard FK because the trail must survive bag soft-delete;
-- the unique combination (bag_id, event_type, event_at) prevents double-logs
-- for the same event-class in the same instant.
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ihms_bloodbank.t_bloodbag_trail (
    id BIGINT NOT NULL, unit_id BIGINT, status BOOLEAN NOT NULL DEFAULT TRUE,
    created_unit_id BIGINT, updated_unit_id BIGINT, added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3), synchronized BOOLEAN NOT NULL DEFAULT FALSE,
    bag_id BIGINT NOT NULL,
    event_type SMALLINT NOT NULL CHECK (event_type BETWEEN 1 AND 12),
    event_at TIMESTAMP(3) NOT NULL,
    actor_id BIGINT,
    ref_doc_type VARCHAR(32),
    ref_doc_id BIGINT,
    remarks VARCHAR(500),
    CONSTRAINT pk_t_bloodbag_trail PRIMARY KEY (id)
);
CREATE INDEX IF NOT EXISTS ix_t_bloodbag_trail_bag ON ihms_bloodbank.t_bloodbag_trail (bag_id, event_at);
CREATE INDEX IF NOT EXISTS ix_t_bloodbag_trail_event_type ON ihms_bloodbank.t_bloodbag_trail (event_type, event_at);
COMMENT ON TABLE ihms_bloodbank.t_bloodbag_trail IS 'Append-only audit log of bag events (M17 bag trail). Survives bag soft-delete; bag_id has no hard FK.';

-- ---------------------------------------------------------------------------
-- t_bloodbag_return — return-to-supplier / quarantine
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ihms_bloodbank.t_bloodbag_return (
    id BIGINT NOT NULL, unit_id BIGINT, status BOOLEAN NOT NULL DEFAULT TRUE,
    created_unit_id BIGINT, updated_unit_id BIGINT, added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3), synchronized BOOLEAN NOT NULL DEFAULT FALSE,
    return_no VARCHAR(64) NOT NULL,
    bag_id BIGINT NOT NULL,
    reason VARCHAR(500),
    returned_to VARCHAR(200),
    doc_status SMALLINT NOT NULL DEFAULT 1 CHECK (doc_status BETWEEN 1 AND 3),
    approved_by BIGINT,
    approved_datetime TIMESTAMP(3),
    completed_at TIMESTAMP(3),
    CONSTRAINT pk_t_bloodbag_return PRIMARY KEY (id),
    CONSTRAINT uq_t_bloodbag_return_no UNIQUE (return_no)
);
CREATE INDEX IF NOT EXISTS ix_t_bloodbag_return_bag ON ihms_bloodbank.t_bloodbag_return (bag_id);
CREATE INDEX IF NOT EXISTS ix_t_bloodbag_return_status ON ihms_bloodbank.t_bloodbag_return (doc_status);
COMMENT ON TABLE ihms_bloodbank.t_bloodbag_return IS 'Bag return / quarantine (M17). doc_status 1=PENDING 2=RETURNED 3=REJECTED.';

-- ---------------------------------------------------------------------------
-- t_bloodbag_discard — write-off for unsalvageable bags
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ihms_bloodbank.t_bloodbag_discard (
    id BIGINT NOT NULL, unit_id BIGINT, status BOOLEAN NOT NULL DEFAULT TRUE,
    created_unit_id BIGINT, updated_unit_id BIGINT, added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3), synchronized BOOLEAN NOT NULL DEFAULT FALSE,
    discard_no VARCHAR(64) NOT NULL,
    bag_id BIGINT NOT NULL,
    reason VARCHAR(500) NOT NULL,
    doc_status SMALLINT NOT NULL DEFAULT 1 CHECK (doc_status BETWEEN 1 AND 3),
    approved_by BIGINT,
    approved_datetime TIMESTAMP(3),
    completed_at TIMESTAMP(3),
    CONSTRAINT pk_t_bloodbag_discard PRIMARY KEY (id),
    CONSTRAINT uq_t_bloodbag_discard_no UNIQUE (discard_no)
);
CREATE INDEX IF NOT EXISTS ix_t_bloodbag_discard_bag ON ihms_bloodbank.t_bloodbag_discard (bag_id);
CREATE INDEX IF NOT EXISTS ix_t_bloodbag_discard_status ON ihms_bloodbank.t_bloodbag_discard (doc_status);
COMMENT ON TABLE ihms_bloodbank.t_bloodbag_discard IS 'Bag discard / write-off (M17). doc_status 1=PENDING 2=DISCARDED 3=REJECTED.';

-- ---------------------------------------------------------------------------
-- t_bloodbank_indent — inter-store component request
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ihms_bloodbank.t_bloodbank_indent (
    id BIGINT NOT NULL, unit_id BIGINT, status BOOLEAN NOT NULL DEFAULT TRUE,
    created_unit_id BIGINT, updated_unit_id BIGINT, added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3), synchronized BOOLEAN NOT NULL DEFAULT FALSE,
    indent_no VARCHAR(64) NOT NULL,
    from_store_id BIGINT NOT NULL,
    to_store_id BIGINT NOT NULL,
    blood_group VARCHAR(5) NOT NULL,
    component VARCHAR(100) NOT NULL,
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    requested_date DATE NOT NULL,
    remarks VARCHAR(500),
    doc_status SMALLINT NOT NULL DEFAULT 1 CHECK (doc_status BETWEEN 1 AND 4),
    approved_by BIGINT,
    approved_datetime TIMESTAMP(3),
    issued_at TIMESTAMP(3),
    CONSTRAINT pk_t_bloodbank_indent PRIMARY KEY (id),
    CONSTRAINT uq_t_bloodbank_indent_no UNIQUE (indent_no),
    CONSTRAINT ck_t_bloodbank_indent_stores CHECK (from_store_id <> to_store_id)
);
CREATE INDEX IF NOT EXISTS ix_t_bloodbank_indent_to_status ON ihms_bloodbank.t_bloodbank_indent (to_store_id, doc_status);
CREATE INDEX IF NOT EXISTS ix_t_bloodbank_indent_from_status ON ihms_bloodbank.t_bloodbank_indent (from_store_id, doc_status);
COMMENT ON TABLE ihms_bloodbank.t_bloodbank_indent IS 'Inter-store component indent (M17). doc_status 1=NEW 2=APPROVED 3=ISSUED 4=CANCELLED.';

-- ---------------------------------------------------------------------------
-- t_bloodbank_cross_unit — inter-blood-bank transfer
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ihms_bloodbank.t_bloodbank_cross_unit (
    id BIGINT NOT NULL, unit_id BIGINT, status BOOLEAN NOT NULL DEFAULT TRUE,
    created_unit_id BIGINT, updated_unit_id BIGINT, added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3), synchronized BOOLEAN NOT NULL DEFAULT FALSE,
    request_no VARCHAR(64) NOT NULL,
    from_bloodbank_id BIGINT NOT NULL,
    to_bloodbank_id BIGINT NOT NULL,
    blood_group VARCHAR(5) NOT NULL,
    component VARCHAR(100) NOT NULL,
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    remarks VARCHAR(500),
    doc_status SMALLINT NOT NULL DEFAULT 1 CHECK (doc_status BETWEEN 1 AND 4),
    acknowledged_by BIGINT,
    acknowledged_datetime TIMESTAMP(3),
    received_by BIGINT,
    received_datetime TIMESTAMP(3),
    CONSTRAINT pk_t_bloodbank_cross_unit PRIMARY KEY (id),
    CONSTRAINT uq_t_bloodbank_cross_unit_no UNIQUE (request_no),
    CONSTRAINT ck_t_bloodbank_cross_unit_units CHECK (from_bloodbank_id <> to_bloodbank_id)
);
CREATE INDEX IF NOT EXISTS ix_t_bloodbank_cross_unit_to_status ON ihms_bloodbank.t_bloodbank_cross_unit (to_bloodbank_id, doc_status);
CREATE INDEX IF NOT EXISTS ix_t_bloodbank_cross_unit_from_status ON ihms_bloodbank.t_bloodbank_cross_unit (from_bloodbank_id, doc_status);
COMMENT ON TABLE ihms_bloodbank.t_bloodbank_cross_unit IS 'Cross-unit (inter-blood-bank) order (M17). doc_status 1=REQUESTED 2=ACKNOWLEDGED 3=RECEIVED 4=CANCELLED.';

-- ---------------------------------------------------------------------------
-- Sequences (IMP-DEC-003)
-- ---------------------------------------------------------------------------
CREATE SEQUENCE IF NOT EXISTS ihms_bloodbank.t_bloodbag_trail_seq       START WITH 1;
CREATE SEQUENCE IF NOT EXISTS ihms_bloodbank.t_bloodbag_return_seq      START WITH 1;
CREATE SEQUENCE IF NOT EXISTS ihms_bloodbank.t_bloodbag_discard_seq     START WITH 1;
CREATE SEQUENCE IF NOT EXISTS ihms_bloodbank.t_bloodbank_indent_seq     START WITH 1;
CREATE SEQUENCE IF NOT EXISTS ihms_bloodbank.t_bloodbank_cross_unit_seq START WITH 1;
