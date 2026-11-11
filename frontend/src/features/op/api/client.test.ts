import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { appointmentApi } from "./client";
import { deriveDoctorsFromSchedules, normalizeSlots } from "./queries";
import type { DoctorSchedule } from "./types";

const fetchMock = vi.fn();

beforeEach(() => {
  vi.stubGlobal("fetch", fetchMock);
});

afterEach(() => {
  vi.unstubAllGlobals();
  fetchMock.mockClear();
});

describe("op client departments", () => {
  it("calls the real URL with the unitId", async () => {
    fetchMock.mockResolvedValue({
      ok: true,
      status: 200,
      json: async () => [],
    });
    await appointmentApi.departments(42);
    expect(String(fetchMock.mock.calls[0][0])).toBe(
      "http://localhost:3000/api/v1/masters/departmentmaster/all?unitId=42",
    );
  });

  it("calls /all with no param when there is no unit", async () => {
    fetchMock.mockResolvedValue({
      ok: true,
      status: 200,
      json: async () => [],
    });
    await appointmentApi.departments(null);
    expect(String(fetchMock.mock.calls[0][0])).toBe(
      "http://localhost:3000/api/v1/masters/departmentmaster/all",
    );
  });
});

describe("op client doctor schedules and slots", () => {
  it("calls /api/v1/doctors/schedules with departmentId filter", async () => {
    fetchMock.mockResolvedValue({
      ok: true,
      status: 200,
      json: async () => [],
    });
    await appointmentApi.doctorSchedules(1);
    expect(String(fetchMock.mock.calls[0][0])).toBe(
      "http://localhost:3000/api/v1/doctors/schedules?departmentId=1",
    );
  });

  it("calls slots endpoint with doctorId and date param", async () => {
    fetchMock.mockResolvedValue({
      ok: true,
      status: 200,
      json: async () => [],
    });
    await appointmentApi.slots(100, "2026-11-12");
    expect(String(fetchMock.mock.calls[0][0])).toBe(
      "http://localhost:3000/api/v1/doctors/100/slots?date=2026-11-12",
    );
  });

  it("sends booking payload with apptDate and slotStart", async () => {
    fetchMock.mockResolvedValue({
      ok: true,
      status: 201,
      json: async () => ({ id: 501 }),
    });
    await appointmentApi.create({
      patientId: 10,
      doctorId: 100,
      scheduleId: 5,
      departmentId: 1,
      apptDate: "2026-11-12",
      slotStart: "09:00:00",
    });
    expect(String(fetchMock.mock.calls[0][0])).toBe(
      "http://localhost:3000/api/v1/appointments",
    );
    const body = JSON.parse(fetchMock.mock.calls[0][1].body);
    expect(body.patientId).toBe(10);
    expect(body.doctorId).toBe(100);
    expect(body.scheduleId).toBe(5);
    expect(body.apptDate).toBe("2026-11-12");
    expect(body.slotStart).toBe("09:00:00");
  });
});

describe("department → doctor mapping (deriveDoctorsFromSchedules)", () => {
  it("derives unique doctors and resolves names from doctor master", () => {
    const schedules: DoctorSchedule[] = [
      { id: 1, doctorId: 100, departmentId: 1, dayOfWeek: 1, startTime: "09:00", endTime: "12:00", status: true },
      { id: 2, doctorId: 100, departmentId: 1, dayOfWeek: 3, startTime: "09:00", endTime: "12:00", status: true }, // duplicate doctor on another day
      { id: 3, doctorId: 101, departmentId: 1, dayOfWeek: 2, startTime: "14:00", endTime: "17:00", status: true },
    ];

    const doctorMasters = [
      { id: 100, firstName: "Anita", lastName: "Sharma" },
      { id: 101, firstName: "Vikram", lastName: "Singh" },
    ];

    const doctors = deriveDoctorsFromSchedules(schedules, doctorMasters);
    expect(doctors).toHaveLength(2);
    expect(doctors[0]).toEqual({
      id: 100,
      name: "Dr. Anita Sharma",
      departmentId: 1,
      isActive: true,
    });
    expect(doctors[1]).toEqual({
      id: 101,
      name: "Dr. Vikram Singh",
      departmentId: 1,
      isActive: true,
    });
  });

  it("uses inline doctor name if present in schedule row", () => {
    const schedules = [
      { id: 1, doctorId: 100, departmentId: 1, dayOfWeek: 1, startTime: "09:00", endTime: "12:00", name: "Dr. Custom Name" },
    ] as unknown as DoctorSchedule[];

    const doctors = deriveDoctorsFromSchedules(schedules);
    expect(doctors).toHaveLength(1);
    expect(doctors[0].name).toBe("Dr. Custom Name");
  });

  it("falls back to Doctor #id if not found in doctor master", () => {
    const schedules: DoctorSchedule[] = [
      { id: 1, doctorId: 999, departmentId: 1, dayOfWeek: 1, startTime: "09:00", endTime: "12:00" },
    ];

    const doctors = deriveDoctorsFromSchedules(schedules, []);
    expect(doctors).toHaveLength(1);
    expect(doctors[0].name).toBe("Doctor #999");
  });

  it("normalizes backend slot DTO into displayable slot", () => {
    const backendSlots = [
      {
        scheduleId: 10,
        doctorId: 100,
        departmentId: 1,
        date: "2026-11-12",
        slotStart: "09:00:00",
        slotEnd: "09:15:00",
        cabin: "Cabin A",
        capacity: 1,
        bookedCount: 0,
        booked: false,
      },
    ] as any;

    const normalized = normalizeSlots(backendSlots);
    expect(normalized).toHaveLength(1);
    expect(normalized[0].startAt).toBe("2026-11-12T09:00:00");
    expect(normalized[0].endAt).toBe("2026-11-12T09:15:00");
    expect(normalized[0].available).toBe(true);
    expect(normalized[0].scheduleId).toBe(10);
    expect(normalized[0].cabin).toBe("Cabin A");
  });
});