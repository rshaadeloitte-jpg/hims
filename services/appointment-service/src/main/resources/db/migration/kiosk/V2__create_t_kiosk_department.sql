-- ============================================================================
-- V2 — Create ihms_kiosk.t_kiosk_department (M31 self-appointment department picker)
-- ============================================================================
CREATE TABLE IF NOT EXISTS ihms_kiosk.t_kiosk_department (
    id BIGINT NOT NULL,
    name VARCHAR(120) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT pk_t_kiosk_department PRIMARY KEY (id)
);
