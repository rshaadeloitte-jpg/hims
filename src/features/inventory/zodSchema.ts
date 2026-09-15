import { z } from "zod";

export const inventoryItemSchema = z.object({
  name: z.string().min(1, "Name is required"),
  code: z.string().min(1, "Code is required"),
  unitId: z.number().int().positive("Unit is required"),
  status: z.string().default("ACTIVE"),
});

export type InventoryItemValues = z.infer<typeof inventoryItemSchema>;

export const inventoryStoreSchema = z.object({
  name: z.string().min(1, "Name is required"),
  code: z.string().min(1, "Code is required"),
  location: z.string().optional(),
});

export type InventoryStoreValues = z.infer<typeof inventoryStoreSchema>;

export const stockMovementSchema = z.object({
  itemId: z.number().int().positive("Item is required"),
  quantity: z.number().min(1, "Quantity must be at least 1"),
  type: z.string().min(1, "Type is required"),
  location: z.string().min(1, "Location is required"),
});

export type StockMovementValues = z.infer<typeof stockMovementSchema>;

// ---- M13 Indent request schema -------------------------------------------

const optionalNumber = z.preprocess(
  (v) => (v === "" || v == null ? undefined : v),
  z.coerce.number().int().positive().optional(),
);

const optionalString = z.preprocess(
  (v) => (v === "" ? undefined : v),
  z.string().optional(),
);

export const indentLineSchema = z.object({
  itemId: z.number().int().positive("Item is required"),
  requestedQty: z.number().int().positive("Quantity must be >= 1"),
  remarks: optionalString,
});

export const indentRequestSchema = z.object({
  storeId: z.number().int().positive("Store is required"),
  requestDate: z.string().regex(/^\d{4}-\d{2}-\d{2}$/, "yyyy-MM-dd").default(() => new Date().toISOString().slice(0, 10)),
  priority: z.enum(["ROUTINE", "URGENT", "EMERGENCY"]).default("ROUTINE"),
  remarks: optionalString,
  lines: z.array(indentLineSchema).min(1, "At least one line is required"),
});

export type IndentRequestValues = z.infer<typeof indentRequestSchema>;
export type IndentLineValues = z.infer<typeof indentLineSchema>;

// ---- M13 Stock adjustment schema ----------------------------------------

export const stockAdjustmentSchema = z.object({
  itemId: z.number().int().positive("Item is required"),
  batchId: optionalNumber,
  adjustmentType: z.enum(["INCREASE", "DECREASE"], {
    required_error: "Adjustment type is required",
  }),
  quantity: z.number().int().positive("Quantity must be >= 1"),
  reason: z.string().min(1, "Reason is required").max(500, "Max 500 chars"),
  reference: optionalString,
});

export type StockAdjustmentValues = z.infer<typeof stockAdjustmentSchema>;

// ---- M13 Item enquiry filter schema -------------------------------------

export const itemEnquiryFilterSchema = z.object({
  keyword: optionalString,
  storeId: optionalNumber,
  category: optionalString,
  lowStockOnly: z.boolean().default(false),
  status: z.enum(["", "ACTIVE", "INACTIVE", "DISCONTINUED"]).optional(),
});

export type ItemEnquiryFilterValues = z.infer<typeof itemEnquiryFilterSchema>;
