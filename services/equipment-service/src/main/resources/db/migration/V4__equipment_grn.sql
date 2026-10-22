CREATE TABLE IF NOT EXISTS equipment_grn (
    id BIGSERIAL PRIMARY KEY,
    grn_no VARCHAR(64) UNIQUE,
    equipment_id BIGINT,
    supplier_id BIGINT,
    grn_date DATE,
    quantity INTEGER DEFAULT 1,
    status VARCHAR(32) DEFAULT 'PENDING',
    remarks TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);