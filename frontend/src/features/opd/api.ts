import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { 
  type AppointmentValues,
  slotSelectionSchema,
} from './validation';

export interface DoctorScheduleSlot {
  slotId: number;
  doctorId: number;
  date: string;
  startTime: string;
  endTime: string;
  available: boolean;
  booked: boolean;
  tokenNo?: number;
}

export function fetchAvailableSlots(params: { doctorId: number; date: string }) {
  return fetch(`/api/opd/slots?${new URLSearchParams({ doctorId: String(params.doctorId), date: params.date })}`)
    .then(res => res.json())
    .then(data => slotSelectionSchema.parse(data));
}

export function useAvailableSlots(doctorId: number, date: string) {
  return useQuery({
    queryKey: ['opd-slots', doctorId, date],
    queryFn: () => fetchAvailableSlots({ doctorId, date }),
    enabled: !!doctorId && !!date,
    staleTime: 5 * 60 * 1000,
  });
}

export function scheduleAppointment(payload: AppointmentValues) {
  return fetch('/api/opd/appointments', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
  }).then(res => res.json());
}

export function useScheduleAppointment() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: scheduleAppointment,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['opd-appointments'] });
    },
  });
}