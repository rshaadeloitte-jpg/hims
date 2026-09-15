import { useState } from 'react';
import { useLabOrder } from './queries';
import { useSampleWorkflow, useBarcodeScanner } from './useSampleWorkflow';
import { BarcodeGenerator } from './BarcodeGenerator';
import type { LabDetail } from './types';

interface SampleCollectionProps {
  orderId: number;
  onCollected?: () => void;
}

/**
 * M15 Sample Collection - Accept/reject workflow with barcode scanning
 */
export function SampleCollection({ orderId, onCollected }: SampleCollectionProps) {
  const { data: order, isLoading } = useLabOrder(orderId);
  const {
    selectedSamples,
    scanningMode,
    setScanningMode,
    toggleSampleSelection,
    clearSelection,
    generateBarcode,
    acceptSample,
    rejectSample,
  } = useSampleWorkflow();

  const [rejectionReason, setRejectionReason] = useState('');
  const [showRejectModal, setShowRejectModal] = useState(false);
  const [activeDetail, setActiveDetail] = useState<LabDetail | null>(null);

  const handleBarcodeScan = (barcode: string) => {
    const detail = order?.details?.find(d => d.barcode === barcode);
    if (detail) {
      toggleSampleSelection(detail.detailId);
    }
  };

  const { simulateScan } = useBarcodeScanner(handleBarcodeScan);

  const handleAccept = async () => {
    if (selectedSamples.size === 0) return;

    for (const detailId of selectedSamples) {
      await acceptSample.mutateAsync({ orderId, detailId });
    }
    
    clearSelection();
    onCollected?.();
  };

  const handleReject = async () => {
    if (!activeDetail || !rejectionReason) return;

    await rejectSample.mutateAsync({
      orderId,
      detailId: activeDetail.detailId,
      reason: rejectionReason,
    });

    setShowRejectModal(false);
    setRejectionReason('');
    setActiveDetail(null);
  };

  if (isLoading) {
    return <div className="p-4 text-center text-gray-500">Loading order...</div>;
  }

  if (!order) {
    return <div className="p-4 text-center text-red-600">Order not found</div>;
  }

  const pendingDetails = order.details?.filter(d => 
    d.status === 'PENDING' || d.status === 'COLLECTED'
  ) ?? [];

  return (
    <section className="space-y-4">
      <header className="flex items-center justify-between">
        <div>
          <h2 className="text-xl font-semibold text-gray-900">Sample Collection</h2>
          <p className="text-sm text-gray-500">Order #{orderId}</p>
        </div>
        <div className="flex gap-2">
          <button
            onClick={() => setScanningMode(!scanningMode)}
            className={`px-4 py-2 rounded-md text-sm font-medium transition-colors ${
              scanningMode
                ? 'bg-green-600 text-white hover:bg-green-700'
                : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
            }`}
          >
            {scanningMode ? '🔴 Scanning Mode' : '📷 Enable Scanner'}
          </button>
          <button
            onClick={handleAccept}
            disabled={selectedSamples.size === 0 || acceptSample.isPending}
            className="px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
          >
            {acceptSample.isPending ? 'Processing...' : `Accept ${selectedSamples.size} Sample${selectedSamples.size !== 1 ? 's' : ''}`}
          </button>
        </div>
      </header>

      {scanningMode && (
        <div className="p-4 bg-green-50 border border-green-200 rounded-lg">
          <div className="flex items-center gap-3">
            <span className="text-sm font-medium text-green-900">Scanner Active</span>
            <input
              type="text"
              placeholder="Scan barcode or enter manually..."
              autoFocus
              onKeyDown={(e) => {
                if (e.key === 'Enter' && e.currentTarget.value) {
                  simulateScan(e.currentTarget.value);
                  e.currentTarget.value = '';
                }
              }}
              className="flex-1 px-3 py-2 border border-green-300 rounded-md focus:outline-none focus:ring-2 focus:ring-green-500 font-mono text-sm"
            />
            <button
              onClick={() => setScanningMode(false)}
              className="px-3 py-2 bg-white text-green-700 border border-green-300 rounded-md hover:bg-green-50 text-sm"
            >
              Disable
            </button>
          </div>
        </div>
      )}

      {/* Sample List */}
      <div className="border border-gray-200 rounded-lg overflow-hidden">
        <table className="w-full">
          <thead className="bg-gray-50 border-b border-gray-200">
            <tr>
              <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider w-12">
                <input
                  type="checkbox"
                  checked={selectedSamples.size === pendingDetails.length && pendingDetails.length > 0}
                  onChange={(e) => {
                    if (e.target.checked) {
                      pendingDetails.forEach(d => toggleSampleSelection(d.detailId));
                    } else {
                      clearSelection();
                    }
                  }}
                  className="rounded border-gray-300"
                />
              </th>
              <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Test</th>
              <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Barcode</th>
              <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Status</th>
              <th className="px-4 py-3 text-right text-xs font-medium text-gray-500 uppercase tracking-wider">Actions</th>
            </tr>
          </thead>
          <tbody className="bg-white divide-y divide-gray-200">
            {pendingDetails.length === 0 ? (
              <tr>
                <td colSpan={5} className="px-4 py-8 text-center text-gray-500">
                  No samples pending collection
                </td>
              </tr>
            ) : (
              pendingDetails.map((detail) => (
                <tr
                  key={detail.detailId}
                  className={`transition-colors ${
                    selectedSamples.has(detail.detailId)
                      ? 'bg-blue-50'
                      : 'hover:bg-gray-50'
                  }`}
                >
                  <td className="px-4 py-3">
                    <input
                      type="checkbox"
                      checked={selectedSamples.has(detail.detailId)}
                      onChange={() => toggleSampleSelection(detail.detailId)}
                      className="rounded border-gray-300"
                    />
                  </td>
                  <td className="px-4 py-3">
                    <div className="text-sm font-medium text-gray-900">{detail.testName}</div>
                    <div className="text-xs text-gray-500 font-mono">{detail.testCode}</div>
                  </td>
                  <td className="px-4 py-3">
                    {detail.barcode ? (
                      <span className="font-mono text-sm text-gray-900">{detail.barcode}</span>
                    ) : (
                      <BarcodeGenerator
                        orderId={orderId}
                        detailId={detail.detailId}
                        testName={detail.testName}
                      />
                    )}
                  </td>
                  <td className="px-4 py-3">
                    <span className={`inline-flex items-center px-2 py-0.5 rounded text-xs font-medium ${
                      detail.status === 'COLLECTED'
                        ? 'bg-green-100 text-green-800'
                        : 'bg-yellow-100 text-yellow-800'
                    }`}>
                      {detail.status}
                    </span>
                  </td>
                  <td className="px-4 py-3 text-right">
                    <button
                      onClick={() => {
                        setActiveDetail(detail);
                        setShowRejectModal(true);
                      }}
                      className="px-3 py-1 bg-red-50 text-red-700 text-xs rounded hover:bg-red-100 transition-colors"
                    >
                      Reject
                    </button>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      {/* Rejection Modal */}
      {showRejectModal && activeDetail && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <div className="bg-white rounded-lg p-6 max-w-md w-full mx-4">
            <h3 className="text-lg font-semibold text-gray-900 mb-4">
              Reject Sample: {activeDetail.testName}
            </h3>
            <div className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Rejection Reason *
                </label>
                <textarea
                  value={rejectionReason}
                  onChange={(e) => setRejectionReason(e.target.value)}
                  rows={3}
                  className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-red-500"
                  placeholder="Enter reason for rejection..."
                />
              </div>
              <div className="flex gap-2">
                <button
                  onClick={handleReject}
                  disabled={!rejectionReason || rejectSample.isPending}
                  className="flex-1 px-4 py-2 bg-red-600 text-white rounded-md hover:bg-red-700 disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  {rejectSample.isPending ? 'Rejecting...' : 'Confirm Rejection'}
                </button>
                <button
                  onClick={() => {
                    setShowRejectModal(false);
                    setRejectionReason('');
                    setActiveDetail(null);
                  }}
                  className="px-4 py-2 bg-gray-100 text-gray-700 rounded-md hover:bg-gray-200"
                >
                  Cancel
                </button>
              </div>
            </div>
          </div>
        </div>
      )}
    </section>
  );
}