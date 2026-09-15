import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { ApiError } from "../../api/client";

const API_BASE = import.meta.env.VITE_API_BASE ?? "/api";

async function apiJson<T>(url: string, options?: RequestInit): Promise<T> {
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

export interface InventoryItem {
  id: number;
  name: string;
  code: string;
  unitId: number;
  status: string;
}

export interface InventoryStore {
  id: number;
  name: string;
  code: string;
  location: string;
}

export interface InventoryBatch {
  id: number;
  batchNumber: string;
  itemId: number;
  expiryDate: string;
  quantity: number;
}

export interface StockBalance {
  id: number;
  itemId: number;
  batchId: number;
  quantity: number;
  location: string;
}

export interface IndentRequest {
  id?: number;
  storeId: number;
  requestDate: string;
  priority: "ROUTINE" | "URGENT" | "EMERGENCY";
  remarks?: string;
  lines: IndentLine[];
  status?: string;
}

export interface IndentLine {
  id?: number;
  itemId: number;
  requestedQty: number;
  remarks?: string;
  itemName?: string;
  itemCode?: string;
}

export interface StockAdjustment {
  id?: number;
  itemId: number;
  batchId?: number;
  adjustmentType: "INCREASE" | "DECREASE";
  quantity: number;
  reason: string;
  reference?: string;
  adjustedBy?: string;
  adjustedAt?: string;
}

export interface ItemEnquiryResult {
  id: number;
  name: string;
  code: string;
  unitName?: string;
  category?: string;
  currentStock: number;
  reorderLevel: number;
  maxStock: number;
  status: string;
  batches: BatchDetail[];
}

export interface BatchDetail {
  id: number;
  batchNumber: string;
  expiryDate: string;
  quantity: number;
  location: string;
}

export interface StockLevelTrend {
  date: string;
  openingStock: number;
  receivedQty: number;
  issuedQty: number;
  closingStock: number;
}

export const inventoryQueryKeys = {
  items: () => ["inventory", "items"] as const,
  item: (id: number) => ["inventory", "items", id] as const,
  stores: () => ["inventory", "stores"] as const,
  batches: () => ["inventory", "batches"] as const,
  balances: () => ["inventory", "balances"] as const,
  indents: () => ["inventory", "indents"] as const,
  indent: (id: number) => ["inventory", "indents", id] as const,
  adjustments: () => ["inventory", "adjustments"] as const,
  itemEnquiry: (filters: Record<string, unknown>) => ["inventory", "enquiry", filters] as const,
  stockTrend: (itemId: number, days: number) => ["inventory", "stock-trend", itemId, days] as const,
};

// ---- Read hooks ---------------------------------------------------------

export function useInventoryItems() {
  return useQuery<InventoryItem[]>({
    queryKey: inventoryQueryKeys.items(),
    queryFn: () => apiJson<InventoryItem[]>(`${API_BASE}/api/v1/items`),
  });
}

export function useInventoryItem(id: number) {
  return useQuery<InventoryItem>({
    queryKey: inventoryQueryKeys.item(id),
    queryFn: () => apiJson<InventoryItem>(`${API_BASE}/api/v1/items/${id}`),
    enabled: id > 0,
  });
}

export function useInventoryStores() {
  return useQuery<InventoryStore[]>({
    queryKey: inventoryQueryKeys.stores(),
    queryFn: () => apiJson<InventoryStore[]>(`${API_BASE}/api/v1/stores`),
  });
}

export function useInventoryBatches() {
  return useQuery<InventoryBatch[]>({
    queryKey: inventoryQueryKeys.batches(),
    queryFn: () => apiJson<InventoryBatch[]>(`${API_BASE}/api/v1/batches`),
  });
}

export function useStockBalances() {
  return useQuery<StockBalance[]>({
    queryKey: inventoryQueryKeys.balances(),
    queryFn: () => apiJson<StockBalance[]>(`${API_BASE}/api/v1/stock/balances`),
  });
}

// ---- M13 Indent hooks ---------------------------------------------------

export function useIndents() {
  return useQuery<IndentRequest[]>({
    queryKey: inventoryQueryKeys.indents(),
    queryFn: () => apiJson<IndentRequest[]>(`${API_BASE}/api/v1/inventory/indents`),
  });
}

export function useCreateIndent() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (payload: Record<string, unknown>) =>
      apiJson<IndentRequest>(`${API_BASE}/api/v1/inventory/indents`, {
        method: "POST",
        body: JSON.stringify(payload),
      }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: inventoryQueryKeys.indents() });
      qc.invalidateQueries({ queryKey: inventoryQueryKeys.balances() });
    },
  });
}

// ---- M13 Stock adjustment hooks ----------------------------------------

export function useStockAdjustments() {
  return useQuery<StockAdjustment[]>({
    queryKey: inventoryQueryKeys.adjustments(),
    queryFn: () => apiJson<StockAdjustment[]>(`${API_BASE}/api/v1/inventory/adjustments`),
  });
}

export function useCreateStockAdjustment() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (payload: Record<string, unknown>) =>
      apiJson<StockAdjustment>(`${API_BASE}/api/v1/inventory/adjustments`, {
        method: "POST",
        body: JSON.stringify(payload),
      }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: inventoryQueryKeys.adjustments() });
      qc.invalidateQueries({ queryKey: inventoryQueryKeys.balances() });
      qc.invalidateQueries({ queryKey: ["inventory", "enquiry"] });
    },
  });
}

// ---- M13 Item enquiry hooks --------------------------------------------

export function useItemEnquiry(filters: Record<string, unknown> = {}) {
  const params = new URLSearchParams();
  Object.entries(filters).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== "") {
      params.set(key, String(value));
    }
  });
  const qs = params.toString();
  return useQuery<ItemEnquiryResult[]>({
    queryKey: inventoryQueryKeys.itemEnquiry(filters),
    queryFn: () => apiJson<ItemEnquiryResult[]>(`${API_BASE}/api/v1/inventory/items/enquiry${qs ? `?${qs}` : ""}`),
  });
}

// ---- M13 Stock trend (for analytics charts) -----------------------------

export function useStockLevelTrend(itemId: number, days = 30) {
  return useQuery<StockLevelTrend[]>({
    queryKey: inventoryQueryKeys.stockTrend(itemId, days),
    queryFn: () => apiJson<StockLevelTrend[]>(`${API_BASE}/api/v1/inventory/stock-trend?itemId=${itemId}&days=${days}`),
    enabled: itemId > 0,
  });
}

// ---- Create hooks -------------------------------------------------------

export function useCreateInventoryItem() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (payload: Record<string, unknown>) =>
      apiJson<InventoryItem>(`${API_BASE}/api/v1/items`, { method: "POST", body: JSON.stringify(payload) }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["inventory", "items"] }),
  });
}

export function useCreateInventoryStore() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (payload: Record<string, unknown>) =>
      apiJson<InventoryStore>(`${API_BASE}/api/v1/stores`, { method: "POST", body: JSON.stringify(payload) }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["inventory", "stores"] }),
  });
}

export function useCreateStockMovement() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (payload: Record<string, unknown>) =>
      apiJson<StockBalance>(`${API_BASE}/api/v1/stock/movements`, { method: "POST", body: JSON.stringify(payload) }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["inventory", "balances"] }),
  });
}
