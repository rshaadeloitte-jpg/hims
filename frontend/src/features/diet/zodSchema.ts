import { z } from "zod";

/**
 * M10 Diet Zod schemas — mirror the diet-service (MIG-023, S18) request DTOs.
 *
 * Legacy status dictionaries preserved verbatim from DietServiceImpl /
 * DietActionStatus enum (see services/diet-service/.../DietOrderService.java):
 *
 *   order_status (diet_order_details):
 *     1 = UNRELEASED, 2 = ACTIVE / SIGNED, 3 = CANCELLED / REPLACED
 *   item_schedule_status (diet_item_schedule_details):
 *     2 = REQUEST_RAISED (default on order), increments toward 6 = COMPLETED/LOCKED
 *     rows with status < 6 are replaceable
 *   diet_action_status enum: HOLD / CANCEL / REJECT / RELEASE
 *
 * The order-number rule (legacy CIMS_GETLATESTAUTOCODEANDINCREMETITBYONE on
 * activity 't_dietorderdetails', template null) is server-side: order_number is
 * a zero-padded sequence. The frontend only validates the request fields.
 */

const positiveLong = z.coerce.number().int().positive();
const optionalPositiveLong = z.preprocess(
  (v) => (v === "" || v == null ? undefined : v),
  z.coerce.number().int().positive().optional(),
);

// ---- Diet order raise (POST /api/v1/diet-orders) --------------------------

/**
 * Raise a patient diet order. Mirrors DietOrderController.raise payload.
 * serviceId is the diet type (legacy orderTypeIen / BERA035 when missing).
 */
export const dietOrderSchema = z.object({
  patientId: positiveLong,
  admissionId: positiveLong,
  wardId: optionalPositiveLong,
  bedId: optionalPositiveLong,
  doctorId: optionalPositiveLong,
  serviceId: positiveLong.describe("Diet type (serviceId) is required"),
  fromDate: z.string().min(1, "From date is required"),
  toDate: optionalPositiveLong.optional(),
  remarks: z.preprocess(
    (v) => (v === "" || v == null ? undefined : v),
    z.string().max(1000, "Remarks too long").optional(),
  ),
  unitId: optionalPositiveLong,
  orderStatus: positiveLong.optional(),
});

export type DietOrderFormValues = z.infer<typeof dietOrderSchema>;

// ---- Food item schedule ordering (POST /{orderId}/food-items) -------------

const foodItemSessionSchema = z.object({
  sessionId: positiveLong,
  itemIds: z.array(positiveLong).min(1, "At least one item is required"),
  caloriesConsume: optionalPositiveLong.optional(),
});

/** Payload to order today's food items for a diet order. */
export const dietFoodOrderSchema = z.object({
  admissionId: positiveLong,
  orderId: positiveLong,
  sessions: z.array(foodItemSessionSchema).min(1, "At least one session is required"),
  unitId: optionalPositiveLong,
});

export type DietFoodOrderFormValues = z.infer<typeof dietFoodOrderSchema>;

// ---- Status / action dictionaries -----------------------------------------

/**
 * diet_order_details.order_status
 * 1 = UNRELEASED (raised) · 2 = ACTIVE (signed) · 3 = CANCELLED / REPLACED
 */
export const ORDER_STATUS = {
  UNRELEASED: 1,
  ACTIVE: 2,
  CANCELLED: 3,
} as const;

export type OrderStatusKey = keyof typeof ORDER_STATUS;
export type OrderStatusValue = (typeof ORDER_STATUS)[OrderStatusKey];

/** Human-readable label for each order_status code. */
export const ORDER_STATUS_LABEL: Record<OrderStatusValue, string> = {
  [ORDER_STATUS.UNRELEASED]: "Unreleased",
  [ORDER_STATUS.ACTIVE]: "Active",
  [ORDER_STATUS.CANCELLED]: "Cancelled",
};

/** Color helper for status badges. */
export const ORDER_STATUS_COLOR: Record<OrderStatusValue, string> = {
  [ORDER_STATUS.UNRELEASED]: "var(--color-warning)",
  [ORDER_STATUS.ACTIVE]: "var(--color-success)",
  [ORDER_STATUS.CANCELLED]: "var(--color-danger)",
};

/**
 * diet_item_schedule_details.item_schedule_status
 * 2 = REQUEST_RAISED · 8 = PREPARING · 9 = PREPARED · 10 = DELIVERED · 6 = COMPLETED/LOCKED
 */
export const SCHEDULE_STATUS = {
  REQUEST_RAISED: 2,
  PREPARING: 8,
  PREPARED: 9,
  DELIVERED: 10,
  COMPLETED: 6,
} as const;

export type ScheduleStatusValue = (typeof SCHEDULE_STATUS)[keyof typeof SCHEDULE_STATUS];

/** Kitchen production stages (item_schedule_status >= 3) — Linear kanban lanes. */
export const KITCHEN_LANE_STATUS = {
  PREPARING: 8,
  PREPARED: 9,
  DELIVERED: 10,
} as const;

export type KitchenLaneKey = keyof typeof KITCHEN_LANE_STATUS;

/** Label + badge color per kitchen lane. */
export interface KitchenLane {
  key: KitchenLaneKey;
  label: string;
  status: ScheduleStatusValue;
  color: string;
}

export const KITCHEN_LANES: KitchenLane[] = [
  {
    key: "PREPARING",
    label: "To Prepare",
    status: SCHEDULE_STATUS.PREPARING,
    color: "var(--color-warning)",
  },
  {
    key: "PREPARED",
    label: "Prepared",
    status: SCHEDULE_STATUS.PREPARED,
    color: "var(--color-info)",
  },
  {
    key: "DELIVERED",
    label: "Delivered",
    status: SCHEDULE_STATUS.DELIVERED,
    color: "var(--color-success)",
  },
];

/** diet_action_status enum: HOLD / CANCEL / REJECT / RELEASE. */
export const DIET_ACTION_STATUS = {
  HOLD: "HOLD",
  CANCEL: "CANCEL",
  REJECT: "REJECT",
  RELEASE: "RELEASE",
} as const;

export type DietActionStatus = (typeof DIET_ACTION_STATUS)[keyof typeof DIET_ACTION_STATUS];

/** Form values for the kitchen action modal. */
export const kitchenActionSchema = z.object({
  actionStatus: z.nativeEnum(DIET_ACTION_STATUS),
  reasonForHold: z.preprocess(
    (v) => (v === "" || v == null ? undefined : v),
    z.string().max(500, "Reason too long").optional(),
  ),
});

export type KitchenActionFormValues = z.infer<typeof kitchenActionSchema>;

/** Form values for advancing a schedule row. */
export const scheduleAdvanceSchema = z.object({
  admissionId: positiveLong,
  dietOrderId: positiveLong,
  dietSessionId: positiveLong,
  scheduleId: positiveLong,
  unitId: optionalPositiveLong,
});

export type ScheduleAdvanceFormValues = z.infer<typeof scheduleAdvanceSchema>;

// ---- Diet type / session masters (from diet-service masters API) ----------

/** diet_session_master row (M_DIETSESSIONMASTER). */
export interface DietSessionMaster {
  id: number;
  description: string;
  fromTime: string;
  toTime: string;
  dietSequenceNumber: number;
  unitId?: number;
  status?: boolean;
}

/** diet_item_master row (M_DIETITEMMASTER) — a food item. */
export interface DietItemMaster {
  id: number;
  dietItemType: string;
  dietItemIen: number;
  description: string;
  energy: number;
  dietTypeIen: string;
  dietSessionId: string;
  localUnit: string;
  unitId?: number;
  status?: boolean;
}

/** today_diet_menu row (T_TODAYDIETMENU) — today's planned menu per diet type/session. */
export interface TodayDietMenu {
  id: number;
  dietTypeId: number;
  description: string;
  dietSessionId: number;
  itemId: number;
  unitId?: number;
  status?: boolean;
}
