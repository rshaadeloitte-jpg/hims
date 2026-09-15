import { z } from "zod";

/* ── Purchase Order ─────────────────────────────────────────────── */

export const purchaseOrderLineSchema = z.object({
  itemId: z.coerce.number().int().positive("Item is required"),
  orderedQty: z.coerce.number().positive("Quantity must be > 0"),
  unitPrice: z.coerce.number().min(0, "Unit price must be >= 0"),
  taxPercent: z.coerce.number().min(0).max(100).default(0),
  leadTimeDays: z.coerce.number().int().min(0).optional(),
  remarks: z.string().max(500).optional(),
});

export const purchaseOrderSchema = z.object({
  storeId: z.coerce.number().int().positive("Store is required"),
  supplierId: z.coerce.number().int().positive("Supplier is required"),
  quotationId: z.coerce.number().int().positive().optional(),
  orderDate: z.string().optional(),
  expectedDate: z.string().optional(),
  remarks: z.string().max(500).optional(),
  lines: z.array(purchaseOrderLineSchema).min(1, "At least one line is required"),
});

export type PurchaseOrderForm = z.infer<typeof purchaseOrderSchema>;
export type PurchaseOrderLineForm = z.infer<typeof purchaseOrderLineSchema>;

/* ── GRN (Goods Receipt Note) ──────────────────────────────────── */

export const grnLineSchema = z.object({
  itemId: z.coerce.number().int().positive("Item is required"),
  quantity: z.coerce.number().positive("Quantity must be > 0"),
  batchId: z.coerce.number().int().positive().optional(),
  batchCode: z.string().max(64).optional(),
  expiryDate: z.string().optional(),
  mrp: z.coerce.number().min(0).optional(),
  purchaseRate: z.coerce.number().min(0).optional(),
  remarks: z.string().max(500).optional(),
}).refine((d) => d.batchId || d.batchCode?.trim(), {
  message: "Either batchId or batchCode is required",
  path: ["batchCode"],
});

export const grnSchema = z.object({
  storeId: z.coerce.number().int().positive("Store is required"),
  supplierId: z.coerce.number().int().positive().optional(),
  grnNo: z.string().max(64).optional(),
  invoiceNo: z.string().max(64).optional(),
  invoiceDate: z.string().optional(),
  receivedDate: z.string().optional(),
  remarks: z.string().max(500).optional(),
  provisional: z.boolean().default(false),
  consignment: z.boolean().default(false),
  lines: z.array(grnLineSchema).min(1, "At least one line is required"),
});

export type GrnForm = z.infer<typeof grnSchema>;
export type GrnLineForm = z.infer<typeof grnLineSchema>;

/* ── Quotation ──────────────────────────────────────────────────── */

export const quotationLineSchema = z.object({
  itemId: z.coerce.number().int().positive("Item is required"),
  quantity: z.coerce.number().positive("Quantity must be > 0"),
  unitPrice: z.coerce.number().min(0, "Unit price must be >= 0"),
  taxPercent: z.coerce.number().min(0).max(100).default(0),
  leadTimeDays: z.coerce.number().int().min(0).optional(),
  remarks: z.string().max(500).optional(),
});

export const quotationSchema = z.object({
  supplierId: z.coerce.number().int().positive("Supplier is required"),
  enquiryRef: z.string().max(64).optional(),
  validUntil: z.string().optional(),
  remarks: z.string().max(500).optional(),
  lines: z.array(quotationLineSchema).min(1, "At least one line is required"),
});

export type QuotationForm = z.infer<typeof quotationSchema>;
export type QuotationLineForm = z.infer<typeof quotationLineSchema>;
