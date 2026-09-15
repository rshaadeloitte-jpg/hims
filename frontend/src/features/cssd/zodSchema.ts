import { z } from "zod";

export const kitSchema = z.object({
  kitName: z.string().min(1, "Kit name required"),
  status: z.enum(["PREPARING", "STERILIZING", "READY", "EXPIRED"]).default("PREPARING"),
});

export type KitForm = z.infer<typeof kitSchema>;
