import { useState } from "react";
import { useForm, useFieldArray } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { Box, Button, HStack, SimpleGrid, Table } from "@chakra-ui/react";
import { FormField, HimsSelect, TextInput } from "../../components/ui";
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
        <Button variant={showForm ? "secondary" : "primary"} onClick={() => setShowForm(!showForm)}>
          {showForm ? "Cancel" : "New Quotation"}
        </Button>
      </header>

      {showForm && (
        <div className="card">
          <h2>New Quotation</h2>
          <form onSubmit={handleSubmit(onSubmit)} noValidate>
            <SimpleGrid columns={{ base: 1, md: 2 }} gap={4} className="form-grid">
              <FormField label="Supplier" error={errors.supplierId?.message} required>
                <HimsSelect id="supplierId" aria-label="Supplier" {...register("supplierId")}>
                  <option value={0}>Select supplier</option>
                  {(suppliers.data ?? []).map((s) => <option key={s.id} value={s.id}>{s.code} - {s.description}</option>)}
                </HimsSelect>
              </FormField>

              <FormField label="Enquiry Reference">
                <TextInput id="enquiryRef" aria-label="Enquiry Reference" type="text" {...register("enquiryRef")} />
              </FormField>

              <FormField label="Valid Until">
                <TextInput id="validUntil" aria-label="Valid Until" type="date" {...register("validUntil")} />
              </FormField>

              <FormField label="Remarks">
                <TextInput id="remarks" aria-label="Remarks" type="text" {...register("remarks")} />
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
                        <TextInput type="number" step="0.0001" aria-label={`Line ${idx + 1} quantity`} {...register(`lines.${idx}.quantity`)} />
                        {errors.lines?.[idx]?.quantity && <span className="field-error">{errors.lines[idx].quantity?.message}</span>}
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
            <Button type="button" variant="secondary" size="sm" onClick={() => append({ itemId: 0, quantity: 1, unitPrice: 0, taxPercent: 0 })} style={{ marginTop: "0.75rem" }}>
              + Add Line
            </Button>

            <HStack gap={2} mt={4} className="form-actions">
              <Button type="submit" variant="primary" disabled={isSubmitting} loading={isSubmitting} loadingText="Creating...">
                Create Quotation
              </Button>
            </HStack>
          </form>
        </div>
      )}

      <div className="card">
        <h2>Compare Quotations</h2>
        <Box maxW="320px" className="field" style={{ maxWidth: 320 }}>
          <FormField label="Item to Compare">
            <HimsSelect id="compareItemId" aria-label="Item to Compare" value={compareItemId} onChange={(e) => setCompareItemId(Number(e.target.value))}>
              <option value={0}>Select item</option>
              {(items.data ?? []).map((i) => <option key={i.id} value={i.id}>{i.itemName}</option>)}
            </HimsSelect>
          </FormField>
        </Box>

        {compare.isLoading && <p className="muted">Loading comparison...</p>}
        {compare.isError && <div className="alert alert--error">Failed to load comparison</div>}
        {compare.data && compare.data.length === 0 && compareItemId > 0 && (
          <p className="empty-state">No quotations found for this item.</p>
        )}

        {Object.keys(groupedCompare).length > 0 && (
          <div className="table-wrap" style={{ marginTop: "1rem" }}>
            <Table.Root size="sm" variant="line">
              <Table.Header>
                <Table.Row>
                  <Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Quotation No</Table.ColumnHeader>
                  <Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Supplier</Table.ColumnHeader>
                  <Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Qty</Table.ColumnHeader>
                  <Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Unit Price</Table.ColumnHeader>
                  <Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Tax %</Table.ColumnHeader>
                  <Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Lead Days</Table.ColumnHeader>
                </Table.Row>
              </Table.Header>
              <Table.Body>
                {Object.entries(groupedCompare).map(([quotationId, rows]) => (
                  rows.map((row, idx) => (
                    <Table.Row key={`${quotationId}-${idx}`}>
                      <Table.Cell>{row.quotationNo}</Table.Cell>
                      <Table.Cell>{row.supplierName ?? row.supplierCode ?? `Supplier ${row.supplierId}`}</Table.Cell>
                      <Table.Cell>{row.quantity}</Table.Cell>
                      <Table.Cell>{row.unitPrice}</Table.Cell>
                      <Table.Cell>{row.taxPercent}%</Table.Cell>
                      <Table.Cell>{row.leadTimeDays ?? "-"}</Table.Cell>
                    </Table.Row>
                  ))
                ))}
              </Table.Body>
            </Table.Root>
          </div>
        )}
      </div>
    </section>
  );
}
