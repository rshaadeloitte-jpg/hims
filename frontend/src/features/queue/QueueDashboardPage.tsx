import { useState } from "react";
import { useCounters, useCreateCounter, useCallNext, useTokens, useIssueToken, useTokenAction } from "./queries";
import { tokenSchema, type TokenValues } from "./zodSchema";
import { zodResolver } from "@hookform/resolvers/zod";
import { useForm } from "react-hook-form";
import { ApiError } from "../../api/client";

export function QueueDashboardPage() {
  const [counterCode, setCounterCode] = useState("");
  const [counterName, setCounterName] = useState("");
  const [departmentId, setDepartmentId] = useState("");
  const [counterError, setCounterError] = useState<string | null>(null);
  const [tokenError, setTokenError] = useState<string | null>(null);
  const [selectedCounter, setSelectedCounter] = useState<number>(0);

  const { data: counters = [] } = useCounters();
  const { data: tokens = [] } = useTokens();
  const createCounter = useCreateCounter();
  const callNext = useCallNext(selectedCounter);
  const issueToken = useIssueToken();
  const noShow = useTokenAction("no-show");
  const complete = useTokenAction("complete");

  const tokenForm = useForm<TokenValues>({
    resolver: zodResolver(tokenSchema),
    defaultValues: {
      priority: "NORMAL",
    },
  });

  const submitToken = async (v: TokenValues) => {
    try {
      setTokenError(null);
      const counterIdNum = Number(v.counterId);
      const selected = counters.find((c) => (c.id ?? c.counterId) === counterIdNum);
      const deptId = selected?.departmentId ?? v.departmentId ?? 1;
      const priorityNum = v.priority === "EMERGENCY" || Number(v.priority) === 2 ? 2 : 1;

      await issueToken.mutateAsync({
        counterId: counterIdNum,
        patientId: Number(v.patientId),
        departmentId: Number(deptId),
        priority: priorityNum,
      });
      tokenForm.reset({ priority: "NORMAL" });
    } catch (err: unknown) {
      const msg =
        err instanceof ApiError
          ? err.message
          : (err as { message?: string })?.message ?? "Failed to issue token";
      setTokenError(msg);
    }
  };

  const addCounter = async () => {
    try {
      setCounterError(null);
      const name = counterName.trim();
      const code = counterCode.trim() || (name ? name.toUpperCase().replace(/[^A-Z0-9]/g, "-").slice(0, 20) : "");
      const deptNum = departmentId.trim() ? Number(departmentId.trim()) : undefined;

      const payload: Record<string, unknown> = {
        code,
        name,
      };
      if (deptNum !== undefined && !isNaN(deptNum)) {
        payload.departmentId = deptNum;
      }

      await createCounter.mutateAsync(payload);
      setCounterCode("");
      setCounterName("");
      setDepartmentId("");
    } catch (err: unknown) {
      const msg =
        err instanceof ApiError
          ? err.message
          : (err as { message?: string })?.message ?? "Failed to create counter";
      setCounterError(msg);
    }
  };

  const counterErrorMessage = counterError || (createCounter.error as ApiError | null)?.message;
  const tokenErrorMessage = tokenError || (issueToken.error as ApiError | null)?.message;

  const statusLabel = (s: number) => {
    const map: Record<number, string> = { 1: "Waiting", 2: "Called", 3: "Completed", 4: "No-show" };
    return map[s] ?? "Unknown";
  };

  return (
    <section className="page">
      <header className="page__header"><h1>Queue Management</h1></header>

      <div className="row">
        <input
          placeholder="Counter code"
          value={counterCode}
          onChange={(e) => setCounterCode(e.target.value)}
        />
        <input
          placeholder="Counter name"
          value={counterName}
          onChange={(e) => setCounterName(e.target.value)}
        />
        <input
          type="number"
          placeholder="Department ID"
          value={departmentId}
          onChange={(e) => setDepartmentId(e.target.value)}
        />
        <button className="btn btn-primary" onClick={addCounter} disabled={createCounter.isPending}>
          + Counter
        </button>
      </div>
      {counterErrorMessage && (
        <div className="alert alert--error" role="alert" style={{ marginTop: "0.5rem" }}>
          {counterErrorMessage}
        </div>
      )}

      <h2>Issue Token</h2>
      <form className="inline-form" onSubmit={tokenForm.handleSubmit(submitToken)}>
        <select aria-label="Counter" {...tokenForm.register("counterId")}>
          <option value="">Select counter…</option>
          {counters.map((c) => {
            const id = c.id ?? c.counterId ?? 0;
            const label = c.code ? `${c.code} - ${c.name}` : (c.name || c.counterName || `Counter ${id}`);
            return (
              <option key={id} value={id}>
                {label}
              </option>
            );
          })}
        </select>
        <input type="number" placeholder="Patient ID" {...tokenForm.register("patientId")} />
        <select aria-label="Priority" {...tokenForm.register("priority")}>
          <option value="NORMAL">Normal</option>
          <option value="EMERGENCY">Emergency</option>
        </select>
        <button type="submit" className="btn btn-primary" disabled={issueToken.isPending}>Issue</button>
      </form>
      {tokenForm.formState.errors.counterId && (
        <p className="field-error" style={{ color: "var(--color-danger, #dc2626)" }}>
          {tokenForm.formState.errors.counterId.message}
        </p>
      )}
      {tokenForm.formState.errors.patientId && (
        <p className="field-error" style={{ color: "var(--color-danger, #dc2626)" }}>
          {tokenForm.formState.errors.patientId.message}
        </p>
      )}
      {tokenErrorMessage && (
        <div className="alert alert--error" role="alert" style={{ marginTop: "0.5rem" }}>
          {tokenErrorMessage}
        </div>
      )}

      <h2>Counters</h2>
      <table className="table">
        <thead><tr><th>Counter</th><th>Tokens</th><th></th></tr></thead>
        <tbody>
          {counters.map((c) => {
            const counterId = c.id ?? c.counterId ?? 0;
            const counterTokens = tokens.filter(
              (t) => t.counterId === counterId || (!t.counterId && Boolean(c.departmentId && t.departmentId === c.departmentId))
            );
            const waiting = counterTokens.filter((t) => (t.queueStatus ?? t.status) === 1).length;
            const displayName = c.code ? `${c.code} - ${c.name}` : (c.name || c.counterName || `Counter ${counterId}`);
            return (
              <tr key={counterId}>
                <td>{displayName}</td>
                <td>{waiting} waiting</td>
                <td>
                  <button className="btn btn-secondary btn--small" onClick={() => { setSelectedCounter(counterId); }}>Manage</button>
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
              {tokens.filter((t) => (t.counterId ?? (t as { counter_id?: number }).counter_id) === selectedCounter).map((t) => {
                const tokenId = t.id ?? t.tokenId ?? 0;
                const qStatus = typeof t.queueStatus === "number" ? t.queueStatus : (typeof t.status === "number" ? t.status : 1);
                const priorityDisplay = t.priority === "EMERGENCY" || (t.priority as unknown) === 2 ? "Emergency" : "Normal";
                return (
                  <tr key={tokenId}>
                    <td>{t.tokenNo}</td>
                    <td>{t.patientId}</td>
                    <td>{priorityDisplay}</td>
                    <td>{statusLabel(qStatus)}</td>
                    <td>
                      {qStatus === 2 && <button className="btn btn-secondary btn--small" onClick={() => noShow.mutate(tokenId)}>No-show</button>}
                      {qStatus === 2 && <button className="btn btn-secondary btn--small" onClick={() => complete.mutate(tokenId)}>Complete</button>}
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </>
      )}
    </section>
  );
}
