import { useState } from "react";
import { useForm, useFieldArray } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { indentRequestSchema, type IndentRequestValues, type IndentLineValues } from "./zodSchema";
import { useInventoryStores, useInventoryItems, useCreateIndent } from "./queries";

function getDefaultValues(): IndentRequestValues {
  return {
    storeId: 0,
    requestDate: new Date().toISOString().slice(0, 10),
    priority: "ROUTINE",
    remarks: "",
    lines: [{ itemId: 0, requestedQty: 1, remarks: "" }],
  };
}

export function IndentForm() {
  const { register, control, handleSubmit, reset, formState: { errors, isSubmitSuccessful } } = useForm<IndentRequestValues>({
    resolver: zodResolver(indentRequestSchema),
    defaultValues: getDefaultValues(),
  });

  const { fields, append, remove } = useFieldArray({ control, name: "lines" });
  const { data: stores = [] } = useInventoryStores();
  const { data: items = [] } = useInventoryItems();
  const createIndent = useCreateIndent();

  const onSubmit = handleSubmit((data) => {
    createIndent.mutate(data, {
      onSuccess: () => reset(getDefaultValues()),
    });
  });

  if (isSubmitSuccessful && !createIndent.isError) {
    return (
      <div className="indent-form">
        <div className="alert alert--success">
          <strong>Indent submitted successfully.</strong>
          <button type="button" className="btn btn--sm" onClick={() => reset(getDefaultValues())}>
            Create Another
          </button>
        </div>
      </div>
    );
  }

  return (
    <form onSubmit={onSubmit} className="indent-form">
      <div className="indent-form__header">
        <h3 className="indent-form__title">New Indent Request</h3>
        <div className="indent-form__meta">
          <span className="indent-form__date">{new Date().toLocaleDateString("en-IN")}</span>
        </div>
      </div>

      {createIndent.isError && (
        <div className="alert alert--error">
          <strong>Submission failed:</strong> {createIndent.error?.message ?? "Please try again."}
        </div>
      )}

      <div className="indent-form__fields">
        <div className="field">
          <label htmlFor="storeId">
            Store <span className="req">*</span>
          </label>
          <select
            id="storeId"
            {...register("storeId", { valueAsNumber: true })}
            className={errors.storeId ? "input--error" : ""}
          >
            <option value={0}>Select store...</option>
            {stores.map((store) => (
              <option key={store.id} value={store.id}>
                {store.name} ({store.code})
              </option>
            ))}
          </select>
          {errors.storeId && <span className="field__error">{errors.storeId.message}</span>}
        </div>

        <div className="field">
          <label htmlFor="requestDate">
            Request Date <span className="req">*</span>
          </label>
          <input
            id="requestDate"
            type="date"
            {...register("requestDate")}
            className={errors.requestDate ? "input--error" : ""}
          />
          {errors.requestDate && <span className="field__error">{errors.requestDate.message}</span>}
        </div>

        <div className="field">
          <label htmlFor="priority">
            Priority <span className="req">*</span>
          </label>
          <select id="priority" {...register("priority")}>
            <option value="ROUTINE">Routine</option>
            <option value="URGENT">Urgent</option>
            <option value="EMERGENCY">Emergency</option>
          </select>
        </div>
      </div>

      <div className="indent-form__lines">
        <div className="indent-form__lines-header">
          <h4 className="indent-form__lines-title">Line Items</h4>
          <button
            type="button"
            className="btn btn--sm btn--secondary"
            onClick={() => append({ itemId: 0, requestedQty: 1, remarks: "" })}
          >
            + Add Line
          </button>
        </div>

        <table className="table">
          <thead>
            <tr>
              <th style={{ width: "45%" }}>Item</th>
              <th style={{ width: "20%" }}>Qty</th>
              <th style={{ width: "25%" }}>Remarks</th>
              <th style={{ width: "10%" }}></th>
            </tr>
          </thead>
          <tbody>
            {fields.map((field, index) => (
              <tr key={field.id}>
                <td>
                  <select
                    {...register(`lines.${index}.itemId`, { valueAsNumber: true })}
                    className={errors.lines?.[index]?.itemId ? "input--error" : ""}
                  >
                    <option value={0}>Select item...</option>
                    {items.map((item) => (
                      <option key={item.id} value={item.id}>
                        {item.name} ({item.code})
                      </option>
                    ))}
                  </select>
                  {errors.lines?.[index]?.itemId && (
                    <span className="field__error">{errors.lines[index]?.itemId?.message}</span>
                  )}
                </td>
                <td>
                  <input
                    type="number"
                    min={1}
                    {...register(`lines.${index}.requestedQty`, { valueAsNumber: true })}
                    className={errors.lines?.[index]?.requestedQty ? "input--error" : ""}
                  />
                  {errors.lines?.[index]?.requestedQty && (
                    <span className="field__error">{errors.lines[index]?.requestedQty?.message}</span>
                  )}
                </td>
                <td>
                  <input type="text" {...register(`lines.${index}.remarks`)} />
                </td>
                <td>
                  {fields.length > 1 && (
                    <button
                      type="button"
                      className="btn btn--sm btn--danger"
                      onClick={() => remove(index)}
                      aria-label="Remove line"
                    >
                      ✕
                    </button>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
        {errors.lines && typeof errors.lines.message === "string" && (
          <span className="field__error">{errors.lines.message}</span>
        )}
      </div>

      <div className="field">
        <label htmlFor="remarks">Overall Remarks</label>
        <textarea id="remarks" rows={2} {...register("remarks")} />
      </div>

      <div className="indent-form__actions">
        <button
          type="submit"
          className="btn btn--primary"
          disabled={createIndent.isPending}
        >
          {createIndent.isPending ? "Submitting..." : "Submit Indent"}
        </button>
        <button type="button" className="btn btn--secondary" onClick={() => reset(getDefaultValues())}>
          Reset
        </button>
      </div>
    </form>
  );
}
