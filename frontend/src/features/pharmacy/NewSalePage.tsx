import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { useCreateItemSale } from "./queries";
import { useFieldArray, useForm } from "react-hook-form";
import { Button } from "@chakra-ui/react";
import { FormField, TextInput } from "../../components/ui";

interface Line {
  itemId: string;
  quantity: string;
  rate: string;
}

interface SaleFormValues {
  patientId: string;
  prescriptionId: string;
  lines: Line[];
}

export function NewSalePage() {
  const navigate = useNavigate();
  const createSale = useCreateItemSale();
  const { register, handleSubmit, control } = useForm<SaleFormValues>({
    defaultValues: { patientId: "", prescriptionId: "", lines: [{ itemId: "", quantity: "1", rate: "" }] },
  });
  const { fields, append, remove } = useFieldArray({ control, name: "lines" });
  const [err, setErr] = useState<string | null>(null);

  const submit = async (values: SaleFormValues) => {
    const patientId = Number(values.patientId);
    if (!patientId || patientId <= 0) { setErr("Patient ID is required"); return; }
    const lines = values.lines
      .filter((l) => l.itemId)
      .map((l) => ({
        itemId: Number(l.itemId),
        quantity: Number(l.quantity),
        ...(l.rate ? { rate: Number(l.rate) } : {}),
      }));
    if (lines.length === 0) { setErr("At least one item line is required"); return; }
    try {
      await createSale.mutateAsync({
        patientId,
        prescriptionId: values.prescriptionId ? Number(values.prescriptionId) : undefined,
        lines,
      });
      navigate("/pharmacy");
    } catch (e) {
      setErr((e as Error).message);
    }
  };

  return (
    <section className="page">
      <header className="page__header"><h1>New Pharmacy Sale</h1></header>
      <form className="form" onSubmit={handleSubmit(submit)}>
        <fieldset>
          <legend>Sale Header</legend>
          <FormField label="Patient ID *" required>
            <TextInput type="number" {...register("patientId")} />
          </FormField>
          <FormField label="Prescription ID">
            <TextInput type="number" {...register("prescriptionId")} />
          </FormField>
        </fieldset>
        <fieldset>
          <legend>Items</legend>
          {fields.map((field, i) => (
            <div key={field.id} className="row">
              <TextInput type="number" placeholder="Item ID" {...register(`lines.${i}.itemId` as const)} />
              <TextInput type="number" placeholder="Qty" {...register(`lines.${i}.quantity` as const)} />
              <TextInput type="number" step="0.01" placeholder="Rate (opt)" {...register(`lines.${i}.rate` as const)} />
              <Button type="button" variant="ghost" size="sm" onClick={() => remove(i)}>✕</Button>
            </div>
          ))}
          <Button type="button" variant="secondary" onClick={() => append({ itemId: "", quantity: "1", rate: "" })}>
            + Add Item
          </Button>
        </fieldset>
        {err && <p className="error">{err}</p>}
        <Button type="submit" variant="primary" disabled={createSale.isPending} loading={createSale.isPending} loadingText="Creating...">
          Create Sale
        </Button>
      </form>
    </section>
  );
}
