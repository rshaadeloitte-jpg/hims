import { useState } from "react";
import { FileUploader } from "./FileUploader";
import { FileViewer } from "./FileViewer";
import { AnnotatedImageViewer } from "./AnnotatedImageViewer";
import type { FileDetails } from "./queries";

/**
 * M34 File/Document Management page.
 * Composes FileUploader (base64 upload with preview) and FileViewer (grid/list view).
 */
export function FilesPage() {
  const [annotatingFile, setAnnotatingFile] = useState<FileDetails | null>(null);
  const [notification, setNotification] = useState<{ type: "success" | "error"; message: string } | null>(null);

  const handleUploadSuccess = (file: { fileName: string; fileToken: string }) => {
    setNotification({ type: "success", message: `Uploaded: ${file.fileName}` });
    setTimeout(() => setNotification(null), 3000);
  };

  const handleUploadError = (error: Error) => {
    setNotification({ type: "error", message: error.message });
    setTimeout(() => setNotification(null), 5000);
  };

  if (annotatingFile) {
    return (
      <section className="page">
        <AnnotatedImageViewer
          fileId={annotatingFile.id}
          fileToken={annotatingFile.fileToken}
          onClose={() => setAnnotatingFile(null)}
        />
      </section>
    );
  }

  return (
    <section className="page">
      <header className="page-header">
        <h1>File / Document Management</h1>
        <p className="muted">Upload, browse, annotate, and manage files and documents.</p>
      </header>

      {notification && (
        <div
          className={`alert alert--${notification.type}`}
          role="alert"
          onClick={() => setNotification(null)}
        >
          {notification.message}
        </div>
      )}

      <div className="card mb-4">
        <h2>Upload File</h2>
        <FileUploader
          onUploadSuccess={handleUploadSuccess}
          onUploadError={handleUploadError}
        />
      </div>

      <div className="card">
        <h2>Browse Files</h2>
        <FileViewer
          onAnnotate={(file) => setAnnotatingFile(file)}
          onPreview={(file) => {
            // Preview: trigger download in new tab
            window.open(
              `${import.meta.env.VITE_FILE_API_BASE ?? "/api"}/api/v1/files/token/${file.fileToken}/download`,
              "_blank",
            );
          }}
        />
      </div>
    </section>
  );
}