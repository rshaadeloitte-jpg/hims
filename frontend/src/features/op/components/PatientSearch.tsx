import { useState } from "react";
import { useDebounce } from "../../../hooks/useDebounce";
import { usePatientSearch } from "../api/queries";
import type { PatientSearchHit } from "../api/types";

interface PatientSearchProps {
  /** Called when the user picks a hit (or wants to walk in a new one). */
  onPick: (hit: PatientSearchHit) => void;
  /** Called when the user explicitly chooses "register new" (no hit). */
  onRegisterNew?: () => void;
  /** placeholder override. */
  placeholder?: string;
  /** Auto-focus the input on mount (used when PatientSearch is the first widget). */
  autoFocus?: boolean;
  /** Free-text label override. */
  label?: string;
}

/**
 * PatientSearch — debounced multi-field lookup (name / MRN / mobile / ABHA).
 * The single search box matches legacy behavior: typing routes the query to
 * GET /api/v1/patients/search which fans out to name/MRN/mobile/ABHA matches
 * server-side.
 *
 * Selection vs "register new" is the central UX fork for the walk-in flow:
 * a hit opens the existing patient; a miss surfaces the "register new"
 * affordance which routes to the demographic form.
 */
export function PatientSearch({
  onPick,
  onRegisterNew,
  placeholder = "Search by name, MRN, mobile or ABHA",
  autoFocus = false,
  label = "Patient",
}: PatientSearchProps) {
  const [input, setInput] = useState<string>("");
  const debounced = useDebounce(input, 250);
  const query = debounced.trim().length >= 2 ? { q: debounced.trim(), limit: 10 } : {};
  const search = usePatientSearch(query, debounced.trim().length >= 2);

  const showResults = debounced.trim().length >= 2;
  const isEmpty = showResults && !search.isLoading && (search.data ?? []).length === 0;

  return (
    <div className="patient-search">
      <label className="field-label" htmlFor="patient-search-input">
        {label}
      </label>
      <input
        id="patient-search-input"
        type="search"
        className="form-input"
        value={input}
        placeholder={placeholder}
        autoFocus={autoFocus}
        onChange={(e) => setInput(e.target.value)}
        autoComplete="off"
        aria-controls="patient-search-results"
        aria-expanded={showResults}
        aria-busy={search.isFetching}
      />

      {showResults ? (
        <div className="patient-search__results" id="patient-search-results" role="listbox">
          {search.isLoading ? (
            <p className="patient-search__hint" role="status">
              Searching…
            </p>
          ) : null}

          {!search.isLoading && (search.data ?? []).length > 0 ? (
            <ul className="patient-search__hits">
              {(search.data ?? []).map((hit) => (
                <li key={hit.id}>
                  <button
                    type="button"
                    className="patient-search__hit"
                    onClick={() => onPick(hit)}
                  >
                    <span className="patient-search__mrn">{hit.mrn ?? `#${hit.id}`}</span>
                    <span className="patient-search__name">
                      {[hit.firstName, hit.lastName].filter(Boolean).join(" ") || "(no name)"}
                    </span>
                    <span className="patient-search__attrs">
                      {hit.sex} · {hit.age != null ? `${hit.age}y` : hit.dateOfBirth ?? "—"}
                      {hit.mobile ? ` · ${hit.mobile}` : ""}
                      {hit.abha ? ` · ABHA ${hit.abha}` : ""}
                    </span>
                  </button>
                </li>
              ))}
            </ul>
          ) : null}

          {isEmpty ? (
            <div className="patient-search__empty">
              <p>No patient matches <strong>{debounced}</strong>.</p>
              {onRegisterNew ? (
                <button
                  type="button"
                  className="btn btn-primary btn-sm"
                  onClick={onRegisterNew}
                >
                  + Register new patient
                </button>
              ) : null}
            </div>
          ) : null}

          {search.isError ? (
            <p className="field-error" role="alert">
              Search failed: {String(search.error)}
            </p>
          ) : null}
        </div>
      ) : null}
    </div>
  );
}
