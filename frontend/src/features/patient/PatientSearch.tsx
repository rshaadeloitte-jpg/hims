import { useState, useCallback, useRef, useEffect } from "react";
import { Button } from "@chakra-ui/react";
import { TextInput } from "../../components/ui";
import { usePatientSearch, type PatientSearchHit } from "./queries";

/**
 * PatientSearch — Linear-style keyboard-navigable search with debounce.
 * Arrow keys navigate results, Enter selects, Escape clears.
 */
export function PatientSearch({
  onSelect,
  searchType = "ALL",
  autoFocus = false,
}: {
  onSelect: (patient: PatientSearchHit) => void;
  searchType?: "ALL" | "NAME" | "MRN" | "MOBILE" | "ABHA" | "AADHAAR";
  autoFocus?: boolean;
}) {
  const [query, setQuery] = useState("");
  const [debouncedQuery, setDebouncedQuery] = useState("");
  const [activeIndex, setActiveIndex] = useState(-1);
  const inputRef = useRef<HTMLInputElement>(null);
  const listRef = useRef<HTMLUListElement>(null);

  // Debounce search
  useEffect(() => {
    const timer = setTimeout(() => setDebouncedQuery(query), 300);
    return () => clearTimeout(timer);
  }, [query]);

  const { data, isLoading, isFetching } = usePatientSearch(debouncedQuery, searchType);

  const hits = data ?? [];

  const handleKeyDown = useCallback(
    (e: React.KeyboardEvent) => {
      if (e.key === "ArrowDown") {
        e.preventDefault();
        setActiveIndex((i) => Math.min(i + 1, hits.length - 1));
      } else if (e.key === "ArrowUp") {
        e.preventDefault();
        setActiveIndex((i) => Math.max(i - 1, -1));
      } else if (e.key === "Enter") {
        e.preventDefault();
        if (activeIndex >= 0 && hits[activeIndex]) {
          onSelect(hits[activeIndex]);
          setQuery("");
          setActiveIndex(-1);
        }
      } else if (e.key === "Escape") {
        setQuery("");
        setActiveIndex(-1);
        inputRef.current?.blur();
      }
    },
    [hits, activeIndex, onSelect],
  );

  // Scroll active item into view
  useEffect(() => {
    if (activeIndex >= 0 && listRef.current) {
      const item = listRef.current.children[activeIndex] as HTMLElement | undefined;
      item?.scrollIntoView({ block: "nearest" });
    }
  }, [activeIndex]);

  // Reset active index when results change
  useEffect(() => {
    setActiveIndex(-1);
  }, [debouncedQuery]);

  return (
    <div className="patient-search" role="search" aria-label="Patient search">
      <div className="patient-search__input-wrap">
        <TextInput
          ref={inputRef}
          type="text"
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          onKeyDown={handleKeyDown}
          onFocus={() => autoFocus}
          placeholder="Search by name, MRN, mobile, ABHA, or Aadhaar..."
          aria-label="Search patients"
          aria-expanded={hits.length > 0}
          aria-controls="patient-search-results"
          aria-activedescendant={activeIndex >= 0 ? `patient-hit-${activeIndex}` : undefined}
          role="combobox"
          autoFocus={autoFocus}
        />
        {isFetching && (
          <span className="patient-search__spinner" aria-hidden="true">
            <span className="spinner spinner--sm" />
          </span>
        )}
        {query && (
          <Button
            type="button"
            variant="ghost"
            size="sm"
            onClick={() => { setQuery(""); setActiveIndex(-1); }}
            aria-label="Clear search"
          >
            ×
          </Button>
        )}
      </div>

      {debouncedQuery.length >= 2 && (
        <div id="patient-search-results" className="patient-search__results" role="listbox">
          {isLoading ? (
            <div className="patient-search__hint" role="status">
              <span className="spinner spinner--sm" aria-hidden="true" /> Searching...
            </div>
          ) : hits.length === 0 ? (
            <div className="patient-search__empty">
              <p className="patient-search__hint">No patients found for "{debouncedQuery}"</p>
              <Button
                type="button"
                variant="primary"
                size="sm"
                onClick={() => onSelect({ patientId: 0, mrNo: "", firstName: "", mobileNo: "" })}
              >
                + Register New Patient
              </Button>
            </div>
          ) : (
            <ul ref={listRef} className="patient-search__hits" role="presentation">
              {hits.map((hit, index) => (
                <li
                  key={hit.patientId}
                  id={`patient-hit-${index}`}
                  role="option"
                  aria-selected={index === activeIndex}
                  className={`patient-search__hit ${index === activeIndex ? "patient-search__hit--active" : ""}`}
                  onClick={() => { onSelect(hit); setQuery(""); setActiveIndex(-1); }}
                  onMouseEnter={() => setActiveIndex(index)}
                >
                  <div className="patient-search__hit-main">
                    <span className="patient-search__name">
                      {hit.firstName} {hit.lastName}
                    </span>
                    <span className="patient-search__mrn">{hit.mrNo}</span>
                  </div>
                  <div className="patient-search__attrs">
                    <span>📱 {hit.mobileNo}</span>
                    {hit.abhaAddress && <span>🪪 {hit.abhaAddress}</span>}
                    {hit.ageYears != null && <span>🎂 {hit.ageYears}y</span>}
                    {hit.gender && <span>{hit.gender}</span>}
                  </div>
                </li>
              ))}
            </ul>
          )}
        </div>
      )}

      {debouncedQuery.length === 1 && (
        <p className="patient-search__hint">Type at least 2 characters to search</p>
      )}
    </div>
  );
}
