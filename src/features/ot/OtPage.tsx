import { useState } from "react";
import { useCreateOtSchedule, useOtSchedules, useUpdateOtScheduleStatus } from "./queries";
import { otScheduleSchema } from "./zodSchema";

const STATUS_OPTIONS = ["SCHEDULED", "IN_PROGRESS", "COMPLETED", "CANCELLED"];

export function OtPage() {
  const { data: schedules = [], isLoading } = useOtSchedules();
  const createSchedule = useCreateOtSchedule();
  const updateStatus = useUpdateOtScheduleStatus();
  const [showForm, setShowForm] = useState(false);
  const [form, setForm] = useState({ patientId: "", surgeryDate: "", otRoom: "", anesthesiaType: "", notes: "" });
  const [errors, setErrors] = useState<Record<string, string>>({});

  const handleSubmit = () => {
    const result = otScheduleSchema.safeParse({
      ...form,
      patientId: form.patientId ? Number(form.patientId) : undefined,
    });
    if (!result.success) {
      const fieldErrors: Record<string, string> = {};
      result.error.errors.forEach((e) => { fieldErrors[e.path[0]] = e.message; });
      setErrors(fieldErrors);
      return;
    }
    createSchedule.mutate(result.data, { onSuccess: () => { setShowForm(false); setForm({ patientId: "", surgeryDate: "", otRoom: "", anesthesiaType: "", notes: "" }); setErrors({}); } });
  };

  if (isLoading) return <p className="muted">Loading OT schedules…</p>;

  return (
    <section className="page">
      <header className="page-header">
        <h1>OT & Surgery</h1>
        <button className="btn btn-primary" onClick={() => setShowForm(!showForm)}>
          {showForm ? "Cancel" : "Schedule Surgery"}
        </button>
      </header>

      {showForm && (
        <div className="card">
          <h2>New Surgery Schedule</h2>
          <div className="form-grid">
            <label>Patient ID {errors.patientId && <span className="error">{errors.patientId}</span>}</label>
            <input value={form.patientId} onChange={(e) => setForm({ ...form, patientId: e.target.value })} />
            <label>Surgery Date {errors.surgeryDate && <span className="error">{errors.surgeryDate}</span>}</label>
            <input type="date" value={form.surgeryDate} onChange={(e) => setForm({ ...form, surgeryDate: e.target.value })} />
            <label>OT Room {errors.otRoom && <span className="error">{errors.otRoom}</span>}</label>
            <input value={form.otRoom} onChange={(e) => setForm({ ...form, otRoom: e.target.value })} />
            <label>Anesthesia Type</label>
            <select value={form.anesthesiaType} onChange={(e) => setForm({ ...form, anesthesiaType: e.target.value })}>
              <option value="">Select…</option>
              <option value="GENERAL">General</option>
              <option value="SPINAL">Spinal</option>
              <option value="EPIDURAL">Epidural</option>
              <option value="LOCAL">Local</option>
            </select>
            <label>Notes</label>
            <textarea value={form.notes} onChange={(e) => setForm({ ...form, notes: e.target.value })} />
          </div>
          <button className="btn btn-primary" onClick={handleSubmit} disabled={createSchedule.isPending}>
            {createSchedule.isPending ? "Saving…" : "Save Schedule"}
          </button>
        </div>
      )}

      <div className="card">
        <table className="table">
          <thead>
            <tr><th>Patient</th><th>Date</th><th>Room</th><th>Anesthesia</th><th>Status</th><th>Actions</th></tr>
          </thead>
          <tbody>
            {schedules.map((s) => (
              <tr key={s.id}>
                <td>{s.patientName}</td>
                <td>{s.surgeryDate}</td>
                <td>{s.otRoom}</td>
                <td>{s.anesthesiaType ?? "—"}</td>
                <td>{s.status}</td>
                <td>
                  <select value={s.status} onChange={(e) => updateStatus.mutate({ id: s.id, status: e.target.value })}>
                    {STATUS_OPTIONS.map((st) => <option key={st} value={st}>{st}</option>)}
                  </select>
                </td>
              </tr>
            ))}
            {schedules.length === 0 && <tr><td colSpan={6} className="muted">No surgeries scheduled.</td></tr>}
          </tbody>
        </table>
      </div>
    </section>
  );
}
