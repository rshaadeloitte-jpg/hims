import { useState } from "react";
import { CaseTracker } from "./CaseTracker";
import { DeviceMonitor } from "./DeviceMonitor";
import { ETicketForm } from "./ETicketForm";
import { useFieldRecords } from "./queries";

type AasmaanTab = "cases" | "devices" | "tickets";

export function AasmaanPage() {
  const [activeTab, setActiveTab] = useState<AasmaanTab>("cases");
  const [selectedCaseId, setSelectedCaseId] = useState<number | undefined>();
  const { data: records = [], isLoading } = useFieldRecords();

  return (
    <section className="page">
      <header className="page-header">
        <h1>Aasmaan Field Sync</h1>
        <p className="text-sm text-gray-500">
          Field worker case management, IoT monitoring, and e-ticketing
        </p>
      </header>

      {/* Legacy field sync table */}
      <div className="card mb-6">
        <div className="bg-gray-50 px-4 py-3 border-b border-gray-200">
          <h3 className="text-sm font-medium text-gray-900">Field Sync Records</h3>
        </div>
        {isLoading ? (
          <div className="p-4 text-center text-gray-500">Loading field records…</div>
        ) : (
          <table className="table">
            <thead>
              <tr>
                <th>Worker</th>
                <th>Village</th>
                <th>Status</th>
                <th>Synced At</th>
              </tr>
            </thead>
            <tbody>
              {records.map((r) => (
                <tr key={r.id}>
                  <td>{r.fieldWorker}</td>
                  <td>{r.village}</td>
                  <td>{r.syncStatus}</td>
                  <td>{r.syncedAt}</td>
                </tr>
              ))}
              {records.length === 0 && (
                <tr>
                  <td colSpan={4} className="muted">
                    No records.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        )}
      </div>

      {/* Tab navigation */}
      <div className="border-b border-gray-200 mb-6">
        <nav className="flex gap-4" aria-label="Aasmaan Sections">
          <button
            type="button"
            className={`pb-2 px-1 text-sm font-medium border-b-2 transition-colors ${
              activeTab === "cases"
                ? "border-blue-600 text-blue-600"
                : "border-transparent text-gray-500 hover:text-gray-700"
            }`}
            onClick={() => setActiveTab("cases")}
          >
            Field Cases
          </button>
          <button
            type="button"
            className={`pb-2 px-1 text-sm font-medium border-b-2 transition-colors ${
              activeTab === "devices"
                ? "border-blue-600 text-blue-600"
                : "border-transparent text-gray-500 hover:text-gray-700"
            }`}
            onClick={() => setActiveTab("devices")}
          >
            IoT Monitor
          </button>
          <button
            type="button"
            className={`pb-2 px-1 text-sm font-medium border-b-2 transition-colors ${
              activeTab === "tickets"
                ? "border-blue-600 text-blue-600"
                : "border-transparent text-gray-500 hover:text-gray-700"
            }`}
            onClick={() => setActiveTab("tickets")}
          >
            E-Tickets
          </button>
        </nav>
      </div>

      {/* Tab content */}
      {activeTab === "cases" && (
        <CaseTracker onCaseSelect={(id) => setSelectedCaseId(id)} />
      )}
      {activeTab === "devices" && (
        <DeviceMonitor selectedCaseId={selectedCaseId} />
      )}
      {activeTab === "tickets" && (
        <ETicketForm defaultCaseId={selectedCaseId} />
      )}
    </section>
  );
}
