import { useState, useCallback } from "react";
import { useVerifyAbha } from "./queries";

/**
 * AbhaInput — ABHA/Aadhaar smart input with verification (Stripe-style inline validation).
 * Supports both ABHA address (@abdm) and Aadhaar number (12-digit) entry.
 */
export function AbhaInput({
  value,
  onChange,
  onVerified,
  error,
  disabled = false,
}: {
  value: string;
  onChange: (value: string) => void;
  onVerified?: (info: { name?: string; gender?: string; dateOfBirth?: string; photo?: string }) => void;
  error?: string;
  disabled?: boolean;
}) {
  const [touched, setTouched] = useState(false);
  const [localError, setLocalError] = useState<string | undefined>();
  const verifyAbha = useVerifyAbha();

  const detectType = (val: string): "abha" | "aadhaar" | "unknown" => {
    if (val.includes("@") || /^[a-zA-Z0-9]{4,32}$/.test(val)) return "abha";
    if (/^\d{4}\s?\d{4}\s?\d{4}$/.test(val) || /^\d{1,12}$/.test(val)) return "aadhaar";
    return "unknown";
  };

  const inputType = detectType(value);

  const handleChange = useCallback(
    (e: React.ChangeEvent<HTMLInputElement>) => {
      const raw = e.target.value;
      // Format Aadhaar with spaces
      let formatted = raw;
      if (detectType(raw) === "aadhaar" || /^\d+$/.test(raw.replace(/\s/g, ""))) {
        const digits = raw.replace(/\s/g, "").slice(0, 12);
        formatted = digits.replace(/(\d{4})(?=\d)/g, "$1 ").trim();
      }
      setLocalError(undefined);
      onChange(formatted);
    },
    [onChange],
  );

  const handleBlur = useCallback(() => {
    setTouched(true);
    if (value && inputType === "unknown") {
      setLocalError("Enter a valid ABHA address or 12-digit Aadhaar");
    }
  }, [value, inputType]);

  const handleVerify = useCallback(async () => {
    if (inputType !== "abha") return;
    try {
      const result = await verifyAbha.mutateAsync(value);
      if (result.verified) {
        onVerified?.(result);
      }
    } catch {
      setLocalError("ABHA verification failed");
    }
  }, [value, inputType, verifyAbha, onVerified]);

  const showError = (touched && localError) || error;

  return (
    <div className="abha-input">
      <label className="field">
        <span className="field__label">
          ABHA / Aadhaar
          <span className="field__hint"> ABHA address or 12-digit Aadhaar</span>
        </span>
        <div className="abha-input__row">
          <input
            type="text"
            className={`form-input ${showError ? "form-input--error" : ""} ${verifyAbha.isSuccess ? "form-input--success" : ""}`}
            value={value}
            onChange={handleChange}
            onBlur={handleBlur}
            disabled={disabled || verifyAbha.isPending}
            placeholder="e.g. abcd1234@abdm or 1234 5678 9012"
            aria-invalid={!!showError}
            aria-describedby={showError ? "abha-error" : undefined}
          />
          {inputType === "abha" && value.length >= 4 && (
            <button
              type="button"
              className="btn btn-secondary btn-sm"
              onClick={handleVerify}
              disabled={verifyAbha.isPending || disabled}
              aria-label="Verify ABHA"
            >
              {verifyAbha.isPending ? (
                <span className="spinner spinner--sm" aria-hidden="true" />
              ) : verifyAbha.isSuccess ? (
                <span aria-hidden="true">✓</span>
              ) : (
                "Verify"
              )}
            </button>
          )}
        </div>
        {showError && (
          <span id="abha-error" className="field-error" role="alert">
            {showError}
          </span>
        )}
        {verifyAbha.isSuccess && (
          <span className="field-success">ABHA verified successfully</span>
        )}
        {!showError && !value && (
          <span className="field__help">
            {inputType === "abha"
              ? "Enter ABHA address to auto-fill patient details"
              : inputType === "aadhaar"
                ? "Enter Aadhaar for identity verification"
                : "Start typing to detect type"}
          </span>
        )}
      </label>
    </div>
  );
}
