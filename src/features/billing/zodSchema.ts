import { z } from "zod";

/**
 * M04 Billing Zod schemas — mirror the billing-service request DTOs.
 * Numeric HTML inputs come through as strings; `z.coerce.number()` normalises them.
 */

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

/** Create a charge line (POST /api/v1/charges). */
export const chargeSchema = z.object({
  serviceId: z.coerce.number().int().positive("Service is required"),
  serviceName: z.string().min(1, "Service name is required"),
  rate: z.coerce.number().nonnegative("Rate must be >= 0"),
  qty: z.coerce.number().int().positive("Quantity must be >= 1"),
});

export type ChargeValues = z.infer<typeof chargeSchema>;

/** Create a bill (POST /api/v1/bills). */
export const createBillSchema = z.object({
  patientId: z.coerce.number().int().positive("Patient is required"),
  encounterId: optionalNumber,
  admissionId: optionalNumber,
  isSelfCompany: z.boolean().default(true),
  prescriberDoctorId: optionalNumber,
  cashCounterId: z.coerce.number().int().default(1),
});

export type CreateBillValues = z.infer<typeof createBillSchema>;

/** Apply concession (POST /api/v1/bills/{id}/concession). */
export const concessionSchema = z.object({
  concessionPlanPct: z.coerce.number().min(0).max(100).optional(),
  concessionBillPct: z.coerce.number().min(0).max(100).optional(),
});

export type ConcessionValues = z.infer<typeof concessionSchema>;

/** Record payment (POST /api/v1/bills/{id}/payments). */
export const paymentSchema = z.object({
  amount: z.coerce.number().positive("Amount must be > 0"),
  paymentMethod: z.enum(["CASH", "CARD", "UPI", "CHEQUE", "ADVANCE"]).default("CASH"),
  referenceNo: optionalString,
  remarks: optionalString,
});

export type PaymentValues = z.infer<typeof paymentSchema>;

/** Settle bill (PUT /api/v1/internal/bills/{id}/settlement). */
export const settlementSchema = z.object({
  paidSelf: z.coerce.number().nonnegative().default(0),
  paidNonSelf: z.coerce.number().nonnegative().default(0),
});

export type SettlementValues = z.infer<typeof settlementSchema>;

/** Credit note (POST /api/v1/bills/{id}/credit-notes). */
export const creditNoteSchema = z.object({
  amount: z.coerce.number().positive("Amount must be > 0"),
  reason: z.string().min(1, "Reason is required"),
});

export type CreditNoteValues = z.infer<typeof creditNoteSchema>;

/** Debit note (POST /api/v1/bills/{id}/debit-notes). */
export const debitNoteSchema = z.object({
  amount: z.coerce.number().positive("Amount must be > 0"),
  reason: z.string().min(1, "Reason is required"),
});

export type DebitNoteValues = z.infer<typeof debitNoteSchema>;

/** Bill list filters. */
export const billFilterSchema = z.object({
  patientId: optionalNumber,
  fromDate: optionalString,
  toDate: optionalString,
  isSettled: z.enum(["", "true", "false"]).optional(),
  page: z.coerce.number().int().nonnegative().default(0),
  size: z.coerce.number().int().positive().default(20),
});

export type BillFilterValues = z.infer<typeof billFilterSchema>;
