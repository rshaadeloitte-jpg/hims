import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { stockAdjustmentSchema, type StockAdjustmentValues } from "./zodSchema";
import { useInventoryItems, useInventoryBatches, useCreateStockAdjustment } from "./queries";

export function StockAdjustment() {
  const { register, handleSubmit, reset, formState: { errors } } = useForm<StockAdjustmentValues>({
    resolver: zodResolver(stockAdjustmentSchema),
    defaultValues: {
      itemId: 0,
      batchId: undefined,
      adjustmentType: "INCREASE",
      quantity: 1,
      reason: "",
      reference: "",
    },
  });

  const { data: items = [] } = useInventoryItems();
  const { data: batches = [] } = useInventoryBatches();
  const createAdjustment = useCreateStockAdjustment();

  const selectedItemId = Number(
    (document.querySelector('select[name="itemId"]') as HTMLSelectElement | null)?.value ?? 0
  );
  const filteredBatches = batches.filter((b) => b.itemId === selectedItemId);

  const onSubmit = handleSubmit((data) => {
    createAdjustment.mutate(data, {
      onSuccess: () => reset(),
    });
  });

  return (
    <form onSubmit={onSubmit} className="stock-adjustment">
      <div className="stock-adjustment__header">
        <h3 className="stock-adjustment__title">Stock Adjustment</h3>
        <p className="muted">Record inventory corrections, damages, or write-offs.</p>
      </div>

      {createAdjustment.isError && (
        <div className="alert alert--error">
          <strong>Adjustment failed:</strong> {createAdjustment.error?.message ?? "Please try again."}
        </div>
      )}

      {createAdjustment.isSuccess && (
        <div className="alert alert--success">
          <strong>Adjustment recorded successfully.</strong>
        </div>
      )}

      <div className="stock-adjustment__fields">
        <div className="field">
          <label htmlFor="adj-itemId">
            Item <span className="req">*</span>
          </label>
          <select
            id="adj-itemId"
            {...register("itemId", { valueAsNumber: true })}
            className={errors.itemId ? "input--error" : ""}
          >
            <option value={0}>Select item...</option>
            {items.map((item) => (
              <option key={item.id} value={item.id}>
                {item.name} ({item.code})
              </option>
            ))}
          </select>
          {errors.itemId && <span className="field__error">{errors.itemId.message}</span>}
        </div>

        <div className="field">
          <label htmlFor="adj-batchId">Batch (optional)</label>
          <select id="adj-batchId" {...register("batchId", { valueAsNumber: true })}>
            <option value="">No specific batch</option>
            {filteredBatches.map((batch) => (
              <option key={batch.id} value={batch.id}>
                {batch.batchNumber} (Exp: {batch.expiryDate})
              </option>
            ))}
          </select>
        </div>

        <div className="field">
          <label htmlFor="adj-type">
            Adjustment Type <span className="req">*</span>
          </label>
          <select
            id="adj-type"
            {...register("adjustmentType")}
            className={errors.adjustmentType ? "input--error" : ""}
          >
            <option value="INCREASE">Increase (Add Stock)</option>
            <option value="DECREASE">Decrease (Remove Stock)</option>
          </select>
          {errors.adjustmentType && <span className="field__error">{errors.adjustmentType.message}</span>}
        </div>

        <div className="field">
          <label htmlFor="adj-quantity">
            Quantity <span className="req">*</span>
          </label>
          <input
            id="adj-quantity"
            type="number"
            min={1}
            {...register("quantity", { valueAsNumber: true })}
            className={errors.quantity ? "input--error" : ""}
          />
          {errors.quantity && <span className="field__error">{errors.quantity.message}</span>}
        </div>

        <div className="field">
          <label htmlFor="adj-reference">Reference / Document No.</label>
          <input id="adj-reference" type="text" {...register("reference")} />
        </div>

        <div className="field field--full">
          <label htmlFor="adj-reason">
            Reason <span className="req">*</span>
          </label>
          <textarea
            id="adj-reason"
            rows={3}
            {...register("reason")}
            placeholder="e.g., Damaged goods, inventory count correction, expiry write-off..."
            className={errors.reason ? "input--error" : ""}
          />
          {errors.reason && <span className="field__error">{errors.reason.message}</span>}
        </div>
      </div>

      <div className="stock-adjustment__actions">
        <button
          type="submit"
          className="btn btn--primary"
          disabled={createAdjustment.isPending}
        >
          {createAdjustment.isPending ? "Recording..." : "Record Adjustment"}
        </button>
        <button type="button" className="btn btn--secondary" onClick={() => reset()}>
          Clear
        </button>
      </div>
    </form>
  );
}
