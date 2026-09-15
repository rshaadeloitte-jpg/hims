import { describe, expect, it, vi, beforeEach, afterEach } from "vitest";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { renderHook, waitFor } from "@testing-library/react";
import {
  useSuppliers,
  useStores,
  useItems,
  usePurchaseOrders,
  useGrns,
  useQuotations,
  useQuotationCompare,
} from "../queries";

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

describe("useSuppliers", () => {
  it("fetches GET /api/v1/suppliers", async () => {
    fetchMock.mockResolvedValueOnce({
      ok: true,
      status: 200,
      json: async () => [{ id: 1, code: "SUP1", description: "Supplier 1" }],
    });
    const { result } = renderHook(() => useSuppliers(), { wrapper: wrapper() });
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(fetchMock).toHaveBeenCalledWith(
      expect.stringContaining("/api/v1/suppliers"),
      expect.any(Object),
    );
    expect(result.current.data?.[0]?.code).toBe("SUP1");
  });
});

describe("useStores", () => {
  it("fetches GET /api/v1/stores", async () => {
    fetchMock.mockResolvedValueOnce({
      ok: true,
      status: 200,
      json: async () => [{ id: 1, code: "STR1", description: "Main Store" }],
    });
    const { result } = renderHook(() => useStores(), { wrapper: wrapper() });
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(fetchMock).toHaveBeenCalledWith(
      expect.stringContaining("/api/v1/stores"),
      expect.any(Object),
    );
  });
});

describe("useItems", () => {
  it("fetches GET /api/v1/items", async () => {
    fetchMock.mockResolvedValueOnce({
      ok: true,
      status: 200,
      json: async () => [{ id: 1, itemName: "Paracetamol" }],
    });
    const { result } = renderHook(() => useItems(), { wrapper: wrapper() });
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(fetchMock).toHaveBeenCalledWith(
      expect.stringContaining("/api/v1/items"),
      expect.any(Object),
    );
  });
});

describe("usePurchaseOrders", () => {
  it("fetches GET /api/v1/inventory/purchase-orders", async () => {
    fetchMock.mockResolvedValueOnce({
      ok: true,
      status: 200,
      json: async () => [{ id: 1, poNo: "PO-1", docStatus: 1 }],
    });
    const { result } = renderHook(() => usePurchaseOrders(), { wrapper: wrapper() });
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(fetchMock).toHaveBeenCalledWith(
      expect.stringContaining("/api/v1/inventory/purchase-orders"),
      expect.any(Object),
    );
    expect(result.current.data?.[0]?.poNo).toBe("PO-1");
  });
});

describe("useGrns", () => {
  it("fetches GET /api/v1/grn", async () => {
    fetchMock.mockResolvedValueOnce({
      ok: true,
      status: 200,
      json: async () => [{ id: 1, grnNo: "GRN-1", docStatus: 1 }],
    });
    const { result } = renderHook(() => useGrns(), { wrapper: wrapper() });
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(fetchMock).toHaveBeenCalledWith(
      expect.stringContaining("/api/v1/grn"),
      expect.any(Object),
    );
  });
});

describe("useQuotations", () => {
  it("fetches GET /api/v1/inventory/quotations", async () => {
    fetchMock.mockResolvedValueOnce({
      ok: true,
      status: 200,
      json: async () => [{ id: 1, quotationNo: "QT-1", docStatus: 1 }],
    });
    const { result } = renderHook(() => useQuotations(), { wrapper: wrapper() });
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(fetchMock).toHaveBeenCalledWith(
      expect.stringContaining("/api/v1/inventory/quotations"),
      expect.any(Object),
    );
  });
});

describe("useQuotationCompare", () => {
  it("fetches GET /api/v1/inventory/quotations/compare?itemId=", async () => {
    fetchMock.mockResolvedValueOnce({
      ok: true,
      status: 200,
      json: async () => [{ quotationId: 1, quotationNo: "QT-1", supplierId: 1, itemId: 5, quantity: 100, unitPrice: 50, taxPercent: 18, leadTimeDays: 7 }],
    });
    const { result } = renderHook(() => useQuotationCompare(5), { wrapper: wrapper() });
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(fetchMock).toHaveBeenCalledWith(
      expect.stringContaining("/api/v1/inventory/quotations/compare?itemId=5"),
      expect.any(Object),
    );
    expect(result.current.data?.[0]?.quotationNo).toBe("QT-1");
  });

  it("does not fetch when itemId is 0", () => {
    const { result } = renderHook(() => useQuotationCompare(0), { wrapper: wrapper() });
    expect(fetchMock).not.toHaveBeenCalled();
    expect(result.current.fetchStatus).toBe("idle");
  });
});
