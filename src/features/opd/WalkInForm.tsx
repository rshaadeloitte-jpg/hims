import { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { walkinSchema, type WalkinValues } from "./zodSchema";
import { useWalkin } from "./queries";
import { PatientSearch } from "../patient/PatientSearch";
import type { PatientSearchHit } from "../patient/queries";

/**
 * M02 Walk-in Registration Form (Page Flows pattern).
 * Kiosk mode hides the patient search for counter staff.
 */
export function WalkInForm({
  kioskMode = false,
  onCheckedIn,
}: {
  kioskMode?: boolean;
  onCheckedIn?: (appointmentId: number) => void;
}) {
  const [patient, setPatient] = useState<PatientSearchHit | null>(null);
  const walkin = useWalkin();

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
    reset,
    setValue,
  } = useForm<WalkinValues>({
    resolver: zodResolver(walkinSchema),
    defaultValues: {
      visitType: "WALK_IN",
    },
  });

  const onSubmit = handleSubmit(async (values) => {
    if (!kioskMode && !patient?.patientId) return;
    const payload = {
      ...values,
      patientId: kioskMode ? values.patientId : patient?.patientId,
    };
    const appt = (await walkin.mutateAsync(payload)) as { id: number };
    onCheckedIn?.(appt.id);
    reset({ visitType: "WALK_IN" });
    if (!kioskMode) setPatient(null);
  });

  return (
    <div className="space-y-4 p-4 border rounded-lg bg-white" aria-label="walkin-form">
      <div className="flex items-center justify-between">
        <h2 className="text-lg font-semibold">Walk-in Registration</h2>
        {kioskMode && (
          <span className="text-xs bg-yellow-100 text-yellow-700 px-2 py-0.5 rounded font-medium">
            Kiosk Mode
          </span>
        )}
      </div>

      {!kioskMode && (
        <div className="space-y-2">
          <label className="text-sm font-medium">Search Patient</label>
          <PatientSearch onSelect={(p) => setPatient(p as PatientSearchHit)} />
          {patient && (
            <div className="p-3 bg-green-50 border border-green-200 rounded text-sm">
              Selected: <strong>{patient.firstName} {patient.lastName}</strong>{" "}
              (MRN {patient.mrNo})
              <button
                type="button"
                className="ml-2 text-blue-600 underline text-xs"
                onClick={() => { setPatient(null); setValue("patientId" as any, undefined); }}
              >
                Change
              </button>
            </div>
          )}
        </div>
      )}

      {kioskMode && (
        <div className="space-y-1">
          <label className="text-sm font-medium">Patient ID *</label>
          <input
            type="number"
            className="w-full border rounded px-2 py-1.5"
            placeholder="Enter patient ID"
            {...register("patientId")}
          />
          {errors.patientId && (
            <p className="text-xs text-red-600">{errors.patientId.message}</p>
          )}
        </div>
      )}

      <form onSubmit={onSubmit} className="space-y-3">
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-3">
          <label className="text-sm space-y-1">
            <span className="font-medium">Doctor ID *</span>
            <input
              type="number"
              className="w-full border rounded px-2 py-1.5"
              placeholder="Doctor ID"
              {...register("doctorId")}
            />
            {errors.doctorId && (
              <p className="text-xs text-red-600">{errors.doctorId.message}</p>
            )}
          </label>

          <label className="text-sm space-y-1">
            <span className="font-medium">Department ID</span>
            <input
              type="number"
              className="w-full border rounded px-2 py-1.5"
              {...register("departmentId")}
            />
          </label>

          <label className="text-sm space-y-1">
            <span className="font-medium">Unit ID</span>
            <input
              type="number"
              className="w-full border rounded px-2 py-1.5"
              {...register("unitId")}
            />
          </label>

          <label className="text-sm space-y-1">
            <span className="font-medium">Visit Type</span>
            <select className="w-full border rounded px-2 py-1.5" {...register("visitType")}>
              <option value="WALK_IN">Walk-in</option>
              <option value="EMERGENCY">Emergency</option>
              <option value="APPOINTMENT">Appointment</option>
            </select>
          </label>
        </div>

        <label className="text-sm space-y-1">
          <span className="font-medium">Cabin</span>
          <input
            type="text"
            className="w-full sm:w-48 border rounded px-2 py-1.5"
            placeholder="e.g. Cabin 3A"
            {...register("cabin")}
          />
        </label>

        {walkin.isError && (
          <p className="text-sm text-red-600">
            {(walkin.error as Error).message}
          </p>
        )}

        <button
          type="submit"
          className="bg-blue-600 text-white px-4 py-2 rounded hover:bg-blue-700 disabled:opacity-50 font-medium"
          disabled={(!kioskMode && !patient) || isSubmitting || walkin.isPending}
        >
          {walkin.isPending ? "Checking in…" : "Check In"}
        </button>
      </form>
    </div>
  );
}