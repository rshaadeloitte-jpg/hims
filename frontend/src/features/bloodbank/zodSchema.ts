import { z } from "zod";

export const donorSchema = z.object({
  name: z.string().min(1, "Name required"),
  age: z.coerce.number().int().positive("Age required"),
  gender: z.enum(["MALE", "FEMALE", "OTHER"]),
  bloodGroup: z.string().min(1, "Blood group required"),
  phone: z.string().optional().or(z.literal("")),
  weight: z.coerce.number().positive("Weight must be positive").optional().or(z.literal(NaN)),
  pulse: z.coerce.number().int().positive("Pulse required").optional().or(z.literal(NaN)),
  bpSystolic: z.coerce.number().int().positive("BP systolic required").optional().or(z.literal(NaN)),
  bpDiastolic: z.coerce.number().int().positive("BP diastolic required").optional().or(z.literal(NaN)),
  hemoglobin: z.coerce.number().positive("Hemoglobin required").optional().or(z.literal(NaN)),
  lastDonationAt: z.string().optional().or(z.literal("")),
});

export type DonorForm = z.infer<typeof donorSchema>;

export const bloodBagSchema = z.object({
  bloodGroup: z.string().min(1, "Blood group required"),
  component: z.string().min(1, "Component required"),
  donorId: z.coerce.number().int().positive("Donor ID required"),
  volume: z.coerce.number().positive("Volume must be positive"),
  collectionDate: z.string().min(1, "Collection date required"),
  expiryDate: z.string().min(1, "Expiry date required"),
});

export type BloodBagForm = z.infer<typeof bloodBagSchema>;

export const crossMatchSchema = z.object({
  bagId: z.coerce.number().int().positive("Bag required"),
  patientId: z.coerce.number().int().positive("Patient required"),
  method: z.enum(["MAJOR", "MINOR", "BOTH"]),
  note: z.string().optional().or(z.literal("")),
});

export type CrossMatchForm = z.infer<typeof crossMatchSchema>;

export const indentSchema = z.object({
  sourceStore: z.string().min(1, "Source store required"),
  targetStore: z.string().min(1, "Target store required"),
  bloodGroup: z.string().min(1, "Blood group required"),
  component: z.string().min(1, "Component required"),
  units: z.coerce.number().int().positive("Units must be positive"),
  note: z.string().optional().or(z.literal("")),
});

export type IndentForm = z.infer<typeof indentSchema>;
