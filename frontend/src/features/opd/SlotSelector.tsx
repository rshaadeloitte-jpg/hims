import { useState, useMemo } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { slotSelectionSchema, type SlotSelectionValues } from './validation';
import { useAvailableSlots } from './api';

interface DoctorScheduleSlot {
  slotId: number;
  doctorId: number;
  date: string;
  startTime: string;
  endTime: string;
  available: boolean;
  booked: boolean;
  tokenNo?: number;
}

interface SlotSelectorProps {
  doctorId: number;
  selectedDate: string;
  onDateSelect: (date: string) => void;
  onSelect: (slot: DoctorScheduleSlot) => void;
  selectedSlot?: DoctorScheduleSlot | null;
}

const DAY_LABELS = ['Su', 'Mo', 'Tu', 'We', 'Th', 'Fr', 'Sa'];
const MONTH_NAMES = [
  'January', 'February', 'March', 'April', 'May', 'June',
  'July', 'August', 'September', 'October', 'November', 'December',
];

function CalendarGrid({
  selectedDate,
  onSelectDate,
}: {
  selectedDate: string;
  onSelectDate: (iso: string) => void;
}) {
  const [viewYear, setViewYear] = useState(() => new Date(selectedDate).getFullYear());
  const [viewMonth, setViewMonth] = useState(() => new Date(selectedDate).getMonth());
  
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  const daysInMonth = new Date(viewYear, viewMonth + 1, 0).getDate();
  const firstDay = new Date(viewYear, viewMonth, 1).getDay();
  const selectedIso = selectedDate;

  const cells = useMemo(() => {
    const arr: (number | null)[] = [];
    for (let i = 0; i < firstDay; i++) arr.push(null);
    for (let d = 1; d <= daysInMonth; d++) arr.push(d);
    while (arr.length % 7 !== 0) arr.push(null);
    return arr;
  }, [viewYear, viewMonth, daysInMonth, firstDay]);

  const prevMonth = () => {
    if (viewMonth === 0) { setViewMonth(11); setViewYear(y => y - 1); }
    else setViewMonth(m => m - 1);
  };
  
  const nextMonth = () => {
    if (viewMonth === 11) { setViewMonth(0); setViewYear(y => y + 1); }
    else setViewMonth(m => m + 1);
  };

  return (
    <div className="border rounded-lg p-3 bg-white" role="grid" aria-label="appointment-calendar">
      <div className="flex items-center justify-between mb-3">
        <button type="button" onClick={prevMonth} className="p-1 hover:bg-gray-100 rounded" aria-label="Previous month">‹</button>
        <span className="text-sm font-medium select-none">
          {MONTH_NAMES[viewMonth]} {viewYear}
        </span>
        <button type="button" onClick={nextMonth} className="p-1 hover:bg-gray-100 rounded" aria-label="Next month">›</button>
      </div>
      <div className="grid grid-cols-7 gap-px">
        {DAY_LABELS.map((d) => (
          <div key={d} className="text-center text-xs text-gray-400 py-1 font-medium">{d}</div>
        ))}
        {cells.map((day, i) => {
          if (day === null) return <div key={`e-${i}`} />;
          const cellDate = new Date(viewYear, viewMonth, day);
          cellDate.setHours(0, 0, 0, 0);
          const iso = cellDate.toISOString().slice(0, 10);
          const isToday = cellDate.getTime() === today.getTime();
          const isSelected = iso === selectedIso;
          const isPast = cellDate < today;

          return (
            <button
              key={iso}
              type="button"
              disabled={isPast}
              onClick={() => onSelectDate(iso)}
              className={[
                "text-xs py-1.5 rounded-md transition-colors relative",
                isSelected ? "bg-blue-600 text-white font-semibold" : "hover:bg-blue-50",
                isToday && !isSelected ? "ring-1 ring-blue-600" : "",
                isPast ? "text-gray-300 cursor-not-allowed" : "text-gray-800",
              ].join(" ")}
            >
              {day}
            </button>
          );
        })}
      </div>
    </div>
  );
}

function SlotGrid({
  slots,
  selectedSlot,
  onSelect,
}: {
  slots: DoctorScheduleSlot[];
  selectedSlot?: DoctorScheduleSlot | null;
  onSelect: (slot: DoctorScheduleSlot) => void;
}) {
  if (slots.length === 0) {
    return <p className="text-sm text-gray-400 py-6 text-center">No slots available for this date.</p>;
  }

  const grouped = slots.reduce<Record<string, DoctorScheduleSlot[]>>((acc, s) => {
    const period = s.startTime < "12:00" ? "Morning" : s.startTime < "17:00" ? "Afternoon" : "Evening";
    acc[period] ??= [];
    acc[period].push(s);
    return acc;
  }, {});

  return (
    <div className="space-y-3" aria-label="available-slots">
      {Object.entries(grouped).map(([period, periodSlots]) => (
        <div key={period}>
          <h4 className="text-xs font-semibold uppercase tracking-wide text-gray-500 mb-1">{period}</h4>
          <div className="flex flex-wrap gap-1.5">
            {periodSlots.map((s) => {
              const isSel = selectedSlot?.slotId === s.slotId;
              return (
                <button
                  key={s.slotId}
                  type="button"
                  disabled={!s.available || s.booked}
                  onClick={() => onSelect(s)}
                  className={[
                    "px-2.5 py-1 text-xs rounded-md border transition-all",
                    isSel
                      ? "bg-blue-600 border-blue-600 text-white shadow-sm"
                      : !s.available || s.booked
                        ? "bg-gray-100 text-gray-400 border-gray-200 cursor-not-allowed"
                        : "bg-white border-gray-200 hover:border-blue-400 hover:text-blue-700",
                  ].join(" ")}
                >
                  {s.startTime}–{s.endTime}
                  {s.booked && <span className="ml-1 text-red-500">●</span>}
                </button>
              );
            })}
          </div>
        </div>
      ))}
    </div>
  );
}

export function SlotSelector({
  doctorId,
  selectedDate,
  onSelect,
  selectedSlot,
}: SlotSelectorProps) {
  const { register } = useForm<SlotSelectionValues>({
    resolver: zodResolver(slotSelectionSchema),
    defaultValues: { doctorId, date: selectedDate },
  });

  const { data: slots, isLoading, isError } = useAvailableSlots(doctorId, selectedDate);

  return (
    <div className="space-y-4 p-4 border rounded-lg bg-white" aria-label="slot-selector">
      <h3 className="text-sm font-semibold">Select Date & Slot</h3>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        <div>
          <input type="hidden" {...register("date")} value={selectedDate} />
          <CalendarGrid
            selectedDate={selectedDate}
            onSelectDate={onDateSelect}
          />
        </div>

        <div>
          {isLoading ? (
            <div className="flex items-center justify-center py-12 text-sm text-gray-400">Loading slots…</div>
          ) : isError ? (
            <p className="text-sm text-red-600">Failed to load slots</p>
          ) : (
            <SlotGrid slots={slots || []} selectedSlot={selectedSlot} onSelect={onSelect} />
          )}
        </div>
      </div>

      {selectedSlot && (
        <div className="flex items-center gap-2 text-sm bg-blue-50 border border-blue-200 rounded p-2">
          <span className="font-medium">Selected:</span>
          <span>{selectedSlot.startTime} – {selectedSlot.endTime}</span>
          {selectedSlot.tokenNo && <span className="text-gray-500">(Token #{selectedSlot.tokenNo})</span>}
        </div>
      )}
    </div>
  );
}