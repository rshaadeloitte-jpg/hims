import { useQuery } from "@tanstack/react-query";
const BASE = import.meta.env.VITE_CHEMO_API_BASE ?? "/api";
export function useChemoRegimens() { return useQuery({ queryKey: ["chemo-regimens"], queryFn: () => fetch(`${BASE}/v1/regimens`).then(r => r.json()) }); }
