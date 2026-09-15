import { useState, useCallback, useRef } from "react";
import { useForm, useFieldArray } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import {
  useOrderSets,
  useCreateOrderSet,
  useUpdateOrderSet,
  useApplyOrderSet,
} from "./queries";
import {
  orderSetSchema,
  type OrderSetForm,
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

const PRIORITIES = ["ROUTINE", "URGENT", "STAT"] as const;

/**
 * OrderSetBuilder — compose named, ordered bundles of service orders.
 * Linear-style drag-and-drop: native HTML5 DnD reorders items in the
 * builder list; items can be added from a palette, removed, and reordered.
 * Stripe-style card grid for existing order sets.
 */
export function OrderSetBuilder() {
  const [showForm, setShowForm] = useState(false);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [draggedIndex, setDraggedIndex] = useState<number | null>(null);
  const [dragOverIndex, setDragOverIndex] = useState<number | null>(null);
  const dragCounter = useRef(0);

  const { data: orderSets = [], isLoading } = useOrderSets();
  const createOrderSet = useCreateOrderSet();
  const updateOrderSet = useUpdateOrderSet();
  const applyOrderSet = useApplyOrderSet();

  const handleSubmit = async (values: OrderSetForm) => {
    try {
      if (editingId) {
        await updateOrderSet.mutateAsync({ id: editingId, payload: values });
      } else {
        await createOrderSet.mutateAsync(values);
      }
      setShowForm(false);
      setEditingId(null);
    } catch (err) {
      console.error("Order set save failed:", err);
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

  const handleApply = useCallback(
    async (orderSetId: number) => {
      const patientId = Number(prompt("Patient ID to apply this order set:"));
      if (!patientId || isNaN(patientId)) return;
      try {
        await applyOrderSet.mutateAsync({ orderSetId, patientId });
        alert("Order set applied successfully.");
      } catch (err) {
        console.error("Apply order set failed:", err);
      }
    },
    [applyOrderSet],
  );

  return (
    <section className="page">
      <header className="page-header">
        <div>
          <h1>Order Sets</h1>
          <p className="muted">
            Compose named bundles of service orders. Drag to reorder items.
          </p>
        </div>
        <button
          type="button"
          className="btn btn--primary"
          onClick={handleNew}
          disabled={createOrderSet.isPending}
        >
          + New Order Set
        </button>
      </header>

      {/* Order set cards — Stripe-style grid */}
      <section className="order-set__grid" aria-label="Order sets">
        {isLoading ? (
          <div className="loading-state">
            <span className="spinner" aria-hidden="true" /> Loading order sets…
          </div>
        ) : orderSets.length === 0 ? (
          <p className="muted">No order sets configured. Create one to get started.</p>
        ) : (
          orderSets.map((os) => (
            <article
              key={os.id}
              className={`order-set-card${
                os.status === "INACTIVE" ? " order-set-card--inactive" : ""
              }`}
            >
              <div className="order-set-card__header">
                <span className="order-set-card__code">{os.code}</span>
                <span
                  className={`order-set-card__status${
                    os.status === "INACTIVE"
                      ? " order-set-card__status--inactive"
                      : ""
                  }`}
                >
                  {os.status}
                </span>
              </div>
              <h3 className="order-set-card__name">{os.name}</h3>
              <span className="order-set-card__category">{os.category}</span>
              {os.description && (
                <p className="order-set-card__desc">{os.description}</p>
              )}
              <div className="order-set-card__meta">
                <span>{os.items.length} item(s)</span>
              </div>
              <div className="order-set-card__actions">
                <button
                  type="button"
                  className="btn btn--ghost btn--sm"
                  onClick={() => handleApply(os.id)}
                  disabled={os.status === "INACTIVE"}
                >
                  Apply
                </button>
                <button
                  type="button"
                  className="btn btn--ghost btn--sm"
                  onClick={() => handleEdit(os.id)}
                >
                  Edit
                </button>
              </div>
            </article>
          ))
        )}
      </section>

      {/* Form slideover */}
      {showForm && (
        <OrderSetForm
          editingId={editingId}
          onSubmit={handleSubmit}
          onCancel={() => {
            setShowForm(false);
            setEditingId(null);
          }}
          isPending={createOrderSet.isPending || updateOrderSet.isPending}
          draggedIndex={draggedIndex}
          setDraggedIndex={setDraggedIndex}
          dragOverIndex={dragOverIndex}
          setDragOverIndex={setDragOverIndex}
          dragCounter={dragCounter}
        />
      )}
    </section>
  );
}

// ---- OrderSetForm ----------------------------------------------------------

interface OrderSetFormProps {
  editingId: number | null;
  onSubmit: (values: OrderSetForm) => Promise<void>;
  onCancel: () => void;
  isPending: boolean;
  draggedIndex: number | null;
  setDraggedIndex: (v: number | null) => void;
  dragOverIndex: number | null;
  setDragOverIndex: (v: number | null) => void;
  dragCounter: React.MutableRefObject<number>;
}

function OrderSetForm({
  editingId,
  onSubmit,
  onCancel,
  isPending,
  draggedIndex,
  setDraggedIndex,
  dragOverIndex,
  setDragOverIndex,
  dragCounter,
}: OrderSetFormProps) {
  const {
    register,
    handleSubmit,
    control,
    formState: { errors },
  } = useForm<OrderSetForm>({
    resolver: zodResolver(orderSetSchema),
    defaultValues: {
      name: "",
      code: "",
      category: "CONSUMABLES",
      description: "",
      items: [{ id: crypto.randomUUID(), serviceType: "CONSUMABLES", serviceId: 0, serviceName: "", qty: 1, priority: "ROUTINE" }],
      status: "ACTIVE",
    },
  });

  const { fields, append, remove, move } = useFieldArray({
    control,
    name: "items",
  });

  // ---- Drag-and-drop handlers (Linear-style native HTML5 DnD) ----

  const handleDragStart = (index: number) => (e: React.DragEvent) => {
    setDraggedIndex(index);
    e.dataTransfer.effectAllowed = "move";
    e.dataTransfer.setData("text/plain", String(index));
  };

  const handleDragEnter = (index: number) => (e: React.DragEvent) => {
    e.preventDefault();
    dragCounter.current += 1;
    if (draggedIndex !== null && draggedIndex !== index) {
      setDragOverIndex(index);
    }
  };

  const handleDragOver = (e: React.DragEvent) => {
    e.preventDefault();
    e.dataTransfer.dropEffect = "move";
  };

  const handleDragLeave = () => {
    dragCounter.current -= 1;
    if (dragCounter.current === 0) {
      setDragOverIndex(null);
    }
  };

  const handleDrop = (dropIndex: number) => (e: React.DragEvent) => {
    e.preventDefault();
    dragCounter.current = 0;
    if (draggedIndex !== null && draggedIndex !== dropIndex) {
      move(draggedIndex, dropIndex);
    }
    setDraggedIndex(null);
    setDragOverIndex(null);
  };

  const handleDragEnd = () => {
    dragCounter.current = 0;
    setDraggedIndex(null);
    setDragOverIndex(null);
  };

  const handleAddItem = () => {
    append({
      id: crypto.randomUUID(),
      serviceType: "CONSUMABLES",
      serviceId: 0,
      serviceName: "",
      qty: 1,
      priority: "ROUTINE",
    });
  };

  return (
    <div className="slideover" role="dialog" aria-modal="true" aria-label="Order set form">
      <div className="slideover__backdrop" onClick={onCancel} />
      <div className="slideover__panel slideover__panel--wide">
        <header className="slideover__header">
          <h2>{editingId ? `Edit Order Set #${editingId}` : "New Order Set"}</h2>
          <button type="button" className="btn btn--ghost btn--sm" onClick={onCancel}>
            ✕
          </button>
        </header>

        <form className="slideover__form" onSubmit={handleSubmit(onSubmit)}>
          {/* Order set details */}
          <h3 className="slideover__section-title">Order Set Details</h3>
          <div className="form-grid">
            <div className="field">
              <label htmlFor="os-name">Name *</label>
              <input
                id="os-name"
                type="text"
                className="form-input"
                {...register("name")}
                aria-invalid={errors.name ? true : undefined}
              />
              {errors.name && <span className="field-error">{errors.name.message}</span>}
            </div>
            <div className="field">
              <label htmlFor="os-code">Code *</label>
              <input
                id="os-code"
                type="text"
                className="form-input"
                {...register("code")}
                aria-invalid={errors.code ? true : undefined}
              />
              {errors.code && <span className="field-error">{errors.code.message}</span>}
            </div>
            <div className="field">
              <label htmlFor="os-category">Category</label>
              <select id="os-category" className="form-input" {...register("category")}>
                {SERVICE_TYPES.map((t) => (
                  <option key={t} value={t}>{t}</option>
                ))}
              </select>
            </div>
            <div className="field">
              <label htmlFor="os-desc">Description</label>
              <input
                id="os-desc"
                type="text"
                className="form-input"
                {...register("description")}
              />
            </div>
            <div className="field">
              <label htmlFor="os-status">Status</label>
              <select id="os-status" className="form-input" {...register("status")}>
                <option value="ACTIVE">Active</option>
                <option value="INACTIVE">Inactive</option>
              </select>
            </div>
          </div>

          {/* Items — draggable list */}
          <h3 className="slideover__section-title">
            Items ({fields.length})
            <span className="muted" style={{ fontWeight: 400, fontSize: "12px", marginLeft: "8px" }}>
              drag to reorder
            </span>
          </h3>

          {errors.items && !errors.items.message && (
            <span className="field-error">{errors.items.message}</span>
          )}

          <div className="order-set-form__items">
            {fields.map((field, idx) => (
              <div
                key={field.id}
                className={`order-set-form__item${
                  dragOverIndex === idx ? " order-set-form__item--drag-over" : ""
                }${draggedIndex === idx ? " order-set-form__item--dragging" : ""}`}
                draggable
                onDragStart={handleDragStart(idx)}
                onDragEnter={handleDragEnter(idx)}
                onDragOver={handleDragOver}
                onDragLeave={handleDragLeave}
                onDrop={handleDrop(idx)}
                onDragEnd={handleDragEnd}
              >
                <div className="order-set-form__drag-handle" aria-hidden="true">
                  ⠿
                </div>
                <div className="order-set-form__item-fields">
                  <div className="field">
                    <label htmlFor={`item-type-${idx}`}>Type</label>
                    <select
                      id={`item-type-${idx}`}
                      className="form-input"
                      {...register(`items.${idx}.serviceType` as const)}
                    >
                      {SERVICE_TYPES.map((t) => (
                        <option key={t} value={t}>{t}</option>
                      ))}
                    </select>
                  </div>
                  <div className="field">
                    <label htmlFor={`item-svcid-${idx}`}>Service ID</label>
                    <input
                      id={`item-svcid-${idx}`}
                      type="number"
                      className="form-input"
                      {...register(`items.${idx}.serviceId` as const)}
                    />
                  </div>
                  <div className="field">
                    <label htmlFor={`item-name-${idx}`}>Service Name</label>
                    <input
                      id={`item-name-${idx}`}
                      type="text"
                      className="form-input"
                      {...register(`items.${idx}.serviceName` as const)}
                    />
                  </div>
                  <div className="field">
                    <label htmlFor={`item-qty-${idx}`}>Qty</label>
                    <input
                      id={`item-qty-${idx}`}
                      type="number"
                      className="form-input"
                      {...register(`items.${idx}.qty` as const)}
                    />
                  </div>
                  <div className="field">
                    <label htmlFor={`item-pri-${idx}`}>Priority</label>
                    <select
                      id={`item-pri-${idx}`}
                      className="form-input"
                      {...register(`items.${idx}.priority` as const)}
                    >
                      {PRIORITIES.map((p) => (
                        <option key={p} value={p}>{p}</option>
                      ))}
                    </select>
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
              onClick={handleAddItem}
            >
              + Add Item
            </button>
          </div>

          {/* Submit */}
          <div className="slideover__footer">
            <button type="submit" className="btn btn--primary" disabled={isPending}>
              {isPending ? "Saving…" : editingId ? "Update Order Set" : "Create Order Set"}
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
