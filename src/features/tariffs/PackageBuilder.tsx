import { useState, useCallback } from "react";
import { useForm, useFieldArray } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import {
  usePackages,
  useCreatePackage,
  useUpdatePackage,
} from "./queries";
import {
  packageSchema,
  packagePlanSelectionSchema,
  type PackageValues,
  type PackagePlanSelectionValues,
  type PackageServiceLineValues,
} from "./zodSchema";

/**
 * PackageBuilder — compose service packages with plan selection.
 * Stripe-style plan selector: toggle coverage tiers (Basic/Gold/Platinum),
 * add/remove service lines, live total preview card on the right.
 */
export function PackageBuilder() {
  const [showForm, setShowForm] = useState(false);
  const [editingId, setEditingId] = useState<number | null>(null);

  const { data: packages = [], isLoading } = usePackages();
  const createPackage = useCreatePackage();
  const updatePackage = useUpdatePackage();

  const handleSubmit = async (values: PackageValues) => {
    try {
      if (editingId) {
        await updatePackage.mutateAsync({ id: editingId, payload: values });
      } else {
        await createPackage.mutateAsync(values);
      }
      setShowForm(false);
      setEditingId(null);
    } catch (err) {
      console.error("Package save failed:", err);
    }
  };

  const handleEdit = useCallback((id: number) => {
    setEditingId(id);
    setShowForm(true);
  }, []);

  const handleNew = useCallback(() => {
    setEditingId(null);
    setShowForm(true);
  }, []);

  return (
    <section className="page">
      <header className="page-header">
        <div>
          <h1>Package Builder</h1>
          <p className="muted">
            Compose service packages, set coverage plans, and manage bundled pricing.
          </p>
        </div>
        <button
          type="button"
          className="btn btn--primary"
          onClick={handleNew}
          disabled={createPackage.isPending}
        >
          + New Package
        </button>
      </header>

      {/* Package cards — Stripe-style 3-col grid */}
      <section className="package-builder__grid" aria-label="Packages">
        {isLoading ? (
          <div className="loading-state">
            <span className="spinner" aria-hidden="true" /> Loading packages…
          </div>
        ) : packages.length === 0 ? (
          <p className="muted">No packages configured. Create one to get started.</p>
        ) : (
          packages.map((pkg) => (
            <article
              key={pkg.id}
              className={`package-card${
                pkg.status === "INACTIVE" ? " package-card--inactive" : ""
              }`}
              onClick={() => handleEdit(pkg.id)}
              role="button"
              tabIndex={0}
              onKeyDown={(e) => {
                if (e.key === "Enter" || e.key === " ") handleEdit(pkg.id);
              }}
            >
              <div className="package-card__header">
                <span className="package-card__code">{pkg.packageCode}</span>
                <span
                  className={`package-card__status${
                    pkg.status === "INACTIVE"
                      ? " package-card__status--inactive"
                      : ""
                  }`}
                >
                  {pkg.status}
                </span>
              </div>
              <h3 className="package-card__name">{pkg.packageName}</h3>
              {pkg.description && (
                <p className="package-card__desc">{pkg.description}</p>
              )}
              <div className="package-card__price">
                <span className="package-card__price-label">Sell Price</span>
                <span className="package-card__price-value">
                  ₹ {pkg.sellPrice.toFixed(2)}
                </span>
                <span className="package-card__price-original">
                  Value: ₹ {pkg.totalValue.toFixed(2)}
                </span>
              </div>
              <div className="package-card__services">
                <span className="package-card__services-count">
                  {pkg.services.length} services
                </span>
                <span className="package-card__services-plans">
                  {pkg.plans.length > 0
                    ? `${pkg.plans.length} plan(s)`
                    : "No plans"}
                </span>
              </div>
            </article>
          ))
        )}
      </section>

      {/* Form slideover */}
      {showForm && (
        <PackageForm
          editingId={editingId}
          onSubmit={handleSubmit}
          onCancel={() => {
            setShowForm(false);
            setEditingId(null);
          }}
          isPending={createPackage.isPending || updatePackage.isPending}
        />
      )}
    </section>
  );
}

// ---- PackageForm -----------------------------------------------------------

interface PackageFormProps {
  editingId: number | null;
  onSubmit: (values: PackageValues) => Promise<void>;
  onCancel: () => void;
  isPending: boolean;
}

function PackageForm({
  editingId,
  onSubmit,
  onCancel,
  isPending,
}: PackageFormProps) {
  const { register, handleSubmit, control, watch, setValue, formState: { errors } } =
    useForm<PackageValues>({
      resolver: zodResolver(packageSchema),
      defaultValues: {
        packageName: "",
        packageCode: "",
        description: "",
        totalValue: 0,
        sellPrice: 0,
        services: [{ serviceId: 0, serviceName: "", qty: 1 }],
        plans: [],
        status: "ACTIVE",
      },
    });

  const { fields: serviceFields, append: appendService, remove: removeService } =
    useFieldArray({ control, name: "services" });
  const { fields: planFields, append: appendPlan, remove: removePlan } =
    useFieldArray({ control, name: "plans" });

  const services = watch("services");
  const plans = watch("plans");

  // Auto-calculate totalValue from services
  const totalValue = services.reduce(
    (sum, s) => sum + (s.serviceId || 0) * (s.qty || 0),
    0,
  );

  return (
    <div className="slideover" role="dialog" aria-modal="true" aria-label="Package form">
      <div className="slideover__backdrop" onClick={onCancel} />
      <div className="slideover__panel slideover__panel--wide">
        <header className="slideover__header">
          <h2>{editingId ? `Edit Package #${editingId}` : "New Package"}</h2>
          <button type="button" className="btn btn--ghost btn--sm" onClick={onCancel}>
            ✕
          </button>
        </header>

        <form className="slideover__form" onSubmit={handleSubmit(onSubmit)}>
          {/* Package details */}
          <h3 className="slideover__section-title">Package Details</h3>
          <div className="form-grid">
            <div className="field">
              <label htmlFor="pkg-name">Package Name *</label>
              <input
                id="pkg-name"
                type="text"
                className="form-input"
                {...register("packageName")}
                aria-invalid={errors.packageName ? true : undefined}
              />
              {errors.packageName && (
                <span className="field-error">{errors.packageName.message}</span>
              )}
            </div>
            <div className="field">
              <label htmlFor="pkg-code">Package Code *</label>
              <input
                id="pkg-code"
                type="text"
                className="form-input"
                {...register("packageCode")}
                aria-invalid={errors.packageCode ? true : undefined}
              />
              {errors.packageCode && (
                <span className="field-error">{errors.packageCode.message}</span>
              )}
            </div>
            <div className="field">
              <label htmlFor="pkg-desc">Description</label>
              <input
                id="pkg-desc"
                type="text"
                className="form-input"
                {...register("description")}
              />
            </div>
            <div className="field">
              <label htmlFor="pkg-sell-price">Sell Price (₹) *</label>
              <input
                id="pkg-sell-price"
                type="number"
                step="0.01"
                className="form-input"
                {...register("sellPrice")}
                aria-invalid={errors.sellPrice ? true : undefined}
              />
              {errors.sellPrice && (
                <span className="field-error">{errors.sellPrice.message}</span>
              )}
            </div>
            <div className="field">
              <label htmlFor="pkg-total-value">Total Value (₹) *</label>
              <input
                id="pkg-total-value"
                type="number"
                step="0.01"
                className="form-input"
                {...register("totalValue")}
                aria-invalid={errors.totalValue ? true : undefined}
              />
              {errors.totalValue && (
                <span className="field-error">{errors.totalValue.message}</span>
              )}
            </div>
            <div className="field">
              <label htmlFor="pkg-status">Status</label>
              <select id="pkg-status" className="form-input" {...register("status")}>
                <option value="ACTIVE">Active</option>
                <option value="INACTIVE">Inactive</option>
              </select>
            </div>
          </div>

          {/* Service lines */}
          <h3 className="slideover__section-title">Service Lines</h3>
          <div className="package-form__services">
            {serviceFields.map((field, idx) => (
              <div key={field.id} className="package-form__service-row">
                <div className="field">
                  <label htmlFor={`svc-id-${idx}`}>Service ID</label>
                  <input
                    id={`svc-id-${idx}`}
                    type="number"
                    className="form-input"
                    {...register(`services.${idx}.serviceId` as const)}
                  />
                </div>
                <div className="field">
                  <label htmlFor={`svc-name-${idx}`}>Service Name</label>
                  <input
                    id={`svc-name-${idx}`}
                    type="text"
                    className="form-input"
                    {...register(`services.${idx}.serviceName` as const)}
                  />
                </div>
                <div className="field">
                  <label htmlFor={`svc-qty-${idx}`}>Qty</label>
                  <input
                    id={`svc-qty-${idx}`}
                    type="number"
                    className="form-input"
                    {...register(`services.${idx}.qty` as const)}
                  />
                </div>
                <button
                  type="button"
                  className="btn btn--ghost btn--sm"
                  onClick={() => removeService(idx)}
                  disabled={serviceFields.length <= 1}
                >
                  ✕
                </button>
              </div>
            ))}
            <button
              type="button"
              className="btn btn--secondary btn--sm"
              onClick={() => appendService({ serviceId: 0, serviceName: "", qty: 1 })}
            >
              + Add Service
            </button>
          </div>

          {/* Plan selection — Stripe-style cards */}
          <h3 className="slideover__section-title">Coverage Plans</h3>
          <div className="package-form__plans">
            {planFields.map((field, idx) => (
              <div key={field.id} className="package-form__plan-row">
                <div className="field">
                  <label htmlFor={`plan-id-${idx}`}>Plan ID</label>
                  <input
                    id={`plan-id-${idx}`}
                    type="number"
                    className="form-input"
                    {...register(`plans.${idx}.planId` as const)}
                  />
                </div>
                <div className="field">
                  <label htmlFor={`plan-name-${idx}`}>Plan Name</label>
                  <input
                    id={`plan-name-${idx}`}
                    type="text"
                    className="form-input"
                    {...register(`plans.${idx}.planName` as const)}
                  />
                </div>
                <div className="field">
                  <label htmlFor={`plan-coverage-${idx}`}>Coverage %</label>
                  <input
                    id={`plan-coverage-${idx}`}
                    type="number"
                    className="form-input"
                    {...register(`plans.${idx}.coveragePercent` as const)}
                  />
                </div>
                <div className="field">
                  <label htmlFor={`plan-max-${idx}`}>Max Coverage (₹)</label>
                  <input
                    id={`plan-max-${idx}`}
                    type="number"
                    className="form-input"
                    {...register(`plans.${idx}.maxCoverageAmount` as const)}
                  />
                </div>
                <button
                  type="button"
                  className="btn btn--ghost btn--sm"
                  onClick={() => removePlan(idx)}
                >
                  ✕
                </button>
              </div>
            ))}
            <button
              type="button"
              className="btn btn--secondary btn--sm"
              onClick={() =>
                appendPlan({
                  planId: 0,
                  planName: "",
                  coveragePercent: 100,
                  maxCoverageAmount: undefined,
                })
              }
            >
              + Add Plan
            </button>
          </div>

          {/* Live preview */}
          <div className="package-form__preview">
            <div className="package-form__preview-card">
              <span className="package-form__preview-label">Package Total</span>
              <span className="package-form__preview-value">
                ₹ {totalValue.toFixed(2)}
              </span>
              <span className="package-form__preview-label">Sell Price</span>
              <span className="package-form__preview-value package-form__preview-value--price">
                ₹ {(watch("sellPrice") || 0).toFixed(2)}
              </span>
            </div>
          </div>

          <div className="slideover__actions">
            <button type="button" className="btn btn--secondary" onClick={onCancel}>
              Cancel
            </button>
            <button type="submit" className="btn btn--primary" disabled={isPending}>
              {isPending ? "Saving..." : editingId ? "Update Package" : "Create Package"}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
