import { useState, useMemo } from "react";
import { Button, Table } from "@chakra-ui/react";
import { HimsSelect, TextInput } from "../../components/ui";
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
  useFileCategories();
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
          <TextInput
            type="text"
            placeholder="Search files…"
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            aria-label="Search files"
          />
          <HimsSelect
            value={ownerType}
            onChange={(e) => setOwnerType(e.target.value)}
            aria-label="Filter by owner type"
          >
            <option value="">All owner types</option>
            {OWNER_TYPES.map((ot) => (
              <option key={ot} value={ot}>{ot}</option>
            ))}
          </HimsSelect>
          <TextInput
            type="number"
            placeholder="Owner ID"
            value={ownerId}
            onChange={(e) => setOwnerId(e.target.value)}
            aria-label="Filter by owner ID"
          />
          <HimsSelect
            value={categoryId}
            onChange={(e) => setCategoryId(e.target.value)}
            aria-label="Filter by category"
          >
            <option value="">All categories</option>
            {FILE_CATEGORIES.map((cat) => (
              <option key={cat} value={cat}>{cat.replace(/_/g, " ")}</option>
            ))}
          </HimsSelect>
        </div>

        <div className="file-viewer__view-toggle" role="group" aria-label="View mode">
          <Button
            type="button"
            variant={viewMode === "grid" ? "secondary" : "ghost"}
            size="sm"
            onClick={() => setViewMode("grid")}
            aria-pressed={viewMode === "grid"}
            aria-label="Grid view"
          >
            ▦
          </Button>
          <Button
            type="button"
            variant={viewMode === "list" ? "secondary" : "ghost"}
            size="sm"
            onClick={() => setViewMode("list")}
            aria-pressed={viewMode === "list"}
            aria-label="List view"
          >
            ☰
          </Button>
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
                  <Button
                    type="button"
                    variant="secondary"
                    size="sm"
                    onClick={(e) => {
                      e.stopPropagation();
                      onAnnotate(file);
                    }}
                    aria-label={`Annotate ${file.fileName}`}
                  >
                    Annotate
                  </Button>
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
                    <Button
                      type="button"
                      variant="primary"
                      size="sm"
                      onClick={(e) => {
                        e.stopPropagation();
                        handleDelete(file.id);
                      }}
                      loading={deleteFile.isPending}
                      loadingText="Deleting..."
                    >
                      Confirm
                    </Button>
                    <Button
                      type="button"
                      variant="secondary"
                      size="sm"
                      onClick={(e) => {
                        e.stopPropagation();
                        setDeleteConfirmId(null);
                      }}
                    >
                      Cancel
                    </Button>
                  </div>
                ) : (
                  <Button
                    type="button"
                    variant="ghost"
                    size="sm"
                    onClick={(e) => {
                      e.stopPropagation();
                      setDeleteConfirmId(file.id);
                    }}
                    aria-label={`Delete ${file.fileName}`}
                  >
                    Delete
                  </Button>
                )}
              </div>
            </div>
          ))}
        </div>
      )}

      {filteredFiles.length > 0 && viewMode === "list" && (
        <Table.Root size="sm" variant="line">
          <Table.Header>
            <Table.Row>
              <Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">File</Table.ColumnHeader>
              <Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Type</Table.ColumnHeader>
              <Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Size</Table.ColumnHeader>
              <Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Owner</Table.ColumnHeader>
              <Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Actions</Table.ColumnHeader>
            </Table.Row>
          </Table.Header>
          <Table.Body>
            {filteredFiles.map((file) => (
              <Table.Row key={file.id}>
                <Table.Cell>
                  <span className="file-viewer__table-icon" aria-hidden="true">
                    {getFileIcon(file.fileType)}
                  </span>
                  {file.fileName}
                </Table.Cell>
                <Table.Cell>{file.fileType}</Table.Cell>
                <Table.Cell>{formatFileSize(file.fileSize)}</Table.Cell>
                <Table.Cell>
                  {file.ownerType ? `${file.ownerType}:${file.ownerId}` : "—"}
                </Table.Cell>
                <Table.Cell>
                  {file.fileType.startsWith("image/") && onAnnotate && (
                    <Button
                      type="button"
                      variant="secondary"
                      size="sm"
                      onClick={() => onAnnotate(file)}
                    >
                      Annotate
                    </Button>
                  )}
                  <a
                    href={getDownloadUrl(file.fileToken)}
                    className="btn btn-xs btn-primary mr-1"
                  >
                    Download
                  </a>
                  <Button
                    type="button"
                    variant="ghost"
                    size="sm"
                    onClick={() => setDeleteConfirmId(file.id)}
                  >
                    Delete
                  </Button>
                </Table.Cell>
              </Table.Row>
            ))}
          </Table.Body>
        </Table.Root>
      )}
    </div>
  );
}
