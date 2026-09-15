import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { chargeSchema, type ChargeValues } from "./zodSchema";

interface Props {
  onAdd: (charge: ChargeValues) => void;
}

export function ChargeEntryForm({ onAdd }: Props) {
  const { register, handleSubmit, reset } = useForm<ChargeValues>({
    resolver: zodResolver(chargeSchema),
    defaultValues: { qty: 1 },
  });

  const submit = (values: ChargeValues) => {
    onAdd(values);
    reset();
  };

  return (
    <form className="inline-form" onSubmit={handleSubmit(submit)}>
      <input type="number" placeholder="Service ID" {...register("serviceId")} />
      <input placeholder="Service Name" {...register("serviceName")} />
      <input type="number" step="0.01" placeholder="Rate" {...register("rate")} />
      <input type="number" placeholder="Qty" {...register("qty")} />
      <button type="submit" className="btn">+ Add Charge</button>
    </form>
  );
}
