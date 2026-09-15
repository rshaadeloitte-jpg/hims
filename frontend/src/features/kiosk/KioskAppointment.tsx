import { useState } from "react";
import { useDepartments, useDoctors, useSlots, useBookAppointment } from "./queries";
import { appointmentBookingSchema } from "./zodSchema";

type AppointmentForm = {
  patientName: string;
  mobileNo: string;
  departmentId: number;
  doctorId: number;
  date: string;
  slotId: number;
  reason: string;
};

export function KioskAppointment() {
  const [form, setForm] = useState<AppointmentForm>({
    patientName: "",
    mobileNo: "",
    departmentId: 0,
    doctorId: 0,
    date: "",
    slotId: 0,
    reason: "",
  });

  const { data: departments = [], isLoading: deptLoading } = useDepartments();
  const { data: doctors = [], isLoading: docsLoading } = useDoctors(form.departmentId || undefined);
  const { data: slots = [], isLoading: slotsLoading } = useSlots(form.doctorId || undefined, form.date || undefined);
  const book = useBookAppointment();

  const [errors, setErrors] = useState<Record<string, string>>({});
  const [submitted, setSubmitted] = useState(false);
  const [appointmentRef, setAppointmentRef] = useState<string>("");

  const handleSubmit = () => {
    const result = appointmentBookingSchema.safeParse(form);
    if (!result.success) {
      const fieldErrors: Record<string, string> = {};
      result.error.errors.forEach((e) => {
        fieldErrors[String(e.path[0])] = e.message;
      });
      setErrors(fieldErrors);
      return;
    }
    setErrors({});
    book.mutate(result.data, {
      onSuccess: (data) => {
        setAppointmentRef(data.tokenNo ?? String(data.appointmentId));
        setSubmitted(true);
      },
    });
  };

  const reset = () => {
    setSubmitted(false);
    setForm({
      patientName: "", mobileNo: "", departmentId: 0, doctorId: 0, date: "", slotId: 0, reason: "",
    });
    setErrors({});
    setAppointmentRef("");
  };

  if (submitted) {
    return (
      <div className="kiosk-view">
        <div className="card kiosk-card">
          <h1 className="kiosk-title">Appointment Booked</h1>
          <p className="kiosk-muted">Your appointment has been confirmed.</p>
          <p className="kiosk-token">Token: {appointmentRef}</p>
          <button className="btn btn-primary kiosk-btn" onClick={reset}>
            Book Another
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="kiosk-view">
      <div className="card kiosk-card">
        <h1 className="kiosk-title">Book Appointment</h1>
        <p className="kiosk-subtitle">Select department, doctor, and slot</p>

        <div className="kiosk-form-grid">
          <div className="field">
            <label className="kiosk-label">
              Patient Name *
              {errors.patientName && <span className="error kiosk-error">{errors.patientName}</span>}
            </label>
            <input
              className="kiosk-input"
              type="text"
              autoCapitalize="words"
              placeholder="Full name"
              value={form.patientName}
              onChange={(e) => setForm({ ...form, patientName: e.target.value })}
            />
          </div>

          <div className="field">
            <label className="kiosk-label">
              Mobile Number *
              {errors.mobileNo && <span className="error kiosk-error">{errors.mobileNo}</span>}
            </label>
            <input
              className="kiosk-input"
              type="tel"
              inputMode="numeric"
              maxLength={10}
              placeholder="10-digit mobile"
              value={form.mobileNo}
              onChange={(e) => setForm({ ...form, mobileNo: e.target.value })}
            />
          </div>

          <div className="field">
            <label className="kiosk-label">
              Department *
              {errors.departmentId && <span className="error kiosk-error">{errors.departmentId}</span>}
            </label>
            <select
              className="kiosk-select kiosk-input"
              value={form.departmentId || ""}
              onChange={(e) => {
                setForm({ ...form, departmentId: Number(e.target.value), doctorId: 0, slotId: 0 });
                setErrors({});
              }}
              disabled={deptLoading}
            >
              <option value="" disabled>Select Department</option>
              {departments.map((d) => (
                <option key={d.id} value={d.id}>{d.name}</option>
              ))}
            </select>
          </div>

          <div className="field">
            <label className="kiosk-label">
              Doctor *
              {errors.doctorId && <span className="error kiosk-error">{errors.doctorId}</span>}
            </label>
            <select
              className="kiosk-select kiosk-input"
              value={form.doctorId || ""}
              onChange={(e) => setForm({ ...form, doctorId: Number(e.target.value), slotId: 0 })}
              disabled={docsLoading || !form.departmentId}
            >
              <option value="" disabled>Select Doctor</option>
              {doctors.map((d) => (
                <option key={d.id} value={d.id}>{d.name}</option>
              ))}
            </select>
          </div>

          <div className="field">
            <label className="kiosk-label">
              Date *
              {errors.date && <span className="error kiosk-error">{errors.date}</span>}
            </label>
            <input
              className="kiosk-input"
              type="date"
              value={form.date}
              onChange={(e) => setForm({ ...form, date: e.target.value })}
            />
          </div>

          <div className="field">
            <label className="kiosk-label">
              Slot *
              {errors.slotId && <span className="error kiosk-error">{errors.slotId}</span>}
            </label>
            <select
              className="kiosk-select kiosk-input"
              value={form.slotId || ""}
              onChange={(e) => setForm({ ...form, slotId: Number(e.target.value) })}
              disabled={slotsLoading || !form.doctorId}
            >
              <option value="" disabled>Select Slot</option>
              {slots
                .filter((s) => s.available)
                .map((s) => (
                  <option key={s.slotId} value={s.slotId}>
                    {s.startTime} - {s.endTime}
                  </option>
                ))}
            </select>
          </div>

          <div className="field">
            <label className="kiosk-label">Reason (optional)</label>
            <input
              className="kiosk-input"
              type="text"
              placeholder="Reason for visit"
              value={form.reason}
              onChange={(e) => setForm({ ...form, reason: e.target.value })}
            />
          </div>
        </div>

        {errors.form && <p className="error kiosk-form-error">{errors.form}</p>}

        <button
          className="btn btn-primary kiosk-btn"
          onClick={handleSubmit}
          disabled={book.isPending || slotsLoading || docsLoading || deptLoading}
        >
          {book.isPending ? "Booking..." : "Book Appointment"}
        </button>
      </div>
    </div>
  );
}
