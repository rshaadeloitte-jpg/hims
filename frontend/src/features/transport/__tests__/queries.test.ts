import { describe, expect, it, vi, beforeEach, afterEach } from "vitest";
import { renderHook, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { createElement } from "react";
import {
  useVehicles,
  useVehicle,
  useVehicleIn,
  useVehicleOut,
  useAllocateVehicle,
  transportQueryKeys,
} from "../queries";

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

describe("Transport queries", () => {
  // ── useVehicles ──────────────────────────────────────────────────────────────

  describe("useVehicles", () => {
    it("fetches GET /api/v1/transport/vehicles", async () => {
      fetchMock.mockResolvedValueOnce({
        ok: true,
        status: 200,
        json: async () => [
          {
            id: 1,
            vehicleNumber: "AMB-001",
            vehicleType: "AMBULANCE",
            driverName: "Dr. Patel",
            status: "IN",
            currentLocation: "Main Gate",
            allocatedTo: undefined,
            lastInAt: "2024-01-15T10:00:00Z",
            lastOutAt: undefined,
          },
        ],
      });

      const { result } = renderHook(() => useVehicles(), { wrapper: wrapper() });
      await waitFor(() => expect(result.current.isSuccess).toBe(true));

      expect(fetchMock).toHaveBeenCalledWith(
        "/api/api/v1/transport/vehicles",
        expect.any(Object)
      );
      expect(result.current.data).toHaveLength(1);
      expect(result.current.data?.[0].vehicleNumber).toBe("AMB-001");
    });
  });

  // ── useVehicle ──────────────────────────────────────────────────────────────

  describe("useVehicle", () => {
    it("fetches single vehicle by ID", async () => {
      fetchMock.mockResolvedValueOnce({
        ok: true,
        status: 200,
        json: async () => ({
          id: 1,
          vehicleNumber: "VAN-001",
          vehicleType: "VAN",
          driverName: "Mr. Sharma",
          status: "OUT",
          currentLocation: "Hospital",
          allocatedTo: "Emergency Dept",
        }),
      });

      const { result } = renderHook(() => useVehicle(1), { wrapper: wrapper() });
      await waitFor(() => expect(result.current.isSuccess).toBe(true));

      expect(fetchMock).toHaveBeenCalledWith(
        "/api/api/v1/transport/vehicles/1",
        expect.any(Object)
      );
    });

    it("is disabled when id is 0", () => {
      const { result } = renderHook(() => useVehicle(0), { wrapper: wrapper() });
      expect(result.current.fetchStatus).toBe("idle");
      expect(fetchMock).not.toHaveBeenCalled();
    });
  });

  // ── useVehicleIn ───────────────────────────────────────────────────────────

  describe("useVehicleIn", () => {
    it("POSTs to /api/v1/transport/vehicles/:id/in", async () => {
      fetchMock.mockResolvedValueOnce({
        ok: true,
        status: 200,
        json: async () => ({
          id: 1,
          vehicleNumber: "AMB-001",
          status: "IN",
          currentLocation: "Service Gate",
        }),
      });

      const { result } = renderHook(() => useVehicleIn(), { wrapper: wrapper() });
      result.current.mutate({ vehicleId: 1, location: "Service Gate", odometerReading: 50000, notes: "Test" });

      await waitFor(() => expect(result.current.isSuccess).toBe(true));
      expect(fetchMock).toHaveBeenCalledWith(
        "/api/api/v1/transport/vehicles/1/in",
        expect.objectContaining({
          method: "POST",
          body: JSON.stringify({ vehicleId: 1, location: "Service Gate", odometerReading: 50000, notes: "Test" }),
        })
      );
    });
  });

  // ── useVehicleOut ──────────────────────────────────────────────────────────

  describe("useVehicleOut", () => {
    it("POSTs to /api/v1/transport/vehicles/:id/out", async () => {
      fetchMock.mockResolvedValueOnce({
        ok: true,
        status: 200,
        json: async () => ({
          id: 1,
          vehicleNumber: "AMB-001",
          status: "OUT",
        }),
      });

      const { result } = renderHook(() => useVehicleOut(), { wrapper: wrapper() });
      result.current.mutate({ vehicleId: 1, destination: "Airport", odometerReading: 50100, purpose: "Emergency" });

      await waitFor(() => expect(result.current.isSuccess).toBe(true));
      expect(fetchMock).toHaveBeenCalledWith(
        "/api/api/v1/transport/vehicles/1/out",
        expect.objectContaining({
          method: "POST",
          body: JSON.stringify({ vehicleId: 1, destination: "Airport", odometerReading: 50100, purpose: "Emergency" }),
        })
      );
    });
  });

  // ── useAllocateVehicle ─────────────────────────────────────────────────────

  describe("useAllocateVehicle", () => {
    it("POSTs to /api/v1/transport/vehicles/:id/allocate", async () => {
      fetchMock.mockResolvedValueOnce({
        ok: true,
        status: 200,
        json: async () => ({
          id: 1,
          vehicleNumber: "AMB-001",
          status: "ALLOCATED",
          allocatedTo: "Dr. Smith",
        }),
      });

      const { result } = renderHook(() => useAllocateVehicle(), { wrapper: wrapper() });
      result.current.mutate({ vehicleId: 1, allocatedTo: "Dr. Smith", purpose: "Duty", expectedReturnAt: "2024-01-15T18:00:00Z" });

      await waitFor(() => expect(result.current.isSuccess).toBe(true));
      expect(fetchMock).toHaveBeenCalledWith(
        "/api/api/v1/transport/vehicles/1/allocate",
        expect.objectContaining({
          method: "POST",
        })
      );
    });
  });

  // ── Query key structure ────────────────────────────────────────────────────

  describe("transportQueryKeys", () => {
    it("generates correct query keys", () => {
      expect(transportQueryKeys.vehicles()).toEqual(["transport", "vehicles"]);
      expect(transportQueryKeys.vehicle(42)).toEqual(["transport", "vehicles", 42]);
      expect(transportQueryKeys.fuelLogs()).toEqual(["transport", "fuel-logs"]);
      expect(transportQueryKeys.fuelLog(1)).toEqual(["transport", "fuel-logs", 1]);
      expect(transportQueryKeys.maintenanceRequests()).toEqual(["transport", "maintenance"]);
      expect(transportQueryKeys.maintenanceRequest(5)).toEqual(["transport", "maintenance", 5]);
    });
  });
});