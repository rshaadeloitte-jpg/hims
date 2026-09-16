import { useState } from "react";
import { Button } from "@chakra-ui/react";
import { FormField, TextInput } from "../../components/ui";
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
          <Button
            role="tab"
            aria-selected={view === "tasks"}
            variant={view === "tasks" ? "primary" : "ghost"}
            size="sm"
            onClick={() => setView("tasks")}
          >
            Nurse Tasks
          </Button>
          <Button
            role="tab"
            aria-selected={view === "goals"}
            variant={view === "goals" ? "primary" : "ghost"}
            size="sm"
            onClick={() => setView("goals")}
          >
            Goals &amp; Interventions
          </Button>
        </nav>
      </header>

      {view === "tasks" && <NurseTaskList />}
      {view === "goals" && (
        <div className="card">
          <div className="wl-goals-interventions__header">
            <h2>Care Goals &amp; Interventions</h2>
            <div className="wl-form__row">
              <FormField label="Admission ID">
                <TextInput
                  id="admissionId"
                  type="number"
                  value={admissionId || ""}
                  onChange={(e) => setAdmissionId(Number(e.target.value))}
                  placeholder="Enter admission ID"
                />
              </FormField>
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
