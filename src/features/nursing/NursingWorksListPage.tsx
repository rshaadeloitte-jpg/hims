import { useState } from "react";
import { useVitals, useRecordVitals, useNursingNotes, useCreateNursingNote, useNursingTasks, useCreateNursingTask, useCompleteTask } from "./queries";
import { vitalsSchema, nursingNoteSchema, nursingTaskSchema, type VitalsValues, type NursingNoteValues, type NursingTaskValues } from "./zodSchema";
import { zodResolver } from "@hookform/resolvers/zod";
import { useForm } from "react-hook-form";

// DEPRECATED: Replaced by NursingWorklist.tsx for M09 implementation
// TODO: Remove after verification
export function NursingWorksListPage() {
  const [patientId, setPatientId] = useState(0);
  const [admissionId, setAdmissionId] = useState(0);
  const { data: vitals = [] } = useVitals(patientId);
  const { data: notes = [] } = useNursingNotes(admissionId);
  const { data: tasks = [] } = useNursingTasks();
  const recordVitals = useRecordVitals();
  const createNote = useCreateNursingNote();
  const createTask = useCreateNursingTask();
  const completeTask = useCompleteTask();

  const vitalsForm = useForm<VitalsValues>({ resolver: zodResolver(vitalsSchema) });
  const noteForm = useForm<NursingNoteValues>({ resolver: zodResolver(nursingNoteSchema) });
  const taskForm = useForm<NursingTaskValues>({ resolver: zodResolver(nursingTaskSchema) });

  const submitVitals = async (v: VitalsValues) => {
    await recordVitals.mutateAsync({ patientId, payload: v as unknown as Record<string, unknown> });
    vitalsForm.reset();
  };
  const submitNote = async (v: NursingNoteValues) => {
    await createNote.mutateAsync({ admissionId, payload: v as unknown as Record<string, unknown> });
    noteForm.reset();
  };
  const submitTask = async (v: NursingTaskValues) => {
    await createTask.mutateAsync(v as unknown as Record<string, unknown>);
    taskForm.reset();
  };

  return (
    <section className="page">
      <header className="page__header"><h1>Nursing — Worklist</h1></header>
      <div className="row">
        <input type="number" placeholder="Patient ID" value={patientId || ""} onChange={(e) => setPatientId(Number(e.target.value))} />
        <input type="number" placeholder="Admission ID" value={admissionId || ""} onChange={(e) => setAdmissionId(Number(e.target.value))} />
      </div>

      <h2>Vitals</h2>
      {patientId > 0 && (
        <form className="inline-form" onSubmit={vitalsForm.handleSubmit(submitVitals)}>
          <input type="number" step="0.1" placeholder="Temp" {...vitalsForm.register("temperature")} />
          <input type="number" placeholder="Pulse" {...vitalsForm.register("pulse")} />
          <input type="number" placeholder="RR" {...vitalsForm.register("respiration")} />
          <input type="number" placeholder="BP Sys" {...vitalsForm.register("bloodPressureSystolic")} />
          <input type="number" placeholder="BP Dia" {...vitalsForm.register("bloodPressureDiastolic")} />
          <input type="number" placeholder="SpO2" {...vitalsForm.register("spO2")} />
          <button type="submit" className="btn" disabled={recordVitals.isPending}>Record</button>
        </form>
      )}
      <table className="table">
        <thead><tr><th>Date</th><th>Temp</th><th>Pulse</th><th>RR</th><th>BP</th><th>SpO2</th></tr></thead>
        <tbody>
          {vitals.slice(0, 10).map((v) => (
            <tr key={v.vitalId}>
              <td>{v.recordedAt?.slice(0, 16)}</td>
              <td>{v.temperature}</td>
              <td>{v.pulse}</td>
              <td>{v.respiration}</td>
              <td>{v.bloodPressureSystolic}/{v.bloodPressureDiastolic}</td>
              <td>{v.spO2}</td>
            </tr>
          ))}
          {vitals.length === 0 && <tr><td colSpan={6} className="muted">No vitals.</td></tr>}
        </tbody>
      </table>

      <h2>Notes</h2>
      {admissionId > 0 && (
        <form className="inline-form" onSubmit={noteForm.handleSubmit(submitNote)}>
          <input placeholder="Category" {...noteForm.register("category")} />
          <input placeholder="Note content" {...noteForm.register("content")} />
          <button type="submit" className="btn" disabled={createNote.isPending}>+ Note</button>
        </form>
      )}
      <table className="table">
        <thead><tr><th>Date</th><th>Category</th><th>Content</th></tr></thead>
        <tbody>
          {notes.slice(0, 10).map((n) => (
            <tr key={n.noteId}><td>{n.createdAt?.slice(0, 16)}</td><td>{n.category ?? "—"}</td><td>{n.content}</td></tr>
          ))}
          {notes.length === 0 && <tr><td colSpan={3} className="muted">No notes.</td></tr>}
        </tbody>
      </table>

      <h2>Tasks</h2>
      <form className="inline-form" onSubmit={taskForm.handleSubmit(submitTask)}>
        <input type="number" placeholder="Admission ID" {...taskForm.register("admissionId")} />
        <input type="number" placeholder="Patient ID" {...taskForm.register("patientId")} />
        <input placeholder="Task Type" {...taskForm.register("taskType")} />
        <input placeholder="Description" {...taskForm.register("description")} />
        <button type="submit" className="btn" disabled={createTask.isPending}>+ Task</button>
      </form>
      <table className="table">
        <thead><tr><th>Type</th><th>Description</th><th>Status</th><th></th></tr></thead>
        <tbody>
          {tasks.map((t) => (
            <tr key={t.taskId}>
              <td>{t.taskType}</td>
              <td>{t.description}</td>
              <td>{t.completed ? "Done" : t.cancelled ? "Cancelled" : "Open"}</td>
              <td>{!t.completed && !t.cancelled && <button className="btn" onClick={() => completeTask.mutate(t.taskId)}>Complete</button>}</td>
            </tr>
          ))}
          {tasks.length === 0 && <tr><td colSpan={4} className="muted">No tasks.</td></tr>}
        </tbody>
      </table>
    </section>
  );
}