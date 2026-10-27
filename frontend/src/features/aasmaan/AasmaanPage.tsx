import { useState } from "react";
import { CaseTracker } from "./CaseTracker";
import { DeviceMonitor } from "./DeviceMonitor";
import { ETicketForm } from "./ETicketForm";
import { useFieldRecords } from "./queries";
import { Table } from "@chakra-ui/react";

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
          <div className="p-4 text-center text-gray-500" role="status">Loading field records…</div>
        ) : (
          <div className="table-wrap">
            <Table.Root size="sm" variant="line">
            <Table.Header>
              <Table.Row>
                <Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Worker</Table.ColumnHeader>
                <Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Village</Table.ColumnHeader>
                <Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Status</Table.ColumnHeader>
                <Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Synced At</Table.ColumnHeader>
              </Table.Row>
            </Table.Header>
            <Table.Body>
              {records.map((r) => (
                <Table.Row key={r.id}>
                  <Table.Cell>{r.fieldWorker}</Table.Cell>
                  <Table.Cell>{r.village}</Table.Cell>
                  <Table.Cell>{r.syncStatus}</Table.Cell>
                  <Table.Cell>{r.syncedAt}</Table.Cell>
                </Table.Row>
              ))}
              {records.length === 0 && (
                <Table.Row>
                  <Table.Cell colSpan={4}>
                    No records.
                  </Table.Cell>
                </Table.Row>
              )}
            </Table.Body>
          </Table.Root>
          </div>
        )}
      </div>

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
