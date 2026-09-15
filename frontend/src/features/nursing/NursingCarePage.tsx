import { useState } from "react";
import {
  useCarePlans,
  useCreateCarePlan,
  useResolveCarePlan,
  useShiftHandovers,
  useCreateShiftHandover,
  useSignHandover,
} from "./queries";
import {
  carePlanSchema,
  shiftHandoverSchema,
  type CarePlanValues,
  type ShiftHandoverValues,
} from "./zodSchema";
import { zodResolver } from "@hookform/resolvers/zod";
import { useForm } from "react-hook-form";

export function NursingCarePage() {
  const [admissionId, setAdmissionId] = useState(0);
  const { data: carePlans = [] } = useCarePlans(admissionId || undefined);
  const { data: handovers = [] } = useShiftHandovers();
  const createPlan = useCreateCarePlan();
  const resolvePlan = useResolveCarePlan();
  const createHandover = useCreateShiftHandover();
  const signHandover = useSignHandover();

  const planForm = useForm<CarePlanValues>({ resolver: zodResolver(carePlanSchema) });
  const handoverForm = useForm<ShiftHandoverValues>({ resolver: zodResolver(shiftHandoverSchema) });

  const submitPlan = async (v: CarePlanValues) => {
    await createPlan.mutateAsync(v);
    planForm.reset();
  };
  const submitHandover = async (v: ShiftHandoverValues) => {
    await createHandover.mutateAsync(v);
    handoverForm.reset();
  };

  return (
    <section className="page">
      <header className="page__header"><h1>Nursing — Care Plans & Handovers</h1></header>
      <div className="row">
        <input type="number" placeholder="Admission ID" value={admissionId || ""} onChange={(e) => setAdmissionId(Number(e.target.value))} />
      </div>

      <h2>Care Plans</h2>
      <table className="table">
        <thead><tr><th>Title</th><th>Admission</th><th>Goals</th><th>Status</th><th></th></tr></thead>
        <tbody>
          {carePlans.map((p) => (
            <tr key={p.carePlanId}>
              <td>{p.title}</td>
              <td>{p.admissionId}</td>
              <td>{p.goals?.length ?? 0}</td>
              <td>{p.resolved ? "Resolved" : "Active"}</td>
              <td>{!p.resolved && <button className="btn" onClick={() => resolvePlan.mutate(p.carePlanId)}>Resolve</button>}</td>
            </tr>
          ))}
          {carePlans.length === 0 && <tr><td colSpan={5} className="muted">No care plans.</td></tr>}
        </tbody>
      </table>
      <form className="inline-form" onSubmit={planForm.handleSubmit(submitPlan)}>
        <input type="number" placeholder="Admission ID" {...planForm.register("admissionId")} />
        <input type="number" placeholder="Patient ID" {...planForm.register("patientId")} />
        <input placeholder="Title" {...planForm.register("title")} />
        <input placeholder="Description" {...planForm.register("description")} />
        <button type="submit" className="btn" disabled={createPlan.isPending}>+ Add Plan</button>
      </form>

      <h2>Shift Handovers</h2>
      <table className="table">
        <thead><tr><th>Shift</th><th>From</th><th>To</th><th>Admission</th><th>Signed</th><th></th></tr></thead>
        <tbody>
          {handovers.map((h) => (
            <tr key={h.handoverId}>
              <td>{h.shift}</td>
              <td>{h.fromNurseId}</td>
              <td>{h.toNurseId}</td>
              <td>{h.admissionId}</td>
              <td>{h.signed ? "Yes" : "No"}</td>
              <td>{!h.signed && <button className="btn" onClick={() => signHandover.mutate(h.handoverId)}>Sign</button>}</td>
            </tr>
          ))}
          {handovers.length === 0 && <tr><td colSpan={6} className="muted">No handovers.</td></tr>}
        </tbody>
      </table>
      <form className="inline-form" onSubmit={handoverForm.handleSubmit(submitHandover)}>
        <input type="number" placeholder="Admission ID" {...handoverForm.register("admissionId")} />
        <input type="number" placeholder="Patient ID" {...handoverForm.register("patientId")} />
        <input type="number" placeholder="From Nurse ID" {...handoverForm.register("fromNurseId")} />
        <input type="number" placeholder="To Nurse ID" {...handoverForm.register("toNurseId")} />
        <input placeholder="Shift" {...handoverForm.register("shift")} />
        <button type="submit" className="btn" disabled={createHandover.isPending}>+ Handover</button>
      </form>
    </section>
  );
}