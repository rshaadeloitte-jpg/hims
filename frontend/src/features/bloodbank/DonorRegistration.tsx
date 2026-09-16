import { useState } from "react";
import { Button, SimpleGrid, Table } from "@chakra-ui/react";
import { FormField, HimsSelect, TextInput } from "../../components/ui";
import { useDonors, useCreateDonor } from "./queries";
import { donorSchema, type DonorForm } from "./zodSchema";

const GENDERS = ["MALE", "FEMALE", "OTHER"] as const;
const BLOOD_GROUPS = ["A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-"];

export function DonorRegistration() {
  const { data: donors = [], isLoading } = useDonors();
  const createDonor = useCreateDonor();
  const [showForm, setShowForm] = useState(false);
  const [errors, setErrors] = useState<Record<string, string>>({});

  const blank: DonorForm = {
    name: "",
    age: "" as unknown as number,
    gender: "MALE",
    bloodGroup: "",
    phone: "",
    weight: "" as unknown as number,
    pulse: "" as unknown as number,
    bpSystolic: "" as unknown as number,
    bpDiastolic: "" as unknown as number,
    hemoglobin: "" as unknown as number,
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
        <Button variant={showForm ? "secondary" : "primary"} onClick={() => setShowForm(!showForm)}>
          {showForm ? "Cancel" : "Register Donor"}
        </Button>
      </header>

      {showForm && (
        <div className="card">
          <h2>Donor Details</h2>
          <SimpleGrid columns={{ base: 1, md: 2 }} gap={4} className="form-grid">
            <FormField label="Full Name" error={errors.name}>
              <TextInput aria-label="Full Name" value={form.name} onChange={(e) => update({ name: e.target.value })} />
            </FormField>
            <FormField label="Age" error={errors.age}>
              <TextInput aria-label="Age" type="number" value={String(form.age)} onChange={(e) => update({ age: e.target.value as unknown as number })} />
            </FormField>
            <FormField label="Gender">
              <HimsSelect aria-label="Gender" value={form.gender} onChange={(e) => update({ gender: e.target.value as DonorForm["gender"] })}>
                <option value="">Select...</option>
                {Array.from(GENDERS).map((o) => (
                  <option key={o} value={o}>{o}</option>
                ))}
              </HimsSelect>
            </FormField>
            <FormField label="Blood Group" error={errors.bloodGroup}>
              <HimsSelect aria-label="Blood Group" value={form.bloodGroup} onChange={(e) => update({ bloodGroup: e.target.value })}>
                <option value="">Select...</option>
                {BLOOD_GROUPS.map((o) => (
                  <option key={o} value={o}>{o}</option>
                ))}
              </HimsSelect>
            </FormField>
            <FormField label="Phone">
              <TextInput aria-label="Phone" value={form.phone ?? ""} onChange={(e) => update({ phone: e.target.value })} />
            </FormField>
            <FormField label="Weight (kg)" error={errors.weight}>
              <TextInput aria-label="Weight (kg)" type="number" value={String(form.weight ?? "")} onChange={(e) => update({ weight: e.target.value as unknown as number })} />
            </FormField>
            <FormField label="Pulse" error={errors.pulse}>
              <TextInput aria-label="Pulse" type="number" value={String(form.pulse ?? "")} onChange={(e) => update({ pulse: e.target.value as unknown as number })} />
            </FormField>
            <FormField label="BP Systolic" error={errors.bpSystolic}>
              <TextInput aria-label="BP Systolic" type="number" value={String(form.bpSystolic ?? "")} onChange={(e) => update({ bpSystolic: e.target.value as unknown as number })} />
            </FormField>
            <FormField label="BP Diastolic" error={errors.bpDiastolic}>
              <TextInput aria-label="BP Diastolic" type="number" value={String(form.bpDiastolic ?? "")} onChange={(e) => update({ bpDiastolic: e.target.value as unknown as number })} />
            </FormField>
            <FormField label="Hemoglobin" error={errors.hemoglobin}>
              <TextInput aria-label="Hemoglobin" type="number" value={String(form.hemoglobin ?? "")} onChange={(e) => update({ hemoglobin: e.target.value as unknown as number })} />
            </FormField>
            <FormField label="Last Donation Date">
              <TextInput aria-label="Last Donation Date" type="date" value={form.lastDonationAt ?? ""} onChange={(e) => update({ lastDonationAt: e.target.value })} />
            </FormField>
          </SimpleGrid>
          <Button variant="primary" onClick={handleRegister} disabled={createDonor.isPending} loading={createDonor.isPending} loadingText="Saving...">
            Register
          </Button>
        </div>
      )}

      <div className="card">
        <h2>Registered Donors</h2>
        <Table.Root size="sm" variant="line">
          <Table.Header>
            <Table.Row><Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Name</Table.ColumnHeader><Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Age</Table.ColumnHeader><Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Gender</Table.ColumnHeader><Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Group</Table.ColumnHeader><Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Phone</Table.ColumnHeader><Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Last Donation</Table.ColumnHeader></Table.Row>
          </Table.Header>
          <Table.Body>
            {donors.map((d) => (
              <Table.Row key={d.id}>
                <Table.Cell>{d.name}</Table.Cell>
                <Table.Cell>{d.age}</Table.Cell>
                <Table.Cell>{d.gender}</Table.Cell>
                <Table.Cell>{d.bloodGroup}</Table.Cell>
                <Table.Cell>{d.phone}</Table.Cell>
                <Table.Cell>{d.lastDonationAt || "-"}</Table.Cell>
              </Table.Row>
            ))}
            {donors.length === 0 && <Table.Row><Table.Cell colSpan={6}>No donors yet.</Table.Cell></Table.Row>}
          </Table.Body>
        </Table.Root>
      </div>
    </section>
  );
}
