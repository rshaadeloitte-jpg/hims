import { describe, expect, it } from "vitest";
import {
  createHeadSchema,
  createAuthoritySchema,
  createLedgerSchema,
  markSuspenseSchema,
  createJournalVoucherSchema,
  journalVoucherLegSchema,
  importBankStatementSchema,
  reconcileBankStatementSchema,
  disputeBankStatementSchema,
  createPoRequestSchema,
  createCollectionSchema,
  collectionFilterSchema,
} from "../zodSchema";

describe("RMRS Zod Schemas & DTO Validation", () => {
  describe("Treasury Masters (MasterController.java)", () => {
    it("validates valid createHeadSchema", () => {
      const result = createHeadSchema.safeParse({ headCode: "H001", headName: "OPD Fees" });
      expect(result.success).toBe(true);
    });

    it("rejects empty headCode in createHeadSchema", () => {
      const result = createHeadSchema.safeParse({ headCode: "", headName: "OPD Fees" });
      expect(result.success).toBe(false);
    });

    it("validates createAuthoritySchema", () => {
      const result = createAuthoritySchema.safeParse({
        authorityCode: "AUTH_SBI",
        authorityName: "State Bank of India",
        headId: 1,
      });
      expect(result.success).toBe(true);
    });

    it("validates createLedgerSchema with headId requirement", () => {
      const valid = createLedgerSchema.safeParse({ ledgerCode: "L001", ledgerName: "Main Cash", headId: 2 });
      expect(valid.success).toBe(true);
      const invalid = createLedgerSchema.safeParse({ ledgerCode: "L001", ledgerName: "Main Cash" });
      expect(invalid.success).toBe(false);
    });

    it("validates markSuspenseSchema", () => {
      const result = markSuspenseSchema.safeParse({ reason: "Unidentified deposit" });
      expect(result.success).toBe(true);
    });
  });

  describe("Journal Vouchers (RmrsM23Controller.java)", () => {
    it("validates journal leg with ledgerId and debit > 0", () => {
      const leg = journalVoucherLegSchema.safeParse({ ledgerId: 10, debit: 250, credit: 0 });
      expect(leg.success).toBe(true);
    });

    it("rejects journal leg when both debit and credit are positive", () => {
      const leg = journalVoucherLegSchema.safeParse({ ledgerId: 10, debit: 250, credit: 250 });
      expect(leg.success).toBe(false);
    });

    it("validates balanced journal voucher", () => {
      const jv = createJournalVoucherSchema.safeParse({
        narration: "Transfer",
        voucherDate: "2026-11-11",
        legs: [
          { ledgerId: 1, debit: 500, credit: 0 },
          { ledgerId: 2, debit: 0, credit: 500 },
        ],
      });
      expect(jv.success).toBe(true);
    });

    it("rejects unbalanced journal voucher", () => {
      const jv = createJournalVoucherSchema.safeParse({
        narration: "Transfer",
        voucherDate: "2026-11-11",
        legs: [
          { ledgerId: 1, debit: 500, credit: 0 },
          { ledgerId: 2, debit: 0, credit: 400 },
        ],
      });
      expect(jv.success).toBe(false);
    });
  });

  describe("Bank Statements (RmrsM23Controller.java)", () => {
    it("validates importBankStatementSchema", () => {
      const result = importBankStatementSchema.safeParse({
        bankLedgerId: 5,
        txnDate: "2026-11-11",
        amount: 1000,
        direction: "CR",
        description: "Direct Deposit",
      });
      expect(result.success).toBe(true);
    });

    it("validates reconcileBankStatementSchema", () => {
      const result = reconcileBankStatementSchema.safeParse({
        matchedEntryId: 101,
        matchedCollectionId: 202,
      });
      expect(result.success).toBe(true);
    });

    it("validates disputeBankStatementSchema", () => {
      const result = disputeBankStatementSchema.safeParse({ reason: "Unrecognized fee" });
      expect(result.success).toBe(true);
    });
  });

  describe("PO Requests (RmrsM23Controller.java)", () => {
    it("validates createPoRequestSchema", () => {
      const result = createPoRequestSchema.safeParse({
        headId: 3,
        amount: 12000,
        remarks: "Surgical gloves",
      });
      expect(result.success).toBe(true);
    });
  });

  describe("Collections (CollectionController.java)", () => {
    it("validates createCollectionSchema", () => {
      const result = createCollectionSchema.safeParse({
        receiptNo: "REC-2026-001",
        collectionDate: "2026-11-11",
        amount: 750,
        headCode: "H001",
        ledgerCode: "L001",
      });
      expect(result.success).toBe(true);
    });

    it("validates collectionFilterSchema", () => {
      const result = collectionFilterSchema.safeParse({
        dateFrom: "2026-11-01",
        dateTo: "2026-11-11",
        headId: 1,
        page: 0,
        size: 50,
      });
      expect(result.success).toBe(true);
    });
  });
});
