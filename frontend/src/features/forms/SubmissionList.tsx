import { useState } from "react";
import { usePatientSubmissions, useEncounterSubmissions, useDeleteSubmission } from "./queries";
import { TextInput } from "../../components/ui";
import { Button, Table } from "@chakra-ui/react";

interface SubmissionListProps {
  /** Optional initial patient ID to load on mount. */
  initialPatientId?: number;
}

type LookupMode = "patient" | "encounter";

/**
 * SubmissionList — cross-form patient lookup.
 * Look up all form submissions by patient or encounter, expand to view JSON,
 * and soft-delete individual records.
 */
export function SubmissionList({ initialPatientId }: SubmissionListProps) {
  const [lookupMode, setLookupMode] = useState<LookupMode>("patient");
  const [inputValue, setInputValue] = useState(initialPatientId ? String(initialPatientId) : "");
  const [activeId, setActiveId] = useState<number | undefined>(initialPatientId);
  const [expandedId, setExpandedId] = useState<number | null>(null);

  const patientQ = usePatientSubmissions(activeId ?? 0);
  const encounterQ = useEncounterSubmissions(activeId ?? 0);

  const submissions = lookupMode === "patient" ? (patientQ.data ?? []) : (encounterQ.data ?? []);
  const isLoading = lookupMode === "patient" ? patientQ.isLoading : encounterQ.isLoading;
  const isError = lookupMode === "patient" ? patientQ.isError : encounterQ.isError;
  const error = lookupMode === "patient" ? patientQ.error : encounterQ.error;

  const handleLookup = () => {
    const id = Number(inputValue);
    if (Number.isFinite(id) && id > 0) {
      setActiveId(id);
      setExpandedId(null);
    }
  };

  const handleModeChange = (mode: LookupMode) => {
    setLookupMode(mode);
    setActiveId(undefined);
    setExpandedId(null);
  };

  const parseSubmission = (json: string): string => {
    try {
      return JSON.stringify(JSON.parse(json), null, 2);
    } catch {
      return json;
    }
  };

  return (
    <div className="submission-list">
      <header className="submission-list__header">
        <h2 className="submission-list__title">Form Submissions</h2>
        <p className="submission-list__subtitle">Cross-form lookup by patient or encounter</p>
      </header>

      {/* Lookup controls */}
      <div className="submission-list__controls">
        <div className="submission-list__mode-toggle" role="group" aria-label="Lookup mode">
          <Button
            variant={lookupMode === "patient" ? "solid" : "outline"}
            size="sm"
            type="button"
            onClick={() => handleModeChange("patient")}
          >
            By Patient
          </Button>
          <Button
            variant={lookupMode === "encounter" ? "solid" : "outline"}
            size="sm"
            type="button"
            onClick={() => handleModeChange("encounter")}
          >
            By Encounter
          </Button>
        </div>

        <div className="submission-list__search">
          <TextInput
            type="number"
            placeholder={lookupMode === "patient" ? "Patient ID" : "Encounter ID"}
            value={inputValue}
            onChange={(e) => setInputValue(e.target.value)}
            onKeyDown={(e) => { if (e.key === "Enter") handleLookup(); }}
          />
          <Button variant="secondary" type="button" onClick={handleLookup}>
            Look up
          </Button>
        </div>
      </div>

      {/* Results */}
      <div className="submission-list__results">
        {!activeId && (
          <p className="submission-list__empty">Enter an ID to look up submissions across all forms.</p>
        )}

        {activeId && isLoading && (
          <p className="submission-list__loading" role="status">Loading…</p>
        )}

        {activeId && isError && (
          <div className="submission-list__alert" role="alert">
            {String((error as Error).message)}
          </div>
        )}

        {activeId && !isLoading && !isError && submissions.length === 0 && (
          <p className="submission-list__empty">
            No submissions found for {lookupMode} ID {activeId}.
          </p>
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
                <SubmissionRow
                  key={s.id}
                  id={s.id}
                  formId={s.formId}
                  patientId={s.patientId}
                  encounterId={s.encounterId}
                  submittedAt={s.submittedAt}
                  status={s.status}
                  submissionJson={s.submissionJson}
                  isExpanded={expandedId === s.id}
                  onToggle={() => setExpandedId(expandedId === s.id ? null : s.id)}
                  parseSubmission={parseSubmission}
                />
              ))}
            </Table.Body>
          </Table.Root>
        )}
      </div>
    </div>
  );
}

interface SubmissionRowProps {
  id: number;
  formId: number;
  patientId?: number;
  encounterId?: number;
  submittedAt: string;
  status?: boolean;
  submissionJson: string;
  isExpanded: boolean;
  onToggle: () => void;
  parseSubmission: (json: string) => string;
}

function SubmissionRow({ id, formId, patientId, encounterId, submittedAt, status, submissionJson, isExpanded, onToggle, parseSubmission }: SubmissionRowProps) {
  return (
    <>
      <Table.Row>
        <Table.Cell>{id}</Table.Cell>
        <Table.Cell>
          <span className="submission-list__form-id">{formId}</span>
          {patientId && <span className="submission-list__meta">Patient: {patientId}</span>}
          {encounterId && <span className="submission-list__meta">Encounter: {encounterId}</span>}
        </Table.Cell>
        <Table.Cell className="submission-list__date">{submittedAt?.slice(0, 19)}</Table.Cell>
        <Table.Cell>
          <span className={`submission-list__status${status === false ? " submission-list__status--deleted" : ""}`}>
            {status === false ? "Deleted" : "Active"}
          </span>
        </Table.Cell>
        <Table.Cell className="submission-list__actions">
          <Button variant="secondary" type="button" onClick={onToggle}>
            {isExpanded ? "Hide" : "View"}
          </Button>
          {status !== false && <DeleteButton submissionId={id} formId={formId} />}
        </Table.Cell>
      </Table.Row>
      {isExpanded && (
        <Table.Row className="submission-list__detail-row">
          <Table.Cell colSpan={5}>
            <pre className="submission-list__json">
              {parseSubmission(submissionJson)}
            </pre>
          </Table.Cell>
        </Table.Row>
      )}
    </>
  );
}

function DeleteButton({ submissionId, formId }: { submissionId: number; formId: number }) {
  const deleteM = useDeleteSubmission(formId);
  return (
    <Button variant="ghost" size="sm"
      type="button"
      onClick={() => { if (window.confirm("Delete this submission?")) deleteM.mutate(submissionId); }}
    >
      Delete
    </Button>
  );
}
