import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { ApiError } from "../../api/client";
import type { DietOrderFormValues, DietFoodOrderFormValues } from "./zodSchema";
import type { DietItemScheduleDetails, DietOrderDetails, DietReport } from "./types";

/**
 * M10 Diet hooks — consume the diet-service REST API (Spring Boot :8087).
 *
 * Endpoints (DietOrderController @ /api/v1/diet-orders, DietMasterController
 * @ /api/v1/diet):
 *   GET  /api/v1/diet-orders                       list orders (admission-scoped)
 *   POST /api/v1/diet-orders                       raise a diet order
 *   GET  /api/v1/diet-orders/{id}                  get a single order
 *   GET  /api/v1/diet-orders/active                latest active order for admission
 *   PUT  /api/v1/diet-orders/sign                  sign (activate) orders
 *   PUT  /api/v1/diet-orders/{id}/status           set order status by id
 *   PUT  /api/v1/diet-orders/status-by-order-id    set status by business orderId
 *   GET  /api/v1/diet-orders/kitchen               kitchen production view
 *   POST /api/v1/diet-orders/{orderId}/food-items  order today's food items
 *   GET  /api/v1/diet-orders/{orderId}/food-items  list schedule rows of an order
 *   PUT  /api/v1/diet-orders/food-items/advance    advance a schedule row
 *   PUT  /api/v1/diet-orders/food-items/uncheck    uncheck a schedule row
 *   PUT  /api/v1/diet-orders/food-items/action-status  set action status (hold/cancel...)
 *   POST /api/v1/diet-orders/food-items/remove-item    remove one item from a row
 *   GET  /api/v1/diet/orders/report                diet report (counts by orderStatus)
 *   GET  /api/v1/diet/sessions                     diet session masters
 *   GET  /api/v1/diet/items                        diet item masters
 */

const DIET_BASE: string = import.meta.env.VITE_DIET_API_BASE ?? "/api";

async function dietJson<T>(url: string, options?: RequestInit): Promise<T> {
  const res = await fetch(url, {
    headers: { "Content-Type": "application/json", "X-Request-ID": crypto.randomUUID() },
    ...options,
  });
  if (!res.ok) {
    const err = await res.json().catch(() => ({ message: res.statusText }));
    throw new ApiError(res.status, err.errorCode ?? "UNKNOWN", err.message ?? "Request failed");
  }
  return res.json();
}

// ---- Query keys ------------------------------------------------------------

export const dietQueryKeys = {
  orders: (admissionId?: number) => ["diet", "orders", { admissionId }] as const,
  order: (id: number) => ["diet", "orders", id] as const,
  activeOrder: (admissionId: number) => ["diet", "orders", "active", admissionId] as const,
  kitchen: (unitId: number, sessionId?: number) => ["diet", "kitchen", { unitId, sessionId }] as const,
  foodItems: (orderId: number) => ["diet", "food-items", orderId] as const,
  report: () => ["diet", "report"] as const,
  sessions: (unitId?: number) => ["diet", "sessions", { unitId }] as const,
  items: (unitId?: number) => ["diet", "items", { unitId }] as const,
  todayMenu: (unitId: number, dietTypeId?: number) => ["diet", "today-menu", { unitId, dietTypeId }] as const,
};

// ---- Diet orders -----------------------------------------------------------

export function useDietOrders(admissionId?: number) {
  return useQuery({
    queryKey: dietQueryKeys.orders(admissionId),
    queryFn: () => {
      const params = new URLSearchParams();
      if (admissionId) {
        params.set("admissionId", String(admissionId));
        params.set("unitId", "1"); // default unit in dev; cross-domain decoration deferred
      }
      const qs = params.toString();
      return dietJson<DietOrderDetails[]>(`${DIET_BASE}/api/v1/diet-orders${qs ? `?${qs}` : ""}`);
    },
    enabled: admissionId !== undefined,
  });
}

export function useDietOrder(id: number) {
  return useQuery({
    queryKey: dietQueryKeys.order(id),
    queryFn: () => dietJson<DietOrderDetails>(`${DIET_BASE}/api/v1/diet-orders/${id}`),
    enabled: id > 0,
  });
}

export function useCreateDietOrder() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (values: DietOrderFormValues) =>
      dietJson<DietOrderDetails>(`${DIET_BASE}/api/v1/diet-orders`, {
        method: "POST",
        body: JSON.stringify(values),
      }),
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: ["diet", "orders"] });
      void qc.invalidateQueries({ queryKey: ["diet", "kitchen"] });
    },
  });
}

export function useUpdateDietOrderStatus() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, status }: { id: number; status: number }) =>
      dietJson<{ updated: boolean }>(`${DIET_BASE}/api/v1/diet-orders/${id}/status`, {
        method: "PUT",
        body: JSON.stringify({ orderStatus: status }),
      }),
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: ["diet", "orders"] });
      void qc.invalidateQueries({ queryKey: ["diet", "report"] });
    },
  });
}

export function useSignDietOrders() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ orderIds }: { orderIds: number[] }) =>
      dietJson<{ signed: boolean }>(`${DIET_BASE}/api/v1/diet-orders/sign`, {
        method: "PUT",
        body: JSON.stringify(orderIds),
      }),
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: ["diet", "orders"] });
      void qc.invalidateQueries({ queryKey: ["diet", "kitchen"] });
    },
  });
}

// ---- Kitchen production view ------------------------------------------------

export function useKitchenOrders(unitId: number, dietSessionId?: number) {
  return useQuery({
    queryKey: dietQueryKeys.kitchen(unitId, dietSessionId),
    queryFn: () => {
      const params = new URLSearchParams();
      params.set("unitId", String(unitId));
      if (dietSessionId) params.set("dietSessionId", String(dietSessionId));
      return dietJson<DietItemScheduleDetails[]>(
        `${DIET_BASE}/api/v1/diet-orders/kitchen?${params}`,
      );
    },
    enabled: unitId > 0,
    refetchInterval: 30_000,
  });
}

export function useOrderFoodItems(orderId: number, unitId: number) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (values: DietFoodOrderFormValues) =>
      dietJson<DietItemScheduleDetails[]>(
        `${DIET_BASE}/api/v1/diet-orders/${orderId}/food-items?unitId=${unitId}`,
        { method: "POST", body: JSON.stringify(values) },
      ),
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: ["diet", "food-items", orderId] });
      void qc.invalidateQueries({ queryKey: ["diet", "kitchen"] });
    },
  });
}

export function useFoodItems(orderId: number) {
  return useQuery({
    queryKey: dietQueryKeys.foodItems(orderId),
    queryFn: () => dietJson<DietItemScheduleDetails[]>(
      `${DIET_BASE}/api/v1/diet-orders/${orderId}/food-items`,
    ),
    enabled: orderId > 0,
  });
}

export function useAdvanceFoodOrder() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (values: {
      admissionId: number;
      dietOrderId: number;
      dietSessionId: number;
      scheduleId: number;
      unitId: number;
    }) => {
      const params = new URLSearchParams({
        admissionId: String(values.admissionId),
        dietOrderId: String(values.dietOrderId),
        dietSessionId: String(values.dietSessionId),
        scheduleId: String(values.scheduleId),
        unitId: String(values.unitId),
      });
      return dietJson<DietItemScheduleDetails>(
        `${DIET_BASE}/api/v1/diet-orders/food-items/advance?${params}`,
        { method: "PUT" },
      );
    },
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: ["diet", "kitchen"] });
    },
  });
}

export function useUncheckFoodOrder() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (values: {
      admissionId: number;
      dietOrderId: number;
      dietSessionId: number;
      scheduleId: number;
      unitId: number;
    }) => {
      const params = new URLSearchParams({
        admissionId: String(values.admissionId),
        dietOrderId: String(values.dietOrderId),
        dietSessionId: String(values.dietSessionId),
        foodOrderId: String(values.scheduleId),
        unitId: String(values.unitId),
      });
      return dietJson<DietItemScheduleDetails>(
        `${DIET_BASE}/api/v1/diet-orders/food-items/uncheck?${params}`,
        { method: "PUT" },
      );
    },
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: ["diet", "kitchen"] });
    },
  });
}

export function useSetFoodActionStatus() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (values: {
      admissionId: number;
      dietOrderId: number;
      dietSessionId: number;
      scheduleId: number;
      unitId: number;
      dietActionStatus: string;
      reasonForHold?: string;
    }) => {
      const params = new URLSearchParams({
        admissionId: String(values.admissionId),
        dietOrderId: String(values.dietOrderId),
        dietSessionId: String(values.dietSessionId),
        foodOrderId: String(values.scheduleId),
        unitId: String(values.unitId),
        dietActionStatus: values.dietActionStatus,
      });
      if (values.reasonForHold) params.set("reasonForHold", values.reasonForHold);
      const body: Record<string, string> = {};
      if (values.reasonForHold) body.reasonForHold = values.reasonForHold;
      return dietJson<DietItemScheduleDetails>(
        `${DIET_BASE}/api/v1/diet-orders/food-items/action-status?${params}`,
        { method: "PUT", body: JSON.stringify(body) },
      );
    },
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: ["diet", "kitchen"] });
    },
  });
}

// ---- Diet report -----------------------------------------------------------

/**
 * M10 diet report (GET /api/v1/diet/orders/report): counts of diet orders
 * grouped by order_status — TOTAL / UNRELEASED / ACTIVE / CANCELLED.
 * Kitchen board uses the same counts as a production summary.
 */
export function useDietReport() {
  return useQuery({
    queryKey: dietQueryKeys.report(),
    queryFn: () => dietJson<DietReport>(`${DIET_BASE}/api/v1/diet/orders/report`),
    refetchInterval: 60_000,
  });
}

// ---- Diet masters ----------------------------------------------------------

/** diet-session-master rows (feeding windows: breakfast/lunch/dinner...). */
export function useDietSessions(unitId: number) {
  return useQuery({
    queryKey: dietQueryKeys.sessions(unitId),
    queryFn: () =>
      dietJson<DietSessionMaster[]>(`${DIET_BASE}/api/v1/diet/sessions?unitId=${unitId}`),
    enabled: unitId > 0,
    staleTime: 10 * 60 * 1000,
  });
}

/** diet-item-master rows (food items). */
export function useDietItems(unitId: number) {
  return useQuery({
    queryKey: dietQueryKeys.items(unitId),
    queryFn: () =>
      dietJson<DietItemMaster[]>(`${DIET_BASE}/api/v1/diet/items?unitId=${unitId}`),
    enabled: unitId > 0,
    staleTime: 10 * 60 * 1000,
  });
}

/** Today's menu items for a unit, optionally narrowed to one diet type. */
export function useTodayMenu(unitId: number, dietTypeId?: number) {
  const qs = new URLSearchParams({ unitId: String(unitId) });
  if (dietTypeId) qs.set("dietTypeId", String(dietTypeId));
  return useQuery({
    queryKey: dietQueryKeys.todayMenu(unitId, dietTypeId),
    queryFn: () => dietJson<TodayDietMenu[]>(`${DIET_BASE}/api/v1/diet/today-menu?${qs}`),
    enabled: unitId > 0,
  });
}
