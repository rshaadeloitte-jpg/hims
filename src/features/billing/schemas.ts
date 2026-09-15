import { z } from "zod";

/**
 * M04 Billing Zod schemas — extended for new billing UI components.
 * Numeric HTML inputs come through as strings; `z.coerce.number()` normalises them.
 */

// ---- Estimation schemas ---------------------------------------------------

const optionalNumber = z
  .preprocess(
    (v) => (v === "" || v == null ? undefined : v),
    z.coerce.number().optional(),
  );

const optionalString = z
  .preprocess(
    (v) => (v === "" ? undefined : v),
    z.string().optional(),
  );

/** Estimation line input. */
export const estimationLineSchema = z.object({
  serviceId: z.coerce.number().int().positive("Service is required"),
  serviceName: z.string().min(1, "Service name is required"),
  rate: z.coerce.number().nonnegative("Rate must be >= 0"),
  qty: z.coerce.number().int().positive("Quantity must be >= 1"),
  taxRate: z.coerce.number().min(0).max(100).default(0),
});

export type EstimationLineValues = z.infer<typeof estimationLineSchema>;

/** Bill estimation request. */
export const estimationRequestSchema = z.object({
  patientId: z.coerce.number().int().positive("Patient is required"),
  encounterId: optionalNumber,
  admissionId: optionalNumber,
  isSelfCompany: z.boolean().default(true),
  sponsorCompanyId: optionalNumber,
  sponsorPlanId: optionalNumber,
  lines: z.array(estimationLineSchema).min(1, "At least one service line is required"),
  concessionPlanPct: z.coerce.number().min(0).max(100).optional(),
  concessionBillPct: z.coerce.number().min(0).max(100).optional(),
});

export type EstimationRequestValues = z.infer<typeof estimationRequestSchema>;

/** Estimation response (what the server returns). */
export const estimationResponseSchema = z.object({
  patientId: z.number(),
  patientName: z.string().optional(),
  isSelfCompany: z.boolean(),
  sponsorCompanyId: z.number().optional(),
  sponsorPlanId: z.number().optional(),
  lines: z.array(
    z.object({
      serviceId: z.number(),
      serviceName: z.string(),
      rate: z.number(),
      qty: z.number(),
      amount: z.number(),
      taxRate: z.number(),
      taxAmount: z.number(),
      totalAmount: z.number(),
    }),
  ),
  subtotal: z.number(),
  totalTax: z.number(),
  concessionAmount: z.number(),
  concessionPlanPct: z.number().optional(),
  concessionBillPct: z.number().optional(),
  selfAmount: z.number(),
  nonSelfAmount: z.number(),
  grandTotal: z.number(),
});

export type EstimationResponseValues = z.infer<typeof estimationResponseSchema>;

// ---- Package subscription schemas -----------------------------------------

export const packageSubscriptionSchema = z.object({
  packageId: z.coerce.number().int().positive("Package is required"),
  startDate: z.string().regex(/^\d{4}-\d{2}-\d{2}$/, "yyyy-MM-dd").optional(),
  notes: optionalString,
});

export type PackageSubscriptionValues = z.infer<typeof packageSubscriptionSchema>;

// ---- Sponsor change schemas -----------------------------------------------

export const sponsorChangeSchema = z.object({
  sponsorCompanyId: z.coerce.number().int().positive("Company is required"),
  sponsorPlanId: optionalNumber,
  effectiveDate: z.string().regex(/^\d{4}-\d{2}-\d{2}$/, "yyyy-MM-dd").optional(),
  remarks: optionalString,
});

export type SponsorChangeValues = z.infer<typeof sponsorChangeSchema>;

// ---- Advance application schemas ------------------------------------------

export const advanceApplySchema = z.object({
  advanceId: z.coerce.number().int().positive("Advance is required"),
  amount: z.coerce.number().positive("Amount must be > 0"),
});

export type AdvanceApplyValues = z.infer<typeof advanceApplySchema>;

// ---- Day-close schemas ----------------------------------------------------

export const dayCloseSchema = z.object({
  date: z.string().regex(/^\d{4}-\d{2}-\d{2}$/, "yyyy-MM-dd"),
  cashierId: z.coerce.number().int().positive("Cashier is required"),
  includeDetails: z.boolean().default(false),
});

export type DayCloseValues = z.infer<typeof dayCloseSchema>;

// ---- Payment form schemas (Stripe-like) -----------------------------------

export const cardPaymentSchema = z.object({
  amount: z.coerce.number().positive("Amount must be > 0"),
  cardNumber: z
    .string()
    .regex(/^\d{13,19}$/, "Invalid card number"),
  cardHolder: z.string().min(1, "Cardholder name is required"),
  expiryMonth: z
    .string()
    .regex(/^(0[1-9]|1[0-2])$/, "MM"),
  expiryYear: z
    .string()
    .regex(/^\d{4}$/, "YYYY"),
  cvv: z
    .string()
    .regex(/^\d{3,4}$/, "Invalid CVV"),
});

export type CardPaymentValues = z.infer<typeof cardPaymentSchema>;

export const upiPaymentSchema = z.object({
  amount: z.coerce.number().positive("Amount must be > 0"),
  upiId: z
    .string()
    .regex(/^[a-zA-Z0-9._-]+@[a-zA-Z]{2,}$/, "Invalid UPI ID"),
});

export type UpiPaymentValues = z.infer<typeof upiPaymentSchema>;

export const chequePaymentSchema = z.object({
  amount: z.coerce.number().positive("Amount must be > 0"),
  bankName: z.string().min(1, "Bank name is required"),
  branchName: z.string().min(1, "Branch name is required"),
  chequeNo: z.string().min(1, "Cheque number is required"),
  chequeDate: z.string().regex(/^\d{4}-\d{2}-\d{2}$/, "yyyy-MM-dd"),
  micrCode: z.string().length(9, "MICR must be 9 digits").optional(),
});

export type ChequePaymentValues = z.infer<typeof chequePaymentSchema>;

// ---- Credit / Debit note extended schemas ---------------------------------

export const creditNoteExtendedSchema = z.object({
  amount: z.coerce.number().positive("Amount must be > 0"),
  reason: z.string().min(1, "Reason is required").max(500, "Max 500 chars"),
  category: z
    .enum(["REFUND", "CANCELLATION", "DISCOUNT_ADJUSTMENT", "OVERCHARGE", "OTHER"])
    .default("OTHER"),
  attachment: z.string().optional(),
  refundMethod: z.enum(["CASH", "BANK_TRANSFER", "ORIGINAL_PAYMENT"]).optional(),
});

export type CreditNoteExtendedValues = z.infer<typeof creditNoteExtendedSchema>;

export const debitNoteExtendedSchema = z.object({
  amount: z.coerce.number().positive("Amount must be > 0"),
  reason: z.string().min(1, "Reason is required").max(500, "Max 500 chars"),
  category: z
    .enum(["ADDITIONAL_CHARGE", "PENALTY", "TAX_ADJUSTMENT", "OTHER"])
    .default("ADDITIONAL_CHARGE"),
  attachment: z.string().optional(),
});

export type DebitNoteExtendedValues = z.infer<typeof debitNoteExtendedSchema>;

// ---- Concession modal extended schema -------------------------------------

export const concessionModalSchema = z.object({
  concessionPlanId: optionalNumber,
  concessionPlanPct: z.coerce.number().min(0).max(100).optional(),
  concessionBillPct: z.coerce.number().min(0).max(100).optional(),
  approvedBy: optionalNumber,
  remarks: z.string().max(500, "Max 500 chars").optional(),
  applyToAllLines: z.boolean().default(false),
});

export type ConcessionModalValues = z.infer<typeof concessionModalSchema>;

// ---- Quick payment schema ------------------------------------------------

export const quickPaymentSchema = z.object({
  paymentMethod: z.enum(["CASH", "CARD", "UPI", "CHEQUE", "ADVANCE"]).default("CASH"),
  amount: z.coerce.number().positive("Amount must be > 0"),
  referenceNo: optionalString,
  remarks: optionalString,
  markAsSettled: z.boolean().default(false),
  generateReceipt: z.boolean().default(true),
});

export type QuickPaymentValues = z.infer<typeof quickPaymentSchema>;

// ---- Ledger filter schema ------------------------------------------------

export const ledgerFilterSchema = z.object({
  billId: optionalNumber,
  fromDate: optionalString,
  toDate: optionalString,
  entryType: z.enum(["", "PAYMENT", "CREDIT_NOTE", "DEBIT_NOTE", "CONCESSION", "REFUND"]).optional(),
  page: z.coerce.number().int().nonnegative().default(0),
  size: z.coerce.number().int().positive().default(20),
});

export type LedgerFilterValues = z.infer<typeof ledgerFilterSchema>;
