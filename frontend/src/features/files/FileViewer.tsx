import { useState, useMemo } from "react";
import {
  useFiles,
  useDeleteFile,
  useFileCategories,
  type FileDetails,
  type FileFilter,
} from "./queries";
import { OWNER_TYPES, FILE_CATEGORIES } from "./zodSchema";

/**
 * M34 FileViewer — Grid/List view for file browsing.
 *
 * Features:
 * - Toggle between grid (card) and list (table) layouts
 * - Filter by owner type/id and category
 * - Download via token URL
 * - Delete with confirmation
 * - Thumbnail preview for images
 * - Stripe-style clean, responsive layout
 */
type ViewMode = "grid" | "list";

export function FileViewer({
  initialOwnerType,
  initialOwnerId,
  initialCategoryId,
  onAnnotate,
  onPreview,
}: {
  initialOwnerType?: string;
  initialOwnerId?: number;
  initialCategoryId?: number;
  onAnnotate?: (file: FileDetails) => void;
  onPreview?: (file: FileDetails) => void;
}) {
  const [viewMode, setViewMode] = useState<ViewMode>("grid");
  const [ownerType, setOwnerType] = useState(initialOwnerType ?? "");
  const [ownerId, setOwnerId] = useState(initialOwnerId ? String(initialOwnerId) : "");
  const [categoryId, setCategoryId] = useState(initialCategoryId ? String(initialCategoryId) : "");
  const [searchTerm, setSearchTerm] = useState("");
  const [deleteConfirmId, setDeleteConfirmId] = useState<number | null>(null);

  const filter: FileFilter = useMemo(
    () => ({
      ownerType: ownerType || undefined,
      ownerId: ownerId ? Number(ownerId) : undefined,
      fileCategoryId: categoryId ? Number(categoryId) : undefined,
    }),
    [ownerType, ownerId, categoryId],
  );

  const filesQuery = useFiles(filter);
  const categoriesQuery = useFileCategories();
  const deleteFile = useDeleteFile();

  const filteredFiles = useMemo(() => {
    if (!filesQuery.data) return [];
    if (!searchTerm) return filesQuery.data;
    const lower = searchTerm.toLowerCase();
    return filesQuery.data.filter(
      (f) =>
        f.fileName.toLowerCase().includes(lower) ||
        f.fileType.toLowerCase().includes(lower) ||
        f.ownerType?.toLowerCase().includes(lower),
    );
  }, [filesQuery.data, searchTerm]);

  const handleDelete = async (id: number) => {
    try {
      await deleteFile.mutateAsync(id);
      setDeleteConfirmId(null);
    } catch {
      // Error handled by mutation state
    }
  };

  const getDownloadUrl = (token: string) =>
    `${import.meta.env.VITE_FILE_API_BASE ?? "/api"}/api/v1/files/token/${token}/download`;

  const formatFileSize = (bytes: number) => {
    if (bytes < 1024) return `${bytes} B`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
    return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
  };

  const getFileIcon = (fileType: string) => {
    if (fileType.startsWith("image/")) return "🖼️";
    if (fileType === "application/pdf") return "📕";
    if (fileType.includes("word")) return "📝";
    if (fileType.includes("excel") || fileType.includes("spreadsheet")) return "📊";
    return "📄";
  };

  return (
    <div className="file-viewer" aria-label="file-viewer">
      {/* Toolbar */}
      <div className="file-viewer__toolbar">
        <div className="file-viewer__filters">
          <input
            type="text"
            placeholder="Search files…"
            className="file-viewer__search"
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            aria-label="Search files"
          />
          <select
            value={ownerType}
            onChange={(e) => setOwnerType(e.target.value)}
            aria-label="Filter by owner type"
          >
            <option value="">All owner types</option>
            {OWNER_TYPES.map((ot) => (
              <option key={ot} value={ot}>{ot}</option>
            ))}
          </select>
          <input
            type="number"
            placeholder="Owner ID"
            className="file-viewer__owner-id"
            value={ownerId}
            onChange={(e) => setOwnerId(e.target.value)}
            aria-label="Filter by owner ID"
          />
          <select
            value={categoryId}
            onChange={(e) => setCategoryId(e.target.value)}
            aria-label="Filter by category"
          >
            <option value="">All categories</option>
            {FILE_CATEGORIES.map((cat) => (
              <option key={cat} value={cat}>{cat.replace(/_/g, " ")}</option>
            ))}
          </select>
        </div>

        <div className="file-viewer__view-toggle" role="group" aria-label="View mode">
          <button
            type="button"
            className={viewMode === "grid" ? "active" : ""}
            onClick={() => setViewMode("grid")}
            aria-pressed={viewMode === "grid"}
            aria-label="Grid view"
          >
            ▦
          </button>
          <button
            type="button"
            className={viewMode === "list" ? "active" : ""}
            onClick={() => setViewMode("list")}
            aria-pressed={viewMode === "list"}
            aria-label="List view"
          >
            ☰
          </button>
        </div>
      </div>

      {/* Content */}
      {filesQuery.isLoading && (
        <p className="file-viewer__loading" aria-live="polite">Loading files…</p>
      )}

      {filesQuery.isError && (
        <p className="file-viewer__error" role="alert">
          Failed to load files: {(filesQuery.error as Error).message}
        </p>
      )}

      {filesQuery.data && filteredFiles.length === 0 && (
        <p className="file-viewer__empty">No files found.</p>
      )}

      {filteredFiles.length > 0 && viewMode === "grid" && (
        <div className="file-viewer__grid" role="list">
          {filteredFiles.map((file) => (
            <div
              key={file.id}
              className="file-card"
              role="listitem"
              onClick={() => onPreview?.(file)}
              style={{ cursor: onPreview ? "pointer" : undefined }}
            >
              <div className="file-card__icon" aria-hidden="true">
                {getFileIcon(file.fileType)}
              </div>
              <div className="file-card__info">
                <p className="file-card__name" title={file.fileName}>
                  {file.fileName}
                </p>
                <p className="file-card__meta">
                  {formatFileSize(file.fileSize)} · {file.fileType.split("/")[1]?.toUpperCase() ?? "FILE"}
                </p>
                {file.ownerType && (
                  <span className="file-card__badge">{file.ownerType}:{file.ownerId}</span>
                )}
              </div>
              <div className="file-card__actions">
                {file.fileType.startsWith("image/") && onAnnotate && (
                  <button
                    type="button"
                    className="btn btn-xs btn-secondary"
                    onClick={(e) => {
                      e.stopPropagation();
                      onAnnotate(file);
                    }}
                    aria-label={`Annotate ${file.fileName}`}
                  >
                    Annotate
                  </button>
                )}
                <a
                  href={getDownloadUrl(file.fileToken)}
                  className="btn btn-xs btn-primary"
                  onClick={(e) => e.stopPropagation()}
                  aria-label={`Download ${file.fileName}`}
                >
                  Download
                </a>
                {deleteConfirmId === file.id ? (
                  <div className="file-card__confirm">
                    <button
                      type="button"
                      className="btn btn-xs btn-danger"
                      onClick={(e) => {
                        e.stopPropagation();
                        handleDelete(file.id);
                      }}
                      disabled={deleteFile.isPending}
                    >
                      Confirm
                    </button>
                    <button
                      type="button"
                      className="btn btn-xs btn-secondary"
                      onClick={(e) => {
                        e.stopPropagation();
                        setDeleteConfirmId(null);
                      }}
                    >
                      Cancel
                    </button>
                  </div>
                ) : (
                  <button
                    type="button"
                    className="btn btn-xs btn-danger"
                    onClick={(e) => {
                      e.stopPropagation();
                      setDeleteConfirmId(file.id);
                    }}
                    aria-label={`Delete ${file.fileName}`}
                  >
                    Delete
                  </button>
                )}
              </div>
            </div>
          ))}
        </div>
      )}

      {filteredFiles.length > 0 && viewMode === "list" && (
        <table className="file-viewer__table">
          <thead>
            <tr>
              <th>File</th>
              <th>Type</th>
              <th>Size</th>
              <th>Owner</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {filteredFiles.map((file) => (
              <tr key={file.id}>
                <td>
                  <span className="file-viewer__table-icon" aria-hidden="true">
                    {getFileIcon(file.fileType)}
                  </span>
                  {file.fileName}
                </td>
                <td>{file.fileType}</td>
                <td>{formatFileSize(file.fileSize)}</td>
                <td>
                  {file.ownerType ? `${file.ownerType}:${file.ownerId}` : "—"}
                </td>
                <td>
                  {file.fileType.startsWith("image/") && onAnnotate && (
                    <button
                      type="button"
                      className="btn btn-xs btn-secondary mr-1"
                      onClick={() => onAnnotate(file)}
                    >
                      Annotate
                    </button>
                  )}
                  <a
                    href={getDownloadUrl(file.fileToken)}
                    className="btn btn-xs btn-primary mr-1"
                  >
                    Download
                  </a>
                  <button
                    type="button"
                    className="btn btn-xs btn-danger"
                    onClick={() => setDeleteConfirmId(file.id)}
                  >
                    Delete
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  );
}