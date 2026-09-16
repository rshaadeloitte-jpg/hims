import { useState, useCallback } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { Box, Button, Checkbox, Table, Textarea } from "@chakra-ui/react";
import { FormField, HimsSelect, TextInput } from "../../components/ui";
import {
  jsyJ1Schema,
  jsyJ2Schema,
  jsyJ3Schema,
  hFormSchema,
  JSY_FORM_TYPES,
  BENEFICIARY_CATEGORIES,
  DELIVERY_TYPES,
  type JsyJ1Values,
  type JsyJ2Values,
  type JsyJ3Values,
  type HFormValues,
} from "./zodSchema";
import { useSaveJsyForm, useJsyForms, type JsyFormDetails } from "./queries";

/**
 * M24 EncounterRecord — JSY statutory forms (J1, J2, J3, H-form).
 *
 * Ported from legacy formj1/2/3, hform templates.
 * Stripe-style form UX: progressive disclosure, real-time validation,
 * accessible error messaging, optimistic submission.
 */

type FormType = "J1" | "J2" | "J3" | "H_FORM";

const FORM_LABELS: Record<FormType, string> = {
  J1: "J1 — ANM Reporting",
  J2: "J2 — Medical Officer / Facility",
  J3: "J3 — District Consolidation",
  H_FORM: "H-Form — High-Risk Pregnancy",
};

export function EncounterRecord() {
  const [activeForm, setActiveForm] = useState<FormType>("J1");
  const [showList, setShowList] = useState(true);

  const saveForm = useSaveJsyForm();
  const { data: forms = [], isLoading: listLoading } = useJsyForms({ formType: activeForm });

  const handleFormSwitch = useCallback((type: FormType) => {
    setActiveForm(type);
  }, []);

  return (
    <section className="space-y-4" aria-label="encounter-record">
      <header className="page-header">
        <h1>PMRD — JSY Forms</h1>
        <p className="muted">Janani Suraksha Yojana statutory reporting forms</p>
      </header>

      {/* Form type tabs */}
      <div className="flex gap-2 border-b pb-2" role="tablist" aria-label="JSY form types">
        {JSY_FORM_TYPES.map((type) => (
          <Button
            key={type}
            role="tab"
            aria-selected={activeForm === type}
            variant={activeForm === type ? "primary" : "secondary"}
            size="sm"
            onClick={() => handleFormSwitch(type)}
          >
            {FORM_LABELS[type]}
          </Button>
        ))}
      </div>

      {/* Toggle form/list view */}
      <div className="flex gap-2">
        <Button
          type="button"
          variant={!showList ? "primary" : "secondary"}
          size="sm"
          onClick={() => setShowList(false)}
        >
          New Entry
        </Button>
        <Button
          type="button"
          variant={showList ? "primary" : "secondary"}
          size="sm"
          onClick={() => setShowList(true)}
        >
          View Records
        </Button>
      </div>

      {showList ? (
        <JsyFormList forms={forms} loading={listLoading} />
      ) : (
        <JsyFormRenderer formType={activeForm} saveForm={saveForm} />
      )}
    </section>
  );
}

function JsyFormList({ forms, loading }: { forms: JsyFormDetails[]; loading: boolean }) {
  if (loading) return <p className="text-sm text-gray-500">Loading forms...</p>;
  if (forms.length === 0) return <p className="text-sm text-gray-500">No forms submitted yet.</p>;

  return (
    <div className="card">
      <Table.Root size="sm" variant="line">
        <Table.Header>
          <Table.Row>
            <Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">ID</Table.ColumnHeader>
            <Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Beneficiary</Table.ColumnHeader>
            <Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Status</Table.ColumnHeader>
            <Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Submitted</Table.ColumnHeader>
            <Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Amount</Table.ColumnHeader>
          </Table.Row>
        </Table.Header>
        <Table.Body>
          {forms.map((f) => (
            <Table.Row key={f.id}>
              <Table.Cell>{f.id}</Table.Cell>
              <Table.Cell>{f.beneficiaryName}</Table.Cell>
              <Table.Cell>
                <span className={`badge badge--${f.status === "APPROVED" ? "success" : f.status === "REJECTED" ? "danger" : "info"}`}>
                  {f.status}
                </span>
              </Table.Cell>
              <Table.Cell>{f.submittedDate}</Table.Cell>
              <Table.Cell>{f.amount ? `₹${f.amount}` : "—"}</Table.Cell>
            </Table.Row>
          ))}
        </Table.Body>
      </Table.Root>
    </div>
  );
}

function JsyFormRenderer({
  formType,
  saveForm,
}: {
  formType: FormType;
  saveForm: ReturnType<typeof useSaveJsyForm>;
}) {
  switch (formType) {
    case "J1":
      return <J1Form saveForm={saveForm} />;
    case "J2":
      return <J2Form saveForm={saveForm} />;
    case "J3":
      return <J3Form saveForm={saveForm} />;
    case "H_FORM":
      return <HFormWrapper saveForm={saveForm} />;
  }
}

// ---- J1 Form ---------------------------------------------------------------

function J1Form({ saveForm }: { saveForm: ReturnType<typeof useSaveJsyForm> }) {
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
    reset,
  } = useForm<JsyJ1Values>({
    resolver: zodResolver(jsyJ1Schema),
    defaultValues: {
      formType: "J1",
      patientId: undefined as unknown as number,
      beneficiaryName: "",
      husbandName: "",
      address: "",
      category: "BPL",
      bplCardNo: "",
      mobileNo: "",
      ancRegistrationDate: "",
      ancVisits: 0,
      deliveryDate: "",
      deliveryType: "NORMAL",
      placeOfDelivery: "",
      childGender: "MALE",
      childWeight: 0,
      anmName: "",
      anmMobile: "",
      submittedDate: "",
    },
  });

  const onSubmit = handleSubmit(async (values) => {
    await saveForm.mutateAsync(values as unknown as Record<string, unknown>);
    reset();
  });

  return (
    <form onSubmit={onSubmit} className="card space-y-4" aria-label="jsy-j1-form">
      <h2 className="text-lg font-semibold">J1 — ANM Reporting Form</h2>

      <fieldset className="grid grid-cols-1 sm:grid-cols-2 gap-3">
        <legend className="sr-only">Beneficiary Details</legend>
        <FormField label="Patient ID" error={errors.patientId?.message} required>
          <TextInput type="number" {...register("patientId")} />
        </FormField>
        <FormField label="Beneficiary Name" error={errors.beneficiaryName?.message} required>
          <TextInput {...register("beneficiaryName")} />
        </FormField>
        <FormField label="Husband Name" error={errors.husbandName?.message} required>
          <TextInput {...register("husbandName")} />
        </FormField>
        <FormField label="Category" error={errors.category?.message} required>
          <HimsSelect {...register("category")}>
            {BENEFICIARY_CATEGORIES.map((c) => (
              <option key={c} value={c}>{c}</option>
            ))}
          </HimsSelect>
        </FormField>
        <FormField label="BPL Card No." error={errors.bplCardNo?.message}>
          <TextInput {...register("bplCardNo")} />
        </FormField>
        <FormField label="Mobile No." error={errors.mobileNo?.message}>
          <TextInput {...register("mobileNo")} placeholder="10-digit" />
        </FormField>
      </fieldset>

      <fieldset className="grid grid-cols-1 sm:grid-cols-2 gap-3">
        <legend className="sr-only">Address</legend>
        <Box gridColumn={{ base: "auto", sm: "span 2" }}>
          <FormField label="Address" error={errors.address?.message} required>
            <Textarea bg="white" borderColor="gray.300" rows={2} {...register("address")} />
          </FormField>
        </Box>
      </fieldset>

      <fieldset className="grid grid-cols-1 sm:grid-cols-3 gap-3">
        <legend className="sr-only">ANC & Delivery</legend>
        <FormField label="ANC Registration Date" error={errors.ancRegistrationDate?.message} required>
          <TextInput type="date" {...register("ancRegistrationDate")} />
        </FormField>
        <FormField label="ANC Visits" error={errors.ancVisits?.message} required>
          <TextInput type="number" {...register("ancVisits")} />
        </FormField>
        <FormField label="Delivery Date" error={errors.deliveryDate?.message} required>
          <TextInput type="date" {...register("deliveryDate")} />
        </FormField>
        <FormField label="Delivery Type" error={errors.deliveryType?.message} required>
          <HimsSelect {...register("deliveryType")}>
            {DELIVERY_TYPES.map((d) => (
              <option key={d} value={d}>{d}</option>
            ))}
          </HimsSelect>
        </FormField>
        <FormField label="Place of Delivery" error={errors.placeOfDelivery?.message} required>
          <TextInput {...register("placeOfDelivery")} />
        </FormField>
        <FormField label="Child Gender" error={errors.childGender?.message} required>
          <HimsSelect {...register("childGender")}>
            <option value="MALE">Male</option>
            <option value="FEMALE">Female</option>
            <option value="OTHER">Other</option>
          </HimsSelect>
        </FormField>
        <FormField label="Child Weight (kg)" error={errors.childWeight?.message} required>
          <TextInput type="number" step="0.1" {...register("childWeight")} />
        </FormField>
      </fieldset>

      <fieldset className="grid grid-cols-1 sm:grid-cols-2 gap-3">
        <legend className="sr-only">ANM Details</legend>
        <FormField label="ANM Name" error={errors.anmName?.message} required>
          <TextInput {...register("anmName")} />
        </FormField>
        <FormField label="ANM Mobile" error={errors.anmMobile?.message} required>
          <TextInput {...register("anmMobile")} placeholder="10-digit" />
        </FormField>
      </fieldset>

      <FormField label="Submitted Date" error={errors.submittedDate?.message} required>
        <TextInput type="date" {...register("submittedDate")} />
      </FormField>

      {saveForm.isError && (
        <p className="text-red-600 text-sm" role="alert">{(saveForm.error as Error).message}</p>
      )}
      {saveForm.isSuccess && (
        <p className="text-green-600 text-sm" role="status">✓ Form submitted successfully</p>
      )}

      <Button
        type="submit"
        variant="primary"
        disabled={isSubmitting || saveForm.isPending}
        loading={saveForm.isPending}
        loadingText="Submitting..."
      >
        Submit J1 Form
      </Button>
    </form>
  );
}

// ---- J2 Form ---------------------------------------------------------------

function J2Form({ saveForm }: { saveForm: ReturnType<typeof useSaveJsyForm> }) {
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
    reset,
    watch,
    setValue,
  } = useForm<JsyJ2Values>({
    resolver: zodResolver(jsyJ2Schema),
    defaultValues: {
      formType: "J2",
      patientId: undefined as unknown as number,
      beneficiaryName: "",
      j1FormId: undefined,
      facilityName: "",
      facilityCode: "",
      moName: "",
      moRegistrationNo: "",
      deliveryDate: "",
      deliveryType: "NORMAL",
      complications: "",
      referralMade: false,
      referralFacility: "",
      dischargeDate: "",
      amountClaimed: 0,
      submittedDate: "",
    },
  });

  const referralMade = watch("referralMade");

  const onSubmit = handleSubmit(async (values) => {
    await saveForm.mutateAsync(values as unknown as Record<string, unknown>);
    reset();
  });

  return (
    <form onSubmit={onSubmit} className="card space-y-4" aria-label="jsy-j2-form">
      <h2 className="text-lg font-semibold">J2 — Medical Officer / Facility Form</h2>

      <fieldset className="grid grid-cols-1 sm:grid-cols-2 gap-3">
        <FormField label="Patient ID" error={errors.patientId?.message} required>
          <TextInput type="number" {...register("patientId")} />
        </FormField>
        <FormField label="Beneficiary Name" error={errors.beneficiaryName?.message} required>
          <TextInput {...register("beneficiaryName")} />
        </FormField>
        <FormField label="J1 Form ID (if linked)" error={errors.j1FormId?.message}>
          <TextInput type="number" {...register("j1FormId")} />
        </FormField>
      </fieldset>

      <fieldset className="grid grid-cols-1 sm:grid-cols-2 gap-3">
        <legend className="sr-only">Facility Details</legend>
        <FormField label="Facility Name" error={errors.facilityName?.message} required>
          <TextInput {...register("facilityName")} />
        </FormField>
        <FormField label="Facility Code" error={errors.facilityCode?.message} required>
          <TextInput {...register("facilityCode")} />
        </FormField>
        <FormField label="MO Name" error={errors.moName?.message} required>
          <TextInput {...register("moName")} />
        </FormField>
        <FormField label="MO Registration No." error={errors.moRegistrationNo?.message} required>
          <TextInput {...register("moRegistrationNo")} />
        </FormField>
      </fieldset>

      <fieldset className="grid grid-cols-1 sm:grid-cols-3 gap-3">
        <legend className="sr-only">Delivery Details</legend>
        <FormField label="Delivery Date" error={errors.deliveryDate?.message} required>
          <TextInput type="date" {...register("deliveryDate")} />
        </FormField>
        <FormField label="Delivery Type" error={errors.deliveryType?.message} required>
          <HimsSelect {...register("deliveryType")}>
            {DELIVERY_TYPES.map((d) => (
              <option key={d} value={d}>{d}</option>
            ))}
          </HimsSelect>
        </FormField>
        <FormField label="Discharge Date" error={errors.dischargeDate?.message} required>
          <TextInput type="date" {...register("dischargeDate")} />
        </FormField>
        <FormField label="Complications" error={errors.complications?.message}>
          <TextInput {...register("complications")} />
        </FormField>
        <FormField label="Amount Claimed (₹)" error={errors.amountClaimed?.message} required>
          <TextInput type="number" {...register("amountClaimed")} />
        </FormField>
      </fieldset>

      <fieldset className="space-y-2">
        <Checkbox.Root checked={!!referralMade} onCheckedChange={(e) => setValue("referralMade", !!e.checked, { shouldValidate: true })}>
          <Checkbox.HiddenInput {...register("referralMade")} />
          <Checkbox.Control />
          <Checkbox.Label>Referral Made</Checkbox.Label>
        </Checkbox.Root>
        {referralMade && (
          <FormField label="Referral Facility" error={errors.referralFacility?.message}>
            <TextInput {...register("referralFacility")} />
          </FormField>
        )}
      </fieldset>

      <FormField label="Submitted Date" error={errors.submittedDate?.message} required>
        <TextInput type="date" {...register("submittedDate")} />
      </FormField>

      {saveForm.isError && (
        <p className="text-red-600 text-sm" role="alert">{(saveForm.error as Error).message}</p>
      )}
      {saveForm.isSuccess && (
        <p className="text-green-600 text-sm" role="status">✓ Form submitted successfully</p>
      )}

      <Button
        type="submit"
        variant="primary"
        disabled={isSubmitting || saveForm.isPending}
        loading={saveForm.isPending}
        loadingText="Submitting..."
      >
        Submit J2 Form
      </Button>
    </form>
  );
}

// ---- J3 Form ---------------------------------------------------------------

function J3Form({ saveForm }: { saveForm: ReturnType<typeof useSaveJsyForm> }) {
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
    reset,
  } = useForm<JsyJ3Values>({
    resolver: zodResolver(jsyJ3Schema),
    defaultValues: {
      formType: "J3",
      districtName: "",
      blockName: "",
      month: "",
      year: new Date().getFullYear(),
      totalJ1Received: 0,
      totalJ2Received: 0,
      totalAmountDisbursed: 0,
      submittedBy: "",
      submittedDate: "",
    },
  });

  const onSubmit = handleSubmit(async (values) => {
    await saveForm.mutateAsync(values as unknown as Record<string, unknown>);
    reset();
  });

  return (
    <form onSubmit={onSubmit} className="card space-y-4" aria-label="jsy-j3-form">
      <h2 className="text-lg font-semibold">J3 — District / Block Consolidation</h2>

      <fieldset className="grid grid-cols-1 sm:grid-cols-2 gap-3">
        <FormField label="District Name" error={errors.districtName?.message} required>
          <TextInput {...register("districtName")} />
        </FormField>
        <FormField label="Block Name" error={errors.blockName?.message} required>
          <TextInput {...register("blockName")} />
        </FormField>
        <FormField label="Month" error={errors.month?.message} required>
          <TextInput {...register("month")} placeholder="e.g. January" />
        </FormField>
        <FormField label="Year" error={errors.year?.message} required>
          <TextInput type="number" {...register("year")} />
        </FormField>
      </fieldset>

      <fieldset className="grid grid-cols-1 sm:grid-cols-3 gap-3">
        <legend className="sr-only">Consolidation Figures</legend>
        <FormField label="Total J1 Received" error={errors.totalJ1Received?.message} required>
          <TextInput type="number" {...register("totalJ1Received")} />
        </FormField>
        <FormField label="Total J2 Received" error={errors.totalJ2Received?.message} required>
          <TextInput type="number" {...register("totalJ2Received")} />
        </FormField>
        <FormField label="Total Amount Disbursed (₹)" error={errors.totalAmountDisbursed?.message} required>
          <TextInput type="number" {...register("totalAmountDisbursed")} />
        </FormField>
      </fieldset>

      <fieldset className="grid grid-cols-1 sm:grid-cols-2 gap-3">
        <FormField label="Submitted By" error={errors.submittedBy?.message} required>
          <TextInput {...register("submittedBy")} />
        </FormField>
        <FormField label="Submitted Date" error={errors.submittedDate?.message} required>
          <TextInput type="date" {...register("submittedDate")} />
        </FormField>
      </fieldset>

      {saveForm.isError && (
        <p className="text-red-600 text-sm" role="alert">{(saveForm.error as Error).message}</p>
      )}
      {saveForm.isSuccess && (
        <p className="text-green-600 text-sm" role="status">✓ Form submitted successfully</p>
      )}

      <Button
        type="submit"
        variant="primary"
        disabled={isSubmitting || saveForm.isPending}
        loading={saveForm.isPending}
        loadingText="Submitting..."
      >
        Submit J3 Form
      </Button>
    </form>
  );
}

// ---- H-Form ----------------------------------------------------------------

function HFormWrapper({ saveForm }: { saveForm: ReturnType<typeof useSaveJsyForm> }) {
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
    reset,
  } = useForm<HFormValues>({
    resolver: zodResolver(hFormSchema),
    defaultValues: {
      formType: "H_FORM",
      patientId: undefined as unknown as number,
      beneficiaryName: "",
      highRiskReason: "",
      lmp: "",
      edd: "",
      riskFactors: [],
      actionTaken: "",
      anmName: "",
      submittedDate: "",
    },
  });

  const onSubmit = handleSubmit(async (values) => {
    await saveForm.mutateAsync(values as unknown as Record<string, unknown>);
    reset();
  });

  return (
    <form onSubmit={onSubmit} className="card space-y-4" aria-label="h-form">
      <h2 className="text-lg font-semibold">H-Form — High-Risk Pregnancy Tracking</h2>

      <fieldset className="grid grid-cols-1 sm:grid-cols-2 gap-3">
        <FormField label="Patient ID" error={errors.patientId?.message} required>
          <TextInput type="number" {...register("patientId")} />
        </FormField>
        <FormField label="Beneficiary Name" error={errors.beneficiaryName?.message} required>
          <TextInput {...register("beneficiaryName")} />
        </FormField>
      </fieldset>

      <FormField label="High-Risk Reason" error={errors.highRiskReason?.message} required>
        <Textarea bg="white" borderColor="gray.300" rows={2} {...register("highRiskReason")} />
      </FormField>

      <fieldset className="grid grid-cols-1 sm:grid-cols-2 gap-3">
        <FormField label="LMP" error={errors.lmp?.message} required>
          <TextInput type="date" {...register("lmp")} />
        </FormField>
        <FormField label="EDD" error={errors.edd?.message} required>
          <TextInput type="date" {...register("edd")} />
        </FormField>
      </fieldset>

      <FormField label="Action Taken" error={errors.actionTaken?.message} required>
        <Textarea bg="white" borderColor="gray.300" rows={2} {...register("actionTaken")} />
      </FormField>

      <fieldset className="grid grid-cols-1 sm:grid-cols-2 gap-3">
        <FormField label="ANM Name" error={errors.anmName?.message} required>
          <TextInput {...register("anmName")} />
        </FormField>
        <FormField label="Submitted Date" error={errors.submittedDate?.message} required>
          <TextInput type="date" {...register("submittedDate")} />
        </FormField>
      </fieldset>

      {saveForm.isError && (
        <p className="text-red-600 text-sm" role="alert">{(saveForm.error as Error).message}</p>
      )}
      {saveForm.isSuccess && (
        <p className="text-green-600 text-sm" role="status">✓ Form submitted successfully</p>
      )}

      <Button
        type="submit"
        variant="primary"
        disabled={isSubmitting || saveForm.isPending}
        loading={saveForm.isPending}
        loadingText="Submitting..."
      >
        Submit H-Form
      </Button>
    </form>
  );
}
