import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { useApplyConcession } from "./hooks";
import { concessionModalSchema, type ConcessionModalValues } from "./schemas";
import { type ConcessionPlan } from "./hooks";

interface ConcessionModalProps {
  billId: number;
  currentTotal: number;
  concessionPlans: ConcessionPlan[];
  onApplied?: () => void;
  onCancel?: () => void;
}

/**
 * ConcessionModal — apply concession/discount to a bill.
 * Supports plan-based concessions (pre-configured discount percentages)
 * and ad-hoc bill-level concessions with approval workflow.
 */
export function ConcessionModal({
  billId,
  currentTotal,
  concessionPlans,
  onApplied,
  onCancel,
}: ConcessionModalProps) {
  const applyConcession = useApplyConcession();

  const {
    register,
    handleSubmit,
    watch,
    setValue,
    formState: { errors },
  } = useForm<ConcessionModalValues>({
    resolver: zodResolver(concessionModalSchema),
    defaultValues: {
      concessionPlanId: undefined as unknown as number | undefined,
      concessionPlanPct: undefined as unknown as number | undefined,
      concessionBillPct: undefined as unknown as number | undefined,
      approvedBy: undefined as unknown as number | undefined,
      remarks: "",
      applyToAllLines: false,
    },
  });

  const planPct = watch("concessionPlanPct");
  const billPct = watch("concessionBillPct");
  const selectedPlanId = watch("concessionPlanId");

  const discountAmount = currentTotal * ((planPct ?? 0) / 100) + currentTotal * ((billPct ?? 0) / 100);
  const finalAmount = currentTotal - discountAmount;

  const handlePlanSelect = (planId: number) => {
    setValue("concessionPlanId", planId);
    const plan = concessionPlans.find((p) => p.planId === planId);
    if (plan) {
      setValue("concessionPlanPct", plan.maxDiscountPercent);
    }
  };

  const handleApply = async (values: ConcessionModalValues) => {
    try {
      await applyConcession.mutateAsync({
        id: billId,
        payload: {
          concessionPlanPct: values.concessionPlanPct,
          concessionBillPct: values.concessionBillPct,
          approvedBy: values.approvedBy,
          remarks: values.remarks,
        },
      });
      onApplied?.();
    } catch (err) {
      console.error("Concession application failed:", err);
    }
  };

  return (
    <div className="concession-modal">
      <div className="concession-modal__header">
        <h3 className="concession-modal__title">Apply Concession</h3>
        <div className="concession-modal__current">
          <span className="concession-modal__current-label">Bill Total</span>
          <span className="concession-modal--current-amount">Rs {(currentTotal).toFixed(2)}</span>
        </div>
      </div>

      <form onSubmit={handleSubmit(handleApply)} className="concession-modal__form">
        {/* Concession plans */}
        {concessionPlans.length > 0 && (
          <div className="concession-modal__plans">
            <label className="concession-modal__label">Concession Plan</label>
            <div className="concession-modal__plan-list">
              {concessionPlans.map((plan) => (
                <button
                  key={plan.planId}
                  type="button"
                  className={`concession-modal__plan${selectedPlanId === plan.planId ? " concession-modal__plan--active" : ""}`}
                  onClick={() => handlePlanSelect(plan.planId)}
                >
                  <span className="concession-modal__plan-name">{plan.planName}</span>
                  <span className="concession-modal__plan-pct">{plan.maxDiscountPercent}%</span>
                  {plan.requiresApproval && (
                    <span className="concession-modal__plan-badge">Approval Required</span>
                  )}
                </button>
              ))}
            </div>
          </div>
        )}

        {/* Manual percentage */}
        <div className="concession-modal__manual">
          <div className="concession-modal__field">
            <label htmlFor="concession-plan-pct">Plan Discount %</label>
            <input
              id="concession-plan-pct"
              type="number"
              step="0.01"
              min="0"
              max="100"
              className="form-input"
              {...register("concessionPlanPct")}
              aria-invalid={errors.concessionPlanPct ? true : undefined}
            />
            {errors.concessionPlanPct && (
              <span className="field-error">{errors.concessionPlanPct.message}</span>
            )}
          </div>
          <div className="concession-modal__field">
            <label htmlFor="concession-bill-pct">Bill Discount %</label>
            <input
              id="concession-bill-pct"
              type="number"
              step="0.01"
              min="0"
              max="100"
              className="form-input"
              {...register("concessionBillPct")}
              aria-invalid={errors.concessionBillPct ? true : undefined}
            />
            {errors.concessionBillPct && (
              <span className="field-error">{errors.concessionBillPct.message}</span>
            )}
          </div>
        </div>

        {/* Approval */}
        <div className="concession-modal__field">
          <label htmlFor="concession-approved-by">Approved By (User ID)</label>
          <input
            id="concession-approved-by"
            type="number"
            className="form-input"
            placeholder="Approver user ID"
            {...register("approvedBy")}
          />
        </div>

        {/* Remarks */}
        <div className="concession-modal__field">
          <label htmlFor="concession-remarks">Remarks</label>
          <textarea
            id="concession-remarks"
            className="form-input"
            rows={2}
            placeholder="Reason for concession..."
            {...register("remarks")}
          />
        </div>

        {/* Apply to all lines */}
        <label className="concession-modal__option">
          <input type="checkbox" {...register("applyToAllLines")} />
          Apply to all charge lines
        </label>

        {/* Summary */}
        <div className="concession-modal__summary">
          <div className="concession-modal__summary-row">
            <span>Original Amount</span>
            <span>Rs {currentTotal.toFixed(2)}</span>
          </div>
          <div className="concession-modal__summary-row concession-modal__summary-row--discount">
            <span>Discount</span>
            <span>- Rs {discountAmount.toFixed(2)}</span>
          </div>
          <div className="concession-modal__summary-row concession-modal__summary-row--final">
            <span>Final Amount</span>
            <span>Rs {finalAmount.toFixed(2)}</span>
          </div>
        </div>

        {/* Actions */}
        <div className="concession-modal__actions">
          <button
            type="button"
            className="btn btn--secondary"
            onClick={onCancel}
            disabled={applyConcession.isPending}
          >
            Cancel
          </button>
          <button
            type="submit"
            className="btn btn--primary"
            disabled={applyConcession.isPending || discountAmount <= 0}
          >
            {applyConcession.isPending ? (
              <>
                <span className="spinner spinner--sm" aria-hidden="true" />
                Applying...
              </>
            ) : (
              "Apply Concession"
            )}
          </button>
        </div>

        {applyConcession.isError && (
          <div className="alert alert--error" role="alert">
            Failed to apply concession: {(applyConcession.error as Error)?.message ?? "Unknown error"}
          </div>
        )}
      </form>
    </div>
  );
}
