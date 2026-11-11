import { describe, expect, it, vi, beforeEach, afterEach } from "vitest";
import { renderHook, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { createElement } from "react";
import { useWasteSummary, useWasteList } from "./queries";

function wrapper() {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return ({ children }: { children: React.ReactNode }) =>
    createElement(QueryClientProvider, { client: qc }, children);
}

const fetchMock = vi.fn();

beforeEach(() => {
  fetchMock.mockReset();
  vi.stubGlobal("fetch", fetchMock);
});

afterEach(() => {
  vi.unstubAllGlobals();
});

describe("BMW queries", () => {
  describe("useWasteSummary", () => {
    it("fetches GET /api/v1/biomedical-waste/counts and maps keys", async () => {
      fetchMock.mockResolvedValueOnce({
        ok: true,
        status: 200,
        json: async () => ({
          TOTAL: 10,
          PENDING: 3,
          ACCEPTED: 2,
          HANDOVERED: 2,
          RECEIVED: 2,
          COMPLETED: 1,
        }),
      });

      const { result } = renderHook(
        () => useWasteSummary(1, 1, "2026-01-01", "2026-01-31"),
        { wrapper: wrapper() },
      );

      await waitFor(() => expect(result.current.isSuccess).toBe(true));

      expect(fetchMock).toHaveBeenCalledTimes(1);
      const [url, init] = fetchMock.mock.calls[0];
      expect(String(url)).toContain("/api/v1/biomedical-waste/counts");
      expect(init?.method).toBeUndefined(); // default GET
      expect(result.current.data).toEqual({
        totalCount: 10,
        pendingCount: 3,
        acceptedCount: 2,
        handOveredCount: 2,
        receivedCount: 2,
        completedCount: 1,
      });
    });
  });

  describe("useWasteList", () => {
    it("fetches GET /api/v1/biomedical-waste with query params", async () => {
      fetchMock.mockResolvedValueOnce({
        ok: true,
        status: 200,
        json: async () => [
          {
            id: 101,
            requisitionNo: "IHMS/BW/26/00001",
            storeId: 1,
            toStoreId: 2,
            requestDate: "2026-11-10T10:00:00.000Z",
            requestStatus: 1,
            handedOverTo: null,
            status: true,
            unitId: 1,
          },
        ],
      });

      const { result } = renderHook(
        () =>
          useWasteList({
            unitId: 1,
            category: "pending",
            search: "BW/26",
            page: 0,
            size: 25,
          }),
        { wrapper: wrapper() },
      );

      await waitFor(() => expect(result.current.isSuccess).toBe(true));

      expect(fetchMock).toHaveBeenCalledTimes(1);
      const [url, init] = fetchMock.mock.calls[0];
      const urlStr = String(url);
      expect(urlStr).toContain("/api/v1/biomedical-waste?");
      expect(urlStr).toContain("unitId=1");
      expect(urlStr).toContain("requestStatus=1");
      expect(urlStr).toContain("search=BW%2F26");
      expect(init?.method).toBeUndefined(); // default GET
      expect(result.current.data?.total).toBe(1);
      expect(result.current.data?.items[0].requisitionNo).toBe("IHMS/BW/26/00001");
      expect(result.current.data?.items[0].date).toBe("2026-11-10T10:00:00.000Z");
    });
  });
});
