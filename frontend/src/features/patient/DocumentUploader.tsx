import { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { Button, SimpleGrid } from "@chakra-ui/react";
import { FormField, HimsSelect, TextInput } from "../../components/ui";
import { documentUploadSchema, type DocumentUploadValues } from "./zodSchema";
import {
  useDeletePatientDocument,
  usePatientDocuments,
  useUploadPatientDocument,
} from "./queries";

/**
 * M02 Document Uploader (G-1). Multipart upload of patient documents to
 * patient-service. The legacy backend stores them in the patient_documents
 * table and optionally syncs to ABDM PHR (interop-service, M38).
 */
export function DocumentUploader({ patientId }: { patientId: number }) {
  const docs = usePatientDocuments(patientId);
  const upload = useUploadPatientDocument(patientId);
  const remove = useDeletePatientDocument(patientId);
  const [lastUploadedName, setLastUploadedName] = useState<string | null>(null);

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<DocumentUploadValues>({
    resolver: zodResolver(documentUploadSchema),
    defaultValues: { documentType: "ID_PROOF", description: "" },
  });

  const onSubmit = handleSubmit(async (values) => {
    const file = values.file[0];
    setLastUploadedName(file.name);
    await upload.mutateAsync({ file, documentType: values.documentType, description: values.description });
    reset({ documentType: values.documentType, description: "" });
  });

  return (
    <section className="space-y-3 p-4 border rounded" aria-label="document-uploader">
      <h2 className="text-lg font-semibold">Patient Documents</h2>

      <form onSubmit={onSubmit} className="space-y-2">
        <SimpleGrid columns={{ base: 1, sm: 3 }} gap={2}>
          <FormField label="Type">
            <HimsSelect {...register("documentType")}>
              {["AADHAAR", "PAN", "INSURANCE", "ID_PROOF", "PHOTO", "DISCHARGE_SUMMARY", "PRESCRIPTION", "OTHER"].map((d) => (
                <option key={d} value={d}>
                  {d}
                </option>
              ))}
            </HimsSelect>
          </FormField>
          <FormField label="Description">
            <TextInput {...register("description")} />
          </FormField>
        </SimpleGrid>

        <FormField
          label="File (max 10 MB)"
          error={(errors.file as { message?: string } | undefined)?.message}
        >
          <TextInput type="file" {...register("file")} />
        </FormField>

        {upload.isError && (
          <p className="text-sm text-red-600">{(upload.error as Error).message}</p>
        )}
        {upload.isSuccess && (
          <p className="text-sm text-green-600">Uploaded {lastUploadedName}</p>
        )}

        <Button
          type="submit"
          variant="primary"
          loading={upload.isPending}
          loadingText="Uploading..."
        >
          Upload
        </Button>
      </form>

      <div>
        <h3 className="font-medium">Existing</h3>
        {docs.isLoading && <p className="text-sm text-gray-500">Loading...</p>}
        {docs.data && docs.data.length === 0 && (
          <p className="text-sm text-gray-500">No documents uploaded yet.</p>
        )}
        {docs.data && docs.data.length > 0 && (
          <ul className="border rounded divide-y">
            {docs.data.map((d) => (
              <li key={d.id} className="p-2 flex items-center gap-2">
                <span className="font-medium">{d.documentType}</span>
                <span className="text-xs text-gray-600 truncate flex-1">{d.fileName}</span>
                <span className="text-xs text-gray-500">
                  {(d.sizeBytes / 1024).toFixed(1)} KB
                </span>
                {d.abdmSyncStatus && (
                  <span className="text-xs px-2 py-0.5 rounded bg-gray-100">{d.abdmSyncStatus}</span>
                )}
                <Button
                  type="button"
                  variant="ghost"
                  size="sm"
                  onClick={() => remove.mutate(d.id)}
                  aria-label={`delete-${d.id}`}
                >
                  Delete
                </Button>
              </li>
            ))}
          </ul>
        )}
      </div>
    </section>
  );
}
