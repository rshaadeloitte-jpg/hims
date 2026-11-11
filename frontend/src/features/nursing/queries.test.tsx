import { describe, expect, it, vi, beforeEach, afterEach } from "vitest";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { renderHook, waitFor } from "@testing-library/react";
import type { ReactNode } from "react";
import { useCarePlans, useShiftHandovers } from "./queries";

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

describe("useCarePlans", () => {
  it("is disabled when no admissionId", () => {
    fetchMock.mockResolvedValue({ ok: true, status: 200, json: async () => [] });
    const { result } = renderHook(() => useCarePlans(), { wrapper: wrapper() });
    expect(fetchMock).not.toHaveBeenCalled();
    expect(result.current.fetchStatus).toBe("idle");
  });

  it("fetches with admissionId query param", async () => {
    fetchMock.mockResolvedValueOnce({ ok: true, status: 200, json: async () => [] });
    const { result } = renderHook(() => useCarePlans(5), { wrapper: wrapper() });
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(fetchMock).toHaveBeenCalledWith("/api/v1/care-plans?admissionId=5", expect.any(Object));
  });
});

describe("useShiftHandovers", () => {
  it("is disabled when no admissionId", () => {
    fetchMock.mockResolvedValue({ ok: true, status: 200, json: async () => [] });
    const { result } = renderHook(() => useShiftHandovers(), { wrapper: wrapper() });
    expect(fetchMock).not.toHaveBeenCalled();
    expect(result.current.fetchStatus).toBe("idle");
  });

  it("fetches with admissionId query param", async () => {
    fetchMock.mockResolvedValueOnce({ ok: true, status: 200, json: async () => [] });
    const { result } = renderHook(() => useShiftHandovers(7), { wrapper: wrapper() });
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(fetchMock).toHaveBeenCalledWith("/api/v1/shift-handovers?admissionId=7", expect.any(Object));
  });
});
