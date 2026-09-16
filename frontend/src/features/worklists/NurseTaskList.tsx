import { useState, useMemo, useCallback } from "react";
import { Button } from "@chakra-ui/react";
import { FormField, HimsSelect } from "../../components/ui";
import { useWorklistPatients, useTasksByPatient, useUpdateTaskStatus, type WorklistPatient, type NursingTaskItem } from "./queries";
import { statusBadgeClass, priorityClass } from "./zodSchema";
import type { TaskStatus } from "./zodSchema";

/**
 * NurseTaskList — patient-wise task aggregation with Linear-style task grouping.
 *
 * Pattern: Left panel = patient list with task counts; Right panel = tasks grouped by status.
 * Mirrors the Linear issue list UX: keyboard-friendly, status columns, priority indicators.
 */

const STATUS_COLUMNS: { key: TaskStatus; label: string }[] = [
  { key: "PENDING", label: "To Do" },
  { key: "IN_PROGRESS", label: "In Progress" },
  { key: "COMPLETED", label: "Done" },
  { key: "CANCELLED", label: "Cancelled" },
];

interface PatientRowProps {
  patient: WorklistPatient;
  isSelected: boolean;
  onSelect: (id: number) => void;
}

function PatientRow({ patient, isSelected, onSelect }: PatientRowProps) {
  return (
    <div
      role="button"
      tabIndex={0}
      aria-selected={isSelected}
      className={`wl-patient-row ${isSelected ? "wl-patient-row--active" : ""}`}
      onClick={() => onSelect(patient.admissionId)}
      onKeyDown={(e) => { if (e.key === "Enter" || e.key === " ") { e.preventDefault(); onSelect(patient.admissionId); } }}
    >
      <div className="wl-patient-row__main">
        <span className="wl-patient-row__name">{patient.name}</span>
        <span className="wl-patient-row__mrn">{patient.mrn}</span>
      </div>
      {patient.wardName && (
        <div className="wl-patient-row__loc">
          {patient.wardName}{patient.bedLabel ? ` · Bed ${patient.bedLabel}` : ""}
        </div>
      )}
      <div className="wl-patient-row__counts">
        <span className="wl-count wl-count--urgent" title="Open tasks">
          {patient.openTasks} open
        </span>
        <span className="wl-count" title="Total tasks">
          {patient.totalTasks} total
        </span>
        {patient.goalsCount > 0 && (
          <span className="wl-count wl-count--goals" title="Care goals">
            {patient.goalsCount} goals
          </span>
        )}
      </div>
    </div>
  );
}

interface TaskCardProps {
  task: NursingTaskItem;
  onStatusChange: (id: number, status: TaskStatus) => void;
}

function TaskCard({ task, onStatusChange }: TaskCardProps) {
  const isOverdue = task.dueTime && new Date(task.dueTime) < new Date() && task.status !== "COMPLETED";

  return (
    <div className={`wl-task-card ${isOverdue ? "wl-task-card--overdue" : ""}`}>
      <div className="wl-task-card__header">
        <span className={`wl-badge ${priorityClass(task.priority)}`}>{task.priority}</span>
        <span className="wl-task-card__type">{task.taskType}</span>
      </div>
      <p className="wl-task-card__desc">{task.description}</p>
      <div className="wl-task-card__meta">
        <span>Patient: {task.patientName ?? task.patientId}</span>
        {task.dueTime && (
          <span className={isOverdue ? "wl-text--danger" : ""}>
            Due: {new Date(task.dueTime).toLocaleString()}
          </span>
        )}
      </div>
      <div className="wl-task-card__actions">
        {task.status === "PENDING" && (
          <Button
            type="button"
            variant="primary"
            size="sm"
            onClick={() => onStatusChange(task.taskId, "IN_PROGRESS")}
          >
            Start
          </Button>
        )}
        {task.status === "IN_PROGRESS" && (
          <>
            <Button
              type="button"
              variant="primary"
              size="sm"
              onClick={() => onStatusChange(task.taskId, "COMPLETED")}
            >
              Complete
            </Button>
            <Button
              type="button"
              variant="ghost"
              size="sm"
              onClick={() => onStatusChange(task.taskId, "CANCELLED")}
            >
              Cancel
            </Button>
          </>
        )}
        {task.status === "COMPLETED" && (
          <span className="wl-badge bg-green-100 text-green-800">Done</span>
        )}
        {task.status === "CANCELLED" && (
          <Button
            type="button"
            variant="ghost"
            size="sm"
            onClick={() => onStatusChange(task.taskId, "PENDING")}
          >
            Reopen
          </Button>
        )}
      </div>
    </div>
  );
}

interface StatusColumnProps {
  status: TaskStatus;
  label: string;
  tasks: NursingTaskItem[];
  onStatusChange: (id: number, status: TaskStatus) => void;
}

function StatusColumn({ status, label, tasks, onStatusChange }: StatusColumnProps) {
  return (
    <div className="wl-column">
      <div className="wl-column__header">
        <span className={`wl-badge ${statusBadgeClass(status)}`}>{label}</span>
        <span className="wl-column__count">{tasks.length}</span>
      </div>
      <div className="wl-column__body">
        {tasks.map((task) => (
          <TaskCard key={task.taskId} task={task} onStatusChange={onStatusChange} />
        ))}
        {tasks.length === 0 && (
          <div className="wl-column__empty">No tasks</div>
        )}
      </div>
    </div>
  );
}

export function NurseTaskList() {
  const [selectedAdmissionId, setSelectedAdmissionId] = useState<number | null>(null);
  const [statusFilter, setStatusFilter] = useState<TaskStatus | "ALL">("ALL");

  const { data: patients = [], isLoading: patientsLoading } = useWorklistPatients();
  const { data: tasks = [], isLoading: tasksLoading } = useTasksByPatient(
    // We use admissionId as the key for tasks; in a real app we'd map admission→patient
    selectedAdmissionId ?? 0,
  );
  const updateStatus = useUpdateTaskStatus();

  const handleSelectPatient = useCallback((admissionId: number) => {
    setSelectedAdmissionId(admissionId);
  }, []);

  const handleStatusChange = useCallback(
    (taskId: number, status: TaskStatus) => {
      updateStatus.mutate({ id: taskId, status });
    },
    [updateStatus],
  );

  const filteredTasks = useMemo(() => {
    if (statusFilter === "ALL") return tasks;
    return tasks.filter((t) => t.status === statusFilter);
  }, [tasks, statusFilter]);

  const groupedTasks = useMemo(() => {
    const groups: Record<TaskStatus, NursingTaskItem[]> = {
      PENDING: [],
      IN_PROGRESS: [],
      COMPLETED: [],
      CANCELLED: [],
    };
    for (const task of filteredTasks) {
      groups[task.status].push(task);
    }
    return groups;
  }, [filteredTasks]);

  const selectedPatient = patients.find((p) => p.admissionId === selectedAdmissionId);

  return (
    <div className="wl-layout">
      {/* Patient list sidebar */}
      <aside className="wl-sidebar">
        <header className="wl-sidebar__header">
          <h2>Patients</h2>
          <span className="wl-sidebar__count">{patients.length}</span>
        </header>
        {patientsLoading ? (
          <div className="wl-loading">Loading patients…</div>
        ) : patients.length === 0 ? (
          <div className="wl-empty">No patients in worklist.</div>
        ) : (
          <div className="wl-patient-list">
            {patients.map((p) => (
              <PatientRow
                key={p.admissionId}
                patient={p}
                isSelected={p.admissionId === selectedAdmissionId}
                onSelect={handleSelectPatient}
              />
            ))}
          </div>
        )}
      </aside>

      {/* Task board */}
      <main className="wl-main">
        <header className="wl-main__header">
          <div>
            <h1>{selectedPatient ? `${selectedPatient.name} — Tasks` : "Nurse Task List"}</h1>
            {selectedPatient && (
              <p className="wl-muted">
                {selectedPatient.mrn} · {selectedPatient.wardName ?? "No ward"}
                {selectedPatient.bedLabel ? ` · Bed ${selectedPatient.bedLabel}` : ""}
              </p>
            )}
          </div>
          <div className="wl-filters">
            <FormField label="Filter by status">
              <HimsSelect
                id="status-filter"
                value={statusFilter}
                onChange={(e) => setStatusFilter(e.target.value as TaskStatus | "ALL")}
                aria-label="Filter by status"
              >
                <option value="ALL">All Statuses</option>
                {STATUS_COLUMNS.map((col) => (
                  <option key={col.key} value={col.key}>{col.label}</option>
                ))}
              </HimsSelect>
            </FormField>
          </div>
        </header>

        {!selectedAdmissionId ? (
          <div className="wl-empty wl-empty--large">
            Select a patient to view their tasks.
          </div>
        ) : tasksLoading ? (
          <div className="wl-loading">Loading tasks…</div>
        ) : tasks.length === 0 ? (
          <div className="wl-empty">No tasks for this patient.</div>
        ) : statusFilter === "ALL" ? (
          <div className="wl-board">
            {STATUS_COLUMNS.map((col) => (
              <StatusColumn
                key={col.key}
                status={col.key}
                label={col.label}
                tasks={groupedTasks[col.key]}
                onStatusChange={handleStatusChange}
              />
            ))}
          </div>
        ) : (
          <div className="wl-single-column">
            <StatusColumn
              status={statusFilter as TaskStatus}
              label={STATUS_COLUMNS.find((c) => c.key === statusFilter)?.label ?? statusFilter}
              tasks={groupedTasks[statusFilter as TaskStatus]}
              onStatusChange={handleStatusChange}
            />
          </div>
        )}
      </main>
    </div>
  );
}
