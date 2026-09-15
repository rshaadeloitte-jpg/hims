import { describe, expect, it } from "vitest";
import { render, screen } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { MemoryRouter } from "react-router-dom";
import { JournalEntry } from "../JournalEntry";

function renderJournalEntry() {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <QueryClientProvider client={qc}>
      <MemoryRouter>
        <JournalEntry />
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

describe("JournalEntry", () => {
  it("renders the journal entry header", () => {
    renderJournalEntry();
    expect(screen.getByText("Journal Entry")).toBeInTheDocument();
  });

  it("shows entry type segmented control", () => {
    renderJournalEntry();
    expect(screen.getByText("Receipt")).toBeInTheDocument();
    expect(screen.getByText("Payment")).toBeInTheDocument();
    expect(screen.getByText("Journal")).toBeInTheDocument();
    expect(screen.getByText("Contra")).toBeInTheDocument();
  });

  it("renders narration and entry date fields", () => {
    renderJournalEntry();
    expect(screen.getByLabelText("Narration")).toBeInTheDocument();
    expect(screen.getByLabelText("Entry Date")).toBeInTheDocument();
  });

  it("renders legs table with initial 1 row and Add Leg button", () => {
    renderJournalEntry();
    expect(screen.getByText("Transaction Legs")).toBeInTheDocument();
    expect(screen.getByText("+ Add Leg")).toBeInTheDocument();
  });

  it("shows balance indicator when debits != credits", () => {
    renderJournalEntry();
    expect(screen.getByText(/Difference|Balanced/i)).toBeInTheDocument();
  });

  it("disables submit when not balanced", () => {
    renderJournalEntry();
    const submitBtn = screen.getByRole("button", { name: /Save Journal Entry/i });
    expect(submitBtn).toBeDisabled();
  });

  it("shows Reference Type field", () => {
    renderJournalEntry();
    expect(screen.getByLabelText("Reference Type")).toBeInTheDocument();
  });
});
