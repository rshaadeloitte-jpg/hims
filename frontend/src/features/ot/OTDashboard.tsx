import { useState, useMemo } from "react";
import {
  useOtSchedules,
  useOtDashboard,
  type OtSchedule,
  type OtRoom,
} from "./queries";

const STATUS_LABELS: Record<number, string> = {
  1: "PLANNED",
  2: "IN_PROGRESS",
  3: "DONE",
  4: "CANCELLED",
};

const STATUS_COLORS: Record<number, { bg: string; text: string; border: string; dot: string }> = {
  1: { bg: "bg-blue-50", text: "text-blue-700", border: "border-blue-200", dot: "bg-blue-500" },
  2: { bg: "bg-yellow-50", text: "text-yellow-700", border: "border-yellow-200", dot: "bg-yellow-500" },
  3: { bg: "bg-green-50", text: "text-green-700", border: "border-green-200", dot: "bg-green-500" },
  4: { bg: "bg-red-50", text: "text-red-700", border: "border-red-200", dot: "bg-red-500" },
};

function getStatusLabel(status?: number): string {
  return status != null ? (STATUS_LABELS[status] ?? "UNKNOWN") : "AVAILABLE";
}

function getStatusColors(status?: number) {
  return status != null ? (STATUS_COLORS[status] ?? STATUS_COLORS[1]) : STATUS_COLORS[1];
}

interface RoomCardProps {
  room: OtRoom;
  schedules: OtSchedule[];
}

function RoomCard({ room, schedules }: RoomCardProps) {
  const roomId = room.id ?? room.roomId ?? 0;
  const isActive = room.isActive !== false;

  const roomSchedules = useMemo(
    () => schedules.filter((s) => String(s.otTable) === String(room.roomName)).sort((a, b) => (a.scheduledStart ?? "").localeCompare(b.scheduledStart ?? "")),
    [schedules, room.roomName]
  );

  const currentSurgery = roomSchedules.find((s) => s.status === 2);
  const nextSurgery = roomSchedules.find((s) => s.status === 1);
  const completedToday = roomSchedules.filter((s) => s.status === 3).length;
  const totalScheduled = roomSchedules.length;

  const status = currentSurgery?.status ?? (nextSurgery ? 1 : undefined);
  const colors = getStatusColors(status);

  return (
    <div className={`border rounded-lg p-4 ${colors.border} ${colors.bg} ${!isActive ? "opacity-60" : ""}`}>
      <div className="flex items-start justify-between mb-3">
        <div>
          <h3 className="text-sm font-semibold text-gray-900">{room.roomName}</h3>
          {room.roomType && <p className="text-xs text-gray-500">{room.roomType}</p>}
        </div>
        <div className="flex items-center gap-2">
          {isActive ? (
            <span className="flex items-center gap-1 text-xs text-green-700">
              <span className="w-2 h-2 bg-green-500 rounded-full animate-pulse" />
              Active
            </span>
          ) : (
            <span className="text-xs text-gray-500">Inactive</span>
          )}
        </div>
      </div>

      {currentSurgery ? (
        <div className="mb-3 p-2 bg-yellow-100 border border-yellow-200 rounded">
          <div className="flex items-center justify-between mb-1">
            <span className="text-xs font-medium text-yellow-800">IN PROGRESS</span>
            <span className="text-xs text-yellow-700">
              {new Date(currentSurgery.scheduledStart!).toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" })}
            </span>
          </div>
          <div className="text-sm text-gray-900">Patient #{currentSurgery.patientId}</div>
          {currentSurgery.details && currentSurgery.details.length > 0 && (
            <div className="text-xs text-gray-600 mt-1">{currentSurgery.details[0]?.procedureName}</div>
          )}
        </div>
      ) : nextSurgery ? (
        <div className="mb-3 p-2 bg-blue-100 border border-blue-200 rounded">
          <div className="flex items-center justify-between mb-1">
            <span className="text-xs font-medium text-blue-800">NEXT</span>
            <span className="text-xs text-blue-700">
              {new Date(nextSurgery.scheduledStart!).toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" })}
            </span>
          </div>
          <div className="text-sm text-gray-900">Patient #{nextSurgery.patientId}</div>
        </div>
      ) : (
        <div className="mb-3 p-2 bg-gray-100 border border-gray-200 rounded">
          <span className="text-xs text-gray-600">No upcoming surgeries</span>
        </div>
      )}

      <div className="flex items-center justify-between text-xs text-gray-600 pt-2 border-t border-gray-200">
        <span>{totalScheduled} scheduled</span>
        <span>{completedToday} done</span>
      </div>
    </div>
  );
}

interface TimelineProps {
  schedules: OtSchedule[];
}

function Timeline({ schedules }: TimelineProps) {
  const hours = Array.from({ length: 14 }, (_, i) => i + 7); // 7 AM to 8 PM
  const sorted = useMemo(() => [...schedules].sort((a, b) => (a.scheduledStart ?? "").localeCompare(b.scheduledStart ?? "")), [schedules]);

  return (
    <div className="border border-gray-200 rounded-lg overflow-hidden bg-white">
      <div className="bg-gray-50 px-4 py-2 border-b border-gray-200">
        <h3 className="text-sm font-medium text-gray-900">Today's Timeline</h3>
      </div>
      <div className="p-4">
        {sorted.length === 0 ? (
          <div className="text-center py-4 text-gray-500 text-sm">No surgeries today</div>
        ) : (
          <div className="space-y-2">
            {sorted.map((s) => {
              const id = s.scheduleId ?? s.id ?? 0;
              const start = s.scheduledStart ? new Date(s.scheduledStart) : null;
              const end = s.scheduledEnd ? new Date(s.scheduledEnd) : null;
              const colors = getStatusColors(s.status);
              return (
                <div key={id} className={`flex items-center gap-3 p-2 border-l-4 ${colors.border} ${colors.bg} rounded-r`}>
                  <div className="text-xs font-mono text-gray-600 w-24 flex-shrink-0">
                    {start ? start.toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" }) : "—"}
                    {end && `–${end.toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" })}`}
                  </div>
                  <div className="flex-1 min-w-0">
                    <div className="text-sm font-medium text-gray-900 truncate">Patient #{s.patientId} — Table {s.otTable}</div>
                    {s.details && s.details.length > 0 && (
                      <div className="text-xs text-gray-600 truncate">{s.details.map((d) => d.procedureName).join(", ")}</div>
                    )}
                  </div>
                  <span className={`text-xs px-2 py-0.5 rounded ${colors.bg} ${colors.text} border ${colors.border}`}>
                    {getStatusLabel(s.status)}
                  </span>
                </div>
              );
            })}
          </div>
        )}
      </div>
    </div>
  );
}

export function OTDashboard() {
  const today = new Date().toISOString().slice(0, 10);
  const [date, setDate] = useState(today);
  const { data: dashboard, isLoading } = useOtDashboard(date);
  const { data: schedules = [] } = useOtSchedules(date);

  const rooms: OtRoom[] = dashboard?.rooms ?? [];
  const todaySchedules = dashboard?.schedules ?? schedules;

  const stats = useMemo(() => {
    const planned = todaySchedules.filter((s) => s.status === 1).length;
    const inProgress = todaySchedules.filter((s) => s.status === 2).length;
    const completed = todaySchedules.filter((s) => s.status === 3).length;
    const cancelled = todaySchedules.filter((s) => s.status === 4).length;
    return { planned, inProgress, completed, cancelled, total: todaySchedules.length };
  }, [todaySchedules]);

  return (
    <section className="space-y-6" aria-label="ot-dashboard">
      <header className="flex items-center justify-between">
        <div>
          <h2 className="text-xl font-semibold text-gray-900">OT Dashboard</h2>
          <p className="text-sm text-gray-500">Real-time room status and surgery board</p>
        </div>
        <div className="flex items-center gap-2">
          <label htmlFor="dashDate" className="text-sm text-gray-600">Date</label>
          <input
            id="dashDate"
            type="date"
            value={date}
            onChange={(e) => setDate(e.target.value)}
            className="border border-gray-300 rounded-md px-2 py-1.5 text-sm focus:ring-2 focus:ring-blue-500"
          />
        </div>
      </header>

      <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
        <div className="bg-blue-50 border border-blue-200 rounded-lg p-4">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-xs font-medium text-blue-700">Planned</p>
              <p className="text-2xl font-bold text-blue-900">{stats.planned}</p>
            </div>
            <div className="text-3xl opacity-40">📋</div>
          </div>
        </div>
        <div className="bg-yellow-50 border border-yellow-200 rounded-lg p-4">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-xs font-medium text-yellow-700">In Progress</p>
              <p className="text-2xl font-bold text-yellow-900">{stats.inProgress}</p>
            </div>
            <div className="text-3xl opacity-40">⚙️</div>
          </div>
        </div>
        <div className="bg-green-50 border border-green-200 rounded-lg p-4">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-xs font-medium text-green-700">Completed</p>
              <p className="text-2xl font-bold text-green-900">{stats.completed}</p>
            </div>
            <div className="text-3xl opacity-40">✓</div>
          </div>
        </div>
        <div className="bg-red-50 border border-red-200 rounded-lg p-4">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-xs font-medium text-red-700">Cancelled</p>
              <p className="text-2xl font-bold text-red-900">{stats.cancelled}</p>
            </div>
            <div className="text-3xl opacity-40">✗</div>
          </div>
        </div>
      </div>

      {isLoading ? (
        <div className="text-center py-8 text-gray-500">Loading OT board...</div>
      ) : (
        <>
          <div>
            <h3 className="text-sm font-semibold text-gray-900 mb-3">Room Status ({rooms.length} rooms)</h3>
            {rooms.length === 0 ? (
              <div className="border border-gray-200 rounded-lg p-6 bg-white text-center text-gray-500 text-sm">
                No OT rooms configured. Rooms will appear here once registered.
              </div>
            ) : (
              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
                {rooms.map((room) => (
                  <RoomCard key={room.id ?? room.roomId ?? room.roomName} room={room} schedules={todaySchedules} />
                ))}
              </div>
            )}
          </div>

          <Timeline schedules={todaySchedules} />
        </>
      )}
    </section>
  );
}
