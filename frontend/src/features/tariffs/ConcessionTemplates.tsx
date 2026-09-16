import { useState, useCallback } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import {
  useConcessionTemplates,
  useCreateConcessionTemplate,
  useDeactivateConcessionTemplate,
} from "./queries";
import {
  concessionTemplateSchema,
  type ConcessionTemplateValues,
} from "./zodSchema";
import { Button, Checkbox } from "@chakra-ui/react";
import { FormField, TextInput } from "../../components/ui";

/**
 * ConcessionTemplates — manage discount templates with max-percent-off config.
 * Stripe-style card grid: each template shows scheme, max discount, approval flag.
 * Inline create/edit with validation (0-100% range).
 */
export function ConcessionTemplates() {
  const [showForm, setShowForm] = useState(false);
  const [_editingId, setEditingId] = useState<number | null>(null);

  const { data: templates = [], isLoading } = useConcessionTemplates();
  const createTemplate = useCreateConcessionTemplate();
  const deactivateTemplate = useDeactivateConcessionTemplate();

  const handleSubmit = async (values: ConcessionTemplateValues) => {
    try {
      await createTemplate.mutateAsync(values);
      setShowForm(false);
    } catch (err) {
      console.error("Concession template save failed:", err);
    }
  };

  const handleDeactivate = useCallback(
    async (id: number) => {
      try {
        await deactivateTemplate.mutateAsync(id);
      } catch (err) {
        console.error("Deactivation failed:", err);
      }
    },
    [deactivateTemplate],
  );

  const handleNew = useCallback(() => {
    setEditingId(null);
    setShowForm(true);
  }, []);

  void _editingId;

  return (
    <section className="page">
      <header className="page-header">
        <div>
          <h1>Concession Templates</h1>
          <p className="muted">
            Configure discount templates with maximum percent-off and approval rules.
          </p>
        </div>
        <Button
          type="button"
          variant="primary"
          onClick={handleNew}
          disabled={createTemplate.isPending}
          loading={createTemplate.isPending}
          loadingText="Loading..."
        >
          + New Template
        </Button>
      </header>

      {/* Template cards — Stripe-style grid */}
      <section className="concession-templates__grid" aria-label="Concession templates">
        {isLoading ? (
          <div className="loading-state">
            <span className="spinner" aria-hidden="true" /> Loading templates…
          </div>
        ) : templates.length === 0 ? (
          <p className="muted">No concession templates configured.</p>
        ) : (
          templates.map((tpl) => (
            <article
              key={tpl.id}
              className={`concession-card${
                tpl.docStatus === 2 ? " concession-card--inactive" : ""
              }`}
            >
              <div className="concession-card__header">
                <span className="concession-card__code">{tpl.code}</span>
                <span
                  className={`concession-card__status${
                    tpl.docStatus === 2
                      ? " concession-card__status--inactive"
                      : ""
                  }`}
                >
                  {tpl.docStatus === 1 ? "ACTIVE" : "INACTIVE"}
                </span>
              </div>
              <h3 className="concession-card__name">{tpl.name}</h3>
              <div className="concession-card__scheme">
                <span className="concession-card__scheme-label">Scheme</span>
                <span className="concession-card__scheme-value">{tpl.scheme}</span>
              </div>
              <div className="concession-card__discount">
                <div className="concession-card__discount-bar">
                  <div
                    className="concession-card__discount-fill"
                    style={{ width: `${Math.min(tpl.maxPercentOff, 100)}%` }}
                  />
                </div>
                <span className="concession-card__discount-value">
                  {tpl.maxPercentOff}% off
                </span>
              </div>
              {tpl.requiresApproval && (
                <div className="concession-card__approval">
                  <span className="concession-card__approval-badge">
                    Approval Required
                  </span>
                </div>
              )}
              {tpl.description && (
                <p className="concession-card__desc">{tpl.description}</p>
              )}
              <div className="concession-card__actions">
                {tpl.docStatus === 1 && (
                  <Button
                    type="button"
                    variant="ghost"
                    size="sm"
                    onClick={() => handleDeactivate(tpl.id)}
                    disabled={deactivateTemplate.isPending}
                  >
                    Deactivate
                  </Button>
                )}
              </div>
            </article>
          ))
        )}
      </section>

      {/* Form slideover */}
      {showForm && (
        <ConcessionTemplateForm
          onSubmit={handleSubmit}
          onCancel={() => setShowForm(false)}
          isPending={createTemplate.isPending}
        />
      )}
    </section>
  );
}

// ---- ConcessionTemplateForm ------------------------------------------------

interface ConcessionTemplateFormProps {
  onSubmit: (values: ConcessionTemplateValues) => Promise<void>;
  onCancel: () => void;
  isPending: boolean;
}

function ConcessionTemplateForm({
  onSubmit,
  onCancel,
  isPending,
}: ConcessionTemplateFormProps) {
  const {
    register,
    handleSubmit,
    watch,
    setValue,
    formState: { errors },
  } = useForm<ConcessionTemplateValues>({
    resolver: zodResolver(concessionTemplateSchema),
    defaultValues: {
      code: "",
      name: "",
      scheme: "",
      description: "",
      maxPercentOff: 0,
      requiresApproval: false,
    },
  });

  const maxPercentOff = watch("maxPercentOff");
  const requiresApproval = watch("requiresApproval");

  return (
    <div
      className="slideover"
      role="dialog"
      aria-modal="true"
      aria-label="Concession template form"
    >
      <div className="slideover__backdrop" onClick={onCancel} />
      <div className="slideover__panel">
        <header className="slideover__header">
          <h2>New Concession Template</h2>
          <Button type="button" variant="ghost" size="sm" onClick={onCancel} aria-label="Close">
            ✕
          </Button>
        </header>

        <form className="slideover__form" onSubmit={handleSubmit(onSubmit)}>
          <div className="form-grid">
            <div className="field">
              <FormField label="Code *" required error={errors.code?.message}>
                <TextInput
                  id="ct-code"
                  type="text"
                  placeholder="e.g. STAFF_DUTY"
                  {...register("code")}
                  aria-invalid={errors.code ? true : undefined}
                />
              </FormField>
            </div>
            <div className="field">
              <FormField label="Name *" required error={errors.name?.message}>
                <TextInput
                  id="ct-name"
                  type="text"
                  placeholder="e.g. Staff Duty Concession"
                  {...register("name")}
                  aria-invalid={errors.name ? true : undefined}
                />
              </FormField>
            </div>
            <div className="field">
              <FormField label="Scheme *" required error={errors.scheme?.message}>
                <TextInput
                  id="ct-scheme"
                  type="text"
                  placeholder="e.g. RGHS, Chiranjeevi, Self"
                  {...register("scheme")}
                  aria-invalid={errors.scheme ? true : undefined}
                />
              </FormField>
            </div>
            <div className="field">
              <FormField label="Description">
                <TextInput
                  id="ct-desc"
                  type="text"
                  placeholder="Optional description"
                  {...register("description")}
                />
              </FormField>
            </div>
            <div className="field">
              <FormField label="Max Percent Off *" required error={errors.maxPercentOff?.message}>
                <div className="concession-form__percent-input">
                  <TextInput
                    id="ct-max-pct"
                    type="number"
                    step="0.01"
                    min="0"
                    max="100"
                    {...register("maxPercentOff")}
                    aria-invalid={errors.maxPercentOff ? true : undefined}
                  />
                  <span className="concession-form__percent-suffix">%</span>
                </div>
              </FormField>
              {/* Visual percent bar */}
              <div className="concession-form__percent-bar">
                <div
                  className="concession-form__percent-fill"
                  style={{ width: `${Math.min(maxPercentOff || 0, 100)}%` }}
                />
              </div>
            </div>
            <div className="field field--check">
              <Checkbox.Root
                checked={!!requiresApproval}
                onCheckedChange={(e) => setValue("requiresApproval", !!e.checked, { shouldValidate: true })}
              >
                <Checkbox.HiddenInput {...register("requiresApproval")} />
                <Checkbox.Control />
                <Checkbox.Label>Requires Approval</Checkbox.Label>
              </Checkbox.Root>
            </div>
          </div>

          <div className="slideover__actions">
            <Button type="button" variant="secondary" onClick={onCancel}>
              Cancel
            </Button>
            <Button type="submit" variant="primary" disabled={isPending} loading={isPending} loadingText="Creating...">
              Create Template
            </Button>
          </div>
        </form>
      </div>
    </div>
  );
}
