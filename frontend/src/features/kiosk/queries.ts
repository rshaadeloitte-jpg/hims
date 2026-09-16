import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { ApiError } from "../../api/client";

export const KIOSK_BASE = import.meta.env.VITE_KIOSK_API_BASE ?? "/api";

async function kioskJson<T>(url: string, options?: RequestInit): Promise<T> {
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

export interface KioskRegistrationResponse {
  tokenNo: string;
  patientId: number;
}

export interface Department {
  id: number;
  name: string;
}

export interface Doctor {
  id: number;
  name: string;
  departmentId: number;
  consultationFee?: number;
}

export interface AppointmentSlot {
  slotId: number;
  startTime: string;
  endTime: string;
  available: boolean;
}

export interface AppointmentBookingResponse {
  appointmentId: number;
  tokenNo: string;
  status: string;
}

export function useDepartments() {
  return useQuery({
    queryKey: ["kiosk-departments"],
    queryFn: () => kioskJson<Department[]>(`${KIOSK_BASE}/v1/kiosk/departments`),
  });
}

export function useDoctors(departmentId?: number) {
  return useQuery({
    queryKey: ["kiosk-doctors", departmentId],
    queryFn: () =>
      kioskJson<Doctor[]>(`${KIOSK_BASE}/v1/kiosk/doctors${departmentId ? `?departmentId=${departmentId}` : ""}`),
    enabled: !!departmentId,
  });
}

export function useSlots(doctorId?: number, date?: string) {
  return useQuery({
    queryKey: ["kiosk-slots", doctorId, date],
    queryFn: () =>
      kioskJson<AppointmentSlot[]>(
        `${KIOSK_BASE}/v1/kiosk/slots?doctorId=${doctorId ?? ""}&date=${date ?? ""}`
      ),
    enabled: !!doctorId && !!date,
  });
}

export function useSelfRegister() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (input: {
      mobileNo: string;
      firstName: string;
      lastName: string;
      gender?: string;
      dob?: string;
    }) => kioskJson<KioskRegistrationResponse>(`${KIOSK_BASE}/v1/kiosk/self-register`, {
      method: "POST",
      body: JSON.stringify(input),
    }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["kiosk-registration"] });
    },
  });
}

export function useBookAppointment() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (input: {
      patientName: string;
      mobileNo: string;
      departmentId: number;
      doctorId: number;
      date: string;
      slotId: number;
      reason?: string;
    }) =>
      kioskJson<AppointmentBookingResponse>(`${KIOSK_BASE}/v1/kiosk/appointments`, {
        method: "POST",
        body: JSON.stringify(input),
      }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["kiosk-appointments"] });
      qc.invalidateQueries({ queryKey: ["kiosk-slots"] });
    },
  });
}

export function useUploadLabResult() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (input: {
      tokenNo: string;
      visitId: string;
      testName: string;
      resultValue: string;
      resultDate: string;
      file?: File;
    }) => {
      const formData = new FormData();
      formData.append("tokenNo", input.tokenNo);
      formData.append("visitId", input.visitId);
      formData.append("testName", input.testName);
      formData.append("resultValue", input.resultValue);
      formData.append("resultDate", input.resultDate);
      if (input.file) formData.append("file", input.file);

      const res = await fetch(`${KIOSK_BASE}/v1/kiosk/lab-results`, {
        method: "POST",
        body: formData,
      });
      if (!res.ok) {
        const err = await res.json().catch(() => ({ message: res.statusText }));
        throw new ApiError(res.status, err.errorCode ?? "UNKNOWN", err.message ?? "Upload failed");
      }
      return res.json();
    },
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["kiosk-lab-results"] });
    },
  });
}
