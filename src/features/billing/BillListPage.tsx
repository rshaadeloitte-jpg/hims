import { useState } from "react";
import { Link } from "react-router-dom";
import { useBills, type BillFilter } from "./queries";
import { billFilterSchema, type BillFilterValues } from "./zodSchema";
import { zodResolver } from "@hookform/resolvers/zod";
import { useForm } from "react-hook-form";

const statusBadge = (label: string, tone: "green" | "red" | "yellow" | "gray") => (
  <span className={`badge badge--${tone}`}>{label}</span>
);

const billStatus = (b: { isSettled: boolean; isCancelled: boolean; isInterim: boolean }) => {
  if (b.isCancelled) return statusBadge("Cancelled", "red");
  if (b.isSettled) return statusBadge("Settled", "green");
  if (b.isInterim) return statusBadge("Interim", "yellow");
  return statusBadge("Open", "gray");
};

export function BillListPage() {
  const { register, handleSubmit } = useForm<BillFilterValues>({
    resolver: zodResolver(billFilterSchema),
    defaultValues: { page: 0, size: 20 },
  });
  const [filter, setFilter] = useState<BillFilter>({ page: 0, size: 20 });
  const { data: bills = [], isLoading, isError, error } = useBills(filter);

  const onSubmit = (values: BillFilterValues) => {
    setFilter({
      patientId: values.patientId,
      fromDate: values.fromDate,
      toDate: values.toDate,
      isSettled: values.isSettled === "" ? undefined : values.isSettled === "true",
      page: values.page,
      size: values.size,
    });
  };

  return (
    <section className="page">
      <header className="page__header">
        <h1>Bills</h1>
        <Link to="/billing/new" className="btn btn--primary">+ New Bill</Link>
      </header>

      <form className="filters" onSubmit={handleSubmit(onSubmit)}>
        <label>
          Patient ID
          <input type="number" {...register("patientId")} />
        </label>
        <label>
          From
          <input type="date" {...register("fromDate")} />
        </label>
        <label>
          To
          <input type="date" {...register("toDate")} />
        </label>
        <label>
          Status
          <select {...register("isSettled")}>
            <option value="">All</option>
            <option value="true">Settled</option>
            <option value="false">Unsettled</option>
          </select>
        </label>
        <button type="submit" className="btn">Search</button>
      </form>

      {isLoading && <p>Loading…</p>}
      {isError && <p className="error">{(error as Error).message}</p>}

      <table className="table">
        <thead>
          <tr>
            <th>Bill No</th>
            <th>Patient</th>
            <th>Total</th>
            <th>Balance</th>
            <th>Status</th>
            <th>Date</th>
          </tr>
        </thead>
        <tbody>
          {bills.map((b) => (
            <tr key={b.billId}>
              <td><Link to={`/billing/${b.billId}`}>{b.billNo}</Link></td>
              <td>{b.patientId}</td>
              <td>{b.totalAmount?.toFixed(2)}</td>
              <td>{(b.balanceSelf + b.balanceNonSelf).toFixed(2)}</td>
              <td>{billStatus(b)}</td>
              <td>{b.finalizedAt?.slice(0, 10) ?? "—"}</td>
            </tr>
          ))}
          {bills.length === 0 && !isLoading && (
            <tr><td colSpan={6} className="muted">No bills found.</td></tr>
          )}
        </tbody>
      </table>
    </section>
  );
}
