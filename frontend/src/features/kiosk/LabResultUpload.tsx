import { useState } from "react";
import { useUploadLabResult } from "./queries";
import { labResultUploadSchema } from "./zodSchema";

type UploadForm = {
  tokenNo: string;
  visitId: string;
  testName: string;
  resultValue: string;
  resultDate: string;
  file: File | null;
};

export function LabResultUpload() {
  const upload = useUploadLabResult();
  const [form, setForm] = useState<UploadForm>({
    tokenNo: "",
    visitId: "",
    testName: "",
    resultValue: "",
    resultDate: "",
    file: null,
  });
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [submitted, setSubmitted] = useState(false);

  const handleSubmit = () => {
    const fieldErrors: Record<string, string> = {};
    if (!form.file) {
      fieldErrors.file = "File required";
    }

    const result = labResultUploadSchema.safeParse({
      tokenNo: form.tokenNo,
      visitId: form.visitId,
      testName: form.testName,
      resultValue: form.resultValue,
      resultDate: form.resultDate,
      file: form.file ?? undefined,
    });

    if (!result.success) {
      result.error.errors.forEach((e) => {
        fieldErrors[String(e.path[0])] = e.message;
      });
    }

    if (Object.keys(fieldErrors).length > 0 || !result.success) {
      setErrors(fieldErrors);
      return;
    }

    setErrors({});
    upload.mutate(result.data, {
      onSuccess: () => setSubmitted(true),
    });
  };

  const reset = () => {
    setSubmitted(false);
    setForm({ tokenNo: "", visitId: "", testName: "", resultValue: "", resultDate: "", file: null });
    setErrors({});
  };

  if (submitted) {
    return (
      <div className="kiosk-view">
        <div className="card kiosk-card">
          <h1 className="kiosk-title">Upload Successful</h1>
          <p className="kiosk-muted">Lab result has been recorded.</p>
          <button className="btn btn-primary kiosk-btn" onClick={reset}>
            Upload Another
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="kiosk-view">
      <div className="card kiosk-card">
        <h1 className="kiosk-title">Lab Result Upload</h1>
        <p className="kiosk-subtitle">Enter result details and attach file</p>

        <div className="kiosk-form-grid">
          <div className="field">
            <label className="kiosk-label">
              Token Number *
              {errors.tokenNo && <span className="error kiosk-error">{errors.tokenNo}</span>}
            </label>
            <input
              className="kiosk-input"
              type="text"
              placeholder="Token number"
              value={form.tokenNo}
              onChange={(e) => setForm({ ...form, tokenNo: e.target.value })}
            />
          </div>

          <div className="field">
            <label className="kiosk-label">
              Visit ID *
              {errors.visitId && <span className="error kiosk-error">{errors.visitId}</span>}
            </label>
            <input
              className="kiosk-input"
              type="text"
              placeholder="Visit ID"
              value={form.visitId}
              onChange={(e) => setForm({ ...form, visitId: e.target.value })}
            />
          </div>

          <div className="field">
            <label className="kiosk-label">
              Test Name *
              {errors.testName && <span className="error kiosk-error">{errors.testName}</span>}
            </label>
            <input
              className="kiosk-input"
              type="text"
              placeholder="e.g. CBC, Glucose"
              value={form.testName}
              onChange={(e) => setForm({ ...form, testName: e.target.value })}
            />
          </div>

          <div className="field">
            <label className="kiosk-label">
              Result Value *
              {errors.resultValue && <span className="error kiosk-error">{errors.resultValue}</span>}
            </label>
            <input
              className="kiosk-input"
              type="text"
              placeholder="e.g. 135"
              value={form.resultValue}
              onChange={(e) => setForm({ ...form, resultValue: e.target.value })}
            />
          </div>

          <div className="field">
            <label className="kiosk-label">
              Result Date *
              {errors.resultDate && <span className="error kiosk-error">{errors.resultDate}</span>}
            </label>
            <input
              className="kiosk-input"
              type="date"
              value={form.resultDate}
              onChange={(e) => setForm({ ...form, resultDate: e.target.value })}
            />
          </div>

          <div className="field">
            <label className="kiosk-label">
              Attach File *
              {errors.file && <span className="error kiosk-error">{errors.file}</span>}
            </label>
            <input
              className="kiosk-input"
              type="file"
              accept="image/*,.pdf"
              onChange={(e) => setForm({ ...form, file: e.target.files?.[0] ?? null })}
            />
            {form.file && <span className="kiosk-muted">{form.file.name}</span>}
          </div>
        </div>

        {errors.form && <p className="error kiosk-form-error">{errors.form}</p>}

        <button
          className="btn btn-primary kiosk-btn"
          onClick={handleSubmit}
          disabled={upload.isPending}
        >
          {upload.isPending ? "Uploading..." : "Upload Result"}
        </button>
      </div>
    </div>
  );
}
