import { z } from "zod";

/** M25 Reporting schemas — mirror reporting-service DTOs. */

/** Create report definition (POST /api/v1/reports). */
export const reportMasterSchema = z.object({
  reportCode: z.string().min(1, "Report code is required").max(40),
  title: z.string().min(1, "Title is required").max(120),
  kind: z.enum(["1", "2"], { message: "1=TABULAR, 2=SUMMARY" }).default("1"),
  definition: z.string().min(2, "Definition JSON is required"),
});

export type ReportMasterValues = z.infer<typeof reportMasterSchema>;

/** Execute report (POST /api/v1/reports/{code}/execute) — dateFrom/dateTo window. */
export const executeReportSchema = z.object({
  dateFrom: z.string().regex(/^\d{4}-\d{2}-\d{2}$/, "yyyy-MM-dd").optional().or(z.literal("")),
  dateTo: z.string().regex(/^\d{4}-\d{2}-\d{2}$/, "yyyy-MM-dd").optional().or(z.literal("")),
});

export type ExecuteReportValues = z.infer<typeof executeReportSchema>;

/** Report catalog search/filter. */
export const reportFilterSchema = z.object({
  search: z.string().optional(),
  activeOnly: z.boolean().default(true),
  kind: z.enum(["", "1", "2"]).optional(),
});

export type ReportFilterValues = z.infer<typeof reportFilterSchema>;

/** Definition builder helpers (mirror V1 schema: TABULAR/SUMMARY shapes). */
export function buildTabularDefinition(table: string, columns: string[], dateField?: string) {
  return JSON.stringify({ table, columns, ...(dateField ? { dateField } : {}) });
}

export function buildSummaryDefinition(table: string, aggregate: "cnt" | "total") {
  return JSON.stringify({ table, aggregate });
}
