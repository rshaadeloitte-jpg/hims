import { z } from "zod";

// ── Vehicle request (existing) ──────────────────────────────────────────────

export const vehicleRequestSchema = z.object({
  vehicleType: z.enum(["AMBULANCE", "VAN", "BUS"]),
  fromLocation: z.string().min(1),
  toLocation: z.string().min(1),
  requester: z.string().min(1),
});

export type VehicleRequestForm = z.infer<typeof vehicleRequestSchema>;

// ── Vehicle status enums ────────────────────────────────────────────────────

export const vehicleStatusSchema = z.enum(["IN", "OUT", "MAINTENANCE", "ALLOCATED"]);
export const maintenanceStatusSchema = z.enum([
  "PENDING",
  "APPROVED",
  "IN_PROGRESS",
  "COMPLETED",
  "REJECTED",
]);

// ── Vehicle ─────────────────────────────────────────────────────────────────

export const vehicleSchema = z.object({
  id: z.number().int().positive(),
  vehicleNumber: z.string().min(1, "Vehicle number is required"),
  vehicleType: z.enum(["AMBULANCE", "VAN", "BUS", "CAR", "TRUCK"]),
  driverName: z.string().min(1, "Driver name is required"),
  status: vehicleStatusSchema,
  currentLocation: z.string().optional(),
  allocatedTo: z.string().optional(),
  lastInAt: z.string().optional(),
  lastOutAt: z.string().optional(),
});

export type Vehicle = z.infer<typeof vehicleSchema>;

// ── Vehicle in / out / allocation ───────────────────────────────────────────

export const vehicleInSchema = z.object({
  vehicleId: z.number().int().positive("Vehicle is required"),
  location: z.string().min(1, "Location is required"),
  odometerReading: z.coerce.number().min(0, "Odometer must be non-negative"),
  notes: z.string().optional(),
});

export const vehicleOutSchema = z.object({
  vehicleId: z.number().int().positive("Vehicle is required"),
  destination: z.string().min(1, "Destination is required"),
  odometerReading: z.coerce.number().min(0, "Odometer must be non-negative"),
  purpose: z.string().min(1, "Purpose is required"),
});

export const vehicleAllocationSchema = z.object({
  vehicleId: z.number().int().positive("Vehicle is required"),
  allocatedTo: z.string().min(1, "Allocation target is required"),
  purpose: z.string().min(1, "Purpose is required"),
  expectedReturnAt: z.string().optional(),
});

export type VehicleInForm = z.infer<typeof vehicleInSchema>;
export type VehicleOutForm = z.infer<typeof vehicleOutSchema>;
export type VehicleAllocationForm = z.infer<typeof vehicleAllocationSchema>;

// ── Fuel log ────────────────────────────────────────────────────────────────

export const fuelLogSchema = z.object({
  id: z.number().int().positive(),
  vehicleId: z.number().int().positive(),
  vehicleNumber: z.string().optional(),
  fuelType: z.enum(["PETROL", "DIESEL", "CNG", "ELECTRIC"]),
  quantity: z.coerce.number().positive(),
  cost: z.coerce.number().positive(),
  odometerReading: z.coerce.number().min(0),
  filledAt: z.string(),
  filledBy: z.string().min(1),
});

export const fuelLogCreateSchema = z.object({
  vehicleId: z.coerce.number().int().positive("Vehicle is required"),
  fuelType: z.enum(["PETROL", "DIESEL", "CNG", "ELECTRIC"]),
  quantity: z.coerce.number().positive("Quantity must be positive"),
  cost: z.coerce.number().positive("Cost must be positive"),
  odometerReading: z.coerce.number().min(0, "Odometer must be non-negative"),
  filledBy: z.string().min(1, "Filled by is required"),
});

export type FuelLog = z.infer<typeof fuelLogSchema>;
export type FuelLogCreateForm = z.infer<typeof fuelLogCreateSchema>;

// ── Maintenance request ─────────────────────────────────────────────────────

export const maintenanceRequestSchema = z.object({
  id: z.number().int().positive(),
  vehicleId: z.number().int().positive(),
  vehicleNumber: z.string().optional(),
  requestType: z.enum(["ROUTINE", "REPAIR", "INSPECTION", "EMERGENCY"]),
  description: z.string().min(1),
  estimatedCost: z.coerce.number().min(0).optional(),
  status: maintenanceStatusSchema,
  requestedBy: z.string().min(1),
  requestedAt: z.string(),
  approvedBy: z.string().optional(),
  approvedAt: z.string().optional(),
});

export const maintenanceRequestCreateSchema = z.object({
  vehicleId: z.number().int().positive("Vehicle is required"),
  requestType: z.enum(["ROUTINE", "REPAIR", "INSPECTION", "EMERGENCY"]),
  description: z.string().min(1, "Description is required"),
  estimatedCost: z.coerce.number().min(0).optional(),
  requestedBy: z.string().min(1, "Requested by is required"),
});

export const maintenanceApprovalSchema = z.object({
  id: z.number().int().positive("Maintenance request is required"),
  status: z.enum(["APPROVED", "REJECTED"]),
  approvedBy: z.string().min(1, "Approver name is required"),
  remarks: z.string().optional(),
});

export type MaintenanceRequest = z.infer<typeof maintenanceRequestSchema>;
export type MaintenanceRequestCreateForm = z.infer<typeof maintenanceRequestCreateSchema>;
export type MaintenanceApprovalForm = z.infer<typeof maintenanceApprovalSchema>;

// ── Status display mapping ──────────────────────────────────────────────────

export const VEHICLE_STATUS = {
  IN: "IN",
  OUT: "OUT",
  MAINTENANCE: "MAINTENANCE",
  ALLOCATED: "ALLOCATED",
} as const;

export const MAINTENANCE_STATUS = {
  PENDING: "PENDING",
  APPROVED: "APPROVED",
  IN_PROGRESS: "IN_PROGRESS",
  COMPLETED: "COMPLETED",
  REJECTED: "REJECTED",
} as const;

export type VehicleStatus = (typeof VEHICLE_STATUS)[keyof typeof VEHICLE_STATUS];
export type MaintenanceStatus = (typeof MAINTENANCE_STATUS)[keyof typeof MAINTENANCE_STATUS];
