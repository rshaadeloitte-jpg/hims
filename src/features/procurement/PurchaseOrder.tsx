import { useState } from "react";
import { useForm, useFieldArray } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { purchaseOrderSchema, type PurchaseOrderForm } from "./zodSchema";
import { useSuppliers, useStores, useItems, useCreatePurchaseOrder } from "./queries";

export function PurchaseOrderPage() {
  const [showForm, setShowForm] = useState(false);
  const createOrder = useCreatePurchaseOrder();
  const suppliers = useSuppliers();
  const stores = useStores();
  const items = useItems();

  const { register, control, handleSubmit, formState: { errors, isSubmitting }, reset } = useForm<PurchaseOrderForm>({
    resolver: zodResolver(purchaseOrderSchema),
    defaultValues: {
      storeId: 0,
      supplierId: 0,
      lines: [{ itemId: 0, orderedQty: 1, unitPrice: 0, taxPercent: 0 }],
    },
  });

  const { fields, append, remove } = useFieldArray({ control, name: "lines" });

  const onSubmit = (data: PurchaseOrderForm) => {
    createOrder.mutate(data, {
      onSuccess: () => { setShowForm(false); reset(); },
    });
  };

  return (
    <section className="page">
      <header className="page-header">
        <div>
          <h1>Purchase Orders</h1>
          <p className="muted">Create and manage purchase orders</p>
        </div>
        <button className="btn btn-primary" onClick={() => setShowForm(!showForm)}>
          {showForm ? "Cancel" : "New PO"}
        </button>
      </header>

      {showForm && (
        <div className="card">
          <h2>New Purchase Order</h2>
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
                <label htmlFor="supplierId">Supplier <span className="req">*</span></label>
                <select id="supplierId" {...register("supplierId")}>
                  <option value={0}>Select supplier</option>
                  {(suppliers.data ?? []).map((s) => <option key={s.id} value={s.id}>{s.code} - {s.description}</option>)}
                </select>
                {errors.supplierId && <span className="field-error">{errors.supplierId.message}</span>}
              </div>

              <div className="field">
                <label htmlFor="quotationId">Quotation (optional)</label>
                <input id="quotationId" type="number" {...register("quotationId")} placeholder="Quotation ID" />
                {errors.quotationId && <span className="field-error">{errors.quotationId.message}</span>}
              </div>

              <div className="field">
                <label htmlFor="orderDate">Order Date</label>
                <input id="orderDate" type="date" {...register("orderDate")} />
              </div>

              <div className="field">
                <label htmlFor="expectedDate">Expected Date</label>
                <input id="expectedDate" type="date" {...register("expectedDate")} />
              </div>

              <div className="field">
                <label htmlFor="remarks">Remarks</label>
                <input id="remarks" type="text" {...register("remarks")} placeholder="Optional remarks" />
              </div>
            </div>

            <h3 style={{ marginTop: "1.5rem" }}>Line Items</h3>
            <div className="table-wrap">
              <table className="data-table">
                <thead>
                  <tr>
                    <th>Item <span className="req">*</span></th>
                    <th>Qty <span className="req">*</span></th>
                    <th>Unit Price <span className="req">*</span></th>
                    <th>Tax %</th>
                    <th>Lead Days</th>
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
                        <input type="number" step="0.0001" {...register(`lines.${idx}.orderedQty`)} />
                        {errors.lines?.[idx]?.orderedQty && <span className="field-error">{errors.lines[idx].orderedQty?.message}</span>}
                      </td>
                      <td>
                        <input type="number" step="0.01" {...register(`lines.${idx}.unitPrice`)} />
                        {errors.lines?.[idx]?.unitPrice && <span className="field-error">{errors.lines[idx].unitPrice?.message}</span>}
                      </td>
                      <td><input type="number" step="0.01" {...register(`lines.${idx}.taxPercent`)} /></td>
                      <td><input type="number" {...register(`lines.${idx}.leadTimeDays`)} /></td>
                      <td><input type="text" {...register(`lines.${idx}.remarks`)} /></td>
                      <td>{fields.length > 1 && <button type="button" className="btn btn-sm btn-secondary" onClick={() => remove(idx)}>Remove</button>}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <button type="button" className="btn btn-secondary btn-sm" onClick={() => append({ itemId: 0, orderedQty: 1, unitPrice: 0, taxPercent: 0 })} style={{ marginTop: "0.75rem" }}>
              + Add Line
            </button>

            {errors.lines && typeof errors.lines.message === "string" && <p className="field-error">{errors.lines.message}</p>}

            <div className="form-actions">
              <button type="submit" className="btn btn-primary" disabled={isSubmitting}>
                {isSubmitting ? "Creating..." : "Create Purchase Order"}
              </button>
            </div>
          </form>
        </div>
      )}
    </section>
  );
}
