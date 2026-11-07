import { describe, expect, it, vi, beforeEach, afterEach } from "vitest";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { renderHook, waitFor } from "@testing-library/react";
import { useDonors, useBloodBags, useIndents, useCreateDonor } from "../queries";

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

describe("bloodbank queries connect to backend", () => {
  it("donor list maps Java fields", async () => {
    fetchMock.mockResolvedValueOnce({
      ok: true,
      status: 200,
      json: async () => [{ donorId: 1, name: "Riya", bloodGroup: "A+", donationStatus: 1, lastActionAt: "2026-01-01T00:00:00Z" }],
    });
    const { result } = renderHook(() => useDonors(), { wrapper: wrapper() });
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(fetchMock).toHaveBeenCalledWith("/api/v1/donors", expect.any(Object));
    expect(result.current.data?.[0].id).toBe(1);
    expect(result.current.data?.[0].name).toBe("Riya");
    expect(result.current.data?.[0].bloodGroup).toBe("A+");
    expect(result.current.data?.[0].registeredAt).toBe("2026-01-01T00:00:00Z");
  });

  it("bag list derives status", async () => {
    fetchMock.mockResolvedValueOnce({
      ok: true,
      status: 200,
      json: async () => [
        { bagId: 2, donorId: 1, bloodGroup: "O+", volumeMl: 450, collectedAt: "2026-01-01T00:00:00Z", expirydate: "2026-12-01", crossMatched: false, isbagexpired: false, issued: false },
        { bagId: 3, donorId: 1, bloodGroup: "O+", volumeMl: 450, collectedAt: "2026-01-01T00:00:00Z", expirydate: "2026-12-01", crossMatched: true, isbagexpired: false, issued: false },
        { bagId: 4, donorId: 1, bloodGroup: "O+", volumeMl: 450, collectedAt: "2026-01-01T00:00:00Z", expirydate: "2026-12-01", crossMatched: false, isbagexpired: true, issued: false },
        { bagId: 5, donorId: 1, bloodGroup: "O+", volumeMl: 450, collectedAt: "2026-01-01T00:00:00Z", expirydate: "2026-12-01", crossMatched: false, isbagexpired: false, issued: true },
      ],
    });
    const { result } = renderHook(() => useBloodBags(), { wrapper: wrapper() });
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(fetchMock).toHaveBeenCalledWith("/api/v1/bags", expect.any(Object));
    const statuses = result.current.data?.map((b) => b.status);
    expect(statuses).toEqual(["AVAILABLE", "RESERVED", "EXPIRED", "ISSUED"]);
  });

  it("indent list derives status", async () => {
    fetchMock.mockResolvedValueOnce({
      ok: true,
      status: 200,
      json: async () => [{ id: 7, indentNo: "I1", fromStoreId: 1, toStoreId: 2, bloodGroup: "A+", component: "WHOLE_BLOOD", quantity: 2, requestedDate: "2026-01-02", remarks: "note", docStatus: 3 }],
    });
    const { result } = renderHook(() => useIndents(), { wrapper: wrapper() });
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(fetchMock).toHaveBeenCalledWith("/api/v1/bloodbank/indents", expect.any(Object));
    expect(result.current.data?.[0].id).toBe(7);
    expect(result.current.data?.[0].units).toBe(2);
    expect(result.current.data?.[0].status).toBe("DISPATCHED");
    expect(result.current.data?.[0].note).toBe("note");
  });

  it("donor create sends only real fields", async () => {
    fetchMock.mockResolvedValueOnce({
      ok: true,
      status: 201,
      json: async () => ({ donorId: 9, name: "Riya", bloodGroup: "A+", donationStatus: 1, lastActionAt: "2026-01-01T00:00:00Z" }),
    });
    const { result } = renderHook(() => useCreateDonor(), { wrapper: wrapper() });
    result.current.mutate({ name: "Riya", bloodGroup: "A+" });
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    const body = JSON.parse((fetchMock.mock.calls[0][1] as { body: string }).body);
    expect(body).toEqual({ name: "Riya", bloodGroup: "A+" });
    expect(fetchMock.mock.calls[0][0]).toBe("/api/v1/donors");
  });

  it("queries throw on !res.ok", async () => {
    fetchMock.mockResolvedValueOnce({ ok: false, status: 500, json: async () => ({ message: "boom" }) });
    const { result } = renderHook(() => useDonors(), { wrapper: wrapper() });
    await waitFor(() => expect(result.current.isError).toBe(true));
  });
});
