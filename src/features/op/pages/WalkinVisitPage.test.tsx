import { describe, expect, it } from "vitest";
import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import { MemoryRouter, Routes, Route } from "react-router-dom";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { WalkinVisitPage } from "./WalkinVisitPage";

const wrap = () => {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } });
  return (
    <QueryClientProvider client={qc}>
      <MemoryRouter initialEntries={["/m/op/walkinVisit"]}>
        <Routes>
          <Route path="/m/op/walkinVisit" element={<WalkinVisitPage />} />
          <Route path="/m/op/appointmentList" element={<div>appt list</div>} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>
  );
};

describe("WalkinVisitPage", () => {
  it("renders the page header and the patient search", () => {
    render(wrap());
    expect(screen.getByRole("heading", { name: /walk-in visit/i })).toBeInTheDocument();
    expect(screen.getByLabelText(/find patient/i)).toBeInTheDocument();
  });

  it("does not render the demographic form until a patient is picked or register-new is clicked", () => {
    render(wrap());
    expect(screen.queryByText(/^Demographics$/i)).not.toBeInTheDocument();
  });

  it("renders the demographic form when the user clicks register-new", async () => {
    render(wrap());
    fireEvent.change(screen.getByLabelText(/find patient/i), { target: { value: "missing-patient" } });
    const newBtn = await screen.findByRole("button", { name: /register new patient/i });
    fireEvent.click(newBtn);
    await waitFor(() => {
      expect(screen.getByText(/^Demographics$/i)).toBeInTheDocument();
    });
  });

  it("picks an existing hit and pre-fills the demographic fields", async () => {
    render(wrap());
    fireEvent.change(screen.getByLabelText(/find patient/i), { target: { value: "Asha" } });
    const hit = await screen.findByText("Asha Verma");
    fireEvent.click(hit);
    const lastName = await screen.findByLabelText(/last name/i) as HTMLInputElement;
    expect(lastName.value).toBe("Verma");
    const firstName = screen.getByLabelText(/first name/i) as HTMLInputElement;
    expect(firstName.value).toBe("Asha");
  });

  it("renders the MLC toggle and the sponsor picker on the form", async () => {
    render(wrap());
    fireEvent.change(screen.getByLabelText(/find patient/i), { target: { value: "missing-patient" } });
    fireEvent.click(await screen.findByRole("button", { name: /register new patient/i }));
    await waitFor(() => expect(screen.getByText(/^Demographics$/i)).toBeInTheDocument());
    expect(screen.getByLabelText(/^this is a medico-legal case/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/sponsor \/ scheme/i)).toBeInTheDocument();
  });

  it("blocks the submit button when DOB and age are both blank (BR-PAT-001 parity)", async () => {
    render(wrap());
    fireEvent.change(screen.getByLabelText(/find patient/i), { target: { value: "missing-patient" } });
    fireEvent.click(await screen.findByRole("button", { name: /register new patient/i }));
    await waitFor(() => expect(screen.getByText(/^Demographics$/i)).toBeInTheDocument());
    // Fill names but leave age + DOB empty
    fireEvent.input(screen.getByLabelText(/first name/i), { target: { value: "A" } });
    fireEvent.input(screen.getByLabelText(/last name/i), { target: { value: "B" } });
    fireEvent.click(screen.getByRole("button", { name: /register & issue token/i }));
    await waitFor(() => {
      expect(screen.getByText(/either dob or age is required/i)).toBeInTheDocument();
    });
  });

  it("submits a new patient, issues a token, and routes to appointmentList", async () => {
    render(wrap());
    fireEvent.change(screen.getByLabelText(/find patient/i), { target: { value: "missing-patient" } });
    fireEvent.click(await screen.findByRole("button", { name: /register new patient/i }));
    await waitFor(() => expect(screen.getByText(/^Demographics$/i)).toBeInTheDocument());
    fireEvent.input(screen.getByLabelText(/first name/i), { target: { value: "Asha" } });
    fireEvent.input(screen.getByLabelText(/last name/i), { target: { value: "Verma" } });
    fireEvent.input(screen.getByLabelText(/age/i), { target: { value: "33" } });
    fireEvent.click(screen.getByRole("button", { name: /register & issue token/i }));
    // The token-issued dialog shows the new token
    await waitFor(() => {
      expect(screen.getByText(/token issued/i)).toBeInTheDocument();
    });
    expect(screen.getByText("OPD-1")).toBeInTheDocument();
    // The Continue button routes to appointmentList
    fireEvent.click(screen.getByRole("button", { name: /continue/i }));
    await waitFor(() => {
      expect(screen.getByText("appt list")).toBeInTheDocument();
    });
  });
});
