import { useState } from "react";
import { useSelfRegister } from "./queries";
import { kioskRegistrationSchema } from "./zodSchema";

export function KioskSelfReg() {
  type FormState = {
    mobileNo: string;
    firstName: string;
    lastName: string;
    gender: "" | "M" | "F" | "O";
    dob: string;
  };

  const register = useSelfRegister();
  const [submitted, setSubmitted] = useState(false);
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [form, setForm] = useState<FormState>({
    mobileNo: "",
    firstName: "",
    lastName: "",
    gender: "",
    dob: "",
  });

  const handleSubmit = () => {
    const result = kioskRegistrationSchema.safeParse(form);
    if (!result.success) {
      const fieldErrors: Record<string, string> = {};
      result.error.errors.forEach((e) => {
        const key = e.path[0];
        fieldErrors[key] = e.message;
      });
      setErrors(fieldErrors);
      return;
    }
    setErrors({});
    register.mutate(result.data, {
      onSuccess: () => setSubmitted(true),
    });
  };

  const reset = () => {
    setSubmitted(false);
    setForm({ mobileNo: "", firstName: "", lastName: "", gender: "", dob: "" });
    setErrors({});
  };

  if (submitted) {
    return (
      <div className="kiosk-view">
        <div className="card kiosk-card">
          <h1 className="kiosk-title">Registration Successful</h1>
          <p className="kiosk-muted">Your registration has been submitted. Please wait for your token.</p>
          <button className="btn btn-primary kiosk-btn" onClick={reset}>
            New Registration
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="kiosk-view">
      <div className="card kiosk-card">
        <h1 className="kiosk-title">Patient Self-Registration</h1>
        <p className="kiosk-subtitle">Please fill in your details</p>

        <div className="kiosk-form-grid">
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
              First Name *
              {errors.firstName && <span className="error kiosk-error">{errors.firstName}</span>}
            </label>
            <input
              className="kiosk-input"
              type="text"
              autoCapitalize="words"
              placeholder="First name"
              value={form.firstName}
              onChange={(e) => setForm({ ...form, firstName: e.target.value })}
            />
          </div>

          <div className="field">
            <label className="kiosk-label">
              Last Name *
              {errors.lastName && <span className="error kiosk-error">{errors.lastName}</span>}
            </label>
            <input
              className="kiosk-input"
              type="text"
              autoCapitalize="words"
              placeholder="Last name"
              value={form.lastName}
              onChange={(e) => setForm({ ...form, lastName: e.target.value })}
            />
          </div>

          <div className="field">
            <label className="kiosk-label">
              Gender *
              {errors.gender && <span className="error kiosk-error">{errors.gender}</span>}
            </label>
            <select
              className="kiosk-select kiosk-input"
              value={form.gender}
              onChange={(e) => setForm({ ...form, gender: e.target.value as FormState["gender"] })}
            >
              <option value="">Select Gender</option>
              <option value="M">Male</option>
              <option value="F">Female</option>
              <option value="O">Other</option>
            </select>
          </div>

          <div className="field">
            <label className="kiosk-label">
              Date of Birth *
              {errors.dob && <span className="error kiosk-error">{errors.dob}</span>}
            </label>
            <input
              className="kiosk-input"
              type="date"
              value={form.dob}
              onChange={(e) => setForm({ ...form, dob: e.target.value })}
            />
          </div>
        </div>

        {errors.form && <p className="error kiosk-form-error">{errors.form}</p>}

        <button
          className="btn btn-primary kiosk-btn"
          onClick={handleSubmit}
          disabled={register.isPending}
        >
          {register.isPending ? "Registering..." : "Register"}
        </button>
      </div>
    </div>
  );
}
