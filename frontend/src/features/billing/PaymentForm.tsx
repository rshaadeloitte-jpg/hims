import { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { useRecordPayment, usePatientAdvances, useApplyAdvance } from "./hooks";
import { paymentSchema, type PaymentValues } from "./zodSchema";
import { quickPaymentSchema, type QuickPaymentValues } from "./schemas";

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
          <span className="payment-form__balance-amount">₹ {balanceAmount.toFixed(2)}</span>
        </div>
      </div>

      <form onSubmit={handleSubmit(handleSubmitPayment)} className="payment-form__form">
        {/* Payment method selector — Stripe-style segmented control */}
        <div className="payment-form__methods" role="group" aria-label="Payment method">
          {(["CASH", "CARD", "UPI", "CHEQUE", "ADVANCE"] as PaymentMethod[]).map((m) => (
            <button
              key={m}
              type="button"
              aria-pressed={method === m}
              className={`payment-form__method${method === m ? " payment-form__method--active" : ""}`}
              onClick={() => handleMethodChange(m)}
            >
              {m === "CASH" && "Cash"}
              {m === "CARD" && "Card"}
              {m === "UPI" && "UPI"}
              {m === "CHEQUE" && "Cheque"}
              {m === "ADVANCE" && `Advance${totalAdvance > 0 ? ` (₹${totalAdvance.toFixed(0)})` : ""}`}
            </button>
          ))}
        </div>

        {/* Amount field */}
        <div className="payment-form__amount-section">
          <label htmlFor="payment-amount" className="payment-form__label">
            Amount
          </label>
          <div className="payment-form__amount-input">
            <span className="payment-form__currency">₹</span>
            <input
              id="payment-amount"
              type="number"
              step="0.01"
              className="payment-form__amount"
              {...register("amount")}
              aria-invalid={errors.amount ? true : undefined}
            />
          </div>
          {errors.amount && <span className="field-error">{errors.amount.message}</span>}
          <div className="payment-form__quick-amounts">
            <button
              type="button"
              className="btn btn--sm btn--ghost"
              onClick={() => setValue("amount", balanceAmount)}
            >
              Full amount
            </button>
            <button
              type="button"
              className="btn btn--sm btn--ghost"
              onClick={() => setValue("amount", Math.ceil(balanceAmount / 2))}
            >
              50%
            </button>
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
                  <label htmlFor="card-number">Card Number</label>
                  <input
                    id="card-number"
                    type="text"
                    className="form-input"
                    placeholder="4242 4242 4242 4242"
                    inputMode="numeric"
                    autoComplete="cc-number"
                  />
                </div>
                <div className="payment-form__card-row">
                  <div className="payment-form__card-expiry">
                    <label htmlFor="card-expiry">Expiry</label>
                    <input
                      id="card-expiry"
                      type="text"
                      className="form-input"
                      placeholder="MM/YY"
                      inputMode="numeric"
                      autoComplete="cc-exp"
                    />
                  </div>
                  <div className="payment-form__card-cvv">
                    <label htmlFor="card-cvv">CVV</label>
                    <input
                      id="card-cvv"
                      type="text"
                      className="form-input"
                      placeholder="123"
                      inputMode="numeric"
                      autoComplete="cc-csc"
                    />
                  </div>
                </div>
              </div>
            </div>
          </div>
        )}

        {method === "UPI" && (
          <div className="payment-form__upi-fields">
            <label htmlFor="upi-id" className="payment-form__label">
              UPI ID
            </label>
            <input
              id="upi-id"
              type="text"
              className="form-input"
              placeholder="yourname@upi"
              autoComplete="off"
            />
            <p className="payment-form__hint">
              Enter UPI ID to generate QR or approve via UPI app
            </p>
          </div>
        )}

        {method === "CHEQUE" && (
          <div className="payment-form__cheque-fields">
            <div className="payment-form__field">
              <label htmlFor="cheque-bank" className="payment-form__label">
                Bank Name
              </label>
              <input
                id="cheque-bank"
                type="text"
                className="form-input"
                placeholder="Bank name"
              />
            </div>
            <div className="payment-form__field">
              <label htmlFor="cheque-no" className="payment-form__label">
                Cheque No
              </label>
              <input
                id="cheque-no"
                type="text"
                className="form-input"
                placeholder="Cheque number"
              />
            </div>
            <div className="payment-form__field">
              <label htmlFor="cheque-date" className="payment-form__label">
                Cheque Date
              </label>
              <input
                id="cheque-date"
                type="date"
                className="form-input"
              />
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
            <label htmlFor="payment-ref" className="payment-form__label">
              Reference No
            </label>
            <input
              id="payment-ref"
              type="text"
              className="form-input"
              placeholder="Transaction / reference number"
              {...register("referenceNo")}
            />
          </div>
          <div className="payment-form__field">
            <label htmlFor="payment-remarks" className="payment-form__label">
              Remarks
            </label>
            <input
              id="payment-remarks"
              type="text"
              className="form-input"
              placeholder="Optional notes"
              {...register("remarks")}
            />
          </div>
        </div>

        {/* Options */}
        <div className="payment-form__options">
          <label className="payment-form__option">
            <input type="checkbox" {...register("markAsSettled")} />
            Mark bill as settled
          </label>
          <label className="payment-form__option">
            <input type="checkbox" {...register("generateReceipt")} />
            Generate receipt
          </label>
        </div>

        {/* Actions */}
        <div className="payment-form__actions">
          <button
            type="button"
            className="btn btn--secondary"
            onClick={onCancel}
            disabled={isPending}
          >
            Cancel
          </button>
          <button
            type="submit"
            className="btn btn--primary btn--lg"
            disabled={isPending || amount <= 0}
          >
            {isPending ? (
              <>
                <span className="spinner spinner--sm" aria-hidden="true" />
                Processing...
              </>
            ) : (
              `Collect ₹ ${amount?.toFixed(2) ?? "0.00"}`
            )}
          </button>
        </div>

        {recordPayment.isError && (
          <div className="alert alert--error" role="alert">
            Payment failed: {(recordPayment.error as Error)?.message ?? "Unknown error"}
          </div>
        )}
        {applyAdvance.isError && (
          <div className="alert alert--error" role="alert">
            Advance application failed: {(applyAdvance.error as Error)?.message ?? "Unknown error"}
          </div>
        )}
      </form>
    </div>
  );
}
