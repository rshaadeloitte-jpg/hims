import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { useIssueCreditNote } from "./hooks";
import { creditNoteExtendedSchema, type CreditNoteExtendedValues } from "./schemas";

interface CreditNoteFormProps {
  billId: number;
  maxAmount: number;
  onIssued?: () => void;
  onCancel?: () => void;
}

/**
 * CreditNoteForm — credit/debit note issuance UI.
 * Supports multiple reason categories and refund methods.
 * Follows Stripe-style form with inline validation and clear visual hierarchy.
 */
export function CreditNoteForm({ billId, maxAmount, onIssued, onCancel }: CreditNoteFormProps) {
  const issueCredit = useIssueCreditNote();

  const {
    register,
    handleSubmit,
    watch,
    setValue,
    formState: { errors },
  } = useForm<CreditNoteExtendedValues>({
    resolver: zodResolver(creditNoteExtendedSchema),
    defaultValues: {
      amount: maxAmount,
      reason: "",
      category: "OTHER",
      refundMethod: undefined,
    },
  });

  const amount = watch("amount");
  const category = watch("category");

  const handleSubmitNote = async (values: CreditNoteExtendedValues) => {
    try {
      await issueCredit.mutateAsync({
        id: billId,
        payload: {
          amount: values.amount,
          reason: values.reason,
          category: values.category,
          refundMethod: values.refundMethod,
        },
      });
      onIssued?.();
    } catch (err) {
      console.error("Credit note issuance failed:", err);
    }
  };

  return (
    <div className="credit-note-form">
      <div className="credit-note-form__header">
        <h3 className="credit-note-form__title">Issue Credit Note</h3>
        <div className="credit-note-form__max">
          <span className="credit-note-form__max-label">Max refundable</span>
          <span className="credit-note-form__max-amount">₹ {maxAmount.toFixed(2)}</span>
        </div>
      </div>

      <form onSubmit={handleSubmit(handleSubmitNote)} className="credit-note-form__form">
        {/* Amount */}
        <div className="field">
          <label htmlFor="credit-amount">Amount</label>
          <div className="credit-note-form__amount">
            <span className="credit-note-form__currency">₹</span>
            <input
              id="credit-amount"
              type="number"
              step="0.01"
              className="form-input"
              {...register("amount")}
              aria-invalid={errors.amount ? true : undefined}
            />
          </div>
          {errors.amount && <span className="field-error">{errors.amount.message}</span>}
          <div className="credit-note-form__quick">
            <button
              type="button"
              className="btn btn--sm btn--ghost"
              onClick={() => setValue("amount", maxAmount)}
            >
              Full refund
            </button>
            <button
              type="button"
              className="btn btn--sm btn--ghost"
              onClick={() => setValue("amount", maxAmount / 2)}
            >
              50%
            </button>
          </div>
        </div>

        {/* Category */}
        <div className="field">
          <label htmlFor="credit-category">Reason Category</label>
          <select id="credit-category" className="form-input" {...register("category")}>
            <option value="REFUND">Refund</option>
            <option value="CANCELLATION">Cancellation</option>
            <option value="DISCOUNT_ADJUSTMENT">Discount Adjustment</option>
            <option value="OVERCHARGE">Overcharge Correction</option>
            <option value="OTHER">Other</option>
          </select>
        </div>

        {/* Reason */}
        <div className="field">
          <label htmlFor="credit-reason">Reason</label>
          <textarea
            id="credit-reason"
            className="form-input"
            rows={3}
            placeholder="Explain the reason for this credit note..."
            {...register("reason")}
            aria-invalid={errors.reason ? true : undefined}
          />
          {errors.reason && <span className="field-error">{errors.reason.message}</span>}
        </div>

        {/* Refund method (if applicable) */}
        {(category === "REFUND" || category === "OVERCHARGE") && (
          <div className="field">
            <label htmlFor="credit-refund-method">Refund Method</label>
            <select id="credit-refund-method" className="form-input" {...register("refundMethod")}>
              <option value="">Select method</option>
              <option value="CASH">Cash refund</option>
              <option value="BANK_TRANSFER">Bank transfer</option>
              <option value="ORIGINAL_PAYMENT">Original payment method</option>
            </select>
          </div>
        )}

        {/* Warning */}
        <div className="alert alert--warning" role="alert">
          <strong>Note:</strong> Credit notes are irreversible. The amount will be
          credited to the patient&apos;s account or refunded via the selected method.
        </div>

        {/* Actions */}
        <div className="credit-note-form__actions">
          <button
            type="button"
            className="btn btn--secondary"
            onClick={onCancel}
            disabled={issueCredit.isPending}
          >
            Cancel
          </button>
          <button
            type="submit"
            className="btn btn--primary"
            disabled={issueCredit.isPending || (amount ?? 0) <= 0}
          >
            {issueCredit.isPending ? (
              <>
                <span className="spinner spinner--sm" aria-hidden="true" />
                Issuing...
              </>
            ) : (
              `Issue Credit Note (₹ ${(amount ?? 0).toFixed(2)})`
            )}
          </button>
        </div>

        {issueCredit.isError && (
          <div className="alert alert--error" role="alert">
            Failed to issue credit note: {(issueCredit.error as Error)?.message ?? "Unknown error"}
          </div>
        )}
      </form>
    </div>
  );
}
