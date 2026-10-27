import { Fragment, useState } from "react";
import { usePatientSubmissions, useEncounterSubmissions } from "./queries";
import { TextInput } from "../../components/ui";
import { Button, Table } from "@chakra-ui/react";

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
        <div className="loading-state" role="status"><span className="spinner" aria-hidden="true" /> Loading…</div>
      ) : submissions.length === 0 ? (
        <p className="muted">No submissions found.</p>
      ) : (
        <Table.Root size="sm" variant="line">
          <Table.Header><Table.Row><Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">ID</Table.ColumnHeader><Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Form</Table.ColumnHeader><Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Encounter</Table.ColumnHeader><Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Submitted</Table.ColumnHeader><Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase"></Table.ColumnHeader></Table.Row></Table.Header>
          <Table.Body>
            {submissions.map((s) => (
              <Fragment key={s.id}>
                <Table.Row>
                  <Table.Cell>{s.id}</Table.Cell>
                  <Table.Cell>{s.formId}</Table.Cell>
                  <Table.Cell>{s.encounterId ?? "—"}</Table.Cell>
                  <Table.Cell>{s.submittedAt?.slice(0, 19)}</Table.Cell>
                  <Table.Cell><Button variant="secondary" size="sm" onClick={() => setOpenId(openId === s.id ? null : s.id)}>View</Button></Table.Cell>
                </Table.Row>
                {openId === s.id && (
                  <Table.Row>
                    <Table.Cell colSpan={5}>
                      <pre className="code-block" style={{ maxHeight: 320, overflow: "auto", background: "#f6f6f6", padding: 12, borderRadius: 6 }}>
                        {JSON.stringify(JSON.parse(s.submissionJson ?? "{}"), null, 2)}
                      </pre>
                    </Table.Cell>
                  </Table.Row>
                )}
              </Fragment>
            ))}
          </Table.Body>
        </Table.Root>
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
        <TextInput type="number" placeholder="Patient ID" value={patientId} onChange={(e) => setPatientId(e.target.value)} />
        <Button variant="secondary"
          onClick={() => {
            setActivePatient(Number(patientId) || undefined);
            setActiveEncounter(undefined);
          }}
        >
          View Patient
        </Button>
        <TextInput type="number" placeholder="Encounter ID" value={encounterId} onChange={(e) => setEncounterId(e.target.value)} />
        <Button variant="secondary"
          onClick={() => {
            setActiveEncounter(Number(encounterId) || undefined);
            setActivePatient(undefined);
          }}
        >
          View Encounter
        </Button>
      </div>
      <SubmissionViewer patientId={activePatient} encounterId={activeEncounter} />
    </>
  );
}