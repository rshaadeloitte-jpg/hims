import { useState } from "react";
import { VehicleTracker } from "./VehicleTracker";
import { FuelStation } from "./FuelStation";
import { MaintenanceTracker } from "./MaintenanceTracker";

type ActiveTab = "vehicles" | "fuel" | "maintenance";

export function TransportPage() {
  const [activeTab, setActiveTab] = useState<ActiveTab>("vehicles");

  return (
    <section className="page">
      <header className="page-header">
        <h1>Transport Management</h1>
      </header>

      <nav className="tn-nav">
        <button
          className={`tn-nav__tab ${activeTab === "vehicles" ? "tn-nav__tab--active" : ""}`}
          onClick={() => setActiveTab("vehicles")}
        >
          Vehicle Tracker
        </button>
        <button
          className={`tn-nav__tab ${activeTab === "fuel" ? "tn-nav__tab--active" : ""}`}
          onClick={() => setActiveTab("fuel")}
        >
          Fuel Station
        </button>
        <button
          className={`tn-nav__tab ${activeTab === "maintenance" ? "tn-nav__tab--active" : ""}`}
          onClick={() => setActiveTab("maintenance")}
        >
          Maintenance Tracker
        </button>
      </nav>

      {activeTab === "vehicles" && <VehicleTracker />}
      {activeTab === "fuel" && <FuelStation />}
      {activeTab === "maintenance" && <MaintenanceTracker />}
    </section>
  );
}