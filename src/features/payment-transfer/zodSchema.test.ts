import { describe, expect, it } from "vitest";
import {
  transferRequestSchema,
  otpVerificationSchema,
  resendOtpSchema,
  transferStatusFilterSchema,
  cancelTransferSchema,
} from "./zodSchema";

describe("transferRequestSchema", () => {
  it("validates a correct transfer request", () => {
    const result = transferRequestSchema.safeParse({
      beneficiaryName: "John Doe",
      beneficiaryAccountNo: "1234567890",
      beneficiaryIfsc: "SBIN0001234",
      amount: 5000,
      scheme: "PM-KISAN",
      purpose: "Annual payment",
      remarks: "First installment",
    });
    expect(result.success).toBe(true);
  });

  it("coerces string amount to number", () => {
    const result = transferRequestSchema.safeParse({
      beneficiaryName: "John Doe",
      beneficiaryAccountNo: "1234567890",
      beneficiaryIfsc: "SBIN0001234",
      amount: "5000",
      scheme: "PM-KISAN",
      purpose: "Annual payment",
    });
    expect(result.success).toBe(true);
    if (result.success) {
      expect(result.data.amount).toBe(5000);
    }
  });

  it("rejects empty beneficiary name", () => {
    const result = transferRequestSchema.safeParse({
      beneficiaryName: "",
      beneficiaryAccountNo: "1234567890",
      beneficiaryIfsc: "SBIN0001234",
      amount: 5000,
      scheme: "PM-KISAN",
      purpose: "Annual payment",
    });
    expect(result.success).toBe(false);
  });

  it("rejects short account number", () => {
    const result = transferRequestSchema.safeParse({
      beneficiaryName: "John Doe",
      beneficiaryAccountNo: "12345",
      beneficiaryIfsc: "SBIN0001234",
      amount: 5000,
      scheme: "PM-KISAN",
      purpose: "Annual payment",
    });
    expect(result.success).toBe(false);
  });

  it("rejects account number with non-digits", () => {
    const result = transferRequestSchema.safeParse({
      beneficiaryName: "John Doe",
      beneficiaryAccountNo: "123456789A",
      beneficiaryIfsc: "SBIN0001234",
      amount: 5000,
      scheme: "PM-KISAN",
      purpose: "Annual payment",
    });
    expect(result.success).toBe(false);
  });

  it("rejects invalid IFSC code", () => {
    const result = transferRequestSchema.safeParse({
      beneficiaryName: "John Doe",
      beneficiaryAccountNo: "1234567890",
      beneficiaryIfsc: "INVALID",
      amount: 5000,
      scheme: "PM-KISAN",
      purpose: "Annual payment",
    });
    expect(result.success).toBe(false);
  });

  it("accepts valid IFSC code", () => {
    const result = transferRequestSchema.safeParse({
      beneficiaryName: "John Doe",
      beneficiaryAccountNo: "1234567890",
      beneficiaryIfsc: "HDFC0001234",
      amount: 5000,
      scheme: "PM-KISAN",
      purpose: "Annual payment",
    });
    expect(result.success).toBe(true);
  });

  it("rejects zero amount", () => {
    const result = transferRequestSchema.safeParse({
      beneficiaryName: "John Doe",
      beneficiaryAccountNo: "1234567890",
      beneficiaryIfsc: "SBIN0001234",
      amount: 0,
      scheme: "PM-KISAN",
      purpose: "Annual payment",
    });
    expect(result.success).toBe(false);
  });

  it("rejects negative amount", () => {
    const result = transferRequestSchema.safeParse({
      beneficiaryName: "John Doe",
      beneficiaryAccountNo: "1234567890",
      beneficiaryIfsc: "SBIN0001234",
      amount: -100,
      scheme: "PM-KISAN",
      purpose: "Annual payment",
    });
    expect(result.success).toBe(false);
  });

  it("rejects amount exceeding limit", () => {
    const result = transferRequestSchema.safeParse({
      beneficiaryName: "John Doe",
      beneficiaryAccountNo: "1234567890",
      beneficiaryIfsc: "SBIN0001234",
      amount: 20000000,
      scheme: "PM-KISAN",
      purpose: "Annual payment",
    });
    expect(result.success).toBe(false);
  });

  it("requires scheme", () => {
    const result = transferRequestSchema.safeParse({
      beneficiaryName: "John Doe",
      beneficiaryAccountNo: "1234567890",
      beneficiaryIfsc: "SBIN0001234",
      amount: 5000,
      scheme: "",
      purpose: "Annual payment",
    });
    expect(result.success).toBe(false);
  });

  it("requires purpose", () => {
    const result = transferRequestSchema.safeParse({
      beneficiaryName: "John Doe",
      beneficiaryAccountNo: "1234567890",
      beneficiaryIfsc: "SBIN0001234",
      amount: 5000,
      scheme: "PM-KISAN",
      purpose: "",
    });
    expect(result.success).toBe(false);
  });

  it("treats empty remarks as undefined", () => {
    const result = transferRequestSchema.safeParse({
      beneficiaryName: "John Doe",
      beneficiaryAccountNo: "1234567890",
      beneficiaryIfsc: "SBIN0001234",
      amount: 5000,
      scheme: "PM-KISAN",
      purpose: "Annual payment",
      remarks: "",
    });
    expect(result.success).toBe(true);
    if (result.success) {
      expect(result.data.remarks).toBeUndefined();
    }
  });
});

describe("otpVerificationSchema", () => {
  it("validates a correct OTP", () => {
    const result = otpVerificationSchema.safeParse({
      transferId: "TRF-123456",
      otp: "123456",
    });
    expect(result.success).toBe(true);
  });

  it("rejects OTP with less than 6 digits", () => {
    const result = otpVerificationSchema.safeParse({
      transferId: "TRF-123456",
      otp: "12345",
    });
    expect(result.success).toBe(false);
  });

  it("rejects OTP with more than 6 digits", () => {
    const result = otpVerificationSchema.safeParse({
      transferId: "TRF-123456",
      otp: "1234567",
    });
    expect(result.success).toBe(false);
  });

  it("rejects OTP with non-digits", () => {
    const result = otpVerificationSchema.safeParse({
      transferId: "TRF-123456",
      otp: "12345A",
    });
    expect(result.success).toBe(false);
  });

  it("requires transfer ID", () => {
    const result = otpVerificationSchema.safeParse({
      transferId: "",
      otp: "123456",
    });
    expect(result.success).toBe(false);
  });
});

describe("resendOtpSchema", () => {
  it("validates resend request", () => {
    const result = resendOtpSchema.safeParse({
      transferId: "TRF-123456",
    });
    expect(result.success).toBe(true);
  });

  it("requires transfer ID", () => {
    const result = resendOtpSchema.safeParse({
      transferId: "",
    });
    expect(result.success).toBe(false);
  });
});

describe("transferStatusFilterSchema", () => {
  it("validates a correct filter", () => {
    const result = transferStatusFilterSchema.safeParse({
      fromDate: "2025-01-01",
      toDate: "2025-12-31",
      status: "COMPLETED",
      page: 0,
      size: 20,
    });
    expect(result.success).toBe(true);
  });

  it("applies defaults", () => {
    const result = transferStatusFilterSchema.safeParse({});
    expect(result.success).toBe(true);
    if (result.success) {
      expect(result.data.page).toBe(0);
      expect(result.data.size).toBe(20);
    }
  });

  it("validates status enum", () => {
    const result = transferStatusFilterSchema.safeParse({
      status: "INVALID",
    });
    expect(result.success).toBe(false);
  });

  it("accepts empty status", () => {
    const result = transferStatusFilterSchema.safeParse({
      status: "",
    });
    expect(result.success).toBe(true);
  });

  it("accepts all valid statuses", () => {
    const statuses = ["PENDING", "PROCESSING", "COMPLETED", "FAILED", "CANCELLED"] as const;
    for (const status of statuses) {
      const result = transferStatusFilterSchema.safeParse({ status });
      expect(result.success).toBe(true);
    }
  });
});

describe("cancelTransferSchema", () => {
  it("validates a correct cancel request", () => {
    const result = cancelTransferSchema.safeParse({
      transferId: "TRF-123456",
      reason: "Wrong beneficiary",
    });
    expect(result.success).toBe(true);
  });

  it("requires transfer ID", () => {
    const result = cancelTransferSchema.safeParse({
      transferId: "",
      reason: "Wrong beneficiary",
    });
    expect(result.success).toBe(false);
  });

  it("requires reason", () => {
    const result = cancelTransferSchema.safeParse({
      transferId: "TRF-123456",
      reason: "",
    });
    expect(result.success).toBe(false);
  });

  it("rejects reason exceeding 500 chars", () => {
    const result = cancelTransferSchema.safeParse({
      transferId: "TRF-123456",
      reason: "a".repeat(501),
    });
    expect(result.success).toBe(false);
  });
});
