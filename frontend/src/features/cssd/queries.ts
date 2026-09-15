import { useQuery, useQueryClient, useMutation } from "@tanstack/react-query";
import { ApiError } from "../../api/client";

const CSSD_BASE: string = import.meta.env.VITE_CSSD_API_BASE ?? "/api";

async function cssdJson<T>(url: string, options?: RequestInit): Promise<T> {
  const res = await fetch(url, {
    headers: { "Content-Type": "application/json", "X-Request-ID": crypto.randomUUID() },
    ...options,
  });
  if (!res.ok) {
    const err = await res.json().catch(() => ({ message: res.statusText }));
    throw new ApiError(res.status, err.errorCode ?? "UNKNOWN", err.message ?? "Request failed");
  }
  return res.json();
}

export interface Kit {
  id: number;
  kitName: string;
  status: string;
  sterilizedAt: string;
  expiresAt: string;
}

export function useKits() {
  return useQuery({ queryKey: ["cssd-kits"], queryFn: () => cssdJson<Kit[]>(`${CSSD_BASE}/v1/kits`) });
}

export function useUpdateKitStatus() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, status }: { id: number; status: string }) =>
      cssdJson<Kit>(`${CSSD_BASE}/v1/kits/${id}/status`, { method: "PATCH", body: JSON.stringify({ status }) }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["cssd-kits"] }),
  });
}
