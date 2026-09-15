import { useState } from "react";
import {
  usePmSchedules,
  useCreatePmSchedule,
  usePmHistory,
  useCompletePm,
  usePmChecklists,
} from "./queries";
import {
  pmScheduleSchema,
  pmCompletionSchema,
  type PmScheduleForm,
  type PmCompletionForm,
} from "./zodSchema";

const MAINTENANCE_TYPE_LABELS: Record<number, string> = {
  1: "Preventive",
  2: "Corrective",
  3: "Calibration",
};

const PM_STATUS_LABELS: Record<number, string> = {
  1: "Scheduled",
  2: "In Progress",
  3: "Completed",
  4: "Cancelled",
};

const STATUS_BADGE: Record<number, string> = {
  1: "badge-info",
  2: "badge-warning",
  3: "badge-success",
  4: "badge-muted",
};

const MONTHS = [
  { key: "jan", label: "Jan" },
  { key: "feb", label: "Feb" },
  { key: "mar", label: "Mar" },
  { key: "apr", label: "Apr" },
  { key: "may", label: "May" },
  { key: "jun", label: "Jun" },
  { key: "jul", label: "Jul" },
  { key: "aug", label: "Aug" },
  { key: "sep", label: "Sep" },
  { key: "oct", label: "Oct" },
  { key: "nov", label: "Nov" },
  { key: "dec", label: "Dec" },
] as const;

const emptyScheduleForm: PmScheduleForm = {
  equipmentId: 0,
  scheduleCode: undefined,
  frequency: 1,
  pmDate: undefined,
  jan: false,
  feb: false,
  mar: false,
  apr: false,
  may: false,
  jun: false,
  jul: false,
  aug: false,
  sep: false,
  oct: false,
  nov: false,
  dec: false,
  pmType: 1,
  storeId: undefined,
};

const emptyCompletionForm: PmCompletionForm = {
  maintenanceNo: undefined,
  maintenanceDate: undefined,
  performedBy: undefined,
  functionalCheckRemark: undefined,
  preventiveMaintenanceRemark: undefined,
  electricalSafetyRemark: undefined,
  calibration: undefined,
  furtherActions: undefined,
  commentsOnUse: undefined,
  remarks: undefined,
  deptInCharge: undefined,
  biomedicalEngineerId: undefined,
  biomedicalInChargeId: undefined,
  verifiedBy: undefined,
  amountIncurred: undefined,
  serviceAgentId: undefined,
  pmScheduleDetailId: undefined,
  itemId: undefined,
};

export function PMSchedule() {
  const { data: schedules = [], isLoading, error } = usePmSchedules();
  const create = useCreatePmSchedule();
  const complete = useCompletePm();
  const { data: checklists = [] } = usePmChecklists();

  const [showForm, setShowForm] = useState(false);
  const [scheduleForm, setScheduleForm] = useState<PmScheduleForm>(emptyScheduleForm);
  const [scheduleErrors, setScheduleErrors] = useState<Record<string, string>>({});

  const [completingId, setCompletingId] = useState<number | null>(null);
  const [completionForm, setCompletionForm] = useState<PmCompletionForm>(emptyCompletionForm);

  const [expandedId, setExpandedId] = useState<number | null>(null);
  const { data: history } = usePmHistory(expandedId ?? 0);

  const handleCreate = () => {
    const result = pmScheduleSchema.safeParse(scheduleForm);
    if (!result.success) {
      const fieldErrors: Record<string, string> = {};
      result.error.errors.forEach((e) => {
        fieldErrors[e.path[0] as string] = e.message;
      });
      setScheduleErrors(fieldErrors);
      return;
    }
    create.mutate(result.data as Record<string, unknown>, {
      onSuccess: () => {
        setShowForm(false);
        setScheduleForm(emptyScheduleForm);
        setScheduleErrors({});
      },
    });
  };

  const handleComplete = () => {
    const result = pmCompletionSchema.safeParse(completionForm);
    if (!result.success) {
      return;
    }
    if (completingId) {
      complete.mutate(
        { scheduleId: completingId, payload: result.data as Record<string, unknown> },
        {
          onSuccess: () => {
            setCompletingId(null);
            setCompletionForm(emptyCompletionForm);
          },
        },
      );
    }
  };

  const toggleMonth = (key: keyof PmScheduleForm) => {
    setScheduleForm({ ...scheduleForm, [key]: !scheduleForm[key] });
  };

  if (isLoading) return <p className="muted">Loading PM schedules…</p>;
  if (error) return <p className="error">Error loading schedules: {error.message}</p>;

  return (
    <section className="page">
      <header className="page-header">
        <h1>Preventive Maintenance & Calibration</h1>
        <button
          className="btn btn-primary"
          onClick={() => {
            setShowForm(!showForm);
            setCompletingId(null);
          }}
        >
          {showForm ? "Cancel" : "New PM Schedule"}
        </button>
      </header>

      {showForm && (
        <div className="card">
          <h2>Create PM Schedule</h2>
          <div className="form-grid">
            <label htmlFor="pm-equipmentId">
              Equipment ID *
              {scheduleErrors.equipmentId && (
                <span className="error">{scheduleErrors.equipmentId}</span>
              )}
            </label>
            <input
              id="pm-equipmentId"
              type="number"
              value={scheduleForm.equipmentId || ""}
              onChange={(e) =>
                setScheduleForm({ ...scheduleForm, equipmentId: Number(e.target.value) })
              }
            />

            <label htmlFor="pm-scheduleCode">Schedule Code</label>
            <input
              id="pm-scheduleCode"
              value={scheduleForm.scheduleCode ?? ""}
              onChange={(e) => setScheduleForm({ ...scheduleForm, scheduleCode: e.target.value })}
            />

            <label htmlFor="pm-frequency">
              Frequency (months) *
              {scheduleErrors.frequency && (
                <span className="error">{scheduleErrors.frequency}</span>
              )}
            </label>
            <input
              id="pm-frequency"
              type="number"
              value={scheduleForm.frequency}
              onChange={(e) => setScheduleForm({ ...scheduleForm, frequency: Number(e.target.value) })}
            />

            <label htmlFor="pm-pmDate">PM Start Date</label>
            <input
              id="pm-pmDate"
              type="date"
              value={scheduleForm.pmDate ?? ""}
              onChange={(e) => setScheduleForm({ ...scheduleForm, pmDate: e.target.value })}
            />

            <label htmlFor="pm-pmType">Maintenance Type</label>
            <select
              id="pm-pmType"
              value={scheduleForm.pmType}
              onChange={(e) => setScheduleForm({ ...scheduleForm, pmType: Number(e.target.value) })}
            >
              <option value={1}>Preventive</option>
              <option value={2}>Corrective</option>
              <option value={3}>Calibration</option>
            </select>

            <label htmlFor="pm-storeId">Store ID</label>
            <input
              id="pm-storeId"
              type="number"
              value={scheduleForm.storeId ?? ""}
              onChange={(e) =>
                setScheduleForm({ ...scheduleForm, storeId: Number(e.target.value) || undefined })
              }
            />

            <label>Service Months</label>
            <div className="month-toggles">
              {MONTHS.map((m) => (
                <label key={m.key} className="month-toggle">
                  <input
                    type="checkbox"
                    checked={scheduleForm[m.key] as boolean}
                    onChange={() => toggleMonth(m.key)}
                  />
                  {m.label}
                </label>
              ))}
            </div>
          </div>
          <button className="btn btn-primary" onClick={handleCreate} disabled={create.isPending}>
            {create.isPending ? "Creating…" : "Create Schedule"}
          </button>
        </div>
      )}

      {completingId && (
        <div className="card">
          <h2>Record Completion — Schedule #{completingId}</h2>
          <div className="form-grid">
            <label htmlFor="pmc-maintenanceNo">Maintenance No.</label>
            <input
              id="pmc-maintenanceNo"
              value={completionForm.maintenanceNo ?? ""}
              onChange={(e) =>
                setCompletionForm({ ...completionForm, maintenanceNo: e.target.value })
              }
            />

            <label htmlFor="pmc-maintenanceDate">Maintenance Date</label>
            <input
              id="pmc-maintenanceDate"
              type="date"
              value={completionForm.maintenanceDate ?? ""}
              onChange={(e) =>
                setCompletionForm({ ...completionForm, maintenanceDate: e.target.value })
              }
            />

            <label htmlFor="pmc-performedBy">Performed By (Engineer ID)</label>
            <input
              id="pmc-performedBy"
              type="number"
              value={completionForm.performedBy ?? ""}
              onChange={(e) =>
                setCompletionForm({
                  ...completionForm,
                  performedBy: Number(e.target.value) || undefined,
                })
              }
            />

            <label htmlFor="pmc-functionalCheckRemark">Functional Check Remark</label>
            <textarea
              id="pmc-functionalCheckRemark"
              value={completionForm.functionalCheckRemark ?? ""}
              onChange={(e) =>
                setCompletionForm({ ...completionForm, functionalCheckRemark: e.target.value })
              }
            />

            <label htmlFor="pmc-preventiveMaintenanceRemark">Preventive Maintenance Remark</label>
            <textarea
              id="pmc-preventiveMaintenanceRemark"
              value={completionForm.preventiveMaintenanceRemark ?? ""}
              onChange={(e) =>
                setCompletionForm({
                  ...completionForm,
                  preventiveMaintenanceRemark: e.target.value,
                })
              }
            />

            <label htmlFor="pmc-electricalSafetyRemark">Electrical Safety Remark</label>
            <textarea
              id="pmc-electricalSafetyRemark"
              value={completionForm.electricalSafetyRemark ?? ""}
              onChange={(e) =>
                setCompletionForm({ ...completionForm, electricalSafetyRemark: e.target.value })
              }
            />

            <label htmlFor="pmc-calibration">Calibration</label>
            <textarea
              id="pmc-calibration"
              value={completionForm.calibration ?? ""}
              onChange={(e) => setCompletionForm({ ...completionForm, calibration: e.target.value })}
            />

            <label htmlFor="pmc-furtherActions">Further Actions</label>
            <textarea
              id="pmc-furtherActions"
              value={completionForm.furtherActions ?? ""}
              onChange={(e) =>
                setCompletionForm({ ...completionForm, furtherActions: e.target.value })
              }
            />

            <label htmlFor="pmc-commentsOnUse">Comments on Use</label>
            <textarea
              id="pmc-commentsOnUse"
              value={completionForm.commentsOnUse ?? ""}
              onChange={(e) =>
                setCompletionForm({ ...completionForm, commentsOnUse: e.target.value })
              }
            />

            <label htmlFor="pmc-remarks">Remarks</label>
            <textarea
              id="pmc-remarks"
              value={completionForm.remarks ?? ""}
              onChange={(e) => setCompletionForm({ ...completionForm, remarks: e.target.value })}
            />

            <label htmlFor="pmc-amountIncurred">Amount Incurred</label>
            <input
              id="pmc-amountIncurred"
              type="number"
              step="0.01"
              value={completionForm.amountIncurred ?? ""}
              onChange={(e) =>
                setCompletionForm({
                  ...completionForm,
                  amountIncurred: Number(e.target.value) || undefined,
                })
              }
            />

            <label htmlFor="pmc-verifiedBy">Verified By</label>
            <input
              id="pmc-verifiedBy"
              type="number"
              value={completionForm.verifiedBy ?? ""}
              onChange={(e) =>
                setCompletionForm({
                  ...completionForm,
                  verifiedBy: Number(e.target.value) || undefined,
                })
              }
            />

            <label htmlFor="pmc-serviceAgentId">Service Agent ID</label>
            <input
              id="pmc-serviceAgentId"
              type="number"
              value={completionForm.serviceAgentId ?? ""}
              onChange={(e) =>
                setCompletionForm({
                  ...completionForm,
                  serviceAgentId: Number(e.target.value) || undefined,
                })
              }
            />
          </div>
          <button
            className="btn btn-primary"
            onClick={handleComplete}
            disabled={complete.isPending}
          >
            {complete.isPending ? "Saving…" : "Record Completion"}
          </button>
        </div>
      )}

      <div className="card">
        <h2>PM Schedules</h2>
        <table className="table">
          <thead>
            <tr>
              <th>ID</th>
              <th>Schedule Code</th>
              <th>Equipment</th>
              <th>Type</th>
              <th>Frequency</th>
              <th>Next Due</th>
              <th>Last Completed</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {schedules.map((s) => (
              <tr key={s.id}>
                <td>{s.id}</td>
                <td>{s.scheduleCode ?? "—"}</td>
                <td>{s.equipmentId}</td>
                <td>
                  <span className="badge badge-info">
                    {MAINTENANCE_TYPE_LABELS[s.pmType] ?? s.pmType}
                  </span>
                </td>
                <td>{s.frequency} mo</td>
                <td>{s.nextDueDate ?? "—"}</td>
                <td>{s.lastCompletedDate ?? "—"}</td>
                <td>
                  <button
                    className="btn btn-sm"
                    onClick={() => setExpandedId(expandedId === s.id ? null : s.id)}
                  >
                    {expandedId === s.id ? "Hide" : "History"}
                  </button>{" "}
                  <button
                    className="btn btn-sm btn-success"
                    onClick={() => setCompletingId(s.id)}
                  >
                    Complete
                  </button>
                </td>
              </tr>
            ))}
            {schedules.length === 0 && (
              <tr>
                <td colSpan={8} className="muted">
                  No PM schedules.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>

      {expandedId && history && history.length > 0 && (
        <div className="card">
          <h2>Execution History — Schedule #{expandedId}</h2>
          <table className="table">
            <thead>
              <tr>
                <th>Maintenance No.</th>
                <th>Date</th>
                <th>Status</th>
                <th>Performed By</th>
                <th>Amount</th>
                <th>Calibration</th>
              </tr>
            </thead>
            <tbody>
              {history.map((h) => (
                <tr key={h.id}>
                  <td>{h.maintenanceNo ?? "—"}</td>
                  <td>{h.maintenanceDate ?? "—"}</td>
                  <td>
                    <span className={`badge ${STATUS_BADGE[h.pmStatus] ?? "badge-muted"}`}>
                      {PM_STATUS_LABELS[h.pmStatus] ?? h.pmStatus}
                    </span>
                  </td>
                  <td>{h.performedBy ?? "—"}</td>
                  <td>{h.amountIncurred != null ? `₹${h.amountIncurred}` : "—"}</td>
                  <td>{h.calibration ?? "—"}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {checklists.length > 0 && (
        <div className="card">
          <h2>PM Checklist Templates</h2>
          <table className="table">
            <thead>
              <tr>
                <th>Code</th>
                <th>Description</th>
              </tr>
            </thead>
            <tbody>
              {checklists.map((c) => (
                <tr key={c.id}>
                  <td>{c.code}</td>
                  <td>{c.description}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </section>
  );
}
