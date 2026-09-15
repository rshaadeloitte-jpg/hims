import { describe, expect, it } from "vitest";
import {
  estimationLineSchema,
  estimationRequestSchema,
  estimationResponseSchema,
  packageSubscriptionSchema,
  sponsorChangeSchema,
  advanceApplySchema,
  dayCloseSchema,
  cardPaymentSchema,
  upiPaymentSchema,
  chequePaymentSchema,
  creditNoteExtendedSchema,
  debitNoteExtendedSchema,
  concessionModalSchema,
  quickPaymentSchema,
  ledgerFilterSchema,
} from "./schemas";

describe("estimationLineSchema", () => {
  it("validates a correct estimation line", () => {
    const result = estimationLineSchema.safeParse({
      serviceId: 1,
      serviceName: "Consultation",
      rate: 500,
      qty: 2,
      taxRate: 5,
    });
    expect(result.success).toBe(true);
  });

  it("coerces string numbers", () => {
    const result = estimationLineSchema.safeParse({
      serviceId: "1",
      serviceName: "Consultation",
      rate: "500",
      qty: "2",
    });
    expect(result.success).toBe(true);
    if (result.success) {
      expect(result.data.serviceId).toBe(1);
      expect(result.data.rate).toBe(500);
      expect(result.data.qty).toBe(2);
    }
  });

  it("rejects negative rate", () => {
    const result = estimationLineSchema.safeParse({
      serviceId: 1,
      serviceName: "Consultation",
      rate: -100,
      qty: 1,
    });
    expect(result.success).toBe(false);
  });

  it("rejects zero quantity", () => {
    const result = estimationLineSchema.safeParse({
      serviceId: 1,
      serviceName: "Consultation",
      rate: 500,
      qty: 0,
    });
    expect(result.success).toBe(false);
  });

  it("rejects missing service name", () => {
    const result = estimationLineSchema.safeParse({
      serviceId: 1,
      serviceName: "",
      rate: 500,
      qty: 1,
    });
    expect(result.success).toBe(false);
  });
});

describe("estimationRequestSchema", () => {
  it("validates a correct estimation request", () => {
    const result = estimationRequestSchema.safeParse({
      patientId: 1,
      isSelfCompany: true,
      lines: [
        { serviceId: 1, serviceName: "Consultation", rate: 500, qty: 1 },
      ],
    });
    expect(result.success).toBe(true);
  });

  it("requires at least one line", () => {
    const result = estimationRequestSchema.safeParse({
      patientId: 1,
      lines: [],
    });
    expect(result.success).toBe(false);
  });

  it("requires patientId", () => {
    const result = estimationRequestSchema.safeParse({
      lines: [
        { serviceId: 1, serviceName: "Consultation", rate: 500, qty: 1 },
      ],
    });
    expect(result.success).toBe(false);
  });

  it("accepts optional fields", () => {
    const result = estimationRequestSchema.safeParse({
      patientId: 1,
      encounterId: 100,
      admissionId: 200,
      isSelfCompany: false,
      sponsorCompanyId: 5,
      sponsorPlanId: 10,
      concessionPlanPct: 10,
      concessionBillPct: 5,
      lines: [
        { serviceId: 1, serviceName: "Consultation", rate: 500, qty: 1, taxRate: 5 },
      ],
    });
    expect(result.success).toBe(true);
  });
});

describe("estimationResponseSchema", () => {
  it("validates a correct estimation response", () => {
    const result = estimationResponseSchema.safeParse({
      patientId: 1,
      patientName: "John Doe",
      isSelfCompany: true,
      lines: [
        {
          serviceId: 1,
          serviceName: "Consultation",
          rate: 500,
          qty: 1,
          amount: 500,
          taxRate: 5,
          taxAmount: 25,
          totalAmount: 525,
        },
      ],
      subtotal: 500,
      totalTax: 25,
      concessionAmount: 0,
      selfAmount: 525,
      nonSelfAmount: 0,
      grandTotal: 525,
    });
    expect(result.success).toBe(true);
  });
});

describe("packageSubscriptionSchema", () => {
  it("validates a correct package subscription", () => {
    const result = packageSubscriptionSchema.safeParse({
      packageId: 1,
      startDate: "2025-01-01",
      notes: "Annual package",
    });
    expect(result.success).toBe(true);
  });

  it("requires packageId", () => {
    const result = packageSubscriptionSchema.safeParse({});
    expect(result.success).toBe(false);
  });

  it("validates date format", () => {
    const result = packageSubscriptionSchema.safeParse({
      packageId: 1,
      startDate: "01-01-2025",
    });
    expect(result.success).toBe(false);
  });
});

describe("sponsorChangeSchema", () => {
  it("validates a correct sponsor change", () => {
    const result = sponsorChangeSchema.safeParse({
      sponsorCompanyId: 1,
      sponsorPlanId: 2,
      effectiveDate: "2025-01-01",
      remarks: "Insurance update",
    });
    expect(result.success).toBe(true);
  });

  it("requires sponsorCompanyId", () => {
    const result = sponsorChangeSchema.safeParse({});
    expect(result.success).toBe(false);
  });
});

describe("advanceApplySchema", () => {
  it("validates a correct advance application", () => {
    const result = advanceApplySchema.safeParse({
      advanceId: 1,
      amount: 1000,
    });
    expect(result.success).toBe(true);
  });

  it("requires positive amount", () => {
    const result = advanceApplySchema.safeParse({
      advanceId: 1,
      amount: -100,
    });
    expect(result.success).toBe(false);
  });
});

describe("dayCloseSchema", () => {
  it("validates a correct day close", () => {
    const result = dayCloseSchema.safeParse({
      date: "2025-01-01",
      cashierId: 1,
      includeDetails: true,
    });
    expect(result.success).toBe(true);
  });

  it("requires valid date format", () => {
    const result = dayCloseSchema.safeParse({
      date: "01-01-2025",
      cashierId: 1,
    });
    expect(result.success).toBe(false);
  });
});

describe("cardPaymentSchema", () => {
  it("validates a correct card payment", () => {
    const result = cardPaymentSchema.safeParse({
      amount: 1000,
      cardNumber: "4242424242424242",
      cardHolder: "John Doe",
      expiryMonth: "12",
      expiryYear: "2025",
      cvv: "123",
    });
    expect(result.success).toBe(true);
  });

  it("rejects invalid card number", () => {
    const result = cardPaymentSchema.safeParse({
      amount: 1000,
      cardNumber: "123",
      cardHolder: "John Doe",
      expiryMonth: "12",
      expiryYear: "2025",
      cvv: "123",
    });
    expect(result.success).toBe(false);
  });

  it("rejects invalid expiry month", () => {
    const result = cardPaymentSchema.safeParse({
      amount: 1000,
      cardNumber: "4242424242424242",
      cardHolder: "John Doe",
      expiryMonth: "13",
      expiryYear: "2025",
      cvv: "123",
    });
    expect(result.success).toBe(false);
  });
});

describe("upiPaymentSchema", () => {
  it("validates a correct UPI payment", () => {
    const result = upiPaymentSchema.safeParse({
      amount: 1000,
      upiId: "user@upi",
    });
    expect(result.success).toBe(true);
  });

  it("rejects invalid UPI ID", () => {
    const result = upiPaymentSchema.safeParse({
      amount: 1000,
      upiId: "invalid",
    });
    expect(result.success).toBe(false);
  });
});

describe("chequePaymentSchema", () => {
  it("validates a correct cheque payment", () => {
    const result = chequePaymentSchema.safeParse({
      amount: 1000,
      bankName: "SBI",
      branchName: "Main Branch",
      chequeNo: "123456",
      chequeDate: "2025-01-01",
      micrCode: "123456789",
    });
    expect(result.success).toBe(true);
  });

  it("requires bank name", () => {
    const result = chequePaymentSchema.safeParse({
      amount: 1000,
      bankName: "",
      branchName: "Main",
      chequeNo: "123",
      chequeDate: "2025-01-01",
    });
    expect(result.success).toBe(false);
  });
});

describe("creditNoteExtendedSchema", () => {
  it("validates a correct credit note", () => {
    const result = creditNoteExtendedSchema.safeParse({
      amount: 500,
      reason: "Overcharge correction",
      category: "OVERCHARGE",
      refundMethod: "CASH",
    });
    expect(result.success).toBe(true);
  });

  it("requires reason", () => {
    const result = creditNoteExtendedSchema.safeParse({
      amount: 500,
      reason: "",
      category: "OTHER",
    });
    expect(result.success).toBe(false);
  });

  it("rejects negative amount", () => {
    const result = creditNoteExtendedSchema.safeParse({
      amount: -100,
      reason: "Test",
      category: "OTHER",
    });
    expect(result.success).toBe(false);
  });
});

describe("debitNoteExtendedSchema", () => {
  it("validates a correct debit note", () => {
    const result = debitNoteExtendedSchema.safeParse({
      amount: 200,
      reason: "Additional charge",
      category: "ADDITIONAL_CHARGE",
    });
    expect(result.success).toBe(true);
  });
});

describe("concessionModalSchema", () => {
  it("validates a correct concession", () => {
    const result = concessionModalSchema.safeParse({
      concessionPlanPct: 10,
      concessionBillPct: 5,
      approvedBy: 1,
      remarks: "Staff discount",
      applyToAllLines: true,
    });
    expect(result.success).toBe(true);
  });

  it("rejects percentage over 100", () => {
    const result = concessionModalSchema.safeParse({
      concessionPlanPct: 150,
    });
    expect(result.success).toBe(false);
  });

  it("allows empty concession (no discount)", () => {
    const result = concessionModalSchema.safeParse({
      applyToAllLines: false,
    });
    expect(result.success).toBe(true);
  });
});

describe("quickPaymentSchema", () => {
  it("validates a correct quick payment", () => {
    const result = quickPaymentSchema.safeParse({
      paymentMethod: "CASH",
      amount: 1000,
      referenceNo: "REF123",
      remarks: "Full payment",
      markAsSettled: true,
      generateReceipt: true,
    });
    expect(result.success).toBe(true);
  });

  it("requires positive amount", () => {
    const result = quickPaymentSchema.safeParse({
      paymentMethod: "CASH",
      amount: 0,
    });
    expect(result.success).toBe(false);
  });

  it("validates payment method enum", () => {
    const result = quickPaymentSchema.safeParse({
      paymentMethod: "INVALID",
      amount: 100,
    });
    expect(result.success).toBe(false);
  });
});

describe("ledgerFilterSchema", () => {
  it("validates a correct ledger filter", () => {
    const result = ledgerFilterSchema.safeParse({
      billId: 1,
      fromDate: "2025-01-01",
      toDate: "2025-12-31",
      entryType: "PAYMENT",
      page: 0,
      size: 20,
    });
    expect(result.success).toBe(true);
  });

  it("applies defaults", () => {
    const result = ledgerFilterSchema.safeParse({});
    expect(result.success).toBe(true);
    if (result.success) {
      expect(result.data.page).toBe(0);
      expect(result.data.size).toBe(20);
    }
  });
});
