import { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import {
  useChambers,
  useAllocateChamber,
  useReleaseChamber,
  useUpdateDeathDetails,
  type ChamberRow,
} from "./queries";
import {
  chamberAllocationSchema,
  mortuaryClearanceSchema,
  deathDetailsSchema,
  CHAMBER_STATUS_TONE,
  CHAMBER_STATUS_LABEL,
  type ChamberAllocationValues,
  type MortuaryClearanceValues,
  type DeathDetailsValues,
} from "./zodSchema";

/**
 * M28 MortuaryBox — box management + death details.
 *
 * Two panels:
 *   1. Chamber grid — shows occupancy, allows allocation + release.
 *   2. Death details form — updatePatientDeathDetails (cause, type, MLC).
 *
 * Ported from `mortuaryBoxManagement.json` + `mortuaryClearanceSchema.json`
 * + `death_form.vm`.
 */

export function MortuaryBox() {
  const { data: chambers = [], isLoading } = useChambers();
  const allocate = useAllocateChamber();
  const release = useReleaseChamber();
  const updateDeath = useUpdateDeathDetails();

  const [showAllocate, setShowAllocate] = useState(false);
  const [showRelease, setShowRelease] = useState<number | null>(null);
  const [showDeathDetails, setShowDeathDetails] = useState<number | null>(null);

  if (isLoading) return <p className="muted">Loading chambers…</p>;

  return (
    <section className="page">
      <header className="page-header">
        <h1>Mortuary Box Management</h1>
        <button className="btn btn-primary" onClick={() => setShowAllocate(!showAllocate)}>
          {showAllocate ? "Cancel" : "Allocate Chamber"}
        </button>
      </header>

      {showAllocate && <AllocationForm onSubmit={(v) => allocate.mutate(v)} />}

      <div className="card">
        <h2>Chambers</h2>
        <table className="table">
          <thead>
            <tr>
              <th>Chamber</th>
              <th>Body</th>
              <th>Admitted</th>
              <th>Status</th>
              <th>Death Cert</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {chambers.map((c) => (
              <ChamberRowView
                key={c.id}
                chamber={c}
                onRelease={() => setShowRelease(c.id)}
                onDeathDetails={() => setShowDeathDetails(c.patientId ?? 0)}
              />
            ))}
            {chambers.length === 0 && (
              <tr>
                <td colSpan={6} className="muted">
                  No chambers allocated.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>

      {showRelease !== null && (
        <ReleaseForm
          chamberId={showRelease}
          onSubmit={(v) => {
            release.mutate(v);
            setShowRelease(null);
          }}
          onCancel={() => setShowRelease(null)}
        />
      )}

      {showDeathDetails !== null && showDeathDetails > 0 && (
        <DeathDetailsForm
          patientId={showDeathDetails}
          onSubmit={(v) => {
            updateDeath.mutate(v);
            setShowDeathDetails(null);
          }}
          onCancel={() => setShowDeathDetails(null)}
        />
      )}
    </section>
  );
}

// ---- Chamber row ------------------------------------------------------------

function ChamberRowView({
  chamber,
  onRelease,
  onDeathDetails,
}: {
  chamber: ChamberRow;
  onRelease: () => void;
  onDeathDetails: () => void;
}) {
  const tone = CHAMBER_STATUS_TONE[(chamber.status as keyof typeof CHAMBER_STATUS_TONE) ?? "EMPTY"] ?? "badge--muted";
  const label = CHAMBER_STATUS_LABEL[(chamber.status as keyof typeof CHAMBER_STATUS_LABEL) ?? chamber.status] ?? chamber.status;

  return (
    <tr>
      <td>{chamber.chamberNo}</td>
      <td>{chamber.bodyName ?? "—"}</td>
      <td>{chamber.admittedAt?.slice(0, 10) ?? "—"}</td>
      <td>
        <span className={`badge ${tone}`}>{label}</span>
      </td>
      <td>{chamber.deathCertNo ?? "—"}</td>
      <td>
        <div className="btn-row">
          {chamber.status === "OCCUPIED" && (
            <>
              <button className="btn btn-sm" onClick={onRelease}>
                Release
              </button>
              {chamber.patientId && (
                <button className="btn btn-sm" onClick={onDeathDetails}>
                  Death Details
                </button>
              )}
            </>
          )}
        </div>
      </td>
    </tr>
  );
}

// ---- Allocation form --------------------------------------------------------

function AllocationForm({ onSubmit }: { onSubmit: (v: ChamberAllocationValues) => void }) {
  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<ChamberAllocationValues>({
    resolver: zodResolver(chamberAllocationSchema),
    defaultValues: {
      chamberId: 0,
      patientId: 0,
      bodyName: "",
      causeOfDeath: "",
      admittedAt: new Date().toISOString().slice(0, 16),
    },
  });

  return (
    <form className="card" onSubmit={handleSubmit(onSubmit)}>
      <h2>Allocate Chamber</h2>
      <div className="form-grid">
        <label>
          Chamber ID
          {errors.chamberId && <span className="error">{errors.chamberId.message}</span>}
        </label>
        <input type="number" {...register("chamberId")} />

        <label>
          Patient ID
          {errors.patientId && <span className="error">{errors.patientId.message}</span>}
        </label>
        <input type="number" {...register("patientId")} />

        <label>
          Body Name
          {errors.bodyName && <span className="error">{errors.bodyName.message}</span>}
        </label>
        <input {...register("bodyName")} />

        <label>Cause of Death (optional)</label>
        <textarea {...register("causeOfDeath")} rows={2} />

        <label>
          Admitted At
          {errors.admittedAt && <span className="error">{errors.admittedAt.message}</span>}
        </label>
        <input type="datetime-local" {...register("admittedAt")} />
      </div>
      <button type="submit" className="btn btn-primary">
        Allocate
      </button>
    </form>
  );
}

// ---- Release form -----------------------------------------------------------

function ReleaseForm({
  chamberId,
  onSubmit,
  onCancel,
}: {
  chamberId: number;
  onSubmit: (v: MortuaryClearanceValues) => void;
  onCancel: () => void;
}) {
  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<MortuaryClearanceValues>({
    resolver: zodResolver(mortuaryClearanceSchema),
    defaultValues: {
      chamberId,
      releasedTo: "",
      relationship: "OTHER",
      recipientId: "",
      releasedAt: new Date().toISOString().slice(0, 16),
      remark: "",
    },
  });

  return (
    <form className="card" onSubmit={handleSubmit(onSubmit)}>
      <h2>Release Body — Chamber {chamberId}</h2>
      <div className="form-grid">
        <label>
          Released To
          {errors.releasedTo && <span className="error">{errors.releasedTo.message}</span>}
        </label>
        <input {...register("releasedTo")} />

        <label>Relationship</label>
        <select {...register("relationship")}>
          <option value="SPOUSE">Spouse</option>
          <option value="PARENT">Parent</option>
          <option value="CHILD">Child</option>
          <option value="SIBLING">Sibling</option>
          <option value="LEGAL">Legal</option>
          <option value="OTHER">Other</option>
        </select>

        <label>Recipient ID</label>
        <input {...register("recipientId")} />

        <label>
          Released At
          {errors.releasedAt && <span className="error">{errors.releasedAt.message}</span>}
        </label>
        <input type="datetime-local" {...register("releasedAt")} />

        <label>Remark</label>
        <textarea {...register("remark")} rows={2} />
      </div>
      <div className="btn-row">
        <button type="submit" className="btn btn-primary">
          Confirm Release
        </button>
        <button type="button" className="btn" onClick={onCancel}>
          Cancel
        </button>
      </div>
    </form>
  );
}

// ---- Death details form -----------------------------------------------------

function DeathDetailsForm({
  patientId,
  onSubmit,
  onCancel,
}: {
  patientId: number;
  onSubmit: (v: DeathDetailsValues) => void;
  onCancel: () => void;
}) {
  const {
    register,
    handleSubmit,
    watch,
    formState: { errors },
  } = useForm<DeathDetailsValues>({
    resolver: zodResolver(deathDetailsSchema),
    defaultValues: {
      patientId,
      isDeath: true,
      deathDate: new Date().toISOString().slice(0, 16),
      causeOfDeath: "",
      deathType: "NATURAL",
      certifiedBy: "",
      isMlc: false,
      mlcNumber: "",
    },
  });

  const isMlc = watch("isMlc");

  return (
    <form className="card" onSubmit={handleSubmit(onSubmit)}>
      <h2>Death Details — Patient {patientId}</h2>
      <div className="form-grid">
        <label>
          Date of Death
          {errors.deathDate && <span className="error">{errors.deathDate.message}</span>}
        </label>
        <input type="datetime-local" {...register("deathDate")} />

        <label>
          Cause of Death
          {errors.causeOfDeath && <span className="error">{errors.causeOfDeath.message}</span>}
        </label>
        <textarea {...register("causeOfDeath")} rows={2} />

        <label>Death Type</label>
        <select {...register("deathType")}>
          <option value="NATURAL">Natural</option>
          <option value="ACCIDENT">Accident</option>
          <option value="SUICIDE">Suicide</option>
          <option value="HOMICIDE">Homicide</option>
          <option value="PENDING">Pending</option>
          <option value="OTHER">Other</option>
        </select>

        <label>
          Certified By
          {errors.certifiedBy && <span className="error">{errors.certifiedBy.message}</span>}
        </label>
        <input {...register("certifiedBy")} />

        <label className="checkbox-row">
          <input type="checkbox" {...register("isMlc")} />
          Medico-Legal Case (MLC)
        </label>

        {isMlc && (
          <>
            <label>
              MLC Number
              {errors.mlcNumber && <span className="error">{errors.mlcNumber.message}</span>}
            </label>
            <input {...register("mlcNumber")} />
          </>
        )}
      </div>
      <div className="btn-row">
        <button type="submit" className="btn btn-primary">
          Save Death Details
        </button>
        <button type="button" className="btn" onClick={onCancel}>
          Cancel
        </button>
      </div>
    </form>
  );
}
