/**
 * M10 Diet runtime types — the diet-service response DTOs that the frontend
 * consumes but that don't need Zod parsing (server is the source of truth).
 *
 * Field names mirror the JPA entities in
 *   services/diet-service/src/main/java/com/hims/diet/domain/model/*
 * and the API surface documented in docs/modernization/04-api-catalog.md (M10).
 */

/** diet_order_details row (T_DIETORDERDETAILS). */
export interface DietOrderDetails {
  id: number;
  orderNumber: string;
  patientId: number;
  admissionId: number;
  wardId: number;
  bedId: number;
  doctorId: number;
  serviceId: number;
  fromDate: string;
  toDate: string;
  remarks: string;
  orderId: number;
  orderStatus: number;
  unitId: number;
  status: boolean;
  addedBy: number;
  addedDatetime: string;
  updatedBy: number;
  updatedDatetime: string;
}

/** diet_item_schedule_details row (T_DIETITEMSCHEDULEDETAILS) — kitchen view. */
export interface DietItemScheduleDetails {
  id: number;
  admissionId: number;
  orderId: number;
  sessionId: number;
  itemIds: string;
  itemScheduleStatus: number;
  dietActionStatus: string | null;
  reasonForHold: string | null;
  dietStatusTime: string;
  foodOrderedDate: string;
  presentCalories: number;
  unitId: number;
  status: boolean;
  addedBy: number;
  addedDatetime: string;
  updatedBy: number;
  updatedDatetime: string;
}

/** Diet report (GET /api/v1/diet/orders/report) — counts by orderStatus. */
export interface DietReport {
  TOTAL: number;
  UNRELEASED: number;
  ACTIVE: number;
  CANCELLED: number;
}
