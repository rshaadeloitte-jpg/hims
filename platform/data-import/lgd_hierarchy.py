"""LGD Hierarchy parser for SpreadsheetML 2003 XML files in state zip archives.

Streams XML files inside LGD state zip files using zipfile.ZipFile.open and
xml.etree.ElementTree.iterparse to handle files exceeding 1 GB uncompressed.
Only Python 3 standard library modules are used.
"""

from __future__ import annotations

from decimal import Decimal, InvalidOperation
import os
import re
from typing import Iterator
import xml.etree.ElementTree as ET
import zipfile

# SpreadsheetML 2003 XML namespace and tags
NS_SS = "urn:schemas-microsoft-com:office:spreadsheet"
TAG_ROW = f"{{{NS_SS}}}Row"
TAG_CELL = f"{{{NS_SS}}}Cell"
TAG_DATA = f"{{{NS_SS}}}Data"
ATTR_INDEX = f"{{{NS_SS}}}Index"

# Regex for title row 2 in village file
STATE_TITLE_RE = re.compile(
    r"(?:All\s+Villages\s+of\s+)?(.+?)\s*\(\s*State\s*code\s*:\s*(\d+)\s*\)",
    re.IGNORECASE,
)

# Table of all 12 kinds and their real normalized output fields
KINDS: dict[str, list[str]] = {
    "districtofSpecificState": [
        "s_no",
        "district_code",
        "district_version",
        "district_name_en",
        "district_name_local",
        "census_2001_code",
        "census_2011_code",
    ],
    "subDistrictofSpecificState": [
        "s_no",
        "district_code",
        "district_name",
        "subdistrict_code",
        "subdistrict_version",
        "subdistrict_name_en",
        "subdistrict_name_local",
        "census_2001_code",
        "census_2011_code",
    ],
    "villageofSpecificState": [
        "s_no",
        "district_code",
        "district_name_en",
        "sub_district_code",
        "sub_district_name_en",
        "village_code",
        "village_version",
        "village_name_en",
        "village_name_local",
        "village_status",
        "census_2001_code",
        "census_2011_code",
        "remark",
    ],
    "priLbSpecificState": [
        "s_no",
        "localbody_type_code",
        "localbody_type_name",
        "localbody_code",
        "localbody_version",
        "localbody_name_en",
        "localbody_name_local",
        "parent_localbody_code",
    ],
    "ulbSpecificState": [
        "s_no",
        "localbody_type_code",
        "localbody_type_name",
        "localbody_code",
        "localbody_version",
        "local_body_name_en",
        "local_body_name_local",
        "census_2001_code",
        "census_2011_code",
    ],
    "tlbSpecificState": [
        "s_no",
        "local_body_code",
        "local_body_version",
        "local_body_name_en",
        "local_body_name_local",
        "localbody_type_code",
        "local_body_type_name",
        "parent_localbody_code",
    ],
    "uLBWardforStateWithCov": [
        "s_no",
        "local_body_code",
        "local_body_name",
        "ward_code",
        "ward_number",
        "ward_name",
        "district_code",
        "district_name",
        "subdistrict_code",
        "subdistrict_name",
    ],
    "uLBWardforState": [
        "s_no",
        "local_body_code",
        "local_body_name",
        "ward_code",
        "ward_number",
        "ward_name",
    ],
    "priWards": [
        "s_no",
        "local_body_code",
        "local_body_name",
        "local_body_type",
        "district_level_parent_name",
        "intermediate_level_parent_name",
        "ward_code",
        "ward_number",
        "ward_name_en",
        "ward_name_local",
    ],
    "villageGramPanchayatMapping": [
        "s_no",
        "district_code",
        "district_name_en",
        "district_census_2011_code",
        "district_census_2001_code",
        "subdistrict_code",
        "subdistrict_name_en",
        "subdistrict_census_2011_code",
        "subdistrict_census_2001_code",
        "village_code",
        "village_name_en",
        "village_census_2011_code",
        "village_census_2001_code",
        "local_body_code",
        "local_body_name_en",
    ],
    "blockofspecificState": [
        "s_no",
        "district_code",
        "district_name_en",
        "block_code",
        "block_version",
        "block_name_en",
        "block_name_local",
    ],
    "allBlockStateWithCoveredVillage": [
        "state_code",
        "state_name_en",
        "district_code",
        "district_name_en",
        "block_code",
        "block_name_en",
        "village_code",
        "village_name_en",
    ],
}


def normalize_header(header_text: str, subheader_text: str = "") -> str:
    """Normalize header and sub-header into snake_case identifier."""
    combined = header_text.strip()
    if subheader_text and subheader_text.strip():
        combined = f"{combined} {subheader_text.strip()}"
    combined = re.sub(r"\(\s*in\s+english\s*\)", "_en", combined, flags=re.I)
    combined = re.sub(r"\(\s*in\s+local\s*\)", "_local", combined, flags=re.I)
    combined = re.sub(r"^s\.?\s*no\.?$", "s_no", combined, flags=re.I)
    s = re.sub(r"[^a-zA-Z0-9_]+", "_", combined)
    return re.sub(r"_+", "_", s).strip("_").lower()


def convert_code(val: str, field_name: str = "code") -> str:
    """Convert code field to integer string (e.g. '35.0' -> '35').

    Raises:
        ValueError on non-integer codes.
    """
    s = str(val).strip() if val is not None else ""
    if not s:
        return ""
    try:
        d = Decimal(s)
    except InvalidOperation:
        raise ValueError(f"Non-integer code for {field_name}: {val!r}")
    if d != d.to_integral_value():
        raise ValueError(f"Non-integer code for {field_name}: {val!r}")
    return str(int(d))


def convert_census_code(val: str, field_name: str = "census_code") -> str:
    """Keep census codes as strings with leading zeros stripped only when numeric."""
    s = str(val).strip() if val is not None else ""
    if not s:
        return ""
    if s.isdigit():
        return s.lstrip("0") or "0"
    if s.endswith(".0") and s[:-2].isdigit():
        return s[:-2].lstrip("0") or "0"
    return s


def find_file_for_kind(z: zipfile.ZipFile, kind: str) -> str:
    """Find member filename in zip matching the given kind prefix."""
    if kind not in KINDS:
        raise ValueError(f"Unknown kind: {kind!r}")
    candidates = []
    for name in z.namelist():
        if name.startswith(kind):
            # Differentiate uLBWardforState from uLBWardforStateWithCov
            if kind == "uLBWardforState" and name.startswith("uLBWardforStateWithCov"):
                continue
            candidates.append(name)
    if not candidates:
        raise ValueError(f"No file found for kind {kind!r} in {z.filename}")
    return candidates[0]


def is_blank_or_footer(cells: dict[int, str]) -> bool:
    """Check if row is empty or a metadata footer."""
    non_empty = [v for v in cells.values() if v.strip()]
    if not non_empty:
        return True
    if len(non_empty) == 1:
        val = non_empty[0]
        # Generation timestamp in column 1 is not a numeric serial/code
        if not val.replace(".", "").isdigit():
            return True
    return False


def _parse_row_cells(elem: ET.Element) -> dict[int, str]:
    """Extract cell values with 1-based column indices respecting ss:Index."""
    cells: dict[int, str] = {}
    curr_col = 1
    for cell in elem.findall(TAG_CELL):
        idx = cell.attrib.get(ATTR_INDEX)
        if idx:
            curr_col = int(idx)
        data = cell.find(TAG_DATA)
        val = "".join(data.itertext()).strip() if data is not None else ""
        if val:
            cells[curr_col] = val
        curr_col += 1
    return cells


def _process_data_row(
    cells: dict[int, str],
    col_keys: dict[int, str],
    expected_fields: list[str],
) -> dict[str, str]:
    """Process cell values, convert codes, and return dict matching expected fields."""
    row_dict: dict[str, str] = {f: "" for f in expected_fields}
    for col, key in col_keys.items():
        val = cells.get(col, "")
        if "census" in key:
            val = convert_census_code(val, key)
        elif "code" in key:
            val = convert_code(val, key)
        elif key == "s_no" and val:
            if val.endswith(".0") and val[:-2].isdigit():
                val = val[:-2]
        row_dict[key] = val
    return row_dict


def state_of(zip_path: str | os.PathLike) -> tuple[int, str]:
    """Parse state code and state name from title row 2 of the village file.

    Returns:
        (state_code, state_name) e.g. (28, "Andhra Pradesh")

    Raises:
        ValueError: if village file is absent or title row cannot be parsed.
    """
    with zipfile.ZipFile(zip_path) as z:
        village_file = find_file_for_kind(z, "villageofSpecificState")
        with z.open(village_file) as f:
            r = 0
            for event, elem in ET.iterparse(f, events=("end",)):
                if elem.tag == TAG_ROW or elem.tag.endswith("Row"):
                    r += 1
                    for data in elem.findall(f".//{TAG_DATA}"):
                        text = "".join(data.itertext()).strip()
                        m = STATE_TITLE_RE.search(text)
                        if m:
                            elem.clear()
                            return int(m.group(2)), m.group(1).strip()
                    elem.clear()
                    if r >= 10:
                        break
    raise ValueError(f"State code and name not found in title row of {zip_path}")


def iter_rows(zip_path: str | os.PathLike, kind: str) -> Iterator[dict[str, str]]:
    """Yield dicts keyed by normalized header names for the specified kind.

    Streams XML using zipfile.ZipFile.open and xml.etree.ElementTree.iterparse,
    clearing each Row element to handle large files without high memory usage.
    """
    if kind not in KINDS:
        raise ValueError(f"Unknown kind: {kind!r}")
    expected_fields = KINDS[kind]

    with zipfile.ZipFile(zip_path) as z:
        fname = find_file_for_kind(z, kind)
        with z.open(fname) as f:
            header_row: tuple[int, dict[int, str]] | None = None
            sub_header: dict[int, str] = {}
            col_keys: dict[int, str] = {}
            r = 0
            for event, elem in ET.iterparse(f, events=("end",)):
                if elem.tag == TAG_ROW or elem.tag.endswith("Row"):
                    r += 1
                    cells = _parse_row_cells(elem)
                    vals = [v.lower() for v in cells.values()]

                    if header_row is None:
                        if any(re.match(r"^s\.?\s*no\.?$", v) for v in vals) or cells.get(1, "").strip().lower() == "state code":
                            header_row = (r, cells)
                    elif not col_keys:
                        if r == header_row[0] + 1 and any("(in english)" in v or "(in local)" in v for v in vals):
                            sub_header = cells
                        max_c = max(list(header_row[1].keys()) + list(sub_header.keys()))
                        for c in range(1, max_c + 1):
                            if c in header_row[1] or c in sub_header:
                                col_keys[c] = normalize_header(header_row[1].get(c, ""), sub_header.get(c, ""))

                        if r > header_row[0] and (not sub_header or r > header_row[0] + 1):
                            if not is_blank_or_footer(cells):
                                yield _process_data_row(cells, col_keys, expected_fields)
                    else:
                        if not is_blank_or_footer(cells):
                            yield _process_data_row(cells, col_keys, expected_fields)
                    elem.clear()


def count_rows(zip_path: str | os.PathLike, kind: str) -> int:
    """Count number of data rows for the given kind in the zip archive."""
    return sum(1 for _ in iter_rows(zip_path, kind))
