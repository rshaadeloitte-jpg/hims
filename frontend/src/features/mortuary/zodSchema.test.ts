import { describe, expect, it } from "vitest";
import {
  chamberAllocationSchema,
  mortuaryClearanceSchema,
  deathDetailsSchema,
  deathCertificateRequestSchema,
  chamberStatusSchema,
  CHAMBER_STATUS_TONE,
  CHAMBER_STATUS_LABEL,
} from "./zodSchema";

describe("chamberAllocationSchema", () => {
  it("accepts valid allocation", () => {
    const result = chamberAllocationSchema.safeParse({
      chamberId: 1,
      patientId: 42,
      bodyName: "John Doe",
      causeOfDeath: "Cardiac arrest",
      admittedAt: "2026-09-11T10:30",
    });
    expect(result.success).toBe(true);
  });

  it("requires chamberId, patientId, bodyName, admittedAt", () => {
    const result = chamberAllocationSchema.safeParse({
      chamberId: 0,
      patientId: 0,
      bodyName: "",
      admittedAt: "",
    });
    expect(result.success).toBe(false);
    if (!result.success) {
      const paths = result.error.issues.map((i) => i.path.join("."));
      expect(paths).toContain("chamberId");
      expect(paths).toContain("patientId");
      expect(paths).toContain("bodyName");
      expect(paths).toContain("admittedAt");
    }
  });

  it("coerces numeric strings to numbers", () => {
    const parsed = chamberAllocationSchema.parse({
      chamberId: "5",
      patientId: "100",
      bodyName: "Jane Smith",
      admittedAt: "2026-09-11T12:00",
    });
    expect(parsed.chamberId).toBe(5);
    expect(typeof parsed.chamberId).toBe("number");
    expect(parsed.patientId).toBe(100);
  });

  it("allows optional causeOfDeath", () => {
    const result = chamberAllocationSchema.safeParse({
      chamberId: 1,
      patientId: 42,
      bodyName: "John Doe",
      admittedAt: "2026-09-11T10:30",
    });
    expect(result.success).toBe(true);
  });
});

describe("mortuaryClearanceSchema", () => {
  it("accepts valid clearance", () => {
    const result = mortuaryClearanceSchema.safeParse({
      chamberId: 1,
      releasedTo: "Mary Doe",
      relationship: "SPOUSE",
      recipientId: "DL-1234",
      releasedAt: "2026-09-12T09:00",
      remark: "Family handover",
    });
    expect(result.success).toBe(true);
  });

  it("requires releasedTo and releasedAt", () => {
    const result = mortuaryClearanceSchema.safeParse({
      chamberId: 1,
      releasedTo: "",
      relationship: "OTHER",
      releasedAt: "",
    });
    expect(result.success).toBe(false);
    if (!result.success) {
      const paths = result.error.issues.map((i) => i.path.join("."));
      expect(paths).toContain("releasedTo");
      expect(paths).toContain("releasedAt");
    }
  });

  it("defaults relationship to OTHER", () => {
    const parsed = mortuaryClearanceSchema.parse({
      chamberId: 1,
      releasedTo: "Test",
      releasedAt: "2026-09-12T09:00",
    });
    expect(parsed.relationship).toBe("OTHER");
  });
});

describe("deathDetailsSchema", () => {
  it("accepts valid death details", () => {
    const result = deathDetailsSchema.safeParse({
      patientId: 42,
      isDeath: true,
      deathDate: "2026-09-11T10:30",
      causeOfDeath: "Myocardial infarction",
      deathType: "NATURAL",
      certifiedBy: "Dr. Smith",
      isMlc: false,
    });
    expect(result.success).toBe(true);
  });

  it("requires isDeath literal true", () => {
    const result = deathDetailsSchema.safeParse({
      patientId: 42,
      isDeath: false,
      deathDate: "2026-09-11T10:30",
      causeOfDeath: "MI",
      certifiedBy: "Dr. Smith",
    });
    expect(result.success).toBe(false);
  });

  it("requires mlcNumber when isMlc is true", () => {
    const result = deathDetailsSchema.safeParse({
      patientId: 42,
      isDeath: true,
      deathDate: "2026-09-11T10:30",
      causeOfDeath: "Road traffic accident",
      deathType: "ACCIDENT",
      certifiedBy: "Dr. Smith",
      isMlc: true,
      mlcNumber: "",
    });
    expect(result.success).toBe(false);
    if (!result.success) {
      const paths = result.error.issues.map((i) => i.path.join("."));
      expect(paths).toContain("mlcNumber");
    }
  });

  it("accepts MLC case with mlcNumber", () => {
    const result = deathDetailsSchema.safeParse({
      patientId: 42,
      isDeath: true,
      deathDate: "2026-09-11T10:30",
      causeOfDeath: "Road traffic accident",
      deathType: "ACCIDENT",
      certifiedBy: "Dr. Smith",
      isMlc: true,
      mlcNumber: "MLC/2026/0042",
    });
    expect(result.success).toBe(true);
  });
});

describe("deathCertificateRequestSchema", () => {
  it("accepts valid cert request", () => {
    const result = deathCertificateRequestSchema.safeParse({
      patientId: 42,
      certNumber: "DC/2026/00042",
      placeOfDeath: "ICU, Block A",
    });
    expect(result.success).toBe(true);
  });

  it("requires patientId and certNumber", () => {
    const result = deathCertificateRequestSchema.safeParse({
      patientId: 0,
      certNumber: "",
    });
    expect(result.success).toBe(false);
  });
});

describe("chamberStatusSchema", () => {
  it("accepts all valid statuses", () => {
    for (const s of ["EMPTY", "OCCUPIED", "CLEARING", "RELEASED"]) {
      expect(chamberStatusSchema.safeParse(s).success).toBe(true);
    }
  });

  it("rejects invalid status", () => {
    expect(chamberStatusSchema.safeParse("INVALID").success).toBe(false);
  });
});

describe("chamber status tone/label maps", () => {
  it("covers all statuses", () => {
    for (const s of ["EMPTY", "OCCUPIED", "CLEARING", "RELEASED"] as const) {
      expect(typeof CHAMBER_STATUS_TONE[s]).toBe("string");
      expect(typeof CHAMBER_STATUS_LABEL[s]).toBe("string");
    }
  });
});
