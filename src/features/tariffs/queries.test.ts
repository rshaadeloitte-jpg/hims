import { describe, expect, it, vi, beforeEach, afterEach } from "vitest";
import { renderHook, waitFor } from "@testing-library/react";
import {
  useTariffs,
  useTariff,
  useCreateTariff,
  useUpdateTariff,
  useServiceCategories,
  usePackages,
  useConcessionTemplates,
  type TariffValues,
} from "./queries";

const fetchMock = vi.fn();

function wrapper() {
  const { QueryClient, QueryClientProvider } = require("@tanstack/react-query");
  const React = require("react");
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return ({ children }: { children: any }) =>
    React.createElement(QueryClientProvider, { client: queryClient }, children);
}

beforeEach(() => {
  fetchMock.mockReset();
  vi.stubGlobal("fetch", fetchMock);
});

afterEach(() => {
  vi.unstubAllGlobals();
});

// ---- useServiceCategories --------------------------------------------------

describe("useServiceCategories", () => {
  it("fetches service categories", async () => {
    const mockCategories = ["CONSULTATION", "DIAGNOSTIC", "PROCEDURE"];
    fetchMock.mockResolvedValueOnce({
      ok: true,
      status: 200,
      json: async () => mockCategories,
    });

    const { result } = renderHook(() => useServiceCategories(), { wrapper });
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data).toEqual(mockCategories);
  });

  it("provides placeholder data initially", async () => {
    fetchMock.mockImplementationOnce(() => new Promise(() => {}));
    const { result } = renderHook(() => useServiceCategories(), { wrapper });
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data).toContain("CONSULTATION");
    expect(result.current.data).toContain("PACKAGE");
  });
});

// ---- useTariffs -------------------------------------------------------------

describe("useTariffs", () => {
  it("fetches tariffs with filter", async () => {
    const mockTariffs = [
      { id: 1, serviceName: "Consultation", baseRate: 500, copayPercent: 10 },
      { id: 2, serviceName: "Lab Test", baseRate: 300, copayPercent: 0 },
    ];
    fetchMock.mockResolvedValueOnce({
      ok: true,
      status: 200,
      json: async () => mockTariffs,
    });

    const filter = { serviceCategory: "CONSULTATION", page: 0, size: 20 };
    const { result } = renderHook(() => useTariffs(filter), { wrapper });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data).toEqual(mockTariffs);
  });
});

// ---- useTariff --------------------------------------------------------------

describe("useTariff", () => {
  it("fetches single tariff", async () => {
    const mockTariff = {
      id: 1,
      serviceId: 101,
      serviceName: "Consultation",
      serviceCategory: "CONSULTATION",
      baseRate: 500,
      copayPercent: 10,
    };
    fetchMock.mockResolvedValueOnce({
      ok: true,
      status: 200,
      json: async () => mockTariff,
    });

    const { result } = renderHook(() => useTariff(1), { wrapper });
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data?.serviceName).toBe("Consultation");
  });

  it("is disabled when id is 0", () => {
    const { result } = renderHook(() => useTariff(0), { wrapper });
    expect(result.current.fetchStatus).toBe("idle");
    expect(fetchMock).not.toHaveBeenCalled();
  });
});

// ---- useCreateTariff --------------------------------------------------------

describe("useCreateTariff", () => {
  it("POSTs to create tariff endpoint", async () => {
    const mockTariff: TariffValues = {
      serviceId: 1,
      serviceName: "New Service",
      serviceCategory: "CONSULTATION",
      baseRate: 500,
      copayPercent: 10,
      status: "ACTIVE",
    };

    fetchMock.mockResolvedValueOnce({
      ok: true,
      status: 201,
      json: async () => ({ id: 1, ...mockTariff }),
    });

    const { result } = renderHook(() => useCreateTariff(), { wrapper });
    const promise = result.current.mutate(mockTariff);
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    await promise;

    expect(fetchMock).toHaveBeenCalledWith(
      expect.stringContaining("/api/v1/masters/tariffs"),
      expect.objectContaining({ method: "POST" }),
    );
  });
});

// ---- useUpdateTariff --------------------------------------------------------

describe("useUpdateTariff", () => {
  it("PUTs to update tariff endpoint", async () => {
    const mockTariff: TariffValues = {
      serviceId: 1,
      serviceName: "Updated Service",
      serviceCategory: "CONSULTATION",
      baseRate: 750,
      copayPercent: 5,
      status: "ACTIVE",
    };

    fetchMock.mockResolvedValueOnce({
      ok: true,
      status: 200,
      json: async () => ({ id: 1, ...mockTariff }),
    });

    const { result } = renderHook(() => useUpdateTariff(), { wrapper });
    result.current.mutate({ id: 1, payload: mockTariff });
    await waitFor(() => expect(result.current.isSuccess).toBe(true));

    expect(fetchMock).toHaveBeenCalledWith(
      expect.stringContaining("/api/v1/masters/tariffs/1"),
      expect.objectContaining({ method: "PUT" }),
    );
  });
});

// ---- usePackages ------------------------------------------------------------

describe("usePackages", () => {
  it("fetches available packages", async () => {
    const mockPackages = [
      {
        id: 1,
        packageName: "Health Checkup",
        packageCode: "HC001",
        totalValue: 5000,
        sellPrice: 4500,
        services: [],
        plans: [],
      },
    ];
    fetchMock.mockResolvedValueOnce({
      ok: true,
      status: 200,
      json: async () => mockPackages,
    });

    const { result } = renderHook(() => usePackages(), { wrapper });
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data?.[0]?.packageName).toBe("Health Checkup");
  });
});

// ---- useConcessionTemplates -------------------------------------------------

describe("useConcessionTemplates", () => {
  it("fetches concession templates", async () => {
    const mockTemplates = [
      {
        id: 1,
        code: "STAFF",
        name: "Staff Discount",
        scheme: "Self",
        maxPercentOff: 20,
        requiresApproval: true,
        docStatus: 1,
      },
    ];
    fetchMock.mockResolvedValueOnce({
      ok: true,
      status: 200,
      json: async () => mockTemplates,
    });

    const { result } = renderHook(() => useConcessionTemplates(), { wrapper });
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data?.[0]?.code).toBe("STAFF");
  });

  it("accepts filter parameter", async () => {
    const mockTemplates: any[] = [];
    fetchMock.mockResolvedValueOnce({
      ok: true,
      status: 200,
      json: async () => mockTemplates,
    });

    const { result } = renderHook(
      () => useConcessionTemplates({ scheme: "RGHS" }),
      { wrapper },
    );
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
  });
});