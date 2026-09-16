import { Checkbox, Textarea } from "@chakra-ui/react";
import { FormField, TextInput } from "../../../components/ui";

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
      <Checkbox.Root
        checked={required}
        onCheckedChange={(e) => onRequiredChange(!!e.checked)}
        aria-describedby="mlc-help"
      >
        {/* Kept as a real checkbox input so the toggle stays a labelled
            form control (tests + assistive tech query it by label). */}
        <Checkbox.HiddenInput checked={required} onChange={(e) => onRequiredChange(e.target.checked)} />
        <Checkbox.Control />
        <Checkbox.Label>This is a Medico-Legal Case (MLC)</Checkbox.Label>
      </Checkbox.Root>
      <p id="mlc-help" className="mlc-section__help">
        Required when the patient is brought in for a legal / police case. MLC
        number + FIR number are mandatory when this toggle is on (BR-PAT-005).
      </p>
      <div className="form-row form-row--two-col">
        <FormField label="MLC number" required={required} error={errors?.mlcNumber}>
          <TextInput
            type="text"
            value={value.mlcNumber ?? ""}
            disabled={!required}
            onChange={(e) => set("mlcNumber", e.target.value)}
            aria-invalid={Boolean(errors?.mlcNumber)}
          />
        </FormField>
        <FormField label="FIR number" required={required} error={errors?.mlcFirNumber}>
          <TextInput
            type="text"
            value={value.mlcFirNumber ?? ""}
            disabled={!required}
            onChange={(e) => set("mlcFirNumber", e.target.value)}
            aria-invalid={Boolean(errors?.mlcFirNumber)}
          />
        </FormField>
      </div>
      <FormField label="Police station">
        <TextInput
          type="text"
          value={value.mlcPoliceStation ?? ""}
          disabled={!required}
          onChange={(e) => set("mlcPoliceStation", e.target.value)}
        />
      </FormField>
      <FormField label="MLC / FIR notes">
        <Textarea
          bg="white"
          borderColor="gray.300"
          rows={2}
          value={value.mlcNotes ?? ""}
          disabled={!required}
          onChange={(e) => set("mlcNotes", e.target.value)}
        />
      </FormField>
    </fieldset>
  );
}
