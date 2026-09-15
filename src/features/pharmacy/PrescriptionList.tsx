import { useState } from "react";
import { usePrescriptions, usePrescription } from "./queries";
import type { Prescription } from "./types";

export function PrescriptionList() {
  const { data: prescriptions = [], isLoading } = usePrescriptions();
  const [selectedId, setSelectedId] = useState(0);
  const { data: prescription } = usePrescription(selectedId);

  if (isLoading) return <p className="p-4 text-center text-gray-500">Loading prescriptions…</p>;

  return (
    <section className="space-y-6">
      <header>
        <h1 className="text-2xl font-bold text-gray-900">Prescription Dispensing</h1>
        <p className="text-sm text-gray-500 mt-1">Process pending prescriptions for dispensing</p>
      </header>

      <div className="overflow-x-auto">
        <table className="min-w-full divide-y divide-gray-200">
          <thead className="bg-gray-50">
            <tr>
              <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                #
              </th>
              <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                Patient
              </th>
              <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                Status
              </th>
              <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                Items
              </th>
              <th scope="col" className="relative px-6 py-3">
                <span className="sr-only">Actions</span>
              </th>
            </tr>
          </thead>
          <tbody className="bg-white divide-y divide-gray-200">
            {prescriptions.map((p) => (
              <tr key={p.id} className={selectedId === p.id ? "bg-blue-50" : ""}>
                <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">{p.id}</td>
                <td className="px-6 py-4 whitespace-nowrap">
                  <div className="text-sm font-medium text-gray-900">{p.patientName}</div>
                  <div className="text-xs text-gray-500">ID: {p.patientId}</div>
                </td>
                <td className="px-6 py-4 whitespace-nowrap">
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
                </td>
                <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">{p.items.length}</td>
                <td className="px-6 py-4 whitespace-nowrap text-right text-sm font-medium">
                  <button
                    onClick={() => setSelectedId(p.id)}
                    disabled={p.status !== "PENDING"}
                    className="text-blue-600 hover:text-blue-900 disabled:opacity-50"
                  >
                    {p.status === "PENDING" ? "Dispense" : "View"}
                  </button>
                </td>
              </tr>
            ))}
            {prescriptions.length === 0 && (
              <tr>
                <td colSpan={5} className="px-6 py-8 text-center text-gray-500">
                  No pending prescriptions
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>

      {prescription && <DispenseForm prescription={prescription} onClose={() => setSelectedId(0)} />}
    </section>
  );
}

interface DispenseFormProps {
  prescription: Prescription;
  onClose: () => void;
}

function DispenseForm({ prescription, onClose }: DispenseFormProps) {
  // Implementation will be in DispenseForm.tsx
  return (
    <div className="mt-6 p-4 bg-gray-50 rounded-lg">
      <h2 className="text-lg font-medium text-gray-900 mb-4">Dispense Prescription #{prescription.id}</h2>
      <div className="space-y-4">
        {prescription.items.map((item) => (
          <div key={item.id} className="flex items-center justify-between p-3 bg-white border rounded-md">
            <div>
              <div className="font-medium text-gray-900">{item.drugName}</div>
              <div className="text-sm text-gray-500">
                {item.strength} | {item.dosage} | Qty: {item.quantity}
              </div>
            </div>
            <div className="text-sm font-medium text-gray-700">Dispensed: {item.dispensedQuantity || 0}</div>
          </div>
        ))}
        <div className="flex justify-end space-x-3">
          <button onClick={onClose} className="px-4 py-2 border border-gray-300 rounded-md text-sm font-medium text-gray-700 hover:bg-gray-50">
            Cancel
          </button>
          <button className="px-4 py-2 bg-blue-600 border border-transparent rounded-md text-sm font-medium text-white hover:bg-blue-700">
            Complete Dispense
          </button>
        </div>
      </div>
    </div>
  );
}