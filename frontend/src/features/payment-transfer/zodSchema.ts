import { z } from "zod";

/**
 * M33 Payment Transfer Zod schemas — DBT (Direct Benefit Transfer) payment workflows.
 * Follows the billing feature's Stripe-inspired payment form patterns.
 */

const optionalString = z
  .preprocess(
    (v) => (v === "" ? undefined : v),
    z.string().optional(),
  );

// ---- Transfer request schema (Stripe-like payment form) --------------------

export const transferRequestSchema = z.object({
  beneficiaryName: z.string().min(1, "Beneficiary name is required").max(100, "Max 100 chars"),
  beneficiaryAccountNo: z
    .string()
    .min(9, "Account number must be 9-18 digits")
    .max(18, "Account number must be 9-18 digits")
    .regex(/^\d+$/, "Account number must contain only digits"),
  beneficiaryIfsc: z
    .string()
    .regex(/^[A-Z]{4}\d{7}$/, "Invalid IFSC code (e.g., SBIN0001234)"),
  amount: z.coerce.number().positive("Amount must be > 0").max(10_000_000, "Amount exceeds limit"),
  scheme: z.string().min(1, "Scheme is required"),
  purpose: z.string().min(1, "Purpose is required").max(200, "Max 200 chars"),
  remarks: optionalString,
});

export type TransferRequestValues = z.infer<typeof transferRequestSchema>;

// ---- OTP verification schema -----------------------------------------------

export const otpVerificationSchema = z.object({
  transferId: z.string().min(1, "Transfer ID is required"),
  otp: z
    .string()
    .length(6, "OTP must be 6 digits")
    .regex(/^\d{6}$/, "OTP must contain only digits"),
});

export type OtpVerificationValues = z.infer<typeof otpVerificationSchema>;

// ---- Resend OTP schema -----------------------------------------------------

export const resendOtpSchema = z.object({
  transferId: z.string().min(1, "Transfer ID is required"),
});

export type ResendOtpValues = z.infer<typeof resendOtpSchema>;

// ---- Transfer status filter schema -----------------------------------------

export const transferStatusFilterSchema = z.object({
  fromDate: optionalString,
  toDate: optionalString,
  status: z.enum(["", "PENDING", "PROCESSING", "COMPLETED", "FAILED", "CANCELLED"]).optional(),
  page: z.coerce.number().int().nonnegative().default(0),
  size: z.coerce.number().int().positive().default(20),
});

export type TransferStatusFilterValues = z.infer<typeof transferStatusFilterSchema>;

// ---- Cancel transfer schema ------------------------------------------------

export const cancelTransferSchema = z.object({
  transferId: z.string().min(1, "Transfer ID is required"),
  reason: z.string().min(1, "Reason is required").max(500, "Max 500 chars"),
});

export type CancelTransferValues = z.infer<typeof cancelTransferSchema>;
