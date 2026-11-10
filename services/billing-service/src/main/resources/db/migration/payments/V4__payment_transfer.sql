CREATE SEQUENCE IF NOT EXISTS payment_transfer_seq START WITH 1;

CREATE TABLE IF NOT EXISTS t_payment_transfer (
    id BIGINT PRIMARY KEY DEFAULT nextval('payment_transfer_seq'),
    beneficiary_name VARCHAR(255) NOT NULL,
    beneficiary_account_no VARCHAR(64) NOT NULL,
    beneficiary_ifsc VARCHAR(32) NOT NULL,
    amount DECIMAL(12,2) NOT NULL,
    scheme VARCHAR(100) NOT NULL,
    purpose VARCHAR(255) NOT NULL,
    remarks VARCHAR(500),
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    utr_no VARCHAR(100),
    failure_reason VARCHAR(500),
    otp VARCHAR(16),
    otp_expires_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_payment_transfer_status ON t_payment_transfer(status);
CREATE INDEX IF NOT EXISTS idx_payment_transfer_scheme ON t_payment_transfer(scheme);
CREATE INDEX IF NOT EXISTS idx_payment_transfer_created ON t_payment_transfer(created_at);