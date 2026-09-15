import { z } from "zod";

/**
 * M23 RMRS (Revenue Management & Revenue Services) — Zod schemas.
 * Covers:
 *   • Journal Entry (multi-leg double-entry bookkeeping)
 *   • Bank Reconciliation (statement import + transaction matching)
 *   • PO Requisition (purchase-order request workflow)
 */

// ── Shared helpers ──────────────────────────────────────────────────────────

const optionalNumber = z
  .preprocess((v) => (v === "" || v == null ? undefined : v), z.coerce.number().optional());

const optionalString = z
  .preprocess((v) => (v === "" ? undefined : v), z.string().optional());

const dateRegex = /^\d{4}-\d{2}-\d{2}$/;
const dateField = (label: string) =>
  z.string().regex(dateRegex, `${label} must be yyyy-MM-dd`);

// ── Journal Entry schemas ───────────────────────────────────────────────────

/** A single leg (line) in a multi-leg journal entry. */
export const journalLegSchema = z
  .object({
    accountId: z.coerce.number().int().positive("Account is required"),
    accountName: z.string().min(1, "Account name is required"),
    costCenterId: optionalNumber,
    costCenterName: z.string().optional(),
    debit: z.coerce.number().min(0, "Debit must be >= 0").default(0),
    credit: z.coerce.number().min(0, "Credit must be >= 0").default(0),
    narration: optionalString,
  })
  .refine((leg) => leg.debit > 0 || leg.credit > 0, {
    message: "Either debit or credit must be > 0",
  })
  .refine((leg) => !(leg.debit > 0 && leg.credit > 0), {
    message: "A leg cannot have both debit and credit amounts",
  });

export type JournalLegValues = z.infer<typeof journalLegSchema>;

/**
 * Journal entry form schema.
 * Enforces double-entry: total debits must equal total credits.
 */
export const journalEntrySchema = z
  .object({
    voucherNo: optionalString,
    entryType: z.enum(["RECEIPT", "PAYMENT", "JOURNAL", "CONTRA"], {
      errorMap: () => ({ message: "Entry type is required" }),
    }),
    entryDate: dateField("Entry date"),
    narration: z.string().min(1, "Narration is required").max(500, "Max 500 chars"),
    referenceType: optionalString,
    referenceId: optionalNumber,
    legs: z
      .array(journalLegSchema)
      .min(2, "At least 2 legs are required for a double-entry")
      .max(20, "Maximum 20 legs allowed"),
  })
  .refine(
    (entry) => {
      const totalDebit = entry.legs.reduce((s, l) => s + l.debit, 0);
      const totalCredit = entry.legs.reduce((s, l) => s + l.credit, 0);
      return Math.abs(totalDebit - totalCredit) < 0.001;
    },
    { message: "Total debits must equal total credits", path: ["legs"] },
  );

export type JournalEntryValues = z.infer<typeof journalEntrySchema>;

// ── Bank Reconciliation schemas ─────────────────────────────────────────────

export const bankStatementLineSchema = z.object({
  transactionDate: dateField("Transaction date"),
  description: z.string().min(1, "Description is required"),
  referenceNo: optionalString,
  debitAmount: z.coerce.number().min(0).default(0),
  creditAmount: z.coerce.number().min(0).default(0),
  balance: optionalNumber,
});

export type BankStatementLineValues = z.infer<typeof bankStatementLineSchema>;

export const bankReconciliationFilterSchema = z.object({
  bankAccountId: z.coerce.number().int().positive("Bank account is required"),
  fromDate: dateField("From date").optional(),
  toDate: dateField("To date").optional(),
  status: z.enum(["ALL", "MATCHED", "UNMATCHED", "DISPUTED"]).default("ALL"),
  page: z.coerce.number().int().nonnegative().default(0),
  size: z.coerce.number().int().positive().default(50),
});

export type BankReconciliationFilterValues = z.infer<typeof bankReconciliationFilterSchema>;

export const bankMatchSchema = z.object({
  statementLineId: z.coerce.number().int().positive(),
  journalEntryId: z.coerce.number().int().positive(),
  matchAmount: z.coerce.number().positive("Match amount must be > 0"),
  remarks: optionalString,
});

export type BankMatchValues = z.infer<typeof bankMatchSchema>;

// ── PO Requisition schemas ──────────────────────────────────────────────────

export const poRequestLineSchema = z.object({
  itemId: z.coerce.number().int().positive("Item is required"),
  itemName: z.string().min(1, "Item name is required"),
  uom: z.string().min(1, "UOM is required"),
  quantity: z.coerce.number().positive("Quantity must be > 0"),
  estimatedRate: z.coerce.number().nonnegative("Rate must be >= 0"),
  specifications: optionalString,
  remarks: optionalString,
});

export type PoRequestLineValues = z.infer<typeof poRequestLineSchema>;

export const poRequestSchema = z.object({
  departmentId: z.coerce.number().int().positive("Department is required"),
  departmentName: z.string().min(1, "Department name is required"),
  requestedBy: z.string().min(1, "Requester name is required"),
  justification: z.string().min(1, "Justification is required").max(1000, "Max 1000 chars"),
  priority: z.enum(["LOW", "MEDIUM", "HIGH", "URGENT"]).default("MEDIUM"),
  expectedDeliveryDate: dateField("Expected delivery date").optional(),
  vendorSuggestion: optionalString,
  lines: z
    .array(poRequestLineSchema)
    .min(1, "At least one item line is required")
    .max(50, "Maximum 50 lines allowed"),
  totalEstimatedAmount: z.coerce.number().nonnegative().default(0),
  approvalNotes: optionalString,
});

export type PoRequestValues = z.infer<typeof poRequestSchema>;

export const poRequestActionSchema = z.object({
  action: z.enum(["SUBMIT", "APPROVE", "REJECT", "CANCEL", "REVISE"]),
  remarks: z.string().min(1, "Remarks are required for this action").max(500, "Max 500 chars"),
});

export type PoRequestActionValues = z.infer<typeof poRequestActionSchema>;
