import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { patientRegistrationSchema, type PatientRegistrationValues } from "./zodSchema";
import { useRegisterPatient } from "./queries";

/**
 * M02 Patient Registration form (Registration Form, G-1). Wraps react-hook-form
 * + zod and posts to patient-service /api/v1/patients. The legacy backend is
 * tolerant of partial payloads (PatientRegApptServiceImpl parity) so we
 * forward every defined field and let the server handle nulls.
 */
export function PatientRegistrationForm({ onCreated }: { onCreated?: (id: number) => void }) {
  const mutation = useRegisterPatient();
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
    reset,
  } = useForm<PatientRegistrationValues>({
    resolver: zodResolver(patientRegistrationSchema),
    defaultValues: {
      firstName: "",
      middleName: "",
      lastName: "",
      mobileNo: "",
      abhaAddress: "",
      address: "",
      email: "",
      emergencyContact: "",
      bloodGroup: "",
    },
  });

  const onSubmit = handleSubmit(async (values) => {
    // Strip empties so backend sees undefined (not "") for optionals
    const payload: Record<string, unknown> = {};
    for (const [k, v] of Object.entries(values)) {
      if (v === "" || v == null) continue;
      payload[k] = v;
    }
    const created = await mutation.mutateAsync(payload);
    onCreated?.(created.patientId);
    reset();
  });

  return (
    <form onSubmit={onSubmit} className="space-y-3 p-4 border rounded" aria-label="patient-registration">
      <h2 className="text-lg font-semibold">Patient Registration</h2>

      <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
        <Field label="First name" required error={errors.firstName?.message}>
          <input className="w-full border rounded px-2 py-1" {...register("firstName")} />
        </Field>
        <Field label="Middle name" error={errors.middleName?.message}>
          <input className="w-full border rounded px-2 py-1" {...register("middleName")} />
        </Field>
        <Field label="Last name" error={errors.lastName?.message}>
          <input className="w-full border rounded px-2 py-1" {...register("lastName")} />
        </Field>
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
        <Field label="Mobile" required error={errors.mobileNo?.message}>
          <input className="w-full border rounded px-2 py-1" inputMode="numeric" {...register("mobileNo")} />
        </Field>
        <Field label="ABHA address" error={errors.abhaAddress?.message}>
          <input className="w-full border rounded px-2 py-1" {...register("abhaAddress")} />
        </Field>
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
        <Field label="Date of birth" error={errors.dateOfBirth?.message}>
          <input type="date" className="w-full border rounded px-2 py-1" {...register("dateOfBirth")} />
        </Field>
        <Field label="Gender ID" error={errors.genderId?.message}>
          <input type="number" className="w-full border rounded px-2 py-1" {...register("genderId")} />
        </Field>
        <Field label="Blood group" error={errors.bloodGroup?.message}>
          <select className="w-full border rounded px-2 py-1" {...register("bloodGroup")}>
            <option value="">Select</option>
            {["A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-"].map((b) => (
              <option key={b} value={b}>
                {b}
              </option>
            ))}
          </select>
        </Field>
      </div>

      <Field label="Address" error={errors.address?.message}>
        <textarea className="w-full border rounded px-2 py-1" rows={2} {...register("address")} />
      </Field>

      <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
        <Field label="Email" error={errors.email?.message}>
          <input type="email" className="w-full border rounded px-2 py-1" {...register("email")} />
        </Field>
        <Field label="Emergency contact" error={errors.emergencyContact?.message}>
          <input className="w-full border rounded px-2 py-1" inputMode="numeric" {...register("emergencyContact")} />
        </Field>
      </div>

      {mutation.isError && (
        <p className="text-sm text-red-600">{(mutation.error as Error).message}</p>
      )}
      {mutation.isSuccess && (
        <p className="text-sm text-green-600">Registered. Patient ID: {mutation.data.patientId}</p>
      )}

      <button
        type="submit"
        className="bg-blue-600 text-white px-4 py-2 rounded disabled:opacity-50"
        disabled={isSubmitting || mutation.isPending}
      >
        {mutation.isPending ? "Saving..." : "Register Patient"}
      </button>
    </form>
  );
}

function Field({
  label,
  children,
  required,
  error,
}: {
  label: string;
  children: React.ReactNode;
  required?: boolean;
  error?: string;
}) {
  return (
    <label className="block text-sm">
      <span className="block font-medium mb-1">
        {label}
        {required && <span className="text-red-500"> *</span>}
      </span>
      {children}
      {error && <span className="text-red-600 text-xs">{error}</span>}
    </label>
  );
}
