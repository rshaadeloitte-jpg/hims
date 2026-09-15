import { useState } from "react";
import { useForm, useFieldArray } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { quotationSchema, type QuotationForm } from "./zodSchema";
import { useSuppliers, useItems, useCreateQuotation, useQuotationCompare, type QuotationCompareRow } from "./queries";

export function QuotationComparison() {
  const [showForm, setShowForm] = useState(false);
  const [compareItemId, setCompareItemId] = useState(0);
  const createQuotation = useCreateQuotation();
  const suppliers = useSuppliers();
  const items = useItems();
  const compare = useQuotationCompare(compareItemId);

  const { register, control, handleSubmit, formState: { errors, isSubmitting }, reset } = useForm<QuotationForm>({
    resolver: zodResolver(quotationSchema),
    defaultValues: {
      supplierId: 0,
      lines: [{ itemId: 0, quantity: 1, unitPrice: 0, taxPercent: 0 }],
    },
  });

  const { fields, append, remove } = useFieldArray({ control, name: "lines" });

  const onSubmit = (data: QuotationForm) => {
    createQuotation.mutate(data, {
      onSuccess: () => { setShowForm(false); reset(); },
    });
  };

  const groupedCompare = (compare.data ?? []).reduce<Record<number, QuotationCompareRow[]>>((acc, row) => {
    if (!acc[row.quotationId]) acc[row.quotationId] = [];
    acc[row.quotationId].push(row);
    return acc;
  }, {});

  return (
    <section className="page">
      <header className="page-header">
        <div>
          <h1>Quotation Comparison</h1>
          <p className="muted">Compare quotations side-by-side for each item</p>
        </div>
        <button className="btn btn-primary" onClick={() => setShowForm(!showForm)}>
          {showForm ? "Cancel" : "New Quotation"}
        </button>
      </header>

      {showForm && (
        <div className="card">
          <h2>New Quotation</h2>
          <form onSubmit={handleSubmit(onSubmit)} noValidate>
            <div className="form-grid">
              <div className="field">
                <label htmlFor="supplierId">Supplier <span className="req">*</span></label>
                <select id="supplierId" {...register("supplierId")}>
                  <option value={0}>Select supplier</option>
                  {(suppliers.data ?? []).map((s) => <option key={s.id} value={s.id}>{s.code} - {s.description}</option>)}
                </select>
                {errors.supplierId && <span className="field-error">{errors.supplierId.message}</span>}
              </div>

              <div className="field">
                <label htmlFor="enquiryRef">Enquiry Reference</label>
                <input id="enquiryRef" type="text" {...register("enquiryRef")} />
              </div>

              <div className="field">
                <label htmlFor="validUntil">Valid Until</label>
                <input id="validUntil" type="date" {...register("validUntil")} />
              </div>

              <div className="field">
                <label htmlFor="remarks">Remarks</label>
                <input id="remarks" type="text" {...register("remarks")} />
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
                        <input type="number" step="0.0001" {...register(`lines.${idx}.quantity`)} />
                        {errors.lines?.[idx]?.quantity && <span className="field-error">{errors.lines[idx].quantity?.message}</span>}
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
            <button type="button" className="btn btn-secondary btn-sm" onClick={() => append({ itemId: 0, quantity: 1, unitPrice: 0, taxPercent: 0 })} style={{ marginTop: "0.75rem" }}>
              + Add Line
            </button>

            <div className="form-actions">
              <button type="submit" className="btn btn-primary" disabled={isSubmitting}>
                {isSubmitting ? "Creating..." : "Create Quotation"}
              </button>
            </div>
          </form>
        </div>
      )}

      <div className="card">
        <h2>Compare Quotations</h2>
        <div className="field" style={{ maxWidth: 320 }}>
          <label htmlFor="compareItemId">Item to Compare</label>
          <select id="compareItemId" value={compareItemId} onChange={(e) => setCompareItemId(Number(e.target.value))}>
            <option value={0}>Select item</option>
            {(items.data ?? []).map((i) => <option key={i.id} value={i.id}>{i.itemName}</option>)}
          </select>
        </div>

        {compare.isLoading && <p className="muted">Loading comparison...</p>}
        {compare.isError && <div className="alert alert--error">Failed to load comparison</div>}
        {compare.data && compare.data.length === 0 && compareItemId > 0 && (
          <p className="empty-state">No quotations found for this item.</p>
        )}

        {Object.keys(groupedCompare).length > 0 && (
          <div className="table-wrap" style={{ marginTop: "1rem" }}>
            <table className="data-table">
              <thead>
                <tr>
                  <th>Quotation No</th>
                  <th>Supplier</th>
                  <th>Qty</th>
                  <th>Unit Price</th>
                  <th>Tax %</th>
                  <th>Lead Days</th>
                </tr>
              </thead>
              <tbody>
                {Object.entries(groupedCompare).map(([quotationId, rows]) => (
                  rows.map((row, idx) => (
                    <tr key={`${quotationId}-${idx}`}>
                      <td>{row.quotationNo}</td>
                      <td>{row.supplierName ?? row.supplierCode ?? `Supplier ${row.supplierId}`}</td>
                      <td className="cell cell--num">{row.quantity}</td>
                      <td className="cell cell--num">{row.unitPrice}</td>
                      <td className="cell cell--num">{row.taxPercent}%</td>
                      <td>{row.leadTimeDays ?? "-"}</td>
                    </tr>
                  ))
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </section>
  );
}
