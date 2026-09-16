import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { useIssueCreditNote } from "./hooks";
import { creditNoteExtendedSchema, type CreditNoteExtendedValues } from "./schemas";
import { Button, Textarea } from "@chakra-ui/react";
import { FormField, HimsSelect, TextInput } from "../../components/ui";

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
          <FormField label="Amount" error={errors.amount?.message}>
            <div className="credit-note-form__amount">
              <span className="credit-note-form__currency">₹</span>
              <TextInput
                id="credit-amount"
                type="number"
                step="0.01"
                {...register("amount")}
                aria-invalid={errors.amount ? true : undefined}
              />
            </div>
          </FormField>
          <div className="credit-note-form__quick">
            <Button
              type="button"
              variant="ghost"
              size="sm"
              onClick={() => setValue("amount", maxAmount)}
            >
              Full refund
            </Button>
            <Button
              type="button"
              variant="ghost"
              size="sm"
              onClick={() => setValue("amount", maxAmount / 2)}
            >
              50%
            </Button>
          </div>
        </div>

        {/* Category */}
        <div className="field">
          <FormField label="Reason Category">
            <HimsSelect id="credit-category" {...register("category")}>
              <option value="REFUND">Refund</option>
              <option value="CANCELLATION">Cancellation</option>
              <option value="DISCOUNT_ADJUSTMENT">Discount Adjustment</option>
              <option value="OVERCHARGE">Overcharge Correction</option>
              <option value="OTHER">Other</option>
            </HimsSelect>
          </FormField>
        </div>

        {/* Reason */}
        <div className="field">
          <FormField label="Reason" error={errors.reason?.message}>
            <Textarea
              id="credit-reason"
              bg="white"
              borderColor="gray.300"
              rows={3}
              placeholder="Explain the reason for this credit note..."
              {...register("reason")}
              aria-invalid={errors.reason ? true : undefined}
            />
          </FormField>
        </div>

        {/* Refund method (if applicable) */}
        {(category === "REFUND" || category === "OVERCHARGE") && (
          <div className="field">
            <FormField label="Refund Method">
              <HimsSelect id="credit-refund-method" {...register("refundMethod")}>
                <option value="">Select method</option>
                <option value="CASH">Cash refund</option>
                <option value="BANK_TRANSFER">Bank transfer</option>
                <option value="ORIGINAL_PAYMENT">Original payment method</option>
              </HimsSelect>
            </FormField>
          </div>
        )}

        {/* Warning */}
        <div className="alert alert--warning" role="alert">
          <strong>Note:</strong> Credit notes are irreversible. The amount will be
          credited to the patient&apos;s account or refunded via the selected method.
        </div>

        {/* Actions */}
        <div className="credit-note-form__actions">
          <Button
            type="button"
            variant="secondary"
            onClick={onCancel}
            disabled={issueCredit.isPending}
          >
            Cancel
          </Button>
          <Button
            type="submit"
            variant="primary"
            disabled={issueCredit.isPending || (amount ?? 0) <= 0}
            loading={issueCredit.isPending}
            loadingText="Issuing..."
          >
            {`Issue Credit Note (₹ ${(amount ?? 0).toFixed(2)})`}
          </Button>
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
