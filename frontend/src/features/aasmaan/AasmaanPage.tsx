import { useState } from "react";
import { CaseTracker } from "./CaseTracker";
import { DeviceMonitor } from "./DeviceMonitor";
import { ETicketForm } from "./ETicketForm";

type AasmaanTab = "cases" | "devices" | "tickets";

export function AasmaanPage() {
  const [activeTab, setActiveTab] = useState<AasmaanTab>("cases");
  const [selectedCaseId, setSelectedCaseId] = useState<number | undefined>();

  return (
    <section className="page">
      <header className="page-header">
        <h1>Aasmaan Field Sync</h1>
        <p className="text-sm text-gray-500">
          Field worker case management, IoT monitoring, and e-ticketing
        </p>
      </header>

      {/* Tab navigation */}
      <nav className="tab-group mb-6" role="tablist" aria-label="Aasmaan Sections">
        <button
          type="button"
          role="tab"
          aria-selected={activeTab === "cases"}
          className={`tab ${activeTab === "cases" ? "tab--active" : ""}`}
          onClick={() => setActiveTab("cases")}
        >
          Field Cases
        </button>
        <button
          type="button"
          role="tab"
          aria-selected={activeTab === "devices"}
          className={`tab ${activeTab === "devices" ? "tab--active" : ""}`}
          onClick={() => setActiveTab("devices")}
        >
          IoT Monitor
        </button>
        <button
          type="button"
          role="tab"
          aria-selected={activeTab === "tickets"}
          className={`tab ${activeTab === "tickets" ? "tab--active" : ""}`}
          onClick={() => setActiveTab("tickets")}
        >
          E-Tickets
        </button>
      </nav>

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
