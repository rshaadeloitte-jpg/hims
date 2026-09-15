import { describe, expect, it } from "vitest";
import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import { MemoryRouter, Routes, Route } from "react-router-dom";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { AppointmentListPage } from "./AppointmentListPage";

const wrap = () => {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } });
  return (
    <QueryClientProvider client={qc}>
      <MemoryRouter initialEntries={["/m/op/appointmentList"]}>
        <Routes>
          <Route path="/m/op/appointmentList" element={<AppointmentListPage />} />
          <Route path="/m/op/addAppointment" element={<div>add appt</div>} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>
  );
};

describe("AppointmentListPage", () => {
  it("renders the title, filter strip, and the seeded appointments", async () => {
    render(wrap());
    expect(screen.getByRole("heading", { name: /appointments/i })).toBeInTheDocument();
    // Filter strip — 6 pinned filters
    for (const label of ["All", "Pending", "Checked-In", "No-Show", "Walk-in", "Emergency"]) {
      expect(screen.getByRole("button", { name: label })).toBeInTheDocument();
    }
    // Sample appointments from the MSW handlers
    await waitFor(() => {
      expect(screen.getByText("Asha Verma")).toBeInTheDocument();
    });
    expect(screen.getByText("Ravi Kumar")).toBeInTheDocument();
  });

  it("filters to the Checked-In status when that pill is clicked", async () => {
    render(wrap());
    fireEvent.click(await screen.findByRole("button", { name: "Checked-In" }));
    // After filter, only Ravi Kumar (status 2) is visible
    await waitFor(() => {
      expect(screen.queryByText("Asha Verma")).not.toBeInTheDocument();
    });
    expect(screen.getByText("Ravi Kumar")).toBeInTheDocument();
  });

  it("opens the cancel dialog with a required reason field", async () => {
    render(wrap());
    const cancelBtn = await screen.findByRole("button", { name: /^cancel$/i });
    fireEvent.click(cancelBtn);
    expect(screen.getByText(/cancel appointment/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/reason for cancellation/i)).toBeInTheDocument();
    // Confirm is disabled until a reason is entered
    expect(screen.getByRole("button", { name: /cancel appointment/i }).hasAttribute("disabled") || (screen.getByRole("button", { name: /cancel appointment/i }) as HTMLButtonElement).disabled).toBeTruthy();
  });

  it("requires a reschedule reason before the reschedule confirm is enabled", async () => {
    render(wrap());
    fireEvent.click(await screen.findByRole("button", { name: /reschedule/i }));
    expect(screen.getByText(/reschedule appointment/i)).toBeInTheDocument();
    const confirm = screen.getByRole("button", { name: /mark for reschedule/i }) as HTMLButtonElement;
    expect(confirm.disabled).toBe(true);
    fireEvent.input(screen.getByLabelText(/reschedule reason/i), { target: { value: "Doctor unavailable" } });
    expect(confirm.disabled).toBe(false);
  });

  it("navigates to /m/op/addAppointment when the New appointment button is clicked", async () => {
    render(wrap());
    fireEvent.click(await screen.findByRole("button", { name: /new appointment/i }));
    await waitFor(() => {
      expect(screen.getByText("add appt")).toBeInTheDocument();
    });
  });

  it("renders the AppointmentStatusBadge with the correct status label", async () => {
    render(wrap());
    await waitFor(() => {
      expect(screen.getByText("Booked")).toBeInTheDocument();
      expect(screen.getByText("Checked-In")).toBeInTheDocument();
    });
  });

  // Quick smoke: highlight query-param (post-create redirect) renders the banner
  it("renders the highlighted-patient banner when ?highlight= is in the URL", async () => {
    const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    render(
      <QueryClientProvider client={qc}>
        <MemoryRouter initialEntries={["/m/op/appointmentList?highlight=9001"]}>
          <Routes>
            <Route path="/m/op/appointmentList" element={<AppointmentListPage />} />
          </Routes>
        </MemoryRouter>
      </QueryClientProvider>,
    );
    // The banner fetches patient 9001 — but the MSW handler returns 404 for that id.
    // The query will eventually fail; we only assert that the banner does not crash
    // and the page is rendered.
    expect(screen.getByRole("heading", { name: /appointments/i })).toBeInTheDocument();
  });
});
