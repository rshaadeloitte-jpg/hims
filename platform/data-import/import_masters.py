#!/usr/bin/env python3
"""Master data importer for iHMS.

Loads WHO ICD-11 MMS, ISO country codes, and test master data into PostgreSQL.
Uses Python 3 standard library only.
Emits CSV files and stages them using psql \\copy and INSERT ... ON CONFLICT DO UPDATE
inside a single database transaction.

Database credentials and connection details are read exclusively from environment variables:
PGHOST, PGUSER, PGPASSWORD, PGDATABASE (and optional PGPORT).
"""

import argparse
import csv
import datetime
import hashlib
import os
import re
import subprocess
import sys
import tempfile
from collections import Counter


ALLOWED_COUNTRY_COLUMNS = (
    "ISO3166-1-Alpha-2",
    "ISO3166-1-Alpha-3",
    "ISO3166-1-numeric",
    "official_name_en",
    "Dial",
)

TEST_DEPARTMENTS = [
    ("General Medicine", True),
    ("General Surgery", True),
    ("Paediatrics", True),
    ("Obstetrics and Gynaecology", True),
    ("Orthopaedics", True),
    ("ENT", True),
    ("Ophthalmology", True),
    ("Dermatology", True),
    ("Psychiatry", True),
    ("Cardiology", True),
    ("Nephrology", True),
    ("Neurology", True),
    ("Emergency", True),
    ("Anaesthesiology", False),
    ("Radiology", False),
    ("Pathology", False),
    ("Blood Bank", False),
    ("Pharmacy", False),
    ("Physiotherapy", False),
    ("Dental", True),
]


def get_psql_cmd() -> list[str]:
    """Return the psql command without any credentials in CLI arguments."""
    return ["psql", "-v", "ON_ERROR_STOP=1"]


def compute_sha256(filepath: str) -> str:
    """Compute SHA-256 hash of a file."""
    h = hashlib.sha256()
    with open(filepath, "rb") as f:
        while chunk := f.read(65536):
            h.update(chunk)
    return h.hexdigest()


def strip_title(raw_title: str) -> str:
    """Strip only the leading '- ' nesting markers and outer quotes from title.
    
    The WHO license forbids altering titles, so internal hyphens, whitespace,
    and capitalization must be strictly preserved.
    """
    if not raw_title:
        return ""
    title = raw_title.strip()
    if len(title) >= 2 and title[0] == '"' and title[-1] == '"':
        title = title[1:-1]
    while title.startswith("- "):
        title = title[2:]
    return title


def derive_parent_linearization_uris(rows: list[dict]) -> list[dict]:
    """Derive parent_linearization_uri from row order and nesting depth.

    - Chapter: parent is None.
    - Block: depth by BlockL depth; depth 1 -> chapter, depth > 1 -> active block at depth - 1.
    - Category: depth by DepthInKind; depth 1 -> enclosing block (or chapter), depth > 1 -> category at depth - 1.
    """
    current_chapter = None
    active_blocks: dict[int, str] = {}
    active_categories: dict[int, str] = {}

    for row in rows:
        ck = row.get("class_kind")
        uri = row.get("linearization_uri")
        depth = row.get("depth_in_kind")
        if depth is None:
            depth = 1

        parent_uri = None
        if ck == "chapter":
            current_chapter = uri
            active_blocks.clear()
            active_categories.clear()
            parent_uri = None
        elif ck == "block":
            block_id = row.get("block_id") or ""
            block_depth = depth
            m = re.search(r"BlockL(\d+)", block_id)
            if m:
                block_depth = int(m.group(1))
            if block_depth <= 1:
                parent_uri = current_chapter
            else:
                parent_uri = active_blocks.get(block_depth - 1, current_chapter)
            active_blocks[block_depth] = uri
            for k in list(active_blocks.keys()):
                if k > block_depth:
                    del active_blocks[k]
            active_categories.clear()
        elif ck == "category":
            cat_depth = depth
            if cat_depth <= 1:
                if active_blocks:
                    parent_uri = active_blocks[max(active_blocks.keys())]
                else:
                    parent_uri = current_chapter
            else:
                parent_uri = active_categories.get(cat_depth - 1)
                if not parent_uri:
                    if active_categories:
                        parent_uri = active_categories[max(active_categories.keys())]
                    elif active_blocks:
                        parent_uri = active_blocks[max(active_blocks.keys())]
                    else:
                        parent_uri = current_chapter
            active_categories[cat_depth] = uri
            for k in list(active_categories.keys()):
                if k > cat_depth:
                    del active_categories[k]

        row["parent_linearization_uri"] = parent_uri
    return rows


def parse_icd11_file(filepath: str, release: str) -> tuple[list[dict], dict[str, int]]:
    """Parse ICD-11 MMS tabulation file and validate class_kind counts."""
    rows = []
    with open(filepath, "r", encoding="utf-8-sig") as f:
        reader = csv.DictReader(f, delimiter="\t")
        for r in reader:
            ck = (r.get("ClassKind") or "").strip().lower()
            code = (r.get("Code") or "").strip() or None
            block_id = (r.get("BlockId") or "").strip() or None
            raw_title = r.get("Title") or ""
            title = strip_title(raw_title)

            depth_str = (r.get("DepthInKind") or "").strip()
            depth = int(depth_str) if depth_str.isdigit() else None

            is_res = (r.get("IsResidual") or "").strip().lower() in ("true", "1", "t")
            chap_no = (r.get("ChapterNo") or "").strip() or None
            is_lf = (r.get("isLeaf") or "").strip().lower() in ("true", "1", "t")

            foundation_uri = (r.get("Foundation URI") or "").strip() or None
            linearization_uri = (r.get("Linearization URI") or "").strip()

            if not linearization_uri:
                continue

            rows.append({
                "foundation_uri": foundation_uri,
                "linearization_uri": linearization_uri,
                "code": code,
                "block_id": block_id,
                "title": title,
                "class_kind": ck,
                "depth_in_kind": depth,
                "is_residual": is_res,
                "chapter_no": chap_no,
                "is_leaf": is_lf,
                "release_version": release,
            })

    counts = Counter(r["class_kind"] for r in rows)
    valid_kinds = {"chapter", "block", "category"}
    for kind in counts:
        if kind not in valid_kinds:
            raise ValueError(f"Unexpected class_kind '{kind}' in {filepath}")

    if sum(counts.values()) != len(rows):
        raise ValueError(f"Count sum mismatch: {sum(counts.values())} vs {len(rows)}")

    # Verify code is null for chapters and blocks
    for r in rows:
        ck = r["class_kind"]
        code = r.get("code")
        if ck in ("chapter", "block") and code:
            raise ValueError(f"ClassKind {ck} must have NULL code, found: {code}")
        if ck == "category" and not code:
            raise ValueError(f"Category {r.get('linearization_uri')} missing code")

    # If full 2025-01 dataset (36,782 rows)
    if release == "2025-01" and len(rows) >= 30000:
        if counts["chapter"] != 28 or counts["block"] != 1415 or counts["category"] != 35339:
            raise ValueError(
                f"WHO 2025-01 expected 28 chapters, 1415 blocks, 35339 categories, got: {counts}"
            )

    rows = derive_parent_linearization_uris(rows)
    return rows, dict(counts)


def map_country_row(row: dict) -> dict | None:
    """Map country CSV row using only allowed columns."""
    filtered = {k: v for k, v in row.items() if k in ALLOWED_COUNTRY_COLUMNS}
    code = (filtered.get("ISO3166-1-Alpha-2") or "").strip()
    name = (filtered.get("official_name_en") or "").strip()
    if not code:
        return None
    return {
        "code": code,
        "name": name,
        "is_default": "true" if code.upper() == "IN" else "false",
        "status": "true",
        "synchronized": "false",
    }


def parse_country_file(filepath: str) -> list[dict]:
    """Parse country codes CSV using only allowed columns."""
    rows = []
    with open(filepath, "r", encoding="utf-8-sig") as f:
        reader = csv.DictReader(f)
        for r in reader:
            mapped = map_country_row(r)
            if mapped:
                rows.append(mapped)
    return rows


def generate_test_data() -> tuple[list[dict], list[dict], list[dict]]:
    """Generate test master data for unit, departments, and specializations without hard-coded IDs."""
    test_unit = [{
        "code": "TEST-UNIT",
        "name": "TEST UNIT - NOT REAL DATA",
        "description": "TEST DATA",
        "status": "true",
        "synchronized": "false",
    }]

    test_departments = []
    test_specializations = []
    for dept_name, is_clinical in TEST_DEPARTMENTS:
        clean_code = re.sub(r"[^A-Za-z0-9]", "", dept_name).upper()[:8]

        test_departments.append({
            "code": f"TEST-DEPT-{clean_code}",
            "description": f"{dept_name} - TEST DATA",
            "isclinical": "true" if is_clinical else "false",
            "status": "true",
            "synchronized": "false",
        })

        test_specializations.append({
            "code": f"TEST-SPEC-{clean_code}",
            "description": f"{dept_name} - TEST DATA",
            "is_doctor_show": "true",
            "is_department_show": "true",
            "status": "true",
            "synchronized": "false",
        })

    return test_unit, test_departments, test_specializations


def generate_icd11_sql(csv_path: str, release_version: str, source_url: str, file_sha256: str, rows_read: int, message: str) -> str:
    """Generate SQL script for staging and upserting ICD-11 data."""
    started_at = datetime.datetime.now(datetime.timezone.utc).isoformat()
    return f"""
BEGIN;
SET search_path TO ihms_master, public;

CREATE TEMP TABLE staging_icd11 (
    foundation_uri VARCHAR(500),
    linearization_uri VARCHAR(500),
    code VARCHAR(32),
    block_id VARCHAR(64),
    title TEXT,
    class_kind VARCHAR(32),
    depth_in_kind INTEGER,
    is_residual BOOLEAN,
    chapter_no VARCHAR(16),
    is_leaf BOOLEAN,
    parent_linearization_uri VARCHAR(500),
    release_version VARCHAR(64)
);

\\copy staging_icd11 FROM '{csv_path}' WITH (FORMAT csv, HEADER true, QUOTE '"', ESCAPE '\\');

INSERT INTO ihms_master.icd11_mms (
    foundation_uri, linearization_uri, code, block_id, title,
    class_kind, depth_in_kind, is_residual, chapter_no, is_leaf,
    parent_linearization_uri, release_version, status, synchronized
)
SELECT
    foundation_uri, linearization_uri, code, block_id, title,
    class_kind, depth_in_kind, is_residual, chapter_no, is_leaf,
    parent_linearization_uri, release_version, TRUE, FALSE
FROM staging_icd11
ON CONFLICT (linearization_uri) DO UPDATE SET
    foundation_uri = EXCLUDED.foundation_uri,
    code = EXCLUDED.code,
    block_id = EXCLUDED.block_id,
    title = EXCLUDED.title,
    class_kind = EXCLUDED.class_kind,
    depth_in_kind = EXCLUDED.depth_in_kind,
    is_residual = EXCLUDED.is_residual,
    chapter_no = EXCLUDED.chapter_no,
    is_leaf = EXCLUDED.is_leaf,
    parent_linearization_uri = EXCLUDED.parent_linearization_uri,
    release_version = EXCLUDED.release_version,
    updated_datetime = CURRENT_TIMESTAMP;

INSERT INTO ihms_master.master_import_log (
    dataset, source_url, release_version, file_sha256,
    rows_read, rows_inserted, rows_updated, started_at, finished_at, status, message
) VALUES (
    'icd11', '{source_url}', '{release_version}', '{file_sha256}',
    {rows_read}, {rows_read}, 0, '{started_at}', CURRENT_TIMESTAMP, 'SUCCESS',
    '{message}'
);

COMMIT;
"""


def generate_countries_sql(csv_path: str, source_url: str, file_sha256: str, rows_read: int, message: str) -> str:
    """Generate SQL script for staging and upserting country data."""
    started_at = datetime.datetime.now(datetime.timezone.utc).isoformat()
    return f"""
BEGIN;
SET search_path TO ihms_master, public;

CREATE TEMP TABLE staging_geo_country (
    code VARCHAR(8),
    name VARCHAR(120),
    is_default BOOLEAN,
    status BOOLEAN,
    synchronized BOOLEAN
);

\\copy staging_geo_country FROM '{csv_path}' WITH (FORMAT csv, HEADER true, QUOTE '"', ESCAPE '\\');

INSERT INTO ihms_master.geo_country (
    id, code, name, is_default, status, synchronized, added_datetime, updated_datetime
)
SELECT
    COALESCE(existing.id, nextval('ihms_master.geo_country_seq')),
    staging.code,
    staging.name,
    staging.is_default,
    staging.status,
    staging.synchronized,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM staging_geo_country staging
LEFT JOIN ihms_master.geo_country existing ON existing.code = staging.code
ON CONFLICT (code) DO UPDATE SET
    name = EXCLUDED.name,
    updated_datetime = CURRENT_TIMESTAMP;

INSERT INTO ihms_master.master_import_log (
    dataset, source_url, release_version, file_sha256,
    rows_read, rows_inserted, rows_updated, started_at, finished_at, status, message
) VALUES (
    'countries', '{source_url}', NULL, '{file_sha256}',
    {rows_read}, {rows_read}, 0, '{started_at}', CURRENT_TIMESTAMP, 'SUCCESS',
    '{message}'
);

COMMIT;
"""


def generate_testdata_sql(unit_csv: str, dept_csv: str, spec_csv: str, file_sha256: str, rows_read: int, message: str) -> str:
    """Generate SQL script for staging and upserting test master data."""
    started_at = datetime.datetime.now(datetime.timezone.utc).isoformat()
    return f"""
BEGIN;
SET search_path TO ihms_master, public;

CREATE TEMP TABLE staging_test_unit (
    code VARCHAR(255),
    name VARCHAR(255),
    description VARCHAR(500),
    status BOOLEAN,
    synchronized BOOLEAN
);
\\copy staging_test_unit FROM '{unit_csv}' WITH (FORMAT csv, HEADER true, QUOTE '"', ESCAPE '\\');

CREATE TEMP TABLE staging_test_dept (
    code VARCHAR(255),
    description VARCHAR(500),
    isclinical BOOLEAN,
    status BOOLEAN,
    synchronized BOOLEAN
);
\\copy staging_test_dept FROM '{dept_csv}' WITH (FORMAT csv, HEADER true, QUOTE '"', ESCAPE '\\');

CREATE TEMP TABLE staging_test_spec (
    code VARCHAR(64),
    description VARCHAR(255),
    is_doctor_show BOOLEAN,
    is_department_show BOOLEAN,
    status BOOLEAN,
    synchronized BOOLEAN
);
\\copy staging_test_spec FROM '{spec_csv}' WITH (FORMAT csv, HEADER true, QUOTE '"', ESCAPE '\\');

-- 1. Test Unit: Update existing test unit (M_UNITMASTER has no unique constraint on CODE)
UPDATE ihms_master."M_UNITMASTER" u
SET
    "NAME" = staging.name,
    "DESCRIPTION" = staging.description,
    "STATUS" = staging.status,
    "UPDATEDDATETIME" = CURRENT_TIMESTAMP
FROM staging_test_unit staging
WHERE u."CODE" = staging.code
  AND u."CODE" = 'TEST-UNIT'
  AND u."CODE" LIKE 'TEST-%';

-- 1. Test Unit: Insert test unit if absent using sequence nextval
INSERT INTO ihms_master."M_UNITMASTER" (
    "ID", "UnitID", "CODE", "NAME", "DESCRIPTION", "STATUS", "SYNCHRONIZED", "ADDEDDATETIME"
)
SELECT
    staging.new_id,
    staging.new_id,
    staging.code,
    staging.name,
    staging.description,
    staging.status,
    staging.synchronized,
    CURRENT_TIMESTAMP
FROM (
    SELECT
        nextval('ihms_master."M_UNITMASTER_SEQ"') AS new_id,
        code, name, description, status, synchronized
    FROM staging_test_unit
    WHERE code = 'TEST-UNIT'
      AND NOT EXISTS (
          SELECT 1 FROM ihms_master."M_UNITMASTER" existing WHERE existing."CODE" = 'TEST-UNIT'
      )
) staging;

-- 2. Test Departments: Match on ("UnitID", "CODE") by lookup and update existing
UPDATE ihms_master."M_DEPARTMENTMASTER" d
SET
    "DESCRIPTION" = staging.description,
    "ISCLINICAL" = staging.isclinical,
    "STATUS" = staging.status,
    "UPDATEDDATETIME" = CURRENT_TIMESTAMP
FROM staging_test_dept staging,
     (SELECT "ID" AS unit_id FROM ihms_master."M_UNITMASTER" WHERE "CODE" = 'TEST-UNIT' LIMIT 1) u
WHERE d."UnitID" = u.unit_id
  AND d."CODE" = staging.code
  AND d."CODE" LIKE 'TEST-%';

-- 2. Test Departments: Match on ("UnitID", "CODE") by lookup and insert absent using sequence nextval
INSERT INTO ihms_master."M_DEPARTMENTMASTER" (
    "ID", "UnitID", "CODE", "DESCRIPTION", "ISCLINICAL", "STATUS", "SYNCHRONIZED", "ADDEDDATETIME"
)
SELECT
    nextval('ihms_master."M_DEPARTMENTMASTER_SEQ"'),
    u.unit_id,
    staging.code,
    staging.description,
    staging.isclinical,
    staging.status,
    staging.synchronized,
    CURRENT_TIMESTAMP
FROM staging_test_dept staging
CROSS JOIN (
    SELECT "ID" AS unit_id FROM ihms_master."M_UNITMASTER" WHERE "CODE" = 'TEST-UNIT' LIMIT 1
) u
WHERE NOT EXISTS (
    SELECT 1
    FROM ihms_master."M_DEPARTMENTMASTER" existing
    WHERE existing."UnitID" = u.unit_id
      AND existing."CODE" = staging.code
);

-- 3. Test Specializations: Use real unique constraint uq_specialization_unit_code (unit_id, code)
INSERT INTO ihms_master.specialization (
    id, unit_id, code, description, is_doctor_show, is_department_show, status, synchronized, added_datetime
)
SELECT
    nextval('ihms_master.specialization_seq'),
    u.unit_id,
    staging.code,
    staging.description,
    staging.is_doctor_show,
    staging.is_department_show,
    staging.status,
    staging.synchronized,
    CURRENT_TIMESTAMP
FROM staging_test_spec staging
CROSS JOIN (
    SELECT "ID" AS unit_id FROM ihms_master."M_UNITMASTER" WHERE "CODE" = 'TEST-UNIT' LIMIT 1
) u
ON CONFLICT (unit_id, code) DO UPDATE SET
    description = EXCLUDED.description,
    is_doctor_show = EXCLUDED.is_doctor_show,
    is_department_show = EXCLUDED.is_department_show,
    status = EXCLUDED.status,
    updated_datetime = CURRENT_TIMESTAMP
WHERE specialization.code LIKE 'TEST-%';

INSERT INTO ihms_master.master_import_log (
    dataset, source_url, release_version, file_sha256,
    rows_read, rows_inserted, rows_updated, started_at, finished_at, status, message
) VALUES (
    'testdata', 'synthetic', NULL, '{file_sha256}',
    {rows_read}, {rows_read}, 0, '{started_at}', CURRENT_TIMESTAMP, 'SUCCESS',
    '{message}'
);

COMMIT;
"""


def write_csv(data: list[dict], fieldnames: list[str]) -> str:
    """Write list of dicts to a temporary CSV file and return its path."""
    fd, path = tempfile.mkstemp(suffix=".csv", prefix="hims_import_")
    with os.fdopen(fd, "w", newline="", encoding="utf-8") as f:
        writer = csv.DictWriter(f, fieldnames=fieldnames, quoting=csv.QUOTE_MINIMAL)
        writer.writeheader()
        writer.writerows(data)
    return path


def execute_psql(sql: str) -> None:
    """Execute SQL script via psql using environment credentials."""
    cmd = get_psql_cmd()
    proc = subprocess.run(cmd, input=sql, text=True, capture_output=True)
    if proc.returncode != 0:
        sys.stderr.write(f"psql failed with code {proc.returncode}:\n{proc.stderr}\n")
        raise RuntimeError(f"psql execution failed: {proc.stderr}")


def handle_icd11(args: argparse.Namespace) -> int:
    filepath = args.file
    release = args.release
    dry_run = args.dry_run

    sha256_hash = compute_sha256(filepath)
    rows, counts = parse_icd11_file(filepath, release)

    msg = f"ICD-11 release {release}: chapters={counts.get('chapter', 0)}, blocks={counts.get('block', 0)}, categories={counts.get('category', 0)}, total={len(rows)}"
    print(f"[icd11] SHA256: {sha256_hash}")
    print(f"[icd11] Counts: {counts}")
    print(f"[icd11] Total rows read: {len(rows)}")

    if dry_run:
        print("[icd11] Dry-run mode: parsed and validated successfully. Skipping DB execution.")
        return 0

    fields = [
        "foundation_uri", "linearization_uri", "code", "block_id", "title",
        "class_kind", "depth_in_kind", "is_residual", "chapter_no", "is_leaf",
        "parent_linearization_uri", "release_version"
    ]
    csv_path = write_csv(rows, fields)
    try:
        sql = generate_icd11_sql(csv_path, release, filepath, sha256_hash, len(rows), msg)
        execute_psql(sql)
        print(f"[icd11] Successfully imported {len(rows)} records into ihms_master.icd11_mms.")
    finally:
        if os.path.exists(csv_path):
            os.remove(csv_path)
    return 0


def handle_countries(args: argparse.Namespace) -> int:
    filepath = args.file
    dry_run = args.dry_run

    sha256_hash = compute_sha256(filepath)
    rows = parse_country_file(filepath)

    msg = f"Imported {len(rows)} countries from {filepath}"
    print(f"[countries] SHA256: {sha256_hash}")
    print(f"[countries] Total rows read: {len(rows)}")

    if dry_run:
        print("[countries] Dry-run mode: parsed and validated successfully. Skipping DB execution.")
        return 0

    fields = ["code", "name", "is_default", "status", "synchronized"]
    csv_path = write_csv(rows, fields)
    try:
        sql = generate_countries_sql(csv_path, filepath, sha256_hash, len(rows), msg)
        execute_psql(sql)
        print(f"[countries] Successfully imported {len(rows)} countries into ihms_master.geo_country.")
    finally:
        if os.path.exists(csv_path):
            os.remove(csv_path)
    return 0


def handle_testdata(args: argparse.Namespace) -> int:
    dry_run = args.dry_run

    test_units, test_depts, test_specs = generate_test_data()
    total_rows = len(test_units) + len(test_depts) + len(test_specs)
    sha256_hash = hashlib.sha256("TEST UNIT - NOT REAL DATA".encode("utf-8")).hexdigest()

    msg = f"Test data: 1 unit, {len(test_depts)} departments, {len(test_specs)} specializations"
    print(f"[testdata] SHA256: {sha256_hash}")
    print(f"[testdata] Unit count: {len(test_units)}")
    print(f"[testdata] Department count: {len(test_depts)}")
    print(f"[testdata] Specialization count: {len(test_specs)}")
    print(f"[testdata] Total rows generated: {total_rows}")

    if dry_run:
        print("[testdata] Dry-run mode: generated and validated successfully. Skipping DB execution.")
        return 0

    unit_csv = write_csv(test_units, ["code", "name", "description", "status", "synchronized"])
    dept_csv = write_csv(test_depts, ["code", "description", "isclinical", "status", "synchronized"])
    spec_csv = write_csv(test_specs, ["code", "description", "is_doctor_show", "is_department_show", "status", "synchronized"])

    try:
        sql = generate_testdata_sql(unit_csv, dept_csv, spec_csv, sha256_hash, total_rows, msg)
        execute_psql(sql)
        print(f"[testdata] Successfully imported test unit, departments, and specializations.")
    finally:
        for p in (unit_csv, dept_csv, spec_csv):
            if os.path.exists(p):
                os.remove(p)
    return 0


def main() -> int:
    parser = argparse.ArgumentParser(description="iHMS Master Data Importer")
    subparsers = parser.add_subparsers(dest="subcommand", required=True)

    # Subcommand: icd11
    p_icd11 = subparsers.add_parser("icd11", help="Import WHO ICD-11 MMS tabulation")
    p_icd11.add_argument("file", help="Path to ICD-11 MMS tabulation file")
    p_icd11.add_argument("--release", default="2025-01", help="Release version (default: 2025-01)")
    p_icd11.add_argument("--dry-run", action="store_true", help="Parse and validate only")
    p_icd11.set_defaults(func=handle_icd11)

    # Subcommand: countries
    p_geo = subparsers.add_parser("countries", help="Import country codes into geo_country")
    p_geo.add_argument("file", help="Path to country codes CSV file")
    p_geo.add_argument("--dry-run", action="store_true", help="Parse and validate only")
    p_geo.set_defaults(func=handle_countries)

    # Subcommand: testdata
    p_test = subparsers.add_parser("testdata", help="Insert standard test unit, departments, specializations")
    p_test.add_argument("--dry-run", action="store_true", help="Generate and validate only")
    p_test.set_defaults(func=handle_testdata)

    args = parser.parse_args()
    return args.func(args)


if __name__ == "__main__":
    sys.exit(main())
