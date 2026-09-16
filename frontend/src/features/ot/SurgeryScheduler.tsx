import { useState, useMemo, useCallback } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { Button, Checkbox, HStack, SimpleGrid } from "@chakra-ui/react";
import { FormField, TextInput } from "../../components/ui";
import {
  useOtSchedules,
  useCreateOtSchedule,
  useStartSurgery,
  useCompleteSurgery,
  useCancelSurgery,
  type OtSchedule,
} from "./queries";
import { otScheduleSchema, type OtScheduleForm } from "./zodSchema";

const DAY_LABELS = ["Su", "Mo", "Tu", "We", "Th", "Fr", "Sa"];
const MONTH_NAMES = [
  "January", "February", "March", "April", "May", "June",
  "July", "August", "September", "October", "November", "December",
];

const STATUS_LABELS: Record<number, string> = {
  1: "PLANNED",
  2: "IN_PROGRESS",
  3: "DONE",
  4: "CANCELLED",
};

const STATUS_COLORS: Record<number, string> = {
  1: "bg-blue-100 text-blue-800 border-blue-200",
  2: "bg-yellow-100 text-yellow-800 border-yellow-200",
  3: "bg-green-100 text-green-800 border-green-200",
  4: "bg-red-100 text-red-800 border-red-200",
};

function getStatusLabel(status?: number): string {
  return status != null ? (STATUS_LABELS[status] ?? "UNKNOWN") : "PLANNED";
}

function getStatusColor(status?: number): string {
  return status != null ? (STATUS_COLORS[status] ?? "bg-gray-100 text-gray-800") : "bg-gray-100 text-gray-800";
}

interface CalendarGridProps {
  selectedDate: string;
  onSelectDate: (iso: string) => void;
  schedules: OtSchedule[];
}

function CalendarGrid({ selectedDate, onSelectDate, schedules }: CalendarGridProps) {
  const [viewYear, setViewYear] = useState(() => new Date(selectedDate + "T00:00:00").getFullYear());
  const [viewMonth, setViewMonth] = useState(() => new Date(selectedDate + "T00:00:00").getMonth());

  const today = new Date();
  today.setHours(0, 0, 0, 0);
  const daysInMonth = new Date(viewYear, viewMonth + 1, 0).getDate();
  const firstDay = new Date(viewYear, viewMonth, 1).getDay();

  const scheduleDates = useMemo(() => {
    const map = new Map<string, number>();
    for (const s of schedules) {
      const d = s.scheduledStart ? s.scheduledStart.slice(0, 10) : null;
      if (d) map.set(d, (map.get(d) ?? 0) + 1);
    }
    return map;
  }, [schedules]);

  const cells = useMemo(() => {
    const arr: (number | null)[] = [];
    for (let i = 0; i < firstDay; i++) arr.push(null);
    for (let d = 1; d <= daysInMonth; d++) arr.push(d);
    return arr;
  }, [firstDay, daysInMonth]);

  const prevMonth = () => {
    if (viewMonth === 0) { setViewYear((y) => y - 1); setViewMonth(11); }
    else setViewMonth((m) => m - 1);
  };
  const nextMonth = () => {
    if (viewMonth === 11) { setViewYear((y) => y + 1); setViewMonth(0); }
    else setViewMonth((m) => m + 1);
  };

  return (
    <div className="border border-gray-200 rounded-lg p-4 bg-white" role="grid" aria-label="surgery-calendar">
      <div className="flex items-center justify-between mb-4">
        <Button type="button" variant="ghost" size="sm" onClick={prevMonth} aria-label="Previous month">
          <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 19l-7-7 7-7" /></svg>
        </Button>
        <h3 className="text-sm font-semibold text-gray-900">{MONTH_NAMES[viewMonth]} {viewYear}</h3>
        <Button type="button" variant="ghost" size="sm" onClick={nextMonth} aria-label="Next month">
          <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5l7 7-7 7" /></svg>
        </Button>
      </div>
      <div className="grid grid-cols-7 gap-1 mb-2">
        {DAY_LABELS.map((d) => (
          <div key={d} className="text-center text-xs font-medium text-gray-500 py-1">{d}</div>
        ))}
      </div>
      <div className="grid grid-cols-7 gap-1">
        {cells.map((day, idx) => {
          if (day == null) return <div key={`empty-${idx}`} className="h-10" />;
          const iso = `${viewYear}-${String(viewMonth + 1).padStart(2, "0")}-${String(day).padStart(2, "0")}`;
          const isSelected = iso === selectedDate;
          const isToday = new Date(iso + "T00:00:00").getTime() === today.getTime();
          const count = scheduleDates.get(iso) ?? 0;
          return (
            <Button
              key={iso}
              type="button"
              variant={isSelected ? "primary" : "ghost"}
              size="sm"
              bg={isSelected ? undefined : isToday ? "blue.50" : undefined}
              color={isSelected ? undefined : isToday ? "blue.700" : undefined}
              onClick={() => onSelectDate(iso)}
              aria-label={`${day} ${MONTH_NAMES[viewMonth]}`}
              aria-selected={isSelected}
            >
              <span>{day}</span>
              {count > 0 && (
                <span className={`absolute bottom-0.5 w-1.5 h-1.5 rounded-full ${isSelected ? "bg-white" : "bg-blue-500"}`} />
              )}
            </Button>
          );
        })}
      </div>
    </div>
  );
}

function SlotGrid({
  selectedDate,
  schedules,
  selectedSlot,
  onSelectSlot,
}: {
  selectedDate: string;
  schedules: OtSchedule[];
  selectedSlot: OtSchedule | null;
  onSelectSlot: (s: OtSchedule) => void;
}) {
  const daySchedules = useMemo(
    () => schedules.filter((s) => (s.scheduledStart ?? "").slice(0, 10) === selectedDate),
    [schedules, selectedDate]
  );

  if (daySchedules.length === 0) {
    return (
      <div className="text-center py-8 text-gray-500 text-sm">
        No surgeries scheduled for {selectedDate}. Select a date with a dot indicator, or create a new schedule below.
      </div>
    );
  }

  return (
    <div className="space-y-2">
      {daySchedules.map((s) => {
        const id = s.scheduleId ?? s.id ?? 0;
        const isSelected = (selectedSlot?.scheduleId ?? selectedSlot?.id) === id;
        const start = s.scheduledStart ? new Date(s.scheduledStart).toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" }) : "—";
        const end = s.scheduledEnd ? new Date(s.scheduledEnd).toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" }) : "—";
        return (
          <Button
            key={id}
            type="button"
            variant={isSelected ? "secondary" : "ghost"}
            onClick={() => onSelectSlot(s)}
            aria-pressed={isSelected}
          >
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-3">
                <div className="text-sm font-medium text-gray-900">{start} – {end}</div>
                <span className={`text-xs px-2 py-0.5 rounded-full border ${getStatusColor(s.status)}`}>
                  {getStatusLabel(s.status)}
                </span>
              </div>
              <div className="text-xs text-gray-500">Table {s.otTable}</div>
            </div>
            <div className="mt-1 text-sm text-gray-600">
              Patient #{s.patientId}{s.patientName ? ` — ${s.patientName}` : ""}
            </div>
            {s.details && s.details.length > 0 && (
              <div className="mt-1 text-xs text-gray-500">
                {s.details.map((d) => d.procedureName).join(", ")}
              </div>
            )}
          </Button>
        );
      })}
    </div>
  );
}

export function SurgeryScheduler() {
  const [selectedDate, setSelectedDate] = useState(() => new Date().toISOString().slice(0, 10));
  const [selectedSlot, setSelectedSlot] = useState<OtSchedule | null>(null);
  const [showForm, setShowForm] = useState(false);

  const { data: schedules = [], isLoading } = useOtSchedules(selectedDate);
  const createMutation = useCreateOtSchedule();
  const startMutation = useStartSurgery();
  const completeMutation = useCompleteSurgery();
  const cancelMutation = useCancelSurgery();

  const {
    register,
    handleSubmit,
    formState: { errors },
    reset,
    setValue,
    watch,
  } = useForm<OtScheduleForm>({
    resolver: zodResolver(otScheduleSchema),
    defaultValues: {
      patientId: undefined,
      otTable: "",
      scheduledStart: "",
      scheduledEnd: "",
      preBilling: false,
      procedures: [],
    },
  });

  const preBilling = watch("preBilling");

  const onSubmit = handleSubmit(async (values) => {
    try {
      await createMutation.mutateAsync({
        ...values,
        scheduledStart: values.scheduledStart,
        scheduledEnd: values.scheduledEnd,
      });
      setShowForm(false);
      reset();
    } catch (error) {
      console.error("Scheduling failed:", error);
    }
  });

  const handleSelectSlot = useCallback((s: OtSchedule) => {
    setSelectedSlot(s);
  }, []);

  const handleDateChange = useCallback((iso: string) => {
    setSelectedDate(iso);
    setSelectedSlot(null);
    setValue("scheduledStart", iso + "T08:00");
    setValue("scheduledEnd", iso + "T10:00");
  }, [setValue]);

  return (
    <section className="space-y-6" aria-label="surgery-scheduler">
      <header className="flex items-center justify-between">
        <div>
          <h2 className="text-xl font-semibold text-gray-900">Surgery Scheduler</h2>
          <p className="text-sm text-gray-500">Linear calendar view — book OT slots by date</p>
        </div>
        <Button
          type="button"
          variant="primary"
          onClick={() => setShowForm(!showForm)}
        >
          {showForm ? "Cancel" : "+ New Surgery"}
        </Button>
      </header>

      {showForm && (
        <div className="border border-gray-200 rounded-lg p-6 bg-white shadow-sm">
          <h3 className="text-lg font-medium text-gray-900 mb-4">Schedule New Surgery</h3>
          <form onSubmit={onSubmit} className="space-y-4">
            <SimpleGrid columns={{ base: 1, md: 2 }} gap={4}>
              <FormField label="Patient ID *" error={errors.patientId?.message} required>
                <TextInput
                  type="number"
                  {...register("patientId", { valueAsNumber: true })}
                />
              </FormField>
              <FormField label="OT Table *" error={errors.otTable?.message} required>
                <TextInput
                  type="text"
                  placeholder="e.g. OT-1"
                  {...register("otTable")}
                />
              </FormField>
              <FormField label="Start Time *" error={errors.scheduledStart?.message} required>
                <TextInput
                  type="datetime-local"
                  {...register("scheduledStart")}
                />
              </FormField>
              <FormField label="End Time *" error={errors.scheduledEnd?.message} required>
                <TextInput
                  type="datetime-local"
                  {...register("scheduledEnd")}
                />
              </FormField>
              <FormField label="Surgeon ID">
                <TextInput
                  type="number"
                  {...register("surgeonId", { valueAsNumber: true })}
                />
              </FormField>
              <FormField label="Anesthetist ID">
                <TextInput
                  type="number"
                  {...register("anesthetistId", { valueAsNumber: true })}
                />
              </FormField>
              <FormField label="Encounter ID" error={errors.encounterId?.message}>
                <TextInput
                  type="number"
                  {...register("encounterId", { valueAsNumber: true })}
                />
              </FormField>
              <FormField label="Admission ID" error={errors.admissionId?.message}>
                <TextInput
                  type="number"
                  {...register("admissionId", { valueAsNumber: true })}
                />
              </FormField>
            </SimpleGrid>
            <Checkbox.Root checked={!!preBilling} onCheckedChange={(e) => setValue("preBilling", !!e.checked, { shouldValidate: true })}>
              <Checkbox.HiddenInput {...register("preBilling")} />
              <Checkbox.Control />
              <Checkbox.Label>Pre-billing (BR-OT-002)</Checkbox.Label>
            </Checkbox.Root>
            {errors.root && <p className="text-xs text-red-600">{errors.root.message}</p>}
            <HStack gap={3} pt={2}>
              <Button
                type="submit"
                variant="primary"
                loading={createMutation.isPending}
                loadingText="Scheduling..."
              >
                Schedule Surgery
              </Button>
              <Button
                type="button"
                variant="secondary"
                onClick={() => { setShowForm(false); reset(); }}
              >
                Cancel
              </Button>
            </HStack>
          </form>
        </div>
      )}

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <div className="lg:col-span-1">
          <CalendarGrid selectedDate={selectedDate} onSelectDate={handleDateChange} schedules={schedules} />
        </div>
        <div className="lg:col-span-2">
          <div className="border border-gray-200 rounded-lg p-4 bg-white">
            <div className="flex items-center justify-between mb-4">
              <h3 className="text-sm font-semibold text-gray-900">
                Slots for <span className="text-blue-600">{selectedDate}</span>
              </h3>
              <span className="text-xs text-gray-500">{schedules.filter((s) => (s.scheduledStart ?? "").slice(0, 10) === selectedDate).length} surgery(s)</span>
            </div>
            {isLoading ? (
              <div className="text-center py-8 text-gray-500 text-sm">Loading schedules...</div>
            ) : (
              <SlotGrid
                selectedDate={selectedDate}
                schedules={schedules}
                selectedSlot={selectedSlot}
                onSelectSlot={handleSelectSlot}
              />
            )}
            {selectedSlot && (
              <div className="mt-4 pt-4 border-t border-gray-200">
                <h4 className="text-sm font-medium text-gray-900 mb-2">Selected Slot Actions</h4>
                <div className="flex flex-wrap gap-2">
                  {selectedSlot.status === 1 && (
                    <Button
                      type="button"
                      variant="primary"
                      onClick={() => startMutation.mutate(selectedSlot.scheduleId ?? selectedSlot.id ?? 0)}
                      loading={startMutation.isPending}
                      loadingText="Starting..."
                    >
                      Start Surgery
                    </Button>
                  )}
                  {selectedSlot.status === 2 && (
                    <Button
                      type="button"
                      variant="primary"
                      onClick={() => completeMutation.mutate(selectedSlot.scheduleId ?? selectedSlot.id ?? 0)}
                      loading={completeMutation.isPending}
                      loadingText="Completing..."
                    >
                      Complete
                    </Button>
                  )}
                  {(selectedSlot.status === 1 || selectedSlot.status === 2) && (
                    <Button
                      type="button"
                      variant="secondary"
                      onClick={() => cancelMutation.mutate(selectedSlot.scheduleId ?? selectedSlot.id ?? 0)}
                      loading={cancelMutation.isPending}
                      loadingText="Cancelling..."
                    >
                      Cancel
                    </Button>
                  )}
                </div>
              </div>
            )}
          </div>
        </div>
      </div>
    </section>
  );
}
