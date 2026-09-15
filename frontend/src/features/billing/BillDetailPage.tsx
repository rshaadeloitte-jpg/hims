import { useState } from "react";
import { useParams, Link } from "react-router-dom";
import {
  useBill,
  usePayments,
  useCreditNotes,
  useDebitNotes,
  useRecordPayment,
  useIssueCreditNote,
  useIssueDebitNote,
  useFinalizeBill,
  useApplyConcession,
} from "./queries";
import {
  type PaymentValues,
  type CreditNoteValues,
  type DebitNoteValues,
  type ConcessionValues,
} from "./zodSchema";
import { paymentSchema, creditNoteSchema, debitNoteSchema, concessionSchema } from "./zodSchema";
import { zodResolver } from "@hookform/resolvers/zod";
import { useForm } from "react-hook-form";

export function BillDetailPage() {
  const { id } = useParams<{ id: string }>();
  const billId = Number(id);
  const { data: bill, isLoading: billLoading } = useBill(billId);
  const { data: payments = [] } = usePayments(billId);
  const { data: creditNotes = [] } = useCreditNotes(billId);
  const { data: debitNotes = [] } = useDebitNotes(billId);

  const [showPayment, setShowPayment] = useState(false);
  const [showCredit, setShowCredit] = useState(false);
  const [showDebit, setShowDebit] = useState(false);
  const [showConcession, setShowConcession] = useState(false);

  const recordPayment = useRecordPayment();
  const issueCredit = useIssueCreditNote();
  const issueDebit = useIssueDebitNote();
  const finalize = useFinalizeBill();
  const applyConcession = useApplyConcession();

  const paymentForm = useForm<PaymentValues>({ resolver: zodResolver(paymentSchema) });
  const creditForm = useForm<CreditNoteValues>({ resolver: zodResolver(creditNoteSchema) });
  const debitForm = useForm<DebitNoteValues>({ resolver: zodResolver(debitNoteSchema) });
  const concessionForm = useForm<ConcessionValues>({ resolver: zodResolver(concessionSchema) });

  if (billLoading) return <p>Loading…</p>;
  if (!bill) return <p>Bill not found.</p>;

  const handlePayment = async (values: PaymentValues) => {
    await recordPayment.mutateAsync({ id: billId, payload: values });
    setShowPayment(false);
    paymentForm.reset();
  };

  const handleCredit = async (values: CreditNoteValues) => {
    await issueCredit.mutateAsync({ id: billId, payload: values });
    setShowCredit(false);
    creditForm.reset();
  };

  const handleDebit = async (values: DebitNoteValues) => {
    await issueDebit.mutateAsync({ id: billId, payload: values });
    setShowDebit(false);
    debitForm.reset();
  };

  const handleConcession = async (values: ConcessionValues) => {
    await applyConcession.mutateAsync({ id: billId, payload: values });
    setShowConcession(false);
    concessionForm.reset();
  };

  return (
    <section className="page">
      <header className="page__header">
        <h1>Bill {bill.billNo}</h1>
        <div className="page__actions">
          {!bill.isSettled && !bill.isCancelled && (
            <>
              <button onClick={() => finalize.mutate(billId)} className="btn btn--primary">Finalize</button>
              <button onClick={() => setShowPayment(true)} className="btn">+ Payment</button>
              <button onClick={() => setShowConcession(true)} className="btn">Concession</button>
            </>
          )}
          <button onClick={() => setShowCredit(true)} className="btn">Credit Note</button>
          <button onClick={() => setShowDebit(true)} className="btn">Debit Note</button>
        </div>
      </header>

      <div className="card-grid">
        <div className="card"><label>Patient ID</label><strong>{bill.patientId}</strong></div>
        <div className="card"><label>Total</label><strong>{bill.totalAmount?.toFixed(2)}</strong></div>
        <div className="card"><label>Balance Self</label><strong>{bill.balanceSelf?.toFixed(2)}</strong></div>
        <div className="card"><label>Balance Non-Self</label><strong>{bill.balanceNonSelf?.toFixed(2)}</strong></div>
        <div className="card"><label>Status</label>
          <strong>{bill.isSettled ? "Settled" : bill.isCancelled ? "Cancelled" : bill.isInterim ? "Interim" : "Open"}</strong>
        </div>
      </div>

      {showPayment && (
        <form className="inline-form" onSubmit={paymentForm.handleSubmit(handlePayment)}>
          <h3>Record Payment</h3>
          <input type="number" step="0.01" placeholder="Amount" {...paymentForm.register("amount")} />
          <select {...paymentForm.register("paymentMethod")}>
            <option value="CASH">Cash</option>
            <option value="CARD">Card</option>
            <option value="UPI">UPI</option>
            <option value="CHEQUE">Cheque</option>
            <option value="ADVANCE">Advance</option>
          </select>
          <input placeholder="Reference No" {...paymentForm.register("referenceNo")} />
          <button type="submit" className="btn btn--primary" disabled={recordPayment.isPending}>Save</button>
          <button type="button" onClick={() => setShowPayment(false)} className="btn">Cancel</button>
        </form>
      )}

      {showCredit && (
        <form className="inline-form" onSubmit={creditForm.handleSubmit(handleCredit)}>
          <h3>Issue Credit Note</h3>
          <input type="number" step="0.01" placeholder="Amount" {...creditForm.register("amount")} />
          <input placeholder="Reason" {...creditForm.register("reason")} />
          <button type="submit" className="btn btn--primary" disabled={issueCredit.isPending}>Save</button>
          <button type="button" onClick={() => setShowCredit(false)} className="btn">Cancel</button>
        </form>
      )}

      {showDebit && (
        <form className="inline-form" onSubmit={debitForm.handleSubmit(handleDebit)}>
          <h3>Issue Debit Note</h3>
          <input type="number" step="0.01" placeholder="Amount" {...debitForm.register("amount")} />
          <input placeholder="Reason" {...debitForm.register("reason")} />
          <button type="submit" className="btn btn--primary" disabled={issueDebit.isPending}>Save</button>
          <button type="button" onClick={() => setShowDebit(false)} className="btn">Cancel</button>
        </form>
      )}

      {showConcession && (
        <form className="inline-form" onSubmit={concessionForm.handleSubmit(handleConcession)}>
          <h3>Apply Concession</h3>
          <input type="number" placeholder="Plan %" {...concessionForm.register("concessionPlanPct")} />
          <input type="number" placeholder="Bill %" {...concessionForm.register("concessionBillPct")} />
          <button type="submit" className="btn btn--primary" disabled={applyConcession.isPending}>Save</button>
          <button type="button" onClick={() => setShowConcession(false)} className="btn">Cancel</button>
        </form>
      )}

      <h2>Charges</h2>
      <table className="table">
        <thead><tr><th>Service</th><th>Rate</th><th>Qty</th><th>Amount</th><th>Tax</th><th>Total</th></tr></thead>
        <tbody>
          {bill.charges?.map((c) => (
            <tr key={c.chargeId}>
              <td>{c.serviceName}</td>
              <td>{c.rate?.toFixed(2)}</td>
              <td>{c.qty}</td>
              <td>{c.amount?.toFixed(2)}</td>
              <td>{c.taxAmount?.toFixed(2)}</td>
              <td>{c.totalAmount?.toFixed(2)}</td>
            </tr>
          ))}
          {(!bill.charges || bill.charges.length === 0) && (
            <tr><td colSpan={6} className="muted">No charges.</td></tr>
          )}
        </tbody>
      </table>

      <h2>Payments</h2>
      <table className="table">
        <thead><tr><th>Date</th><th>Method</th><th>Amount</th><th>Reference</th></tr></thead>
        <tbody>
          {payments.map((p) => (
            <tr key={p.paymentId}>
              <td>{p.createdAt?.slice(0, 10)}</td>
              <td>{p.paymentMethod}</td>
              <td>{p.amount?.toFixed(2)}</td>
              <td>{p.referenceNo ?? "—"}</td>
            </tr>
          ))}
          {payments.length === 0 && <tr><td colSpan={4} className="muted">No payments.</td></tr>}
        </tbody>
      </table>

      <h2>Credit Notes</h2>
      <table className="table">
        <thead><tr><th>Date</th><th>Amount</th><th>Reason</th></tr></thead>
        <tbody>
          {creditNotes.map((cn) => (
            <tr key={cn.creditNoteId}><td>{cn.createdAt?.slice(0, 10)}</td><td>{cn.amount?.toFixed(2)}</td><td>{cn.reason}</td></tr>
          ))}
          {creditNotes.length === 0 && <tr><td colSpan={3} className="muted">No credit notes.</td></tr>}
        </tbody>
      </table>

      <h2>Debit Notes</h2>
      <table className="table">
        <thead><tr><th>Date</th><th>Amount</th><th>Reason</th><th>Status</th></tr></thead>
        <tbody>
          {debitNotes.map((dn) => (
            <tr key={dn.debitNoteId}><td>{dn.createdAt?.slice(0, 10)}</td><td>{dn.amount?.toFixed(2)}</td><td>{dn.reason}</td><td>{dn.status}</td></tr>
          ))}
          {debitNotes.length === 0 && <tr><td colSpan={4} className="muted">No debit notes.</td></tr>}
        </tbody>
      </table>

      <Link to="/billing" className="btn">← Back to Bills</Link>
    </section>
  );
}
