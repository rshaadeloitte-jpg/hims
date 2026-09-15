import { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { useCreateWasteRequest, useStores, useWasteBags } from "./queries";
import {
  wasteRequestSchema,
  wasteBagLineSchema,
  type WasteRequestValues,
  type WasteBagLine,
} from "./zodSchema";

/**
 * WasteRequest — Stripe-style progressive form for creating a biomedical waste
 * request. Ported from createBioMedicalRequest.html + addNewBioRequest.
 *
 * Workflow:
 *   1. Pick from/to store + date.
 *   2. Add bag lines (waste-bag master + qty + weight + remark).
 *   3. Submit → backend auto-generates barcodes per bag.
 *
 * The form uses react-hook-form + zodResolver for real-time validation
 * (Stripe-style inline errors).
 */
interface WasteRequestProps {
  unitId: number;
  defaultStoreId?: number;
  onSuccess?: (requisitionNo: string) => void;
  onCancel?: () => void;
}

export function WasteRequest({
  unitId,
  defaultStoreId,
  onSuccess,
  onCancel,
}: WasteRequestProps) {
  const { data: stores = [] } = useStores(unitId);
  const { data: bagMasters = [] } = useWasteBags();
  const createRequest = useCreateWasteRequest();

  const [bagLines, setBagLines] = useState<WasteBagLine[]>([]);
  const [bagErrors, setBagErrors] = useState<Record<string, string>>({});

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<WasteRequestValues>({
    resolver: zodResolver(wasteRequestSchema),
    defaultValues: {
      unitId,
      storeId: defaultStoreId,
      date: new Date().toISOString().slice(0, 10),
      details: [],
    },
  });

  const addBagLine = () => {
    const result = wasteBagLineSchema.safeParse({
      bagMasterId: 0,
      qty: 1,
      weight: 0,
      remark: "",
    });
    if (!result.success) return;
    setBagLines((prev) => [...prev, result.data]);
  };

  const updateBagLine = (index: number, field: keyof WasteBagLine, value: unknown) => {
    setBagLines((prev) =>
      prev.map((line, i) => (i === index ? { ...line, [field]: value } : line)),
    );
  };

  const removeBagLine = (index: number) => {
    setBagLines((prev) => prev.filter((_, i) => i !== index));
  };

  const onSubmit = (values: WasteRequestValues) => {
    if (bagLines.length === 0) {
      setBagErrors({ form: "Add at least one waste bag" });
      return;
    }
    // Validate each bag line
    const validated: WasteBagLine[] = [];
    for (const line of bagLines) {
      const r = wasteBagLineSchema.safeParse(line);
      if (!r.success) {
        setBagErrors({ form: "Fix bag line errors before submitting" });
        return;
      }
      validated.push(r.data);
    }
    createRequest.mutate(
      { ...values, details: validated },
      {
        onSuccess: (res) => {
          onSuccess?.(res.requisitionNo);
        },
      },
    );
  };

  return (
    <div className="card">
      <h2>Create Biomedical Waste Request</h2>
      <form onSubmit={handleSubmit(onSubmit)} className="master-form">
        <div className="form-grid">
          <div className={`field ${errors.storeId ? "field--error" : ""}`}>
            <label htmlFor="storeId">
              From Store <span className="req">*</span>
            </label>
            <select id="storeId" {...register("storeId")}>
              <option value="">Select store…</option>
              {stores.map((s) => (
                <option key={s.id} value={s.id}>
                  {s.description}
                </option>
              ))}
            </select>
            {errors.storeId && (
              <span className="field-error">{errors.storeId.message}</span>
            )}
          </div>

          <div className={`field ${errors.toStoreId ? "field--error" : ""}`}>
            <label htmlFor="toStoreId">To Store</label>
            <select id="toStoreId" {...register("toStoreId")}>
              <option value="">Select…</option>
              {stores
                .filter((s) => s.isBioMedicalStore)
                .map((s) => (
                  <option key={s.id} value={s.id}>
                    {s.description}
                  </option>
                ))}
            </select>
            {errors.toStoreId && (
              <span className="field-error">{errors.toStoreId.message}</span>
            )}
          </div>

          <div className={`field ${errors.date ? "field--error" : ""}`}>
            <label htmlFor="date">
              Date <span className="req">*</span>
            </label>
            <input id="date" type="date" {...register("date")} />
            {errors.date && (
              <span className="field-error">{errors.date.message}</span>
            )}
          </div>
        </div>

        {/* Bag lines — Stripe-style inline add */}
        <div className="bag-lines">
          <div className="bag-lines__header">
            <h3>Waste Bags</h3>
            <button
              type="button"
              className="btn btn-secondary btn-sm"
              onClick={addBagLine}
            >
              + Add Bag
            </button>
          </div>

          {bagErrors.form && (
            <div className="alert alert--error" role="alert">
              {bagErrors.form}
            </div>
          )}

          {bagLines.length > 0 && (
            <table className="table">
              <thead>
                <tr>
                  <th>Bag Type</th>
                  <th>Qty</th>
                  <th>Weight (kg)</th>
                  <th>Remark</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                {bagLines.map((line, idx) => (
                  <tr key={idx}>
                    <td>
                      <select
                        value={line.bagMasterId || ""}
                        onChange={(e) =>
                          updateBagLine(idx, "bagMasterId", Number(e.target.value))
                        }
                      >
                        <option value="">Select…</option>
                        {bagMasters.map((b) => (
                          <option key={b.id} value={b.id}>
                            {b.description}
                          </option>
                        ))}
                      </select>
                    </td>
                    <td>
                      <input
                        type="number"
                        min={1}
                        value={line.qty}
                        onChange={(e) =>
                          updateBagLine(idx, "qty", Number(e.target.value))
                        }
                      />
                    </td>
                    <td>
                      <input
                        type="number"
                        step="0.01"
                        min={0}
                        value={line.weight}
                        onChange={(e) =>
                          updateBagLine(idx, "weight", Number(e.target.value))
                        }
                      />
                    </td>
                    <td>
                      <input
                        type="text"
                        value={line.remark ?? ""}
                        onChange={(e) =>
                          updateBagLine(idx, "remark", e.target.value)
                        }
                        placeholder="Optional"
                      />
                    </td>
                    <td>
                      <button
                        type="button"
                        className="btn btn-sm"
                        onClick={() => removeBagLine(idx)}
                        aria-label="Remove bag"
                      >
                        ✕
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}

          {bagLines.length === 0 && (
            <p className="muted">No bags added yet. Click "+ Add Bag" to start.</p>
          )}
        </div>

        {createRequest.isError && (
          <div className="alert alert--error" role="alert">
            Failed to create request:{" "}
            {(createRequest.error as Error)?.message ?? "Unknown error"}
          </div>
        )}

        <div className="form-actions">
          <button
            type="button"
            className="btn btn-secondary"
            onClick={onCancel}
            disabled={createRequest.isPending}
          >
            Cancel
          </button>
          <button
            type="submit"
            className="btn btn-primary"
            disabled={createRequest.isPending}
          >
            {createRequest.isPending ? "Submitting…" : "Create Request"}
          </button>
        </div>
      </form>
    </div>
  );
}
