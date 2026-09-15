import { useState } from "react";
import {
  useComplaints,
  useRaiseComplaint,
  useComplaint,
  useAddResolution,
  EquipmentComplaint,
  EquipmentComplaintResolution,
} from "./queries";
import {
  equipmentComplaintSchema,
  complaintResolutionWithDetailsSchema,
  type EquipmentComplaintForm,
  type ComplaintResolutionWithDetailsForm,
} from "./zodSchema";

const COMPLAINT_STATUS_LABELS: Record<number, string> = {
  1: "Open",
  2: "In Progress",
  3: "Resolved",
  4: "Closed",
};

const STATUS_BADGE: Record<number, string> = {
  1: "badge-danger",
  2: "badge-warning",
  3: "badge-success",
  4: "badge-muted",
};

const KANBAN_COLUMNS = [
  { status: 1, label: "Open", color: "kanban-open" },
  { status: 2, label: "In Progress", color: "kanban-progress" },
  { status: 3, label: "Resolved", color: "kanban-resolved" },
  { status: 4, label: "Closed", color: "kanban-closed" },
];

const emptyComplaintForm: EquipmentComplaintForm = {
  equipmentId: 0,
  categoryId: undefined,
  complaintTypeId: undefined,
  severityId: undefined,
  description: "",
  reportedBy: undefined,
  reportedDate: undefined,
  roomNo: undefined,
  extensionNo: undefined,
  assignedTo: undefined,
  expectedResolutionDate: undefined,
};

const emptyResolutionForm: ComplaintResolutionWithDetailsForm = {
  resolutionNotes: "",
  partsReplaced: undefined,
  serviceAgentId: undefined,
  assignedTo: undefined,
  assignedById: undefined,
  resolvedStatus: 3,
  resolutionDate: undefined,
  details: undefined,
};

export function ComplaintTracker() {
  const { data: complaints = [], isLoading, error } = useComplaints();
  const raise = useRaiseComplaint();
  const addResolution = useAddResolution();

  const [showForm, setShowForm] = useState(false);
  const [showKanban, setShowKanban] = useState(false);
  const [complaintForm, setComplaintForm] = useState<EquipmentComplaintForm>(emptyComplaintForm);
  const [complaintErrors, setComplaintErrors] = useState<Record<string, string>>({});

  const [resolvingId, setResolvingId] = useState<number | null>(null);
  const [resolutionForm, setResolutionForm] =
    useState<ComplaintResolutionWithDetailsForm>(emptyResolutionForm);
  const [resolutionErrors, setResolutionErrors] = useState<Record<string, string>>({});

  const [expandedId, setExpandedId] = useState<number | null>(null);
  const { data: complaintDetail } = useComplaint(expandedId ?? 0);

  const handleRaise = () => {
    const result = equipmentComplaintSchema.safeParse(complaintForm);
    if (!result.success) {
      const fieldErrors: Record<string, string> = {};
      result.error.errors.forEach((e) => {
        fieldErrors[e.path[0] as string] = e.message;
      });
      setComplaintErrors(fieldErrors);
      return;
    }
    raise.mutate(result.data as Record<string, unknown>, {
      onSuccess: () => {
        setShowForm(false);
        setComplaintForm(emptyComplaintForm);
        setComplaintErrors({});
      },
    });
  };

  const handleResolve = () => {
    const result = complaintResolutionWithDetailsSchema.safeParse(resolutionForm);
    if (!result.success) {
      const fieldErrors: Record<string, string> = {};
      result.error.errors.forEach((e) => {
        fieldErrors[e.path[0] as string] = e.message;
      });
      setResolutionErrors(fieldErrors);
      return;
    }
    if (resolvingId) {
      addResolution.mutate(
        { complaintId: resolvingId, payload: result.data as Record<string, unknown> },
        {
          onSuccess: () => {
            setResolvingId(null);
            setResolutionForm(emptyResolutionForm);
            setResolutionErrors({});
          },
        },
      );
    }
  };

  const complaintsByStatus = (status: number) =>
    complaints.filter((c) => c.complaintStatus === status);

  if (isLoading) return <p className="muted">Loading complaints…</p>;
  if (error) return <p className="error">Error loading complaints: {error.message}</p>;

  return (
    <section className="page">
      <header className="page-header">
        <h1>Complaint Tracker</h1>
        <div className="header-actions">
          <button
            className={`btn ${showKanban ? "btn-secondary" : "btn-outline"}`}
            onClick={() => setShowKanban(!showKanban)}
          >
            {showKanban ? "Table View" : "Kanban View"}
          </button>
          <button
            className="btn btn-primary"
            onClick={() => {
              setShowForm(!showForm);
              setResolvingId(null);
            }}
          >
            {showForm ? "Cancel" : "Raise Complaint"}
          </button>
        </div>
      </header>

      {showForm && (
        <div className="card">
          <h2>Raise New Complaint</h2>
          <div className="form-grid">
            <label htmlFor="cmp-equipmentId">
              Equipment ID *
              {complaintErrors.equipmentId && (
                <span className="error">{complaintErrors.equipmentId}</span>
              )}
            </label>
            <input
              id="cmp-equipmentId"
              type="number"
              value={complaintForm.equipmentId || ""}
              onChange={(e) =>
                setComplaintForm({ ...complaintForm, equipmentId: Number(e.target.value) })
              }
            />

            <label htmlFor="cmp-categoryId">Category ID</label>
            <input
              id="cmp-categoryId"
              type="number"
              value={complaintForm.categoryId ?? ""}
              onChange={(e) =>
                setComplaintForm({
                  ...complaintForm,
                  categoryId: Number(e.target.value) || undefined,
                })
              }
            />

            <label htmlFor="cmp-complaintTypeId">Complaint Type ID</label>
            <input
              id="cmp-complaintTypeId"
              type="number"
              value={complaintForm.complaintTypeId ?? ""}
              onChange={(e) =>
                setComplaintForm({
                  ...complaintForm,
                  complaintTypeId: Number(e.target.value) || undefined,
                })
              }
            />

            <label htmlFor="cmp-severityId">Severity ID</label>
            <input
              id="cmp-severityId"
              type="number"
              value={complaintForm.severityId ?? ""}
              onChange={(e) =>
                setComplaintForm({
                  ...complaintForm,
                  severityId: Number(e.target.value) || undefined,
                })
              }
            />

            <label htmlFor="cmp-description">
              Description *
              {complaintErrors.description && (
                <span className="error">{complaintErrors.description}</span>
              )}
            </label>
            <textarea
              id="cmp-description"
              value={complaintForm.description}
              onChange={(e) => setComplaintForm({ ...complaintForm, description: e.target.value })}
            />

            <label htmlFor="cmp-reportedBy">Reported By (User ID)</label>
            <input
              id="cmp-reportedBy"
              type="number"
              value={complaintForm.reportedBy ?? ""}
              onChange={(e) =>
                setComplaintForm({
                  ...complaintForm,
                  reportedBy: Number(e.target.value) || undefined,
                })
              }
            />

            <label htmlFor="cmp-roomNo">Room No.</label>
            <input
              id="cmp-roomNo"
              value={complaintForm.roomNo ?? ""}
              onChange={(e) => setComplaintForm({ ...complaintForm, roomNo: e.target.value })}
            />

            <label htmlFor="cmp-extensionNo">Extension No.</label>
            <input
              id="cmp-extensionNo"
              value={complaintForm.extensionNo ?? ""}
              onChange={(e) => setComplaintForm({ ...complaintForm, extensionNo: e.target.value })}
            />

            <label htmlFor="cmp-assignedTo">Assigned To (User ID)</label>
            <input
              id="cmp-assignedTo"
              type="number"
              value={complaintForm.assignedTo ?? ""}
              onChange={(e) =>
                setComplaintForm({
                  ...complaintForm,
                  assignedTo: Number(e.target.value) || undefined,
                })
              }
            />

            <label htmlFor="cmp-expectedResolutionDate">Expected Resolution Date</label>
            <input
              id="cmp-expectedResolutionDate"
              type="date"
              value={complaintForm.expectedResolutionDate ?? ""}
              onChange={(e) =>
                setComplaintForm({
                  ...complaintForm,
                  expectedResolutionDate: e.target.value,
                })
              }
            />
          </div>
          <button className="btn btn-primary" onClick={handleRaise} disabled={raise.isPending}>
            {raise.isPending ? "Raising…" : "Submit Complaint"}
          </button>
        </div>
      )}

      {resolvingId && (
        <div className="card">
          <h2>Resolve Complaint #{resolvingId}</h2>
          <div className="form-grid">
            <label htmlFor="res-resolutionNotes">
              Resolution Notes *
              {resolutionErrors.resolutionNotes && (
                <span className="error">{resolutionErrors.resolutionNotes}</span>
              )}
            </label>
            <textarea
              id="res-resolutionNotes"
              value={resolutionForm.resolutionNotes}
              onChange={(e) =>
                setResolutionForm({ ...resolutionForm, resolutionNotes: e.target.value })
              }
            />

            <label htmlFor="res-partsReplaced">Parts Replaced</label>
            <textarea
              id="res-partsReplaced"
              value={resolutionForm.partsReplaced ?? ""}
              onChange={(e) =>
                setResolutionForm({ ...resolutionForm, partsReplaced: e.target.value })
              }
            />

            <label htmlFor="res-serviceAgentId">Service Agent ID</label>
            <input
              id="res-serviceAgentId"
              type="number"
              value={resolutionForm.serviceAgentId ?? ""}
              onChange={(e) =>
                setResolutionForm({
                  ...resolutionForm,
                  serviceAgentId: Number(e.target.value) || undefined,
                })
              }
            />

            <label htmlFor="res-assignedTo">Assigned To</label>
            <input
              id="res-assignedTo"
              type="number"
              value={resolutionForm.assignedTo ?? ""}
              onChange={(e) =>
                setResolutionForm({
                  ...resolutionForm,
                  assignedTo: Number(e.target.value) || undefined,
                })
              }
            />

            <label htmlFor="res-resolvedStatus">Resolved Status</label>
            <select
              id="res-resolvedStatus"
              value={resolutionForm.resolvedStatus}
              onChange={(e) =>
                setResolutionForm({ ...resolutionForm, resolvedStatus: Number(e.target.value) })
              }
            >
              <option value={1}>Open</option>
              <option value={2}>In Progress</option>
              <option value={3}>Resolved</option>
              <option value={4}>Closed</option>
            </select>

            <label htmlFor="res-resolutionDate">Resolution Date</label>
            <input
              id="res-resolutionDate"
              type="date"
              value={resolutionForm.resolutionDate ?? ""}
              onChange={(e) =>
                setResolutionForm({ ...resolutionForm, resolutionDate: e.target.value })
              }
            />
          </div>
          <button
            className="btn btn-primary"
            onClick={handleResolve}
            disabled={addResolution.isPending}
          >
            {addResolution.isPending ? "Saving…" : "Record Resolution"}
          </button>
        </div>
      )}

      {showKanban ? (
        <div className="kanban-board">
          {KANBAN_COLUMNS.map((col) => (
            <div key={col.status} className={`kanban-column ${col.color}`}>
              <div className="kanban-column-header">
                <span>{col.label}</span>
                <span className="kanban-count">{complaintsByStatus(col.status).length}</span>
              </div>
              <div className="kanban-cards">
                {complaintsByStatus(col.status).map((c) => (
                  <ComplaintCard
                    key={c.id}
                    complaint={c}
                    onExpand={() => setExpandedId(expandedId === c.id ? null : c.id)}
                    onResolve={() => setResolvingId(c.id)}
                    expanded={expandedId === c.id}
                    detail={expandedId === c.id ? complaintDetail : undefined}
                  />
                ))}
              </div>
            </div>
          ))}
        </div>
      ) : (
        <div className="card">
          <table className="table">
            <thead>
              <tr>
                <th>ID</th>
                <th>Complaint No.</th>
                <th>Equipment</th>
                <th>Description</th>
                <th>Status</th>
                <th>Reported</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {complaints.map((c) => (
                <tr key={c.id}>
                  <td>{c.id}</td>
                  <td>{c.complaintNo ?? "—"}</td>
                  <td>{c.equipmentId}</td>
                  <td>{c.description}</td>
                  <td>
                    <span
                      className={`badge ${STATUS_BADGE[c.complaintStatus] ?? "badge-muted"}`}
                    >
                      {COMPLAINT_STATUS_LABELS[c.complaintStatus] ?? c.complaintStatus}
                    </span>
                  </td>
                  <td>{c.reportedDate ?? "—"}</td>
                  <td>
                    <button
                      className="btn btn-sm"
                      onClick={() => setExpandedId(expandedId === c.id ? null : c.id)}
                    >
                      {expandedId === c.id ? "Hide" : "Detail"}
                    </button>{" "}
                    {c.complaintStatus < 3 && (
                      <button className="btn btn-sm btn-success" onClick={() => setResolvingId(c.id)}>
                        Resolve
                      </button>
                    )}
                  </td>
                </tr>
              ))}
              {complaints.length === 0 && (
                <tr>
                  <td colSpan={7} className="muted">
                    No complaints.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      )}

      {expandedId && !showKanban && complaintDetail && (
        <div className="card">
          <h2>Complaint #{expandedId} Details</h2>
          <p>
            <strong>Description:</strong> {complaintDetail.complaint.description}
          </p>
          <p>
            <strong>Accession:</strong> {complaintDetail.complaint.accessionNo ?? "—"}
          </p>
          {complaintDetail.resolutions.length > 0 && (
            <>
              <h3>Resolutions</h3>
              <table className="table">
                <thead>
                  <tr>
                    <th>Date</th>
                    <th>Notes</th>
                    <th>Parts Replaced</th>
                    <th>Status</th>
                  </tr>
                </thead>
                <tbody>
                  {complaintDetail.resolutions.map((r) => (
                    <tr key={r.id}>
                      <td>{r.resolutionDate ?? "—"}</td>
                      <td>{r.resolutionNotes}</td>
                      <td>{r.partsReplaced ?? "—"}</td>
                      <td>
                        <span className="badge badge-success">
                          {COMPLAINT_STATUS_LABELS[r.resolvedStatus] ?? r.resolvedStatus}
                        </span>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </>
          )}
        </div>
      )}
    </section>
  );
}

function ComplaintCard({
  complaint,
  onExpand,
  onResolve,
  expanded,
  detail,
}: {
  complaint: EquipmentComplaint;
  onExpand: () => void;
  onResolve: () => void;
  expanded: boolean;
  detail?: { resolutions: EquipmentComplaintResolution[] };
}) {
  return (
    <div className="kanban-card">
      <div className="kanban-card-header">
        <span className="kanban-card-id">#{complaint.id}</span>
        {complaint.complaintNo && (
          <span className="kanban-card-no">{complaint.complaintNo}</span>
        )}
      </div>
      <div className="kanban-card-body">
        <p className="kanban-card-desc">{complaint.description}</p>
        <p className="kanban-card-meta">Eq: {complaint.equipmentId}</p>
        {complaint.accessionNo && (
          <p className="kanban-card-meta">Acc: {complaint.accessionNo}</p>
        )}
      </div>
      <div className="kanban-card-actions">
        <button className="btn btn-sm" onClick={onExpand}>
          {expanded ? "Hide" : "View"}
        </button>
        {complaint.complaintStatus < 3 && (
          <button className="btn btn-sm btn-success" onClick={onResolve}>
            Resolve
          </button>
        )}
      </div>
      {expanded && detail && detail.resolutions.length > 0 && (
        <div className="kanban-card-detail">
          <small>
            {detail.resolutions.length} resolution(s) recorded
          </small>
        </div>
      )}
    </div>
  );
}
