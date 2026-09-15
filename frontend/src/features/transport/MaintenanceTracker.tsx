import { useState } from "react";
import {
  useVehicles,
  useMaintenanceRequests,
  useCreateMaintenanceRequest,
  useApproveMaintenanceRequest,
} from "./queries";
import {
  maintenanceRequestCreateSchema,
  maintenanceApprovalSchema,
} from "./zodSchema";
import type { MaintenanceRequestCreateForm, MaintenanceApprovalForm } from "./zodSchema";
import { StatusIndicator } from "./StatusIndicator";

const REQUEST_TYPES = ["ROUTINE", "REPAIR", "INSPECTION", "EMERGENCY"] as const;

const emptyRequestForm: MaintenanceRequestCreateForm = {
  vehicleId: 0,
  requestType: "ROUTINE",
  description: "",
  estimatedCost: undefined,
  requestedBy: "",
};

export function MaintenanceTracker() {
  const { data: vehicles = [], isLoading: vehiclesLoading } = useVehicles();
  const { data: requests = [], isLoading: requestsLoading, isError } = useMaintenanceRequests();
  const createRequest = useCreateMaintenanceRequest();
  const approveRequest = useApproveMaintenanceRequest();

  const [showForm, setShowForm] = useState(false);
  const [form, setForm] = useState<MaintenanceRequestCreateForm>(emptyRequestForm);
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [approvalForm, setApprovalForm] = useState<MaintenanceApprovalForm>({
    id: 0,
    status: "APPROVED",
    approvedBy: "",
    remarks: "",
  });

  const handleCreateRequest = () => {
    const result = maintenanceRequestCreateSchema.safeParse(form);
    if (!result.success) {
      const fieldErrors: Record<string, string> = {};
      result.error.errors.forEach((e) => { fieldErrors[e.path[0] as string] = e.message; });
      setErrors(fieldErrors);
      return;
    }
    createRequest.mutate(result.data, {
      onSuccess: () => { setShowForm(false); setForm(emptyRequestForm); setErrors({}); },
    });
  };

  const handleApprove = (requestId: number, status: "APPROVED" | "REJECTED") => {
    const payload: MaintenanceApprovalForm = {
      ...approvalForm,
      id: requestId,
      status,
    };
    const result = maintenanceApprovalSchema.safeParse(payload);
    if (!result.success) return;
    approveRequest.mutate(result.data, {
      onSuccess: () => setApprovalForm({ id: 0, status: "APPROVED", approvedBy: "", remarks: "" }),
    });
  };

  const canApprove = (status: string) => status === "PENDING";

  if (vehiclesLoading || requestsLoading) {
    return <div className="loading-state"><div className="spinner" />Loading maintenance data…</div>;
  }

  return (
    <section className="page">
      <header className="page-header">
        <div>
          <h1>Maintenance Tracker</h1>
          <p className="muted">Vehicle maintenance requests &amp; approvals</p>
        </div>
        <button className="btn btn-primary" onClick={() => setShowForm(!showForm)}>
          {showForm ? "Cancel" : "New Request"}
        </button>
      </header>

      {showForm && (
        <div className="card">
          <h2>New Maintenance Request</h2>
          <div className="form-grid">
            <div className="field">
              <label>Vehicle <span className="req">*</span></label>
              <select
                value={form.vehicleId || ""}
                onChange={(e) => setForm({ ...form, vehicleId: Number(e.target.value) })}
              >
                <option value="">Select vehicle…</option>
                {vehicles.map((v) => (
                  <option key={v.id} value={v.id}>{v.vehicleNumber} ({v.vehicleType})</option>
                ))}
              </select>
              {errors.vehicleId && <span className="field-error">{errors.vehicleId}</span>}
            </div>
            <div className="field">
              <label>Request Type <span className="req">*</span></label>
              <select
                value={form.requestType}
                onChange={(e) => setForm({ ...form, requestType: e.target.value as MaintenanceRequestCreateForm["requestType"] })}
              >
                {REQUEST_TYPES.map((t) => (
                  <option key={t} value={t}>{t}</option>
                ))}
              </select>
            </div>
            <div className="field">
              <label>Estimated Cost (₹)</label>
              <input
                type="number"
                step="0.01"
                value={form.estimatedCost ?? ""}
                onChange={(e) => setForm({ ...form, estimatedCost: Number(e.target.value) || undefined })}
                placeholder="Optional"
              />
            </div>
            <div className="field">
              <label>Description <span className="req">*</span></label>
              <input
                value={form.description}
                onChange={(e) => setForm({ ...form, description: e.target.value })}
                placeholder="Describe the issue"
              />
              {errors.description && <span className="field-error">{errors.description}</span>}
            </div>
            <div className="field">
              <label>Requested By <span className="req">*</span></label>
              <input
                value={form.requestedBy}
                onChange={(e) => setForm({ ...form, requestedBy: e.target.value })}
                placeholder="Your name"
              />
              {errors.requestedBy && <span className="field-error">{errors.requestedBy}</span>}
            </div>
            <div className="form-actions">
              <button className="btn btn-primary" onClick={handleCreateRequest} disabled={createRequest.isPending}>
                {createRequest.isPending ? "Submitting…" : "Submit Request"}
              </button>
            </div>
          </div>
        </div>
      )}

      <div className="card">
        <h2>Maintenance Requests</h2>
        {isError && <div className="alert alert--error">Failed to load maintenance requests.</div>}
        <table className="data-table">
          <thead>
            <tr>
              <th>ID</th>
              <th>Vehicle</th>
              <th>Type</th>
              <th>Description</th>
              <th>Cost</th>
              <th>Requested By</th>
              <th>Status</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {requests.map((req) => (
              <tr key={req.id}>
                <td>#{req.id}</td>
                <td>{req.vehicleNumber ?? `Vehicle #${req.vehicleId}`}</td>
                <td>{req.requestType}</td>
                <td>{req.description}</td>
                <td>{req.estimatedCost ? `₹${req.estimatedCost}` : "—"}</td>
                <td>{req.requestedBy}</td>
                <td><StatusIndicator status={req.status} /></td>
                <td>
                  {canApprove(req.status) && (
                    <div className="tn-actions">
                      <button
                        className="btn btn-sm btn-secondary"
                        onClick={() => handleApprove(req.id, "APPROVED")}
                        disabled={approveRequest.isPending}
                      >
                        Approve
                      </button>
                      <button
                        className="btn btn-sm btn-secondary"
                        onClick={() => handleApprove(req.id, "REJECTED")}
                        disabled={approveRequest.isPending}
                      >
                        Reject
                      </button>
                    </div>
                  )}
                </td>
              </tr>
            ))}
            {requests.length === 0 && (
              <tr><td colSpan={8} className="muted">No maintenance requests.</td></tr>
            )}
          </tbody>
        </table>
      </div>

      {approvalForm.id > 0 && (
        <div className="card">
          <h3>Approve / Reject Request #{approvalForm.id}</h3>
          <div className="form-grid">
            <div className="field">
              <label>Approver Name <span className="req">*</span></label>
              <input
                value={approvalForm.approvedBy}
                onChange={(e) => setApprovalForm({ ...approvalForm, approvedBy: e.target.value })}
                placeholder="Your name"
              />
            </div>
            <div className="field">
              <label>Remarks</label>
              <input
                value={approvalForm.remarks || ""}
                onChange={(e) => setApprovalForm({ ...approvalForm, remarks: e.target.value })}
                placeholder="Optional remarks"
              />
            </div>
          </div>
        </div>
      )}
    </section>
  );
}
