import { describe, expect, it, vi, beforeEach, afterEach } from "vitest";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { renderHook, waitFor } from "@testing-library/react";
import { useFollowUpList, useScheduleFollowUp } from "./queries";

/**
 * Wire-level coverage for the OPD queries module. The backend is exercised in
 * appointment-service (M02ControllerTest, 9 tests). Here we prove the
 * TanStack-Query hooks hit the right URLs.
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

describe("useFollowUpList", () => {
  it("hits the by-appointment endpoint when state.appointmentId is set", async () => {
    fetchMock.mockResolvedValueOnce({
      ok: true,
      status: 200,
      json: async () => [{ id: 1, patientId: 5, followUpDate: "2027-04-01", followUpStatus: 1 }],
    });

    const { result } = renderHook(() => useFollowUpList({ appointmentId: 42 }), { wrapper: wrapper() });
    await waitFor(() => expect(result.current.isSuccess).toBe(true));

    expect(fetchMock).toHaveBeenCalledWith(
      "/api/api/v1/appointments/42/follow-ups",
      expect.objectContaining({ headers: expect.any(Object) }),
    );
    expect(result.current.data).toEqual([
      expect.objectContaining({ id: 1, followUpStatus: 1 }),
    ]);
  });

  it("hits the by-patient endpoint with date query param", async () => {
    fetchMock.mockResolvedValueOnce({ ok: true, status: 200, json: async () => [] });

    const { result } = renderHook(
      () => useFollowUpList({ patientId: 7, date: "2027-04-01" }),
      { wrapper: wrapper() },
    );
    await waitFor(() => expect(result.current.isSuccess).toBe(true));

    expect(fetchMock).toHaveBeenCalledWith(
      "/api/api/v1/appointments/follow-ups/patient/7?date=2027-04-01",
      expect.any(Object),
    );
  });

  it("is disabled when the filter is incomplete", async () => {
    fetchMock.mockResolvedValue({ ok: true, status: 200, json: async () => [] });

    const { result } = renderHook(() => useFollowUpList({ patientId: 7 }), { wrapper: wrapper() });
    // No fetch should have been issued
    expect(fetchMock).not.toHaveBeenCalled();
    expect(result.current.fetchStatus).toBe("idle");
  });
});

describe("useScheduleFollowUp", () => {
  it("POSTs the body and returns the saved entity", async () => {
    fetchMock.mockResolvedValueOnce({
      ok: true,
      status: 201,
      json: async () => ({ id: 99, patientId: 5, followUpDate: "2027-04-01", followUpStatus: 1 }),
    });

    const { result } = renderHook(() => useScheduleFollowUp(), { wrapper: wrapper() });
    result.current.mutate({ id: 42, payload: { patientId: 5, followUpDate: "2027-04-01" } });
    await waitFor(() => expect(result.current.isSuccess).toBe(true));

    expect(fetchMock).toHaveBeenCalledWith(
      "/api/api/v1/appointments/42/follow-up",
      expect.objectContaining({
        method: "POST",
        body: JSON.stringify({ patientId: 5, followUpDate: "2027-04-01" }),
      }),
    );
    expect(result.current.data?.id).toBe(99);
  });
});
