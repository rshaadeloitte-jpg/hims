/**
 * Type definitions for the OP module's three downstream services.
 *
 *   patient-service  :8090  (MIG-030 / Wave 3)
 *   appointment-service :8091  (WP-4 / Wave 4)
 *   queue-service    :8089  (MIG-022 / Wave 2)
 *
 * Shapes are SUBSETS of the OpenAPI-generated contracts: the OP screens only
 * touch the fields they actually use. The OpenAPI documents are the source
 * of truth for the full wire surface; these interfaces pin what the screens
 * depend on so MSW handlers and component tests can be written against the
 * same vocabulary.
 */

/** patient-service — registration / demographic form (BR-PAT-001..006). */
export type PatientSex = "M" | "F" | "O" | "U";

export interface PatientSponsor {
  sponsorId: number;
  sponsorCode?: string;
  sponsorName?: string;
  schemeCode?: string;
  schemeName?: string;
  policyNo?: string;
  validFrom?: string;
  validTo?: string;
  isPrimary?: boolean;
}

export interface Patient {
  id: number;
  /** Master Registration Number (legacy T_REGISTRATION.MRNO; BR-PAT-001). */
  mrn?: string;
  /** Temporary MRN (BR-PAT-001 t-prefix); promotion moves to mrn. */
  isTempMrn?: boolean;
  firstName: string;
  middleName?: string;
  lastName?: string;
  fatherName?: string;
  motherName?: string;
  dateOfBirth?: string;       // YYYY-MM-DD
  age?: number;
  sex: PatientSex;
  mobile?: string;
  aadhaar?: string;
  abha?: string;
  abhaNumber?: string;
  /** emergency contact fields from the walk-in form. */
  emergencyContactName?: string;
  emergencyContactPhone?: string;
  /** MLC details (BR-PAT-005 / FIR-001). */
  mlcNumber?: string;
  mlcFirNumber?: string;
  mlcPoliceStation?: string;
  mlcNotes?: string;
  sponsors?: PatientSponsor[];
  status?: boolean;
  createdAt?: string;
  updatedAt?: string;
}

export interface PatientSearchHit {
  id: number;
  mrn?: string;
  firstName: string;
  lastName?: string;
  mobile?: string;
  abha?: string;
  dateOfBirth?: string;
  age?: number;
  sex: PatientSex;
}

export interface PatientSearchQuery {
  q?: string;          // free-text — name / MRN / mobile / ABHA
  mrn?: string;
  mobile?: string;
  abha?: string;
  limit?: number;
}

/** appointment-service — BR-APT-001 forward-only status machine. */
export type AppointmentStatus =
  | 1  // BOOKED
  | 2  // CHECKED_IN
  | 3  // COMPLETED
  | 4  // CANCELLED
  | 5  // RESCHEDULED
  | 6; // NO_SHOW

export type AppointmentSource = "WALK_IN" | "ONLINE" | "EMERGENCY" | "TELECONSULT" | "FPV" | "RGHS";

export interface AppointmentSlot {
  slotId: number;
  doctorId: number;
  doctorName: string;
  startAt: string;        // ISO-8601
  endAt: string;
  capacity: number;
  booked: number;
  /** Computed for convenience; true iff booked < capacity. */
  available: boolean;
}

export interface Appointment {
  id: number;
  patientId: number;
  patientName?: string;
  patientMrn?: string;
  doctorId: number;
  doctorName?: string;
  departmentId: number;
  departmentName?: string;
  cabinId?: number;
  startAt: string;
  endAt: string;
  status: AppointmentStatus;
  source?: AppointmentSource;
  notes?: string;
  reason?: string;
  tokenNumber?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface AppointmentListQuery {
  status?: AppointmentStatus | "all" | "checkedIn" | "pending" | "noShow" | "walkIn" | "emergency";
  doctorId?: number;
  departmentId?: number;
  patientId?: number;
  dateFrom?: string;
  dateTo?: string;
  page?: number;
  size?: number;
}

export interface AppointmentListResponse {
  items: Appointment[];
  page: number;
  size: number;
  total: number;
  hasMore: boolean;
}

export interface Doctor {
  id: number;
  name: string;
  departmentId: number;
  departmentName?: string;
  isActive?: boolean;
}

export interface Department {
  id: number;
  name: string;
  isOpd?: boolean;
  isActive?: boolean;
}

/** queue-service — MIG-022 token issuance. */
export type QueuePriority = "NORMAL" | "EMERGENCY" | "VIP";

export interface QueueToken {
  id: number;
  tokenNumber: string;     // daily-reset counter per cabinet (legacy OPDTokenNo)
  patientId: number;
  patientName?: string;
  patientMrn?: string;
  doctorId?: number;
  doctorName?: string;
  departmentId?: number;
  departmentName?: string;
  counterId?: number;
  counterName?: string;
  priority: QueuePriority;
  status: "WAITING" | "IN_PROGRESS" | "COMPLETED" | "CANCELLED" | "NO_SHOW" | "TRANSFERRED";
  issuedAt: string;
  calledAt?: string;
  completedAt?: string;
  position?: number;
}

export interface QueueTokenCreate {
  patientId: number;
  doctorId?: number;
  departmentId?: number;
  counterId?: number;
  priority?: QueuePriority;
  notes?: string;
}

/** ABDM / ABHA — minimal contract for the OP screen (catalog #25). */
export type AbhaStatus = "PENDING_OTP" | "OTP_VERIFIED" | "LINKED" | "FAILED";

export interface AbhaEnrollment {
  id: number;
  patientId: number;
  aadhaar?: string;
  mobile?: string;
  status: AbhaStatus;
  abhaNumber?: string;
  abhaAddress?: string;
  qrPayload?: string;     // for the scan-and-verify UI
  createdAt?: string;
  linkedAt?: string;
}
