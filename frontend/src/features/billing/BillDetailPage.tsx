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
import { Button, Table, Text } from "@chakra-ui/react";
import { HimsSelect, TextInput } from "../../components/ui";

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

  if (billLoading) return <div className="loading-state" role="status"><span className="spinner" aria-hidden="true" /> Loading…</div>;
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
              <Button onClick={() => finalize.mutate(billId)} variant="primary">Finalize</Button>
              <Button onClick={() => setShowPayment(true)} variant="secondary">+ Payment</Button>
              <Button onClick={() => setShowConcession(true)} variant="secondary">Concession</Button>
            </>
          )}
          <Button onClick={() => setShowCredit(true)} variant="secondary">Credit Note</Button>
          <Button onClick={() => setShowDebit(true)} variant="secondary">Debit Note</Button>
        </div>
      </header>

      <div className="card-grid">
        <div className="card"><Text fontSize="sm" fontWeight="medium">Patient ID</Text><strong>{bill.patientId}</strong></div>
        <div className="card"><Text fontSize="sm" fontWeight="medium">Total</Text><strong>{bill.totalAmount?.toFixed(2)}</strong></div>
        <div className="card"><Text fontSize="sm" fontWeight="medium">Balance Self</Text><strong>{bill.balanceSelf?.toFixed(2)}</strong></div>
        <div className="card"><Text fontSize="sm" fontWeight="medium">Balance Non-Self</Text><strong>{bill.balanceNonSelf?.toFixed(2)}</strong></div>
        <div className="card"><Text fontSize="sm" fontWeight="medium">Status</Text>
          <strong>{bill.isSettled ? "Settled" : bill.isCancelled ? "Cancelled" : bill.isInterim ? "Interim" : "Open"}</strong>
        </div>
      </div>

      {showPayment && (
        <form className="inline-form" onSubmit={paymentForm.handleSubmit(handlePayment)}>
          <h3>Record Payment</h3>
          <TextInput type="number" step="0.01" placeholder="Amount" {...paymentForm.register("amount")} />
          <HimsSelect {...paymentForm.register("paymentMethod")}>
            <option value="CASH">Cash</option>
            <option value="CARD">Card</option>
            <option value="UPI">UPI</option>
            <option value="CHEQUE">Cheque</option>
            <option value="ADVANCE">Advance</option>
          </HimsSelect>
          <TextInput placeholder="Reference No" {...paymentForm.register("referenceNo")} />
          <Button type="submit" variant="primary" loading={recordPayment.isPending} loadingText="Saving">Save</Button>
          <Button type="button" onClick={() => setShowPayment(false)} variant="secondary">Cancel</Button>
        </form>
      )}

      {showCredit && (
        <form className="inline-form" onSubmit={creditForm.handleSubmit(handleCredit)}>
          <h3>Issue Credit Note</h3>
          <TextInput type="number" step="0.01" placeholder="Amount" {...creditForm.register("amount")} />
          <TextInput placeholder="Reason" {...creditForm.register("reason")} />
          <Button type="submit" variant="primary" loading={issueCredit.isPending} loadingText="Saving">Save</Button>
          <Button type="button" onClick={() => setShowCredit(false)} variant="secondary">Cancel</Button>
        </form>
      )}

      {showDebit && (
        <form className="inline-form" onSubmit={debitForm.handleSubmit(handleDebit)}>
          <h3>Issue Debit Note</h3>
          <TextInput type="number" step="0.01" placeholder="Amount" {...debitForm.register("amount")} />
          <TextInput placeholder="Reason" {...debitForm.register("reason")} />
          <Button type="submit" variant="primary" loading={issueDebit.isPending} loadingText="Saving">Save</Button>
          <Button type="button" onClick={() => setShowDebit(false)} variant="secondary">Cancel</Button>
        </form>
      )}

      {showConcession && (
        <form className="inline-form" onSubmit={concessionForm.handleSubmit(handleConcession)}>
          <h3>Apply Concession</h3>
          <TextInput type="number" placeholder="Plan %" {...concessionForm.register("concessionPlanPct")} />
          <TextInput type="number" placeholder="Bill %" {...concessionForm.register("concessionBillPct")} />
          <Button type="submit" variant="primary" loading={applyConcession.isPending} loadingText="Saving">Save</Button>
          <Button type="button" onClick={() => setShowConcession(false)} variant="secondary">Cancel</Button>
        </form>
      )}

      <h2>Charges</h2>
      <Table.Root size="sm" variant="line">
        <Table.Header><Table.Row><Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Service</Table.ColumnHeader><Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Rate</Table.ColumnHeader><Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Qty</Table.ColumnHeader><Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Amount</Table.ColumnHeader><Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Tax</Table.ColumnHeader><Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Total</Table.ColumnHeader></Table.Row></Table.Header>
        <Table.Body>
          {bill.charges?.map((c) => (
            <Table.Row key={c.chargeId}>
              <Table.Cell>{c.serviceName}</Table.Cell>
              <Table.Cell>{c.rate?.toFixed(2)}</Table.Cell>
              <Table.Cell>{c.qty}</Table.Cell>
              <Table.Cell>{c.amount?.toFixed(2)}</Table.Cell>
              <Table.Cell>{c.taxAmount?.toFixed(2)}</Table.Cell>
              <Table.Cell>{c.totalAmount?.toFixed(2)}</Table.Cell>
            </Table.Row>
          ))}
          {(!bill.charges || bill.charges.length === 0) && (
            <Table.Row><Table.Cell colSpan={6} className="muted">No charges.</Table.Cell></Table.Row>
          )}
        </Table.Body>
      </Table.Root>

      <h2>Payments</h2>
      <Table.Root size="sm" variant="line">
        <Table.Header><Table.Row><Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Date</Table.ColumnHeader><Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Method</Table.ColumnHeader><Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Amount</Table.ColumnHeader><Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Reference</Table.ColumnHeader></Table.Row></Table.Header>
        <Table.Body>
          {payments.map((p) => (
            <Table.Row key={p.paymentId}>
              <Table.Cell>{p.createdAt?.slice(0, 10)}</Table.Cell>
              <Table.Cell>{p.paymentMethod}</Table.Cell>
              <Table.Cell>{p.amount?.toFixed(2)}</Table.Cell>
              <Table.Cell>{p.referenceNo ?? "—"}</Table.Cell>
            </Table.Row>
          ))}
          {payments.length === 0 && <Table.Row><Table.Cell colSpan={4} className="muted">No payments.</Table.Cell></Table.Row>}
        </Table.Body>
      </Table.Root>

      <h2>Credit Notes</h2>
      <Table.Root size="sm" variant="line">
        <Table.Header><Table.Row><Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Date</Table.ColumnHeader><Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Amount</Table.ColumnHeader><Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Reason</Table.ColumnHeader></Table.Row></Table.Header>
        <Table.Body>
          {creditNotes.map((cn) => (
            <Table.Row key={cn.creditNoteId}><Table.Cell>{cn.createdAt?.slice(0, 10)}</Table.Cell><Table.Cell>{cn.amount?.toFixed(2)}</Table.Cell><Table.Cell>{cn.reason}</Table.Cell></Table.Row>
          ))}
          {creditNotes.length === 0 && <Table.Row><Table.Cell colSpan={3} className="muted">No credit notes.</Table.Cell></Table.Row>}
        </Table.Body>
      </Table.Root>

      <h2>Debit Notes</h2>
      <Table.Root size="sm" variant="line">
        <Table.Header><Table.Row><Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Date</Table.ColumnHeader><Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Amount</Table.ColumnHeader><Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Reason</Table.ColumnHeader><Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Status</Table.ColumnHeader></Table.Row></Table.Header>
        <Table.Body>
          {debitNotes.map((dn) => (
            <Table.Row key={dn.debitNoteId}><Table.Cell>{dn.createdAt?.slice(0, 10)}</Table.Cell><Table.Cell>{dn.amount?.toFixed(2)}</Table.Cell><Table.Cell>{dn.reason}</Table.Cell><Table.Cell>{dn.status}</Table.Cell></Table.Row>
          ))}
          {debitNotes.length === 0 && <Table.Row><Table.Cell colSpan={4} className="muted">No debit notes.</Table.Cell></Table.Row>}
        </Table.Body>
      </Table.Root>

      <Link to="/billing" className="btn">← Back to Bills</Link>
    </section>
  );
}
