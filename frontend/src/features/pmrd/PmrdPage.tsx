import { useState } from "react";
import { Button } from "@chakra-ui/react";
import { FormField, TextInput } from "../../components/ui";
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
        <FormField label="Patient ID">
          <TextInput
            type="number"
            placeholder="Enter patient ID"
            value={patientId ?? ""}
            onChange={(e) => setPatientId(e.target.value ? Number(e.target.value) : undefined)}
          />
        </FormField>
      </div>

      {/* Tab navigation */}
      <div className="flex gap-2 border-b pb-2 mb-4" role="tablist">
        <Button
          role="tab"
          aria-selected={activeTab === "forms"}
          variant={activeTab === "forms" ? "primary" : "secondary"}
          size="sm"
          onClick={() => setActiveTab("forms")}
        >
          JSY Forms
        </Button>
        <Button
          role="tab"
          aria-selected={activeTab === "documents"}
          variant={activeTab === "documents" ? "primary" : "secondary"}
          size="sm"
          onClick={() => setActiveTab("documents")}
        >
          Documents
        </Button>
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
