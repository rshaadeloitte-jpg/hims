import { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { dispenseSchema, type DispenseValues } from "./zodSchema";
import { useDispense } from "./queries";
import type { Prescription } from "./types";

interface DispenseFormProps {
  prescription: Prescription;
  onClose: () => void;
}

export function DispenseForm({ prescription, onClose }: DispenseFormProps) {
  const dispense = useDispense();
  const [showSuccess, setShowSuccess] = useState(false);

  const { register, handleSubmit, formState, getValues } = useForm<DispenseValues>({
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
      <div className="mt-6 p-4 bg-green-50 border border-green-200 rounded-md">
        <div className="flex">
          <div className="ml-3">
            <h3 className="text-sm font-medium text-green-800">Prescription dispensed successfully</h3>
            <div className="mt-2">
              <button
                onClick={onClose}
                className="px-4 py-2 bg-green-600 border border-transparent rounded-md text-sm font-medium text-white hover:bg-green-700"
              >
                Close
              </button>
            </div>
          </div>
        </div>
      </div>
    );
  }

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="mt-6 space-y-6">
      <div className="bg-gray-50 rounded-lg p-4">
        <h2 className="text-lg font-medium text-gray-900 mb-4">
          Dispense Prescription #{prescription.id} - {prescription.patientName}
        </h2>

        <div className="space-y-3">
          {prescription.items.map((item, index) => (
            <div key={item.id} className="flex items-center p-3 bg-white border rounded-lg">
              <div className="flex-1">
                <div className="font-medium text-gray-900">{item.drugName}</div>
                <div className="text-sm text-gray-500">
                  {item.strength} | {item.dosage} | Qty: {item.quantity}
                </div>
              </div>
              <div className="ml-4">
                <input
                  type="number"
                  min="1"
                  max={item.quantity}
                  {...register(`items.${index}.dispensedQuantity` as const)}
                  className="w-20 px-2 py-1 border border-gray-300 rounded-md focus:ring-blue-500 focus:border-blue-500"
                />
              </div>
            </div>
          ))}
        </div>
      </div>

      <div className="flex justify-end space-x-3">
        <button
          type="button"
          onClick={onClose}
          disabled={dispense.isPending}
          className="px-4 py-2 border border-gray-300 rounded-md text-sm font-medium text-gray-700 hover:bg-gray-50 disabled:opacity-50"
        >
          Cancel
        </button>
        <button
          type="submit"
          disabled={dispense.isPending || !formState.isValid}
          className="px-4 py-2 bg-blue-600 border border-transparent rounded-md text-sm font-medium text-white hover:bg-blue-700 disabled:opacity-50"
        >
          {dispense.isPending ? "Processing..." : "Complete Dispense"}
        </button>
      </div>

      {formState.errors.items && (
        <div className="text-sm text-red-600">Please verify all dispensed quantities are valid</div>
      )}
    </form>
  );
}