import type { Patient } from "../api/types";

interface PatientBannerProps {
  patient: Patient;
  /** Optional badge slot for status / scheme / NHNC. */
  rightSlot?: React.ReactNode;
  /** Called when the user clicks the banner (e.g. to open the detail page). */
  onClick?: () => void;
}

/**
 * PatientBanner — the persistent strip at the top of every clinical page
 * (09 §1 — "PatientBanner" cross-cutting component). Shows the minimum safe
 * identification set: MRN + name + sex + DOB/age + mobile, plus the bar-coded
 * look of the legacy facesheet header.
 *
 * `tabIndex=0` + keyboard handler: the banner is interactive when `onClick`
 * is supplied; falls back to a static strip otherwise.
 */
export function PatientBanner({ patient, rightSlot, onClick }: PatientBannerProps) {
  const interactive = typeof onClick === "function";
  const fullName = [patient.firstName, patient.middleName, patient.lastName]
    .filter(Boolean)
    .join(" ");

  const ageDisplay =
    patient.age != null
      ? `${patient.age}y`
      : patient.dateOfBirth
        ? `DOB ${patient.dateOfBirth}`
        : "—";

  const mrn = patient.mrn ?? (patient.isTempMrn ? "t-MRN (pending)" : "—");

  return (
    <div
      className={`patient-banner${interactive ? " patient-banner--clickable" : ""}`}
      role={interactive ? "button" : undefined}
      tabIndex={interactive ? 0 : undefined}
      onClick={onClick}
      onKeyDown={
        interactive
          ? (e) => {
              if (e.key === "Enter" || e.key === " ") {
                e.preventDefault();
                onClick?.();
              }
            }
          : undefined
      }
      aria-label={interactive ? `Open patient ${fullName}` : undefined}
    >
      <div className="patient-banner__bar" aria-hidden="true" />
      <div className="patient-banner__content">
        <div className="patient-banner__id">
          <span className="patient-banner__mrn" title="Master Registration Number (BR-PAT-001)">
            {mrn}
          </span>
          <span className="patient-banner__name" title={fullName}>
            {fullName || "(no name)"}
          </span>
        </div>
        <div className="patient-banner__attrs">
          <span className="badge badge--muted" title="Sex">
            {patient.sex ?? "—"}
          </span>
          <span className="patient-banner__age" title="Age / DOB">
            {ageDisplay}
          </span>
          {patient.mobile ? (
            <span className="patient-banner__mobile" title="Mobile">
              📞 {patient.mobile}
            </span>
          ) : null}
          {patient.abha ? (
            <span className="badge badge--success" title="ABHA linked">
              ABHA: {patient.abha}
            </span>
          ) : null}
        </div>
      </div>
      {rightSlot ? <div className="patient-banner__right">{rightSlot}</div> : null}
    </div>
  );
}
