import { describe, expect, it } from "vitest";
import {
  caseSchema,
  eTicketSchema,
  monitorSchema,
  deviceSchema,
  caseFilterSchema,
} from "./zodSchema";

describe("caseSchema", () => {
  it("validates a valid case", () => {
    const result = caseSchema.safeParse({
      patientId: 100,
      caseType: "ANC",
      status: 1,
    });
    expect(result.success).toBe(true);
  });

  it("requires patientId", () => {
    const result = caseSchema.safeParse({
      patientId: 0,
      caseType: "ANC",
    });
    expect(result.success).toBe(false);
  });

  it("requires caseType", () => {
    const result = caseSchema.safeParse({
      patientId: 100,
      caseType: "",
    });
    expect(result.success).toBe(false);
  });

  it("defaults status to 1", () => {
    const result = caseSchema.safeParse({
      patientId: 100,
      caseType: "ANC",
    });
    expect(result.success).toBe(true);
    if (result.success) {
      expect(result.data.status).toBe(1);
    }
  });

  it("rejects status > 3", () => {
    const result = caseSchema.safeParse({
      patientId: 100,
      caseType: "ANC",
      status: 4,
    });
    expect(result.success).toBe(false);
  });

  it("coerces string patientId to number", () => {
    const result = caseSchema.safeParse({
      patientId: "100",
      caseType: "ANC",
    });
    expect(result.success).toBe(true);
    if (result.success) {
      expect(result.data.patientId).toBe(100);
    }
  });
});

describe("eTicketSchema", () => {
  it("validates a valid e-ticket", () => {
    const result = eTicketSchema.safeParse({
      subject: "Patient not reachable",
      priority: "HIGH",
    });
    expect(result.success).toBe(true);
  });

  it("requires subject", () => {
    const result = eTicketSchema.safeParse({
      subject: "",
      priority: "MEDIUM",
    });
    expect(result.success).toBe(false);
  });

  it("defaults priority to MEDIUM", () => {
    const result = eTicketSchema.safeParse({
      subject: "Test issue",
    });
    expect(result.success).toBe(true);
    if (result.success) {
      expect(result.data.priority).toBe("MEDIUM");
    }
  });

  it("defaults status to OPEN", () => {
    const result = eTicketSchema.safeParse({
      subject: "Test issue",
    });
    expect(result.success).toBe(true);
    if (result.success) {
      expect(result.data.status).toBe("OPEN");
    }
  });

  it("rejects invalid priority", () => {
    const result = eTicketSchema.safeParse({
      subject: "Test",
      priority: "URGENT",
    });
    expect(result.success).toBe(false);
  });

  it("accepts optional caseId", () => {
    const result = eTicketSchema.safeParse({
      caseId: 42,
      subject: "Test issue",
    });
    expect(result.success).toBe(true);
    if (result.success) {
      expect(result.data.caseId).toBe(42);
    }
  });

  it("rejects subject > 120 chars", () => {
    const result = eTicketSchema.safeParse({
      subject: "a".repeat(121),
    });
    expect(result.success).toBe(false);
  });
});

describe("monitorSchema", () => {
  it("validates a valid metric", () => {
    const result = monitorSchema.safeParse({
      caseId: 1,
      metric: "BP_SYSTOLIC",
      value: 120,
    });
    expect(result.success).toBe(true);
  });

  it("requires caseId", () => {
    const result = monitorSchema.safeParse({
      caseId: 0,
      metric: "BP",
      value: 120,
    });
    expect(result.success).toBe(false);
  });

  it("requires metric", () => {
    const result = monitorSchema.safeParse({
      caseId: 1,
      metric: "",
      value: 120,
    });
    expect(result.success).toBe(false);
  });

  it("requires value", () => {
    const result = monitorSchema.safeParse({
      caseId: 1,
      metric: "BP",
      value: "not-a-number",
    });
    expect(result.success).toBe(false);
  });

  it("coerces string value to number", () => {
    const result = monitorSchema.safeParse({
      caseId: 1,
      metric: "BP",
      value: "120",
    });
    expect(result.success).toBe(true);
    if (result.success) {
      expect(result.data.value).toBe(120);
    }
  });
});

describe("deviceSchema", () => {
  it("validates a valid device", () => {
    const result = deviceSchema.safeParse({
      deviceName: "BP Monitor A",
      deviceType: "BP_MONITOR",
      serialNo: "BP001",
    });
    expect(result.success).toBe(true);
  });

  it("requires deviceName", () => {
    const result = deviceSchema.safeParse({
      deviceName: "",
      deviceType: "BP_MONITOR",
      serialNo: "BP001",
    });
    expect(result.success).toBe(false);
  });

  it("requires serialNo", () => {
    const result = deviceSchema.safeParse({
      deviceName: "BP Monitor",
      deviceType: "BP_MONITOR",
      serialNo: "",
    });
    expect(result.success).toBe(false);
  });

  it("defaults deviceType to OTHER", () => {
    const result = deviceSchema.safeParse({
      deviceName: "Test Device",
      serialNo: "TEST001",
    });
    expect(result.success).toBe(true);
    if (result.success) {
      expect(result.data.deviceType).toBe("OTHER");
    }
  });

  it("defaults status to ACTIVE", () => {
    const result = deviceSchema.safeParse({
      deviceName: "Test Device",
      serialNo: "TEST001",
    });
    expect(result.success).toBe(true);
    if (result.success) {
      expect(result.data.status).toBe("ACTIVE");
    }
  });

  it("rejects invalid deviceType", () => {
    const result = deviceSchema.safeParse({
      deviceName: "Test",
      deviceType: "INVALID",
      serialNo: "TEST001",
    });
    expect(result.success).toBe(false);
  });
});

describe("caseFilterSchema", () => {
  it("validates empty filter", () => {
    const result = caseFilterSchema.safeParse({});
    expect(result.success).toBe(true);
  });

  it("validates filter with status", () => {
    const result = caseFilterSchema.safeParse({ status: "1" });
    expect(result.success).toBe(true);
  });

  it("rejects invalid status", () => {
    const result = caseFilterSchema.safeParse({ status: "5" });
    expect(result.success).toBe(false);
  });

  it("coerces page and size", () => {
    const result = caseFilterSchema.safeParse({
      page: "2",
      size: "50",
    });
    expect(result.success).toBe(true);
    if (result.success) {
      expect(result.data.page).toBe(2);
      expect(result.data.size).toBe(50);
    }
  });
});
