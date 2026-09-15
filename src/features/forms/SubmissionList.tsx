import { useState } from "react";
import { usePatientSubmissions, useEncounterSubmissions, useDeleteSubmission } from "./queries";

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
          <button
            type="button"
            className={`submission-list__mode-btn${lookupMode === "patient" ? " submission-list__mode-btn--active" : ""}`}
            onClick={() => handleModeChange("patient")}
          >
            By Patient
          </button>
          <button
            type="button"
            className={`submission-list__mode-btn${lookupMode === "encounter" ? " submission-list__mode-btn--active" : ""}`}
            onClick={() => handleModeChange("encounter")}
          >
            By Encounter
          </button>
        </div>

        <div className="submission-list__search">
          <input
            type="number"
            className="submission-list__input"
            placeholder={lookupMode === "patient" ? "Patient ID" : "Encounter ID"}
            value={inputValue}
            onChange={(e) => setInputValue(e.target.value)}
            onKeyDown={(e) => { if (e.key === "Enter") handleLookup(); }}
          />
          <button type="button" className="submission-list__search-btn" onClick={handleLookup}>
            Look up
          </button>
        </div>
      </div>

      {/* Results */}
      <div className="submission-list__results">
        {!activeId && (
          <p className="submission-list__empty">Enter an ID to look up submissions across all forms.</p>
        )}

        {activeId && isLoading && (
          <p className="submission-list__loading">Loading…</p>
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
          <table className="submission-list__table">
            <thead>
              <tr>
                <th>ID</th>
                <th>Form</th>
                <th>Submitted</th>
                <th>Status</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
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
            </tbody>
          </table>
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
      <tr className={isExpanded ? "submission-list__row--expanded" : ""}>
        <td>{id}</td>
        <td>
          <span className="submission-list__form-id">{formId}</span>
          {patientId && <span className="submission-list__meta">Patient: {patientId}</span>}
          {encounterId && <span className="submission-list__meta">Encounter: {encounterId}</span>}
        </td>
        <td className="submission-list__date">{submittedAt?.slice(0, 19)}</td>
        <td>
          <span className={`submission-list__status${status === false ? " submission-list__status--deleted" : ""}`}>
            {status === false ? "Deleted" : "Active"}
          </span>
        </td>
        <td className="submission-list__actions">
          <button type="button" className="submission-list__btn" onClick={onToggle}>
            {isExpanded ? "Hide" : "View"}
          </button>
          {status !== false && <DeleteButton submissionId={id} formId={formId} />}
        </td>
      </tr>
      {isExpanded && (
        <tr className="submission-list__detail-row">
          <td colSpan={5}>
            <pre className="submission-list__json">
              {parseSubmission(submissionJson)}
            </pre>
          </td>
        </tr>
      )}
    </>
  );
}

function DeleteButton({ submissionId, formId }: { submissionId: number; formId: number }) {
  const deleteM = useDeleteSubmission(formId);
  return (
    <button
      type="button"
      className="submission-list__btn submission-list__btn--danger"
      onClick={() => { if (window.confirm("Delete this submission?")) deleteM.mutate(submissionId); }}
    >
      Delete
    </button>
  );
}
