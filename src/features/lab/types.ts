/** M15 Lab domain types */

export interface LabTest {
  testId: number;
  testCode: string;
  testName: string;
  department: string;
  sampleType: string;
  containerType: string;
  turnaroundTime: number; // hours
  rate: number;
  isOutsourced: boolean;
  referenceRange?: string;
  criticalLow?: number;
  criticalHigh?: number;
  normalLow?: number;
  normalHigh?: number;
  unit?: string;
}

export interface LabOrder {
  orderId: number;
  patientId: number;
  patientName?: string;
  encounterId?: number;
  isOutSourced: boolean;
  status: LabOrderStatus;
  orderDate: string;
  details?: LabDetail[];
}

export type LabOrderStatus = 
  | 'ORDERED' 
  | 'COLLECTED' 
  | 'IN_PROGRESS' 
  | 'COMPLETED' 
  | 'CANCELLED'
  | 'VERIFIED';

export interface LabDetail {
  detailId: number;
  orderId: number;
  testId: number;
  testName: string;
  testCode: string;
  status: LabDetailStatus;
  barcode?: string;
  sampleCollectedAt?: string;
  resultValue?: string;
  resultUnit?: string;
  referenceRange?: string;
  isCritical?: boolean;
  isVerified?: boolean;
  verifiedBy?: number;
  verifiedAt?: string;
  remarks?: string;
}

export type LabDetailStatus = 
  | 'PENDING' 
  | 'COLLECTED' 
  | 'IN_PROGRESS' 
  | 'COMPLETED' 
  | 'VERIFIED'
  | 'REJECTED';

export interface Sample {
  sampleId: number;
  orderId: number;
  detailId: number;
  barcode: string;
  patientId: number;
  testName: string;
  sampleType: string;
  collectedAt: string;
  collectedBy: number;
  status: 'PENDING' | 'ACCEPTED' | 'REJECTED' | 'IN_PROGRESS';
  rejectionReason?: string;
}

export interface ResultEntry {
  detailId: number;
  value: string;
  unit?: string;
  referenceRange?: string;
  isAbnormal?: boolean;
  isCritical?: boolean;
  remarks?: string;
}

export interface VerificationStatus {
  detailId: number;
  isVerified: boolean;
  verifiedBy?: number;
  verifiedAt?: string;
  verifierName?: string;
}

export interface Machine {
  machineId: number;
  name: string;
  code: string;
  interfaceType: string;
  status: 'ACTIVE' | 'INACTIVE' | 'MAINTENANCE';
  currentQueue?: number;
}

export interface WorkloadItem {
  machineId: number;
  machineName: string;
  pendingCount: number;
  inProgressCount: number;
  completedToday: number;
}

export interface CriticalAlert {
  detailId: number;
  orderId: number;
  patientId: number;
  patientName?: string;
  testName: string;
  resultValue: string;
  referenceRange: string;
  criticalLevel: 'HIGH' | 'LOW';
  notifiedAt?: string;
  notifiedBy?: number;
}

export interface LabReport {
  orderId: number;
  patientId: number;
  patientName: string;
  patientAge?: number;
  patientGender?: string;
  orderDate: string;
  collectedAt?: string;
  verifiedAt?: string;
  details: LabDetail[];
  reportGeneratedAt: string;
  generatedBy: number;
}