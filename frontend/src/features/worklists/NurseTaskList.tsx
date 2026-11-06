import { useState } from "react";
import { useWorklistPatients } from "./queries";

export function NurseTaskList() {
  const [admissionIdInput, setAdmissionIdInput] = useState("");
  const admissionId = Number(admissionIdInput) || 0;
  const { data: worklist, isLoading } = useWorklistPatients(admissionId > 0 ? admissionId : undefined);

  return (
    <div className="wl-layout">
      <section className="page">
        <header className="page-header"><h1>Nurse Task List</h1></header>
        <div className="card">
          <label htmlFor="admission-id">Admission ID</label>
          <input
            id="admission-id"
            type="number"
            value={admissionIdInput}
            onChange={(e) => setAdmissionIdInput(e.target.value)}
          />
        </div>
        {isLoading && <div className="wl-loading">Loading worklist…</div>}
        {admissionId > 0 && worklist && (
          <div className="card">
            <p>Admission {worklist.admissionId}</p>
            <p>{worklist.openTasks} open / {worklist.totalTasks} total</p>
            <ul>
              {worklist.tabs.map((t) => (
                <li key={t.name}>{t.name}: {t.open}/{t.total}</li>
              ))}
            </ul>
          </div>
        )}
        {admissionId > 0 && !isLoading && !worklist && (
          <div className="wl-empty">No worklist found.</div>
        )}
      </section>
    </div>
  );
}
