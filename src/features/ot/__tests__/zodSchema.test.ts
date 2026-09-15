import { describe, expect, it } from "vitest";
import {
  otScheduleSchema,
  anesthesiaPreOpSchema,
  vitalSignSchema,
  anesthesiaDrugSchema,
  anesthesiaPostOpSchema,
  checklistCreateSchema,
  checklistItemSchema,
  checklistCompletionSchema,
  otRoomSchema,
} from "../zodSchema";

describe("otScheduleSchema", () => {
  it("validates a complete schedule with encounterId", () => {
    const result = otScheduleSchema.safeParse({
      patientId: 100,
      encounterId: 200,
      otTable: "OT-1",
      scheduledStart: "2026-09-11T08:00",
      scheduledEnd: "2026-09-11T10:00",
      surgeonId: 5,
      anesthetistId: 6,
      preBilling: true,
      procedures: [{ procedureCode: "APPEND", procedureName: "Appendectomy", rate: 5000 }],
    });
    expect(result.success).toBe(true);
  });

  it("validates a schedule with admissionId instead of encounterId", () => {
    const result = otScheduleSchema.safeParse({
      patientId: 100,
      admissionId: 300,
      otTable: "OT-2",
      scheduledStart: "2026-09-11T08:00",
      scheduledEnd: "2026-09-11T10:00",
    });
    expect(result.success).toBe(true);
  });

  it("rejects when neither encounterId nor admissionId is provided (BR-OT-001)", () => {
    const result = otScheduleSchema.safeParse({
      patientId: 100,
      otTable: "OT-1",
      scheduledStart: "2026-09-11T08:00",
      scheduledEnd: "2026-09-11T10:00",
    });
    expect(result.success).toBe(false);
  });

  it("rejects negative patientId", () => {
    const result = otScheduleSchema.safeParse({
      patientId: -1,
      encounterId: 200,
      otTable: "OT-1",
      scheduledStart: "2026-09-11T08:00",
      scheduledEnd: "2026-09-11T10:00",
    });
    expect(result.success).toBe(false);
  });

  it("rejects empty otTable", () => {
    const result = otScheduleSchema.safeParse({
      patientId: 100,
      encounterId: 200,
      otTable: "",
      scheduledStart: "2026-09-11T08:00",
      scheduledEnd: "2026-09-11T10:00",
    });
    expect(result.success).toBe(false);
  });

  it("accepts empty procedures array", () => {
    const result = otScheduleSchema.safeParse({
      patientId: 100,
      encounterId: 200,
      otTable: "OT-1",
      scheduledStart: "2026-09-11T08:00",
      scheduledEnd: "2026-09-11T10:00",
      procedures: [],
    });
    expect(result.success).toBe(true);
  });
});

describe("anesthesiaPreOpSchema", () => {
  it("validates correct pre-op data", () => {
    const result = anesthesiaPreOpSchema.safeParse({
      scheduleId: 1,
      patientId: 100,
      asaGrade: "ASA_II",
      airway: "Mallampati I",
      comorbidities: "Hypertension",
    });
    expect(result.success).toBe(true);
  });

  it("rejects missing asaGrade", () => {
    const result = anesthesiaPreOpSchema.safeParse({
      scheduleId: 1,
      patientId: 100,
      airway: "Mallampati I",
    });
    expect(result.success).toBe(false);
  });

  it("accepts without comorbidities (optional)", () => {
    const result = anesthesiaPreOpSchema.safeParse({
      scheduleId: 1,
      patientId: 100,
      asaGrade: "ASA_I",
      airway: "Normal",
    });
    expect(result.success).toBe(true);
  });
});

describe("vitalSignSchema", () => {
  it("validates correct vital signs", () => {
    const result = vitalSignSchema.safeParse({
      recordedAt: "2026-09-11T08:30",
      hr: 72,
      bpSys: 120,
      bpDia: 80,
      spo2: 98,
      rr: 16,
      temp: 36.5,
    });
    expect(result.success).toBe(true);
  });

  it("rejects hr out of range", () => {
    const result = vitalSignSchema.safeParse({
      recordedAt: "2026-09-11T08:30",
      hr: 400,
    });
    expect(result.success).toBe(false);
  });

  it("rejects spo2 > 100", () => {
    const result = vitalSignSchema.safeParse({
      recordedAt: "2026-09-11T08:30",
      spo2: 110,
    });
    expect(result.success).toBe(false);
  });

  it("accepts partial vitals", () => {
    const result = vitalSignSchema.safeParse({
      recordedAt: "2026-09-11T08:30",
      hr: 72,
    });
    expect(result.success).toBe(true);
  });
});

describe("anesthesiaDrugSchema", () => {
  it("validates correct drug data", () => {
    const result = anesthesiaDrugSchema.safeParse({
      drugName: "Propofol",
      dose: "200mg",
      route: "IV",
      administeredAt: "2026-09-11T08:05",
    });
    expect(result.success).toBe(true);
  });

  it("rejects missing drugName", () => {
    const result = anesthesiaDrugSchema.safeParse({
      dose: "200mg",
      route: "IV",
      administeredAt: "2026-09-11T08:05",
    });
    expect(result.success).toBe(false);
  });
});

describe("anesthesiaPostOpSchema", () => {
  it("validates correct post-op data", () => {
    const result = anesthesiaPostOpSchema.safeParse({
      pacuMinutes: 45,
      recoveryScore: 8,
      complications: "None",
    });
    expect(result.success).toBe(true);
  });

  it("rejects recovery score > 10", () => {
    const result = anesthesiaPostOpSchema.safeParse({
      recoveryScore: 15,
    });
    expect(result.success).toBe(false);
  });
});

describe("checklistCreateSchema", () => {
  it("validates SIGN_IN phase", () => {
    const result = checklistCreateSchema.safeParse({
      scheduleId: 1,
      patientId: 100,
      phase: "SIGN_IN",
    });
    expect(result.success).toBe(true);
  });

  it("validates TIME_OUT phase", () => {
    const result = checklistCreateSchema.safeParse({
      scheduleId: 1,
      patientId: 100,
      phase: "TIME_OUT",
    });
    expect(result.success).toBe(true);
  });

  it("validates SIGN_OUT phase", () => {
    const result = checklistCreateSchema.safeParse({
      scheduleId: 1,
      patientId: 100,
      phase: "SIGN_OUT",
    });
    expect(result.success).toBe(true);
  });

  it("rejects invalid phase", () => {
    const result = checklistCreateSchema.safeParse({
      scheduleId: 1,
      patientId: 100,
      phase: "INVALID",
    });
    expect(result.success).toBe(false);
  });
});

describe("checklistItemSchema", () => {
  it("validates correct item", () => {
    const result = checklistItemSchema.safeParse({
      itemName: "Patient confirmed identity",
      category: "identity",
    });
    expect(result.success).toBe(true);
  });

  it("rejects empty itemName", () => {
    const result = checklistItemSchema.safeParse({
      itemName: "",
      category: "identity",
    });
    expect(result.success).toBe(false);
  });
});

describe("checklistCompletionSchema", () => {
  it("validates COMPLETED status", () => {
    const result = checklistCompletionSchema.safeParse({ overallStatus: "COMPLETED" });
    expect(result.success).toBe(true);
  });

  it("validates ABORTED status", () => {
    const result = checklistCompletionSchema.safeParse({ overallStatus: "ABORTED" });
    expect(result.success).toBe(true);
  });

  it("rejects invalid status", () => {
    const result = checklistCompletionSchema.safeParse({ overallStatus: "INVALID" });
    expect(result.success).toBe(false);
  });
});

describe("otRoomSchema", () => {
  it("validates correct room data", () => {
    const result = otRoomSchema.safeParse({ name: "OT-1", type: "General" });
    expect(result.success).toBe(true);
  });

  it("rejects empty name", () => {
    const result = otRoomSchema.safeParse({ name: "", type: "General" });
    expect(result.success).toBe(false);
  });
});
