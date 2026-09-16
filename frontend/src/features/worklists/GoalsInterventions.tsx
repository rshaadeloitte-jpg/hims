import { useState, useMemo, useCallback } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { Button, Textarea } from "@chakra-ui/react";
import { FormField, TextInput } from "../../components/ui";
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
            <Button
              type="button"
              variant="primary"
              size="sm"
              onClick={() => onStatusChange(goal.goalId, "ACHIEVED")}
            >
              Mark Achieved
            </Button>
            <Button
              type="button"
              variant="ghost"
              size="sm"
              onClick={() => onStatusChange(goal.goalId, "DISCONTINUED")}
            >
              Discontinue
            </Button>
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
            <Button
              type="button"
              variant="primary"
              size="sm"
              onClick={() => onStatusChange(intervention.interventionId, "IN_PROGRESS")}
            >
              Start
            </Button>
            <Button
              type="button"
              variant="ghost"
              size="sm"
              onClick={() => onStatusChange(intervention.interventionId, "SKIPPED")}
            >
              Skip
            </Button>
          </div>
        )}
        {intervention.status === "IN_PROGRESS" && (
          <Button
            type="button"
            variant="primary"
            size="sm"
            onClick={() => onStatusChange(intervention.interventionId, "COMPLETED")}
          >
            Complete
          </Button>
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
        <FormField label="Goal *" error={errors.goalText?.message} required>
          <Textarea
            id="goalText"
            bg="white"
            borderColor="gray.300"
            placeholder="Describe the care goal..."
            {...register("goalText")}
          />
        </FormField>
      </div>
      <div className="wl-form__row">
        <FormField label="Target Date">
          <TextInput id="targetDate" type="date" {...register("targetDate")} />
        </FormField>
      </div>
      <div className="wl-form__actions">
        <Button type="submit" variant="primary" loading={createGoal.isPending} loadingText="Adding…">
          Add Goal
        </Button>
        <Button type="button" variant="ghost" onClick={onClose}>
          Cancel
        </Button>
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
        <FormField label="Intervention *" error={errors.interventionText?.message} required>
          <Textarea
            id="interventionText"
            bg="white"
            borderColor="gray.300"
            placeholder="Describe the intervention..."
            {...register("interventionText")}
          />
        </FormField>
      </div>
      <div className="wl-form__row">
        <FormField label="Scheduled Time">
          <TextInput
            id="scheduledTime"
            type="datetime-local"
            {...register("scheduledTime")}
          />
        </FormField>
      </div>
      <div className="wl-form__actions">
        <Button type="submit" variant="primary" loading={createIntervention.isPending} loadingText="Adding…">
          Add Intervention
        </Button>
        <Button type="button" variant="ghost" onClick={onClose}>
          Cancel
        </Button>
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
        <Button
          role="tab"
          aria-selected={activeTab === "goals"}
          variant={activeTab === "goals" ? "primary" : "ghost"}
          size="sm"
          onClick={() => setActiveTab("goals")}
        >
          Goals
          <span className="wl-tab__badge">{activeGoals}</span>
        </Button>
        <Button
          role="tab"
          aria-selected={activeTab === "interventions"}
          variant={activeTab === "interventions" ? "primary" : "ghost"}
          size="sm"
          onClick={() => setActiveTab("interventions")}
        >
          Interventions
          <span className="wl-tab__badge">{activeInterventions}</span>
        </Button>
      </nav>

      {/* Goals tab */}
      {activeTab === "goals" && (
        <div className="wl-tab-panel">
          <div className="wl-tab-panel__header">
            <h4>Care Goals ({sortedGoals.length})</h4>
            {!showGoalForm && (
              <Button
                type="button"
                variant="primary"
                size="sm"
                onClick={() => setShowGoalForm(true)}
              >
                + New Goal
              </Button>
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
              <Button
                type="button"
                variant="primary"
                size="sm"
                onClick={() => setShowInterventionForm(true)}
              >
                + New Intervention
              </Button>
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
