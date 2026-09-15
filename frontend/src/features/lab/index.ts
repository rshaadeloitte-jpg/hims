/**
 * M15 Lab Feature Module
 * 
 * Components:
 * - TestBooking: Linear-style data table for test selection
 * - BarcodeGenerator: Sample ID generation
 * - SampleCollection: Accept/reject workflow with barcode scanning
 * - ResultEntry: Input fields with reference range validation
 * - VerificationQueue: Double-verification workflow
 * - CriticalAlerts: Flagging abnormal results
 * - WorkloadDashboard: Machine-wise queue visualization
 * - ReportPreview: Print-ready report layout
 * 
 * Hooks:
 * - useLabTests: Fetch available lab tests
 * - useSampleWorkflow: Manage sample collection workflow
 * - useResultValidation: Validate results against reference ranges
 * 
 * Legacy:
 * - LabOrdersPage: Original monolithic page (deprecated)
 */

// Components
export { TestBooking } from './TestBooking';
export { BarcodeGenerator } from './BarcodeGenerator';
export { SampleCollection } from './SampleCollection';
export { ResultEntry } from './ResultEntry';
export { VerificationQueue } from './VerificationQueue';
export { CriticalAlerts } from './CriticalAlerts';
export { WorkloadDashboard } from './WorkloadDashboard';
export { ReportPreview } from './ReportPreview';

// Legacy (deprecated)
export { LabOrdersPage } from './LabOrdersPage';

// Hooks
export { useLabTests, useLabTest } from './useLabTests';
export { useSampleWorkflow, useBarcodeScanner } from './useSampleWorkflow';
export { useResultValidation, useBatchResultValidation } from './useResultValidation';

// Queries (re-export from queries.ts)
export {
  useLabOrders,
  useLabOrder,
  useCreateLabOrder,
  useLabOrderAction,
  useEnterResults,
  useFinalizeResult,
  useBarcode,
  useAcceptReject,
  useVerifyResult,
  useNotifyCritical,
  useLabWorkload,
  useCovidOrders,
  useMachines,
  useCreateMachine,
  labQueryKeys,
} from './queries';

// Types
export type {
  LabTest,
  LabOrder,
  LabOrderStatus,
  LabDetail,
  LabDetailStatus,
  Sample,
  ResultEntry,
  VerificationStatus,
  Machine,
  WorkloadItem,
  CriticalAlert,
  LabReport,
} from './types';

// Schemas (re-export from zodSchema.ts)
export {
  labOrderSchema,
  labResultSchema,
  labRejectSchema,
} from './zodSchema';

export type {
  LabOrderValues,
  LabResultValues,
  LabRejectValues,
} from './zodSchema';