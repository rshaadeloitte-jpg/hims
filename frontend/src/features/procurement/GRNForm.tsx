import { useState } from "react";
import { useForm, useFieldArray } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { Button, Checkbox, HStack, SimpleGrid, Table } from "@chakra-ui/react";
import { FormField, HimsSelect, TextInput } from "../../components/ui";
import { grnSchema, type GrnForm } from "./zodSchema";
import { useSuppliers, useStores, useItems, usePostGrn } from "./queries";

export function GRNForm() {
  const [showForm, setShowForm] = useState(false);
  const postGrn = usePostGrn();
  const suppliers = useSuppliers();
  const stores = useStores();
  const items = useItems();

  const { register, control, handleSubmit, watch, setValue, formState: { errors, isSubmitting }, reset } = useForm<GrnForm>({
    resolver: zodResolver(grnSchema),
    defaultValues: {
      storeId: 0,
      provisional: false,
      consignment: false,
      lines: [{ itemId: 0, quantity: 1 }],
    },
  });

  const { fields, append, remove } = useFieldArray({ control, name: "lines" });
  const provisional = watch("provisional");
  const consignment = watch("consignment");

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
        <Button variant={showForm ? "secondary" : "primary"} onClick={() => setShowForm(!showForm)}>
          {showForm ? "Cancel" : "New GRN"}
        </Button>
      </header>

      {showForm && (
        <div className="card">
          <h2>New GRN</h2>
          <form onSubmit={handleSubmit(onSubmit)} noValidate>
            <SimpleGrid columns={{ base: 1, md: 2 }} gap={4} className="form-grid">
              <FormField label="Store" error={errors.storeId?.message} required>
                <HimsSelect id="storeId" aria-label="Store" {...register("storeId")}>
                  <option value={0}>Select store</option>
                  {(stores.data ?? []).map((s) => <option key={s.id} value={s.id}>{s.code} - {s.description}</option>)}
                </HimsSelect>
              </FormField>

              <FormField label="Supplier">
                <HimsSelect id="supplierId" aria-label="Supplier" {...register("supplierId")}>
                  <option value="">Select supplier (optional)</option>
                  {(suppliers.data ?? []).map((s) => <option key={s.id} value={s.id}>{s.code} - {s.description}</option>)}
                </HimsSelect>
              </FormField>

              <FormField label="GRN No">
                <TextInput id="grnNo" aria-label="GRN No" type="text" {...register("grnNo")} placeholder="Auto-generated if blank" />
              </FormField>

              <FormField label="Invoice No">
                <TextInput id="invoiceNo" aria-label="Invoice No" type="text" {...register("invoiceNo")} />
              </FormField>

              <FormField label="Invoice Date">
                <TextInput id="invoiceDate" aria-label="Invoice Date" type="date" {...register("invoiceDate")} />
              </FormField>

              <FormField label="Received Date">
                <TextInput id="receivedDate" aria-label="Received Date" type="date" {...register("receivedDate")} />
              </FormField>

              <FormField label="Remarks">
                <TextInput id="remarks" aria-label="Remarks" type="text" {...register("remarks")} placeholder="Optional remarks" />
              </FormField>

              <Checkbox.Root
                checked={!!provisional}
                onCheckedChange={(e) => setValue("provisional", !!e.checked, { shouldValidate: true })}
              >
                <Checkbox.HiddenInput id="provisional" aria-label="Provisional" {...register("provisional")} />
                <Checkbox.Control />
                <Checkbox.Label>Provisional</Checkbox.Label>
              </Checkbox.Root>

              <Checkbox.Root
                checked={!!consignment}
                onCheckedChange={(e) => setValue("consignment", !!e.checked, { shouldValidate: true })}
              >
                <Checkbox.HiddenInput id="consignment" aria-label="Consignment" {...register("consignment")} />
                <Checkbox.Control />
                <Checkbox.Label>Consignment</Checkbox.Label>
              </Checkbox.Root>
            </SimpleGrid>

            <h3 style={{ marginTop: "1.5rem" }}>Line Items</h3>
            <div className="table-wrap">
              <Table.Root size="sm" variant="line">
                <Table.Header>
                  <Table.Row>
                    <Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Item *</Table.ColumnHeader>
                    <Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Qty *</Table.ColumnHeader>
                    <Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Batch ID</Table.ColumnHeader>
                    <Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Batch Code</Table.ColumnHeader>
                    <Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Expiry Date</Table.ColumnHeader>
                    <Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">MRP</Table.ColumnHeader>
                    <Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Purchase Rate</Table.ColumnHeader>
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
                        <TextInput type="number" step="0.0001" aria-label={`Line ${idx + 1} quantity`} {...register(`lines.${idx}.quantity`)} />
                        {errors.lines?.[idx]?.quantity && <span className="field-error">{errors.lines[idx].quantity?.message}</span>}
                      </Table.Cell>
                      <Table.Cell><TextInput type="number" aria-label={`Line ${idx + 1} batch ID`} {...register(`lines.${idx}.batchId`)} /></Table.Cell>
                      <Table.Cell><TextInput type="text" aria-label={`Line ${idx + 1} batch code`} {...register(`lines.${idx}.batchCode`)} /></Table.Cell>
                      <Table.Cell><TextInput type="date" aria-label={`Line ${idx + 1} expiry date`} {...register(`lines.${idx}.expiryDate`)} /></Table.Cell>
                      <Table.Cell><TextInput type="number" step="0.01" aria-label={`Line ${idx + 1} MRP`} {...register(`lines.${idx}.mrp`)} /></Table.Cell>
                      <Table.Cell><TextInput type="number" step="0.01" aria-label={`Line ${idx + 1} purchase rate`} {...register(`lines.${idx}.purchaseRate`)} /></Table.Cell>
                      <Table.Cell><TextInput type="text" aria-label={`Line ${idx + 1} remarks`} {...register(`lines.${idx}.remarks`)} /></Table.Cell>
                      <Table.Cell>{fields.length > 1 && <Button type="button" variant="secondary" size="sm" onClick={() => remove(idx)}>Remove</Button>}</Table.Cell>
                    </Table.Row>
                  ))}
                </Table.Body>
              </Table.Root>
            </div>
            <Button type="button" variant="secondary" size="sm" onClick={() => append({ itemId: 0, quantity: 1 })} style={{ marginTop: "0.75rem" }}>
              + Add Line
            </Button>

            {errors.lines && typeof errors.lines.message === "string" && <p className="field-error">{errors.lines.message}</p>}

            <HStack gap={2} mt={4} className="form-actions">
              <Button type="submit" variant="primary" disabled={isSubmitting} loading={isSubmitting} loadingText="Posting...">
                Post GRN
              </Button>
            </HStack>
          </form>
        </div>
      )}
    </section>
  );
}
