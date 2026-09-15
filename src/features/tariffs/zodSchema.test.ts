import { describe, expect, it } from "vitest";
import {
  tariffSchema,
  tariffFilterSchema,
  packageSchema,
  packageServiceLineSchema,
  packagePlanSelectionSchema,
  concessionTemplateSchema,
  concessionTemplateFilterSchema,
  healthPlanSchema,
  ServiceCategoryEnum,
  type TariffValues,
  type PackageValues,
  type ConcessionTemplateValues,
} from "./zodSchema";

describe("tariffSchema", () => {
  it("validates a correct tariff", () => {
    const result = tariffSchema.safeParse({
      serviceId: 1,
      serviceName: "Consultation",
      serviceCategory: "CONSULTATION",
      baseRate: 500,
      copayPercent: 10,
      packageClass: "General",
      status: "ACTIVE",
    });
    expect(result.success).toBe(true);
  });

  it("coerces string numbers", () => {
    const result = tariffSchema.safeParse({
      serviceId: "1",
      serviceName: "Consultation",
      serviceCategory: "CONSULTATION",
      baseRate: "500",
      copayPercent: "10",
    });
    expect(result.success).toBe(true);
    if (result.success) {
      expect(result.data.serviceId).toBe(1);
      expect(result.data.baseRate).toBe(500);
      expect(result.data.copayPercent).toBe(10);
    }
  });

  it("rejects negative baseRate", () => {
    const result = tariffSchema.safeParse({
      serviceId: 1,
      serviceName: "Consultation",
      serviceCategory: "CONSULTATION",
      baseRate: -100,
    });
    expect(result.success).toBe(false);
  });

  it("rejects copay > 100", () => {
    const result = tariffSchema.safeParse({
      serviceId: 1,
      serviceName: "Consultation",
      serviceCategory: "CONSULTATION",
      baseRate: 500,
      copayPercent: 150,
    });
    expect(result.success).toBe(false);
  });

  it("rejects missing serviceName", () => {
    const result = tariffSchema.safeParse({
      serviceId: 1,
      serviceName: "",
      serviceCategory: "CONSULTATION",
      baseRate: 500,
    });
    expect(result.success).toBe(false);
  });

  it("rejects invalid service category", () => {
    const result = tariffSchema.safeParse({
      serviceId: 1,
      serviceName: "Test",
      serviceCategory: "INVALID",
      baseRate: 500,
    });
    expect(result.success).toBe(false);
  });

  it("applies default copayPercent", () => {
    const result = tariffSchema.safeParse({
      serviceId: 1,
      serviceName: "Consultation",
      serviceCategory: "CONSULTATION",
      baseRate: 500,
    });
    expect(result.success).toBe(true);
    if (result.success) {
      expect(result.data.copayPercent).toBe(0);
    }
  });
});

describe("tariffFilterSchema", () => {
  it("applies defaults", () => {
    const result = tariffFilterSchema.safeParse({});
    expect(result.success).toBe(true);
    if (result.success) {
      expect(result.data.page).toBe(0);
      expect(result.data.size).toBe(20);
    }
  });

  it("accepts category filter", () => {
    const result = tariffFilterSchema.safeParse({
      serviceCategory: "CONSULTATION",
    });
    expect(result.success).toBe(true);
  });

  it("accepts status filter", () => {
    const result = tariffFilterSchema.safeParse({
      status: "ACTIVE",
    });
    expect(result.success).toBe(true);
  });
});

describe("ServiceCategoryEnum", () => {
  it("validates all categories", () => {
    const categories = [
      "CONSULTATION",
      "DIAGNOSTIC",
      "PROCEDURE",
      "ROOM_CHARGE",
      "PHARMACY",
      "LABORATORY",
      "RADIOLOGY",
      "PACKAGE",
      "MISCELLANEOUS",
    ];
    for (const cat of categories) {
      const result = ServiceCategoryEnum.safeParse(cat);
      expect(result.success).toBe(true);
    }
  });

  it("rejects unknown category", () => {
    const result = ServiceCategoryEnum.safeParse("UNKNOWN");
    expect(result.success).toBe(false);
  });
});

describe("packageSchema", () => {
  it("validates a correct package", () => {
    const result = packageSchema.safeParse({
      packageName: "Health Checkup",
      packageCode: "HC001",
      description: "Full body checkup",
      totalValue: 5000,
      sellPrice: 4500,
      services: [
        { serviceId: 1, serviceName: "Consultation", qty: 1 },
        { serviceId: 2, serviceName: "Blood Test", qty: 1 },
      ],
      plans: [],
      status: "ACTIVE",
    });
    expect(result.success).toBe(true);
  });

  it("requires at least one service", () => {
    const result = packageSchema.safeParse({
      packageName: "Test",
      packageCode: "TEST",
      totalValue: 1000,
      sellPrice: 900,
      services: [],
    });
    expect(result.success).toBe(false);
  });

  it("requires positive sellPrice", () => {
    const result = packageSchema.safeParse({
      packageName: "Test",
      packageCode: "TEST",
      totalValue: 1000,
      sellPrice: 0,
      services: [{ serviceId: 1, serviceName: "Test", qty: 1 }],
    });
    expect(result.success).toBe(false);
  });

  it("accepts packages without plans", () => {
    const result = packageSchema.safeParse({
      packageName: "Test",
      packageCode: "TEST",
      totalValue: 1000,
      sellPrice: 900,
      services: [{ serviceId: 1, serviceName: "Test", qty: 1 }],
    });
    expect(result.success).toBe(true);
  });
});

describe("packageServiceLineSchema", () => {
  it("validates a correct service line", () => {
    const result = packageServiceLineSchema.safeParse({
      serviceId: 1,
      serviceName: "Consultation",
      qty: 2,
    });
    expect(result.success).toBe(true);
  });

  it("coerces string qty", () => {
    const result = packageServiceLineSchema.safeParse({
      serviceId: "1",
      serviceName: "Consultation",
      qty: "2",
    });
    expect(result.success).toBe(true);
    if (result.success) {
      expect(result.data.qty).toBe(2);
    }
  });
});

describe("packagePlanSelectionSchema", () => {
  it("validates a correct plan selection", () => {
    const result = packagePlanSelectionSchema.safeParse({
      planId: 1,
      planName: "Gold",
      coveragePercent: 80,
      maxCoverageAmount: 50000,
    });
    expect(result.success).toBe(true);
  });

  it("applies default coveragePercent", () => {
    const result = packagePlanSelectionSchema.safeParse({
      planId: 1,
      planName: "Basic",
    });
    expect(result.success).toBe(true);
    if (result.success) {
      expect(result.data.coveragePercent).toBe(100);
    }
  });
});

describe("concessionTemplateSchema", () => {
  it("validates a correct template", () => {
    const result = concessionTemplateSchema.safeParse({
      code: "STAFF",
      name: "Staff Discount",
      scheme: "Self",
      description: "For hospital staff",
      maxPercentOff: 20,
      requiresApproval: true,
    });
    expect(result.success).toBe(true);
  });

  it("coerces maxPercentOff", () => {
    const result = concessionTemplateSchema.safeParse({
      code: "STAFF",
      name: "Staff Discount",
      scheme: "Self",
      maxPercentOff: "20.5",
    });
    expect(result.success).toBe(true);
    if (result.success) {
      expect(result.data.maxPercentOff).toBe(20.5);
    }
  });

  it("rejects maxPercentOff > 100", () => {
    const result = concessionTemplateSchema.safeParse({
      code: "STAFF",
      name: "Staff Discount",
      scheme: "Self",
      maxPercentOff: 150,
    });
    expect(result.success).toBe(false);
  });

  it("rejects negative maxPercentOff", () => {
    const result = concessionTemplateSchema.safeParse({
      code: "STAFF",
      name: "Staff Discount",
      scheme: "Self",
      maxPercentOff: -5,
    });
    expect(result.success).toBe(false);
  });

  it("rejects missing required fields", () => {
    const result = concessionTemplateSchema.safeParse({
      code: "",
      name: "",
      scheme: "",
      maxPercentOff: 0,
    });
    expect(result.success).toBe(false);
  });

  it("applies defaults", () => {
    const result = concessionTemplateSchema.safeParse({
      code: "STAFF",
      name: "Staff Discount",
      scheme: "Self",
      maxPercentOff: 20,
    });
    expect(result.success).toBe(true);
    if (result.success) {
      expect(result.data.requiresApproval).toBe(false);
      expect(result.data.description).toBeUndefined();
    }
  });
});

describe("concessionTemplateFilterSchema", () => {
  it("validates with all fields", () => {
    const result = concessionTemplateFilterSchema.safeParse({
      scheme: "RGHS",
      status: "1",
    });
    expect(result.success).toBe(true);
  });

  it("allows empty filter", () => {
    const result = concessionTemplateFilterSchema.safeParse({});
    expect(result.success).toBe(true);
  });
});

describe("healthPlanSchema", () => {
  it("validates a correct health plan", () => {
    const result = healthPlanSchema.safeParse({
      planName: "Gold Plan",
      planCode: "GOLD",
      companyId: 1,
      coveragePercent: 80,
      maxCoverageAmount: 50000,
      exclusions: ["Cosmetic surgery"],
      status: "ACTIVE",
    });
    expect(result.success).toBe(true);
  });

  it("applies defaults", () => {
    const result = healthPlanSchema.safeParse({
      planName: "Basic",
      planCode: "BASIC",
      companyId: 1,
    });
    expect(result.success).toBe(true);
    if (result.success) {
      expect(result.data.coveragePercent).toBe(100);
      expect(result.data.exclusions).toEqual([]);
      expect(result.data.status).toBe("ACTIVE");
    }
  });

  it("requires companyId", () => {
    const result = healthPlanSchema.safeParse({
      planName: "Basic",
      planCode: "BASIC",
    });
    expect(result.success).toBe(false);
  });
});
