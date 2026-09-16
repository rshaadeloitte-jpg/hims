/**
 * M34 File/Document Management Feature Module
 *
 * Components:
 * - FileUploader: Base64 upload with live preview (Stripe-style)
 * - FileViewer: Grid/List view with filtering and download
 * - AnnotatedImageViewer: Canvas-based image annotation (ImageAnnotationMaster parity)
 *
 * Hooks (from queries.ts):
 * - useFiles, useFile, useUploadFile, useDeleteFile
 * - useFileCategories, useFileCategoryTree, useCreateFileCategory
 * - useFileAnnotation, useSaveAnnotation
 * - useDownloadFileBase64
 *
 * Schemas (from zodSchema.ts):
 * - fileUploadSchema, fileFilterSchema, fileCategorySchema, annotationSchema
 */

// Components
export { FileUploader } from "./FileUploader";
export { FileViewer } from "./FileViewer";
export { AnnotatedImageViewer } from "./AnnotatedImageViewer";

// Queries
export {
  useFiles,
  useAllFiles,
  useFile,
  useFileByToken,
  useUploadFile,
  useDeleteFile,
  useDownloadFileBase64,
  useFileCategories,
  useFileCategoryTree,
  useCreateFileCategory,
  useRenameFileCategory,
  useDeleteFileCategory,
  useFileAnnotation,
  useSaveAnnotation,
  fileQueryKeys,
  readFileAsDataURL,
  stripDataURIPrefix,
} from "./queries";

// Types
export type {
  FileDetails,
  FileCategory,
  FileFilter,
  AnnotationView,
  FileUploadPayload,
} from "./queries";

// Schemas
export {
  fileUploadSchema,
  fileFilterSchema,
  fileCategorySchema,
  annotationSchema,
  renameCategorySchema,
  FILE_CATEGORIES,
  OWNER_TYPES,
} from "./zodSchema";

// Schema types
export type {
  FileUploadValues,
  FileFilterValues,
  FileCategoryValues,
  AnnotationValues,
  RenameCategoryValues,
  FileCategory as FileCategoryCode,
  OwnerType,
} from "./zodSchema";