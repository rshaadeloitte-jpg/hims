import { useQuery } from "@tanstack/react-query";
import { ApiError } from "../../api/client";
const BASE = import.meta.env.VITE_CHEMO_API_BASE ?? "/api";
export function useChemoRegimens(patientId?: number) {
  return useQuery({
    queryKey: ["chemo-regimens", patientId],
    queryFn: async () => {
      const url = patientId
        ? `${BASE}/chemo/regimens/patient/${patientId}`
        : `${BASE}/chemo/regimens/patient/0`;
      const r = await fetch(url);
      if (!r.ok) throw new ApiError(r.status, "ERR", "Request failed");
      return r.json();
    },
    enabled: !!patientId,
  });
}
