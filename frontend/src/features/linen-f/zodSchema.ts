import { z } from "zod";
export const linenRequestSchema = z.object({ fromStore: z.string().min(1), toStore: z.string().min(1), items: z.array(z.object({ itemId: z.number(), quantity: z.number() })) });
export type LinenRequestForm = z.infer<typeof linenRequestSchema>;
