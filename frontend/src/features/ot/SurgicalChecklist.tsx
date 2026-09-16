import { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { Button, SimpleGrid } from "@chakra-ui/react";
import { FormField, HimsSelect, TextInput } from "../../components/ui";
import {
  useChecklists,
  useCreateChecklist,
  useAddChecklistItem,
  useRecordChecklistCompletion,
  type SurgicalChecklist,
} from "./queries";
import {
  checklistCreateSchema,
  checklistItemSchema,
  checklistCompletionSchema,
  type ChecklistCreateForm,
  type ChecklistItemForm,
  type ChecklistCompletionForm,
} from "./zodSchema";

const WHO_SIGN_IN_ITEMS = [
  { itemName: "Patient has confirmed identity, site, procedure, consent", category: "identity" },
  { itemName: "Surgical site marked / N/A", category: "site" },
  { itemName: "Anaesthesia safety check completed", category: "anaesthesia" },
  { itemName: "Pulse oximeter on patient and functioning", category: "monitoring" },
  { itemName: "Patient has known allergy", category: "allergy" },
  { itemName: "Difficult airway / aspiration risk", category: "airway" },
  { itemName: "Risk of >500ml blood loss (7ml/kg in children)", category: "blood_loss" },
];

const WHO_TIME_OUT_ITEMS = [
  { itemName: "All team members introduce themselves", category: "team" },
  { itemName: "Surgeon, anaesthesia professional, nurse confirm patient, site, procedure", category: "confirmation" },
  { itemName: "Anticipated critical events reviewed", category: "critical_events" },
  { itemName: "Prophylactic antibiotics given within last 60 minutes", category: "antibiotics" },
  { itemName: "Essential imaging displayed", category: "imaging" },
];

const WHO_SIGN_OUT_ITEMS = [
  { itemName: "Nurse confirms procedure recorded", category: "documentation" },
  { itemName: "Instruments, sponge, needle counts correct", category: "counts" },
  { itemName: "Specimens labelled correctly", category: "specimens" },
  { itemName: "Equipment problems identified", category: "equipment" },
  { itemName: "Surgeon, anaesthesia, nurse review recovery concerns", category: "recovery" },
];

function getWhoItems(phase: string) {
  switch (phase) {
    case "SIGN_IN": return WHO_SIGN_IN_ITEMS;
    case "TIME_OUT": return WHO_TIME_OUT_ITEMS;
    case "SIGN_OUT": return WHO_SIGN_OUT_ITEMS;
    default: return [];
  }
}

function ChecklistCard({ checklist }: { checklist: SurgicalChecklist }) {
  const checklistId = checklist.checklistId ?? checklist.id ?? 0;
  const [expanded, setExpanded] = useState(false);
  const addItemMutation = useAddChecklistItem();
  const recordCompletionMutation = useRecordChecklistCompletion();

  const {
    register: registerItem,
    handleSubmit: handleSubmitItem,
    reset: resetItem,
  } = useForm<ChecklistItemForm>({
    resolver: zodResolver(checklistItemSchema),
    defaultValues: { itemName: "", category: "" },
  });

  const {
    register: registerCompletion,
    handleSubmit: handleSubmitCompletion,
  } = useForm<ChecklistCompletionForm>({
    resolver: zodResolver(checklistCompletionSchema),
    defaultValues: { overallStatus: "COMPLETED" },
  });

  const whoItems = getWhoItems(checklist.phase ?? "");
  const completed = checklist.completed ?? false;

  const onAddItem = handleSubmitItem(async (values) => {
    try {
      await addItemMutation.mutateAsync({ checklistId, ...values });
      resetItem();
    } catch (error) {
      console.error("Add item failed:", error);
    }
  });

  const onRecordCompletion = handleSubmitCompletion(async (values) => {
    try {
      await recordCompletionMutation.mutateAsync({ checklistId, ...values });
    } catch (error) {
      console.error("Record completion failed:", error);
    }
  });

  return (
    <div className={`border rounded-lg bg-white ${completed ? "border-green-300" : "border-gray-200"}`}>
      <Button
        type="button"
        variant="ghost"
        onClick={() => setExpanded(!expanded)}
      >
        <div className="flex items-center gap-3">
          <span className={`w-3 h-3 rounded-full ${completed ? "bg-green-500" : "bg-yellow-500"}`} />
          <div>
            <h4 className="text-sm font-semibold text-gray-900">{checklist.phase}</h4>
            <p className="text-xs text-gray-500">Patient #{checklist.patientId} — Schedule #{checklist.scheduleId}</p>
          </div>
        </div>
        <div className="flex items-center gap-2">
          {completed && <span className="text-xs px-2 py-0.5 bg-green-100 text-green-700 rounded">COMPLETED</span>}
          <svg className={`w-4 h-4 text-gray-400 transition-transform ${expanded ? "rotate-180" : ""}`} fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7" />
          </svg>
        </div>
      </Button>

      {expanded && (
        <div className="px-4 pb-4 space-y-3 border-t border-gray-100 pt-3">
          <div className="space-y-2">
            {whoItems.map((item, idx) => (
              <div key={idx} className="flex items-center gap-2 p-2 bg-gray-50 rounded text-xs">
                <span className="w-2 h-2 bg-blue-400 rounded-full flex-shrink-0" />
                <span className="text-gray-700 flex-1">{item.itemName}</span>
                <span className="text-gray-400 text-[10px] uppercase">{item.category}</span>
              </div>
            ))}
          </div>

          {!completed && (
            <>
              <form onSubmit={onAddItem} className="flex gap-2">
                <TextInput
                  type="text"
                  placeholder="Add checklist item..."
                  {...registerItem("itemName")}
                />
                <TextInput
                  type="text"
                  placeholder="Category"
                  {...registerItem("category")}
                />
                <Button type="submit" variant="primary" size="sm" loading={addItemMutation.isPending} loadingText="Adding...">
                  Add
                </Button>
              </form>

              <form onSubmit={onRecordCompletion} className="flex items-center gap-2 pt-2 border-t border-gray-100">
                <HimsSelect {...registerCompletion("overallStatus")}>
                  <option value="COMPLETED">Completed</option>
                  <option value="ABORTED">Aborted</option>
                  <option value="PENDING">Pending</option>
                </HimsSelect>
                <Button type="submit" variant="primary" size="sm" loading={recordCompletionMutation.isPending} loadingText="Recording...">
                  Record Completion
                </Button>
              </form>
            </>
          )}
        </div>
      )}
    </div>
  );
}

function CreateChecklistForm() {
  const mutation = useCreateChecklist();
  const {
    register,
    handleSubmit,
    formState: { errors },
    reset,
  } = useForm<ChecklistCreateForm>({
    resolver: zodResolver(checklistCreateSchema),
    defaultValues: { scheduleId: 0, patientId: 0, phase: "SIGN_IN" },
  });

  const onSubmit = handleSubmit(async (values) => {
    try {
      await mutation.mutateAsync(values);
      reset();
    } catch (error) {
      console.error("Create checklist failed:", error);
    }
  });

  return (
    <form onSubmit={onSubmit} className="p-4 border border-gray-200 rounded-lg bg-white space-y-3">
      <h3 className="text-sm font-semibold text-gray-900">Create WHO Surgical Checklist</h3>
      <SimpleGrid columns={{ base: 1, md: 3 }} gap={3}>
        <FormField label="Schedule ID *" error={errors.scheduleId?.message} required>
          <TextInput type="number" {...register("scheduleId", { valueAsNumber: true })} />
        </FormField>
        <FormField label="Patient ID *" error={errors.patientId?.message} required>
          <TextInput type="number" {...register("patientId", { valueAsNumber: true })} />
        </FormField>
        <FormField label="Phase *" error={errors.phase?.message} required>
          <HimsSelect {...register("phase")}>
            <option value="SIGN_IN">Sign In (Before Induction)</option>
            <option value="TIME_OUT">Time Out (Before Incision)</option>
            <option value="SIGN_OUT">Sign Out (Before Leaving OR)</option>
          </HimsSelect>
        </FormField>
      </SimpleGrid>
      <Button type="submit" variant="primary" loading={mutation.isPending} loadingText="Creating...">
        Create Checklist
      </Button>
    </form>
  );
}

export function SurgicalChecklist() {
  const { data: checklists = [], isLoading } = useChecklists();

  const grouped = {
    SIGN_IN: checklists.filter((c) => c.phase === "SIGN_IN"),
    TIME_OUT: checklists.filter((c) => c.phase === "TIME_OUT"),
    SIGN_OUT: checklists.filter((c) => c.phase === "SIGN_OUT"),
  };

  if (isLoading) return <div className="text-center py-8 text-gray-500 text-sm">Loading checklists...</div>;

  return (
    <section className="space-y-6" aria-label="surgical-checklist">
      <header>
        <h2 className="text-xl font-semibold text-gray-900">Surgical Safety Checklist</h2>
        <p className="text-sm text-gray-500">WHO Surgical Safety Checklist — Sign In, Time Out, Sign Out</p>
      </header>

      <CreateChecklistForm />

      <div className="space-y-4">
        {(["SIGN_IN", "TIME_OUT", "SIGN_OUT"] as const).map((phase) => (
          <div key={phase}>
            <h3 className="text-sm font-semibold text-gray-900 mb-2 flex items-center gap-2">
              <span className={`w-2 h-2 rounded-full ${phase === "SIGN_IN" ? "bg-blue-500" : phase === "TIME_OUT" ? "bg-yellow-500" : "bg-green-500"}`} />
              {phase.replace("_", " ")} ({grouped[phase].length})
            </h3>
            {grouped[phase].length === 0 ? (
              <div className="text-xs text-gray-500 p-3 border border-dashed border-gray-200 rounded">
                No {phase.replace("_", " ").toLowerCase()} checklists created yet.
              </div>
            ) : (
              <div className="space-y-2">
                {grouped[phase].map((c) => (
                  <ChecklistCard key={c.checklistId ?? c.id ?? 0} checklist={c} />
                ))}
              </div>
            )}
          </div>
        ))}
      </div>
    </section>
  );
}
