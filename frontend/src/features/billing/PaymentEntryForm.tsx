import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { paymentSchema, type PaymentValues } from "./zodSchema";
import { Button } from "@chakra-ui/react";
import { HimsSelect, TextInput } from "../../components/ui";

interface Props {
  onSubmit: (payment: PaymentValues) => void;
}

export function PaymentEntryForm({ onSubmit }: Props) {
  const { register, handleSubmit, reset } = useForm<PaymentValues>({
    resolver: zodResolver(paymentSchema),
    defaultValues: { paymentMethod: "CASH" },
  });

  const submit = (values: PaymentValues) => {
    onSubmit(values);
    reset();
  };

  return (
    <form className="inline-form" onSubmit={handleSubmit(submit)}>
      <TextInput type="number" step="0.01" placeholder="Amount" {...register("amount")} />
      <HimsSelect {...register("paymentMethod")}>
        <option value="CASH">Cash</option>
        <option value="CARD">Card</option>
        <option value="UPI">UPI</option>
        <option value="CHEQUE">Cheque</option>
        <option value="ADVANCE">Advance</option>
      </HimsSelect>
      <TextInput placeholder="Reference No" {...register("referenceNo")} />
      <TextInput placeholder="Remarks" {...register("remarks")} />
      <Button type="submit" variant="primary">+ Record</Button>
    </form>
  );
}
