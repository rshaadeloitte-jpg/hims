import { useState } from "react";
import { usePatientSubmissions, useEncounterSubmissions, useDeleteSubmission, formKeys, useQueryClient } from "./queries";
import { TextInput } from "../../components/ui";
import { Button, Table } from "@chakra-ui/react";

/**
 * CrossFormSubmissionsPage — unified submission lookup across all forms.
 * Search by patient ID or encounter ID to see all related submissions.
 */
export function CrossFormSubmissionsPage() {
  const [mode, setMode] = useState<"patient" | "encounter">("patient");
  const [searchValue, setSearchValue] = useState("");
  const [submittedId, setSubmittedId] = useState<number | undefined>(undefined);

  const queryClient = useQueryClient();

  const patientQ = usePatientSubmissions(submittedId ?? 0);
  const encounterQ = useEncounterSubmissions(submittedId ?? 0);

  const submissions = mode === "patient" ? (patientQ.data ?? []) : (encounterQ.data ?? []);
  const isLoading = mode === "patient" ? patientQ.isLoading : encounterQ.isLoading;

  const handleSearch = () => {
    const id = Number(searchValue);
    if (Number.isFinite(id) && id > 0) {
      setSubmittedId(id);
    }
  };

  const handleDelete = (formId: number, submissionId: number) => {
    if (window.confirm("Delete this submission?")) {
      queryClient.setQueryData(formKeys.submissions(formId), (old: unknown) => {
        if (!old) return old;
        return (old as Array<{ id: number; status?: boolean }>).map((s) =>
          s.id === submissionId ? { ...s, status: false } : s,
        );
      });
      useDeleteSubmission(formId).mutate(submissionId);
    }
  };

  return (
    <div className="page">
      <header className="page-header">
        <h1>Cross-Form Submissions</h1>
        <p className="muted">Search by patient or encounter ID</p>
      </header>

      <div className="submission-list__controls">
        <div className="submission-list__mode-toggle" role="group" aria-label="Search mode">
          <Button
            variant={mode === "patient" ? "solid" : "outline"}
            size="sm"
            type="button"
            onClick={() => setMode("patient")}
          >
            By Patient
          </Button>
          <Button
            variant={mode === "encounter" ? "solid" : "outline"}
            size="sm"
            type="button"
            onClick={() => setMode("encounter")}
          >
            By Encounter
          </Button>
        </div>

        <div className="submission-list__search">
          <TextInput
            type="number"
            placeholder={mode === "patient" ? "Patient ID" : "Encounter ID"}
            value={searchValue}
            onChange={(e) => setSearchValue(e.target.value)}
            onKeyDown={(e) => { if (e.key === "Enter") handleSearch(); }}
          />
          <Button variant="secondary" type="button" onClick={handleSearch}>
            Look up
          </Button>
        </div>
      </div>

      {isLoading && <p className="submission-list__loading" role="status">Loading…</p>}

      {submittedId && !isLoading && submissions.length === 0 && (
        <p className="submission-list__empty">No submissions found for {mode} ID {submittedId}.</p>
      )}

      {submissions.length > 0 && (
        <Table.Root size="sm" variant="line" className="submission-list__table">
          <Table.Header>
            <Table.Row>
              <Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">ID</Table.ColumnHeader>
              <Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Form</Table.ColumnHeader>
              <Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Submitted</Table.ColumnHeader>
              <Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Status</Table.ColumnHeader>
              <Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase"></Table.ColumnHeader>
            </Table.Row>
          </Table.Header>
          <Table.Body>
            {submissions.map((s) => (
              <Table.Row key={s.id}>
                <Table.Cell>{s.id}</Table.Cell>
                <Table.Cell><code>{s.formId}</code></Table.Cell>
                <Table.Cell className="submission-list__date">{s.submittedAt?.slice(0, 19)}</Table.Cell>
                <Table.Cell>
                  <span className={`submission-list__status${s.status === false ? " submission-list__status--deleted" : ""}`}>
                    {s.status === false ? "Deleted" : "Active"}
                  </span>
                </Table.Cell>
                <Table.Cell>
                  {s.status !== false && (
                    <Button variant="ghost" size="sm"
                      onClick={() => handleDelete(s.formId, s.id)}
                    >
                      Delete
                    </Button>
                  )}
                </Table.Cell>
              </Table.Row>
            ))}
          </Table.Body>
        </Table.Root>
      )}
    </div>
  );
}
