import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { ApiError } from "../../api/client";

/**
 * M39 Commons — TanStack Query hooks for the commons API.
 *
 * Endpoints (reference-service CommonsController, :8081):
 *   PUT    /api/v1/commons/config        — set app/unit config key-value
 *   GET    /api/v1/commons/config        — list configs (optional unitId)
 *   GET    /api/v1/commons/config/{key}  — get single config
 *   POST   /api/v1/commons/audit         — record audit entry
 *   GET    /api/v1/commons/audit/by-entity?entityType=&entityId=
 *   GET    /api/v1/commons/audit/by-actor/{actorId}
 */

const COMMONS_BASE: string = import.meta.env.VITE_API_BASE ?? "/api";

async function commonsJson<T>(url: string, options?: RequestInit): Promise<T> {
  const res = await fetch(url, {
    headers: { Accept: "application/json", "X-Request-ID": crypto.randomUUID() },
    ...options,
  });
  if (!res.ok) {
    const err = await res.json().catch(() => ({ message: res.statusText }));
    throw new ApiError(res.status, err.errorCode ?? "UNKNOWN", err.message ?? "Request failed");
  }
  if (res.status === 204) return undefined as T;
  return res.json();
}

// ---- Types ----------------------------------------------------------------

export interface AppConfigEntry {
  id?: number;
  unitId?: number | null;
  configKey: string;
  configValue?: string | null;
  description?: string | null;
  status?: boolean;
  addedby?: number;
  addeddatetime?: string;
  updatedby?: number;
  updateddatetime?: string;
}

export interface AuditLogEntry {
  id?: number;
  entityType: string;
  entityId: number;
  action: string;
  actorId?: number | null;
  actorUsername?: string | null;
  diff?: string | null;
  occurredAt?: string;
  status?: boolean;
  addedby?: number;
  addeddatetime?: string;
}

export interface AuditDiffPreview {
  key: string;
  oldValue?: string;
  newValue?: string;
}

// ---- Query keys -----------------------------------------------------------

export const commonsQueryKeys = {
  config: (unitId?: number | null) => ["commons", "config", unitId ?? null] as const,
  configByKey: (key: string, unitId?: number | null) =>
    ["commons", "config", unitId ?? null, key] as const,
  auditByEntity: (entityType: string, entityId: number) =>
    ["commons", "audit", "entity", entityType, entityId] as const,
  auditByActor: (actorId: number) => ["commons", "audit", "actor", actorId] as const,
};

// ---- App config -----------------------------------------------------------

export function useAppConfigList(unitId?: number | null) {
  return useQuery({
    queryKey: commonsQueryKeys.config(unitId),
    queryFn: () =>
      commonsJson<AppConfigEntry[]>(
        `${COMMONS_BASE}/commons/config${unitId != null ? `?unitId=${unitId}` : ""}`,
      ),
    staleTime: 30_000,
  });
}

export function useAppConfigByKey(key: string, unitId?: number | null) {
  return useQuery({
    queryKey: commonsQueryKeys.configByKey(key, unitId),
    queryFn: () =>
      commonsJson<{ key: string; value: string }>(
        `${COMMONS_BASE}/commons/config/${encodeURIComponent(key)}${unitId != null ? `?unitId=${unitId}` : ""}`,
      ),
    enabled: key.length > 0,
  });
}

export function useSetAppConfig(unitId?: number | null) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (payload: { key: string; value?: string; description?: string }) =>
      commonsJson<AppConfigEntry>(`${COMMONS_BASE}/commons/config${unitId != null ? `?unitId=${unitId}` : ""}`, {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(payload),
      }),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ["commons", "config"] });
    },
  });
}

// ---- Audit trail ----------------------------------------------------------

export function useAuditByEntity(entityType: string, entityId: number) {
  return useQuery({
    queryKey: commonsQueryKeys.auditByEntity(entityType, entityId),
    queryFn: () =>
      commonsJson<AuditLogEntry[]>(
        `${COMMONS_BASE}/commons/audit/by-entity?entityType=${encodeURIComponent(entityType)}&entityId=${entityId}`,
      ),
    enabled: entityType.length > 0 && entityId > 0,
    staleTime: 10_000,
  });
}

export function useAuditByActor(actorId: number) {
  return useQuery({
    queryKey: commonsQueryKeys.auditByActor(actorId),
    queryFn: () => commonsJson<AuditLogEntry[]>(`${COMMONS_BASE}/commons/audit/by-actor/${actorId}`),
    enabled: actorId > 0,
  });
}

export function useRecordAudit() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (payload: {
      entityType: string;
      entityId: number;
      action: string;
      actorId?: number;
      actorUsername?: string;
      diff?: Record<string, unknown>;
    }) =>
      commonsJson<AuditLogEntry>(`${COMMONS_BASE}/commons/audit`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(payload),
      }),
    onSuccess: (_, vars) => {
      void queryClient.invalidateQueries({
        queryKey: commonsQueryKeys.auditByEntity(vars.entityType, vars.entityId),
      });
      if (vars.actorId) {
        void queryClient.invalidateQueries({
          queryKey: commonsQueryKeys.auditByActor(vars.actorId),
        });
      }
    },
  });
}
