import { useDoctorShares } from "./queries";

export function DoctorSharePage() {
  const { data } = useDoctorShares(0, 50);
  const shares = data?.content ?? [];
  return (
    <section className="page">
      <header className="page-header"><h1>Doctor Share</h1></header>
      <div className="card">
        <table className="table">
          <thead><tr><th>Share ID</th><th>Doctor ID</th><th>Bill ID</th><th>Share Amount</th><th>Hospital Amount</th><th>Billed</th></tr></thead>
          <tbody>
            {shares.map((s: any) => <tr key={s.id}><td>{s.id}</td><td>{s.doctorId}</td><td>{s.billId}</td><td>{s.shareAmount}</td><td>{s.hospitalAmount}</td><td>{s.isBilled ? "Yes" : "No"}</td></tr>)}
            {shares.length === 0 && <tr><td colSpan={6} className="muted">No doctor shares.</td></tr>}
          </tbody>
        </table>
      </div>
    </section>
  );
}