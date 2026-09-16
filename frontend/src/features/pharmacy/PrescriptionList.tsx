import { useState } from "react";
import { usePrescriptions, usePrescription } from "./queries";
import type { Prescription } from "./types";
import { Box, Button, HStack, Table, VStack } from "@chakra-ui/react";

export function PrescriptionList() {
  const { data: prescriptions = [], isLoading } = usePrescriptions();
  const [selectedId, setSelectedId] = useState(0);
  const { data: prescription } = usePrescription(selectedId);

  if (isLoading) return <p className="p-4 text-center text-gray-500">Loading prescriptions…</p>;

  return (
    <VStack gap={6} align="stretch" as="section">
      <header>
        <h1 className="text-2xl font-bold text-gray-900">Prescription Dispensing</h1>
        <p className="text-sm text-gray-500 mt-1">Process pending prescriptions for dispensing</p>
      </header>

      <Box overflowX="auto">
        <Table.Root size="sm" variant="line">
          <Table.Header>
            <Table.Row>
              <Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase" scope="col">
                #
              </Table.ColumnHeader>
              <Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase" scope="col">
                Patient
              </Table.ColumnHeader>
              <Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase" scope="col">
                Status
              </Table.ColumnHeader>
              <Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase" scope="col">
                Items
              </Table.ColumnHeader>
              <Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase" scope="col">
                <span className="sr-only">Actions</span>
              </Table.ColumnHeader>
            </Table.Row>
          </Table.Header>
          <Table.Body>
            {prescriptions.map((p) => (
              <Table.Row key={p.id} bg={selectedId === p.id ? "blue.50" : undefined}>
                <Table.Cell>{p.id}</Table.Cell>
                <Table.Cell>
                  <div className="text-sm font-medium text-gray-900">{p.patientName}</div>
                  <div className="text-xs text-gray-500">ID: {p.patientId}</div>
                </Table.Cell>
                <Table.Cell>
                  <span
                    className={`px-2 inline-flex text-xs leading-5 font-semibold rounded-full ${
                      p.status === "PENDING"
                        ? "bg-yellow-100 text-yellow-800"
                        : p.status === "DISPENSED"
                        ? "bg-green-100 text-green-800"
                        : "bg-red-100 text-red-800"
                    }`}
                  >
                    {p.status}
                  </span>
                </Table.Cell>
                <Table.Cell>{p.items.length}</Table.Cell>
                <Table.Cell>
                  <Button
                    onClick={() => setSelectedId(p.id)}
                    disabled={p.status !== "PENDING"}
                    variant="ghost"
                    size="sm"
                  >
                    {p.status === "PENDING" ? "Dispense" : "View"}
                  </Button>
                </Table.Cell>
              </Table.Row>
            ))}
            {prescriptions.length === 0 && (
              <Table.Row>
                <Table.Cell colSpan={5} className="px-6 py-8 text-center text-gray-500">
                  No pending prescriptions
                </Table.Cell>
              </Table.Row>
            )}
          </Table.Body>
        </Table.Root>
      </Box>

      {prescription && <DispenseForm prescription={prescription} onClose={() => setSelectedId(0)} />}
    </VStack>
  );
}

interface DispenseFormProps {
  prescription: Prescription;
  onClose: () => void;
}

function DispenseForm({ prescription, onClose }: DispenseFormProps) {
  // Implementation will be in DispenseForm.tsx
  return (
    <Box mt={6} p={4} bg="gray.50" borderRadius="lg">
      <h2 className="text-lg font-medium text-gray-900 mb-4">Dispense Prescription #{prescription.id}</h2>
      <VStack gap={4} align="stretch">
        {prescription.items.map((item) => (
          <HStack key={item.id} justify="space-between" p={3} bg="white" borderWidth="1px" borderRadius="md">
            <div>
              <div className="font-medium text-gray-900">{item.drugName}</div>
              <div className="text-sm text-gray-500">
                {item.strength} | {item.dosage} | Qty: {item.quantity}
              </div>
            </div>
            <div className="text-sm font-medium text-gray-700">Dispensed: {item.dispensedQuantity || 0}</div>
          </HStack>
        ))}
        <HStack justify="flex-end" gap={3}>
          <Button onClick={onClose} variant="secondary">
            Cancel
          </Button>
          <Button variant="primary">
            Complete Dispense
          </Button>
        </HStack>
      </VStack>
    </Box>
  );
}
