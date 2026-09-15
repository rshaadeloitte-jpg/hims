import { describe, expect, it } from "vitest";
import {
  ashaWorkSchema,
  incentiveSchema,
  geographyMasterSchema,
  ashaWorkFilterSchema,
  incentiveFilterSchema,
  geographyFilterSchema,
  TRACKING_TYPE,
  VISIT_STATUS,
} from "./zodSchema";

describe("M36 EJS/ASHA Zod schemas", () => {
  // ---- ashaWorkSchema ----

  describe("ashaWorkSchema", () => {
    it("validates a complete ANC record", () => {
      const result = ashaWorkSchema.safeParse({
        beneficiaryId: 1,
        beneficiaryName: "Priya Sharma",
        trackingType: "ANC",
        visitDate: "2026-09-15",
        ashaId: 10,
        ashaName: "Sunita Devi",
        status: "SCHEDULED",
        gestationalWeek: 24,
        weight: 58.5,
        bloodPressureSystolic: 120,
        bloodPressureDiastolic: 80,
        hemoglobin: 11.2,
        immunizationGiven: "TT2",
        nextVisitDate: "2026-10-15",
      });
      expect(result.success).toBe(true);
    });

    it("validates a PNC record", () => {
      const result = ashaWorkSchema.safeParse({
        beneficiaryId: 2,
        beneficiaryName: "Anjali Kumari",
        trackingType: "PNC",
        visitDate: "2026-09-10",
        ashaId: 10,
        ashaName: "Sunita Devi",
        status: "COMPLETED",
        deliveryDate: "2026-09-01",
        deliveryType: "NORMAL",
        postnatalDay: 9,
      });
      expect(result.success).toBe(true);
    });

    it("validates a child-care record", () => {
      const result = ashaWorkSchema.safeParse({
        beneficiaryId: 3,
        beneficiaryName: "Baby Riya",
        trackingType: "CHILD",
        visitDate: "2026-09-12",
        ashaId: 11,
        ashaName: "Meena Yadav",
        status: "COMPLETED",
        childAgeMonths: 18,
        weightChild: 11.2,
        heightChild: 82,
        immunizationStatus: "UPTO_DATE",
        nutritionStatus: "NORMAL",
      });
      expect(result.success).toBe(true);
    });

    it("rejects missing required fields", () => {
      const result = ashaWorkSchema.safeParse({
        beneficiaryName: "",
        trackingType: "ANC",
        visitDate: "",
        ashaName: "",
      });
      expect(result.success).toBe(false);
      if (!result.success) {
        const paths = result.error.issues.map((i) => i.path.join("."));
        expect(paths).toContain("beneficiaryName");
        expect(paths).toContain("visitDate");
        expect(paths).toContain("ashaName");
      }
    });

    it("rejects invalid tracking type", () => {
      const result = ashaWorkSchema.safeParse({
        beneficiaryId: 1,
        beneficiaryName: "Test",
        trackingType: "INVALID",
        visitDate: "2026-09-15",
        ashaId: 10,
        ashaName: "ASHA",
      });
      expect(result.success).toBe(false);
    });

    it("applies default status SCHEDULED", () => {
      const parsed = ashaWorkSchema.parse({
        beneficiaryId: 1,
        beneficiaryName: "Test",
        trackingType: "ANC",
        visitDate: "2026-09-15",
        ashaId: 10,
        ashaName: "ASHA",
      });
      expect(parsed.status).toBe("SCHEDULED");
    });

    it("treats empty optional fields as undefined", () => {
      const parsed = ashaWorkSchema.parse({
        beneficiaryId: 1,
        beneficiaryName: "Test",
        trackingType: "ANC",
        visitDate: "2026-09-15",
        ashaId: 10,
        ashaName: "ASHA",
        remarks: "",
        nextVisitDate: "",
      });
      expect(parsed.remarks).toBeUndefined();
      expect(parsed.nextVisitDate).toBeUndefined();
    });
  });

  // ---- incentiveSchema ----

  describe("incentiveSchema", () => {
    it("validates a complete incentive record", () => {
      const result = incentiveSchema.safeParse({
        ashaId: 10,
        ashaName: "Sunita Devi",
        month: "2026-09",
        ancVisits: 12,
        pncVisits: 8,
        childCareVisits: 15,
        immunizationReferrals: 6,
        institutionalDeliveryReferrals: 4,
        ancIncentive: 1800,
        pncIncentive: 1200,
        childCareIncentive: 1500,
        immunizationIncentive: 600,
        deliveryIncentive: 2000,
        totalIncentive: 7100,
        status: "APPROVED",
        approvedBy: "Block Coordinator",
        approvedAt: "2026-09-20T10:00:00Z",
      });
      expect(result.success).toBe(true);
    });

    it("applies default zero values for amounts", () => {
      const parsed = incentiveSchema.parse({
        ashaId: 10,
        ashaName: "Sunita Devi",
        month: "2026-09",
      });
      expect(parsed.ancVisits).toBe(0);
      expect(parsed.totalIncentive).toBe(0);
      expect(parsed.status).toBe("PENDING");
    });

    it("rejects negative incentive amounts", () => {
      const result = incentiveSchema.safeParse({
        ashaId: 10,
        ashaName: "Sunita Devi",
        month: "2026-09",
        totalIncentive: -100,
      });
      expect(result.success).toBe(false);
    });

    it("rejects invalid status", () => {
      const result = incentiveSchema.safeParse({
        ashaId: 10,
        ashaName: "Sunita Devi",
        month: "2026-09",
        status: "INVALID",
      });
      expect(result.success).toBe(false);
    });
  });

  // ---- geographyMasterSchema ----

  describe("geographyMasterSchema", () => {
    it("validates a complete geography record", () => {
      const result = geographyMasterSchema.safeParse({
        ashaId: 10,
        ashaName: "Sunita Devi",
        state: "Rajasthan",
        district: "Jaipur",
        block: "Sanganer",
        village: "Pragpura",
        subCenter: "Pragpura SC",
        totalPopulation: 2450,
        targetCouples: 320,
        pregnantWomen: 45,
        childrenUnder5: 180,
        households: 420,
        eligibleCouples: 280,
        isActive: true,
        lastEnumerationDate: "2026-08-15",
        remarks: "Enumeration complete",
      });
      expect(result.success).toBe(true);
    });

    it("applies default zero values for population", () => {
      const parsed = geographyMasterSchema.parse({
        ashaId: 10,
        ashaName: "Sunita Devi",
        state: "Rajasthan",
        district: "Jaipur",
        block: "Sanganer",
        village: "Pragpura",
        subCenter: "Pragpura SC",
      });
      expect(parsed.totalPopulation).toBe(0);
      expect(parsed.isActive).toBe(true);
    });

    it("rejects missing required geography fields", () => {
      const result = geographyMasterSchema.safeParse({
        ashaId: 10,
        ashaName: "Sunita Devi",
        state: "",
        district: "",
        block: "",
        village: "",
        subCenter: "",
      });
      expect(result.success).toBe(false);
      if (!result.success) {
        const paths = result.error.issues.map((i) => i.path.join("."));
        expect(paths).toContain("state");
        expect(paths).toContain("district");
        expect(paths).toContain("village");
      }
    });
  });

  // ---- Filter schemas ----

  describe("ashaWorkFilterSchema", () => {
    it("accepts empty filters with defaults", () => {
      const parsed = ashaWorkFilterSchema.parse({});
      expect(parsed.page).toBe(0);
      expect(parsed.size).toBe(20);
      expect(parsed.trackingType).toBe("");
    });

    it("accepts valid tracking type filter", () => {
      const parsed = ashaWorkFilterSchema.parse({ trackingType: "ANC" });
      expect(parsed.trackingType).toBe("ANC");
    });

    it("rejects invalid tracking type", () => {
      const result = ashaWorkFilterSchema.safeParse({ trackingType: "INVALID" });
      expect(result.success).toBe(false);
    });
  });

  describe("incentiveFilterSchema", () => {
    it("accepts empty filters with defaults", () => {
      const parsed = incentiveFilterSchema.parse({});
      expect(parsed.page).toBe(0);
      expect(parsed.size).toBe(20);
    });
  });

  describe("geographyFilterSchema", () => {
    it("accepts empty filters with defaults", () => {
      const parsed = geographyFilterSchema.parse({});
      expect(parsed.page).toBe(0);
      expect(parsed.size).toBe(20);
    });

    it("accepts isActive filter", () => {
      const parsed = geographyFilterSchema.parse({ isActive: "true" });
      expect(parsed.isActive).toBe("true");
    });
  });

  // ---- Constants ----

  describe("constants", () => {
    it("TRACKING_TYPE has correct values", () => {
      expect(TRACKING_TYPE.ANC).toBe("ANC");
      expect(TRACKING_TYPE.PNC).toBe("PNC");
      expect(TRACKING_TYPE.CHILD).toBe("CHILD");
    });

    it("VISIT_STATUS has correct values", () => {
      expect(VISIT_STATUS.SCHEDULED).toBe("SCHEDULED");
      expect(VISIT_STATUS.COMPLETED).toBe("COMPLETED");
      expect(VISIT_STATUS.MISSED).toBe("MISSED");
      expect(VISIT_STATUS.RESCHEDULED).toBe("RESCHEDULED");
    });
  });
});
