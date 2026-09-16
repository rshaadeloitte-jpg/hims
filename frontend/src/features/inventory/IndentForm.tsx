import { useForm, useFieldArray } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { Button, HStack, SimpleGrid, Table, Textarea } from "@chakra-ui/react";
import { FormField, HimsSelect, TextInput } from "../../components/ui";
import { indentRequestSchema, type IndentRequestValues } from "./zodSchema";
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
          <Button type="button" variant="secondary" size="sm" onClick={() => reset(getDefaultValues())}>
            Create Another
          </Button>
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

      <SimpleGrid columns={{ base: 1, md: 3 }} gap={4} className="indent-form__fields">
        <FormField label="Store" error={errors.storeId?.message} required>
          <HimsSelect
            id="storeId"
            aria-label="Store"
            {...register("storeId", { valueAsNumber: true })}
          >
            <option value={0}>Select store...</option>
            {stores.map((store) => (
              <option key={store.id} value={store.id}>
                {store.name} ({store.code})
              </option>
            ))}
          </HimsSelect>
        </FormField>

        <FormField label="Request Date" error={errors.requestDate?.message} required>
          <TextInput
            id="requestDate"
            aria-label="Request Date"
            type="date"
            {...register("requestDate")}
          />
        </FormField>

        <FormField label="Priority" required>
          <HimsSelect id="priority" aria-label="Priority" {...register("priority")}>
            <option value="ROUTINE">Routine</option>
            <option value="URGENT">Urgent</option>
            <option value="EMERGENCY">Emergency</option>
          </HimsSelect>
        </FormField>
      </SimpleGrid>

      <div className="indent-form__lines">
        <HStack justify="space-between" className="indent-form__lines-header">
          <h4 className="indent-form__lines-title">Line Items</h4>
          <Button
            type="button"
            variant="secondary"
            size="sm"
            onClick={() => append({ itemId: 0, requestedQty: 1, remarks: "" })}
          >
            + Add Line
          </Button>
        </HStack>

        <Table.Root size="sm" variant="line">
          <Table.Header>
            <Table.Row>
              <Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase" style={{ width: "45%" }}>Item</Table.ColumnHeader>
              <Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase" style={{ width: "20%" }}>Qty</Table.ColumnHeader>
              <Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase" style={{ width: "25%" }}>Remarks</Table.ColumnHeader>
              <Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase" style={{ width: "10%" }}></Table.ColumnHeader>
            </Table.Row>
          </Table.Header>
          <Table.Body>
            {fields.map((field, index) => (
              <Table.Row key={field.id}>
                <Table.Cell>
                  <HimsSelect
                    aria-label={`Line ${index + 1} item`}
                    {...register(`lines.${index}.itemId`, { valueAsNumber: true })}
                  >
                    <option value={0}>Select item...</option>
                    {items.map((item) => (
                      <option key={item.id} value={item.id}>
                        {item.name} ({item.code})
                      </option>
                    ))}
                  </HimsSelect>
                  {errors.lines?.[index]?.itemId && (
                    <span className="field__error">{errors.lines[index]?.itemId?.message}</span>
                  )}
                </Table.Cell>
                <Table.Cell>
                  <TextInput
                    type="number"
                    min={1}
                    aria-label={`Line ${index + 1} quantity`}
                    {...register(`lines.${index}.requestedQty`, { valueAsNumber: true })}
                  />
                  {errors.lines?.[index]?.requestedQty && (
                    <span className="field__error">{errors.lines[index]?.requestedQty?.message}</span>
                  )}
                </Table.Cell>
                <Table.Cell>
                  <TextInput type="text" aria-label={`Line ${index + 1} remarks`} {...register(`lines.${index}.remarks`)} />
                </Table.Cell>
                <Table.Cell>
                  {fields.length > 1 && (
                    <Button
                      type="button"
                      variant="ghost"
                      size="sm"
                      onClick={() => remove(index)}
                      aria-label="Remove line"
                    >
                      ✕
                    </Button>
                  )}
                </Table.Cell>
              </Table.Row>
            ))}
          </Table.Body>
        </Table.Root>
        {errors.lines && typeof errors.lines.message === "string" && (
          <span className="field__error">{errors.lines.message}</span>
        )}
      </div>

      <FormField label="Overall Remarks">
        <Textarea id="remarks" aria-label="Overall Remarks" rows={2} {...register("remarks")} bg="white" borderColor="gray.300" />
      </FormField>

      <HStack gap={2} mt={4} className="indent-form__actions">
        <Button
          type="submit"
          variant="primary"
          disabled={createIndent.isPending}
          loading={createIndent.isPending}
          loadingText="Submitting..."
        >
          Submit Indent
        </Button>
        <Button type="button" variant="secondary" onClick={() => reset(getDefaultValues())}>
          Reset
        </Button>
      </HStack>
    </form>
  );
}
