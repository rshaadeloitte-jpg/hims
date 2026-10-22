import { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { useRecordPayment, usePatientAdvances, useApplyAdvance } from "./hooks";
import { quickPaymentSchema, type QuickPaymentValues } from "./schemas";
import { Button, Checkbox } from "@chakra-ui/react";
import { FormField, TextInput } from "../../components/ui";

interface PaymentFormProps {
  billId: number;
  patientId: number;
  balanceAmount: number;
  onPaymentRecorded?: () => void;
  onCancel?: () => void;
}

type PaymentMethod = "CASH" | "CARD" | "UPI" | "CHEQUE" | "ADVANCE";

/**
 * PaymentForm — Stripe-inspired payment collection UI.
 * Supports multiple payment methods with method-specific fields.
 * Shows advance balance if available for the patient.
 */
export function PaymentForm({
  billId,
  patientId,
  balanceAmount,
  onPaymentRecorded,
  onCancel,
}: PaymentFormProps) {
  const [method, setMethod] = useState<PaymentMethod>("CASH");
  const [useAdvance, setUseAdvance] = useState(false);

  const { data: advances = [] } = usePatientAdvances(patientId);
  const recordPayment = useRecordPayment();
  const applyAdvance = useApplyAdvance();

  const {
    register,
    handleSubmit,
    watch,
    setValue,
    formState: { errors },
  } = useForm<QuickPaymentValues>({
    resolver: zodResolver(quickPaymentSchema),
    defaultValues: {
      paymentMethod: "CASH",
      amount: balanceAmount,
      referenceNo: "",
      remarks: "",
      markAsSettled: false,
      generateReceipt: true,
    },
  });

  const amount = watch("amount");
  const markAsSettled = watch("markAsSettled");
  const generateReceipt = watch("generateReceipt");
  const availableAdvances = advances.filter((a) => !a.isSettled);
  const totalAdvance = availableAdvances.reduce((s, a) => s + a.amount, 0);

  const handleMethodChange = (newMethod: PaymentMethod) => {
    setMethod(newMethod);
    setValue("paymentMethod", newMethod);
  };

  const handleSubmitPayment = async (values: QuickPaymentValues) => {
    try {
      if (useAdvance && availableAdvances.length > 0) {
        const advance = availableAdvances[0];
        await applyAdvance.mutateAsync({
          billId,
          advanceId: advance.advanceId,
          amount: Math.min(values.amount, advance.amount),
        });
      } else {
        await recordPayment.mutateAsync({
          id: billId,
          payload: {
            amount: values.amount,
            paymentMethod: method,
            referenceNo: values.referenceNo,
            remarks: values.remarks,
          },
        });
      }
      onPaymentRecorded?.();
    } catch (err) {
      console.error("Payment failed:", err);
    }
  };

  const isPending = recordPayment.isPending || applyAdvance.isPending;

  return (
    <div className="payment-form">
      <div className="payment-form__header">
        <h3 className="payment-form__title">Record Payment</h3>
        <div className="payment-form__balance">
          <span className="payment-form__balance-label">Balance Due</span>
          <span className="payment-form__balance-amount" aria-live="polite">₹ {balanceAmount.toFixed(2)}</span>
        </div>
      </div>

      <form onSubmit={handleSubmit(handleSubmitPayment)} className="payment-form__form">
        {/* Payment method selector — Stripe-style segmented control */}
        <div className="payment-form__methods" role="group" aria-label="Payment method">
          {(["CASH", "CARD", "UPI", "CHEQUE", "ADVANCE"] as PaymentMethod[]).map((m) => (
            <Button
              key={m}
              type="button"
              size="sm"
              variant={method === m ? "primary" : "secondary"}
              aria-pressed={method === m}
              className={`payment-form__method${method === m ? " payment-form__method--active" : ""}`}
              onClick={() => handleMethodChange(m)}
            >
              {m === "CASH" && "Cash"}
              {m === "CARD" && "Card"}
              {m === "UPI" && "UPI"}
              {m === "CHEQUE" && "Cheque"}
              {m === "ADVANCE" && `Advance${totalAdvance > 0 ? ` (₹${totalAdvance.toFixed(0)})` : ""}`}
            </Button>
          ))}
        </div>

        {/* Amount field */}
        <div className="payment-form__amount-section">
          <FormField label="Amount" error={errors.amount?.message}>
            <div className="payment-form__amount-input">
              <span className="payment-form__currency">₹</span>
              <TextInput
                id="payment-amount"
                type="number"
                step="0.01"
                className="payment-form__amount"
                {...register("amount")}
                aria-invalid={errors.amount ? true : undefined}
              />
            </div>
          </FormField>
          <div className="payment-form__quick-amounts">
            <Button
              type="button"
              variant="ghost"
              size="sm"
              onClick={() => setValue("amount", balanceAmount)}
            >
              Full amount
            </Button>
            <Button
              type="button"
              variant="ghost"
              size="sm"
              onClick={() => setValue("amount", Math.ceil(balanceAmount / 2))}
            >
              50%
            </Button>
          </div>
        </div>

        {/* Method-specific fields */}
        {method === "CARD" && (
          <div className="payment-form__card-fields">
            <div className="payment-form__card-element">
              {/* Stripe Card Element placeholder — in production, this would be
                  the actual Stripe CardElement from @stripe/react-stripe-js */}
              <div className="payment-form__card-mock">
                <div className="payment-form__card-number">
                  <FormField label="Card Number">
                    <TextInput
                      id="card-number"
                      type="text"
                      placeholder="4242 4242 4242 4242"
                      inputMode="numeric"
                      autoComplete="cc-number"
                    />
                  </FormField>
                </div>
                <div className="payment-form__card-row">
                  <div className="payment-form__card-expiry">
                    <FormField label="Expiry">
                      <TextInput
                        id="card-expiry"
                        type="text"
                        placeholder="MM/YY"
                        inputMode="numeric"
                        autoComplete="cc-exp"
                      />
                    </FormField>
                  </div>
                  <div className="payment-form__card-cvv">
                    <FormField label="CVV">
                      <TextInput
                        id="card-cvv"
                        type="text"
                        placeholder="123"
                        inputMode="numeric"
                        autoComplete="cc-csc"
                      />
                    </FormField>
                  </div>
                </div>
              </div>
            </div>
          </div>
        )}

        {method === "UPI" && (
          <div className="payment-form__upi-fields">
            <FormField label="UPI ID">
              <TextInput
                id="upi-id"
                type="text"
                placeholder="yourname@upi"
                autoComplete="off"
              />
            </FormField>
            <p className="payment-form__hint">
              Enter UPI ID to generate QR or approve via UPI app
            </p>
          </div>
        )}

        {method === "CHEQUE" && (
          <div className="payment-form__cheque-fields">
            <div className="payment-form__field">
              <FormField label="Bank Name">
                <TextInput
                  id="cheque-bank"
                  type="text"
                  placeholder="Bank name"
                />
              </FormField>
            </div>
            <div className="payment-form__field">
              <FormField label="Cheque No">
                <TextInput
                  id="cheque-no"
                  type="text"
                  placeholder="Cheque number"
                />
              </FormField>
            </div>
            <div className="payment-form__field">
              <FormField label="Cheque Date">
                <TextInput
                  id="cheque-date"
                  type="date"
                />
              </FormField>
            </div>
          </div>
        )}

        {method === "ADVANCE" && availableAdvances.length > 0 && (
          <div className="payment-form__advance-fields">
            <div className="payment-form__advance-list">
              {availableAdvances.map((adv) => (
                <label key={adv.advanceId} className="payment-form__advance-item">
                  <input
                    type="radio"
                    name="advance"
                    value={adv.advanceId}
                    onChange={() => setUseAdvance(true)}
                  />
                  <span className="payment-form__advance-amount">₹ {adv.amount.toFixed(2)}</span>
                  <span className="payment-form__advance-date">{adv.createdAt?.slice(0, 10)}</span>
                  <span className="payment-form__advance-method">{adv.paymentMethod}</span>
                </label>
              ))}
            </div>
          </div>
        )}

        {method === "ADVANCE" && availableAdvances.length === 0 && (
          <div className="payment-form__no-advances">
            <p className="muted">No available advances for this patient.</p>
          </div>
        )}

        {/* Reference & remarks */}
        <div className="payment-form__extra-fields">
          <div className="payment-form__field">
            <FormField label="Reference No">
              <TextInput
                id="payment-ref"
                type="text"
                placeholder="Transaction / reference number"
                {...register("referenceNo")}
              />
            </FormField>
          </div>
          <div className="payment-form__field">
            <FormField label="Remarks">
              <TextInput
                id="payment-remarks"
                type="text"
                placeholder="Optional notes"
                {...register("remarks")}
              />
            </FormField>
          </div>
        </div>

        {/* Options */}
        <div className="payment-form__options">
          <Checkbox.Root
            checked={!!markAsSettled}
            onCheckedChange={(e) => setValue("markAsSettled", !!e.checked, { shouldValidate: true })}
          >
            <Checkbox.HiddenInput {...register("markAsSettled")} />
            <Checkbox.Control />
            <Checkbox.Label>Mark bill as settled</Checkbox.Label>
          </Checkbox.Root>
          <Checkbox.Root
            checked={!!generateReceipt}
            onCheckedChange={(e) => setValue("generateReceipt", !!e.checked, { shouldValidate: true })}
          >
            <Checkbox.HiddenInput {...register("generateReceipt")} />
            <Checkbox.Control />
            <Checkbox.Label>Generate receipt</Checkbox.Label>
          </Checkbox.Root>
        </div>

        {/* Actions */}
        <div className="payment-form__actions">
          <Button
            type="button"
            variant="secondary"
            onClick={onCancel}
            disabled={isPending}
          >
            Cancel
          </Button>
          <Button
            type="submit"
            variant="primary"
            disabled={isPending || amount <= 0}
            loading={isPending}
            loadingText="Processing..."
          >
            {`Collect ₹ ${amount?.toFixed(2) ?? "0.00"}`}
          </Button>
        </div>

        {recordPayment.isError && (
          <div className="alert alert--error" role="alert" aria-live="assertive">
            Payment failed: {(recordPayment.error as Error)?.message ?? "Unknown error"}
          </div>
        )}
        {applyAdvance.isError && (
          <div className="alert alert--error" role="alert" aria-live="assertive">
            Advance application failed: {(applyAdvance.error as Error)?.message ?? "Unknown error"}
          </div>
        )}
      </form>
    </div>
  );
}
