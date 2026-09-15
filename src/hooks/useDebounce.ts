import { useEffect, useState } from "react";

/**
 * Generic debounced-value hook. Updates the returned value `delay` ms after
 * the input stops changing; cancels the timer on every re-render.
 *
 * Used by PatientSearch so we don't fire a /patients/search round-trip on
 * every keystroke. The 250 ms default matches the legacy webapp's debounce.
 */
export function useDebounce<T>(value: T, delay = 250): T {
  const [debounced, setDebounced] = useState<T>(value);

  useEffect(() => {
    const handle = setTimeout(() => setDebounced(value), delay);
    return () => clearTimeout(handle);
  }, [value, delay]);

  return debounced;
}
