import { z } from "zod";

export const doctorShareFormSchema = z.object({
  doctorId: z.coerce.number().int().positive("Doctor ID must be a positive number"),
  service: z.string().min(1, "Service is required").max(200, "Max 200 chars"),
  sharePercent: z
    .coerce.number()
    .int()
    .min(1, "Share percent must be at least 1")
    .max(100, "Share percent must be at most 100"),
  baseAmount: z
    .coerce.number()
    .int()
    .min(0, "Base amount must be at least 0"),
  period: z.string().min(1, "Period is required"),
});

export type DoctorShareFormValues = z.infer<typeof doctorShareFormSchema>;

export const doctorShareEditSchema = z.object({
  id: z.string().min(1, "ID is required"),
  doctorId: z.coerce.number().int().positive(),
  service: z.string().min(1, "Service is required").max(200, "Max 200 chars"),
  sharePercent: z
    .coerce.number()
    .int()
    .min(1, "Share percent must be at least 1")
    .max(100, "Share percent must be at most 100"),
  baseAmount: z
    .coerce.number()
    .int()
    .min(0, "Base amount must be at least 0"),
  period: z.string().min(1, "Period is required"),
});

export type DoctorShareEditValues = z.infer<typeof doctorShareEditSchema>;

export const payoutRecordSchema = z.object({
  id: z.string().uuid(),
  doctorId: z.coerce.number().int().positive(),
  doctorName: z.string().min(1),
  service: z.string().min(1),
  baseAmount: z.coerce.number().int().positive(),
  sharePercent: z.coerce.number().int().min(1).max(100),
  shareAmount: z.coerce.number().int().positive(),
  period: z.string().min(1),
  status: z.enum(["PENDING", "PROCESSING", "COMPLETED", "FAILED", "CANCELLED"]),
  createdAt: z.string(),
  updatedAt: z.string().optional(),
});

export type PayoutRecord = z.infer<typeof payoutRecordSchema>;

export const payoutFilterSchema = z.object({
  fromDate: z.string().optional(),
  toDate: z.string().optional(),
  status: z.enum(["", "PENDING", "PROCESSING", "COMPLETED", "FAILED", "CANCELLED"]).optional(),
  page: z.coerce.number().int().nonnegative().default(0),
  size: z.coerce.number().int().positive().default(20),
});

export type PayoutFilterValues = z.infer<typeof payoutFilterSchema>;