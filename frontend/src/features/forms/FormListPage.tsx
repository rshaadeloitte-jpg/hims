import { useState } from "react";
import { ApiError } from "../../api/client";
import {
  useFormsBySpecialty,
  useFormSchema,
  useSubmitForm,
  useFormSpecialties,
  useFormSubmissions,
  useDeleteSubmission,
} from "./queries";
import { JsonSchemaForm } from "./components/JsonSchemaForm";
import { FormField, HimsSelect, TextInput } from "../../components/ui";
import { Button, Table } from "@chakra-ui/react";

/**
 * M40 Clinical Forms — dynamic JSON Schema renderer.
 * Lists forms (specialty filter), renders the versioned schema as typed inputs
 * (string/number/boolean/enum/array/nested objects, date formats), validates
 * required fields client-side, submits, and lists submissions with soft-delete.
 */
export function FormListPage() {
  const { data: specialties = [] } = useFormSpecialties();
  const [specialty, setSpecialty] = useState("");
  const formsQ = useFormsBySpecialty(specialty || undefined);
  const [selectedId, setSelectedId] = useState<number | null>(null);
  const schemaQ = useFormSchema(selectedId ?? 0);
  const submitM = useSubmitForm(selectedId ?? 0);
  const submissionsQ = useFormSubmissions(selectedId ?? 0);
  const deleteM = useDeleteSubmission(selectedId ?? 0);
  const [formData, setFormData] = useState<Record<string, unknown>>({});
  const [patientId, setPatientId] = useState("");

  const forms = formsQ.data ?? [];

  const openForm = (id: number) => {
    setSelectedId(id);
    setFormData({});
    setPatientId("");
    submitM.reset();
  };

  const requiredFields = (schemaQ.data?.schema?.required ?? []) as string[];

  const handleSubmit = () => {
    // Client-side required check against schema
    const missing = requiredFields.filter((f) => {
      const v = formData[f];
      return v === undefined || v === null || v === "";
    });
    if (missing.length > 0) {
      alert(`Missing required fields: ${missing.join(", ")}`);
      return;
    }
    const pid = patientId.trim() === "" ? undefined : Number(patientId);
    submitM.mutate({ data: formData, patientId: pid });
  };

  return (
    <div className="page">
      <header className="page__header">
        <h1>Clinical Forms (M40)</h1>
        <p className="muted">Schema-driven forms engine — rendered from versioned JSON Schema</p>
      </header>

      {formsQ.isError ? (
        <div className="alert alert--error" role="alert">
          <strong>{formsQ.error instanceof ApiError ? formsQ.error.code : "Error"}</strong>: {String((formsQ.error as Error).message)}
        </div>
      ) : null}

      <div className="row" style={{ marginBottom: 12 }}>
        <FormField label="Specialty"><HimsSelect value={specialty} onChange={(e) => { setSpecialty(e.target.value); setSelectedId(null); }}>
            <option value="">All</option>
            {specialties.map((s) => <option key={s} value={s}>{s}</option>)}
          </HimsSelect></FormField>
      </div>

      <section className="card">
        {formsQ.isLoading ? (
          <p>Loading forms…</p>
        ) : forms.length === 0 ? (
          <p className="empty-state">No forms found{specialty ? ` for ${specialty}` : ""}.</p>
        ) : (
          <Table.Root size="sm" variant="line">
            <Table.Header>
              <Table.Row><Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Code</Table.ColumnHeader><Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Name</Table.ColumnHeader><Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Specialty</Table.ColumnHeader><Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Ver</Table.ColumnHeader><Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Action</Table.ColumnHeader></Table.Row>
            </Table.Header>
            <Table.Body>
              {forms.map(f => (
                <Table.Row key={f.id}>
                  <Table.Cell><code>{f.code}</code></Table.Cell>
                  <Table.Cell>{f.name}</Table.Cell>
                  <Table.Cell>{f.specialty}</Table.Cell>
                  <Table.Cell>{f.version}</Table.Cell>
                  <Table.Cell><Button variant="secondary" size="sm" onClick={() => openForm(f.id)}>Open</Button></Table.Cell>
                </Table.Row>
              ))}
            </Table.Body>
          </Table.Root>
        )}
      </section>

      {selectedId != null && (
        <section className="card" style={{ marginTop: 16 }}>
          <h2>
            {schemaQ.data?.name ?? `Form #${selectedId}`}
            <span className="badge">{schemaQ.data?.specialty}</span>
            <span className="badge">v{schemaQ.data?.version}</span>
          </h2>

          {schemaQ.isLoading ? (
            <p>Loading schema…</p>
          ) : schemaQ.isError ? (
            <p className="field-error">Schema load failed: {String((schemaQ.error as Error).message)}</p>
          ) : schemaQ.data?.schema ? (
            <>
              <JsonSchemaForm
                schema={schemaQ.data.schema}
                data={formData}
                onChange={setFormData}
              />

              <div className="row" style={{ marginTop: 16 }}>
                <FormField label="Patient ID (optional)">
                  <TextInput
                    type="number"
                    value={patientId}
                    onChange={(e) => setPatientId(e.target.value)}
                    style={{ marginLeft: 8, width: 120 }}
                  />
                </FormField>
                <Button variant="primary" loading={submitM.isPending} loadingText="Submitting…" disabled={submitM.isPending} onClick={handleSubmit}>
                  Submit
                </Button>
              </div>
              {submitM.isSuccess ? (
                <p className="notice" role="status">
                  Submitted ✓ id={(submitM.data as unknown as { id: number }).id}
                </p>
              ) : null}
              {submitM.isError ? (
                <p className="field-error" role="alert">{String((submitM.error as Error).message)}</p>
              ) : null}

              <h3 style={{ marginTop: 24 }}>Submissions</h3>
              {submissionsQ.isLoading ? <div className="loading-state" role="status"><span className="spinner" aria-hidden="true" /> Loading…</div> : (
                <Table.Root size="sm" variant="line">
                  <Table.Header><Table.Row><Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">ID</Table.ColumnHeader><Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Patient</Table.ColumnHeader><Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Encounter</Table.ColumnHeader><Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Submitted</Table.ColumnHeader><Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Status</Table.ColumnHeader><Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase"></Table.ColumnHeader></Table.Row></Table.Header>
                  <Table.Body>
                    {(submissionsQ.data ?? []).map((s) => (
                      <Table.Row key={s.id}>
                        <Table.Cell>{s.id}</Table.Cell>
                        <Table.Cell>{s.patientId ?? "—"}</Table.Cell>
                        <Table.Cell>{s.encounterId ?? "—"}</Table.Cell>
                        <Table.Cell>{s.submittedAt?.slice(0, 19)}</Table.Cell>
                        <Table.Cell>{s.status === false ? "Deleted" : "Active"}</Table.Cell>
                        <Table.Cell>
                          {s.status !== false && (
                            <Button variant="ghost" size="sm" onClick={() => deleteM.mutate(s.id)}>Delete</Button>
                          )}
                        </Table.Cell>
                      </Table.Row>
                    ))}
                    {(submissionsQ.data ?? []).length === 0 && (
                      <Table.Row><Table.Cell colSpan={6}>No submissions yet.</Table.Cell></Table.Row>
                    )}
                  </Table.Body>
                </Table.Root>
              )}
            </>
          ) : null}
        </section>
      )}
    </div>
  );
}