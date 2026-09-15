import { useState } from "react";
import { useForm, useFieldArray } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { grnSchema, type GrnForm } from "./zodSchema";
import { useSuppliers, useStores, useItems, usePostGrn } from "./queries";

export function GRNForm() {
  const [showForm, setShowForm] = useState(false);
  const postGrn = usePostGrn();
  const suppliers = useSuppliers();
  const stores = useStores();
  const items = useItems();

  const { register, control, handleSubmit, formState: { errors, isSubmitting }, reset } = useForm<GrnForm>({
    resolver: zodResolver(grnSchema),
    defaultValues: {
      storeId: 0,
      provisional: false,
      consignment: false,
      lines: [{ itemId: 0, quantity: 1 }],
    },
  });

  const { fields, append, remove } = useFieldArray({ control, name: "lines" });

  const onSubmit = (data: GrnForm) => {
    postGrn.mutate(data, {
      onSuccess: () => { setShowForm(false); reset(); },
    });
  };

  return (
    <section className="page">
      <header className="page-header">
        <div>
          <h1>Goods Receipt Notes</h1>
          <p className="muted">Post GRNs with quality control</p>
        </div>
        <button className="btn btn-primary" onClick={() => setShowForm(!showForm)}>
          {showForm ? "Cancel" : "New GRN"}
        </button>
      </header>

      {showForm && (
        <div className="card">
          <h2>New GRN</h2>
          <form onSubmit={handleSubmit(onSubmit)} noValidate>
            <div className="form-grid">
              <div className="field">
                <label htmlFor="storeId">Store <span className="req">*</span></label>
                <select id="storeId" {...register("storeId")}>
                  <option value={0}>Select store</option>
                  {(stores.data ?? []).map((s) => <option key={s.id} value={s.id}>{s.code} - {s.description}</option>)}
                </select>
                {errors.storeId && <span className="field-error">{errors.storeId.message}</span>}
              </div>

              <div className="field">
                <label htmlFor="supplierId">Supplier</label>
                <select id="supplierId" {...register("supplierId")}>
                  <option value="">Select supplier (optional)</option>
                  {(suppliers.data ?? []).map((s) => <option key={s.id} value={s.id}>{s.code} - {s.description}</option>)}
                </select>
              </div>

              <div className="field">
                <label htmlFor="grnNo">GRN No</label>
                <input id="grnNo" type="text" {...register("grnNo")} placeholder="Auto-generated if blank" />
              </div>

              <div className="field">
                <label htmlFor="invoiceNo">Invoice No</label>
                <input id="invoiceNo" type="text" {...register("invoiceNo")} />
              </div>

              <div className="field">
                <label htmlFor="invoiceDate">Invoice Date</label>
                <input id="invoiceDate" type="date" {...register("invoiceDate")} />
              </div>

              <div className="field">
                <label htmlFor="receivedDate">Received Date</label>
                <input id="receivedDate" type="date" {...register("receivedDate")} />
              </div>

              <div className="field">
                <label htmlFor="remarks">Remarks</label>
                <input id="remarks" type="text" {...register("remarks")} placeholder="Optional remarks" />
              </div>

              <div className="field field--check">
                <input id="provisional" type="checkbox" {...register("provisional")} />
                <label htmlFor="provisional">Provisional</label>
              </div>

              <div className="field field--check">
                <input id="consignment" type="checkbox" {...register("consignment")} />
                <label htmlFor="consignment">Consignment</label>
              </div>
            </div>

            <h3 style={{ marginTop: "1.5rem" }}>Line Items</h3>
            <div className="table-wrap">
              <table className="data-table">
                <thead>
                  <tr>
                    <th>Item <span className="req">*</span></th>
                    <th>Qty <span className="req">*</span></th>
                    <th>Batch ID</th>
                    <th>Batch Code</th>
                    <th>Expiry Date</th>
                    <th>MRP</th>
                    <th>Purchase Rate</th>
                    <th>Remarks</th>
                    <th></th>
                  </tr>
                </thead>
                <tbody>
                  {fields.map((field, idx) => (
                    <tr key={field.id}>
                      <td>
                        <select {...register(`lines.${idx}.itemId`)}>
                          <option value={0}>Select item</option>
                          {(items.data ?? []).map((i) => <option key={i.id} value={i.id}>{i.itemName}</option>)}
                        </select>
                        {errors.lines?.[idx]?.itemId && <span className="field-error">{errors.lines[idx].itemId?.message}</span>}
                      </td>
                      <td>
                        <input type="number" step="0.0001" {...register(`lines.${idx}.quantity`)} />
                        {errors.lines?.[idx]?.quantity && <span className="field-error">{errors.lines[idx].quantity?.message}</span>}
                      </td>
                      <td><input type="number" {...register(`lines.${idx}.batchId`)} /></td>
                      <td><input type="text" {...register(`lines.${idx}.batchCode`)} /></td>
                      <td><input type="date" {...register(`lines.${idx}.expiryDate`)} /></td>
                      <td><input type="number" step="0.01" {...register(`lines.${idx}.mrp`)} /></td>
                      <td><input type="number" step="0.01" {...register(`lines.${idx}.purchaseRate`)} /></td>
                      <td><input type="text" {...register(`lines.${idx}.remarks`)} /></td>
                      <td>{fields.length > 1 && <button type="button" className="btn btn-sm btn-secondary" onClick={() => remove(idx)}>Remove</button>}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <button type="button" className="btn btn-secondary btn-sm" onClick={() => append({ itemId: 0, quantity: 1 })} style={{ marginTop: "0.75rem" }}>
              + Add Line
            </button>

            {errors.lines && typeof errors.lines.message === "string" && <p className="field-error">{errors.lines.message}</p>}

            <div className="form-actions">
              <button type="submit" className="btn btn-primary" disabled={isSubmitting}>
                {isSubmitting ? "Posting..." : "Post GRN"}
              </button>
            </div>
          </form>
        </div>
      )}
    </section>
  );
}
