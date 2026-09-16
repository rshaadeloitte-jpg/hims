import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { FormDesigner } from "./FormDesigner";
import { useCreateForm } from "./queries";
import type { FormDefinitionValues } from "./zodSchema";
import { Button } from "@chakra-ui/react";

/**
 * FormDesignerPage — wraps the designer with navigation and mutation handling.
 */
export function FormDesignerPage() {
  const navigate = useNavigate();
  const createM = useCreateForm();
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = (values: FormDefinitionValues) => {
    setError(null);
    createM.mutate(values, {
      onSuccess: () => navigate("/forms"),
      onError: (err) => setError(err instanceof Error ? err.message : "Failed to create form"),
    });
  };

  return (
    <div className="page">
      <header className="page-header">
        <div>
          <h1>New Form</h1>
          <p className="muted">Design a clinical form with versioned JSON schema</p>
        </div>
        <Button variant="secondary" onClick={() => navigate("/forms")}>
          ← Back to Forms
        </Button>
      </header>

      {error && (
        <div className="alert alert--error" role="alert">
          {error}
        </div>
      )}

      <FormDesigner
        onSubmit={handleSubmit}
        onCancel={() => navigate("/forms")}
        isSubmitting={createM.isPending}
      />
    </div>
  );
}
