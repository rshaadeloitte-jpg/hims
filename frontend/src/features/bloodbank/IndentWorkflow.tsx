import { useState } from "react";
import { Button, SimpleGrid, Table } from "@chakra-ui/react";
import { FormField, HimsSelect, TextInput } from "../../components/ui";
import { useIndents, useCreateIndent, useUpdateIndentStatus } from "./queries";

const STATUSES = ["REQUESTED", "APPROVED", "DISPATCHED", "RECEIVED", "REJECTED"] as const;
const BLOOD_GROUPS = ["A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-"];
const COMPONENTS = ["WHOLE_BLOOD", "PLASMA", "PLATELETS", "RED_CELLS", "CRYOPRECIPITATE"];
const STORES = ["BB-MAIN", "BB-OPD", "BB-OT", "BB-ICU", "BB-PHARMACY"];

type IndentFormState = {
  sourceStore: string;
  targetStore: string;
  bloodGroup: string;
  component: string;
  units: string;
  note: string;
};

export function IndentWorkflow() {
  const { data: indents = [], isLoading } = useIndents();
  const createIndent = useCreateIndent();
  const updateStatus = useUpdateIndentStatus();
  const [showForm, setShowForm] = useState(false);
  const [errors, setErrors] = useState<Record<string, string>>({});

  const blank: IndentFormState = { sourceStore: "", targetStore: "", bloodGroup: "", component: "", units: "", note: "" };
  const [form, setForm] = useState<IndentFormState>(blank);

  const update = (patch: Partial<IndentFormState>) => setForm((f) => ({ ...f, ...patch }));

  const handleSubmit = () => {
    const payload = {
      sourceStore: form.sourceStore,
      targetStore: form.targetStore,
      bloodGroup: form.bloodGroup,
      component: form.component,
      units: Number(form.units),
      note: form.note || undefined,
    };
    if (!payload.sourceStore || !payload.targetStore || !payload.bloodGroup || !payload.component) {
      setErrors({ sourceStore: payload.sourceStore ? "" : "Required", targetStore: payload.targetStore ? "" : "Required", bloodGroup: payload.bloodGroup ? "" : "Required", component: payload.component ? "" : "Required" });
      return;
    }
    if (Number.isNaN(payload.units) || payload.units <= 0) {
      setErrors({ units: "Valid units required" });
      return;
    }
    createIndent.mutate(payload, {
      onSuccess: () => {
        setShowForm(false);
        setForm(blank);
        setErrors({});
      },
    });
  };

  if (isLoading) return <p className="muted">Loading indents...</p>;

  return (
    <section className="page">
      <header className="page-header">
        <h1>Indent Workflow</h1>
        <Button variant={showForm ? "secondary" : "primary"} onClick={() => setShowForm(!showForm)}>
          {showForm ? "Cancel" : "Request Blood"}
        </Button>
      </header>

      {showForm && (
        <div className="card">
          <h2>New Indent Request</h2>
          <SimpleGrid columns={{ base: 1, md: 2 }} gap={4} className="form-grid">
            <FormField label="Source Store" error={errors.sourceStore || undefined}>
              <HimsSelect aria-label="Source Store" value={form.sourceStore} onChange={(e) => update({ sourceStore: e.target.value })}>
                <option value="">Select...</option>
                {STORES.map((o) => (
                  <option key={o} value={o}>{o}</option>
                ))}
              </HimsSelect>
            </FormField>
            <FormField label="Target Store" error={errors.targetStore || undefined}>
              <HimsSelect aria-label="Target Store" value={form.targetStore} onChange={(e) => update({ targetStore: e.target.value })}>
                <option value="">Select...</option>
                {STORES.map((o) => (
                  <option key={o} value={o}>{o}</option>
                ))}
              </HimsSelect>
            </FormField>
            <FormField label="Blood Group" error={errors.bloodGroup || undefined}>
              <HimsSelect aria-label="Blood Group" value={form.bloodGroup} onChange={(e) => update({ bloodGroup: e.target.value })}>
                <option value="">Select...</option>
                {BLOOD_GROUPS.map((o) => (
                  <option key={o} value={o}>{o}</option>
                ))}
              </HimsSelect>
            </FormField>
            <FormField label="Component" error={errors.component || undefined}>
              <HimsSelect aria-label="Component" value={form.component} onChange={(e) => update({ component: e.target.value })}>
                <option value="">Select...</option>
                {COMPONENTS.map((o) => (
                  <option key={o} value={o}>{o}</option>
                ))}
              </HimsSelect>
            </FormField>
            <FormField label="Units" error={errors.units}>
              <TextInput aria-label="Units" type="number" value={form.units} onChange={(e) => update({ units: e.target.value })} />
            </FormField>
            <FormField label="Note">
              <TextInput aria-label="Note" value={form.note} onChange={(e) => update({ note: e.target.value })} />
            </FormField>
          </SimpleGrid>
          <Button variant="primary" onClick={handleSubmit} disabled={createIndent.isPending} loading={createIndent.isPending} loadingText="Submitting...">
            Submit Request
          </Button>
        </div>
      )}

      <div className="card">
        <h2>Inter-Store Requests</h2>
        <Table.Root size="sm" variant="line">
          <Table.Header>
            <Table.Row><Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Source</Table.ColumnHeader><Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Target</Table.ColumnHeader><Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Group</Table.ColumnHeader><Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Component</Table.ColumnHeader><Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Units</Table.ColumnHeader><Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Status</Table.ColumnHeader><Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Requested</Table.ColumnHeader><Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Actions</Table.ColumnHeader></Table.Row>
          </Table.Header>
          <Table.Body>
            {indents.map((ind) => (
              <Table.Row key={ind.id}>
                <Table.Cell>{ind.sourceStore}</Table.Cell>
                <Table.Cell>{ind.targetStore}</Table.Cell>
                <Table.Cell>{ind.bloodGroup}</Table.Cell>
                <Table.Cell>{ind.component}</Table.Cell>
                <Table.Cell>{ind.units}</Table.Cell>
                <Table.Cell>{ind.status}</Table.Cell>
                <Table.Cell>{new Date(ind.requestedAt).toLocaleString()}</Table.Cell>
                <Table.Cell>
                  <HimsSelect
                    aria-label={`Status for indent ${ind.id}`}
                    value={ind.status}
                    onChange={(e) => updateStatus.mutate({ id: ind.id, status: e.target.value as typeof ind.status })}
                  >
                    {STATUSES.map((s) => (
                      <option key={s} value={s}>{s}</option>
                    ))}
                  </HimsSelect>
                </Table.Cell>
              </Table.Row>
            ))}
            {indents.length === 0 && <Table.Row><Table.Cell colSpan={8}>No indent requests.</Table.Cell></Table.Row>}
          </Table.Body>
        </Table.Root>
      </div>
    </section>
  );
}
