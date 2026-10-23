import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  useBill,
  useBills,
  useRecordPayment,
  useIssueCreditNote,
  useIssueDebitNote,
  useApplyConcession,
  useFinalizeBill,
  type Bill,
  type Charge,
  type Payment,
  type CreditNote,
  type DebitNote,
  type BillFilter,
  billingQueryKeys,
  generateSafeUUID,
} from "./queries";

// Re-export everything from queries
export {
  useBill,
  useBills,
  useRecordPayment,
  useIssueCreditNote,
  useIssueDebitNote,
  useApplyConcession,
  useFinalizeBill,
};
export type { Bill, Charge, Payment, CreditNote, DebitNote, BillFilter };
export { billingQueryKeys };

const BILLING_BASE: string = import.meta.env.VITE_BILLING_API_BASE ?? "/api";

async function billingJson<T>(url: string, options?: RequestInit): Promise<T> {
  const res = await fetch(url, {
    headers: { "Content-Type": "application/json", "X-Request-ID": generateSafeUUID() },
    ...options,
  });
  if (!res.ok) {
    const err = await res.json().catch(() => ({ message: res.statusText }));
    throw new Error(err.message ?? "Request failed");
  }
  return res.json();
}

// ---- Estimation types & hooks ---------------------------------------------

export interface EstimationLine {
  serviceId: number;
  serviceName: string;
  rate: number;
  qty: number;
  amount: number;
  taxRate: number;
  taxAmount: number;
  totalAmount: number;
}

export interface BillEstimation {
  patientId: number;
  patientName?: string;
  isSelfCompany: boolean;
  sponsorCompanyId?: number;
  sponsorPlanId?: number;
  lines: EstimationLine[];
  subtotal: number;
  totalTax: number;
  concessionAmount: number;
  concessionPlanPct?: number;
  concessionBillPct?: number;
  selfAmount: number;
  nonSelfAmount: number;
  grandTotal: number;
}

export interface EstimationRequest {
  patientId: number;
  encounterId?: number;
  admissionId?: number;
  isSelfCompany?: boolean;
  sponsorCompanyId?: number;
  sponsorPlanId?: number;
  lines: Array<{
    serviceId: number;
    serviceName: string;
    rate: number;
    qty: number;
    taxRate?: number;
  }>;
  concessionPlanPct?: number;
  concessionBillPct?: number;
}

export function useBillEstimation() {
  return useMutation({
    mutationFn: (payload: EstimationRequest) =>
      billingJson<BillEstimation>(`${BILLING_BASE}/api/v1/bills/estimate`, {
        method: "POST",
        body: JSON.stringify(payload),
      }),
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

// ---- Bill history / ledger --------------------------------------------------

export interface LedgerEntry {
  entryId: number;
  billId: number;
  entryType: "PAYMENT" | "CREDIT_NOTE" | "DEBIT_NOTE" | "CONCESSION" | "REFUND";
  amount: number;
  runningBalance: number;
  reference?: string;
  remarks?: string;
  entryDate: string;
  createdAt: string;
}

export function useBillLedger(billId: number) {
  return useQuery<LedgerEntry[]>({
    queryKey: ["billing", "ledger", billId],
    queryFn: () => billingJson<LedgerEntry[]>(`${BILLING_BASE}/api/v1/bills/${billId}/ledger`),
    enabled: billId > 0,
  });
}

// ---- Package subscription ---------------------------------------------------

export interface PackageSubscription {
  subscriptionId: number;
  packageId: number;
  packageName: string;
  billId: number;
  startDate: string;
  endDate: string;
  isActive: boolean;
  servicesIncluded: number;
}

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

export function useSubscribePackage() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ billId, packageId }: { billId: number; packageId: number }) =>
      billingJson<PackageSubscription>(`${BILLING_BASE}/api/v1/bills/${billId}/package-subscription`, {
        method: "POST",
        body: JSON.stringify({ packageId }),
      }),
    onSuccess: (_, { billId }) => {
      void qc.invalidateQueries({ queryKey: ["billing", "ledger", billId] });
      void qc.invalidateQueries({ queryKey: billingQueryKeys.bill(billId) });
    },
  });
}

// ---- Sponsor change --------------------------------------------------------

export interface SponsorCompany {
  companyId: number;
  companyName: string;
  companyCode: string;
  contactPerson?: string;
  contactNo?: string;
  isActive: boolean;
}

export interface SponsorPlan {
  planId: number;
  companyId: number;
  planName: string;
  planCode: string;
  coveragePercent: number;
  maxCoverageAmount: number;
  isActive: boolean;
}

export function useSponsorCompanies() {
  return useQuery<SponsorCompany[]>({
    queryKey: ["billing", "sponsors"],
    queryFn: () => billingJson<SponsorCompany[]>(`${BILLING_BASE}/api/v1/sponsors?isActive=true`),
    staleTime: 10 * 60 * 1000,
  });
}

export function useSponsorPlans(companyId: number | undefined) {
  return useQuery<SponsorPlan[]>({
    queryKey: ["billing", "sponsors", companyId, "plans"],
    queryFn: () => billingJson<SponsorPlan[]>(`${BILLING_BASE}/api/v1/sponsors/${companyId}/plans`),
    enabled: !!companyId,
    staleTime: 10 * 60 * 1000,
  });
}

export function useChangeSponsor() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({
      billId,
      companyId,
      planId,
    }: {
      billId: number;
      companyId: number;
      planId?: number;
    }) =>
      billingJson<Bill>(`${BILLING_BASE}/api/v1/bills/${billId}/sponsor`, {
        method: "PUT",
        body: JSON.stringify({ sponsorCompanyId: companyId, sponsorPlanId: planId }),
      }),
    onSuccess: (_, { billId }) => {
      void qc.invalidateQueries({ queryKey: billingQueryKeys.bill(billId) });
      void qc.invalidateQueries({ queryKey: ["billing", "ledger", billId] });
    },
  });
}

// ---- Advance payments -------------------------------------------------------

export interface Advance {
  advanceId: number;
  patientId: number;
  amount: number;
  paymentMethod: string;
  referenceNo?: string;
  remarks?: string;
  isSettled: boolean;
  createdAt: string;
}

export function usePatientAdvances(patientId: number) {
  return useQuery<Advance[]>({
    queryKey: ["billing", "advances", patientId],
    queryFn: () => billingJson<Advance[]>(`${BILLING_BASE}/api/v1/patients/${patientId}/advances`),
    enabled: patientId > 0,
  });
}

export function useApplyAdvance() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({
      billId,
      advanceId,
      amount,
    }: {
      billId: number;
      advanceId: number;
      amount: number;
    }) =>
      billingJson<Bill>(`${BILLING_BASE}/api/v1/bills/${billId}/apply-advance`, {
        method: "POST",
        body: JSON.stringify({ advanceId, amount }),
      }),
    onSuccess: (_, { billId }) => {
      void qc.invalidateQueries({ queryKey: billingQueryKeys.bill(billId) });
      void qc.invalidateQueries({ queryKey: ["billing", "ledger", billId] });
    },
  });
}

// ---- Payment receipt -------------------------------------------------------

export interface PaymentReceipt {
  receiptId: number;
  paymentId: number;
  billId: number;
  receiptNo: string;
  amount: number;
  paymentMethod: string;
  paidByName?: string;
  paidByContact?: string;
  generatedAt: string;
}

export function usePaymentReceipt(paymentId: number) {
  return useQuery<PaymentReceipt>({
    queryKey: ["billing", "receipt", paymentId],
    queryFn: () => billingJson<PaymentReceipt>(`${BILLING_BASE}/api/v1/payments/${paymentId}/receipt`),
    enabled: paymentId > 0,
  });
}

// ---- Day-close summary ------------------------------------------------------

export interface DayCloseSummary {
  date: string;
  cashierId: number;
  totalBills: number;
  totalAmount: number;
  totalPayments: number;
  paymentBreakdown: Record<string, number>;
  totalConcession: number;
  totalCreditNotes: number;
  totalDebitNotes: number;
  netRevenue: number;
}

export function useDayCloseSummary(date: string, cashierId: number) {
  return useQuery<DayCloseSummary>({
    queryKey: ["billing", "day-close", date, cashierId],
    queryFn: () =>
      billingJson<DayCloseSummary>(
        `${BILLING_BASE}/api/v1/billing/day-close?date=${date}&cashierId=${cashierId}`,
      ),
    enabled: !!date && cashierId > 0,
  });
}

// ---- Revenue analytics ------------------------------------------------------

export interface RevenueAnalytics {
  period: "DAILY" | "WEEKLY" | "MONTHLY";
  labels: string[];
  revenue: number[];
  outstanding: number[];
  collections: number[];
  totalRevenue: number;
  totalOutstanding: number;
  totalCollections: number;
}

export function useRevenueAnalytics(period: "DAILY" | "WEEKLY" | "MONTHLY" = "DAILY", days: number = 30) {
  return useQuery<RevenueAnalytics>({
    queryKey: ["billing", "analytics", "revenue", period, days],
    queryFn: () =>
      billingJson<RevenueAnalytics>(
        `${BILLING_BASE}/api/v1/billing/analytics/revenue?period=${period}&days=${days}`,
      ),
    staleTime: 5 * 60 * 1000,
  });
}

export function useCollectionAnalytics(period: "DAILY" | "WEEKLY" | "MONTHLY" = "DAILY", days: number = 30) {
  return useQuery<RevenueAnalytics>({
    queryKey: ["billing", "analytics", "collection", period, days],
    queryFn: () =>
      billingJson<RevenueAnalytics>(
        `${BILLING_BASE}/api/v1/billing/analytics/collection?period=${period}&days=${days}`,
      ),
    staleTime: 5 * 60 * 1000,
  });
}
