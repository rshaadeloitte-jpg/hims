import { useState } from 'react';
import { Button, Table } from '@chakra-ui/react';
import { TextInput } from "../../components/ui";
import { useLabOrder, useEnterResults } from './queries';
import { useResultValidation } from './useResultValidation';

interface ResultEntryProps {
  orderId: number;
  onCompleted?: () => void;
}

export function ResultEntry({ orderId, onCompleted }: ResultEntryProps) {
  const { data: order, isLoading } = useLabOrder(orderId);
  const enterResults = useEnterResults();
  const [resultValues, setResultValues] = useState<Record<number, string>>({});
  const [remarks, setRemarks] = useState<Record<number, string>>({});
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
        <Button variant="primary" size="sm" onClick={handleSubmit} loading={enterResults.isPending} loadingText="Saving..." disabled={!hasValues || enterResults.isPending}>
          {enterResults.isPending ? 'Saving...' : 'Save Results'}
        </Button>
      </header>
      <div className="border-2 border-dashed border-gray-300 rounded-lg p-6 text-center bg-gray-50">
        <p className="text-sm text-gray-600 mb-2">Drag and drop result files here, or</p>
        <Button variant="secondary" size="sm">Browse Files</Button>
        <p className="text-xs text-gray-500 mt-2">Supported formats: PDF, CSV, Excel (Max 10MB)</p>
      </div>
      <div className="border border-gray-200 rounded-lg overflow-hidden">
        <Table.Root size="sm" variant="line">
          <Table.Header>
            <Table.Row>
              <Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Test</Table.ColumnHeader>
              <Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Reference Range</Table.ColumnHeader>
              <Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Result Value</Table.ColumnHeader>
              <Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Status</Table.ColumnHeader>
              <Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Remarks</Table.ColumnHeader>
            </Table.Row>
          </Table.Header>
          <Table.Body>
            {pendingDetails.length === 0 ? (
              <Table.Row><Table.Cell colSpan={5}>No tests pending result entry</Table.Cell></Table.Row>
            ) : pendingDetails.map((detail) => {
              const validation = validate(resultValues[detail.detailId] || '');
              return (
                <Table.Row key={detail.detailId}>
                  <Table.Cell><div className="text-sm font-medium text-gray-900">{detail.testName}</div><div className="text-xs text-gray-500 font-mono">{detail.testCode}</div></Table.Cell>
                  <Table.Cell><span className="font-mono text-sm text-gray-600">{detail.referenceRange || 'N/A'}</span></Table.Cell>
                  <Table.Cell>
                    <TextInput type="text" value={resultValues[detail.detailId] || ''} onChange={(e) => setResultValues(prev => ({ ...prev, [detail.detailId]: e.target.value }))} placeholder="Enter value" />
                    {validation.message && <div className={`text-xs ${validation.isCritical ? 'text-red-700 font-semibold' : validation.isAbnormal ? 'text-yellow-700' : 'text-green-700'}`}>{validation.message}</div>}
                  </Table.Cell>
                  <Table.Cell><span className={`inline-flex items-center px-2 py-0.5 rounded text-xs font-medium ${detail.status === 'IN_PROGRESS' ? 'bg-blue-100 text-blue-800' : 'bg-gray-100 text-gray-800'}`}>{detail.status}</span></Table.Cell>
                  <Table.Cell><TextInput type="text" value={remarks[detail.detailId] || ''} onChange={(e) => setRemarks(prev => ({ ...prev, [detail.detailId]: e.target.value }))} placeholder="Optional remarks" /></Table.Cell>
                </Table.Row>
              );
            })}
          </Table.Body>
        </Table.Root>
      </div>
    </section>
  );
}