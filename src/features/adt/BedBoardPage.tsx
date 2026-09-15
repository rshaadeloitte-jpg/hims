import { useState } from "react";
import { useCensus, useBeds, useCleaningComplete, useAdmissions, useAdmit, useDischarge } from "./queries";
import { admitSchema, type AdmitValues } from "./zodSchema";
import { zodResolver } from "@hookform/resolvers/zod";
import { useForm } from "react-hook-form";

export function BedBoardPage() {
  const { data: census = [] } = useCensus();
  const { data: beds = [] } = useBeds();
  const { data: admissions = [] } = useAdmissions(undefined, true);
  const admit = useAdmit();
  const discharge = useDischarge();
  const cleaning = useCleaningComplete();

  const admitForm = useForm<AdmitValues>({ resolver: zodResolver(admitSchema) });
  const [showAdmit, setShowAdmit] = useState(false);

  const submitAdmit = async (v: AdmitValues) => {
    await admit.mutateAsync(v as unknown as Record<string, unknown>);
    setShowAdmit(false);
    admitForm.reset();
  };

  const statusLabel = (s: number) => {
    const map: Record<number, string> = { 1: "Free", 2: "Occupied", 3: "Reserved", 4: "Maintenance", 5: "Cleaning" };
    return map[s] ?? "Unknown";
  };

  return (
    <section className="page">
      <header className="page__header">
        <h1>ADT — Bed Board</h1>
        <button className="btn btn--primary" onClick={() => setShowAdmit(!showAdmit)}>Admit</button>
      </header>

      {showAdmit && (
        <form className="inline-form" onSubmit={admitForm.handleSubmit(submitAdmit)}>
          <input type="number" placeholder="Patient ID" {...admitForm.register("patientId")} />
          <input type="number" placeholder="Encounter ID" {...admitForm.register("encounterId")} />
          <input placeholder="Admission Type" {...admitForm.register("admissionType")} />
          <input type="number" placeholder="Bed ID (opt)" {...admitForm.register("bedId")} />
          <label><input type="checkbox" {...admitForm.register("isMlc")} /> MLC</label>
          <button type="submit" className="btn" disabled={admit.isPending}>Admit</button>
        </form>
      )}

      <h2>Census</h2>
      <table className="table">
        <thead><tr><th>Ward</th><th>Total</th><th>Occupied</th><th>Free</th><th>Cleaning</th><th>Interim</th></tr></thead>
        <tbody>
          {census.map((c) => (
            <tr key={c.wardId}><td>{c.wardName}</td><td>{c.totalBeds}</td><td>{c.occupied}</td><td>{c.free}</td><td>{c.cleaning}</td><td>{c.interimCount}</td></tr>
          ))}
          {census.length === 0 && <tr><td colSpan={6} className="muted">No census data.</td></tr>}
        </tbody>
      </table>

      <h2>Beds</h2>
      <table className="table">
        <thead><tr><th>Bed</th><th>Ward</th><th>Status</th><th></th></tr></thead>
        <tbody>
          {beds.map((b) => (
            <tr key={b.bedId}>
              <td>{b.bedNo}</td>
              <td>{b.wardName ?? b.wardId}</td>
              <td>{statusLabel(b.status)}</td>
              <td>{b.status === 5 && <button className="btn" onClick={() => cleaning.mutate(b.bedId)}>Cleaning Done</button>}</td>
            </tr>
          ))}
          {beds.length === 0 && <tr><td colSpan={4} className="muted">No beds.</td></tr>}
        </tbody>
      </table>

      <h2>Active Admissions</h2>
      <table className="table">
        <thead><tr><th>IPD No</th><th>Patient</th><th>Type</th><th>Doctor</th><th>MLC</th><th></th></tr></thead>
        <tbody>
          {admissions.map((a) => (
            <tr key={a.admissionId}>
              <td>{a.ipdNo}</td>
              <td>{a.patientId}</td>
              <td>{a.admissionType}</td>
              <td>{a.doctorId}</td>
              <td>{a.isMlc ? "Yes" : "No"}</td>
              <td>
                <button className="btn btn--small" onClick={() => discharge.mutate({ id: a.admissionId, payload: {} })}>Discharge</button>
              </td>
            </tr>
          ))}
          {admissions.length === 0 && <tr><td colSpan={6} className="muted">No active admissions.</td></tr>}
        </tbody>
      </table>
    </section>
  );
}