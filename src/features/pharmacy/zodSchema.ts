import { z } from "zod";

/** M07 Pharmacy schemas — mirror the pharmacy-service request DTOs. */

const optionalNumber = z.preprocess((v) => (v === "" || v == null ? undefined : v), z.coerce.number().optional());
const optionalString = z.preprocess((v) => (v === "" ? undefined : v), z.string().optional());

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
  drugId: z.coerce.number().int().positive(),
  allergyType: z.string().min(1, "Allergy type required"),
  severity: z.enum(["MILD", "MODERATE", "SEVERE"]).default("MODERATE"),
  notes: z.string().optional(),
});

export type DrugAllergyValues = z.infer<typeof drugAllergySchema>;

/** Drug interaction form validation */
export const drugInteractionSchema = z.object({
  drugIdA: z.coerce.number().int().positive(),
  drugIdB: z.coerce.number().int().positive(),
  interactionType: z.string().min(1, "Interaction type required"),
  severity: z.enum(["MINOR", "MODERATE", "MAJOR"]).default("MODERATE"),
  notes: z.string().optional(),
});

export type DrugInteractionValues = z.infer<typeof drugInteractionSchema>;
