import { useState } from "react";
import { Button, SimpleGrid, Table } from "@chakra-ui/react";
import { FormField, HimsSelect, TextInput } from "../../components/ui";
import { useCrossMatches, useCreateCrossMatch, useUpdateCrossMatchCompatibility } from "./queries";

const METHODS = ["MAJOR", "MINOR", "BOTH"] as const;
const STATUSES = ["COMPATIBLE", "INCOMPATIBLE", "PENDING"] as const;

type CrossMatchFormState = {
  bagId: string;
  patientId: string;
  method: "MAJOR" | "MINOR" | "BOTH";
  note: string;
};

export function CrossMatch() {
  const { data: matches = [], isLoading } = useCrossMatches();
  const createMatch = useCreateCrossMatch();
  const updateCompatibility = useUpdateCrossMatchCompatibility();
  const [showForm, setShowForm] = useState(false);
  const [errors, setErrors] = useState<Record<string, string>>({});

  const blank: CrossMatchFormState = { bagId: "", patientId: "", method: "MAJOR", note: "" };
  const [form, setForm] = useState<CrossMatchFormState>(blank);

  const update = (patch: Partial<CrossMatchFormState>) => setForm((f) => ({ ...f, ...patch }));

  const handleSubmit = () => {
    const payload = {
      bagId: Number(form.bagId),
      patientId: Number(form.patientId),
      method: form.method,
      note: form.note || undefined,
    };
    if (Number.isNaN(payload.bagId) || payload.bagId <= 0) {
      setErrors({ bagId: "Valid bag ID required" });
      return;
    }
    if (Number.isNaN(payload.patientId) || payload.patientId <= 0) {
      setErrors({ patientId: "Valid patient ID required" });
      return;
    }
    createMatch.mutate(payload, {
      onSuccess: () => {
        setShowForm(false);
        setForm(blank);
        setErrors({});
      },
    });
  };

  if (isLoading) return <p className="muted">Loading cross matches...</p>;

  return (
    <section className="page">
      <header className="page-header">
        <h1>Cross Match</h1>
        <Button variant={showForm ? "secondary" : "primary"} onClick={() => setShowForm(!showForm)}>
          {showForm ? "Cancel" : "New Cross Match"}
        </Button>
      </header>

      {showForm && (
        <div className="card">
          <h2>New Cross Match</h2>
          <SimpleGrid columns={{ base: 1, md: 2 }} gap={4} className="form-grid">
            <FormField label="Bag ID" error={errors.bagId}>
              <TextInput aria-label="Bag ID" type="number" value={form.bagId} onChange={(e) => update({ bagId: e.target.value })} />
            </FormField>
            <FormField label="Patient ID" error={errors.patientId}>
              <TextInput aria-label="Patient ID" type="number" value={form.patientId} onChange={(e) => update({ patientId: e.target.value })} />
            </FormField>
            <FormField label="Method">
              <HimsSelect aria-label="Method" value={form.method} onChange={(e) => update({ method: e.target.value as CrossMatchFormState["method"] })}>
                <option value="">Select...</option>
                {Array.from(METHODS).map((o) => (
                  <option key={o} value={o}>{o}</option>
                ))}
              </HimsSelect>
            </FormField>
            <FormField label="Note">
              <TextInput aria-label="Note" value={form.note} onChange={(e) => update({ note: e.target.value })} />
            </FormField>
          </SimpleGrid>
          <Button variant="primary" onClick={handleSubmit} disabled={createMatch.isPending} loading={createMatch.isPending} loadingText="Saving...">
            Submit
          </Button>
        </div>
      )}

      <div className="card">
        <h2>Cross Match Requests</h2>
        <Table.Root size="sm" variant="line">
          <Table.Header>
            <Table.Row><Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Bag</Table.ColumnHeader><Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Patient</Table.ColumnHeader><Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Method</Table.ColumnHeader><Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Compatibility</Table.ColumnHeader><Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Requested At</Table.ColumnHeader><Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Actions</Table.ColumnHeader></Table.Row>
          </Table.Header>
          <Table.Body>
            {matches.map((m) => (
              <Table.Row key={m.id}>
                <Table.Cell>{m.bagId}</Table.Cell>
                <Table.Cell>{m.patientId}</Table.Cell>
                <Table.Cell>{m.method}</Table.Cell>
                <Table.Cell>{m.compatibility}</Table.Cell>
                <Table.Cell>{new Date(m.requestedAt).toLocaleString()}</Table.Cell>
                <Table.Cell>
                  <HimsSelect
                    aria-label={`Compatibility for match ${m.id}`}
                    value={m.compatibility}
                    onChange={(e) => updateCompatibility.mutate({ id: m.id, compatibility: e.target.value as typeof m.compatibility })}
                  >
                    {Array.from(STATUSES).map((s) => (
                      <option key={s} value={s}>{s}</option>
                    ))}
                  </HimsSelect>
                </Table.Cell>
              </Table.Row>
            ))}
            {matches.length === 0 && <Table.Row><Table.Cell colSpan={6}>No cross matches.</Table.Cell></Table.Row>}
          </Table.Body>
        </Table.Root>
      </div>
    </section>
  );
}
