import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { rcmJson } from "./queries";
import type { Claim } from "./queries";

interface DischargeVerificationProps {
  patientId: number;
  onVerified?: () => void;
}

type ClearanceStatus = "pending" | "bill_generated" | "bill_verified" | "cleared" | "blocked";

const CLEARANCE_LABELS: Record<ClearanceStatus, string> = {
  pending: "Pending Verification",
  bill_generated: "Bill Generated",
  bill_verified: "Bill Verified",
  cleared: "Cleared for Discharge",
  blocked: "Blocked — Outstanding",
};

const CLEARANCE_TONES: Record<ClearanceStatus, string> = {
  pending: "badge--warning",
  bill_generated: "badge--info",
  bill_verified: "badge--info",
  cleared: "badge--success",
  blocked: "badge--danger",
};

function ClearanceBadge({ status }: { status: ClearanceStatus }) {
  return (
    <span className={`badge ${CLEARANCE_TONES[status]}`}>{CLEARANCE_LABELS[status]}</span>
  );
}

export function DischargeVerification({ patientId, onVerified }: DischargeVerificationProps) {
  const qc = useQueryClient();
  const { data: bills, isLoading } = useQuery({
    queryKey: ["rcm-discharge", patientId],
    queryFn: () => rcmJson<Claim[]>(`/api/v1/rcm/patients/${patientId}/bills`),
    enabled: !!patientId,
  });

  const clearMutation = useMutation({
    mutationFn: () => rcmJson<{ success: boolean }>(`/api/v1/rcm/patients/${patientId}/discharge`, { method: "POST" }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["rcm-discharge", patientId] });
      onVerified?.();
    },
  });

  const blockMutation = useMutation({
    mutationFn: () => rcmJson<{ success: boolean }>(`/api/v1/rcm/patients/${patientId}/block-discharge`, { method: "POST" }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["rcm-discharge", patientId] });
    },
  });

  if (isLoading) return <p className="muted">Loading discharge status…</p>;
  if (!bills || bills.length === 0) return <p className="muted">No bills found for this patient.</p>;

  const hasOutstanding = bills.some((b) => b.status === "unpaid" || b.status === "partial");
  const allVerified = bills.every((b) => b.status === "settled" || b.status === "approved");
  const currentStatus: ClearanceStatus = allVerified ? "cleared" : hasOutstanding ? "blocked" : bills.length > 1 ? "bill_verified" : "bill_generated";

  return (
    <section className="card discharge-verification" aria-label="Discharge Verification">
      <header className="card__header">
        <h2>Discharge Verification — Patient ID {patientId}</h2>
        <ClearanceBadge status={currentStatus} />
      </header>

      <div className="discharge-verification__bills">
        <table className="table">
          <thead>
            <tr>
              <th>Bill ID</th>
              <th>Amount</th>
              <th>Status</th>
              <th>Settled</th>
            </tr>
          </thead>
          <tbody>
            {bills.map((bill: Claim) => (
              <tr key={bill.id}>
                <td>#{bill.id}</td>
                <td>₹{bill.amount.toLocaleString("en-IN")}</td>
                <td>{bill.status}</td>
                <td>{bill.status === "settled" || bill.status === "approved" ? "✓" : "✗"}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <footer className="discharge-verification__actions">
        {currentStatus === "cleared" ? (
          <span className="discharge-verification__success">✓ All bills cleared. Patient eligible for discharge.</span>
        ) : (
          <>
            <button
              className="btn btn--primary"
              onClick={() => clearMutation.mutate()}
              disabled={clearMutation.isPending}
            >
              {clearMutation.isPending ? "Verifying…" : "Verify & Clear"}
            </button>
            {hasOutstanding && (
              <button
                className="btn btn--danger"
                onClick={() => blockMutation.mutate()}
                disabled={blockMutation.isPending}
              >
                {blockMutation.isPending ? "Blocking…" : "Block Discharge"}
              </button>
            )}
          </>
        )}
      </footer>
    </section>
  );
}
