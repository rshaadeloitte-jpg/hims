import { z } from "zod";

export const kioskRegistrationSchema = z.object({
  mobileNo: z.string().regex(/^\d{10}$/, "Invalid 10-digit mobile number"),
  firstName: z.string().min(1, "First name required"),
  lastName: z.string().min(1, "Last name required"),
  gender: z.enum(["M", "F", "O"]).optional().or(z.literal("")),
  dob: z
    .string()
    .refine((val) => val === "" || /^\d{4}-\d{2}-\d{2}$/.test(val), "Date must be yyyy-MM-dd")
    .optional(),
});

export type KioskRegistrationForm = z.infer<typeof kioskRegistrationSchema>;

export const departmentSchema = z.object({
  id: z.number().int().positive(),
  name: z.string().min(1),
});

export type Department = z.infer<typeof departmentSchema>;

export const doctorSchema = z.object({
  id: z.number().int().positive(),
  name: z.string().min(1),
  departmentId: z.number().int().positive(),
  consultationFee: z.number().nonnegative().optional(),
});

export type Doctor = z.infer<typeof doctorSchema>;

export const appointmentSlotSchema = z.object({
  slotId: z.number().int().positive(),
  startTime: z.string().min(1),
  endTime: z.string().min(1),
  available: z.boolean(),
});

export type AppointmentSlot = z.infer<typeof appointmentSlotSchema>;

export const appointmentBookingSchema = z.object({
  patientName: z.string().min(1, "Patient name required"),
  mobileNo: z.string().regex(/^\d{10}$/, "Invalid 10-digit mobile"),
  departmentId: z.number().int().positive("Department required"),
  doctorId: z.number().int().positive("Doctor required"),
  date: z.string().regex(/^\d{4}-\d{2}-\d{2}$/, "Date required"),
  slotId: z.number().int().positive("Select a slot"),
  reason: z.string().optional(),
});

export type AppointmentBookingForm = z.infer<typeof appointmentBookingSchema>;

export const labResultUploadSchema = z.object({
  tokenNo: z.string().min(1, "Token number required"),
  visitId: z.string().min(1, "Visit ID required"),
  testName: z.string().min(1, "Test name required"),
  resultValue: z.string().min(1, "Result value required"),
  resultDate: z.string().regex(/^\d{4}-\d{2}-\d{2}$/, "Date must be yyyy-MM-dd"),
  file: z.instanceof(File).optional(),
});

export type LabResultUploadForm = z.infer<typeof labResultUploadSchema>;
