import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { ApiError } from "../../api/client";

const PROC_BASE: string = import.meta.env.VITE_PROCUREMENT_API_BASE ?? "/api";

async function procJson<T>(url: string, options?: RequestInit): Promise<T> {
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

/* ── Types ──────────────────────────────────────────────────────── */

export interface Supplier {
  id: number;
  code: string;
  description: string;
  addressLine1?: string;
  addressLine2?: string;
  city?: string;
  state?: string;
  country?: string;
  pincode?: string;
  contactPerson1Name?: string;
  contactPerson1Mobile?: string;
  contactPerson1Email?: string;
  phoneNo?: string;
  fax?: string;
  drugLicenceNumber?: string;
  panNumber?: string;
  isManufacturer?: boolean;
  isInhouseSupplier?: boolean;
  isEmpanelled?: boolean;
}

export interface Store {
  id: number;
  code: string;
  description: string;
  isMainStore?: boolean;
  isCentralStore?: boolean;
  isPharmacyStore?: boolean;
  isCssdStore?: boolean;
  isImplantStore?: boolean;
  isAssetStore?: boolean;
  isSubStore?: boolean;
  isPatientRequisition?: boolean;
  itemGroupId?: number;
  wingId?: number;
  floorId?: number;
  prefix?: string;
  sequenceName?: string;
  gstNo?: string;
  pharmacyLicenseNo?: string;
  phoneNo?: string;
  email?: string;
}

export interface Item {
  id: number;
  itemCode?: string;
  itemName: string;
  itemNameHindi?: string;
  brandName?: string;
  strength?: string;
  purchaseRate?: number;
  mrp?: number;
}

export interface PurchaseOrder {
  id: number;
  poNo: string;
  storeId: number;
  supplierId: number;
  quotationId?: number;
  orderDate?: string;
  expectedDate?: string;
  remarks?: string;
  docStatus: number;
  approvedBy?: number;
  approvedDatetime?: string;
  issuedAt?: string;
  closedAt?: string;
  supplierName?: string;
  totalAmount?: number;
  status?: string;
  createdAt?: string;
}

export interface PurchaseOrderDetail {
  id: number;
  poId: number;
  itemId: number;
  orderedQty: number;
  receivedQty: number;
  unitPrice: number;
  taxPercent: number;
  leadTimeDays?: number;
  remarks?: string;
}

export interface PurchaseOrderInput {
  storeId: number;
  supplierId: number;
  quotationId?: number;
  orderDate?: string;
  expectedDate?: string;
  remarks?: string;
  lines: { itemId: number; orderedQty: number; unitPrice: number; taxPercent?: number; leadTimeDays?: number; remarks?: string }[];
}

export interface Grn {
  id: number;
  grnNo: string;
  storeId: number;
  supplierId?: number;
  invoiceNo?: string;
  invoiceDate?: string;
  receivedDate?: string;
  remarks?: string;
  docStatus: number;
  approvedBy?: number;
  approvedDatetime?: string;
  provisional: boolean;
  consignment: boolean;
}

export interface GrnDetail {
  id: number;
  grnId: number;
  itemId: number;
  batchId?: number;
  quantity: number;
  purchaseRate?: number;
  mrp?: number;
  remarks?: string;
}

export interface GrnInput {
  storeId: number;
  supplierId?: number;
  grnNo?: string;
  invoiceNo?: string;
  invoiceDate?: string;
  receivedDate?: string;
  remarks?: string;
  provisional?: boolean;
  consignment?: boolean;
  lines: { itemId: number; quantity: number; batchId?: number; batchCode?: string; expiryDate?: string; mrp?: number; purchaseRate?: number; remarks?: string }[];
}

export interface Quotation {
  id: number;
  quotationNo: string;
  supplierId: number;
  enquiryRef?: string;
  validUntil?: string;
  submittedAt?: string;
  awardedAt?: string;
  awardedPoId?: number;
  remarks?: string;
  docStatus: number;
}

export interface QuotationDetail {
  id: number;
  quotationId: number;
  itemId: number;
  quantity: number;
  unitPrice: number;
  taxPercent: number;
  leadTimeDays?: number;
  remarks?: string;
}

export interface QuotationInput {
  supplierId: number;
  enquiryRef?: string;
  validUntil?: string;
  remarks?: string;
  lines: { itemId: number; quantity: number; unitPrice: number; taxPercent?: number; leadTimeDays?: number; remarks?: string }[];
}

export interface QuotationCompareRow {
  quotationId: number;
  quotationNo: string;
  supplierId: number;
  supplierCode?: string;
  supplierName?: string;
  itemId: number;
  quantity: number;
  unitPrice: number;
  taxPercent: number;
  leadTimeDays?: number;
}

/* ── Query keys ─────────────────────────────────────────────────── */

export const procurementQueryKeys = {
  suppliers: ["procurement", "suppliers"] as const,
  stores: ["procurement", "stores"] as const,
  items: ["procurement", "items"] as const,
  purchaseOrders: ["procurement", "purchase-orders"] as const,
  purchaseOrder: (id: number) => ["procurement", "purchase-order", id] as const,
  purchaseOrderLines: (id: number) => ["procurement", "purchase-order", id, "lines"] as const,
  grns: ["procurement", "grns"] as const,
  grn: (id: number) => ["procurement", "grn", id] as const,
  grnLines: (id: number) => ["procurement", "grn", id, "lines"] as const,
  quotations: ["procurement", "quotations"] as const,
  quotation: (id: number) => ["procurement", "quotation", id] as const,
  quotationLines: (id: number) => ["procurement", "quotation", id, "lines"] as const,
  quotationCompare: (itemId: number) => ["procurement", "quotation", "compare", itemId] as const,
};

/* ── Supplier hooks ─────────────────────────────────────────────── */

export function useSuppliers() {
  return useQuery({
    queryKey: procurementQueryKeys.suppliers,
    queryFn: () => procJson<Supplier[]>(`${PROC_BASE}/v1/suppliers`),
    staleTime: 5 * 60_000,
  });
}

/* ── Store hooks ────────────────────────────────────────────────── */

export function useStores() {
  return useQuery({
    queryKey: procurementQueryKeys.stores,
    queryFn: () => procJson<Store[]>(`${PROC_BASE}/v1/stores`),
    staleTime: 5 * 60_000,
  });
}

/* ── Item hooks ─────────────────────────────────────────────────── */

export function useItems() {
  return useQuery({
    queryKey: procurementQueryKeys.items,
    queryFn: () => procJson<Item[]>(`${PROC_BASE}/v1/items`),
    staleTime: 5 * 60_000,
  });
}

/* ── Purchase Order hooks ───────────────────────────────────────── */

export function usePurchaseOrders(storeId?: number, supplierId?: number, status?: number) {
  return useQuery({
    queryKey: [...procurementQueryKeys.purchaseOrders, { storeId, supplierId, status }],
    queryFn: () => procJson<PurchaseOrder[]>(`${PROC_BASE}/v1/inventory/purchase-orders`),
  });
}

export function usePurchaseOrder(id: number) {
  return useQuery({
    queryKey: procurementQueryKeys.purchaseOrder(id),
    queryFn: () => procJson<PurchaseOrder>(`${PROC_BASE}/v1/inventory/purchase-orders/${id}`),
    enabled: id > 0,
  });
}

export function usePurchaseOrderLines(id: number) {
  return useQuery({
    queryKey: procurementQueryKeys.purchaseOrderLines(id),
    queryFn: () => procJson<PurchaseOrderDetail[]>(`${PROC_BASE}/v1/inventory/purchase-orders/${id}/lines`),
    enabled: id > 0,
  });
}

export function useCreatePurchaseOrder() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (input: PurchaseOrderInput) =>
      procJson<PurchaseOrder>(`${PROC_BASE}/v1/inventory/purchase-orders`, { method: "POST", body: JSON.stringify(input) }),
    onSuccess: () => qc.invalidateQueries({ queryKey: procurementQueryKeys.purchaseOrders }),
  });
}

export function useSubmitPurchaseOrder() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: number) =>
      procJson<PurchaseOrder>(`${PROC_BASE}/v1/inventory/purchase-orders/${id}/submit`, { method: "POST" }),
    onSuccess: () => qc.invalidateQueries({ queryKey: procurementQueryKeys.purchaseOrders }),
  });
}

export function useApprovePurchaseOrder() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: number) =>
      procJson<PurchaseOrder>(`${PROC_BASE}/v1/inventory/purchase-orders/${id}/approve`, { method: "POST" }),
    onSuccess: () => qc.invalidateQueries({ queryKey: procurementQueryKeys.purchaseOrders }),
  });
}

export function useRejectPurchaseOrder() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: number) =>
      procJson<PurchaseOrder>(`${PROC_BASE}/v1/inventory/purchase-orders/${id}/reject`, { method: "POST" }),
    onSuccess: () => qc.invalidateQueries({ queryKey: procurementQueryKeys.purchaseOrders }),
  });
}

export function useIssuePurchaseOrder() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: number) =>
      procJson<PurchaseOrder>(`${PROC_BASE}/v1/inventory/purchase-orders/${id}/issue`, { method: "POST" }),
    onSuccess: () => qc.invalidateQueries({ queryKey: procurementQueryKeys.purchaseOrders }),
  });
}

export function useCancelPurchaseOrder() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: number) =>
      procJson<PurchaseOrder>(`${PROC_BASE}/v1/inventory/purchase-orders/${id}/cancel`, { method: "POST" }),
    onSuccess: () => qc.invalidateQueries({ queryKey: procurementQueryKeys.purchaseOrders }),
  });
}

/* ── GRN hooks ──────────────────────────────────────────────────── */

export function useGrns() {
  return useQuery({
    queryKey: procurementQueryKeys.grns,
    queryFn: () => procJson<Grn[]>(`${PROC_BASE}/v1/grn`),
  });
}

export function useGrn(id: number) {
  return useQuery({
    queryKey: procurementQueryKeys.grn(id),
    queryFn: () => procJson<Grn>(`${PROC_BASE}/v1/grn/${id}`),
    enabled: id > 0,
  });
}

export function useGrnLines(id: number) {
  return useQuery({
    queryKey: procurementQueryKeys.grnLines(id),
    queryFn: () => procJson<GrnDetail[]>(`${PROC_BASE}/v1/grn/${id}/lines`),
    enabled: id > 0,
  });
}

export function usePostGrn() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (input: GrnInput) =>
      procJson<Grn>(`${PROC_BASE}/v1/grn`, { method: "POST", body: JSON.stringify(input) }),
    onSuccess: () => qc.invalidateQueries({ queryKey: procurementQueryKeys.grns }),
  });
}

/* ── Quotation hooks ────────────────────────────────────────────── */

export function useQuotations(supplierId?: number, status?: number) {
  return useQuery({
    queryKey: [...procurementQueryKeys.quotations, { supplierId, status }],
    queryFn: () => procJson<Quotation[]>(`${PROC_BASE}/v1/inventory/quotations`),
  });
}

export function useQuotation(id: number) {
  return useQuery({
    queryKey: procurementQueryKeys.quotation(id),
    queryFn: () => procJson<Quotation>(`${PROC_BASE}/v1/inventory/quotations/${id}`),
    enabled: id > 0,
  });
}

export function useQuotationLines(id: number) {
  return useQuery({
    queryKey: procurementQueryKeys.quotationLines(id),
    queryFn: () => procJson<QuotationDetail[]>(`${PROC_BASE}/v1/inventory/quotations/${id}/lines`),
    enabled: id > 0,
  });
}

export function useCreateQuotation() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (input: QuotationInput) =>
      procJson<Quotation>(`${PROC_BASE}/v1/inventory/quotations`, { method: "POST", body: JSON.stringify(input) }),
    onSuccess: () => qc.invalidateQueries({ queryKey: procurementQueryKeys.quotations }),
  });
}

export function useSubmitQuotation() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: number) =>
      procJson<Quotation>(`${PROC_BASE}/v1/inventory/quotations/${id}/submit`, { method: "POST" }),
    onSuccess: () => qc.invalidateQueries({ queryKey: procurementQueryKeys.quotations }),
  });
}

export function useRejectQuotation() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: number) =>
      procJson<Quotation>(`${PROC_BASE}/v1/inventory/quotations/${id}/reject`, { method: "POST" }),
    onSuccess: () => qc.invalidateQueries({ queryKey: procurementQueryKeys.quotations }),
  });
}

export function useExpireQuotation() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: number) =>
      procJson<Quotation>(`${PROC_BASE}/v1/inventory/quotations/${id}/expire`, { method: "POST" }),
    onSuccess: () => qc.invalidateQueries({ queryKey: procurementQueryKeys.quotations }),
  });
}

export function useQuotationCompare(itemId: number) {
  return useQuery({
    queryKey: procurementQueryKeys.quotationCompare(itemId),
    queryFn: () => procJson<QuotationCompareRow[]>(`${PROC_BASE}/v1/inventory/quotations/compare?itemId=${itemId}`),
    enabled: itemId > 0,
  });
}
