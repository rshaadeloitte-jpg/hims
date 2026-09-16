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
import { FormField, HimsSelect, TextInput } from "../../components/ui";
import { Button, Textarea } from "@chakra-ui/react";
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
        <Button variant="secondary"
          onClick={() => setShowForm(!showForm)}
          className="bg-red-600 text-white px-4 py-2 rounded hover:bg-red-700 text-sm"
          aria-expanded={showForm}
        >
          {showForm ? "Cancel" : "+ Log Exception"}
        </Button>
      </header>

      {/* Create exception form */}
      {showForm && (
        <form
          onSubmit={handleCreateException}
          className="card p-4 space-y-4"
          aria-label="Create BCMA exception"
        >
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
            <FormField label="Patient ID  {errors.patientId && ( {errors.patientId.message} )}" required><TextInput
                type="number"
                {...register("patientId", { valueAsNumber: true })}
                aria-invalid={errors.patientId ? "true" : undefined}
              /></FormField>

            <FormField label="Drug NDC  {errors.drugNdc && ( {errors.drugNdc.message} )}" required><TextInput
                type="text"
                {...register("drugNdc")}
                aria-invalid={errors.drugNdc ? "true" : undefined}
              /></FormField>

            <FormField label="Exception Type  {errors.exceptionType && ( {errors.exceptionType.message} )}" required><HimsSelect
                {...register("exceptionType")}
                aria-invalid={errors.exceptionType ? "true" : undefined}
              >
                <option value="">Select type…</option>
                {EXCEPTION_TYPES.map((t) => (
                  <option key={t} value={t}>
                    {t.replace(/_/g, " ")}
                  </option>
                ))}
              </HimsSelect></FormField>

            <FormField label="Reason  {errors.reason && ( {errors.reason.message} )}" required><Textarea bg="white" borderColor="gray.300"
                rows={3}
                placeholder="Describe the exception in detail..."
                {...register("reason")}
                aria-invalid={errors.reason ? "true" : undefined}
              /></FormField>

            <FormField label="Override Nurse ID  {errors.overrideNurse && ( {errors.overrideNurse.message} )}" required><TextInput
                type="number"
                {...register("overrideNurse", { valueAsNumber: true })}
                aria-invalid={errors.overrideNurse ? "true" : undefined}
              /></FormField>

            <FormField label="Ward ID  {errors.wardId && ( {errors.wardId.message} )}" required><TextInput
                type="number"
                {...register("wardId", { valueAsNumber: true })}
                aria-invalid={errors.wardId ? "true" : undefined}
              /></FormField>

            <FormField label="Scanned By  {errors.scannedBy && ( {errors.scannedBy.message} )}" required><TextInput
                type="number"
                {...register("scannedBy", { valueAsNumber: true })}
                aria-invalid={errors.scannedBy ? "true" : undefined}
              /></FormField>
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
            <Button variant="primary" loading={!isValid || createException.isPending} loadingText="Logging..."
              type="submit"
              disabled={!isValid || createException.isPending}>
              Log Exception
            </Button>
            <Button variant="secondary"
              type="button"
              onClick={() => { reset(); setShowForm(false); }}
              className="px-4 py-2 border rounded hover:bg-gray-50"
            >
              Cancel
            </Button>
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
          <Button variant="secondary"
            onClick={onStartOverride}>
            Override
          </Button>
        )}
      </div>

      {/* Inline override form */}
      {isOverriding && !exc.overridden && (
        <form
          onSubmit={handleSubmit(onOverride)}
          className="mt-3 pt-3 border-t border-current/20 space-y-3"
          aria-label="Override exception"
        >
          <FormField label="Justification  {errors.justification && ( {errors.justification.message} )}" required><Textarea bg="white" borderColor="gray.300"
              rows={2}
              placeholder="Clinical justification for override..."
              {...register("justification")}
              aria-invalid={errors.justification ? "true" : undefined}
            /></FormField>

          <FormField label="Override Nurse ID  {errors.overrideNurse && ( {errors.overrideNurse.message} )}" required><TextInput
              type="number"
              {...register("overrideNurse", { valueAsNumber: true })}
              aria-invalid={errors.overrideNurse ? "true" : undefined}
            /></FormField>

          <div className="flex gap-2">
            <Button variant="primary" loading={!isValid || isPending} loadingText="Applying..."
              type="submit"
              disabled={!isValid || isPending}>
              Apply Override
            </Button>
            <Button variant="secondary"
              type="button"
              onClick={onCancelOverride}>
              Cancel
            </Button>
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
