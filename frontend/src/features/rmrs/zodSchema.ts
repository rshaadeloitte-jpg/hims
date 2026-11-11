import { z } from "zod";

/**
 * M23 RMRS (Revenue Management & Revenue Services) — Zod schemas & DTOs.
 * Aligned with backend controllers:
 *   • RmrsM23Controller: /api/v1/rmrs/journal-vouchers, /api/v1/rmrs/bank-statements, /api/v1/rmrs/po-requests, /api/v1/rmrs/ledgers/{id}/suspense
 *   • MasterController: /api/v1/masters/heads, /api/v1/masters/authorities, /api/v1/masters/ledgers
 *   • CollectionController: /api/v1/collections
 */

// ── Shared helpers ──────────────────────────────────────────────────────────

const optionalNumber = z.preprocess(
  (v) => (v === "" || v == null ? undefined : v),
  z.coerce.number().optional(),
);

const optionalString = z.preprocess(
  (v) => (v === "" || v == null ? undefined : v),
  z.string().optional(),
);

const dateRegex = /^\d{4}-\d{2}-\d{2}$/;
const dateField = (label: string) =>
  z.string().regex(dateRegex, `${label} must be yyyy-MM-dd`);

// ── Treasury Masters schemas (MasterController.java) ────────────────────────

export const createHeadSchema = z.object({
  headCode: z.string().min(1, "Head code is required").max(20, "Max 20 chars"),
  headName: z.string().min(1, "Head name is required").max(100, "Max 100 chars"),
});
export type CreateHeadRequest = z.infer<typeof createHeadSchema>;

export const createAuthoritySchema = z.object({
  authorityCode: z.string().min(1, "Authority code is required").max(20, "Max 20 chars"),
  authorityName: z.string().max(120, "Max 120 chars").optional(),
  headId: optionalNumber,
});
export type CreateAuthorityRequest = z.infer<typeof createAuthoritySchema>;

export const createLedgerSchema = z.object({
  ledgerCode: z.string().min(1, "Ledger code is required").max(20, "Max 20 chars"),
  ledgerName: z.string().max(120, "Max 120 chars").optional(),
  headId: z.coerce.number().int().positive("Head ID is required"),
});
export type CreateLedgerRequest = z.infer<typeof createLedgerSchema>;

export const markSuspenseSchema = z.object({
  reason: optionalString,
});
export type MarkSuspenseRequest = z.infer<typeof markSuspenseSchema>;

// ── Journal Voucher schemas (RmrsM23Controller.java) ────────────────────────

export const journalVoucherLegSchema = z
  .object({
    id: optionalNumber,
    legId: optionalNumber,
    voucherId: optionalNumber,
    ledgerId: optionalNumber,
    accountId: optionalNumber,
    accountName: z.string().optional(),
    costCenterId: optionalNumber,
    costCenterName: z.string().optional(),
    debit: z.coerce.number().min(0, "Debit must be >= 0").default(0),
    credit: z.coerce.number().min(0, "Credit must be >= 0").default(0),
    narration: optionalString,
  })
  .refine(
    (leg) =>
      (leg.ledgerId != null && leg.ledgerId > 0) ||
      (leg.accountId != null && leg.accountId > 0),
    { message: "Ledger/Account is required", path: ["ledgerId"] },
  )
  .refine((leg) => (leg.debit ?? 0) > 0 || (leg.credit ?? 0) > 0, {
    message: "Either debit or credit must be > 0",
  })
  .refine((leg) => !((leg.debit ?? 0) > 0 && (leg.credit ?? 0) > 0), {
    message: "A leg cannot have both debit and credit amounts",
  });

export type JournalLegValues = z.infer<typeof journalVoucherLegSchema>;
export const journalLegSchema = journalVoucherLegSchema;

export const createJournalVoucherSchema = z
  .object({
    voucherNo: optionalString,
    narration: optionalString,
    voucherDate: dateField("Voucher date").optional(),
    entryDate: dateField("Entry date").optional(),
    entryType: z.enum(["RECEIPT", "PAYMENT", "JOURNAL", "CONTRA"]).optional(),
    referenceType: optionalString,
    referenceId: optionalNumber,
    legs: z
      .array(journalVoucherLegSchema)
      .min(2, "At least 2 legs are required for a double-entry")
      .max(20, "Maximum 20 legs allowed"),
  })
  .refine(
    (entry) => {
      const totalDebit = entry.legs.reduce((s, l) => s + (l.debit ?? 0), 0);
      const totalCredit = entry.legs.reduce((s, l) => s + (l.credit ?? 0), 0);
      return Math.abs(totalDebit - totalCredit) < 0.001;
    },
    { message: "Total debits must equal total credits", path: ["legs"] },
  );

export type JournalEntryValues = z.infer<typeof createJournalVoucherSchema>;
export const journalEntrySchema = createJournalVoucherSchema;

// ── Bank Statement & Reconciliation schemas (RmrsM23Controller.java) ────────

export const bankStatementLineSchema = z.object({
  transactionDate: dateField("Transaction date").optional(),
  txnDate: dateField("Transaction date").optional(),
  description: optionalString,
  referenceNo: optionalString,
  externalRef: optionalString,
  direction: z.enum(["DR", "CR"]).optional(),
  debitAmount: z.coerce.number().min(0).default(0),
  creditAmount: z.coerce.number().min(0).default(0),
  amount: optionalNumber,
  balance: optionalNumber,
});
export type BankStatementLineValues = z.infer<typeof bankStatementLineSchema>;

export const importBankStatementSchema = z.object({
  bankLedgerId: z.coerce.number().int().positive("Bank ledger ID is required"),
  txnDate: dateField("Transaction date"),
  amount: z.coerce.number().positive("Amount must be > 0"),
  direction: z.enum(["DR", "CR"], {
    errorMap: () => ({ message: "Direction must be DR or CR" }),
  }),
  description: optionalString,
  externalRef: optionalString,
});
export type ImportBankStatementValues = z.infer<typeof importBankStatementSchema>;

export const reconcileBankStatementSchema = z.object({
  matchedEntryId: optionalNumber,
  matchedCollectionId: optionalNumber,
});
export type ReconcileBankStatementValues = z.infer<typeof reconcileBankStatementSchema>;

export const disputeBankStatementSchema = z.object({
  reason: optionalString,
});
export type DisputeBankStatementValues = z.infer<typeof disputeBankStatementSchema>;

export const bankReconciliationFilterSchema = z.object({
  bankAccountId: z.coerce.number().int().nonnegative().default(0),
  fromDate: dateField("From date").optional(),
  toDate: dateField("To date").optional(),
  status: z.enum(["ALL", "IMPORTED", "RECONCILED", "DISPUTED", "MATCHED", "UNMATCHED"]).default("ALL"),
  page: z.coerce.number().int().nonnegative().default(0),
  size: z.coerce.number().int().positive().default(50),
});
export type BankReconciliationFilterValues = z.infer<typeof bankReconciliationFilterSchema>;

export const bankMatchSchema = z.object({
  statementLineId: z.coerce.number().int().positive(),
  journalEntryId: optionalNumber,
  matchedEntryId: optionalNumber,
  matchedCollectionId: optionalNumber,
  matchAmount: z.coerce.number().positive("Match amount must be > 0").optional(),
  remarks: optionalString,
});
export type BankMatchValues = z.infer<typeof bankMatchSchema>;

// ── PO Request schemas (RmrsM23Controller.java) ──────────────────────────────

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

export const createPoRequestSchema = z.object({
  requestingDeptId: optionalNumber,
  departmentId: optionalNumber,
  departmentName: optionalString,
  headId: z.coerce.number().int().positive("Head ID is required").default(1),
  ledgerId: optionalNumber,
  amount: z.coerce.number().positive("Amount must be > 0").optional(),
  totalEstimatedAmount: optionalNumber,
  remarks: optionalString,
  justification: optionalString,
  requestedBy: optionalString,
  priority: z.enum(["LOW", "MEDIUM", "HIGH", "URGENT"]).optional(),
  expectedDeliveryDate: optionalString,
  vendorSuggestion: optionalString,
  lines: z.array(poRequestLineSchema).default([]),
});
export type CreatePoRequestValues = z.infer<typeof createPoRequestSchema>;
export type PoRequestValues = CreatePoRequestValues;
export const poRequestSchema = createPoRequestSchema;

export const poRequestActionSchema = z.object({
  action: z.enum(["SUBMIT", "APPROVE", "REJECT", "CANCEL", "REVISE", "ISSUE"]),
  remarks: optionalString,
  issuedPoId: optionalNumber,
});
export type PoRequestActionValues = z.infer<typeof poRequestActionSchema>;

// ── Collection schemas (CollectionController.java) ───────────────────────────

export const createCollectionSchema = z.object({
  receiptNo: z.string().min(1, "Receipt number is required").max(24, "Max 24 chars"),
  collectionDate: dateField("Collection date"),
  patientId: optionalNumber,
  amount: z.coerce.number().positive("Amount must be > 0"),
  headCode: z.string().min(1, "Head code is required"),
  ledgerCode: z.string().min(1, "Ledger code is required"),
  authorityCode: optionalString,
  paymentRef: optionalString,
  collectedBy: optionalString,
});
export type CreateCollectionValues = z.infer<typeof createCollectionSchema>;

export const collectionFilterSchema = z.object({
  dateFrom: dateField("From date").optional(),
  dateTo: dateField("To date").optional(),
  headId: optionalNumber,
  page: z.coerce.number().int().nonnegative().default(0),
  size: z.coerce.number().int().positive().default(20),
});
export type CollectionFilterValues = z.infer<typeof collectionFilterSchema>;
