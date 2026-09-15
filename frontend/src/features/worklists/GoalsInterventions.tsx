import { useState, useMemo, useCallback } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import {
  useCarePlansByAdmission,
  useCreateCareGoal,
  useCreateIntervention,
  useUpdateGoalStatus,
  useUpdateInterventionStatus,
  type CarePlan,
  type CareGoal,
  type Intervention,
} from "./queries";
import {
  careGoalFormSchema,
  interventionFormSchema,
  statusBadgeClass,
  type CareGoalFormValues,
  type InterventionFormValues,
  type GoalStatus,
  type InterventionStatus,
} from "./zodSchema";

/**
 * GoalsInterventions — tabbed view of care goals + interventions for a patient.
 *
 * Linear-style UX: tab navigation, inline create forms, status badges,
 * chronological ordering with achieved/completed markers.
 */

type ActiveTab = "goals" | "interventions";

interface GoalsInterventionsProps {
  admissionId: number;
}

function GoalCard({ goal, onStatusChange }: { goal: CareGoal; onStatusChange: (id: number, status: GoalStatus) => void }) {
  return (
    <div className="wl-card">
      <div className="wl-card__header">
        <span className={`wl-badge ${statusBadgeClass(goal.status)}`}>{goal.status}</span>
        {goal.targetDate && (
          <span className="wl-card__date">Target: {new Date(goal.targetDate).toLocaleDateString()}</span>
        )}
      </div>
      <p className="wl-card__text">{goal.goalText}</p>
      <div className="wl-card__footer">
        <span className="wl-muted">Added: {new Date(goal.createdAt).toLocaleDateString()}</span>
        {goal.status === "ACTIVE" && (
          <div className="wl-card__actions">
            <button
              type="button"
              className="wl-btn wl-btn--sm wl-btn--success"
              onClick={() => onStatusChange(goal.goalId, "ACHIEVED")}
            >
              Mark Achieved
            </button>
            <button
              type="button"
              className="wl-btn wl-btn--sm wl-btn--ghost"
              onClick={() => onStatusChange(goal.goalId, "DISCONTINUED")}
            >
              Discontinue
            </button>
          </div>
        )}
        {goal.achievedAt && goal.status === "ACHIEVED" && (
          <span className="wl-text--success">Achieved: {new Date(goal.achievedAt).toLocaleDateString()}</span>
        )}
      </div>
    </div>
  );
}

function InterventionCard({
  intervention,
  onStatusChange,
}: {
  intervention: Intervention;
  onStatusChange: (id: number, status: InterventionStatus) => void;
}) {
  return (
    <div className="wl-card">
      <div className="wl-card__header">
        <span className={`wl-badge ${statusBadgeClass(intervention.status)}`}>{intervention.status}</span>
        {intervention.scheduledTime && (
          <span className="wl-card__date">
            Scheduled: {new Date(intervention.scheduledTime).toLocaleString()}
          </span>
        )}
      </div>
      <p className="wl-card__text">{intervention.interventionText}</p>
      <div className="wl-card__footer">
        <span className="wl-muted">Added: {new Date(intervention.createdAt).toLocaleDateString()}</span>
        {intervention.status === "SCHEDULED" && (
          <div className="wl-card__actions">
            <button
              type="button"
              className="wl-btn wl-btn--sm wl-btn--primary"
              onClick={() => onStatusChange(intervention.interventionId, "IN_PROGRESS")}
            >
              Start
            </button>
            <button
              type="button"
              className="wl-btn wl-btn--sm wl-btn--ghost"
              onClick={() => onStatusChange(intervention.interventionId, "SKIPPED")}
            >
              Skip
            </button>
          </div>
        )}
        {intervention.status === "IN_PROGRESS" && (
          <button
            type="button"
            className="wl-btn wl-btn--sm wl-btn--success"
            onClick={() => onStatusChange(intervention.interventionId, "COMPLETED")}
          >
            Complete
          </button>
        )}
        {intervention.completedAt && intervention.status === "COMPLETED" && (
          <span className="wl-text--success">
            Completed: {new Date(intervention.completedAt).toLocaleDateString()}
          </span>
        )}
      </div>
    </div>
  );
}

function GoalCreateForm({ carePlanId, onClose }: { carePlanId: number; onClose: () => void }) {
  const createGoal = useCreateCareGoal();
  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<CareGoalFormValues>({
    resolver: zodResolver(careGoalFormSchema),
    defaultValues: { carePlanId, goalText: "", targetDate: "", status: "ACTIVE" },
  });

  const onSubmit = async (values: CareGoalFormValues) => {
    try {
      await createGoal.mutateAsync(values);
      reset();
      onClose();
    } catch (err) {
      console.error("Failed to create goal:", err);
    }
  };

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="wl-form">
      <div className="wl-form__row">
        <label htmlFor="goalText" className="wl-form__label">
          Goal *
        </label>
        <textarea
          id="goalText"
          className="wl-form__textarea"
          placeholder="Describe the care goal..."
          {...register("goalText")}
        />
        {errors.goalText && <span className="wl-form__error">{errors.goalText.message}</span>}
      </div>
      <div className="wl-form__row">
        <label htmlFor="targetDate" className="wl-form__label">
          Target Date
        </label>
        <input id="targetDate" type="date" className="wl-form__input" {...register("targetDate")} />
      </div>
      <div className="wl-form__actions">
        <button type="submit" className="wl-btn wl-btn--primary" disabled={createGoal.isPending}>
          {createGoal.isPending ? "Adding…" : "Add Goal"}
        </button>
        <button type="button" className="wl-btn wl-btn--ghost" onClick={onClose}>
          Cancel
        </button>
      </div>
      {createGoal.isError && (
        <div className="wl-form__error-banner">Failed to create goal. Please try again.</div>
      )}
    </form>
  );
}

function InterventionCreateForm({ carePlanId, onClose }: { carePlanId: number; onClose: () => void }) {
  const createIntervention = useCreateIntervention();
  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<InterventionFormValues>({
    resolver: zodResolver(interventionFormSchema),
    defaultValues: { carePlanId, interventionText: "", scheduledTime: "", status: "SCHEDULED" },
  });

  const onSubmit = async (values: InterventionFormValues) => {
    try {
      await createIntervention.mutateAsync(values);
      reset();
      onClose();
    } catch (err) {
      console.error("Failed to create intervention:", err);
    }
  };

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="wl-form">
      <div className="wl-form__row">
        <label htmlFor="interventionText" className="wl-form__label">
          Intervention *
        </label>
        <textarea
          id="interventionText"
          className="wl-form__textarea"
          placeholder="Describe the intervention..."
          {...register("interventionText")}
        />
        {errors.interventionText && (
          <span className="wl-form__error">{errors.interventionText.message}</span>
        )}
      </div>
      <div className="wl-form__row">
        <label htmlFor="scheduledTime" className="wl-form__label">
          Scheduled Time
        </label>
        <input
          id="scheduledTime"
          type="datetime-local"
          className="wl-form__input"
          {...register("scheduledTime")}
        />
      </div>
      <div className="wl-form__actions">
        <button type="submit" className="wl-btn wl-btn--primary" disabled={createIntervention.isPending}>
          {createIntervention.isPending ? "Adding…" : "Add Intervention"}
        </button>
        <button type="button" className="wl-btn wl-btn--ghost" onClick={onClose}>
          Cancel
        </button>
      </div>
      {createIntervention.isError && (
        <div className="wl-form__error-banner">Failed to create intervention. Please try again.</div>
      )}
    </form>
  );
}

function CarePlanSection({ carePlan }: { carePlan: CarePlan }) {
  const [activeTab, setActiveTab] = useState<ActiveTab>("goals");
  const [showGoalForm, setShowGoalForm] = useState(false);
  const [showInterventionForm, setShowInterventionForm] = useState(false);

  const updateGoalStatus = useUpdateGoalStatus();
  const updateInterventionStatus = useUpdateInterventionStatus();

  const handleGoalStatusChange = useCallback(
    (id: number, status: GoalStatus) => {
      updateGoalStatus.mutate({ id, status });
    },
    [updateGoalStatus],
  );

  const handleInterventionStatusChange = useCallback(
    (id: number, status: InterventionStatus) => {
      updateInterventionStatus.mutate({ id, status });
    },
    [updateInterventionStatus],
  );

  const sortedGoals = useMemo(
    () => [...carePlan.goals].sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()),
    [carePlan.goals],
  );

  const sortedInterventions = useMemo(
    () =>
      [...carePlan.interventions].sort(
        (a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime(),
      ),
    [carePlan.interventions],
  );

  const activeGoals = sortedGoals.filter((g) => g.status === "ACTIVE").length;
  const activeInterventions = sortedInterventions.filter((i) => i.status !== "COMPLETED" && i.status !== "SKIPPED").length;

  return (
    <section className="wl-section">
      <header className="wl-section__header">
        <div>
          <h3 className="wl-section__title">{carePlan.title}</h3>
          {carePlan.description && <p className="wl-muted">{carePlan.description}</p>}
        </div>
        {carePlan.resolved && <span className="wl-badge bg-green-100 text-green-800">Resolved</span>}
      </header>

      {/* Tabs */}
      <nav className="wl-tabs" role="tablist">
        <button
          role="tab"
          aria-selected={activeTab === "goals"}
          className={`wl-tab ${activeTab === "goals" ? "wl-tab--active" : ""}`}
          onClick={() => setActiveTab("goals")}
        >
          Goals
          <span className="wl-tab__badge">{activeGoals}</span>
        </button>
        <button
          role="tab"
          aria-selected={activeTab === "interventions"}
          className={`wl-tab ${activeTab === "interventions" ? "wl-tab--active" : ""}`}
          onClick={() => setActiveTab("interventions")}
        >
          Interventions
          <span className="wl-tab__badge">{activeInterventions}</span>
        </button>
      </nav>

      {/* Goals tab */}
      {activeTab === "goals" && (
        <div className="wl-tab-panel">
          <div className="wl-tab-panel__header">
            <h4>Care Goals ({sortedGoals.length})</h4>
            {!showGoalForm && (
              <button
                type="button"
                className="wl-btn wl-btn--sm wl-btn--primary"
                onClick={() => setShowGoalForm(true)}
              >
                + New Goal
              </button>
            )}
          </div>
          {showGoalForm && (
            <GoalCreateForm carePlanId={carePlan.carePlanId} onClose={() => setShowGoalForm(false)} />
          )}
          <div className="wl-card-list">
            {sortedGoals.map((goal) => (
              <GoalCard key={goal.goalId} goal={goal} onStatusChange={handleGoalStatusChange} />
            ))}
            {sortedGoals.length === 0 && <div className="wl-empty">No goals defined.</div>}
          </div>
        </div>
      )}

      {/* Interventions tab */}
      {activeTab === "interventions" && (
        <div className="wl-tab-panel">
          <div className="wl-tab-panel__header">
            <h4>Interventions ({sortedInterventions.length})</h4>
            {!showInterventionForm && (
              <button
                type="button"
                className="wl-btn wl-btn--sm wl-btn--primary"
                onClick={() => setShowInterventionForm(true)}
              >
                + New Intervention
              </button>
            )}
          </div>
          {showInterventionForm && (
            <InterventionCreateForm
              carePlanId={carePlan.carePlanId}
              onClose={() => setShowInterventionForm(false)}
            />
          )}
          <div className="wl-card-list">
            {sortedInterventions.map((intervention) => (
              <InterventionCard
                key={intervention.interventionId}
                intervention={intervention}
                onStatusChange={handleInterventionStatusChange}
              />
            ))}
            {sortedInterventions.length === 0 && <div className="wl-empty">No interventions defined.</div>}
          </div>
        </div>
      )}
    </section>
  );
}

export function GoalsInterventions({ admissionId }: GoalsInterventionsProps) {
  const { data: carePlans = [], isLoading, isError } = useCarePlansByAdmission(admissionId);

  if (isLoading) {
    return <div className="wl-loading">Loading care plans…</div>;
  }

  if (isError) {
    return <div className="wl-error">Failed to load care plans.</div>;
  }

  if (carePlans.length === 0) {
    return (
      <div className="wl-empty wl-empty--large">
        No care plans found for this admission.
      </div>
    );
  }

  return (
    <div className="wl-goals-interventions">
      <header className="wl-goals-interventions__header">
        <h2>Care Goals &amp; Interventions</h2>
        <span className="wl-muted">{carePlans.length} care plan{carePlans.length !== 1 ? "s" : ""}</span>
      </header>
      <div className="wl-careplan-list">
        {carePlans.map((cp) => (
          <CarePlanSection key={cp.carePlanId} carePlan={cp} />
        ))}
      </div>
    </div>
  );
}
