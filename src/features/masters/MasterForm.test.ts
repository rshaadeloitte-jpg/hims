import { describe, expect, it } from "vitest";
import { filterDirtyFields, recordToFormValues } from "./components/MasterForm";
import { getEntityMeta } from "./entityMeta";

/**
 * Red-team F-18: on edit (PUT, nonNull=true) only user-changed fields may be
 * sent — absent optionals must not be clobbered by form defaults (false / "").
 */
describe("filterDirtyFields (F-18: undefined = untouched)", () => {
  it("keeps only dirty fields", () => {
    const values = { code: "D01", description: "Cardiology", email: "", status: false, unitId: "7" };
    const dirty = { description: true, status: true };
    expect(filterDirtyFields(values, dirty)).toEqual({
      description: "Cardiology",
      status: false,
    });
  });

  it("returns {} when nothing was edited", () => {
    expect(filterDirtyFields({ code: "D01", status: false }, {})).toEqual({});
  });

  it("keeps false/empty values when the user actively changed them", () => {
    const values = { status: false, isclinical: "", email: "" };
    const dirty = { status: true, isclinical: true };
    expect(filterDirtyFields(values, dirty)).toEqual({ status: false, isclinical: "" });
  });
});

describe("recordToFormValues", () => {
  it("keeps optional NULLs out of the payload path (absent booleans -> false default)", () => {
    const unitMeta = getEntityMeta("unitmaster")!;
    const values = recordToFormValues(unitMeta, {
      id: 7,
      unitId: 1,
      code: "U001",
      name: "Jaipur",
    });
    // Absent optional boolean defaults false in the FORM...
    expect(values.status).toBe(false);
    // ...but a subsequent filterDirtyFields({...}, {}) call sends nothing back.
    expect(filterDirtyFields(values as Record<string, unknown>, {})).toEqual({});
  });
});
