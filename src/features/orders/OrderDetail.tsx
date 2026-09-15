import { useState } from "react";
import { useParams } from "react-router-dom";
import {
  useClinicalOrder,
  useWorkflowSteps,
  useInvestigations,
  useExternalOrders,
  useUpdateOrderStatus,
} from "./queries";
import { orderStatusUpdateSchema, type OrderStatusUpdateForm } from "./zodSchema";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";

const STATUS_COLORS: Record<string, string> = {
  PENDING: "var(--color-warning)",
  IN_PROGRESS: "var(--color-primary)",
  COMPLETED: "var(--color-success)",
  CANCELLED: "var(--color-danger)",
  VERIFIED: "var(--color-success)",
  REJECTED: "var(--color-danger)",
};

const PRIORITY_BADGES: Record<string, string> = {
  ROUTINE: "badge--secondary",
  URGENT: "badge--warning",
  STAT: "badge--danger",
};

/**
 * OrderDetail — full view of a single clinical order with workflow steps,
 * investigations, external orders, and status management.
 */
export function OrderDetail() {
  const { id } = useParams<{ id: string }>();
  const orderId = Number(id);

  const { data: order, isLoading: orderLoading } = useClinicalOrder(orderId);
  const { data: workflowSteps = [] } = useWorkflowSteps(orderId);
  const { data: investigations = [] } = useInvestigations(orderId);
  const { data: externalOrders = [] } = useExternalOrders(orderId);
  const updateStatus = useUpdateOrderStatus();

  const [activeTab, setActiveTab] = useState<"workflow" | "investigations" | "external">("workflow");

  const statusForm = useForm<OrderStatusUpdateForm>({
    resolver: zodResolver(orderStatusUpdateSchema),
    defaultValues: { status: order?.status as any, remarks: "" },
  });

  if (orderLoading) return <p className="muted">Loading order…</p>;
  if (!order) return <p className="muted">Order not found.</p>;

  const handleStatusUpdate = async (values: OrderStatusUpdateForm) => {
    await updateStatus.mutateAsync({ id: orderId, status: values.status });
  };

  return (
    <section className="page">
      <header className="page__header">
        <div>
          <h1>Order #{order.id}</h1>
          <p className="muted">
            {order.patientName} · {order.orderType} · {order.priority}
          </p>
        </div>
        <div className="page__actions">
          <span
            className="badge"
            style={{ backgroundColor: STATUS_COLORS[order.status] ?? "var(--color-border)", color: "white" }}
          >
            {order.status}
          </span>
        </div>
      </header>

      {/* Order summary cards */}
      <div className="card-grid">
        <div className="card">
          <label>Patient</label>
          <strong>{order.patientName}</strong>
        </div>
        <div className="card">
          <label>Type</label>
          <strong>{order.orderType}</strong>
        </div>
        <div className="card">
          <label>Priority</label>
          <strong className={`badge ${PRIORITY_BADGES[order.priority] ?? ""}`}>
            {order.priority}
          </strong>
        </div>
        <div className="card">
          <label>Ordered By</label>
          <strong>{order.orderedBy}</strong>
        </div>
        <div className="card">
          <label>Ordered At</label>
          <strong>{new Date(order.orderedAt).toLocaleString()}</strong>
        </div>
      </div>

      {/* Description */}
      <div className="card" style={{ marginTop: "16px" }}>
        <label>Description</label>
        <p>{order.description}</p>
      </div>

      {/* Status update */}
      <div className="card" style={{ marginTop: "16px" }}>
        <h3>Update Status</h3>
        <form className="inline-form" onSubmit={statusForm.handleSubmit(handleStatusUpdate)}>
          <select {...statusForm.register("status")}>
            <option value="PENDING">Pending</option>
            <option value="IN_PROGRESS">In Progress</option>
            <option value="COMPLETED">Completed</option>
            <option value="CANCELLED">Cancelled</option>
            <option value="VERIFIED">Verified</option>
            <option value="REJECTED">Rejected</option>
          </select>
          <input placeholder="Remarks (optional)" {...statusForm.register("remarks")} />
          <button type="submit" className="btn btn--primary" disabled={updateStatus.isPending}>
            Update
          </button>
        </form>
      </div>

      {/* Tabs */}
      <div className="tabs" role="tablist" style={{ marginTop: "16px" }}>
        <button
          role="tab"
          aria-selected={activeTab === "workflow"}
          className={`tab${activeTab === "workflow" ? " tab--active" : ""}`}
          onClick={() => setActiveTab("workflow")}
        >
          Workflow ({workflowSteps.length})
        </button>
        <button
          role="tab"
          aria-selected={activeTab === "investigations"}
          className={`tab${activeTab === "investigations" ? " tab--active" : ""}`}
          onClick={() => setActiveTab("investigations")}
        >
          Investigations ({investigations.length})
        </button>
        <button
          role="tab"
          aria-selected={activeTab === "external"}
          className={`tab${activeTab === "external" ? " tab--active" : ""}`}
          onClick={() => setActiveTab("external")}
        >
          External ({externalOrders.length})
        </button>
      </div>

      {/* Tab content */}
      {activeTab === "workflow" && (
        <WorkflowTab steps={workflowSteps} orderId={orderId} />
      )}
      {activeTab === "investigations" && (
        <InvestigationsTab investigations={investigations} />
      )}
      {activeTab === "external" && (
        <ExternalTab externalOrders={externalOrders} />
      )}
    </section>
  );
}

// ---- WorkflowTab -----------------------------------------------------------

function WorkflowTab({ steps, orderId }: { steps: any[]; orderId: number }) {
  if (steps.length === 0) {
    return <p className="muted">No workflow steps for this order.</p>;
  }

  const sorted = [...steps].sort((a, b) => a.stepOrder - b.stepOrder);

  return (
    <ol className="workflow-runtime__steps">
      {sorted.map((step, idx) => (
        <li key={step.stepId} className="workflow-runtime__step">
          <div className="workflow-runtime__indicator">
            <span
              className="workflow-runtime__dot"
              style={{ backgroundColor: STATUS_COLORS[step.status] ?? "var(--color-border)" }}
            />
            {idx < sorted.length - 1 && <span className="workflow-runtime__line" />}
          </div>
          <div className="workflow-runtime__content">
            <div className="workflow-runtime__header">
              <span className="workflow-runtime__name">{step.stepName}</span>
              <span className="badge">{step.status}</span>
            </div>
            <div className="workflow-runtime__meta">
              <span>Role: {step.role}</span>
              {step.assignedTo && <span> · Assigned: {step.assignedTo}</span>}
              {step.completedAt && <span> · Done: {new Date(step.completedAt).toLocaleString()}</span>}
            </div>
            {step.remarks && <p className="workflow-runtime__remarks">{step.remarks}</p>}
          </div>
        </li>
      ))}
    </ol>
  );
}

// ---- InvestigationsTab ----------------------------------------------------

function InvestigationsTab({ investigations }: { investigations: any[] }) {
  if (investigations.length === 0) {
    return <p className="muted">No investigations for this order.</p>;
  }

  return (
    <table className="table">
      <thead>
        <tr>
          <th>Service</th>
          <th>Status</th>
          <th>Priority</th>
          <th>Result</th>
          <th>Range</th>
          <th>Critical</th>
        </tr>
      </thead>
      <tbody>
        {investigations.map((inv) => (
          <tr key={inv.id}>
            <td>{inv.serviceName}</td>
            <td>
              <span
                className="badge"
                style={{ backgroundColor: STATUS_COLORS[inv.status] ?? "var(--color-border)", color: "white" }}
              >
                {inv.status}
              </span>
            </td>
            <td>{inv.priority}</td>
            <td>
              {inv.resultValue ? `${inv.resultValue} ${inv.unit ?? ""}` : "—"}
            </td>
            <td>{inv.referenceRange ?? "—"}</td>
            <td>{inv.isCritical ? <span className="badge badge--danger">CRITICAL</span> : "—"}</td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}

// ---- ExternalTab -----------------------------------------------------------

function ExternalTab({ externalOrders }: { externalOrders: any[] }) {
  if (externalOrders.length === 0) {
    return <p className="muted">No external orders for this order.</p>;
  }

  return (
    <table className="table">
      <thead>
        <tr>
          <th>Lab</th>
          <th>Test</th>
          <th>Sample</th>
          <th>Status</th>
          <th>Instructions</th>
        </tr>
      </thead>
      <tbody>
        {externalOrders.map((ext) => (
          <tr key={ext.id}>
            <td>{ext.externalLabName}</td>
            <td>{ext.testName}</td>
            <td>{ext.sampleType ?? "—"}</td>
            <td>
              <span
                className="badge"
                style={{ backgroundColor: STATUS_COLORS[ext.status] ?? "var(--color-border)", color: "white" }}
              >
                {ext.status}
              </span>
            </td>
            <td>{ext.instructions ?? "—"}</td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}
