import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import {
  feedbackSchema,
  type FeedbackFormValues,
  FEEDBACK_QUESTIONS,
} from "./zodSchema";

interface FeedbackFormProps {
  grievanceId: number;
  submitting: boolean;
  onSubmit: (values: FeedbackFormValues) => void;
  onCancel: () => void;
}

function StarRating({
  value,
  onChange,
  name,
  error,
}: {
  value: number;
  onChange: (v: number) => void;
  name: string;
  error?: string;
}) {
  return (
    <div className="star-rating" role="radiogroup" aria-label={name}>
      {[1, 2, 3, 4, 5].map((n) => (
        <button
          key={n}
          type="button"
          className={`star-rating__btn ${n <= value ? "star-rating__btn--on" : ""}`}
          onClick={() => onChange(n)}
          aria-label={`${n} star${n > 1 ? "s" : ""}`}
          aria-pressed={n === value}
        >
          ★
        </button>
      ))}
      {error ? <span className="field-error" role="alert">{error}</span> : null}
    </div>
  );
}

export function FeedbackForm({ grievanceId, submitting, onSubmit, onCancel }: FeedbackFormProps) {
  const {
    register,
    handleSubmit,
    setValue,
    watch,
    formState: { errors, isDirty },
  } = useForm<FeedbackFormValues>({
    resolver: zodResolver(feedbackSchema),
    defaultValues: {
      grievanceId,
      question1: 0,
      question2: 0,
      question3: 0,
      question4: 0,
      question5: 0,
      freeTextFeedback: "",
      email: "",
    },
  });

  const q1 = watch("question1");
  const q2 = watch("question2");
  const q3 = watch("question3");
  const q4 = watch("question4");
  const q5 = watch("question5");

  return (
    <form className="master-form" onSubmit={handleSubmit(onSubmit)} noValidate>
      <div className="form-grid">
        <div className="field field--wide">
          <h2 className="feedback-title">Satisfaction Feedback</h2>
          <p className="muted">
            Rate your experience on a scale of 1 (very dissatisfied) to 5 (very satisfied).
          </p>
        </div>

        <div className={`field field--wide ${errors.question1 ? "field--error" : ""}`}>
          <label>
            1. {FEEDBACK_QUESTIONS[0]} <span className="req" aria-hidden="true"> *</span>
          </label>
          <StarRating
            value={q1}
            onChange={(v) => setValue("question1", v, { shouldValidate: true })}
            name="question1"
            error={errors.question1?.message}
          />
        </div>

        <div className={`field field--wide ${errors.question2 ? "field--error" : ""}`}>
          <label>
            2. {FEEDBACK_QUESTIONS[1]} <span className="req" aria-hidden="true"> *</span>
          </label>
          <StarRating
            value={q2}
            onChange={(v) => setValue("question2", v, { shouldValidate: true })}
            name="question2"
            error={errors.question2?.message}
          />
        </div>

        <div className={`field field--wide ${errors.question3 ? "field--error" : ""}`}>
          <label>
            3. {FEEDBACK_QUESTIONS[2]} <span className="req" aria-hidden="true"> *</span>
          </label>
          <StarRating
            value={q3}
            onChange={(v) => setValue("question3", v, { shouldValidate: true })}
            name="question3"
            error={errors.question3?.message}
          />
        </div>

        <div className={`field field--wide ${errors.question4 ? "field--error" : ""}`}>
          <label>
            4. {FEEDBACK_QUESTIONS[3]} <span className="req" aria-hidden="true"> *</span>
          </label>
          <StarRating
            value={q4}
            onChange={(v) => setValue("question4", v, { shouldValidate: true })}
            name="question4"
            error={errors.question4?.message}
          />
        </div>

        <div className={`field field--wide ${errors.question5 ? "field--error" : ""}`}>
          <label>
            5. {FEEDBACK_QUESTIONS[4]} <span className="req" aria-hidden="true"> *</span>
          </label>
          <StarRating
            value={q5}
            onChange={(v) => setValue("question5", v, { shouldValidate: true })}
            name="question5"
            error={errors.question5?.message}
          />
        </div>

        <div className={`field field--wide ${errors.freeTextFeedback ? "field--error" : ""}`}>
          <label htmlFor="fb-free-text">Additional Comments</label>
          <textarea
            id="fb-free-text"
            rows={3}
            placeholder="Share any additional feedback (optional)…"
            aria-invalid={errors.freeTextFeedback ? true : undefined}
            {...register("freeTextFeedback")}
          />
          {errors.freeTextFeedback ? (
            <p className="field-error" role="alert">{errors.freeTextFeedback.message}</p>
          ) : null}
        </div>

        <div className={`field ${errors.email ? "field--error" : ""}`}>
          <label htmlFor="fb-email">Email (optional)</label>
          <input
            id="fb-email"
            type="email"
            placeholder="For follow-up if needed"
            aria-invalid={errors.email ? true : undefined}
            {...register("email")}
          />
          {errors.email ? (
            <p className="field-error" role="alert">{errors.email.message}</p>
          ) : null}
        </div>
      </div>

      <div className="form-actions">
        <button type="button" className="btn btn-secondary" onClick={onCancel} disabled={submitting}>
          Cancel
        </button>
        <button type="submit" className="btn btn-primary" disabled={submitting}>
          {submitting ? "Submitting…" : isDirty ? "Submit Feedback" : "Submit Feedback"}
        </button>
      </div>
    </form>
  );
}
