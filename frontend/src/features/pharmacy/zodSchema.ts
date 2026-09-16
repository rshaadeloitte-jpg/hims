import { z } from "zod";

/** M07 Pharmacy schemas — mirror the pharmacy-service request DTOs. */

const optionalNumber = z.preprocess((v) => (v === "" || v == null ? undefined : v), z.coerce.number().optional());

/** Create item sale (POST /api/v1/item-sales). */
export const itemSaleSchema = z.object({
  patientId: z.coerce.number().int().positive("Patient is required"),
  prescriptionId: optionalNumber,
  lines: z.array(
    z.object({
      itemId: z.coerce.number().int().positive("Item is required"),
      quantity: z.coerce.number().positive("Quantity must be > 0"),
      rate: z.coerce.number().nonnegative().optional(),
    }),
  ),
});

export type ItemSaleValues = z.infer<typeof itemSaleSchema>;

/** Dispense form validation matching pharmacy-service requirements */
export const dispenseSchema = z.object({
  prescriptionId: z.coerce.number().int().positive(),
  items: z.array(
    z.object({
      itemId: z.coerce.number().int().positive(),
      dispensedQuantity: z.coerce.number().int().min(1),
    })
  ),
});

export type DispenseValues = z.infer<typeof dispenseSchema>;

/** Drug allergy form validation */
export const drugAllergySchema = z.object({
  drugId: z.coerce.number().int().positive().optional(),
  allergen: z.string().min(1, "Allergen required"),
  allergyType: z.string().optional(),
  severity: z.enum(["MILD", "MODERATE", "SEVERE"]).default("MODERATE"),
  reaction: z.string().optional(),
  notes: z.string().optional(),
});

export type DrugAllergyValues = z.infer<typeof drugAllergySchema>;

/** Drug interaction form validation */
export const drugInteractionSchema = z.object({
  drug1Id: z.coerce.number().int().positive(),
  drug2Id: z.coerce.number().int().positive(),
  interactionType: z.string().optional(),
  severity: z.enum(["MILD", "MODERATE", "SEVERE"]).default("MODERATE"),
  notes: z.string().optional(),
});

export type DrugInteractionValues = z.infer<typeof drugInteractionSchema>;
