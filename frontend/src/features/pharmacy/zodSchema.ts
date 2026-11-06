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
      qty: z.coerce.number().int().positive("Quantity must be > 0"),
      batchId: z.string().optional(),
    }),
  ),
});

export type ItemSaleValues = z.infer<typeof itemSaleSchema>;

/** Dispense lines for an item sale (POST /api/v1/item-sales/{id}/dispense). */
export const dispenseSchema = z.object({
  lines: z.array(
    z.object({
      lineNo: z.coerce.number().int().positive(),
      qty: z.coerce.number().int().min(1),
    })
  ),
});

export type DispenseValues = z.infer<typeof dispenseSchema>;

/** Drug allergy form validation */
export const drugAllergySchema = z.object({
  patientId: z.coerce.number().int().positive().optional(),
  allergen: z.string().min(1, "Allergen required"),
  allergyType: z.string().optional(),
  severity: z.enum(["MILD", "MODERATE", "SEVERE", "ANAPHYLAXIS"]).default("MODERATE"),
  reactionDescription: z.string().optional(),
});

export type DrugAllergyValues = z.infer<typeof drugAllergySchema>;

/** Drug interaction form validation */
export const drugInteractionSchema = z.object({
  patientId: z.coerce.number().int().positive().optional(),
  drug1Id: z.coerce.number().int().positive(),
  drug2Id: z.coerce.number().int().positive(),
  interactionType: z.enum(["DRUG_DRUG", "DRUG_FOOD", "DRUG_DISEASE"]).default("DRUG_DRUG"),
  severity: z.enum(["MINOR", "MODERATE", "MAJOR", "CONTRAINDICATED"]).default("MODERATE"),
  description: z.string().optional(),
});

export type DrugInteractionValues = z.infer<typeof drugInteractionSchema>;
