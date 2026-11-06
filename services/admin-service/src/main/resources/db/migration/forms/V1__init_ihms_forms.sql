-- ============================================================================
-- V1 — ihms_forms schema (TIU Clinical Forms engine, M40)
-- Clean target of legacy M_NOTETITLEMASTER + T_PATIENTNOTE + T_NOTEDETAILS
-- + TIU XML engine (XMLToJsonUntil / tiuxml) JSON-schema evolution.
-- Pattern: versioned JSON Schema store (t_form_schema) + submissions (t_form_submission).
-- 368 legacy TIU forms (modules/tiuxmls/schemas/) → 5 seeded here to prove pattern
-- (ponytail: add when volume proves; bulk 368 deferred).
-- ONE-TABLE-ONE-OWNER: ALL tables here belong to the forms service.
-- REST-only: no outbox until forms have a second consumer.
-- ============================================================================

CREATE SCHEMA IF NOT EXISTS ihms_forms;

SET search_path TO ihms_forms, public;

-- ---------------------------------------------------------------------------
-- t_form_schema — versioned JSON Schema store (ported from M_NOTETITLEMASTER.noteInfoJson)
-- code is the form key (e.g. NURSING_INITIAL_ASSESSMENT), version is 1..n,
-- uq ensures one row per (code, version). Latest version = max(version) per code.
-- schema_json is JSONB (draft-07) used by frontend dynamic renderer.
-- legacy_tiuxml_ref holds original TIU file mapping for traceability.
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ihms_forms.t_form_schema (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(64) NOT NULL,
    name VARCHAR(255) NOT NULL,
    specialty VARCHAR(100) NOT NULL, -- NURSING, EMERGENCY, ANESTHESIA, GENERAL_MEDICINE, etc.
    description TEXT,
    schema_json JSONB NOT NULL,
    version INT NOT NULL DEFAULT 1,
    status BOOLEAN NOT NULL DEFAULT TRUE,
    legacy_tiuxml_ref VARCHAR(255),
    added_by BIGINT,
    added_datetime TIMESTAMP(3),
    updated_by BIGINT,
    updated_datetime TIMESTAMP(3),
    CONSTRAINT uq_form_schema_code_version UNIQUE (code, version)
);
CREATE INDEX IF NOT EXISTS ix_form_schema_code ON ihms_forms.t_form_schema (code);
CREATE INDEX IF NOT EXISTS ix_form_schema_specialty ON ihms_forms.t_form_schema (specialty);
CREATE INDEX IF NOT EXISTS ix_form_schema_status ON ihms_forms.t_form_schema (status);
COMMENT ON TABLE ihms_forms.t_form_schema IS 'Versioned JSON Schema store (ported from M_NOTETITLEMASTER + TIU XML engine). code+version unique; latest = max version per code.';

-- ---------------------------------------------------------------------------
-- t_form_submission — per-patient filled form instance (ported from T_PATIENTNOTE + T_NOTEDETAILS)
-- form_id FK to t_form_schema.id (immutable version pin: submission locks the schema version used)
-- submission_json is JSONB document validated against schema_json at submit time (app-level, ponytail: no DB CHECK yet)
-- patient_id / encounter_id are opaque IDs in phase 1 (cross-service validation deferred).
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ihms_forms.t_form_submission (
    id BIGSERIAL PRIMARY KEY,
    form_id BIGINT NOT NULL REFERENCES ihms_forms.t_form_schema (id),
    patient_id BIGINT,
    encounter_id BIGINT,
    submission_json JSONB NOT NULL,
    submitted_by BIGINT,
    submitted_at TIMESTAMP(3) NOT NULL DEFAULT NOW(),
    status BOOLEAN NOT NULL DEFAULT TRUE,
    added_datetime TIMESTAMP(3),
    CONSTRAINT fk_submission_form FOREIGN KEY (form_id) REFERENCES ihms_forms.t_form_schema(id)
);
CREATE INDEX IF NOT EXISTS ix_form_submission_form ON ihms_forms.t_form_submission (form_id);
CREATE INDEX IF NOT EXISTS ix_form_submission_patient ON ihms_forms.t_form_submission (patient_id);
CREATE INDEX IF NOT EXISTS ix_form_submission_encounter ON ihms_forms.t_form_submission (encounter_id);
CREATE INDEX IF NOT EXISTS ix_form_submission_status ON ihms_forms.t_form_submission (status);
COMMENT ON TABLE ihms_forms.t_form_submission IS 'Per-patient filled form (ported from T_PATIENTNOTE/T_NOTEDETAILS). submission_json validated against t_form_schema.schema_json at submit time.';

-- ---------------------------------------------------------------------------
-- Seed top-5 specialty forms (prove pattern across NURSING / EMERGENCY / ANESTHESIA / GENERAL_MEDICINE)
-- Each schema_json is JSON Schema draft-07 (https://json-schema.org/draft-07/schema#)
-- Kept minimal but realistic: different field types (string, number, array, enum, boolean)
-- Legacy TIU mapping noted in legacy_tiuxml_ref for traceability.
-- ---------------------------------------------------------------------------
INSERT INTO ihms_forms.t_form_schema (code, name, specialty, description, schema_json, version, status, legacy_tiuxml_ref, added_by, added_datetime)
VALUES
(
    'NURSING_INITIAL_ASSESSMENT',
    'Nursing Initial Assessment',
    'NURSING',
    'Admission nursing assessment (ported from NURSINGINITIALASSESSMENTfinal1 TIU). Vitals, allergies, pain, fall risk.',
    '{
      "$schema": "http://json-schema.org/draft-07/schema#",
      "title": "Nursing Initial Assessment",
      "type": "object",
      "properties": {
        "chiefComplaint": {"type": "string", "title": "Chief Complaint", "minLength": 1},
        "historyOfPresentIllness": {"type": "string", "title": "History of Present Illness"},
        "allergies": {"type": "array", "title": "Allergies", "items": {"type": "string"}},
        "vitalSigns": {"type": "object", "title": "Vital Signs", "properties": {
          "bloodPressure": {"type": "string", "title": "BP (mmHg)"},
          "temperature": {"type": "number", "title": "Temp (°C)"},
          "pulse": {"type": "number", "title": "Pulse (bpm)"},
          "respiratoryRate": {"type": "number", "title": "RR (/min)"},
          "spo2": {"type": "number", "title": "SpO2 (%)", "minimum": 0, "maximum": 100}
        }},
        "painScore": {"type": "integer", "title": "Pain Score (0-10)", "minimum": 0, "maximum": 10},
        "fallRisk": {"type": "boolean", "title": "Fall Risk"},
        "nurseNotes": {"type": "string", "title": "Nurse Notes"}
      },
      "required": ["chiefComplaint", "painScore"]
    }'::jsonb,
    1, TRUE, 'NURSINGINITIALASSESSMENTfinal1.json', 1, NOW()
),
(
    'EMERGENCY_TRIAGE',
    'Emergency Triage Assessment',
    'EMERGENCY',
    'ER triage (ported from ERASSESSMENT_V10 TIU). ESI level, arrival mode, vitals, priority.',
    '{
      "$schema": "http://json-schema.org/draft-07/schema#",
      "title": "Emergency Triage Assessment",
      "type": "object",
      "properties": {
        "triageCategory": {"type": "string", "title": "Triage Category (ESI)", "enum": ["ESI-1", "ESI-2", "ESI-3", "ESI-4", "ESI-5"]},
        "chiefComplaint": {"type": "string", "title": "Chief Complaint", "minLength": 1},
        "arrivalMode": {"type": "string", "title": "Arrival Mode", "enum": ["Walk-in", "Ambulance", "Referral", "Police"]},
        "vitalSigns": {"type": "object", "title": "Vitals", "properties": {
          "bp": {"type": "string"},
          "pulse": {"type": "number"},
          "spo2": {"type": "number", "minimum": 0, "maximum": 100}
        }},
        "allergiesKnown": {"type": "boolean", "title": "Known Allergies"},
        "triageNotes": {"type": "string", "title": "Triage Notes"}
      },
      "required": ["triageCategory", "chiefComplaint"]
    }'::jsonb,
    1, TRUE, 'ERASSESSMENT_V10.json', 1, NOW()
),
(
    'DISCHARGE_SUMMARY',
    'Discharge Summary',
    'GENERAL_MEDICINE',
    'Discharge summary (ported from Discharge checklists + T_PATIENTNOTE discharge type). Diagnosis, procedures, meds, follow-up.',
    '{
      "$schema": "http://json-schema.org/draft-07/schema#",
      "title": "Discharge Summary",
      "type": "object",
      "properties": {
        "admissionId": {"type": "integer", "title": "Admission ID"},
        "dischargeDate": {"type": "string", "format": "date", "title": "Discharge Date"},
        "finalDiagnosis": {"type": "string", "title": "Final Diagnosis", "minLength": 1},
        "proceduresPerformed": {"type": "array", "title": "Procedures", "items": {"type": "string"}},
        "medicationsOnDischarge": {"type": "array", "title": "Meds on Discharge", "items": {"type": "object", "properties": {
          "drug": {"type": "string"}, "dose": {"type": "string"}, "duration": {"type": "string"}
        }}},
        "followUpDate": {"type": "string", "format": "date", "title": "Follow-up Date"},
        "dischargeNotes": {"type": "string", "title": "Discharge Notes"}
      },
      "required": ["finalDiagnosis", "dischargeDate"]
    }'::jsonb,
    1, TRUE, 'Discharge_checklist_govt_institution.json', 1, NOW()
),
(
    'PRE_ANESTHETIC_CHECK',
    'Pre-Anesthetic Check',
    'ANESTHESIA',
    'Pre-anesthetic evaluation (ported from Pre_Anesthetic TIU). ASA, airway, comorbidities, plan, consent.',
    '{
      "$schema": "http://json-schema.org/draft-07/schema#",
      "title": "Pre-Anesthetic Check",
      "type": "object",
      "properties": {
        "asaGrade": {"type": "string", "title": "ASA Grade", "enum": ["ASA-I", "ASA-II", "ASA-III", "ASA-IV", "ASA-V", "ASA-VI"]},
        "airwayAssessment": {"type": "string", "title": "Airway Assessment", "enum": ["Mallampati I", "Mallampati II", "Mallampati III", "Mallampati IV"]},
        "comorbidities": {"type": "array", "title": "Comorbidities", "items": {"type": "string"}},
        "anestheticPlan": {"type": "string", "title": "Anesthetic Plan", "enum": ["GA", "Spinal", "Epidural", "Regional", "MAC"]},
        "consentObtained": {"type": "boolean", "title": "Consent Obtained"},
        "anesthetistNotes": {"type": "string", "title": "Notes"}
      },
      "required": ["asaGrade", "airwayAssessment", "anestheticPlan", "consentObtained"]
    }'::jsonb,
    1, TRUE, 'Pre_Anesthetic.json', 1, NOW()
),
(
    'DAILY_NURSING_REPORT',
    'Daily Nursing Report',
    'NURSING',
    '24h nursing report (ported from DAILY_NURSING_REPORT_FORM_V10 TIU). Vitals trend, intake/output, interventions.',
    '{
      "$schema": "http://json-schema.org/draft-07/schema#",
      "title": "Daily Nursing Report",
      "type": "object",
      "properties": {
        "reportDate": {"type": "string", "format": "date", "title": "Report Date"},
        "shift": {"type": "string", "title": "Shift", "enum": ["Morning", "Evening", "Night"]},
        "vitals": {"type": "object", "title": "Vitals", "properties": {
          "bp": {"type": "string"}, "temp": {"type": "number"}, "pulse": {"type": "number"}
        }},
        "intakeOutput": {"type": "object", "title": "Intake/Output (ml)", "properties": {
          "intake": {"type": "number", "minimum": 0},
          "output": {"type": "number", "minimum": 0}
        }},
        "interventions": {"type": "array", "title": "Interventions", "items": {"type": "string"}},
        "nursingNotes": {"type": "string", "title": "Nursing Notes"}
      },
      "required": ["reportDate", "shift"]
    }'::jsonb,
    1, TRUE, 'DAILY_NURSING_REPORT_FORM_V10.json', 1, NOW()
)
ON CONFLICT (code, version) DO NOTHING;
