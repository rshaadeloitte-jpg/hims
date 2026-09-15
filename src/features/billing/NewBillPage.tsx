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
              <input
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
              <button type="button" onClick={() => {
                setSelectedPatient(null);
                billForm.setValue("patientId", undefined as any);
              }}>Change</button>
            </div>
          )}
        </fieldset>

        <fieldset>
          <legend>Bill Options</legend>
          <label>
            Encounter ID
            <input type="number" {...billForm.register("encounterId")} />
          </label>
          <label>
            Admission ID
            <input type="number" {...billForm.register("admissionId")} />
          </label>
          <label>
            Prescriber Doctor
            <input type="number" {...billForm.register("prescriberDoctorId")} />
          </label>
          <label>
            <input type="checkbox" {...billForm.register("isSelfCompany")} />
            Self Company
          </label>
        </fieldset>

        <fieldset>
          <legend>Charges</legend>
          <ChargeEntryForm onAdd={handleAddCharge} />
          {charges.length > 0 && (
            <table className="table">
              <thead><tr><th>Service</th><th>Rate</th><th>Qty</th><th></th></tr></thead>
              <tbody>
                {charges.map((c, i) => (
                  <tr key={i}>
                    <td>{c.serviceName}</td>
                    <td>{c.rate?.toFixed(2)}</td>
                    <td>{c.qty}</td>
                    <td><button type="button" onClick={() => handleRemoveCharge(i)}>✕</button></td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </fieldset>

        <button type="submit" className="btn btn--primary" disabled={createBill.isPending || !selectedPatient}>
          {createBill.isPending ? "Creating..." : "Create Bill"}
        </button>
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
      <input type="number" placeholder="Service ID" {...register("serviceId")} />
      <input placeholder="Service Name" {...register("serviceName")} />
      <input type="number" step="0.01" placeholder="Rate" {...register("rate")} />
      <input type="number" placeholder="Qty" {...register("qty")} />
      <button type="submit" className="btn">+ Add</button>
    </form>
  );
}
