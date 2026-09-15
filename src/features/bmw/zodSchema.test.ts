import { describe, expect, it } from "vitest";
import {
  wasteRequestStatusSchema,
  STATUS_ID,
  STATUS_TONE,
  STATUS_LABEL,
  ALLOWED_NEXT_STATUS,
  wasteRequestSchema,
  wasteBagLineSchema,
  wasteSummarySchema,
  wasteListFilterSchema,
  handoverFormSchema,
  decomposeFormSchema,
  bagStatusUpdateSchema,
} from "./zodSchema";

describe("BMW zod schemas", () => {
  describe("wasteRequestStatusSchema", () => {
    it.each([
      ["PENDING", true],
      ["ACCEPTED", true],
      ["HANDOVERED", true],
      ["RECEIVED", true],
      ["COMPLETED", true],
      ["REJECTED", false],
      ["DRAFT", false],
    ])("validates %s as %s", (status, expected) => {
      const result = wasteRequestStatusSchema.safeParse(status);
      expect(result.success).toBe(expected);
    });
  });

  describe("STATUS_ID", () => {
    it("maps each status to its legacy numeric id", () => {
      expect(STATUS_ID.PENDING).toBe(1);
      expect(STATUS_ID.ACCEPTED).toBe(2);
      expect(STATUS_ID.HANDOVERED).toBe(3);
      expect(STATUS_ID.RECEIVED).toBe(4);
      expect(STATUS_ID.COMPLETED).toBe(5);
    });
  });

  describe("STATUS_TONE", () => {
    it("maps PENDING to warning, ACCEPTED to info, COMPLETED to success", () => {
      expect(STATUS_TONE.PENDING).toBe("badge--warning");
      expect(STATUS_TONE.ACCEPTED).toBe("badge--info");
      expect(STATUS_TONE.HANDOVERED).toBe("badge--info");
      expect(STATUS_TONE.RECEIVED).toBe("badge--success");
      expect(STATUS_TONE.COMPLETED).toBe("badge--success");
    });
  });

  describe("STATUS_LABEL", () => {
    it("maps statuses to human-readable labels", () => {
      expect(STATUS_LABEL.PENDING).toBe("Pending");
      expect(STATUS_LABEL.COMPLETED).toBe("Completed");
    });
  });

  describe("ALLOWED_NEXT_STATUS", () => {
    it("PENDING can transition to ACCEPTED or HANDOVERED", () => {
      expect(ALLOWED_NEXT_STATUS.PENDING).toContain("ACCEPTED");
      expect(ALLOWED_NEXT_STATUS.PENDING).toContain("HANDOVERED");
    });
    it("COMPLETED has no next statuses", () => {
      expect(ALLOWED_NEXT_STATUS.COMPLETED).toEqual([]);
    });
    it("the lifecycle is forward-only (no backward transitions)", () => {
      for (const status of wasteRequestStatusSchema.options) {
        const next = ALLOWED_NEXT_STATUS[status];
        for (const n of next) {
          expect(STATUS_ID[n]).toBeGreaterThan(STATUS_ID[status]);
        }
      }
    });
  });

  describe("wasteBagLineSchema", () => {
    it("validates a correct bag line", () => {
      const result = wasteBagLineSchema.safeParse({
        bagMasterId: 1,
        qty: 2,
        weight: 5.5,
        remark: "Test",
      });
      expect(result.success).toBe(true);
    });

    it("rejects zero quantity", () => {
      const result = wasteBagLineSchema.safeParse({
        bagMasterId: 1,
        qty: 0,
        weight: 1,
      });
      expect(result.success).toBe(false);
    });

    it("rejects negative weight", () => {
      const result = wasteBagLineSchema.safeParse({
        bagMasterId: 1,
        qty: 1,
        weight: -1,
      });
      expect(result.success).toBe(false);
    });

    it("defaults qty to 1", () => {
      const result = wasteBagLineSchema.safeParse({
        bagMasterId: 1,
        weight: 1,
      });
      expect(result.success).toBe(true);
      if (result.success) {
        expect(result.data.qty).toBe(1);
      }
    });

    it("coerces string numbers", () => {
      const result = wasteBagLineSchema.safeParse({
        bagMasterId: "5",
        qty: "3",
        weight: "2.5",
      });
      expect(result.success).toBe(true);
      if (result.success) {
        expect(result.data.bagMasterId).toBe(5);
        expect(result.data.qty).toBe(3);
        expect(result.data.weight).toBe(2.5);
      }
    });
  });

  describe("wasteRequestSchema", () => {
    it("validates a correct request with bag details", () => {
      const result = wasteRequestSchema.safeParse({
        unitId: 1,
        storeId: 10,
        toStoreId: 20,
        date: "2026-09-11",
        details: [
          { bagMasterId: 1, qty: 1, weight: 2.5 },
          { bagMasterId: 2, qty: 3, weight: 1.0 },
        ],
      });
      expect(result.success).toBe(true);
    });

    it("requires at least one bag detail", () => {
      const result = wasteRequestSchema.safeParse({
        unitId: 1,
        storeId: 10,
        date: "2026-09-11",
        details: [],
      });
      expect(result.success).toBe(false);
    });

    it("requires storeId", () => {
      const result = wasteRequestSchema.safeParse({
        unitId: 1,
        date: "2026-09-11",
        details: [{ bagMasterId: 1, qty: 1, weight: 1 }],
      });
      expect(result.success).toBe(false);
    });

    it("requires date", () => {
      const result = wasteRequestSchema.safeParse({
        unitId: 1,
        storeId: 10,
        details: [{ bagMasterId: 1, qty: 1, weight: 1 }],
      });
      expect(result.success).toBe(false);
    });
  });

  describe("wasteSummarySchema", () => {
    it("validates a correct summary", () => {
      const result = wasteSummarySchema.safeParse({
        totalCount: 100,
        pendingCount: 10,
        acceptedCount: 20,
        handOveredCount: 30,
        receivedCount: 25,
        completedCount: 15,
      });
      expect(result.success).toBe(true);
    });
  });

  describe("wasteListFilterSchema", () => {
    it("applies defaults for page and size", () => {
      const result = wasteListFilterSchema.safeParse({});
      expect(result.success).toBe(true);
      if (result.success) {
        expect(result.data.page).toBe(0);
        expect(result.data.size).toBe(25);
      }
    });

    it("validates category enum", () => {
      const result = wasteListFilterSchema.safeParse({ category: "pending" });
      expect(result.success).toBe(true);
    });

    it("rejects invalid category", () => {
      const result = wasteListFilterSchema.safeParse({ category: "invalid" });
      expect(result.success).toBe(false);
    });
  });

  describe("handoverFormSchema", () => {
    it("validates a correct handover", () => {
      const result = handoverFormSchema.safeParse({ handedOverTo: "John Doe" });
      expect(result.success).toBe(true);
    });

    it("requires handedOverTo", () => {
      const result = handoverFormSchema.safeParse({ handedOverTo: "" });
      expect(result.success).toBe(false);
    });
  });

  describe("decomposeFormSchema", () => {
    it("validates a correct decompose form", () => {
      const result = decomposeFormSchema.safeParse({
        firmName: "EcoWaste Ltd",
        contactPerson: "Jane Smith",
        contactNo: "9876543210",
      });
      expect(result.success).toBe(true);
    });

    it("rejects empty firm name", () => {
      const result = decomposeFormSchema.safeParse({
        firmName: "",
        contactPerson: "Jane",
        contactNo: "9876543210",
      });
      expect(result.success).toBe(false);
    });

    it("rejects contact person with numbers", () => {
      const result = decomposeFormSchema.safeParse({
        firmName: "EcoWaste",
        contactPerson: "Jane123",
        contactNo: "9876543210",
      });
      expect(result.success).toBe(false);
    });

    it("rejects mobile not 10 digits", () => {
      const result = decomposeFormSchema.safeParse({
        firmName: "EcoWaste",
        contactPerson: "Jane",
        contactNo: "123",
      });
      expect(result.success).toBe(false);
    });

    it("rejects mobile with non-digits", () => {
      const result = decomposeFormSchema.safeParse({
        firmName: "EcoWaste",
        contactPerson: "Jane",
        contactNo: "abcdefghij",
      });
      expect(result.success).toBe(false);
    });
  });

  describe("bagStatusUpdateSchema", () => {
    it("validates a correct bag status update", () => {
      const result = bagStatusUpdateSchema.safeParse({
        bioMedicalWasteId: 1,
        id: 10,
        unitId: 1,
        bagStatus: 2,
        weight: 5.5,
        remark: "OK",
        handedOverTo: "John",
      });
      expect(result.success).toBe(true);
    });

    it("rejects bagStatus out of range", () => {
      const result = bagStatusUpdateSchema.safeParse({
        bioMedicalWasteId: 1,
        id: 10,
        unitId: 1,
        bagStatus: 6,
      });
      expect(result.success).toBe(false);
    });
  });
});
