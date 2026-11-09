import { useClaimDetail, useApproveClaim, useRejectClaim } from "./queries";
import { ApiError } from "../../api/client";

interface DMApprovalProps {
  claimId: number;
  onApproved?: () => void;
  onRejected?: () => void;
}

const STATUS_LABELS: Record<number, string> = {
  0: "INITIATED",
  1: "SENT",
  2: "ACKNOWLEDGED",
  3: "COMPLETED",
  4: "APPROVED",
  5: "REJECTED",
};

function statusName(code: number): string {
  return STATUS_LABELS[code] ?? `UNKNOWN (${code})`;
}

function errorMessage(err: unknown): string {
  if (err instanceof ApiError) return err.message;
  if (err instanceof Error) return err.message;
  return "Request failed";
}

export function DMApproval({ claimId, onApproved, onRejected }: DMApprovalProps) {
  const { data: claim, isLoading, isError, error } = useClaimDetail(claimId);
  const approve = useApproveClaim();
  const reject = useRejectClaim();

  const isFinal = claim ? [3, 4, 5].includes(claim.statusCode) : false;
  const isPendingMutation = approve.isPending || reject.isPending;
  const canAct = !!claim && !isFinal && !isPendingMutation;
  const mutationError = (approve.error ?? reject.error) as unknown;

  return (
    <section className="card dm-approval" aria-label="DM Approval Workflow">
      <header className="card__header">
        <h2>DM Approval — Claim #{claimId}</h2>
      </header>

      {isLoading && <p className="muted">Loading claim…</p>}

      {isError && (
        <p role="alert" className="error">
          {errorMessage(error)}
        </p>
      )}

      {claim && (
        <>
          <dl className="detail-grid">
            <dt>Request ID</dt>
            <dd>{claim.requestId}</dd>
            <dt>Patient ID</dt>
            <dd>{claim.patientId}</dd>
            <dt>Bill ID</dt>
            <dd>{claim.billId ?? "—"}</dd>
            <dt>Correlation ID</dt>
            <dd>{claim.correlationId}</dd>
            <dt>Status</dt>
            <dd>{statusName(claim.statusCode)}</dd>
          </dl>

          {mutationError && (
            <p role="alert" className="error">
              {errorMessage(mutationError)}
            </p>
          )}

          <footer className="dm-approval__actions">
            <button
              className="btn btn-primary"
              disabled={!canAct}
              onClick={() =>
                approve.mutate(claimId, {
                  onSuccess: () => onApproved?.(),
                })
              }
            >
              {approve.isPending ? "Approving…" : "Approve"}
            </button>
            <button
              className="btn btn-secondary"
              disabled={!canAct}
              onClick={() =>
                reject.mutate(claimId, {
                  onSuccess: () => onRejected?.(),
                })
              }
            >
              {reject.isPending ? "Rejecting…" : "Reject"}
            </button>
          </footer>
        </>
      )}

      {!isLoading && !isError && !claim && <p className="muted">No claim found.</p>}
    </section>
  );
}
