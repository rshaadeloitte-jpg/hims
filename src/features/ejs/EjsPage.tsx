import { useState } from "react";
import "../../styles/ejs.css";
import { AshaWork } from "./AshaWork";
import { IncentiveDashboard } from "./IncentiveDashboard";
import { GeographyMaster } from "./GeographyMaster";

type EjsTab = "asha-work" | "incentives" | "geography";

/**
 * EjsPage — main EJS/ASHA tracking hub.
 * Three sub-modules: ASHA work tracking, incentive dashboard, geography register.
 */
export function EjsPage() {
  const [tab, setTab] = useState<EjsTab>("asha-work");

  const tabs: { value: EjsTab; label: string }[] = [
    { value: "asha-work", label: "ASHA Work" },
    { value: "incentives", label: "Incentives" },
    { value: "geography", label: "Geography" },
  ];

  return (
    <div className="page">
      <header className="page-header">
        <div>
          <h1>EJS / ASHA Tracking</h1>
          <p className="muted">Accredited Social Health Activist — work tracking, incentives, and population registers.</p>
        </div>
      </header>

      {/* Module tabs */}
      <section className="card toolbar" aria-label="Modules">
        <div className="tab-group">
          {tabs.map((t) => (
            <button
              key={t.value}
              className={`tab ${tab === t.value ? "tab--active" : ""}`}
              onClick={() => setTab(t.value)}
            >
              {t.label}
            </button>
          ))}
        </div>
      </section>

      {/* Tab content */}
      <div className="ejs-content">
        {tab === "asha-work" ? <AshaWork /> : null}
        {tab === "incentives" ? <IncentiveDashboard /> : null}
        {tab === "geography" ? <GeographyMaster /> : null}
      </div>
    </div>
  );
}
