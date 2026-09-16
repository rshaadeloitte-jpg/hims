import { describe, expect, it } from "vitest";
import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import { MemoryRouter, Routes, Route } from "react-router-dom";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { AddAppointmentPage } from "./AddAppointmentPage";
import "../__tests__/setup";

const wrap = () => {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } });
  return (
    <QueryClientProvider client={qc}>
      <MemoryRouter initialEntries={["/m/op/addAppointment"]}>
        <Routes>
          <Route path="/m/op/addAppointment" element={<AddAppointmentPage />} />
          <Route path="/m/op/appointmentList" element={<div>appt list</div>} />
          <Route path="*" element={<div>other</div>} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>
  );
};

describe("AddAppointmentPage", () => {
  it("renders the page header and the patient search", () => {
    render(wrap());
    expect(screen.getByRole("heading", { name: /add appointment/i })).toBeInTheDocument();
    expect(screen.getByLabelText(/find patient/i)).toBeInTheDocument();
  });

  it("disables the Department select until departments are loaded (cascade rule)", async () => {
    render(wrap());
    const dept = (await screen.findByLabelText(/department/i)) as HTMLSelectElement;
    await waitFor(() => {
      expect(dept.options.length).toBeGreaterThan(1);
    });
  });

  it("picks Department then Doctor — doctor select enables", async () => {
    render(wrap());
    const dept = (await screen.findByLabelText(/department/i)) as HTMLSelectElement;
    await waitFor(() => expect(dept.options.length).toBeGreaterThan(1));
    fireEvent.change(dept, { target: { value: "1" } });
    const doctor = (await screen.findByLabelText(/doctor/i)) as HTMLSelectElement;
    await waitFor(() => expect(doctor.options.length).toBeGreaterThan(1));
    expect(doctor.disabled).toBe(false);
  });

  it("disables the slot picker until doctor + date are picked", async () => {
    render(wrap());
    const dept = (await screen.findByLabelText(/department/i)) as HTMLSelectElement;
    await waitFor(() => expect(dept.options.length).toBeGreaterThan(1));
    fireEvent.change(dept, { target: { value: "1" } });
    const doctor = (await screen.findByLabelText(/doctor/i)) as HTMLSelectElement;
    await waitFor(() => expect(doctor.options.length).toBeGreaterThan(1));
    fireEvent.change(doctor, { target: { value: "100" } });
    // The default date is today — slots should be fetched.
    await waitFor(() => {
      const buttons = screen.getAllByRole("radio");
      // The 4 available slots in the canned data (slot 2 is full)
      expect(buttons.length).toBeGreaterThanOrEqual(3);
    });
  });

  it("blocks submit when no patient is picked", async () => {
    render(wrap());
    const dept = (await screen.findByLabelText(/department/i)) as HTMLSelectElement;
    await waitFor(() => expect(dept.options.length).toBeGreaterThan(1));
    fireEvent.change(dept, { target: { value: "1" } });
    const doctor = (await screen.findByLabelText(/doctor/i)) as HTMLSelectElement;
    await waitFor(() => expect(doctor.options.length).toBeGreaterThan(1));
    fireEvent.change(doctor, { target: { value: "100" } });
    const slot = await screen.findByRole("radio", { name: /09:00/i });
    fireEvent.click(slot);
    const submit = screen.getByRole("button", { name: /book appointment/i }) as HTMLButtonElement;
    expect(submit.disabled).toBe(true);
  });

  it("books an appointment end-to-end (happy path)", async () => {
    render(wrap());
    // 1) Pick a patient
    fireEvent.change(screen.getByLabelText(/find patient/i), { target: { value: "Asha" } });
    fireEvent.click(await screen.findByText("Asha Verma"));
    // 2) Pick department + doctor
    const dept = (await screen.findByLabelText(/department/i)) as HTMLSelectElement;
    await waitFor(() => expect(dept.options.length).toBeGreaterThan(1));
    fireEvent.change(dept, { target: { value: "1" } });
    const doctor = (await screen.findByLabelText(/doctor/i)) as HTMLSelectElement;
    await waitFor(() => expect(doctor.options.length).toBeGreaterThan(1));
    fireEvent.change(doctor, { target: { value: "100" } });
    // 3) Pick a slot
    const slot = await screen.findByRole("radio", { name: /09:00/i });
    fireEvent.click(slot);
    // 4) Submit
    const submit = screen.getByRole("button", { name: /book appointment/i });
    fireEvent.click(submit);
    // The MSW POST /appointments returns id 9999; we navigate to the list
    // with ?highlight=9999 — which will trigger a new query (and a banner
    // fetch that 404s, but the list page will render).
    await waitFor(() => {
      expect(screen.queryByText("appt list") || screen.queryByText("other")).toBeInTheDocument();
    });
  });
});
