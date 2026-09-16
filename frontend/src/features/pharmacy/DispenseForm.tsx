import { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { dispenseSchema, type DispenseValues } from "./zodSchema";
import { useDispense } from "./queries";
import type { Prescription } from "./types";
import { Box, Button, HStack, VStack } from "@chakra-ui/react";
import { TextInput } from "../../components/ui";

interface DispenseFormProps {
  prescription: Prescription;
  onClose: () => void;
}

export function DispenseForm({ prescription, onClose }: DispenseFormProps) {
  const dispense = useDispense();
  const [showSuccess, setShowSuccess] = useState(false);

  const { register, handleSubmit, formState } = useForm<DispenseValues>({
    resolver: zodResolver(dispenseSchema),
    defaultValues: {
      prescriptionId: prescription.id,
      items: prescription.items.map((item) => ({
        itemId: item.id,
        dispensedQuantity: item.quantity,
      })),
    },
  });

  const onSubmit = (data: DispenseValues) => {
    dispense.mutate(data, {
      onSuccess: () => {
        setShowSuccess(true);
        setTimeout(() => {
          onClose();
          setShowSuccess(false);
        }, 2000);
      },
    });
  };

  if (showSuccess) {
    return (
      <Box mt={6} p={4} bg="green.50" borderWidth="1px" borderColor="green.200" borderRadius="md">
        <div className="flex">
          <div className="ml-3">
            <h3 className="text-sm font-medium text-green-800">Prescription dispensed successfully</h3>
            <div className="mt-2">
              <Button
                onClick={onClose}
                variant="primary"
              >
                Close
              </Button>
            </div>
          </div>
        </div>
      </Box>
    );
  }

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="mt-6 space-y-6">
      <Box bg="gray.50" borderRadius="lg" p={4}>
        <h2 className="text-lg font-medium text-gray-900 mb-4">
          Dispense Prescription #{prescription.id} - {prescription.patientName}
        </h2>

        <VStack gap={3} align="stretch">
          {prescription.items.map((item, index) => (
            <HStack key={item.id} align="center" p={3} bg="white" borderWidth="1px" borderRadius="lg">
              <div className="flex-1">
                <div className="font-medium text-gray-900">{item.drugName}</div>
                <div className="text-sm text-gray-500">
                  {item.strength} | {item.dosage} | Qty: {item.quantity}
                </div>
              </div>
              <div className="ml-4">
                <TextInput
                  type="number"
                  min="1"
                  max={item.quantity}
                  {...register(`items.${index}.dispensedQuantity` as const)}
                />
              </div>
            </HStack>
          ))}
        </VStack>
      </Box>

      <HStack justify="flex-end" gap={3}>
        <Button
          type="button"
          onClick={onClose}
          disabled={dispense.isPending}
          variant="secondary"
        >
          Cancel
        </Button>
        <Button
          type="submit"
          disabled={dispense.isPending || !formState.isValid}
          variant="primary"
          loading={dispense.isPending}
          loadingText="Processing..."
        >
          Complete Dispense
        </Button>
      </HStack>

      {formState.errors.items && (
        <div className="text-sm text-red-600">Please verify all dispensed quantities are valid</div>
      )}
    </form>
  );
}
