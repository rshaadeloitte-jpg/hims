import { describe, expect, it, vi, beforeEach, afterEach } from "vitest";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { renderHook, waitFor } from "@testing-library/react";
import {
  useWorklistPatients,
  useTasksByAdmission,
  useTasksByPatient,
  useCreateTask,
  useCarePlansByAdmission,
  useCarePlan,
  useCreateCareGoal,
  useCreateIntervention,
  worklistQueryKeys,
} from "./queries";

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

describe("useWorklistPatients", () => {
  it("fetches worklist patients", async () => {
    fetchMock.mockResolvedValueOnce({
      ok: true,
      status: 200,
      json: async () => [
        {
          id: 1,
          name: "John Doe",
          mrn: "MRN001",
          admissionId: 100,
          wardName: "ICU",
          bedLabel: "A1",
          goalsCount: 2,
          medsCount: 5,
          labsCount: 3,
          investigationsCount: 1,
          openTasks: 4,
          totalTasks: 8,
        },
      ],
    });

    const { result } = renderHook(() => useWorklistPatients(), { wrapper: wrapper() });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));

    expect(result.current.data).toHaveLength(1);
    expect(result.current.data?.[0].mrn).toBe("MRN001");
    expect(result.current.data?.[0].openTasks).toBe(4);
  });

  it("passes filter as query params", async () => {
    fetchMock.mockResolvedValueOnce({
      ok: true,
      status: 200,
      json: async () => [],
    });

    const { result } = renderHook(
      () => useWorklistPatients({ status: "PENDING", priority: "HIGH", dateFrom: "2026-01-01" }),
      { wrapper: wrapper() },
    );

    await waitFor(() => expect(result.current.isSuccess).toBe(true));

    expect(fetchMock).toHaveBeenCalledWith(
      "/api/api/v1/nursing/worklist?status=PENDING&priority=HIGH&dateFrom=2026-01-01",
      expect.any(Object),
    );
  });

  it("skips ALL and empty filter values", async () => {
    fetchMock.mockResolvedValueOnce({
      ok: true,
      status: 200,
      json: async () => [],
    });

    const { result } = renderHook(
      () => useWorklistPatients({ status: "ALL", priority: "ALL", assignedTo: "" }),
      { wrapper: wrapper() },
    );

    await waitFor(() => expect(result.current.isSuccess).toBe(true));

    expect(fetchMock).toHaveBeenCalledWith(
      "/api/api/v1/nursing/worklist",
      expect.any(Object),
    );
  });
});

describe("useTasksByAdmission", () => {
  it("fetches tasks for admission", async () => {
    fetchMock.mockResolvedValueOnce({
      ok: true,
      status: 200,
      json: async () => [
        {
          taskId: 1,
          admissionId: 100,
          patientId: 50,
          patientName: "Jane Doe",
          taskType: "MEDICATION",
          description: "Administer insulin",
          priority: "HIGH",
          status: "PENDING",
          dueTime: "2026-09-11T14:00:00",
          createdAt: "2026-09-11T08:00:00",
        },
      ],
    });

    const { result } = renderHook(() => useTasksByAdmission(100), { wrapper: wrapper() });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));

    expect(result.current.data).toHaveLength(1);
    expect(result.current.data?.[0].taskType).toBe("MEDICATION");
    expect(result.current.data?.[0].priority).toBe("HIGH");
  });

  it("is disabled when admissionId is 0", () => {
    const { result } = renderHook(() => useTasksByAdmission(0), { wrapper: wrapper() });

    expect(fetchMock).not.toHaveBeenCalled();
    expect(result.current.fetchStatus).toBe("idle");
  });
});

describe("useTasksByPatient", () => {
  it("fetches tasks for patient", async () => {
    fetchMock.mockResolvedValueOnce({
      ok: true,
      status: 200,
      json: async () => [
        {
          taskId: 2,
          admissionId: 101,
          patientId: 75,
          taskType: "ASSESSMENT",
          description: "Pain assessment",
          priority: "MEDIUM",
          status: "IN_PROGRESS",
          createdAt: "2026-09-11T09:00:00",
        },
      ],
    });

    const { result } = renderHook(() => useTasksByPatient(75), { wrapper: wrapper() });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));

    expect(result.current.data).toHaveLength(1);
    expect(result.current.data?.[0].patientId).toBe(75);
  });

  it("is disabled when patientId is 0", () => {
    const { result } = renderHook(() => useTasksByPatient(0), { wrapper: wrapper() });

    expect(fetchMock).not.toHaveBeenCalled();
    expect(result.current.fetchStatus).toBe("idle");
  });
});

describe("useCreateTask", () => {
  it("creates task and invalidates queries", async () => {
    fetchMock.mockResolvedValueOnce({
      ok: true,
      status: 201,
      json: async () => ({
        taskId: 99,
        admissionId: 100,
        patientId: 50,
        taskType: "MEDICATION",
        description: "New task",
        priority: "MEDIUM",
        status: "PENDING",
        createdAt: "2026-09-11T10:00:00",
      }),
    });

    const { result } = renderHook(() => useCreateTask(), { wrapper: wrapper() });

    result.current.mutate({
      admissionId: 100,
      patientId: 50,
      taskType: "MEDICATION",
      description: "New task",
      priority: "MEDIUM",
    });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));

    expect(fetchMock).toHaveBeenCalledWith(
      "/api/api/v1/tasks",
      expect.objectContaining({
        method: "POST",
        body: JSON.stringify({
          admissionId: 100,
          patientId: 50,
          taskType: "MEDICATION",
          description: "New task",
          priority: "MEDIUM",
        }),
      }),
    );
  });
});

describe("useCarePlansByAdmission", () => {
  it("fetches care plans for admission", async () => {
    fetchMock.mockResolvedValueOnce({
      ok: true,
      status: 200,
      json: async () => [
        {
          carePlanId: 1,
          admissionId: 100,
          patientId: 50,
          title: "Post-op care",
          description: "Monitor for complications",
          goals: [
            {
              goalId: 1,
              carePlanId: 1,
              goalText: "Pain under control",
              status: "ACTIVE",
              createdAt: "2026-09-11T08:00:00",
            },
          ],
          interventions: [
            {
              interventionId: 1,
              carePlanId: 1,
              interventionText: "Monitor vitals q4h",
              status: "SCHEDULED",
              createdAt: "2026-09-11T08:00:00",
            },
          ],
          resolved: false,
          createdAt: "2026-09-11T08:00:00",
        },
      ],
    });

    const { result } = renderHook(() => useCarePlansByAdmission(100), { wrapper: wrapper() });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));

    expect(result.current.data).toHaveLength(1);
    expect(result.current.data?.[0].title).toBe("Post-op care");
    expect(result.current.data?.[0].goals).toHaveLength(1);
    expect(result.current.data?.[0].interventions).toHaveLength(1);
  });
});

describe("useCarePlan", () => {
  it("fetches single care plan", async () => {
    fetchMock.mockResolvedValueOnce({
      ok: true,
      status: 200,
      json: async () => ({
        carePlanId: 5,
        admissionId: 100,
        patientId: 50,
        title: "Diabetes management",
        goals: [],
        interventions: [],
        resolved: false,
        createdAt: "2026-09-11T08:00:00",
      }),
    });

    const { result } = renderHook(() => useCarePlan(5), { wrapper: wrapper() });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));

    expect(result.current.data?.carePlanId).toBe(5);
    expect(result.current.data?.title).toBe("Diabetes management");
  });

  it("is disabled when id is 0", () => {
    const { result } = renderHook(() => useCarePlan(0), { wrapper: wrapper() });

    expect(fetchMock).not.toHaveBeenCalled();
    expect(result.current.fetchStatus).toBe("idle");
  });
});

describe("useCreateCareGoal", () => {
  it("creates goal via care plan endpoint", async () => {
    fetchMock.mockResolvedValueOnce({
      ok: true,
      status: 201,
      json: async () => ({
        goalId: 10,
        carePlanId: 1,
        goalText: "Walk 50m unassisted",
        status: "ACTIVE",
        createdAt: "2026-09-11T12:00:00",
      }),
    });

    const { result } = renderHook(() => useCreateCareGoal(), { wrapper: wrapper() });

    result.current.mutate({
      carePlanId: 1,
      goalText: "Walk 50m unassisted",
      status: "ACTIVE",
    });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));

    expect(fetchMock).toHaveBeenCalledWith(
      "/api/api/v1/care-plans/1/goals",
      expect.objectContaining({
        method: "POST",
        body: JSON.stringify({
          carePlanId: 1,
          goalText: "Walk 50m unassisted",
          status: "ACTIVE",
        }),
      }),
    );
  });
});

describe("useCreateIntervention", () => {
  it("creates intervention via care plan endpoint", async () => {
    fetchMock.mockResolvedValueOnce({
      ok: true,
      status: 201,
      json: async () => ({
        interventionId: 20,
        carePlanId: 1,
        interventionText: "Wound dressing change",
        status: "SCHEDULED",
        createdAt: "2026-09-11T12:00:00",
      }),
    });

    const { result } = renderHook(() => useCreateIntervention(), { wrapper: wrapper() });

    result.current.mutate({
      carePlanId: 1,
      interventionText: "Wound dressing change",
      status: "SCHEDULED",
    });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));

    expect(fetchMock).toHaveBeenCalledWith(
      "/api/api/v1/care-plans/1/interventions",
      expect.objectContaining({
        method: "POST",
        body: JSON.stringify({
          carePlanId: 1,
          interventionText: "Wound dressing change",
          status: "SCHEDULED",
        }),
      }),
    );
  });
});

describe("worklistQueryKeys", () => {
  it("generates patients key with filter", () => {
    const key = worklistQueryKeys.patients({ status: "PENDING" });
    expect(key).toEqual(["worklist", "patients", { status: "PENDING" }]);
  });

  it("generates tasks by admission key", () => {
    const key = worklistQueryKeys.tasksByAdmission(100);
    expect(key).toEqual(["worklist", "tasks", 100]);
  });

  it("generates care plan key", () => {
    const key = worklistQueryKeys.carePlan(5);
    expect(key).toEqual(["worklist", "care-plan", 5]);
  });
});
