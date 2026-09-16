import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { chargeSchema, type ChargeValues } from "./zodSchema";
import { Button } from "@chakra-ui/react";
import { TextInput } from "../../components/ui";

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
      <TextInput type="number" placeholder="Service ID" {...register("serviceId")} />
      <TextInput placeholder="Service Name" {...register("serviceName")} />
      <TextInput type="number" step="0.01" placeholder="Rate" {...register("rate")} />
      <TextInput type="number" placeholder="Qty" {...register("qty")} />
      <Button type="submit" variant="primary">+ Add Charge</Button>
    </form>
  );
}
