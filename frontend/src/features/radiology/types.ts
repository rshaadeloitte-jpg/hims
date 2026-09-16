/** M16 Radiology domain types — mirror radiology-service DTOs. */

export interface RadOrderLine {
  itemId: number;
  qty: number;
}

export interface RadOrder {
  radOrderId: number;
  id?: number;
  patientId: number;
  patientName?: string;
  encounterId?: number;
  admissionId?: number;
  modality: "XRAY" | "CT" | "MRI" | "US";
  bodyPart?: string;
  priority: 1 | 2;
  status: any;
  imageCaptured: boolean;
  scheduledAt?: string;
  reportText?: string;
  reportedBy?: string;
  addedDatetime?: string;
  details?: RadOrderDetail[];
}

export interface RadOrderDetail {
  radOrderId: number;
  lineNo: number;
  itemId: number;
  qty: number;
  retestRequested: boolean;
  retestReason?: string;
  rescheduledTo?: string;
  addedDatetime?: string;
}

export interface RadTemplate {
  id: number;
  templateCode: string;
  modality: string;
  bodyTemplate: string;
  description?: string;
  addedDatetime?: string;
}

export const MODALITIES = ["XRAY", "CT", "MRI", "US"] as const;
export type Modality = typeof MODALITIES[number];

export const PRIORITIES = ["ROUTINE", "URGENT"] as const;
export type Priority = typeof PRIORITIES[number];

export type RadOrderStatus = 1 | 2 | 3 | 4;

export const RAD_STATUS_MAP: Record<number, { label: string; color: string }> = {
  1: { label: "SCHEDULED", color: "bg-blue-100 text-blue-800" },
  2: { label: "IN PROGRESS", color: "bg-yellow-100 text-yellow-800" },
  3: { label: "COMPLETED", color: "bg-green-100 text-green-800" },
  4: { label: "CANCELLED", color: "bg-red-100 text-red-800" },
};

export function statusLabel(status: number): string {
  return RAD_STATUS_MAP[status]?.label ?? String(status);
}

export function statusColor(status: number): string {
  return RAD_STATUS_MAP[status]?.color ?? "bg-gray-100 text-gray-800";
}

export interface RadStudyItem {
  itemId: number;
  itemCode: string;
  itemName: string;
  modality: Modality;
  department: string;
  tat: number;
  rate: number;
}
