import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { useCreateItemSale } from "./queries";
import { useFieldArray, useForm } from "react-hook-form";

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
          <label>
            Patient ID *
            <input type="number" {...register("patientId")} />
          </label>
          <label>
            Prescription ID
            <input type="number" {...register("prescriptionId")} />
          </label>
        </fieldset>
        <fieldset>
          <legend>Items</legend>
          {fields.map((field, i) => (
            <div key={field.id} className="row">
              <input type="number" placeholder="Item ID" {...register(`lines.${i}.itemId` as const)} />
              <input type="number" placeholder="Qty" {...register(`lines.${i}.quantity` as const)} />
              <input type="number" step="0.01" placeholder="Rate (opt)" {...register(`lines.${i}.rate` as const)} />
              <button type="button" onClick={() => remove(i)}>✕</button>
            </div>
          ))}
          <button type="button" className="btn" onClick={() => append({ itemId: "", quantity: "1", rate: "" })}>
            + Add Item
          </button>
        </fieldset>
        {err && <p className="error">{err}</p>}
        <button type="submit" className="btn btn--primary" disabled={createSale.isPending}>
          {createSale.isPending ? "Creating..." : "Create Sale"}
        </button>
      </form>
    </section>
  );
}