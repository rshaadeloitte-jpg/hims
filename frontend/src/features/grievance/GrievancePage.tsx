import { useState } from "react";
import { GrievanceForm } from "./GrievanceForm";
import { GrievanceBoard } from "./GrievanceBoard";
import { FeedbackForm } from "./FeedbackForm";
import {
  useCategories,
  useSeverities,
  useCreateGrievance,
  useSubmitFeedback,
  type GrievanceTicket,
} from "./queries";

type View = "board" | "raise" | "feedback";

export function GrievancePage() {
  const [view, setView] = useState<View>("board");
  const [selectedTicket, setSelectedTicket] = useState<GrievanceTicket | null>(null);

  const { data: categories = [] } = useCategories();
  const { data: severities = [] } = useSeverities();
  const createGrievance = useCreateGrievance();
  const submitFeedback = useSubmitFeedback();

  const handleRaise = (values: Parameters<typeof createGrievance.mutate>[0]) => {
    createGrievance.mutate(values, {
      onSuccess: () => {
        setView("board");
      },
    });
  };

  const handleFeedback = (values: Parameters<typeof submitFeedback.mutate>[0]["payload"]) => {
    if (!selectedTicket) return;
    submitFeedback.mutate(
      { id: selectedTicket.id, payload: values },
      { onSuccess: () => { setSelectedTicket(null); setView("board"); } },
    );
  };

  return (
    <div className="page">
      <header className="page-header">
        <div>
          <h1>Grievance Management</h1>
          <p className="muted">M22 — raise, track, and resolve grievances</p>
        </div>
        <nav className="toolbar-row">
          <button
            className={`btn ${view === "board" ? "btn-primary" : "btn-secondary"}`}
            onClick={() => setView("board")}
          >
            Board
          </button>
          <button
            className={`btn ${view === "raise" ? "btn-primary" : "btn-secondary"}`}
            onClick={() => setView("raise")}
          >
            Raise Ticket
          </button>
        </nav>
      </header>

      {createGrievance.isError && (
        <div className="alert alert--error" role="alert">
          Failed to submit grievance. Please try again.
        </div>
      )}

      {submitFeedback.isError && (
        <div className="alert alert--error" role="alert">
          Failed to submit feedback. Please try again.
        </div>
      )}

      {view === "board" && (
        <GrievanceBoard
          onSelectTicket={(t) => {
            setSelectedTicket(t);
            if (t.issueStatus === 3) {
              setView("feedback");
            }
          }}
        />
      )}

      {view === "raise" && (
        <div className="card">
          <h2>Raise a Grievance</h2>
          <GrievanceForm
            categories={categories}
            severities={severities}
            submitting={createGrievance.isPending}
            onSubmit={handleRaise}
            onCancel={() => setView("board")}
          />
        </div>
      )}

      {view === "feedback" && selectedTicket && (
        <div className="card">
          <h2>Feedback for {selectedTicket.ticketNo}</h2>
          <p className="muted">Help us improve by rating your experience.</p>
          <FeedbackForm
            grievanceId={selectedTicket.id}
            submitting={submitFeedback.isPending}
            onSubmit={handleFeedback}
            onCancel={() => { setSelectedTicket(null); setView("board"); }}
          />
        </div>
      )}
    </div>
  );
}
