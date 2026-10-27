import { useState } from "react";
import { VehicleTracker } from "./VehicleTracker";
import { FuelStation } from "./FuelStation";
import { MaintenanceTracker } from "./MaintenanceTracker";

type ActiveTab = "vehicles" | "fuel" | "maintenance";

const TABS: { key: ActiveTab; label: string }[] = [
  { key: "vehicles", label: "Vehicle Tracker" },
  { key: "fuel", label: "Fuel Station" },
  { key: "maintenance", label: "Maintenance Tracker" },
];

export function TransportPage() {
  const [activeTab, setActiveTab] = useState<ActiveTab>("vehicles");

  return (
    <section className="page">
      <header className="page-header">
        <div>
          <h1>Transport Management</h1>
          <p className="muted">Vehicle tracking, fuel, and maintenance.</p>
        </div>
      </header>

      <nav className="tab-group" role="tablist" aria-label="Transport sections">
        {TABS.map((t) => (
          <button
            key={t.key}
            type="button"
            role="tab"
            aria-selected={activeTab === t.key}
            className={`tab ${activeTab === t.key ? "tab--active" : ""}`}
            onClick={() => setActiveTab(t.key)}
          >
            {t.label}
          </button>
        ))}
      </nav>

      {activeTab === "vehicles" && <VehicleTracker />}
      {activeTab === "fuel" && <FuelStation />}
      {activeTab === "maintenance" && <MaintenanceTracker />}
    </section>
  );
}
