import { describe, expect, it, vi, beforeEach, afterEach } from "vitest";
import { renderHook, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { createElement } from "react";
import {
  usePrescriptions,
  usePrescription,
  useItemSales,
  useCreateItemSale,
  useSaleReturns,
  useCompleteSaleReturn,
  useRejectSaleReturn,
  useInterventions,
  useDrugAllergies,
  useCreateDrugAllergy,
  useDrugInteractions,
  useCreateDrugInteraction,
} from "./queries";

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

describe("Pharmacy queries", () => {
  it("usePrescriptions GET /api/v1/prescriptions", async () => {
    fetchMock.mockResolvedValueOnce({ ok: true, status: 200, json: async () => [{ prescriptionId: 1, patientId: 2, prescriberId: 3, statusCode: 1, details: [] }] });
    const { result } = renderHook(() => usePrescriptions(), { wrapper: wrapper() });
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(fetchMock).toHaveBeenCalledWith("/api/v1/prescriptions", expect.any(Object));
  });

  it("usePrescription GET /api/v1/prescriptions/:id", async () => {
    fetchMock.mockResolvedValueOnce({ ok: true, status: 200, json: async () => ({ prescriptionId: 5, patientId: 2, prescriberId: 3, statusCode: 1, details: [] }) });
    const { result } = renderHook(() => usePrescription(5), { wrapper: wrapper() });
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(fetchMock).toHaveBeenCalledWith("/api/v1/prescriptions/5", expect.any(Object));
  });

  it("usePrescription disabled for 0", () => {
    const { result } = renderHook(() => usePrescription(0), { wrapper: wrapper() });
    expect(result.current.fetchStatus).toBe("idle");
    expect(fetchMock).not.toHaveBeenCalled();
  });

  it("useItemSales GET /api/v1/item-sales", async () => {
    fetchMock.mockResolvedValueOnce({ ok: true, status: 200, json: async () => [{ saleId: 1, patientId: 2, statusCode: 1, dispensed: false }] });
    const { result } = renderHook(() => useItemSales(), { wrapper: wrapper() });
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(fetchMock).toHaveBeenCalledWith("/api/v1/item-sales", expect.any(Object));
  });

  it("useCreateItemSale POST /api/v1/item-sales", async () => {
    fetchMock.mockResolvedValueOnce({ ok: true, status: 201, json: async () => ({ saleId: 1 }) });
    const { result } = renderHook(() => useCreateItemSale(), { wrapper: wrapper() });
    result.current.mutate({ patientId: 2, lines: [{ itemId: 3, qty: 1 }] });
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(fetchMock).toHaveBeenCalledWith("/api/v1/item-sales", expect.objectContaining({ method: "POST" }));
  });

  it("useSaleReturns GET /api/v1/sale-returns", async () => {
    fetchMock.mockResolvedValueOnce({ ok: true, status: 200, json: async () => [{ returnId: 1, originalSaleId: 2, patientId: 3, returnReason: "DAMAGED", statusCode: 1 }] });
    const { result } = renderHook(() => useSaleReturns(), { wrapper: wrapper() });
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(fetchMock).toHaveBeenCalledWith("/api/v1/sale-returns", expect.any(Object));
  });

  it("useCompleteSaleReturn POST /api/v1/sale-returns/:id/complete", async () => {
    fetchMock.mockResolvedValueOnce({ ok: true, status: 200, json: async () => ({ returnId: 4, statusCode: 3 }) });
    const { result } = renderHook(() => useCompleteSaleReturn(), { wrapper: wrapper() });
    result.current.mutate(4);
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(fetchMock).toHaveBeenCalledWith("/api/v1/sale-returns/4/complete", expect.objectContaining({ method: "POST" }));
  });

  it("useRejectSaleReturn POST /api/v1/sale-returns/:id/reject", async () => {
    fetchMock.mockResolvedValueOnce({ ok: true, status: 200, json: async () => ({ returnId: 4, statusCode: 4 }) });
    const { result } = renderHook(() => useRejectSaleReturn(), { wrapper: wrapper() });
    result.current.mutate(4);
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(fetchMock).toHaveBeenCalledWith("/api/v1/sale-returns/4/reject", expect.objectContaining({ method: "POST" }));
  });

  it("useInterventions requires patientId", async () => {
    const { result } = renderHook(() => useInterventions(), { wrapper: wrapper() });
    expect(result.current.fetchStatus).toBe("idle");
    expect(fetchMock).not.toHaveBeenCalled();
  });

  it("useInterventions GET /api/v1/interventions?patientId=", async () => {
    fetchMock.mockResolvedValueOnce({ ok: true, status: 200, json: async () => [{ interventionId: 1, patientId: 9, interventionType: "X", pharmacistId: 1 }] });
    const { result } = renderHook(() => useInterventions(9), { wrapper: wrapper() });
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(fetchMock).toHaveBeenCalledWith("/api/v1/interventions?patientId=9", expect.any(Object));
  });

  it("useDrugAllergies requires patientId", () => {
    const { result } = renderHook(() => useDrugAllergies(), { wrapper: wrapper() });
    expect(result.current.fetchStatus).toBe("idle");
    expect(fetchMock).not.toHaveBeenCalled();
  });

  it("useDrugAllergies GET /api/v1/drug-allergies/patient/:id", async () => {
    fetchMock.mockResolvedValueOnce({ ok: true, status: 200, json: async () => [{ allergyId: 1, patientId: 9, allergen: "Penicillin", severity: "MILD" }] });
    const { result } = renderHook(() => useDrugAllergies(9), { wrapper: wrapper() });
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(fetchMock).toHaveBeenCalledWith("/api/v1/drug-allergies/patient/9", expect.any(Object));
  });

  it("useCreateDrugAllergy POST /api/v1/drug-allergies", async () => {
    fetchMock.mockResolvedValueOnce({ ok: true, status: 201, json: async () => ({ allergyId: 1 }) });
    const { result } = renderHook(() => useCreateDrugAllergy(), { wrapper: wrapper() });
    result.current.mutate({ patientId: 9, allergen: "Penicillin", severity: "MILD" });
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(fetchMock).toHaveBeenCalledWith("/api/v1/drug-allergies", expect.objectContaining({ method: "POST" }));
  });

  it("useDrugInteractions requires patientId", () => {
    const { result } = renderHook(() => useDrugInteractions(), { wrapper: wrapper() });
    expect(result.current.fetchStatus).toBe("idle");
    expect(fetchMock).not.toHaveBeenCalled();
  });

  it("useDrugInteractions GET /api/v1/drug-interactions/patient/:id", async () => {
    fetchMock.mockResolvedValueOnce({ ok: true, status: 200, json: async () => [{ interactionId: 1, patientId: 9, drug1Id: 1, drug2Id: 2, interactionType: "DRUG_DRUG", severity: "MINOR", acknowledged: false }] });
    const { result } = renderHook(() => useDrugInteractions(9), { wrapper: wrapper() });
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(fetchMock).toHaveBeenCalledWith("/api/v1/drug-interactions/patient/9", expect.any(Object));
  });

  it("useCreateDrugInteraction POST /api/v1/drug-interactions", async () => {
    fetchMock.mockResolvedValueOnce({ ok: true, status: 201, json: async () => ({ interactionId: 1 }) });
    const { result } = renderHook(() => useCreateDrugInteraction(), { wrapper: wrapper() });
    result.current.mutate({ patientId: 9, drug1Id: 1, drug2Id: 2, interactionType: "DRUG_DRUG", severity: "MINOR" });
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(fetchMock).toHaveBeenCalledWith("/api/v1/drug-interactions", expect.objectContaining({ method: "POST" }));
  });
});
