import { useState } from "react";
import { Button, SimpleGrid, Table } from "@chakra-ui/react";
import { FormField, HimsSelect, TextInput } from "../../components/ui";
import { useDonors, useCreateDonor } from "./queries";
import { donorSchema } from "./zodSchema";

const BLOOD_GROUPS = ["A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-"];

export function DonorRegistration() {
  const { data: donors = [], isLoading } = useDonors();
  const createDonor = useCreateDonor();
  const [showForm, setShowForm] = useState(false);
  const [errors, setErrors] = useState<Record<string, string>>({});

  const blank = { name: "", bloodGroup: "" };
  const [form, setForm] = useState(blank);

  const update = (patch: Partial<typeof blank>) => setForm((f) => ({ ...f, ...patch }));

  const handleRegister = () => {
    const result = donorSchema.safeParse(form);
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
          <p className="muted">Demographics and vitals are not saved by the backend yet.</p>
          <SimpleGrid columns={{ base: 1, md: 2 }} gap={4} className="form-grid">
            <FormField label="Full Name" error={errors.name}>
              <TextInput aria-label="Full Name" value={form.name} onChange={(e) => update({ name: e.target.value })} />
            </FormField>
            <FormField label="Blood Group" error={errors.bloodGroup}>
              <HimsSelect aria-label="Blood Group" value={form.bloodGroup} onChange={(e) => update({ bloodGroup: e.target.value })}>
                <option value="">Select...</option>
                {BLOOD_GROUPS.map((o) => (
                  <option key={o} value={o}>{o}</option>
                ))}
              </HimsSelect>
            </FormField>
            <FormField label="Age">
              <TextInput aria-label="Age" type="number" disabled value="" />
            </FormField>
            <FormField label="Gender">
              <HimsSelect aria-label="Gender" disabled value="">
                <option value="">Select...</option>
              </HimsSelect>
            </FormField>
            <FormField label="Phone">
              <TextInput aria-label="Phone" disabled value="" />
            </FormField>
            <FormField label="Weight (kg)">
              <TextInput aria-label="Weight (kg)" type="number" disabled value="" />
            </FormField>
            <FormField label="Pulse">
              <TextInput aria-label="Pulse" type="number" disabled value="" />
            </FormField>
            <FormField label="BP Systolic">
              <TextInput aria-label="BP Systolic" type="number" disabled value="" />
            </FormField>
            <FormField label="BP Diastolic">
              <TextInput aria-label="BP Diastolic" type="number" disabled value="" />
            </FormField>
            <FormField label="Hemoglobin">
              <TextInput aria-label="Hemoglobin" type="number" disabled value="" />
            </FormField>
            <FormField label="Last Donation Date">
              <TextInput aria-label="Last Donation Date" type="date" disabled value="" />
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
                <Table.Cell>{d.age ?? "-"}</Table.Cell>
                <Table.Cell>{d.gender ?? "-"}</Table.Cell>
                <Table.Cell>{d.bloodGroup}</Table.Cell>
                <Table.Cell>{d.phone ?? "-"}</Table.Cell>
                <Table.Cell>{d.lastDonationAt || d.registeredAt || "-"}</Table.Cell>
              </Table.Row>
            ))}
            {donors.length === 0 && <Table.Row><Table.Cell colSpan={6}>No donors yet.</Table.Cell></Table.Row>}
          </Table.Body>
        </Table.Root>
      </div>
    </section>
  );
}