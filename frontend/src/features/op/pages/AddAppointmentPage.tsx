import React from "react";
import { useMemo, useState } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { Button, Text, Textarea } from "@chakra-ui/react";
import { FormField, HimsSelect, TextInput } from "../../../components/ui";
import {
  useCreateAppointment,
  useDepartments,
  useDoctorSchedules,
  useDoctorSlots,
} from "../api/queries";
import { useOpSession } from "../store/opSession";
import { PatientSearch } from "../components/PatientSearch";
import { PatientBanner } from "../components/PatientBanner";
import { usePatient } from "../api/usePatient";
import type { Doctor, PatientSearchHit } from "../api/types";

/**
 * AddAppointmentPage — legacy `addAppointment` mapped to React.
 *
 * Flow: Department -> Doctor -> Slot cascade with REAL-TIME availability.
 * The dependency chain is a classic three-level cascade:
 *
 *   1. Department: GET /v1/departments                 (one-shot, 5 min cache)
 *   2. Doctor:    GET /v1/doctors/schedules?dept=       (depends on #1, enabled only after pick)
 *   3. Slot:      GET /v1/doctors/{id}/slots?date=      (depends on #2 + selected date)
 *
 * Each level disables the next until the previous has resolved, preventing
 * the cascade from firing all three requests on mount (which is the
 * classic OP-page waste — the legacy webapp did this client-side).
 *
 * The slot picker is a 2D grid (morning / afternoon / evening columns by
 * half-hour rows) and updates as soon as the slot query resolves. Picking a
 * slot pre-fills the time fields; submitting creates the appointment via
 * POST /v1/appointments (BR-APT-001 unique-slot constraint).
 */
export function AddAppointmentPage() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const prefillPatientId = searchParams.get("patientId")
    ? Number(searchParams.get("patientId"))
    : undefined;
  const { duz, isNhnc, appType } = useOpSession();
  const [departmentId, setDepartmentId] = useState<number | null>(null);
  const [doctorId, setDoctorId] = useState<number | null>(null);
  const [date, setDate] = useState<string>(todayIso());
  const [selectedSlot, setSelectedSlot] = useState<SlotRow | null>(null);
  const [selectedPatient, setSelectedPatient] = useState<PatientSearchHit | null>(null);
  const [notes, setNotes] = useState<string>("");

  const departments = useDepartments();
  const doctors = useDoctorSchedules(departmentId ?? undefined);
  const slots = useDoctorSlots(doctorId ?? undefined, date);

  const create = useCreateAppointment();
  const { patient: fullPatient } = usePatient(prefillPatientId);

  const onPickPatient = (hit: PatientSearchHit) => {
    setSelectedPatient(hit);
  };

  const onSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedPatient || !doctorId || !selectedSlot) return;
    void create
      .mutateAsync({
        patientId: selectedPatient.id,
        doctorId,
        departmentId: departmentId ?? 0,
        startAt: selectedSlot.startAt,
        endAt: selectedSlot.endAt,
        source: appType === "EMG" ? "EMERGENCY" : isNhnc ? "ONLINE" : "WALK_IN",
        notes: notes.trim() || undefined,
      })
      .then((appt) => {
        navigate(`/m/op/appointmentList?highlight=${appt.id}`);
      })
      .catch(() => {
        // The error is rendered below via the create.error state.
      });
  };

  const doctorOptions: Doctor[] = doctors.data ?? [];

  return (
    <div className="page page--add-appt">
      <header className="page-header">
        <div>
          <h1>Add Appointment</h1>
          <p className="muted">Department → Doctor → Slot. Slot times update in real time.</p>
        </div>
        {duz ? (
          <p className="muted" title="Booking under DUZ">
            Booking as DUZ <code>{duz}</code>
          </p>
        ) : null}
      </header>

      {prefillPatientId && fullPatient ? (
        <div className="card card--banner">
          <PatientBanner patient={fullPatient} />
        </div>
      ) : (
        <section className="card" aria-label="Patient">
          <PatientSearch onPick={onPickPatient} autoFocus label="Find patient" />
        </section>
      )}

      {selectedPatient ? (
        <section className="card card--banner">
          <PatientBannerLite hit={selectedPatient} />
        </section>
      ) : null}

      <form className="card add-appt-form" onSubmit={onSubmit}>
        <div className="form-row form-row--three-col">
          <FormField label="Department">
            <HimsSelect
              value={departmentId ?? ""}
              onChange={(e) => {
                setDepartmentId(e.target.value ? Number(e.target.value) : null);
                setDoctorId(null);
                setSelectedSlot(null);
              }}
              disabled={!departments.data}
            >
              <option value="">— Select —</option>
              {(departments.data ?? []).map((d) => (
                <option key={d.id} value={d.id}>{d.name}</option>
              ))}
            </HimsSelect>
          </FormField>
          <FormField label="Doctor">
            <HimsSelect
              value={doctorId ?? ""}
              onChange={(e) => {
                setDoctorId(e.target.value ? Number(e.target.value) : null);
                setSelectedSlot(null);
              }}
              disabled={!departmentId || doctors.isLoading}
            >
              <option value="">
                {doctors.isLoading ? "Loading..." : departmentId ? "— Select —" : "Pick a department first"}
              </option>
              {doctorOptions.map((d) => (
                <option key={d.id} value={d.id}>
                  {d.name}
                  {d.departmentName ? ` · ${d.departmentName}` : ""}
                </option>
              ))}
            </HimsSelect>
          </FormField>
          <FormField label="Date">
            <TextInput
              type="date"
              value={date}
              min={todayIso()}
              onChange={(e) => {
                setDate(e.target.value);
                setSelectedSlot(null);
              }}
            />
          </FormField>
        </div>

        <SlotPicker
          loading={slots.isLoading}
          slots={(slots.data ?? []) as SlotRow[]}
          selected={selectedSlot}
          onSelect={setSelectedSlot}
          doctorId={doctorId}
        />

        <div className="form-row form-row--two-col">
          <FormField label="Reason / notes">
            <Textarea
              bg="white"
              borderColor="gray.300"
              rows={3}
              value={notes}
              onChange={(e) => setNotes(e.target.value)}
              placeholder="Optional — visible on the appointment and printed on the slip"
            />
          </FormField>
          <div className="field">
            <Text fontSize="sm" fontWeight="medium">Source</Text>
            <p className="form-readonly">{sourceLabel(appType, isNhnc)}</p>
            <p className="field-hint">Set by the OP route in the session state (Fg.g() equivalent).</p>
          </div>
        </div>

        {create.isError ? (
          <p className="alert alert--error" role="alert">
            Could not book: {String((create.error as Error)?.message ?? create.error)}
          </p>
        ) : null}

        <footer className="add-appt-form__actions">
          <Button
            type="button"
            variant="secondary"
            onClick={() => navigate(-1)}
            disabled={create.isPending}
          >
            Cancel
          </Button>
          <Button
            type="submit"
            variant="primary"
            disabled={!selectedPatient || !doctorId || !selectedSlot}
            loading={create.isPending}
            loadingText="Booking…"
          >
            Book appointment
          </Button>
        </footer>
      </form>
    </div>
  );
}

interface SlotRow {
  slotId: number;
  doctorId: number;
  doctorName: string;
  startAt: string;
  endAt: string;
  capacity: number;
  booked: number;
  available: boolean;
}



function SlotPicker({
  loading,
  slots,
  selected,
  onSelect,
  doctorId,
}: {
  loading: boolean;
  slots: SlotRow[];
  selected: SlotRow | null;
  onSelect: (s: SlotRow) => void;
  doctorId?: number | null;
}) {
  // Group slots by hour-of-day into morning / afternoon / evening columns.
  const grouped = useMemo(() => groupByDayPart(slots), [slots]);

  if (loading) {
    return (
      <div className="slot-picker" role="status" aria-busy="true">
        <p className="muted">Loading slots…</p>
      </div>
    );
  }

  if (!doctorId) {
    return (
      <div className="slot-picker" role="status">
        <p className="muted">Select a department and doctor to view available slots.</p>
      </div>
    );
  }

  if (slots.length === 0) {
    return (
      <div className="slot-picker" role="status">
        <p className="muted">
          No slots for this doctor on this date. Try another date or doctor.
        </p>
      </div>
    );
  }

  return (
    <div className="slot-picker" role="radiogroup" aria-label="Available slots">
      {(["morning", "afternoon", "evening"] as const).map((part) => {
        const list = grouped[part];
        if (list.length === 0) return null;
        return (
          <div key={part} className={`slot-picker__col slot-picker__col--${part}`}>
            <h3>{part[0].toUpperCase() + part.slice(1)}</h3>
            <div className="slot-picker__list">
              {list.map((s) => {
                const isSelected = selected?.slotId === s.slotId;
                return (
                  <Button
                    key={s.slotId}
                    type="button"
                    variant={isSelected ? "primary" : "secondary"}
                    size="sm"
                    role="radio"
                    aria-checked={isSelected}
                    disabled={!s.available}
                    onClick={() => onSelect(s)}
                  >
                    <span className="slot-pill__time">
                      {new Date(s.startAt).toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" })}
                    </span>
                    <span className="slot-pill__cap">
                      {s.booked}/{s.capacity}
                    </span>
                  </Button>
                );
              })}
            </div>
          </div>
        );
      })}
    </div>
  );
}

function groupByDayPart(slots: SlotRow[]): Record<"morning" | "afternoon" | "evening", SlotRow[]> {
  const out: Record<"morning" | "afternoon" | "evening", SlotRow[]> = {
    morning: [],
    afternoon: [],
    evening: [],
  };
  for (const s of slots) {
    const h = new Date(s.startAt).getHours();
    if (h < 12) out.morning.push(s);
    else if (h < 17) out.afternoon.push(s);
    else out.evening.push(s);
  }
  return out;
}

function PatientBannerLite({ hit }: { hit: PatientSearchHit }) {
  // Lightweight PatientBanner wrapper for the picked search hit (we have
  // only the search fields, not the full record). The actual record is
  // loaded by the existing-patient flow when navigating from this page.
  return (
    <div className="patient-banner patient-banner--lite">
      <div className="patient-banner__bar" aria-hidden="true" />
      <div className="patient-banner__content">
        <div className="patient-banner__id">
          <span className="patient-banner__mrn">{hit.mrn ?? `#${hit.id}`}</span>
          <span className="patient-banner__name">
            {[hit.firstName, hit.lastName].filter(Boolean).join(" ") || "(no name)"}
          </span>
        </div>
        <div className="patient-banner__attrs">
          <span className="badge badge--muted">{hit.sex}</span>
          {hit.age != null ? <span>{hit.age}y</span> : null}
          {hit.dateOfBirth ? <span>DOB {hit.dateOfBirth}</span> : null}
          {hit.mobile ? <span>📞 {hit.mobile}</span> : null}
        </div>
      </div>
    </div>
  );
}

function todayIso(): string {
  return new Date().toISOString().slice(0, 10);
}

function sourceLabel(appType: string, nhnc: boolean): string {
  if (appType === "EMG") return "Emergency";
  return nhnc ? "Online (NHNC)" : "Walk-in";
}
