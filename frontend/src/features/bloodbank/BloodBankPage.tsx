import { useState } from "react";
import { Button, SimpleGrid, Table } from "@chakra-ui/react";
import { FormField, HimsSelect, TextInput } from "../../components/ui";
import { useBloodBags, useBloodBankStock, useCreateBloodBag, useUpdateBloodBagStatus } from "./queries";
import { bloodBagSchema } from "./zodSchema";

const COMPONENTS = ["WHOLE_BLOOD", "PLASMA", "PLATELETS", "RED_CELLS", "CRYOPRECIPITATE"];
const STATUSES = ["AVAILABLE", "RESERVED", "ISSUED", "EXPIRED", "DISCARDED"];

export function BloodBankPage() {
  const { data: bags = [], isLoading } = useBloodBags();
  const { data: stock } = useBloodBankStock();
  const createBag = useCreateBloodBag();
  const updateStatus = useUpdateBloodBagStatus();
  const [showForm, setShowForm] = useState(false);
  const [form, setForm] = useState({ bloodGroup: "", component: "", donorId: "", volume: "" });
  const [errors, setErrors] = useState<Record<string, string>>({});

  const handleSubmit = () => {
    const result = bloodBagSchema.safeParse({ ...form, donorId: Number(form.donorId), volume: Number(form.volume) });
    if (!result.success) {
      const fieldErrors: Record<string, string> = {};
      result.error.errors.forEach((e) => { fieldErrors[e.path[0]] = e.message; });
      setErrors(fieldErrors);
      return;
    }
    createBag.mutate(result.data, { onSuccess: () => { setShowForm(false); setForm({ bloodGroup: "", component: "", donorId: "", volume: "" }); setErrors({}); } });
  };

  if (isLoading) return <p className="muted">Loading blood bank…</p>;

  return (
    <section className="page">
      <header className="page-header">
        <h1>Blood Bank</h1>
        <Button variant={showForm ? "secondary" : "primary"} onClick={() => setShowForm(!showForm)}>
          {showForm ? "Cancel" : "Register Bag"}
        </Button>
      </header>

      {stock && (
        <div className="card">
          <h2>Stock Summary</h2>
          <div className="stock-grid">
            {Object.entries(stock).map(([group, count]) => (
              <div key={group} className="stock-item"><span>{group}</span><strong>{count}</strong></div>
            ))}
          </div>
        </div>
      )}

      {showForm && (
        <div className="card">
          <h2>New Blood Bag</h2>
          <SimpleGrid columns={{ base: 1, md: 2 }} gap={4} className="form-grid">
            <FormField label="Blood Group" error={errors.bloodGroup}>
              <HimsSelect aria-label="Blood Group" value={form.bloodGroup} onChange={(e) => setForm({ ...form, bloodGroup: e.target.value })}>
                <option value="">Select…</option>
                {["A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-"].map((g) => <option key={g} value={g}>{g}</option>)}
              </HimsSelect>
            </FormField>
            <FormField label="Component" error={errors.component}>
              <HimsSelect aria-label="Component" value={form.component} onChange={(e) => setForm({ ...form, component: e.target.value })}>
                <option value="">Select…</option>
                {COMPONENTS.map((c) => <option key={c} value={c}>{c}</option>)}
              </HimsSelect>
            </FormField>
            <FormField label="Donor ID" error={errors.donorId}>
              <TextInput aria-label="Donor ID" value={form.donorId} onChange={(e) => setForm({ ...form, donorId: e.target.value })} />
            </FormField>
            <FormField label="Volume (ml)" error={errors.volume}>
              <TextInput aria-label="Volume (ml)" type="number" value={form.volume} onChange={(e) => setForm({ ...form, volume: e.target.value })} />
            </FormField>
          </SimpleGrid>
          <Button variant="primary" onClick={handleSubmit} disabled={createBag.isPending} loading={createBag.isPending} loadingText="Saving…">
            Save Bag
          </Button>
        </div>
      )}

      <div className="card">
        <Table.Root size="sm" variant="line">
          <Table.Header>
            <Table.Row><Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Bag No</Table.ColumnHeader><Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Group</Table.ColumnHeader><Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Component</Table.ColumnHeader><Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Collected</Table.ColumnHeader><Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Expiry</Table.ColumnHeader><Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Status</Table.ColumnHeader><Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Actions</Table.ColumnHeader></Table.Row>
          </Table.Header>
          <Table.Body>
            {bags.map((b) => (
              <Table.Row key={b.id}>
                <Table.Cell>{b.bagNo}</Table.Cell>
                <Table.Cell>{b.bloodGroup}</Table.Cell>
                <Table.Cell>{b.component}</Table.Cell>
                <Table.Cell>{b.collectionDate}</Table.Cell>
                <Table.Cell>{b.expiryDate}</Table.Cell>
                <Table.Cell>{b.status}</Table.Cell>
                <Table.Cell>
                  <HimsSelect aria-label={`Status for bag ${b.bagNo}`} value={b.status} onChange={(e) => updateStatus.mutate({ id: b.id, status: e.target.value })}>
                    {STATUSES.map((s) => <option key={s} value={s}>{s}</option>)}
                  </HimsSelect>
                </Table.Cell>
              </Table.Row>
            ))}
            {bags.length === 0 && <Table.Row><Table.Cell colSpan={7}>No bags registered.</Table.Cell></Table.Row>}
          </Table.Body>
        </Table.Root>
      </div>
    </section>
  );
}
