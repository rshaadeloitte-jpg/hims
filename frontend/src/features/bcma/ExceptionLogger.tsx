import { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import {
  exceptionCreateSchema,
  exceptionOverrideSchema,
  EXCEPTION_TYPES,
  type ExceptionCreateValues,
  type ExceptionOverrideValues,
  type ExceptionResponse,
} from "./zodSchema";
import {
  useCreateException,
  useOverrideException,
  useExceptions,
} from "./queries";

/**
 * ExceptionLogger — M30 BCMA exception logging + override UI.
 *
 * Stripe-style inline verification patterns:
 * - Real-time Zod validation with inline per-field error messages
 * - Progressive disclosure (fields appear based on exception type)
 * - Clear visual hierarchy for severity
 * - Override flow with required justification
 * - Accessible form controls with proper labeling
 */

const SEVERITY_STYLES = {
  LOW: "bg-yellow-100 text-yellow-800 border-yellow-200",
  MEDIUM: "bg-orange-100 text-orange-800 border-orange-200",
  HIGH: "bg-red-100 text-red-800 border-red-200",
  CRITICAL: "bg-red-200 text-red-900 border-red-300 font-bold",
};

export function ExceptionLogger({ patientId }: { patientId?: number }) {
  const [showForm, setShowForm] = useState(false);
  const [overrideTarget, setOverrideTarget] = useState<number | null>(null);

  const { data: exceptions = [], isLoading } = useExceptions(patientId);
  const createException = useCreateException();
  const overrideException = useOverrideException();

  // Exception creation form
  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isValid },
    watch,
  } = useForm<ExceptionCreateValues>({
    resolver: zodResolver(exceptionCreateSchema),
    mode: "onChange",
    defaultValues: {
      patientId: patientId ?? 0,
      drugNdc: "",
      exceptionType: undefined,
      reason: "",
      overrideNurse: 0,
      wardId: 0,
      scannedBy: 0,
    },
  });

  const selectedType = watch("exceptionType");

  const handleCreateException = handleSubmit(async (values) => {
    try {
      await createException.mutateAsync(values);
      reset();
      setShowForm(false);
    } catch {
      // Error surfaced via mutation state
    }
  });

  const handleOverride = async (exceptionId: number, values: ExceptionOverrideValues) => {
    try {
      await overrideException.mutateAsync({ exceptionId, ...values });
      setOverrideTarget(null);
    } catch {
      // Error surfaced via mutation state
    }
  };

  return (
    <div className="bcma-exception-logger" role="region" aria-label="BCMA Exception Logger">
      <header className="page-header flex items-center justify-between">
        <div>
          <h2>Exception Log</h2>
          <p className="text-sm text-gray-500">
            Document and override BCMA verification exceptions
          </p>
        </div>
        <button
          onClick={() => setShowForm(!showForm)}
          className="bg-red-600 text-white px-4 py-2 rounded hover:bg-red-700 text-sm"
          aria-expanded={showForm}
        >
          {showForm ? "Cancel" : "+ Log Exception"}
        </button>
      </header>

      {/* Create exception form */}
      {showForm && (
        <form
          onSubmit={handleCreateException}
          className="card p-4 space-y-4"
          aria-label="Create BCMA exception"
        >
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
            <label className="text-sm">
              <span className="block font-medium mb-1">Patient ID *</span>
              <input
                type="number"
                className="w-full border rounded px-3 py-2"
                {...register("patientId", { valueAsNumber: true })}
                aria-invalid={errors.patientId ? "true" : undefined}
              />
              {errors.patientId && (
                <span className="text-xs text-red-600 mt-1 block">{errors.patientId.message}</span>
              )}
            </label>

            <label className="text-sm">
              <span className="block font-medium mb-1">Drug NDC *</span>
              <input
                type="text"
                className="w-full border rounded px-3 py-2 font-mono"
                {...register("drugNdc")}
                aria-invalid={errors.drugNdc ? "true" : undefined}
              />
              {errors.drugNdc && (
                <span className="text-xs text-red-600 mt-1 block">{errors.drugNdc.message}</span>
              )}
            </label>

            <label className="text-sm">
              <span className="block font-medium mb-1">Exception Type *</span>
              <select
                className="w-full border rounded px-3 py-2"
                {...register("exceptionType")}
                aria-invalid={errors.exceptionType ? "true" : undefined}
              >
                <option value="">Select type…</option>
                {EXCEPTION_TYPES.map((t) => (
                  <option key={t} value={t}>
                    {t.replace(/_/g, " ")}
                  </option>
                ))}
              </select>
              {errors.exceptionType && (
                <span className="text-xs text-red-600 mt-1 block">{errors.exceptionType.message}</span>
              )}
            </label>

            <label className="text-sm sm:col-span-2 lg:col-span-3">
              <span className="block font-medium mb-1">Reason *</span>
              <textarea
                className="w-full border rounded px-3 py-2"
                rows={3}
                placeholder="Describe the exception in detail..."
                {...register("reason")}
                aria-invalid={errors.reason ? "true" : undefined}
              />
              {errors.reason && (
                <span className="text-xs text-red-600 mt-1 block">{errors.reason.message}</span>
              )}
            </label>

            <label className="text-sm">
              <span className="block font-medium mb-1">Override Nurse ID *</span>
              <input
                type="number"
                className="w-full border rounded px-3 py-2"
                {...register("overrideNurse", { valueAsNumber: true })}
                aria-invalid={errors.overrideNurse ? "true" : undefined}
              />
              {errors.overrideNurse && (
                <span className="text-xs text-red-600 mt-1 block">{errors.overrideNurse.message}</span>
              )}
            </label>

            <label className="text-sm">
              <span className="block font-medium mb-1">Ward ID *</span>
              <input
                type="number"
                className="w-full border rounded px-3 py-2"
                {...register("wardId", { valueAsNumber: true })}
                aria-invalid={errors.wardId ? "true" : undefined}
              />
              {errors.wardId && (
                <span className="text-xs text-red-600 mt-1 block">{errors.wardId.message}</span>
              )}
            </label>

            <label className="text-sm">
              <span className="block font-medium mb-1">Scanned By *</span>
              <input
                type="number"
                className="w-full border rounded px-3 py-2"
                {...register("scannedBy", { valueAsNumber: true })}
                aria-invalid={errors.scannedBy ? "true" : undefined}
              />
              {errors.scannedBy && (
                <span className="text-xs text-red-600 mt-1 block">{errors.scannedBy.message}</span>
              )}
            </label>
          </div>

          {/* Contextual help based on exception type */}
          {selectedType && (
            <div className="bg-blue-50 border border-blue-200 rounded px-3 py-2 text-sm text-blue-800">
              <strong>Guidance:</strong> {getExceptionGuidance(selectedType)}
            </div>
          )}

          {createException.isError && (
            <p className="text-sm text-red-600 bg-red-50 border border-red-200 rounded px-3 py-2" role="alert">
              {(createException.error as Error).message}
            </p>
          )}

          <div className="flex gap-3">
            <button
              type="submit"
              disabled={!isValid || createException.isPending}
              className="bg-red-600 text-white px-4 py-2 rounded hover:bg-red-700 disabled:opacity-50 disabled:cursor-not-allowed"
            >
              {createException.isPending ? "Logging..." : "Log Exception"}
            </button>
            <button
              type="button"
              onClick={() => { reset(); setShowForm(false); }}
              className="px-4 py-2 border rounded hover:bg-gray-50"
            >
              Cancel
            </button>
          </div>
        </form>
      )}

      {/* Exception list */}
      <div className="card mt-4">
        <h3 className="font-medium text-sm mb-3">Exception History</h3>
        {isLoading ? (
          <div className="space-y-2" aria-busy="true" aria-label="Loading exceptions">
            {[1, 2, 3].map((i) => (
              <div key={i} className="h-16 bg-gray-100 animate-pulse rounded" />
            ))}
          </div>
        ) : exceptions.length === 0 ? (
          <p className="text-sm text-gray-400">No exceptions logged.</p>
        ) : (
          <div className="space-y-3">
            {exceptions.map((exc) => (
              <ExceptionCard
                key={exc.exceptionId}
                exception={exc}
                isOverriding={overrideTarget === exc.exceptionId}
                onStartOverride={() => setOverrideTarget(exc.exceptionId)}
                onCancelOverride={() => setOverrideTarget(null)}
                onOverride={(values) => handleOverride(exc.exceptionId, values)}
                isPending={overrideException.isPending}
              />
            ))}
          </div>
        )}
      </div>
    </div>
  );
}

// ---- Exception card ---------------------------------------------------------

function ExceptionCard({
  exception: exc,
  isOverriding,
  onStartOverride,
  onCancelOverride,
  onOverride,
  isPending,
}: {
  exception: ExceptionResponse;
  isOverriding: boolean;
  onStartOverride: () => void;
  onCancelOverride: () => void;
  onOverride: (values: ExceptionOverrideValues) => void;
  isPending: boolean;
}) {
  const {
    register,
    handleSubmit,
    formState: { errors, isValid },
  } = useForm<ExceptionOverrideValues>({
    resolver: zodResolver(exceptionOverrideSchema),
    mode: "onChange",
    defaultValues: {
      justification: "",
      overrideNurse: 0,
    },
  });

  const severity = getSeverity(exc.exceptionType);

  return (
    <div className={`border rounded p-3 ${SEVERITY_STYLES[severity]}`}>
      <div className="flex items-start justify-between gap-3">
        <div className="flex-1 min-w-0">
          <div className="flex items-center gap-2 mb-1">
            <span className="font-medium text-sm">{exc.exceptionType.replace(/_/g, " ")}</span>
            {exc.overridden ? (
              <span className="px-2 py-0.5 bg-orange-200 text-orange-900 rounded text-xs">Overridden</span>
            ) : (
              <span className="px-2 py-0.5 bg-red-200 text-red-900 rounded text-xs">Active</span>
            )}
          </div>
          <p className="text-sm">{exc.reason}</p>
          <div className="text-xs mt-1 opacity-75">
            Patient: {exc.patientId} · Drug: {exc.drugNdc} · Ward: {exc.wardId} ·{" "}
            {new Date(exc.createdAt).toLocaleString()}
          </div>
          {exc.overridden && exc.overrideJustification && (
            <div className="mt-2 text-xs bg-orange-50 border border-orange-200 rounded px-2 py-1">
              <strong>Override:</strong> {exc.overrideJustification}
            </div>
          )}
        </div>

        {!exc.overridden && !isOverriding && (
          <button
            onClick={onStartOverride}
            className="bg-orange-600 text-white px-3 py-1.5 rounded text-xs hover:bg-orange-700 shrink-0"
          >
            Override
          </button>
        )}
      </div>

      {/* Inline override form */}
      {isOverriding && !exc.overridden && (
        <form
          onSubmit={handleSubmit(onOverride)}
          className="mt-3 pt-3 border-t border-current/20 space-y-3"
          aria-label="Override exception"
        >
          <label className="text-sm block">
            <span className="block font-medium mb-1">Justification *</span>
            <textarea
              className="w-full border rounded px-3 py-2 text-sm bg-white"
              rows={2}
              placeholder="Clinical justification for override..."
              {...register("justification")}
              aria-invalid={errors.justification ? "true" : undefined}
            />
            {errors.justification && (
              <span className="text-xs text-red-600 mt-1 block">{errors.justification.message}</span>
            )}
          </label>

          <label className="text-sm block">
            <span className="block font-medium mb-1">Override Nurse ID *</span>
            <input
              type="number"
              className="w-full border rounded px-3 py-2 text-sm bg-white"
              {...register("overrideNurse", { valueAsNumber: true })}
              aria-invalid={errors.overrideNurse ? "true" : undefined}
            />
            {errors.overrideNurse && (
              <span className="text-xs text-red-600 mt-1 block">{errors.overrideNurse.message}</span>
            )}
          </label>

          <div className="flex gap-2">
            <button
              type="submit"
              disabled={!isValid || isPending}
              className="bg-orange-600 text-white px-3 py-1.5 rounded text-xs hover:bg-orange-700 disabled:opacity-50"
            >
              {isPending ? "Applying..." : "Apply Override"}
            </button>
            <button
              type="button"
              onClick={onCancelOverride}
              className="px-3 py-1.5 border rounded text-xs hover:bg-white/50"
            >
              Cancel
            </button>
          </div>
        </form>
      )}
    </div>
  );
}

// ---- Helpers ----------------------------------------------------------------

function getSeverity(exceptionType: string): "LOW" | "MEDIUM" | "HIGH" | "CRITICAL" {
  switch (exceptionType) {
    case "ALLERGY_CONFLICT":
    case "DRUG_INTERACTION":
      return "CRITICAL";
    case "DOSE_OUT_OF_RANGE":
    case "PATIENT_MISMATCH":
      return "HIGH";
    case "ROUTE_MISMATCH":
    case "DUPLICATE_THERAPY":
      return "MEDIUM";
    default:
      return "LOW";
  }
}

function getExceptionGuidance(type: string): string {
  switch (type) {
    case "ALLERGY_CONFLICT":
      return "Patient allergy profile conflicts with ordered medication. Verify allergy documentation before override.";
    case "DOSE_OUT_OF_RANGE":
      return "Dose falls outside typical range for this patient. Confirm with prescriber if intentional.";
    case "ROUTE_MISMATCH":
      return "Administration route does not match order. Verify if route change was authorized.";
    case "TIME_MISMATCH":
      return "Current time is outside the scheduled window. Confirm if early/late administration is clinically appropriate.";
    case "PATIENT_MISMATCH":
      return "Patient identity does not match medication order. Re-scan wristband and medication.";
    case "DRUG_INTERACTION":
      return "Potential drug-drug interaction detected. Review interaction severity before proceeding.";
    case "DUPLICATE_THERAPY":
      return "Patient may already be receiving this medication class. Verify with care team.";
    default:
      return "Document the exception clearly for audit trail purposes.";
  }
}

export default ExceptionLogger;
