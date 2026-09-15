import { describe, expect, it, vi, beforeEach, afterEach } from "vitest";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { renderHook, waitFor } from "@testing-library/react";
import type { ReactNode } from "react";
import {
  useCases,
  useCase,
  useCreateCase,
  useETickets,
  useMonitors,
  useDevices,
  useCreateDevice,
} from "./queries";

function wrapper() {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return ({ children }: { children: ReactNode }) => (
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

describe("useCases", () => {
  it("fetches cases from API", async () => {
    fetchMock.mockResolvedValueOnce({
      ok: true,
      status: 200,
      json: async () => [
        { caseId: 1, patientId: 100, caseType: "ANC", status: 1, openedOn: "2024-01-15" },
      ],
    });

    const { result } = renderHook(() => useCases({ page: 0, size: 20 }), { wrapper: wrapper() });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));

    expect(fetchMock).toHaveBeenCalledWith(
      "/api/cases?page=0&size=20",
      expect.any(Object),
    );
    expect(result.current.data).toHaveLength(1);
    expect(result.current.data?.[0].caseId).toBe(1);
  });

  it("includes filter params in URL", async () => {
    fetchMock.mockResolvedValueOnce({
      ok: true,
      status: 200,
      json: async () => [],
    });

    const { result } = renderHook(
      () => useCases({ status: "1", caseType: "ANC", page: 0, size: 20 }),
      { wrapper: wrapper() },
    );

    await waitFor(() => expect(result.current.isSuccess).toBe(true));

    expect(fetchMock).toHaveBeenCalledWith(
      "/api/cases?status=1&caseType=ANC&page=0&size=20",
      expect.any(Object),
    );
  });
});

describe("useCase", () => {
  it("fetches single case", async () => {
    fetchMock.mockResolvedValueOnce({
      ok: true,
      status: 200,
      json: async () => ({ caseId: 1, patientId: 100, caseType: "ANC", status: 1 }),
    });

    const { result } = renderHook(() => useCase(1), { wrapper: wrapper() });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));

    expect(fetchMock).toHaveBeenCalledWith("/api/cases/1", expect.any(Object));
    expect(result.current.data?.caseId).toBe(1);
  });

  it("is disabled when id is 0", () => {
    const { result } = renderHook(() => useCase(0), { wrapper: wrapper() });

    expect(fetchMock).not.toHaveBeenCalled();
    expect(result.current.fetchStatus).toBe("idle");
  });
});

describe("useCreateCase", () => {
  it("creates a new case", async () => {
    fetchMock.mockResolvedValueOnce({
      ok: true,
      status: 201,
      json: async () => ({ caseId: 10, patientId: 100, caseType: "ANC", status: 1 }),
    });

    const { result } = renderHook(() => useCreateCase(), { wrapper: wrapper() });

    result.current.mutate({
      patientId: 100,
      caseType: "ANC",
      status: 1,
    });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));

    expect(fetchMock).toHaveBeenCalledWith(
      "/api/cases",
      expect.objectContaining({
        method: "POST",
        body: JSON.stringify({ patientId: 100, caseType: "ANC", status: 1 }),
      }),
    );
    expect(result.current.data?.caseId).toBe(10);
  });
});

describe("useETickets", () => {
  it("fetches e-tickets", async () => {
    fetchMock.mockResolvedValueOnce({
      ok: true,
      status: 200,
      json: async () => [
        { ticketId: 1, subject: "Test", priority: "HIGH", status: "OPEN" },
      ],
    });

    const { result } = renderHook(() => useETickets(), { wrapper: wrapper() });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));

    expect(fetchMock).toHaveBeenCalledWith("/api/e-tickets", expect.any(Object));
    expect(result.current.data).toHaveLength(1);
  });
});

describe("useMonitors", () => {
  it("fetches monitors without case filter", async () => {
    fetchMock.mockResolvedValueOnce({
      ok: true,
      status: 200,
      json: async () => [
        { monitorId: 1, caseId: 1, metric: "BP", value: 120 },
      ],
    });

    const { result } = renderHook(() => useMonitors(), { wrapper: wrapper() });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));

    expect(fetchMock).toHaveBeenCalledWith("/api/monitor", expect.any(Object));
    expect(result.current.data).toHaveLength(1);
  });

  it("fetches monitors with case filter", async () => {
    fetchMock.mockResolvedValueOnce({
      ok: true,
      status: 200,
      json: async () => [],
    });

    const { result } = renderHook(() => useMonitors(5), { wrapper: wrapper() });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));

    expect(fetchMock).toHaveBeenCalledWith("/api/monitor?caseId=5", expect.any(Object));
  });
});

describe("useDevices", () => {
  it("fetches devices", async () => {
    fetchMock.mockResolvedValueOnce({
      ok: true,
      status: 200,
      json: async () => [
        { deviceId: 1, deviceName: "BP Monitor", deviceType: "BP_MONITOR", serialNo: "BP001", status: "ACTIVE" },
      ],
    });

    const { result } = renderHook(() => useDevices(), { wrapper: wrapper() });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));

    expect(fetchMock).toHaveBeenCalledWith("/api/devices", expect.any(Object));
    expect(result.current.data).toHaveLength(1);
  });
});

describe("useCreateDevice", () => {
  it("registers a new device", async () => {
    fetchMock.mockResolvedValueOnce({
      ok: true,
      status: 201,
      json: async () => ({ deviceId: 5, deviceName: "New Device", deviceType: "BP_MONITOR", serialNo: "ND001", status: "ACTIVE" }),
    });

    const { result } = renderHook(() => useCreateDevice(), { wrapper: wrapper() });

    result.current.mutate({
      deviceName: "New Device",
      deviceType: "BP_MONITOR",
      serialNo: "ND001",
      status: "ACTIVE",
    });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));

    expect(fetchMock).toHaveBeenCalledWith(
      "/api/devices",
      expect.objectContaining({
        method: "POST",
        body: JSON.stringify({
          deviceName: "New Device",
          deviceType: "BP_MONITOR",
          serialNo: "ND001",
          status: "ACTIVE",
        }),
      }),
    );
    expect(result.current.data?.deviceId).toBe(5);
  });
});
