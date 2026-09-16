import { useMemo } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import {
  grievanceSchema,
  type GrievanceFormValues,
} from "./zodSchema";
import type { GrievanceCategory, GrievanceSeverity } from "./queries";

interface GrievanceFormProps {
  categories: GrievanceCategory[];
  severities: GrievanceSeverity[];
  submitting: boolean;
  onSubmit: (values: GrievanceFormValues) => void;
  onCancel: () => void;
}

const ISSUE_TYPE_LABELS: Record<string, string> = {
  LOW: "Low — minor inconvenience",
  MEDIUM: "Medium — affects workflow",
  HIGH: "High — significant disruption",
  CRITICAL: "Critical — system down / urgent",
};

export function GrievanceForm({
  categories,
  severities,
  submitting,
  onSubmit,
  onCancel,
}: GrievanceFormProps) {
  const {
    register,
    handleSubmit,
    formState: { errors, isDirty },
  } = useForm<GrievanceFormValues>({
    resolver: zodResolver(grievanceSchema),
    defaultValues: {
      name: "",
      mobileNo: "",
      address: "",
      categoryId: undefined as unknown as number,
      subCategoryIds: [],
      issue: "",
      severityId: undefined as unknown as number,
      mrno: "",
      isCitizen: true,
    },
  });

  const sortedSeverities = useMemo(
    () => [...severities].sort((a, b) => a.id - b.id),
    [severities],
  );

  return (
    <form className="master-form" onSubmit={handleSubmit(onSubmit)} noValidate>
      <div className="form-grid">
        <div className={`field ${errors.name ? "field--error" : ""}`}>
          <label htmlFor="grv-name">
            Name <span className="req" aria-hidden="true"> *</span>
          </label>
          <input
            id="grv-name"
            type="text"
            placeholder="Your full name"
            aria-invalid={errors.name ? true : undefined}
            {...register("name")}
          />
          {errors.name ? (
            <p className="field-error" role="alert">{errors.name.message}</p>
          ) : null}
        </div>

        <div className={`field ${errors.mobileNo ? "field--error" : ""}`}>
          <label htmlFor="grv-mobile">Mobile Number</label>
          <input
            id="grv-mobile"
            type="tel"
            placeholder="10-digit mobile"
            aria-invalid={errors.mobileNo ? true : undefined}
            {...register("mobileNo")}
          />
          {errors.mobileNo ? (
            <p className="field-error" role="alert">{errors.mobileNo.message}</p>
          ) : null}
        </div>

        <div className={`field ${errors.categoryId ? "field--error" : ""}`}>
          <label htmlFor="grv-category">
            Category <span className="req" aria-hidden="true"> *</span>
          </label>
          <select
            id="grv-category"
            aria-invalid={errors.categoryId ? true : undefined}
            {...register("categoryId")}
          >
            <option value="">— Select category —</option>
            {categories.map((c) => (
              <option key={c.id} value={c.id}>
                {c.description}
              </option>
            ))}
          </select>
          {errors.categoryId ? (
            <p className="field-error" role="alert">{errors.categoryId.message}</p>
          ) : null}
        </div>

        <div className={`field ${errors.severityId ? "field--error" : ""}`}>
          <label htmlFor="grv-severity">
            Severity <span className="req" aria-hidden="true"> *</span>
          </label>
          <select
            id="grv-severity"
            aria-invalid={errors.severityId ? true : undefined}
            {...register("severityId")}
          >
            <option value="">— Select severity —</option>
            {sortedSeverities.map((s) => (
              <option key={s.id} value={s.id}>
                {s.code} — {s.description}
              </option>
            ))}
          </select>
          {errors.severityId ? (
            <p className="field-error" role="alert">{errors.severityId.message}</p>
          ) : null}
        </div>

        <div className={`field ${errors.mrno ? "field--error" : ""}`}>
          <label htmlFor="grv-mrno">MRN (optional)</label>
          <input
            id="grv-mrno"
            type="text"
            placeholder="Medical record number"
            {...register("mrno")}
          />
          {errors.mrno ? (
            <p className="field-error" role="alert">{errors.mrno.message}</p>
          ) : null}
        </div>

        <div className={`field ${errors.address ? "field--error" : ""}`}>
          <label htmlFor="grv-address">Address</label>
          <input
            id="grv-address"
            type="text"
            placeholder="Contact address"
            {...register("address")}
          />
          {errors.address ? (
            <p className="field-error" role="alert">{errors.address.message}</p>
          ) : null}
        </div>

        <div className={`field field--wide ${errors.issue ? "field--error" : ""}`}>
          <label htmlFor="grv-issue">
            Issue Description <span className="req" aria-hidden="true"> *</span>
          </label>
          <textarea
            id="grv-issue"
            rows={4}
            placeholder="Describe the grievance in detail…"
            aria-invalid={errors.issue ? true : undefined}
            {...register("issue")}
          />
          {errors.issue ? (
            <p className="field-error" role="alert">{errors.issue.message}</p>
          ) : null}
        </div>

        {/* Severity guide */}
        <div className="field field--wide">
          <label>Severity Guide</label>
          <div className="severity-guide">
            {(["LOW", "MEDIUM", "HIGH", "CRITICAL"] as const).map((code) => (
              <div key={code} className={`severity-guide__item severity-guide__item--${code.toLowerCase()}`}>
                <span className="severity-guide__code">{code}</span>
                <span className="severity-guide__desc">{ISSUE_TYPE_LABELS[code]}</span>
              </div>
            ))}
          </div>
        </div>
      </div>

      <div className="form-actions">
        <button type="button" className="btn btn-secondary" onClick={onCancel} disabled={submitting}>
          Cancel
        </button>
        <button type="submit" className="btn btn-primary" disabled={submitting}>
          {submitting ? "Submitting…" : isDirty ? "Submit Grievance" : "Submit Grievance"}
        </button>
      </div>
    </form>
  );
}
