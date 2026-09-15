import { useState } from "react";
import { FormRenderer } from "./FormRenderer";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { submissionFilterSchema } from "./zodSchema";
import { usePatientSubmissions, useEncounterSubmissions, useDeleteSubmission, formKeys, useQueryClient } from "./queries";
import type { SubmissionFilterValues } from "./zodSchema";

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
          <button
            type="button"
            className={`submission-list__mode-btn${mode === "patient" ? " submission-list__mode-btn--active" : ""}`}
            onClick={() => setMode("patient")}
          >
            By Patient
          </button>
          <button
            type="button"
            className={`submission-list__mode-btn${mode === "encounter" ? " submission-list__mode-btn--active" : ""}`}
            onClick={() => setMode("encounter")}
          >
            By Encounter
          </button>
        </div>

        <div className="submission-list__search">
          <input
            type="number"
            className="submission-list__input"
            placeholder={mode === "patient" ? "Patient ID" : "Encounter ID"}
            value={searchValue}
            onChange={(e) => setSearchValue(e.target.value)}
            onKeyDown={(e) => { if (e.key === "Enter") handleSearch(); }}
          />
          <button type="button" className="submission-list__search-btn" onClick={handleSearch}>
            Look up
          </button>
        </div>
      </div>

      {isLoading && <p className="submission-list__loading">Loading…</p>}

      {submittedId && !isLoading && submissions.length === 0 && (
        <p className="submission-list__empty">No submissions found for {mode} ID {submittedId}.</p>
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
              <tr key={s.id}>
                <td>{s.id}</td>
                <td><code>{s.formId}</code></td>
                <td className="submission-list__date">{s.submittedAt?.slice(0, 19)}</td>
                <td>
                  <span className={`submission-list__status${s.status === false ? " submission-list__status--deleted" : ""}`}>
                    {s.status === false ? "Deleted" : "Active"}
                  </span>
                </td>
                <td>
                  {s.status !== false && (
                    <button
                      className="submission-list__btn submission-list__btn--danger"
                      onClick={() => handleDelete(s.formId, s.id)}
                    >
                      Delete
                    </button>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  );
}
