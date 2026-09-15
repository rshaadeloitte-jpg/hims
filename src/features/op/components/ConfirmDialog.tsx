import { useEffect, useRef, type ReactNode } from "react";

interface ConfirmDialogProps {
  open: boolean;
  title: string;
  body: ReactNode;
  /** Optional reason / extra input the user must fill in (e.g. cancel reason). */
  reasonField?: {
    label: string;
    placeholder?: string;
    required?: boolean;
    value: string;
    onChange: (next: string) => void;
  };
  /** Confirm button label; default "Confirm". */
  confirmLabel?: string;
  /** Cancel button label; default "Cancel". */
  cancelLabel?: string;
  /** Tonal variant of the confirm button. */
  confirmTone?: "primary" | "danger";
  /** Whether the confirm action is in flight. */
  busy?: boolean;
  onConfirm: () => void;
  onCancel: () => void;
}

/**
 * ConfirmDialog — small modal for "are you sure?" prompts (cancel/reschedule
 * reason, ABHA confirm-link, etc.). Implemented as a native <dialog> element
 * so it works without a UI library and is keyboard-navigable out of the box.
 *
 * Focus management: when opened, the first focusable element inside the dialog
 * receives focus (the cancel button by default). The Escape key triggers
 * `onCancel`. Click on the backdrop triggers `onCancel` too.
 */
export function ConfirmDialog({
  open,
  title,
  body,
  reasonField,
  confirmLabel = "Confirm",
  cancelLabel = "Cancel",
  confirmTone = "primary",
  busy,
  onConfirm,
  onCancel,
}: ConfirmDialogProps) {
  const dialogRef = useRef<HTMLDialogElement | null>(null);
  const cancelRef = useRef<HTMLButtonElement | null>(null);

  useEffect(() => {
    const d = dialogRef.current;
    if (!d) return;
    if (open && !d.open) {
      d.showModal();
      // focus the cancel button — Enter on the body will NOT trigger confirm.
      requestAnimationFrame(() => cancelRef.current?.focus());
    } else if (!open && d.open) {
      d.close();
    }
  }, [open]);

  return (
    <dialog
      ref={dialogRef}
      className="confirm-dialog"
      onCancel={(e) => {
        e.preventDefault();
        if (!busy) onCancel();
      }}
      onClick={(e) => {
        // click on the backdrop (outside the content box) closes
        if (e.target === dialogRef.current && !busy) onCancel();
      }}
      aria-labelledby="confirm-dialog-title"
    >
      <div className="confirm-dialog__body">
        <h2 id="confirm-dialog-title" className="confirm-dialog__title">
          {title}
        </h2>
        <div className="confirm-dialog__content">{body}</div>
        {reasonField ? (
          <div className="confirm-dialog__reason">
            <label className="field-label" htmlFor="confirm-dialog-reason">
              {reasonField.label}
            </label>
            <textarea
              id="confirm-dialog-reason"
              className="form-textarea"
              rows={3}
              placeholder={reasonField.placeholder}
              required={reasonField.required}
              value={reasonField.value}
              onChange={(e) => reasonField.onChange(e.target.value)}
            />
          </div>
        ) : null}
        <div className="confirm-dialog__actions">
          <button
            ref={cancelRef}
            type="button"
            className="btn btn-secondary"
            onClick={onCancel}
            disabled={busy}
          >
            {cancelLabel}
          </button>
          <button
            type="button"
            className={`btn ${confirmTone === "danger" ? "btn-danger" : "btn-primary"}`}
            onClick={onConfirm}
            disabled={busy || (reasonField?.required && !reasonField.value.trim())}
          >
            {busy ? "Working…" : confirmLabel}
          </button>
        </div>
      </div>
    </dialog>
  );
}
