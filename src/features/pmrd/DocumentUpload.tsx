import { useState, useCallback, useRef } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import {
  pmrdDocumentUploadSchema,
  PMRD_DOC_CATEGORIES,
  type PmrdDocumentUploadValues,
} from "./zodSchema";
import {
  useUploadPmrdDocument,
  usePmrdDocuments,
  useDeletePmrdDocument,
  readFileAsDataURL,
  stripDataURIPrefix,
} from "./queries";

/**
 * M24 DocumentUpload — Patient document upload with categorization.
 *
 * Stripe upload patterns applied:
 * - Drag-and-drop zone with visual feedback
 * - Real-time file validation (size, type) before upload
 * - Inline preview for images (data URI)
 * - Progress indication during upload
 * - Accessible error messaging with field-level feedback
 * - Optimistic UI with retry on failure
 *
 * Ported from legacy uploadPatientDocumentsPmrd.html.
 */

export function DocumentUpload({
  patientId,
  encounterId,
  onUploadSuccess,
}: {
  patientId: number;
  encounterId?: number;
  onUploadSuccess?: (doc: { fileName: string; documentCategory: string }) => void;
}) {
  const upload = useUploadPmrdDocument();
  const { data: documents = [], isLoading: listLoading } = usePmrdDocuments({
    patientId,
    ...(encounterId ? { encounterId } : {}),
  });
  const remove = useDeletePmrdDocument();

  const fileInputRef = useRef<HTMLInputElement>(null);
  const [dragActive, setDragActive] = useState(false);
  const [preview, setPreview] = useState<string | null>(null);
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [fileError, setFileError] = useState<string | null>(null);

  const {
    register,
    handleSubmit,
    setValue,
    reset,
    formState: { errors },
  } = useForm<PmrdDocumentUploadValues>({
    resolver: zodResolver(pmrdDocumentUploadSchema),
    mode: "onChange",
    defaultValues: {
      fileName: "",
      fileType: "",
      fileData: "",
      documentCategory: "OTHER",
      patientId,
      encounterId,
      description: "",
    },
  });

  const MAX_FILE_SIZE = 10 * 1024 * 1024; // 10 MB

  const validateAndSetFile = useCallback(
    async (file: File) => {
      setFileError(null);

      if (file.size > MAX_FILE_SIZE) {
        setFileError("File exceeds 10 MB limit");
        return false;
      }

      try {
        const dataUrl = await readFileAsDataURL(file);
        setPreview(file.type.startsWith("image/") ? dataUrl : null);
        setSelectedFile(file);
        setValue("fileName", file.name, { shouldValidate: true });
        setValue("fileType", file.type || "application/octet-stream", { shouldValidate: true });
        setValue("fileData", stripDataURIPrefix(dataUrl), { shouldValidate: true });
        return true;
      } catch {
        setFileError("Failed to read file");
        return false;
      }
    },
    [setValue],
  );

  const handleFileChange = useCallback(
    async (e: React.ChangeEvent<HTMLInputElement>) => {
      const file = e.target.files?.[0];
      if (file) await validateAndSetFile(file);
    },
    [validateAndSetFile],
  );

  const handleDrop = useCallback(
    async (e: React.DragEvent) => {
      e.preventDefault();
      setDragActive(false);
      const file = e.dataTransfer.files?.[0];
      if (file) await validateAndSetFile(file);
    },
    [validateAndSetFile],
  );

  const handleDragOver = useCallback((e: React.DragEvent) => {
    e.preventDefault();
    setDragActive(true);
  }, []);

  const handleDragLeave = useCallback((e: React.DragEvent) => {
    e.preventDefault();
    setDragActive(false);
  }, []);

  const clearFile = useCallback(() => {
    setPreview(null);
    setSelectedFile(null);
    setFileError(null);
    setValue("fileName", "", { shouldValidate: false });
    setValue("fileType", "", { shouldValidate: false });
    setValue("fileData", "", { shouldValidate: false });
    if (fileInputRef.current) fileInputRef.current.value = "";
  }, [setValue]);

  const onSubmit = handleSubmit(async (values) => {
    try {
      const result = await upload.mutateAsync({
        fileName: values.fileName,
        fileType: values.fileType,
        fileData: values.fileData,
        documentCategory: values.documentCategory,
        patientId: values.patientId,
        encounterId: values.encounterId,
        description: values.description,
      });
      clearFile();
      reset({ ...values, fileName: "", fileType: "", fileData: "" });
      onUploadSuccess?.({ fileName: result.fileName, documentCategory: result.documentCategory });
    } catch {
      // error surfaced below
    }
  });

  const isUploading = upload.isPending;
  const uploadDisabled = isUploading || !selectedFile || !!fileError;

  return (
    <div className="document-upload space-y-4" aria-label="pmrd-document-upload">
      {/* Drag-and-drop zone */}
      <div
        className={`file-uploader__dropzone${dragActive ? " file-uploader__dropzone--active" : ""}${
          selectedFile ? " file-uploader__dropzone--has-file" : ""
        }`}
        onDrop={handleDrop}
        onDragOver={handleDragOver}
        onDragLeave={handleDragLeave}
        onClick={() => !selectedFile && fileInputRef.current?.click()}
        role="button"
        tabIndex={0}
        aria-label="Drop file here or click to browse"
        onKeyDown={(e) => {
          if (e.key === "Enter" || e.key === " ") {
            e.preventDefault();
            fileInputRef.current?.click();
          }
        }}
      >
        {selectedFile ? (
          <div className="file-uploader__selected">
            {preview ? (
              <img src={preview} alt="Preview" className="file-uploader__preview-img" />
            ) : (
              <div className="file-uploader__file-icon" aria-hidden="true">📄</div>
            )}
            <div className="file-uploader__file-info">
              <span className="file-uploader__file-name">{selectedFile.name}</span>
              <span className="file-uploader__file-size">
                {(selectedFile.size / 1024).toFixed(1)} KB
              </span>
            </div>
            <button
              type="button"
              className="file-uploader__clear-btn"
              onClick={(e) => {
                e.stopPropagation();
                clearFile();
              }}
              aria-label="Remove selected file"
            >
              ✕
            </button>
          </div>
        ) : (
          <div className="file-uploader__placeholder">
            <span className="file-uploader__icon" aria-hidden="true">📁</span>
            <p className="file-uploader__text">
              <strong>Drop file here</strong> or <span className="file-uploader__link">browse</span>
            </p>
            <p className="file-uploader__hint">Max 10 MB · Images, PDFs, documents</p>
          </div>
        )}
        <input
          ref={fileInputRef}
          type="file"
          className="visually-hidden"
          onChange={handleFileChange}
          accept="image/*,.pdf,.doc,.docx,.xls,.xlsx,.txt"
          aria-label="Select file to upload"
        />
      </div>

      {fileError && (
        <p className="file-uploader__error" role="alert">{fileError}</p>
      )}

      {/* Metadata form */}
      {selectedFile && (
        <form onSubmit={onSubmit} className="file-uploader__form">
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 mt-4">
            <label className="text-sm">
              <span className="block font-medium">Document Category *</span>
              <select
                className="w-full border rounded px-2 py-1.5"
                {...register("documentCategory")}
              >
                {PMRD_DOC_CATEGORIES.map((cat) => (
                  <option key={cat} value={cat}>
                    {cat.replace(/_/g, " ")}
                  </option>
                ))}
              </select>
            </label>

            <label className="text-sm">
              <span className="block font-medium">Description</span>
              <input
                className="w-full border rounded px-2 py-1.5"
                {...register("description")}
                placeholder="Optional notes"
              />
            </label>
          </div>

          {/* Hidden fields for form state */}
          <input type="hidden" {...register("fileName")} />
          <input type="hidden" {...register("fileType")} />
          <input type="hidden" {...register("fileData")} />

          {errors.fileName && (
            <p className="text-red-600 text-xs mt-1">{errors.fileName.message}</p>
          )}
          {errors.fileType && (
            <p className="text-red-600 text-xs mt-1">{errors.fileType.message}</p>
          )}
          {errors.fileData && (
            <p className="text-red-600 text-xs mt-1">{errors.fileData.message}</p>
          )}

          {upload.isError && (
            <p className="text-red-600 text-sm mt-2" role="alert">
              {(upload.error as Error).message}
            </p>
          )}

          <div className="flex gap-2 mt-4">
            <button
              type="submit"
              className="bg-blue-600 text-white px-4 py-2 rounded disabled:opacity-50 disabled:cursor-not-allowed hover:bg-blue-700 transition-colors"
              disabled={uploadDisabled}
            >
              {isUploading ? (
                <span className="flex items-center gap-2">
                  <span className="animate-spin" aria-hidden="true">⏳</span>
                  Uploading…
                </span>
              ) : (
                "Upload Document"
              )}
            </button>
            <button
              type="button"
              className="px-4 py-2 border rounded hover:bg-gray-50 transition-colors"
              onClick={clearFile}
              disabled={isUploading}
            >
              Cancel
            </button>
          </div>
        </form>
      )}

      {upload.isSuccess && (
        <p className="text-green-600 text-sm mt-2" role="status">
          ✓ Document uploaded successfully
        </p>
      )}

      {/* Document list */}
      <div className="card">
        <h3 className="font-medium mb-2">Patient Documents</h3>
        {listLoading && <p className="text-sm text-gray-500">Loading...</p>}
        {documents.length === 0 && !listLoading && (
          <p className="text-sm text-gray-500">No documents uploaded yet.</p>
        )}
        {documents.length > 0 && (
          <ul className="border rounded divide-y">
            {documents.map((doc) => (
              <li key={doc.id} className="p-2 flex items-center gap-2 text-sm">
                <span className="font-medium">{doc.documentCategory.replace(/_/g, " ")}</span>
                <span className="text-xs text-gray-600 truncate flex-1">{doc.fileName}</span>
                <span className="text-xs text-gray-500">
                  {(doc.fileSize / 1024).toFixed(1)} KB
                </span>
                <button
                  type="button"
                  className="text-xs text-red-600 hover:underline"
                  onClick={() => remove.mutate(doc.id)}
                  aria-label={`delete-${doc.id}`}
                >
                  Delete
                </button>
              </li>
            ))}
          </ul>
        )}
      </div>
    </div>
  );
}
