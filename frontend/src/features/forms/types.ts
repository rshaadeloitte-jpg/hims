export interface FormDefinition {
  id: number;
  code: string;
  name: string;
  specialty: string;
  description: string;
  version: number;
  legacyTiuxmlRef?: string;
  // when fetched via /schema the schema is under `schema` key
  schemaJson?: string;
  schema?: Record<string, unknown>;
}

export interface FormSchemaResponse {
  id: number;
  code: string;
  name: string;
  specialty: string;
  description: string;
  version: number;
  schema: Record<string, unknown>;
  legacyTiuxmlRef: string;
}

export interface FormSubmission {
  id: number;
  formId: number;
  patientId?: number;
  encounterId?: number;
  submissionJson: string;
  submittedBy?: number;
  submittedAt: string;
  status?: boolean;
}
