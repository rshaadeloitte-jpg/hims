import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { chargeSchema, type ChargeValues } from "./zodSchema";
import { Button, Table } from "@chakra-ui/react";
import { FormField, TextInput } from "../../components/ui";

interface ChargeItemsProps {
  charges: ChargeValues[];
  onAdd: (charge: ChargeValues) => void;
  onRemove: (index: number) => void;
}

/**
 * ChargeItems — manages charge line items for a bill.
 * Stripe-style inline form: add a line, see it appear in the table below.
 * Supports quick-entry with tab navigation between fields.
 */
export function ChargeItems({ charges, onAdd, onRemove }: ChargeItemsProps) {
  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<ChargeValues>({
    resolver: zodResolver(chargeSchema),
    defaultValues: { qty: 1, rate: 0, serviceId: undefined as unknown as number, serviceName: "" },
  });

  const handleAdd = (values: ChargeValues) => {
    onAdd(values);
    reset({ qty: 1, rate: 0, serviceId: undefined as unknown as number, serviceName: "" });
  };

  return (
    <div className="charge-items">
      <form className="charge-items__form" onSubmit={handleSubmit(handleAdd)}>
        <div className="charge-items__row">
          <div className="charge-items__field charge-items__field--id">
            <FormField label="Service ID" error={errors.serviceId?.message}>
              <TextInput
                id="charge-service-id"
                type="number"
                placeholder="Svc ID"
                {...register("serviceId")}
                aria-invalid={errors.serviceId ? true : undefined}
              />
            </FormField>
          </div>
          <div className="charge-items__field charge-items__field--name">
            <FormField label="Service Name" error={errors.serviceName?.message}>
              <TextInput
                id="charge-service-name"
                type="text"
                placeholder="Service name"
                {...register("serviceName")}
                aria-invalid={errors.serviceName ? true : undefined}
              />
            </FormField>
          </div>
          <div className="charge-items__field charge-items__field--rate">
            <FormField label="Rate" error={errors.rate?.message}>
              <TextInput
                id="charge-rate"
                type="number"
                step="0.01"
                placeholder="Rate"
                {...register("rate")}
                aria-invalid={errors.rate ? true : undefined}
              />
            </FormField>
          </div>
          <div className="charge-items__field charge-items__field--qty">
            <FormField label="Quantity" error={errors.qty?.message}>
              <TextInput
                id="charge-qty"
                type="number"
                placeholder="Qty"
                {...register("qty")}
                aria-invalid={errors.qty ? true : undefined}
              />
            </FormField>
          </div>
          <Button type="submit" variant="primary" size="sm" className="charge-items__add">
            + Add
          </Button>
        </div>
      </form>

      {charges.length > 0 ? (
        <div className="charge-items__table-wrap">
          <Table.Root size="sm" variant="line" className="charge-items__table">
            <Table.Header>
              <Table.Row>
                <Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase" scope="col">#</Table.ColumnHeader>
                <Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase" scope="col">Service</Table.ColumnHeader>
                <Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase" scope="col" className="charge-items__col--num">Rate</Table.ColumnHeader>
                <Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase" scope="col" className="charge-items__col--num">Qty</Table.ColumnHeader>
                <Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase" scope="col" className="charge-items__col--num">Amount</Table.ColumnHeader>
                <Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase" scope="col" className="charge-items__col--action"></Table.ColumnHeader>
              </Table.Row>
            </Table.Header>
            <Table.Body>
              {charges.map((charge, index) => (
                <Table.Row key={index}>
                  <Table.Cell className="charge-items__col--num">{index + 1}</Table.Cell>
                  <Table.Cell>
                    <span className="charge-items__service-name">{charge.serviceName}</span>
                    <span className="charge-items__service-id">ID: {charge.serviceId}</span>
                  </Table.Cell>
                  <Table.Cell className="charge-items__col--num">₹ {charge.rate.toFixed(2)}</Table.Cell>
                  <Table.Cell className="charge-items__col--num">{charge.qty}</Table.Cell>
                  <Table.Cell className="charge-items__col--num charge-items__amount">
                    ₹ {(charge.rate * charge.qty).toFixed(2)}
                  </Table.Cell>
                  <Table.Cell className="charge-items__col--action">
                    <Button
                      type="button"
                      variant="ghost"
                      size="sm"
                      className="charge-items__remove"
                      onClick={() => onRemove(index)}
                      aria-label={`Remove ${charge.serviceName}`}
                    >
                      ✕
                    </Button>
                  </Table.Cell>
                </Table.Row>
              ))}
            </Table.Body>
            <Table.Footer>
              <Table.Row>
                <Table.Cell colSpan={4} className="charge-items__total-label">
                  Subtotal
                </Table.Cell>
                <Table.Cell className="charge-items__col--num charge-items__total-value">
                  ₹ {charges.reduce((s, c) => s + c.rate * c.qty, 0).toFixed(2)}
                </Table.Cell>
                <Table.Cell></Table.Cell>
              </Table.Row>
            </Table.Footer>
          </Table.Root>
        </div>
      ) : (
        <div className="charge-items__empty">
          <p className="muted">No charge items yet. Add services above.</p>
        </div>
      )}
    </div>
  );
}
