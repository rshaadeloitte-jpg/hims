import { describe, expect, it, vi, beforeEach, afterEach } from "vitest";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { renderHook, waitFor } from "@testing-library/react";
import type { ReactNode } from "react";
import { useCensus, useBeds } from "./queries";

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

describe("useCensus", () => {
  it("fetches census object and exposes wards array", async () => {
    fetchMock.mockResolvedValueOnce({
      ok: true,
      status: 200,
      json: async () => ({ wards: [], totalActiveAdmissions: 0, interimCount: 0, generatedAt: "2026-01-01T00:00:00Z" }),
    });

    const { result } = renderHook(() => useCensus(), { wrapper: wrapper() });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));

    expect(fetchMock).toHaveBeenCalledWith("/api/v1/adt/census", expect.any(Object));
    expect(result.current.data?.wards).toEqual([]);
  });

  it("renders wards from census.wards", async () => {
    fetchMock.mockResolvedValueOnce({
      ok: true,
      status: 200,
      json: async () => ({
        wards: [{ wardId: 1, wardName: "Ward A", totalBeds: 10, occupied: 4, free: 6, cleaning: 0 }],
      }),
    });

    const { result } = renderHook(() => useCensus(), { wrapper: wrapper() });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));

    expect(result.current.data?.wards).toHaveLength(1);
    expect(result.current.data?.wards[0].wardName).toBe("Ward A");
  });
});

describe("useBeds", () => {
  it("fetches beds", async () => {
    fetchMock.mockResolvedValueOnce({
      ok: true,
      status: 200,
      json: async () => [],
    });

    const { result } = renderHook(() => useBeds(), { wrapper: wrapper() });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));

    expect(fetchMock).toHaveBeenCalledWith("/api/v1/beds", expect.any(Object));
    expect(result.current.data).toEqual([]);
  });
});
