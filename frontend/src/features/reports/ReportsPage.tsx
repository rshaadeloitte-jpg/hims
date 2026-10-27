import { useState, type FormEvent } from "react";
import { Link } from "react-router-dom";
import { useReports, useCreateReport, useExecuteReport, parseReportResult } from "./queries";
import { reportMasterSchema, type ReportMasterValues } from "./zodSchema";
import { zodResolver } from "@hookform/resolvers/zod";
import { useForm } from "react-hook-form";

export function ReportsPage() {
  const { data: reports = [], isLoading } = useReports(true);
  const createReport = useCreateReport();
  const executeReport = useExecuteReport();
  const [creating, setCreating] = useState(false);
  const [executing, setExecuting] = useState<string | null>(null);
  const [result, setResult] = useState<{ rows: Record<string, unknown>[]; summary: string | null } | null>(null);
  const [err, setErr] = useState<string | null>(null);

  const createForm = useForm<ReportMasterValues>({ resolver: zodResolver(reportMasterSchema) });

  const submitCreate = async (v: ReportMasterValues) => {
    setErr(null);
    try {
      await createReport.mutateAsync({
        reportCode: v.reportCode,
        title: v.title,
        kind: Number(v.kind),
        definition: JSON.parse(v.definition),
      } as unknown as Record<string, unknown>);
      createForm.reset();
      setCreating(false);
    } catch (e) {
      setErr((e as Error).message);
    }
  };

  const submitExecute = async (code: string, e: FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    setErr(null);
    setExecuting(code);
    try {
      const fd = new FormData(e.currentTarget);
      const params: Record<string, unknown> = {};
      const dateFrom = String(fd.get("dateFrom") ?? "");
      const dateTo = String(fd.get("dateTo") ?? "");
      if (dateFrom) params.dateFrom = dateFrom;
      if (dateTo) params.dateTo = dateTo;
      const execution = await executeReport.mutateAsync({ code, params: Object.keys(params).length ? params : undefined });
      setResult(parseReportResult(execution));
    } catch (e2) {
      setErr((e2 as Error).message);
    } finally {
      setExecuting(null);
    }
  };

  const kindLabel = (k: number) => k === 1 ? "TABULAR" : "SUMMARY";

  return (
    <section className="page">
      <header className="page__header">
        <h1>Reports (M25)</h1>
        <button className="btn" onClick={() => setCreating(!creating)}>
          {creating ? "Cancel" : "+ New Report"}
        </button>
      </header>

      {creating && (
        <form className="form" onSubmit={createForm.handleSubmit(submitCreate)}>
          <fieldset>
            <legend>Create Report Definition</legend>
            <label>Code<input {...createForm.register("reportCode")} placeholder="e.g. opd_summary" /></label>
            <label>Title<input {...createForm.register("title")} placeholder="OPD Summary" /></label>
            <label>Kind
              <select {...createForm.register("kind")}>
                <option value="1">TABULAR (table)</option>
                <option value="2">SUMMARY (aggregate)</option>
              </select>
            </label>
            <label>Definition JSON
              <textarea {...createForm.register("definition")} rows={3}
                placeholder={'{"table":"rm_encounter_stats","columns":["stat_date","encounter_type","cnt"],"dateField":"stat_date"}'} />
            </label>
            <p className="muted">TABULAR: {"{"}table, columns[], dateField?{"}"} | SUMMARY: {"{"}table, aggregate: "cnt"|"total"{"}"}</p>
            <button type="submit" className="btn btn--primary" disabled={createReport.isPending}>Save</button>
          </fieldset>
        </form>
      )}

      {err && <p className="error">{err}</p>}

      {isLoading ? <div className="loading-state" role="status"><span className="spinner" aria-hidden="true" /> Loading…</div> : (
        <table className="table">
          <thead><tr><th>Code</th><th>Title</th><th>Kind</th><th>Active</th><th></th></tr></thead>
          <tbody>
            {reports.map((r) => (
              <tr key={r.reportId}>
                <td>{r.reportCode}</td>
                <td>{r.title}</td>
                <td><span className="badge">{kindLabel(r.kind)}</span></td>
                <td>{r.isActive ? "Yes" : "No"}</td>
                <td>
                  <details>
                    <summary className="btn btn--small">Run</summary>
                    <form onSubmit={(e) => submitExecute(r.reportCode, e)}>
                      <div className="row">
                        <input type="date" name="dateFrom" />
                        <input type="date" name="dateTo" />
                        <button type="submit" className="btn btn--small" disabled={executing === r.reportCode}>
                          {executing === r.reportCode ? "Running…" : "Go"}
                        </button>
                      </div>
                    </form>
                    <Link to={`/reports/executions?reportCode=${r.reportCode}`} className="btn btn--small">History</Link>
                  </details>
                </td>
              </tr>
            ))}
            {reports.length === 0 && <tr><td colSpan={5} className="muted">No report definitions.</td></tr>}
          </tbody>
        </table>
      )}

      {result && (
        <section>
          <h2>Result</h2>
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
          <button className="btn" onClick={() => setResult(null)}>Clear</button>
        </section>
      )}
    </section>
  );
}