import { useQuery } from "@tanstack/react-query";
import { ApiError } from "../../api/client";
const BASE = import.meta.env.VITE_CHEMO_API_BASE ?? "/api";
export function useChemoRegimens() {
  return useQuery({
    queryKey: ["chemo-regimens"],
    queryFn: async () => {
      const r = await fetch(`${BASE}/v1/regimens`);
      if (!r.ok) throw new ApiError(r.status, "ERR", "Request failed");
      return r.json();
    },
  });
}