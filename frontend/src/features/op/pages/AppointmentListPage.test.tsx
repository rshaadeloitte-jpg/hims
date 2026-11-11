import { describe, expect, it } from "vitest";
import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import { MemoryRouter, Routes, Route } from "react-router-dom";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { AppointmentListPage } from "./AppointmentListPage";
import "../__tests__/setup";

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
    expect(screen.getAllByText(/cancel appointment/i).length).toBeGreaterThanOrEqual(1);
    expect(screen.getByLabelText(/reason for cancellation/i)).toBeInTheDocument();
    // Confirm is disabled until a reason is entered
    const confirmBtn = screen.getAllByRole("button", { name: /cancel appointment/i })[0];
    expect(confirmBtn.hasAttribute("disabled") || (confirmBtn as HTMLButtonElement).disabled).toBeTruthy();
  });

  it("requires a reschedule reason before the reschedule confirm is enabled", async () => {
    render(wrap());
    fireEvent.click(await screen.findByRole("button", { name: /reschedule/i }));
    expect(screen.getAllByText(/reschedule appointment/i).length).toBeGreaterThanOrEqual(1);
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
      expect(screen.getAllByText("Booked").length).toBeGreaterThanOrEqual(1);
      expect(screen.getAllByText("Checked-In").length).toBeGreaterThanOrEqual(1);
    });
  });

  // Highlight query-param (post-create redirect) renders the banner with real patient data
  it("renders the highlighted-patient banner with real data when ?highlight= is in the URL", async () => {
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
    expect(screen.getByRole("heading", { name: /appointments/i })).toBeInTheDocument();
    await waitFor(() => {
      expect(screen.getByText(/Just created appointment #9001/)).toBeInTheDocument();
      expect(screen.getAllByText("Asha Verma").length).toBeGreaterThanOrEqual(1);
      expect(screen.getAllByText("MRN-7001").length).toBeGreaterThanOrEqual(1);
    });
  });

  it("handles plain array API responses and correctly maps fields and filters", async () => {
    render(wrap());
    // Both seeded appointments should be rendered from the plain array
    await waitFor(() => {
      expect(screen.getByText("Asha Verma")).toBeInTheDocument();
      expect(screen.getByText("Ravi Kumar")).toBeInTheDocument();
    });

    // Token numbers from tokenNo / tokenNumber
    expect(screen.getByText("OPD-1")).toBeInTheDocument();

    // Filter to Walk-in
    fireEvent.click(screen.getByRole("button", { name: "Walk-in" }));
    await waitFor(() => {
      expect(screen.getByText("Asha Verma")).toBeInTheDocument();
      expect(screen.queryByText("Ravi Kumar")).not.toBeInTheDocument();
    });

    // Filter to Pending (apptStatus 1)
    fireEvent.click(screen.getByRole("button", { name: "Pending" }));
    await waitFor(() => {
      expect(screen.getByText("Asha Verma")).toBeInTheDocument();
      expect(screen.queryByText("Ravi Kumar")).not.toBeInTheDocument();
    });

    // Filter to Checked-In (apptStatus 2)
    fireEvent.click(screen.getByRole("button", { name: "Checked-In" }));
    await waitFor(() => {
      expect(screen.queryByText("Asha Verma")).not.toBeInTheDocument();
      expect(screen.getByText("Ravi Kumar")).toBeInTheDocument();
    });
  });
});
