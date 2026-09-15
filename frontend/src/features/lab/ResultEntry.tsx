import { useState } from 'react';
import { useLabOrder, useEnterResults } from './queries';
import { useResultValidation } from './useResultValidation';
import type { LabDetail, LabTest } from './types';

interface ResultEntryProps {
  orderId: number;
  onCompleted?: () => void;
}

export function ResultEntry({ orderId, onCompleted }: ResultEntryProps) {
  const { data: order, isLoading } = useLabOrder(orderId);
  const enterResults = useEnterResults();
  const [resultValues, setResultValues] = useState<Record<number, string>>({});
  const [remarks, setRemarks] = useState<Record<number, string>>({});
  const testMap = new Map<number, LabTest>();
  const { validate } = useResultValidation();

  const handleSubmit = async () => {
    const results = Object.entries(resultValues)
      .filter(([_, value]) => value.trim() !== '')
      .map(([detailId, value]) => ({
        detailId: Number(detailId),
        value,
        remarks: remarks[Number(detailId)] || undefined,
      }));
    if (results.length === 0) return;
    try {
      for (const result of results) {
        await enterResults.mutateAsync({
          id: orderId,
          detailId: result.detailId,
          results: [{ value: result.value, remarks: result.remarks }],
        });
      }
      setResultValues({});
      setRemarks({});
      onCompleted?.();
    } catch (err) {
      console.error('Failed to enter results:', err);
    }
  };

  if (isLoading) return <div className="p-4 text-center text-gray-500">Loading order...</div>;
  if (!order) return <div className="p-4 text-center text-red-600">Order not found</div>;

  const pendingDetails = order.details?.filter(d => 
    d.status === 'IN_PROGRESS' || d.status === 'COLLECTED'
  ) ?? [];
  const hasValues = Object.values(resultValues).some(v => v.trim() !== '');

  return (
    <section className="space-y-4">
      <header className="flex items-center justify-between">
        <div>
          <h2 className="text-xl font-semibold text-gray-900">Result Entry</h2>
          <p className="text-sm text-gray-500">Order #{orderId}</p>
        </div>
        <button onClick={handleSubmit} disabled={!hasValues || enterResults.isPending} className="px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700 disabled:opacity-50 transition-colors">
          {enterResults.isPending ? 'Saving...' : 'Save Results'}
        </button>
      </header>
      <div className="border-2 border-dashed border-gray-300 rounded-lg p-6 text-center bg-gray-50">
        <p className="text-sm text-gray-600 mb-2">Drag and drop result files here, or</p>
        <button className="px-4 py-2 bg-white border border-gray-300 rounded-md hover:bg-gray-50 text-sm font-medium text-gray-700">Browse Files</button>
        <p className="text-xs text-gray-500 mt-2">Supported formats: PDF, CSV, Excel (Max 10MB)</p>
      </div>
      <div className="border border-gray-200 rounded-lg overflow-hidden">
        <table className="w-full">
          <thead className="bg-gray-50 border-b border-gray-200">
            <tr>
              <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Test</th>
              <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Reference Range</th>
              <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Result Value</th>
              <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Status</th>
              <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Remarks</th>
            </tr>
          </thead>
          <tbody className="bg-white divide-y divide-gray-200">
            {pendingDetails.length === 0 ? (
              <tr><td colSpan={5} className="px-4 py-8 text-center text-gray-500">No tests pending result entry</td></tr>
            ) : pendingDetails.map((detail) => {
              const validation = validate(resultValues[detail.detailId] || '');
              return (
                <tr key={detail.detailId} className="hover:bg-gray-50">
                  <td className="px-4 py-3"><div className="text-sm font-medium text-gray-900">{detail.testName}</div><div className="text-xs text-gray-500 font-mono">{detail.testCode}</div></td>
                  <td className="px-4 py-3"><span className="font-mono text-sm text-gray-600">{detail.referenceRange || 'N/A'}</span></td>
                  <td className="px-4 py-3">
                    <input type="text" value={resultValues[detail.detailId] || ''} onChange={(e) => setResultValues(prev => ({ ...prev, [detail.detailId]: e.target.value }))} placeholder="Enter value" className={`w-full px-3 py-2 border rounded-md focus:outline-none focus:ring-2 font-mono text-sm ${validation.isCritical ? 'border-red-500 focus:ring-red-500 bg-red-50' : validation.isAbnormal ? 'border-yellow-500 focus:ring-yellow-500 bg-yellow-50' : 'border-gray-300 focus:ring-blue-500'}`} />
                    {validation.message && <div className={`text-xs ${validation.isCritical ? 'text-red-700 font-semibold' : validation.isAbnormal ? 'text-yellow-700' : 'text-green-700'}`}>{validation.message}</div>}
                  </td>
                  <td className="px-4 py-3"><span className={`inline-flex items-center px-2 py-0.5 rounded text-xs font-medium ${detail.status === 'IN_PROGRESS' ? 'bg-blue-100 text-blue-800' : 'bg-gray-100 text-gray-800'}`}>{detail.status}</span></td>
                  <td className="px-4 py-3"><input type="text" value={remarks[detail.detailId] || ''} onChange={(e) => setRemarks(prev => ({ ...prev, [detail.detailId]: e.target.value }))} placeholder="Optional remarks" className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 text-sm" /></td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>
    </section>
  );
}