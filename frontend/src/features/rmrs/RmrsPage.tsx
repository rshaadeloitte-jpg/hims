import { useJournalEntries } from "./queries";

export function RmrsPage() {
  const { data } = useJournalEntries();
  const journals = Array.isArray(data) ? data : [];

  const getStatusText = (status: number | string | undefined) => {
    if (typeof status === "string") return status;
    if (status === 1) return "DRAFT";
    if (status === 2) return "POSTED";
    if (status === 3) return "REVERSED";
    return status != null ? String(status) : "—";
  };

  return (
    <section className="page">
      <header className="page-header">
        <h1>RMRS Treasury</h1>
      </header>
      <div className="card">
        <table className="table">
          <thead>
            <tr>
              <th>Voucher No</th>
              <th>Type</th>
              <th>Amount</th>
              <th>Date</th>
              <th>Status</th>
            </tr>
          </thead>
          <tbody>
            {journals.map((j: any) => (
              <tr key={j.id ?? j.voucherId ?? j.voucherNo}>
                <td>{j.voucherNo}</td>
                <td>{j.narration ?? j.type ?? "—"}</td>
                <td>{j.amount ?? "—"}</td>
                <td>{j.voucherDate ?? j.date ?? "—"}</td>
                <td>{getStatusText(j.docStatus ?? j.status)}</td>
              </tr>
            ))}
            {journals.length === 0 && (
              <tr>
                <td colSpan={5} className="muted">
                  No journal entries.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
    </section>
  );
}
