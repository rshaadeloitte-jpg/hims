import { FormRenderer } from "./FormRenderer";
import type { FormSchemaResponse } from "./types";
import { Button } from "@chakra-ui/react";

interface FormRendererPageProps {
  schemaResponse: FormSchemaResponse;
  onBack: () => void;
}

/**
 * FormRendererPage — full-page form renderer with Stripe-inspired UI.
 * Navigates back to the form list after submission or on cancel.
 */
export function FormRendererPage({ schemaResponse, onBack }: FormRendererPageProps) {
  const handleSuccess = () => {
    // Optionally show a success modal or auto-close
  };

  return (
    <div className="page">
      <header className="page-header">
        <div>
          <h1>{schemaResponse.name}</h1>
          <p className="muted">{schemaResponse.specialty} • v{schemaResponse.version}</p>
        </div>
        <Button variant="secondary" onClick={onBack}>
          ← Back to Forms
        </Button>
      </header>

      <div className="card">
        <FormRenderer
          schemaResponse={schemaResponse}
          onSuccess={handleSuccess}
        />
      </div>
    </div>
  );
}
