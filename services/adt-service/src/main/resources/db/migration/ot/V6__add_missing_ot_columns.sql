ALTER TABLE ihms_ot.t_anesthesia_drug ADD COLUMN IF NOT EXISTS unit_id BIGINT;
ALTER TABLE ihms_ot.t_anesthesia_drug ADD COLUMN IF NOT EXISTS created_unit_id BIGINT;
ALTER TABLE ihms_ot.t_anesthesia_drug ADD COLUMN IF NOT EXISTS updated_unit_id BIGINT;
ALTER TABLE ihms_ot.t_anesthesia_drug ADD COLUMN IF NOT EXISTS added_by BIGINT;
ALTER TABLE ihms_ot.t_anesthesia_drug ADD COLUMN IF NOT EXISTS added_datetime TIMESTAMP;
ALTER TABLE ihms_ot.t_anesthesia_drug ADD COLUMN IF NOT EXISTS updated_by BIGINT;
ALTER TABLE ihms_ot.t_anesthesia_drug ADD COLUMN IF NOT EXISTS updated_datetime TIMESTAMP;
ALTER TABLE ihms_ot.t_anesthesia_drug ADD COLUMN IF NOT EXISTS synchronized BOOLEAN DEFAULT FALSE;

ALTER TABLE ihms_ot.t_anesthesia_record ADD COLUMN IF NOT EXISTS unit_id BIGINT;
ALTER TABLE ihms_ot.t_anesthesia_record ADD COLUMN IF NOT EXISTS created_unit_id BIGINT;
ALTER TABLE ihms_ot.t_anesthesia_record ADD COLUMN IF NOT EXISTS updated_unit_id BIGINT;
ALTER TABLE ihms_ot.t_anesthesia_record ADD COLUMN IF NOT EXISTS added_by BIGINT;
ALTER TABLE ihms_ot.t_anesthesia_record ADD COLUMN IF NOT EXISTS added_datetime TIMESTAMP;
ALTER TABLE ihms_ot.t_anesthesia_record ADD COLUMN IF NOT EXISTS updated_by BIGINT;
ALTER TABLE ihms_ot.t_anesthesia_record ADD COLUMN IF NOT EXISTS updated_datetime TIMESTAMP;
ALTER TABLE ihms_ot.t_anesthesia_record ADD COLUMN IF NOT EXISTS synchronized BOOLEAN DEFAULT FALSE;

ALTER TABLE ihms_ot.t_checklist_completion ADD COLUMN IF NOT EXISTS unit_id BIGINT;
ALTER TABLE ihms_ot.t_checklist_completion ADD COLUMN IF NOT EXISTS created_unit_id BIGINT;
ALTER TABLE ihms_ot.t_checklist_completion ADD COLUMN IF NOT EXISTS updated_unit_id BIGINT;
ALTER TABLE ihms_ot.t_checklist_completion ADD COLUMN IF NOT EXISTS added_by BIGINT;
ALTER TABLE ihms_ot.t_checklist_completion ADD COLUMN IF NOT EXISTS added_datetime TIMESTAMP;
ALTER TABLE ihms_ot.t_checklist_completion ADD COLUMN IF NOT EXISTS updated_by BIGINT;
ALTER TABLE ihms_ot.t_checklist_completion ADD COLUMN IF NOT EXISTS updated_datetime TIMESTAMP;
ALTER TABLE ihms_ot.t_checklist_completion ADD COLUMN IF NOT EXISTS synchronized BOOLEAN DEFAULT FALSE;

ALTER TABLE ihms_ot.t_checklist_item ADD COLUMN IF NOT EXISTS unit_id BIGINT;
ALTER TABLE ihms_ot.t_checklist_item ADD COLUMN IF NOT EXISTS created_unit_id BIGINT;
ALTER TABLE ihms_ot.t_checklist_item ADD COLUMN IF NOT EXISTS updated_unit_id BIGINT;
ALTER TABLE ihms_ot.t_checklist_item ADD COLUMN IF NOT EXISTS added_by BIGINT;
ALTER TABLE ihms_ot.t_checklist_item ADD COLUMN IF NOT EXISTS added_datetime TIMESTAMP;
ALTER TABLE ihms_ot.t_checklist_item ADD COLUMN IF NOT EXISTS updated_by BIGINT;
ALTER TABLE ihms_ot.t_checklist_item ADD COLUMN IF NOT EXISTS updated_datetime TIMESTAMP;
ALTER TABLE ihms_ot.t_checklist_item ADD COLUMN IF NOT EXISTS synchronized BOOLEAN DEFAULT FALSE;

ALTER TABLE ihms_ot.t_ot_billing_estimate ADD COLUMN IF NOT EXISTS unit_id BIGINT;
ALTER TABLE ihms_ot.t_ot_billing_estimate ADD COLUMN IF NOT EXISTS created_unit_id BIGINT;
ALTER TABLE ihms_ot.t_ot_billing_estimate ADD COLUMN IF NOT EXISTS updated_unit_id BIGINT;
ALTER TABLE ihms_ot.t_ot_billing_estimate ADD COLUMN IF NOT EXISTS added_by BIGINT;
ALTER TABLE ihms_ot.t_ot_billing_estimate ADD COLUMN IF NOT EXISTS added_datetime TIMESTAMP;
ALTER TABLE ihms_ot.t_ot_billing_estimate ADD COLUMN IF NOT EXISTS updated_by BIGINT;
ALTER TABLE ihms_ot.t_ot_billing_estimate ADD COLUMN IF NOT EXISTS updated_datetime TIMESTAMP;
ALTER TABLE ihms_ot.t_ot_billing_estimate ADD COLUMN IF NOT EXISTS synchronized BOOLEAN DEFAULT FALSE;

ALTER TABLE ihms_ot.t_ot_nurse_schedule ADD COLUMN IF NOT EXISTS unit_id BIGINT;
ALTER TABLE ihms_ot.t_ot_nurse_schedule ADD COLUMN IF NOT EXISTS created_unit_id BIGINT;
ALTER TABLE ihms_ot.t_ot_nurse_schedule ADD COLUMN IF NOT EXISTS updated_unit_id BIGINT;
ALTER TABLE ihms_ot.t_ot_nurse_schedule ADD COLUMN IF NOT EXISTS added_by BIGINT;
ALTER TABLE ihms_ot.t_ot_nurse_schedule ADD COLUMN IF NOT EXISTS added_datetime TIMESTAMP;
ALTER TABLE ihms_ot.t_ot_nurse_schedule ADD COLUMN IF NOT EXISTS updated_by BIGINT;
ALTER TABLE ihms_ot.t_ot_nurse_schedule ADD COLUMN IF NOT EXISTS updated_datetime TIMESTAMP;
ALTER TABLE ihms_ot.t_ot_nurse_schedule ADD COLUMN IF NOT EXISTS synchronized BOOLEAN DEFAULT FALSE;

ALTER TABLE ihms_ot.t_ot_procedure_cost ADD COLUMN IF NOT EXISTS unit_id BIGINT;
ALTER TABLE ihms_ot.t_ot_procedure_cost ADD COLUMN IF NOT EXISTS created_unit_id BIGINT;
ALTER TABLE ihms_ot.t_ot_procedure_cost ADD COLUMN IF NOT EXISTS updated_unit_id BIGINT;
ALTER TABLE ihms_ot.t_ot_procedure_cost ADD COLUMN IF NOT EXISTS added_by BIGINT;
ALTER TABLE ihms_ot.t_ot_procedure_cost ADD COLUMN IF NOT EXISTS added_datetime TIMESTAMP;
ALTER TABLE ihms_ot.t_ot_procedure_cost ADD COLUMN IF NOT EXISTS updated_by BIGINT;
ALTER TABLE ihms_ot.t_ot_procedure_cost ADD COLUMN IF NOT EXISTS updated_datetime TIMESTAMP;
ALTER TABLE ihms_ot.t_ot_procedure_cost ADD COLUMN IF NOT EXISTS synchronized BOOLEAN DEFAULT FALSE;

ALTER TABLE ihms_ot.t_ot_room ADD COLUMN IF NOT EXISTS unit_id BIGINT;
ALTER TABLE ihms_ot.t_ot_room ADD COLUMN IF NOT EXISTS created_unit_id BIGINT;
ALTER TABLE ihms_ot.t_ot_room ADD COLUMN IF NOT EXISTS updated_unit_id BIGINT;
ALTER TABLE ihms_ot.t_ot_room ADD COLUMN IF NOT EXISTS added_by BIGINT;
ALTER TABLE ihms_ot.t_ot_room ADD COLUMN IF NOT EXISTS added_datetime TIMESTAMP;
ALTER TABLE ihms_ot.t_ot_room ADD COLUMN IF NOT EXISTS updated_by BIGINT;
ALTER TABLE ihms_ot.t_ot_room ADD COLUMN IF NOT EXISTS updated_datetime TIMESTAMP;
ALTER TABLE ihms_ot.t_ot_room ADD COLUMN IF NOT EXISTS synchronized BOOLEAN DEFAULT FALSE;

ALTER TABLE ihms_ot.t_ot_staff_schedule ADD COLUMN IF NOT EXISTS unit_id BIGINT;
ALTER TABLE ihms_ot.t_ot_staff_schedule ADD COLUMN IF NOT EXISTS created_unit_id BIGINT;
ALTER TABLE ihms_ot.t_ot_staff_schedule ADD COLUMN IF NOT EXISTS updated_unit_id BIGINT;
ALTER TABLE ihms_ot.t_ot_staff_schedule ADD COLUMN IF NOT EXISTS added_by BIGINT;
ALTER TABLE ihms_ot.t_ot_staff_schedule ADD COLUMN IF NOT EXISTS added_datetime TIMESTAMP;
ALTER TABLE ihms_ot.t_ot_staff_schedule ADD COLUMN IF NOT EXISTS updated_by BIGINT;
ALTER TABLE ihms_ot.t_ot_staff_schedule ADD COLUMN IF NOT EXISTS updated_datetime TIMESTAMP;
ALTER TABLE ihms_ot.t_ot_staff_schedule ADD COLUMN IF NOT EXISTS synchronized BOOLEAN DEFAULT FALSE;

ALTER TABLE ihms_ot.t_surgical_checklist ADD COLUMN IF NOT EXISTS unit_id BIGINT;
ALTER TABLE ihms_ot.t_surgical_checklist ADD COLUMN IF NOT EXISTS created_unit_id BIGINT;
ALTER TABLE ihms_ot.t_surgical_checklist ADD COLUMN IF NOT EXISTS updated_unit_id BIGINT;
ALTER TABLE ihms_ot.t_surgical_checklist ADD COLUMN IF NOT EXISTS added_by BIGINT;
ALTER TABLE ihms_ot.t_surgical_checklist ADD COLUMN IF NOT EXISTS added_datetime TIMESTAMP;
ALTER TABLE ihms_ot.t_surgical_checklist ADD COLUMN IF NOT EXISTS updated_by BIGINT;
ALTER TABLE ihms_ot.t_surgical_checklist ADD COLUMN IF NOT EXISTS updated_datetime TIMESTAMP;
ALTER TABLE ihms_ot.t_surgical_checklist ADD COLUMN IF NOT EXISTS synchronized BOOLEAN DEFAULT FALSE;

ALTER TABLE ihms_ot.t_vital_sign_record ADD COLUMN IF NOT EXISTS unit_id BIGINT;
ALTER TABLE ihms_ot.t_vital_sign_record ADD COLUMN IF NOT EXISTS created_unit_id BIGINT;
ALTER TABLE ihms_ot.t_vital_sign_record ADD COLUMN IF NOT EXISTS updated_unit_id BIGINT;
ALTER TABLE ihms_ot.t_vital_sign_record ADD COLUMN IF NOT EXISTS added_by BIGINT;
ALTER TABLE ihms_ot.t_vital_sign_record ADD COLUMN IF NOT EXISTS added_datetime TIMESTAMP;
ALTER TABLE ihms_ot.t_vital_sign_record ADD COLUMN IF NOT EXISTS updated_by BIGINT;
ALTER TABLE ihms_ot.t_vital_sign_record ADD COLUMN IF NOT EXISTS updated_datetime TIMESTAMP;
ALTER TABLE ihms_ot.t_vital_sign_record ADD COLUMN IF NOT EXISTS synchronized BOOLEAN DEFAULT FALSE;
