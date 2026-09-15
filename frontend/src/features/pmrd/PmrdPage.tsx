import { useState } from "react";
import { EncounterRecord } from "./EncounterRecord";
import { DocumentUpload } from "./DocumentUpload";

/**
 * M24 PMRD Page — Public Medical Record Department.
 * Main entry point for JSY forms and patient document management.
 */
export function PmrdPage() {
  const [activeTab, setActiveTab] = useState<"forms" | "documents">("forms");
  const [patientId, setPatientId] = useState<number | undefined>(undefined);

  return (
    <section className="page">
      <header className="page-header">
        <h1>PMRD — Public Medical Record Department</h1>
        <p className="muted">JSY statutory forms &amp; patient document management</p>
      </header>

      {/* Patient ID input */}
      <div className="card mb-4">
        <label className="text-sm">
          <span className="block font-medium">Patient ID</span>
          <input
            type="number"
            className="w-full border rounded px-2 py-1.5"
            placeholder="Enter patient ID"
            value={patientId ?? ""}
            onChange={(e) => setPatientId(e.target.value ? Number(e.target.value) : undefined)}
          />
        </label>
      </div>

      {/* Tab navigation */}
      <div className="flex gap-2 border-b pb-2 mb-4" role="tablist">
        <button
          role="tab"
          aria-selected={activeTab === "forms"}
          className={`px-3 py-1.5 text-sm rounded-t transition-colors ${
            activeTab === "forms"
              ? "bg-blue-600 text-white"
              : "bg-gray-100 text-gray-700 hover:bg-gray-200"
          }`}
          onClick={() => setActiveTab("forms")}
        >
          JSY Forms
        </button>
        <button
          role="tab"
          aria-selected={activeTab === "documents"}
          className={`px-3 py-1.5 text-sm rounded-t transition-colors ${
            activeTab === "documents"
              ? "bg-blue-600 text-white"
              : "bg-gray-100 text-gray-700 hover:bg-gray-200"
          }`}
          onClick={() => setActiveTab("documents")}
        >
          Documents
        </button>
      </div>

      {/* Tab content */}
      {activeTab === "forms" ? (
        <EncounterRecord />
      ) : patientId ? (
        <DocumentUpload patientId={patientId} />
      ) : (
        <div className="card">
          <p className="text-sm text-gray-500">Please enter a Patient ID to manage documents.</p>
        </div>
      )}
    </section>
  );
}
