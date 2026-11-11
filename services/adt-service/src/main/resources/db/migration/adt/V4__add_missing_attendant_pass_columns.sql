ALTER TABLE ihms_adt.t_attendant_pass ADD COLUMN IF NOT EXISTS unit_id BIGINT;
ALTER TABLE ihms_adt.t_attendant_pass ADD COLUMN IF NOT EXISTS created_unit_id BIGINT;
ALTER TABLE ihms_adt.t_attendant_pass ADD COLUMN IF NOT EXISTS updated_unit_id BIGINT;
ALTER TABLE ihms_adt.t_attendant_pass ADD COLUMN IF NOT EXISTS synchronized BOOLEAN DEFAULT FALSE;
