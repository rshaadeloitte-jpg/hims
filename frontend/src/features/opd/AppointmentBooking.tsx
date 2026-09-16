import { useState } from 'react';
import { DoctorScheduleSlot } from './api';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { Button, SimpleGrid } from '@chakra-ui/react';
import { FormField, TextInput } from '../../components/ui';
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
    if (!id || !selectedSlot) return;

    const payload = {
      ...values,
      parentAppointmentId: id,
      slotStart: selectedSlot.startTime,
      slotEnd: selectedSlot.endTime,
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

      <FormField label="Parent appointment ID">
        <TextInput
          type="number"
          value={parentId}
          onChange={(e) => setParentId(e.target.value)}
        />
      </FormField>

      <SlotSelector
        doctorId={0} // Will be set via parent prop
        selectedDate={selectedDate}
        onDateSelect={setSelectedDate}
        onSelect={setSelectedSlot}
        selectedSlot={selectedSlot}
      />

      <form onSubmit={onSubmit} className="space-y-2">
        <SimpleGrid columns={{ base: 1, sm: 3 }} gap={2}>
          <FormField label="Doctor ID" error={errors.doctorId?.message}>
            <TextInput
              type="number"
              {...register("doctorId")}
            />
          </FormField>
          <FormField label="Department ID" error={errors.departmentId?.message}>
            <TextInput
              type="number"
              {...register("departmentId")}
            />
          </FormField>
          <FormField label="Cabin" error={errors.cabin?.message}>
            <TextInput
              {...register("cabin")}
            />
          </FormField>
        </SimpleGrid>

        {mutation.isError && (
          <p className="text-sm text-red-600">Scheduling failed. Please try again.</p>
        )}

        <Button
          type="submit"
          variant="primary"
          disabled={!parentId || !selectedSlot}
          loading={mutation.isPending}
          loadingText="Scheduling..."
        >
          Schedule Appointment
        </Button>
      </form>
    </div>
  );
}
