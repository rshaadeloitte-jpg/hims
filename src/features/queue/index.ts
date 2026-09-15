// M27 Queue — public API
export { QueueBoard } from "./QueueBoard";
export { CabinDisplay } from "./CabinDisplay";
export { TokenManager } from "./TokenManager";
export { DoctorBatch } from "./DoctorBatch";
export { NurseDesk } from "./NurseDesk";
export { DepartmentQueue } from "./DepartmentQueue";
export { CallPatient } from "./CallPatient";
export { WaitTimeDisplay } from "./WaitTimeDisplay";
export { QueueDashboardPage } from "./QueueDashboardPage";

// Hooks
export { useQueueRealtime, type ConnectionStatus } from "./hooks/useQueueRealtime";
export { computeWaitTime, useWaitTimeCalc, formatWaitTime } from "./hooks/useWaitTime";

// Data hooks
export {
  useCounters,
  useCreateCounter,
  useTokens,
  useDepartmentTokens,
  useIssueToken,
  useCallNext,
  useCallPatient,
  useTokenAction,
  useTransferToken,
  useDashboard,
  useDepartmentDashboard,
  useCabinBoard,
  useDoctorBatch,
  useWaitTime,
  queueQueryKeys,
  type Counter,
  type QueueToken,
  type TokenDashboard,
  type DepartmentDashboard,
  type DoctorBatchItem,
  type WaitTimeEstimate,
  type QueueEvent,
} from "./queries";

// Schemas
export {
  counterSchema,
  tokenSchema,
  transferSchema,
  departmentFilterSchema,
  doctorBatchFilterSchema,
  callPatientSchema,
  TOKEN_STATUS,
  type CounterValues,
  type TokenValues,
  type TransferValues,
  type DepartmentFilterValues,
  type DoctorBatchFilterValues,
  type CallPatientValues,
  type TokenStatus,
} from "./zodSchema";