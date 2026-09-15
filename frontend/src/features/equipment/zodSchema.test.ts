import { describe, expect, it } from "vitest";
import {
  equipmentSchema,
  equipmentUpdateSchema,
  equipmentTransferSchema,
  equipmentComplaintSchema,
  complaintResolutionWithDetailsSchema,
  pmScheduleSchema,
  pmCompletionSchema,
  pmChecklistSchema,
  complaintStatusMasterSchema,
  maintenanceTypeMasterSchema,
  complaintFilterSchema,
  pmScheduleFilterSchema,
  equipmentFilterSchema,
} from "./zodSchema";

describe("M20 Equipment Zod schemas", () => {
  // ---- Equipment (asset register) -----------------------------------------
  describe("equipmentSchema", () => {
    it("accepts a valid equipment registration", () => {
      const result = equipmentSchema.safeParse({
        itemId: 1,
        accessionNo: "ACC-001",
        serialNo: "SN-123",
        model: "Model X",
        make: "Acme Corp",
        workingStatus: 1,
      });
      expect(result.success).toBe(true);
    });

    it("rejects missing required itemId", () => {
      const result = equipmentSchema.safeParse({
        itemId: 0,
        accessionNo: "ACC-001",
      });
      expect(result.success).toBe(false);
    });

    it("rejects missing required accessionNo", () => {
      const result = equipmentSchema.safeParse({
        itemId: 1,
        accessionNo: "",
      });
      expect(result.success).toBe(false);
    });

    it("coerces string itemId to number", () => {
      const result = equipmentSchema.safeParse({
        itemId: "42",
        accessionNo: "ACC-001",
      });
      expect(result.success).toBe(true);
      if (result.success) {
        expect(result.data.itemId).toBe(42);
      }
    });

    it("defaults workingStatus to 1 (WORKING)", () => {
      const result = equipmentSchema.safeParse({
        itemId: 1,
        accessionNo: "ACC-001",
      });
      expect(result.success).toBe(true);
      if (result.success) {
        expect(result.data.workingStatus).toBe(1);
      }
    });

    it("rejects workingStatus outside 1..4", () => {
      const result = equipmentSchema.safeParse({
        itemId: 1,
        accessionNo: "ACC-001",
        workingStatus: 5,
      });
      expect(result.success).toBe(false);
    });

    it("treats empty optional strings as undefined", () => {
      const result = equipmentSchema.safeParse({
        itemId: 1,
        accessionNo: "ACC-001",
        serialNo: "",
        model: "",
      });
      expect(result.success).toBe(true);
      if (result.success) {
        expect(result.data.serialNo).toBeUndefined();
        expect(result.data.model).toBeUndefined();
      }
    });
  });

  describe("equipmentUpdateSchema", () => {
    it("allows partial updates (all fields optional)", () => {
      const result = equipmentUpdateSchema.safeParse({
        model: "Updated Model",
      });
      expect(result.success).toBe(true);
    });

    it("allows empty object (no-op update)", () => {
      const result = equipmentUpdateSchema.safeParse({});
      expect(result.success).toBe(true);
    });
  });

  describe("equipmentTransferSchema", () => {
    it("accepts a valid transfer", () => {
      const result = equipmentTransferSchema.safeParse({
        toDepartmentId: 5,
        reason: "Relocating to ICU",
      });
      expect(result.success).toBe(true);
    });

    it("rejects missing toDepartmentId", () => {
      const result = equipmentTransferSchema.safeParse({
        toDepartmentId: 0,
      });
      expect(result.success).toBe(false);
    });
  });

  // ---- Complaints ----------------------------------------------------------
  describe("equipmentComplaintSchema", () => {
    it("accepts a valid complaint", () => {
      const result = equipmentComplaintSchema.safeParse({
        equipmentId: 10,
        description: "Machine making loud noise",
        roomNo: "ICU-1",
      });
      expect(result.success).toBe(true);
    });

    it("rejects missing equipmentId", () => {
      const result = equipmentComplaintSchema.safeParse({
        equipmentId: 0,
        description: "Broken",
      });
      expect(result.success).toBe(false);
    });

    it("rejects empty description", () => {
      const result = equipmentComplaintSchema.safeParse({
        equipmentId: 10,
        description: "",
      });
      expect(result.success).toBe(false);
    });
  });

  describe("complaintResolutionWithDetailsSchema", () => {
    it("accepts a valid resolution", () => {
      const result = complaintResolutionWithDetailsSchema.safeParse({
        resolutionNotes: "Replaced bearing",
        partsReplaced: "Bearing SKF-6205",
        resolvedStatus: 3,
      });
      expect(result.success).toBe(true);
    });

    it("rejects empty resolutionNotes", () => {
      const result = complaintResolutionWithDetailsSchema.safeParse({
        resolutionNotes: "",
      });
      expect(result.success).toBe(false);
    });

    it("accepts resolution with detail lines", () => {
      const result = complaintResolutionWithDetailsSchema.safeParse({
        resolutionNotes: "Fixed",
        details: [
          { itemId: 100, quantity: 2, cost: 500, totalAmount: 1000 },
        ],
      });
      expect(result.success).toBe(true);
    });

    it("defaults resolvedStatus to 3 (RESOLVED)", () => {
      const result = complaintResolutionWithDetailsSchema.safeParse({
        resolutionNotes: "Done",
      });
      expect(result.success).toBe(true);
      if (result.success) {
        expect(result.data.resolvedStatus).toBe(3);
      }
    });
  });

  // ---- PM schedules --------------------------------------------------------
  describe("pmScheduleSchema", () => {
    it("accepts a valid PM schedule", () => {
      const result = pmScheduleSchema.safeParse({
        equipmentId: 10,
        frequency: 3,
        pmDate: "2026-01-15",
        jan: true,
        apr: true,
        jul: true,
        oct: true,
        pmType: 1,
      });
      expect(result.success).toBe(true);
    });

    it("rejects missing equipmentId", () => {
      const result = pmScheduleSchema.safeParse({
        equipmentId: 0,
        frequency: 3,
      });
      expect(result.success).toBe(false);
    });

    it("rejects non-positive frequency", () => {
      const result = pmScheduleSchema.safeParse({
        equipmentId: 10,
        frequency: 0,
      });
      expect(result.success).toBe(false);
    });

    it("defaults pmType to 1 (PREVENTIVE)", () => {
      const result = pmScheduleSchema.safeParse({
        equipmentId: 10,
        frequency: 6,
      });
      expect(result.success).toBe(true);
      if (result.success) {
        expect(result.data.pmType).toBe(1);
      }
    });

    it("rejects pmType outside 1..3", () => {
      const result = pmScheduleSchema.safeParse({
        equipmentId: 10,
        frequency: 6,
        pmType: 5,
      });
      expect(result.success).toBe(false);
    });
  });

  describe("pmCompletionSchema", () => {
    it("accepts a valid completion record", () => {
      const result = pmCompletionSchema.safeParse({
        maintenanceNo: "PM-2026-00001",
        maintenanceDate: "2026-03-15",
        performedBy: 5,
        functionalCheckRemark: "All OK",
        preventiveMaintenanceRemark: "Lubricated",
        electricalSafetyRemark: "Ground OK",
        calibration: "Calibrated to spec",
        amountIncurred: 2500,
      });
      expect(result.success).toBe(true);
    });

    it("allows empty object (server fills defaults)", () => {
      const result = pmCompletionSchema.safeParse({});
      expect(result.success).toBe(true);
    });

    it("rejects negative amount", () => {
      const result = pmCompletionSchema.safeParse({
        amountIncurred: -100,
      });
      expect(result.success).toBe(false);
    });
  });

  describe("pmChecklistSchema", () => {
    it("accepts a valid checklist template", () => {
      const result = pmChecklistSchema.safeParse({
        code: "CHK-001",
        description: "Check oil level",
      });
      expect(result.success).toBe(true);
    });

    it("rejects empty code", () => {
      const result = pmChecklistSchema.safeParse({
        code: "",
        description: "Check oil level",
      });
      expect(result.success).toBe(false);
    });
  });

  // ---- Master dictionaries -------------------------------------------------
  describe("complaintStatusMasterSchema", () => {
    it("accepts a valid status master", () => {
      const result = complaintStatusMasterSchema.safeParse({
        code: "OPEN",
        description: "Complaint is open",
        isResolved: false,
        isClosed: false,
        isReopened: false,
      });
      expect(result.success).toBe(true);
    });
  });

  describe("maintenanceTypeMasterSchema", () => {
    it("accepts a valid maintenance type", () => {
      const result = maintenanceTypeMasterSchema.safeParse({
        code: "PREVENTIVE",
        description: "Preventive maintenance",
      });
      expect(result.success).toBe(true);
    });
  });

  // ---- Search filters ------------------------------------------------------
  describe("complaintFilterSchema", () => {
    it("accepts empty filter", () => {
      const result = complaintFilterSchema.safeParse({});
      expect(result.success).toBe(true);
    });

    it("accepts populated filter", () => {
      const result = complaintFilterSchema.safeParse({
        equipmentId: 10,
        complaintStatus: 1,
        search: "noise",
      });
      expect(result.success).toBe(true);
    });
  });

  describe("pmScheduleFilterSchema", () => {
    it("accepts empty filter", () => {
      const result = pmScheduleFilterSchema.safeParse({});
      expect(result.success).toBe(true);
    });

    it("accepts populated filter", () => {
      const result = pmScheduleFilterSchema.safeParse({
        equipmentId: 10,
        pmType: 1,
        dueFrom: "2026-01-01",
        dueTo: "2026-12-31",
      });
      expect(result.success).toBe(true);
    });
  });

  describe("equipmentFilterSchema", () => {
    it("accepts empty filter", () => {
      const result = equipmentFilterSchema.safeParse({});
      expect(result.success).toBe(true);
    });

    it("accepts populated filter", () => {
      const result = equipmentFilterSchema.safeParse({
        workingStatus: 1,
        search: "MRI",
      });
      expect(result.success).toBe(true);
    });
  });
});
