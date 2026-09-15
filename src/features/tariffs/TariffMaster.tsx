import { useState, useCallback } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import {
  useServiceCategories,
  useTariffs,
  useCreateTariff,
  useUpdateTariff,
} from "./queries";
import {
  tariffSchema,
  tariffFilterSchema,
  type TariffValues,
  type TariffFilterValues,
} from "./zodSchema";

/**
 * TariffMaster — CRUD for service tariffs with category filter cards.
 * Stripe pricing card pattern: category cards form the top row (selectable),
 * each tariff card below shows base rate + copay; inline edit slideover.
 */
export function TariffMaster() {
  const [filter, setFilter] = useState<TariffFilterValues>({
    serviceCategory: "",
    search: undefined,
    status: "",
    page: 0,
    size: 20,
  });
  const [showForm, setShowForm] = useState(false);
  const [editingId, setEditingId] = useState<number | null>(null);

  const { data: categories = [] } = useServiceCategories();
  const { data: tariffs = [], isLoading } = useTariffs(filter);
  const createTariff = useCreateTariff();
  const updateTariff = useUpdateTariff();

  const handleCategorySelect = useCallback((category: string) => {
    setFilter((prev) => ({
      ...prev,
      serviceCategory: prev.serviceCategory === category ? "" : category,
      page: 0,
    }));
  }, []);

  const handleSubmit = async (values: TariffValues) => {
    try {
      if (editingId) {
        await updateTariff.mutateAsync({ id: editingId, payload: values });
      } else {
        await createTariff.mutateAsync(values);
      }
      setShowForm(false);
      setEditingId(null);
    } catch (err) {
      console.error("Tariff save failed:", err);
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
          <h1>Tariff Masters</h1>
          <p className="muted">Manage service tariffs by category with rate & copay configuration.</p>
        </div>
        <button
          type="button"
          className="btn btn--primary"
          onClick={handleNew}
          disabled={createTariff.isPending}
        >
          + New Tariff
        </button>
      </header>

      {/* Category cards — Stripe-style top row */}
      <section className="tariff-master__categories" aria-label="Service categories">
        <div className="tariff-master__category-row">
          {categories.map((cat) => (
            <button
              key={cat}
              type="button"
              className={`tariff-master__category-card${
                filter.serviceCategory === cat
                  ? " tariff-master__category-card--active"
                  : ""
              }`}
              aria-pressed={filter.serviceCategory === cat}
              onClick={() => handleCategorySelect(cat)}
            >
              <span className="tariff-master__category-icon">
                {getCategoryIcon(cat)}
              </span>
              <span className="tariff-master__category-label">
                {cat.replace(/_/g, " ")}
              </span>
            </button>
          ))}
        </div>
      </section>

      {/* Filter bar */}
      <section className="card toolbar">
        <form
          className="toolbar-row"
          onSubmit={(e) => {
            e.preventDefault();
          }}
        >
          <div className="field field--search">
            <label htmlFor="tariff-search">Search</label>
            <input
              id="tariff-search"
              type="text"
              placeholder="Service name or ID..."
              defaultValue={filter.search ?? ""}
              onKeyDown={(e) => {
                if (e.key === "Enter") {
                  setFilter((prev) => ({
                    ...prev,
                    search: (e.target as HTMLInputElement).value || undefined,
                    page: 0,
                  }));
                }
              }}
            />
          </div>
          <div className="field">
            <label htmlFor="tariff-status">Status</label>
            <select
              id="tariff-status"
              value={filter.status ?? ""}
              onChange={(e) =>
                setFilter((prev) => ({
                  ...prev,
                  status: e.target.value as "" | "ACTIVE" | "INACTIVE",
                  page: 0,
                }))
              }
            >
              <option value="">All</option>
              <option value="ACTIVE">Active</option>
              <option value="INACTIVE">Inactive</option>
            </select>
          </div>
        </form>
      </section>

      {/* Tariff list — Stripe pricing card grid */}
      <section className="tariff-master__grid" aria-label="Tariffs">
        {isLoading ? (
          <div className="loading-state">
            <span className="spinner" aria-hidden="true" /> Loading tariffs…
          </div>
        ) : tariffs.length === 0 ? (
          <p className="muted">No tariffs configured. Create one to get started.</p>
        ) : (
          tariffs.map((t) => (
            <article
              key={t.id}
              className={`tariff-card${t.status === "INACTIVE" ? " tariff-card--inactive" : ""}`}
              onClick={() => handleEdit(t.id)}
              role="button"
              tabIndex={0}
              onKeyDown={(e) => {
                if (e.key === "Enter" || e.key === " ") handleEdit(t.id);
              }}
            >
              <div className="tariff-card__header">
                <span className="tariff-card__category-badge">{t.serviceCategory}</span>
                <span
                  className={`tariff-card__status${
                    t.status === "INACTIVE" ? " tariff-card__status--inactive" : ""
                  }`}
                >
                  {t.status}
                </span>
              </div>
              <h3 className="tariff-card__name">{t.serviceName}</h3>
              <p className="tariff-card__id">ID: {t.serviceId}</p>
              <div className="tariff-card__rate">
                <span className="tariff-card__currency">₹</span>
                <span className="tariff-card__amount">{t.baseRate.toFixed(2)}</span>
              </div>
              {t.copayPercent > 0 && (
                <div className="tariff-card__copay">
                  Copay: {t.copayPercent}%
                </div>
              )}
              {t.packageClass && (
                <div className="tariff-card__class">{t.packageClass}</div>
              )}
            </article>
          ))
        )}
      </section>

      {/* Inline form slideover */}
      {showForm && (
        <TariffForm
          editingId={editingId}
          defaultCategory={filter.serviceCategory || undefined}
          onSubmit={handleSubmit}
          onCancel={() => {
            setShowForm(false);
            setEditingId(null);
          }}
          isPending={createTariff.isPending || updateTariff.isPending}
        />
      )}
    </section>
  );
}

// ---- TariffForm -----------------------------------------------------------

interface TariffFormProps {
  editingId: number | null;
  defaultCategory?: string;
  onSubmit: (values: TariffValues) => Promise<void>;
  onCancel: () => void;
  isPending: boolean;
}

function TariffForm({
  editingId,
  defaultCategory,
  onSubmit,
  onCancel,
  isPending,
}: TariffFormProps) {
  const { register, handleSubmit, formState: { errors }, reset } = useForm<TariffValues>({
    resolver: zodResolver(tariffSchema),
    defaultValues: {
      serviceId: 0,
      serviceName: "",
      serviceCategory: defaultCategory ?? "CONSULTATION",
      baseRate: 0,
      copayPercent: 0,
      packageClass: "",
      status: "ACTIVE",
    },
  });

  return (
    <div className="slideover" role="dialog" aria-modal="true" aria-label="Tariff form">
      <div className="slideover__backdrop" onClick={onCancel} />
      <div className="slideover__panel">
        <header className="slideover__header">
          <h2>{editingId ? `Edit Tariff #${editingId}` : "New Tariff"}</h2>
          <button type="button" className="btn btn--ghost btn--sm" onClick={onCancel}>
            ✕
          </button>
        </header>

        <form
          className="slideover__form"
          onSubmit={handleSubmit(onSubmit)}
        >
          <div className="form-grid">
            <div className="field">
              <label htmlFor="svc-id">Service ID *</label>
              <input
                id="svc-id"
                type="number"
                className="form-input"
                {...register("serviceId")}
                aria-invalid={errors.serviceId ? true : undefined}
              />
              {errors.serviceId && (
                <span className="field-error">{errors.serviceId.message}</span>
              )}
            </div>
            <div className="field">
              <label htmlFor="svc-name">Service Name *</label>
              <input
                id="svc-name"
                type="text"
                className="form-input"
                {...register("serviceName")}
                aria-invalid={errors.serviceName ? true : undefined}
              />
              {errors.serviceName && (
                <span className="field-error">{errors.serviceName.message}</span>
              )}
            </div>
            <div className="field">
              <label htmlFor="svc-cat">Category *</label>
              <select id="svc-cat" className="form-input" {...register("serviceCategory")}>
                <option value="CONSULTATION">Consultation</option>
                <option value="DIAGNOSTIC">Diagnostic</option>
                <option value="PROCEDURE">Procedure</option>
                <option value="ROOM_CHARGE">Room Charge</option>
                <option value="PHARMACY">Pharmacy</option>
                <option value="LABORATORY">Laboratory</option>
                <option value="RADIOLOGY">Radiology</option>
                <option value="PACKAGE">Package</option>
                <option value="MISCELLANEOUS">Miscellaneous</option>
              </select>
            </div>
            <div className="field">
              <label htmlFor="base-rate">Base Rate (₹) *</label>
              <input
                id="base-rate"
                type="number"
                step="0.01"
                className="form-input"
                {...register("baseRate")}
                aria-invalid={errors.baseRate ? true : undefined}
              />
              {errors.baseRate && (
                <span className="field-error">{errors.baseRate.message}</span>
              )}
            </div>
            <div className="field">
              <label htmlFor="copay">Copay %</label>
              <input
                id="copay"
                type="number"
                step="0.01"
                min="0"
                max="100"
                className="form-input"
                {...register("copayPercent")}
                aria-invalid={errors.copayPercent ? true : undefined}
              />
              {errors.copayPercent && (
                <span className="field-error">{errors.copayPercent.message}</span>
              )}
            </div>
            <div className="field">
              <label htmlFor="pkg-class">Package Class</label>
              <input
                id="pkg-class"
                type="text"
                className="form-input"
                placeholder="e.g. General, Semi-Private, Deluxe"
                {...register("packageClass")}
              />
            </div>
            <div className="field">
              <label htmlFor="status">Status</label>
              <select id="status" className="form-input" {...register("status")}>
                <option value="ACTIVE">Active</option>
                <option value="INACTIVE">Inactive</option>
              </select>
            </div>
          </div>

          <div className="slideover__actions">
            <button type="button" className="btn btn--secondary" onClick={onCancel}>
              Cancel
            </button>
            <button type="submit" className="btn btn--primary" disabled={isPending}>
              {isPending ? "Saving..." : editingId ? "Update Tariff" : "Create Tariff"}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

// ---- Helpers ---------------------------------------------------------------

function getCategoryIcon(category: string): string {
  const icons: Record<string, string> = {
    CONSULTATION: "🩺",
    DIAGNOSTIC: "🔬",
    PROCEDURE: "⚕️",
    ROOM_CHARGE: "🛏️",
    PHARMACY: "💊",
    LABORATORY: "🧪",
    RADIOLOGY: "📡",
    PACKAGE: "📦",
    MISCELLANEOUS: "📋",
  };
  return icons[category] ?? "📋";
}
