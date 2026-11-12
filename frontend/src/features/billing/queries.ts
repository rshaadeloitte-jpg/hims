import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { ApiError } from "../../api/client";

/**
 * M04 Billing hooks — consume the billing-service REST API (Spring Boot :8098).
 * Uses the Vite proxy (VITE_BILLING_API_BASE) or production gateway.
 */

const BILLING_BASE: string = import.meta.env.VITE_BILLING_API_BASE ?? "";

export function generateSafeUUID(): string {
  if (typeof crypto !== "undefined" && typeof crypto.randomUUID === "function") {
    return crypto.randomUUID();
  }
  return "xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx".replace(/[xy]/g, (c) => {
    const r = (Math.random() * 16) | 0;
    const v = c === "x" ? r : (r & 0x3) | 0x8;
    return v.toString(16);
  });
}

async function billingJson<T>(url: string, options?: RequestInit): Promise<T> {
  const res = await fetch(url, {
    headers: { "Content-Type": "application/json", "X-Request-ID": generateSafeUUID() },
    ...options,
  });
  if (!res.ok) {
    const err = await res.json().catch(() => ({ message: res.statusText }));
    throw new ApiError(res.status, err.errorCode ?? "UNKNOWN", err.message ?? "Request failed");
  }
  return res.json();
}

// ---- Types -----------------------------------------------------------------

export interface ChargeCalculation {
  lineTotal?: number;
  selfRate?: number;
  nonSelfRate?: number;
  concession?: number;
  taxA?: number;
  taxB?: number;
  taxC?: number;
  taxTotal?: number;
  selfAmount?: number;
  nonSelfAmount?: number;
  doctorShare?: number | null;
  hospitalShare?: number | null;
}

export interface Charge {
  chargeId: number;
  billId?: number;
  serviceId: number;
  serviceName: string;
  rate: number;
  qty: number;
  taxPctA?: number;
  taxPctB?: number;
  taxPctC?: number;
  applyAfterConcession?: boolean;
  coPayPct?: number;
  concessionPlanPct?: number;
  concessionBillPct?: number;
  totalBeforeFinalization?: number;
  selfBeforeFinalization?: number;
  nonselfBeforeFinalization?: number;
  calculations?: ChargeCalculation[] | ChargeCalculation;
  amount?: number;
  taxAmount?: number;
  totalAmount?: number;
  isCancelled?: boolean;
}

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

export interface PaymentModeDetail {
  modeDetailId?: number;
  paymentId?: number;
  mode: number;
  amount: number;
}

export interface AdvanceConsumption {
  consumptionId?: number;
  advanceId: number;
  paymentId?: number;
  consumedAmount: number;
}

export interface Payment {
  paymentId: number;
  billId: number;
  patientId?: number;
  amount: number;
  transactionType?: number;
  changeAmount?: number;
  modes?: PaymentModeDetail[];
  advanceConsumptions?: AdvanceConsumption[];
  paymentMethod?: string;
  referenceNo?: string;
  remarks?: string;
  addedDatetime?: string;
  createdAt?: string;
}

export interface PaymentModeLineInput {
  mode: number;
  amount: number;
}

export interface CreatePaymentPayload {
  billId: number;
  patientId: number;
  amount: number;
  advanceUse: boolean;
  modes: PaymentModeLineInput[];
}

export const PAYMENT_METHOD_TO_MODE: Record<string, number> = {
  CASH: 1,
  CARD: 2,
  CHEQUE: 3,
  UPI: 4,
  ADVANCE: 7,
};

export const MODE_TO_PAYMENT_METHOD: Record<number, string> = {
  1: "Cash",
  2: "Card",
  3: "Cheque",
  4: "UPI",
  7: "Advance",
};

export function getChargeDisplayValues(c: Charge) {
  const calc: ChargeCalculation | undefined = Array.isArray(c.calculations)
    ? c.calculations[0]
    : (c.calculations as ChargeCalculation | undefined);

  const rate = c.rate ?? 0;
  const qty = c.qty ?? 1;

  const amount =
    calc?.lineTotal ??
    c.totalBeforeFinalization ??
    c.amount ??
    rate * qty;

  const tax =
    calc?.taxTotal ??
    (calc?.taxA != null || calc?.taxB != null || calc?.taxC != null
      ? (calc.taxA ?? 0) + (calc.taxB ?? 0) + (calc.taxC ?? 0)
      : c.taxAmount ?? 0);

  const total =
    calc?.selfAmount != null && calc?.nonSelfAmount != null
      ? calc.selfAmount + calc.nonSelfAmount
      : c.selfBeforeFinalization != null && c.nonselfBeforeFinalization != null
        ? c.selfBeforeFinalization + c.nonselfBeforeFinalization
        : c.totalBeforeFinalization != null
          ? c.totalBeforeFinalization
          : c.totalAmount ?? amount + tax;

  return {
    rate,
    qty,
    amount,
    tax,
    total,
  };
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
    queryFn: () => billingJson<Payment[]>(`${BILLING_BASE}/api/v1/payments?billId=${billId}`),
    enabled: billId > 0,
  });
}

export function useRecordPayment() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (vars: CreatePaymentPayload | { id: number; payload: Record<string, unknown> }) => {
      let body: CreatePaymentPayload;
      if ("billId" in vars && "patientId" in vars && "modes" in vars) {
        body = vars;
      } else {
        const v = vars as { id: number; payload: Record<string, unknown> };
        const p = v.payload;
        if ("modes" in p) {
          body = {
            billId: (p.billId as number) ?? v.id,
            patientId: (p.patientId as number) ?? 0,
            amount: Number(p.amount),
            advanceUse: Boolean(p.advanceUse),
            modes: (p.modes as PaymentModeLineInput[]) ?? [],
          };
        } else {
          const method = String(p.paymentMethod ?? "CASH").toUpperCase();
          const isAdvance = method === "ADVANCE" || Boolean(p.advanceUse);
          const amount = Number(p.amount);
          body = {
            billId: v.id,
            patientId: (p.patientId as number) ?? 0,
            amount,
            advanceUse: isAdvance,
            modes: isAdvance ? [] : [{ mode: PAYMENT_METHOD_TO_MODE[method] ?? 1, amount }],
          };
        }
      }
      return billingJson<Payment>(`${BILLING_BASE}/api/v1/payments`, {
        method: "POST",
        body: JSON.stringify(body),
      });
    },
    onSuccess: (_, vars) => {
      const billId = "billId" in vars ? vars.billId : vars.id;
      if (billId) {
        void qc.invalidateQueries({ queryKey: billingQueryKeys.payments(billId) });
        void qc.invalidateQueries({ queryKey: billingQueryKeys.bill(billId) });
      }
      void qc.invalidateQueries({ queryKey: ["bills"] });
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
    queryFn: () =>
      billingJson<unknown[]>(
        `${BILLING_BASE}/api/v1/patients/search?q=${encodeURIComponent(query)}&query=${encodeURIComponent(query)}`,
      ),
    enabled: query.trim().length >= 2,
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
