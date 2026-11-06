CREATE TABLE IF NOT EXISTS equipment_scrap (
    id BIGSERIAL PRIMARY KEY,
    equipment_id BIGINT,
    scrap_date DATE,
    scrap_value NUMERIC(12,2),
    buyer VARCHAR(255),
    status VARCHAR(32) DEFAULT 'PENDING',
    remarks TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS equipment_med_gas_indent (
    id BIGSERIAL PRIMARY KEY,
    indent_no VARCHAR(64) UNIQUE,
    department_id BIGINT,
    gas_type VARCHAR(64),
    quantity INTEGER DEFAULT 1,
    status VARCHAR(32) DEFAULT 'PENDING',
    requested_by BIGINT,
    approved_by BIGINT,
    remarks TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);