import { useState } from "react";
import { useDonors, useCreateDonor } from "./queries";
import { donorSchema, type DonorForm } from "./zodSchema";

const GENDERS = ["MALE", "FEMALE", "OTHER"] as const;
const BLOOD_GROUPS = ["A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-"];

type FormFieldProps = {
  label: string;
  value: string;
  onChange: (v: string) => void;
  type?: string;
  error?: string;
  options?: string[];
};

function FormField({ label, value, onChange, type = "text", error, options }: FormFieldProps) {
  return (
    <label>
      {label}
      {type === "select" ? (
        <select value={value} onChange={(e) => onChange(e.target.value)}>
          <option value="">Select...</option>
          {options?.map((o) => (
            <option key={o} value={o}>{o}</option>
          ))}
        </select>
      ) : (
        <input type={type} value={value} onChange={(e) => onChange(e.target.value)} />
      )}
      {error && <span className="error">{error}</span>}
    </label>
  );
}

export function DonorRegistration() {
  const { data: donors = [], isLoading } = useDonors();
  const createDonor = useCreateDonor();
  const [showForm, setShowForm] = useState(false);
  const [errors, setErrors] = useState<Record<string, string>>({});

  const blank: DonorForm = {
    name: "",
    age: "",
    gender: "MALE",
    bloodGroup: "",
    phone: "",
    weight: "",
    pulse: "",
    bpSystolic: "",
    bpDiastolic: "",
    hemoglobin: "",
    lastDonationAt: "",
  };
  const [form, setForm] = useState<DonorForm>(blank);

  const update = (patch: Partial<DonorForm>) => setForm((f) => ({ ...f, ...patch }));

  const handleRegister = () => {
    const result = donorSchema.safeParse({
      ...form,
      age: Number(form.age),
      weight: form.weight ? Number(form.weight) : undefined,
      pulse: form.pulse ? Number(form.pulse) : undefined,
      bpSystolic: form.bpSystolic ? Number(form.bpSystolic) : undefined,
      bpDiastolic: form.bpDiastolic ? Number(form.bpDiastolic) : undefined,
      hemoglobin: form.hemoglobin ? Number(form.hemoglobin) : undefined,
      lastDonationAt: form.lastDonationAt || undefined,
    });
    if (!result.success) {
      const fieldErrors: Record<string, string> = {};
      result.error.errors.forEach((e) => { fieldErrors[e.path[0]] = e.message; });
      setErrors(fieldErrors);
      return;
    }
    createDonor.mutate(result.data, {
      onSuccess: () => {
        setShowForm(false);
        setForm(blank);
        setErrors({});
      },
    });
  };

  if (isLoading) return <p className="muted">Loading donors...</p>;

  return (
    <section className="page">
      <header className="page-header">
        <h1>Donor Registration</h1>
        <button className="btn btn-primary" onClick={() => setShowForm(!showForm)}>
          {showForm ? "Cancel" : "Register Donor"}
        </button>
      </header>

      {showForm && (
        <div className="card">
          <h2>Donor Details</h2>
          <div className="form-grid">
            <FormField label="Full Name" value={form.name} onChange={(v) => update({ name: v })} error={errors.name} />
            <FormField label="Age" value={String(form.age)} onChange={(v) => update({ age: v })} type="number" error={errors.age} />
            <FormField label="Gender" value={form.gender} onChange={(v) => update({ gender: v as DonorForm["gender"] })} type="select" options={Array.from(GENDERS)} />
            <FormField label="Blood Group" value={form.bloodGroup} onChange={(v) => update({ bloodGroup: v })} type="select" error={errors.bloodGroup} options={BLOOD_GROUPS} />
            <FormField label="Phone" value={form.phone ?? ""} onChange={(v) => update({ phone: v })} />
            <FormField label="Weight (kg)" value={String(form.weight ?? "")} onChange={(v) => update({ weight: v })} type="number" error={errors.weight} />
            <FormField label="Pulse" value={String(form.pulse ?? "")} onChange={(v) => update({ pulse: v })} type="number" error={errors.pulse} />
            <FormField label="BP Systolic" value={String(form.bpSystolic ?? "")} onChange={(v) => update({ bpSystolic: v })} type="number" error={errors.bpSystolic} />
            <FormField label="BP Diastolic" value={String(form.bpDiastolic ?? "")} onChange={(v) => update({ bpDiastolic: v })} type="number" error={errors.bpDiastolic} />
            <FormField label="Hemoglobin" value={String(form.hemoglobin ?? "")} onChange={(v) => update({ hemoglobin: v })} type="number" error={errors.hemoglobin} />
            <FormField label="Last Donation Date" value={form.lastDonationAt ?? ""} onChange={(v) => update({ lastDonationAt: v })} type="date" />
          </div>
          <button className="btn btn-primary" onClick={handleRegister} disabled={createDonor.isPending}>
            {createDonor.isPending ? "Saving..." : "Register"}
          </button>
        </div>
      )}

      <div className="card">
        <h2>Registered Donors</h2>
        <table className="table">
          <thead>
            <tr><th>Name</th><th>Age</th><th>Gender</th><th>Group</th><th>Phone</th><th>Last Donation</th></tr>
          </thead>
          <tbody>
            {donors.map((d) => (
              <tr key={d.id}>
                <td>{d.name}</td>
                <td>{d.age}</td>
                <td>{d.gender}</td>
                <td>{d.bloodGroup}</td>
                <td>{d.phone}</td>
                <td>{d.lastDonationAt || "-"}</td>
              </tr>
            ))}
            {donors.length === 0 && <tr><td colSpan={6} className="muted">No donors yet.</td></tr>}
          </tbody>
        </table>
      </div>
    </section>
  );
}
