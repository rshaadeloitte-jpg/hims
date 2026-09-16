import { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import {
  useDecomposeRequest,
  useHandoverToFirm,
  useReceiveBioRequest,
  useUpdateBagStatus,
  useWasteDetails,
} from "./queries";
import {
  bagStatusUpdateSchema,
  decomposeFormSchema,
  handoverFormSchema,
  type BagStatusUpdate,
  type DecomposeFormValues,
  type HandoverFormValues,
} from "./zodSchema";

/**
 * HandoverView — the handover-to-firm workflow for a single waste request.
 *
 * Covers the three late-stage transitions from the legacy BMW module:
 *   - handOverBags (PENDING/ACCEPTED → HANDOVERED) — capture person name
 *   - receiveBioRequestMainStore (HANDOVERED → RECEIVED) — capture bag weights
 *   - processBioRequest (RECEIVED → COMPLETED) — capture disposal-firm info
 *
 * Plus the bag-wise status update flow (updateBagStatus) which is the newer
 * "bagWiseStatusUpdateInBMW" path: select bags, advance their status, enter
 * weight + optionally a handover recipient.
 *
 * The Stripe-style status badges render the current state at every step.
 */
interface HandoverViewProps {
  requestId: number;
  unitId: number;
  onClose?: () => void;
}

type Stage = "handover" | "receive" | "decompose" | "bagwise";

export function HandoverView({ requestId, unitId, onClose }: HandoverViewProps) {
  const { data: details = [], isLoading } = useWasteDetails(requestId, unitId);
  const handoverToFirm = useHandoverToFirm();
  const receiveRequest = useReceiveBioRequest();
  const decomposeRequest = useDecomposeRequest();
  const updateBagStatus = useUpdateBagStatus();

  const [stage, setStage] = useState<Stage>("handover");

  const {
    register: registerHandover,
    handleSubmit: handleHandoverSubmit,
    formState: { errors: handoverErrors },
  } = useForm<HandoverFormValues>({
    resolver: zodResolver(handoverFormSchema),
    defaultValues: { handedOverTo: "" },
  });

  const {
    register: registerDecompose,
    handleSubmit: handleDecomposeSubmit,
    formState: { errors: decomposeErrors },
  } = useForm<DecomposeFormValues>({
    resolver: zodResolver(decomposeFormSchema),
    defaultValues: { firmName: "", contactPerson: "", contactNo: "" },
  });

  // Bag-wise selection state
  const [selectedBags, setSelectedBags] = useState<Record<number, boolean>>({});
  const [bagWeights, setBagWeights] = useState<Record<number, string>>({});
  const [handedOverToBagwise, setHandedOverToBagwise] = useState("");

  const toggleBag = (id: number) =>
    setSelectedBags((prev) => ({ ...prev, [id]: !prev[id] }));

  const handleHandover = (values: HandoverFormValues) => {
    handoverToFirm.mutate(
      { id: requestId, unitId, ...values },
      { onSuccess: () => setStage("receive") },
    );
  };

  const handleReceive = () => {
    receiveRequest.mutate(
      { id: requestId, unitId },
      { onSuccess: () => setStage("decompose") },
    );
  };

  const handleDecompose = (values: DecomposeFormValues) => {
    decomposeRequest.mutate(
      { id: requestId, unitId, ...values },
      { onSuccess: onClose },
    );
  };

  const handleBagwiseUpdate = () => {
    const updates: BagStatusUpdate[] = Object.entries(selectedBags)
      .filter(([, sel]) => sel)
      .map(([id]) => ({
        bioMedicalWasteId: requestId,
        id: Number(id),
        unitId,
        bagStatus: 2, // ACCEPTED → advance to next
        weight: bagWeights[Number(id)] ? Number(bagWeights[Number(id)]) : undefined,
        handedOverTo: handedOverToBagwise || undefined,
      }));
    if (updates.length === 0) return;
    // Validate each
    const valid: BagStatusUpdate[] = [];
    for (const u of updates) {
      const r = bagStatusUpdateSchema.safeParse(u);
      if (!r.success) return;
      valid.push(r.data);
    }
    updateBagStatus.mutate(valid, { onSuccess: onClose });
  };

  if (isLoading) return <p className="muted">Loading bag details…</p>;

  return (
    <div className="card">
      <div className="page-header">
        <h2>Handover — Request #{requestId}</h2>
        <div className="toolbar-row">
          {(["handover", "receive", "decompose", "bagwise"] as Stage[]).map((s) => (
            <button
              key={s}
              type="button"
              className={`filter-pill${stage === s ? " filter-pill--active" : ""}`}
              onClick={() => setStage(s)}
            >
              {s === "handover" && "Hand Over"}
              {s === "receive" && "Receive"}
              {s === "decompose" && "Decompose"}
              {s === "bagwise" && "Bag-wise"}
            </button>
          ))}
        </div>
      </div>

      {/* Stripe-style status timeline */}
      <div className="timeline">
        {[
          { label: "Pending", tone: "badge--warning" },
          { label: "Accepted", tone: "badge--info" },
          { label: "Handed Over", tone: "badge--info" },
          { label: "Received", tone: "badge--success" },
          { label: "Completed", tone: "badge--success" },
        ].map((step, i) => (
          <span key={i} className={`badge ${step.tone}`}>
            {step.label}
          </span>
        ))}
      </div>

      {/* Bag details table with barcode */}
      <table className="table">
        <thead>
          <tr>
            {stage === "bagwise" && <th>Select</th>}
            <th>Bag #</th>
            <th>Barcode</th>
            <th>Weight (kg)</th>
            <th>Status</th>
            <th>Firm</th>
          </tr>
        </thead>
        <tbody>
          {details.map((d) => (
            <tr key={d.id}>
              {stage === "bagwise" && (
                <td>
                  <input
                    type="checkbox"
                    checked={!!selectedBags[d.id]}
                    onChange={() => toggleBag(d.id)}
                  />
                </td>
               )}
              <td>{d.bagName ?? `Bag ${d.id}`}</td>
              <td>
                <code>{d.barcodeNo}</code>
              </td>
              <td>
                {stage === "bagwise" && selectedBags[d.id] ? (
                  <input
                    type="number"
                    step="0.01"
                    value={bagWeights[d.id] ?? ""}
                    onChange={(e) =>
                      setBagWeights((p) => ({ ...p, [d.id]: e.target.value }))
                    }
                    placeholder="Weight"
                  />
                ) : (
                  d.weight ?? "—"
                )}
              </td>
              <td>
                <span className={`badge badge--${d.bagStatus === 2 ? "info" : d.bagStatus === 3 ? "warning" : "muted"}`}>
                  {d.bagStatus === 1 && "Pending"}
                  {d.bagStatus === 2 && "Accepted"}
                  {d.bagStatus === 3 && "Handed Over"}
                  {d.bagStatus === 4 && "Received"}
                  {d.bagStatus === 5 && "Completed"}
                  {!d.bagStatus && "—"}
                </span>
              </td>
              <td>{d.firmName ?? "—"}</td>
            </tr>
          ))}
          {details.length === 0 && (
            <tr>
              <td colSpan={stage === "bagwise" ? 6 : 5} className="muted">
                No bags in this request.
              </td>
            </tr>
          )}
        </tbody>
      </table>

      {/* Stage-specific form */}
      {stage === "handover" && (
        <form onSubmit={handleHandoverSubmit(handleHandover)} className="master-form">
          <div className={`field ${handoverErrors.handedOverTo ? "field--error" : ""}`}>
            <label htmlFor="handedOverTo">
              Handed Over To <span className="req">*</span>
            </label>
            <input
              id="handedOverTo"
              placeholder="Person receiving the bags"
              {...registerHandover("handedOverTo")}
            />
            {handoverErrors.handedOverTo && (
              <span className="field-error">{handoverErrors.handedOverTo.message}</span>
            )}
          </div>
          <button
            type="submit"
            className="btn btn-primary"
            disabled={handoverToFirm.isPending}
          >
            {handoverToFirm.isPending ? "Handing over…" : "Confirm Handover"}
          </button>
        </form>
      )}

      {stage === "receive" && (
        <div>
          <p className="muted">
            All bags handed over. Confirm receipt at the main store to advance to
            the decomposing stage.
          </p>
          <button
            className="btn btn-primary"
            onClick={handleReceive}
            disabled={receiveRequest.isPending}
          >
            {receiveRequest.isPending ? "Receiving…" : "Confirm Receipt"}
          </button>
        </div>
      )}

      {stage === "decompose" && (
        <form onSubmit={handleDecomposeSubmit(handleDecompose)} className="master-form">
          <h3>Dispatch to Disposal Firm</h3>
          <div className="form-grid">
            <div className={`field ${decomposeErrors.firmName ? "field--error" : ""}`}>
              <label htmlFor="firmName">
                Firm Name <span className="req">*</span>
              </label>
              <input id="firmName" {...registerDecompose("firmName")} />
              {decomposeErrors.firmName && (
                <span className="field-error">{decomposeErrors.firmName.message}</span>
              )}
            </div>
            <div className={`field ${decomposeErrors.contactPerson ? "field--error" : ""}`}>
              <label htmlFor="contactPerson">
                Contact Person <span className="req">*</span>
              </label>
              <input id="contactPerson" {...registerDecompose("contactPerson")} />
              {decomposeErrors.contactPerson && (
                <span className="field-error">{decomposeErrors.contactPerson.message}</span>
              )}
            </div>
            <div className={`field ${decomposeErrors.contactNo ? "field--error" : ""}`}>
              <label htmlFor="contactNo">
                Mobile No <span className="req">*</span>
              </label>
              <input id="contactNo" maxLength={10} {...registerDecompose("contactNo")} />
              {decomposeErrors.contactNo && (
                <span className="field-error">{decomposeErrors.contactNo.message}</span>
              )}
            </div>
          </div>
          <button
            type="submit"
            className="btn btn-primary"
            disabled={decomposeRequest.isPending}
          >
            {decomposeRequest.isPending ? "Processing…" : "Complete Disposal"}
          </button>
        </form>
      )}

      {stage === "bagwise" && (
        <div>
          <div className="field">
            <label htmlFor="handedOverToBagwise">Handed Over To (if advancing)</label>
            <input
              id="handedOverToBagwise"
              value={handedOverToBagwise}
              onChange={(e) => setHandedOverToBagwise(e.target.value)}
              placeholder="Person receiving the bags"
            />
          </div>
          <button
            className="btn btn-primary"
            onClick={handleBagwiseUpdate}
            disabled={updateBagStatus.isPending || !Object.values(selectedBags).some(Boolean)}
          >
            {updateBagStatus.isPending ? "Updating…" : "Update Selected Bags"}
          </button>
        </div>
      )}

      {handoverToFirm.isError && (
        <div className="alert alert--error" role="alert">
          Handover failed: {(handoverToFirm.error as Error)?.message}
        </div>
      )}
      {receiveRequest.isError && (
        <div className="alert alert--error" role="alert">
          Receipt failed: {(receiveRequest.error as Error)?.message}
        </div>
      )}
      {decomposeRequest.isError && (
        <div className="alert alert--error" role="alert">
          Decompose failed: {(decomposeRequest.error as Error)?.message}
        </div>
      )}
      {updateBagStatus.isError && (
        <div className="alert alert--error" role="alert">
          Bag update failed: {(updateBagStatus.error as Error)?.message}
        </div>
      )}
    </div>
  );
}
