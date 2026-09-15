import { useState } from 'react';
import { DoctorScheduleSlot } from './api';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { appointmentSchema, type AppointmentValues } from './validation';
import { useScheduleAppointment } from './api';
import { SlotSelector } from './SlotSelector';

export function AppointmentBooking({
  parentAppointmentId,
  onScheduled,
}: {
  parentAppointmentId?: number;
  onScheduled?: (appointmentId: number) => void;
}) {
  const [selectedSlot, setSelectedSlot] = useState<DoctorScheduleSlot | null>(null);
  const [selectedDate, setSelectedDate] = useState(() => new Date().toISOString().slice(0, 10));
  const [parentId, setParentId] = useState<string>(parentAppointmentId?.toString() ?? '');
  
  const mutation = useScheduleAppointment();
  const {
    register,
    handleSubmit,
    formState: { errors },
    reset,
  } = useForm<AppointmentValues>({
    resolver: zodResolver(appointmentSchema),
    defaultValues: { followUpDate: selectedDate },
  });

  const onSubmit = handleSubmit(async (values) => {
    const id = parentId ? Number(parentId) : 0;
    if (!id) return;
    
    const payload = {
      ...values,
      parentAppointmentId: id,
      slotStart: selectedSlot?.startTime,
      slotEnd: selectedSlot?.endTime,
      date: selectedDate,
    };
    
    try {
      const created = await mutation.mutateAsync(payload);
      onScheduled?.(created.id);
      reset();
      setSelectedSlot(null);
    } catch (error) {
      console.error('Scheduling failed:', error);
    }
  });

  return (
    <div className="p-4 border rounded space-y-4" aria-label="appointment-booking">
      <h2 className="text-xl font-semibold">Appointment Booking</h2>

      <label className="text-sm block">
        <span className="block font-medium">Parent appointment ID</span>
        <input
          type="number"
          className="w-full sm:w-64 border rounded px-2 py-1"
          value={parentId}
          onChange={(e) => setParentId(e.target.value)}
        />
      </label>

      <SlotSelector
        doctorId={0} // Will be set via parent prop
        selectedDate={selectedDate}
        onDateSelect={setSelectedDate}
        onSelect={setSelectedSlot}
        selectedSlot={selectedSlot}
      />

      <form onSubmit={onSubmit} className="space-y-2">
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-2">
          <Field label="Doctor ID" error={errors.doctorId?.message}>
            <input 
              type="number" 
              className="w-full border rounded px-2 py-1" 
              {...register("doctorId")} 
            />
          </Field>
          <Field label="Department ID" error={errors.departmentId?.message}>
            <input 
              type="number" 
              className="w-full border rounded px-2 py-1" 
              {...register("departmentId")} 
            />
          </Field>
          <Field label="Cabin" error={errors.cabin?.message}>
            <input 
              className="w-full border rounded px-2 py-1" 
              {...register("cabin")} 
            />
          </Field>
        </div>

        {mutation.isError && (
          <p className="text-sm text-red-600">Scheduling failed. Please try again.</p>
        )}

        <button
          type="submit"
          className="bg-blue-600 text-white px-4 py-2 rounded disabled:opacity-50 w-full"
          disabled={mutation.isPending || !parentId || !selectedSlot}
        >
          {mutation.isPending ? "Scheduling..." : "Schedule Appointment"}
        </button>
      </form>
    </div>
  );
}

function Field({
  label,
  children,
  error,
}: {
  label: string;
  children: React.ReactNode;
  error?: string;
}) {
  return (
    <label className="text-sm block">
      <span className="block font-medium">{label}</span>
      {children}
      {error && <span className="text-red-600 text-xs">{error}</span>}
    </label>
  );
}