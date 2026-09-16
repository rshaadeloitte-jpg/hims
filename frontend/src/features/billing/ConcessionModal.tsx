import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { useApplyConcession } from "./hooks";
import { concessionModalSchema, type ConcessionModalValues } from "./schemas";
import { type ConcessionPlan } from "./hooks";
import { Button, Checkbox, Text, Textarea } from "@chakra-ui/react";
import { FormField, TextInput } from "../../components/ui";

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
  const applyToAllLines = watch("applyToAllLines");

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
            <Text fontSize="sm" fontWeight="medium" className="concession-modal__label">Concession Plan</Text>
            <div className="concession-modal__plan-list">
              {concessionPlans.map((plan) => (
                <Button
                  key={plan.planId}
                  type="button"
                  variant="secondary"
                  className={`concession-modal__plan${selectedPlanId === plan.planId ? " concession-modal__plan--active" : ""}`}
                  onClick={() => handlePlanSelect(plan.planId)}
                >
                  <span className="concession-modal__plan-name">{plan.planName}</span>
                  <span className="concession-modal__plan-pct">{plan.maxDiscountPercent}%</span>
                  {plan.requiresApproval && (
                    <span className="concession-modal__plan-badge">Approval Required</span>
                  )}
                </Button>
              ))}
            </div>
          </div>
        )}

        {/* Manual percentage */}
        <div className="concession-modal__manual">
          <div className="concession-modal__field">
            <FormField label="Plan Discount %" error={errors.concessionPlanPct?.message}>
              <TextInput
                id="concession-plan-pct"
                type="number"
                step="0.01"
                min="0"
                max="100"
                {...register("concessionPlanPct")}
                aria-invalid={errors.concessionPlanPct ? true : undefined}
              />
            </FormField>
          </div>
          <div className="concession-modal__field">
            <FormField label="Bill Discount %" error={errors.concessionBillPct?.message}>
              <TextInput
                id="concession-bill-pct"
                type="number"
                step="0.01"
                min="0"
                max="100"
                {...register("concessionBillPct")}
                aria-invalid={errors.concessionBillPct ? true : undefined}
              />
            </FormField>
          </div>
        </div>

        {/* Approval */}
        <div className="concession-modal__field">
          <FormField label="Approved By (User ID)">
            <TextInput
              id="concession-approved-by"
              type="number"
              placeholder="Approver user ID"
              {...register("approvedBy")}
            />
          </FormField>
        </div>

        {/* Remarks */}
        <div className="concession-modal__field">
          <FormField label="Remarks">
            <Textarea
              id="concession-remarks"
              bg="white"
              borderColor="gray.300"
              rows={2}
              placeholder="Reason for concession..."
              {...register("remarks")}
            />
          </FormField>
        </div>

        {/* Apply to all lines */}
        <Checkbox.Root
          checked={!!applyToAllLines}
          onCheckedChange={(e) => setValue("applyToAllLines", !!e.checked, { shouldValidate: true })}
        >
          <Checkbox.HiddenInput {...register("applyToAllLines")} />
          <Checkbox.Control />
          <Checkbox.Label>Apply to all charge lines</Checkbox.Label>
        </Checkbox.Root>

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
          <Button
            type="button"
            variant="secondary"
            onClick={onCancel}
            disabled={applyConcession.isPending}
          >
            Cancel
          </Button>
          <Button
            type="submit"
            variant="primary"
            disabled={applyConcession.isPending || discountAmount <= 0}
            loading={applyConcession.isPending}
            loadingText="Applying..."
          >
            Apply Concession
          </Button>
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
