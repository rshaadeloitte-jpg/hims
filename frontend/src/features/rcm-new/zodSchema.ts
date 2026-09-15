import { z } from "zod";

export const claimSchema = z.object({
  patientName: z.string().min(1),
  scheme: z.string().min(1, "Scheme required"),
  amount: z.coerce.number().positive("Amount must be positive"),
});

export type ClaimForm = z.infer<typeof claimSchema>;

export const dmApprovalSchema = z.object({
  claimId: z.number().positive(),
  approvalStage: z.enum(["pending", "dlh_approval", "dm_approval", "approved", "rejected"]),
  action: z.enum(["approve", "reject"]),
});

export type DMApprovalForm = z.infer<typeof dmApprovalSchema>;

export const dischargeVerificationSchema = z.object({
  patientId: z.number().positive(),
  clearanceStatus: z.enum(["pending", "bill_generated", "bill_verified", "cleared", "blocked"]),
});

export type DischargeVerificationForm = z.infer<typeof dischargeVerificationSchema>;

export const chiranjeeviSchemeSchema = z.object({
  schemeId: z.number().positive(),
  approvalStatus: z.enum(["pending", "under_review", "approved", "rejected"]),
  action: z.enum(["approve", "reject"]),
});

export type ChiranjeeviSchemeForm = z.infer<typeof chiranjeeviSchemeSchema>;
