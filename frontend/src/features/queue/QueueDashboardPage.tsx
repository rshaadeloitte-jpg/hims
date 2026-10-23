import { useState } from "react";
import { useCounters, useCreateCounter, useCallNext, useTokens, useIssueToken, useTokenAction } from "./queries";
import { tokenSchema, type TokenValues } from "./zodSchema";
import { zodResolver } from "@hookform/resolvers/zod";
import { useForm } from "react-hook-form";

export function QueueDashboardPage() {
  const [counterName, setCounterName] = useState("");
  const [selectedCounter, setSelectedCounter] = useState<number>(0);
  const { data: counters = [] } = useCounters();
  const { data: tokens = [] } = useTokens();
  const createCounter = useCreateCounter();
  const callNext = useCallNext(selectedCounter);
  const issueToken = useIssueToken();
  const noShow = useTokenAction("no-show");
  const complete = useTokenAction("complete");

  const tokenForm = useForm<TokenValues>({ resolver: zodResolver(tokenSchema) });

  const submitToken = async (v: TokenValues) => {
    await issueToken.mutateAsync(v as unknown as Record<string, unknown>);
    tokenForm.reset();
  };

  const addCounter = async () => {
    if (!counterName) return;
    await createCounter.mutateAsync({ counterName });
    setCounterName("");
  };

  const statusLabel = (s: number) => {
    const map: Record<number, string> = { 1: "Waiting", 2: "Called", 3: "Completed", 4: "No-show" };
    return map[s] ?? "Unknown";
  };

  return (
    <section className="page">
      <header className="page__header"><h1>Queue Management</h1></header>

      <div className="row">
        <input placeholder="Counter name" value={counterName} onChange={(e) => setCounterName(e.target.value)} />
        <button className="btn btn-primary" onClick={addCounter} disabled={createCounter.isPending}>+ Counter</button>
      </div>

      <h2>Issue Token</h2>
      <form className="inline-form" onSubmit={tokenForm.handleSubmit(submitToken)}>
        <input type="number" placeholder="Counter ID" {...tokenForm.register("counterId")} />
        <input type="number" placeholder="Patient ID" {...tokenForm.register("patientId")} />
        <select {...tokenForm.register("priority")}>
          <option value="NORMAL">Normal</option>
          <option value="EMERGENCY">Emergency</option>
        </select>
        <button type="submit" className="btn btn-primary" disabled={issueToken.isPending}>Issue</button>
      </form>

      <h2>Counters</h2>
      <table className="table">
        <thead><tr><th>Counter</th><th>Tokens</th><th></th></tr></thead>
        <tbody>
          {counters.map((c) => {
            const counterTokens = tokens.filter((t) => t.counterId === c.counterId);
            const waiting = counterTokens.filter((t) => t.status === 1).length;
            return (
              <tr key={c.counterId}>
                <td>{c.counterName}</td>
                <td>{waiting} waiting</td>
                <td>
                  <button className="btn btn-secondary btn--small" onClick={() => { setSelectedCounter(c.counterId); }}>Manage</button>
                </td>
              </tr>
            );
          })}
          {counters.length === 0 && <tr><td colSpan={3} className="muted">No counters.</td></tr>}
        </tbody>
      </table>

      {selectedCounter > 0 && (
        <>
          <h2>Counter #{selectedCounter} — Tokens</h2>
          <button className="btn btn-primary" onClick={() => callNext.mutate()} disabled={callNext.isPending}>Call Next</button>
          <table className="table">
            <thead><tr><th>Token</th><th>Patient</th><th>Priority</th><th>Status</th><th></th></tr></thead>
            <tbody>
              {tokens.filter((t) => t.counterId === selectedCounter).map((t) => (
                <tr key={t.tokenId}>
                  <td>{t.tokenNo}</td>
                  <td>{t.patientId}</td>
                  <td>{t.priority}</td>
                  <td>{statusLabel(t.status)}</td>
                  <td>
                    {t.status === 2 && <button className="btn btn-secondary btn--small" onClick={() => noShow.mutate(t.tokenId)}>No-show</button>}
                    {t.status === 2 && <button className="btn btn-secondary btn--small" onClick={() => complete.mutate(t.tokenId)}>Complete</button>}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </>
      )}
    </section>
  );
}