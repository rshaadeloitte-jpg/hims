import { useState } from "react";
import { useForm, useFieldArray } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import {
  poRequestSchema,
  type PoRequestValues,
} from "./zodSchema";

interface PORequestProps {
  onSubmitted?: (id: number) => void;
}

interface Department {
  departmentId: number;
  departmentName: string;
  headName?: string;
}

const DEPARTMENTS: Department[] = [
  { departmentId: 1, departmentName: "General Ward", headName: "Dr. A" },
  { departmentId: 2, departmentName: "ICU", headName: "Dr. B" },
  { departmentId: 3, departmentName: "Pharmacy", headName: "Mr. C" },
  { departmentId: 4, departmentName: "Laboratory", headName: "Dr. D" },
  { departmentId: 5, departmentName: "Radiology", headName: "Dr. E" },
  { departmentId: 6, departmentName: "Operation Theatre", headName: "Dr. F" },
  { departmentId: 7, departmentName: "Housekeeping", headName: "Mr. G" },
  { departmentId: 8, departmentName: "Biomedical Engineering", headName: "Eng. H" },
];

/**
 * PORequest — Purchase Order Requisition workflow.
 *
 * Flow:
 *   1. Select department + requester
 *   2. Add item lines (item search, quantity, estimated rate)
 *   3. Review total → submit for approval
 *   4. Status tracking: DRAFT → PENDING_APPROVAL → APPROVED → ORDERED
 */
export function PORequest({ onSubmitted }: PORequestProps) {
  const [step, setStep] = useState<"draft" | "review" | "submitted">("draft");
  const [submittedId, setSubmittedId] = useState<number | null>(null);

  const {
    register,
    control,
    handleSubmit,
    watch,
    setValue,
    formState: { errors, isSubmitting },
  } = useForm<PoRequestValues>({
    resolver: zodResolver(poRequestSchema),
    defaultValues: {
      departmentId: 0,
      departmentName: "",
      requestedBy: "",
      priority: "MEDIUM",
      justification: "",
      totalEstimatedAmount: 0,
      lines: [],
    },
  });

  const { fields, append, remove } = useFieldArray({
    control,
    name: "lines",
  });

  const lines = watch("lines");
  const totalAmount = lines.reduce(
    (sum, line) => sum + (line.quantity ?? 0) * (line.estimatedRate ?? 0),
    0,
  );

  const addLine = () => {
    append({
      itemId: 0,
      itemName: "",
      uom: "EA",
      quantity: 1,
      estimatedRate: 0,
      specifications: "",
      remarks: "",
    });
  };

  const removeLine = (index: number) => {
    if (fields.length > 1) {
      remove(index);
    }
  };

  const onSubmit = async (values: PoRequestValues) => {
    try {
      const payload = { ...values, totalEstimatedAmount: totalAmount };
      const resp = await fetch("/api/v1/po/requisitions", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(payload),
      });
      if (!resp.ok) throw new Error(`HTTP ${resp.status}`);
      const data = await resp.json();
      setSubmittedId(data.requisitionId);
      setStep("submitted");
      onSubmitted?.(data.requisitionId);
    } catch (err) {
      console.error("PO submission failed:", err);
    }
  };

  if (step === "submitted") {
    return (
      <div className="rmrs-po-request">
        <div className="card text-center py-8">
          <div className="text-4xl mb-3">✓</div>
          <h2 className="text-xl font-semibold text-green-700">
            Requisition Submitted
          </h2>
          <p className="text-gray-500 mt-1">Requisition #{submittedId}</p>
          <button
            type="button"
            className="btn btn--primary mt-4"
            onClick={() => {
              setStep("draft");
              setSubmittedId(null);
            }}
          >
            Create New Requisition
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="rmrs-po-request">
      <div className="page-header">
        <h1>Purchase Requisition</h1>
        <p className="text-sm text-gray-500">
          Create a purchase request for department supplies
        </p>
      </div>

      {/* Step indicator */}
      <div className="flex gap-2 mb-6">
        {["draft", "review"].map((s, idx) => (
          <div
            key={s}
            className={`flex items-center gap-2 px-3 py-1 rounded-full text-sm ${
              step === s
                ? "bg-blue-100 text-blue-800 font-medium"
                : "bg-gray-100 text-gray-500"
            }`}
          >
            <span className="w-5 h-5 rounded-full bg-current text-white text-xs flex items-center justify-center">
              {idx + 1}
            </span>
            {s === "draft" ? "Draft" : "Review"}
          </div>
        ))}
      </div>

      {step === "draft" && (
        <form
          onSubmit={handleSubmit(() => setStep("review"))}
          className="space-y-6"
        >
          {/* Header info */}
          <div className="card">
            <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
              <div>
                <label className="form-label">Department</label>
                <select
                  className="form-input"
                  {...register("departmentId")}
                  onChange={(e) => {
                    const dept = DEPARTMENTS.find(
                      (d) => d.departmentId === Number(e.target.value),
                    );
                    setValue("departmentId", Number(e.target.value));
                    setValue("departmentName", dept?.departmentName ?? "");
                  }}
                >
                  <option value={0}>Select department...</option>
                  {DEPARTMENTS.map((d) => (
                    <option key={d.departmentId} value={d.departmentId}>
                      {d.departmentName}
                    </option>
                  ))}
                </select>
                {errors.departmentId && (
                  <span className="field-error">{errors.departmentId.message}</span>
                )}
              </div>

              <div>
                <label className="form-label" htmlFor="requestedBy">
                  Requested By
                </label>
                <input
                  id="requestedBy"
                  type="text"
                  className="form-input"
                  placeholder="Your name"
                  {...register("requestedBy")}
                />
                {errors.requestedBy && (
                  <span className="field-error">{errors.requestedBy.message}</span>
                )}
              </div>

              <div>
                <label className="form-label">Priority</label>
                <div className="flex gap-2" role="group">
                  {(["LOW", "MEDIUM", "HIGH", "URGENT"] as const).map((p) => (
                    <button
                      key={p}
                      type="button"
                      className={`btn btn--sm ${
                        watch("priority") === p ? "btn--primary" : "btn--secondary"
                      }`}
                      onClick={() => setValue("priority", p)}
                    >
                      {p.charAt(0) + p.slice(1).toLowerCase()}
                    </button>
                  ))}
                </div>
              </div>
            </div>

            <div className="mt-4">
              <label className="form-label" htmlFor="justification">
                Justification
              </label>
              <textarea
                id="justification"
                className="form-input"
                rows={3}
                placeholder="Why is this purchase needed?"
                {...register("justification")}
              />
              {errors.justification && (
                <span className="field-error">{errors.justification.message}</span>
              )}
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-4 mt-4">
              <div>
                <label className="form-label" htmlFor="expectedDeliveryDate">
                  Expected Delivery Date
                </label>
                <input
                  id="expectedDeliveryDate"
                  type="date"
                  className="form-input"
                  {...register("expectedDeliveryDate")}
                />
              </div>
              <div>
                <label className="form-label" htmlFor="vendorSuggestion">
                  Suggested Vendor
                </label>
                <input
                  id="vendorSuggestion"
                  type="text"
                  className="form-input"
                  placeholder="Optional vendor name"
                  {...register("vendorSuggestion")}
                />
              </div>
            </div>
          </div>

          {/* Item lines */}
          <div className="card">
            <div className="flex justify-between items-center mb-3">
              <h2 className="text-lg font-semibold">Items</h2>
              <button
                type="button"
                className="btn btn--sm btn--secondary"
                onClick={addLine}
              >
                + Add Item
              </button>
            </div>

            {fields.length === 0 ? (
              <div className="text-center py-6 text-gray-400">
                No items added yet. Click "Add Item" to begin.
              </div>
            ) : (
              <div className="overflow-x-auto">
                <table className="data-table">
                  <thead>
                    <tr>
                      <th>Item</th>
                      <th>UOM</th>
                      <th className="text-right">Qty</th>
                      <th className="text-right">Rate (est.)</th>
                      <th className="text-right">Total</th>
                      <th>Specs</th>
                      <th>Actions</th>
                    </tr>
                  </thead>
                  <tbody>
                    {fields.map((field, index) => (
                      <tr key={field.id}>
                        <td>
                          <input
                            type="text"
                            className="form-input form-input--sm"
                            placeholder="Item name"
                            {...register(`lines.${index}.itemName`)}
                          />
                        </td>
                        <td>
                          <input
                            type="text"
                            className="form-input form-input--sm w-16"
                            {...register(`lines.${index}.uom`)}
                          />
                        </td>
                        <td className="text-right">
                          <input
                            type="number"
                            className="form-input form-input--sm text-right w-20"
                            {...register(`lines.${index}.quantity`)}
                          />
                        </td>
                        <td className="text-right">
                          <input
                            type="number"
                            step="0.01"
                            className="form-input form-input--sm text-right w-24"
                            {...register(`lines.${index}.estimatedRate`)}
                          />
                        </td>
                        <td className="text-right font-mono font-medium">
                          ₹
                          {(
                            (lines[index]?.quantity ?? 0) *
                            (lines[index]?.estimatedRate ?? 0)
                          ).toFixed(2)}
                        </td>
                        <td>
                          <input
                            type="text"
                            className="form-input form-input--sm"
                            placeholder="Specs/remarks"
                            {...register(`lines.${index}.specifications`)}
                          />
                        </td>
                        <td>
                          {fields.length > 1 && (
                            <button
                              type="button"
                              className="btn btn--xs btn--danger"
                              onClick={() => removeLine(index)}
                            >
                              ✕
                            </button>
                          )}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                  <tfoot>
                    <tr className="bg-gray-50">
                      <td colSpan={4} className="text-right font-semibold">
                        Total Estimated Amount
                      </td>
                      <td className="text-right font-mono font-bold text-lg">
                        ₹ {totalAmount.toFixed(2)}
                      </td>
                      <td colSpan={2}></td>
                    </tr>
                  </tfoot>
                </table>
              </div>
            )}

            {errors.lines && (
              <div className="alert alert--error mt-3">
                {errors.lines.message ?? "At least one item is required"}
              </div>
            )}
          </div>

          {/* Actions */}
          <div className="flex justify-end gap-3">
            <button type="button" className="btn btn--secondary">
              Save Draft
            </button>
            <button type="submit" className="btn btn--primary" disabled={fields.length === 0}>
              Review & Submit
            </button>
          </div>
        </form>
      )}

      {/* Review step */}
      {step === "review" && (
        <div className="space-y-6">
          <div className="card">
            <h2 className="text-lg font-semibold mb-4">Review Requisition</h2>
            <div className="grid grid-cols-2 gap-4 text-sm">
              <div>
                <span className="text-gray-500">Department:</span>{" "}
                <span className="font-medium">
                  {DEPARTMENTS.find((d) => d.departmentId === watch("departmentId"))
                    ?.departmentName ?? "—"}
                </span>
              </div>
              <div>
                <span className="text-gray-500">Requested By:</span>{" "}
                <span className="font-medium">{watch("requestedBy") || "—"}</span>
              </div>
              <div>
                <span className="text-gray-500">Priority:</span>{" "}
                <span className="badge badge--info">{watch("priority")}</span>
              </div>
              <div>
                <span className="text-gray-500">Expected Delivery:</span>{" "}
                <span className="font-medium">
                  {watch("expectedDeliveryDate") ?? "—"}
                </span>
              </div>
            </div>
            <div className="mt-4">
              <span className="text-gray-500">Justification:</span>
              <p className="mt-1 text-sm">{watch("justification") || "—"}</p>
            </div>
          </div>

          <div className="card">
            <h3 className="font-semibold mb-3">Items ({fields.length})</h3>
            <table className="data-table">
              <thead>
                <tr>
                  <th>Item</th>
                  <th>UOM</th>
                  <th className="text-right">Qty</th>
                  <th className="text-right">Rate</th>
                  <th className="text-right">Total</th>
                </tr>
              </thead>
              <tbody>
                {lines.map((line, i) => (
                  <tr key={i}>
                    <td>{line.itemName}</td>
                    <td>{line.uom}</td>
                    <td className="text-right">{line.quantity}</td>
                    <td className="text-right">₹{(line.estimatedRate ?? 0).toFixed(2)}</td>
                    <td className="text-right font-mono">
                      ₹
                      {((line.quantity ?? 0) * (line.estimatedRate ?? 0)).toFixed(2)}
                    </td>
                  </tr>
                ))}
              </tbody>
              <tfoot>
                <tr className="bg-blue-50">
                  <td colSpan={4} className="text-right font-bold">
                    Grand Total
                  </td>
                  <td className="text-right font-mono font-bold text-lg text-blue-800">
                    ₹{totalAmount.toFixed(2)}
                  </td>
                </tr>
              </tfoot>
            </table>
          </div>

          <div className="flex justify-between">
            <button
              type="button"
              className="btn btn--secondary"
              onClick={() => setStep("draft")}
            >
              ← Back to Edit
            </button>
            <button
              type="button"
              className="btn btn--primary btn--lg"
              onClick={handleSubmit(onSubmit)}
              disabled={isSubmitting}
            >
              {isSubmitting ? "Submitting..." : "Submit for Approval"}
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
