// M07 Pharmacy prescription dispensing types
export interface Prescription {
  id: number;
  patientId: number;
  patientName: string;
  encounterId: number;
  status: "PENDING" | "DISPENSED" | "CANCELLED";
  createdAt: string;
  items: PrescriptionItem[];
}

export interface PrescriptionItem {
  id: number;
  drugId: number;
  drugName: string;
  strength: string;
  dosage: string;
  quantity: number;
  dispensedQuantity?: number;
  instructions: string;
}

export interface DispensePayload {
  prescriptionId: number;
  items: {
    itemId: number;
    dispensedQuantity: number;
  }[];
}

export interface ItemSale {
  saleId: number;
  patientId: number;
  totalAmount: number;
  isDispensed: boolean;
  isReturned: boolean;
  createdAt: string;
}

export interface SaleReturn {
  returnId: number;
  saleId: number;
  status: number;
  createdAt: string;
}

export interface Intervention {
  id: number;
  type: string;
  createdAt: string;
}