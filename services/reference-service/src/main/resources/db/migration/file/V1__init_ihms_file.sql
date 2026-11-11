-- ============================================================================
-- V1 — ihms_file schema (File/Document Management service, M34)
-- Clean target of legacy FileDetails (M_FILEDETAILS) + FileStore (M_FILESTORE)
-- + ImageAnnotationMaster, HospitalGeoLocationMapping (annotation).
-- ONE-TABLE-ONE-OWNER: ALL tables here belong to the file service.
-- REST-only: no outbox until file events have a second consumer.
-- ============================================================================

CREATE SCHEMA IF NOT EXISTS ihms_file;

SET search_path TO ihms_file, public;

-- ---------------------------------------------------------------------------
-- t_file_category — clean target of M_FILESTORE category tree
-- (FileManagementServiceImpl: addDocumentCategory / getFileCategoryTree)
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ihms_file.t_file_category (
    id BIGSERIAL PRIMARY KEY,
    parent_id BIGINT REFERENCES ihms_file.t_file_category (id),
    code VARCHAR(64) NOT NULL,
    name VARCHAR(255) NOT NULL,
    document_type VARCHAR(50), -- e.g. PATIENT, LAB, GRIEVANCE, EQUIPMENT, GENERIC
    file_path VARCHAR(500),
    status BOOLEAN NOT NULL DEFAULT TRUE,
    added_by BIGINT,
    added_datetime TIMESTAMP(3),
    updated_by BIGINT,
    updated_datetime TIMESTAMP(3),
    CONSTRAINT uq_file_category_code UNIQUE (code)
);
CREATE INDEX IF NOT EXISTS ix_file_category_parent ON ihms_file.t_file_category (parent_id);
CREATE INDEX IF NOT EXISTS ix_file_category_doc_type ON ihms_file.t_file_category (document_type);
CREATE INDEX IF NOT EXISTS ix_file_category_status ON ihms_file.t_file_category (status);
COMMENT ON TABLE ihms_file.t_file_category IS 'File category tree (ported from M_FILESTORE). parent_id null = root.';

-- ---------------------------------------------------------------------------
-- t_file_details — clean target of M_FILEDETAILS (file token + content)
-- file_token is the download key (UUID), file_content is BYTEA (ponytail: PG bytea until MinIO needed)
-- owner_type/owner_id link to patient/lab/grievance/equipment (generic FK)
-- annotation_json holds annotated image view data (ImageAnnotationMaster parity)
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ihms_file.t_file_details (
    id BIGSERIAL PRIMARY KEY,
    file_category_id BIGINT REFERENCES ihms_file.t_file_category (id),
    owner_type VARCHAR(50), -- PATIENT, LAB, GRIEVANCE, EQUIPMENT, etc.
    owner_id BIGINT,
    file_name VARCHAR(255) NOT NULL,
    file_type VARCHAR(100) NOT NULL,
    file_size BIGINT NOT NULL,
    file_token VARCHAR(100) NOT NULL,
    file_content BYTEA NOT NULL,
    file_url VARCHAR(500),
    annotation_json TEXT, -- JSON for annotated image view (ImageAnnotationMaster)
    status BOOLEAN NOT NULL DEFAULT TRUE,
    added_by BIGINT,
    added_datetime TIMESTAMP(3),
    updated_by BIGINT,
    updated_datetime TIMESTAMP(3),
    CONSTRAINT uq_file_details_token UNIQUE (file_token)
);
CREATE INDEX IF NOT EXISTS ix_file_details_category ON ihms_file.t_file_details (file_category_id);
CREATE INDEX IF NOT EXISTS ix_file_details_owner ON ihms_file.t_file_details (owner_type, owner_id);
CREATE INDEX IF NOT EXISTS ix_file_details_status ON ihms_file.t_file_details (status);
COMMENT ON TABLE ihms_file.t_file_details IS 'File blobs (ported from M_FILEDETAILS). file_token is download key; file_content BYTEA; annotation_json for image annotation view.';

-- ---------------------------------------------------------------------------
-- Seed baseline categories (mirrors legacy FileStore defaults)
-- ---------------------------------------------------------------------------
INSERT INTO ihms_file.t_file_category (code, name, document_type, status, added_by, added_datetime)
VALUES
    ('PATIENT_DOCS', 'Patient Documents', 'PATIENT', TRUE, 1, NOW()),
    ('LAB_DOCS', 'Lab Documents', 'LAB', TRUE, 1, NOW()),
    ('GRIEVANCE_DOCS', 'Grievance Documents', 'GRIEVANCE', TRUE, 1, NOW()),
    ('EQUIPMENT_DOCS', 'Equipment Documents', 'EQUIPMENT', TRUE, 1, NOW()),
    ('GENERIC', 'Generic Documents', 'GENERIC', TRUE, 1, NOW())
ON CONFLICT (code) DO NOTHING;
