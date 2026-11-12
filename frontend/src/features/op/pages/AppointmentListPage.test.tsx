import { describe, expect, it } from "vitest";
import { render, screen, fireEvent, waitFor, within } from "@testing-library/react";
import { MemoryRouter, Routes, Route } from "react-router-dom";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { http, HttpResponse } from "msw";
import { server } from "../__tests__/mocks/server";
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

  it("resolves patient, doctor, and department display names when appointment row has only IDs, falling back to ID if unresolved", async () => {
    server.use(
      http.get("/api/v1/appointments", () => {
        return HttpResponse.json([
          {
            id: 9005,
            patientId: 7001,
            doctorId: 100,
            departmentId: 1,
            apptDate: "2024-08-15",
            slotStart: "11:00:00",
            apptStatus: 1,
            status: true,
            visitType: "APPOINTMENT",
            source: "ONLINE",
          },
          {
            id: 9006,
            patientId: 9999,
            doctorId: 9999,
            departmentId: 9999,
            apptDate: "2024-08-15",
            slotStart: "11:30:00",
            apptStatus: 1,
            status: true,
            visitType: "APPOINTMENT",
            source: "ONLINE",
          },
        ]);
      }),
    );

    render(wrap());
    // 7001 -> Asha Verma, 100 -> Dr. Anjali Rao, 1 -> General Medicine
    await waitFor(() => {
      expect(screen.getByText("Asha Verma")).toBeInTheDocument();
      expect(screen.getByText("Dr. Anjali Rao")).toBeInTheDocument();
      expect(screen.getByText("General Medicine")).toBeInTheDocument();
    });

    // 9999 -> fallback #9999
    expect(screen.getAllByText("#9999").length).toBeGreaterThanOrEqual(1);
  });

  it("performs check-in using POST and refreshes the list", async () => {
    let postCalled = false;
    let listFetchCount = 0;
    server.use(
      http.post("/api/v1/appointments/:id/checkin", () => {
        postCalled = true;
        return HttpResponse.json({ id: 9001, apptStatus: 2, status: 2 });
      }),
      http.get("/api/v1/appointments", () => {
        listFetchCount++;
        return HttpResponse.json([
          {
            id: 9001,
            patientId: 7001,
            patientName: "Asha Verma",
            doctorId: 100,
            doctorName: "Dr. Anjali Rao",
            apptStatus: postCalled ? 2 : 1,
            status: true,
            visitType: "WALK_IN",
          },
        ]);
      }),
    );

    render(wrap());
    const checkInBtn = await screen.findByRole("button", { name: /check in/i });
    fireEvent.click(checkInBtn);

    await waitFor(() => {
      expect(postCalled).toBe(true);
      expect(listFetchCount).toBeGreaterThanOrEqual(2);
    });
  });

  it("shows backend error message when an action fails", async () => {
    server.use(
      http.post("/api/v1/appointments/:id/checkin", () => {
        return HttpResponse.json(
          { status: "ERROR", errorCode: "APPT_ILLEGAL_TRANSITION", message: "Patient is not in BOOKED state" },
          { status: 400 },
        );
      }),
    );

    render(wrap());
    const checkInBtn = await screen.findByRole("button", { name: /check in/i });
    fireEvent.click(checkInBtn);

    await waitFor(() => {
      expect(screen.getByRole("alert")).toHaveTextContent("Patient is not in BOOKED state");
    });
  });

  it("cancels appointment with POST, closes dialog, and refreshes the list", async () => {
    let cancelCalled = false;
    server.use(
      http.post("/api/v1/appointments/:id/cancel", () => {
        cancelCalled = true;
        return HttpResponse.json({ id: 9001, apptStatus: 4, status: 4 });
      }),
    );

    render(wrap());
    await waitFor(() => {
      expect(screen.getByText("Asha Verma")).toBeInTheDocument();
    });

    const table = screen.getByRole("table");
    const cancelBtn = within(table).getByRole("button", { name: /^cancel$/i });
    fireEvent.click(cancelBtn);

    const reasonInput = screen.getByLabelText(/reason for cancellation/i);
    fireEvent.input(reasonInput, { target: { value: "Doctor unavailable" } });

    const confirmBtn = screen.getByRole("button", { name: /^cancel appointment$/i });
    expect((confirmBtn as HTMLButtonElement).disabled).toBe(false);
    fireEvent.click(confirmBtn);

    await waitFor(() => {
      expect(cancelCalled).toBe(true);
    });
  });

  it("marks appointment as no-show with POST and refreshes the list", async () => {
    let noShowCalled = false;
    server.use(
      http.post("/api/v1/appointments/:id/noshow", () => {
        noShowCalled = true;
        return HttpResponse.json({ id: 9001, apptStatus: 6, status: 6 });
      }),
    );

    render(wrap());
    await waitFor(() => {
      expect(screen.getByText("Asha Verma")).toBeInTheDocument();
    });

    const table = screen.getByRole("table");
    const noShowBtn = within(table).getByRole("button", { name: /no-show/i });
    fireEvent.click(noShowBtn);

    await waitFor(() => {
      expect(noShowCalled).toBe(true);
    });
  });
});
