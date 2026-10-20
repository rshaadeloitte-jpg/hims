-- H2 sequences for @DataJpaTest (mirrors Flyway V1-V4 sequences in ihms_patient schema).
-- Required because PatientRegistrationService uses native nextval() calls.
CREATE SCHEMA IF NOT EXISTS ihms_patient;
CREATE SEQUENCE IF NOT EXISTS ihms_patient."patient_reg_seq" START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS ihms_patient."patient_document_seq" START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS ihms_patient."patient_sponsor_seq" START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS ihms_patient."patient_mlc_seq" START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS ihms_patient."patient_relation_seq" START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS ihms_patient."patient_aadhaar_verif_seq" START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS ihms_patient."patient_social_assess_seq" START WITH 1 INCREMENT BY 1;
