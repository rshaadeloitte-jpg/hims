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
        <label>
          Specialty
          <select value={specialty} onChange={(e) => { setSpecialty(e.target.value); setSelectedId(null); }}>
            <option value="">All</option>
            {specialties.map((s) => <option key={s} value={s}>{s}</option>)}
          </select>
        </label>
      </div>

      <section className="card">
        {formsQ.isLoading ? (
          <p>Loading forms…</p>
        ) : forms.length === 0 ? (
          <p className="empty-state">No forms found{specialty ? ` for ${specialty}` : ""}.</p>
        ) : (
          <table className="table">
            <thead>
              <tr><th>Code</th><th>Name</th><th>Specialty</th><th>Ver</th><th>Action</th></tr>
            </thead>
            <tbody>
              {forms.map(f => (
                <tr key={f.id} className={selectedId === f.id ? "row-selected" : ""}>
                  <td><code>{f.code}</code></td>
                  <td>{f.name}</td>
                  <td>{f.specialty}</td>
                  <td>{f.version}</td>
                  <td><button className="btn btn--small" onClick={() => openForm(f.id)}>Open</button></td>
                </tr>
              ))}
            </tbody>
          </table>
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
                <label>
                  Patient ID (optional)
                  <input type="number" value={patientId} onChange={(e) => setPatientId(e.target.value)} style={{ marginLeft: 8, width: 120 }} />
                </label>
                <button className="btn btn--primary" disabled={submitM.isPending} onClick={handleSubmit}>
                  {submitM.isPending ? "Submitting…" : "Submit"}
                </button>
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
              {submissionsQ.isLoading ? <p>Loading…</p> : (
                <table className="table">
                  <thead><tr><th>ID</th><th>Patient</th><th>Encounter</th><th>Submitted</th><th>Status</th><th></th></tr></thead>
                  <tbody>
                    {(submissionsQ.data ?? []).map((s) => (
                      <tr key={s.id}>
                        <td>{s.id}</td>
                        <td>{s.patientId ?? "—"}</td>
                        <td>{s.encounterId ?? "—"}</td>
                        <td>{s.submittedAt?.slice(0, 19)}</td>
                        <td>{s.status === false ? "Deleted" : "Active"}</td>
                        <td>
                          {s.status !== false && (
                            <button className="btn btn--small btn--danger" onClick={() => deleteM.mutate(s.id)}>Delete</button>
                          )}
                        </td>
                      </tr>
                    ))}
                    {(submissionsQ.data ?? []).length === 0 && (
                      <tr><td colSpan={6} className="muted">No submissions yet.</td></tr>
                    )}
                  </tbody>
                </table>
              )}
            </>
          ) : null}
        </section>
      )}
    </div>
  );
}