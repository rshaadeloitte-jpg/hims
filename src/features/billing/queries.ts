import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { ApiError } from "../../api/client";

/**
 * M04 Billing hooks — consume the billing-service REST API (Spring Boot :8098).
 * Uses the Vite proxy (VITE_BILLING_API_BASE) or production gateway.
 */

const BILLING_BASE: string = import.meta.env.VITE_BILLING_API_BASE ?? "/api";

async function billingJson<T>(url: string, options?: RequestInit): Promise<T> {
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

// ---- Types -----------------------------------------------------------------

export interface Bill {
  billId: number;
  patientId: number;
  encounterId?: number;
  admissionId?: number;
  cashCounterId: number;
  prescriberDoctorId?: number;
  isSelfCompany: boolean;
  billNo: string;
  isInterim: boolean;
  isSettled: boolean;
  isCancelled: boolean;
  totalAmount: number;
  taxAmount: number;
  concessionAmount: number;
  selfAmount: number;
  nonSelfAmount: number;
  balanceSelf: number;
  balanceNonSelf: number;
  changeAmount: number;
  finalizedAt?: string;
  sponsorCompanyId?: number;
  sponsorPlanId?: number;
  charges?: Charge[];
}

export interface Charge {
  chargeId: number;
  billId?: number;
  serviceId: number;
  serviceName: string;
  rate: number;
  qty: number;
  amount: number;
  taxAmount: number;
  totalAmount: number;
  isCancelled: boolean;
}

export interface Payment {
  paymentId: number;
  billId: number;
  amount: number;
  paymentMethod: string;
  referenceNo?: string;
  remarks?: string;
  createdAt: string;
}

export interface CreditNote {
  creditNoteId: number;
  billId: number;
  amount: number;
  reason: string;
  createdAt: string;
}

export interface DebitNote {
  debitNoteId: number;
  billId: number;
  amount: number;
  reason: string;
  createdAt: string;
  status: number;
}

export interface BillFilter {
  patientId?: number;
  fromDate?: string;
  toDate?: string;
  isSettled?: boolean;
  page?: number;
  size?: number;
}

// ---- Query keys ------------------------------------------------------------

export const billingQueryKeys = {
  bills: (filter: BillFilter) => ["bills", filter] as const,
  bill: (id: number) => ["bills", id] as const,
  charges: (billId: number) => ["bills", billId, "charges"] as const,
  payments: (billId: number) => ["bills", billId, "payments"] as const,
  creditNotes: (billId: number) => ["bills", billId, "credit-notes"] as const,
  debitNotes: (billId: number) => ["bills", billId, "debit-notes"] as const,
  patientSearch: (query: string) => ["patients", "search", query] as const,
};

// ---- Bill list -------------------------------------------------------------

export function useBills(filter: BillFilter) {
  return useQuery<Bill[]>({
    queryKey: billingQueryKeys.bills(filter),
    queryFn: async () => {
      const params = new URLSearchParams();
      if (filter.patientId) params.set("patientId", String(filter.patientId));
      if (filter.fromDate) params.set("fromDate", filter.fromDate);
      if (filter.toDate) params.set("toDate", filter.toDate);
      if (filter.isSettled !== undefined) params.set("isSettled", String(filter.isSettled));
      params.set("page", String(filter.page ?? 0));
      params.set("size", String(filter.size ?? 20));
      return billingJson<Bill[]>(`${BILLING_BASE}/api/v1/bills?${params}`);
    },
  });
}

// ---- Single bill -----------------------------------------------------------

export function useBill(id: number) {
  return useQuery<Bill>({
    queryKey: billingQueryKeys.bill(id),
    queryFn: () => billingJson<Bill>(`${BILLING_BASE}/api/v1/bills/${id}`),
    enabled: id > 0,
  });
}

// ---- Create bill -----------------------------------------------------------

export function useCreateBill() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (payload: Record<string, unknown>) =>
      billingJson<Bill>(`${BILLING_BASE}/api/v1/bills`, {
        method: "POST",
        body: JSON.stringify(payload),
      }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["bills"] }),
  });
}

// ---- Charges ---------------------------------------------------------------

export function useCreateCharge() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (payload: Record<string, unknown>) =>
      billingJson<Charge>(`${BILLING_BASE}/api/v1/charges`, {
        method: "POST",
        body: JSON.stringify(payload),
      }),
    onSuccess: (_, vars) => {
      if ((vars as { billId?: number }).billId) {
        void qc.invalidateQueries({ queryKey: billingQueryKeys.charges((vars as { billId: number }).billId) });
      }
    },
  });
}

// ---- Finalize bill ---------------------------------------------------------

export function useFinalizeBill() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (billId: number) =>
      billingJson<Bill>(`${BILLING_BASE}/api/v1/bills/${billId}/finalize`, { method: "POST" }),
    onSuccess: (_, id) => qc.invalidateQueries({ queryKey: billingQueryKeys.bill(id) }),
  });
}

// ---- Concession ------------------------------------------------------------

export function useApplyConcession() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, payload }: { id: number; payload: Record<string, unknown> }) =>
      billingJson<Bill>(`${BILLING_BASE}/api/v1/bills/${id}/concession`, {
        method: "POST",
        body: JSON.stringify(payload),
      }),
    onSuccess: (_, { id }) => qc.invalidateQueries({ queryKey: billingQueryKeys.bill(id) }),
  });
}

// ---- Payments --------------------------------------------------------------

export function usePayments(billId: number) {
  return useQuery<Payment[]>({
    queryKey: billingQueryKeys.payments(billId),
    queryFn: () => billingJson<Payment[]>(`${BILLING_BASE}/api/v1/bills/${billId}/payments`),
    enabled: billId > 0,
  });
}

export function useRecordPayment() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, payload }: { id: number; payload: Record<string, unknown> }) =>
      billingJson<Payment>(`${BILLING_BASE}/api/v1/bills/${id}/payments`, {
        method: "POST",
        body: JSON.stringify(payload),
      }),
    onSuccess: (_, { id }) => {
      void qc.invalidateQueries({ queryKey: billingQueryKeys.payments(id) });
      void qc.invalidateQueries({ queryKey: billingQueryKeys.bill(id) });
    },
  });
}

// ---- Settlement ------------------------------------------------------------

export function useSettleBill() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, payload }: { id: number; payload: Record<string, unknown> }) =>
      billingJson<Bill>(`${BILLING_BASE}/api/v1/internal/bills/${id}/settlement`, {
        method: "PUT",
        body: JSON.stringify(payload),
      }),
    onSuccess: (_, { id }) => qc.invalidateQueries({ queryKey: billingQueryKeys.bill(id) }),
  });
}

// ---- Credit notes ----------------------------------------------------------

export function useCreditNotes(billId: number) {
  return useQuery<CreditNote[]>({
    queryKey: billingQueryKeys.creditNotes(billId),
    queryFn: () => billingJson<CreditNote[]>(`${BILLING_BASE}/api/v1/bills/${billId}/credit-notes`),
    enabled: billId > 0,
  });
}

export function useIssueCreditNote() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, payload }: { id: number; payload: Record<string, unknown> }) =>
      billingJson<CreditNote>(`${BILLING_BASE}/api/v1/bills/${id}/credit-notes`, {
        method: "POST",
        body: JSON.stringify(payload),
      }),
    onSuccess: (_, { id }) => {
      void qc.invalidateQueries({ queryKey: billingQueryKeys.creditNotes(id) });
      void qc.invalidateQueries({ queryKey: billingQueryKeys.bill(id) });
    },
  });
}

// ---- Debit notes -----------------------------------------------------------

export function useDebitNotes(billId: number) {
  return useQuery<DebitNote[]>({
    queryKey: billingQueryKeys.debitNotes(billId),
    queryFn: () => billingJson<DebitNote[]>(`${BILLING_BASE}/api/v1/bills/${billId}/debit-notes`),
    enabled: billId > 0,
  });
}

export function useIssueDebitNote() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, payload }: { id: number; payload: Record<string, unknown> }) =>
      billingJson<DebitNote>(`${BILLING_BASE}/api/v1/bills/${id}/debit-notes`, {
        method: "POST",
        body: JSON.stringify(payload),
      }),
    onSuccess: (_, { id }) => {
      void qc.invalidateQueries({ queryKey: billingQueryKeys.debitNotes(id) });
      void qc.invalidateQueries({ queryKey: billingQueryKeys.bill(id) });
    },
  });
}

// ---- Patient search (for NewBillPage) --------------------------------------

export function usePatientSearch(query: string) {
  return useQuery<unknown[]>({
    queryKey: billingQueryKeys.patientSearch(query),
    queryFn: () => billingJson<unknown[]>(`${BILLING_BASE}/api/v1/patients/search?query=${encodeURIComponent(query)}`),
    enabled: query.length >= 3,
  });
}

// ---- Concession plans -------------------------------------------------------

export interface ConcessionPlan {
  planId: number;
  planName: string;
  planCode: string;
  maxDiscountPercent: number;
  requiresApproval: boolean;
  isActive: boolean;
}

export function useConcessionPlans() {
  return useQuery<ConcessionPlan[]>({
    queryKey: ["billing", "concession-plans"],
    queryFn: () => billingJson<ConcessionPlan[]>(`${BILLING_BASE}/api/v1/concession-plans`),
    staleTime: 5 * 60 * 1000,
  });
}

// ---- Packages ---------------------------------------------------------------

export interface Package {
  packageId: number;
  packageName: string;
  packageCode: string;
  description?: string;
  totalValue: number;
  sellPrice: number;
  services: Array<{ serviceId: number; serviceName: string; qty: number }>;
  isActive: boolean;
}

export function useAvailablePackages() {
  return useQuery<Package[]>({
    queryKey: ["billing", "packages"],
    queryFn: () => billingJson<Package[]>(`${BILLING_BASE}/api/v1/packages?isActive=true`),
    staleTime: 10 * 60 * 1000,
  });
}

// ---- Sponsor companies -----------------------------------------------------

export interface SponsorCompany {
  companyId: number;
  companyName: string;
  companyCode: string;
  contactPerson?: string;
  contactNo?: string;
  isActive: boolean;
}

export function useSponsorCompanies() {
  return useQuery<SponsorCompany[]>({
    queryKey: ["billing", "sponsors"],
    queryFn: () => billingJson<SponsorCompany[]>(`${BILLING_BASE}/api/v1/sponsors?isActive=true`),
    staleTime: 10 * 60 * 1000,
  });
}
