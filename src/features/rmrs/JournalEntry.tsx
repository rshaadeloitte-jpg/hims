import { useState } from "react";
import { useForm, useFieldArray } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import {
  journalEntrySchema,
  type JournalEntryValues,
} from "./zodSchema";

interface JournalEntryProps {
  onSaved?: (entry: { journalId: number; voucherNo: string }) => void;
  onCancel?: () => void;
}

const ACCOUNT_OPTIONS = [
  { id: 1, name: "Cash in Hand", code: "1000" },
  { id: 2, name: "Bank - SBI Current", code: "1010" },
  { id: 3, name: "Bank - HDFC Savings", code: "1011" },
  { id: 4, name: "Patient Receivables", code: "1100" },
  { id: 5, name: "Supplier Payables", code: "2100" },
  { id: 6, name: "Sales Revenue", code: "4000" },
  { id: 7, name: "Purchase Expenses", code: "5000" },
  { id: 8, name: "Bank Charges", code: "5500" },
  { id: 9, name: "Discount Received", code: "4500" },
  { id: 10, name: "Discount Allowed", code: "5500" },
];

/**
 * JournalEntry — multi-leg double-entry bookkeeping form.
 *
 * UX:
 *   • Users add/remove legs dynamically (rows of debit/credit).
 *   • Real-time validation: each leg must have debit XOR credit > 0.
 *   • Real-time balance check: total debits must equal total credits.
 *   • Stripe-inspired card layout with segmented entry-type selector.
 */
export function JournalEntry({ onSaved, onCancel }: JournalEntryProps) {
  const [entryType, setEntryType] = useState<JournalEntryValues["entryType"]>("JOURNAL");

  const {
    register,
    control,
    handleSubmit,
    watch,
    setValue,
    formState: { errors, isSubmitting },
  } = useForm<JournalEntryValues>({
    resolver: zodResolver(journalEntrySchema),
    defaultValues: {
      entryType: "JOURNAL",
      entryDate: new Date().toISOString().slice(0, 10),
      narration: "",
      legs: [{ accountId: 0, accountName: "", debit: 0, credit: 0 }],
    },
  });

  const { fields, append, remove } = useFieldArray({
    control,
    name: "legs",
  });

  // Watch legs for real-time balance calculation
  const legs = watch("legs");
  const totalDebit = legs.reduce((s, l) => s + (l.debit ?? 0), 0);
  const totalCredit = legs.reduce((s, l) => s + (l.credit ?? 0), 0);
  const isBalanced = Math.abs(totalDebit - totalCredit) < 0.001;

  const addLeg = () => {
    append({ accountId: 0, accountName: "", debit: 0, credit: 0 });
  };

  const removeLeg = (index: number) => {
    if (fields.length > 2) {
      remove(index);
    }
  };

  const onSubmit = async (values: JournalEntryValues) => {
    try {
      const resp = await fetch("/api/v1/journals", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(values),
      });
      if (!resp.ok) throw new Error(`HTTP ${resp.status}`);
      const data = await resp.json();
      onSaved?.({ journalId: data.journalId, voucherNo: data.voucherNo });
    } catch (err) {
      console.error("Journal save failed:", err);
    }
  };

  return (
    <div className="rmrs-journal-entry">
      <div className="page-header">
        <h1>Journal Entry</h1>
        <p className="text-sm text-gray-500">Double-entry bookkeeping — debits must equal credits</p>
      </div>

      <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">
        {/* Header / metadata */}
        <div className="card">
          <div className="grid grid-cols-1 md:grid-cols-5 gap-4">
            <div>
              <label className="form-label">Entry Type</label>
              <div className="flex gap-2" role="group">
                {(["RECEIPT", "PAYMENT", "JOURNAL", "CONTRA"] as const).map((type) => (
                  <button
                    key={type}
                    type="button"
                    className={`btn btn--sm ${entryType === type ? "btn--primary" : "btn--secondary"}`}
                    onClick={() => {
                      setEntryType(type);
                      setValue("entryType", type);
                    }}
                  >
                    {type.charAt(0) + type.slice(1).toLowerCase()}
                  </button>
                ))}
              </div>
            </div>

            <div>
              <label className="form-label" htmlFor="entryDate">
                Entry Date
              </label>
              <input
                id="entryDate"
                type="date"
                className="form-input"
                {...register("entryDate")}
              />
              {errors.entryDate && <span className="field-error">{errors.entryDate.message}</span>}
            </div>

            <div className="md:col-span-2">
              <label className="form-label" htmlFor="narration">
                Narration
              </label>
              <input
                id="narration"
                type="text"
                className="form-input"
                placeholder="Describe the transaction..."
                {...register("narration")}
              />
              {errors.narration && <span className="field-error">{errors.narration.message}</span>}
            </div>

            <div>
              <label className="form-label" htmlFor="referenceType">
                Reference Type
              </label>
              <input
                id="referenceType"
                type="text"
                className="form-input"
                placeholder="e.g. INVOICE"
                {...register("referenceType")}
              />
            </div>
          </div>
        </div>

        {/* Legs table — Stripe-style */}
        <div className="card">
          <div className="flex justify-between items-center mb-3">
            <h2 className="text-lg font-semibold">Transaction Legs</h2>
            <button type="button" className="btn btn--sm btn--secondary" onClick={addLeg}>
              + Add Leg
            </button>
          </div>

          <div className="overflow-x-auto">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Account</th>
                  <th>Cost Center</th>
                  <th className="text-right">Debit</th>
                  <th className="text-right">Credit</th>
                  <th>Narration</th>
                  <th className="text-center">Actions</th>
                </tr>
              </thead>
              <tbody>
                {fields.map((_, index) => (
                  <LegRow
                    key={fields[index]?.id ?? `leg-${index}`}
                    index={index}
                    register={register}
                    setValue={setValue}
                    onRemove={() => removeLeg(index)}
                    canRemove={fields.length > 2}
                  />
                ))}
              </tbody>
            </table>
          </div>

          {/* Balance summary */}
          <div className="border-t pt-3 mt-3 space-y-2">
            <div className="flex justify-end gap-8 text-sm">
              <div className="text-right">
                <span className="text-gray-500">Total Debit:</span>
                <span className={`font-mono font-semibold ${totalDebit > 0 ? "text-green-700" : ""}`}>
                  ₹ {totalDebit.toFixed(2)}
                </span>
              </div>
              <div className="text-right">
                <span className="text-gray-500">Total Credit:</span>
                <span className={`font-mono font-semibold ${totalCredit > 0 ? "text-blue-700" : ""}`}>
                  ₹ {totalCredit.toFixed(2)}
                </span>
              </div>
            </div>
            <div className="flex justify-end">
              <span
                className={`inline-flex items-center px-3 py-1 rounded-full text-xs font-medium ${
                  isBalanced
                    ? "bg-green-100 text-green-800"
                    : "bg-red-100 text-red-800"
                }`}
              >
                {isBalanced ? "✓ Balanced" : `⚠ Difference: ₹ ${(totalDebit - totalCredit).toFixed(2)}`}
              </span>
            </div>
          </div>
        </div>

        {/* Form actions */}
        <div className="flex justify-end gap-3">
          {onCancel && (
            <button type="button" className="btn btn--secondary" onClick={onCancel} disabled={isSubmitting}>
              Cancel
            </button>
          )}
          <button
            type="submit"
            className="btn btn--primary"
            disabled={isSubmitting || !isBalanced}
          >
            {isSubmitting ? (
              <>
                <span className="spinner spinner--sm" aria-hidden="true" />
                Saving...
              </>
            ) : (
              "Save Journal Entry"
            )}
          </button>
        </div>

        {errors.root?.message && (
          <div className="alert alert--error" role="alert">
            {errors.root.message}
          </div>
        )}
      </form>
    </div>
  );
}

/** Single leg row — account autocomplete, debit/credit, narration. */
function LegRow({
  index,
  register,
  setValue,
  onRemove,
  canRemove,
}: {
  index: number;
  register: any;
  setValue: any;
  onRemove: () => void;
  canRemove: boolean;
}) {
  const [search, setSearch] = useState("");
  const [showDropdown, setShowDropdown] = useState(false);

  const filtered = ACCOUNT_OPTIONS.filter(
    (a) =>
      a.name.toLowerCase().includes(search.toLowerCase()) ||
      a.code.includes(search),
  );

  return (
    <tr>
      <td>
        <div className="relative">
          <input
            type="text"
            className="form-input form-input--sm"
            placeholder="Search account..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            onFocus={() => setShowDropdown(true)}
            onBlur={() => setTimeout(() => setShowDropdown(false), 200)}
          />
          {showDropdown && search && (
            <div className="absolute z-10 w-full bg-white border border-gray-200 rounded shadow-lg max-h-40 overflow-y-auto">
              {filtered.length === 0 ? (
                <p className="px-2 py-1 text-sm text-gray-500">No matches</p>
              ) : (
                filtered.map((acc) => (
                  <button
                    key={acc.id}
                    type="button"
                    className="w-full text-left px-2 py-1 hover:bg-gray-100 text-sm"
                    onMouseDown={(e) => {
                      e.preventDefault();
                      setValue(`legs.${index}.accountId`, acc.id);
                      setValue(`legs.${index}.accountName`, acc.name);
                      setSearch(acc.name);
                      setShowDropdown(false);
                    }}
                  >
                    <span className="font-mono text-xs text-gray-500">{acc.code}</span> {acc.name}
                  </button>
                ))
              )}
            </div>
          )}
          {/* Hidden inputs so react-hook-form tracks them */}
          <input type="hidden" {...register(`legs.${index}.accountName`)} />
          <input type="hidden" {...register(`legs.${index}.accountId`)} />
        </div>
      </td>
      <td>
        <input
          type="text"
          className="form-input form-input--sm"
          placeholder="Cost center..."
          {...register(`legs.${index}.costCenterName`)}
        />
      </td>
      <td className="text-right">
        <input
          type="number"
          step="0.01"
          className="form-input form-input--sm text-right"
          {...register(`legs.${index}.debit`)}
        />
      </td>
      <td className="text-right">
        <input
          type="number"
          step="0.01"
          className="form-input form-input--sm text-right"
          {...register(`legs.${index}.credit`)}
        />
      </td>
      <td>
        <input
          type="text"
          className="form-input form-input--sm"
          placeholder="Line narration"
          {...register(`legs.${index}.narration`)}
        />
      </td>
      <td className="text-center">
        {canRemove && (
          <button
            type="button"
            className="btn btn--xs btn--danger"
            onClick={onRemove}
            title="Remove this leg"
          >
            ✕
          </button>
        )}
      </td>
    </tr>
  );
}
