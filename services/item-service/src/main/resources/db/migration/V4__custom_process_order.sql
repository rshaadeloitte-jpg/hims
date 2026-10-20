-- ============================================================================
-- V4 — M13 custom process orders (CPO): one-off cross-store purchase request to
-- an external/central supplier with partial-receive semantics.
-- doc_status: 1=PENDING 2=APPROVED 3=PARTIALLY_RECEIVED 4=RECEIVED 5=CANCELLED
-- ============================================================================
SET search_path TO ihms_item, public;
CREATE TABLE IF NOT EXISTS ihms_item.t_custom_process_order (
    id BIGINT NOT NULL, unit_id BIGINT, status BOOLEAN NOT NULL DEFAULT TRUE,
    created_unit_id BIGINT, updated_unit_id BIGINT, added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3), synchronized BOOLEAN NOT NULL DEFAULT FALSE,
    cpo_no VARCHAR(64) NOT NULL,
    store_id BIGINT NOT NULL REFERENCES ihms_item.store (id),
    supplier_id BIGINT REFERENCES ihms_item.supplier (id),
    dept_id BIGINT,
    request_date DATE,
    required_date DATE,
    remarks VARCHAR(500),
    doc_status SMALLINT NOT NULL DEFAULT 1 CHECK (doc_status BETWEEN 1 AND 5),
    approved_by BIGINT,
    approved_datetime TIMESTAMP(3),
    CONSTRAINT pk_t_custom_process_order PRIMARY KEY (id),
    CONSTRAINT uq_t_custom_process_order_no UNIQUE (cpo_no)
);
COMMENT ON COLUMN ihms_item.t_custom_process_order.doc_status IS '1=PENDING 2=APPROVED 3=PARTIALLY_RECEIVED 4=RECEIVED 5=CANCELLED.';
CREATE INDEX IF NOT EXISTS ix_t_custom_process_order_store ON ihms_item.t_custom_process_order (store_id, doc_status);
CREATE INDEX IF NOT EXISTS ix_t_custom_process_order_supplier ON ihms_item.t_custom_process_order (supplier_id);

CREATE TABLE IF NOT EXISTS ihms_item.t_custom_process_order_detail (
    id BIGINT NOT NULL, unit_id BIGINT, status BOOLEAN NOT NULL DEFAULT TRUE,
    created_unit_id BIGINT, updated_unit_id BIGINT, added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3), synchronized BOOLEAN NOT NULL DEFAULT FALSE,
    cpo_id BIGINT NOT NULL REFERENCES ihms_item.t_custom_process_order (id),
    item_id BIGINT NOT NULL REFERENCES ihms_item.item (id),
    batch_id BIGINT REFERENCES ihms_item.item_batch (id),
    ordered_qty NUMERIC(19,4) NOT NULL CHECK (ordered_qty > 0),
    received_qty NUMERIC(19,4) NOT NULL DEFAULT 0 CHECK (received_qty >= 0),
    unit_price NUMERIC(19,2),
    remarks VARCHAR(500),
    CONSTRAINT pk_t_custom_process_order_detail PRIMARY KEY (id)
);
CREATE INDEX IF NOT EXISTS ix_t_custom_process_order_detail_hdr ON ihms_item.t_custom_process_order_detail (cpo_id);
CREATE INDEX IF NOT EXISTS ix_t_custom_process_order_detail_item ON ihms_item.t_custom_process_order_detail (item_id);

CREATE SEQUENCE IF NOT EXISTS ihms_item.t_custom_process_order_seq START WITH 1;
CREATE SEQUENCE IF NOT EXISTS ihms_item.t_custom_process_order_detail_seq START WITH 1;
