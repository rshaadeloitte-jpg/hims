// M07 Pharmacy types aligned with pharmacy-service / ordering-service DTOs.
export interface Prescription {
  prescriptionId: number;
  patientId: number;
  encounterId?: number | null;
  admissionId?: number | null;
  prescriberId: number;
  wardId?: number | null;
  statusCode: number;
  details?: PrescriptionItem[];
}

export interface PrescriptionItem {
  prescriptionId?: number;
  lineNo: number;
  itemId: number;
  qty: number | string;
  dose: string;
  frequency: string;
}

export interface DispensePayload {
  prescriptionId: number;
  items: {
    itemId: number;
    dispensedQuantity: number;
  }[];
}

export interface ItemSaleDetail {
  saleId: number;
  lineNo: number;
  itemId: number;
  batchId?: string | null;
  qty: number;
  pendingQty: number;
  dispensed: boolean;
  verified?: boolean;
  partialDispense?: boolean;
}

export interface ItemSale {
  saleId: number;
  patientId: number;
  visitRef?: string | null;
  prescriptionId?: number | null;
  statusCode: number;
  dispensed: boolean;
  details?: ItemSaleDetail[];
}

export interface SaleReturnLine {
  returnId?: number;
  lineNo: number;
  itemId: number;
  batchId?: string | null;
  returnQty: number;
  unitPrice: number | string;
  refundAmount?: number | string;
  lineReason?: string | null;
}

export interface SaleReturn {
  returnId: number;
  originalSaleId: number;
  patientId: number;
  returnReason: string;
  statusCode: number;
  totalRefundAmount?: number | string;
  approvedBy?: number | null;
  remarks?: string | null;
  lines?: SaleReturnLine[];
}

export interface Intervention {
  interventionId: number;
  patientId: number;
  saleId?: number | null;
  interventionType: string;
  notes?: string | null;
  pharmacistId: number;
}

export interface DrugAllergy {
  allergyId: number;
  patientId: number;
  allergen: string;
  allergyType?: string | null;
  severity: string;
  reactionDescription?: string | null;
  recordedBy?: number | null;
  recordedAt?: string | null;
}

export interface DrugInteraction {
  interactionId: number;
  patientId: number;
  drug1Id: number;
  drug2Id: number;
  interactionType: string;
  severity: string;
  description?: string | null;
  acknowledged: boolean;
  acknowledgedBy?: number | null;
  acknowledgedAt?: string | null;
  recordedAt?: string | null;
}
