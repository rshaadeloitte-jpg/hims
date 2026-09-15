import { describe, expect, it } from "vitest";
import { getEntityMeta, type EntityMeta } from "./entityMeta";
import { entityMetaToZod } from "./zodSchema";

const unitMeta = getEntityMeta("unitmaster")!;
const schema = entityMetaToZod(unitMeta);

describe("entityMeta → zod schema conversion (mini FAUG-schema adapter)", () => {
  it("rejects blank values for required text fields", () => {
    const result = schema.safeParse({ unitId: 1, code: "", name: "Jaipur Hospital", status: true });
    expect(result.success).toBe(false);
    if (!result.success) {
      const paths = result.error.issues.map((i) => i.path.join("."));
      expect(paths).toContain("code");
    }
  });

  it("coerces numeric strings into numbers (number coercion)", () => {
    // HTML inputs submit strings; the adapter must coerce them for the payload.
    const parsed = schema.parse({ unitId: "42", code: "U001", name: "Jaipur Hospital" });
    expect(parsed.unitId).toBe(42);
    expect(typeof parsed.unitId).toBe("number");
  });

  it("rejects non-numeric input for number fields", () => {
    const result = schema.safeParse({ unitId: "abc", code: "U001", name: "Jaipur" });
    expect(result.success).toBe(false);
    if (!result.success) {
      expect(result.error.issues[0]?.path.join(".")).toBe("unitId");
    }
  });

  it("treats blank optional text fields as undefined", () => {
    const parsed = schema.parse({
      unitId: 1,
      code: "U001",
      name: "Jaipur Hospital",
      email: "",
      description: "",
      status: false,
    });
    expect(parsed.email).toBeUndefined();
    expect(parsed.description).toBeUndefined();
  });

  it("skips readOnly/system fields (id, addeddatetime)", () => {
    const parsed = schema.parse({ unitId: 1, code: "U001", name: "Jaipur" });
    expect("id" in parsed).toBe(false);
    expect("addeddatetime" in parsed).toBe(false);
  });

  it("validates select fields with options (optional + required)", () => {
    const meta: EntityMeta = {
      name: "demo",
      label: "Demo",
      fields: [
        {
          name: "kind",
          label: "Kind",
          type: "select",
          required: true,
          options: [
            { value: "a", label: "A" },
            { value: "b", label: "B" },
          ],
        },
      ],
    };
    const demo = entityMetaToZod(meta);
    expect(demo.safeParse({ kind: "" }).success).toBe(false);
    expect(demo.safeParse({ kind: "a" }).success).toBe(true);
  });

  it("keeps the whole entity schema compatible with the RHF resolver", () => {
    // ZodObject instance is a plain zod schema — parse/parseAsync surface intact.
    expect(typeof schema.parse).toBe("function");
    expect(schema._def.typeName).toBe("ZodObject");
  });
});
