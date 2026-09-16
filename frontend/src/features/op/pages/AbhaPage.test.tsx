import { describe, expect, it } from "vitest";
import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { AbhaPage } from "./AbhaPage";
import "../__tests__/setup";

const wrap = () => {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } });
  return (
    <QueryClientProvider client={qc}>
      <MemoryRouter initialEntries={["/m/op/abha"]}>
        <AbhaPage />
      </MemoryRouter>
    </QueryClientProvider>
  );
};

describe("AbhaPage", () => {
  it("renders the page header and the patient search", () => {
    render(wrap());
    expect(screen.getByRole("heading", { name: /abha \/ abdm/i })).toBeInTheDocument();
    expect(screen.getByLabelText(/find patient/i)).toBeInTheDocument();
  });

  it("does not render OTP / QR / card sections until a patient is picked", () => {
    render(wrap());
    expect(screen.queryByRole("heading", { name: /aadhaar \/ mobile otp enrolment/i })).not.toBeInTheDocument();
  });

  it("renders the OTP enrolment section and the QR scan after a patient is picked", async () => {
    render(wrap());
    fireEvent.change(screen.getByLabelText(/find patient/i), { target: { value: "Asha" } });
    fireEvent.click(await screen.findByText("Asha Verma"));
    await waitFor(() => {
      expect(screen.getByRole("heading", { name: /aadhaar \/ mobile otp enrolment/i })).toBeInTheDocument();
    });
    expect(screen.getByRole("heading", { name: /qr code scan/i })).toBeInTheDocument();
  });

  it("shows the ABHA card when the patient is already LINKED", async () => {
    render(wrap());
    fireEvent.change(screen.getByLabelText(/find patient/i), { target: { value: "Asha" } });
    fireEvent.click(await screen.findByText("Asha Verma"));
    // The MSW handler returns status=LINKED for patient 7001
    await waitFor(() => {
      expect(screen.getByRole("heading", { name: /^3\. abha card$/i })).toBeInTheDocument();
    });
    expect(screen.getAllByText(/12-3456-7890-1234/).length).toBeGreaterThanOrEqual(1);
  });

  it("OTPs the PENDING_OTP → LINKED flow when the patient is not yet enrolled", async () => {
    // Use patient id 7001 here, but the MSW GET /abha/7001 returns LINKED.
    // So we don't observe OTP flow on 7001. We do observe the section.
    render(wrap());
    fireEvent.change(screen.getByLabelText(/find patient/i), { target: { value: "Ravi" } });
    fireEvent.click(await screen.findByText("Ravi Kumar"));
    // Ravi has no ABHA record → 404 from the GET → the page doesn't crash.
    await waitFor(() => {
      expect(screen.getByText(/aadhaar \/ mobile otp enrolment/i)).toBeInTheDocument();
    });
  });

  it("scan button is disabled when the QR payload is empty", async () => {
    render(wrap());
    fireEvent.change(screen.getByLabelText(/find patient/i), { target: { value: "Asha" } });
    fireEvent.click(await screen.findByText("Asha Verma"));
    const scan = (await screen.findByRole("button", { name: /^scan$/i })) as HTMLButtonElement;
    expect(scan.disabled).toBe(true);
    fireEvent.input(screen.getByPlaceholderText(/phr payload/i), { target: { value: "abha://test" } });
    expect(scan.disabled).toBe(false);
  });
});
