import { useState, useCallback } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
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
          <button
            key={type}
            role="tab"
            aria-selected={activeForm === type}
            className={`px-3 py-1.5 text-sm rounded-t transition-colors ${
              activeForm === type
                ? "bg-blue-600 text-white"
                : "bg-gray-100 text-gray-700 hover:bg-gray-200"
            }`}
            onClick={() => handleFormSwitch(type)}
          >
            {FORM_LABELS[type]}
          </button>
        ))}
      </div>

      {/* Toggle form/list view */}
      <div className="flex gap-2">
        <button
          type="button"
          className={`px-3 py-1 text-sm rounded ${!showList ? "bg-blue-600 text-white" : "bg-gray-100"}`}
          onClick={() => setShowList(false)}
        >
          New Entry
        </button>
        <button
          type="button"
          className={`px-3 py-1 text-sm rounded ${showList ? "bg-blue-600 text-white" : "bg-gray-100"}`}
          onClick={() => setShowList(true)}
        >
          View Records
        </button>
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
      <table className="table">
        <thead>
          <tr>
            <th>ID</th>
            <th>Beneficiary</th>
            <th>Status</th>
            <th>Submitted</th>
            <th>Amount</th>
          </tr>
        </thead>
        <tbody>
          {forms.map((f) => (
            <tr key={f.id}>
              <td>{f.id}</td>
              <td>{f.beneficiaryName}</td>
              <td>
                <span className={`badge badge--${f.status === "APPROVED" ? "success" : f.status === "REJECTED" ? "danger" : "info"}`}>
                  {f.status}
                </span>
              </td>
              <td>{f.submittedDate}</td>
              <td>{f.amount ? `₹${f.amount}` : "—"}</td>
            </tr>
          ))}
        </tbody>
      </table>
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
        <Field label="Patient ID" error={errors.patientId?.message} required>
          <input type="number" className="w-full border rounded px-2 py-1.5" {...register("patientId")} />
        </Field>
        <Field label="Beneficiary Name" error={errors.beneficiaryName?.message} required>
          <input className="w-full border rounded px-2 py-1.5" {...register("beneficiaryName")} />
        </Field>
        <Field label="Husband Name" error={errors.husbandName?.message} required>
          <input className="w-full border rounded px-2 py-1.5" {...register("husbandName")} />
        </Field>
        <Field label="Category" error={errors.category?.message} required>
          <select className="w-full border rounded px-2 py-1.5" {...register("category")}>
            {BENEFICIARY_CATEGORIES.map((c) => (
              <option key={c} value={c}>{c}</option>
            ))}
          </select>
        </Field>
        <Field label="BPL Card No." error={errors.bplCardNo?.message}>
          <input className="w-full border rounded px-2 py-1.5" {...register("bplCardNo")} />
        </Field>
        <Field label="Mobile No." error={errors.mobileNo?.message}>
          <input className="w-full border rounded px-2 py-1.5" {...register("mobileNo")} placeholder="10-digit" />
        </Field>
      </fieldset>

      <fieldset className="grid grid-cols-1 sm:grid-cols-2 gap-3">
        <legend className="sr-only">Address</legend>
        <Field label="Address" error={errors.address?.message} required className="sm:col-span-2">
          <textarea className="w-full border rounded px-2 py-1.5" rows={2} {...register("address")} />
        </Field>
      </fieldset>

      <fieldset className="grid grid-cols-1 sm:grid-cols-3 gap-3">
        <legend className="sr-only">ANC & Delivery</legend>
        <Field label="ANC Registration Date" error={errors.ancRegistrationDate?.message} required>
          <input type="date" className="w-full border rounded px-2 py-1.5" {...register("ancRegistrationDate")} />
        </Field>
        <Field label="ANC Visits" error={errors.ancVisits?.message} required>
          <input type="number" className="w-full border rounded px-2 py-1.5" {...register("ancVisits")} />
        </Field>
        <Field label="Delivery Date" error={errors.deliveryDate?.message} required>
          <input type="date" className="w-full border rounded px-2 py-1.5" {...register("deliveryDate")} />
        </Field>
        <Field label="Delivery Type" error={errors.deliveryType?.message} required>
          <select className="w-full border rounded px-2 py-1.5" {...register("deliveryType")}>
            {DELIVERY_TYPES.map((d) => (
              <option key={d} value={d}>{d}</option>
            ))}
          </select>
        </Field>
        <Field label="Place of Delivery" error={errors.placeOfDelivery?.message} required>
          <input className="w-full border rounded px-2 py-1.5" {...register("placeOfDelivery")} />
        </Field>
        <Field label="Child Gender" error={errors.childGender?.message} required>
          <select className="w-full border rounded px-2 py-1.5" {...register("childGender")}>
            <option value="MALE">Male</option>
            <option value="FEMALE">Female</option>
            <option value="OTHER">Other</option>
          </select>
        </Field>
        <Field label="Child Weight (kg)" error={errors.childWeight?.message} required>
          <input type="number" step="0.1" className="w-full border rounded px-2 py-1.5" {...register("childWeight")} />
        </Field>
      </fieldset>

      <fieldset className="grid grid-cols-1 sm:grid-cols-2 gap-3">
        <legend className="sr-only">ANM Details</legend>
        <Field label="ANM Name" error={errors.anmName?.message} required>
          <input className="w-full border rounded px-2 py-1.5" {...register("anmName")} />
        </Field>
        <Field label="ANM Mobile" error={errors.anmMobile?.message} required>
          <input className="w-full border rounded px-2 py-1.5" {...register("anmMobile")} placeholder="10-digit" />
        </Field>
      </fieldset>

      <Field label="Submitted Date" error={errors.submittedDate?.message} required>
        <input type="date" className="w-full border rounded px-2 py-1.5" {...register("submittedDate")} />
      </Field>

      {saveForm.isError && (
        <p className="text-red-600 text-sm" role="alert">{(saveForm.error as Error).message}</p>
      )}
      {saveForm.isSuccess && (
        <p className="text-green-600 text-sm" role="status">✓ Form submitted successfully</p>
      )}

      <button
        type="submit"
        className="bg-blue-600 text-white px-4 py-2 rounded disabled:opacity-50 disabled:cursor-not-allowed hover:bg-blue-700 transition-colors"
        disabled={isSubmitting || saveForm.isPending}
      >
        {saveForm.isPending ? "Submitting..." : "Submit J1 Form"}
      </button>
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
        <Field label="Patient ID" error={errors.patientId?.message} required>
          <input type="number" className="w-full border rounded px-2 py-1.5" {...register("patientId")} />
        </Field>
        <Field label="Beneficiary Name" error={errors.beneficiaryName?.message} required>
          <input className="w-full border rounded px-2 py-1.5" {...register("beneficiaryName")} />
        </Field>
        <Field label="J1 Form ID (if linked)" error={errors.j1FormId?.message}>
          <input type="number" className="w-full border rounded px-2 py-1.5" {...register("j1FormId")} />
        </Field>
      </fieldset>

      <fieldset className="grid grid-cols-1 sm:grid-cols-2 gap-3">
        <legend className="sr-only">Facility Details</legend>
        <Field label="Facility Name" error={errors.facilityName?.message} required>
          <input className="w-full border rounded px-2 py-1.5" {...register("facilityName")} />
        </Field>
        <Field label="Facility Code" error={errors.facilityCode?.message} required>
          <input className="w-full border rounded px-2 py-1.5" {...register("facilityCode")} />
        </Field>
        <Field label="MO Name" error={errors.moName?.message} required>
          <input className="w-full border rounded px-2 py-1.5" {...register("moName")} />
        </Field>
        <Field label="MO Registration No." error={errors.moRegistrationNo?.message} required>
          <input className="w-full border rounded px-2 py-1.5" {...register("moRegistrationNo")} />
        </Field>
      </fieldset>

      <fieldset className="grid grid-cols-1 sm:grid-cols-3 gap-3">
        <legend className="sr-only">Delivery Details</legend>
        <Field label="Delivery Date" error={errors.deliveryDate?.message} required>
          <input type="date" className="w-full border rounded px-2 py-1.5" {...register("deliveryDate")} />
        </Field>
        <Field label="Delivery Type" error={errors.deliveryType?.message} required>
          <select className="w-full border rounded px-2 py-1.5" {...register("deliveryType")}>
            {DELIVERY_TYPES.map((d) => (
              <option key={d} value={d}>{d}</option>
            ))}
          </select>
        </Field>
        <Field label="Discharge Date" error={errors.dischargeDate?.message} required>
          <input type="date" className="w-full border rounded px-2 py-1.5" {...register("dischargeDate")} />
        </Field>
        <Field label="Complications" error={errors.complications?.message}>
          <input className="w-full border rounded px-2 py-1.5" {...register("complications")} />
        </Field>
        <Field label="Amount Claimed (₹)" error={errors.amountClaimed?.message} required>
          <input type="number" className="w-full border rounded px-2 py-1.5" {...register("amountClaimed")} />
        </Field>
      </fieldset>

      <fieldset className="space-y-2">
        <label className="flex items-center gap-2 text-sm">
          <input type="checkbox" {...register("referralMade")} />
          <span>Referral Made</span>
        </label>
        {referralMade && (
          <Field label="Referral Facility" error={errors.referralFacility?.message}>
            <input className="w-full border rounded px-2 py-1.5" {...register("referralFacility")} />
          </Field>
        )}
      </fieldset>

      <Field label="Submitted Date" error={errors.submittedDate?.message} required>
        <input type="date" className="w-full border rounded px-2 py-1.5" {...register("submittedDate")} />
      </Field>

      {saveForm.isError && (
        <p className="text-red-600 text-sm" role="alert">{(saveForm.error as Error).message}</p>
      )}
      {saveForm.isSuccess && (
        <p className="text-green-600 text-sm" role="status">✓ Form submitted successfully</p>
      )}

      <button
        type="submit"
        className="bg-blue-600 text-white px-4 py-2 rounded disabled:opacity-50 disabled:cursor-not-allowed hover:bg-blue-700 transition-colors"
        disabled={isSubmitting || saveForm.isPending}
      >
        {saveForm.isPending ? "Submitting..." : "Submit J2 Form"}
      </button>
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
        <Field label="District Name" error={errors.districtName?.message} required>
          <input className="w-full border rounded px-2 py-1.5" {...register("districtName")} />
        </Field>
        <Field label="Block Name" error={errors.blockName?.message} required>
          <input className="w-full border rounded px-2 py-1.5" {...register("blockName")} />
        </Field>
        <Field label="Month" error={errors.month?.message} required>
          <input className="w-full border rounded px-2 py-1.5" {...register("month")} placeholder="e.g. January" />
        </Field>
        <Field label="Year" error={errors.year?.message} required>
          <input type="number" className="w-full border rounded px-2 py-1.5" {...register("year")} />
        </Field>
      </fieldset>

      <fieldset className="grid grid-cols-1 sm:grid-cols-3 gap-3">
        <legend className="sr-only">Consolidation Figures</legend>
        <Field label="Total J1 Received" error={errors.totalJ1Received?.message} required>
          <input type="number" className="w-full border rounded px-2 py-1.5" {...register("totalJ1Received")} />
        </Field>
        <Field label="Total J2 Received" error={errors.totalJ2Received?.message} required>
          <input type="number" className="w-full border rounded px-2 py-1.5" {...register("totalJ2Received")} />
        </Field>
        <Field label="Total Amount Disbursed (₹)" error={errors.totalAmountDisbursed?.message} required>
          <input type="number" className="w-full border rounded px-2 py-1.5" {...register("totalAmountDisbursed")} />
        </Field>
      </fieldset>

      <fieldset className="grid grid-cols-1 sm:grid-cols-2 gap-3">
        <Field label="Submitted By" error={errors.submittedBy?.message} required>
          <input className="w-full border rounded px-2 py-1.5" {...register("submittedBy")} />
        </Field>
        <Field label="Submitted Date" error={errors.submittedDate?.message} required>
          <input type="date" className="w-full border rounded px-2 py-1.5" {...register("submittedDate")} />
        </Field>
      </fieldset>

      {saveForm.isError && (
        <p className="text-red-600 text-sm" role="alert">{(saveForm.error as Error).message}</p>
      )}
      {saveForm.isSuccess && (
        <p className="text-green-600 text-sm" role="status">✓ Form submitted successfully</p>
      )}

      <button
        type="submit"
        className="bg-blue-600 text-white px-4 py-2 rounded disabled:opacity-50 disabled:cursor-not-allowed hover:bg-blue-700 transition-colors"
        disabled={isSubmitting || saveForm.isPending}
      >
        {saveForm.isPending ? "Submitting..." : "Submit J3 Form"}
      </button>
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
        <Field label="Patient ID" error={errors.patientId?.message} required>
          <input type="number" className="w-full border rounded px-2 py-1.5" {...register("patientId")} />
        </Field>
        <Field label="Beneficiary Name" error={errors.beneficiaryName?.message} required>
          <input className="w-full border rounded px-2 py-1.5" {...register("beneficiaryName")} />
        </Field>
      </fieldset>

      <Field label="High-Risk Reason" error={errors.highRiskReason?.message} required>
        <textarea className="w-full border rounded px-2 py-1.5" rows={2} {...register("highRiskReason")} />
      </Field>

      <fieldset className="grid grid-cols-1 sm:grid-cols-2 gap-3">
        <Field label="LMP" error={errors.lmp?.message} required>
          <input type="date" className="w-full border rounded px-2 py-1.5" {...register("lmp")} />
        </Field>
        <Field label="EDD" error={errors.edd?.message} required>
          <input type="date" className="w-full border rounded px-2 py-1.5" {...register("edd")} />
        </Field>
      </fieldset>

      <Field label="Action Taken" error={errors.actionTaken?.message} required>
        <textarea className="w-full border rounded px-2 py-1.5" rows={2} {...register("actionTaken")} />
      </Field>

      <fieldset className="grid grid-cols-1 sm:grid-cols-2 gap-3">
        <Field label="ANM Name" error={errors.anmName?.message} required>
          <input className="w-full border rounded px-2 py-1.5" {...register("anmName")} />
        </Field>
        <Field label="Submitted Date" error={errors.submittedDate?.message} required>
          <input type="date" className="w-full border rounded px-2 py-1.5" {...register("submittedDate")} />
        </Field>
      </fieldset>

      {saveForm.isError && (
        <p className="text-red-600 text-sm" role="alert">{(saveForm.error as Error).message}</p>
      )}
      {saveForm.isSuccess && (
        <p className="text-green-600 text-sm" role="status">✓ Form submitted successfully</p>
      )}

      <button
        type="submit"
        className="bg-blue-600 text-white px-4 py-2 rounded disabled:opacity-50 disabled:cursor-not-allowed hover:bg-blue-700 transition-colors"
        disabled={isSubmitting || saveForm.isPending}
      >
        {saveForm.isPending ? "Submitting..." : "Submit H-Form"}
      </button>
    </form>
  );
}

// ---- Shared field component -----------------------------------------------

function Field({
  label,
  error,
  required,
  className,
  children,
}: {
  label: string;
  error?: string;
  required?: boolean;
  className?: string;
  children: React.ReactNode;
}) {
  return (
    <label className={`text-sm ${className ?? ""}`}>
      <span className="block font-medium">
        {label}
        {required && <span className="text-red-500 ml-0.5">*</span>}
      </span>
      {children}
      {error && <span className="text-red-600 text-xs">{error}</span>}
    </label>
  );
}
