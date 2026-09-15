import { z } from "zod";

/** M27 Queue schemas — mirror queue-service request DTOs. */

const optionalNumber = z.preprocess((v) => (v === "" || v == null ? undefined : v), z.coerce.number().optional());

/** Create counter (POST /api/v1/queue/counters). */
export const counterSchema = z.object({
  counterName: z.string().min(1, "Name is required"),
  departmentId: optionalNumber,
  cabinId: optionalNumber,
});

export type CounterValues = z.infer<typeof counterSchema>;

/** Issue token (POST /api/v1/queue/tokens). */
export const tokenSchema = z.object({
  counterId: z.coerce.number().int().positive("Counter is required"),
  patientId: z.coerce.number().int().positive("Patient is required"),
  priority: z.enum(["NORMAL", "EMERGENCY"]).default("NORMAL"),
});

export type TokenValues = z.infer<typeof tokenSchema>;

/** Transfer token to another counter. */
export const transferSchema = z.object({
  tokenId: z.coerce.number().int().positive(),
  targetCounterId: z.coerce.number().int().positive("Target counter is required"),
});

export type TransferValues = z.infer<typeof transferSchema>;

/** Department queue filter. */
export const departmentFilterSchema = z.object({
  departmentId: z.coerce.number().int().positive("Department is required"),
  status: z.enum(["ALL", "WAITING", "CALLED", "COMPLETED", "NO_SHOW"]).default("ALL"),
  priority: z.enum(["ALL", "NORMAL", "EMERGENCY"]).default("ALL"),
});

export type DepartmentFilterValues = z.infer<typeof departmentFilterSchema>;

/** Doctor batch filter. */
export const doctorBatchFilterSchema = z.object({
  doctorId: z.coerce.number().int().positive("Doctor is required"),
  date: z.string().optional(),
});

export type DoctorBatchFilterValues = z.infer<typeof doctorBatchFilterSchema>;

/** Call patient action. */
export const callPatientSchema = z.object({
  tokenId: z.coerce.number().int().positive("Token is required"),
  counterId: z.coerce.number().int().positive("Counter is required"),
});

export type CallPatientValues = z.infer<typeof callPatientSchema>;

/** Token status enum for display mapping. */
export const TOKEN_STATUS = {
  1: "WAITING",
  2: "CALLED",
  3: "COMPLETED",
  4: "NO_SHOW",
} as const;

export type TokenStatus = (typeof TOKEN_STATUS)[keyof typeof TOKEN_STATUS];