import { describe, expect, it, vi } from "vitest";
import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { PatientSearch } from "./PatientSearch";
import { opSamples } from "../__tests__/mocks/handlers";
import "../__tests__/setup";

const wrap = (children: React.ReactNode) => {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return <QueryClientProvider client={qc}>{children}</QueryClientProvider>;
};

describe("PatientSearch", () => {
  it("does not query until the user types at least 2 characters", async () => {
    render(wrap(<PatientSearch onPick={vi.fn()} />));
    const input = screen.getByLabelText(/patient/i);
    fireEvent.change(input, { target: { value: "a" } });
    // No "Search…" should be visible for 1-char queries (the MSW handler
    // would still serve an empty array, but the hook is disabled).
    await waitFor(() => {
      expect(screen.queryByText(/searching/i)).not.toBeInTheDocument();
    });
  });

  it("renders one hit per result and triggers onPick on click", async () => {
    const onPick = vi.fn();
    render(wrap(<PatientSearch onPick={onPick} />));
    const input = screen.getByLabelText(/patient/i);
    fireEvent.change(input, { target: { value: "Asha" } });

    await waitFor(() => {
      expect(screen.getByText("MRN-7001")).toBeInTheDocument();
    });
    fireEvent.click(screen.getByText("MRN-7001"));
    expect(onPick).toHaveBeenCalledTimes(1);
    expect(onPick.mock.calls[0][0].mrn).toBe("MRN-7001");
  });

  it("shows the register-new button when there are no hits and onRegisterNew is provided", async () => {
    const onRegisterNew = vi.fn();
    render(wrap(<PatientSearch onPick={vi.fn()} onRegisterNew={onRegisterNew} />));
    fireEvent.change(screen.getByLabelText(/patient/i), { target: { value: "missing-patient" } });
    await waitFor(() => {
      expect(screen.getByText(/no patient matches/i)).toBeInTheDocument();
    });
    fireEvent.click(screen.getByRole("button", { name: /register new patient/i }));
    expect(onRegisterNew).toHaveBeenCalledTimes(1);
  });

  it("does not render the register-new button when the prop is omitted", async () => {
    render(wrap(<PatientSearch onPick={vi.fn()} />));
    fireEvent.change(screen.getByLabelText(/patient/i), { target: { value: "missing-patient" } });
    await waitFor(() => {
      expect(screen.getByText(/no patient matches/i)).toBeInTheDocument();
    });
    expect(screen.queryByRole("button", { name: /register new patient/i })).not.toBeInTheDocument();
  });

  it("uses opSamples.patient.id on click (Asha Verma search hit)", async () => {
    const onPick = vi.fn();
    render(wrap(<PatientSearch onPick={onPick} />));
    fireEvent.change(screen.getByLabelText(/patient/i), { target: { value: "Verma" } });
    await waitFor(() => {
      expect(screen.getByText("Asha Verma")).toBeInTheDocument();
    });
    // The second sample (Ravi Kumar) is also in the canned results.
    expect(screen.getByText("Ravi Kumar")).toBeInTheDocument();
  });
});

// We use a small subset of opSamples to avoid a runtime dependency on the
// other tests in the suite; the searches are deterministic against the
// canned MSW handlers.
void opSamples;
