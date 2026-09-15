import { useState, useEffect } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import {
  fiveRightsScanSchema,
  rightPatientSchema,
  rightDrugSchema,
  rightDoseSchema,
  rightRouteSchema,
  rightTimeSchema,
  type FiveRightsScanValues,
  type VerificationResponse,
  type VerificationStep,
  VERIFICATION_STEPS,
} from "./zodSchema";
import {
  useStartFiveRightsScan,
  useVerifyRight,
  useFinalizeVerification,
  useFiveRightsScans,
} from "./queries";

/**
 * ScanWorkflow — M30 BCMA five-rights bedside verification.
 *
 * Stripe-style inline verification patterns:
 * - Step wizard with clear progress indicator
 * - Real-time validation per step (inline error messaging)
 * - Visual feedback: ✓ green for pass, ✗ red for fail with context
 * - Override flow when rights fail (audited server-side)
 * - Accessible: each step is a labeled group, keyboard-navigable
 * - Skeleton loading while in-flight, error states with retry
 */

const STEP_LABELS: Record<VerificationStep, string> = {
  PATIENT: "Right Patient",
  DRUG: "Right Drug",
  DOSE: "Right Dose",
  ROUTE: "Right Route",
  TIME: "Right Time",
};

const STEP_DESCRIPTIONS: Record<VerificationStep, string> = {
  PATIENT: "Scan patient wristband and verify identity matches medication order",
  DRUG: "Scan drug barcode and verify it matches the ordered medication",
  DOSE: "Confirm dosage on label matches the order",
  ROUTE: "Verify administration route (oral, IV, IM, topical, etc.)",
  TIME: "Confirm this is the correct time for this dose",
};

export function ScanWorkflow({ patientId }: { patientId?: number }) {
  const [bcmaId, setBcmaId] = useState<number | null>(null);
  const [stepResults, setStepResults] = useState<Record<string, boolean>>({});
  const [currentStep, setCurrentStep] = useState<number>(-1); // -1 = initial scan form
  const [error, setError] = useState<string | null>(null);

  const { data: scans = [], isLoading: scansLoading } = useFiveRightsScans(patientId);
  const startScan = useStartFiveRightsScan();
  const verifyRight = useVerifyRight();
  const finalize = useFinalizeVerification();

  // Reset state when bcmaId changes (new scan)
  useEffect(() => {
    if (bcmaId === null) {
      setStepResults({});
      setCurrentStep(-1);
    }
  }, [bcmaId]);

  // Initial scan form
  const {
    register: registerInitial,
    handleSubmit: handleSubmitInitial,
    formState: { errors: errorsInitial, isValid: isValidInitial },
  } = useForm<FiveRightsScanValues>({
    resolver: zodResolver(fiveRightsScanSchema),
    mode: "onChange",
    defaultValues: {
      patientId: patientId ?? 0,
      wristbandBarcode: "",
      drugNdc: "",
    },
  });

  const handleStartScan = handleSubmitInitial(async (values) => {
    setError(null);
    try {
      const result = await startScan.mutateAsync(values);
      setBcmaId(result.bcmaId);
      setCurrentStep(0); // Move to first verification step
    } catch (err) {
      setError((err as Error).message);
    }
  });

  // Per-step verification forms
  const {
    register: registerPatient,
    handleSubmit: handleSubmitPatient,
  } = useForm({ resolver: zodResolver(rightPatientSchema), defaultValues: { match: true } });

  const {
    register: registerDrug,
    handleSubmit: handleSubmitDrug,
  } = useForm({ resolver: zodResolver(rightDrugSchema), defaultValues: { match: true } });

  const {
    register: registerDose,
    handleSubmit: handleSubmitDose,
  } = useForm({ resolver: zodResolver(rightDoseSchema), defaultValues: { match: true } });

  const {
    register: registerRoute,
    handleSubmit: handleSubmitRoute,
  } = useForm({ resolver: zodResolver(rightRouteSchema), defaultValues: { match: true } });

  const {
    register: registerTime,
    handleSubmit: handleSubmitTime,
  } = useForm({ resolver: zodResolver(rightTimeSchema), defaultValues: { match: true } });

  const handleStepVerification = async (
    step: VerificationStep,
    match: boolean
  ) => {
    if (!bcmaId) return;
    setError(null);
    try {
      const stepLower = step.toLowerCase() as "patient" | "drug" | "dose" | "route" | "time";
      await verifyRight.mutateAsync({ step: stepLower, bcmaId, match });
      setStepResults((prev) => ({ ...prev, [step]: match }));

      if (currentStep < VERIFICATION_STEPS.length - 1) {
        setCurrentStep((s) => s + 1);
      }
    } catch (err) {
      setError((err as Error).message);
    }
  };

  const handleFinalize = async () => {
    if (!bcmaId) return;
    setError(null);
    try {
      await finalize.mutateAsync(bcmaId);
      // Reset for next scan
      setBcmaId(null);
      setStepResults({});
      setCurrentStep(-1);
    } catch (err) {
      setError((err as Error).message);
    }
  };

  const handleReset = () => {
    setBcmaId(null);
    setStepResults({});
    setCurrentStep(-1);
    setError(null);
  };

  const isAllRightsPassed =
    bcmaId !== null &&
    Object.values(stepResults).length === VERIFICATION_STEPS.length &&
    Object.values(stepResults).every((r) => r === true);

  const canFinalize =
    bcmaId !== null &&
    Object.values(stepResults).length === VERIFICATION_STEPS.length;

  // ---- Initial scan form ---------------------------------------------------

  if (currentStep === -1) {
    return (
      <div className="bcma-scan-workflow" role="region" aria-label="BCMA Scan Workflow">
        <header className="page-header">
          <h2>5-Rights Bedside Verification</h2>
          <p className="text-sm text-gray-500">
            Scan patient wristband and medication to begin verification
          </p>
        </header>

        <form
          onSubmit={handleStartScan}
          className="card p-4 space-y-4"
          aria-label="Start BCMA scan"
        >
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
            <label className="text-sm">
              <span className="block font-medium mb-1">Patient ID *</span>
              <input
                type="number"
                className="w-full border rounded px-3 py-2"
                {...registerInitial("patientId", { valueAsNumber: true })}
                aria-invalid={errorsInitial.patientId ? "true" : undefined}
              />
              {errorsInitial.patientId && (
                <span className="field-error text-xs text-red-600 mt-1 block">
                  {errorsInitial.patientId.message}
                </span>
              )}
            </label>

            <label className="text-sm">
              <span className="block font-medium mb-1">Wristband Barcode *</span>
              <input
                type="text"
                className="w-full border rounded px-3 py-2 font-mono"
                placeholder="Scan wristband..."
                {...registerInitial("wristbandBarcode")}
                aria-invalid={errorsInitial.wristbandBarcode ? "true" : undefined}
              />
              {errorsInitial.wristbandBarcode && (
                <span className="field-error text-xs text-red-600 mt-1 block">
                  {errorsInitial.wristbandBarcode.message}
                </span>
              )}
            </label>

            <label className="text-sm">
              <span className="block font-medium mb-1">Drug NDC *</span>
              <input
                type="text"
                className="w-full border rounded px-3 py-2 font-mono"
                placeholder="Scan medication..."
                {...registerInitial("drugNdc")}
                aria-invalid={errorsInitial.drugNdc ? "true" : undefined}
              />
              {errorsInitial.drugNdc && (
                <span className="field-error text-xs text-red-600 mt-1 block">
                  {errorsInitial.drugNdc.message}
                </span>
              )}
            </label>
          </div>

          {error && (
            <p className="text-sm text-red-600 bg-red-50 border border-red-200 rounded px-3 py-2" role="alert">
              {error}
            </p>
          )}

          <div className="flex items-center gap-3">
            <button
              type="submit"
              className="bg-blue-600 text-white px-4 py-2 rounded hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed"
              disabled={!isValidInitial || startScan.isPending}
            >
              {startScan.isPending ? "Starting..." : "Start Verification"}
            </button>
            <span className="text-xs text-gray-500">
              All fields required · Barcode scanner auto-advances
            </span>
          </div>
        </form>

        {/* Recent scans table */}
        <div className="card mt-4">
          <h3 className="font-medium text-sm mb-3">Recent Verifications</h3>
          {scansLoading ? (
            <div className="space-y-2" aria-busy="true" aria-label="Loading recent scans">
              {[1, 2, 3].map((i) => (
                <div key={i} className="h-10 bg-gray-100 animate-pulse rounded" />
              ))}
            </div>
          ) : scans.length === 0 ? (
            <p className="text-sm text-gray-400">No verifications yet.</p>
          ) : (
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b text-left">
                  <th className="py-2 px-2">Patient</th>
                  <th className="py-2 px-2">Drug NDC</th>
                  <th className="py-2 px-2">Scan Time</th>
                  <th className="py-2 px-2">Status</th>
                </tr>
              </thead>
              <tbody>
                {scans.slice(0, 5).map((s) => (
                  <tr key={s.bcmaId} className="border-b">
                    <td className="py-2 px-2">{s.patientId}</td>
                    <td className="py-2 px-2 font-mono text-xs">{s.drugNdc}</td>
                    <td className="py-2 px-2">{new Date(s.scanTime).toLocaleString()}</td>
                    <td className="py-2 px-2">
                      <StatusBadge verification={s} />
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      </div>
    );
  }

  // ---- Step-by-step verification wizard ------------------------------------

  const step = VERIFICATION_STEPS[currentStep];

  return (
    <div className="bcma-scan-workflow" role="region" aria-label="BCMA Verification Steps">
      {/* Progress indicator */}
      <div className="flex items-center gap-2 mb-4">
        {VERIFICATION_STEPS.map((s, i) => (
          <div
            key={s}
            className={`flex-1 h-2 rounded ${
              stepResults[s] === true
                ? "bg-green-500"
                : stepResults[s] === false
                ? "bg-red-500"
                : i === currentStep
                ? "bg-blue-500"
                : "bg-gray-200"
            }`}
            aria-label={`${STEP_LABELS[s]}: ${
              stepResults[s] === true
                ? "passed"
                : stepResults[s] === false
                ? "failed"
                : i === currentStep
                ? "in progress"
                : "pending"
            }`}
          />
        ))}
      </div>

      <header className="page-header mb-4">
        <h2>Step {currentStep + 1} of {VERIFICATION_STEPS.length}: {STEP_LABELS[step]}</h2>
        <p className="text-sm text-gray-500">{STEP_DESCRIPTIONS[step]}</p>
      </header>

      {/* Current step form */}
      <div className="card p-4" role="group" aria-label={`Verify ${STEP_LABELS[step]}`}>
        {error && (
          <p className="text-sm text-red-600 bg-red-50 border border-red-200 rounded px-3 py-2 mb-4" role="alert">
            {error}
          </p>
        )}

        {step === "PATIENT" && (
          <form onSubmit={handleSubmitPatient((v) => handleStepVerification("PATIENT", v.match))} className="space-y-3">
            <label className="flex items-center gap-3 cursor-pointer p-3 border rounded hover:bg-green-50">
              <input type="checkbox" {...registerPatient("match")} className="w-5 h-5" />
              <span className="font-medium">Patient identity verified — wristband matches order</span>
            </label>
            <button type="submit" className="bg-green-600 text-white px-4 py-2 rounded hover:bg-green-700">
              Confirm Right Patient →
            </button>
          </form>
        )}

        {step === "DRUG" && (
          <form onSubmit={handleSubmitDrug((v) => handleStepVerification("DRUG", v.match))} className="space-y-3">
            <label className="flex items-center gap-3 cursor-pointer p-3 border rounded hover:bg-green-50">
              <input type="checkbox" {...registerDrug("match")} className="w-5 h-5" />
              <span className="font-medium">Drug barcode verified — medication matches order</span>
            </label>
            <button type="submit" className="bg-green-600 text-white px-4 py-2 rounded hover:bg-green-700">
              Confirm Right Drug →
            </button>
          </form>
        )}

        {step === "DOSE" && (
          <form onSubmit={handleSubmitDose((v) => handleStepVerification("DOSE", v.match))} className="space-y-3">
            <label className="flex items-center gap-3 cursor-pointer p-3 border rounded hover:bg-green-50">
              <input type="checkbox" {...registerDose("match")} className="w-5 h-5" />
              <span className="font-medium">Dose verified — label matches prescribed amount</span>
            </label>
            <button type="submit" className="bg-green-600 text-white px-4 py-2 rounded hover:bg-green-700">
              Confirm Right Dose →
            </button>
          </form>
        )}

        {step === "ROUTE" && (
          <form onSubmit={handleSubmitRoute((v) => handleStepVerification("ROUTE", v.match))} className="space-y-3">
            <label className="flex items-center gap-3 cursor-pointer p-3 border rounded hover:bg-green-50">
              <input type="checkbox" {...registerRoute("match")} className="w-5 h-5" />
              <span className="font-medium">Route verified — administration route matches order</span>
            </label>
            <button type="submit" className="bg-green-600 text-white px-4 py-2 rounded hover:bg-green-700">
              Confirm Right Route →
            </button>
          </form>
        )}

        {step === "TIME" && (
          <form onSubmit={handleSubmitTime((v) => handleStepVerification("TIME", v.match))} className="space-y-3">
            <label className="flex items-center gap-3 cursor-pointer p-3 border rounded hover:bg-green-50">
              <input type="checkbox" {...registerTime("match")} className="w-5 h-5" />
              <span className="font-medium">Time verified — this is the correct administration time</span>
            </label>
            <button type="submit" className="bg-green-600 text-white px-4 py-2 rounded hover:bg-green-700">
              Confirm Right Time
            </button>
          </form>
        )}
      </div>

      {/* Step results summary */}
      <div className="card mt-4 p-4">
        <h3 className="font-medium text-sm mb-3">Verification Summary</h3>
        <div className="grid grid-cols-1 sm:grid-cols-5 gap-2">
          {VERIFICATION_STEPS.map((s) => (
            <div
              key={s}
              className={`px-3 py-2 rounded text-sm text-center font-medium ${
                stepResults[s] === true
                  ? "bg-green-100 text-green-800 border border-green-200"
                  : stepResults[s] === false
                  ? "bg-red-100 text-red-800 border border-red-200"
                  : "bg-gray-100 text-gray-500 border border-gray-200"
              }`}
            >
              {stepResults[s] === true ? "✓" : stepResults[s] === false ? "✗" : "○"} {STEP_LABELS[s]}
            </div>
          ))}
        </div>

        {canFinalize && (
          <div className="mt-4 flex gap-3">
            <button
              onClick={handleFinalize}
              disabled={finalize.isPending}
              className={`px-4 py-2 rounded font-medium ${
                isAllRightsPassed
                  ? "bg-green-600 hover:bg-green-700 text-white"
                  : "bg-orange-600 hover:bg-orange-700 text-white"
              } disabled:opacity-50`}
            >
              {finalize.isPending
                ? "Finalizing..."
                : isAllRightsPassed
                ? "✓ Finalize & Administer"
                : "Finalize (Override Required)"}
            </button>
            <button
              onClick={handleReset}
              className="px-4 py-2 border rounded hover:bg-gray-50"
            >
              Cancel
            </button>
          </div>
        )}
      </div>
    </div>
  );
}

// ---- Helpers ---------------------------------------------------------------

function StatusBadge({ verification }: { verification: VerificationResponse }) {
  if (verification.administered) {
    return <span className="px-2 py-0.5 bg-green-100 text-green-800 rounded text-xs">Administered</span>;
  }
  if (verification.overridden) {
    return <span className="px-2 py-0.5 bg-orange-100 text-orange-800 rounded text-xs">Overridden</span>;
  }
  if (verification.allRightsPassed) {
    return <span className="px-2 py-0.5 bg-green-100 text-green-800 rounded text-xs">All Passed</span>;
  }
  return <span className="px-2 py-0.5 bg-red-100 text-red-800 rounded text-xs">Failed</span>;
}

export default ScanWorkflow;
