#!/usr/bin/env python3
import os
import re
import unittest
from pathlib import Path

from import_masters import (
    ALLOWED_COUNTRY_COLUMNS,
    compute_sha256,
    derive_parent_linearization_uris,
    generate_countries_sql,
    generate_icd11_sql,
    generate_test_data,
    generate_testdata_sql,
    get_psql_cmd,
    map_country_row,
    parse_country_file,
    parse_icd11_file,
    strip_title,
)

TESTDATA_DIR = Path(__file__).resolve().parent / "testdata"
ICD11_SAMPLE = TESTDATA_DIR / "icd11-sample.txt"
COUNTRY_SAMPLE = TESTDATA_DIR / "country-sample.csv"


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


if __name__ == "__main__":
    unittest.main()
