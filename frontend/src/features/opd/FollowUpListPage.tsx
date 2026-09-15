import { useState } from "react";
import { useCancelFollowUp, useCompleteFollowUp, useFollowUpList } from "./queries";
import type { FollowUpListState } from "./queries";

/**
 * M02 Follow-up List (G-1). Three view modes:
 *  - by parent appointment: GET /appointments/{id}/follow-ups
 *  - by patient + date:    GET /appointments/follow-ups/patient/{id}?date=
 *  - by doctor + date:     GET /appointments/follow-ups/doctor/{id}?date=
 */
export function FollowUpListPage() {
  const [mode, setMode] = useState<"appointment" | "patient" | "doctor">("appointment");
  const [apptId, setApptId] = useState("");
  const [patientId, setPatientId] = useState("");
  const [doctorId, setDoctorId] = useState("");
  const [date, setDate] = useState(new Date().toISOString().slice(0, 10));

  const state: FollowUpListState = (() => {
    if (mode === "appointment") return { appointmentId: apptId ? Number(apptId) : undefined };
    if (mode === "patient") return { patientId: patientId ? Number(patientId) : undefined, date };
    return { doctorId: doctorId ? Number(doctorId) : undefined, date };
  })();

  const list = useFollowUpList(state);
  const cancel = useCancelFollowUp();
  const complete = useCompleteFollowUp();

  return (
    <div className="p-4 border rounded space-y-3" aria-label="follow-up-list">
      <h2 className="text-xl font-semibold">Follow-up Visits</h2>

      <div className="flex gap-2 items-end flex-wrap">
        <label className="text-sm">
          <span className="block font-medium">Mode</span>
          <select
            className="border rounded px-2 py-1"
            value={mode}
            onChange={(e) => setMode(e.target.value as typeof mode)}
          >
            <option value="appointment">By parent appointment</option>
            <option value="patient">By patient + date</option>
            <option value="doctor">By doctor + date</option>
          </select>
        </label>
        {mode === "appointment" && (
          <label className="text-sm">
            <span className="block font-medium">Appointment ID</span>
            <input className="border rounded px-2 py-1" type="number" value={apptId} onChange={(e) => setApptId(e.target.value)} />
          </label>
        )}
        {mode === "patient" && (
          <label className="text-sm">
            <span className="block font-medium">Patient ID</span>
            <input className="border rounded px-2 py-1" type="number" value={patientId} onChange={(e) => setPatientId(e.target.value)} />
          </label>
        )}
        {mode === "doctor" && (
          <label className="text-sm">
            <span className="block font-medium">Doctor ID</span>
            <input className="border rounded px-2 py-1" type="number" value={doctorId} onChange={(e) => setDoctorId(e.target.value)} />
          </label>
        )}
        {mode !== "appointment" && (
          <label className="text-sm">
            <span className="block font-medium">Date</span>
            <input className="border rounded px-2 py-1" type="date" value={date} onChange={(e) => setDate(e.target.value)} />
          </label>
        )}
      </div>

      {list.isLoading && <p className="text-sm text-gray-500">Loading...</p>}
      {list.isError && <p className="text-sm text-red-600">{(list.error as Error).message}</p>}
      {list.data && list.data.length === 0 && <p className="text-sm text-gray-500">No follow-ups.</p>}
      {list.data && list.data.length > 0 && (
        <table className="w-full text-sm border rounded">
          <thead className="bg-gray-100">
            <tr>
              <th className="p-2 text-left">ID</th>
              <th className="p-2 text-left">Date</th>
              <th className="p-2 text-left">Patient</th>
              <th className="p-2 text-left">Doctor</th>
              <th className="p-2 text-left">Status</th>
              <th className="p-2 text-left">Actions</th>
            </tr>
          </thead>
          <tbody>
            {list.data.map((f) => (
              <tr key={f.id} className="border-t">
                <td className="p-2">{f.id}</td>
                <td className="p-2">{f.followUpDate}</td>
                <td className="p-2">{f.patientId}</td>
                <td className="p-2">{f.doctorId ?? "-"}</td>
                <td className="p-2">
                  {f.followUpStatus === 1 ? "SCHEDULED" : f.followUpStatus === 2 ? "COMPLETED" : "CANCELLED"}
                </td>
                <td className="p-2 space-x-1">
                  <button
                    type="button"
                    className="text-xs px-2 py-0.5 rounded bg-blue-600 text-white disabled:opacity-50"
                    disabled={complete.isPending || f.followUpStatus === 2}
                    onClick={() => complete.mutate(f.id)}
                  >
                    Complete
                  </button>
                  <button
                    type="button"
                    className="text-xs px-2 py-0.5 rounded bg-red-600 text-white disabled:opacity-50"
                    disabled={cancel.isPending || f.followUpStatus === 3}
                    onClick={() => cancel.mutate(f.id)}
                  >
                    Cancel
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  );
}
