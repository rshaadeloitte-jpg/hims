import { useState } from "react";
import { NurseTaskList } from "./NurseTaskList";
import { GoalsInterventions } from "./GoalsInterventions";

/**
 * WorklistsPage — M26 landing page combining nurse task list and goals/interventions.
 * Tab-style navigation between the two views.
 */

type View = "tasks" | "goals";

export function WorklistsPage() {
  const [view, setView] = useState<View>("tasks");
  const [admissionId, setAdmissionId] = useState<number>(0);

  return (
    <section className="page">
      <header className="page-header">
        <h1>Patient Care Worklists</h1>
        <nav className="wl-tabs" role="tablist">
          <button
            role="tab"
            aria-selected={view === "tasks"}
            className={`wl-tab ${view === "tasks" ? "wl-tab--active" : ""}`}
            onClick={() => setView("tasks")}
          >
            Nurse Tasks
          </button>
          <button
            role="tab"
            aria-selected={view === "goals"}
            className={`wl-tab ${view === "goals" ? "wl-tab--active" : ""}`}
            onClick={() => setView("goals")}
          >
            Goals &amp; Interventions
          </button>
        </nav>
      </header>

      {view === "tasks" && <NurseTaskList />}
      {view === "goals" && (
        <div className="card">
          <div className="wl-goals-interventions__header">
            <h2>Care Goals &amp; Interventions</h2>
            <div className="wl-form__row">
              <label htmlFor="admissionId" className="wl-form__label">Admission ID</label>
              <input
                id="admissionId"
                type="number"
                className="wl-form__input"
                value={admissionId || ""}
                onChange={(e) => setAdmissionId(Number(e.target.value))}
                placeholder="Enter admission ID"
              />
            </div>
          </div>
          {admissionId > 0 ? (
            <GoalsInterventions admissionId={admissionId} />
          ) : (
            <div className="wl-empty">Enter an admission ID to view care goals and interventions.</div>
          )}
        </div>
      )}
    </section>
  );
}
