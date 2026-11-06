import { useState } from "react";
import { usePrescriptions, usePrescription } from "./queries";
import { Box, Button, Table, VStack } from "@chakra-ui/react";

const statusLabel = (code: number) =>
  code === 1 ? "ACTIVE" : code === 2 ? "COMPLETED" : code === 3 ? "CANCELLED" : String(code);

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
        <p className="text-sm text-amber-600 mt-1">
          Dispensing a prescription is not supported by the backend; use Item Sales instead.
        </p>
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
              <Table.Row key={p.prescriptionId} bg={selectedId === p.prescriptionId ? "blue.50" : undefined}>
                <Table.Cell>{p.prescriptionId}</Table.Cell>
                <Table.Cell>
                  <div className="text-sm font-medium text-gray-900">Patient {p.patientId}</div>
                  <div className="text-xs text-gray-500">ID: {p.patientId}</div>
                </Table.Cell>
                <Table.Cell>
                  <span
                    className={`px-2 inline-flex text-xs leading-5 font-semibold rounded-full ${
                      p.statusCode === 1
                        ? "bg-yellow-100 text-yellow-800"
                        : p.statusCode === 2
                        ? "bg-green-100 text-green-800"
                        : "bg-red-100 text-red-800"
                    }`}
                  >
                    {statusLabel(p.statusCode)}
                  </span>
                </Table.Cell>
                <Table.Cell>{p.details?.length ?? 0}</Table.Cell>
                <Table.Cell>
                  <Button
                    onClick={() => setSelectedId(p.prescriptionId)}
                    disabled
                    variant="ghost"
                    size="sm"
                    title="Dispensing a prescription is not supported by the backend"
                  >
                    Dispense (unavailable)
                  </Button>
                </Table.Cell>
              </Table.Row>
            ))}
            {prescriptions.length === 0 && (
              <Table.Row>
                <Table.Cell colSpan={5} className="px-6 py-8 text-center text-gray-500">
                  No prescriptions
                </Table.Cell>
              </Table.Row>
            )}
          </Table.Body>
        </Table.Root>
      </Box>

      {prescription && (
        <Box mt={6} p={4} bg="gray.50" borderRadius="lg">
          <h2 className="text-lg font-medium text-gray-900 mb-4">
            Prescription #{prescription.prescriptionId}
          </h2>
          <VStack gap={4} align="stretch">
            {(prescription.details ?? []).map((item) => (
              <div key={item.lineNo} className="flex justify-between p-3 bg-white border rounded-md">
                <div>
                  <div className="font-medium text-gray-900">Item {item.itemId}</div>
                  <div className="text-sm text-gray-500">
                    {item.dose} | {item.frequency} | Qty: {item.qty}
                  </div>
                </div>
              </div>
            ))}
          </VStack>
        </Box>
      )}
    </VStack>
  );
}
