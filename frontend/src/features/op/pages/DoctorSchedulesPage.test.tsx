import { describe, expect, it } from "vitest";
import { render, screen, fireEvent, waitFor, within } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { http, HttpResponse } from "msw";
import { server } from "../__tests__/mocks/server";
import { DoctorSchedulesPage } from "./DoctorSchedulesPage";
import "../__tests__/setup";

const wrap = () => {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } });
  return (
    <QueryClientProvider client={qc}>
      <MemoryRouter>
        <DoctorSchedulesPage />
      </MemoryRouter>
    </QueryClientProvider>
  );
};

const form = () => within(screen.getByRole("region", { name: /new schedule/i }));

describe("DoctorSchedulesPage", () => {
  it("lists schedules with doctor names instead of ids", async () => {
    render(wrap());
    expect(await screen.findAllByText(/Anjali/)).not.toHaveLength(0);
    expect(screen.getAllByText(/Maya/).length).toBeGreaterThan(0);
  });

  it("rejects an end time that is not after the start time", async () => {
    render(wrap());
    await screen.findAllByText(/Anjali/);
    const f = form();
    fireEvent.change(f.getByLabelText(/^doctor/i), { target: { value: "100" } });
    fireEvent.change(f.getByLabelText(/day of week/i), { target: { value: "4" } });
    fireEvent.change(f.getByLabelText(/start time/i), { target: { value: "17:00" } });
    fireEvent.change(f.getByLabelText(/end time/i), { target: { value: "09:00" } });
    fireEvent.click(f.getByRole("button", { name: /create schedule/i }));
    expect(await f.findByText(/end time must be after start time/i)).toBeInTheDocument();
  });

  it("creates a schedule with the backend's query parameters", async () => {
    let captured: URL | null = null;
    server.use(
      http.post("/api/v1/doctors/:id/schedules", ({ request }) => {
        captured = new URL(request.url);
        return HttpResponse.json({ id: 99, doctorId: 100, dayOfWeek: 4, startTime: "09:00:00", endTime: "13:00:00", slotMinutes: 15, status: true }, { status: 201 });
      }),
    );
    render(wrap());
    await screen.findAllByText(/Anjali/);
    const f = form();
    fireEvent.change(f.getByLabelText(/^doctor/i), { target: { value: "100" } });
    fireEvent.change(f.getByLabelText(/day of week/i), { target: { value: "4" } });
    fireEvent.change(f.getByLabelText(/start time/i), { target: { value: "09:00" } });
    fireEvent.change(f.getByLabelText(/end time/i), { target: { value: "13:00" } });
    fireEvent.click(f.getByRole("button", { name: /create schedule/i }));
    expect(await screen.findByText(/schedule created/i)).toBeInTheDocument();
    await waitFor(() => expect(captured).not.toBeNull());
    const url = captured as unknown as URL;
    expect(url.pathname).toBe("/api/v1/doctors/100/schedules");
    expect(url.searchParams.get("dayOfWeek")).toBe("4");
    expect(url.searchParams.get("startTime")).toBe("09:00:00");
    expect(url.searchParams.get("endTime")).toBe("13:00:00");
    expect(url.searchParams.get("unitId")).toBeTruthy();
  });
});
