import { useState } from "react";
import { useDrugAllergies, useCreateDrugAllergy, useDrugInteractions, useCreateDrugInteraction } from "./queries";
import { drugAllergySchema, drugInteractionSchema, type DrugAllergyValues, type DrugInteractionValues } from "./zodSchema";
import { zodResolver } from "@hookform/resolvers/zod";
import { useForm } from "react-hook-form";
import { Button, Table } from "@chakra-ui/react";
import { HimsSelect, TextInput } from "../../components/ui";

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
        <TextInput type="number" placeholder="Patient ID" value={input} onChange={(e) => setInput(e.target.value)} />
        <Button variant="primary" onClick={loadPatient}>Load</Button>
      </div>

      {patientId > 0 && (
        <>
          <h2>Allergies</h2>
          <Table.Root size="sm" variant="line">
            <Table.Header><Table.Row><Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Allergen</Table.ColumnHeader><Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Type</Table.ColumnHeader><Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Severity</Table.ColumnHeader><Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Reaction</Table.ColumnHeader></Table.Row></Table.Header>
            <Table.Body>
              {allergies.map((a) => (
                <Table.Row key={a.allergyId}><Table.Cell>{a.allergen}</Table.Cell><Table.Cell>{a.allergyType ?? "—"}</Table.Cell><Table.Cell>{a.severity ?? "—"}</Table.Cell><Table.Cell>{a.reaction ?? "—"}</Table.Cell></Table.Row>
              ))}
              {allergies.length === 0 && <Table.Row><Table.Cell colSpan={4} className="muted">No allergies on record.</Table.Cell></Table.Row>}
            </Table.Body>
          </Table.Root>
          <form className="inline-form" onSubmit={allergyForm.handleSubmit(submitAllergy)}>
            <TextInput placeholder="Allergen" {...allergyForm.register("allergen")} />
            <TextInput placeholder="Type" {...allergyForm.register("allergyType")} />
            <HimsSelect {...allergyForm.register("severity")}>
              <option value="">Severity</option>
              <option value="MILD">Mild</option>
              <option value="MODERATE">Moderate</option>
              <option value="SEVERE">Severe</option>
            </HimsSelect>
            <Button type="submit" variant="primary" loading={createAllergy.isPending} loadingText="Adding...">+ Add Allergy</Button>
          </form>

          <h2>Interactions</h2>
          <Table.Root size="sm" variant="line">
            <Table.Header><Table.Row><Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Drug 1</Table.ColumnHeader><Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Drug 2</Table.ColumnHeader><Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Severity</Table.ColumnHeader><Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Acknowledged</Table.ColumnHeader></Table.Row></Table.Header>
            <Table.Body>
              {interactions.map((i) => (
                <Table.Row key={i.interactionId}><Table.Cell>{i.drug1Id}</Table.Cell><Table.Cell>{i.drug2Id}</Table.Cell><Table.Cell>{i.severity ?? "—"}</Table.Cell><Table.Cell>{i.acknowledged ? "Yes" : "No"}</Table.Cell></Table.Row>
              ))}
              {interactions.length === 0 && <Table.Row><Table.Cell colSpan={4} className="muted">No interactions on record.</Table.Cell></Table.Row>}
            </Table.Body>
          </Table.Root>
          <form className="inline-form" onSubmit={interactionForm.handleSubmit(submitInteraction)}>
            <TextInput type="number" placeholder="Drug 1 ID" {...interactionForm.register("drug1Id")} />
            <TextInput type="number" placeholder="Drug 2 ID" {...interactionForm.register("drug2Id")} />
            <HimsSelect {...interactionForm.register("severity")}>
              <option value="">Severity</option>
              <option value="MILD">Mild</option>
              <option value="MODERATE">Moderate</option>
              <option value="SEVERE">Severe</option>
            </HimsSelect>
            <Button type="submit" variant="primary" loading={createInteraction.isPending} loadingText="Saving...">+ Record</Button>
          </form>
        </>
      )}
    </section>
  );
}
