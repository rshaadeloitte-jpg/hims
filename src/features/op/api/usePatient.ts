import { useEffect, useState } from "react";
import type { Patient } from "../api/types";

/**
 * usePatient — fetch a single patient by id (lazy). Used by screens that
 * need the full record (facesheet, walk-in edit) given only an id.
 *
 * Kept tiny because the OP screens typically already have the patient
 * object from the search step; this hook covers the remaining case where
 * the patient comes from a deep link or a refresh.
 */
export function usePatient(id: number | undefined) {
  const [patient, setPatient] = useState<Patient | null>(null);
  const [loading, setLoading] = useState<boolean>(false);
  const [error, setError] = useState<Error | null>(null);

  useEffect(() => {
    if (id == null) {
      setPatient(null);
      return;
    }
    let cancelled = false;
    setLoading(true);
    setError(null);
    void (async () => {
      try {
        const res = await fetch(`/api/v1/patients/${id}`);
        if (!res.ok) {
          if (!cancelled) setError(new Error(`HTTP ${res.status}`));
          return;
        }
        const data = (await res.json()) as Patient;
        if (!cancelled) setPatient(data);
      } catch (e) {
        if (!cancelled) setError(e instanceof Error ? e : new Error(String(e)));
      } finally {
        if (!cancelled) setLoading(false);
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [id]);

  return { patient, loading, error };
}
