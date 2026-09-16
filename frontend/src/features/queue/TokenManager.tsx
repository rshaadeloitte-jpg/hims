import { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { useTokens, useIssueToken, useTokenAction, useTransferToken, useCounters } from "./queries";
import { tokenSchema, transferSchema, TOKEN_STATUS, type TokenValues, type TransferValues } from "./zodSchema";

/**
 * M27 TokenManager — issue, transfer, and manage queue tokens.
 */
export function TokenManager() {
  const { data: tokens = [], isLoading } = useTokens();
  const { data: counters = [] } = useCounters();
  const issueToken = useIssueToken();
  const noShow = useTokenAction("no-show");
  const complete = useTokenAction("complete");
  const transfer = useTransferToken();
  const [transferTarget, setTransferTarget] = useState<number>(0);

  const tokenForm = useForm<TokenValues>({ resolver: zodResolver(tokenSchema) });
  const transferForm = useForm<TransferValues>({ resolver: zodResolver(transferSchema) });

  const submitToken = async (v: TokenValues) => {
    await issueToken.mutateAsync(v as unknown as Record<string, unknown>);
    tokenForm.reset();
  };

  const submitTransfer = async (v: TransferValues) => {
    await transfer.mutateAsync({ tokenId: v.tokenId, targetCounterId: v.targetCounterId });
    transferForm.reset();
  };

  const statusLabel = (s: number) => TOKEN_STATUS[s as keyof typeof TOKEN_STATUS] ?? "Unknown";

  if (isLoading) {
    return (
      <div className="loading-state">
        <div className="spinner" />
        Loading tokens…
      </div>
    );
  }

  return (
    <section className="page">
      <header className="page-header">
        <h1>Token Manager</h1>
      </header>

      {/* Issue token form */}
      <div className="card">
        <h2>Issue Token</h2>
        <form className="q-toolbar" onSubmit={tokenForm.handleSubmit(submitToken)}>
          <div className="field">
            <label>Counter</label>
            <select {...tokenForm.register("counterId")}>
              <option value="">Select…</option>
              {counters.map((c) => (
                <option key={c.counterId} value={c.counterId}>{c.counterName}</option>
              ))}
            </select>
          </div>
          <div className="field">
            <label>Patient ID</label>
            <input type="number" placeholder="Patient ID" {...tokenForm.register("patientId")} />
          </div>
          <div className="field">
            <label>Priority</label>
            <select {...tokenForm.register("priority")}>
              <option value="NORMAL">Normal</option>
              <option value="EMERGENCY">Emergency</option>
            </select>
          </div>
          <button type="submit" className="btn btn-primary" disabled={issueToken.isPending}>
            {issueToken.isPending ? "Issuing…" : "Issue Token"}
          </button>
        </form>
        {tokenForm.formState.errors.counterId && (
          <p className="field-error">{tokenForm.formState.errors.counterId.message}</p>
        )}
        {tokenForm.formState.errors.patientId && (
          <p className="field-error">{tokenForm.formState.errors.patientId.message}</p>
        )}
      </div>

      {/* Token list */}
      <div className="card">
        <h2>Tokens ({tokens.length})</h2>
        <div className="table-wrap">
          <table className="data-table">
            <thead>
              <tr>
                <th>Token</th>
                <th>Patient</th>
                <th>Counter</th>
                <th>Priority</th>
                <th>Status</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {tokens.map((t) => (
                <tr key={t.tokenId}>
                  <td className="cell--num">#{t.tokenNo}</td>
                  <td>{t.patientId}</td>
                  <td>{counters.find((c) => c.counterId === t.counterId)?.counterName ?? t.counterId}</td>
                  <td>
                    <span className={`q-priority q-priority--${t.priority.toLowerCase()}`}>{t.priority}</span>
                  </td>
                  <td>
                    <span className={`q-status q-status--${statusLabel(t.status).toLowerCase().replace("_", "-")}`}>
                      {statusLabel(t.status)}
                    </span>
                  </td>
                  <td>
                    <div className="q-manager-actions">
                      {t.status === 2 && (
                        <>
                          <button className="btn btn-sm btn-secondary" onClick={() => complete.mutate(t.tokenId)} disabled={complete.isPending}>
                            Complete
                          </button>
                          <button className="btn btn-sm btn-secondary" onClick={() => noShow.mutate(t.tokenId)} disabled={noShow.isPending}>
                            No-show
                          </button>
                        </>
                      )}
                      {t.status === 1 && (
                        <button className="btn btn-sm btn-secondary" onClick={() => setTransferTarget(t.tokenId)}>
                          Transfer
                        </button>
                      )}
                    </div>
                  </td>
                </tr>
              ))}
              {tokens.length === 0 && (
                <tr><td colSpan={6} className="q-empty">No tokens issued.</td></tr>
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* Transfer dialog */}
      {transferTarget > 0 && (
        <div className="card">
          <h2>Transfer Token #{tokens.find((t) => t.tokenId === transferTarget)?.tokenNo}</h2>
          <form className="q-toolbar" onSubmit={transferForm.handleSubmit(submitTransfer)}>
            <input type="hidden" value={transferTarget} {...transferForm.register("tokenId")} />
            <div className="field">
              <label>Target Counter</label>
              <select {...transferForm.register("targetCounterId")}>
                <option value="">Select…</option>
                {counters
                  .filter((c) => c.counterId !== tokens.find((t) => t.tokenId === transferTarget)?.counterId)
                  .map((c) => (
                    <option key={c.counterId} value={c.counterId}>{c.counterName}</option>
                  ))}
              </select>
            </div>
            <button type="submit" className="btn btn-primary" disabled={transfer.isPending}>Transfer</button>
            <button type="button" className="btn btn-secondary" onClick={() => setTransferTarget(0)}>Cancel</button>
          </form>
        </div>
      )}
    </section>
  );
}