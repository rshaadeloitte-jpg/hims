import { describe, expect, it } from "vitest";
import {
  clinicalOrderSchema,
  orderSetSchema,
  orderSetItemSchema,
  applyOrderSetSchema,
  cwfDefinitionSchema,
  cwfStepSchema,
  scwfDefinitionSchema,
  workflowStepStatusSchema,
  externalOrderSchema,
  orderStatusUpdateSchema,
} from "./zodSchema";

describe("clinicalOrderSchema", () => {
  it("accepts valid order", () => {
    const result = clinicalOrderSchema.safeParse({
      patientId: 1,
      orderType: "LAB",
      description: "Fasting glucose",
      priority: "ROUTINE",
    });
    expect(result.success).toBe(true);
  });

  it("rejects missing patientId", () => {
    const result = clinicalOrderSchema.safeParse({
      patientId: 0,
      orderType: "LAB",
      description: "Test",
      priority: "ROUTINE",
    });
    expect(result.success).toBe(false);
  });

  it("rejects invalid priority", () => {
    const result = clinicalOrderSchema.safeParse({
      patientId: 1,
      orderType: "LAB",
      description: "Test",
      priority: "HIGH",
    });
    expect(result.success).toBe(false);
  });

  it("coerces string patientId", () => {
    const result = clinicalOrderSchema.safeParse({
      patientId: "42",
      orderType: "LAB",
      description: "Test",
      priority: "URGENT",
    });
    expect(result.success).toBe(true);
    if (result.success) {
      expect(result.data.patientId).toBe(42);
    }
  });
});

describe("orderSetItemSchema", () => {
  it("accepts valid item", () => {
    const result = orderSetItemSchema.safeParse({
      id: "item-1",
      serviceType: "LAB",
      serviceId: 100,
      serviceName: "Glucose",
      qty: 1,
      priority: "ROUTINE",
    });
    expect(result.success).toBe(true);
  });

  it("defaults qty and priority", () => {
    const result = orderSetItemSchema.safeParse({
      id: "item-2",
      serviceType: "CONSUMABLES",
      serviceId: 5,
      serviceName: "Syringe",
    });
    expect(result.success).toBe(true);
    if (result.success) {
      expect(result.data.qty).toBe(1);
      expect(result.data.priority).toBe("ROUTINE");
    }
  });

  it("rejects invalid service type", () => {
    const result = orderSetItemSchema.safeParse({
      id: "item-3",
      serviceType: "INVALID",
      serviceId: 1,
      serviceName: "X",
    });
    expect(result.success).toBe(false);
  });
});

describe("orderSetSchema", () => {
  it("accepts valid order set", () => {
    const result = orderSetSchema.safeParse({
      name: "Diabetes Panel",
      code: "DM-PANEL",
      category: "LAB",
      description: "Standard diabetes workup",
      items: [
        { id: "i1", serviceType: "LAB", serviceId: 100, serviceName: "FBS", qty: 1, priority: "ROUTINE" },
        { id: "i2", serviceType: "LAB", serviceId: 101, serviceName: "HbA1c", qty: 1, priority: "ROUTINE" },
      ],
      status: "ACTIVE",
    });
    expect(result.success).toBe(true);
  });

  it("rejects empty items array", () => {
    const result = orderSetSchema.safeParse({
      name: "Empty",
      code: "EMPTY",
      category: "LAB",
      items: [],
      status: "ACTIVE",
    });
    expect(result.success).toBe(false);
  });

  it("requires name and code", () => {
    const result = orderSetSchema.safeParse({
      name: "",
      code: "",
      category: "LAB",
      items: [
        { id: "i1", serviceType: "LAB", serviceId: 1, serviceName: "X", qty: 1 },
      ],
    });
    expect(result.success).toBe(false);
    if (!result.success) {
      const paths = result.error.issues.map((i) => i.path.join("."));
      expect(paths).toContain("name");
      expect(paths).toContain("code");
    }
  });
});

describe("applyOrderSetSchema", () => {
  it("accepts valid apply payload", () => {
    const result = applyOrderSetSchema.safeParse({
      orderSetId: 1,
      patientId: 100,
      encounterId: 50,
    });
    expect(result.success).toBe(true);
  });

  it("coerces strings to numbers", () => {
    const result = applyOrderSetSchema.safeParse({
      orderSetId: "1",
      patientId: "100",
    });
    expect(result.success).toBe(true);
    if (result.success) {
      expect(result.data.orderSetId).toBe(1);
      expect(result.data.patientId).toBe(100);
    }
  });
});

describe("cwfStepSchema", () => {
  it("accepts valid step", () => {
    const result = cwfStepSchema.safeParse({
      stepId: 1,
      stepName: "Doctor Review",
      stepOrder: 0,
      role: "DOCTOR",
      isMandatory: true,
    });
    expect(result.success).toBe(true);
  });

  it("rejects invalid role", () => {
    const result = cwfStepSchema.safeParse({
      stepId: 1,
      stepName: "Bad Step",
      stepOrder: 0,
      role: "INVALID_ROLE",
    });
    expect(result.success).toBe(false);
  });
});

describe("cwfDefinitionSchema", () => {
  it("accepts valid CWF definition", () => {
    const result = cwfDefinitionSchema.safeParse({
      cwfId: 1,
      cwfName: "Lab Approval",
      serviceType: "LAB",
      steps: [
        { stepId: 1, stepName: "Collect Sample", stepOrder: 0, role: "NURSE", isMandatory: true },
        { stepId: 2, stepName: "Review Result", stepOrder: 1, role: "DOCTOR", isMandatory: true },
      ],
      status: "ACTIVE",
    });
    expect(result.success).toBe(true);
  });

  it("rejects empty steps", () => {
    const result = cwfDefinitionSchema.safeParse({
      cwfId: 1,
      cwfName: "Empty Workflow",
      serviceType: "LAB",
      steps: [],
    });
    expect(result.success).toBe(false);
  });
});

describe("scwfDefinitionSchema", () => {
  it("accepts valid SCWF with condition", () => {
    const result = scwfDefinitionSchema.safeParse({
      scwfId: 1,
      scwfName: "Diabetes Auto-Referral",
      serviceType: "CONSULT",
      condition: "diagnosis = 'diabetes' AND hba1c > 9",
      steps: [
        { stepId: 1, stepName: "Endocrinologist Review", stepOrder: 0, role: "DOCTOR" },
      ],
      status: "ACTIVE",
    });
    expect(result.success).toBe(true);
  });

  it("requires condition", () => {
    const result = scwfDefinitionSchema.safeParse({
      scwfId: 1,
      scwfName: "Bad SCWF",
      serviceType: "LAB",
      condition: "",
      steps: [{ stepId: 1, stepName: "X", stepOrder: 0, role: "DOCTOR" }],
    });
    expect(result.success).toBe(false);
  });
});

describe("workflowStepStatusSchema", () => {
  it("accepts valid step status", () => {
    const result = workflowStepStatusSchema.safeParse({
      stepId: 1,
      stepName: "Review",
      stepOrder: 0,
      role: "DOCTOR",
      status: "COMPLETED",
      assignedTo: "Dr. Smith",
      completedAt: "2024-01-01T10:00:00Z",
      completedBy: "Dr. Smith",
      remarks: "Approved",
    });
    expect(result.success).toBe(true);
  });

  it("rejects invalid status", () => {
    const result = workflowStepStatusSchema.safeParse({
      stepId: 1,
      stepName: "X",
      stepOrder: 0,
      role: "DOCTOR",
      status: "DONE",
    });
    expect(result.success).toBe(false);
  });
});

describe("externalOrderSchema", () => {
  it("accepts valid external order", () => {
    const result = externalOrderSchema.safeParse({
      orderId: 1,
      externalLabName: "SRL Diagnostics",
      externalLabCode: "SRL-001",
      testName: "Lipid Profile",
      testCode: "LP",
      sampleType: "Serum",
      instructions: "Fasting 12 hours",
      status: "PENDING",
    });
    expect(result.success).toBe(true);
  });

  it("defaults status to PENDING", () => {
    const result = externalOrderSchema.safeParse({
      orderId: 1,
      externalLabName: "LabCorp",
      testName: "CBC",
    });
    expect(result.success).toBe(true);
    if (result.success) {
      expect(result.data.status).toBe("PENDING");
    }
  });
});

describe("orderStatusUpdateSchema", () => {
  it("accepts valid update", () => {
    const result = orderStatusUpdateSchema.safeParse({
      status: "COMPLETED",
      remarks: "Results verified",
    });
    expect(result.success).toBe(true);
  });

  it("rejects invalid status", () => {
    const result = orderStatusUpdateSchema.safeParse({
      status: "UNKNOWN",
    });
    expect(result.success).toBe(false);
  });
});
