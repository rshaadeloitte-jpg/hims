import { useState } from "react";
import { usePatientDetail, type PatientDetail } from "./queries";

/**
 * PatientProfile — Epic MyChart-style patient profile view.
 * Read-only display with edit action, photo, and key sections.
 */
export function PatientProfile({
  patientId,
  onEdit,
  onNavigate,
}: {
  patientId: number;
  onEdit?: (patient: PatientDetail) => void;
  onNavigate?: (path: string) => void;
}) {
  const [expandedSections, setExpandedSections] = useState<Set<string>>(
    new Set(["demographics", "contact"]),
  );
  const { data: patient, isLoading, isError, error } = usePatientDetail(patientId);

  const toggleSection = (section: string) => {
    setExpandedSections((prev) => {
      const next = new Set(prev);
      if (next.has(section)) next.delete(section);
      else next.add(section);
      return next;
    });
  };

  if (isLoading) {
    return (
      <div className="patient-profile" aria-busy="true">
        <div className="patient-profile__skeleton">
          <div className="skeleton skeleton--avatar" />
          <div className="skeleton skeleton--line skeleton--line-lg" />
          <div className="skeleton skeleton--line" />
          <div className="skeleton skeleton--line" />
        </div>
      </div>
    );
  }

  if (isError || !patient) {
    return (
      <div className="patient-profile" role="alert">
        <div className="alert alert--error">
          <strong>Error loading profile:</strong>{" "}
          {error instanceof Error ? error.message : "Patient not found"}
        </div>
      </div>
    );
  }

  return (
    <article className="patient-profile" aria-label={`Patient profile ${patient.mrNo}`}>
      {/* Header banner */}
      <header className="patient-profile__header">
        <div className="patient-profile__identity">
          {patient.photoUrl ? (
            <img src={patient.photoUrl} alt="" className="patient-profile__avatar" />
          ) : (
            <div className="patient-profile__avatar patient-profile__avatar--placeholder">
              {patient.firstName.charAt(0)}
              {patient.lastName?.charAt(0) ?? ""}
            </div>
          )}
          <div>
            <h1 className="patient-profile__name">
              {patient.firstName} {patient.middleName} {patient.lastName}
            </h1>
            <div className="patient-profile__ids">
              <span className="badge">MRN: {patient.mrNo}</span>
              {patient.abhaAddress && (
                <span className="badge badge--info">ABHA: {patient.abhaAddress}</span>
              )}
            </div>
          </div>
        </div>
        <div className="patient-profile__actions">
          {onEdit && (
            <button
              type="button"
              className="btn btn-secondary"
              onClick={() => onEdit(patient)}
            >
              Edit Profile
            </button>
          )}
          {onNavigate && (
            <button
              type="button"
              className="btn btn-primary"
              onClick={() => onNavigate(`/patients/${patient.patientId}/documents`)}
            >
              Upload Documents
            </button>
          )}
        </div>
      </header>

      {/* Demographics section */}
      <section className="patient-profile__section">
        <button
          type="button"
          className="patient-profile__section-header"
          onClick={() => toggleSection("demographics")}
          aria-expanded={expandedSections.has("demographics")}
        >
          <h2>Demographics</h2>
          <span className="patient-profile__chevron" aria-hidden="true">
            {expandedSections.has("demographics") ? "▲" : "▼"}
          </span>
        </button>
        {expandedSections.has("demographics") && (
          <dl className="patient-profile__grid">
            <div className="patient-profile__item">
              <dt>Gender</dt>
              <dd>{patient.genderId === 1 ? "Male" : patient.genderId === 2 ? "Female" : patient.genderId === 3 ? "Other" : "—"}</dd>
            </div>
            <div className="patient-profile__item">
              <dt>Date of Birth</dt>
              <dd>{patient.dateOfBirth ?? "—"}</dd>
            </div>
            <div className="patient-profile__item">
              <dt>Blood Group</dt>
              <dd>{patient.bloodGroup ?? "—"}</dd>
            </div>
            <div className="patient-profile__item">
              <dt>Marital Status</dt>
              <dd>{patient.maritalStatus ?? "—"}</dd>
            </div>
          </dl>
        )}
      </section>

      {/* Contact section */}
      <section className="patient-profile__section">
        <button
          type="button"
          className="patient-profile__section-header"
          onClick={() => toggleSection("contact")}
          aria-expanded={expandedSections.has("contact")}
        >
          <h2>Contact Information</h2>
          <span className="patient-profile__chevron" aria-hidden="true">
            {expandedSections.has("contact") ? "▲" : "▼"}
          </span>
        </button>
        {expandedSections.has("contact") && (
          <dl className="patient-profile__grid">
            <div className="patient-profile__item">
              <dt>Mobile</dt>
              <dd>{patient.mobileNo}</dd>
            </div>
            <div className="patient-profile__item">
              <dt>Email</dt>
              <dd>{patient.email ?? "—"}</dd>
            </div>
            <div className="patient-profile__item patient-profile__item--full">
              <dt>Address</dt>
              <dd>
                {[patient.address, patient.city, patient.state, patient.pincode]
                  .filter(Boolean)
                  .join(", ") || "—"}
              </dd>
            </div>
            <div className="patient-profile__item">
              <dt>Emergency Contact</dt>
              <dd>{patient.emergencyContact ?? "—"}</dd>
            </div>
          </dl>
        )}
      </section>

      {/* IDs section */}
      <section className="patient-profile__section">
        <button
          type="button"
          className="patient-profile__section-header"
          onClick={() => toggleSection("ids")}
          aria-expanded={expandedSections.has("ids")}
        >
          <h2>Identity Documents</h2>
          <span className="patient-profile__chevron" aria-hidden="true">
            {expandedSections.has("ids") ? "▲" : "▼"}
          </span>
        </button>
        {expandedSections.has("ids") && (
          <dl className="patient-profile__grid">
            <div className="patient-profile__item">
              <dt>Aadhaar</dt>
              <dd>{patient.aadhaarNumber ?? "—"}</dd>
            </div>
            <div className="patient-profile__item">
              <dt>ABHA</dt>
              <dd>{patient.abhaAddress ?? "—"}</dd>
            </div>
          </dl>
        )}
      </section>

      {/* MLC section */}
      {patient.isMlc && (
        <section className="patient-profile__section patient-profile__section--warning">
          <h2>⚡ Medico-Legal Case (MLC)</h2>
          <dl className="patient-profile__grid">
            <div className="patient-profile__item">
              <dt>MLC Number</dt>
              <dd>{patient.mlcNumber}</dd>
            </div>
            <div className="patient-profile__item">
              <dt>FIR Number</dt>
              <dd>{patient.mlcFirNumber ?? "—"}</dd>
            </div>
            <div className="patient-profile__item">
              <dt>Police Station</dt>
              <dd>{patient.mlcPoliceStation ?? "—"}</dd>
            </div>
            {patient.mlcNotes && (
              <div className="patient-profile__item patient-profile__item--full">
                <dt>Notes</dt>
                <dd>{patient.mlcNotes}</dd>
              </div>
            )}
          </dl>
        </section>
      )}

      {/* Mother-child section */}
      {patient.isChildPatient && (
        <section className="patient-profile__section">
          <h2>Mother-Child Link</h2>
          <dl className="patient-profile__grid">
            <div className="patient-profile__item">
              <dt>Mother MRN</dt>
              <dd>{patient.motherMrn ?? "—"}</dd>
            </div>
            <div className="patient-profile__item">
              <dt>Birth Weight</dt>
              <dd>{patient.birthWeightGrams ? `${patient.birthWeightGrams}g` : "—"}</dd>
            </div>
            <div className="patient-profile__item">
              <dt>Gestational Age</dt>
              <dd>{patient.gestationalAgeWeeks ? `${patient.gestationalAgeWeeks} weeks` : "—"}</dd>
            </div>
          </dl>
        </section>
      )}

      {/* COVID section */}
      {patient.covidVaccinationStatus && (
        <section className="patient-profile__section">
          <h2>COVID-19</h2>
          <dl className="patient-profile__grid">
            <div className="patient-profile__item">
              <dt>Vaccination Status</dt>
              <dd>{patient.covidVaccinationStatus}</dd>
            </div>
            <div className="patient-profile__item">
              <dt>Last Vaccine Date</dt>
              <dd>{patient.lastCovidVaccineDate ?? "—"}</dd>
            </div>
            <div className="patient-profile__item">
              <dt>Past Infection</dt>
              <dd>{patient.covidInfectionHistory ? `Yes (${patient.lastCovidInfectionDate})` : "No"}</dd>
            </div>
          </dl>
        </section>
      )}
    </article>
  );
}
