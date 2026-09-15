import { useQuery } from "@tanstack/react-query";
import { ApiError } from "../../api/client";
const BASE = import.meta.env.VITE_LINEN_API_BASE ?? "/api";
export function useLinenRequests() { return useQuery({ queryKey: ["linen-reqs"], queryFn: () => fetch(`${BASE}/v1/linen-requests`).then(r => r.json()) }); }
