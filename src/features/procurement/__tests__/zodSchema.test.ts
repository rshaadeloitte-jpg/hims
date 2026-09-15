import { describe, expect, it } from "vitest";
import {
  purchaseOrderSchema,
  purchaseOrderLineSchema,
  grnSchema,
  grnLineSchema,
  quotationSchema,
  quotationLineSchema,
} from "../zodSchema";

describe("purchaseOrderLineSchema", () => {
  it("validates a correct line", () => {
    const result = purchaseOrderLineSchema.safeParse({
      itemId: 1, orderedQty: 10, unitPrice: 99.99, taxPercent: 12, leadTimeDays: 7, remarks: "Urgent",
    });
    expect(result.success).toBe(true);
  });

  it("coerces string numbers", () => {
    const parsed = purchaseOrderLineSchema.parse({
      itemId: "5", orderedQty: "100", unitPrice: "50.5", taxPercent: "18",
    });
    expect(parsed.itemId).toBe(5);
    expect(parsed.orderedQty).toBe(100);
    expect(parsed.unitPrice).toBe(50.5);
    expect(parsed.taxPercent).toBe(18);
  });

  it("rejects zero quantity", () => {
    const result = purchaseOrderLineSchema.safeParse({ itemId: 1, orderedQty: 0, unitPrice: 10 });
    expect(result.success).toBe(false);
  });

  it("rejects negative unit price", () => {
    const result = purchaseOrderLineSchema.safeParse({ itemId: 1, orderedQty: 1, unitPrice: -5 });
    expect(result.success).toBe(false);
  });

  it("rejects missing itemId", () => {
    const result = purchaseOrderLineSchema.safeParse({ itemId: 0, orderedQty: 1, unitPrice: 10 });
    expect(result.success).toBe(false);
  });
});

describe("purchaseOrderSchema", () => {
  const validInput = {
    storeId: 1,
    supplierId: 2,
    orderDate: "2026-09-11",
    expectedDate: "2026-09-20",
    remarks: "Test PO",
    lines: [{ itemId: 1, orderedQty: 10, unitPrice: 100, taxPercent: 12 }],
  };

  it("validates a correct PO", () => {
    const result = purchaseOrderSchema.safeParse(validInput);
    expect(result.success).toBe(true);
  });

  it("requires at least one line", () => {
    const result = purchaseOrderSchema.safeParse({ storeId: 1, supplierId: 2, lines: [] });
    expect(result.success).toBe(false);
    if (!result.success) {
      expect(result.error.issues[0]?.path).toContain("lines");
    }
  });

  it("requires storeId", () => {
    const result = purchaseOrderSchema.safeParse({ ...validInput, storeId: 0 });
    expect(result.success).toBe(false);
  });

  it("requires supplierId", () => {
    const result = purchaseOrderSchema.safeParse({ ...validInput, supplierId: 0 });
    expect(result.success).toBe(false);
  });

  it("allows optional quotationId", () => {
    const result = purchaseOrderSchema.safeParse({ ...validInput, quotationId: 99 });
    expect(result.success).toBe(true);
  });

  it("coerces numeric strings", () => {
    const parsed = purchaseOrderSchema.parse({
      ...validInput,
      storeId: "3",
      supplierId: "4",
      lines: [{ itemId: "10", orderedQty: "5", unitPrice: "200.50" }],
    });
    expect(parsed.storeId).toBe(3);
    expect(parsed.supplierId).toBe(4);
    expect(parsed.lines[0].itemId).toBe(10);
  });
});

describe("grnLineSchema", () => {
  it("validates with batchId", () => {
    const result = grnLineSchema.safeParse({ itemId: 1, quantity: 5, batchId: 10 });
    expect(result.success).toBe(true);
  });

  it("validates with batchCode", () => {
    const result = grnLineSchema.safeParse({ itemId: 1, quantity: 5, batchCode: "BATCH-001" });
    expect(result.success).toBe(true);
  });

  it("rejects when neither batchId nor batchCode provided", () => {
    const result = grnLineSchema.safeParse({ itemId: 1, quantity: 5 });
    expect(result.success).toBe(false);
    if (!result.success) {
      expect(result.error.issues[0]?.path).toContain("batchCode");
    }
  });

  it("rejects zero quantity", () => {
    const result = grnLineSchema.safeParse({ itemId: 1, quantity: 0, batchId: 1 });
    expect(result.success).toBe(false);
  });
});

describe("grnSchema", () => {
  const validInput = {
    storeId: 1,
    supplierId: 2,
    invoiceNo: "INV-001",
    invoiceDate: "2026-09-11",
    receivedDate: "2026-09-11",
    provisional: false,
    consignment: false,
    lines: [{ itemId: 1, quantity: 10, batchCode: "B001" }],
  };

  it("validates a correct GRN", () => {
    const result = grnSchema.safeParse(validInput);
    expect(result.success).toBe(true);
  });

  it("requires storeId", () => {
    const result = grnSchema.safeParse({ ...validInput, storeId: 0 });
    expect(result.success).toBe(false);
  });

  it("requires at least one line", () => {
    const result = grnSchema.safeParse({ ...validInput, lines: [] });
    expect(result.success).toBe(false);
  });

  it("defaults provisional and consignment to false", () => {
    const parsed = grnSchema.parse({ storeId: 1, lines: [{ itemId: 1, quantity: 1, batchCode: "X" }] });
    expect(parsed.provisional).toBe(false);
    expect(parsed.consignment).toBe(false);
  });

  it("allows optional supplierId", () => {
    const { supplierId: _sup, ...rest } = validInput;
    const result = grnSchema.safeParse(rest);
    expect(result.success).toBe(true);
  });
});

describe("quotationLineSchema", () => {
  it("validates a correct line", () => {
    const result = quotationLineSchema.safeParse({
      itemId: 1, quantity: 100, unitPrice: 50, taxPercent: 18, leadTimeDays: 5,
    });
    expect(result.success).toBe(true);
  });

  it("coerces strings", () => {
    const parsed = quotationLineSchema.parse({
      itemId: "3", quantity: "50", unitPrice: "25.5", taxPercent: "12",
    });
    expect(parsed.itemId).toBe(3);
    expect(parsed.quantity).toBe(50);
    expect(parsed.unitPrice).toBe(25.5);
  });

  it("rejects negative price", () => {
    const result = quotationLineSchema.safeParse({ itemId: 1, quantity: 1, unitPrice: -1 });
    expect(result.success).toBe(false);
  });
});

describe("quotationSchema", () => {
  const validInput = {
    supplierId: 1,
    enquiryRef: "ENQ-001",
    validUntil: "2026-12-31",
    remarks: "Test quotation",
    lines: [{ itemId: 1, quantity: 100, unitPrice: 50, taxPercent: 18 }],
  };

  it("validates a correct quotation", () => {
    const result = quotationSchema.safeParse(validInput);
    expect(result.success).toBe(true);
  });

  it("requires supplierId", () => {
    const result = quotationSchema.safeParse({ ...validInput, supplierId: 0 });
    expect(result.success).toBe(false);
  });

  it("requires at least one line", () => {
    const result = quotationSchema.safeParse({ ...validInput, lines: [] });
    expect(result.success).toBe(false);
  });

  it("allows optional fields", () => {
    const { enquiryRef: _er, validUntil: _vu, remarks: _rm, ...minimal } = validInput;
    const result = quotationSchema.safeParse(minimal);
    expect(result.success).toBe(true);
  });
});
