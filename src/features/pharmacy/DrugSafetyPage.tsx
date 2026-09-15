import { useState } from "react";
import { useDrugAllergies, useCreateDrugAllergy, useDrugInteractions, useCreateDrugInteraction } from "./queries";
import { drugAllergySchema, drugInteractionSchema, type DrugAllergyValues, type DrugInteractionValues } from "./zodSchema";
import { zodResolver } from "@hookform/resolvers/zod";
import { useForm } from "react-hook-form";

export function DrugSafetyPage() {
  const [patientId, setPatientId] = useState<number>(0);
  const [input, setInput] = useState("");
  const { data: allergies = [] } = useDrugAllergies(patientId);
  const { data: interactions = [] } = useDrugInteractions(patientId);
  const createAllergy = useCreateDrugAllergy();
  const createInteraction = useCreateDrugInteraction();

  const allergyForm = useForm<DrugAllergyValues>({ resolver: zodResolver(drugAllergySchema) });
  const interactionForm = useForm<DrugInteractionValues>({ resolver: zodResolver(drugInteractionSchema) });

  const loadPatient = () => {
    const id = Number(input);
    if (id > 0) setPatientId(id);
  };

  const submitAllergy = async (v: DrugAllergyValues) => {
    await createAllergy.mutateAsync({ ...v, patientId });
    allergyForm.reset();
  };
  const submitInteraction = async (v: DrugInteractionValues) => {
    await createInteraction.mutateAsync({ ...v, patientId });
    interactionForm.reset();
  };

  return (
    <section className="page">
      <header className="page__header"><h1>Drug Safety</h1></header>
      <div className="row">
        <input type="number" placeholder="Patient ID" value={input} onChange={(e) => setInput(e.target.value)} />
        <button className="btn btn--primary" onClick={loadPatient}>Load</button>
      </div>

      {patientId > 0 && (
        <>
          <h2>Allergies</h2>
          <table className="table">
            <thead><tr><th>Allergen</th><th>Type</th><th>Severity</th><th>Reaction</th></tr></thead>
            <tbody>
              {allergies.map((a) => (
                <tr key={a.allergyId}><td>{a.allergen}</td><td>{a.allergyType ?? "—"}</td><td>{a.severity ?? "—"}</td><td>{a.reaction ?? "—"}</td></tr>
              ))}
              {allergies.length === 0 && <tr><td colSpan={4} className="muted">No allergies on record.</td></tr>}
            </tbody>
          </table>
          <form className="inline-form" onSubmit={allergyForm.handleSubmit(submitAllergy)}>
            <input placeholder="Allergen" {...allergyForm.register("allergen")} />
            <input placeholder="Type" {...allergyForm.register("allergyType")} />
            <select {...allergyForm.register("severity")}>
              <option value="">Severity</option>
              <option value="MILD">Mild</option>
              <option value="MODERATE">Moderate</option>
              <option value="SEVERE">Severe</option>
            </select>
            <button type="submit" className="btn" disabled={createAllergy.isPending}>+ Add Allergy</button>
          </form>

          <h2>Interactions</h2>
          <table className="table">
            <thead><tr><th>Drug 1</th><th>Drug 2</th><th>Severity</th><th>Acknowledged</th></tr></thead>
            <tbody>
              {interactions.map((i) => (
                <tr key={i.interactionId}><td>{i.drug1Id}</td><td>{i.drug2Id}</td><td>{i.severity ?? "—"}</td><td>{i.acknowledged ? "Yes" : "No"}</td></tr>
              ))}
              {interactions.length === 0 && <tr><td colSpan={4} className="muted">No interactions on record.</td></tr>}
            </tbody>
          </table>
          <form className="inline-form" onSubmit={interactionForm.handleSubmit(submitInteraction)}>
            <input type="number" placeholder="Drug 1 ID" {...interactionForm.register("drug1Id")} />
            <input type="number" placeholder="Drug 2 ID" {...interactionForm.register("drug2Id")} />
            <select {...interactionForm.register("severity")}>
              <option value="">Severity</option>
              <option value="MILD">Mild</option>
              <option value="MODERATE">Moderate</option>
              <option value="SEVERE">Severe</option>
            </select>
            <button type="submit" className="btn" disabled={createInteraction.isPending}>+ Record</button>
          </form>
        </>
      )}
    </section>
  );
}