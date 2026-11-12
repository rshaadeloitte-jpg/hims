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
from decimal import Decimal, InvalidOperation
import hashlib
import os
import re
import subprocess
import sys
import tempfile
import time
from collections import Counter
import xml.etree.ElementTree as ET
import zipfile

try:
    import lgd_hierarchy
except ImportError:
    from platform.data_import import lgd_hierarchy


ALLOWED_COUNTRY_COLUMNS = (
    "ISO3166-1-Alpha-2",
    "ISO3166-1-Alpha-3",
    "ISO3166-1-numeric",
    "official_name_en",
    "Dial",
)

EXPECTED_STATE_HEADERS = [
    "S.No.",
    "State Code",
    "State Version",
    "State Name (In English)",
    "State Name (In Local)",
    "Census 2001 Code",
    "Census 2011 Code",
    "State or UT",
]

EXPECTED_DISTRICT_HEADERS = [
    "S.No.",
    "State Code",
    "State Name (In English)",
    "District Code",
    "District Name(In English)",
    "Census 2001 Code",
    "Census 2011 Code",
]

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


def to_int_str(val: object, field_name: str = "value") -> str:
    """Convert number like '35.0' or '35' to integer string '35'.

    Rejects non-integer values (e.g. '35.5') and non-numeric strings.
    """
    if val is None:
        raise ValueError(f"Expected integer for {field_name}, got None")
    s = str(val).strip()
    if not s:
        raise ValueError(f"Expected integer for {field_name}, got empty string")
    try:
        d = Decimal(s)
    except InvalidOperation:
        raise ValueError(f"Invalid integer string for {field_name}: '{val}'")
    if d != d.to_integral_value():
        raise ValueError(f"Non-integer value for {field_name}: '{val}'")
    return str(int(d))


def parse_census_code(val: object, field_name: str = "Census 2011 Code") -> str | None:
    """Convert Census 2011 code to integer string or None when 0 or empty."""
    if val is None or not str(val).strip():
        return None
    s = to_int_str(val, field_name=field_name)
    if s == "0":
        return None
    return s


def col_letter_to_index(col_str: str) -> int:
    """Convert Excel column letters (A, B, ..., Z, AA) to 0-based index."""
    idx = 0
    for c in col_str.upper():
        if "A" <= c <= "Z":
            idx = idx * 26 + (ord(c) - ord("A") + 1)
    return idx - 1


def col_index_to_letter(col_idx: int) -> str:
    """Convert 0-based column index to Excel column letters (0 -> A, 27 -> AB)."""
    res = ""
    col_idx += 1
    while col_idx > 0:
        col_idx, rem = divmod(col_idx - 1, 26)
        res = chr(ord("A") + rem) + res
    return res


def read_shared_strings(zf: zipfile.ZipFile) -> list[str]:
    """Read sharedStrings.xml from an xlsx archive if present."""
    shared_strings = []
    try:
        with zf.open("xl/sharedStrings.xml") as f:
            tree = ET.parse(f)
            root = tree.getroot()
            for si in root:
                text = "".join(
                    elem.text or ""
                    for elem in si.iter()
                    if elem.tag.endswith("}t") or elem.tag == "t"
                )
                shared_strings.append(text)
    except KeyError:
        pass
    return shared_strings


def get_first_sheet_path(zf: zipfile.ZipFile) -> str:
    """Determine the archive path of the first worksheet in an xlsx file."""
    try:
        with zf.open("xl/workbook.xml") as f:
            wb_root = ET.parse(f).getroot()
        first_sheet = None
        for elem in wb_root.iter():
            if elem.tag.endswith("}sheet") or elem.tag == "sheet":
                first_sheet = elem
                break
        if first_sheet is not None:
            r_id = None
            for k, v in first_sheet.attrib.items():
                if k.endswith("}id") or k == "id":
                    r_id = v
                    break
            if r_id:
                with zf.open("xl/_rels/workbook.xml.rels") as f:
                    rels_root = ET.parse(f).getroot()
                for rel in rels_root:
                    if rel.attrib.get("Id") == r_id:
                        target = rel.attrib.get("Target", "")
                        if target.startswith("/"):
                            return target.lstrip("/")
                        elif target.startswith("xl/"):
                            return target
                        else:
                            return f"xl/{target}"
    except Exception:
        pass

    namelist = zf.namelist()
    if "xl/worksheets/sheet1.xml" in namelist:
        return "xl/worksheets/sheet1.xml"
    sheet_files = sorted(
        n for n in namelist if n.startswith("xl/worksheets/sheet") and n.endswith(".xml")
    )
    if sheet_files:
        return sheet_files[0]
    raise ValueError("No worksheet found in xlsx archive")


def read_xlsx_rows(filepath: str) -> list[list[str]]:
    """Read the first worksheet of an .xlsx file using zipfile and xml.etree.

    Returns a list of rows, each represented as a list of string cell values.
    """
    with zipfile.ZipFile(filepath, "r") as zf:
        shared_strings = read_shared_strings(zf)
        sheet_path = get_first_sheet_path(zf)
        with zf.open(sheet_path) as f:
            sheet_root = ET.parse(f).getroot()

    sheet_data = None
    for elem in sheet_root.iter():
        if elem.tag.endswith("}sheetData") or elem.tag == "sheetData":
            sheet_data = elem
            break

    if sheet_data is None:
        return []

    rows: list[list[str]] = []
    for row_elem in sheet_data:
        if not (row_elem.tag.endswith("}row") or row_elem.tag == "row"):
            continue

        cells: dict[int, str] = {}
        max_col_idx = -1
        current_col_idx = 0

        for c in row_elem:
            if not (c.tag.endswith("}c") or c.tag == "c"):
                continue

            r_attr = c.attrib.get("r", "")
            m = re.match(r"^([A-Za-z]+)", r_attr)
            if m:
                col_idx = col_letter_to_index(m.group(1))
            else:
                col_idx = current_col_idx
            current_col_idx = col_idx + 1

            t_attr = c.attrib.get("t", "")
            v_elem = None
            is_elem = None
            for child in c:
                tag = child.tag
                if tag.endswith("}v") or tag == "v":
                    v_elem = child
                elif tag.endswith("}is") or tag == "is":
                    is_elem = child

            val = ""
            if t_attr == "s":
                if v_elem is not None and v_elem.text is not None:
                    s_idx = int(v_elem.text)
                    if 0 <= s_idx < len(shared_strings):
                        val = shared_strings[s_idx]
            elif t_attr == "inlineStr":
                if is_elem is not None:
                    val = "".join(
                        elem.text or ""
                        for elem in is_elem.iter()
                        if elem.tag.endswith("}t") or elem.tag == "t"
                    )
            elif t_attr == "b":
                val = "true" if v_elem is not None and v_elem.text == "1" else "false"
            else:
                val = v_elem.text if v_elem is not None and v_elem.text is not None else ""

            cells[col_idx] = val
            if col_idx > max_col_idx:
                max_col_idx = col_idx

        if max_col_idx >= 0:
            rows.append([cells.get(i, "") for i in range(max_col_idx + 1)])
        else:
            rows.append([])

    return rows


def find_header_col(
    header_map: dict[str, int],
    name: str,
    alternatives: list[str] | None = None,
    filepath: str = "file",
) -> int:
    """Find column index by header name (case-insensitive and whitespace-normalized).

    Fails clearly if header is missing.
    """
    candidates = [name] + (alternatives or [])
    for cand in candidates:
        if cand in header_map:
            return header_map[cand]
        cand_norm = re.sub(r"\s+", " ", cand).strip().lower()
        for k, v in header_map.items():
            k_norm = re.sub(r"\s+", " ", k).strip().lower()
            if k_norm == cand_norm:
                return v
    raise ValueError(f"Missing required header '{name}' in {filepath}")


def parse_lgd_states(filepath: str) -> list[dict]:
    """Parse LGD states export (.xlsx).

    Row 1 is title, row 2 is header. Finds columns by name.
    Converts numbers like '35.0' to '35' and rejects non-integer values.
    """
    rows = read_xlsx_rows(filepath)
    if len(rows) < 2:
        raise ValueError(f"States file {filepath} must contain at least title and header rows")

    raw_headers = rows[1]
    header_map = {col.strip(): idx for idx, col in enumerate(raw_headers) if col.strip()}

    for h in EXPECTED_STATE_HEADERS:
        find_header_col(header_map, h, filepath=filepath)

    st_code_idx = find_header_col(header_map, "State Code", filepath=filepath)
    st_name_idx = find_header_col(header_map, "State Name (In English)", filepath=filepath)

    states = []
    for row_idx, row in enumerate(rows[2:], start=3):
        if not any(cell.strip() for cell in row):
            continue
        raw_code = row[st_code_idx] if st_code_idx < len(row) else ""
        raw_name = row[st_name_idx] if st_name_idx < len(row) else ""

        code = to_int_str(raw_code, field_name=f"State Code at row {row_idx}")
        name = raw_name.strip()
        if not name:
            raise ValueError(f"Empty State Name at row {row_idx} in {filepath}")

        states.append({
            "code": code,
            "name": name,
        })

    return states


def parse_lgd_districts(filepath: str) -> list[dict]:
    """Parse LGD districts export (.xlsx).

    Row 1 is title, row 2 is header. Finds columns by name.
    Converts numbers like '35.0' to '35' and rejects non-integer values.
    """
    rows = read_xlsx_rows(filepath)
    if len(rows) < 2:
        raise ValueError(f"Districts file {filepath} must contain at least title and header rows")

    raw_headers = rows[1]
    header_map = {col.strip(): idx for idx, col in enumerate(raw_headers) if col.strip()}

    for h in EXPECTED_DISTRICT_HEADERS:
        alts = ["District Name (In English)"] if h == "District Name(In English)" else None
        find_header_col(header_map, h, alternatives=alts, filepath=filepath)

    st_code_idx = find_header_col(header_map, "State Code", filepath=filepath)
    dist_code_idx = find_header_col(header_map, "District Code", filepath=filepath)
    dist_name_idx = find_header_col(
        header_map,
        "District Name(In English)",
        alternatives=["District Name (In English)"],
        filepath=filepath,
    )
    census_idx = find_header_col(header_map, "Census 2011 Code", filepath=filepath)

    districts = []
    for row_idx, row in enumerate(rows[2:], start=3):
        if not any(cell.strip() for cell in row):
            continue
        raw_st_code = row[st_code_idx] if st_code_idx < len(row) else ""
        raw_dist_code = row[dist_code_idx] if dist_code_idx < len(row) else ""
        raw_name = row[dist_name_idx] if dist_name_idx < len(row) else ""
        raw_census = row[census_idx] if census_idx < len(row) else ""

        state_code = to_int_str(raw_st_code, field_name=f"State Code at row {row_idx}")
        code = to_int_str(raw_dist_code, field_name=f"District Code at row {row_idx}")
        name = raw_name.strip()
        if not name:
            raise ValueError(f"Empty District Name at row {row_idx} in {filepath}")
        census_2011_code = parse_census_code(raw_census, field_name=f"Census 2011 Code at row {row_idx}")

        districts.append({
            "state_code": state_code,
            "code": code,
            "name": name,
            "census_2011_code": census_2011_code or "",
        })

    return districts


def validate_lgd(states: list[dict], districts: list[dict], exact_counts: bool = True) -> None:
    """Validate LGD dataset before database operations.

    Checks:
    - Exactly 36 states and 790 districts (when exact_counts is True)
    - No duplicate state codes
    - No duplicate district codes
    - Every district maps to a loaded state
    """
    if exact_counts:
        if len(states) != 36:
            raise ValueError(f"LGD validation failed: expected exactly 36 states, got {len(states)}")
        if len(districts) != 790:
            raise ValueError(f"LGD validation failed: expected exactly 790 districts, got {len(districts)}")

    state_codes = set()
    for s in states:
        code = s["code"]
        if code in state_codes:
            raise ValueError(f"LGD validation failed: duplicate state code '{code}'")
        state_codes.add(code)

    district_codes = set()
    for d in districts:
        code = d["code"]
        if code in district_codes:
            raise ValueError(f"LGD validation failed: duplicate district code '{code}'")
        district_codes.add(code)

        st_code = d["state_code"]
        if st_code not in state_codes:
            raise ValueError(
                f"LGD validation failed: district '{d.get('name')}' (code {code}) references unknown state code '{st_code}'"
            )


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

WITH upserted AS (
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
        updated_datetime = CURRENT_TIMESTAMP
    RETURNING (xmax = 0) AS inserted
),
counts AS (
    SELECT
        COUNT(*) FILTER (WHERE inserted) AS rows_inserted,
        COUNT(*) FILTER (WHERE NOT inserted) AS rows_updated
    FROM upserted
)
INSERT INTO ihms_master.master_import_log (
    dataset, source_url, release_version, file_sha256,
    rows_read, rows_inserted, rows_updated, started_at, finished_at, status, message
)
SELECT
    'icd11', '{source_url}', '{release_version}', '{file_sha256}',
    {rows_read}, counts.rows_inserted, counts.rows_updated, '{started_at}', CURRENT_TIMESTAMP, 'SUCCESS',
    '{message}'
FROM counts;

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

WITH upserted AS (
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
        updated_datetime = CURRENT_TIMESTAMP
    RETURNING (xmax = 0) AS inserted
),
counts AS (
    SELECT
        COUNT(*) FILTER (WHERE inserted) AS rows_inserted,
        COUNT(*) FILTER (WHERE NOT inserted) AS rows_updated
    FROM upserted
)
INSERT INTO ihms_master.master_import_log (
    dataset, source_url, release_version, file_sha256,
    rows_read, rows_inserted, rows_updated, started_at, finished_at, status, message
)
SELECT
    'countries', '{source_url}', NULL, '{file_sha256}',
    {rows_read}, counts.rows_inserted, counts.rows_updated, '{started_at}', CURRENT_TIMESTAMP, 'SUCCESS',
    '{message}'
FROM counts;

COMMIT;
"""


def generate_testdata_sql(unit_csv: str, dept_csv: str, spec_csv: str, file_sha256: str, rows_read: int, message: str) -> str:
    """Generate SQL script for staging and upserting test master data."""
    started_at = datetime.datetime.now(datetime.timezone.utc).isoformat()
    return f"""
BEGIN;
SET search_path TO ihms_master, public;

CREATE TEMP TABLE staging_counts (
    inserted INT,
    updated INT
) ON COMMIT DROP;

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
WITH upd AS (
    UPDATE ihms_master."M_UNITMASTER" u
    SET
        "NAME" = staging.name,
        "DESCRIPTION" = staging.description,
        "STATUS" = staging.status,
        "UPDATEDDATETIME" = CURRENT_TIMESTAMP
    FROM staging_test_unit staging
    WHERE u."CODE" = staging.code
      AND u."CODE" = 'TEST-UNIT'
      AND u."CODE" LIKE 'TEST-%'
    RETURNING 1
)
INSERT INTO staging_counts (inserted, updated)
SELECT 0, COUNT(*) FROM upd;

-- 1. Test Unit: Insert test unit if absent using sequence nextval
WITH ins AS (
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
    ) staging
    RETURNING 1
)
INSERT INTO staging_counts (inserted, updated)
SELECT COUNT(*), 0 FROM ins;

-- 2. Test Departments: Match on ("UnitID", "CODE") by lookup and update existing
WITH upd AS (
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
      AND d."CODE" LIKE 'TEST-%'
    RETURNING 1
)
INSERT INTO staging_counts (inserted, updated)
SELECT 0, COUNT(*) FROM upd;

-- 2. Test Departments: Match on ("UnitID", "CODE") by lookup and insert absent using sequence nextval
WITH ins AS (
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
    )
    RETURNING 1
)
INSERT INTO staging_counts (inserted, updated)
SELECT COUNT(*), 0 FROM ins;

-- 3. Test Specializations: Use real unique constraint uq_specialization_unit_code (unit_id, code)
WITH upserted AS (
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
    WHERE specialization.code LIKE 'TEST-%'
    RETURNING (xmax = 0) AS inserted
)
INSERT INTO staging_counts (inserted, updated)
SELECT
    COUNT(*) FILTER (WHERE inserted),
    COUNT(*) FILTER (WHERE NOT inserted)
FROM upserted;

INSERT INTO ihms_master.master_import_log (
    dataset, source_url, release_version, file_sha256,
    rows_read, rows_inserted, rows_updated, started_at, finished_at, status, message
)
SELECT
    'testdata', 'synthetic', NULL, '{file_sha256}',
    {rows_read},
    COALESCE(SUM(inserted), 0),
    COALESCE(SUM(updated), 0),
    '{started_at}',
    CURRENT_TIMESTAMP,
    'SUCCESS',
    '{message}'
FROM staging_counts;

COMMIT;
"""


def generate_lgd_sql(
    states_csv: str,
    districts_csv: str,
    source_url: str,
    file_sha256: str,
    rows_read: int,
    message: str,
) -> str:
    """Generate SQL script for staging and upserting LGD states and districts."""
    started_at = datetime.datetime.now(datetime.timezone.utc).isoformat()
    return f"""
BEGIN;
SET search_path TO ihms_master, public;

-- Ensure country with code 'IN' exists or abort
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM ihms_master.geo_country WHERE code = 'IN') THEN
        RAISE EXCEPTION 'Country with code ''IN'' does not exist in ihms_master.geo_country';
    END IF;
END $$;

CREATE TEMP TABLE staging_counts (
    inserted INT,
    updated INT
) ON COMMIT DROP;

CREATE TEMP TABLE staging_geo_state (
    code VARCHAR(16),
    name VARCHAR(120)
);

\\copy staging_geo_state FROM '{states_csv}' WITH (FORMAT csv, HEADER true, QUOTE '"', ESCAPE '\\');

CREATE TEMP TABLE staging_geo_district (
    state_code VARCHAR(16),
    code VARCHAR(16),
    name VARCHAR(120),
    census_2011_code VARCHAR(16)
);

\\copy staging_geo_district FROM '{districts_csv}' WITH (FORMAT csv, HEADER true, QUOTE '"', ESCAPE '\\');

-- Abort if any district does not map to a loaded state
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM staging_geo_district d
        WHERE NOT EXISTS (
            SELECT 1 FROM staging_geo_state s WHERE s.code = d.state_code
        )
    ) THEN
        RAISE EXCEPTION 'Every district must map to a loaded state';
    END IF;
END $$;

-- Upsert geo_state: conflict target is uq_geo_state_country_code (country_id, code)
WITH upserted_states AS (
    INSERT INTO ihms_master.geo_state (
        id, country_id, code, name, status, synchronized, added_datetime, updated_datetime
    )
    SELECT
        COALESCE(existing.id, nextval('ihms_master.geo_state_seq')),
        c.id,
        staging.code,
        staging.name,
        TRUE,
        FALSE,
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    FROM staging_geo_state staging
    CROSS JOIN (SELECT id FROM ihms_master.geo_country WHERE code = 'IN' LIMIT 1) c
    LEFT JOIN ihms_master.geo_state existing
        ON existing.country_id = c.id AND existing.code = staging.code
    ON CONFLICT (country_id, code) DO UPDATE SET
        name = EXCLUDED.name,
        updated_datetime = CURRENT_TIMESTAMP
    RETURNING (xmax = 0) AS inserted
)
INSERT INTO staging_counts (inserted, updated)
SELECT
    COUNT(*) FILTER (WHERE inserted),
    COUNT(*) FILTER (WHERE NOT inserted)
FROM upserted_states;

-- Upsert geo_district: conflict target is uq_geo_district_state_code (state_id, code)
WITH upserted_districts AS (
    INSERT INTO ihms_master.geo_district (
        id, state_id, code, name, census_2011_code, status, synchronized, added_datetime, updated_datetime
    )
    SELECT
        COALESCE(existing.id, nextval('ihms_master.geo_district_seq')),
        s.id,
        staging.code,
        staging.name,
        NULLIF(staging.census_2011_code, ''),
        TRUE,
        FALSE,
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    FROM staging_geo_district staging
    CROSS JOIN (SELECT id FROM ihms_master.geo_country WHERE code = 'IN' LIMIT 1) c
    JOIN ihms_master.geo_state s
        ON s.country_id = c.id AND s.code = staging.state_code
    LEFT JOIN ihms_master.geo_district existing
        ON existing.state_id = s.id AND existing.code = staging.code
    ON CONFLICT (state_id, code) DO UPDATE SET
        name = EXCLUDED.name,
        census_2011_code = EXCLUDED.census_2011_code,
        updated_datetime = CURRENT_TIMESTAMP
    RETURNING (xmax = 0) AS inserted
)
INSERT INTO staging_counts (inserted, updated)
SELECT
    COUNT(*) FILTER (WHERE inserted),
    COUNT(*) FILTER (WHERE NOT inserted)
FROM upserted_districts;

INSERT INTO ihms_master.master_import_log (
    dataset, source_url, release_version, file_sha256,
    rows_read, rows_inserted, rows_updated, started_at, finished_at, status, message
)
SELECT
    'lgd',
    '{source_url}',
    NULL,
    '{file_sha256}',
    {rows_read},
    COALESCE(SUM(inserted), 0),
    COALESCE(SUM(updated), 0),
    '{started_at}',
    CURRENT_TIMESTAMP,
    'SUCCESS',
    '{message}'
FROM staging_counts;

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


def handle_lgd(args: argparse.Namespace) -> int:
    states_file = args.states_file
    districts_file = args.districts_file
    dry_run = args.dry_run
    skip_count_check = getattr(args, "skip_count_check", False)

    sha_states = compute_sha256(states_file)
    sha_districts = compute_sha256(districts_file)
    combined_sha = hashlib.sha256(f"{sha_states}:{sha_districts}".encode("utf-8")).hexdigest()

    states = parse_lgd_states(states_file)
    districts = parse_lgd_districts(districts_file)

    validate_lgd(states, districts, exact_counts=not skip_count_check)

    total_rows = len(states) + len(districts)
    msg = f"LGD import: {len(states)} states, {len(districts)} districts"
    print(f"[lgd] States SHA256: {sha_states}")
    print(f"[lgd] Districts SHA256: {sha_districts}")
    print(f"[lgd] States parsed: {len(states)}")
    print(f"[lgd] Districts parsed: {len(districts)}")
    print(f"[lgd] Total rows: {total_rows}")

    if dry_run:
        print("[lgd] Dry-run mode: parsed and validated successfully. Skipping DB execution.")
        return 0

    states_csv = write_csv(states, ["code", "name"])
    districts_csv = write_csv(districts, ["state_code", "code", "name", "census_2011_code"])
    source_info = f"{states_file}; {districts_file}"[:500]

    try:
        sql = generate_lgd_sql(states_csv, districts_csv, source_info, combined_sha, total_rows, msg)
        execute_psql(sql)
        print(f"[lgd] Successfully imported {len(states)} states and {len(districts)} districts.")
    finally:
        for p in (states_csv, districts_csv):
            if os.path.exists(p):
                os.remove(p)
    return 0


VALID_LGD_STATE_CODES = {str(i) for i in range(1, 39)}


def check_lgd_hierarchy_orphans(
    state_code: int | str,
    raw_rows: dict[str, list[dict]],
    known_districts: set[str] | None = None,
) -> tuple[dict[str, list[dict]], dict[str, int], set[str], dict[str, list[dict]]]:
    """Validate parent-child relationships, detect collisions, and check 0.5% threshold.

    Returns:
        (valid_rows, skip_counts, colliding_ward_codes, orphans_per_kind)

    Raises:
        RuntimeError: if orphan rows for any kind exceed 0.5% of that kind's rows.
    """
    district_codes = {
        str(r["district_code"]).strip()
        for r in raw_rows.get("districtofSpecificState", [])
        if r.get("district_code")
    }
    if known_districts:
        district_codes.update(known_districts)

    # Sub-districts
    sd_valid = []
    sd_orphans = []
    subdistrict_codes = set()
    for r in raw_rows.get("subDistrictofSpecificState", []):
        d_code = str(r.get("district_code", "")).strip()
        sd_code = str(r.get("subdistrict_code", "")).strip()
        if d_code in district_codes:
            sd_valid.append(r)
            if sd_code:
                subdistrict_codes.add(sd_code)
        else:
            sd_orphans.append(r)

    # Blocks
    block_valid = []
    block_orphans = []
    block_codes = set()
    for r in raw_rows.get("blockofspecificState", []):
        d_code = str(r.get("district_code", "")).strip()
        b_code = str(r.get("block_code", "")).strip()
        if d_code in district_codes:
            block_valid.append(r)
            if b_code:
                block_codes.add(b_code)
        else:
            block_orphans.append(r)

    # Villages
    village_valid = []
    village_orphans = []
    village_codes = set()
    for r in raw_rows.get("villageofSpecificState", []):
        sd_code = str(r.get("sub_district_code", "")).strip()
        v_code = str(r.get("village_code", "")).strip()
        if sd_code in subdistrict_codes:
            village_valid.append(r)
            if v_code:
                village_codes.add(v_code)
        else:
            village_orphans.append(r)

    # Local bodies
    lb_codes = set()
    for kind in ("priLbSpecificState", "ulbSpecificState", "tlbSpecificState"):
        for r in raw_rows.get(kind, []):
            code = str(r.get("localbody_code") or r.get("local_body_code") or "").strip()
            if code:
                lb_codes.add(code)

    # Wards
    pri_ward_valid = []
    pri_ward_orphans = []
    pri_ward_codes = set()
    for r in raw_rows.get("priWards", []):
        lb_c = str(r.get("local_body_code", "")).strip()
        w_c = str(r.get("ward_code", "")).strip()
        if lb_c in lb_codes:
            pri_ward_valid.append(r)
            if w_c:
                pri_ward_codes.add(w_c)
        else:
            pri_ward_orphans.append(r)

    ulb_ward_valid = []
    ulb_ward_orphans = []
    ulb_ward_codes = set()
    for r in raw_rows.get("uLBWardforState", []):
        lb_c = str(r.get("local_body_code", "")).strip()
        w_c = str(r.get("ward_code", "")).strip()
        if lb_c in lb_codes:
            ulb_ward_valid.append(r)
            if w_c:
                ulb_ward_codes.add(w_c)
        else:
            ulb_ward_orphans.append(r)

    # Collision detection across PRI and ULB
    colliding_ward_codes = pri_ward_codes & ulb_ward_codes
    if colliding_ward_codes:
        for c in sorted(colliding_ward_codes):
            sys.stderr.write(f"[lgd-hierarchy] Ward code collision across PRI and ULB: code={c}\n")

    colliding_pri = [r for r in pri_ward_valid if str(r.get("ward_code", "")).strip() in colliding_ward_codes]
    colliding_ulb = [r for r in ulb_ward_valid if str(r.get("ward_code", "")).strip() in colliding_ward_codes]

    pri_ward_valid = [r for r in pri_ward_valid if str(r.get("ward_code", "")).strip() not in colliding_ward_codes]
    ulb_ward_valid = [r for r in ulb_ward_valid if str(r.get("ward_code", "")).strip() not in colliding_ward_codes]

    valid_ward_codes = (pri_ward_codes | ulb_ward_codes) - colliding_ward_codes

    # Ward coverage: skip rows with blank district
    cov_valid = []
    cov_orphans = []
    cov_blank_districts = 0
    for r in raw_rows.get("uLBWardforStateWithCov", []):
        d_code = str(r.get("district_code") or "").strip()
        if not d_code:
            cov_blank_districts += 1
            continue
        w_code = str(r.get("ward_code") or "").strip()
        sd_code = str(r.get("subdistrict_code") or "").strip()
        is_orphan = False
        if w_code not in valid_ward_codes:
            is_orphan = True
        if d_code not in district_codes:
            is_orphan = True
        if sd_code and sd_code not in subdistrict_codes:
            is_orphan = True
        if is_orphan:
            cov_orphans.append(r)
        else:
            cov_valid.append(r)

    # Block-Village
    bv_valid = []
    bv_orphans = []
    for r in raw_rows.get("allBlockStateWithCoveredVillage", []):
        b_code = str(r.get("block_code") or "").strip()
        v_code = str(r.get("village_code") or "").strip()
        if b_code in block_codes and v_code in village_codes:
            bv_valid.append(r)
        else:
            bv_orphans.append(r)

    # Village-GP: skip villages LGD maps to no gram panchayat (blank local body)
    vgp_valid = []
    vgp_orphans = []
    vgp_unmapped = 0
    for r in raw_rows.get("villageGramPanchayatMapping", []):
        v_code = str(r.get("village_code") or "").strip()
        lb_code = str(r.get("local_body_code") or "").strip()
        if not lb_code:
            vgp_unmapped += 1
            continue
        if v_code in village_codes and lb_code in lb_codes:
            vgp_valid.append(r)
        else:
            vgp_orphans.append(r)

    orphans_per_kind: dict[str, list[dict]] = {
        "districtofSpecificState": [],
        "subDistrictofSpecificState": sd_orphans,
        "blockofspecificState": block_orphans,
        "villageofSpecificState": village_orphans,
        "priLbSpecificState": [],
        "ulbSpecificState": [],
        "tlbSpecificState": [],
        "priWards": pri_ward_orphans + colliding_pri,
        "uLBWardforState": ulb_ward_orphans + colliding_ulb,
        "uLBWardforStateWithCov": cov_orphans,
        "allBlockStateWithCoveredVillage": bv_orphans,
        "villageGramPanchayatMapping": vgp_orphans,
    }

    # Evaluate 0.5% threshold per kind
    for kind, o_list in orphans_per_kind.items():
        total_kind_rows = len(raw_rows.get(kind, []))
        if total_kind_rows > 0:
            rate = len(o_list) / total_kind_rows
            if rate > 0.005:
                sys.stderr.write(
                    f"[lgd-hierarchy] Orphan threshold exceeded for {kind}: "
                    f"{len(o_list)} of {total_kind_rows} ({rate:.2%}) > 0.5%\n"
                )
                print(f"[lgd-hierarchy] First 20 orphans for {kind}:")
                for idx, o in enumerate(o_list[:20], 1):
                    print(f"  [{idx}] {o}")
                raise RuntimeError(
                    f"State {state_code} rolled back: orphans for {kind} exceed 0.5% threshold ({len(o_list)}/{total_kind_rows})"
                )

    skip_counts = {
        "districtofSpecificState": 0,
        "subDistrictofSpecificState": len(sd_orphans),
        "blockofspecificState": len(block_orphans),
        "villageofSpecificState": len(village_orphans),
        "priLbSpecificState": 0,
        "ulbSpecificState": 0,
        "tlbSpecificState": 0,
        "priWards": len(pri_ward_orphans) + len(colliding_pri),
        "uLBWardforState": len(ulb_ward_orphans) + len(colliding_ulb),
        "uLBWardforStateWithCov": cov_blank_districts + len(cov_orphans),
        "allBlockStateWithCoveredVillage": len(bv_orphans),
        "villageGramPanchayatMapping": vgp_unmapped + len(vgp_orphans),
    }

    valid_rows = {
        "districtofSpecificState": raw_rows.get("districtofSpecificState", []),
        "subDistrictofSpecificState": sd_valid,
        "blockofspecificState": block_valid,
        "villageofSpecificState": village_valid,
        "priLbSpecificState": raw_rows.get("priLbSpecificState", []),
        "ulbSpecificState": raw_rows.get("ulbSpecificState", []),
        "tlbSpecificState": raw_rows.get("tlbSpecificState", []),
        "priWards": pri_ward_valid,
        "uLBWardforState": ulb_ward_valid,
        "uLBWardforStateWithCov": cov_valid,
        "allBlockStateWithCoveredVillage": bv_valid,
        "villageGramPanchayatMapping": vgp_valid,
    }

    return valid_rows, skip_counts, colliding_ward_codes, orphans_per_kind


def generate_lgd_hierarchy_sql(
    state_code: int | str,
    state_name: str,
    csv_paths: dict[str, str],
    file_sha256: str,
    counts: dict[str, dict[str, int]],
    source_url: str = "",
) -> str:
    """Generate SQL script for staging and upserting LGD full hierarchy in one transaction.

    Stages each kind from temporary CSVs, validates parent relationships, handles
    ward collisions, executes INSERT ... ON CONFLICT with ids from sequence nextval,
    computes inserted and updated counts using RETURNING (xmax = 0), and writes to master_import_log.
    """
    started_at = datetime.datetime.now(datetime.timezone.utc).isoformat()
    state_code_str = str(state_code).strip()
    safe_state_name = state_name.replace("'", "''")
    safe_source_url = source_url.replace("'", "''")

    districts_csv = csv_paths.get("geo_district") or csv_paths.get("districtofSpecificState") or ""
    subdistricts_csv = csv_paths.get("geo_sub_district") or csv_paths.get("subDistrictofSpecificState") or ""
    blocks_csv = csv_paths.get("geo_block") or csv_paths.get("blockofspecificState") or ""
    villages_csv = csv_paths.get("geo_village") or csv_paths.get("villageofSpecificState") or ""
    local_bodies_csv = csv_paths.get("lgd_local_body") or csv_paths.get("priLbSpecificState") or ""
    wards_csv = csv_paths.get("lgd_ward") or csv_paths.get("priWards") or ""
    coverage_csv = csv_paths.get("lgd_ward_coverage") or csv_paths.get("uLBWardforStateWithCov") or ""
    block_village_csv = csv_paths.get("geo_block_village") or csv_paths.get("allBlockStateWithCoveredVillage") or ""
    village_gp_csv = csv_paths.get("lgd_village_gp") or csv_paths.get("villageGramPanchayatMapping") or ""

    # Build per-kind log statements
    log_statements = []
    for kind in lgd_hierarchy.KINDS:
        k_info = counts.get(kind, {})
        read_cnt = k_info.get("read", 0)
        skip_cnt = k_info.get("skipped", 0)
        msg_str = (
            f"State {state_code_str} ({safe_state_name}) {kind}: "
            f"read={read_cnt}, inserted=' || COALESCE((SELECT inserted FROM staging_counts WHERE kind = '{kind}'), 0) || ', "
            f"updated=' || COALESCE((SELECT updated FROM staging_counts WHERE kind = '{kind}'), 0) || ', "
            f"skipped={skip_cnt}"
        )
        stmt = f"""
INSERT INTO ihms_master.master_import_log (
    dataset, source_url, release_version, file_sha256,
    rows_read, rows_inserted, rows_updated, started_at, finished_at, status, message
)
VALUES (
    'lgd_hierarchy:{kind}',
    '{safe_source_url}',
    'State {state_code_str}',
    '{file_sha256}',
    {read_cnt},
    COALESCE((SELECT inserted FROM staging_counts WHERE kind = '{kind}'), 0),
    COALESCE((SELECT updated FROM staging_counts WHERE kind = '{kind}'), 0),
    '{started_at}',
    CURRENT_TIMESTAMP,
    'SUCCESS',
    '{msg_str}'
);"""
        log_statements.append(stmt)

    all_logs = "\n".join(log_statements)

    return f"""
BEGIN;
SET search_path TO ihms_master, public;

-- Verify state exists
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM ihms_master.geo_state WHERE code = '{state_code_str}') THEN
        RAISE EXCEPTION 'State code ''{state_code_str}'' not found in ihms_master.geo_state';
    END IF;
END $$;

CREATE TEMP TABLE staging_counts (
    kind VARCHAR(64),
    inserted INT,
    updated INT
) ON COMMIT DROP;

CREATE TEMP TABLE staging_geo_district (
    code VARCHAR(16),
    name VARCHAR(120),
    census_2011_code VARCHAR(16)
);

CREATE TEMP TABLE staging_geo_sub_district (
    district_code VARCHAR(32),
    code VARCHAR(32),
    lgd_version VARCHAR(16),
    name VARCHAR(255),
    name_local VARCHAR(255),
    census_2001_code VARCHAR(16),
    census_2011_code VARCHAR(16)
);

CREATE TEMP TABLE staging_geo_block (
    district_code VARCHAR(32),
    code VARCHAR(32),
    lgd_version VARCHAR(16),
    name VARCHAR(255),
    name_local VARCHAR(255)
);

CREATE TEMP TABLE staging_geo_village (
    district_code VARCHAR(32),
    sub_district_code VARCHAR(32),
    code VARCHAR(32),
    lgd_version VARCHAR(16),
    name VARCHAR(255),
    name_local VARCHAR(255),
    village_status VARCHAR(64),
    census_2001_code VARCHAR(16),
    census_2011_code VARCHAR(16)
);

CREATE TEMP TABLE staging_lgd_local_body (
    body_category VARCHAR(3),
    type_code VARCHAR(16),
    type_name VARCHAR(255),
    code VARCHAR(32),
    lgd_version VARCHAR(16),
    name VARCHAR(255),
    name_local VARCHAR(255),
    parent_code VARCHAR(32),
    census_2001_code VARCHAR(16),
    census_2011_code VARCHAR(16)
);

CREATE TEMP TABLE staging_lgd_ward (
    local_body_code VARCHAR(32),
    ward_category VARCHAR(3),
    code VARCHAR(32),
    ward_number VARCHAR(64),
    name VARCHAR(255),
    name_local VARCHAR(255)
);

CREATE TEMP TABLE staging_lgd_ward_coverage (
    ward_code VARCHAR(32),
    district_code VARCHAR(32),
    subdistrict_code VARCHAR(32)
);

CREATE TEMP TABLE staging_geo_block_village (
    block_code VARCHAR(32),
    village_code VARCHAR(32)
);

CREATE TEMP TABLE staging_lgd_village_gp (
    village_code VARCHAR(32),
    local_body_code VARCHAR(32)
);

\\copy staging_geo_district FROM '{districts_csv}' WITH (FORMAT csv, HEADER true, QUOTE '"', ESCAPE '\\');
\\copy staging_geo_sub_district FROM '{subdistricts_csv}' WITH (FORMAT csv, HEADER true, QUOTE '"', ESCAPE '\\');
\\copy staging_geo_block FROM '{blocks_csv}' WITH (FORMAT csv, HEADER true, QUOTE '"', ESCAPE '\\');
\\copy staging_geo_village FROM '{villages_csv}' WITH (FORMAT csv, HEADER true, QUOTE '"', ESCAPE '\\');
\\copy staging_lgd_local_body FROM '{local_bodies_csv}' WITH (FORMAT csv, HEADER true, QUOTE '"', ESCAPE '\\');
\\copy staging_lgd_ward FROM '{wards_csv}' WITH (FORMAT csv, HEADER true, QUOTE '"', ESCAPE '\\');
\\copy staging_lgd_ward_coverage FROM '{coverage_csv}' WITH (FORMAT csv, HEADER true, QUOTE '"', ESCAPE '\\');
\\copy staging_geo_block_village FROM '{block_village_csv}' WITH (FORMAT csv, HEADER true, QUOTE '"', ESCAPE '\\');
\\copy staging_lgd_village_gp FROM '{village_gp_csv}' WITH (FORMAT csv, HEADER true, QUOTE '"', ESCAPE '\\');

-- Orphan threshold and collision validation DO block
DO $$
DECLARE
    v_total INT;
    v_orphans INT;
    v_state_id BIGINT;
    r RECORD;
BEGIN
    SELECT id INTO v_state_id FROM ihms_master.geo_state WHERE code = '{state_code_str}';

    -- Check geo_sub_district orphans
    SELECT COUNT(*) INTO v_total FROM staging_geo_sub_district;
    SELECT COUNT(*) INTO v_orphans FROM staging_geo_sub_district st
    WHERE NOT EXISTS (
        SELECT 1 FROM ihms_master.geo_district d
        WHERE d.state_id = v_state_id AND d.code = st.district_code
    ) AND NOT EXISTS (
        SELECT 1 FROM staging_geo_district gd WHERE gd.code = st.district_code
    );
    IF v_total > 0 AND (v_orphans::numeric / v_total::numeric) > 0.005 THEN
        FOR r IN
            SELECT st.code, st.district_code, st.name
            FROM staging_geo_sub_district st
            WHERE NOT EXISTS (
                SELECT 1 FROM ihms_master.geo_district d
                WHERE d.state_id = v_state_id AND d.code = st.district_code
            ) AND NOT EXISTS (
                SELECT 1 FROM staging_geo_district gd WHERE gd.code = st.district_code
            )
            LIMIT 20
        LOOP
            RAISE NOTICE 'Orphan geo_sub_district: code=%, district_code=%, name=%', r.code, r.district_code, r.name;
        END LOOP;
        RAISE EXCEPTION 'Orphans for geo_sub_district exceed 0.5%% threshold: % orphans out of % rows', v_orphans, v_total;
    END IF;

    -- Check geo_block orphans
    SELECT COUNT(*) INTO v_total FROM staging_geo_block;
    SELECT COUNT(*) INTO v_orphans FROM staging_geo_block st
    WHERE NOT EXISTS (
        SELECT 1 FROM ihms_master.geo_district d
        WHERE d.state_id = v_state_id AND d.code = st.district_code
    ) AND NOT EXISTS (
        SELECT 1 FROM staging_geo_district gd WHERE gd.code = st.district_code
    );
    IF v_total > 0 AND (v_orphans::numeric / v_total::numeric) > 0.005 THEN
        FOR r IN
            SELECT st.code, st.district_code, st.name
            FROM staging_geo_block st
            WHERE NOT EXISTS (
                SELECT 1 FROM ihms_master.geo_district d
                WHERE d.state_id = v_state_id AND d.code = st.district_code
            ) AND NOT EXISTS (
                SELECT 1 FROM staging_geo_district gd WHERE gd.code = st.district_code
            )
            LIMIT 20
        LOOP
            RAISE NOTICE 'Orphan geo_block: code=%, district_code=%, name=%', r.code, r.district_code, r.name;
        END LOOP;
        RAISE EXCEPTION 'Orphans for geo_block exceed 0.5%% threshold: % orphans out of % rows', v_orphans, v_total;
    END IF;

    -- Check geo_village orphans
    SELECT COUNT(*) INTO v_total FROM staging_geo_village;
    SELECT COUNT(*) INTO v_orphans FROM staging_geo_village st
    WHERE NOT EXISTS (
        SELECT 1 FROM ihms_master.geo_sub_district sd
        JOIN ihms_master.geo_district d ON sd.district_id = d.id
        WHERE d.state_id = v_state_id AND sd.code = st.sub_district_code
    ) AND NOT EXISTS (
        SELECT 1 FROM staging_geo_sub_district gsd WHERE gsd.code = st.sub_district_code
    );
    IF v_total > 0 AND (v_orphans::numeric / v_total::numeric) > 0.005 THEN
        FOR r IN
            SELECT st.code, st.sub_district_code, st.name
            FROM staging_geo_village st
            WHERE NOT EXISTS (
                SELECT 1 FROM ihms_master.geo_sub_district sd
                JOIN ihms_master.geo_district d ON sd.district_id = d.id
                WHERE d.state_id = v_state_id AND sd.code = st.sub_district_code
            ) AND NOT EXISTS (
                SELECT 1 FROM staging_geo_sub_district gsd WHERE gsd.code = st.sub_district_code
            )
            LIMIT 20
        LOOP
            RAISE NOTICE 'Orphan geo_village: code=%, sub_district_code=%, name=%', r.code, r.sub_district_code, r.name;
        END LOOP;
        RAISE EXCEPTION 'Orphans for geo_village exceed 0.5%% threshold: % orphans out of % rows', v_orphans, v_total;
    END IF;

    -- Check lgd_ward orphans and collisions
    SELECT COUNT(*) INTO v_total FROM staging_lgd_ward;
    SELECT COUNT(*) INTO v_orphans FROM staging_lgd_ward st
    WHERE (NOT EXISTS (
        SELECT 1 FROM ihms_master.lgd_local_body lb
        WHERE lb.state_id = v_state_id AND lb.code = st.local_body_code
    ) AND NOT EXISTS (
        SELECT 1 FROM staging_lgd_local_body lb WHERE lb.code = st.local_body_code
    )) OR st.code IN (
        SELECT code FROM staging_lgd_ward GROUP BY code HAVING COUNT(DISTINCT ward_category) > 1
        UNION
        SELECT w.code FROM ihms_master.lgd_ward w WHERE w.ward_category <> st.ward_category
    );
    IF v_total > 0 AND (v_orphans::numeric / v_total::numeric) > 0.005 THEN
        FOR r IN
            SELECT st.code, st.local_body_code, st.ward_category, st.name
            FROM staging_lgd_ward st
            WHERE (NOT EXISTS (
                SELECT 1 FROM ihms_master.lgd_local_body lb
                WHERE lb.state_id = v_state_id AND lb.code = st.local_body_code
            ) AND NOT EXISTS (
                SELECT 1 FROM staging_lgd_local_body lb WHERE lb.code = st.local_body_code
            )) OR st.code IN (
                SELECT code FROM staging_lgd_ward GROUP BY code HAVING COUNT(DISTINCT ward_category) > 1
                UNION
                SELECT w.code FROM ihms_master.lgd_ward w WHERE w.ward_category <> st.ward_category
            )
            LIMIT 20
        LOOP
            RAISE NOTICE 'Orphan/collision lgd_ward: code=%, local_body_code=%, category=%, name=%', r.code, r.local_body_code, r.ward_category, r.name;
        END LOOP;
        RAISE EXCEPTION 'Orphans/collisions for lgd_ward exceed 0.5%% threshold: % orphans out of % rows', v_orphans, v_total;
    END IF;
END $$;

-- 1. Upsert geo_district
WITH upserted AS (
    INSERT INTO ihms_master.geo_district (
        id, state_id, code, name, census_2011_code, status, synchronized, added_datetime, updated_datetime
    )
    SELECT
        COALESCE(existing.id, nextval('ihms_master.geo_district_seq')),
        s.id,
        staging.code,
        staging.name,
        NULLIF(staging.census_2011_code, ''),
        TRUE,
        FALSE,
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    FROM (
        SELECT DISTINCT ON (code) code, name, census_2011_code
        FROM staging_geo_district
    ) staging
    CROSS JOIN (SELECT id FROM ihms_master.geo_state WHERE code = '{state_code_str}' LIMIT 1) s
    LEFT JOIN ihms_master.geo_district existing
        ON existing.state_id = s.id AND existing.code = staging.code
    ON CONFLICT (state_id, code) DO UPDATE SET
        name = EXCLUDED.name,
        census_2011_code = COALESCE(EXCLUDED.census_2011_code, geo_district.census_2011_code),
        updated_datetime = CURRENT_TIMESTAMP
    RETURNING (xmax = 0) AS inserted
)
INSERT INTO staging_counts (kind, inserted, updated)
SELECT
    'districtofSpecificState',
    COUNT(*) FILTER (WHERE inserted),
    COUNT(*) FILTER (WHERE NOT inserted)
FROM upserted;

-- 2. Upsert geo_sub_district
WITH upserted AS (
    INSERT INTO ihms_master.geo_sub_district (
        id, district_id, code, lgd_version, name, name_local,
        census_2001_code, census_2011_code, status, synchronized, added_datetime, updated_datetime
    )
    SELECT
        COALESCE(existing.id, nextval('ihms_master.geo_sub_district_seq')),
        d.id,
        staging.code,
        CASE WHEN staging.lgd_version ~ '^\\d+$' THEN staging.lgd_version::INTEGER ELSE NULL END,
        staging.name,
        NULLIF(staging.name_local, ''),
        NULLIF(staging.census_2001_code, ''),
        NULLIF(staging.census_2011_code, ''),
        TRUE,
        FALSE,
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    FROM (
        SELECT DISTINCT ON (district_code, code) district_code, code, lgd_version, name, name_local, census_2001_code, census_2011_code
        FROM staging_geo_sub_district
    ) staging
    CROSS JOIN (SELECT id FROM ihms_master.geo_state WHERE code = '{state_code_str}' LIMIT 1) s
    JOIN ihms_master.geo_district d
        ON d.state_id = s.id AND d.code = staging.district_code
    LEFT JOIN ihms_master.geo_sub_district existing
        ON existing.district_id = d.id AND existing.code = staging.code
    ON CONFLICT (district_id, code) DO UPDATE SET
        name = EXCLUDED.name,
        name_local = EXCLUDED.name_local,
        lgd_version = EXCLUDED.lgd_version,
        census_2001_code = EXCLUDED.census_2001_code,
        census_2011_code = EXCLUDED.census_2011_code,
        updated_datetime = CURRENT_TIMESTAMP
    RETURNING (xmax = 0) AS inserted
)
INSERT INTO staging_counts (kind, inserted, updated)
SELECT
    'subDistrictofSpecificState',
    COUNT(*) FILTER (WHERE inserted),
    COUNT(*) FILTER (WHERE NOT inserted)
FROM upserted;

-- 3. Upsert geo_block
WITH upserted AS (
    INSERT INTO ihms_master.geo_block (
        id, district_id, code, lgd_version, name, name_local, status, synchronized, added_datetime, updated_datetime
    )
    SELECT
        COALESCE(existing.id, nextval('ihms_master.geo_block_seq')),
        d.id,
        staging.code,
        CASE WHEN staging.lgd_version ~ '^\\d+$' THEN staging.lgd_version::INTEGER ELSE NULL END,
        staging.name,
        NULLIF(staging.name_local, ''),
        TRUE,
        FALSE,
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    FROM (
        SELECT DISTINCT ON (code) district_code, code, lgd_version, name, name_local
        FROM staging_geo_block
    ) staging
    CROSS JOIN (SELECT id FROM ihms_master.geo_state WHERE code = '{state_code_str}' LIMIT 1) s
    JOIN ihms_master.geo_district d
        ON d.state_id = s.id AND d.code = staging.district_code
    LEFT JOIN ihms_master.geo_block existing
        ON existing.code = staging.code
    ON CONFLICT (code) DO UPDATE SET
        district_id = EXCLUDED.district_id,
        name = EXCLUDED.name,
        name_local = EXCLUDED.name_local,
        lgd_version = EXCLUDED.lgd_version,
        updated_datetime = CURRENT_TIMESTAMP
    RETURNING (xmax = 0) AS inserted
)
INSERT INTO staging_counts (kind, inserted, updated)
SELECT
    'blockofspecificState',
    COUNT(*) FILTER (WHERE inserted),
    COUNT(*) FILTER (WHERE NOT inserted)
FROM upserted;

-- 4. Upsert geo_village
WITH upserted AS (
    INSERT INTO ihms_master.geo_village (
        id, sub_district_id, code, lgd_version, name, name_local,
        village_status, census_2001_code, census_2011_code, status, synchronized, added_datetime, updated_datetime
    )
    SELECT
        COALESCE(existing.id, nextval('ihms_master.geo_village_seq')),
        sd.id,
        staging.code,
        CASE WHEN staging.lgd_version ~ '^\\d+$' THEN staging.lgd_version::INTEGER ELSE NULL END,
        staging.name,
        NULLIF(staging.name_local, ''),
        NULLIF(staging.village_status, ''),
        NULLIF(staging.census_2001_code, ''),
        NULLIF(staging.census_2011_code, ''),
        TRUE,
        FALSE,
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    FROM (
        SELECT DISTINCT ON (code) sub_district_code, code, lgd_version, name, name_local, village_status, census_2001_code, census_2011_code
        FROM staging_geo_village
    ) staging
    CROSS JOIN (SELECT id FROM ihms_master.geo_state WHERE code = '{state_code_str}' LIMIT 1) s
    JOIN ihms_master.geo_district d ON d.state_id = s.id
    JOIN ihms_master.geo_sub_district sd ON sd.district_id = d.id AND sd.code = staging.sub_district_code
    LEFT JOIN ihms_master.geo_village existing
        ON existing.code = staging.code
    ON CONFLICT (code) DO UPDATE SET
        sub_district_id = EXCLUDED.sub_district_id,
        name = EXCLUDED.name,
        name_local = EXCLUDED.name_local,
        lgd_version = EXCLUDED.lgd_version,
        village_status = EXCLUDED.village_status,
        census_2001_code = EXCLUDED.census_2001_code,
        census_2011_code = EXCLUDED.census_2011_code,
        updated_datetime = CURRENT_TIMESTAMP
    RETURNING (xmax = 0) AS inserted
)
INSERT INTO staging_counts (kind, inserted, updated)
SELECT
    'villageofSpecificState',
    COUNT(*) FILTER (WHERE inserted),
    COUNT(*) FILTER (WHERE NOT inserted)
FROM upserted;

-- 5a. Upsert lgd_local_body (PRI)
WITH upserted AS (
    INSERT INTO ihms_master.lgd_local_body (
        id, state_id, body_category, type_code, type_name, code, lgd_version,
        name, name_local, parent_code, census_2001_code, census_2011_code,
        status, synchronized, added_datetime, updated_datetime
    )
    SELECT
        COALESCE(existing.id, nextval('ihms_master.lgd_local_body_seq')),
        s.id,
        staging.body_category,
        NULLIF(staging.type_code, ''),
        NULLIF(staging.type_name, ''),
        staging.code,
        CASE WHEN staging.lgd_version ~ '^\\d+$' THEN staging.lgd_version::INTEGER ELSE NULL END,
        staging.name,
        NULLIF(staging.name_local, ''),
        NULLIF(staging.parent_code, ''),
        NULLIF(staging.census_2001_code, ''),
        NULLIF(staging.census_2011_code, ''),
        TRUE,
        FALSE,
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    FROM (
        SELECT DISTINCT ON (code) *
        FROM staging_lgd_local_body
        WHERE body_category = 'PRI'
    ) staging
    CROSS JOIN (SELECT id FROM ihms_master.geo_state WHERE code = '{state_code_str}' LIMIT 1) s
    LEFT JOIN ihms_master.lgd_local_body existing
        ON existing.code = staging.code
    ON CONFLICT (code) DO UPDATE SET
        body_category = EXCLUDED.body_category,
        type_code = EXCLUDED.type_code,
        type_name = EXCLUDED.type_name,
        name = EXCLUDED.name,
        name_local = EXCLUDED.name_local,
        lgd_version = EXCLUDED.lgd_version,
        parent_code = EXCLUDED.parent_code,
        census_2001_code = EXCLUDED.census_2001_code,
        census_2011_code = EXCLUDED.census_2011_code,
        updated_datetime = CURRENT_TIMESTAMP
    RETURNING (xmax = 0) AS inserted
)
INSERT INTO staging_counts (kind, inserted, updated)
SELECT
    'priLbSpecificState',
    COUNT(*) FILTER (WHERE inserted),
    COUNT(*) FILTER (WHERE NOT inserted)
FROM upserted;

-- 5b. Upsert lgd_local_body (ULB)
WITH upserted AS (
    INSERT INTO ihms_master.lgd_local_body (
        id, state_id, body_category, type_code, type_name, code, lgd_version,
        name, name_local, parent_code, census_2001_code, census_2011_code,
        status, synchronized, added_datetime, updated_datetime
    )
    SELECT
        COALESCE(existing.id, nextval('ihms_master.lgd_local_body_seq')),
        s.id,
        staging.body_category,
        NULLIF(staging.type_code, ''),
        NULLIF(staging.type_name, ''),
        staging.code,
        CASE WHEN staging.lgd_version ~ '^\\d+$' THEN staging.lgd_version::INTEGER ELSE NULL END,
        staging.name,
        NULLIF(staging.name_local, ''),
        NULLIF(staging.parent_code, ''),
        NULLIF(staging.census_2001_code, ''),
        NULLIF(staging.census_2011_code, ''),
        TRUE,
        FALSE,
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    FROM (
        SELECT DISTINCT ON (code) *
        FROM staging_lgd_local_body
        WHERE body_category = 'ULB'
    ) staging
    CROSS JOIN (SELECT id FROM ihms_master.geo_state WHERE code = '{state_code_str}' LIMIT 1) s
    LEFT JOIN ihms_master.lgd_local_body existing
        ON existing.code = staging.code
    ON CONFLICT (code) DO UPDATE SET
        body_category = EXCLUDED.body_category,
        type_code = EXCLUDED.type_code,
        type_name = EXCLUDED.type_name,
        name = EXCLUDED.name,
        name_local = EXCLUDED.name_local,
        lgd_version = EXCLUDED.lgd_version,
        parent_code = EXCLUDED.parent_code,
        census_2001_code = EXCLUDED.census_2001_code,
        census_2011_code = EXCLUDED.census_2011_code,
        updated_datetime = CURRENT_TIMESTAMP
    RETURNING (xmax = 0) AS inserted
)
INSERT INTO staging_counts (kind, inserted, updated)
SELECT
    'ulbSpecificState',
    COUNT(*) FILTER (WHERE inserted),
    COUNT(*) FILTER (WHERE NOT inserted)
FROM upserted;

-- 5c. Upsert lgd_local_body (TLB)
WITH upserted AS (
    INSERT INTO ihms_master.lgd_local_body (
        id, state_id, body_category, type_code, type_name, code, lgd_version,
        name, name_local, parent_code, census_2001_code, census_2011_code,
        status, synchronized, added_datetime, updated_datetime
    )
    SELECT
        COALESCE(existing.id, nextval('ihms_master.lgd_local_body_seq')),
        s.id,
        staging.body_category,
        NULLIF(staging.type_code, ''),
        NULLIF(staging.type_name, ''),
        staging.code,
        CASE WHEN staging.lgd_version ~ '^\\d+$' THEN staging.lgd_version::INTEGER ELSE NULL END,
        staging.name,
        NULLIF(staging.name_local, ''),
        NULLIF(staging.parent_code, ''),
        NULLIF(staging.census_2001_code, ''),
        NULLIF(staging.census_2011_code, ''),
        TRUE,
        FALSE,
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    FROM (
        SELECT DISTINCT ON (code) *
        FROM staging_lgd_local_body
        WHERE body_category = 'TLB'
    ) staging
    CROSS JOIN (SELECT id FROM ihms_master.geo_state WHERE code = '{state_code_str}' LIMIT 1) s
    LEFT JOIN ihms_master.lgd_local_body existing
        ON existing.code = staging.code
    ON CONFLICT (code) DO UPDATE SET
        body_category = EXCLUDED.body_category,
        type_code = EXCLUDED.type_code,
        type_name = EXCLUDED.type_name,
        name = EXCLUDED.name,
        name_local = EXCLUDED.name_local,
        lgd_version = EXCLUDED.lgd_version,
        parent_code = EXCLUDED.parent_code,
        census_2001_code = EXCLUDED.census_2001_code,
        census_2011_code = EXCLUDED.census_2011_code,
        updated_datetime = CURRENT_TIMESTAMP
    RETURNING (xmax = 0) AS inserted
)
INSERT INTO staging_counts (kind, inserted, updated)
SELECT
    'tlbSpecificState',
    COUNT(*) FILTER (WHERE inserted),
    COUNT(*) FILTER (WHERE NOT inserted)
FROM upserted;

-- 6a. Upsert lgd_ward (PRI)
WITH upserted AS (
    INSERT INTO ihms_master.lgd_ward (
        id, local_body_id, ward_category, code, ward_number, name, name_local,
        status, synchronized, added_datetime, updated_datetime
    )
    SELECT
        COALESCE(existing.id, nextval('ihms_master.lgd_ward_seq')),
        lb.id,
        staging.ward_category,
        staging.code,
        NULLIF(staging.ward_number, ''),
        staging.name,
        NULLIF(staging.name_local, ''),
        TRUE,
        FALSE,
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    FROM (
        SELECT DISTINCT ON (code) *
        FROM staging_lgd_ward
        WHERE ward_category = 'PRI'
    ) staging
    CROSS JOIN (SELECT id FROM ihms_master.geo_state WHERE code = '{state_code_str}' LIMIT 1) s
    JOIN ihms_master.lgd_local_body lb
        ON lb.state_id = s.id AND lb.code = staging.local_body_code
    LEFT JOIN ihms_master.lgd_ward existing
        ON existing.code = staging.code
    WHERE NOT EXISTS (
        SELECT 1 FROM ihms_master.lgd_ward w
        WHERE w.code = staging.code AND w.ward_category <> staging.ward_category
    )
    ON CONFLICT (code) DO UPDATE SET
        local_body_id = EXCLUDED.local_body_id,
        ward_number = EXCLUDED.ward_number,
        name = EXCLUDED.name,
        name_local = EXCLUDED.name_local,
        updated_datetime = CURRENT_TIMESTAMP
    WHERE lgd_ward.ward_category = EXCLUDED.ward_category
    RETURNING (xmax = 0) AS inserted
)
INSERT INTO staging_counts (kind, inserted, updated)
SELECT
    'priWards',
    COUNT(*) FILTER (WHERE inserted),
    COUNT(*) FILTER (WHERE NOT inserted)
FROM upserted;

-- 6b. Upsert lgd_ward (ULB)
WITH upserted AS (
    INSERT INTO ihms_master.lgd_ward (
        id, local_body_id, ward_category, code, ward_number, name, name_local,
        status, synchronized, added_datetime, updated_datetime
    )
    SELECT
        COALESCE(existing.id, nextval('ihms_master.lgd_ward_seq')),
        lb.id,
        staging.ward_category,
        staging.code,
        NULLIF(staging.ward_number, ''),
        staging.name,
        NULLIF(staging.name_local, ''),
        TRUE,
        FALSE,
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    FROM (
        SELECT DISTINCT ON (code) *
        FROM staging_lgd_ward
        WHERE ward_category = 'ULB'
    ) staging
    CROSS JOIN (SELECT id FROM ihms_master.geo_state WHERE code = '{state_code_str}' LIMIT 1) s
    JOIN ihms_master.lgd_local_body lb
        ON lb.state_id = s.id AND lb.code = staging.local_body_code
    LEFT JOIN ihms_master.lgd_ward existing
        ON existing.code = staging.code
    WHERE NOT EXISTS (
        SELECT 1 FROM ihms_master.lgd_ward w
        WHERE w.code = staging.code AND w.ward_category <> staging.ward_category
    )
    ON CONFLICT (code) DO UPDATE SET
        local_body_id = EXCLUDED.local_body_id,
        ward_number = EXCLUDED.ward_number,
        name = EXCLUDED.name,
        name_local = EXCLUDED.name_local,
        updated_datetime = CURRENT_TIMESTAMP
    WHERE lgd_ward.ward_category = EXCLUDED.ward_category
    RETURNING (xmax = 0) AS inserted
)
INSERT INTO staging_counts (kind, inserted, updated)
SELECT
    'uLBWardforState',
    COUNT(*) FILTER (WHERE inserted),
    COUNT(*) FILTER (WHERE NOT inserted)
FROM upserted;

-- 7. Upsert lgd_ward_coverage
WITH upserted AS (
    INSERT INTO ihms_master.lgd_ward_coverage (
        id, ward_id, district_id, sub_district_id, status, synchronized, added_datetime, updated_datetime
    )
    SELECT
        COALESCE(existing.id, nextval('ihms_master.lgd_ward_coverage_seq')),
        w.id,
        d.id,
        sd.id,
        TRUE,
        FALSE,
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    FROM (
        SELECT DISTINCT ON (ward_code, district_code, subdistrict_code) *
        FROM staging_lgd_ward_coverage
        WHERE district_code IS NOT NULL AND district_code <> ''
    ) staging
    CROSS JOIN (SELECT id FROM ihms_master.geo_state WHERE code = '{state_code_str}' LIMIT 1) s
    JOIN ihms_master.lgd_ward w ON w.code = staging.ward_code
    JOIN ihms_master.geo_district d ON d.state_id = s.id AND d.code = staging.district_code
    LEFT JOIN ihms_master.geo_sub_district sd ON sd.district_id = d.id AND sd.code = staging.subdistrict_code
    LEFT JOIN ihms_master.lgd_ward_coverage existing
        ON existing.ward_id = w.id
       AND existing.district_id = d.id
       AND (existing.sub_district_id = sd.id OR (existing.sub_district_id IS NULL AND sd.id IS NULL))
    ON CONFLICT (ward_id, district_id, sub_district_id) DO UPDATE SET
        updated_datetime = CURRENT_TIMESTAMP
    RETURNING (xmax = 0) AS inserted
)
INSERT INTO staging_counts (kind, inserted, updated)
SELECT
    'uLBWardforStateWithCov',
    COUNT(*) FILTER (WHERE inserted),
    COUNT(*) FILTER (WHERE NOT inserted)
FROM upserted;

-- 8. Upsert geo_block_village
WITH upserted AS (
    INSERT INTO ihms_master.geo_block_village (
        id, block_id, village_id, status, synchronized, added_datetime, updated_datetime
    )
    SELECT
        COALESCE(existing.id, nextval('ihms_master.geo_block_village_seq')),
        b.id,
        v.id,
        TRUE,
        FALSE,
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    FROM (
        SELECT DISTINCT ON (block_code, village_code) *
        FROM staging_geo_block_village
    ) staging
    JOIN ihms_master.geo_block b ON b.code = staging.block_code
    JOIN ihms_master.geo_village v ON v.code = staging.village_code
    LEFT JOIN ihms_master.geo_block_village existing
        ON existing.block_id = b.id AND existing.village_id = v.id
    ON CONFLICT (block_id, village_id) DO UPDATE SET
        updated_datetime = CURRENT_TIMESTAMP
    RETURNING (xmax = 0) AS inserted
)
INSERT INTO staging_counts (kind, inserted, updated)
SELECT
    'allBlockStateWithCoveredVillage',
    COUNT(*) FILTER (WHERE inserted),
    COUNT(*) FILTER (WHERE NOT inserted)
FROM upserted;

-- 9. Upsert lgd_village_gram_panchayat
WITH upserted AS (
    INSERT INTO ihms_master.lgd_village_gram_panchayat (
        id, village_id, local_body_id, status, synchronized, added_datetime, updated_datetime
    )
    SELECT
        COALESCE(existing.id, nextval('ihms_master.lgd_village_gram_panchayat_seq')),
        v.id,
        lb.id,
        TRUE,
        FALSE,
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    FROM (
        SELECT DISTINCT ON (village_code, local_body_code) *
        FROM staging_lgd_village_gp
    ) staging
    JOIN ihms_master.geo_village v ON v.code = staging.village_code
    JOIN ihms_master.lgd_local_body lb ON lb.code = staging.local_body_code
    LEFT JOIN ihms_master.lgd_village_gram_panchayat existing
        ON existing.village_id = v.id AND existing.local_body_id = lb.id
    ON CONFLICT (village_id, local_body_id) DO UPDATE SET
        updated_datetime = CURRENT_TIMESTAMP
    RETURNING (xmax = 0) AS inserted
)
INSERT INTO staging_counts (kind, inserted, updated)
SELECT
    'villageGramPanchayatMapping',
    COUNT(*) FILTER (WHERE inserted),
    COUNT(*) FILTER (WHERE NOT inserted)
FROM upserted;

-- Master import log entries for each kind
{all_logs}

COMMIT;
"""


def process_lgd_hierarchy_state(
    zip_path: str,
    dry_run: bool = False,
    known_districts: set[str] | None = None,
) -> tuple[dict[str, dict[str, int]], int, str, str]:
    """Process a single state zip archive for LGD full hierarchy import."""
    st_code, st_name = lgd_hierarchy.state_of(zip_path)
    if str(st_code) not in VALID_LGD_STATE_CODES:
        raise ValueError(f"State code {st_code} does not match any existing geo_state code")

    file_sha256 = compute_sha256(zip_path)
    raw_rows = {k: list(lgd_hierarchy.iter_rows(zip_path, k)) for k in lgd_hierarchy.KINDS}

    valid_rows, skip_counts, colliding_wards, orphans_per_kind = check_lgd_hierarchy_orphans(
        st_code, raw_rows, known_districts=known_districts
    )

    counts = {}
    for kind in lgd_hierarchy.KINDS:
        r_cnt = len(raw_rows.get(kind, []))
        sk_cnt = skip_counts.get(kind, 0)
        v_cnt = len(valid_rows.get(kind, []))
        counts[kind] = {
            "read": r_cnt,
            "skipped": sk_cnt,
            "inserted": 0 if dry_run else v_cnt,
            "updated": 0,
        }

    if dry_run:
        return counts, st_code, st_name, file_sha256

    # Staging CSV data preparation
    districts_data = [
        {"code": r["district_code"], "name": r["district_name_en"], "census_2011_code": r.get("census_2011_code", "")}
        for r in valid_rows["districtofSpecificState"]
    ]
    subdistricts_data = [
        {
            "district_code": r["district_code"],
            "code": r["subdistrict_code"],
            "lgd_version": r.get("subdistrict_version", ""),
            "name": r["subdistrict_name_en"],
            "name_local": r.get("subdistrict_name_local", ""),
            "census_2001_code": r.get("census_2001_code", ""),
            "census_2011_code": r.get("census_2011_code", ""),
        }
        for r in valid_rows["subDistrictofSpecificState"]
    ]
    blocks_data = [
        {
            "district_code": r["district_code"],
            "code": r["block_code"],
            "lgd_version": r.get("block_version", ""),
            "name": r["block_name_en"],
            "name_local": r.get("block_name_local", ""),
        }
        for r in valid_rows["blockofspecificState"]
    ]
    villages_data = [
        {
            "district_code": r.get("district_code", ""),
            "sub_district_code": r["sub_district_code"],
            "code": r["village_code"],
            "lgd_version": r.get("village_version", ""),
            "name": r["village_name_en"],
            "name_local": r.get("village_name_local", ""),
            "village_status": r.get("village_status", ""),
            "census_2001_code": r.get("census_2001_code", ""),
            "census_2011_code": r.get("census_2011_code", ""),
        }
        for r in valid_rows["villageofSpecificState"]
    ]
    local_bodies_data = []
    for r in valid_rows["priLbSpecificState"]:
        local_bodies_data.append({
            "body_category": "PRI",
            "type_code": r.get("localbody_type_code", ""),
            "type_name": r.get("localbody_type_name", ""),
            "code": r["localbody_code"],
            "lgd_version": r.get("localbody_version", ""),
            "name": r["localbody_name_en"],
            "name_local": r.get("localbody_name_local", ""),
            "parent_code": r.get("parent_localbody_code", ""),
            "census_2001_code": "",
            "census_2011_code": "",
        })
    for r in valid_rows["ulbSpecificState"]:
        local_bodies_data.append({
            "body_category": "ULB",
            "type_code": r.get("localbody_type_code", ""),
            "type_name": r.get("localbody_type_name", ""),
            "code": r["localbody_code"],
            "lgd_version": r.get("localbody_version", ""),
            "name": r["local_body_name_en"],
            "name_local": r.get("local_body_name_local", ""),
            "parent_code": "",
            "census_2001_code": r.get("census_2001_code", ""),
            "census_2011_code": r.get("census_2011_code", ""),
        })
    for r in valid_rows["tlbSpecificState"]:
        local_bodies_data.append({
            "body_category": "TLB",
            "type_code": r.get("localbody_type_code", ""),
            "type_name": r.get("local_body_type_name", ""),
            "code": r["local_body_code"],
            "lgd_version": r.get("local_body_version", ""),
            "name": r["local_body_name_en"],
            "name_local": r.get("local_body_name_local", ""),
            "parent_code": r.get("parent_localbody_code", ""),
            "census_2001_code": "",
            "census_2011_code": "",
        })

    wards_data = []
    for r in valid_rows["priWards"]:
        wards_data.append({
            "local_body_code": r["local_body_code"],
            "ward_category": "PRI",
            "code": r["ward_code"],
            "ward_number": r.get("ward_number", ""),
            "name": r["ward_name_en"],
            "name_local": r.get("ward_name_local", ""),
        })
    for r in valid_rows["uLBWardforState"]:
        wards_data.append({
            "local_body_code": r["local_body_code"],
            "ward_category": "ULB",
            "code": r["ward_code"],
            "ward_number": r.get("ward_number", ""),
            "name": r["ward_name"],
            "name_local": "",
        })

    coverage_data = [
        {
            "ward_code": r["ward_code"],
            "district_code": r["district_code"].strip(),
            "subdistrict_code": r.get("subdistrict_code", "").strip(),
        }
        for r in valid_rows["uLBWardforStateWithCov"]
    ]

    block_village_data = [
        {"block_code": r["block_code"], "village_code": r["village_code"]}
        for r in valid_rows["allBlockStateWithCoveredVillage"]
    ]

    village_gp_data = [
        {"village_code": r["village_code"], "local_body_code": r["local_body_code"]}
        for r in valid_rows["villageGramPanchayatMapping"]
    ]

    dist_csv = write_csv(districts_data, ["code", "name", "census_2011_code"])
    subdist_csv = write_csv(
        subdistricts_data,
        ["district_code", "code", "lgd_version", "name", "name_local", "census_2001_code", "census_2011_code"],
    )
    block_csv = write_csv(
        blocks_data,
        ["district_code", "code", "lgd_version", "name", "name_local"],
    )
    village_csv = write_csv(
        villages_data,
        ["district_code", "sub_district_code", "code", "lgd_version", "name", "name_local", "village_status", "census_2001_code", "census_2011_code"],
    )
    lb_csv = write_csv(
        local_bodies_data,
        ["body_category", "type_code", "type_name", "code", "lgd_version", "name", "name_local", "parent_code", "census_2001_code", "census_2011_code"],
    )
    ward_csv = write_csv(
        wards_data,
        ["local_body_code", "ward_category", "code", "ward_number", "name", "name_local"],
    )
    cov_csv = write_csv(
        coverage_data,
        ["ward_code", "district_code", "subdistrict_code"],
    )
    bv_csv = write_csv(
        block_village_data,
        ["block_code", "village_code"],
    )
    vgp_csv = write_csv(
        village_gp_data,
        ["village_code", "local_body_code"],
    )

    csv_paths = {
        "geo_district": dist_csv,
        "geo_sub_district": subdist_csv,
        "geo_block": block_csv,
        "geo_village": village_csv,
        "lgd_local_body": lb_csv,
        "lgd_ward": ward_csv,
        "lgd_ward_coverage": cov_csv,
        "geo_block_village": bv_csv,
        "lgd_village_gp": vgp_csv,
    }

    try:
        sql = generate_lgd_hierarchy_sql(
            st_code, st_name, csv_paths, file_sha256, counts, source_url=os.path.basename(zip_path)
        )
        execute_psql(sql)
    finally:
        for p in csv_paths.values():
            if os.path.exists(p):
                os.remove(p)

    return counts, st_code, st_name, file_sha256


def handle_lgd_hierarchy(args: argparse.Namespace) -> int:
    target = args.dir
    dry_run = args.dry_run
    state_filter = getattr(args, "state", None)

    if os.path.isfile(target) and target.endswith(".zip"):
        zip_files = [target]
    elif os.path.isdir(target):
        zip_files = [
            os.path.join(target, f)
            for f in os.listdir(target)
            if f.endswith(".zip")
        ]
    else:
        sys.stderr.write(f"Invalid directory or zip file: {target}\n")
        return 1

    # Order states by file size, smallest first
    zip_files.sort(key=lambda p: os.path.getsize(p))

    if state_filter is not None:
        target_code = str(state_filter).lstrip("0")
        filtered = []
        for z in zip_files:
            try:
                st_code, _ = lgd_hierarchy.state_of(z)
                if str(st_code).lstrip("0") == target_code or str(st_code) == str(state_filter):
                    filtered.append(z)
            except Exception as e:
                sys.stderr.write(f"Warning: could not read state from {z}: {e}\n")
        zip_files = filtered

    if not zip_files:
        print("[lgd-hierarchy] No matching state zip files found.")
        return 0

    overall_success = True
    for zip_path in zip_files:
        try:
            t0 = time.time()
            counts, st_code, st_name, file_sha256 = process_lgd_hierarchy_state(
                zip_path, dry_run=dry_run
            )
            elapsed = time.time() - t0

            print(f"\n======================================================================")
            print(f"LGD HIERARCHY IMPORT SUMMARY: {st_name} (State Code: {st_code})")
            print(f"SHA256: {file_sha256}")
            print(f"Elapsed Time: {elapsed:.3f}s")
            print(f"Mode: {'DRY-RUN (read-only, no database writes)' if dry_run else 'COMMITTED'}")
            print(f"----------------------------------------------------------------------")
            print(f"{'Kind':<32} {'Read':>8} {'Inserted':>10} {'Updated':>10} {'Skipped':>10}")
            print(f"----------------------------------------------------------------------")
            for kind in lgd_hierarchy.KINDS:
                k_counts = counts.get(kind, {})
                r = k_counts.get("read", 0)
                ins = k_counts.get("inserted", 0)
                upd = k_counts.get("updated", 0)
                sk = k_counts.get("skipped", 0)
                print(f"{kind:<32} {r:>8} {ins:>10} {upd:>10} {sk:>10}")
            print(f"======================================================================\n")

        except Exception as e:
            sys.stderr.write(f"[lgd-hierarchy] Error processing {zip_path}: {e}\n")
            overall_success = False

    return 0 if overall_success else 1


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

    # Subcommand: lgd
    p_lgd = subparsers.add_parser("lgd", help="Import LGD states and districts from .xlsx files")
    p_lgd.add_argument("states_file", metavar="states.xlsx", help="Path to LGD states .xlsx file")
    p_lgd.add_argument("districts_file", metavar="districts.xlsx", help="Path to LGD districts .xlsx file")
    p_lgd.add_argument("--dry-run", action="store_true", help="Parse and validate only")
    p_lgd.add_argument("--skip-count-check", action="store_true", help="Skip exact 36 states and 790 districts count check")
    p_lgd.set_defaults(func=handle_lgd)

    # Subcommand: lgd-hierarchy
    p_hier = subparsers.add_parser("lgd-hierarchy", help="Import LGD full hierarchy from state zip files")
    p_hier.add_argument("dir", help="Directory containing state zip files")
    p_hier.add_argument("--state", help="Filter by state code (e.g. 4 or 31)")
    p_hier.add_argument("--dry-run", action="store_true", help="Parse and validate read-only, write nothing")
    p_hier.set_defaults(func=handle_lgd_hierarchy)

    args = parser.parse_args()
    return args.func(args)


if __name__ == "__main__":
    sys.exit(main())
