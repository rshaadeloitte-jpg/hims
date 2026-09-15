import { describe, expect, it, vi, beforeEach, afterEach } from "vitest";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { renderHook, waitFor } from "@testing-library/react";
import {
  useCounters,
  useTokens,
  useDashboard,
  useCabinBoard,
  useDoctorBatch,
  useWaitTime,
  useIssueToken,
  useCallNext,
  useCallPatient,
  useTokenAction,
  useTransferToken,
  useDepartmentDashboard,
  useDepartmentTokens,
} from "../queries";

/**
 * M27 Queue hooks — wire-level coverage.
 * Pins the REST URLs consumed by the queue-service (port 8107).
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

// ---- GET endpoints ---------------------------------------------------------

describe("useCounters", () => {
  it("fetches GET /api/v1/queue/counters", async () => {
    fetchMock.mockResolvedValueOnce({
      ok: true, status: 200,
      json: async () => [{ counterId: 1, counterName: "OPD-1", isActive: true }],
    });
    const { result } = renderHook(() => useCounters(), { wrapper: wrapper() });
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(fetchMock).toHaveBeenCalledWith("/api/api/v1/queue/counters", expect.any(Object));
    expect(result.current.data?.[0]?.counterName).toBe("OPD-1");
  });
});

describe("useTokens", () => {
  it("fetches GET /api/v1/queue/tokens", async () => {
    fetchMock.mockResolvedValueOnce({
      ok: true, status: 200,
      json: async () => [{ tokenId: 1, tokenNo: 42, patientId: 7, status: 1, priority: "NORMAL", counterId: 1, createdAt: "2027-01-01T09:00:00Z" }],
    });
    const { result } = renderHook(() => useTokens(), { wrapper: wrapper() });
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(fetchMock).toHaveBeenCalledWith("/api/api/v1/queue/tokens", expect.any(Object));
    expect(result.current.data?.[0]?.tokenNo).toBe(42);
  });
});

describe("useDashboard", () => {
  it("fetches GET /api/v1/queue/dashboard", async () => {
    fetchMock.mockResolvedValueOnce({
      ok: true, status: 200,
      json: async () => [{ counterId: 1, counterName: "OPD-1", nowServing: null, waiting: [], called: [] }],
    });
    const { result } = renderHook(() => useDashboard(0), { wrapper: wrapper() });
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(fetchMock).toHaveBeenCalledWith("/api/api/v1/queue/dashboard", expect.any(Object));
  });
});

describe("useCabinBoard", () => {
  it("is disabled when counterId is 0", () => {
    fetchMock.mockResolvedValue({ ok: true, status: 200, json: async () => ({}) });
    const { result } = renderHook(() => useCabinBoard(0), { wrapper: wrapper() });
    expect(fetchMock).not.toHaveBeenCalled();
    expect(result.current.fetchStatus).toBe("idle");
  });

  it("fetches GET /api/v1/queue/cabin/:id/board", async () => {
    fetchMock.mockResolvedValueOnce({
      ok: true, status: 200,
      json: async () => ({ counterId: 3, counterName: "Cabin-3", nowServing: null, waiting: [], called: [] }),
    });
    const { result } = renderHook(() => useCabinBoard(3), { wrapper: wrapper() });
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(fetchMock).toHaveBeenCalledWith("/api/api/v1/queue/cabin/3/board", expect.any(Object));
  });
});

describe("useDoctorBatch", () => {
  it("is disabled when doctorId is 0", () => {
    fetchMock.mockResolvedValue({ ok: true, status: 200, json: async () => [] });
    const { result } = renderHook(() => useDoctorBatch(0), { wrapper: wrapper() });
    expect(fetchMock).not.toHaveBeenCalled();
  });

  it("fetches GET /api/v1/queue/doctor/:id/batch", async () => {
    fetchMock.mockResolvedValueOnce({
      ok: true, status: 200,
      json: async () => [{ tokenId: 10, tokenNo: 5, patientId: 99, priority: "NORMAL", status: 1, createdAt: "2027-01-01T10:00:00Z" }],
    });
    const { result } = renderHook(() => useDoctorBatch(42), { wrapper: wrapper() });
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(fetchMock).toHaveBeenCalledWith("/api/api/v1/queue/doctor/42/batch", expect.any(Object));
    expect(result.current.data?.[0]?.patientId).toBe(99);
  });
});

describe("useWaitTime", () => {
  it("fetches GET /api/v1/queue/counters/:id/wait-time", async () => {
    fetchMock.mockResolvedValueOnce({
      ok: true, status: 200,
      json: async () => ({ counterId: 1, avgWaitMinutes: 12, currentWaiting: 5, estimatedWaitMinutes: 60 }),
    });
    const { result } = renderHook(() => useWaitTime(1), { wrapper: wrapper() });
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(fetchMock).toHaveBeenCalledWith("/api/api/v1/queue/counters/1/wait-time", expect.any(Object));
    expect(result.current.data?.estimatedWaitMinutes).toBe(60);
  });
});

describe("useDepartmentDashboard", () => {
  it("is disabled when departmentId is 0", () => {
    fetchMock.mockResolvedValue({ ok: true, status: 200, json: async () => ({}) });
    renderHook(() => useDepartmentDashboard(0), { wrapper: wrapper() });
    expect(fetchMock).not.toHaveBeenCalled();
  });
});

describe("useDepartmentTokens", () => {
  it("fetches GET /api/v1/queue/department/:id/tokens", async () => {
    fetchMock.mockResolvedValueOnce({
      ok: true, status: 200,
      json: async () => [],
    });
    const { result } = renderHook(() => useDepartmentTokens(5), { wrapper: wrapper() });
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(fetchMock).toHaveBeenCalledWith("/api/api/v1/queue/department/5/tokens", expect.any(Object));
  });
});

// ---- POST endpoints --------------------------------------------------------

describe("useIssueToken", () => {
  it("POSTs to /api/v1/queue/tokens", async () => {
    fetchMock.mockResolvedValueOnce({
      ok: true, status: 201,
      json: async () => ({ tokenId: 1, tokenNo: 1, patientId: 5, counterId: 1, priority: "NORMAL", status: 1, createdAt: "2027-01-01T09:00:00Z" }),
    });
    const { result } = renderHook(() => useIssueToken(), { wrapper: wrapper() });
    result.current.mutate({ counterId: 1, patientId: 5, priority: "NORMAL" });
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(fetchMock).toHaveBeenCalledWith(
      "/api/api/v1/queue/tokens",
      expect.objectContaining({
        method: "POST",
        body: JSON.stringify({ counterId: 1, patientId: 5, priority: "NORMAL" }),
      }),
    );
  });
});

describe("useCallNext", () => {
  it("POSTs to /api/v1/queue/counters/:id/call-next", async () => {
    fetchMock.mockResolvedValueOnce({
      ok: true, status: 200,
      json: async () => ({ tokenId: 2, tokenNo: 3, patientId: 7, counterId: 1, priority: "NORMAL", status: 2, createdAt: "2027-01-01T09:00:00Z" }),
    });
    const { result } = renderHook(() => useCallNext(1), { wrapper: wrapper() });
    result.current.mutate();
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(fetchMock).toHaveBeenCalledWith(
      "/api/api/v1/queue/counters/1/call-next",
      expect.objectContaining({ method: "POST" }),
    );
  });
});

describe("useCallPatient", () => {
  it("POSTs to /api/v1/queue/tokens/:id/call", async () => {
    fetchMock.mockResolvedValueOnce({
      ok: true, status: 200,
      json: async () => ({ tokenId: 5, tokenNo: 10, patientId: 3, counterId: 2, priority: "NORMAL", status: 2, createdAt: "2027-01-01T09:00:00Z" }),
    });
    const { result } = renderHook(() => useCallPatient(), { wrapper: wrapper() });
    result.current.mutate(5);
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(fetchMock).toHaveBeenCalledWith(
      "/api/api/v1/queue/tokens/5/call",
      expect.objectContaining({ method: "POST" }),
    );
  });
});

describe("useTokenAction", () => {
  it.each(["no-show", "complete", "transfer"] as const)("POSTs to /api/v1/queue/tokens/:id/%s", async (action) => {
    fetchMock.mockResolvedValueOnce({
      ok: true, status: 200,
      json: async () => ({ tokenId: 1, tokenNo: 1, patientId: 1, counterId: 1, priority: "NORMAL", status: 3, createdAt: "2027-01-01T09:00:00Z" }),
    });
    const { result } = renderHook(() => useTokenAction(action), { wrapper: wrapper() });
    result.current.mutate(1);
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(fetchMock).toHaveBeenCalledWith(
      `/api/api/v1/queue/tokens/1/${action}`,
      expect.objectContaining({ method: "POST" }),
    );
  });
});

describe("useTransferToken", () => {
  it("POSTs to /api/v1/queue/tokens/:id/transfer with targetCounterId", async () => {
    fetchMock.mockResolvedValueOnce({
      ok: true, status: 200,
      json: async () => ({ tokenId: 1, tokenNo: 1, patientId: 1, counterId: 2, priority: "NORMAL", status: 1, createdAt: "2027-01-01T09:00:00Z" }),
    });
    const { result } = renderHook(() => useTransferToken(), { wrapper: wrapper() });
    result.current.mutate({ tokenId: 1, targetCounterId: 2 });
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(fetchMock).toHaveBeenCalledWith(
      "/api/api/v1/queue/tokens/1/transfer",
      expect.objectContaining({
        method: "POST",
        body: JSON.stringify({ targetCounterId: 2 }),
      }),
    );
  });
});