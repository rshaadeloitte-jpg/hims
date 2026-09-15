import { useMemo } from "react";
import { useQuery } from "@tanstack/react-query";
import { TOKEN_STATUS } from "../queue/zodSchema";

// ── Types ─────────────────────────────────────────────────────────────────
export interface QueuePatient {
  tokenId: number;
  tokenNo: number;
  patientId: number;
  patientName?: string;
  mrNo?: string;
  priority: "NORMAL" | "EMERGENCY" | "VIP";
  status: number; // 1=waiting 2=in-consult 3=completed 4=no-show
  createdAt: string;
  cabin?: string;
}

export interface DoctorQueueState {
  doctorId: number;
  date: string;
  onCallPatient?: (tokenId: number) => void;
  onComplete?: (tokenId: number) => void;
  onNoShow?: (tokenId: number) => void;
  _onTransfer?: (tokenId: number) => void;
}

// ── Column definition ─────────────────────────────────────────────────────
const COLUMNS: { key: "waiting" | "inConsult" | "completed" | "noShow"; label: string; status: number; color: string }[] = [
  { key: "waiting", label: "Waiting", status: 1, color: "amber" },
  { key: "inConsult", label: "In Consult", status: 2, color: "blue" },
  { key: "completed", label: "Completed", status: 3, color: "green" },
  { key: "noShow", label: "No Show", status: 4, color: "gray" },
];

// ── Patient Card ──────────────────────────────────────────────────────────
function PatientCard({
  patient,
  onCall,
  onComplete,
  onNoShow,
}: {
  patient: QueuePatient;
  onCall?: (id: number) => void;
  onComplete?: (id: number) => void;
  onNoShow?: (id: number) => void;
}) {
  const statusKey = patient.status as 1 | 2 | 3 | 4;
  const statusLabel = TOKEN_STATUS[statusKey] ?? "Unknown";
  const borderMap: Record<number, string> = {
    1: "border-l-amber-400",
    2: "border-l-blue-500",
    3: "border-l-green-500",
    4: "border-l-gray-300",
  };
  const priorityBadge = patient.priority === "EMERGENCY"
    ? "bg-red-100 text-red-700"
    : patient.priority === "VIP"
      ? "bg-purple-100 text-purple-700"
      : "bg-gray-100 text-gray-600";

  return (
    <div
      className={`border-l-4 ${borderMap[patient.status] ?? "border-l-gray-200"} bg-white rounded-md shadow-sm p-3 space-y-1.5`}
    >
      <div className="flex items-center justify-between">
        <span className="text-xs font-mono font-semibold text-gray-500">#{patient.tokenNo}</span>
        <span className={`text-xs px-1.5 py-0.5 rounded ${priorityBadge}`}>{patient.priority}</span>
      </div>
      <div className="text-sm font-medium truncate">
        {patient.patientName ?? `Patient ${patient.patientId}`}
      </div>
      {patient.mrNo && <div className="text-xs text-gray-400">MRN: {patient.mrNo}</div>}
      {patient.cabin && <div className="text-xs text-gray-400">Cabin: {patient.cabin}</div>}
      <div className="text-xs text-gray-400">
        {new Date(patient.createdAt).toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" })}
      </div>
      <div className="text-xs font-medium text-gray-500">{statusLabel}</div>

      {patient.status === 1 && onCall && (
        <button
          type="button"
          className="mt-1 w-full text-xs bg-blue-600 text-white py-1 rounded hover:bg-blue-700"
          onClick={() => onCall(patient.tokenId)}
        >
          Call
        </button>
      )}
      {patient.status === 2 && (
        <div className="flex gap-1 mt-1">
          {onComplete && (
            <button
              type="button"
              className="flex-1 text-xs bg-green-600 text-white py-1 rounded hover:bg-green-700"
              onClick={() => onComplete(patient.tokenId)}
            >
              Complete
            </button>
          )}
          {onNoShow && (
            <button
              type="button"
              className="flex-1 text-xs bg-gray-500 text-white py-1 rounded hover:bg-gray-600"
              onClick={() => onNoShow(patient.tokenId)}
            >
              No-show
            </button>
          )}
        </div>
      )}
    </div>
  );
}

// ── Fetch hook (uses queue-service backend) ───────────────────────────────
const QUEUE_BASE: string = import.meta.env.VITE_QUEUE_API_BASE ?? "/api";

function useDoctorQueue(doctorId: number, date: string) {
  return useQuery<QueuePatient[]>({
    queryKey: ["opd", "doctor-queue", doctorId, date],
    queryFn: async () => {
      const res = await fetch(
        `${QUEUE_BASE}/api/v1/queue/doctor/${doctorId}/batch?date=${date}`,
        { headers: { "X-Request-ID": crypto.randomUUID() } },
      );
      if (!res.ok) throw new Error("Failed to load doctor queue");
      return res.json();
    },
    enabled: doctorId > 0 && !!date,
  });
}

// ── Main Export ───────────────────────────────────────────────────────────
export function DoctorQueue({
  doctorId,
  date,
  onCallPatient,
  onComplete,
  onNoShow,
  onTransfer,
}: DoctorQueueState) {
  const { data: patients = [], isLoading, isError } = useDoctorQueue(doctorId, date);

  const columns = useMemo(() => {
    const map: Record<string, QueuePatient[]> = { waiting: [], inConsult: [], completed: [], noShow: [] };
    patients.forEach((p) => {
      if (p.status === 1) map.waiting.push(p);
      else if (p.status === 2) map.inConsult.push(p);
      else if (p.status === 3) map.completed.push(p);
      else if (p.status === 4) map.noShow.push(p);
    });
    return map;
  }, [patients]);

  const waitingCount = columns.waiting.length;
  const inConsultCount = columns.inConsult.length;

  if (isLoading) return <div className="p-8 text-center text-sm text-gray-400">Loading queue…</div>;
  if (isError) return <div className="p-4 text-sm text-red-600">Failed to load queue.</div>;

  return (
    <div className="space-y-3" aria-label="doctor-queue">
      {/* Summary bar */}
      <div className="flex items-center gap-4 text-sm">
        <span className="font-medium">Doctor #{doctorId}</span>
        <span className="text-gray-400">·</span>
        <span className="text-amber-600 font-medium">{waitingCount} waiting</span>
        <span className="text-blue-600 font-medium">{inConsultCount} in consult</span>
      </div>

      {/* Kanban columns */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-3">
        {COLUMNS.map((col) => (
          <div key={col.key} className="space-y-2">
            <div className="flex items-center justify-between">
              <h4 className="text-xs font-semibold uppercase tracking-wide text-gray-500">{col.label}</h4>
              <span className="text-xs bg-gray-100 px-2 py-0.5 rounded-full">{columns[col.key].length}</span>
            </div>
            <div className="space-y-2 min-h-12">
              {columns[col.key].map((p) => (
                <PatientCard
                  key={p.tokenId}
                  patient={p}
                  onCall={onCallPatient}
                  onComplete={onComplete}
                  onNoShow={onNoShow}
                />
              ))}
              {columns[col.key].length === 0 && (
                <div className="text-xs text-gray-300 text-center py-4 border-2 border-dashed border-gray-100 rounded">
                  Empty
                </div>
              )}
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}