import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { ApiError } from "../../api/client";
import type {
  JournalEntryValues,
  BankReconciliationFilterValues,
  BankMatchValues,
  PoRequestValues,
  PoRequestActionValues,
} from "./zodSchema";

/**
 * M23 RMRS — TanStack Query hooks for the RMRS (Revenue Management & Revenue
 * Services) backend. Consumes the rmrs-service REST API.
 *
 * Endpoints (versioned under /v1):
 *   Journals     GET  /v1/journals               (list)
 *                POST /v1/journals               (create — validated double-entry)
 *                GET  /v1/journals/{id}          (read single)
 *                PUT  /v1/journals/{id}/post     (post to ledger)
 *   Bank         GET  /v1/bank/accounts          (list accounts)
 *   Recon        POST /v1/bank/reconciliation/statement  (import statement)
 *                GET  /v1/bank/reconciliation             (list matches/filter)
 *                POST /v1/bank/reconciliation/match       (match statement↔journal)
 *   PO Req       GET  /v1/po/requisitions        (list)
 *                POST /v1/po/requisitions        (create)
 *                GET  /v1/po/requisitions/{id}   (read)
 *                PUT  /v1/po/requisitions/{id}/action (submit/approve/reject)
 *                GET  /v1/po/departments         (master data)
 *                GET  /v1/po/items               (master data)
 */

const RMRS_BASE: string = import.meta.env.VITE_RMRS_API_BASE ?? "/api";

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

// ── Types ────────────────────────────────────────────────────────────────────

export interface JournalEntry {
  journalId: number;
  voucherNo: string;
  entryType: string;
  entryDate: string;
  narration: string;
  referenceType?: string;
  referenceId?: number;
  status: string;
  totalDebit: number;
  totalCredit: number;
  isPosted: boolean;
  postedAt?: string;
  createdBy: number;
  createdAt: string;
  legs: JournalLeg[];
}

export interface JournalLeg {
  legId: number;
  journalId: number;
  accountId: number;
  accountName: string;
  costCenterId?: number;
  costCenterName?: string;
  debit: number;
  credit: number;
  narration?: string;
}

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

export interface BankStatementRow {
  lineId: number;
  statementDate: string;
  description: string;
  referenceNo?: string;
  debitAmount: number;
  creditAmount: number;
  balance?: number;
  isMatched: boolean;
  matchedJournalId?: number;
  matchStatus: "UNMATCHED" | "MATCHED" | "DISPUTED" | "PARTIAL";
}

export interface BankReconciliation {
  statementId: number;
  bankAccountId: number;
  accountNumber: string;
  statementDate: string;
  fromDate: string;
  toDate: string;
  openingBalance: number;
  closingBalance: number;
  totalDebits: number;
  totalCredits: number;
  netChange: number;
  unmatchedCount: number;
  matchedCount: number;
  disputedCount: number;
  lines: BankStatementRow[];
}

export interface PoRequisition {
  requisitionId: number;
  requisitionNo: string;
  departmentId: number;
  departmentName: string;
  requestedBy: string;
  priority: string;
  status: "DRAFT" | "PENDING_APPROVAL" | "APPROVED" | "REJECTED" | "CANCELLED" | "ORDERED";
  justification: string;
  totalEstimatedAmount: number;
  expectedDeliveryDate?: string;
  vendorSuggestion?: string;
  createdBy: number;
  createdAt: string;
  updatedAt: string;
  lines: PoRequisitionLine[];
  approvals: PoApproval[];
}

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

// ── Query keys ─────────────────────────────────────────────────────────────

export const rmrsQueryKeys = {
  journals: ["rmrs", "journals"] as const,
  journal: (id: number) => ["rmrs", "journals", id] as const,
  bankAccounts: ["rmrs", "bank", "accounts"] as const,
  reconciliation: (filter: BankReconciliationFilterValues) => ["rmrs", "bank", "recon", filter] as const,
  statementRows: (statementId: number) => ["rmrs", "bank", "recon", statementId, "rows"] as const,
  poRequisitions: (filter?: PoRequisitionListFilter) => ["rmrs", "po", "requisitions", filter] as const,
  poRequisition: (id: number) => ["rmrs", "po", "requisitions", id] as const,
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

// ── Journal Entry queries ──────────────────────────────────────────────────

export function useJournalEntries() {
  return useQuery<JournalEntry[]>({
    queryKey: rmrsQueryKeys.journals,
    queryFn: () => rmrsJson<JournalEntry[]>(`${RMRS_BASE}/v1/journals`),
  });
}

export function useJournalEntry(id: number) {
  return useQuery<JournalEntry>({
    queryKey: rmrsQueryKeys.journal(id),
    queryFn: () => rmrsJson<JournalEntry>(`${RMRS_BASE}/v1/journals/${id}`),
    enabled: id > 0,
  });
}

export function useCreateJournalEntry() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (payload: JournalEntryValues) =>
      rmrsJson<JournalEntry>(`${RMRS_BASE}/v1/journals`, {
        method: "POST",
        body: JSON.stringify(payload),
      }),
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: ["rmrs", "journals"] });
    },
  });
}

export function usePostJournalEntry() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: number) =>
      rmrsJson<JournalEntry>(`${RMRS_BASE}/v1/journals/${id}/post`, { method: "PUT" }),
    onSuccess: (_, id) => {
      void qc.invalidateQueries({ queryKey: rmrsQueryKeys.journal(id) });
    },
  });
}

// ── Bank Reconciliation queries ──────────────────────────────────────────────

export function useBankAccounts() {
  return useQuery<BankAccount[]>({
    queryKey: rmrsQueryKeys.bankAccounts,
    queryFn: () => rmrsJson<BankAccount[]>(`${RMRS_BASE}/v1/bank/accounts`),
  });
}

export function useBankReconciliation(filter: BankReconciliationFilterValues) {
  return useQuery<BankReconciliation>({
    queryKey: rmrsQueryKeys.reconciliation(filter),
    queryFn: () => {
      const params = new URLSearchParams();
      params.set("bankAccountId", String(filter.bankAccountId));
      if (filter.fromDate) params.set("fromDate", filter.fromDate);
      if (filter.toDate) params.set("toDate", filter.toDate);
      params.set("status", filter.status);
      params.set("page", String(filter.page ?? 0));
      params.set("size", String(filter.size ?? 50));
      return rmrsJson<BankReconciliation>(`${RMRS_BASE}/v1/bank/reconciliation?${params}`);
    },
    enabled: filter.bankAccountId > 0,
  });
}

export function useImportStatement() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (payload: { bankAccountId: number; lines: unknown[] }) =>
      rmrsJson<{ statementId: number; importedRows: number }>(
        `${RMRS_BASE}/v1/bank/reconciliation/statement`,
        {
          method: "POST",
          body: JSON.stringify(payload),
        },
      ),
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: ["rmrs", "bank", "recon"] });
    },
  });
}

export function useMatchStatement() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (payload: BankMatchValues | BankMatchValues[]) =>
      rmrsJson<{ matchId: number; status: string }>(
        `${RMRS_BASE}/v1/bank/reconciliation/match`,
        {
          method: "POST",
          body: JSON.stringify(payload),
        },
      ),
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: ["rmrs", "bank", "recon"] });
    },
  });
}

// ── PO Requisition queries ──────────────────────────────────────────────────

export function usePoRequisitions(filter?: PoRequisitionListFilter) {
  return useQuery<PoRequisition[]>({
    queryKey: rmrsQueryKeys.poRequisitions(filter),
    queryFn: () => {
      const params = new URLSearchParams();
      if (filter?.status) params.set("status", filter.status);
      if (filter?.departmentId) params.set("departmentId", String(filter.departmentId));
      if (filter?.fromDate) params.set("fromDate", filter.fromDate);
      if (filter?.toDate) params.set("toDate", filter.toDate);
      params.set("page", String(filter?.page ?? 0));
      params.set("size", String(filter?.size ?? 20));
      return rmrsJson<PoRequisition[]>(`${RMRS_BASE}/v1/po/requisitions?${params}`);
    },
  });
}

export function usePoRequisition(id: number) {
  return useQuery<PoRequisition>({
    queryKey: rmrsQueryKeys.poRequisition(id),
    queryFn: () => rmrsJson<PoRequisition>(`${RMRS_BASE}/v1/po/requisitions/${id}`),
    enabled: id > 0,
  });
}

export function useCreatePoRequisition() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (payload: PoRequestValues) =>
      rmrsJson<PoRequisition>(`${RMRS_BASE}/v1/po/requisitions`, {
        method: "POST",
        body: JSON.stringify(payload),
      }),
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: ["rmrs", "po", "requisitions"] });
    },
  });
}

export function usePoRequisitionAction() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (payload: { id: number; action: PoRequestActionValues }) =>
      rmrsJson<PoRequisition>(`${RMRS_BASE}/v1/po/requisitions/${payload.id}/action`, {
        method: "PUT",
        body: JSON.stringify(payload.action),
      }),
    onSuccess: (_, { id }) => {
      void qc.invalidateQueries({ queryKey: rmrsQueryKeys.poRequisition(id) });
      void qc.invalidateQueries({ queryKey: ["rmrs", "po", "requisitions"] });
    },
  });
}

export function usePoDepartments() {
  return useQuery<PoDepartment[]>({
    queryKey: rmrsQueryKeys.poDepartments,
    queryFn: () => rmrsJson<PoDepartment[]>(`${RMRS_BASE}/v1/po/departments`),
  });
}

export function usePoItems(search?: string) {
  return useQuery<PoItem[]>({
    queryKey: rmrsQueryKeys.poItems(search),
    queryFn: () => {
      const params = search ? `?search=${encodeURIComponent(search)}` : "";
      return rmrsJson<PoItem[]>(`${RMRS_BASE}/v1/po/items${params}`);
    },
    staleTime: 5 * 60 * 1000,
  });
}
