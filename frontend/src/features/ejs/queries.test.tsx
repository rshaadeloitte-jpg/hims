import { describe, expect, it, vi, beforeEach, afterEach } from "vitest";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { renderHook, waitFor } from "@testing-library/react";
import {
  useAshaWorkList,
  useAshaWorkDetail,
  useCreateAshaWork,
  useUpdateAshaWork,
  useIncentiveList,
  useIncentiveDetail,
  useApproveIncentive,
  usePayIncentive,
  useGeographyList,
  useGeographyDetail,
  useCreateGeography,
  useUpdateGeography,
  useAshaWorkers,
} from "./queries";

/**
 * M36 EJS/ASHA queries — wire-level coverage.
 * Pins the REST URLs consumed by the EJS service.
 */

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

// ---- AshaWork endpoints ----

describe("useAshaWorkList", () => {
  it("fetches GET /api/v1/ejs/asha-work with filters", async () => {
    fetchMock.mockResolvedValueOnce({
      ok: true, status: 200,
      json: async () => [{ id: 1, beneficiaryName: "Priya", trackingType: "ANC", status: "SCHEDULED" }],
    });
    const { result } = renderHook(() => useAshaWorkList({ trackingType: "ANC", page: 0, size: 20 }), { wrapper: wrapper() });
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(fetchMock).toHaveBeenCalledWith(
      expect.stringContaining("/api/v1/ejs/asha-work"),
      expect.any(Object),
    );
    expect(result.current.data?.[0]?.beneficiaryName).toBe("Priya");
  });
});

describe("useAshaWorkDetail", () => {
  it("fetches GET /api/v1/ejs/asha-work/:id", async () => {
    fetchMock.mockResolvedValueOnce({
      ok: true, status: 200,
      json: async () => ({ id: 1, beneficiaryName: "Priya", trackingType: "ANC" }),
    });
    const { result } = renderHook(() => useAshaWorkDetail(1), { wrapper: wrapper() });
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(fetchMock).toHaveBeenCalledWith(
      expect.stringContaining("/api/v1/ejs/asha-work/1"),
      expect.any(Object),
    );
  });

  it("is disabled when id is 0", () => {
    fetchMock.mockResolvedValue({ ok: true, status: 200, json: async () => ({}) });
    renderHook(() => useAshaWorkDetail(0), { wrapper: wrapper() });
    expect(fetchMock).not.toHaveBeenCalled();
  });
});

describe("useCreateAshaWork", () => {
  it("POSTs to /api/v1/ejs/asha-work", async () => {
    fetchMock.mockResolvedValueOnce({
      ok: true, status: 201,
      json: async () => ({ id: 1, beneficiaryName: "Priya", trackingType: "ANC" }),
    });
    const { result } = renderHook(() => useCreateAshaWork(), { wrapper: wrapper() });
    result.current.mutate({ beneficiaryName: "Priya", trackingType: "ANC" });
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(fetchMock).toHaveBeenCalledWith(
      expect.stringContaining("/api/v1/ejs/asha-work"),
      expect.objectContaining({ method: "POST" }),
    );
  });
});

describe("useUpdateAshaWork", () => {
  it("PUTs to /api/v1/ejs/asha-work/:id", async () => {
    fetchMock.mockResolvedValueOnce({
      ok: true, status: 200,
      json: async () => ({ id: 1, beneficiaryName: "Priya", trackingType: "PNC" }),
    });
    const { result } = renderHook(() => useUpdateAshaWork(1), { wrapper: wrapper() });
    result.current.mutate({ trackingType: "PNC" });
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(fetchMock).toHaveBeenCalledWith(
      expect.stringContaining("/api/v1/ejs/asha-work/1"),
      expect.objectContaining({ method: "PUT" }),
    );
  });
});

// ---- Incentive endpoints ----

describe("useIncentiveList", () => {
  it("fetches GET /api/v1/ejs/incentives with filters", async () => {
    fetchMock.mockResolvedValueOnce({
      ok: true, status: 200,
      json: async () => [{ id: 1, ashaName: "Sunita", month: "2026-09", totalIncentive: 7100 }],
    });
    const { result } = renderHook(() => useIncentiveList({ month: "2026-09" }), { wrapper: wrapper() });
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(fetchMock).toHaveBeenCalledWith(
      expect.stringContaining("/api/v1/ejs/incentives"),
      expect.any(Object),
    );
    expect(result.current.data?.[0]?.totalIncentive).toBe(7100);
  });
});

describe("useIncentiveDetail", () => {
  it("fetches GET /api/v1/ejs/incentives/:id", async () => {
    fetchMock.mockResolvedValueOnce({
      ok: true, status: 200,
      json: async () => ({ id: 1, ashaName: "Sunita", totalIncentive: 7100 }),
    });
    const { result } = renderHook(() => useIncentiveDetail(1), { wrapper: wrapper() });
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(fetchMock).toHaveBeenCalledWith(
      expect.stringContaining("/api/v1/ejs/incentives/1"),
      expect.any(Object),
    );
  });
});

describe("useApproveIncentive", () => {
  it("POSTs to /api/v1/ejs/incentives/:id/approve", async () => {
    fetchMock.mockResolvedValueOnce({
      ok: true, status: 200,
      json: async () => ({ id: 1, status: "APPROVED" }),
    });
    const { result } = renderHook(() => useApproveIncentive(1), { wrapper: wrapper() });
    result.current.mutate();
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(fetchMock).toHaveBeenCalledWith(
      expect.stringContaining("/api/v1/ejs/incentives/1/approve"),
      expect.objectContaining({ method: "POST" }),
    );
  });
});

describe("usePayIncentive", () => {
  it("POSTs to /api/v1/ejs/incentives/:id/pay", async () => {
    fetchMock.mockResolvedValueOnce({
      ok: true, status: 200,
      json: async () => ({ id: 1, status: "PAID" }),
    });
    const { result } = renderHook(() => usePayIncentive(1), { wrapper: wrapper() });
    result.current.mutate();
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(fetchMock).toHaveBeenCalledWith(
      expect.stringContaining("/api/v1/ejs/incentives/1/pay"),
      expect.objectContaining({ method: "POST" }),
    );
  });
});

// ---- Geography endpoints ----

describe("useGeographyList", () => {
  it("fetches GET /api/v1/ejs/geography with filters", async () => {
    fetchMock.mockResolvedValueOnce({
      ok: true, status: 200,
      json: async () => [{ id: 1, village: "Pragpura", totalPopulation: 2450 }],
    });
    const { result } = renderHook(() => useGeographyList({ state: "Rajasthan" }), { wrapper: wrapper() });
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(fetchMock).toHaveBeenCalledWith(
      expect.stringContaining("/api/v1/ejs/geography"),
      expect.any(Object),
    );
    expect(result.current.data?.[0]?.village).toBe("Pragpura");
  });
});

describe("useGeographyDetail", () => {
  it("fetches GET /api/v1/ejs/geography/:id", async () => {
    fetchMock.mockResolvedValueOnce({
      ok: true, status: 200,
      json: async () => ({ id: 1, village: "Pragpura" }),
    });
    const { result } = renderHook(() => useGeographyDetail(1), { wrapper: wrapper() });
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(fetchMock).toHaveBeenCalledWith(
      expect.stringContaining("/api/v1/ejs/geography/1"),
      expect.any(Object),
    );
  });
});

describe("useCreateGeography", () => {
  it("POSTs to /api/v1/ejs/geography", async () => {
    fetchMock.mockResolvedValueOnce({
      ok: true, status: 201,
      json: async () => ({ id: 1, village: "Pragpura" }),
    });
    const { result } = renderHook(() => useCreateGeography(), { wrapper: wrapper() });
    result.current.mutate({ village: "Pragpura" });
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(fetchMock).toHaveBeenCalledWith(
      expect.stringContaining("/api/v1/ejs/geography"),
      expect.objectContaining({ method: "POST" }),
    );
  });
});

describe("useUpdateGeography", () => {
  it("PUTs to /api/v1/ejs/geography/:id", async () => {
    fetchMock.mockResolvedValueOnce({
      ok: true, status: 200,
      json: async () => ({ id: 1, village: "Pragpura" }),
    });
    const { result } = renderHook(() => useUpdateGeography(1), { wrapper: wrapper() });
    result.current.mutate({ village: "Pragpura" });
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(fetchMock).toHaveBeenCalledWith(
      expect.stringContaining("/api/v1/ejs/geography/1"),
      expect.objectContaining({ method: "PUT" }),
    );
  });
});

// ---- ASHA workers ----

describe("useAshaWorkers", () => {
  it("fetches GET /api/v1/ejs/asha-workers", async () => {
    fetchMock.mockResolvedValueOnce({
      ok: true, status: 200,
      json: async () => [{ ashaId: 10, ashaName: "Sunita Devi" }],
    });
    const { result } = renderHook(() => useAshaWorkers(), { wrapper: wrapper() });
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(fetchMock).toHaveBeenCalledWith(
      expect.stringContaining("/api/v1/ejs/asha-workers"),
      expect.any(Object),
    );
    expect(result.current.data?.[0]?.ashaName).toBe("Sunita Devi");
  });
});
