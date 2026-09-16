import { useState, useCallback } from "react";
import { useForm, useFieldArray } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import {
  useCwfDefinitions,
  useScwfDefinitions,
  useCreateCwfDefinition,
  useUpdateCwfDefinition,
  useCreateScwfDefinition,
  useUpdateScwfDefinition,
  useWorkflowSteps,
  useAdvanceWorkflowStep,
} from "./queries";
import {
  cwfDefinitionSchema,
  scwfDefinitionSchema,
  type CwfDefinitionForm,
  type ScwfDefinitionForm,
  type CwfStep,
} from "./zodSchema";

const SERVICE_TYPES = [
  "CONSULT",
  "LAB",
  "RADIOLOGY",
  "NURSING",
  "PROCEDURE",
  "CONSUMABLES",
  "MISCELLANEOUS",
] as const;

const ROLES = ["DOCTOR", "NURSE", "LAB_TECH", "RAD_TECH", "PHARMACIST"] as const;

const STEP_STATUS_COLORS: Record<string, string> = {
  PENDING: "var(--color-border)",
  IN_PROGRESS: "var(--color-primary)",
  COMPLETED: "var(--color-success)",
  SKIPPED: "var(--color-text-muted)",
  REJECTED: "var(--color-danger)",
};

/**
 * ClinicalWorkflow — manage CWF/SCWF multi-step approval workflows.
 * Linear-style step visualization: vertical stepper with status badges,
 * role assignments, and advance/reject actions.
 */
export function ClinicalWorkflow() {
  const [activeTab, setActiveTab] = useState<"cwf" | "scwf">("cwf");
  const [showForm, setShowForm] = useState(false);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [selectedOrderId, setSelectedOrderId] = useState<number | null>(null);

  const { data: cwfDefs = [], isLoading: cwfLoading } = useCwfDefinitions();
  const { data: scwfDefs = [], isLoading: scwfLoading } = useScwfDefinitions();
  const createCwf = useCreateCwfDefinition();
  const updateCwf = useUpdateCwfDefinition();
  const createScwf = useCreateScwfDefinition();
  const updateScwf = useUpdateScwfDefinition();

  const isLoading = activeTab === "cwf" ? cwfLoading : scwfLoading;
  const definitions = activeTab === "cwf" ? cwfDefs : scwfDefs;

  const handleNew = useCallback(() => {
    setEditingId(null);
    setShowForm(true);
  }, []);

  const handleEdit = useCallback((id: number) => {
    setEditingId(id);
    setShowForm(true);
  }, []);

  return (
    <section className="page">
      <header className="page-header">
        <div>
          <h1>Clinical Workflows</h1>
          <p className="muted">
            Define CWF/SCWF multi-step approval chains for service orders.
          </p>
        </div>
        <button
          type="button"
          className="btn btn--primary"
          onClick={handleNew}
        >
          + New Workflow
        </button>
      </header>

      {/* Tabs */}
      <div className="tabs" role="tablist">
        <button
          role="tab"
          aria-selected={activeTab === "cwf"}
          className={`tab${activeTab === "cwf" ? " tab--active" : ""}`}
          onClick={() => setActiveTab("cwf")}
        >
          CWF ({cwfDefs.length})
        </button>
        <button
          role="tab"
          aria-selected={activeTab === "scwf"}
          className={`tab${activeTab === "scwf" ? " tab--active" : ""}`}
          onClick={() => setActiveTab("scwf")}
        >
          SCWF ({scwfDefs.length})
        </button>
      </div>

      {/* Workflow list */}
      <section className="clinical-workflow__list" aria-label="Workflow definitions">
        {isLoading ? (
          <div className="loading-state">
            <span className="spinner" aria-hidden="true" /> Loading…
          </div>
        ) : definitions.length === 0 ? (
          <p className="muted">No {activeTab.toUpperCase()} definitions configured.</p>
        ) : (
          definitions.map((def) => {
            const defId = "cwfId" in def ? def.cwfId : def.scwfId;
            const defName = "cwfName" in def ? def.cwfName : def.scwfName;
            return (
              <article key={defId} className="workflow-card">
                <div className="workflow-card__header">
                  <h3>{defName}</h3>
                  <span
                    className={`workflow-card__status workflow-card__status--${def.status.toLowerCase()}`}
                  >
                    {def.status}
                  </span>
                </div>
              <div className="workflow-card__meta">
                <span className="badge">{def.serviceType}</span>
                {"condition" in def && (
                  <span className="muted">Condition: {def.condition}</span>
                )}
              </div>

              {/* Step preview — Linear-style vertical stepper */}
              <ol className="workflow-steps">
                {def.steps
                  .sort((a, b) => a.stepOrder - b.stepOrder)
                  .map((step, idx) => (
                    <li key={step.stepId} className="workflow-step">
                      <div className="workflow-step__indicator">
                        <span className="workflow-step__dot">{idx + 1}</span>
                        {idx < def.steps.length - 1 && (
                          <span className="workflow-step__line" />
                        )}
                      </div>
                      <div className="workflow-step__content">
                        <span className="workflow-step__name">{step.stepName}</span>
                        <span className="workflow-step__role badge">{step.role}</span>
                        {step.isMandatory && (
                          <span className="workflow-step__mandatory">Required</span>
                        )}
                      </div>
                    </li>
                  ))}
              </ol>

              <div className="workflow-card__actions">
                <button
                  type="button"
                  className="btn btn--ghost btn--sm"
                  onClick={() => handleEdit(defId)}
                >
                  Edit
                </button>
              </div>
            </article>
            );
          })
        )}
      </section>

      {/* Workflow runtime viewer */}
      {selectedOrderId && (
        <WorkflowRuntime
          orderId={selectedOrderId}
          onClose={() => setSelectedOrderId(null)}
        />
      )}

      {/* Form slideover */}
      {showForm && (
        <WorkflowForm
          editingId={editingId}
          isScwf={activeTab === "scwf"}
          onSubmit={async (values: CwfDefinitionForm | ScwfDefinitionForm) => {
            try {
              if (activeTab === "cwf") {
                const v = values as CwfDefinitionForm;
                if (editingId) {
                  await updateCwf.mutateAsync({ id: editingId, payload: v });
                } else {
                  await createCwf.mutateAsync(v);
                }
              } else {
                const v = values as ScwfDefinitionForm;
                if (editingId) {
                  await updateScwf.mutateAsync({ id: editingId, payload: v });
                } else {
                  await createScwf.mutateAsync(v);
                }
              }
              setShowForm(false);
              setEditingId(null);
            } catch (err) {
              console.error("Workflow save failed:", err);
            }
          }}
          onCancel={() => {
            setShowForm(false);
            setEditingId(null);
          }}
          isPending={
            createCwf.isPending ||
            updateCwf.isPending ||
            createScwf.isPending ||
            updateScwf.isPending
          }
        />
      )}
    </section>
  );
}

// ---- WorkflowForm ----------------------------------------------------------

interface WorkflowFormProps {
  editingId: number | null;
  isScwf: boolean;
  onSubmit: (values: CwfDefinitionForm | ScwfDefinitionForm) => Promise<void>;
  onCancel: () => void;
  isPending: boolean;
}

function WorkflowForm({
  editingId,
  isScwf,
  onSubmit,
  onCancel,
  isPending,
}: WorkflowFormProps) {
  const defaultSteps: CwfStep[] = [
    {
      stepId: 1,
      stepName: "",
      stepOrder: 0,
      role: "DOCTOR",
      isMandatory: true,
    },
  ];

  const {
    register,
    handleSubmit,
    control,
  } = useForm<CwfDefinitionForm | ScwfDefinitionForm>({
    resolver: zodResolver(isScwf ? scwfDefinitionSchema : cwfDefinitionSchema),
    defaultValues: isScwf
      ? {
          scwfId: 0,
          scwfName: "",
          serviceType: "CONSUMABLES",
          condition: "",
          steps: defaultSteps,
          status: "ACTIVE",
        }
      : {
          cwfId: 0,
          cwfName: "",
          serviceType: "CONSUMABLES",
          steps: defaultSteps,
          status: "ACTIVE",
        },
  });

  const { fields, append, remove } = useFieldArray({
    control: control as any,
    name: "steps" as any,
  });

  const handleAddStep = () => {
    append({
      stepId: fields.length + 1,
      stepName: "",
      stepOrder: fields.length,
      role: "DOCTOR",
      isMandatory: true,
    } as any);
  };

  return (
    <div className="slideover" role="dialog" aria-modal="true" aria-label="Workflow form">
      <div className="slideover__backdrop" onClick={onCancel} />
      <div className="slideover__panel slideover__panel--wide">
        <header className="slideover__header">
          <h2>
            {editingId ? `Edit ${isScwf ? "SCWF" : "CWF"} #${editingId}` : `New ${isScwf ? "SCWF" : "CWF"}`}
          </h2>
          <button type="button" className="btn btn--ghost btn--sm" onClick={onCancel}>
            ✕
          </button>
        </header>

        <form className="slideover__form" onSubmit={handleSubmit(onSubmit)}>
          <h3 className="slideover__section-title">Workflow Details</h3>
          <div className="form-grid">
            <div className="field">
              <label htmlFor="wf-name">Name *</label>
              <input
                id="wf-name"
                type="text"
                className="form-input"
                {...(isScwf
                  ? register("scwfName" as any)
                  : register("cwfName" as any))}
              />
            </div>
            <div className="field">
              <label htmlFor="wf-service-type">Service Type</label>
              <select
                id="wf-service-type"
                className="form-input"
                {...(isScwf
                  ? register("serviceType" as any)
                  : register("serviceType" as any))}
              >
                {SERVICE_TYPES.map((t) => (
                  <option key={t} value={t}>{t}</option>
                ))}
              </select>
            </div>
            {isScwf && (
              <div className="field">
                <label htmlFor="wf-condition">Trigger Condition *</label>
                <input
                  id="wf-condition"
                  type="text"
                  className="form-input"
                  placeholder="e.g. age > 60 AND diagnosis = 'diabetes'"
                  {...register("condition" as any)}
                />
              </div>
            )}
            <div className="field">
              <label htmlFor="wf-status">Status</label>
              <select
                id="wf-status"
                className="form-input"
                {...(isScwf
                  ? register("status" as any)
                  : register("status" as any))}
              >
                <option value="ACTIVE">Active</option>
                <option value="INACTIVE">Inactive</option>
              </select>
            </div>
          </div>

          {/* Steps */}
          <h3 className="slideover__section-title">Steps ({fields.length})</h3>

          <div className="workflow-form__steps">
            {fields.map((field, idx) => (
              <div key={field.id} className="workflow-form__step">
                <div className="workflow-form__step-number">{idx + 1}</div>
                <div className="workflow-form__step-fields">
                  <div className="field">
                    <label htmlFor={`step-name-${idx}`}>Step Name</label>
                    <input
                      id={`step-name-${idx}`}
                      type="text"
                      className="form-input"
                      {...register(`steps.${idx}.stepName` as any)}
                    />
                  </div>
                  <div className="field">
                    <label htmlFor={`step-role-${idx}`}>Role</label>
                    <select
                      id={`step-role-${idx}`}
                      className="form-input"
                      {...register(`steps.${idx}.role` as any)}
                    >
                      {ROLES.map((r) => (
                        <option key={r} value={r}>{r.replace("_", " ")}</option>
                      ))}
                    </select>
                  </div>
                  <div className="field field--check">
                    <label htmlFor={`step-mandatory-${idx}`}>
                      <input
                        id={`step-mandatory-${idx}`}
                        type="checkbox"
                        {...register(`steps.${idx}.isMandatory` as any)}
                      />
                      Mandatory
                    </label>
                  </div>
                </div>
                <button
                  type="button"
                  className="btn btn--ghost btn--sm"
                  onClick={() => remove(idx)}
                  disabled={fields.length <= 1}
                >
                  ✕
                </button>
              </div>
            ))}

            <button
              type="button"
              className="btn btn--secondary btn--sm"
              onClick={handleAddStep}
            >
              + Add Step
            </button>
          </div>

          <div className="slideover__footer">
            <button type="submit" className="btn btn--primary" disabled={isPending}>
              {isPending ? "Saving…" : editingId ? "Update" : "Create"}
            </button>
            <button type="button" className="btn" onClick={onCancel}>
              Cancel
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

// ---- WorkflowRuntime -------------------------------------------------------

interface WorkflowRuntimeProps {
  orderId: number;
  onClose: () => void;
}

function WorkflowRuntime({ orderId, onClose }: WorkflowRuntimeProps) {
  const { data: steps = [], isLoading } = useWorkflowSteps(orderId);
  const advanceStep = useAdvanceWorkflowStep();

  const handleAdvance = async (stepId: number, action: "COMPLETE" | "REJECT" | "SKIP") => {
    try {
      await advanceStep.mutateAsync({ orderId, stepId, action });
    } catch (err) {
      console.error("Workflow advance failed:", err);
    }
  };

  return (
    <div className="slideover" role="dialog" aria-modal="true" aria-label="Workflow runtime">
      <div className="slideover__backdrop" onClick={onClose} />
      <div className="slideover__panel">
        <header className="slideover__header">
          <h2>Workflow — Order #{orderId}</h2>
          <button type="button" className="btn btn--ghost btn--sm" onClick={onClose}>
            ✕
          </button>
        </header>

        <div className="slideover__form">
          {isLoading ? (
            <p className="muted">Loading workflow steps…</p>
          ) : steps.length === 0 ? (
            <p className="muted">No workflow steps for this order.</p>
          ) : (
            <ol className="workflow-runtime__steps">
              {steps
                .sort((a, b) => a.stepOrder - b.stepOrder)
                .map((step) => (
                  <li key={step.stepId} className="workflow-runtime__step">
                    <div className="workflow-runtime__indicator">
                      <span
                        className="workflow-runtime__dot"
                        style={{ backgroundColor: STEP_STATUS_COLORS[step.status] }}
                      />
                    </div>
                    <div className="workflow-runtime__content">
                      <div className="workflow-runtime__header">
                        <span className="workflow-runtime__name">{step.stepName}</span>
                        <span
                          className={`badge badge--${step.status.toLowerCase()}`}
                          style={{
                            backgroundColor: STEP_STATUS_COLORS[step.status],
                            color: "white",
                          }}
                        >
                          {step.status}
                        </span>
                      </div>
                      <div className="workflow-runtime__meta">
                        <span>Role: {step.role}</span>
                        {step.assignedTo && <span>Assigned: {step.assignedTo}</span>}
                        {step.completedAt && <span>Done: {step.completedAt}</span>}
                      </div>
                      {step.remarks && (
                        <p className="workflow-runtime__remarks">{step.remarks}</p>
                      )}
                      {step.status === "PENDING" && (
                        <div className="workflow-runtime__actions">
                          <button
                            type="button"
                            className="btn btn--primary btn--sm"
                            onClick={() => handleAdvance(step.stepId, "COMPLETE")}
                            disabled={advanceStep.isPending}
                          >
                            Complete
                          </button>
                          <button
                            type="button"
                            className="btn btn--ghost btn--sm"
                            onClick={() => handleAdvance(step.stepId, "SKIP")}
                            disabled={advanceStep.isPending}
                          >
                            Skip
                          </button>
                          <button
                            type="button"
                            className="btn btn--danger btn--sm"
                            onClick={() => handleAdvance(step.stepId, "REJECT")}
                            disabled={advanceStep.isPending}
                          >
                            Reject
                          </button>
                        </div>
                      )}
                    </div>
                  </li>
                ))}
            </ol>
          )}
        </div>
      </div>
    </div>
  );
}
