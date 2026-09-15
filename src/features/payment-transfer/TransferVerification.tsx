import { useState, useEffect, useRef } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import {
  otpVerificationSchema,
  type OtpVerificationValues,
} from "./zodSchema";
import { useVerifyOtp, useResendOtp } from "./queries";

interface TransferVerificationProps {
  transferId: string;
  onSuccess?: () => void;
  onCancel?: () => void;
  onBack?: () => void;
}

const OTP_LENGTH = 6;
const RESEND_COOLDOWN_SECONDS = 30;

/**
 * TransferVerification — OTP verification step for DBT payment transfers.
 * Auto-submits when all 6 digits are entered. Supports resend with cooldown.
 */
export function TransferVerification({
  transferId,
  onSuccess,
  onCancel,
  onBack,
}: TransferVerificationProps) {
  const [serverError, setServerError] = useState<string | null>(null);
  const [cooldown, setCooldown] = useState(0);
  const verifyOtp = useVerifyOtp();
  const resendOtp = useResendOtp();

  const {
    register,
    handleSubmit,
    setValue,
    watch,
    setFocus,
    formState: { errors },
  } = useForm<OtpVerificationValues>({
    resolver: zodResolver(otpVerificationSchema),
    defaultValues: {
      transferId,
      otp: "",
    },
  });

  const otpValue = watch("otp");

  // Cooldown timer for resend button
  useEffect(() => {
    if (cooldown <= 0) return;
    const timer = setInterval(() => {
      setCooldown((prev) => prev - 1);
    }, 1000);
    return () => clearInterval(timer);
  }, [cooldown]);

  // Auto-focus OTP input on mount
  useEffect(() => {
    setFocus("otp");
  }, [setFocus]);

  // Auto-submit when OTP is complete
  useEffect(() => {
    if (otpValue.length === OTP_LENGTH && /^\d{6}$/.test(otpValue)) {
      void handleSubmit(handleVerify)();
    }
  }, [otpValue]);

  const handleVerify = async (values: OtpVerificationValues) => {
    setServerError(null);
    try {
      await verifyOtp.mutateAsync({
        transferId: values.transferId,
        otp: values.otp,
      });
      onSuccess?.();
    } catch (err) {
      setServerError((err as Error)?.message ?? "Verification failed. Please try again.");
      setValue("otp", "");
      setFocus("otp");
    }
  };

  const handleResend = async () => {
    if (cooldown > 0 || resendOtp.isPending) return;
    try {
      await resendOtp.mutateAsync({ transferId });
      setCooldown(RESEND_COOLDOWN_SECONDS);
    } catch (err) {
      setServerError((err as Error)?.message ?? "Failed to resend OTP.");
    }
  };

  const isPending = verifyOtp.isPending || resendOtp.isPending;

  return (
    <div className="transfer-verification">
      <div className="transfer-verification__header">
        <h3 className="transfer-verification__title">Verify OTP</h3>
        <p className="transfer-verification__subtitle">
          Enter the 6-digit OTP sent to your registered mobile number
        </p>
      </div>

      <form onSubmit={handleSubmit(handleVerify)} className="transfer-verification__form">
        <input type="hidden" {...register("transferId")} />

        {/* OTP Input — Stripe-style single input with visual feedback */}
        <div className="transfer-verification__otp-section">
          <label htmlFor="otp-input" className="transfer-verification__label">
            One-Time Password
          </label>
          <input
            id="otp-input"
            type="text"
            className="transfer-verification__otp-input"
            inputMode="numeric"
            autoComplete="one-time-code"
            maxLength={OTP_LENGTH}
            placeholder="• • • • • •"
            {...register("otp")}
            aria-invalid={errors.otp || serverError ? true : undefined}
            aria-describedby="otp-hint"
          />
          <p id="otp-hint" className="transfer-verification__hint">
            Enter the {OTP_LENGTH}-digit code sent to your phone
          </p>
          {errors.otp && (
            <span className="field-error">{errors.otp.message}</span>
          )}
          {serverError && (
            <div className="alert alert--error" role="alert">
              {serverError}
            </div>
          )}
        </div>

        {/* Timer and Resend */}
        <div className="transfer-verification__resend-section">
          {cooldown > 0 ? (
            <span className="transfer-verification__cooldown">
              Resend OTP in {cooldown}s
            </span>
          ) : (
            <button
              type="button"
              className="btn btn--link"
              onClick={handleResend}
              disabled={isPending}
            >
              Resend OTP
            </button>
          )}
        </div>

        {/* Transfer ID badge */}
        <div className="transfer-verification__transfer-id">
          <span className="transfer-verification__id-label">Transfer ID</span>
          <span className="transfer-verification__id-value">{transferId}</span>
        </div>

        {/* Actions */}
        <div className="transfer-verification__actions">
          <button
            type="button"
            className="btn btn--secondary"
            onClick={onBack}
            disabled={isPending}
          >
            Back
          </button>
          <button
            type="button"
            className="btn btn--ghost"
            onClick={onCancel}
            disabled={isPending}
          >
            Cancel
          </button>
          <button
            type="submit"
            className="btn btn--primary btn--lg"
            disabled={isPending || otpValue.length !== OTP_LENGTH}
          >
            {isPending ? (
              <>
                <span className="spinner spinner--sm" aria-hidden="true" />
                Verifying...
              </>
            ) : (
              "Verify & Submit"
            )}
          </button>
        </div>

        {verifyOtp.isError && !serverError && (
          <div className="alert alert--error" role="alert">
            Verification failed: {(verifyOtp.error as Error)?.message ?? "Unknown error"}
          </div>
        )}
      </form>
    </div>
  );
}
