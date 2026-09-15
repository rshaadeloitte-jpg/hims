import { useMemo, useState } from "react";
import { useExecuteReport, parseReportResult } from "./queries";
import { executeReportSchema, type ExecuteReportValues } from "./zodSchema";
import { zodResolver } from "@hookform/resolvers/zod";
import { useForm } from "react-hook-form";

const STATUS_LABEL: Record<number, string> = { 0: "QUEUED", 1: "DONE", 2: "FAILED" };

interface ReportViewerProps {
  /** Pre-selected report code (optional — can be chosen from dropdown). */
  initialCode?: string;
}

/**
 * ReportViewer — tabular report with date-range filters + CSV export.
 *
 * Flow:
 *   1. User picks a report + sets dateFrom/dateTo.
 *   2. On submit, the report is executed via POST /api/v1/reports/{code}/execute.
 *   3. The result rows render in a sortable table.
 *   4. "Export CSV" serialises the visible rows to a downloadable file.
 */
export function ReportViewer({ initialCode = "" }: ReportViewerProps) {
  const [code, setCode] = useState<string>(initialCode);
  const [execution, setExecution] = useState<ReturnType<typeof parseReportResult> | null>(null);
  const [rawExecution, setRawExecution] = useState<{ status: number; rowCount: number } | null>(null);
  const [err, setErr] = useState<string | null>(null);

  const { register, handleSubmit, formState: { errors } } = useForm<ExecuteReportValues>({
    resolver: zodResolver(executeReportSchema),
    defaultValues: { dateFrom: "", dateTo: "" },
  });

  const executeReport = useExecuteReport();

  const submit = async (v: ExecuteReportValues) => {
    if (!code) {
      setErr("Select a report first.");
      return;
    }
    setErr(null);
    setExecution(null);
    setRawExecution(null);
    try {
      const params: Record<string, unknown> = {};
      if (v.dateFrom) params.dateFrom = v.dateFrom;
      if (v.dateTo) params.dateTo = v.dateTo;
      const result = await executeReport.mutateAsync({
        code,
        params: Object.keys(params).length ? params : undefined,
      });
      setExecution(parseReportResult(result));
      setRawExecution({ status: result.status, rowCount: result.rowCount });
    } catch (e) {
      setErr((e as Error).message);
    }
  };

  const columns = useMemo(() => {
    if (!execution || execution.rows.length === 0) return [];
    return Object.keys(execution.rows[0]);
  }, [execution]);

  const exportCsv = () => {
    if (!execution || execution.rows.length === 0) return;
    const header = columns.join(",");
    const lines = execution.rows.map((row) =>
      columns.map((c) => {
        const v = row[c];
        const s = String(v ?? "");
        return s.includes(",") || s.includes('"') || s.includes("\n")
          ? `"${s.replace(/"/g, '""')}"`
          : s;
      }).join(","),
    );
    const csv = [header, ...lines].join("\n");
    const blob = new Blob([csv], { type: "text/csv;charset=utf-8;" });
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = `${code || "report"}-${Date.now()}.csv`;
    a.click();
    URL.revokeObjectURL(url);
  };

  return (
    <section className="page">
      <header className="page-header">
        <div>
          <h1>Report Viewer</h1>
          <p className="muted">Run a tabular report, filter by date, export to CSV.</p>
        </div>
      </header>

      {/* ── Filter bar ───────────────────────────────────────────── */}
      <form className="card filters" onSubmit={handleSubmit(submit)}>
        <div className="toolbar-row">
          <div className="field">
            <label htmlFor="report-code">Report Code</label>
            <input
              id="report-code"
              list="report-codes"
              value={code}
              onChange={(e) => setCode(e.target.value)}
              placeholder="e.g. opd_summary"
            />
            <datalist id="report-codes">
              <option value="opd_summary" />
              <option value="ipd_daily" />
              <option value="revenue_by_dept" />
              <option value="lab_orders" />
            </datalist>
          </div>
          <div className="field">
            <label htmlFor="date-from">From</label>
            <input id="date-from" type="date" {...register("dateFrom")} />
            {errors.dateFrom && <p className="field-error">{errors.dateFrom.message}</p>}
          </div>
          <div className="field">
            <label htmlFor="date-to">To</label>
            <input id="date-to" type="date" {...register("dateTo")} />
            {errors.dateTo && <p className="field-error">{errors.dateTo.message}</p>}
          </div>
          <div className="field" style={{ alignSelf: "flex-end" }}>
            <button
              type="submit"
              className="btn btn-primary"
              disabled={executeReport.isPending}
            >
              {executeReport.isPending ? "Running…" : "Run Report"}
            </button>
          </div>
        </div>
      </form>

      {err && (
        <div className="alert alert--error" role="alert">
          {err}
        </div>
      )}

      {/* ── Result table ──────────────────────────────────────────── */}
      {execution && rawExecution && (
        <div className="card">
          <div className="result-header">
            <div>
              <h2>Result</h2>
              <p className="muted">
                Status: <span className={`badge badge--${rawExecution.status === 1 ? "success" : "muted"}`}>
                  {STATUS_LABEL[rawExecution.status] ?? rawExecution.status}
                </span>{" "}
                &middot; Rows: {rawExecution.rowCount ?? 0}
              </p>
            </div>
            {execution.rows.length > 0 && (
              <button className="btn btn-secondary btn-sm" onClick={exportCsv}>
                Export CSV
              </button>
            )}
          </div>

          {execution.rows.length > 0 ? (
            <div className="table-wrap">
              <table className="data-table">
                <thead>
                  <tr>
                    {columns.map((col) => (
                      <th key={col}>{col}</th>
                    ))}
                  </tr>
                </thead>
                <tbody>
                  {execution.rows.map((row, i) => (
                    <tr key={i}>
                      {columns.map((col) => (
                        <td key={col} className={typeof row[col] === "number" ? "cell--num" : ""}>
                          {String(row[col] ?? "")}
                        </td>
                      ))}
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          ) : execution.summary ? (
            <p>
              Summary: <strong>{execution.summary}</strong>
            </p>
          ) : (
            <p className="muted">No result data.</p>
          )}
        </div>
      )}
    </section>
  );
}
