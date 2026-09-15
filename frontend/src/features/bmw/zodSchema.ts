import { z } from "zod";

/**
 * M18 Biomedical Waste (BMW) — Zod schemas.
 *
 * Ported from the legacy FAUG engine (BioMedicalWasteManagementServiceImpl.java,
 * `BioMedicalWasteRequestStatus` enum) and the FAUG schema/config JSON. The
 * request lifecycle is a strict forward-only state machine:
 *
 *   PENDING(1) → ACCEPTED(2) → HANDOVERED(3) → RECEIVED(4) → COMPLETED(5)
 *
 * Each bag carries its own status which mirrors the request status but is
 * advanced per-bag via `updateBagStatus`. On completion the bag is "decomposed"
 * by a disposal firm (firmName / contactPerson / contactNo).
 */

/** Request status — mirrors BioMedicalWasteRequestStatus (legacy 1..5). */
export const wasteRequestStatusSchema = z.enum([
  "PENDING",
  "ACCEPTED",
  "HANDOVERED",
  "RECEIVED",
  "COMPLETED",
]);

export type WasteRequestStatus = z.infer<typeof wasteRequestStatusSchema>;

/** Numeric status ids used on the wire by the legacy backend (1..5). */
export const STATUS_ID: Record<WasteRequestStatus, number> = {
  PENDING: 1,
  ACCEPTED: 2,
  HANDOVERED: 3,
  RECEIVED: 4,
  COMPLETED: 5,
};

/**
 * Stripe-style status badge mapping. Reuses the design-token badge classes
 * from global.css (badge--success / badge--info / badge--warning / badge--muted
 * / badge--danger) so the chip renders consistently with the rest of the app
 * while keeping the "Stripe status pill" visual language.
 */
export const STATUS_TONE: Record<WasteRequestStatus, string> = {
  PENDING: "badge--warning",
  ACCEPTED: "badge--info",
  HANDOVERED: "badge--info",
  RECEIVED: "badge--success",
  COMPLETED: "badge--success",
};

export const STATUS_LABEL: Record<WasteRequestStatus, string> = {
  PENDING: "Pending",
  ACCEPTED: "Accepted",
  HANDOVERED: "Handed Over",
  RECEIVED: "Received",
  COMPLETED: "Completed",
};

/** Forward-only transition map — next allowed statuses from a given status. */
export const ALLOWED_NEXT_STATUS: Record<WasteRequestStatus, WasteRequestStatus[]> =
  {
    PENDING: ["ACCEPTED", "HANDOVERED"],
    ACCEPTED: ["HANDOVERED"],
    HANDOVERED: ["RECEIVED"],
    RECEIVED: ["COMPLETED"],
    COMPLETED: [],
  };

/** Waste bag master entries (the `Waste_Bag_Master` global in the legacy app). */
export const wasteBagMasterSchema = z.object({
  id: z.coerce.number(),
  name: z.string().min(1),
  description: z.string().optional(),
});

/** A single bag line within a waste request. */
export const wasteBagLineSchema = z.object({
  /** Master waste-bag id — selects from Waste_Bag_Master. */
  bagMasterId: z.coerce.number({ invalid_type_error: "Select a waste bag type" }).positive(),
  /** Quantity of bags of this type. Defaults to 1 per the legacy form. */
  qty: z.coerce.number().int().positive("Quantity must be ≥ 1").default(1),
  /** Weight in kg — validated to 2 decimals in the legacy form (FEI0085). */
  weight: z.coerce.number().positive("Weight must be > 0").max(9999, "Weight too large"),
  /** Optional remark per bag. */
  remark: z.string().max(200).optional(),
});

/**
 * WasteRequest form (Stripe-style: progressive, validates bag lines + barcode
 * presence). The request itself only carries unit/store/date; the bag details
 * come in as a list.
 */
export const wasteRequestSchema = z.object({
  unitId: z.coerce.number().positive(),
  storeId: z.coerce.number().positive("From store is required"),
  toStoreId: z.coerce.number().optional(),
  date: z.string().min(1, "Date is required"),
  details: z.array(wasteBagLineSchema).min(1, "Add at least one waste bag"),
});

export type WasteRequestValues = z.infer<typeof wasteRequestSchema>;

/** Bag-level detail returned by getBioRequestDetails. */
export const wasteBagDetailSchema = z.object({
  id: z.coerce.number(),
  barcodeNo: z.string(),
  sequenceNumber: z.string().optional(),
  barCodeDetails: z.string().optional(),
  weight: z.coerce.number().optional(),
  remark: z.string().optional(),
  qty: z.coerce.number().optional(),
  bagId: z.coerce.number().optional(),
  bagName: z.string().optional(),
  addedDateTime: z.string().optional(),
  store: z.string().optional(),
  bagStatus: z.coerce.number().optional(),
  firmName: z.string().optional(),
  contactPerson: z.string().optional(),
  contactNo: z.string().optional(),
  handedOverTo: z.string().optional(),
  bioMedicalWasteId: z.coerce.number().optional(),
  unitId: z.coerce.number(),
});

export type WasteBagDetail = z.infer<typeof wasteBagDetailSchema>;

/** Summary counts by request status (getBioMedWasteCount). */
export const wasteSummarySchema = z.object({
  totalCount: z.number(),
  pendingCount: z.number(),
  acceptedCount: z.number(),
  handOveredCount: z.number(),
  receivedCount: z.number(),
  completedCount: z.number(),
});

export type WasteSummary = z.infer<typeof wasteSummarySchema>;

/** Paginated list filter. */
export const wasteListFilterSchema = z.object({
  fromDate: z.string().optional(),
  toDate: z.string().optional(),
  category: z.enum(["", "pending", "accepted", "handovered", "received", "completed"]).optional(),
  storeId: z.coerce.number().optional(),
  fromStoreId: z.coerce.number().optional(),
  search: z.string().optional(),
  page: z.coerce.number().int().nonnegative().default(0),
  size: z.coerce.number().int().positive().default(25),
});

export type WasteListFilterValues = z.infer<typeof wasteListFilterSchema>;

/** Handover-to-firm form (handOverBags → handedOverTo). */
export const handoverFormSchema = z.object({
  /** Person the physical bags are handed to (free-text in the legacy form). */
  handedOverTo: z.string().min(1, "Handed over to is required").max(100),
});

export type HandoverFormValues = z.infer<typeof handoverFormSchema>;

/**
 * Decompose / dispatch-to-disposal-firm form (processBioRequest). Mirrors the
 * decomposeBioWaste.html template: firmName (pre-filled from the last supplier
 * firm), contactPerson, contactNo (10-digit mobile).
 */
export const decomposeFormSchema = z.object({
  firmName: z.string().min(1, "Firm name is required").max(100),
  contactPerson: z
    .string()
    .min(1, "Contact person is required")
    .max(60)
    .regex(/^[a-zA-Z\s]+$/, "Contact person must be letters only"),
  contactNo: z
    .string()
    .length(10, "Mobile must be 10 digits")
    .regex(/^\d{10}$/, "Mobile must be 10 digits"),
});

export type DecomposeFormValues = z.infer<typeof decomposeFormSchema>;

/** Firm info returned by handedOverToFirm. */
export const firmInfoSchema = z.object({
  description: z.string().optional(),
  contactPerson1name: z.string().optional(),
  contactPerson1mobileno: z.string().optional(),
});

export type FirmInfo = z.infer<typeof firmInfoSchema>;

/** Barcode validation — validateBarcode returns a boolean on the wire. */
export const barcodeValidateResponseSchema = z.boolean();

/** Bulk bag-status update payload (updateBagStatus). */
export const bagStatusUpdateSchema = z.object({
  bioMedicalWasteId: z.coerce.number().positive(),
  id: z.coerce.number().positive(),
  unitId: z.coerce.number().positive(),
  bagStatus: z.coerce.number().int().min(1).max(5),
  weight: z.coerce.number().optional(),
  remark: z.string().optional(),
  handedOverTo: z.string().optional(),
});

export type BagStatusUpdate = z.infer<typeof bagStatusUpdateSchema>;

/** Store master entry (for the from/to store dropdowns). */
export const storeSchema = z.object({
  id: z.coerce.number(),
  description: z.string(),
  isBioMedicalStore: z.boolean().optional(),
});

export type Store = z.infer<typeof storeSchema>;
