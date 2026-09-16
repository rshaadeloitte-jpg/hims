import { useState } from "react";
import { useForm, useFieldArray } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { Button, HStack, SimpleGrid, Table } from "@chakra-ui/react";
import { FormField, HimsSelect, TextInput } from "../../components/ui";
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
        <Button variant={showForm ? "secondary" : "primary"} onClick={() => setShowForm(!showForm)}>
          {showForm ? "Cancel" : "New PO"}
        </Button>
      </header>

      {showForm && (
        <div className="card">
          <h2>New Purchase Order</h2>
          <form onSubmit={handleSubmit(onSubmit)} noValidate>
            <SimpleGrid columns={{ base: 1, md: 2 }} gap={4} className="form-grid">
              <FormField label="Store" error={errors.storeId?.message} required>
                <HimsSelect id="storeId" aria-label="Store" {...register("storeId")}>
                  <option value={0}>Select store</option>
                  {(stores.data ?? []).map((s) => <option key={s.id} value={s.id}>{s.code} - {s.description}</option>)}
                </HimsSelect>
              </FormField>

              <FormField label="Supplier" error={errors.supplierId?.message} required>
                <HimsSelect id="supplierId" aria-label="Supplier" {...register("supplierId")}>
                  <option value={0}>Select supplier</option>
                  {(suppliers.data ?? []).map((s) => <option key={s.id} value={s.id}>{s.code} - {s.description}</option>)}
                </HimsSelect>
              </FormField>

              <FormField label="Quotation (optional)" error={errors.quotationId?.message}>
                <TextInput id="quotationId" aria-label="Quotation (optional)" type="number" {...register("quotationId")} placeholder="Quotation ID" />
              </FormField>

              <FormField label="Order Date">
                <TextInput id="orderDate" aria-label="Order Date" type="date" {...register("orderDate")} />
              </FormField>

              <FormField label="Expected Date">
                <TextInput id="expectedDate" aria-label="Expected Date" type="date" {...register("expectedDate")} />
              </FormField>

              <FormField label="Remarks">
                <TextInput id="remarks" aria-label="Remarks" type="text" {...register("remarks")} placeholder="Optional remarks" />
              </FormField>
            </SimpleGrid>

            <h3 style={{ marginTop: "1.5rem" }}>Line Items</h3>
            <div className="table-wrap">
              <Table.Root size="sm" variant="line">
                <Table.Header>
                  <Table.Row>
                    <Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Item *</Table.ColumnHeader>
                    <Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Qty *</Table.ColumnHeader>
                    <Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Unit Price *</Table.ColumnHeader>
                    <Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Tax %</Table.ColumnHeader>
                    <Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Lead Days</Table.ColumnHeader>
                    <Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Remarks</Table.ColumnHeader>
                    <Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase"></Table.ColumnHeader>
                  </Table.Row>
                </Table.Header>
                <Table.Body>
                  {fields.map((field, idx) => (
                    <Table.Row key={field.id}>
                      <Table.Cell>
                        <HimsSelect aria-label={`Line ${idx + 1} item`} {...register(`lines.${idx}.itemId`)}>
                          <option value={0}>Select item</option>
                          {(items.data ?? []).map((i) => <option key={i.id} value={i.id}>{i.itemName}</option>)}
                        </HimsSelect>
                        {errors.lines?.[idx]?.itemId && <span className="field-error">{errors.lines[idx].itemId?.message}</span>}
                      </Table.Cell>
                      <Table.Cell>
                        <TextInput type="number" step="0.0001" aria-label={`Line ${idx + 1} quantity`} {...register(`lines.${idx}.orderedQty`)} />
                        {errors.lines?.[idx]?.orderedQty && <span className="field-error">{errors.lines[idx].orderedQty?.message}</span>}
                      </Table.Cell>
                      <Table.Cell>
                        <TextInput type="number" step="0.01" aria-label={`Line ${idx + 1} unit price`} {...register(`lines.${idx}.unitPrice`)} />
                        {errors.lines?.[idx]?.unitPrice && <span className="field-error">{errors.lines[idx].unitPrice?.message}</span>}
                      </Table.Cell>
                      <Table.Cell><TextInput type="number" step="0.01" aria-label={`Line ${idx + 1} tax percent`} {...register(`lines.${idx}.taxPercent`)} /></Table.Cell>
                      <Table.Cell><TextInput type="number" aria-label={`Line ${idx + 1} lead days`} {...register(`lines.${idx}.leadTimeDays`)} /></Table.Cell>
                      <Table.Cell><TextInput type="text" aria-label={`Line ${idx + 1} remarks`} {...register(`lines.${idx}.remarks`)} /></Table.Cell>
                      <Table.Cell>{fields.length > 1 && <Button type="button" variant="secondary" size="sm" onClick={() => remove(idx)}>Remove</Button>}</Table.Cell>
                    </Table.Row>
                  ))}
                </Table.Body>
              </Table.Root>
            </div>
            <Button type="button" variant="secondary" size="sm" onClick={() => append({ itemId: 0, orderedQty: 1, unitPrice: 0, taxPercent: 0 })} style={{ marginTop: "0.75rem" }}>
              + Add Line
            </Button>

            {errors.lines && typeof errors.lines.message === "string" && <p className="field-error">{errors.lines.message}</p>}

            <HStack gap={2} mt={4} className="form-actions">
              <Button type="submit" variant="primary" disabled={isSubmitting} loading={isSubmitting} loadingText="Creating...">
                Create Purchase Order
              </Button>
            </HStack>
          </form>
        </div>
      )}
    </section>
  );
}
