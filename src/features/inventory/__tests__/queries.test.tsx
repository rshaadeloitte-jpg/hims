import { describe, expect, it, vi, beforeEach, afterEach } from "vitest";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { renderHook, waitFor, render, screen } from "@testing-library/react";
import {
  useInventoryItems,
  useInventoryStores,
  useStockBalances,
  useCreateIndent,
  useCreateStockAdjustment,
  useItemEnquiry,
} from "../queries";
import { IndentForm } from "../IndentForm";
import { StockAdjustment } from "../StockAdjustment";

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

describe("useInventoryItems", () => {
  it("fetches inventory items", async () => {
    fetchMock.mockResolvedValueOnce({
      ok: true,
      status: 200,
      json: async () => [
        { id: 1, name: "Paracetamol", code: "PCM-001", unitId: 1, status: "ACTIVE" },
      ],
    });

    const { result } = renderHook(() => useInventoryItems(), { wrapper: wrapper() });
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(fetchMock).toHaveBeenCalledWith(
      "/api/api/v1/items",
      expect.any(Object),
    );
    expect(result.current.data).toHaveLength(1);
    expect(result.current.data?.[0].name).toBe("Paracetamol");
  });
});

describe("useInventoryStores", () => {
  it("fetches inventory stores", async () => {
    fetchMock.mockResolvedValueOnce({
      ok: true,
      status: 200,
      json: async () => [
        { id: 1, name: "Main Store", code: "STORE-01", location: "Building A" },
      ],
    });

    const { result } = renderHook(() => useInventoryStores(), { wrapper: wrapper() });
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data?.[0].name).toBe("Main Store");
  });
});

describe("useStockBalances", () => {
  it("fetches stock balances", async () => {
    fetchMock.mockResolvedValueOnce({
      ok: true,
      status: 200,
      json: async () => [
        { id: 1, itemId: 1, batchId: 1, quantity: 500, location: "A1" },
      ],
    });

    const { result } = renderHook(() => useStockBalances(), { wrapper: wrapper() });
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data?.[0].quantity).toBe(500);
  });
});

describe("useCreateIndent", () => {
  it("submits indent request successfully", async () => {
    fetchMock.mockResolvedValueOnce({
      ok: true,
      status: 201,
      json: async () => ({
        id: 10,
        storeId: 1,
        requestDate: "2024-01-01",
        priority: "ROUTINE",
        lines: [{ itemId: 1, requestedQty: 100 }],
        status: "SUBMITTED",
      }),
    });

    const { result } = renderHook(() => useCreateIndent(), { wrapper: wrapper() });

    result.current.mutate({
      storeId: 1,
      requestDate: "2024-01-01",
      priority: "ROUTINE",
      lines: [{ itemId: 1, requestedQty: 100 }],
    });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data?.status).toBe("SUBMITTED");
  });
});

describe("useCreateStockAdjustment", () => {
  it("submits stock adjustment successfully", async () => {
    fetchMock.mockResolvedValueOnce({
      ok: true,
      status: 201,
      json: async () => ({
        id: 5,
        itemId: 1,
        adjustmentType: "DECREASE",
        quantity: 5,
        reason: "Damaged goods",
      }),
    });

    const { result } = renderHook(() => useCreateStockAdjustment(), { wrapper: wrapper() });

    result.current.mutate({
      itemId: 1,
      adjustmentType: "DECREASE",
      quantity: 5,
      reason: "Damaged goods",
    });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data?.adjustmentType).toBe("DECREASE");
  });
});

describe("useItemEnquiry", () => {
  it("fetches item enquiry results", async () => {
    fetchMock.mockResolvedValueOnce({
      ok: true,
      status: 200,
      json: async () => [
        {
          id: 1,
          name: "Paracetamol",
          code: "PCM-001",
          currentStock: 500,
          reorderLevel: 100,
          maxStock: 1000,
          status: "ACTIVE",
          batches: [{ id: 1, batchNumber: "B001", expiryDate: "2025-12-31", quantity: 500, location: "A1" }],
        },
      ],
    });

    const { result } = renderHook(
      () => useItemEnquiry({ keyword: "Para" }),
      { wrapper: wrapper() },
    );

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data).toHaveLength(1);
    expect(result.current.data?.[0].currentStock).toBe(500);
  });
});

describe("IndentForm", () => {
  it("renders indent form with all fields", () => {
    fetchMock.mockResolvedValue({ ok: true, status: 200, json: async () => [] });

    render(<IndentForm />, { wrapper: wrapper() });

    expect(screen.getByLabelText(/Store/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/Request Date/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/Priority/i)).toBeInTheDocument();
    expect(screen.getByText("Submit Indent")).toBeInTheDocument();
  });
});

describe("StockAdjustment", () => {
  it("renders stock adjustment form", () => {
    fetchMock.mockResolvedValue({ ok: true, status: 200, json: async () => [] });

    render(<StockAdjustment />, { wrapper: wrapper() });

    expect(screen.getByLabelText(/Item/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/Adjustment Type/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/Quantity/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/Reason/i)).toBeInTheDocument();
    expect(screen.getByText("Record Adjustment")).toBeInTheDocument();
  });
});
