"""Unit tests for lgd_hierarchy module.

Tests streaming XML parsing, header normalization, duplicate header resolution,
code conversion and validation, blank row filtering, and per-kind row counts
using real sample zips (chandigarh.zip and lakshadweep.zip).
"""

from __future__ import annotations

import io
import os
from pathlib import Path
import unittest
from unittest.mock import patch
import zipfile

from lgd_hierarchy import (
    KINDS,
    convert_census_code,
    convert_code,
    count_rows,
    iter_rows,
    normalize_header,
    state_of,
)

BASE_DIR = Path(__file__).resolve().parent
TESTDATA_DIR = BASE_DIR / "testdata" / "lgd"
CHANDIGARH_ZIP = TESTDATA_DIR / "chandigarh.zip"
LAKSHADWEEP_ZIP = TESTDATA_DIR / "lakshadweep.zip"

EXPECTED_COUNTS_CHANDIGARH = {
    "districtofSpecificState": 1,
    "subDistrictofSpecificState": 1,
    "villageofSpecificState": 0,
    "priLbSpecificState": 0,
    "ulbSpecificState": 1,
    "tlbSpecificState": 0,
    "uLBWardforStateWithCov": 35,
    "uLBWardforState": 35,
    "priWards": 0,
    "villageGramPanchayatMapping": 0,
    "blockofspecificState": 1,
    "allBlockStateWithCoveredVillage": 0,
}

EXPECTED_COUNTS_LAKSHADWEEP = {
    "districtofSpecificState": 1,
    "subDistrictofSpecificState": 10,
    "villageofSpecificState": 27,
    "priLbSpecificState": 11,
    "ulbSpecificState": 0,
    "tlbSpecificState": 0,
    "uLBWardforStateWithCov": 0,
    "uLBWardforState": 0,
    "priWards": 1,
    "villageGramPanchayatMapping": 27,
    "blockofspecificState": 10,
    "allBlockStateWithCoveredVillage": 10,
}


def _create_synthetic_zip(filename: str, xml_content: str) -> io.BytesIO:
    """Helper to create an in-memory zip archive containing a SpreadsheetML file."""
    buf = io.BytesIO()
    with zipfile.ZipFile(buf, "w", zipfile.ZIP_DEFLATED) as z:
        z.writestr(filename, xml_content.encode("utf-8"))
    buf.seek(0)
    return buf


class TestLgdHierarchy(unittest.TestCase):
    """Test suite for LGD hierarchy parser."""

    def test_state_of_real_zips(self) -> None:
        """Verify state_of parses (state_code, state_name) from title row 2 of village file."""
        ch_code, ch_name = state_of(CHANDIGARH_ZIP)
        self.assertEqual(ch_code, 4)
        self.assertEqual(ch_name, "Chandigarh")

        lk_code, lk_name = state_of(LAKSHADWEEP_ZIP)
        self.assertEqual(lk_code, 31)
        self.assertEqual(lk_name, "Lakshadweep")

    def test_state_of_exceptions(self) -> None:
        """Verify state_of raises ValueError if village file is absent or invalid."""
        # Zip without village file
        empty_zip = _create_synthetic_zip(
            "districtofSpecificState2026.xls",
            '<?xml version="1.0"?><Workbook xmlns="urn:schemas-microsoft-com:office:spreadsheet"/>',
        )
        with self.assertRaises(ValueError):
            state_of(empty_zip)

        # Village file without state title
        bad_title_xml = """<?xml version="1.0"?>
<Workbook xmlns="urn:schemas-microsoft-com:office:spreadsheet" xmlns:ss="urn:schemas-microsoft-com:office:spreadsheet">
  <Worksheet ss:Name="Sheet1">
    <Table>
      <Row><Cell><Data ss:Type="String">Local Government Directory</Data></Cell></Row>
      <Row><Cell><Data ss:Type="String">Random title with no code</Data></Cell></Row>
    </Table>
  </Worksheet>
</Workbook>"""
        bad_zip = _create_synthetic_zip("villageofSpecificState2026.xls", bad_title_xml)
        with self.assertRaises(ValueError):
            state_of(bad_zip)

    def test_all_12_kinds_parse_no_exception(self) -> None:
        """Verify that each of the 12 kinds parses with no exception for both real zips."""
        self.assertEqual(len(KINDS), 12)
        for zpath in (CHANDIGARH_ZIP, LAKSHADWEEP_ZIP):
            for kind in KINDS:
                rows = list(iter_rows(zpath, kind))
                self.assertIsInstance(rows, list)

    def test_expected_keys_present_for_each_kind(self) -> None:
        """Verify that yielded dicts contain all expected keys matching KINDS table."""
        for zpath in (CHANDIGARH_ZIP, LAKSHADWEEP_ZIP):
            for kind, expected_fields in KINDS.items():
                for row in iter_rows(zpath, kind):
                    self.assertEqual(list(row.keys()), expected_fields)
                    for key in expected_fields:
                        self.assertIn(key, row)

    def test_duplicate_headers_become_distinct_keys(self) -> None:
        """Verify duplicate English and Local headers become distinct keys."""
        # districtofSpecificState
        self.assertIn("district_name_en", KINDS["districtofSpecificState"])
        self.assertIn("district_name_local", KINDS["districtofSpecificState"])

        # subDistrictofSpecificState
        self.assertIn("subdistrict_name_en", KINDS["subDistrictofSpecificState"])
        self.assertIn("subdistrict_name_local", KINDS["subDistrictofSpecificState"])

        # villageofSpecificState
        self.assertIn("village_name_en", KINDS["villageofSpecificState"])
        self.assertIn("village_name_local", KINDS["villageofSpecificState"])

        # priLbSpecificState
        self.assertIn("localbody_name_en", KINDS["priLbSpecificState"])
        self.assertIn("localbody_name_local", KINDS["priLbSpecificState"])

        # ulbSpecificState
        self.assertIn("local_body_name_en", KINDS["ulbSpecificState"])
        self.assertIn("local_body_name_local", KINDS["ulbSpecificState"])

        # tlbSpecificState
        self.assertIn("local_body_name_en", KINDS["tlbSpecificState"])
        self.assertIn("local_body_name_local", KINDS["tlbSpecificState"])

        # priWards
        self.assertIn("ward_name_en", KINDS["priWards"])
        self.assertIn("ward_name_local", KINDS["priWards"])

        # blockofspecificState
        self.assertIn("block_name_en", KINDS["blockofspecificState"])
        self.assertIn("block_name_local", KINDS["blockofspecificState"])

        # Check in actual rows where English and Local are present
        ch_dist = next(iter_rows(CHANDIGARH_ZIP, "districtofSpecificState"))
        self.assertEqual(ch_dist["district_name_en"], "Chandigarh")
        self.assertEqual(ch_dist["district_name_local"], "CHANDIGARH")

        lk_dist = next(iter_rows(LAKSHADWEEP_ZIP, "districtofSpecificState"))
        self.assertEqual(lk_dist["district_name_en"], "Lakshadweep District")
        self.assertEqual(lk_dist["district_name_local"], "LAKSHADWEEP DISTRICT")

    def test_code_conversion_and_rejection(self) -> None:
        """Verify code fields convert to integer strings and non-integers are rejected."""
        # Unit conversion tests
        self.assertEqual(convert_code("35.0"), "35")
        self.assertEqual(convert_code("35"), "35")
        self.assertEqual(convert_code("044.0"), "44")
        self.assertEqual(convert_code("1"), "1")
        self.assertEqual(convert_code("0"), "0")
        self.assertEqual(convert_code(""), "")
        self.assertEqual(convert_code(None), "")

        # Rejection of 35.5 and non-numeric codes
        with self.assertRaises(ValueError):
            convert_code("35.5")
        with self.assertRaises(ValueError):
            convert_code("abc")
        with self.assertRaises(ValueError):
            convert_code("12.34.56")

        # Test within iter_rows on synthetic XML
        valid_xml = """<?xml version="1.0"?>
<Workbook xmlns="urn:schemas-microsoft-com:office:spreadsheet" xmlns:ss="urn:schemas-microsoft-com:office:spreadsheet">
  <Worksheet ss:Name="Sheet1">
    <Table>
      <Row><Cell><Data ss:Type="String">Local Government Directory</Data></Cell></Row>
      <Row><Cell><Data ss:Type="String">All Districts</Data></Cell></Row>
      <Row><Cell><Data ss:Type="String"></Data></Cell></Row>
      <Row>
        <Cell ss:Index="1"><Data ss:Type="String">S.No.</Data></Cell>
        <Cell ss:Index="2"><Data ss:Type="String">District Code</Data></Cell>
        <Cell ss:Index="3"><Data ss:Type="String">District Version</Data></Cell>
        <Cell ss:Index="4"><Data ss:Type="String">District Name</Data></Cell>
        <Cell ss:Index="5"><Data ss:Type="String">District Name</Data></Cell>
        <Cell ss:Index="6"><Data ss:Type="String">Census 2001 Code</Data></Cell>
        <Cell ss:Index="7"><Data ss:Type="String">Census 2011 Code</Data></Cell>
      </Row>
      <Row>
        <Cell ss:Index="4"><Data ss:Type="String">(In English)</Data></Cell>
        <Cell ss:Index="5"><Data ss:Type="String">(In Local)</Data></Cell>
      </Row>
      <Row>
        <Cell ss:Index="1"><Data ss:Type="String">1</Data></Cell>
        <Cell ss:Index="2"><Data ss:Type="String">35.0</Data></Cell>
        <Cell ss:Index="3"><Data ss:Type="String">1</Data></Cell>
        <Cell ss:Index="4"><Data ss:Type="String">Test</Data></Cell>
        <Cell ss:Index="5"><Data ss:Type="String">TEST</Data></Cell>
        <Cell ss:Index="6"><Data ss:Type="String">01</Data></Cell>
        <Cell ss:Index="7"><Data ss:Type="String">055</Data></Cell>
      </Row>
    </Table>
  </Worksheet>
</Workbook>"""
        z_valid = _create_synthetic_zip("districtofSpecificState2026.xls", valid_xml)
        rows = list(iter_rows(z_valid, "districtofSpecificState"))
        self.assertEqual(len(rows), 1)
        self.assertEqual(rows[0]["district_code"], "35")
        self.assertEqual(rows[0]["census_2001_code"], "1")
        self.assertEqual(rows[0]["census_2011_code"], "55")

        # Invalid XML with 35.5 in district_code
        invalid_xml = valid_xml.replace(">35.0<", ">35.5<")
        z_invalid = _create_synthetic_zip("districtofSpecificState2026.xls", invalid_xml)
        with self.assertRaises(ValueError):
            list(iter_rows(z_invalid, "districtofSpecificState"))

    def test_census_code_conversion(self) -> None:
        """Verify census codes strip leading zeros only when numeric."""
        self.assertEqual(convert_census_code("055"), "55")
        self.assertEqual(convert_census_code("01"), "1")
        self.assertEqual(convert_census_code("00277"), "277")
        self.assertEqual(convert_census_code("00001000"), "1000")
        self.assertEqual(convert_census_code("0"), "0")
        self.assertEqual(convert_census_code("00"), "0")
        self.assertEqual(convert_census_code(""), "")
        self.assertEqual(convert_census_code(None), "")

        # Non-numeric keeps leading zeros
        self.assertEqual(convert_census_code("00A1"), "00A1")
        self.assertEqual(convert_census_code("N/A"), "N/A")

    def test_streaming_enforced(self) -> None:
        """Verify streaming is enforced by ensuring zipfile.ZipFile.read is never called."""
        with patch.object(zipfile.ZipFile, "read", side_effect=AssertionError("ZipFile.read called!")):
            # state_of
            c_code, _ = state_of(CHANDIGARH_ZIP)
            self.assertEqual(c_code, 4)
            l_code, _ = state_of(LAKSHADWEEP_ZIP)
            self.assertEqual(l_code, 31)

            # iter_rows and count_rows for all 12 kinds
            for kind in KINDS:
                count_rows(CHANDIGARH_ZIP, kind)
                count_rows(LAKSHADWEEP_ZIP, kind)

    def test_blank_rows_skipped(self) -> None:
        """Verify blank rows and generation timestamp footers are skipped."""
        xml_with_blanks = """<?xml version="1.0"?>
<Workbook xmlns="urn:schemas-microsoft-com:office:spreadsheet" xmlns:ss="urn:schemas-microsoft-com:office:spreadsheet">
  <Worksheet ss:Name="Sheet1">
    <Table>
      <Row><Cell><Data ss:Type="String">Local Government Directory</Data></Cell></Row>
      <Row><Cell><Data ss:Type="String">All Districts</Data></Cell></Row>
      <Row><Cell><Data ss:Type="String"></Data></Cell></Row>
      <Row>
        <Cell ss:Index="1"><Data ss:Type="String">S.No.</Data></Cell>
        <Cell ss:Index="2"><Data ss:Type="String">District Code</Data></Cell>
        <Cell ss:Index="3"><Data ss:Type="String">District Version</Data></Cell>
        <Cell ss:Index="4"><Data ss:Type="String">District Name</Data></Cell>
        <Cell ss:Index="5"><Data ss:Type="String">District Name</Data></Cell>
        <Cell ss:Index="6"><Data ss:Type="String">Census 2001 Code</Data></Cell>
        <Cell ss:Index="7"><Data ss:Type="String">Census 2011 Code</Data></Cell>
      </Row>
      <Row>
        <Cell ss:Index="4"><Data ss:Type="String">(In English)</Data></Cell>
        <Cell ss:Index="5"><Data ss:Type="String">(In Local)</Data></Cell>
      </Row>
      <Row/>
      <Row><Cell ss:Index="1"><Data ss:Type="String">   </Data></Cell></Row>
      <Row>
        <Cell ss:Index="1"><Data ss:Type="String">1</Data></Cell>
        <Cell ss:Index="2"><Data ss:Type="String">44</Data></Cell>
        <Cell ss:Index="3"><Data ss:Type="String">1</Data></Cell>
        <Cell ss:Index="4"><Data ss:Type="String">Dist A</Data></Cell>
        <Cell ss:Index="5"><Data ss:Type="String">DIST A</Data></Cell>
        <Cell ss:Index="6"><Data ss:Type="String">01</Data></Cell>
        <Cell ss:Index="7"><Data ss:Type="String">055</Data></Cell>
      </Row>
      <Row/>
      <Row><Cell ss:Index="1"><Data ss:Type="String">Nov 8, 2026, 7:43 AM</Data></Cell></Row>
    </Table>
  </Worksheet>
</Workbook>"""
        z_blanks = _create_synthetic_zip("districtofSpecificState2026.xls", xml_with_blanks)
        rows = list(iter_rows(z_blanks, "districtofSpecificState"))
        self.assertEqual(len(rows), 1)
        self.assertEqual(rows[0]["district_name_en"], "Dist A")

    def test_row_counts_and_print_per_kind(self) -> None:
        """Validate row counts for both zips and print per-kind table in test output."""
        ch_counts: dict[str, int] = {}
        lk_counts: dict[str, int] = {}

        for kind in KINDS:
            ch_counts[kind] = count_rows(CHANDIGARH_ZIP, kind)
            lk_counts[kind] = count_rows(LAKSHADWEEP_ZIP, kind)

        # Print per-kind row counts table
        sep = "=" * 70
        print(f"\n{sep}")
        print("LGD HIERARCHY PER-KIND ROW COUNTS")
        print(sep)
        print(f"{'Kind':35s} {'Chandigarh':>15s} {'Lakshadweep':>15s}")
        print("-" * 70)
        for kind in KINDS:
            print(f"{kind:35s} {ch_counts[kind]:15d} {lk_counts[kind]:15d}")
        print(sep)

        # Assert exact counts match expected counts
        self.assertEqual(ch_counts, EXPECTED_COUNTS_CHANDIGARH)
        self.assertEqual(lk_counts, EXPECTED_COUNTS_LAKSHADWEEP)


if __name__ == "__main__":
    unittest.main()
