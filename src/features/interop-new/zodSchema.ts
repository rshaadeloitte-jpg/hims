import { z } from "zod";
export const syncStatusSchema = z.object({ systemName: z.string().min(1) });
export type SyncStatusForm = z.infer<typeof syncStatusSchema>;
