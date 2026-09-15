import { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { chargeSchema, type ChargeValues } from "./zodSchema";

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
            <label htmlFor="charge-service-id" className="sr-only">
              Service ID
            </label>
            <input
              id="charge-service-id"
              type="number"
              className="form-input"
              placeholder="Svc ID"
              {...register("serviceId")}
              aria-invalid={errors.serviceId ? true : undefined}
            />
            {errors.serviceId && (
              <span className="field-error">{errors.serviceId.message}</span>
            )}
          </div>
          <div className="charge-items__field charge-items__field--name">
            <label htmlFor="charge-service-name" className="sr-only">
              Service Name
            </label>
            <input
              id="charge-service-name"
              type="text"
              className="form-input"
              placeholder="Service name"
              {...register("serviceName")}
              aria-invalid={errors.serviceName ? true : undefined}
            />
            {errors.serviceName && (
              <span className="field-error">{errors.serviceName.message}</span>
            )}
          </div>
          <div className="charge-items__field charge-items__field--rate">
            <label htmlFor="charge-rate" className="sr-only">
              Rate
            </label>
            <input
              id="charge-rate"
              type="number"
              step="0.01"
              className="form-input"
              placeholder="Rate"
              {...register("rate")}
              aria-invalid={errors.rate ? true : undefined}
            />
            {errors.rate && (
              <span className="field-error">{errors.rate.message}</span>
            )}
          </div>
          <div className="charge-items__field charge-items__field--qty">
            <label htmlFor="charge-qty" className="sr-only">
              Quantity
            </label>
            <input
              id="charge-qty"
              type="number"
              className="form-input"
              placeholder="Qty"
              {...register("qty")}
              aria-invalid={errors.qty ? true : undefined}
            />
            {errors.qty && (
              <span className="field-error">{errors.qty.message}</span>
            )}
          </div>
          <button type="submit" className="btn btn--primary btn--sm charge-items__add">
            + Add
          </button>
        </div>
      </form>

      {charges.length > 0 ? (
        <div className="charge-items__table-wrap">
          <table className="charge-items__table">
            <thead>
              <tr>
                <th scope="col">#</th>
                <th scope="col">Service</th>
                <th scope="col" className="charge-items__col--num">Rate</th>
                <th scope="col" className="charge-items__col--num">Qty</th>
                <th scope="col" className="charge-items__col--num">Amount</th>
                <th scope="col" className="charge-items__col--action"></th>
              </tr>
            </thead>
            <tbody>
              {charges.map((charge, index) => (
                <tr key={index}>
                  <td className="charge-items__col--num">{index + 1}</td>
                  <td>
                    <span className="charge-items__service-name">{charge.serviceName}</span>
                    <span className="charge-items__service-id">ID: {charge.serviceId}</span>
                  </td>
                  <td className="charge-items__col--num">₹ {charge.rate.toFixed(2)}</td>
                  <td className="charge-items__col--num">{charge.qty}</td>
                  <td className="charge-items__col--num charge-items__amount">
                    ₹ {(charge.rate * charge.qty).toFixed(2)}
                  </td>
                  <td className="charge-items__col--action">
                    <button
                      type="button"
                      className="btn btn--sm btn--ghost charge-items__remove"
                      onClick={() => onRemove(index)}
                      aria-label={`Remove ${charge.serviceName}`}
                    >
                      ✕
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
            <tfoot>
              <tr>
                <td colSpan={4} className="charge-items__total-label">
                  Subtotal
                </td>
                <td className="charge-items__col--num charge-items__total-value">
                  ₹ {charges.reduce((s, c) => s + c.rate * c.qty, 0).toFixed(2)}
                </td>
                <td></td>
              </tr>
            </tfoot>
          </table>
        </div>
      ) : (
        <div className="charge-items__empty">
          <p className="muted">No charge items yet. Add services above.</p>
        </div>
      )}
    </div>
  );
}
