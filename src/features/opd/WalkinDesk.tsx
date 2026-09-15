import { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { walkinSchema, type WalkinValues } from "./zodSchema";
import { useWalkin } from "./queries";
import { PatientSearch } from "../patient/PatientSearch";
import type { PatientSearchHit } from "../patient/queries";

/**
 * M02 Walk-in Desk (G-1). Combines patient search + walk-in check-in via
 * POST /api/v1/appointments/walkin. On success the appointment id is
 * surfaced so the front desk can hand the slip to the patient immediately.
 */
export function WalkinDesk({ onCheckedIn }: { onCheckedIn?: (appointmentId: number) => void }) {
  const [patient, setPatient] = useState<PatientSearchHit | null>(null);
  const walkin = useWalkin();

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
    reset,
  } = useForm<WalkinValues>({
    resolver: zodResolver(walkinSchema),
    defaultValues: { visitType: "WALK_IN", doctorId: "" as unknown as number, departmentId: "" as unknown as number },
  });

  const onSubmit = handleSubmit(async (values) => {
    const payload = {
      ...values,
      patientId: patient?.patientId,
    };
    const appt = (await walkin.mutateAsync(payload)) as { id: number };
    onCheckedIn?.(appt.id);
    reset({ visitType: values.visitType, doctorId: "" as unknown as number, departmentId: "" as unknown as number });
  });

  return (
    <div className="space-y-4 p-4 border rounded" aria-label="walkin-desk">
      <h2 className="text-xl font-semibold">Walk-in Desk</h2>
      <PatientSearch onSelect={(p) => setPatient(p as PatientSearchHit)} />

      {patient && (
        <div className="p-3 bg-green-50 border border-green-200 rounded text-sm">
          Selected: <strong>{patient.firstName} {patient.lastName}</strong>{" "}
          (MRN {patient.mrNo}, {patient.mobileNo})
        </div>
      )}

      <form onSubmit={onSubmit} className="space-y-2">
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-2">
          <label className="text-sm">
            <span className="block font-medium">Doctor ID *</span>
            <input type="number" className="w-full border rounded px-2 py-1" {...register("doctorId")} />
            {errors.doctorId && <span className="text-red-600 text-xs">{errors.doctorId.message}</span>}
          </label>
          <label className="text-sm">
            <span className="block font-medium">Department ID</span>
            <input type="number" className="w-full border rounded px-2 py-1" {...register("departmentId")} />
          </label>
          <label className="text-sm">
            <span className="block font-medium">Visit type</span>
            <select className="w-full border rounded px-2 py-1" {...register("visitType")}>
              <option value="WALK_IN">Walk-in</option>
              <option value="EMERGENCY">Emergency</option>
            </select>
          </label>
        </div>

        {walkin.isError && (
          <p className="text-sm text-red-600">{(walkin.error as Error).message}</p>
        )}

        <button
          type="submit"
          className="bg-blue-600 text-white px-4 py-2 rounded disabled:opacity-50"
          disabled={!patient || isSubmitting || walkin.isPending}
        >
          {walkin.isPending ? "Checking in..." : "Check In"}
        </button>
      </form>
    </div>
  );
}
