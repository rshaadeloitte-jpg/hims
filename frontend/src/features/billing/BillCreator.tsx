import { useState, useCallback } from "react";
import { useNavigate } from "react-router-dom";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import {
  useCreateBill,
  usePatientSearch,
  useConcessionPlans,
  useAvailablePackages,
  useSponsorCompanies,
} from "./queries";
import { createBillSchema, type CreateBillValues } from "./zodSchema";
import { ChargeItems } from "./ChargeItems";
import { type ChargeValues } from "./zodSchema";
import { EstimationPreview } from "./EstimationPreview";
import { Button, Checkbox } from "@chakra-ui/react";
import { FormField, TextInput } from "../../components/ui";

interface BillCreatorProps {
  onBillCreated?: (billId: number) => void;
}

/**
 * BillCreator — main bill creation orchestrator.
 * Stripe-inspired layout: patient search on the left, charge items on the right,
 * estimation preview slides in as a bottom panel. Follows the
 * "create then add charges" pattern from the billing-service.
 */
export function BillCreator({ onBillCreated }: BillCreatorProps) {
  const navigate = useNavigate();
  const [selectedPatient, setSelectedPatient] = useState<{ id: number; name: string } | null>(null);
  const [patientQuery, setPatientQuery] = useState("");
  const [charges, setCharges] = useState<ChargeValues[]>([]);
  const [showEstimation, setShowEstimation] = useState(false);

  const { data: patients = [] } = usePatientSearch(patientQuery);
  const { data: concessionPlans = [] } = useConcessionPlans();
  const { data: packages = [] } = useAvailablePackages();
  const { data: sponsors = [] } = useSponsorCompanies();

  const createBill = useCreateBill();

  const {
    register,
    handleSubmit,
    setValue,
    watch,
    formState: { errors },
  } = useForm<CreateBillValues>({
    resolver: zodResolver(createBillSchema),
    defaultValues: {
      isSelfCompany: true,
      cashCounterId: 1,
      patientId: undefined as unknown as number,
    },
  });

  const isSelfCompany = watch("isSelfCompany");

  const handleSelectPatient = useCallback(
    (patient: { id: number; name: string }) => {
      setSelectedPatient(patient);
      setValue("patientId", patient.id);
    },
    [setValue],
  );

  const handleAddCharge = useCallback((charge: ChargeValues) => {
    setCharges((prev) => [...prev, charge]);
  }, []);

  const handleRemoveCharge = useCallback((index: number) => {
    setCharges((prev) => prev.filter((_, i) => i !== index));
  }, []);

  const handleClearCharges = useCallback(() => {
    setCharges([]);
  }, []);

  const handleSubmitBill = async (values: CreateBillValues) => {
    if (!selectedPatient) return;
    try {
      const bill = await createBill.mutateAsync({
        ...values,
        patientId: selectedPatient.id,
      });
      onBillCreated?.(bill.billId);
      navigate(`/billing/${bill.billId}`);
    } catch (err) {
      console.error("Bill creation failed:", err);
    }
  };

  const subtotal = charges.reduce((sum, c) => sum + c.rate * c.qty, 0);
  const canSubmit = selectedPatient && charges.length > 0 && !createBill.isPending;

  return (
    <div className="bill-creator">
      <div className="bill-creator__header">
        <h2 className="bill-creator__title">Create New Bill</h2>
        <div className="bill-creator__actions">
          <Button
            type="button"
            variant="secondary"
            onClick={() => setShowEstimation(!showEstimation)}
            disabled={charges.length === 0}
          >
            {showEstimation ? "Hide Estimate" : "Preview Estimate"}
          </Button>
          <Button
            type="button"
            variant="secondary"
            onClick={() => navigate("/billing")}
          >
            Cancel
          </Button>
        </div>
      </div>

      <form onSubmit={handleSubmit(handleSubmitBill)} className="bill-creator__form">
        {/* Patient selection panel */}
        <section className="bill-creator__panel bill-creator__panel--patient">
          <h3 className="bill-creator__subtitle">Patient</h3>

          {!selectedPatient ? (
            <div className="bill-creator__search">
              <FormField label="Search patient">
                <TextInput
                  id="patient-search"
                  type="search"
                  placeholder="Name, MRN, mobile, ABHA..."
                  value={patientQuery}
                  onChange={(e) => setPatientQuery(e.target.value)}
                  autoComplete="off"
                />
              </FormField>
              {patients.length > 0 && (
                <ul className="bill-creator__results" role="listbox">
                  {patients.map((p: any) => (
                    <li key={p.patientId}>
                      <Button
                        type="button"
                        variant="ghost"
                        size="sm"
                        className="bill-creator__result"
                        onClick={() =>
                          handleSelectPatient({
                            id: p.patientId,
                            name: p.name ?? (`${p.firstName ?? ""} ${p.lastName ?? ""}`.trim() || `Patient ${p.patientId}`),
                          })
                        }
                      >
                        <span className="bill-creator__result-name">
                          {p.name ?? (`${p.firstName ?? ""} ${p.lastName ?? ""}`.trim() || `Patient ${p.patientId}`)}
                        </span>
                        <span className="bill-creator__result-meta">
                          {p.mobileNo ?? p.mobile ?? ""} {p.mrn ?? ""}
                        </span>
                      </Button>
                    </li>
                  ))}
                </ul>
              )}
            </div>
          ) : (
            <div className="bill-creator__selected-patient">
              <span className="bill-creator__patient-name">{selectedPatient.name}</span>
              <span className="bill-creator__patient-id">ID: {selectedPatient.id}</span>
              <Button
                type="button"
                variant="ghost"
                size="sm"
                onClick={() => {
                  setSelectedPatient(null);
                  setValue("patientId", undefined as unknown as number);
                  setPatientQuery("");
                }}
              >
                Change
              </Button>
            </div>
          )}
        </section>

        {/* Bill options panel */}
        <section className="bill-creator__panel bill-creator__panel--options">
          <h3 className="bill-creator__subtitle">Bill Options</h3>
          <div className="bill-creator__options-grid">
            <div className="field">
              <FormField label="Encounter ID" error={errors.encounterId?.message}>
                <TextInput
                  id="encounterId"
                  type="number"
                  {...register("encounterId")}
                />
              </FormField>
            </div>
            <div className="field">
              <FormField label="Admission ID" error={errors.admissionId?.message}>
                <TextInput
                  id="admissionId"
                  type="number"
                  {...register("admissionId")}
                />
              </FormField>
            </div>
            <div className="field">
              <FormField label="Prescriber Doctor">
                <TextInput
                  id="prescriberDoctorId"
                  type="number"
                  {...register("prescriberDoctorId")}
                />
              </FormField>
            </div>
            <div className="field field--check">
              <Checkbox.Root
                checked={!!isSelfCompany}
                onCheckedChange={(e) => setValue("isSelfCompany", !!e.checked, { shouldValidate: true })}
              >
                <Checkbox.HiddenInput {...register("isSelfCompany")} />
                <Checkbox.Control />
                <Checkbox.Label>Self Company</Checkbox.Label>
              </Checkbox.Root>
            </div>
          </div>
        </section>

        {/* Charges panel */}
        <section className="bill-creator__panel bill-creator__panel--charges">
          <div className="bill-creator__charges-header">
            <h3 className="bill-creator__subtitle">Charge Items</h3>
            {charges.length > 0 && (
              <Button
                type="button"
                variant="ghost"
                size="sm"
                onClick={handleClearCharges}
              >
                Clear all
              </Button>
            )}
          </div>
          <ChargeItems
            charges={charges}
            onAdd={handleAddCharge}
            onRemove={handleRemoveCharge}
          />
          {charges.length > 0 && (
            <div className="bill-creator__subtotal">
              <span>Subtotal</span>
              <strong>₹ {subtotal.toFixed(2)}</strong>
            </div>
          )}
        </section>

        {/* Estimation preview */}
        {showEstimation && (
          <section className="bill-creator__panel bill-creator__panel--estimation">
            <EstimationPreview
              patientId={selectedPatient?.id}
              charges={charges}
              isSelfCompany={isSelfCompany}
              sponsorCompanyId={watch("sponsorCompanyId" as any)}
              concessionPlans={concessionPlans}
              packages={packages}
              sponsors={sponsors}
            />
          </section>
        )}

        {/* Submit */}
        <div className="bill-creator__footer">
          <Button
            type="submit"
            variant="primary"
            disabled={!canSubmit}
            loading={createBill.isPending}
            loadingText="Creating Bill..."
          >
            Create Bill
          </Button>
          {!selectedPatient && (
            <p className="bill-creator__hint">Select a patient to continue</p>
          )}
          {selectedPatient && charges.length === 0 && (
            <p className="bill-creator__hint">Add at least one charge item</p>
          )}
        </div>

        {createBill.isError && (
          <div className="alert alert--error" role="alert">
            Failed to create bill: {(createBill.error as Error)?.message ?? "Unknown error"}
          </div>
        )}
      </form>
    </div>
  );
}
