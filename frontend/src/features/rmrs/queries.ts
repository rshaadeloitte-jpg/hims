import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { ApiError } from "../../api/client";
import type {
  JournalEntryValues,
  CreateHeadRequest,
  CreateAuthorityRequest,
  CreateLedgerRequest,
  ImportBankStatementValues,
  BankReconciliationFilterValues,
  BankMatchValues,
  PoRequestValues,
  PoRequestActionValues,
  CreateCollectionValues,
  CollectionFilterValues,
} from "./zodSchema";

/**
 * M23 RMRS (Revenue Management & Revenue Services) — TanStack Query hooks.
 * Serves real endpoints matching backend controllers:
 *   • RmrsM23Controller:
 *       GET/POST /api/v1/rmrs/journal-vouchers (?status=)
 *       GET      /api/v1/rmrs/journal-vouchers/{id}
 *       GET      /api/v1/rmrs/journal-vouchers/{id}/legs
 *       POST     /api/v1/rmrs/journal-vouchers/{id}/post
 *       POST     /api/v1/rmrs/journal-vouchers/{id}/reverse
 *       POST     /api/v1/rmrs/ledgers/{id}/suspense
 *       POST     /api/v1/rmrs/ledgers/{id}/suspense/clear
 *       GET/POST /api/v1/rmrs/bank-statements (?status=)
 *       POST     /api/v1/rmrs/bank-statements/{id}/reconcile
 *       POST     /api/v1/rmrs/bank-statements/{id}/dispute
 *       GET/POST /api/v1/rmrs/po-requests (?status=)
 *       GET      /api/v1/rmrs/po-requests/{id}
 *       POST     /api/v1/rmrs/po-requests/{id}/approve
 *       POST     /api/v1/rmrs/po-requests/{id}/reject
 *       POST     /api/v1/rmrs/po-requests/{id}/issue
 *   • MasterController:
 *       GET/POST /api/v1/masters/heads
 *       GET/POST /api/v1/masters/authorities
 *       GET/POST /api/v1/masters/ledgers
 *   • CollectionController:
 *       GET/POST /api/v1/collections (?dateFrom=&dateTo=&headId=&page=&size=)
 *       GET      /api/v1/collections/{id}
 *       GET      /api/v1/collections/summary/service-wise (?dateFrom=&dateTo=)
 *       GET      /api/v1/collections/summary/user-wise (?dateFrom=&dateTo=)
 */

const rawBase = import.meta.env.VITE_RMRS_API_BASE ?? "";
const RMRS_BASE = rawBase.endsWith("/api") ? rawBase.slice(0, -4) : rawBase;

async function rmrsJson<T>(url: string, options?: RequestInit): Promise<T> {
  const res = await fetch(url, {
    headers: { "Content-Type": "application/json", "X-Request-ID": crypto.randomUUID() },
    ...options,
  });
  if (!res.ok) {
    const err = await res.json().catch(() => ({ message: res.statusText }));
    throw new ApiError(res.status, err.errorCode ?? "UNKNOWN", err.message ?? "Request failed");
  }
  return res.json();
}

// ── Domain Types ─────────────────────────────────────────────────────────────

export interface HeadMaster {
  id: number;
  headId: number;
  headCode: string;
  headName?: string;
  isActive?: boolean;
}

export interface AuthorityMaster {
  id: number;
  authorityId: number;
  authorityCode: string;
  authorityName?: string;
  headId?: number;
}

export interface LedgerMaster {
  id: number;
  ledgerId: number;
  ledgerCode: string;
  ledgerName?: string;
  headId: number;
  suspense?: boolean;
  suspenseReason?: string;
}

export interface JournalVoucher {
  id: number;
  voucherId?: number;
  voucherNo: string;
  narration?: string;
  voucherDate: string;
  docStatus: number; // 1 = DRAFT, 2 = POSTED, 3 = REVERSED
  postedBy?: number;
  postedDatetime?: string;
  reversedBy?: number;
  reversedDatetime?: string;
}
export type JournalEntry = JournalVoucher;

export interface JournalVoucherLeg {
  id: number;
  legId?: number;
  voucherId: number;
  ledgerId: number;
  debit: number;
  credit: number;
}
export type JournalLeg = JournalVoucherLeg;

export interface BankStatement {
  id: number;
  statementLineId?: number;
  bankLedgerId: number;
  txnDate: string;
  amount: number;
  direction: string;
  description?: string;
  externalRef?: string;
  docStatus: number; // 1 = IMPORTED, 2 = RECONCILED, 3 = DISPUTED
  matchedEntryId?: number;
  matchedCollectionId?: number;
}
export type BankStatementRow = BankStatement;
export type BankReconciliation = BankStatement[];

export interface BankAccount {
  accountId: number;
  accountName: string;
  accountNumber: string;
  bankName: string;
  branchName?: string;
  ifscCode?: string;
  accountType: "SAVINGS" | "CURRENT" | "CA";
  openingBalance: number;
  currency: string;
}

export interface PoRequest {
  id: number;
  requestId?: number;
  requestNo: string;
  requestingDeptId?: number;
  headId: number;
  ledgerId?: number;
  amount: number;
  remarks?: string;
  docStatus: number; // 1 = NEW, 2 = APPROVED, 3 = ISSUED, 4 = REJECTED
  approvedBy?: number;
  approvedDatetime?: string;
  issuedPoId?: number;
}
export type PoRequisition = PoRequest;

export interface PoRequisitionLine {
  lineId: number;
  requisitionId: number;
  itemId: number;
  itemName: string;
  uom: string;
  quantity: number;
  estimatedRate: number;
  lineTotal: number;
  specifications?: string;
  remarks?: string;
}

export interface PoApproval {
  approvalId: number;
  requisitionId: number;
  approverId: number;
  approverName: string;
  action: string;
  status: "PENDING" | "APPROVED" | "REJECTED";
  remarks?: string;
  createdAt: string;
}

export interface PoDepartment {
  departmentId: number;
  departmentName: string;
  headName?: string;
  headEmail?: string;
}

export interface PoItem {
  itemId: number;
  itemCode: string;
  itemName: string;
  uom: string;
  estimatedRate: number;
  category: string;
  description?: string;
}

export interface RmrsCollection {
  id: number;
  collectionId: number;
  receiptNo: string;
  collectionDate: string;
  patientId?: number;
  amount: number;
  headId: number;
  ledgerId: number;
  authorityId?: number;
  paymentRef?: string;
  counter?: string;
  collectedBy?: string;
}

export interface ServiceWiseSummaryRow {
  headId: number;
  headCode: string;
  headName?: string;
  totalAmount: number;
  collectionCount: number;
}

export interface UserWiseSummaryRow {
  collectedBy: string;
  totalAmount: number;
  collectionCount: number;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

// ── Query Keys ───────────────────────────────────────────────────────────────

export const rmrsQueryKeys = {
  journals: (status?: number) => ["rmrs", "journal-vouchers", status] as const,
  journal: (id: number) => ["rmrs", "journal-vouchers", id] as const,
  journalLegs: (id: number) => ["rmrs", "journal-vouchers", id, "legs"] as const,
  heads: ["rmrs", "masters", "heads"] as const,
  authorities: ["rmrs", "masters", "authorities"] as const,
  ledgers: ["rmrs", "masters", "ledgers"] as const,
  collections: (filter?: CollectionFilterValues) => ["rmrs", "collections", filter] as const,
  collection: (id: number) => ["rmrs", "collections", id] as const,
  summaryServiceWise: (dateFrom?: string, dateTo?: string) =>
    ["rmrs", "collections", "summary-service-wise", dateFrom, dateTo] as const,
  summaryUserWise: (dateFrom?: string, dateTo?: string) =>
    ["rmrs", "collections", "summary-user-wise", dateFrom, dateTo] as const,
  bankStatements: (status?: number) => ["rmrs", "bank-statements", status] as const,
  bankAccounts: ["rmrs", "bank", "accounts"] as const,
  reconciliation: (filter: BankReconciliationFilterValues) => ["rmrs", "bank", "recon", filter] as const,
  statementRows: (statementId: number) => ["rmrs", "bank", "recon", statementId, "rows"] as const,
  poRequisitions: (filter?: PoRequisitionListFilter | number) => ["rmrs", "po-requests", filter] as const,
  poRequisition: (id: number) => ["rmrs", "po-requests", id] as const,
  poDepartments: ["rmrs", "po", "departments"] as const,
  poItems: (search?: string) => ["rmrs", "po", "items", search] as const,
};

export interface PoRequisitionListFilter {
  status?: string;
  departmentId?: number;
  fromDate?: string;
  toDate?: string;
  page?: number;
  size?: number;
}

// ── Helpers ──────────────────────────────────────────────────────────────────

function normalizeJournal(j: any): JournalVoucher {
  const id = (j?.id ?? j?.voucherId) as number;
  return {
    ...j,
    id,
    voucherId: id,
    voucherNo: (j?.voucherNo as string) ?? "",
    voucherDate: (j?.voucherDate as string) ?? "",
    docStatus: (j?.docStatus as number) ?? 1,
  } as JournalVoucher;
}

function normalizeLedger(l: any): LedgerMaster {
  const id = (l?.id ?? l?.ledgerId) as number;
  return {
    ...l,
    id,
    ledgerId: id,
    ledgerCode: (l?.ledgerCode as string) ?? "",
    headId: (l?.headId as number) ?? 0,
  } as LedgerMaster;
}

function normalizeHead(h: any): HeadMaster {
  const id = (h?.id ?? h?.headId) as number;
  return {
    ...h,
    id,
    headId: id,
    headCode: (h?.headCode as string) ?? "",
  } as HeadMaster;
}

function normalizeAuthority(a: any): AuthorityMaster {
  const id = (a?.id ?? a?.authorityId) as number;
  return {
    ...a,
    id,
    authorityId: id,
    authorityCode: (a?.authorityCode as string) ?? "",
  } as AuthorityMaster;
}

function normalizeCollection(c: any): RmrsCollection {
  const id = (c?.id ?? c?.collectionId) as number;
  return {
    ...c,
    id,
    collectionId: id,
    receiptNo: (c?.receiptNo as string) ?? "",
    collectionDate: (c?.collectionDate as string) ?? "",
    amount: (c?.amount as number) ?? 0,
    headId: (c?.headId as number) ?? 0,
    ledgerId: (c?.ledgerId as number) ?? 0,
  } as RmrsCollection;
}

function normalizePoRequest(p: any): PoRequest {
  const id = (p?.id ?? p?.requestId) as number;
  return {
    ...p,
    id,
    requestId: id,
    requestNo: (p?.requestNo as string) ?? "",
    headId: (p?.headId as number) ?? 0,
    amount: (p?.amount as number) ?? 0,
    docStatus: (p?.docStatus as number) ?? 1,
  } as PoRequest;
}

function normalizeBankStatement(b: any): BankStatement {
  const id = (b?.id ?? b?.statementLineId) as number;
  return {
    ...b,
    id,
    statementLineId: id,
    bankLedgerId: (b?.bankLedgerId as number) ?? 0,
    txnDate: (b?.txnDate as string) ?? "",
    amount: (b?.amount as number) ?? 0,
    direction: (b?.direction as string) ?? "DR",
    docStatus: (b?.docStatus as number) ?? 1,
  } as BankStatement;
}

// ── Journal Voucher Queries (RmrsM23Controller.java) ────────────────────────

export function useJournalEntries(status?: number) {
  return useQuery<JournalVoucher[]>({
    queryKey: rmrsQueryKeys.journals(status),
    queryFn: async () => {
      const url = `${RMRS_BASE}/api/v1/rmrs/journal-vouchers${status != null ? `?status=${status}` : ""}`;
      const data = await rmrsJson<Record<string, unknown>[]>(url);
      return Array.isArray(data) ? data.map(normalizeJournal) : [];
    },
  });
}

export function useJournalEntry(id: number) {
  return useQuery<JournalVoucher>({
    queryKey: rmrsQueryKeys.journal(id),
    queryFn: async () => {
      const data = await rmrsJson<Record<string, unknown>>(`${RMRS_BASE}/api/v1/rmrs/journal-vouchers/${id}`);
      return normalizeJournal(data);
    },
    enabled: id > 0,
  });
}

export function useJournalVoucherLegs(id: number) {
  return useQuery<JournalVoucherLeg[]>({
    queryKey: rmrsQueryKeys.journalLegs(id),
    queryFn: () => rmrsJson<JournalVoucherLeg[]>(`${RMRS_BASE}/api/v1/rmrs/journal-vouchers/${id}/legs`),
    enabled: id > 0,
  });
}

export function useCreateJournalEntry() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (payload: JournalEntryValues | Record<string, unknown>) => {
      const rawLegs = (payload as { legs?: Array<Record<string, unknown>> }).legs ?? [];
      const body = {
        narration: (payload as { narration?: string }).narration,
        voucherDate:
          (payload as { voucherDate?: string }).voucherDate ??
          (payload as { entryDate?: string }).entryDate ??
          new Date().toISOString().slice(0, 10),
        legs: rawLegs.map((l) => ({
          ledgerId: l.ledgerId ?? l.accountId,
          debit: l.debit ?? 0,
          credit: l.credit ?? 0,
        })),
      };
      return rmrsJson<JournalVoucher>(`${RMRS_BASE}/api/v1/rmrs/journal-vouchers`, {
        method: "POST",
        body: JSON.stringify(body),
      }).then(normalizeJournal);
    },
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: ["rmrs", "journal-vouchers"] });
    },
  });
}
export const useCreateJournalVoucher = useCreateJournalEntry;

export function usePostJournalEntry() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: number) =>
      rmrsJson<JournalVoucher>(`${RMRS_BASE}/api/v1/rmrs/journal-vouchers/${id}/post`, {
        method: "POST",
      }).then(normalizeJournal),
    onSuccess: (_, id) => {
      void qc.invalidateQueries({ queryKey: rmrsQueryKeys.journal(id) });
      void qc.invalidateQueries({ queryKey: ["rmrs", "journal-vouchers"] });
    },
  });
}
export const usePostJournalVoucher = usePostJournalEntry;

export function useReverseJournalEntry() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: number) =>
      rmrsJson<JournalVoucher>(`${RMRS_BASE}/api/v1/rmrs/journal-vouchers/${id}/reverse`, {
        method: "POST",
      }).then(normalizeJournal),
    onSuccess: (_, id) => {
      void qc.invalidateQueries({ queryKey: rmrsQueryKeys.journal(id) });
      void qc.invalidateQueries({ queryKey: ["rmrs", "journal-vouchers"] });
    },
  });
}
export const useReverseJournalVoucher = useReverseJournalEntry;

// ── Treasury Masters Queries (MasterController.java) ────────────────────────

export function useTreasuryHeads() {
  return useQuery<HeadMaster[]>({
    queryKey: rmrsQueryKeys.heads,
    queryFn: async () => {
      const data = await rmrsJson<Record<string, unknown>[]>(`${RMRS_BASE}/api/v1/masters/heads`);
      return Array.isArray(data) ? data.map(normalizeHead) : [];
    },
  });
}

export function useCreateTreasuryHead() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (payload: CreateHeadRequest) =>
      rmrsJson<HeadMaster>(`${RMRS_BASE}/api/v1/masters/heads`, {
        method: "POST",
        body: JSON.stringify(payload),
      }).then(normalizeHead),
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: rmrsQueryKeys.heads });
    },
  });
}

export function useTreasuryAuthorities() {
  return useQuery<AuthorityMaster[]>({
    queryKey: rmrsQueryKeys.authorities,
    queryFn: async () => {
      const data = await rmrsJson<Record<string, unknown>[]>(`${RMRS_BASE}/api/v1/masters/authorities`);
      return Array.isArray(data) ? data.map(normalizeAuthority) : [];
    },
  });
}

export function useCreateTreasuryAuthority() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (payload: CreateAuthorityRequest) =>
      rmrsJson<AuthorityMaster>(`${RMRS_BASE}/api/v1/masters/authorities`, {
        method: "POST",
        body: JSON.stringify(payload),
      }).then(normalizeAuthority),
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: rmrsQueryKeys.authorities });
    },
  });
}

export function useTreasuryLedgers() {
  return useQuery<LedgerMaster[]>({
    queryKey: rmrsQueryKeys.ledgers,
    queryFn: async () => {
      const data = await rmrsJson<Record<string, unknown>[]>(`${RMRS_BASE}/api/v1/masters/ledgers`);
      return Array.isArray(data) ? data.map(normalizeLedger) : [];
    },
  });
}

export function useCreateTreasuryLedger() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (payload: CreateLedgerRequest) =>
      rmrsJson<LedgerMaster>(`${RMRS_BASE}/api/v1/masters/ledgers`, {
        method: "POST",
        body: JSON.stringify(payload),
      }).then(normalizeLedger),
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: rmrsQueryKeys.ledgers });
    },
  });
}

export function useMarkSuspense() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, reason }: { id: number; reason?: string }) =>
      rmrsJson<LedgerMaster>(`${RMRS_BASE}/api/v1/rmrs/ledgers/${id}/suspense`, {
        method: "POST",
        body: JSON.stringify({ reason: reason ?? null }),
      }).then(normalizeLedger),
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: rmrsQueryKeys.ledgers });
    },
  });
}

export function useClearSuspense() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: number) =>
      rmrsJson<LedgerMaster>(`${RMRS_BASE}/api/v1/rmrs/ledgers/${id}/suspense/clear`, {
        method: "POST",
      }).then(normalizeLedger),
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: rmrsQueryKeys.ledgers });
    },
  });
}

// ── Bank Statement Queries (RmrsM23Controller.java) ─────────────────────────

export function useBankStatements(status?: number) {
  return useQuery<BankStatement[]>({
    queryKey: rmrsQueryKeys.bankStatements(status),
    queryFn: async () => {
      const url = `${RMRS_BASE}/api/v1/rmrs/bank-statements${status != null ? `?status=${status}` : ""}`;
      const data = await rmrsJson<Record<string, unknown>[]>(url);
      return Array.isArray(data) ? data.map(normalizeBankStatement) : [];
    },
  });
}

export function useBankAccounts() {
  return useQuery<BankAccount[]>({
    queryKey: rmrsQueryKeys.bankAccounts,
    queryFn: async () => {
      const ledgers = await rmrsJson<Record<string, unknown>[]>(`${RMRS_BASE}/api/v1/masters/ledgers`);
      return (Array.isArray(ledgers) ? ledgers : []).map((l, index) => {
        const id = (l.id ?? l.ledgerId ?? index + 1) as number;
        return {
          accountId: id,
          accountName: (l.ledgerName as string) ?? `Ledger ${id}`,
          accountNumber: (l.ledgerCode as string) ?? `L-${id}`,
          bankName: "Treasury",
          accountType: "CURRENT",
          openingBalance: 0,
          currency: "INR",
        };
      });
    },
  });
}

export function useBankReconciliation(filter: BankReconciliationFilterValues) {
  return useQuery<BankReconciliation>({
    queryKey: rmrsQueryKeys.reconciliation(filter),
    queryFn: async () => {
      let statusParam = "";
      if (filter.status === "IMPORTED" || filter.status === "UNMATCHED") statusParam = "?status=1";
      else if (filter.status === "RECONCILED" || filter.status === "MATCHED") statusParam = "?status=2";
      else if (filter.status === "DISPUTED") statusParam = "?status=3";
      const data = await rmrsJson<Record<string, unknown>[]>(`${RMRS_BASE}/api/v1/rmrs/bank-statements${statusParam}`);
      return (Array.isArray(data) ? data : []).map(normalizeBankStatement);
    },
  });
}

export function useImportBankStatement() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (payload: ImportBankStatementValues | { bankAccountId: number; lines?: unknown[] }) => {
      const bankLedgerId =
        (payload as ImportBankStatementValues).bankLedgerId ??
        (payload as { bankAccountId: number }).bankAccountId;
      const body = {
        bankLedgerId,
        txnDate: (payload as ImportBankStatementValues).txnDate ?? new Date().toISOString().slice(0, 10),
        amount: (payload as ImportBankStatementValues).amount ?? 1,
        direction: (payload as ImportBankStatementValues).direction ?? "DR",
        description: (payload as ImportBankStatementValues).description ?? null,
        externalRef: (payload as ImportBankStatementValues).externalRef ?? null,
      };
      return rmrsJson<BankStatement>(`${RMRS_BASE}/api/v1/rmrs/bank-statements`, {
        method: "POST",
        body: JSON.stringify(body),
      }).then(normalizeBankStatement);
    },
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: ["rmrs", "bank-statements"] });
      void qc.invalidateQueries({ queryKey: ["rmrs", "bank", "recon"] });
    },
  });
}
export const useImportStatement = useImportBankStatement;

export function useReconcileBankStatement() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (payload: { id?: number; statementLineId?: number; matchedEntryId?: number; matchedCollectionId?: number } | BankMatchValues) => {
      const lineId =
        (payload as { id?: number }).id ??
        (payload as { statementLineId?: number }).statementLineId ??
        0;
      const body = {
        matchedEntryId:
          (payload as { matchedEntryId?: number }).matchedEntryId ??
          (payload as { journalEntryId?: number }).journalEntryId ??
          null,
        matchedCollectionId: (payload as { matchedCollectionId?: number }).matchedCollectionId ?? null,
      };
      return rmrsJson<BankStatement>(`${RMRS_BASE}/api/v1/rmrs/bank-statements/${lineId}/reconcile`, {
        method: "POST",
        body: JSON.stringify(body),
      }).then(normalizeBankStatement);
    },
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: ["rmrs", "bank-statements"] });
      void qc.invalidateQueries({ queryKey: ["rmrs", "bank", "recon"] });
    },
  });
}
export const useMatchStatement = useReconcileBankStatement;

export function useDisputeBankStatement() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, reason }: { id: number; reason?: string }) =>
      rmrsJson<BankStatement>(`${RMRS_BASE}/api/v1/rmrs/bank-statements/${id}/dispute`, {
        method: "POST",
        body: JSON.stringify({ reason: reason ?? null }),
      }).then(normalizeBankStatement),
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: ["rmrs", "bank-statements"] });
      void qc.invalidateQueries({ queryKey: ["rmrs", "bank", "recon"] });
    },
  });
}

// ── PO Request Queries (RmrsM23Controller.java) ─────────────────────────────

export function usePoRequisitions(filter?: PoRequisitionListFilter | number) {
  return useQuery<PoRequisition[]>({
    queryKey: rmrsQueryKeys.poRequisitions(filter),
    queryFn: async () => {
      let statusParam = "";
      if (typeof filter === "number") {
        statusParam = `?status=${filter}`;
      } else if (filter?.status) {
        const s = filter.status.toUpperCase();
        const code = s === "NEW" || s === "DRAFT" ? 1 : s === "APPROVED" ? 2 : s === "ISSUED" ? 3 : s === "REJECTED" ? 4 : undefined;
        if (code != null) statusParam = `?status=${code}`;
      }
      const data = await rmrsJson<Record<string, unknown>[]>(`${RMRS_BASE}/api/v1/rmrs/po-requests${statusParam}`);
      return (Array.isArray(data) ? data : []).map(normalizePoRequest);
    },
  });
}

export function usePoRequisition(id: number) {
  return useQuery<PoRequisition>({
    queryKey: rmrsQueryKeys.poRequisition(id),
    queryFn: async () => {
      const data = await rmrsJson<Record<string, unknown>>(`${RMRS_BASE}/api/v1/rmrs/po-requests/${id}`);
      return normalizePoRequest(data);
    },
    enabled: id > 0,
  });
}

export function useCreatePoRequisition() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (payload: PoRequestValues | Record<string, unknown>) => {
      const body = {
        requestingDeptId:
          (payload as { requestingDeptId?: number }).requestingDeptId ??
          (payload as { departmentId?: number }).departmentId ??
          null,
        headId: (payload as { headId?: number }).headId ?? 1,
        ledgerId: (payload as { ledgerId?: number }).ledgerId ?? null,
        amount:
          (payload as { amount?: number }).amount ??
          (payload as { totalEstimatedAmount?: number }).totalEstimatedAmount ??
          1,
        remarks:
          (payload as { remarks?: string }).remarks ??
          (payload as { justification?: string }).justification ??
          null,
      };
      return rmrsJson<PoRequisition>(`${RMRS_BASE}/api/v1/rmrs/po-requests`, {
        method: "POST",
        body: JSON.stringify(body),
      }).then(normalizePoRequest);
    },
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: ["rmrs", "po-requests"] });
    },
  });
}

export function useApprovePoRequisition() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: number) =>
      rmrsJson<PoRequisition>(`${RMRS_BASE}/api/v1/rmrs/po-requests/${id}/approve`, {
        method: "POST",
      }).then(normalizePoRequest),
    onSuccess: (_, id) => {
      void qc.invalidateQueries({ queryKey: rmrsQueryKeys.poRequisition(id) });
      void qc.invalidateQueries({ queryKey: ["rmrs", "po-requests"] });
    },
  });
}

export function useRejectPoRequisition() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: number) =>
      rmrsJson<PoRequisition>(`${RMRS_BASE}/api/v1/rmrs/po-requests/${id}/reject`, {
        method: "POST",
      }).then(normalizePoRequest),
    onSuccess: (_, id) => {
      void qc.invalidateQueries({ queryKey: rmrsQueryKeys.poRequisition(id) });
      void qc.invalidateQueries({ queryKey: ["rmrs", "po-requests"] });
    },
  });
}

export function useIssuePoRequisition() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, issuedPoId }: { id: number; issuedPoId?: number }) =>
      rmrsJson<PoRequisition>(`${RMRS_BASE}/api/v1/rmrs/po-requests/${id}/issue`, {
        method: "POST",
        body: JSON.stringify({ issuedPoId: issuedPoId ?? null }),
      }).then(normalizePoRequest),
    onSuccess: (_, { id }) => {
      void qc.invalidateQueries({ queryKey: rmrsQueryKeys.poRequisition(id) });
      void qc.invalidateQueries({ queryKey: ["rmrs", "po-requests"] });
    },
  });
}

export function usePoRequisitionAction() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (payload: { id: number; action: PoRequestActionValues | string }) => {
      const actionStr = typeof payload.action === "string" ? payload.action : payload.action.action;
      const a = actionStr.toLowerCase();
      const endpoint = a.includes("reject") ? "reject" : a.includes("issue") ? "issue" : "approve";
      const body = typeof payload.action === "object" ? payload.action : {};
      return rmrsJson<PoRequisition>(`${RMRS_BASE}/api/v1/rmrs/po-requests/${payload.id}/${endpoint}`, {
        method: "POST",
        body: JSON.stringify(body),
      }).then(normalizePoRequest);
    },
    onSuccess: (_, { id }) => {
      void qc.invalidateQueries({ queryKey: rmrsQueryKeys.poRequisition(id) });
      void qc.invalidateQueries({ queryKey: ["rmrs", "po-requests"] });
    },
  });
}

export function usePoDepartments() {
  return useQuery<PoDepartment[]>({
    queryKey: rmrsQueryKeys.poDepartments,
    queryFn: async () => {
      const heads = await rmrsJson<Record<string, unknown>[]>(`${RMRS_BASE}/api/v1/masters/heads`);
      return (Array.isArray(heads) ? heads : []).map((h) => {
        const id = (h.id ?? h.headId) as number;
        return {
          departmentId: id,
          departmentName: (h.headName as string) ?? (h.headCode as string) ?? `Head ${id}`,
        };
      });
    },
  });
}

export function usePoItems(search?: string) {
  return useQuery<PoItem[]>({
    queryKey: rmrsQueryKeys.poItems(search),
    queryFn: async () => {
      const ledgers = await rmrsJson<Record<string, unknown>[]>(`${RMRS_BASE}/api/v1/masters/ledgers`);
      return (Array.isArray(ledgers) ? ledgers : []).map((l) => {
        const id = (l.id ?? l.ledgerId) as number;
        return {
          itemId: id,
          itemCode: (l.ledgerCode as string) ?? `L-${id}`,
          itemName: (l.ledgerName as string) ?? `Ledger ${id}`,
          uom: "EA",
          estimatedRate: 100,
          category: "General",
        };
      });
    },
    staleTime: 5 * 60 * 1000,
  });
}

// ── Collection Queries (CollectionController.java) ──────────────────────────

export function useCollections(filter?: CollectionFilterValues) {
  return useQuery<PageResponse<RmrsCollection> | RmrsCollection[]>({
    queryKey: rmrsQueryKeys.collections(filter),
    queryFn: async () => {
      const params = new URLSearchParams();
      if (filter?.dateFrom) params.set("dateFrom", filter.dateFrom);
      if (filter?.dateTo) params.set("dateTo", filter.dateTo);
      if (filter?.headId != null) params.set("headId", String(filter.headId));
      if (filter?.page != null) params.set("page", String(filter.page));
      if (filter?.size != null) params.set("size", String(filter.size));
      const qs = params.toString();
      const url = `${RMRS_BASE}/api/v1/collections${qs ? `?${qs}` : ""}`;
      const data = await rmrsJson<any>(url);
      if (data && Array.isArray(data.content)) {
        return {
          ...data,
          content: data.content.map(normalizeCollection),
        };
      }
      return Array.isArray(data) ? data.map(normalizeCollection) : [];
    },
  });
}

export function useCollection(id: number) {
  return useQuery<RmrsCollection>({
    queryKey: rmrsQueryKeys.collection(id),
    queryFn: async () => {
      const data = await rmrsJson<Record<string, unknown>>(`${RMRS_BASE}/api/v1/collections/${id}`);
      return normalizeCollection(data);
    },
    enabled: id > 0,
  });
}

export function useCreateCollection() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (payload: CreateCollectionValues) =>
      rmrsJson<RmrsCollection>(`${RMRS_BASE}/api/v1/collections`, {
        method: "POST",
        body: JSON.stringify(payload),
      }).then(normalizeCollection),
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: ["rmrs", "collections"] });
    },
  });
}

export function useServiceWiseSummary(dateFrom?: string, dateTo?: string) {
  return useQuery<ServiceWiseSummaryRow[]>({
    queryKey: rmrsQueryKeys.summaryServiceWise(dateFrom, dateTo),
    queryFn: () => {
      const params = new URLSearchParams();
      if (dateFrom) params.set("dateFrom", dateFrom);
      if (dateTo) params.set("dateTo", dateTo);
      const qs = params.toString();
      return rmrsJson<ServiceWiseSummaryRow[]>(
        `${RMRS_BASE}/api/v1/collections/summary/service-wise${qs ? `?${qs}` : ""}`,
      );
    },
  });
}

export function useUserWiseSummary(dateFrom?: string, dateTo?: string) {
  return useQuery<UserWiseSummaryRow[]>({
    queryKey: rmrsQueryKeys.summaryUserWise(dateFrom, dateTo),
    queryFn: () => {
      const params = new URLSearchParams();
      if (dateFrom) params.set("dateFrom", dateFrom);
      if (dateTo) params.set("dateTo", dateTo);
      const qs = params.toString();
      return rmrsJson<UserWiseSummaryRow[]>(
        `${RMRS_BASE}/api/v1/collections/summary/user-wise${qs ? `?${qs}` : ""}`,
      );
    },
  });
}
