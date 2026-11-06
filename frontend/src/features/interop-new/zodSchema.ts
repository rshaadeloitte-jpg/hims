import { z } from "zod";
export const syncStatusSchema = z.object({
  drugCode: z.string().min(1),
  drugName: z.string().min(1),
  syncStatus: z.number(),
});
export type SyncStatusForm = z.infer<typeof syncStatusSchema>;
