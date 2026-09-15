import { describe, expect, it } from "vitest";
import {
  formDefinitionSchema,
  formSubmissionSchema,
  submissionFilterSchema,
  validateSchemaJson,
} from "./zodSchema";

describe("M40 Forms Zod schemas", () => {
  describe("formDefinitionSchema", () => {
    it("accepts a valid form definition", () => {
      const result = formDefinitionSchema.safeParse({
        code: "CONSULT_V1",
        name: "Consultation Note",
        specialty: "General Medicine",
        description: "Standard OPD consultation form",
        version: 1,
        schema: { type: "object", properties: {}, required: [] },
      });
      expect(result.success).toBe(true);
    });

    it("requires uppercase code", () => {
      const result = formDefinitionSchema.safeParse({
        code: "lowercase_code",
        name: "Test",
        specialty: "Cardiology",
        version: 1,
        schema: {},
      });
      expect(result.success).toBe(false);
    });

    it("requires code, name, and specialty", () => {
      const result = formDefinitionSchema.safeParse({
        code: "",
        name: "",
        specialty: "",
        version: 1,
        schema: {},
      });
      expect(result.success).toBe(false);
      if (!result.success) {
        const paths = result.error.issues.map((i) => i.path.join("."));
        expect(paths).toContain("code");
        expect(paths).toContain("name");
        expect(paths).toContain("specialty");
      }
    });

    it("defaults version to 1", () => {
      const parsed = formDefinitionSchema.parse({
        code: "TEST_V1",
        name: "Test",
        specialty: "TestSpecialty",
        schema: {},
      });
      expect(parsed.version).toBe(1);
    });

    it("coerces string version to number", () => {
      const parsed = formDefinitionSchema.parse({
        code: "TEST_V1",
        name: "Test",
        specialty: "TestSpecialty",
        version: "3",
        schema: {},
      });
      expect(parsed.version).toBe(3);
      expect(typeof parsed.version).toBe("number");
    });

    it("treats blank description as undefined", () => {
      const parsed = formDefinitionSchema.parse({
        code: "TEST_V1",
        name: "Test",
        specialty: "TestSpecialty",
        description: "",
        version: 1,
        schema: {},
      });
      expect(parsed.description).toBeUndefined();
    });

    it("rejects non-positive version", () => {
      const result = formDefinitionSchema.safeParse({
        code: "TEST_V1",
        name: "Test",
        specialty: "TestSpecialty",
        version: 0,
        schema: {},
      });
      expect(result.success).toBe(false);
    });
  });

  describe("formSubmissionSchema", () => {
    it("parses a valid submission", () => {
      const result = formSubmissionSchema.safeParse({
        id: 1,
        formId: 10,
        patientId: 100,
        encounterId: 200,
        submissionJson: '{"key": "value"}',
        submittedBy: 5,
        submittedAt: "2025-09-11T10:00:00Z",
        status: true,
      });
      expect(result.success).toBe(true);
    });

    it("allows optional fields to be missing", () => {
      const result = formSubmissionSchema.safeParse({
        id: 1,
        formId: 10,
        submissionJson: "{}",
        submittedAt: "2025-09-11T10:00:00Z",
      });
      expect(result.success).toBe(true);
    });

    it("rejects missing required fields", () => {
      const result = formSubmissionSchema.safeParse({
        submissionJson: "{}",
        submittedAt: "2025-09-11T10:00:00Z",
      });
      expect(result.success).toBe(false);
    });

    it("coerces string IDs to numbers", () => {
      const parsed = formSubmissionSchema.parse({
        id: "1",
        formId: "10",
        patientId: "100",
        submissionJson: "{}",
        submittedAt: "2025-09-11T10:00:00Z",
      });
      expect(parsed.id).toBe(1);
      expect(parsed.formId).toBe(10);
      expect(parsed.patientId).toBe(100);
    });
  });

  describe("submissionFilterSchema", () => {
    it("accepts empty filter", () => {
      const result = submissionFilterSchema.safeParse({});
      expect(result.success).toBe(true);
    });

    it("coerces string IDs to numbers", () => {
      const parsed = submissionFilterSchema.parse({
        patientId: "123",
        encounterId: "456",
        formId: "789",
        fromDate: "2025-01-01",
        toDate: "2025-12-31",
      });
      expect(parsed.patientId).toBe(123);
      expect(parsed.encounterId).toBe(456);
      expect(parsed.formId).toBe(789);
    });

    it("treats blank strings as undefined", () => {
      const parsed = submissionFilterSchema.parse({
        patientId: "",
        fromDate: "",
        toDate: "",
      });
      expect(parsed.patientId).toBeUndefined();
      expect(parsed.fromDate).toBeUndefined();
      expect(parsed.toDate).toBeUndefined();
    });
  });

  describe("validateSchemaJson", () => {
    it("validates correct JSON object", () => {
      const result = validateSchemaJson('{"type": "object", "properties": {}}');
      expect(result.ok).toBe(true);
      if (result.ok) {
        expect(result.value).toEqual({ type: "object", properties: {} });
      }
    });

    it("rejects invalid JSON", () => {
      const result = validateSchemaJson("{invalid json}");
      expect(result.ok).toBe(false);
      if (!result.ok) {
        expect(result.error).toBeTruthy();
      }
    });

    it("rejects JSON array", () => {
      const result = validateSchemaJson("[1,2,3]");
      expect(result.ok).toBe(false);
      if (!result.ok) {
        expect(result.error).toBe("Schema must be a JSON object");
      }
    });

    it("rejects JSON primitive", () => {
      const result = validateSchemaJson("42");
      expect(result.ok).toBe(false);
    });

    it("rejects null", () => {
      const result = validateSchemaJson("null");
      expect(result.ok).toBe(false);
    });
  });
});
