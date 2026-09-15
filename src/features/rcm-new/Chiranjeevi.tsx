import { useState } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { rcmJson } from "./queries";

interface ChiranjeeviScheme {
  id: number;
  patientName: string;
  patientId: number;
  schemeName: string;
  eligibilityDate: string;
  approvalStatus: "pending" | "under_review" | "approved" | "rejected";
  amount: number;
}

interface ChiranjeeviProps {
  onApproved?: (scheme: ChiranjeeviScheme) => void;
  onRejected?: (schemeId: number) => void;
}

type SchemeStatus = "pending" | "under_review" | "approved" | "rejected";

const SCHEME_LABELS: Record<SchemeStatus, string> = {
  pending: "Pending",
  under_review: "Under Review",
  approved: "Approved",
  rejected: "Rejected",
};

const SCHEME_TONES: Record<SchemeStatus, string> = {
  pending: "badge--warning",
  under_review: "badge--info",
  approved: "badge--success",
  rejected: "badge--danger",
};

function SchemeBadge({ status }: { status: SchemeStatus }) {
  return (
    <span className={`badge ${SCHEME_TONES[status]}`}>{SCHEME_LABELS[status]}</span>
  );
}

const APPROVAL_STAGES: SchemeStatus[] = ["pending", "under_review", "approved"];

export function Chiranjeevi({ onApproved, onRejected }: ChiranjeeviProps) {
  const qc = useQueryClient();
  const { data: schemes, isLoading } = useQuery({
    queryKey: ["rcm-chiranjeevi"],
    queryFn: () => rcmJson<ChiranjeeviScheme[]>(`/api/v1/rcm/chiranjeevi/schemes`),
  });

  const approveMutation = useMutation({
    mutationFn: (schemeId: number) =>
      rcmJson<ChiranjeeviScheme>(`/api/v1/rcm/chiranjeevi/schemes/${schemeId}/approve`, { method: "PATCH" }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["rcm-chiranjeevi"] });
      onApproved?.(schemes?.[0]!);
    },
  });

  const rejectMutation = useMutation({
    mutationFn: (schemeId: number) =>
      rcmJson<ChiranjeeviScheme>(`/api/v1/rcm/chiranjeevi/schemes/${schemeId}/reject`, { method: "PATCH" }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["rcm-chiranjeevi"] });
      onRejected?.(schemes?.[0]?.id!);
    },
  });

  if (isLoading) return <p className="muted">Loading Chiranjeevi schemes…</p>;
  if (!schemes || schemes.length === 0) return <p className="muted">No schemes found.</p>;

  return (
    <section className="card chiranjeevi" aria-label="Chiranjeevi Scheme Approval">
      <header className="card__header">
        <h2>Chiranjeevi Scheme Approval</h2>
        <span className="badge badge--info">{schemes.length} Scheme(s)</span>
      </header>

      <div className="chiranjeevi__list">
        {schemes.map((scheme) => {
          const currentIndex = APPROVAL_STAGES.indexOf(scheme.approvalStatus);
          const isApproved = scheme.approvalStatus === "approved";
          const isRejected = scheme.approvalStatus === "rejected";
          const isFinal = isApproved || isRejected;

          return (
            <div key={scheme.id} className={`chiranjeevi__card ${isFinal ? "chiranjeevi__card--final" : ""}`}>
              <div className="chiranjeevi__card-header">
                <span className="chiranjeevi__patient">{scheme.patientName}</span>
                <SchemeBadge status={scheme.approvalStatus} />
              </div>

              <div className="chiranjeevi__pipeline">
                {APPROVAL_STAGES.map((stage, i) => {
                  const isActive = i <= currentIndex;
                  const isCurrent = stage === scheme.approvalStatus;
                  return (
                    <span key={stage} className={`pipeline-step ${isActive ? "pipeline-step--active" : ""} ${isCurrent ? "pipeline-step--current" : ""}`}>
                      {i + 1}. {SCHEME_LABELS[stage]}
                    </span>
                  );
                })}
              </div>

              <dl className="detail-grid">
                <dt>Patient ID</dt><dd>{scheme.patientId}</dd>
                <dt>Amount</dt><dd>₹{scheme.amount.toLocaleString("en-IN")}</dd>
                <dt>Eligibility</dt><dd>{new Date(scheme.eligibilityDate).toLocaleDateString("en-IN")}</dd>
              </dl>

              {!isFinal && (
                <footer className="chiranjeevi__actions">
                  <button
                    className="btn btn--primary"
                    onClick={() => approveMutation.mutate(scheme.id)}
                    disabled={approveMutation.isPending}
                  >
                    {approveMutation.isPending ? "Approving…" : "Approve"}
                  </button>
                  <button
                    className="btn btn--danger"
                    onClick={() => rejectMutation.mutate(scheme.id)}
                    disabled={rejectMutation.isPending}
                  >
                    {rejectMutation.isPending ? "Rejecting…" : "Reject"}
                  </button>
                </footer>
              )}

              {isApproved && <span className="chiranjeevi__success">✓ Scheme approved and enrolled.</span>}
              {isRejected && <span className="chiranjeevi__rejected">✗ Scheme rejected.</span>}
            </div>
          );
        })}
      </div>
    </section>
  );
}
