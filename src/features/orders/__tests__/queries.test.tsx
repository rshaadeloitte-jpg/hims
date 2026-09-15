import { describe, expect, it, vi, beforeEach, afterEach } from "vitest";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { renderHook, waitFor } from "@testing-library/react";
import {
  useOrderSets,
  useCreateOrderSet,
  useApplyOrderSet,
  useCwfDefinitions,
  useScwfDefinitions,
  useWorkflowSteps,
  useAdvanceWorkflowStep,
  useExternalOrders,
  useCreateExternalOrder,
  useInvestigations,
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

describe("useOrderSets", () => {
  it("fetches order sets from API", async () => {
    fetchMock.mockResolvedValueOnce({
      ok: true,
      status: 200,
      json: async () => [
        {
          id: 1,
          name: "Diabetes Panel",
          code: "DM-PANEL",
          category: "LAB",
          items: [{ id: "i1", serviceType: "LAB", serviceId: 100, serviceName: "FBS", qty: 1, priority: "ROUTINE" }],
          status: "ACTIVE",
          createdAt: "2024-01-01T00:00:00Z",
          updatedAt: "2024-01-01T00:00:00Z",
        },
      ],
    });

    const { result } = renderHook(() => useOrderSets(), { wrapper: wrapper() });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));

    expect(fetchMock).toHaveBeenCalledWith(
      "/api/v1/order-sets",
      expect.any(Object),
    );
    expect(result.current.data).toHaveLength(1);
    expect(result.current.data?.[0].code).toBe("DM-PANEL");
  });
});

describe("useCreateOrderSet", () => {
  it("POSTs new order set", async () => {
    fetchMock.mockResolvedValueOnce({
      ok: true,
      status: 201,
      json: async () => ({ id: 99, name: "Test", code: "TEST", category: "LAB", items: [], status: "ACTIVE" }),
    });

    const { result } = renderHook(() => useCreateOrderSet(), { wrapper: wrapper() });

    await result.current.mutateAsync({ name: "Test", code: "TEST", category: "LAB", items: [] });

    expect(fetchMock).toHaveBeenCalledWith(
      "/api/v1/order-sets",
      expect.objectContaining({ method: "POST", body: expect.any(String) }),
    );
  });
});

describe("useApplyOrderSet", () => {
  it("POSTs apply order set payload", async () => {
    fetchMock.mockResolvedValueOnce({
      ok: true,
      status: 200,
      json: async () => [{ id: 10, patientId: 100, orderType: "LAB", description: "FBS", status: "PENDING", priority: "ROUTINE", orderedBy: "System", orderedAt: "2024-01-01T00:00:00Z" }],
    });

    const { result } = renderHook(() => useApplyOrderSet(), { wrapper: wrapper() });

    await result.current.mutateAsync({ orderSetId: 1, patientId: 100 });

    expect(fetchMock).toHaveBeenCalledWith(
      "/api/v1/order-sets/apply",
      expect.objectContaining({ method: "POST" }),
    );
  });
});

describe("useCwfDefinitions", () => {
  it("fetches CWF definitions", async () => {
    fetchMock.mockResolvedValueOnce({
      ok: true,
      status: 200,
      json: async () => [
        {
          cwfId: 1,
          cwfName: "Lab Approval",
          serviceType: "LAB",
          steps: [{ stepId: 1, stepName: "Review", stepOrder: 0, role: "DOCTOR", isMandatory: true }],
          status: "ACTIVE",
        },
      ],
    });

    const { result } = renderHook(() => useCwfDefinitions(), { wrapper: wrapper() });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));

    expect(fetchMock).toHaveBeenCalledWith("/api/v1/cwf", expect.any(Object));
    expect(result.current.data?.[0].cwfName).toBe("Lab Approval");
  });
});

describe("useScwfDefinitions", () => {
  it("fetches SCWF definitions", async () => {
    fetchMock.mockResolvedValueOnce({
      ok: true,
      status: 200,
      json: async () => [
        {
          scwfId: 1,
          scwfName: "DM Referral",
          serviceType: "CONSULT",
          condition: "hba1c > 9",
          steps: [{ stepId: 1, stepName: "Endo Review", stepOrder: 0, role: "DOCTOR" }],
          status: "ACTIVE",
        },
      ],
    });

    const { result } = renderHook(() => useScwfDefinitions(), { wrapper: wrapper() });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));

    expect(fetchMock).toHaveBeenCalledWith("/api/v1/scwf", expect.any(Object));
    expect(result.current.data?.[0].condition).toBe("hba1c > 9");
  });
});

describe("useWorkflowSteps", () => {
  it("fetches workflow steps for an order", async () => {
    fetchMock.mockResolvedValueOnce({
      ok: true,
      status: 200,
      json: async () => [
        { stepId: 1, stepName: "Review", stepOrder: 0, role: "DOCTOR", status: "PENDING" },
        { stepId: 2, stepName: "Verify", stepOrder: 1, role: "LAB_TECH", status: "PENDING" },
      ],
    });

    const { result } = renderHook(() => useWorkflowSteps(42), { wrapper: wrapper() });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));

    expect(fetchMock).toHaveBeenCalledWith(
      "/api/v1/orders/42/workflow",
      expect.any(Object),
    );
    expect(result.current.data).toHaveLength(2);
  });

  it("does not fetch when orderId is 0", () => {
    const { result } = renderHook(() => useWorkflowSteps(0), { wrapper: wrapper() });
    expect(result.current.isFetching).toBe(false);
  });
});

describe("useAdvanceWorkflowStep", () => {
  it("POSTs step completion", async () => {
    fetchMock.mockResolvedValueOnce({
      ok: true,
      status: 200,
      json: async () => [{ stepId: 1, status: "COMPLETED" }],
    });

    const { result } = renderHook(() => useAdvanceWorkflowStep(), { wrapper: wrapper() });

    await result.current.mutateAsync({ orderId: 42, stepId: 1, action: "COMPLETE" });

    expect(fetchMock).toHaveBeenCalledWith(
      "/api/v1/orders/42/workflow/1/complete",
      expect.objectContaining({ method: "POST" }),
    );
  });
});

describe("useExternalOrders", () => {
  it("fetches external orders for an order", async () => {
    fetchMock.mockResolvedValueOnce({
      ok: true,
      status: 200,
      json: async () => [
        {
          id: 1,
          orderId: 42,
          externalLabName: "SRL",
          testName: "CBC",
          status: "PENDING",
          createdAt: "2024-01-01T00:00:00Z",
        },
      ],
    });

    const { result } = renderHook(() => useExternalOrders(42), { wrapper: wrapper() });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));

    expect(fetchMock).toHaveBeenCalledWith(
      "/api/v1/orders/42/external",
      expect.any(Object),
    );
    expect(result.current.data?.[0].externalLabName).toBe("SRL");
  });
});

describe("useInvestigations", () => {
  it("fetches investigations for an order", async () => {
    fetchMock.mockResolvedValueOnce({
      ok: true,
      status: 200,
      json: async () => [
        {
          id: 1,
          orderId: 42,
          serviceId: 100,
          serviceName: "FBS",
          status: "COMPLETED",
          priority: "ROUTINE",
          orderedBy: "Dr. Smith",
          orderedAt: "2024-01-01T00:00:00Z",
          resultValue: "95",
          unit: "mg/dL",
          referenceRange: "70-100",
        },
      ],
    });

    const { result } = renderHook(() => useInvestigations(42), { wrapper: wrapper() });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));

    expect(fetchMock).toHaveBeenCalledWith(
      "/api/v1/orders/42/investigations",
      expect.any(Object),
    );
    expect(result.current.data?.[0].resultValue).toBe("95");
  });
});
