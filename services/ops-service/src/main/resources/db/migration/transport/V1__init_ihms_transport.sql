-- MIG-023 sub-slice (S17): transport / vehicle request lifecycle — clean target schema.
-- Single-writer ownership: transport-service is the sole writer of ihms_transport
-- (same extraction pattern as ihms_item / ihms_grievance / ihms_waste / ihms_linen,
-- IMP-DEC-026/032/033/034).
--
-- Legacy sources (evidence authority, /mnt/d/ihms read-only):
--   t_vehiclerequest          -> vehicle_request          (TransportRequest.java)
--   t_vehiclerequestdetails   -> vehicle_request_details  (TransportRequestDetails.java)
--   M_AMBULANCEMASTER         -> vehicle                  (Ambulancemaster.java)
--   M_DRIVERMASTER            -> driver                   (DriverMaster.java)
--   t_fuelstationdetails      -> fuel_station_details     (FuelStationDetails.java)
--   t_vehiclemaintenance      -> vehicle_maintenance      (VehicleMaintenance.java)
--   M_VEHICLEREQUESTTYPEMASTER-> vehicle_request_type     (VehicleRequestTypeMaster.java)
--   M_AMBULANCETYPEMASTER     -> ambulance_type           (AmbulanceTypeMaster.java)
--
-- Lifecycle (legacy TransportServiceImpl): a request is raised (patient requests
-- auto-approve), a vehicle + driver are allocated (both flip isAvailable=false),
-- optionally approved, then vehicle-out / vehicle-in details are recorded
-- (completed=true releases vehicle + driver), or the request is cancelled
-- (isCancelled=true releases vehicle + driver).
--
-- Cross-domain references deferred (dependency-neutral): patient_id / doctor_id /
-- staff_id / department_id point at patient + IAM domains — no FK until those
-- integration slices; notification mail, billing charges (addTransportCharge /
-- getChargesForTransport / vehicle-in billing), and patient/doctor/staff search
-- + mrno/designation lookups are deferred.

CREATE SCHEMA IF NOT EXISTS ihms_transport;

-- ------------------------------------------------------- vehicle request --

CREATE TABLE IF NOT EXISTS ihms_transport.vehicle_request (
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
    requisition_by VARCHAR(255),
    mobile_no BIGINT,
    mobile_country_code BIGINT,
    vehicle_type_id BIGINT,
    vehicle_id BIGINT,
    location VARCHAR(255),
    request_type BIGINT,
    is_charge_exempted BOOLEAN NOT NULL DEFAULT FALSE,
    exemption_catg_id BIGINT,
    bid_aid VARCHAR(255),
    request_date_time TIMESTAMP(3),
    expiry_date_time TIMESTAMP(3),
    remarks VARCHAR(1000),
    is_doctor_request BOOLEAN NOT NULL DEFAULT FALSE,
    user_type BIGINT,
    department_id BIGINT,
    doctor_id BIGINT,
    staff_id BIGINT,
    is_pickup_trip BOOLEAN NOT NULL DEFAULT FALSE,
    is_drop_trip BOOLEAN NOT NULL DEFAULT FALSE,
    is_deadbody_trip BOOLEAN NOT NULL DEFAULT FALSE,
    staff_instrument_req VARCHAR(255),
    is_vehicle_out BOOLEAN NOT NULL DEFAULT FALSE,
    completed BOOLEAN NOT NULL DEFAULT FALSE,
    patient_id BIGINT,
    driver_id BIGINT,
    is_approved BOOLEAN NOT NULL DEFAULT FALSE,
    allocated_doctors VARCHAR(1000),
    allocated_nurses VARCHAR(1000),
    request_to_date_time TIMESTAMP(3),
    is_cancelled BOOLEAN NOT NULL DEFAULT FALSE,
    approved_by VARCHAR(255),
    is_charged BOOLEAN NOT NULL DEFAULT FALSE,
    opd_ipd SMALLINT,
    opd_ipd_id BIGINT,
    CONSTRAINT pk_vehicle_request PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS ix_vehicle_request_unit ON ihms_transport.vehicle_request (unit_id);
CREATE INDEX IF NOT EXISTS ix_vehicle_request_vehicle ON ihms_transport.vehicle_request (vehicle_id);
CREATE INDEX IF NOT EXISTS ix_vehicle_request_driver ON ihms_transport.vehicle_request (driver_id);
CREATE INDEX IF NOT EXISTS ix_vehicle_request_req_dt ON ihms_transport.vehicle_request (request_date_time);
CREATE INDEX IF NOT EXISTS ix_vehicle_request_type ON ihms_transport.vehicle_request (request_type);

CREATE TABLE IF NOT EXISTS ihms_transport.vehicle_request_details (
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
    vehicle_request_id BIGINT NOT NULL REFERENCES ihms_transport.vehicle_request (id),
    start_odm_reading BIGINT,
    end_odm_reading BIGINT,
    trip_distance_estimated BIGINT,
    trip_distance BIGINT,
    calculated_trip_fare BIGINT,
    actual_fare BIGINT,
    charge_type BIGINT,
    out_time TIMESTAMP(3),
    in_time TIMESTAMP(3),
    fare_per_km NUMERIC(18,4),
    CONSTRAINT pk_vehicle_request_details PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS ix_vrd_request ON ihms_transport.vehicle_request_details (vehicle_request_id);

-- ---------------------------------------------------------------- masters --

CREATE TABLE IF NOT EXISTS ihms_transport.vehicle (
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
    vehicle_name VARCHAR(255),
    vehicle_make VARCHAR(255),
    vehicle_type VARCHAR(255),
    vehicle_no VARCHAR(255),
    purchase_date TIMESTAMP(3),
    purchase_amount NUMERIC(18,4),
    cash_lease_donated INTEGER,
    institute_name VARCHAR(255),
    contact_person VARCHAR(255),
    contact_no VARCHAR(255),
    chasy_no VARCHAR(255),
    address VARCHAR(1000),
    no_of_lease_months INTEGER,
    amount_each_month NUMERIC(18,4),
    invoice_no VARCHAR(255),
    vehicle_location VARCHAR(255),
    is_available BOOLEAN NOT NULL DEFAULT TRUE,
    fare_per_km NUMERIC(18,4),
    vehicle_average BIGINT,
    insurance_no VARCHAR(255),
    insurance_expiry_date TIMESTAMP(3),
    puc_no VARCHAR(255),
    puc_expiry_date TIMESTAMP(3),
    is_108 BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT pk_vehicle PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS ix_vehicle_unit ON ihms_transport.vehicle (unit_id);

CREATE TABLE IF NOT EXISTS ihms_transport.driver (
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
    first_name VARCHAR(255),
    dob TIMESTAMP(3),
    mobile_no BIGINT,
    gender_id BIGINT,
    driving_lic_no VARCHAR(255),
    is_available BOOLEAN NOT NULL DEFAULT TRUE,
    license_type VARCHAR(255),
    license_exp_date TIMESTAMP(3),
    driver_experience BIGINT,
    CONSTRAINT pk_driver PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS ix_driver_unit ON ihms_transport.driver (unit_id);

CREATE TABLE IF NOT EXISTS ihms_transport.vehicle_request_type (
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
    description VARCHAR(255),
    CONSTRAINT pk_vehicle_request_type PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS ihms_transport.ambulance_type (
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
    description VARCHAR(255),
    is_for_patient BOOLEAN,
    CONSTRAINT pk_ambulance_type PRIMARY KEY (id)
);

-- ----------------------------------------------------- fuel + maintenance --

CREATE TABLE IF NOT EXISTS ihms_transport.fuel_station_details (
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
    station_details VARCHAR(1000),
    vehicle_id BIGINT,
    vehicle_type_id BIGINT,
    qty NUMERIC(18,4),
    rate NUMERIC(18,4),
    fuel_type BIGINT,
    receipt_no VARCHAR(255),
    date_time TIMESTAMP(3),
    CONSTRAINT pk_fuel_station_details PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS ix_fuel_unit ON ihms_transport.fuel_station_details (unit_id);
CREATE INDEX IF NOT EXISTS ix_fuel_dt ON ihms_transport.fuel_station_details (date_time);

CREATE TABLE IF NOT EXISTS ihms_transport.vehicle_maintenance (
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
    workshop_name VARCHAR(255),
    vehicle_id BIGINT,
    vehicle_type_id BIGINT,
    estimated_cost NUMERIC(18,4),
    request_date TIMESTAMP(3),
    reason VARCHAR(1000),
    maintenance_date TIMESTAMP(3),
    is_approved BOOLEAN NOT NULL DEFAULT FALSE,
    is_completed BOOLEAN NOT NULL DEFAULT FALSE,
    actual_cost NUMERIC(18,4),
    is_accidental_case BOOLEAN NOT NULL DEFAULT FALSE,
    date_of_accident TIMESTAMP(3),
    accidental_cause VARCHAR(1000),
    accidental_type VARCHAR(255),
    accidental_type_id BIGINT,
    workshop_id BIGINT,
    repair_works VARCHAR(1000),
    CONSTRAINT pk_vehicle_maintenance PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS ix_vm_unit ON ihms_transport.vehicle_maintenance (unit_id);
CREATE INDEX IF NOT EXISTS ix_vm_vehicle ON ihms_transport.vehicle_maintenance (vehicle_id);

-- -------------------------------------------------------------- sequences --
-- Explicit sequences per migrated table (IMP-DEC-003; no BIGSERIAL).

CREATE SEQUENCE IF NOT EXISTS ihms_transport.vehicle_request_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS ihms_transport.vehicle_request_details_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS ihms_transport.vehicle_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS ihms_transport.driver_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS ihms_transport.vehicle_request_type_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS ihms_transport.ambulance_type_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS ihms_transport.fuel_station_details_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS ihms_transport.vehicle_maintenance_seq START WITH 1 INCREMENT BY 1;
