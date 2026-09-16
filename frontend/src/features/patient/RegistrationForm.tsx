import { useState, useCallback, useMemo } from "react";
import { useForm, useWatch } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { Button, Checkbox, Textarea } from "@chakra-ui/react";
import { FormField, HimsSelect, TextInput } from "../../components/ui";
import {
  patientRegistrationSchema,
  type PatientRegistrationValues,
  type MlcValues,
  type MotherChildLinkValues,
} from "./zodSchema";
import { useRegisterPatient, useUpdatePatient, type PatientDetail } from "./queries";
import { AbhaInput } from "./AbhaInput";
import { PhotoCapture } from "./PhotoCapture";

/**
 * RegistrationForm — Stripe-style form with real-time validation,
 * Linear-style keyboard navigation, Epic MyChart flow.
 * Multi-section with progress indicator and inline errors.
 */

type SectionKey = "identity" | "contact" | "demographics" | "mlc" | "motherChild" | "covid" | "photo";

const SECTION_ORDER: SectionKey[] = ["identity", "contact", "demographics", "photo", "mlc", "motherChild", "covid"];

const DEFAULT_VALUES: PatientRegistrationValues = {
  firstName: "",
  middleName: "",
  lastName: "",
  fatherName: "",
  motherName: "",
  mobileNo: "",
  emergencyContact: "",
  email: "",
  genderId: undefined,
  dateOfBirth: "",
  bloodGroup: "",
  maritalStatus: "",
  address: "",
  city: "",
  state: "",
  pincode: "",
  abhaAddress: "",
  aadhaarNumber: "",
  unitId: undefined,
  mlc: { isMlc: false, mlcNumber: "", mlcFirNumber: "", mlcPoliceStation: "", mlcNotes: "" },
  motherChildLink: { isChildPatient: false, motherPatientId: undefined, motherMrn: "", birthWeightGrams: undefined, gestationalAgeWeeks: undefined },
  covidTracking: { covidVaccinationStatus: undefined, lastCovidVaccineDate: "", covidInfectionHistory: false, lastCovidInfectionDate: "" },
  photo: { photoFileId: "", photoCapturedAt: "", photoDataUrl: "" },
};

export function RegistrationForm({
  patientId,
  initialData,
  onSaved,
  onCancel,
}: {
  patientId?: number;
  initialData?: PatientDetail;
  onSaved?: (id: number) => void;
  onCancel?: () => void;
}) {
  const [activeSection, setActiveSection] = useState<SectionKey>("identity");
  const [completedSections, setCompletedSections] = useState<Set<SectionKey>>(new Set());

  const isEdit = !!patientId;
  const registerMutation = useRegisterPatient();
  const updateMutation = useUpdatePatient(patientId ?? 0);

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
    trigger,
    setValue,
    getValues,
    reset,
    control,
    watch,
  } = useForm<PatientRegistrationValues>({
    resolver: zodResolver(patientRegistrationSchema),
    defaultValues: initialData ? mapPatientToForm(initialData) : DEFAULT_VALUES,
    mode: "onChange",
  });

  // Watch MLC and mother-child for conditional rendering
  const mlc = useWatch({ control, name: "mlc" }) as MlcValues;
  const motherChild = useWatch({ control, name: "motherChildLink" }) as MotherChildLinkValues;
  const covidInfectionHistory = watch("covidTracking.covidInfectionHistory");

  const sectionProgress = useMemo(() => {
    const completed = completedSections.size;
    const total = SECTION_ORDER.length;
    return Math.round((completed / total) * 100);
  }, [completedSections]);

  const validateSection = useCallback(
    async (section: SectionKey): Promise<boolean> => {
      let fields: (keyof PatientRegistrationValues)[] = [];
      switch (section) {
        case "identity":
          fields = ["firstName", "lastName", "abhaAddress", "aadhaarNumber"];
          break;
        case "contact":
          fields = ["mobileNo", "email", "emergencyContact", "pincode"];
          break;
        case "demographics":
          fields = ["genderId", "dateOfBirth", "bloodGroup", "address"];
          break;
        case "photo":
          return true; // optional
        case "mlc":
          fields = ["mlc"];
          break;
        case "motherChild":
          fields = ["motherChildLink"];
          break;
        case "covid":
          fields = ["covidTracking"];
          break;
      }
      return await trigger(fields, { shouldFocus: true });
    },
    [trigger],
  );

  const goToSection = useCallback(
    async (next: SectionKey) => {
      // Validate current section before leaving
      if (next !== activeSection) {
        const valid = await validateSection(activeSection);
        if (valid) {
          setCompletedSections((prev) => new Set(prev).add(activeSection));
        }
      }
      setActiveSection(next);
    },
    [activeSection, validateSection],
  );

  const handleNext = useCallback(async () => {
    const valid = await validateSection(activeSection);
    if (valid) {
      setCompletedSections((prev) => new Set(prev).add(activeSection));
      const idx = SECTION_ORDER.indexOf(activeSection);
      if (idx < SECTION_ORDER.length - 1) {
        setActiveSection(SECTION_ORDER[idx + 1]);
      }
    }
  }, [activeSection, validateSection]);

  const handlePrev = useCallback(() => {
    const idx = SECTION_ORDER.indexOf(activeSection);
    if (idx > 0) {
      setActiveSection(SECTION_ORDER[idx - 1]);
    }
  }, [activeSection]);

  const onSubmit = handleSubmit(async (values) => {
    const payload: Record<string, unknown> = {};
    // Strip empties so backend sees undefined (not "") for optionals
    for (const [k, v] of Object.entries(values)) {
      if (v === "" || v == null) continue;
      if (typeof v === "object" && !Array.isArray(v)) {
        // For nested objects, only include non-empty
        const nested: Record<string, unknown> = {};
        for (const [nk, nv] of Object.entries(v)) {
          if (nv !== "" && nv != null && nv !== false) {
            nested[nk] = nv;
          }
        }
        if (Object.keys(nested).length > 0) payload[k] = nested;
      } else {
        payload[k] = v;
      }
    }

    let saved: PatientDetail;
    if (isEdit && patientId) {
      saved = await updateMutation.mutateAsync(payload);
    } else {
      saved = await registerMutation.mutateAsync(payload);
    }
    onSaved?.(saved.patientId);
    reset();
  });

  const mutationError = registerMutation.isError
    ? (registerMutation.error as Error)
    : updateMutation.isError
      ? (updateMutation.error as Error)
      : null;

  const isPending = registerMutation.isPending || updateMutation.isPending;

  return (
    <form
      onSubmit={onSubmit}
      className="registration-form"
      aria-label="patient-registration-form"
      noValidate
    >
      {/* Progress header */}
      <header className="registration-form__header">
        <h1>{isEdit ? "Edit Patient" : "Register New Patient"}</h1>
        <div className="registration-form__progress" role="progressbar" aria-valuenow={sectionProgress} aria-valuemin={0} aria-valuemax={100}>
          <div className="registration-form__progress-bar" style={{ width: `${sectionProgress}%` }} />
          <span className="registration-form__progress-text">{sectionProgress}% complete</span>
        </div>
      </header>

      {/* Section nav — Linear-style */}
      <nav className="registration-form__nav" aria-label="Form sections">
        {SECTION_ORDER.map((section, index) => (
          <Button
            key={section}
            type="button"
            variant="ghost"
            size="sm"
            onClick={() => goToSection(section)}
            aria-current={activeSection === section ? "step" : undefined}
          >
            <span className="registration-form__nav-num">{index + 1}</span>
            <span className="registration-form__nav-label">{sectionLabel(section)}</span>
          </Button>
        ))}
      </nav>

      {/* Section content */}
      <main className="registration-form__body">
        {activeSection === "identity" && (
          <Section title="Identity" subtitle="Patient's legal name and identification">
            <div className="form-row form-row--3col">
              <FormField label="First name" required error={errors.firstName?.message}>
                <TextInput {...register("firstName")} autoFocus placeholder="First name" />
              </FormField>
              <FormField label="Middle name" error={errors.middleName?.message}>
                <TextInput {...register("middleName")} placeholder="Middle name" />
              </FormField>
              <FormField label="Last name" error={errors.lastName?.message}>
                <TextInput {...register("lastName")} placeholder="Last name" />
              </FormField>
            </div>
            <div className="form-row form-row--2col">
              <FormField label="Father's name" error={errors.fatherName?.message}>
                <TextInput {...register("fatherName")} placeholder="Father's name" />
              </FormField>
              <FormField label="Mother's name" error={errors.motherName?.message}>
                <TextInput {...register("motherName")} placeholder="Mother's name" />
              </FormField>
            </div>
            <FormField label="Aadhaar Number" error={errors.aadhaarNumber?.message}>
              <TextInput {...register("aadhaarNumber")} placeholder="XXXX XXXX XXXX" />
            </FormField>
            <AbhaInput
              value={getValues("abhaAddress") ?? ""}
              onChange={(v) => setValue("abhaAddress", v, { shouldValidate: true })}
              error={errors.abhaAddress?.message}
              onVerified={(info) => {
                if (info.name) {
                  const parts = info.name.split(" ");
                  if (!getValues("firstName")) setValue("firstName", parts[0]);
                  if (!getValues("lastName") && parts.length > 1) setValue("lastName", parts.slice(1).join(" "));
                }
                if (info.gender) setValue("genderId", info.gender === "M" ? 1 : info.gender === "F" ? 2 : 3);
                if (info.dateOfBirth) setValue("dateOfBirth", info.dateOfBirth);
              }}
            />
          </Section>
        )}

        {activeSection === "contact" && (
          <Section title="Contact Information" subtitle="How to reach the patient">
            <div className="form-row form-row--2col">
              <FormField label="Mobile" required error={errors.mobileNo?.message}>
                <TextInput {...register("mobileNo")} inputMode="numeric" placeholder="10-digit number" />
              </FormField>
              <FormField label="Emergency contact" error={errors.emergencyContact?.message}>
                <TextInput {...register("emergencyContact")} inputMode="numeric" placeholder="Emergency number" />
              </FormField>
            </div>
            <FormField label="Email" error={errors.email?.message}>
              <TextInput {...register("email")} type="email" placeholder="email@example.com" />
            </FormField>
            <FormField label="Address" error={errors.address?.message}>
              <Textarea bg="white" borderColor="gray.300" {...register("address")} rows={2} placeholder="Street address" />
            </FormField>
            <div className="form-row form-row--3col">
              <FormField label="City" error={errors.city?.message}>
                <TextInput {...register("city")} placeholder="City" />
              </FormField>
              <FormField label="State" error={errors.state?.message}>
                <TextInput {...register("state")} placeholder="State" />
              </FormField>
              <FormField label="Pincode" error={errors.pincode?.message}>
                <TextInput {...register("pincode")} inputMode="numeric" placeholder="6-digit pincode" />
              </FormField>
            </div>
          </Section>
        )}

        {activeSection === "demographics" && (
          <Section title="Demographics" subtitle="Clinical and personal details">
            <div className="form-row form-row--3col">
              <FormField label="Gender" required error={errors.genderId?.message}>
                <HimsSelect {...register("genderId")}>
                  <option value="">Select gender</option>
                  <option value={1}>Male</option>
                  <option value={2}>Female</option>
                  <option value={3}>Other</option>
                </HimsSelect>
              </FormField>
              <FormField label="Date of birth" error={errors.dateOfBirth?.message}>
                <TextInput type="date" {...register("dateOfBirth")} />
              </FormField>
              <FormField label="Blood group" error={errors.bloodGroup?.message}>
                <HimsSelect {...register("bloodGroup")}>
                  <option value="">Select</option>
                  {["A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-"].map((b) => (
                    <option key={b} value={b}>{b}</option>
                  ))}
                </HimsSelect>
              </FormField>
            </div>
            <FormField label="Marital status" error={errors.maritalStatus?.message}>
              <HimsSelect {...register("maritalStatus")}>
                <option value="">Select</option>
                <option value="SINGLE">Single</option>
                <option value="MARRIED">Married</option>
                <option value="DIVORCED">Divorced</option>
                <option value="WIDOWED">Widowed</option>
              </HimsSelect>
            </FormField>
          </Section>
        )}

        {activeSection === "photo" && (
          <Section title="Patient Photo" subtitle="For identification and records">
            <PhotoCapture
              value={getValues("photo.photoDataUrl") || undefined}
              onChange={(dataUrl, _file) => {
                setValue("photo.photoDataUrl", dataUrl);
                setValue("photo.photoCapturedAt", new Date().toISOString());
              }}
            />
          </Section>
        )}

        {activeSection === "mlc" && (
          <Section title="Medico-Legal Case (MLC)" subtitle="Mark if this is a legal case">
            <div className="mlc-section">
              <Checkbox.Root
                checked={!!mlc?.isMlc}
                onCheckedChange={(e) => setValue("mlc.isMlc", !!e.checked, { shouldValidate: true })}
              >
                <Checkbox.HiddenInput {...register("mlc.isMlc")} />
                <Checkbox.Control />
                <Checkbox.Label>This is a Medico-Legal Case (MLC)</Checkbox.Label>
              </Checkbox.Root>
              {mlc?.isMlc && (
                <>
                  <p className="mlc-section__help">
                    MLC cases require FIR details and police station information.
                  </p>
                  <div className="form-row form-row--2col">
                    <FormField label="MLC Number" error={errors.mlc?.mlcNumber?.message}>
                      <TextInput {...register("mlc.mlcNumber")} placeholder="MLC-XXXX" />
                    </FormField>
                    <FormField label="FIR Number" error={errors.mlc?.mlcFirNumber?.message}>
                      <TextInput {...register("mlc.mlcFirNumber")} placeholder="FIR Number" />
                    </FormField>
                  </div>
                  <FormField label="Police Station" error={errors.mlc?.mlcPoliceStation?.message}>
                    <TextInput {...register("mlc.mlcPoliceStation")} placeholder="Police station name" />
                  </FormField>
                  <FormField label="MLC Notes" error={errors.mlc?.mlcNotes?.message}>
                    <Textarea bg="white" borderColor="gray.300" {...register("mlc.mlcNotes")} rows={3} placeholder="Additional notes" />
                  </FormField>
                </>
              )}
            </div>
          </Section>
        )}

        {activeSection === "motherChild" && (
          <Section title="Mother-Child Link" subtitle="For pediatric patients">
            <Checkbox.Root
              checked={!!motherChild?.isChildPatient}
              onCheckedChange={(e) => setValue("motherChildLink.isChildPatient", !!e.checked, { shouldValidate: true })}
            >
              <Checkbox.HiddenInput {...register("motherChildLink.isChildPatient")} />
              <Checkbox.Control />
              <Checkbox.Label>This is a child patient (link to mother)</Checkbox.Label>
            </Checkbox.Root>
            {motherChild?.isChildPatient && (
              <div className="form-row form-row--2col">
                <FormField label="Mother's Patient ID" error={errors.motherChildLink?.motherPatientId?.message}>
                  <TextInput type="number" {...register("motherChildLink.motherPatientId")} placeholder="Mother's ID" />
                </FormField>
                <FormField label="Mother's MRN" error={errors.motherChildLink?.motherMrn?.message}>
                  <TextInput {...register("motherChildLink.motherMrn")} placeholder="Mother's MRN" />
                </FormField>
                <FormField label="Birth Weight (grams)" error={errors.motherChildLink?.birthWeightGrams?.message}>
                  <TextInput type="number" {...register("motherChildLink.birthWeightGrams")} placeholder="e.g. 3200" />
                </FormField>
                <FormField label="Gestational Age (weeks)" error={errors.motherChildLink?.gestationalAgeWeeks?.message}>
                  <TextInput type="number" {...register("motherChildLink.gestationalAgeWeeks")} placeholder="e.g. 38" />
                </FormField>
              </div>
            )}
          </Section>
        )}

        {activeSection === "covid" && (
          <Section title="COVID-19 Tracking" subtitle="Vaccination and infection history">
            <div className="form-row form-row--2col">
              <FormField label="Vaccination Status" error={errors.covidTracking?.covidVaccinationStatus?.message}>
                <HimsSelect {...register("covidTracking.covidVaccinationStatus")}>
                  <option value="">Select</option>
                  <option value="UNVACCINATED">Unvaccinated</option>
                  <option value="PARTIAL">Partially Vaccinated</option>
                  <option value="FULL">Fully Vaccinated</option>
                  <option value="BOOSTER">Booster Dose</option>
                </HimsSelect>
              </FormField>
              <FormField label="Last Vaccine Date" error={errors.covidTracking?.lastCovidVaccineDate?.message}>
                <TextInput type="date" {...register("covidTracking.lastCovidVaccineDate")} />
              </FormField>
            </div>
            <Checkbox.Root
              checked={!!covidInfectionHistory}
              onCheckedChange={(e) => setValue("covidTracking.covidInfectionHistory", !!e.checked, { shouldValidate: true })}
            >
              <Checkbox.HiddenInput {...register("covidTracking.covidInfectionHistory")} />
              <Checkbox.Control />
              <Checkbox.Label>Past COVID-19 infection</Checkbox.Label>
            </Checkbox.Root>
            {covidInfectionHistory && (
              <FormField label="Last Infection Date" error={errors.covidTracking?.lastCovidInfectionDate?.message}>
                <TextInput type="date" {...register("covidTracking.lastCovidInfectionDate")} />
              </FormField>
            )}
          </Section>
        )}
      </main>

      {/* Error banner */}
      {mutationError && (
        <div className="alert alert--error" role="alert">
          <strong>Save failed:</strong> {mutationError.message}
        </div>
      )}

      {/* Navigation actions */}
      <footer className="registration-form__actions">
        <Button
          type="button"
          variant="secondary"
          onClick={handlePrev}
          disabled={SECTION_ORDER.indexOf(activeSection) === 0}
        >
          ← Previous
        </Button>
        <div className="registration-form__actions-right">
          {onCancel && (
            <Button type="button" variant="secondary" onClick={onCancel}>
              Cancel
            </Button>
          )}
          {SECTION_ORDER.indexOf(activeSection) < SECTION_ORDER.length - 1 ? (
            <Button type="button" variant="primary" onClick={handleNext}>
              Next →
            </Button>
          ) : (
            <Button
              type="submit"
              variant="primary"
              disabled={isSubmitting}
              loading={isPending}
              loadingText="Saving..."
            >
              {isEdit ? "Update Patient" : "Register Patient"}
            </Button>
          )}
        </div>
      </footer>
    </form>
  );
}

// ---- helpers ----------------------------------------------------------------

function Section({
  title,
  subtitle,
  children,
}: {
  title: string;
  subtitle?: string;
  children: React.ReactNode;
}) {
  return (
    <fieldset className="form-section">
      <legend className="form-section__legend">
        <h2>{title}</h2>
        {subtitle && <p className="form-section__subtitle">{subtitle}</p>}
      </legend>
      {children}
    </fieldset>
  );
}

function sectionLabel(key: SectionKey): string {
  const labels: Record<SectionKey, string> = {
    identity: "Identity",
    contact: "Contact",
    demographics: "Demographics",
    photo: "Photo",
    mlc: "MLC",
    motherChild: "Mother-Child",
    covid: "COVID-19",
  };
  return labels[key];
}

function mapPatientToForm(p: PatientDetail): PatientRegistrationValues {
  return {
    firstName: p.firstName,
    middleName: p.middleName ?? "",
    lastName: p.lastName ?? "",
    fatherName: p.fatherName ?? "",
    motherName: p.motherName ?? "",
    mobileNo: p.mobileNo,
    emergencyContact: p.emergencyContact ?? "",
    email: p.email ?? "",
    genderId: p.genderId,
    dateOfBirth: p.dateOfBirth ?? "",
    bloodGroup: p.bloodGroup ?? "",
    maritalStatus: p.maritalStatus ?? "",
    address: p.address ?? "",
    city: p.city ?? "",
    state: p.state ?? "",
    pincode: p.pincode ?? "",
    abhaAddress: p.abhaAddress ?? "",
    aadhaarNumber: p.aadhaarNumber ?? "",
    unitId: p.unitId,
    mlc: {
      isMlc: p.isMlc ?? false,
      mlcNumber: p.mlcNumber ?? "",
      mlcFirNumber: p.mlcFirNumber ?? "",
      mlcPoliceStation: p.mlcPoliceStation ?? "",
      mlcNotes: p.mlcNotes ?? "",
    },
    motherChildLink: {
      isChildPatient: p.isChildPatient ?? false,
      motherPatientId: p.motherPatientId,
      motherMrn: p.motherMrn ?? "",
      birthWeightGrams: p.birthWeightGrams,
      gestationalAgeWeeks: p.gestationalAgeWeeks,
    },
    covidTracking: {
      covidVaccinationStatus: p.covidVaccinationStatus,
      lastCovidVaccineDate: p.lastCovidVaccineDate ?? "",
      covidInfectionHistory: p.covidInfectionHistory ?? false,
      lastCovidInfectionDate: p.lastCovidInfectionDate ?? "",
    },
    photo: {
      photoFileId: "",
      photoCapturedAt: "",
      photoDataUrl: "",
    },
  };
}
