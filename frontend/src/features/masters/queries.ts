import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { legacyWirePage, mastersApi } from "../../api/client";
import type { ListQuery, MasterPayload, MasterRecord } from "../../api/client";
import type { EntityMeta } from "./entityMeta";

/**
 * TanStack Query hooks for the masters API. The list query key carries the full
 * filter state so search/page-size changes automatically trigger refetches.
 */

/** Filter/pagination state of the master list (kept in the page component). */
export interface MasterListState {
  entity: string;
  /** Unit scope; undefined = unscoped (/all). */
  unitId?: number;
  /** Applied search term (search on the wire). */
  search?: string;
  /** Applied search fields (searchFld on the wire). */
  searchFld?: string;
  /** 0-based page index — converted to the legacy page param on the wire. */
  pageIndex: number;
  pageSize: number;
}

export const masterQueryKeys = {
  entities: ["masters", "entities"] as const,
  list: (state: MasterListState) => ["masters", "list", state] as const,
  detail: (entity: string, id: string, unitId?: number) =>
    ["masters", "detail", entity, id, unitId ?? null] as const,
};

/** GET /api/v1/masters — entity registry names. */
export function useMasterEntities() {
  return useQuery({
    queryKey: masterQueryKeys.entities,
    queryFn: mastersApi.listEntities,
    staleTime: 5 * 60_000,
  });
}

/**
 * Unit-scoped (or unscoped /all) list with search + legacy pagination.
 * The wire page param is `pageIndex * pageSize` so the server-side
 * `offset = (page / count) * count` equals pageIndex * pageSize exactly.
 */
export function useMasterList(state: MasterListState, meta: EntityMeta | null) {
  const searchFld = state.searchFld ?? meta?.searchFields?.join(" ") ?? undefined;
  return useQuery({
    queryKey: masterQueryKeys.list({ ...state, searchFld }),
    queryFn: async (): Promise<MasterRecord[]> => {
      const wire: ListQuery = {
        unitId: state.unitId,
        search: state.search || undefined,
        searchFld,
        page: legacyWirePage(state.pageIndex, state.pageSize),
        count: state.pageSize,
      };
      return state.unitId != null
        ? mastersApi.listScoped(state.entity, wire)
        : mastersApi.listAll(state.entity, wire);
    },
    enabled: state.entity.length > 0,
    placeholderData: (prev) => prev,
  });
}

/**
 * GET /api/v1/masters/{entity}/{id} — single record for the edit form.
 * unitId (carried from the list row, red-team F-6) is passed through to the
 * wire; unit-scoped entities 404 without it.
 */
export function useMasterDetail(entity: string, id: string, unitId?: number) {
  return useQuery({
    queryKey: masterQueryKeys.detail(entity, id, unitId),
    queryFn: () => mastersApi.getById(entity, id, unitId),
    enabled: id.length > 0 && id !== "new",
  });
}

/**
 * POST (create) / PUT (update) with unit scope; invalidates list + detail
 * caches on success. unitId is passed on create too when the form arrived
 * from a scoped list (server: unitId param ?? payload.unitId).
 */
export function useSaveMaster(entity: string, id: string, unitId?: number) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (payload: MasterPayload) =>
      id === "new"
        ? mastersApi.create(entity, payload, unitId)
        : mastersApi.update(entity, id, payload, unitId),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ["masters", "list"] });
      void queryClient.invalidateQueries({ queryKey: ["masters", "detail"] });
    },
  });
}
