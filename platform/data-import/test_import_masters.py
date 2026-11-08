#!/usr/bin/env python3
import io
import os
import re
import subprocess
import sys
import tempfile
import unittest
import zipfile
from pathlib import Path

from import_masters import (
    ALLOWED_COUNTRY_COLUMNS,
    EXPECTED_DISTRICT_HEADERS,
    EXPECTED_STATE_HEADERS,
    col_index_to_letter,
    compute_sha256,
    derive_parent_linearization_uris,
    find_header_col,
    generate_countries_sql,
    generate_icd11_sql,
    generate_lgd_sql,
    generate_test_data,
    generate_testdata_sql,
    get_psql_cmd,
    map_country_row,
    parse_census_code,
    parse_country_file,
    parse_icd11_file,
    parse_lgd_districts,
    parse_lgd_states,
    strip_title,
    to_int_str,
    validate_lgd,
)

TESTDATA_DIR = Path(__file__).resolve().parent / "testdata"
ICD11_SAMPLE = TESTDATA_DIR / "icd11-sample.txt"
COUNTRY_SAMPLE = TESTDATA_DIR / "country-sample.csv"


def create_test_xlsx(rows: list[list[str]], filepath: str) -> None:
    """Helper to build a valid minimal .xlsx file using zipfile and xml.etree."""
    shared_strings: list[str] = []
    string_map: dict[str, int] = {}
    for r in rows:
        for val in r:
            if val not in string_map:
                string_map[val] = len(shared_strings)
                shared_strings.append(val)

    sst_parts = [
        '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>',
        f'<sst xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" count="{len(shared_strings)}" uniqueCount="{len(shared_strings)}">',
    ]
    for s in shared_strings:
        sst_parts.append(f"<si><t>{s}</t></si>")
    sst_parts.append("</sst>")

    sheet_parts = [
        '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>',
        '<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><sheetData>',
    ]
    for r_idx, row in enumerate(rows, start=1):
        sheet_parts.append(f'<row r="{r_idx}">')
        for c_idx, val in enumerate(row):
            col_letter = col_index_to_letter(c_idx)
            s_idx = string_map[val]
            sheet_parts.append(f'<c r="{col_letter}{r_idx}" t="s"><v>{s_idx}</v></c>')
        sheet_parts.append("</row>")
    sheet_parts.append("</sheetData></worksheet>")

    with zipfile.ZipFile(filepath, "w") as zf:
        zf.writestr("xl/sharedStrings.xml", "".join(sst_parts))
        zf.writestr("xl/worksheets/sheet1.xml", "".join(sheet_parts))
        zf.writestr(
            "xl/workbook.xml",
            '<?xml version="1.0" encoding="UTF-8" standalone="yes"?><workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><sheets><sheet name="Sheet1" sheetId="1" r:id="rId1"/></sheets></workbook>',
        )


class TestImportMasters(unittest.TestCase):

    def test_title_stripping_keeps_text_exact(self):
        """Test that title stripping removes leading '- ' markers and outer quotes, keeping text exact."""
        # Single marker
        self.assertEqual(
            strip_title("- Gastroenteritis or colitis of infectious origin"),
            "Gastroenteritis or colitis of infectious origin"
        )
        # Double marker
        self.assertEqual(
            strip_title("- - Bacterial intestinal infections"),
            "Bacterial intestinal infections"
        )
        # Triple marker
        self.assertEqual(
            strip_title("- - - Cholera"),
            "Cholera"
        )
        # Quadruple marker
        self.assertEqual(
            strip_title("- - - - Enteropathogenic Escherichia coli infection"),
            "Enteropathogenic Escherichia coli infection"
        )
        # Five markers
        self.assertEqual(
            strip_title("- - - - - Acute amoebiasis"),
            "Acute amoebiasis"
        )
        # Outer quotes with no marker
        self.assertEqual(
            strip_title('"Certain infectious or parasitic diseases"'),
            "Certain infectious or parasitic diseases"
        )
        # Outer quotes with markers
        self.assertEqual(
            strip_title('"- - - Cholera"'),
            "Cholera"
        )
        # Text with internal hyphen and quotes: internal hyphen must remain untouched
        self.assertEqual(
            strip_title("- - - Disease - with specific sub-type"),
            "Disease - with specific sub-type"
        )
        # Empty title
        self.assertEqual(strip_title(""), "")

    def test_parent_derivation_on_sample(self):
        """Test parent_linearization_uri derivation on icd11-sample.txt."""
        rows, counts = parse_icd11_file(str(ICD11_SAMPLE), release="2025-01")
        row_map = {r.get("code") or r.get("block_id") or "chapter": r for r in rows}

        # Chapter has parent None
        chapter_row = row_map["chapter"]
        self.assertIsNone(chapter_row["parent_linearization_uri"])
        chap_uri = chapter_row["linearization_uri"]

        # Level 1 block parent is chapter
        block_l1 = row_map["BlockL1-1A0"]
        self.assertEqual(block_l1["parent_linearization_uri"], chap_uri)
        block_l1_uri = block_l1["linearization_uri"]

        # Level 2 block parent is Level 1 block
        block_l2 = row_map["BlockL2-1A0"]
        self.assertEqual(block_l2["parent_linearization_uri"], block_l1_uri)
        block_l2_uri = block_l2["linearization_uri"]

        # Depth 1 category (1A00) parent is enclosing block
        cat_1a00 = row_map["1A00"]
        self.assertEqual(cat_1a00["parent_linearization_uri"], block_l2_uri)

        # Depth 1 category (1A03) parent is enclosing block
        cat_1a03 = row_map["1A03"]
        self.assertEqual(cat_1a03["parent_linearization_uri"], block_l2_uri)
        cat_1a03_uri = cat_1a03["linearization_uri"]

        # Depth 2 category (1A03.0) parent is depth 1 category 1A03
        cat_1a03_0 = row_map["1A03.0"]
        self.assertEqual(cat_1a03_0["parent_linearization_uri"], cat_1a03_uri)

        # Residual depth 2 category (1A03.Y) parent is depth 1 category 1A03
        cat_1a03_y = row_map["1A03.Y"]
        self.assertEqual(cat_1a03_y["parent_linearization_uri"], cat_1a03_uri)

        # Next depth 1 category (1A04) parent resets back to enclosing block
        cat_1a04 = row_map["1A04"]
        self.assertEqual(cat_1a04["parent_linearization_uri"], block_l2_uri)

        # BlockL2-1A1 parent is BlockL1-1A0
        block_l2_1a1 = row_map["BlockL2-1A1"]
        self.assertEqual(block_l2_1a1["parent_linearization_uri"], block_l1_uri)

        # 1A11 parent is BlockL2-1A1
        cat_1a11 = row_map["1A11"]
        self.assertEqual(cat_1a11["parent_linearization_uri"], block_l2_1a1["linearization_uri"])

        # 1A11.0 parent is 1A11
        cat_1a11_0 = row_map["1A11.0"]
        self.assertEqual(cat_1a11_0["parent_linearization_uri"], cat_1a11["linearization_uri"])

    def test_class_kind_counts(self):
        """Test validation and tallying of class_kind on the sample file."""
        rows, counts = parse_icd11_file(str(ICD11_SAMPLE), release="2025-01")
        self.assertEqual(counts["chapter"], 1)
        self.assertEqual(counts["block"], 4)
        self.assertEqual(counts["category"], 39)
        self.assertEqual(len(rows), 44)

    def test_country_column_mapping(self):
        """Test country column mapping uses only allowed columns and maps to real geo_country columns."""
        countries = parse_country_file(str(COUNTRY_SAMPLE))
        self.assertEqual(len(countries), 5)

        expected = [
            ("AF", "Afghanistan"),
            ("AX", "Åland Islands"),
            ("AL", "Albania"),
            ("DZ", "Algeria"),
            ("AS", "American Samoa"),
        ]
        for country, (expected_code, expected_name) in zip(countries, expected):
            self.assertEqual(country["code"], expected_code)
            self.assertEqual(country["name"], expected_name)
            # Ensure mapped row only has legitimate target database columns
            self.assertTrue(set(country.keys()).issubset({"code", "name", "is_default", "status", "synchronized"}))

        # Check raw mapping directly to verify restricted columns are ignored
        raw_row = {
            "ISO3166-1-Alpha-2": "IN",
            "ISO3166-1-Alpha-3": "IND",
            "ISO3166-1-numeric": "356",
            "official_name_en": "India",
            "Dial": "91",
            "FIFA": "IND",
            "UNTERM Chinese Formal": "印度共和国",
            "UNTERM Russian Short": "Индия",
            "wikidata_id": "https://www.wikidata.org/wiki/Q668",
        }
        mapped = map_country_row(raw_row)
        self.assertIsNotNone(mapped)
        self.assertEqual(mapped["code"], "IN")
        self.assertEqual(mapped["name"], "India")
        self.assertEqual(mapped["is_default"], "true")
        # Verify no restricted keys entered the target
        self.assertNotIn("FIFA", mapped)
        self.assertNotIn("UNTERM Chinese Formal", mapped)
        self.assertNotIn("wikidata_id", mapped)

    def test_generated_sql_uses_on_conflict(self):
        """Test that generated SQL statements use ON CONFLICT DO UPDATE for idempotency."""
        # 1. ICD-11
        icd_sql = generate_icd11_sql("/tmp/dummy.csv", "2025-01", "dummy.txt", "abc123sha", 10, "test msg")
        self.assertIn("ON CONFLICT (linearization_uri) DO UPDATE SET", icd_sql)
        self.assertIn("ihms_master.master_import_log", icd_sql)
        self.assertIn("BEGIN;", icd_sql)
        self.assertIn("COMMIT;", icd_sql)

        # 2. Countries
        countries_sql = generate_countries_sql("/tmp/dummy.csv", "dummy.csv", "abc123sha", 5, "test msg")
        self.assertIn("ON CONFLICT (code) DO UPDATE SET", countries_sql)
        self.assertIn("ihms_master.master_import_log", countries_sql)
        self.assertIn("BEGIN;", countries_sql)
        self.assertIn("COMMIT;", countries_sql)

        # 3. Test data
        testdata_sql = generate_testdata_sql("/tmp/u.csv", "/tmp/d.csv", "/tmp/s.csv", "abc123sha", 41, "test msg")
        self.assertIn("ON CONFLICT (unit_id, code) DO UPDATE SET", testdata_sql)
        self.assertNotIn('ON CONFLICT ("ID")', testdata_sql)
        self.assertNotIn("ON CONFLICT (id)", testdata_sql)
        self.assertIn("ihms_master.master_import_log", testdata_sql)
        self.assertIn("BEGIN;", testdata_sql)
        self.assertIn("COMMIT;", testdata_sql)

    def test_no_literal_id_in_generated_sql(self):
        """Test that generated SQL and test data contain no hard-coded literal IDs."""
        test_units, test_depts, test_specs = generate_test_data()
        for item in test_units + test_depts + test_specs:
            self.assertNotIn("id", item, f"Found hard-coded id in test data item: {item}")
            self.assertNotIn("unit_id", item, f"Found hard-coded unit_id in test data item: {item}")

        testdata_sql = generate_testdata_sql("/tmp/u.csv", "/tmp/d.csv", "/tmp/s.csv", "abc123sha", 41, "test msg")
        self.assertNotIn("9999", testdata_sql)
        for n in range(9101, 9121):
            self.assertNotIn(str(n), testdata_sql)
        for n in range(9201, 9221):
            self.assertNotIn(str(n), testdata_sql)

        self.assertNotIn("id BIGINT", testdata_sql)
        self.assertNotIn("unit_id BIGINT", testdata_sql)

    def test_every_update_carries_test_guard(self):
        """Test that every UPDATE statement carries WHERE code LIKE 'TEST-%' using real column name."""
        testdata_sql = generate_testdata_sql("/tmp/u.csv", "/tmp/d.csv", "/tmp/s.csv", "abc123sha", 41, "test msg")

        # Specific per-table guards
        self.assertIn('u."CODE" LIKE \'TEST-%\'', testdata_sql)
        self.assertIn('d."CODE" LIKE \'TEST-%\'', testdata_sql)
        self.assertIn("specialization.code LIKE 'TEST-%'", testdata_sql)

        # Generic check: every statement containing UPDATE must carry the TEST- guard
        stmts = [s.strip() for s in testdata_sql.split(";") if re.search(r"\bUPDATE\b", s, re.IGNORECASE)]
        self.assertEqual(len(stmts), 3)  # Unit UPDATE, Dept UPDATE, Specialization DO UPDATE
        for stmt in stmts:
            self.assertIn("LIKE 'TEST-%'", stmt, f"Update statement missing TEST- guard:\n{stmt}")

    def test_uses_real_sequence_names(self):
        """Test that new rows take nextval from the real sequence names defined in V1 and V5."""
        testdata_sql = generate_testdata_sql("/tmp/u.csv", "/tmp/d.csv", "/tmp/s.csv", "abc123sha", 41, "test msg")

        # Sequences from V1 and V5
        self.assertIn('nextval(\'ihms_master."M_UNITMASTER_SEQ"\')', testdata_sql)
        self.assertIn('nextval(\'ihms_master."M_DEPARTMENTMASTER_SEQ"\')', testdata_sql)
        self.assertIn("nextval('ihms_master.specialization_seq')", testdata_sql)

    def test_idempotency_lookups_and_unique_constraints(self):
        """Test idempotency: Unit uses select lookup, Departments match on (UnitID, CODE), Specializations use uq constraint."""
        testdata_sql = generate_testdata_sql("/tmp/u.csv", "/tmp/d.csv", "/tmp/s.csv", "abc123sha", 41, "test msg")

        # Unit: lookup by CODE = 'TEST-UNIT', insert if absent (no ON CONFLICT)
        self.assertIn('existing."CODE" = \'TEST-UNIT\'', testdata_sql)
        self.assertIn("NOT EXISTS", testdata_sql)
        self.assertNotIn('ON CONFLICT ("CODE")', testdata_sql)
        self.assertNotIn('ON CONFLICT ("ID")', testdata_sql)

        # Departments: match on ("UnitID", "CODE") by lookup
        self.assertIn('d."UnitID" = u.unit_id', testdata_sql)
        self.assertIn('d."CODE" = staging.code', testdata_sql)
        self.assertIn('existing."UnitID" = u.unit_id', testdata_sql)
        self.assertIn('existing."CODE" = staging.code', testdata_sql)

        # Specializations: real unique constraint uq_specialization_unit_code (unit_id, code)
        self.assertIn("ON CONFLICT (unit_id, code) DO UPDATE SET", testdata_sql)

        # Unit lookup referenced by both departments and specializations
        unit_lookup = 'SELECT "ID" AS unit_id FROM ihms_master."M_UNITMASTER" WHERE "CODE" = \'TEST-UNIT\' LIMIT 1'
        self.assertIn(unit_lookup, testdata_sql)
        self.assertEqual(testdata_sql.count(unit_lookup), 3)  # dept update, dept insert, spec insert

    def test_rerun_idempotency_simulation(self):
        """Simulate the SQL logic and verify running it twice leaves the exact same row counts and preserves real data."""
        test_units, test_depts, test_specs = generate_test_data()

        seq = {"unit": 0, "dept": 0, "spec": 0}
        units_table = []
        depts_table = []
        specs_table = []

        def run_import():
            # 1. Unit: update if exists with TEST-
            for u in units_table:
                if u["CODE"] == "TEST-UNIT" and u["CODE"].startswith("TEST-"):
                    u["NAME"] = test_units[0]["name"]
                    u["DESCRIPTION"] = test_units[0]["description"]
            # 1. Unit: insert if absent
            if not any(u["CODE"] == "TEST-UNIT" for u in units_table):
                seq["unit"] += 1
                new_id = seq["unit"]
                units_table.append({
                    "ID": new_id,
                    "UnitID": new_id,
                    "CODE": test_units[0]["code"],
                    "NAME": test_units[0]["name"],
                    "DESCRIPTION": test_units[0]["description"],
                })

            # Unit lookup
            test_unit_id = next(u["ID"] for u in units_table if u["CODE"] == "TEST-UNIT")

            # 2. Departments: update existing matching (UnitID, CODE) where CODE LIKE 'TEST-%'
            for d in depts_table:
                if d["UnitID"] == test_unit_id and d["CODE"].startswith("TEST-"):
                    match = next((s for s in test_depts if s["code"] == d["CODE"]), None)
                    if match:
                        d["DESCRIPTION"] = match["description"]

            # 2. Departments: insert absent
            for s in test_depts:
                if not any(d["UnitID"] == test_unit_id and d["CODE"] == s["code"] for d in depts_table):
                    seq["dept"] += 1
                    depts_table.append({
                        "ID": seq["dept"],
                        "UnitID": test_unit_id,
                        "CODE": s["code"],
                        "DESCRIPTION": s["description"],
                    })

            # 3. Specializations: ON CONFLICT (unit_id, code) DO UPDATE WHERE code LIKE 'TEST-%'
            for s in test_specs:
                existing = next((sp for sp in specs_table if sp["unit_id"] == test_unit_id and sp["code"] == s["code"]), None)
                if existing:
                    if existing["code"].startswith("TEST-"):
                        existing["description"] = s["description"]
                else:
                    seq["spec"] += 1
                    specs_table.append({
                        "id": seq["spec"],
                        "unit_id": test_unit_id,
                        "code": s["code"],
                        "description": s["description"],
                    })

        # Add pre-existing real rows
        units_table.append({"ID": 100, "UnitID": 100, "CODE": "REAL-UNIT-A", "NAME": "Real Hospital", "DESCRIPTION": "Real"})
        depts_table.append({"ID": 200, "UnitID": 100, "CODE": "REAL-DEPT-CARD", "DESCRIPTION": "Real Dept"})
        specs_table.append({"id": 300, "unit_id": 100, "code": "REAL-SPEC-CARD", "description": "Real Spec"})

        # Run 1
        run_import()
        count_u1 = len(units_table)
        count_d1 = len(depts_table)
        count_s1 = len(specs_table)
        self.assertEqual(count_u1, 2)   # 1 real + 1 test
        self.assertEqual(count_d1, 21)  # 1 real + 20 test
        self.assertEqual(count_s1, 21)  # 1 real + 20 test

        # Run 2 (Rerun)
        run_import()
        self.assertEqual(len(units_table), count_u1)
        self.assertEqual(len(depts_table), count_d1)
        self.assertEqual(len(specs_table), count_s1)

        # Verify real rows were NEVER modified
        self.assertEqual(units_table[0]["NAME"], "Real Hospital")
        self.assertEqual(depts_table[0]["DESCRIPTION"], "Real Dept")
        self.assertEqual(specs_table[0]["description"], "Real Spec")

    def test_no_credentials_in_arguments(self):
        """Test that psql command line arguments do not include credentials or connection strings."""
        cmd = get_psql_cmd()
        # Verify command only contains psql and non-credential flags
        self.assertEqual(cmd, ["psql", "-v", "ON_ERROR_STOP=1"])
        for arg in cmd:
            self.assertFalse(arg.startswith("-U"), f"Found username in argument: {arg}")
            self.assertFalse(arg.startswith("-W"), f"Found password flag in argument: {arg}")
            self.assertFalse(arg.startswith("-d"), f"Found database in argument: {arg}")
            self.assertFalse(arg.startswith("-h"), f"Found host in argument: {arg}")
            self.assertFalse(arg.startswith("-p"), f"Found port in argument: {arg}")
            self.assertFalse("--password" in arg)
            self.assertFalse("--username" in arg)
            self.assertFalse("postgresql://" in arg)

    def test_build_tiny_xlsx_and_parse_lgd(self):
        """Test building tiny .xlsx files with zipfile and parsing them (2 states, 3 districts)."""
        with tempfile.TemporaryDirectory() as tmpdir:
            states_path = os.path.join(tmpdir, "states.xlsx")
            districts_path = os.path.join(tmpdir, "districts.xlsx")

            states_data = [
                ["All State of India"],
                [
                    "S.No.",
                    "State Code",
                    "State Version",
                    "State Name (In English)",
                    "State Name (In Local)",
                    "Census 2001 Code",
                    "Census 2011 Code",
                    "State or UT",
                ],
                [
                    "1.0",
                    "35.0",
                    "1.0",
                    "Andaman and Nicobar Islands",
                    "अंडमान और निकोबार द्वीप समूह",
                    "35.0",
                    "35.0",
                    "UT",
                ],
                [
                    "2.0",
                    "28.0",
                    "1.0",
                    "Andhra Pradesh",
                    "ఆంధ్రప్రదేశ్",
                    "28.0",
                    "28.0",
                    "State",
                ],
            ]
            districts_data = [
                ["All District of India"],
                [
                    "S.No.",
                    "State Code",
                    "State Name (In English)",
                    "District Code",
                    "District Name(In English)",
                    "Census 2001 Code",
                    "Census 2011 Code",
                ],
                [
                    "1.0",
                    "35.0",
                    "Andaman and Nicobar Islands",
                    "601.0",
                    "Nicobars",
                    "0.0",
                    "638.0",
                ],
                [
                    "2.0",
                    "35.0",
                    "Andaman and Nicobar Islands",
                    "602.0",
                    "North and Middle Andaman",
                    "0.0",
                    "639.0",
                ],
                [
                    "3.0",
                    "28.0",
                    "Andhra Pradesh",
                    "502.0",
                    "Anantapur",
                    "0.0",
                    "0.0",
                ],
            ]

            create_test_xlsx(states_data, states_path)
            create_test_xlsx(districts_data, districts_path)

            states = parse_lgd_states(states_path)
            districts = parse_lgd_districts(districts_path)

            self.assertEqual(len(states), 2)
            self.assertEqual(states[0], {"code": "35", "name": "Andaman and Nicobar Islands"})
            self.assertEqual(states[1], {"code": "28", "name": "Andhra Pradesh"})

            self.assertEqual(len(districts), 3)
            self.assertEqual(
                districts[0],
                {"state_code": "35", "code": "601", "name": "Nicobars", "census_2011_code": "638"},
            )
            self.assertEqual(
                districts[1],
                {
                    "state_code": "35",
                    "code": "602",
                    "name": "North and Middle Andaman",
                    "census_2011_code": "639",
                },
            )
            # Census 2011 code 0.0 becomes empty string (NULL in DB)
            self.assertEqual(
                districts[2],
                {"state_code": "28", "code": "502", "name": "Anantapur", "census_2011_code": ""},
            )

            # Validation without exact 36/790 succeeds
            validate_lgd(states, districts, exact_counts=False)

    def test_header_lookup_by_name(self):
        """Test finding columns by header name, even when columns are scrambled or have spacing differences."""
        with tempfile.TemporaryDirectory() as tmpdir:
            states_path = os.path.join(tmpdir, "states_scrambled.xlsx")
            districts_path = os.path.join(tmpdir, "districts_scrambled.xlsx")

            # Scrambled column order for states
            states_data = [
                ["Title Row"],
                [
                    "State Name (In English)",
                    "S.No.",
                    "State or UT",
                    "State Code",
                    "State Version",
                    "State Name (In Local)",
                    "Census 2001 Code",
                    "Census 2011 Code",
                ],
                [
                    "Andaman and Nicobar Islands",
                    "1.0",
                    "UT",
                    "35.0",
                    "1.0",
                    "Local Name",
                    "35.0",
                    "35.0",
                ],
            ]
            create_test_xlsx(states_data, states_path)
            states = parse_lgd_states(states_path)
            self.assertEqual(len(states), 1)
            self.assertEqual(states[0]["code"], "35")
            self.assertEqual(states[0]["name"], "Andaman and Nicobar Islands")

            # Scrambled column order and "District Name (In English)" with space
            districts_data = [
                ["Title Row"],
                [
                    "District Code",
                    "District Name (In English)",
                    "Census 2011 Code",
                    "State Code",
                    "S.No.",
                    "State Name (In English)",
                    "Census 2001 Code",
                ],
                [
                    "601.0",
                    "Nicobars",
                    "638.0",
                    "35.0",
                    "1.0",
                    "Andaman and Nicobar Islands",
                    "0.0",
                ],
            ]
            create_test_xlsx(districts_data, districts_path)
            districts = parse_lgd_districts(districts_path)
            self.assertEqual(len(districts), 1)
            self.assertEqual(districts[0]["state_code"], "35")
            self.assertEqual(districts[0]["code"], "601")
            self.assertEqual(districts[0]["name"], "Nicobars")
            self.assertEqual(districts[0]["census_2011_code"], "638")

    def test_integer_conversion_and_rejection_of_float(self):
        """Test integer conversion converts '35.0' to '35' and rejects non-integer floats like '35.5'."""
        self.assertEqual(to_int_str("35.0"), "35")
        self.assertEqual(to_int_str("35"), "35")
        self.assertEqual(to_int_str("0.0"), "0")
        self.assertEqual(to_int_str("0"), "0")
        self.assertEqual(to_int_str("100.00"), "100")

        with self.assertRaises(ValueError):
            to_int_str("35.5")
        with self.assertRaises(ValueError):
            to_int_str("35.1")
        with self.assertRaises(ValueError):
            to_int_str("abc")
        with self.assertRaises(ValueError):
            to_int_str("")
        with self.assertRaises(ValueError):
            to_int_str(None)

        self.assertIsNone(parse_census_code("0.0"))
        self.assertIsNone(parse_census_code("0"))
        self.assertIsNone(parse_census_code(""))
        self.assertIsNone(parse_census_code(None))
        self.assertEqual(parse_census_code("638.0"), "638")
        with self.assertRaises(ValueError):
            parse_census_code("35.5")

        # Test parsing an .xlsx file with '35.5' rejects it
        with tempfile.TemporaryDirectory() as tmpdir:
            bad_path = os.path.join(tmpdir, "bad_state.xlsx")
            bad_data = [
                ["Title"],
                EXPECTED_STATE_HEADERS,
                ["1.0", "35.5", "1.0", "Bad State", "Local", "0.0", "0.0", "State"],
            ]
            create_test_xlsx(bad_data, bad_path)
            with self.assertRaises(ValueError) as ctx:
                parse_lgd_states(bad_path)
            self.assertIn("35.5", str(ctx.exception))

    def test_missing_header_fails(self):
        """Test that missing required headers fail clearly."""
        with tempfile.TemporaryDirectory() as tmpdir:
            # States missing "State Code"
            p1 = os.path.join(tmpdir, "missing_code.xlsx")
            headers_no_code = [h for h in EXPECTED_STATE_HEADERS if h != "State Code"]
            create_test_xlsx([["Title"], headers_no_code, ["1.0"] * len(headers_no_code)], p1)
            with self.assertRaises(ValueError) as ctx:
                parse_lgd_states(p1)
            self.assertIn("Missing required header 'State Code'", str(ctx.exception))

            # States missing "State Name (In English)"
            p2 = os.path.join(tmpdir, "missing_name.xlsx")
            headers_no_name = [h for h in EXPECTED_STATE_HEADERS if h != "State Name (In English)"]
            create_test_xlsx([["Title"], headers_no_name, ["1.0"] * len(headers_no_name)], p2)
            with self.assertRaises(ValueError) as ctx:
                parse_lgd_states(p2)
            self.assertIn("Missing required header 'State Name (In English)'", str(ctx.exception))

            # Districts missing "District Code"
            p3 = os.path.join(tmpdir, "missing_dist_code.xlsx")
            headers_no_dist = [h for h in EXPECTED_DISTRICT_HEADERS if h != "District Code"]
            create_test_xlsx([["Title"], headers_no_dist, ["1.0"] * len(headers_no_dist)], p3)
            with self.assertRaises(ValueError) as ctx:
                parse_lgd_districts(p3)
            self.assertIn("Missing required header 'District Code'", str(ctx.exception))

    def test_district_with_unknown_state_aborts(self):
        """Test that a district referencing an unknown state code aborts validation and SQL contains check."""
        states = [{"code": "35", "name": "Andaman"}]
        districts = [{"state_code": "99", "code": "901", "name": "Lost District", "census_2011_code": ""}]

        with self.assertRaises(ValueError) as ctx:
            validate_lgd(states, districts, exact_counts=False)
        self.assertIn("unknown state code '99'", str(ctx.exception))

        sql = generate_lgd_sql("/tmp/s.csv", "/tmp/d.csv", "src", "sha256", 2, "msg")
        self.assertIn("Every district must map to a loaded state", sql)

    def test_lgd_exact_counts_and_duplicates_validation(self):
        """Test validation of 36 states, 790 districts, and duplicate code checks."""
        states_2 = [{"code": "35", "name": "S1"}, {"code": "28", "name": "S2"}]
        districts_3 = [
            {"state_code": "35", "code": "601", "name": "D1", "census_2011_code": ""},
            {"state_code": "35", "code": "602", "name": "D2", "census_2011_code": ""},
            {"state_code": "28", "code": "502", "name": "D3", "census_2011_code": ""},
        ]
        # Exact count check fails for partial test data
        with self.assertRaises(ValueError) as ctx:
            validate_lgd(states_2, districts_3, exact_counts=True)
        self.assertIn("expected exactly 36 states", str(ctx.exception))

        # Duplicate state code fails
        dup_states = [{"code": "35", "name": "S1"}, {"code": "35", "name": "S2"}]
        with self.assertRaises(ValueError) as ctx:
            validate_lgd(dup_states, districts_3, exact_counts=False)
        self.assertIn("duplicate state code '35'", str(ctx.exception))

        # Duplicate district code fails
        dup_districts = [
            {"state_code": "35", "code": "601", "name": "D1", "census_2011_code": ""},
            {"state_code": "35", "code": "601", "name": "D2", "census_2011_code": ""},
        ]
        with self.assertRaises(ValueError) as ctx:
            validate_lgd(states_2, dup_districts, exact_counts=False)
        self.assertIn("duplicate district code '601'", str(ctx.exception))

    def test_lgd_generated_sql_uses_two_unique_constraints_and_nextval(self):
        """Test that generate_lgd_sql uses uq_geo_state_country_code, uq_geo_district_state_code, and sequences."""
        sql = generate_lgd_sql("/tmp/states.csv", "/tmp/districts.csv", "src", "sha256", 5, "msg")

        # Conflict targets
        self.assertIn("ON CONFLICT (country_id, code) DO UPDATE SET", sql)
        self.assertIn("ON CONFLICT (state_id, code) DO UPDATE SET", sql)

        # Sequences from V5 migration
        self.assertIn("nextval('ihms_master.geo_state_seq')", sql)
        self.assertIn("nextval('ihms_master.geo_district_seq')", sql)

        # Country 'IN' check and single transaction
        self.assertIn("Country with code ''IN'' does not exist", sql)
        self.assertIn("BEGIN;", sql)
        self.assertIn("COMMIT;", sql)

    def test_name_hindi_and_abdm_code_untouched(self):
        """Test that name_hindi and abdm_code are untouched by the LGD import SQL."""
        sql = generate_lgd_sql("/tmp/states.csv", "/tmp/districts.csv", "src", "sha256", 5, "msg")

        # geo_state table insert and update
        state_insert_block = sql[sql.index("INSERT INTO ihms_master.geo_state"):sql.index("INSERT INTO ihms_master.geo_district")]
        self.assertNotIn("name_hindi", state_insert_block)
        self.assertNotIn("abdm_code", state_insert_block)

        # geo_district table insert and update
        dist_insert_block = sql[sql.index("INSERT INTO ihms_master.geo_district"):sql.index("INSERT INTO ihms_master.master_import_log")]
        self.assertNotIn("abdm_district_code", dist_insert_block)

    def test_log_counting_fix(self):
        """Test that every subcommand correctly computes inserted and updated counts using RETURNING (xmax = 0)."""
        icd_sql = generate_icd11_sql("/tmp/icd.csv", "2025-01", "src", "sha", 100, "msg")
        geo_sql = generate_countries_sql("/tmp/geo.csv", "src", "sha", 100, "msg")
        test_sql = generate_testdata_sql("/tmp/u.csv", "/tmp/d.csv", "/tmp/s.csv", "sha", 41, "msg")
        lgd_sql = generate_lgd_sql("/tmp/s.csv", "/tmp/d.csv", "src", "sha", 826, "msg")

        for name, sql in [("icd11", icd_sql), ("countries", geo_sql), ("testdata", test_sql), ("lgd", lgd_sql)]:
            self.assertIn("(xmax = 0) AS inserted", sql, f"{name} missing (xmax = 0) AS inserted")
            self.assertNotIn("{rows_read}, {rows_read}, 0", sql, f"{name} still hardcoding rows_inserted = rows_read")

        # Verify simulation of xmax counting logic: first run inserts, rerun updates
        def simulate_xmax_counting(incoming_keys: list[str], existing_keys: set[str]):
            inserted = 0
            updated = 0
            for key in incoming_keys:
                if key in existing_keys:
                    # Conflict DO UPDATE: xmax != 0 -> inserted is False
                    updated += 1
                else:
                    # New insert: xmax = 0 -> inserted is True
                    inserted += 1
                    existing_keys.add(key)
            return inserted, updated

        db_keys: set[str] = set()
        incoming = ["K1", "K2", "K3"]

        # Run 1: all new
        ins1, upd1 = simulate_xmax_counting(incoming, db_keys)
        self.assertEqual(ins1, 3)
        self.assertEqual(upd1, 0)

        # Run 2: all rerun (all conflict and update)
        ins2, upd2 = simulate_xmax_counting(incoming, db_keys)
        self.assertEqual(ins2, 0)
        self.assertEqual(upd2, 3)

    def test_lgd_cli_dry_run(self):
        """Test lgd subcommand via CLI in --dry-run mode with 36 states and 790 districts."""
        with tempfile.TemporaryDirectory() as tmpdir:
            s_path = os.path.join(tmpdir, "states.xlsx")
            d_path = os.path.join(tmpdir, "districts.xlsx")

            s_rows = [["Title"], EXPECTED_STATE_HEADERS] + [
                ["1.0", f"{i}.0", "1.0", f"State {i}", "Local", "0.0", "0.0", "State"]
                for i in range(1, 37)
            ]
            d_rows = [["Title"], EXPECTED_DISTRICT_HEADERS] + [
                ["1.0", "1.0", "State 1", f"{i}.0", f"District {i}", "0.0", "0.0"]
                for i in range(1, 791)
            ]
            create_test_xlsx(s_rows, s_path)
            create_test_xlsx(d_rows, d_path)

            script_path = os.path.join(os.path.dirname(__file__), "import_masters.py")
            res = subprocess.run(
                [sys.executable, script_path, "lgd", s_path, d_path, "--dry-run"],
                capture_output=True,
                text=True,
            )
            self.assertEqual(res.returncode, 0, f"CLI stderr: {res.stderr}")
            self.assertIn("States parsed: 36", res.stdout)
            self.assertIn("Districts parsed: 790", res.stdout)
            self.assertIn("Total rows: 826", res.stdout)
            self.assertIn("Dry-run mode", res.stdout)


if __name__ == "__main__":
    unittest.main()
