interface MlcDetailsSectionProps {
  /** Bound form values (controlled component). */
  value: {
    mlcNumber?: string;
    mlcFirNumber?: string;
    mlcPoliceStation?: string;
    mlcNotes?: string;
  };
  onChange: (next: MlcDetailsSectionProps["value"]) => void;
  /** Whether the MLC details are required (toggle from the parent). */
  required: boolean;
  onRequiredChange: (next: boolean) => void;
  /** Validation errors keyed by field name; rendered below the input. */
  errors?: Record<string, string>;
}

/**
 * MLCDetailsSection — Medico-Legal Case details (BR-PAT-005, FIR-001).
 * Folded into the walk-in demographic form (steps 1 + 2). When `required`
 * is true the MLC number / FIR number are validated as required; the
 * section also surfaces the FIR number, police station, and notes.
 *
 * The "MLC required" toggle drives the BR-PAT-005 validation rule: when
 * the case is medico-legal the registration form must capture the MLC
 * number + FIR + police station BEFORE the patient can be persisted.
 */
export function MlcDetailsSection({ value, onChange, required, onRequiredChange, errors }: MlcDetailsSectionProps) {
  const set = <K extends keyof MlcDetailsSectionProps["value"]>(k: K, v: string) =>
    onChange({ ...value, [k]: v });

  return (
    <fieldset className="mlc-section">
      <legend>MLC / FIR details</legend>
      <label className="mlc-section__toggle">
        <input
          type="checkbox"
          checked={required}
          onChange={(e) => onRequiredChange(e.target.checked)}
          aria-describedby="mlc-help"
        />
        <span>This is a Medico-Legal Case (MLC)</span>
      </label>
      <p id="mlc-help" className="mlc-section__help">
        Required when the patient is brought in for a legal / police case. MLC
        number + FIR number are mandatory when this toggle is on (BR-PAT-005).
      </p>
      <div className="form-row form-row--two-col">
        <div className="field">
          <label htmlFor="mlc-number" className="field-label">
            MLC number {required ? <span className="req">*</span> : null}
          </label>
          <input
            id="mlc-number"
            type="text"
            className="form-input"
            value={value.mlcNumber ?? ""}
            disabled={!required}
            onChange={(e) => set("mlcNumber", e.target.value)}
            aria-invalid={Boolean(errors?.mlcNumber)}
          />
          {errors?.mlcNumber ? <p className="field-error">{errors.mlcNumber}</p> : null}
        </div>
        <div className="field">
          <label htmlFor="mlc-fir" className="field-label">
            FIR number {required ? <span className="req">*</span> : null}
          </label>
          <input
            id="mlc-fir"
            type="text"
            className="form-input"
            value={value.mlcFirNumber ?? ""}
            disabled={!required}
            onChange={(e) => set("mlcFirNumber", e.target.value)}
            aria-invalid={Boolean(errors?.mlcFirNumber)}
          />
          {errors?.mlcFirNumber ? <p className="field-error">{errors.mlcFirNumber}</p> : null}
        </div>
      </div>
      <div className="field">
        <label htmlFor="mlc-ps" className="field-label">
          Police station
        </label>
        <input
          id="mlc-ps"
          type="text"
          className="form-input"
          value={value.mlcPoliceStation ?? ""}
          disabled={!required}
          onChange={(e) => set("mlcPoliceStation", e.target.value)}
        />
      </div>
      <div className="field">
        <label htmlFor="mlc-notes" className="field-label">
          MLC / FIR notes
        </label>
        <textarea
          id="mlc-notes"
          rows={2}
          className="form-textarea"
          value={value.mlcNotes ?? ""}
          disabled={!required}
          onChange={(e) => set("mlcNotes", e.target.value)}
        />
      </div>
    </fieldset>
  );
}
