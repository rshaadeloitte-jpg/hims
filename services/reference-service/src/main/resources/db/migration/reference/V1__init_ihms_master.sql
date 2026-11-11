-- ============================================================================
-- iHMS Modernization — Wave 0 (MIG-006 / slice S0)
-- Schema: ihms_master (schema-per-domain convention; IMP-DEC-001/007)
-- Tables are 1:1, case-preserved, quoted names from legacy JPA entities:
--   M_UNITMASTER    <- Unitmaster.java (MBase + ~90 columns, verified 2026-08-14)
--   M_DEPARTMENTMASTER <- Departmentmaster.java
--   M_COMPANYMASTER <- Companymaster.java
-- Plus transactional outbox (IMP-DEC-004, KAFKA-EVENT-CATALOG envelope).
-- Column sizes are provisional pending live Oracle DDL dump (MIG-001, BLK-001);
-- data-loss-free upper bounds used. Types per POSTGRESQL-MIGRATION mapping.
-- ============================================================================

CREATE SCHEMA IF NOT EXISTS ihms_master;

SET search_path TO ihms_master, public;

-- ---------------------------------------------------------------------------
-- M_UNITMASTER (Unitmaster)
-- ---------------------------------------------------------------------------
CREATE TABLE "M_UNITMASTER" (
    "ID"              BIGINT       NOT NULL,
    "UnitID"          BIGINT,
    "STATUS"          BOOLEAN,
    "CREATEDUNITID"   BIGINT,
    "UPDATEDUNITID"   BIGINT,
    "ADDEDBY"         BIGINT,
    "ADDEDDATETIME"   TIMESTAMP(3),
    "UPDATEDBY"       BIGINT,
    "UPDATEDDATETIME" TIMESTAMP(3),
    "SYNCHRONIZED"    BOOLEAN      NOT NULL DEFAULT FALSE,
    "CODE"            VARCHAR(255),
    "NAME"            VARCHAR(255),
    "DESCRIPTION"     VARCHAR(500),
    "ADDRESSLINE1"    VARCHAR(500),
    "ADDRESSLINE2"    VARCHAR(500),
    "ADDRESSLINE3"    VARCHAR(500),
    "COUNTRY"         VARCHAR(255),
    "STATE"           VARCHAR(255),
    "DISTRICT"        VARCHAR(255),
    "TALUKA"          VARCHAR(255),
    "AREA"            VARCHAR(255),
    "PINCODE"         VARCHAR(64),
    "SERVERNAME"      VARCHAR(255),
    "DATABASENAME"    VARCHAR(255),
    "PHARMACYLICENSENO" VARCHAR(255),
    "CLINICREGNO"     VARCHAR(255),
    "SHOPNO"          VARCHAR(255),
    "TRADENO"         VARCHAR(255),
    "CITY"            VARCHAR(255),
    "CONTACTNO"       VARCHAR(64),
    "RESINOCOUNTRYCODE" INTEGER,
    "RESISTDCODE"     INTEGER,
    "FAXNO"           VARCHAR(64),
    "EMAIL"           VARCHAR(255),
    "UNITPREFIXFORMRNO" VARCHAR(32),
    "LOGO"            BYTEA,
    "VAT_TIN_No"      VARCHAR(64),
    "ISMEDVERIFY"     BOOLEAN,
    "DISTRICTID"      BIGINT,
    "TENANTID"        BIGINT,
    "CATEGORY"        BIGINT,
    "TYPE"            BIGINT,
    "ISALLOWONLINE"   BOOLEAN      NOT NULL DEFAULT FALSE,
    "LONGITUDE"       VARCHAR(64),
    "LATIUTDE"        VARCHAR(64),
    "ISPARENT"        BOOLEAN      NOT NULL DEFAULT FALSE,
    "PARENTID"        BIGINT,
    "ISVIRTUAL"       BOOLEAN      NOT NULL DEFAULT FALSE,
    "VIRTUALID"       BIGINT,
    "CONTRACTFROMDATE" DATE,
    "CONTRACTTODATE"  DATE,
    "ISSUSPENSION"    BOOLEAN      NOT NULL DEFAULT FALSE,
    "ISTPU"           BOOLEAN      NOT NULL DEFAULT FALSE,
    "ISTSU"           BOOLEAN      NOT NULL DEFAULT FALSE,
    "DEFAULTGENDERID" BIGINT,
    "NINCODE"         VARCHAR(255),
    "ALLOWDIRECTONLINEAPPOINTMENT" BOOLEAN NOT NULL DEFAULT FALSE,
    "ALLOWLABNORMALRESULTREVIEW"   BOOLEAN NOT NULL DEFAULT TRUE,
    "BILLNUMBERLISINTEGRATION"     BOOLEAN NOT NULL DEFAULT FALSE,
    "CONSULTTYPE"     INTEGER      NOT NULL DEFAULT 0,
    "ENABLEEHR"       BOOLEAN      NOT NULL DEFAULT FALSE,
    "ENABLEOTPBASEDLOGIN" BOOLEAN  NOT NULL DEFAULT FALSE,
    "ENABLEWHATSAPP"  BOOLEAN      NOT NULL DEFAULT FALSE,
    "FACILITYLINKED"  BOOLEAN      NOT NULL DEFAULT FALSE,
    "ISEAUSHADHIDATASYNC" BOOLEAN  NOT NULL DEFAULT FALSE,
    "ISMULTIMAINSTORE" BOOLEAN     NOT NULL DEFAULT FALSE,
    "ISOPLABELREQUIRED" BOOLEAN    NOT NULL DEFAULT FALSE,
    "ISOPERATIONAL"   BOOLEAN      NOT NULL DEFAULT FALSE,
    "ISPHARMACYUPLOADPRESC" BOOLEAN NOT NULL DEFAULT FALSE,
    "ISPHARMACYVISITREQUIRED" BOOLEAN NOT NULL DEFAULT FALSE,
    "PARENTCHILDUNITBASED" BOOLEAN NOT NULL DEFAULT FALSE,
    "SHOWPATHOLOCATION" BOOLEAN    NOT NULL DEFAULT FALSE,
    "SHOWPATHOSUBSPECIALIZATION"   BOOLEAN NOT NULL DEFAULT FALSE,
    "HOSPITALTYPE"    INTEGER,
    "ISAUTOINDENT"    BOOLEAN      NOT NULL DEFAULT FALSE,
    "AUTOINDENTEXECUTIONTIME" VARCHAR(64),
    "ISAUTOINDENTFOR" BIGINT,
    "PATHIESTYPE_NEW" VARCHAR(255),
    "APIKEY"          VARCHAR(4000),
    "EAUSHADHISTOREID" VARCHAR(255),
    "EAUSHADHIWAREHOUSEID" VARCHAR(255),
    "EAPARENTSTOREID" BIGINT,
    "EASTOREID"       BIGINT,
    "apiToken"        VARCHAR(4000),
    "EAUSPCTS_STOREID" BIGINT,
    "OUTREACHID"      BIGINT,
    "ISCENTRALSTORE"  BOOLEAN      NOT NULL DEFAULT FALSE,
    "ISCENTRALLYOUTSOURCEDLAB" BOOLEAN NOT NULL DEFAULT FALSE,
    "ISCWFCOVERSHEET" SMALLINT,
    "CENTRALSTOREIDS" VARCHAR(4000),
    "ERAKTKOSHUNITID" VARCHAR(255),
    "ERAKTOSHKEY"     VARCHAR(4000),
    "MERCHANTCODE"    VARCHAR(255),
    "ABDMID"          VARCHAR(255),
    "HMISID"          VARCHAR(255),
    "HMISCODE"        VARCHAR(255),
    "HISTONOCATEGORY" VARCHAR(255),
    "ALLOWEDOPERATORMOBILENUMBERSJSON" VARCHAR(4000),
    "BEDCAPACITY"     BIGINT,
    "EHRCODE"         VARCHAR(255),
    "EAUSHADHISTOREDESC" VARCHAR(255),
    "ABDMHFRID"       VARCHAR(255),
    "ABDMFACILITYNAME" VARCHAR(255),
    "LOCALEDESCRIPTION" VARCHAR(500),
    CONSTRAINT "PK_M_UNITMASTER" PRIMARY KEY ("ID")
);
COMMENT ON TABLE "M_UNITMASTER" IS 'Legacy Unitmaster entity (Unitmaster.java). 1:1 name/type mapping; sizes provisional until MIG-001 DDL dump.';

-- ---------------------------------------------------------------------------
-- M_DEPARTMENTMASTER (Departmentmaster)
-- ---------------------------------------------------------------------------
CREATE TABLE "M_DEPARTMENTMASTER" (
    "ID"              BIGINT       NOT NULL,
    "UnitID"          BIGINT,
    "STATUS"          BOOLEAN,
    "CREATEDUNITID"   BIGINT,
    "UPDATEDUNITID"   BIGINT,
    "ADDEDBY"         BIGINT,
    "ADDEDDATETIME"   TIMESTAMP(3),
    "UPDATEDBY"       BIGINT,
    "UPDATEDDATETIME" TIMESTAMP(3),
    "SYNCHRONIZED"    BOOLEAN      NOT NULL DEFAULT FALSE,
    "CODE"            VARCHAR(255),
    "DESCRIPTION"     VARCHAR(500),
    "ISCLINICAL"      BOOLEAN      NOT NULL DEFAULT FALSE,
    "ISPAEDIATRICSDEPARTMENT" BOOLEAN NOT NULL DEFAULT FALSE,
    "FEMALEDEPARTMENT" BOOLEAN     NOT NULL DEFAULT FALSE,
    "TIMESLOT"        INTEGER,
    "NOOFPATIENT"     INTEGER,
    "UPDATEDWINDOWSLOGINNAME" VARCHAR(255),
    "DEFAULTDOCTORID" BIGINT,
    "TOKENPREFIX"     VARCHAR(64),
    "AGELIMIT"        INTEGER,
    "ISALLIEDHEALTH"  BOOLEAN      NOT NULL DEFAULT FALSE,
    "ISASSESS"        BOOLEAN      NOT NULL DEFAULT FALSE,
    "MAINTAINANCESTORE" BOOLEAN    NOT NULL DEFAULT FALSE,
    "ONLINEAPPOINTMENTCOUNT" INTEGER NOT NULL DEFAULT 0,
    "APPOINTMENTSLOT" NUMERIC(19,2),
    "ISIPD"           BOOLEAN      NOT NULL DEFAULT FALSE,
    "MAPPEDPHARMACY"  BIGINT,
    "DEFAULTDEPARTMENT" BOOLEAN    NOT NULL DEFAULT FALSE,
    "ISFAMILYPLANNINGDEPARTMENT" BOOLEAN NOT NULL DEFAULT FALSE,
    "EMAIL"           VARCHAR(255),
    "LOCALEDESCRIPTION" VARCHAR(500),
    "DEFAULTROOMID"   BIGINT,
    "qmsNursingStationAvailable" BOOLEAN NOT NULL DEFAULT FALSE,
    "FROMAGE"         INTEGER      NOT NULL DEFAULT 0,
    "TOAGE"           INTEGER      NOT NULL DEFAULT 100,
    "ISTELECONSULT"   BOOLEAN      NOT NULL DEFAULT FALSE,
    "ISEMERGENCY"     BOOLEAN      NOT NULL DEFAULT FALSE,
    "QUEUEPAGESIZE"   BIGINT       NOT NULL DEFAULT 4,
    CONSTRAINT "PK_M_DEPARTMENTMASTER" PRIMARY KEY ("ID")
);
COMMENT ON TABLE "M_DEPARTMENTMASTER" IS 'Legacy Departmentmaster entity (Departmentmaster.java). 1:1 mapping; sizes provisional until MIG-001.';

-- ---------------------------------------------------------------------------
-- M_COMPANYMASTER (Companymaster)
-- ---------------------------------------------------------------------------
CREATE TABLE "M_COMPANYMASTER" (
    "ID"              BIGINT       NOT NULL,
    "UnitID"          BIGINT,
    "STATUS"          BOOLEAN,
    "CREATEDUNITID"   BIGINT,
    "UPDATEDUNITID"   BIGINT,
    "ADDEDBY"         BIGINT,
    "ADDEDDATETIME"   TIMESTAMP(3),
    "UPDATEDBY"       BIGINT,
    "UPDATEDDATETIME" TIMESTAMP(3),
    "SYNCHRONIZED"    BOOLEAN      NOT NULL DEFAULT FALSE,
    "CODE"            VARCHAR(255),
    "DESCRIPTION"     VARCHAR(500),
    "COMPANYTYPEID"   BIGINT,
    "TOTALLIMIT"      NUMERIC(19,2),
    "USEDAMOUNT"      NUMERIC(19,2),
    "CREDITLIMIT"     BIGINT,
    "SERVICECHARGE"   NUMERIC(19,2),
    "ADDRESS"         VARCHAR(500),
    "COUNTRYID"       BIGINT,
    "STATEID"         BIGINT,
    "CITYID"          BIGINT,
    "AREA"            VARCHAR(255),
    "PINCODE"         VARCHAR(64),
    "CONTACTPERSON"   VARCHAR(255),
    "MOBILECOUNTRYCODE" BIGINT,
    "MOBILENO"        BIGINT,
    "COUNTRYCODE"     BIGINT,
    "STDCODE"         BIGINT,
    "PHONENO"         BIGINT,
    "REPORTFORMAT"    BYTEA,
    "REPORTPATH"      VARCHAR(500),
    "MOUDISPER"       NUMERIC(19,4),
    "FLATPER"         NUMERIC(19,4),
    "DESIGNATION"     VARCHAR(255),
    "EMAIL"           VARCHAR(255),
    "CREDIT_ALLOWED"  BOOLEAN,
    "COMPANYNAMEHINDI" VARCHAR(500),
    CONSTRAINT "PK_M_COMPANYMASTER" PRIMARY KEY ("ID")
);
COMMENT ON TABLE "M_COMPANYMASTER" IS 'Legacy Companymaster entity (Companymaster.java). 1:1 mapping; sizes provisional until MIG-001.';

-- ---------------------------------------------------------------------------
-- Transactional outbox (IMP-DEC-004; envelope per KAFKA-EVENT-CATALOG)
-- ---------------------------------------------------------------------------
CREATE TABLE outbox (
    id               BIGSERIAL    PRIMARY KEY,
    event_id         UUID         NOT NULL,
    event_type       VARCHAR(100) NOT NULL,
    version          VARCHAR(16)  NOT NULL DEFAULT 'v1',
    aggregate_type   VARCHAR(100) NOT NULL,
    aggregate_id     BIGINT       NOT NULL,
    partition_key    VARCHAR(100) NOT NULL,
    payload          JSONB        NOT NULL,
    idempotency_key  VARCHAR(255),
    producer_service VARCHAR(100) NOT NULL,
    occurred_at      TIMESTAMP(3) NOT NULL,
    status           VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    attempts         INTEGER      NOT NULL DEFAULT 0,
    published_at     TIMESTAMP(3),
    created_at       TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP
);
COMMENT ON TABLE outbox IS 'Transactional outbox: business tx writes event here atomically; KafkaOutboxRelay publishes and marks PUBLISHED. Status: PENDING/PUBLISHED/FAILED/DLQ.';
CREATE INDEX idx_outbox_dispatch ON outbox (status, created_at) WHERE status = 'PENDING';

-- ---------------------------------------------------------------------------
-- Sequences (convention <TABLE>_SEQ per legacy SEQ_CREATE_SCRIPT / evidence
-- T_EQUIPMENTCHECKLIST_SEQ, m_vitaltypes_seq; exact names verified at MIG-001)
-- ---------------------------------------------------------------------------
CREATE SEQUENCE "M_UNITMASTER_SEQ"      START WITH 1 INCREMENT BY 1 NO CYCLE;
CREATE SEQUENCE "M_DEPARTMENTMASTER_SEQ" START WITH 1 INCREMENT BY 1 NO CYCLE;
CREATE SEQUENCE "M_COMPANYMASTER_SEQ"    START WITH 1 INCREMENT BY 1 NO CYCLE;
