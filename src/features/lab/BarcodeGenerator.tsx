import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { labQueryKeys } from './queries';
import type { LabDetail } from './types';

const LAB_BASE: string = import.meta.env.VITE_LAB_API_BASE ?? '/api';

async function labJson<T>(url: string, options?: RequestInit): Promise<T> {
  const res = await fetch(url, {
    headers: { 
      'Content-Type': 'application/json',
      'X-Request-ID': crypto.randomUUID()
    },
    ...options,
  });
  if (!res.ok) {
    const err = await res.json().catch(() => ({ message: res.statusText }));
    throw new Error(err.message ?? 'Request failed');
  }
  return res.json();
}

interface BarcodeGeneratorProps {
  orderId: number;
  detailId: number;
  testName: string;
  onGenerated?: (barcode: string) => void;
}

/**
 * M15 Barcode Generator - Generate sample IDs for lab tests
 */
export function BarcodeGenerator({ orderId, detailId, testName, onGenerated }: BarcodeGeneratorProps) {
  const [generatedBarcode, setGeneratedBarcode] = useState<string | null>(null);
  const qc = useQueryClient();

  const generateBarcode = useMutation({
    mutationFn: () =>
      labJson<LabDetail>(`${LAB_BASE}/api/v1/lab/orders/${orderId}/details/${detailId}/barcode`, {
        method: 'POST',
      }),
    onSuccess: (data) => {
      setGeneratedBarcode(data.barcode ?? null);
      void qc.invalidateQueries({ queryKey: labQueryKeys.orders() });
      if (data.barcode) {
        onGenerated?.(data.barcode);
      }
    },
  });

  const handleGenerate = () => {
    generateBarcode.mutate();
  };

  const handlePrint = () => {
    if (!generatedBarcode) return;
    
    const printWindow = window.open('', '_blank');
    if (!printWindow) return;

    printWindow.document.write(`
      <!DOCTYPE html>
      <html>
      <head>
        <title>Barcode Label</title>
        <style>
          body { font-family: monospace; padding: 20px; }
          .label { border: 2px solid #000; padding: 15px; width: 300px; margin: 0 auto; }
          .barcode { font-size: 24px; text-align: center; letter-spacing: 2px; margin: 10px 0; }
          .test-name { font-size: 12px; text-align: center; margin-top: 10px; }
          @media print {
            body { padding: 0; }
            .no-print { display: none; }
          }
        </style>
      </head>
      <body>
        <div class="label">
          <div class="barcode">${generatedBarcode}</div>
          <div class="test-name">${testName}</div>
          <div style="text-align: center; font-size: 10px; margin-top: 5px;">
            Order #${orderId}
          </div>
        </div>
        <div class="no-print" style="text-align: center; margin-top: 20px;">
          <button onclick="window.print()">Print</button>
          <button onclick="window.close()">Close</button>
        </div>
      </body>
      </html>
    `);
    printWindow.document.close();
  };

  return (
    <div className="p-4 border border-gray-200 rounded-lg bg-white">
      <div className="flex items-center justify-between mb-3">
        <h3 className="text-sm font-medium text-gray-900">Barcode Generation</h3>
        <span className="text-xs text-gray-500 font-mono">Order #{orderId}</span>
      </div>

      <div className="space-y-3">
        <div className="text-sm text-gray-600">
          <span className="font-medium">Test:</span> {testName}
        </div>

        {generatedBarcode ? (
          <div className="space-y-3">
            <div className="p-4 bg-gray-50 border border-gray-300 rounded-md">
              <div className="text-center">
                <div className="font-mono text-2xl tracking-wider text-gray-900 mb-2">
                  {generatedBarcode}
                </div>
                <div className="text-xs text-gray-500">{testName}</div>
              </div>
            </div>
            <div className="flex gap-2">
              <button
                onClick={handlePrint}
                className="flex-1 px-3 py-2 bg-blue-600 text-white text-sm rounded-md hover:bg-blue-700 transition-colors"
              >
                Print Label
              </button>
              <button
                onClick={() => {
                  setGeneratedBarcode(null);
                }}
                className="px-3 py-2 bg-gray-100 text-gray-700 text-sm rounded-md hover:bg-gray-200 transition-colors"
              >
                Regenerate
              </button>
            </div>
          </div>
        ) : (
          <button
            onClick={handleGenerate}
            disabled={generateBarcode.isPending}
            className="w-full px-4 py-2 bg-green-600 text-white text-sm rounded-md hover:bg-green-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
          >
            {generateBarcode.isPending ? 'Generating...' : 'Generate Barcode'}
          </button>
        )}

        {generateBarcode.isError && (
          <div className="p-2 bg-red-50 border border-red-200 rounded-md">
            <p className="text-xs text-red-800">
              Error: {(generateBarcode.error as Error).message}
            </p>
          </div>
        )}
      </div>
    </div>
  );
}