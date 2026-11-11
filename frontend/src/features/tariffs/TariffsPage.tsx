export function TariffsPage() {
  return (
    <section className="page" aria-label="tariffs-page">
      <header className="page-header">
        <h1>Tariffs & Concessions</h1>
      </header>
      <div className="card" role="region" aria-label="tariff-status">
        <div className="empty-state" style={{ padding: "2.5rem 1rem", textAlign: "center" }}>
          <h2 style={{ fontSize: "1.25rem", fontWeight: 600, color: "#1f2937", marginBottom: "0.5rem" }}>
            Tariff management is not available yet
          </h2>
          <p className="muted" style={{ color: "#6b7280", maxWidth: "480px", margin: "0 auto" }}>
            Backend services for tariff and price master configuration are pending integration.
          </p>
        </div>
      </div>
    </section>
  );
}
