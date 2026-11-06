
interface DMApprovalProps {
  claimId: number;
  onApproved?: () => void;
  onRejected?: () => void;
}

export function DMApproval({ claimId }: DMApprovalProps) {
  return (
    <section className="card dm-approval" aria-label="DM Approval Workflow">
      <header className="card__header">
        <h2>DM Approval — Claim #{claimId}</h2>
      </header>
      <p className="muted">Claim detail, approve and reject are not available.</p>
      <footer className="dm-approval__actions">
        <button className="btn btn-primary" disabled>Approve</button>
        <button className="btn btn-secondary" disabled>Reject</button>
      </footer>
    </section>
  );
}
