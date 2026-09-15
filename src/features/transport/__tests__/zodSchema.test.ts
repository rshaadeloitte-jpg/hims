import { describe, expect, it } from "vitest";
import {
  vehicleRequestSchema,
  vehicleInSchema,
  vehicleOutSchema,
  vehicleAllocationSchema,
  fuelLogCreateSchema,
  maintenanceRequestCreateSchema,
  maintenanceApprovalSchema,
  VEHICLE_STATUS,
  MAINTENANCE_STATUS,
  vehicleStatusSchema,
  maintenanceStatusSchema,
} from "../zodSchema";

export type TransportTestForm = {
  vehicleType: string;
  fromLocation: string;
  toLocation: string;
  requester: string;
};

describe("zodSchema", () => {
  // ── VehicleRequest ──────────────────────────────────────────────────────────────

  describe("vehicleRequestSchema", () => {
    it("validates a valid vehicle request", () => {
      const result = vehicleRequestSchema.safeParse({
        vehicleType: "AMBULANCE",
        fromLocation: "ER",
        toLocation: "Radiology",
        requester: "Dr. Rao",
      });
      expect(result.success).toBe(true);
    });

    it("rejects invalid vehicle type", () => {
      const result = vehicleRequestSchema.safeParse({
        vehicleType: "TRUCK",
        fromLocation: "ER",
        toLocation: "Radiology",
        requester: "Dr. Rao",
      });
      expect(result.success).toBe(false);
    });

    it("rejects empty from location", () => {
      const result = vehicleRequestSchema.safeParse({
        vehicleType: "VAN",
        fromLocation: "",
        toLocation: "Ward B",
        requester: "Nurse A",
      });
      expect(result.success).toBe(false);
    });

    it("rejects empty to location", () => {
      const result = vehicleRequestSchema.safeParse({
        vehicleType: "BUS",
        fromLocation: "Main Entrance",
        toLocation: "",
        requester: "Staff B",
      });
      expect(result.success).toBe(false);
    });

    it("rejects empty requester", () => {
      const result = vehicleRequestSchema.safeParse({
        vehicleType: "AMBULANCE",
        fromLocation: "ER",
        toLocation: "CT Scan",
        requester: "",
      });
      expect(result.success).toBe(false);
    });
  });

  // ── vehicleStatusSchema ───────────────────────────────────────────────────────

  describe("vehicleStatusSchema", () => {
    it("accepts valid vehicle statuses", () => {
      expect(vehicleStatusSchema.safeParse("IN").success).toBe(true);
      expect(vehicleStatusSchema.safeParse("OUT").success).toBe(true);
      expect(vehicleStatusSchema.safeParse("MAINTENANCE").success).toBe(true);
      expect(vehicleStatusSchema.safeParse("ALLOCATED").success).toBe(true);
    });

    it("rejects invalid status", () => {
      expect(vehicleStatusSchema.safeParse("UNKNOWN").success).toBe(false);
    });
  });

  // ── maintenanceStatusSchema ─────────────────────────────────────────────────

  describe("maintenanceStatusSchema", () => {
    it("accepts valid maintenance statuses", () => {
      expect(maintenanceStatusSchema.safeParse("PENDING").success).toBe(true);
      expect(maintenanceStatusSchema.safeParse("APPROVED").success).toBe(true);
      expect(maintenanceStatusSchema.safeParse("IN_PROGRESS").success).toBe(true);
      expect(maintenanceStatusSchema.safeParse("COMPLETED").success).toBe(true);
      expect(maintenanceStatusSchema.safeParse("REJECTED").success).toBe(true);
    });

    it("rejects invalid status", () => {
      expect(maintenanceStatusSchema.safeParse("DONE").success).toBe(false);
    });
  });

  // ── vehicleInSchema ───────────────────────────────────────────────────────────

  describe("vehicleInSchema", () => {
    it("validates vehicle in form", () => {
      const result = vehicleInSchema.safeParse({
        vehicleId: 1,
        location: "Main Gate",
        odometerReading: 10000,
        notes: "Test vehicle",
      });
      expect(result.success).toBe(true);
      expect(result.data?.vehicleId).toBe(1);
    });

    it("rejects missing vehicle ID", () => {
      const result = vehicleInSchema.safeParse({
        vehicleId: 0,
        location: "Main Gate",
        odometerReading: 10000,
      });
      expect(result.success).toBe(false);
    });

    it("rejects empty location", () => {
      const result = vehicleInSchema.safeParse({
        vehicleId: 1,
        location: "",
        odometerReading: 10000,
      });
      expect(result.success).toBe(false);
    });

    it("rejects negative odometer", () => {
      const result = vehicleInSchema.safeParse({
        vehicleId: 1,
        location: "Main Gate",
        odometerReading: -100,
      });
      expect(result.success).toBe(false);
    });

    it("handles string number for odometer", () => {
      const result = vehicleInSchema.safeParse({
        vehicleId: 1,
        location: "Main Gate",
        odometerReading: "10000",
      });
      expect(result.success).toBe(true);
      expect(typeof result.data?.odometerReading).toBe("number");
    });
  });

  // ── vehicleOutSchema ──────────────────────────────────────────────────────────

  describe("vehicleOutSchema", () => {
    it("validates vehicle out form", () => {
      const result = vehicleOutSchema.safeParse({
        vehicleId: 1,
        destination: "Airport",
        odometerReading: 10100,
        purpose: "Emergency transfer",
      });
      expect(result.success).toBe(true);
    });

    it("rejects missing destination", () => {
      const result = vehicleOutSchema.safeParse({
        vehicleId: 1,
        destination: "",
        odometerReading: 10100,
        purpose: "Emergency",
      });
      expect(result.success).toBe(false);
    });

    it("rejects missing purpose", () => {
      const result = vehicleOutSchema.safeParse({
        vehicleId: 1,
        destination: "Airport",
        odometerReading: 10100,
        purpose: "",
      });
      expect(result.success).toBe(false);
    });
  });

  // ── vehicleAllocationSchema ───────────────────────────────────────────────────

  describe("vehicleAllocationSchema", () => {
    it("validates allocation form", () => {
      const result = vehicleAllocationSchema.safeParse({
        vehicleId: 1,
        allocatedTo: "Dr. Sharma",
        purpose: "Duty shift",
        expectedReturnAt: "2024-01-15T18:00:00Z",
      });
      expect(result.success).toBe(true);
    });

    it("validates allocation without expected return", () => {
      const result = vehicleAllocationSchema.safeParse({
        vehicleId: 1,
        allocatedTo: "Dr. Sharma",
        purpose: "Duty shift",
      });
      expect(result.success).toBe(true);
    });

    it("rejects empty allocatedTo", () => {
      const result = vehicleAllocationSchema.safeParse({
        vehicleId: 1,
        allocatedTo: "",
        purpose: "Duty shift",
      });
      expect(result.success).toBe(false);
    });
  });

  // ── fuelLogCreateSchema ─────────────────────────────────────────────────────

  describe("fuelLogCreateSchema", () => {
    it("validates fuel log create form", () => {
      const result = fuelLogCreateSchema.safeParse({
        vehicleId: 1,
        fuelType: "DIESEL",
        quantity: 50,
        cost: 3000,
        odometerReading: 10000,
        filledBy: "Driver A",
      });
      expect(result.success).toBe(true);
    });

    it("rejects invalid fuel type", () => {
      const result = fuelLogCreateSchema.safeParse({
        vehicleId: 1,
        fuelType: "GAS",
        quantity: 50,
        cost: 3000,
        odometerReading: 10000,
        filledBy: "Driver A",
      });
      expect(result.success).toBe(false);
    });

    it("rejects zero quantity", () => {
      const result = fuelLogCreateSchema.safeParse({
        vehicleId: 1,
        fuelType: "PETROL",
        quantity: 0,
        cost: 3000,
        odometerReading: 10000,
        filledBy: "Driver A",
      });
      expect(result.success).toBe(false);
    });

    it("handles string numbers for numeric fields", () => {
      const result = fuelLogCreateSchema.safeParse({
        vehicleId: "1",
        fuelType: "DIESEL",
        quantity: "50.5",
        cost: "3000.50",
        odometerReading: "10000",
        filledBy: "Driver A",
      });
      expect(result.success).toBe(true);
      expect(typeof result.data?.quantity).toBe("number");
    });
  });

  // ── maintenanceRequestCreateSchema ───────────────────────────────────────────

  describe("maintenanceRequestCreateSchema", () => {
    it("validates maintenance request", () => {
      const result = maintenanceRequestCreateSchema.safeParse({
        vehicleId: 1,
        requestType: "REPAIR",
        description: "Engine oil leak",
        estimatedCost: 15000,
        requestedBy: "Mechanic Singh",
      });
      expect(result.success).toBe(true);
    });

    it("accepts request without estimated cost", () => {
      const result = maintenanceRequestCreateSchema.safeParse({
        vehicleId: 1,
        requestType: "ROUTINE",
        description: "Regular servicing",
        requestedBy: "Driver",
      });
      expect(result.success).toBe(true);
    });

    it("rejects invalid requestType", () => {
      const result = maintenanceRequestCreateSchema.safeParse({
        vehicleId: 1,
        requestType: "BREAKDOWN",
        description: "Engine problem",
        requestedBy: "Driver",
      });
      expect(result.success).toBe(false);
    });

    it("rejects missing description", () => {
      const result = maintenanceRequestCreateSchema.safeParse({
        vehicleId: 1,
        requestType: "EMERGENCY",
        description: "",
        requestedBy: "Driver",
      });
      expect(result.success).toBe(false);
    });
  });

  // ── maintenanceApprovalSchema ────────────────────────────────────────────────

  describe("maintenanceApprovalSchema", () => {
    it("validates approval form", () => {
      const result = maintenanceApprovalSchema.safeParse({
        id: 42,
        status: "APPROVED",
        approvedBy: "Manager",
        remarks: "Approved for repair",
      });
      expect(result.success).toBe(true);
    });

    it("accepts approval without remarks", () => {
      const result = maintenanceApprovalSchema.safeParse({
        id: 42,
        status: "APPROVED",
        approvedBy: "Manager",
      });
      expect(result.success).toBe(true);
    });

    it("rejects invalid id", () => {
      const result = maintenanceApprovalSchema.safeParse({
        id: 0,
        status: "APPROVED",
        approvedBy: "Manager",
      });
      expect(result.success).toBe(false);
    });

    it("rejects empty approvedBy", () => {
      const result = maintenanceApprovalSchema.safeParse({
        id: 42,
        status: "REJECTED",
        approvedBy: "",
      });
      expect(result.success).toBe(false);
    });
  });

  // ── Status enums ─────────────────────────────────────────────────────────────

  describe("VEHICLE_STATUS enum", () => {
    it("contains expected values", () => {
      expect(VEHICLE_STATUS.IN).toBe("IN");
      expect(VEHICLE_STATUS.OUT).toBe("OUT");
      expect(VEHICLE_STATUS.MAINTENANCE).toBe("MAINTENANCE");
      expect(VEHICLE_STATUS.ALLOCATED).toBe("ALLOCATED");
    });
  });

  describe("MAINTENANCE_STATUS enum", () => {
    it("contains expected values", () => {
      expect(MAINTENANCE_STATUS.PENDING).toBe("PENDING");
      expect(MAINTENANCE_STATUS.APPROVED).toBe("APPROVED");
      expect(MAINTENANCE_STATUS.IN_PROGRESS).toBe("IN_PROGRESS");
      expect(MAINTENANCE_STATUS.COMPLETED).toBe("COMPLETED");
      expect(MAINTENANCE_STATUS.REJECTED).toBe("REJECTED");
    });
  });
});