import { describe, expect, it, vi, beforeEach, afterEach } from "vitest";
import { renderHook, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import {
  useJournalEntries,
  useJournalEntry,
  useJournalVoucherLegs,
  useCreateJournalEntry,
  usePostJournalEntry,
  useReverseJournalEntry,
  useTreasuryHeads,
  useTreasuryAuthorities,
  useTreasuryLedgers,
  useCollections,
  useServiceWiseSummary,
  useUserWiseSummary,
  useBankStatements,
  usePoRequisitions,
} from "../queries";

function createWrapper() {
  const qc = new QueryClient({
    defaultOptions: {
      queries: { retry: false },
      mutations: { retry: false },
    },
  });
  return ({ children }: { children: React.ReactNode }) => (
    <QueryClientProvider client={qc}>{children}</QueryClientProvider>
  );
}

describe("RMRS Queries - Real Backend Endpoints", () => {
  let fetchMock: ReturnType<typeof vi.fn>;

  beforeEach(() => {
    fetchMock = vi.fn().mockImplementation(() => {
      return Promise.resolve(
        new Response(JSON.stringify([]), {
          status: 200,
          headers: { "Content-Type": "application/json" },
        }),
      );
    });
    vi.stubGlobal("fetch", fetchMock);
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("useJournalEntries calls /api/v1/rmrs/journal-vouchers", async () => {
    fetchMock.mockResolvedValueOnce(
      new Response(
        JSON.stringify([
          { id: 1, voucherNo: "JV-001", narration: "Rent payment", voucherDate: "2026-11-11", docStatus: 1 },
        ]),
        { status: 200, headers: { "Content-Type": "application/json" } },
      ),
    );
    const { result } = renderHook(() => useJournalEntries(), { wrapper: createWrapper() });
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(fetchMock).toHaveBeenCalledWith(
      expect.stringContaining("/api/v1/rmrs/journal-vouchers"),
      expect.any(Object),
    );
    expect(result.current.data?.[0].voucherNo).toBe("JV-001");
  });

  it("useJournalEntries with status filter passes status query param", async () => {
    fetchMock.mockResolvedValueOnce(
      new Response(JSON.stringify([]), { status: 200, headers: { "Content-Type": "application/json" } }),
    );
    const { result } = renderHook(() => useJournalEntries(2), { wrapper: createWrapper() });
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(fetchMock).toHaveBeenCalledWith(
      expect.stringContaining("/api/v1/rmrs/journal-vouchers?status=2"),
      expect.any(Object),
    );
  });

  it("useJournalEntry calls /api/v1/rmrs/journal-vouchers/:id", async () => {
    fetchMock.mockResolvedValueOnce(
      new Response(
        JSON.stringify({ id: 10, voucherNo: "JV-010", narration: "Test", voucherDate: "2026-11-11", docStatus: 1 }),
        { status: 200, headers: { "Content-Type": "application/json" } },
      ),
    );
    const { result } = renderHook(() => useJournalEntry(10), { wrapper: createWrapper() });
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(fetchMock).toHaveBeenCalledWith(
      expect.stringContaining("/api/v1/rmrs/journal-vouchers/10"),
      expect.any(Object),
    );
    expect(result.current.data?.voucherNo).toBe("JV-010");
  });

  it("useJournalVoucherLegs calls /api/v1/rmrs/journal-vouchers/:id/legs", async () => {
    fetchMock.mockResolvedValueOnce(
      new Response(
        JSON.stringify([{ id: 1, voucherId: 10, ledgerId: 5, debit: 100, credit: 0 }]),
        { status: 200, headers: { "Content-Type": "application/json" } },
      ),
    );
    const { result } = renderHook(() => useJournalVoucherLegs(10), { wrapper: createWrapper() });
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(fetchMock).toHaveBeenCalledWith(
      expect.stringContaining("/api/v1/rmrs/journal-vouchers/10/legs"),
      expect.any(Object),
    );
  });

  it("useCreateJournalEntry POSTs to /api/v1/rmrs/journal-vouchers", async () => {
    fetchMock.mockResolvedValueOnce(
      new Response(
        JSON.stringify({ id: 22, voucherNo: "JV-022", narration: "Salary", voucherDate: "2026-11-11", docStatus: 1 }),
        { status: 201, headers: { "Content-Type": "application/json" } },
      ),
    );
    const { result } = renderHook(() => useCreateJournalEntry(), { wrapper: createWrapper() });
    result.current.mutate({
      narration: "Salary",
      voucherDate: "2026-11-11",
      legs: [
        { ledgerId: 1, debit: 500, credit: 0 },
        { ledgerId: 2, debit: 0, credit: 500 },
      ],
    });
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(fetchMock).toHaveBeenCalledWith(
      expect.stringContaining("/api/v1/rmrs/journal-vouchers"),
      expect.objectContaining({
        method: "POST",
        body: JSON.stringify({
          narration: "Salary",
          voucherDate: "2026-11-11",
          legs: [
            { ledgerId: 1, debit: 500, credit: 0 },
            { ledgerId: 2, debit: 0, credit: 500 },
          ],
        }),
      }),
    );
  });

  it("usePostJournalEntry POSTs to /api/v1/rmrs/journal-vouchers/:id/post", async () => {
    fetchMock.mockResolvedValueOnce(
      new Response(
        JSON.stringify({ id: 5, voucherNo: "JV-005", docStatus: 2 }),
        { status: 200, headers: { "Content-Type": "application/json" } },
      ),
    );
    const { result } = renderHook(() => usePostJournalEntry(), { wrapper: createWrapper() });
    result.current.mutate(5);
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(fetchMock).toHaveBeenCalledWith(
      expect.stringContaining("/api/v1/rmrs/journal-vouchers/5/post"),
      expect.objectContaining({ method: "POST" }),
    );
  });

  it("useReverseJournalEntry POSTs to /api/v1/rmrs/journal-vouchers/:id/reverse", async () => {
    fetchMock.mockResolvedValueOnce(
      new Response(
        JSON.stringify({ id: 5, voucherNo: "JV-005", docStatus: 3 }),
        { status: 200, headers: { "Content-Type": "application/json" } },
      ),
    );
    const { result } = renderHook(() => useReverseJournalEntry(), { wrapper: createWrapper() });
    result.current.mutate(5);
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(fetchMock).toHaveBeenCalledWith(
      expect.stringContaining("/api/v1/rmrs/journal-vouchers/5/reverse"),
      expect.objectContaining({ method: "POST" }),
    );
  });

  it("useTreasuryHeads calls /api/v1/masters/heads", async () => {
    fetchMock.mockResolvedValueOnce(
      new Response(
        JSON.stringify([{ headId: 1, headCode: "H01", headName: "Op Revenue", isActive: true }]),
        { status: 200, headers: { "Content-Type": "application/json" } },
      ),
    );
    const { result } = renderHook(() => useTreasuryHeads(), { wrapper: createWrapper() });
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(fetchMock).toHaveBeenCalledWith(
      expect.stringContaining("/api/v1/masters/heads"),
      expect.any(Object),
    );
  });

  it("useTreasuryAuthorities calls /api/v1/masters/authorities", async () => {
    fetchMock.mockResolvedValueOnce(
      new Response(
        JSON.stringify([{ authorityId: 1, authorityCode: "AUTH1", authorityName: "Auth 1", headId: 1 }]),
        { status: 200, headers: { "Content-Type": "application/json" } },
      ),
    );
    const { result } = renderHook(() => useTreasuryAuthorities(), { wrapper: createWrapper() });
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(fetchMock).toHaveBeenCalledWith(
      expect.stringContaining("/api/v1/masters/authorities"),
      expect.any(Object),
    );
  });

  it("useTreasuryLedgers calls /api/v1/masters/ledgers", async () => {
    fetchMock.mockResolvedValueOnce(
      new Response(
        JSON.stringify([{ ledgerId: 1, ledgerCode: "L01", ledgerName: "Cash", headId: 1 }]),
        { status: 200, headers: { "Content-Type": "application/json" } },
      ),
    );
    const { result } = renderHook(() => useTreasuryLedgers(), { wrapper: createWrapper() });
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(fetchMock).toHaveBeenCalledWith(
      expect.stringContaining("/api/v1/masters/ledgers"),
      expect.any(Object),
    );
  });

  it("useCollections calls /api/v1/collections with filter params", async () => {
    fetchMock.mockResolvedValueOnce(
      new Response(
        JSON.stringify({
          content: [{ collectionId: 101, receiptNo: "REC-01", amount: 1500, headId: 1, ledgerId: 2 }],
          totalElements: 1,
        }),
        { status: 200, headers: { "Content-Type": "application/json" } },
      ),
    );
    const { result } = renderHook(() => useCollections({ headId: 1, page: 0, size: 20 }), { wrapper: createWrapper() });
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(fetchMock).toHaveBeenCalledWith(
      expect.stringContaining("/api/v1/collections?headId=1&page=0&size=20"),
      expect.any(Object),
    );
  });

  it("useServiceWiseSummary calls /api/v1/collections/summary/service-wise", async () => {
    fetchMock.mockResolvedValueOnce(
      new Response(
        JSON.stringify([{ headId: 1, headCode: "H01", totalAmount: 5000, collectionCount: 10 }]),
        { status: 200, headers: { "Content-Type": "application/json" } },
      ),
    );
    const { result } = renderHook(() => useServiceWiseSummary("2026-11-01", "2026-11-11"), { wrapper: createWrapper() });
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(fetchMock).toHaveBeenCalledWith(
      expect.stringContaining("/api/v1/collections/summary/service-wise?dateFrom=2026-11-01&dateTo=2026-11-11"),
      expect.any(Object),
    );
  });

  it("useUserWiseSummary calls /api/v1/collections/summary/user-wise", async () => {
    fetchMock.mockResolvedValueOnce(
      new Response(
        JSON.stringify([{ collectedBy: "admin", totalAmount: 7000, collectionCount: 14 }]),
        { status: 200, headers: { "Content-Type": "application/json" } },
      ),
    );
    const { result } = renderHook(() => useUserWiseSummary("2026-11-01", "2026-11-11"), { wrapper: createWrapper() });
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(fetchMock).toHaveBeenCalledWith(
      expect.stringContaining("/api/v1/collections/summary/user-wise?dateFrom=2026-11-01&dateTo=2026-11-11"),
      expect.any(Object),
    );
  });

  it("useBankStatements calls /api/v1/rmrs/bank-statements", async () => {
    fetchMock.mockResolvedValueOnce(
      new Response(
        JSON.stringify([{ id: 1, bankLedgerId: 5, amount: 2000, direction: "CR", docStatus: 1 }]),
        { status: 200, headers: { "Content-Type": "application/json" } },
      ),
    );
    const { result } = renderHook(() => useBankStatements(), { wrapper: createWrapper() });
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(fetchMock).toHaveBeenCalledWith(
      expect.stringContaining("/api/v1/rmrs/bank-statements"),
      expect.any(Object),
    );
  });

  it("usePoRequisitions calls /api/v1/rmrs/po-requests", async () => {
    fetchMock.mockResolvedValueOnce(
      new Response(
        JSON.stringify([{ id: 1, requestNo: "REQ-01", headId: 1, amount: 5000, docStatus: 1 }]),
        { status: 200, headers: { "Content-Type": "application/json" } },
      ),
    );
    const { result } = renderHook(() => usePoRequisitions(), { wrapper: createWrapper() });
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(fetchMock).toHaveBeenCalledWith(
      expect.stringContaining("/api/v1/rmrs/po-requests"),
      expect.any(Object),
    );
  });
});
