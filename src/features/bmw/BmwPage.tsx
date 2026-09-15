import { useState } from "react";
import { WasteDashboard } from "./WasteDashboard";
import { WasteRequest } from "./WasteRequest";

/**
 * BmwPage — entry page for the M18 Biomedical Waste feature.
 *
 * Routes between the dashboard (list + summary) and the create-request form.
 * Session-derived unitId/storeId would come from an auth context in a real app;
 * here we use sensible defaults that the dev proxy can serve.
 */
export function BmwPage() {
  const [showForm, setShowForm] = useState(false);
  const [refreshKey, setRefreshKey] = useState(0);

  // In a real app these would come from an auth/session context
  const unitId = Number(import.meta.env.VITE_BMW_UNIT_ID ?? 1);
  const storeId = Number(import.meta.env.VITE_BMW_STORE_ID ?? 1);
  const isBioMedicalStore = import.meta.env.VITE_BMW_IS_STORE === "true";

  return (
    <div className="bmw-page">
      {showForm ? (
        <WasteRequest
          unitId={unitId}
          defaultStoreId={storeId}
          onCancel={() => setShowForm(false)}
          onSuccess={() => {
            setShowForm(false);
            setRefreshKey((k) => k + 1);
          }}
        />
      ) : (
        <WasteDashboard
          key={refreshKey}
          unitId={unitId}
          storeId={storeId}
          isBioMedicalStore={isBioMedicalStore}
        />
      )}

      {!showForm && (
        <button
          className="btn btn-primary bmw-page__create-btn"
          onClick={() => setShowForm(true)}
        >
          + Create Waste Request
        </button>
      )}
    </div>
  );
}
