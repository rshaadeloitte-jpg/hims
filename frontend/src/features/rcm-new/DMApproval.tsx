import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { rcmJson } from "./queries";
import type { Claim } from "./queries";
import React from "react";

interface DMApprovalProps {
  claimId: number;
  onApproved?: () => void;
  onRejected?: () => void;
}

type ApprovalStage = "pending" | "dlh_approval" | "dm_approval" | "approved" | "rejected";

const STAGE_LABELS: Record<ApprovalStage, string> = {
  pending: "Pending",
  dlh_approval: "DLH Approval",
  dm_approval: "DM Approval",
  approved: "Approved",
  rejected: "Rejected",
};

const STAGE_TONES: Record<ApprovalStage, string> = {
  pending: "badge--warning",
  dlh_approval: "badge--info",
  dm_approval: "badge--info",
  approved: "badge--success",
  rejected: "badge--danger",
};

const APPROVAL_STAGES: ApprovalStage[] = ["pending", "dlh_approval", "dm_approval", "approved"];

function ApprovalBadge({ stage }: { stage: ApprovalStage }) {
  return (
    <span className={`badge ${STAGE_TONES[stage]}`} title={`RCM-${stage}`}>
      {STAGE_LABELS[stage]}
    </span>
  );
}

function StripeBadge({ approved }: { approved: boolean }) {
  return (
    <span className={`stripe-badge ${approved ? "stripe-badge--approved" : "stripe-badge--pending"}`}>
      {approved ? "✓ Stripe Approved" : "◷ Stripe Pending"}
    </span>
  );
}

export function DMApproval({ claimId, onApproved, onRejected }: DMApprovalProps) {
  const qc = useQueryClient();
  const { data: claim, isLoading } = useQuery({
    queryKey: ["rcm-claim", claimId],
    queryFn: () => rcmJson<Claim>(`/api/v1/rcm/claims/${claimId}`),
    enabled: !!claimId,
  });

  const approveMutation = useMutation({
    mutationFn: () => rcmJson<Claim>(`/api/v1/rcm/claims/${claimId}/approve`, { method: "PATCH" }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["rcm-claims"] });
      qc.invalidateQueries({ queryKey: ["rcm-claim", claimId] });
      onApproved?.();
    },
  });

  const rejectMutation = useMutation({
    mutationFn: () => rcmJson<Claim>(`/api/v1/rcm/claims/${claimId}/reject`, { method: "PATCH" }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["rcm-claims"] });
      qc.invalidateQueries({ queryKey: ["rcm-claim", claimId] });
      onRejected?.();
    },
  });

  if (isLoading) return <p className="muted">Loading approval…</p>;
  if (!claim) return <p className="muted">Claim not found.</p>;

  const currentStage: ApprovalStage = (claim.status?.toLowerCase() as ApprovalStage) ?? "pending";
  const isFinal = currentStage === "approved" || currentStage === "rejected";
  const currentIndex = APPROVAL_STAGES.indexOf(currentStage);
  const isDmStage = currentStage === "dm_approval";
  const isStripeApproved = currentStage === "approved" || currentStage === "dlh_approval";

  return (
    <section className="card dm-approval" aria-label="DM Approval Workflow">
      <header className="card__header">
        <h2>DM Approval — Claim #{claim.id}</h2>
        <ApprovalBadge stage={currentStage} />
      </header>

      <div className="dm-approval__pipeline">
        {APPROVAL_STAGES.map((stage, i) => {
          const isActive = i <= currentIndex;
          const isCurrent = stage === currentStage;
          return (
            <React.Fragment key={stage}>
              <span className={`pipeline-step ${isActive ? "pipeline-step--active" : ""} ${isCurrent ? "pipeline-step--current" : ""}`}>
                {i + 1}. {STAGE_LABELS[stage]}
              </span>
              {i < APPROVAL_STAGES.length - 1 && (
                <span className={`pipeline-arrow ${isActive ? "pipeline-arrow--active" : ""}`}>→</span>
              )}
            </React.Fragment>
          );
        })}
      </div>

      <div className="dm-approval__details">
        <dl className="detail-grid">
          <dt>Patient</dt><dd>{claim.patientName}</dd>
          <dt>Scheme</dt><dd>{claim.scheme}</dd>
          <dt>Amount</dt><dd>₹{claim.amount.toLocaleString("en-IN")}</dd>
          <dt>Submitted</dt><dd>{new Date(claim.submittedAt).toLocaleDateString("en-IN")}</dd>
        </dl>
      </div>

      <StripeBadge approved={isStripeApproved} />

      {isDmStage && !isFinal && (
        <footer className="dm-approval__actions">
          <button
            className="btn btn--primary"
            onClick={() => approveMutation.mutate()}
            disabled={approveMutation.isPending}
          >
            {approveMutation.isPending ? "Approving…" : "Approve"}
          </button>
          <button
            className="btn btn--danger"
            onClick={() => rejectMutation.mutate()}
            disabled={rejectMutation.isPending}
          >
            {rejectMutation.isPending ? "Rejecting…" : "Reject"}
          </button>
        </footer>
      )}
    </section>
  );
}
