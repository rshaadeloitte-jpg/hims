-- MIG-023 sub-slice (S16): linen & laundry request lifecycle — clean target schema.
-- Single-writer ownership: linen-service is the sole writer of ihms_linen
-- (same extraction pattern as ihms_item / ihms_grievance / ihms_waste,
-- IMP-DEC-026/032/033).
--
-- Legacy sources (evidence authority, /mnt/d/ihms read-only):
--   T_LINENREQUEST         -> linen_request          (LinenRequest.java)
--   T_LINENREQUESTDETAILS  -> linen_request_details  (LinenRequestDetails.java)
--
-- Status dictionary (legacy LinenRequestStatus enum, LinenLaundryServiceImpl):
--   1=PENDING 2=ACCEPTED 3=HANDOVERED 4=RECEIVED 5=WASHED 6=DISPATCHED 7=COMPLETED
-- Requisition number rule (legacy linen.no.template=%s/LR/%d/%s +
-- getLatestAutocodeSeq on activity 'T_LinenRequest'):
--   {unitPrefix}/LR/{year-2000}/{%05d seq}.
--
-- Cross-domain references deferred (dependency-neutral): details.item_id /
-- details.batch_id point at ihms_item (item-service) — no FK until the
-- item-service integration slice; getItemsForLinenRequest (item read) and
-- updateItemStockForDamagedQty (stock write on complete) are deferred.

CREATE SCHEMA IF NOT EXISTS ihms_linen;

-- ------------------------------------------------------------- requests --

CREATE TABLE IF NOT EXISTS ihms_linen.linen_request (
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
    requisition_no VARCHAR(64) NOT NULL,
    store_id BIGINT,
    to_store_id BIGINT,
    request_date TIMESTAMP(3),
    request_status BIGINT NOT NULL DEFAULT 1,
    handed_over_to VARCHAR(255),
    received_by VARCHAR(255),
    CONSTRAINT pk_linen_request PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS ix_linen_request_unit ON ihms_linen.linen_request (unit_id);
CREATE INDEX IF NOT EXISTS ix_linen_request_store ON ihms_linen.linen_request (store_id);
CREATE INDEX IF NOT EXISTS ix_linen_request_to_store ON ihms_linen.linen_request (to_store_id);
CREATE INDEX IF NOT EXISTS ix_linen_request_req_date ON ihms_linen.linen_request (request_date);
CREATE INDEX IF NOT EXISTS ix_linen_request_req_status ON ihms_linen.linen_request (request_status);

CREATE TABLE IF NOT EXISTS ihms_linen.linen_request_details (
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
    linen_request_id BIGINT NOT NULL REFERENCES ihms_linen.linen_request (id),
    item_id BIGINT,
    batch_id BIGINT,
    quantity BIGINT NOT NULL DEFAULT 0,
    remark VARCHAR(255),
    received_quantity BIGINT,
    damaged_quantity BIGINT,
    dispatched_quantity BIGINT,
    final_quantity BIGINT,
    reason VARCHAR(255) DEFAULT '',
    CONSTRAINT pk_linen_request_details PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS ix_linen_details_request ON ihms_linen.linen_request_details (linen_request_id);
CREATE INDEX IF NOT EXISTS ix_linen_details_item ON ihms_linen.linen_request_details (item_id);

-- -------------------------------------------------------------- sequences --
-- Explicit sequences per migrated table (IMP-DEC-003; no BIGSERIAL).

CREATE SEQUENCE IF NOT EXISTS ihms_linen.linen_request_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS ihms_linen.linen_request_details_seq START WITH 1 INCREMENT BY 1;
-- Requisition-number sequence (legacy CIMS_GETLATESTAUTOCODEANDINCREMETITBYONE
-- on activity 'T_LinenRequest').
CREATE SEQUENCE IF NOT EXISTS ihms_linen.linen_request_req_no_seq START WITH 1 INCREMENT BY 1;
