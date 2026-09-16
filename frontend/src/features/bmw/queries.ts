import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { ApiError } from "../../api/client";
import type {
  BagStatusUpdate,
  DecomposeFormValues,
  FirmInfo,
  HandoverFormValues,
  Store,
  WasteBagDetail,
  WasteListFilterValues,
  WasteRequestValues,
  WasteSummary,
} from "./zodSchema";

/**
 * M18 Biomedical Waste (BMW) — TanStack Query hooks.
 *
 * Wire surface ported from BioMedicalWasteManagementServiceImpl.java:
 *   GET  /getBioMedWasteCount          → summary counts by status
 *   POST /getbioMedicalWasteStatus     → paginated request list
 *   POST /addNewBioRequest             → create request (with bag details)
 *   GET  /getBioRequestDetails         → per-request bag details (+ barcodes)
 *   GET  /approveBioMedicalRequest     → PENDING → ACCEPTED
 *   GET  /handOverBags                 → HANDOVERED (to firm)
 *   POST /receiveBioRequestMainStore   → RECEIVED
 *   GET  /handedOverToFirm            → last disposal-firm info
 *   GET  /processBioRequest            → COMPLETED (decompose by firm)
 *   GET  /validateBarcode              → barcode membership check
 *   POST /updateBagStatus              → per-bag status advance
 */

const BMW_BASE: string = import.meta.env.VITE_BMW_API_BASE ?? "/api";
const BMW_PATH = `${BMW_BASE}/api/rest/bioMedicalWasteManagement`;

/** Shared typed fetcher — mirrors the bmwJson shape from the legacy module. */
async function bmwJson<T>(url: string, options?: RequestInit): Promise<T> {
  const res = await fetch(url, {
    headers: {
      "Content-Type": "application/json",
      "X-Request-ID": crypto.randomUUID(),
    },
    ...options,
  });
  if (!res.ok) {
    const err = await res.json().catch(() => ({ message: res.statusText }));
    throw new ApiError(
      res.status,
      err.errorCode ?? "UNKNOWN",
      err.message ?? "Request failed",
    );
  }
  if (res.status === 204) return undefined as T;
  return res.json();
}

// ---- Query keys --------------------------------------------------------------
export const wasteQueryKeys = {
  all: ["waste"] as const,
  summary: ["waste", "summary"] as const,
  list: (filter: WasteListFilterValues) => ["waste", "list", filter] as const,
  detail: (id: number, unitId: number) => ["waste", "detail", id, unitId] as const,
  stores: ["waste", "stores"] as const,
  bags: ["waste", "bags"] as const,
  firm: (unitId: number) => ["waste", "firm", unitId] as const,
};

// ---- Summary counts ----------------------------------------------------------
export function useWasteSummary(unitId: number, storeId: number, from: string, to: string) {
  return useQuery<WasteSummary>({
    queryKey: wasteQueryKeys.summary,
    queryFn: () =>
      bmwJson<WasteSummary>(`${BMW_PATH}/getBioMedWasteCount`, {
        method: "POST",
        body: JSON.stringify({ unitId, storeId, fromDate: from, toDate: to }),
      }),
    enabled: Boolean(unitId && storeId && from && to),
    staleTime: 60_000,
  });
}

// ---- Paginated request list --------------------------------------------------
export interface WasteRequestRow {
  id: number;
  requisitionNo: string;
  fromStore: string;
  toStore: string;
  storeId: number;
  toStoreId: number;
  date: string;
  requestStatus: number;
  handedOverTo: string;
  status: boolean;
  unitId: number;
}

export interface WasteListResponse {
  items: WasteRequestRow[];
  total: number;
  page: number;
  size: number;
}

export function useWasteList(filter: WasteListFilterValues) {
  return useQuery<WasteListResponse>({
    queryKey: wasteQueryKeys.list(filter),
    queryFn: async () => {
      const params = new URLSearchParams();
      params.set("page", String(filter.page ?? 0));
      params.set("count", String(filter.size ?? 25));
      if (filter.search) params.set("search", filter.search);
      const body: Record<string, unknown> = {
        unitId: filter.storeId, // unitId resolved from session in real app
      };
      // category → requestStatus id
      if (filter.category) {
        // server expects category name (pending/accepted/handovered/received/completed)
      }
      if (filter.fromDate) body.fromDate = filter.fromDate;
      if (filter.toDate) body.toDate = filter.toDate;
      if (filter.storeId) (body as any).storeId = filter.storeId;
      if (filter.fromStoreId) (body as any).fromStoreId = filter.fromStoreId;
      const resp = await bmwJson<unknown[]>(
        `${BMW_PATH}/getbioMedicalWasteStatus?${params}`,
        { method: "POST", body: JSON.stringify(body) },
      );
      // The legacy backend returns a bare array; derive total from length.
      return {
        items: resp as unknown as WasteRequestRow[],
        total: resp.length,
        page: filter.page ?? 0,
        size: filter.size ?? 25,
      };
    },
  });
}

// ---- Per-request bag details (with barcodes) ---------------------------------
export function useWasteDetails(id: number, unitId: number) {
  return useQuery<WasteBagDetail[]>({
    queryKey: wasteQueryKeys.detail(id, unitId),
    queryFn: () =>
      bmwJson<WasteBagDetail[]>(
        `${BMW_PATH}/getBioRequestDetails?id=${id}&unitId=${unitId}`,
      ),
    enabled: Boolean(id && unitId),
    staleTime: 30_000,
  });
}

// ---- Create waste request ----------------------------------------------------
export function useCreateWasteRequest() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (payload: WasteRequestValues) =>
      bmwJson<{ requisitionNo: string }>(`${BMW_PATH}/addNewBioRequest`, {
        method: "POST",
        body: JSON.stringify({
          bioMedicalWaste: {
            unitId: payload.unitId,
            storeId: payload.storeId,
            toStoreId: payload.toStoreId,
            date: payload.date,
          },
          bioMedicalWasteDetails: payload.details.map((d) => ({
            bagId: d.bagMasterId,
            qty: d.qty,
            weight: d.weight,
            remark: d.remark,
            unitId: payload.unitId,
          })),
        }),
      }),
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: wasteQueryKeys.all });
    },
  });
}

// ---- Approve (PENDING → ACCEPTED) -------------------------------------------
export function useApproveRequest() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, unitId }: { id: number; unitId: number }) =>
      bmwJson<boolean>(
        `${BMW_PATH}/approveBioMedicalRequest?id=${id}&unitId=${unitId}`,
      ),
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: wasteQueryKeys.all });
    },
  });
}

// ---- Hand over to firm -------------------------------------------------------
export function useHandoverToFirm() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, unitId, ...rest }: HandoverFormValues & { id: number; unitId: number }) =>
      bmwJson<string>(
        `${BMW_PATH}/handOverBags?id=${id}&unitId=${unitId}&handedOverTo=${encodeURIComponent(rest.handedOverTo)}`,
      ),
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: wasteQueryKeys.all });
    },
  });
}

// ---- Receive at main store ---------------------------------------------------
export function useReceiveBioRequest() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, unitId }: { id: number; unitId: number }) =>
      bmwJson<void>(`${BMW_PATH}/receiveBioRequestMainStore`, {
        method: "POST",
        body: JSON.stringify({ bioMedicalWasteId: id, unitId }),
      }),
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: wasteQueryKeys.all });
    },
  });
}

// ---- Decompose / dispatch to disposal firm -----------------------------------
export function useDecomposeRequest() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, unitId, ...firm }: DecomposeFormValues & { id: number; unitId: number }) =>
      bmwJson<boolean>(
        `${BMW_PATH}/processBioRequest?id=${id}&unitId=${unitId}&firmName=${encodeURIComponent(firm.firmName)}&contactPerson=${encodeURIComponent(firm.contactPerson)}&contactNo=${firm.contactNo}`,
      ),
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: wasteQueryKeys.all });
    },
  });
}

// ---- Handed-over-to firm info ------------------------------------------------
export function useHandedOverToFirm(unitId: number) {
  return useQuery<FirmInfo>({
    queryKey: wasteQueryKeys.firm(unitId),
    queryFn: () => bmwJson<FirmInfo>(`${BMW_PATH}/handedOverToFirm`),
    enabled: Boolean(unitId),
    staleTime: 5 * 60_000,
  });
}

// ---- Barcode validation ------------------------------------------------------
export function useValidateBarcode() {
  return useMutation({
    mutationFn: ({ id, unitId, barcode }: { id: number; unitId: number; barcode: string }) =>
      bmwJson<boolean>(
        `${BMW_PATH}/validateBarcode?id=${id}&unitId=${unitId}&barcode=${encodeURIComponent(barcode)}`,
      ),
  });
}

// ---- Per-bag status update ---------------------------------------------------
export function useUpdateBagStatus() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (updates: BagStatusUpdate[]) =>
      bmwJson<void>(`${BMW_PATH}/updateBagStatus`, {
        method: "POST",
        body: JSON.stringify(updates),
      }),
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: wasteQueryKeys.all });
    },
  });
}

// ---- Reference data: stores + bag masters ------------------------------------
export function useStores(unitId: number) {
  return useQuery<Store[]>({
    queryKey: wasteQueryKeys.stores,
    queryFn: () => bmwJson<Store[]>(`${BMW_BASE}/api/v1/stores?unitId=${unitId}`),
    enabled: Boolean(unitId),
    staleTime: 5 * 60_000,
  });
}

export function useWasteBags() {
  return useQuery<Store[]>({
    queryKey: wasteQueryKeys.bags,
    queryFn: () => bmwJson<Store[]>(`${BMW_BASE}/api/v1/masters/wastebag`),
    staleTime: 5 * 60_000,
  });
}
