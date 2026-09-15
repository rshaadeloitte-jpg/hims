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

/**
 * ConcessionTemplates — manage discount templates with max-percent-off config.
 * Stripe-style card grid: each template shows scheme, max discount, approval flag.
 * Inline create/edit with validation (0-100% range).
 */
export function ConcessionTemplates() {
  const [showForm, setShowForm] = useState(false);
  const [editingId, setEditingId] = useState<number | null>(null);

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

  return (
    <section className="page">
      <header className="page-header">
        <div>
          <h1>Concession Templates</h1>
          <p className="muted">
            Configure discount templates with maximum percent-off and approval rules.
          </p>
        </div>
        <button
          type="button"
          className="btn btn--primary"
          onClick={handleNew}
          disabled={createTemplate.isPending}
        >
          + New Template
        </button>
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
                  <button
                    type="button"
                    className="btn btn--ghost btn--sm"
                    onClick={() => handleDeactivate(tpl.id)}
                    disabled={deactivateTemplate.isPending}
                  >
                    Deactivate
                  </button>
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
          <button type="button" className="btn btn--ghost btn--sm" onClick={onCancel}>
            ✕
          </button>
        </header>

        <form className="slideover__form" onSubmit={handleSubmit(onSubmit)}>
          <div className="form-grid">
            <div className="field">
              <label htmlFor="ct-code">Code *</label>
              <input
                id="ct-code"
                type="text"
                className="form-input"
                placeholder="e.g. STAFF_DUTY"
                {...register("code")}
                aria-invalid={errors.code ? true : undefined}
              />
              {errors.code && (
                <span className="field-error">{errors.code.message}</span>
              )}
            </div>
            <div className="field">
              <label htmlFor="ct-name">Name *</label>
              <input
                id="ct-name"
                type="text"
                className="form-input"
                placeholder="e.g. Staff Duty Concession"
                {...register("name")}
                aria-invalid={errors.name ? true : undefined}
              />
              {errors.name && (
                <span className="field-error">{errors.name.message}</span>
              )}
            </div>
            <div className="field">
              <label htmlFor="ct-scheme">Scheme *</label>
              <input
                id="ct-scheme"
                type="text"
                className="form-input"
                placeholder="e.g. RGHS, Chiranjeevi, Self"
                {...register("scheme")}
                aria-invalid={errors.scheme ? true : undefined}
              />
              {errors.scheme && (
                <span className="field-error">{errors.scheme.message}</span>
              )}
            </div>
            <div className="field">
              <label htmlFor="ct-desc">Description</label>
              <input
                id="ct-desc"
                type="text"
                className="form-input"
                placeholder="Optional description"
                {...register("description")}
              />
            </div>
            <div className="field">
              <label htmlFor="ct-max-pct">Max Percent Off *</label>
              <div className="concession-form__percent-input">
                <input
                  id="ct-max-pct"
                  type="number"
                  step="0.01"
                  min="0"
                  max="100"
                  className="form-input"
                  {...register("maxPercentOff")}
                  aria-invalid={errors.maxPercentOff ? true : undefined}
                />
                <span className="concession-form__percent-suffix">%</span>
              </div>
              {errors.maxPercentOff && (
                <span className="field-error">{errors.maxPercentOff.message}</span>
              )}
              {/* Visual percent bar */}
              <div className="concession-form__percent-bar">
                <div
                  className="concession-form__percent-fill"
                  style={{ width: `${Math.min(maxPercentOff || 0, 100)}%` }}
                />
              </div>
            </div>
            <div className="field field--check">
              <label htmlFor="ct-approval">
                <input
                  id="ct-approval"
                  type="checkbox"
                  {...register("requiresApproval")}
                />
                Requires Approval
              </label>
            </div>
          </div>

          <div className="slideover__actions">
            <button type="button" className="btn btn--secondary" onClick={onCancel}>
              Cancel
            </button>
            <button type="submit" className="btn btn--primary" disabled={isPending}>
              {isPending ? "Creating..." : "Create Template"}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
