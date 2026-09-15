/**
 * M08 Clinical Orders feature module.
 *
 * Components:
 * - OrderSetBuilder: Drag-and-drop order set composition
 * - ClinicalWorkflow: CWF/SCWF multi-step approval workflow management
 * - OrderDetail: Full order view with workflow, investigations, external orders
 *
 * Schemas: zodSchema.ts (OrderSet, CWF, SCWF, ExternalOrder, etc.)
 * Queries: queries.ts (TanStack Query hooks for ordering-service)
 */

export { OrderSetBuilder } from "./OrderSetBuilder";
export { ClinicalWorkflow } from "./ClinicalWorkflow";
export { OrderDetail } from "./OrderDetail";
export { OrdersPage } from "./OrdersPage";

export * from "./queries";
export * from "./zodSchema";
