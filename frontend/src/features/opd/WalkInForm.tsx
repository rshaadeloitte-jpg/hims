import { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { Button, SimpleGrid, Text } from "@chakra-ui/react";
import { FormField, HimsSelect, TextInput } from "../../components/ui";
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
          <Text fontSize="sm" fontWeight="medium">Search Patient</Text>
          <PatientSearch onSelect={(p) => setPatient(p as PatientSearchHit)} />
          {patient && (
            <div className="p-3 bg-green-50 border border-green-200 rounded text-sm">
              Selected: <strong>{patient.firstName} {patient.lastName}</strong>{" "}
              (MRN {patient.mrNo})
              <Button
                type="button"
                variant="ghost"
                size="sm"
                onClick={() => { setPatient(null); setValue("patientId" as any, undefined); }}
              >
                Change
              </Button>
            </div>
          )}
        </div>
      )}

      {kioskMode && (
        <FormField label="Patient ID" required error={errors.patientId?.message}>
          <TextInput
            type="number"
            placeholder="Enter patient ID"
            {...register("patientId")}
          />
        </FormField>
      )}

      <form onSubmit={onSubmit} className="space-y-3">
        <SimpleGrid columns={{ base: 1, sm: 2, lg: 4 }} gap={3}>
          <FormField label="Doctor ID" required error={errors.doctorId?.message}>
            <TextInput
              type="number"
              placeholder="Doctor ID"
              {...register("doctorId")}
            />
          </FormField>

          <FormField label="Department ID" error={errors.departmentId?.message}>
            <TextInput
              type="number"
              {...register("departmentId")}
            />
          </FormField>

          <FormField label="Unit ID" error={errors.unitId?.message}>
            <TextInput
              type="number"
              {...register("unitId")}
            />
          </FormField>

          <FormField label="Visit Type">
            <HimsSelect {...register("visitType")}>
              <option value="WALK_IN">Walk-in</option>
              <option value="EMERGENCY">Emergency</option>
              <option value="APPOINTMENT">Appointment</option>
            </HimsSelect>
          </FormField>
        </SimpleGrid>

        <FormField label="Cabin">
          <TextInput
            type="text"
            placeholder="e.g. Cabin 3A"
            {...register("cabin")}
          />
        </FormField>

        {walkin.isError && (
          <p className="text-sm text-red-600">
            {(walkin.error as Error).message}
          </p>
        )}

        <Button
          type="submit"
          variant="primary"
          disabled={(!kioskMode && !patient) || isSubmitting}
          loading={walkin.isPending}
          loadingText="Checking in…"
        >
          Check In
        </Button>
      </form>
    </div>
  );
}
