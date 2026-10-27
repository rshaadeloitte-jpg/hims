import { useState } from "react";
import { useNavigate } from "react-router-dom";
import {
  useCreateBill,
  useCreateCharge,
  usePatientSearch,
} from "./queries";
import { type ChargeValues } from "./zodSchema";
import { createBillSchema, chargeSchema, type CreateBillValues } from "./zodSchema";
import { zodResolver } from "@hookform/resolvers/zod";
import { useForm } from "react-hook-form";
import { Button, Checkbox, Table } from "@chakra-ui/react";
import { FormField, TextInput } from "../../components/ui";

export function NewBillPage() {
  const navigate = useNavigate();
  const [patientQuery, setPatientQuery] = useState("");
  const { data: patients = [] } = usePatientSearch(patientQuery);
  const [selectedPatient, setSelectedPatient] = useState<{ id: number; name: string } | null>(null);
  const [charges, setCharges] = useState<ChargeValues[]>([]);

  const billForm = useForm<CreateBillValues>({
    resolver: zodResolver(createBillSchema),
    defaultValues: { isSelfCompany: true, cashCounterId: 1 },
  });

  const createBill = useCreateBill();
  const createCharge = useCreateCharge();
  const isSelfCompany = billForm.watch("isSelfCompany");

  const handleAddCharge = (charge: ChargeValues) => {
    setCharges((prev) => [...prev, charge]);
  };

  const handleRemoveCharge = (index: number) => {
    setCharges((prev) => prev.filter((_, i) => i !== index));
  };

  const handleSubmit = async (values: CreateBillValues) => {
    try {
      const bill = await createBill.mutateAsync(values);
      // Create charges if any
      for (const charge of charges) {
        await createCharge.mutateAsync({ ...charge, billId: bill.billId });
      }
      navigate(`/billing/${bill.billId}`);
    } catch (err) {
      console.error(err);
    }
  };

  return (
    <section className="page">
      <header className="page__header">
        <h1>New Bill</h1>
      </header>

      <form className="form" onSubmit={billForm.handleSubmit(handleSubmit)}>
        <fieldset>
          <legend>Patient</legend>
          {!selectedPatient ? (
            <>
              <TextInput
                type="text"
                placeholder="Search by name, mobile, ABHA..."
                value={patientQuery}
                onChange={(e) => setPatientQuery(e.target.value)}
              />
              {patients.length > 0 && (
                <ul className="search-results">
                  {patients.map((p: any) => (
                    <li key={p.patientId} onClick={() => {
                      setSelectedPatient({ id: p.patientId, name: p.name ?? `Patient ${p.patientId}` });
                      billForm.setValue("patientId", p.patientId);
                    }}>
                      {p.name ?? `Patient ${p.patientId}`} — {p.mobileNo ?? ""}
                    </li>
                  ))}
                </ul>
              )}
            </>
          ) : (
            <div className="selected-patient">
              {selectedPatient.name} (ID: {selectedPatient.id})
              <Button type="button" variant="ghost" size="sm" onClick={() => {
                setSelectedPatient(null);
                billForm.setValue("patientId", undefined as any);
              }}>Change</Button>
            </div>
          )}
        </fieldset>

        <fieldset>
          <legend>Bill Options</legend>
          <FormField label="Encounter ID">
            <TextInput type="number" {...billForm.register("encounterId")} />
          </FormField>
          <FormField label="Admission ID">
            <TextInput type="number" {...billForm.register("admissionId")} />
          </FormField>
          <FormField label="Prescriber Doctor">
            <TextInput type="number" {...billForm.register("prescriberDoctorId")} />
          </FormField>
          <Checkbox.Root
            gap={2}
            checked={!!isSelfCompany}
            onCheckedChange={(e) => billForm.setValue("isSelfCompany", !!e.checked, { shouldValidate: true })}
          >
            <Checkbox.HiddenInput {...billForm.register("isSelfCompany")} />
            <Checkbox.Control />
            <Checkbox.Label>Self Company</Checkbox.Label>
          </Checkbox.Root>
        </fieldset>

        <fieldset>
          <legend>Charges</legend>
          <ChargeEntryForm onAdd={handleAddCharge} />
          {charges.length > 0 && (
            <Table.Root size="sm" variant="line">
              <Table.Header><Table.Row><Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Service</Table.ColumnHeader><Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Rate</Table.ColumnHeader><Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Qty</Table.ColumnHeader><Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase"></Table.ColumnHeader></Table.Row></Table.Header>
              <Table.Body>
                {charges.map((c, i) => (
                  <Table.Row key={i}>
                    <Table.Cell>{c.serviceName}</Table.Cell>
                    <Table.Cell>{c.rate?.toFixed(2)}</Table.Cell>
                    <Table.Cell>{c.qty}</Table.Cell>
                    <Table.Cell><Button type="button" variant="ghost" size="sm" onClick={() => handleRemoveCharge(i)}>✕</Button></Table.Cell>
                  </Table.Row>
                ))}
              </Table.Body>
            </Table.Root>
          )}
        </fieldset>

        <Button type="submit" variant="primary" disabled={createBill.isPending || !selectedPatient} loading={createBill.isPending} loadingText="Creating...">
          Create Bill
        </Button>
      </form>
    </section>
  );
}

function ChargeEntryForm({ onAdd }: { onAdd: (c: ChargeValues) => void }) {
  const { register, handleSubmit, reset } = useForm<ChargeValues>({
    resolver: zodResolver(chargeSchema),
    defaultValues: { qty: 1 },
  });

  const submit = (values: ChargeValues) => {
    onAdd(values);
    reset();
  };

  return (
    <form className="inline-form" onSubmit={handleSubmit(submit)}>
      <TextInput type="number" placeholder="Service ID" {...register("serviceId")} />
      <TextInput placeholder="Service Name" {...register("serviceName")} />
      <TextInput type="number" step="0.01" placeholder="Rate" {...register("rate")} />
      <TextInput type="number" placeholder="Qty" {...register("qty")} />
      <Button type="submit" variant="primary">+ Add</Button>
    </form>
  );
}
