import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import {
  transferRequestSchema,
  type TransferRequestValues,
} from "./zodSchema";
import { useInitiateTransfer, useTransferSchemes } from "./queries";

interface TransferRequestProps {
  onTransferInitiated?: (transferId: string) => void;
}

/**
 * TransferRequest — Stripe-inspired payment collection form for DBT transfers.
 * Collects beneficiary details, account info, amount, scheme, and purpose.
 */
export function TransferRequest({ onTransferInitiated }: TransferRequestProps) {
  const initiateTransfer = useInitiateTransfer();
  const { data: schemes = [] } = useTransferSchemes();

  const {
    register,
    handleSubmit,
    watch,
    setValue,
    reset,
    formState: { errors },
  } = useForm<TransferRequestValues>({
    resolver: zodResolver(transferRequestSchema),
    defaultValues: {
      beneficiaryName: "",
      beneficiaryAccountNo: "",
      beneficiaryIfsc: "",
      amount: undefined as unknown as number,
      scheme: "",
      purpose: "",
      remarks: "",
    },
  });

  const amount = watch("amount");
  const ifsc = watch("beneficiaryIfsc");
  const accountNo = watch("beneficiaryAccountNo");

  // Mask account number for display: show only last 4 digits
  const maskedAccount =
    accountNo.length > 4
      ? `XXXX${accountNo.slice(-4)}`
      : accountNo;

  const formatIfsc = (value: string) => {
    const cleaned = value.toUpperCase().replace(/[^A-Z0-9]/g, "");
    return cleaned.slice(0, 11);
  };

  const handleIfscChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const formatted = formatIfsc(e.target.value);
    setValue("beneficiaryIfsc", formatted, { shouldValidate: true });
  };

  const formatAccountNumber = (value: string) => {
    return value.replace(/\D/g, "").slice(0, 18);
  };

  const handleAccountChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const formatted = formatAccountNumber(e.target.value);
    setValue("beneficiaryAccountNo", formatted, { shouldValidate: true });
  };

  const handleSubmitRequest = async (values: TransferRequestValues) => {
    try {
      const result = await initiateTransfer.mutateAsync({
        beneficiaryName: values.beneficiaryName.trim(),
        beneficiaryAccountNo: values.beneficiaryAccountNo,
        beneficiaryIfsc: values.beneficiaryIfsc,
        amount: values.amount,
        scheme: values.scheme,
        purpose: values.purpose,
        remarks: values.remarks || undefined,
      });
      onTransferInitiated?.(result.transferId);
      reset();
    } catch (err) {
      console.error("Transfer initiation failed:", err);
    }
  };

  const isPending = initiateTransfer.isPending;

  return (
    <div className="transfer-request">
      <div className="transfer-request__header">
        <h3 className="transfer-request__title">New Payment Transfer</h3>
        <p className="transfer-request__subtitle">
          Initiate a Direct Benefit Transfer (DBT) to a beneficiary
        </p>
      </div>

      <form onSubmit={handleSubmit(handleSubmitRequest)} className="transfer-request__form">
        {/* Beneficiary Details Section */}
        <fieldset className="transfer-request__section">
          <legend className="transfer-request__section-title">Beneficiary Details</legend>

          <div className="transfer-request__field">
            <label htmlFor="beneficiary-name" className="transfer-request__label">
              Beneficiary Name
            </label>
            <input
              id="beneficiary-name"
              type="text"
              className="form-input"
              placeholder="Enter beneficiary full name"
              autoComplete="name"
              {...register("beneficiaryName")}
              aria-invalid={errors.beneficiaryName ? true : undefined}
            />
            {errors.beneficiaryName && (
              <span className="field-error">{errors.beneficiaryName.message}</span>
            )}
          </div>

          <div className="transfer-request__row">
            <div className="transfer-request__field transfer-request__field--wide">
              <label htmlFor="account-no" className="transfer-request__label">
                Account Number
              </label>
              <input
                id="account-no"
                type="text"
                className="form-input"
                placeholder="Enter bank account number"
                inputMode="numeric"
                autoComplete="off"
                value={accountNo}
                onChange={handleAccountChange}
                aria-invalid={errors.beneficiaryAccountNo ? true : undefined}
              />
              {accountNo.length > 0 && (
                <span className="transfer-request__masked">
                  {maskedAccount}
                </span>
              )}
              {errors.beneficiaryAccountNo && (
                <span className="field-error">{errors.beneficiaryAccountNo.message}</span>
              )}
            </div>

            <div className="transfer-request__field transfer-request__field--narrow">
              <label htmlFor="ifsc" className="transfer-request__label">
                IFSC Code
              </label>
              <input
                id="ifsc"
                type="text"
                className="form-input transfer-request__ifsc"
                placeholder="SBIN0001234"
                autoComplete="off"
                value={ifsc}
                onChange={handleIfscChange}
                aria-invalid={errors.beneficiaryIfsc ? true : undefined}
              />
              {errors.beneficiaryIfsc && (
                <span className="field-error">{errors.beneficiaryIfsc.message}</span>
              )}
            </div>
          </div>
        </fieldset>

        {/* Transfer Details Section */}
        <fieldset className="transfer-request__section">
          <legend className="transfer-request__section-title">Transfer Details</legend>

          <div className="transfer-request__amount-section">
            <label htmlFor="transfer-amount" className="transfer-request__label">
              Transfer Amount
            </label>
            <div className="transfer-request__amount-input">
              <span className="transfer-request__currency">₹</span>
              <input
                id="transfer-amount"
                type="number"
                step="0.01"
                min="1"
                className="transfer-request__amount"
                placeholder="0.00"
                {...register("amount", { valueAsNumber: true })}
                aria-invalid={errors.amount ? true : undefined}
              />
            </div>
            {errors.amount && (
              <span className="field-error">{errors.amount.message}</span>
            )}
            <div className="transfer-request__quick-amounts">
              {[500, 1000, 2000, 5000, 10000].map((amt) => (
                <button
                  key={amt}
                  type="button"
                  className="btn btn--sm btn--ghost"
                  onClick={() => setValue("amount", amt)}
                >
                  ₹{amt.toLocaleString("en-IN")}
                </button>
              ))}
            </div>
          </div>

          <div className="transfer-request__field">
            <label htmlFor="scheme" className="transfer-request__label">
              Scheme
            </label>
            <select
              id="scheme"
              className="form-input"
              {...register("scheme")}
              aria-invalid={errors.scheme ? true : undefined}
            >
              <option value="">Select scheme</option>
              {schemes.map((scheme) => (
                <option key={scheme} value={scheme}>
                  {scheme}
                </option>
              ))}
              <option value="Other">Other</option>
            </select>
            {errors.scheme && (
              <span className="field-error">{errors.scheme.message}</span>
            )}
          </div>

          <div className="transfer-request__field">
            <label htmlFor="purpose" className="transfer-request__label">
              Purpose
            </label>
            <input
              id="purpose"
              type="text"
              className="transfer-request__textarea"
              placeholder="Brief description of the transfer purpose"
              {...register("purpose")}
              aria-invalid={errors.purpose ? true : undefined}
            />
            {errors.purpose && (
              <span className="field-error">{errors.purpose.message}</span>
            )}
          </div>

          <div className="transfer-request__field">
            <label htmlFor="remarks" className="transfer-request__label">
              Remarks <span className="optional">(optional)</span>
            </label>
            <input
              id="remarks"
              type="text"
              className="form-input"
              placeholder="Additional notes (optional)"
              {...register("remarks")}
            />
          </div>
        </fieldset>

        {/* Summary Card */}
        {amount > 0 && (
          <div className="transfer-request__summary">
            <div className="transfer-request__summary-row">
              <span className="transfer-request__summary-label">Amount</span>
              <span className="transfer-request__summary-value">
                ₹ {amount.toLocaleString("en-IN", { minimumFractionDigits: 2 })}
              </span>
            </div>
            <div className="transfer-request__summary-row">
              <span className="transfer-request__summary-label">To</span>
              <span className="transfer-request__summary-value">
                {watch("beneficiaryName") || "—"}
              </span>
            </div>
            {accountNo.length > 0 && (
              <div className="transfer-request__summary-row">
                <span className="transfer-request__summary-label">Account</span>
                <span className="transfer-request__summary-value">
                  {maskedAccount}
                </span>
              </div>
            )}
            {ifsc.length > 0 && (
              <div className="transfer-request__summary-row">
                <span className="transfer-request__summary-label">IFSC</span>
                <span className="transfer-request__summary-value">
                  {ifsc}
                </span>
              </div>
            )}
          </div>
        )}

        {/* Actions */}
        <div className="transfer-request__actions">
          <button
            type="button"
            className="btn btn--secondary"
            onClick={() => reset()}
            disabled={isPending}
          >
            Clear
          </button>
          <button
            type="submit"
            className="btn btn--primary btn--lg"
            disabled={isPending || !amount || amount <= 0}
          >
            {isPending ? (
              <>
                <span className="spinner spinner--sm" aria-hidden="true" />
                Initiating Transfer...
              </>
            ) : (
              `Initiate Transfer${amount > 0 ? ` — ₹${amount.toLocaleString("en-IN", { minimumFractionDigits: 2 })}` : ""}`
            )}
          </button>
        </div>

        {initiateTransfer.isError && (
          <div className="alert alert--error" role="alert">
            Transfer failed: {(initiateTransfer.error as Error)?.message ?? "Unknown error"}
          </div>
        )}
      </form>
    </div>
  );
}
