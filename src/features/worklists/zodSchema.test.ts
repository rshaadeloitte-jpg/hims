import { describe, expect, it } from "vitest";
import {
  worklistFilterSchema,
  nursingTaskFormSchema,
  careGoalFormSchema,
  interventionFormSchema,
  statusBadgeClass,
  priorityClass,
  TASK_STATUS,
  TASK_PRIORITY,
  GOAL_STATUS,
  INTERVENTION_STATUS,
} from "./zodSchema";

describe("worklistFilterSchema", () => {
  it("accepts empty filter (all defaults)", () => {
    const result = worklistFilterSchema.parse({});
    expect(result.status).toBe("ALL");
    expect(result.priority).toBe("ALL");
  });

  it("accepts valid date range filter", () => {
    const result = worklistFilterSchema.parse({
      dateFrom: "2026-01-01",
      dateTo: "2026-01-31",
      status: "PENDING",
      priority: "HIGH",
    });
    expect(result.dateFrom).toBe("2026-01-01");
    expect(result.status).toBe("PENDING");
    expect(result.priority).toBe("HIGH");
  });

  it("rejects invalid status enum", () => {
    const result = worklistFilterSchema.safeParse({ status: "INVALID" });
    expect(result.success).toBe(false);
  });

  it("rejects invalid priority enum", () => {
    const result = worklistFilterSchema.safeParse({ priority: "SUPER_URGENT" });
    expect(result.success).toBe(false);
  });

  it("coerces numeric assignedTo", () => {
    const result = worklistFilterSchema.parse({ assignedTo: "42", wardId: "5" });
    expect(result.assignedTo).toBe(42);
    expect(result.wardId).toBe(5);
  });

  it("treats empty string assignedTo as undefined", () => {
    const result = worklistFilterSchema.parse({ assignedTo: "", wardId: "" });
    expect(result.assignedTo).toBeUndefined();
    expect(result.wardId).toBeUndefined();
  });
});

describe("nursingTaskFormSchema", () => {
  const validTask = {
    admissionId: 1,
    patientId: 100,
    taskType: "MEDICATION",
    description: "Administer IV antibiotics",
    priority: "HIGH" as const,
    dueTime: "2026-09-11T14:00:00",
    assignedTo: 5,
  };

  it("accepts a valid task form", () => {
    const result = nursingTaskFormSchema.parse(validTask);
    expect(result.taskType).toBe("MEDICATION");
    expect(result.priority).toBe("HIGH");
  });

  it("requires admissionId", () => {
    const result = nursingTaskFormSchema.safeParse({ ...validTask, admissionId: undefined });
    expect(result.success).toBe(false);
  });

  it("requires patientId", () => {
    const result = nursingTaskFormSchema.safeParse({ ...validTask, patientId: undefined });
    expect(result.success).toBe(false);
  });

  it("rejects empty taskType", () => {
    const result = nursingTaskFormSchema.safeParse({ ...validTask, taskType: "" });
    expect(result.success).toBe(false);
  });

  it("rejects empty description", () => {
    const result = nursingTaskFormSchema.safeParse({ ...validTask, description: "" });
    expect(result.success).toBe(false);
  });

  it("defaults priority to MEDIUM", () => {
    const { priority, ...rest } = validTask;
    const result = nursingTaskFormSchema.parse(rest);
    expect(result.priority).toBe("MEDIUM");
  });

  it("treats empty dueTime as undefined", () => {
    const result = nursingTaskFormSchema.parse({ ...validTask, dueTime: "" });
    expect(result.dueTime).toBeUndefined();
  });

  it("treats empty assignedTo as undefined", () => {
    const result = nursingTaskFormSchema.parse({ ...validTask, assignedTo: "" });
    expect(result.assignedTo).toBeUndefined();
  });
});

describe("careGoalFormSchema", () => {
  const validGoal = {
    carePlanId: 1,
    goalText: "Patient will maintain stable vitals for 24h",
    targetDate: "2026-09-15",
    status: "ACTIVE" as const,
  };

  it("accepts a valid goal form", () => {
    const result = careGoalFormSchema.parse(validGoal);
    expect(result.goalText).toContain("stable vitals");
    expect(result.status).toBe("ACTIVE");
  });

  it("requires carePlanId", () => {
    const result = careGoalFormSchema.safeParse({ ...validGoal, carePlanId: undefined });
    expect(result.success).toBe(false);
  });

  it("rejects empty goalText", () => {
    const result = careGoalFormSchema.safeParse({ ...validGoal, goalText: "" });
    expect(result.success).toBe(false);
  });

  it("defaults status to ACTIVE", () => {
    const { status, ...rest } = validGoal;
    const result = careGoalFormSchema.parse(rest);
    expect(result.status).toBe("ACTIVE");
  });

  it("treats empty targetDate as undefined", () => {
    const result = careGoalFormSchema.parse({ ...validGoal, targetDate: "" });
    expect(result.targetDate).toBeUndefined();
  });
});

describe("interventionFormSchema", () => {
  const validIntervention = {
    carePlanId: 1,
    interventionText: "Monitor vitals every 4 hours",
    scheduledTime: "2026-09-11T16:00:00",
    status: "SCHEDULED" as const,
    assignedTo: 5,
  };

  it("accepts a valid intervention form", () => {
    const result = interventionFormSchema.parse(validIntervention);
    expect(result.interventionText).toContain("Monitor vitals");
    expect(result.status).toBe("SCHEDULED");
  });

  it("requires carePlanId", () => {
    const result = interventionFormSchema.safeParse({ ...validIntervention, carePlanId: undefined });
    expect(result.success).toBe(false);
  });

  it("rejects empty interventionText", () => {
    const result = interventionFormSchema.safeParse({ ...validIntervention, interventionText: "" });
    expect(result.success).toBe(false);
  });

  it("defaults status to SCHEDULED", () => {
    const { status, ...rest } = validIntervention;
    const result = interventionFormSchema.parse(rest);
    expect(result.status).toBe("SCHEDULED");
  });

  it("treats empty scheduledTime as undefined", () => {
    const result = interventionFormSchema.parse({ ...validIntervention, scheduledTime: "" });
    expect(result.scheduledTime).toBeUndefined();
  });
});

describe("statusBadgeClass", () => {
  it("returns blue class for PENDING / SCHEDULED / ACTIVE", () => {
    expect(statusBadgeClass(TASK_STATUS.PENDING)).toContain("blue");
    expect(statusBadgeClass(INTERVENTION_STATUS.SCHEDULED)).toContain("blue");
    expect(statusBadgeClass(GOAL_STATUS.ACTIVE)).toContain("blue");
  });

  it("returns yellow class for IN_PROGRESS", () => {
    expect(statusBadgeClass(TASK_STATUS.IN_PROGRESS)).toContain("yellow");
    expect(statusBadgeClass(INTERVENTION_STATUS.IN_PROGRESS)).toContain("yellow");
  });

  it("returns green class for COMPLETED / ACHIEVED", () => {
    expect(statusBadgeClass(TASK_STATUS.COMPLETED)).toContain("green");
    expect(statusBadgeClass(GOAL_STATUS.ACHIEVED)).toContain("green");
    expect(statusBadgeClass(INTERVENTION_STATUS.COMPLETED)).toContain("green");
  });

  it("returns gray class for CANCELLED / DISCONTINUED / SKIPPED", () => {
    expect(statusBadgeClass(TASK_STATUS.CANCELLED)).toContain("gray");
    expect(statusBadgeClass(GOAL_STATUS.DISCONTINUED)).toContain("gray");
    expect(statusBadgeClass(INTERVENTION_STATUS.SKIPPED)).toContain("gray");
  });
});

describe("priorityClass", () => {
  it("returns red for URGENT", () => {
    expect(priorityClass(TASK_PRIORITY.URGENT)).toContain("red");
  });

  it("returns orange for HIGH", () => {
    expect(priorityClass(TASK_PRIORITY.HIGH)).toContain("orange");
  });

  it("returns blue for MEDIUM", () => {
    expect(priorityClass(TASK_PRIORITY.MEDIUM)).toContain("blue");
  });

  it("returns gray for LOW", () => {
    expect(priorityClass(TASK_PRIORITY.LOW)).toContain("gray");
  });
});
