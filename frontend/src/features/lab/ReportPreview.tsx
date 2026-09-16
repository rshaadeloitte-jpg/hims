import { Button, Table } from '@chakra-ui/react';
import { useLabOrder } from './queries';
import type { LabReport, LabDetail } from './types';

interface ReportPreviewProps { orderId: number; onClose?: () => void; }

export function ReportPreview({ orderId, onClose }: ReportPreviewProps) {
  const { data: order, isLoading } = useLabOrder(orderId);
  const handlePrint = () => window.print();
  if (isLoading) return <div className="p-4 text-center text-gray-500">Loading report...</div>;
  if (!order) return <div className="p-4 text-center text-red-600">Order not found</div>;

  const completedDetails = order.details?.filter(d => d.status === 'VERIFIED' || d.status === 'COMPLETED') ?? [];
  const report: LabReport = {
    orderId: order.orderId, patientId: order.patientId,
    patientName: order.patientName || `Patient #${order.patientId}`,
    orderDate: order.orderDate, collectedAt: order.details?.[0]?.sampleCollectedAt,
    verifiedAt: order.details?.[0]?.verifiedAt, details: completedDetails,
    reportGeneratedAt: new Date().toISOString(), generatedBy: 1,
  };

  return (
    <section className="space-y-4">
      <div className="flex items-center justify-between no-print">
        <div><h2 className="text-xl font-semibold text-gray-900">Report Preview</h2><p className="text-sm text-gray-500">Order #{report.orderId}</p></div>
        <div className="flex gap-2"><Button variant="primary" size="sm" onClick={handlePrint}>🖨️ Print Report</Button>{onClose && <Button variant="secondary" size="sm" onClick={onClose}>Close</Button>}</div>
      </div>
      <div className="bg-white border border-gray-300 rounded-lg shadow-lg max-w-4xl mx-auto print:border-0 print:shadow-none">
        <div className="border-b-2 border-gray-900 p-8"><div className="flex items-start justify-between"><div><h1 className="text-3xl font-bold text-gray-900">LABORATORY REPORT</h1><p className="text-sm text-gray-600 mt-1">Diagnostic & Research Laboratory</p></div><div className="text-right"><div className="text-xs text-gray-500">Report No</div><div className="font-mono text-lg font-bold text-gray-900">LAB-{report.orderId.toString().padStart(6, '0')}</div></div></div></div>
        <div className="p-8 bg-gray-50 border-b border-gray-300">
          <div className="grid grid-cols-2 gap-6">
            <div><h3 className="text-xs font-semibold text-gray-500 uppercase tracking-wider mb-2">Patient Information</h3><div className="space-y-1"><div className="flex"><span className="text-sm text-gray-600 w-32">Patient Name:</span><span className="text-sm font-medium text-gray-900">{report.patientName}</span></div><div className="flex"><span className="text-sm text-gray-600 w-32">Patient ID:</span><span className="text-sm font-mono text-gray-900">{report.patientId}</span></div>{report.patientAge && <div className="flex"><span className="text-sm text-gray-600 w-32">Age:</span><span className="text-sm text-gray-900">{report.patientAge} years</span></div>}{report.patientGender && <div className="flex"><span className="text-sm text-gray-600 w-32">Gender:</span><span className="text-sm text-gray-900">{report.patientGender}</span></div>}</div></div>
            <div><h3 className="text-xs font-semibold text-gray-500 uppercase tracking-wider mb-2">Test Information</h3><div className="space-y-1"><div className="flex"><span className="text-sm text-gray-600 w-32">Order Date:</span><span className="text-sm text-gray-900">{new Date(report.orderDate).toLocaleDateString()}</span></div>{report.collectedAt && <div className="flex"><span className="text-sm text-gray-600 w-32">Collected:</span><span className="text-sm text-gray-900">{new Date(report.collectedAt).toLocaleString()}</span></div>}{report.verifiedAt && <div className="flex"><span className="text-sm text-gray-600 w-32">Verified:</span><span className="text-sm text-gray-900">{new Date(report.verifiedAt).toLocaleString()}</span></div>}</div></div>
          </div>
        </div>
        <div className="p-8"><h3 className="text-lg font-semibold text-gray-900 mb-4">Test Results</h3>{completedDetails.length === 0 ? (<div className="text-center py-8 text-gray-500">No completed tests available</div>) : (<Table.Root size="sm" variant="line"><Table.Header><Table.Row><Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Test Name</Table.ColumnHeader><Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Result</Table.ColumnHeader><Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Reference Range</Table.ColumnHeader><Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Status</Table.ColumnHeader></Table.Row></Table.Header><Table.Body>{completedDetails.map((detail) => (<Table.Row key={detail.detailId}><Table.Cell><div className="text-sm font-medium text-gray-900">{detail.testName}</div><div className="text-xs text-gray-500 font-mono">{detail.testCode}</div></Table.Cell><Table.Cell><div className={`text-lg font-mono font-bold ${detail.isCritical ? 'text-red-600' : detail.resultValue && isAbnormal(detail) ? 'text-orange-600' : 'text-gray-900'}`}>{detail.resultValue || 'N/A'}{detail.resultUnit && <span className="text-sm font-normal text-gray-600 ml-1">{detail.resultUnit}</span>}</div></Table.Cell><Table.Cell><span className="text-sm font-mono text-gray-600">{detail.referenceRange || 'N/A'}</span></Table.Cell><Table.Cell>{detail.isCritical ? (<span className="inline-flex items-center px-2 py-1 rounded text-xs font-bold bg-red-100 text-red-800">⚠️ CRITICAL</span>) : detail.resultValue && isAbnormal(detail) ? (<span className="inline-flex items-center px-2 py-1 rounded text-xs font-medium bg-orange-100 text-orange-800">Abnormal</span>) : (<span className="inline-flex items-center px-2 py-1 rounded text-xs font-medium bg-green-100 text-green-800">Normal</span>)}</Table.Cell></Table.Row>))}</Table.Body></Table.Root>)}{completedDetails.some(d => d.remarks) && (<div className="mt-6 pt-6 border-t border-gray-300"><h4 className="text-sm font-semibold text-gray-900 mb-2">Remarks</h4><div className="space-y-2">{completedDetails.filter(d => d.remarks).map(detail => (<div key={detail.detailId} className="text-sm"><span className="font-medium text-gray-700">{detail.testName}:</span><span className="text-gray-600 ml-2">{detail.remarks}</span></div>))}</div></div>)}</div>
        <div className="border-t-2 border-gray-900 p-8 bg-gray-50"><div className="grid grid-cols-3 gap-8"><div><div className="border-b border-gray-400 mb-2 h-12" /><p className="text-xs text-gray-600 text-center">Lab Technician</p></div><div><div className="border-b border-gray-400 mb-2 h-12" /><p className="text-xs text-gray-600 text-center">Pathologist</p></div><div><div className="border-b border-gray-400 mb-2 h-12" /><p className="text-xs text-gray-600 text-center">Authorized Signatory</p></div></div><div className="mt-6 text-center"><p className="text-xs text-gray-500">This is a computer-generated report. No signature is required for validity.</p><p className="text-xs text-gray-400 mt-2">Generated on: {new Date(report.reportGeneratedAt).toLocaleString()}</p></div></div>
      </div>
      <style>{`@media print { .no-print { display: none !important; } body { margin: 0; padding: 0; } section { page-break-inside: avoid; } }`}</style>
    </section>
  );
}

function isAbnormal(detail: LabDetail): boolean {
  if (!detail.resultValue || !detail.referenceRange) return false;
  const value = parseFloat(detail.resultValue);
  if (isNaN(value)) return false;
  const match = detail.referenceRange.match(/([\d.]+)\s*-\s*([\d.]+)/);
  if (!match) return false;
  const low = parseFloat(match[1]);
  const high = parseFloat(match[2]);
  return value < low || value > high;
}