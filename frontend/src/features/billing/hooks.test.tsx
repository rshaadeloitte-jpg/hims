import { describe, expect, it, vi, beforeEach, afterEach } from "vitest";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { renderHook, waitFor } from "@testing-library/react";
import {
  useBillEstimation,
  useConcessionPlans,
  useBillLedger,
  useAvailablePackages,
  useSponsorCompanies,
  useSponsorPlans,
  usePatientAdvances,
  usePaymentReceipt,
  useDayCloseSummary,
  useRevenueAnalytics,
} from "./hooks";

function wrapper() {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return ({ children }: { children: React.ReactNode }) => (
    <QueryClientProvider client={qc}>{children}</QueryClientProvider>
  );
}

const fetchMock = vi.fn();

beforeEach(() => {
  fetchMock.mockReset();
  vi.stubGlobal("fetch", fetchMock);
});

afterEach(() => {
  vi.unstubAllGlobals();
});

describe("useBillEstimation", () => {
  it("posts estimation request and returns estimation", async () => {
    const mockEstimation = {
      patientId: 1,
      patientName: "John Doe",
      isSelfCompany: true,
      lines: [
        {
          serviceId: 1,
          serviceName: "Consultation",
          rate: 500,
          qty: 1,
          amount: 500,
          taxRate: 5,
          taxAmount: 25,
          totalAmount: 525,
        },
      ],
      subtotal: 500,
      totalTax: 25,
      concessionAmount: 0,
      selfAmount: 525,
      nonSelfAmount: 0,
      grandTotal: 525,
    };
    fetchMock.mockResolvedValueOnce({
      ok: true,
      status: 200,
      json: async () => mockEstimation,
    });

    const { result } = renderHook(() => useBillEstimation(), { wrapper: wrapper() });
    result.current.mutate({
      patientId: 1,
      lines: [{ serviceId: 1, serviceName: "Consultation", rate: 500, qty: 1 }],
    });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(fetchMock).toHaveBeenCalledWith(
      expect.stringContaining("/api/v1/bills/estimate"),
      expect.objectContaining({ method: "POST" }),
    );
    expect(result.current.data?.grandTotal).toBe(525);
  });
});

describe("useConcessionPlans", () => {
  it("fetches concession plans", async () => {
    const mockPlans = [
      { planId: 1, planName: "Staff Discount", planCode: "STAFF", maxDiscountPercent: 10, requiresApproval: false, isActive: true },
    ];
    fetchMock.mockResolvedValueOnce({
      ok: true,
      status: 200,
      json: async () => mockPlans,
    });

    const { result } = renderHook(() => useConcessionPlans(), { wrapper: wrapper() });
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data?.[0]?.planName).toBe("Staff Discount");
  });
});

describe("useBillLedger", () => {
  it("fetches ledger entries for a bill", async () => {
    const mockLedger = [
      {
        entryId: 1,
        billId: 100,
        entryType: "PAYMENT",
        amount: 500,
        runningBalance: 500,
        reference: "REF001",
        entryDate: "2025-01-01",
        createdAt: "2025-01-01T10:00:00Z",
      },
    ];
    fetchMock.mockResolvedValueOnce({
      ok: true,
      status: 200,
      json: async () => mockLedger,
    });

    const { result } = renderHook(() => useBillLedger(100), { wrapper: wrapper() });
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data?.[0]?.amount).toBe(500);
  });

  it("is disabled when billId is 0", () => {
    const { result } = renderHook(() => useBillLedger(0), { wrapper: wrapper() });
    expect(result.current.fetchStatus).toBe("idle");
    expect(fetchMock).not.toHaveBeenCalled();
  });
});

describe("useAvailablePackages", () => {
  it("fetches available packages", async () => {
    const mockPackages = [
      { packageId: 1, packageName: "Health Checkup", packageCode: "HC001", totalValue: 5000, sellPrice: 4500, services: [], isActive: true },
    ];
    fetchMock.mockResolvedValueOnce({
      ok: true,
      status: 200,
      json: async () => mockPackages,
    });

    const { result } = renderHook(() => useAvailablePackages(), { wrapper: wrapper() });
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data?.[0]?.packageName).toBe("Health Checkup");
  });
});

describe("useSponsorCompanies", () => {
  it("fetches sponsor companies", async () => {
    const mockSponsors = [
      { companyId: 1, companyName: "ABC Insurance", companyCode: "ABC", isActive: true },
    ];
    fetchMock.mockResolvedValueOnce({
      ok: true,
      status: 200,
      json: async () => mockSponsors,
    });

    const { result } = renderHook(() => useSponsorCompanies(), { wrapper: wrapper() });
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data?.[0]?.companyName).toBe("ABC Insurance");
  });
});

describe("useSponsorPlans", () => {
  it("fetches plans for a company", async () => {
    const mockPlans = [
      { planId: 1, companyId: 1, planName: "Gold Plan", planCode: "GOLD", coveragePercent: 80, maxCoverageAmount: 50000, isActive: true },
    ];
    fetchMock.mockResolvedValueOnce({
      ok: true,
      status: 200,
      json: async () => mockPlans,
    });

    const { result } = renderHook(() => useSponsorPlans(1), { wrapper: wrapper() });
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data?.[0]?.planName).toBe("Gold Plan");
  });

  it("is disabled when companyId is undefined", () => {
    const { result } = renderHook(() => useSponsorPlans(undefined), { wrapper: wrapper() });
    expect(result.current.fetchStatus).toBe("idle");
    expect(fetchMock).not.toHaveBeenCalled();
  });
});

describe("usePatientAdvances", () => {
  it("fetches patient advances", async () => {
    const mockAdvances = [
      { advanceId: 1, patientId: 1, amount: 1000, paymentMethod: "CASH", isSettled: false, createdAt: "2025-01-01T10:00:00Z" },
    ];
    fetchMock.mockResolvedValueOnce({
      ok: true,
      status: 200,
      json: async () => mockAdvances,
    });

    const { result } = renderHook(() => usePatientAdvances(1), { wrapper: wrapper() });
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data?.[0]?.amount).toBe(1000);
  });
});

describe("usePaymentReceipt", () => {
  it("fetches payment receipt", async () => {
    const mockReceipt = {
      receiptId: 1,
      paymentId: 1,
      billId: 100,
      receiptNo: "RCP001",
      amount: 500,
      paymentMethod: "CASH",
      generatedAt: "2025-01-01T10:00:00Z",
    };
    fetchMock.mockResolvedValueOnce({
      ok: true,
      status: 200,
      json: async () => mockReceipt,
    });

    const { result } = renderHook(() => usePaymentReceipt(1), { wrapper: wrapper() });
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data?.receiptNo).toBe("RCP001");
  });
});

describe("useDayCloseSummary", () => {
  it("fetches day close summary", async () => {
    const mockSummary = {
      date: "2025-01-01",
      cashierId: 1,
      totalBills: 10,
      totalAmount: 25000,
      totalPayments: 20000,
      paymentBreakdown: { CASH: 15000, CARD: 5000 },
      totalConcession: 1000,
      totalCreditNotes: 500,
      totalDebitNotes: 200,
      netRevenue: 18500,
    };
    fetchMock.mockResolvedValueOnce({
      ok: true,
      status: 200,
      json: async () => mockSummary,
    });

    const { result } = renderHook(() => useDayCloseSummary("2025-01-01", 1), { wrapper: wrapper() });
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data?.totalBills).toBe(10);
  });
});

describe("useRevenueAnalytics", () => {
  it("fetches revenue analytics", async () => {
    const mockAnalytics = {
      period: "DAILY" as const,
      labels: ["2025-01-01", "2025-01-02"],
      revenue: [10000, 15000],
      outstanding: [5000, 7000],
      collections: [8000, 12000],
      totalRevenue: 25000,
      totalOutstanding: 12000,
      totalCollections: 20000,
    };
    fetchMock.mockResolvedValueOnce({
      ok: true,
      status: 200,
      json: async () => mockAnalytics,
    });

    const { result } = renderHook(() => useRevenueAnalytics("DAILY", 30), { wrapper: wrapper() });
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data?.totalRevenue).toBe(25000);
  });
});
