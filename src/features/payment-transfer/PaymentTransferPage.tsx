import { useState } from "react";
import { TransferRequest } from "./TransferRequest";
import { TransferVerification } from "./TransferVerification";
import { TransferStatus } from "./TransferStatus";

type PaymentTransferView = "request" | "verify" | "status";

/**
 * PaymentTransferPage — M33 Payment Transfer feature container.
 * Orchestrates the transfer workflow: request → verify → status.
 */
export function PaymentTransferPage() {
  const [view, setView] = useState<PaymentTransferView>("request");
  const [activeTransferId, setActiveTransferId] = useState<string | undefined>();

  const handleTransferInitiated = (transferId: string) => {
    setActiveTransferId(transferId);
    setView("verify");
  };

  const handleVerificationSuccess = () => {
    setView("status");
  };

  const handleBackToRequest = () => {
    setView("request");
    setActiveTransferId(undefined);
  };

  return (
    <section className="page">
      <header className="page-header">
        <h1>Payment Transfer (DBT)</h1>
        <nav className="page-header__nav">
          <button
            type="button"
            className={`btn btn--sm${view === "request" ? " btn--primary" : " btn--ghost"}`}
            onClick={() => setView("request")}
          >
            New Transfer
          </button>
          <button
            type="button"
            className={`btn btn--sm${view === "status" ? " btn--primary" : " btn--ghost"}`}
            onClick={() => setView("status")}
          >
            Transfer Status
          </button>
        </nav>
      </header>

      <div className="card">
        {view === "request" && (
          <TransferRequest onTransferInitiated={handleTransferInitiated} />
        )}
        {view === "verify" && activeTransferId && (
          <TransferVerification
            transferId={activeTransferId}
            onSuccess={handleVerificationSuccess}
            onCancel={handleBackToRequest}
            onBack={handleBackToRequest}
          />
        )}
        {view === "status" && (
          <TransferStatus initialTransferId={activeTransferId} />
        )}
      </div>
    </section>
  );
}
