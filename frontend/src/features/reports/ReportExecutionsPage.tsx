import { useState } from "react";
import { useReportExecutions, useReportExecution, parseReportResult } from "./queries";

const STATUS_LABEL: Record<number, string> = { 0: "QUEUED", 1: "DONE", 2: "FAILED" };

export function ReportExecutionsPage() {
  const reportCode = new URLSearchParams(window.location.search).get("reportCode") ?? undefined;
  const [selectedId, setSelectedId] = useState(0);
  const { data: executions = [], isLoading } = useReportExecutions(reportCode);
  const { data: execution } = useReportExecution(selectedId);

  const result = execution ? parseReportResult(execution) : null;

  return (
    <section className="page">
      <header className="page__header">
        <h1>Report Executions{reportCode ? ` — ${reportCode}` : ""}</h1>
      </header>

      {isLoading ? <p>Loading…</p> : (
        <table className="table">
          <thead><tr><th>Execution</th><th>Status</th><th>Rows</th><th>Executed At</th></tr></thead>
          <tbody>
            {executions.map((ex) => (
              <tr key={ex.executionId} className={selectedId === ex.executionId ? "row-selected" : ""} onClick={() => setSelectedId(ex.executionId)}>
                <td>{ex.executionId}</td>
                <td>{STATUS_LABEL[ex.status] ?? ex.status}</td>
                <td>{ex.rowCount ?? "—"}</td>
                <td>{ex.executedAt?.slice(0, 19)}</td>
              </tr>
            ))}
            {executions.length === 0 && <tr><td colSpan={4} className="muted">No executions yet.</td></tr>}
          </tbody>
        </table>
      )}

      {execution && result && (
        <section>
          <h2>Execution #{execution.executionId} — Result</h2>
          <p className="muted">Status: {STATUS_LABEL[execution.status] ?? execution.status} · Rows: {execution.rowCount ?? 0}</p>
          {result.rows.length > 0 ? (
            <table className="table">
              <thead>
                <tr>{Object.keys(result.rows[0]).map((k) => <th key={k}>{k}</th>)}</tr>
              </thead>
              <tbody>
                {result.rows.map((row, i) => (
                  <tr key={i}>{Object.values(row).map((v, j) => <td key={j}>{String(v ?? "")}</td>)}</tr>
                ))}
              </tbody>
            </table>
          ) : result.summary ? (
            <p>Summary: <strong>{result.summary}</strong></p>
          ) : (
            <p className="muted">No result data.</p>
          )}
        </section>
      )}
    </section>
  );
}