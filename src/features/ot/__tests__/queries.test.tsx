import { describe, expect, it, vi, beforeEach, afterEach } from "vitest";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { renderHook, waitFor } from "@testing-library/react";
import {
  useOtSchedules,
  useOtSchedule,
  useCreateOtSchedule,
  useStartSurgery,
  useCompleteSurgery,
  useCancelSurgery,
  useAnesthesiaRecords,
  useCreatePreOp,
  useVitalSigns,
  useAddVitalSign,
  useChecklists,
  useCreateChecklist,
  useCompleteChecklistItem,
  useOtRooms,
  useStaffSchedule,
  useNurseSchedule,
  useBillingEstimates,
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

describe("useOtSchedules", () => {
  it("fetches OT schedules from API", async () => {
    fetchMock.mockResolvedValueOnce({
      ok: true,
      status: 200,
      json: async () => [
        {
          scheduleId: 1,
          patientId: 100,
          patientName: "John Doe",
          otTable: "OT-1",
          scheduledStart: "2026-09-11T08:00:00",
          scheduledEnd: "2026-09-11T10:00:00",
          status: 1,
        },
      ],
    });

    const { result } = renderHook(() => useOtSchedules("2026-09-11"), { wrapper: wrapper() });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));

    expect(fetchMock).toHaveBeenCalledWith(
      expect.stringContaining("/api/v1/ot/schedules"),
      expect.any(Object)
    );
    expect(result.current.data).toHaveLength(1);
    expect(result.current.data?.[0].scheduleId).toBe(1);
  });

  it("includes date and status query params when provided", async () => {
    fetchMock.mockResolvedValueOnce({
      ok: true,
      status: 200,
      json: async () => [],
    });

    const { result } = renderHook(() => useOtSchedules("2026-09-11", 1), { wrapper: wrapper() });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));

    expect(fetchMock).toHaveBeenCalledWith(
      expect.stringContaining("date=2026-09-11"),
      expect.any(Object)
    );
    expect(fetchMock).toHaveBeenCalledWith(
      expect.stringContaining("status=1"),
      expect.any(Object)
    );
  });
});

describe("useOtSchedule", () => {
  it("fetches single schedule with details", async () => {
    fetchMock.mockResolvedValueOnce({
      ok: true,
      status: 200,
      json: async () => ({
        scheduleId: 1,
        patientId: 100,
        otTable: "OT-1",
        scheduledStart: "2026-09-11T08:00:00",
        scheduledEnd: "2026-09-11T10:00:00",
        status: 1,
        details: [{ detailId: 1, procedureCode: "APPEND", procedureName: "Appendectomy" }],
      }),
    });

    const { result } = renderHook(() => useOtSchedule(1), { wrapper: wrapper() });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));

    expect(fetchMock).toHaveBeenCalledWith(
      expect.stringContaining("/api/v1/ot/schedules/1"),
      expect.any(Object)
    );
    expect(result.current.data?.scheduleId).toBe(1);
    expect(result.current.data?.details).toHaveLength(1);
  });

  it("is disabled when id is 0", () => {
    const { result } = renderHook(() => useOtSchedule(0), { wrapper: wrapper() });

    expect(fetchMock).not.toHaveBeenCalled();
    expect(result.current.fetchStatus).toBe("idle");
  });
});

describe("useCreateOtSchedule", () => {
  it("creates new OT schedule", async () => {
    fetchMock.mockResolvedValueOnce({
      ok: true,
      status: 201,
      json: async () => ({
        scheduleId: 10,
        patientId: 100,
        otTable: "OT-1",
        scheduledStart: "2026-09-11T08:00:00",
        scheduledEnd: "2026-09-11T10:00:00",
        status: 1,
      }),
    });

    const { result } = renderHook(() => useCreateOtSchedule(), { wrapper: wrapper() });

    result.current.mutate({
      patientId: 100,
      encounterId: 200,
      otTable: "OT-1",
      scheduledStart: "2026-09-11T08:00",
      scheduledEnd: "2026-09-11T10:00",
    });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));

    expect(fetchMock).toHaveBeenCalledWith(
      expect.stringContaining("/api/v1/ot/schedules"),
      expect.objectContaining({ method: "POST" })
    );
    expect(result.current.data?.scheduleId).toBe(10);
  });
});

describe("useStartSurgery", () => {
  it("starts surgery via POST", async () => {
    fetchMock.mockResolvedValueOnce({
      ok: true,
      status: 200,
      json: async () => ({ scheduleId: 1, status: 2 }),
    });

    const { result } = renderHook(() => useStartSurgery(), { wrapper: wrapper() });

    result.current.mutate(1);

    await waitFor(() => expect(result.current.isSuccess).toBe(true));

    expect(fetchMock).toHaveBeenCalledWith(
      expect.stringContaining("/api/v1/ot/schedules/1/start"),
      expect.objectContaining({ method: "POST" })
    );
  });
});

describe("useCompleteSurgery", () => {
  it("completes surgery via POST", async () => {
    fetchMock.mockResolvedValueOnce({
      ok: true,
      status: 200,
      json: async () => ({ scheduleId: 1, status: 3 }),
    });

    const { result } = renderHook(() => useCompleteSurgery(), { wrapper: wrapper() });

    result.current.mutate(1);

    await waitFor(() => expect(result.current.isSuccess).toBe(true));

    expect(fetchMock).toHaveBeenCalledWith(
      expect.stringContaining("/api/v1/ot/schedules/1/complete"),
      expect.objectContaining({ method: "POST" })
    );
  });
});

describe("useCancelSurgery", () => {
  it("cancels surgery via POST", async () => {
    fetchMock.mockResolvedValueOnce({
      ok: true,
      status: 200,
      json: async () => ({ scheduleId: 1, status: 4 }),
    });

    const { result } = renderHook(() => useCancelSurgery(), { wrapper: wrapper() });

    result.current.mutate(1);

    await waitFor(() => expect(result.current.isSuccess).toBe(true));

    expect(fetchMock).toHaveBeenCalledWith(
      expect.stringContaining("/api/v1/ot/schedules/1/cancel"),
      expect.objectContaining({ method: "POST" })
    );
  });
});

describe("useAnesthesiaRecords", () => {
  it("fetches anesthesia records", async () => {
    fetchMock.mockResolvedValueOnce({
      ok: true,
      status: 200,
      json: async () => [
        { recordId: 1, patientId: 100, asaGrade: "ASA_II", airwayAssessment: "Normal" },
      ],
    });

    const { result } = renderHook(() => useAnesthesiaRecords(), { wrapper: wrapper() });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));

    expect(result.current.data).toHaveLength(1);
    expect(result.current.data?.[0].recordId).toBe(1);
  });
});

describe("useCreatePreOp", () => {
  it("creates pre-op assessment", async () => {
    fetchMock.mockResolvedValueOnce({
      ok: true,
      status: 200,
      json: async () => ({ recordId: 5, patientId: 100, asaGrade: "ASA_II" }),
    });

    const { result } = renderHook(() => useCreatePreOp(), { wrapper: wrapper() });

    result.current.mutate({
      scheduleId: 1,
      patientId: 100,
      asaGrade: "ASA_II",
      airway: "Normal",
    });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));

    expect(fetchMock).toHaveBeenCalledWith(
      expect.stringContaining("/ot/anesthesia/pre-op"),
      expect.objectContaining({ method: "POST" })
    );
    expect(result.current.data?.recordId).toBe(5);
  });
});

describe("useVitalSigns", () => {
  it("fetches vital signs for a record", async () => {
    fetchMock.mockResolvedValueOnce({
      ok: true,
      status: 200,
      json: async () => [
        { vitalId: 1, recordId: 1, heartRate: 72, bpSystolic: 120, bpDiastolic: 80, spo2: 98 },
      ],
    });

    const { result } = renderHook(() => useVitalSigns(1), { wrapper: wrapper() });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));

    expect(fetchMock).toHaveBeenCalledWith(
      expect.stringContaining("/ot/anesthesia/1/vitals"),
      expect.any(Object)
    );
    expect(result.current.data).toHaveLength(1);
  });

  it("is disabled when recordId is 0", () => {
    const { result } = renderHook(() => useVitalSigns(0), { wrapper: wrapper() });

    expect(fetchMock).not.toHaveBeenCalled();
    expect(result.current.fetchStatus).toBe("idle");
  });
});

describe("useAddVitalSign", () => {
  it("adds vital sign to a record", async () => {
    fetchMock.mockResolvedValueOnce({
      ok: true,
      status: 200,
      json: async () => ({ vitalId: 1, recordId: 1, heartRate: 75 }),
    });

    const { result } = renderHook(() => useAddVitalSign(), { wrapper: wrapper() });

    result.current.mutate({
      recordId: 1,
      data: { recordedAt: "2026-09-11T08:30", heartRate: 75, bpSystolic: 120, bpDiastolic: 80, spo2: 98, respiratoryRate: 16, temperature: "36.5" },
    });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));

    expect(fetchMock).toHaveBeenCalledWith(
      expect.stringContaining("/ot/anesthesia/1/vitals"),
      expect.objectContaining({ method: "POST" })
    );
  });
});

describe("useChecklists", () => {
  it("fetches surgical checklists", async () => {
    fetchMock.mockResolvedValueOnce({
      ok: true,
      status: 200,
      json: async () => [
        { checklistId: 1, scheduleId: 1, patientId: 100, phase: "SIGN_IN", completed: false },
      ],
    });

    const { result } = renderHook(() => useChecklists(), { wrapper: wrapper() });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));

    expect(result.current.data).toHaveLength(1);
    expect(result.current.data?.[0].phase).toBe("SIGN_IN");
  });
});

describe("useCreateChecklist", () => {
  it("creates new checklist", async () => {
    fetchMock.mockResolvedValueOnce({
      ok: true,
      status: 200,
      json: async () => ({ checklistId: 1, scheduleId: 1, patientId: 100, phase: "SIGN_IN" }),
    });

    const { result } = renderHook(() => useCreateChecklist(), { wrapper: wrapper() });

    result.current.mutate({ scheduleId: 1, patientId: 100, phase: "SIGN_IN" });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));

    expect(fetchMock).toHaveBeenCalledWith(
      expect.stringContaining("/ot/checklists"),
      expect.objectContaining({ method: "POST" })
    );
    expect(result.current.data?.checklistId).toBe(1);
  });
});

describe("useCompleteChecklistItem", () => {
  it("completes a checklist item", async () => {
    fetchMock.mockResolvedValueOnce({
      ok: true,
      status: 200,
      json: async () => ({ itemId: 1, completed: true }),
    });

    const { result } = renderHook(() => useCompleteChecklistItem(), { wrapper: wrapper() });

    result.current.mutate(1);

    await waitFor(() => expect(result.current.isSuccess).toBe(true));

    expect(fetchMock).toHaveBeenCalledWith(
      expect.stringContaining("/ot/checklists/items/1/complete"),
      expect.objectContaining({ method: "POST" })
    );
  });
});

describe("useOtRooms", () => {
  it("fetches OT rooms", async () => {
    fetchMock.mockResolvedValueOnce({
      ok: true,
      status: 200,
      json: async () => [
        { roomId: 1, roomName: "OT-1", roomType: "General", isActive: true },
      ],
    });

    const { result } = renderHook(() => useOtRooms(), { wrapper: wrapper() });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));

    expect(result.current.data).toHaveLength(1);
    expect(result.current.data?.[0].roomName).toBe("OT-1");
  });
});

describe("useStaffSchedule", () => {
  it("fetches staff schedule", async () => {
    fetchMock.mockResolvedValueOnce({
      ok: true,
      status: 200,
      json: async () => [
        { id: 1, scheduleId: 1, role: "Surgeon", staffId: 5 },
      ],
    });

    const { result } = renderHook(() => useStaffSchedule(1), { wrapper: wrapper() });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));

    expect(fetchMock).toHaveBeenCalledWith(
      expect.stringContaining("/ot/staff-schedule?scheduleId=1"),
      expect.any(Object)
    );
    expect(result.current.data).toHaveLength(1);
  });

  it("is disabled when scheduleId is 0", () => {
    const { result } = renderHook(() => useStaffSchedule(0), { wrapper: wrapper() });

    expect(fetchMock).not.toHaveBeenCalled();
    expect(result.current.fetchStatus).toBe("idle");
  });
});

describe("useNurseSchedule", () => {
  it("fetches nurse schedule", async () => {
    fetchMock.mockResolvedValueOnce({
      ok: true,
      status: 200,
      json: async () => [
        { id: 1, staffId: 10, dutyDate: "2026-09-11", shiftType: "Morning" },
      ],
    });

    const { result } = renderHook(() => useNurseSchedule(10), { wrapper: wrapper() });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));

    expect(fetchMock).toHaveBeenCalledWith(
      expect.stringContaining("/ot/nurse-schedule?staffId=10"),
      expect.any(Object)
    );
    expect(result.current.data).toHaveLength(1);
  });
});

describe("useBillingEstimates", () => {
  it("fetches billing estimates", async () => {
    fetchMock.mockResolvedValueOnce({
      ok: true,
      status: 200,
      json: async () => [
        { estimateId: 1, scheduleId: 1, procedureCode: "APPEND", totalAmount: 5000 },
      ],
    });

    const { result } = renderHook(() => useBillingEstimates(1), { wrapper: wrapper() });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));

    expect(result.current.data).toHaveLength(1);
    expect(result.current.data?.[0].totalAmount).toBe(5000);
  });
});
