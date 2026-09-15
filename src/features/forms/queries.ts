import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { formsApi } from "../../api/client";
import type { FormDefinition, FormSchemaResponse, FormSubmission } from "./types";

export const formKeys = {
  list: ["forms", "list"] as const,
  schema: (id: number) => ["forms", "schema", id] as const,
  submissions: (id: number) => ["forms", "submissions", id] as const,
  specialties: ["forms", "specialties"] as const,
  patientSubmissions: (patientId: number) => ["forms", "submissions", "patient", patientId] as const,
  encounterSubmissions: (encounterId: number) => ["forms", "submissions", "encounter", encounterId] as const,
};

export function useForms() {
  return useQuery({
    queryKey: formKeys.list,
    queryFn: formsApi.list,
    staleTime: 5 * 60_000,
  });
}

export function useFormSchema(id: number) {
  return useQuery<FormSchemaResponse>({
    queryKey: formKeys.schema(id),
    queryFn: () => formsApi.getSchema(id),
    enabled: Number.isFinite(id) && id > 0,
  });
}

export function useSubmitForm(formId: number) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (payload: { data: Record<string, unknown>; patientId?: number; encounterId?: number }) =>
      formsApi.submit(formId, payload.data, payload.patientId, payload.encounterId),
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: formKeys.submissions(formId) });
    },
  });
}

// re-export for convenience
export type { FormDefinition, FormSchemaResponse };

// ---- Specialty filter -------------------------------------------------------

export function useFormSpecialties() {
  return useQuery<string[]>({
    queryKey: formKeys.specialties,
    queryFn: formsApi.specialties,
    staleTime: 5 * 60_000,
  });
}

export function useFormsBySpecialty(specialty?: string) {
  return useQuery({
    queryKey: ["forms", "list", specialty],
    queryFn: () => (specialty ? formsApi.listBySpecialty(specialty) : formsApi.list()),
    enabled: specialty === undefined || specialty !== "",
    staleTime: 5 * 60_000,
  });
}

// ---- Submissions ------------------------------------------------------------

export function useFormSubmissions(formId: number) {
  return useQuery<FormSubmission[]>({
    queryKey: formKeys.submissions(formId),
    queryFn: () => formsApi.listSubmissions(formId),
    enabled: Number.isFinite(formId) && formId > 0,
  });
}

export function usePatientSubmissions(patientId: number) {
  return useQuery<FormSubmission[]>({
    queryKey: formKeys.patientSubmissions(patientId),
    queryFn: () => formsApi.listByPatient(patientId),
    enabled: Number.isFinite(patientId) && patientId > 0,
  });
}

export function useEncounterSubmissions(encounterId: number) {
  return useQuery<FormSubmission[]>({
    queryKey: formKeys.encounterSubmissions(encounterId),
    queryFn: () => formsApi.listByEncounter(encounterId),
    enabled: Number.isFinite(encounterId) && encounterId > 0,
  });
}

export function useDeleteSubmission(formId: number) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (submissionId: number) => formsApi.deleteSubmission(formId, submissionId),
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: ["forms", "submissions"] });
    },
  });
}

/** Create a new form definition (designer → API). */
export function useCreateForm() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (payload: {
      code: string;
      name: string;
      specialty: string;
      description?: string;
      version: number;
      schema: Record<string, unknown>;
    }) => formsApi.create(payload),
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: formKeys.list });
    },
  });
}

/** Update an existing form definition. */
export function useUpdateForm(id: number) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (payload: {
      code: string;
      name: string;
      specialty: string;
      description?: string;
      version: number;
      schema: Record<string, unknown>;
    }) => formsApi.update(id, payload),
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: formKeys.list });
      void qc.invalidateQueries({ queryKey: formKeys.schema(id) });
    },
  });
}

// Re-export useQueryClient for convenience
export { useQueryClient };
