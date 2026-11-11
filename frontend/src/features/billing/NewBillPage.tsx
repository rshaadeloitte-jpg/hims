import { useState } from "react";
import { useNavigate } from "react-router-dom";
import {
  useCreateBill,
  useCreateCharge,
  usePatientSearch,
} from "./queries";
import { type ChargeValues, createBillSchema, chargeSchema, type CreateBillValues } from "./zodSchema";
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
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

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
    setErrorMessage(null);
    if (!selectedPatient) {
      setErrorMessage("Please select a patient");
      return;
    }
    if (charges.length === 0) {
      setErrorMessage("At least one charge is required to create a bill");
      return;
    }

    setIsSubmitting(true);
    try {
      // 1. For each charge row POST /api/v1/charges {serviceId, serviceName, rate, qty} -> response has chargeId
      const chargeIds: number[] = [];
      for (const charge of charges) {
        const createdCharge = await createCharge.mutateAsync({
          serviceId: Number(charge.serviceId),
          serviceName: charge.serviceName,
          rate: Number(charge.rate),
          qty: Number(charge.qty),
        });
        const chargeId = createdCharge?.chargeId ?? (createdCharge as any)?.id;
        if (!chargeId) {
          throw new Error("Failed to create charge: missing chargeId in response");
        }
        chargeIds.push(Number(chargeId));
      }

      // 2. POST /api/v1/bills {patientId, encounterId?, admissionId?, prescriberDoctorId?, isSelfCompany, chargeIds:[...]} -> response has billId
      const billPayload: Record<string, unknown> = {
        patientId: Number(values.patientId ?? selectedPatient.id),
        cashCounterId: Number(values.cashCounterId ?? 1),
        isSelfCompany: values.isSelfCompany ?? true,
        chargeIds,
      };
      if (values.encounterId != null && String(values.encounterId) !== "") {
        billPayload.encounterId = Number(values.encounterId);
      }
      if (values.admissionId != null && String(values.admissionId) !== "") {
        billPayload.admissionId = Number(values.admissionId);
      }
      if (values.prescriberDoctorId != null && String(values.prescriberDoctorId) !== "") {
        billPayload.prescriberDoctorId = Number(values.prescriberDoctorId);
      }

      const bill = await createBill.mutateAsync(billPayload);
      const billId = bill?.billId ?? (bill as any)?.id;
      if (!billId) {
        throw new Error("Failed to create bill: missing billId in response");
      }
      navigate(`/billing/${billId}`);
    } catch (err: any) {
      const msg = err?.message || (typeof err === "string" ? err : "Failed to create bill");
      setErrorMessage(msg);
      console.error("Failed to create bill:", err);
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <section className="page">
      <header className="page__header">
        <h1>New Bill</h1>
      </header>

      {errorMessage && (
        <div className="alert alert--error" role="alert" style={{ marginBottom: "1rem" }}>
          {errorMessage}
        </div>
      )}

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
                  {patients.map((p: any) => {
                    const pid = Number(p.id ?? p.patientId);
                    const fullName = [p.firstName, p.middleName, p.lastName].filter(Boolean).join(" ");
                    const pname = p.name || fullName || `Patient ${pid}`;
                    const pmobile = p.mobile ?? p.mobileNo ?? "";
                    return (
                      <li
                        key={pid}
                        onClick={() => {
                          setSelectedPatient({ id: pid, name: pname });
                          billForm.setValue("patientId", pid, { shouldValidate: true });
                        }}
                      >
                        {pname} — {pmobile}
                      </li>
                    );
                  })}
                </ul>
              )}
            </>
          ) : (
            <div className="selected-patient">
              {selectedPatient.name} (ID: {selectedPatient.id})
              <Button
                type="button"
                variant="ghost"
                size="sm"
                onClick={() => {
                  setSelectedPatient(null);
                  billForm.setValue("patientId", undefined as any, { shouldValidate: true });
                }}
              >
                Change
              </Button>
            </div>
          )}
          {billForm.formState.errors.patientId && (
            <p className="field__error" role="alert">
              {billForm.formState.errors.patientId.message}
            </p>
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
              <Table.Header>
                <Table.Row>
                  <Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">
                    Service
                  </Table.ColumnHeader>
                  <Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">
                    Rate
                  </Table.ColumnHeader>
                  <Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">
                    Qty
                  </Table.ColumnHeader>
                  <Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">
                    Total
                  </Table.ColumnHeader>
                  <Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase"></Table.ColumnHeader>
                </Table.Row>
              </Table.Header>
              <Table.Body>
                {charges.map((c, i) => (
                  <Table.Row key={i}>
                    <Table.Cell>
                      {c.serviceName} (#{c.serviceId})
                    </Table.Cell>
                    <Table.Cell>{Number(c.rate).toFixed(2)}</Table.Cell>
                    <Table.Cell>{c.qty}</Table.Cell>
                    <Table.Cell>{(Number(c.rate) * Number(c.qty)).toFixed(2)}</Table.Cell>
                    <Table.Cell>
                      <Button type="button" variant="ghost" size="sm" onClick={() => handleRemoveCharge(i)}>
                        ✕
                      </Button>
                    </Table.Cell>
                  </Table.Row>
                ))}
              </Table.Body>
            </Table.Root>
          )}
        </fieldset>

        <Button
          type="submit"
          variant="primary"
          disabled={isSubmitting || createBill.isPending || createCharge.isPending || !selectedPatient}
          loading={isSubmitting || createBill.isPending || createCharge.isPending}
          loadingText="Creating..."
          onClick={(e) => {
            e.preventDefault();
            void billForm.handleSubmit(handleSubmit)(e);
          }}
        >
          Create Bill
        </Button>
      </form>
    </section>
  );
}

function ChargeEntryForm({ onAdd }: { onAdd: (c: ChargeValues) => void }) {
  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<ChargeValues>({
    resolver: zodResolver(chargeSchema),
    defaultValues: { qty: 1 },
  });

  const submit = (values: ChargeValues) => {
    onAdd(values);
    reset({ qty: 1, serviceId: undefined as any, serviceName: "", rate: undefined as any });
  };

  return (
    <div
      className="inline-form"
      onKeyDown={(e) => {
        if (e.key === "Enter") {
          e.preventDefault();
          e.stopPropagation();
          void handleSubmit(submit)();
        }
      }}
    >
      <TextInput type="number" placeholder="Service ID" {...register("serviceId")} />
      <TextInput placeholder="Service Name" {...register("serviceName")} />
      <TextInput type="number" step="0.01" placeholder="Rate" {...register("rate")} />
      <TextInput type="number" placeholder="Qty" {...register("qty")} />
      <Button
        type="button"
        variant="primary"
        onClick={(e) => {
          e.preventDefault();
          e.stopPropagation();
          void handleSubmit(submit)(e);
        }}
      >
        + Add
      </Button>
      {errors.serviceId && <p className="field__error" role="alert">{errors.serviceId.message}</p>}
      {errors.serviceName && <p className="field__error" role="alert">{errors.serviceName.message}</p>}
      {errors.rate && <p className="field__error" role="alert">{errors.rate.message}</p>}
      {errors.qty && <p className="field__error" role="alert">{errors.qty.message}</p>}
    </div>
  );
}
