import { useMemo, useState } from "react";
import { useDashboardCounts, useReportExecutions } from "./queries";
import { AreaChart, BarChart, Sparkline, type SeriesPoint } from "./components/charts";

/** Demo trend data — in production this comes from a /api/v1/dashboard/trend endpoint. */
const PATIENT_TREND: SeriesPoint[] = [
  { label: "Mon", value: 42 },
  { label: "Tue", value: 58 },
  { label: "Wed", value: 51 },
  { label: "Thu", value: 73 },
  { label: "Fri", value: 84 },
  { label: "Sat", value: 67 },
  { label: "Sun", value: 79 },
];

const ENCOUNTER_TREND: SeriesPoint[] = [
  { label: "Mon", value: 312 },
  { label: "Tue", value: 287 },
  { label: "Wed", value: 354 },
  { label: "Thu", value: 298 },
  { label: "Fri", value: 410 },
  { label: "Sat", value: 225 },
  { label: "Sun", value: 190 },
];

const REVENUE_TREND: SeriesPoint[] = [
  { label: "Mon", value: 84500 },
  { label: "Tue", value: 92300 },
  { label: "Wed", value: 78100 },
  { label: "Thu", value: 105600 },
  { label: "Fri", value: 118200 },
  { label: "Sat", value: 67800 },
  { label: "Sun", value: 54200 },
];

const REVENUE_BY_DEPT: SeriesPoint[] = [
  { label: "OPD", value: 245000 },
  { label: "IPD", value: 380000 },
  { label: "ER", value: 124000 },
  { label: "Lab", value: 89000 },
  { label: "Pharm", value: 156000 },
];

function formatCurrency(n: number): string {
  return `₹${n.toLocaleString("en-IN", { maximumFractionDigits: 0 })}`;
}

/**
 * ReportDashboard — BI dashboard with count widgets + Vercel-analytics-style
 * charts. KPI cards show headline numbers with a sparkline; larger panels
 * render area / bar charts for trend + breakdown views.
 */
export function ReportDashboard() {
  const [asOf, setAsOf] = useState("");
  const { data: counts, isLoading, isError, error } = useDashboardCounts(asOf || undefined);

  // Recent executions for the "latest run" strip.
  const { data: executions } = useReportExecutions(undefined, undefined);

  const recentRuns = useMemo(() => (executions ?? []).slice(0, 5), [executions]);

  return (
    <section className="page">
      <header className="page-header">
        <div>
          <h1>Hospital Dashboard</h1>
          <p className="muted">Real-time KPI overview — M25 Reporting Module.</p>
        </div>
        <div className="field" style={{ marginBottom: 0 }}>
          <label htmlFor="asof-date">As of date</label>
          <input
            id="asof-date"
            type="date"
            value={asOf}
            onChange={(e) => setAsOf(e.target.value)}
          />
        </div>
      </header>

      {isError && (
        <div className="alert alert--error" role="alert">
          Failed to load dashboard counts: {error instanceof Error ? error.message : String(error)}
        </div>
      )}

      {/* ── KPI count widgets ─────────────────────────────────────── */}
      <div className="kpi-grid">
        <KpiCard
          label="Patients Registered"
          value={counts?.patients}
          loading={isLoading}
          trend={PATIENT_TREND}
          color="#1d4ed8"
        />
        <KpiCard
          label="Encounters (today)"
          value={counts?.encounters}
          loading={isLoading}
          trend={ENCOUNTER_TREND}
          color="#15803d"
        />
        <KpiCard
          label="Revenue (₹)"
          value={counts?.revenueTotal}
          loading={isLoading}
          trend={REVENUE_TREND}
          color="#b45309"
          formatValue={formatCurrency}
        />
      </div>

      {/* ── Trend panels ──────────────────────────────────────────── */}
      <div className="dashboard-grid">
        <div className="card chart-card">
          <h2 className="chart-card__title">Patient Registrations</h2>
          <p className="muted chart-card__subtitle">Last 7 days</p>
          <AreaChart
            data={PATIENT_TREND}
            color="#1d4ed8"
            gradientFrom="rgba(29, 78, 216, 0.28)"
            gradientTo="rgba(29, 78, 216, 0)"
            aria-label="Patient registration trend"
          />
        </div>

        <div className="card chart-card">
          <h2 className="chart-card__title">Encounter Volume</h2>
          <p className="muted chart-card__subtitle">Last 7 days</p>
          <AreaChart
            data={ENCOUNTER_TREND}
            color="#15803d"
            gradientFrom="rgba(21, 128, 61, 0.28)"
            gradientTo="rgba(21, 128, 61, 0)"
            aria-label="Encounter volume trend"
          />
        </div>

        <div className="card chart-card chart-card--wide">
          <h2 className="chart-card__title">Revenue by Department</h2>
          <p className="muted chart-card__subtitle">Current billing cycle</p>
          <BarChart
            data={REVENUE_BY_DEPT}
            color="#1d4ed8"
            formatValue={formatCurrency}
            aria-label="Revenue breakdown by department"
          />
        </div>
      </div>

      {/* ── Recent executions strip ───────────────────────────────── */}
      <div className="card">
        <h2 className="chart-card__title">Recent Report Runs</h2>
        <div className="table-wrap">
          <table className="data-table">
            <thead>
              <tr>
                <th>Execution ID</th>
                <th>Report</th>
                <th>Status</th>
                <th>Rows</th>
                <th>Executed At</th>
              </tr>
            </thead>
            <tbody>
              {recentRuns.map((ex) => (
                <tr key={ex.executionId}>
                  <td>{ex.executionId}</td>
                  <td>{ex.reportId}</td>
                  <td>
                    <span className={`badge badge--${ex.status === 1 ? "success" : ex.status === 2 ? "muted" : "muted"}`}>
                      {ex.status === 0 ? "QUEUED" : ex.status === 1 ? "DONE" : ex.status === 2 ? "FAILED" : String(ex.status)}
                    </span>
                  </td>
                  <td className="cell--num">{ex.rowCount ?? "—"}</td>
                  <td>{ex.executedAt?.slice(0, 19)}</td>
                </tr>
              ))}
              {recentRuns.length === 0 && (
                <tr>
                  <td colSpan={5} className="muted">
                    No executions yet.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>
    </section>
  );
}

interface KpiCardProps {
  label: string;
  value: number | undefined;
  loading: boolean;
  trend: SeriesPoint[];
  color: string;
  formatValue?: (n: number) => string;
}

/**
 * KpiCard — headline number with sparkline + label. Styled after the Vercel
 * Analytics summary cards.
 */
function KpiCard({ label, value, loading, trend, color, formatValue = (n) => n.toLocaleString() }: KpiCardProps) {
  const display = value == null ? "—" : formatValue(value);

  return (
    <div className="card kpi-card">
      <div className="kpi-card__header">
        <span className="kpi-card__label">{label}</span>
      </div>
      <div className="kpi-card__body">
        <span className="kpi-card__value">{loading ? "…" : display}</span>
        <Sparkline data={trend} color={color} aria-label={`${label} sparkline`} />
      </div>
    </div>
  );
}
