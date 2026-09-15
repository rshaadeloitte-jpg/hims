import { Fragment, useState } from "react";
import { usePatientSubmissions, useEncounterSubmissions } from "./queries";

function SubmissionViewer({ patientId, encounterId }: { patientId?: number; encounterId?: number }) {
  const patientQ = usePatientSubmissions(patientId ?? 0);
  const encounterQ = useEncounterSubmissions(encounterId ?? 0);
  const submissions = patientId
    ? (patientQ.data ?? [])
    : encounterId
      ? (encounterQ.data ?? [])
      : [];
  const isLoading = patientId ? patientQ.isLoading : encounterQ.isLoading;
  const [openId, setOpenId] = useState<number | null>(null);

  return (
    <section className="page">
      <header className="page__header">
        <h1>Form Submissions{patientId ? ` — Patient ${patientId}` : encounterId ? ` — Encounter ${encounterId}` : ""}</h1>
      </header>

      {isLoading ? (
        <p>Loading…</p>
      ) : submissions.length === 0 ? (
        <p className="muted">No submissions found.</p>
      ) : (
        <table className="table">
          <thead><tr><th>ID</th><th>Form</th><th>Encounter</th><th>Submitted</th><th></th></tr></thead>
          <tbody>
            {submissions.map((s) => (
              <Fragment key={s.id}>
                <tr>
                  <td>{s.id}</td>
                  <td>{s.formId}</td>
                  <td>{s.encounterId ?? "—"}</td>
                  <td>{s.submittedAt?.slice(0, 19)}</td>
                  <td><button className="btn btn--small" onClick={() => setOpenId(openId === s.id ? null : s.id)}>View</button></td>
                </tr>
                {openId === s.id && (
                  <tr>
                    <td colSpan={5}>
                      <pre className="code-block" style={{ maxHeight: 320, overflow: "auto", background: "#f6f6f6", padding: 12, borderRadius: 6 }}>
                        {JSON.stringify(JSON.parse(s.submissionJson ?? "{}"), null, 2)}
                      </pre>
                    </td>
                  </tr>
                )}
              </Fragment>
            ))}
          </tbody>
        </table>
      )}
    </section>
  );
}

export function PatientSubmissionsPage() {
  const [patientId, setPatientId] = useState("");
  const [encounterId, setEncounterId] = useState("");
  const [activePatient, setActivePatient] = useState<number | undefined>(undefined);
  const [activeEncounter, setActiveEncounter] = useState<number | undefined>(undefined);

  return (
    <>
      <div className="row" style={{ padding: "1rem", gap: 8 }}>
        <input type="number" placeholder="Patient ID" value={patientId} onChange={(e) => setPatientId(e.target.value)} />
        <button
          className="btn"
          onClick={() => {
            setActivePatient(Number(patientId) || undefined);
            setActiveEncounter(undefined);
          }}
        >
          View Patient
        </button>
        <input type="number" placeholder="Encounter ID" value={encounterId} onChange={(e) => setEncounterId(e.target.value)} />
        <button
          className="btn"
          onClick={() => {
            setActiveEncounter(Number(encounterId) || undefined);
            setActivePatient(undefined);
          }}
        >
          View Encounter
        </button>
      </div>
      <SubmissionViewer patientId={activePatient} encounterId={activeEncounter} />
    </>
  );
}