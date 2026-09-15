import { describe, expect, it, vi, beforeEach, afterEach } from "vitest";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { renderHook, waitFor } from "@testing-library/react";
import {
  useChambers,
  useDeceasedList,
  useAllocateChamber,
  useReleaseChamber,
  useUpdateDeathDetails,
  useDeathCertificate,
  useRequestCertificatePrint,
} from "./queries";

/**
 * M28 Mortuary hooks — wire-level coverage.
 * Pins the REST URLs consumed by the patient-service mortuary endpoints.
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

// ---- GET endpoints ---------------------------------------------------------

describe("useChambers", () => {
  it("fetches GET /api/patient/getMortuaryPatients", async () => {
    fetchMock.mockResolvedValueOnce({
      ok: true,
      status: 200,
      json: async () => [
        { id: 1, chamberNo: "M-01", bodyName: "John Doe", admittedAt: "2026-09-11T10:30:00Z", status: "OCCUPIED" },
      ],
    });
    const { result } = renderHook(() => useChambers(), { wrapper: wrapper() });
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(fetchMock).toHaveBeenCalledWith(
      "/api/patient/getMortuaryPatients",
      expect.any(Object),
    );
    expect(result.current.data?.[0]?.chamberNo).toBe("M-01");
  });
});

describe("useDeceasedList", () => {
  it("POSTs to /api/patient/getMortuaryPatients", async () => {
    fetchMock.mockResolvedValueOnce({
      ok: true,
      status: 200,
      json: async () => [],
    });
    const { result } = renderHook(() => useDeceasedList(), { wrapper: wrapper() });
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(fetchMock).toHaveBeenCalledWith(
      "/api/patient/getMortuaryPatients",
      expect.objectContaining({ method: "POST" }),
    );
  });
});

describe("useDeathCertificate", () => {
  it("is disabled when patientId is 0", () => {
    fetchMock.mockResolvedValue({ ok: true, status: 200, json: async () => ({}) });
    const { result } = renderHook(() => useDeathCertificate(0), { wrapper: wrapper() });
    expect(fetchMock).not.toHaveBeenCalled();
    expect(result.current.fetchStatus).toBe("idle");
  });

  it("fetches GET /api/patient/getPatientDetailsForDeathForm", async () => {
    fetchMock.mockResolvedValueOnce({
      ok: true,
      status: 200,
      json: async () => ({
        certNumber: "DC/2026/00042",
        patientId: 42,
        patientName: "John Doe",
        deathDate: "2026-09-11T10:30:00Z",
        causeOfDeath: "MI",
        deathType: "NATURAL",
        certifiedBy: "Dr. Smith",
        placeOfDeath: "ICU",
        hospitalName: "Test Hospital",
        printedAt: "2026-09-12T09:00:00Z",
      }),
    });
    const { result } = renderHook(() => useDeathCertificate(42), { wrapper: wrapper() });
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(fetchMock).toHaveBeenCalledWith(
      "/api/patient/getPatientDetailsForDeathForm?patientId=42",
      expect.any(Object),
    );
    expect(result.current.data?.certNumber).toBe("DC/2026/00042");
  });
});

// ---- POST endpoints --------------------------------------------------------

describe("useAllocateChamber", () => {
  it("POSTs to /api/patient/saveMortuaryAllocation", async () => {
    fetchMock.mockResolvedValueOnce({
      ok: true,
      status: 200,
      json: async () => ({ chamberId: 1, bodyName: "John Doe" }),
    });
    const { result } = renderHook(() => useAllocateChamber(), { wrapper: wrapper() });
    result.current.mutate({
      chamberId: 1,
      patientId: 42,
      bodyName: "John Doe",
      admittedAt: "2026-09-11T10:30",
    });
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(fetchMock).toHaveBeenCalledWith(
      "/api/patient/saveMortuaryAllocation",
      expect.objectContaining({
        method: "POST",
        body: JSON.stringify({
          chamberId: 1,
          patientId: 42,
          bodyName: "John Doe",
          admittedAt: "2026-09-11T10:30",
        }),
      }),
    );
  });
});

describe("useReleaseChamber", () => {
  it("POSTs to /api/patient/mortuaryClearance", async () => {
    fetchMock.mockResolvedValueOnce({
      ok: true,
      status: 200,
      json: async () => ({ chamberId: 1, releasedAt: "2026-09-12T09:00:00Z" }),
    });
    const { result } = renderHook(() => useReleaseChamber(), { wrapper: wrapper() });
    result.current.mutate({
      chamberId: 1,
      releasedTo: "Mary Doe",
      relationship: "SPOUSE",
      releasedAt: "2026-09-12T09:00",
    });
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(fetchMock).toHaveBeenCalledWith(
      "/api/patient/mortuaryClearance",
      expect.objectContaining({ method: "POST" }),
    );
  });
});

describe("useUpdateDeathDetails", () => {
  it("POSTs to /api/patient/updatePatientDeathDetails", async () => {
    fetchMock.mockResolvedValueOnce({
      ok: true,
      status: 200,
      json: async () => true,
    });
    const { result } = renderHook(() => useUpdateDeathDetails(), { wrapper: wrapper() });
    result.current.mutate({
      patientId: 42,
      isDeath: true,
      deathDate: "2026-09-11T10:30",
      causeOfDeath: "MI",
      deathType: "NATURAL",
      certifiedBy: "Dr. Smith",
      isMlc: false,
    });
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(fetchMock).toHaveBeenCalledWith(
      "/api/patient/updatePatientDeathDetails",
      expect.objectContaining({ method: "POST" }),
    );
  });
});

describe("useRequestCertificatePrint", () => {
  it("POSTs to /api/patient/generateDeathCertificate", async () => {
    fetchMock.mockResolvedValueOnce({
      ok: true,
      status: 200,
      json: async () => ({ printed: true, certNumber: "DC/2026/00042" }),
    });
    const { result } = renderHook(() => useRequestCertificatePrint(), { wrapper: wrapper() });
    result.current.mutate({
      patientId: 42,
      certNumber: "DC/2026/00042",
    });
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(fetchMock).toHaveBeenCalledWith(
      "/api/patient/generateDeathCertificate",
      expect.objectContaining({ method: "POST" }),
    );
  });
});
