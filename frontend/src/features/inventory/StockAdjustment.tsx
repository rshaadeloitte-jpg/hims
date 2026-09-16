import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { Button, HStack, SimpleGrid, Textarea } from "@chakra-ui/react";
import { FormField, HimsSelect, TextInput } from "../../components/ui";
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

      <SimpleGrid columns={{ base: 1, md: 2 }} gap={4} className="stock-adjustment__fields">
        <FormField label="Item" error={errors.itemId?.message} required>
          <HimsSelect
            id="adj-itemId"
            aria-label="Item"
            {...register("itemId", { valueAsNumber: true })}
          >
            <option value={0}>Select item...</option>
            {items.map((item) => (
              <option key={item.id} value={item.id}>
                {item.name} ({item.code})
              </option>
            ))}
          </HimsSelect>
        </FormField>

        <FormField label="Batch (optional)">
          <HimsSelect id="adj-batchId" aria-label="Batch (optional)" {...register("batchId", { valueAsNumber: true })}>
            <option value="">No specific batch</option>
            {filteredBatches.map((batch) => (
              <option key={batch.id} value={batch.id}>
                {batch.batchNumber} (Exp: {batch.expiryDate})
              </option>
            ))}
          </HimsSelect>
        </FormField>

        <FormField label="Adjustment Type" error={errors.adjustmentType?.message} required>
          <HimsSelect
            id="adj-type"
            aria-label="Adjustment Type"
            {...register("adjustmentType")}
          >
            <option value="INCREASE">Increase (Add Stock)</option>
            <option value="DECREASE">Decrease (Remove Stock)</option>
          </HimsSelect>
        </FormField>

        <FormField label="Quantity" error={errors.quantity?.message} required>
          <TextInput
            id="adj-quantity"
            aria-label="Quantity"
            type="number"
            min={1}
            {...register("quantity", { valueAsNumber: true })}
          />
        </FormField>

        <FormField label="Reference / Document No.">
          <TextInput id="adj-reference" aria-label="Reference / Document No." type="text" {...register("reference")} />
        </FormField>

        <FormField label="Reason" error={errors.reason?.message} required>
          <Textarea
            id="adj-reason"
            aria-label="Reason"
            rows={3}
            {...register("reason")}
            placeholder="e.g., Damaged goods, inventory count correction, expiry write-off..."
            bg="white"
            borderColor="gray.300"
          />
        </FormField>
      </SimpleGrid>

      <HStack gap={2} mt={4} className="stock-adjustment__actions">
        <Button
          type="submit"
          variant="primary"
          disabled={createAdjustment.isPending}
          loading={createAdjustment.isPending}
          loadingText="Recording..."
        >
          Record Adjustment
        </Button>
        <Button type="button" variant="secondary" onClick={() => reset()}>
          Clear
        </Button>
      </HStack>
    </form>
  );
}
