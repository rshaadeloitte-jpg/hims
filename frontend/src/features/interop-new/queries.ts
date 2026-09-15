import { useQuery } from "@tanstack/react-query";
import { ApiError } from "../../api/client";

const INTEROP_BASE: string = import.meta.env.VITE_INTEROP_API_BASE ?? "/api";

async function j<T>(url: string): Promise<T> {
  const r = await fetch(url, { headers: { "X-Request-ID": crypto.randomUUID() } });
  if (!r.ok) throw new ApiError(r.status, "ERR", "Failed");
  return r.json();
}

export interface SyncStatus {
  id: number;
  systemName: string;
  lastSync: string;
  status: string;
}

export function useSyncStatuses() {
  return useQuery({ queryKey: ["interop-sync"], queryFn: () => j<SyncStatus[]>(`${INTEROP_BASE}/v1/interop/sync-statuses`) });
}
