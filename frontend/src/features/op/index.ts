// Public OP-module barrel — re-exports the four pages and the shared widgets
// so route shells can import them with a single path.
//
//   /m/op/walkinVisit      -> WalkinVisitPage
//   /m/op/addAppointment  -> AddAppointmentPage
//   /m/op/appointmentList -> AppointmentListPage
//   /m/op/abha            -> AbhaPage
export { WalkinVisitPage } from "./pages/WalkinVisitPage";
export { AddAppointmentPage } from "./pages/AddAppointmentPage";
export { AppointmentListPage } from "./pages/AppointmentListPage";
export { AbhaPage } from "./pages/AbhaPage";

// Re-exports for the API surface and shared widgets (useful for tests and for
// the app shell that wires the routes into the router).
export * from "./api/client";
export * from "./api/queries";
export * from "./api/types";

export { PatientBanner } from "./components/PatientBanner";
export { PatientSearch } from "./components/PatientSearch";
export { DataTable } from "./components/DataTable";
export type { DataTableColumn, SortDir } from "./components/DataTable";
export {
  AppointmentStatusBadge,
  AbhaStatusBadge,
  QueueStatusBadge,
  QueuePriorityBadge,
  YesNoBadge,
} from "./components/StatusBadges";
export { ConfirmDialog } from "./components/ConfirmDialog";
export { MlcDetailsSection } from "./components/MlcDetailsSection";

export { useOpSession, resetOpSession } from "./store/opSession";
export type { OpSession, WalkinDraft } from "./store/opSession";
