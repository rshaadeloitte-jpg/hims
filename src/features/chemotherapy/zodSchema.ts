import { z } from "zod";
export const regimenSchema = z.object({ patientId: z.coerce.number().int().positive(), regimenName: z.string().min(1), cycles: z.coerce.number().int().positive() });
export type RegimenForm = z.infer<typeof regimenSchema>;
