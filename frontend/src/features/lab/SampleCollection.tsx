import { useState } from 'react';
import { Button, Checkbox, Table, Textarea } from '@chakra-ui/react';
import { FormField, TextInput } from "../../components/ui";
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
          <Button
            variant="secondary"
            size="sm"
            onClick={() => setScanningMode(!scanningMode)}
          >
            {scanningMode ? '🔴 Scanning Mode' : '📷 Enable Scanner'}
          </Button>
          <Button
            variant="primary"
            size="sm"
            onClick={handleAccept}
            loading={acceptSample.isPending}
            loadingText="Processing..."
            disabled={selectedSamples.size === 0 || acceptSample.isPending}
          >
            {acceptSample.isPending ? 'Processing...' : `Accept ${selectedSamples.size} Sample${selectedSamples.size !== 1 ? 's' : ''}`}
          </Button>
        </div>
      </header>

      {scanningMode && (
        <div className="p-4 bg-green-50 border border-green-200 rounded-lg">
          <div className="flex items-center gap-3">
            <span className="text-sm font-medium text-green-900">Scanner Active</span>
            <TextInput
              type="text"
              placeholder="Scan barcode or enter manually..."
              autoFocus
              onKeyDown={(e) => {
                if (e.key === 'Enter' && e.currentTarget.value) {
                  simulateScan(e.currentTarget.value);
                  e.currentTarget.value = '';
                }
              }}
            />
            <Button
              variant="secondary"
              size="sm"
              onClick={() => setScanningMode(false)}
            >
              Disable
            </Button>
          </div>
        </div>
      )}

      {/* Sample List */}
      <div className="border border-gray-200 rounded-lg overflow-hidden">
        <Table.Root size="sm" variant="line">
          <Table.Header>
            <Table.Row>
              <Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">
                <Checkbox.Root
                  checked={selectedSamples.size === pendingDetails.length && pendingDetails.length > 0}
                  onCheckedChange={(e) => {
                    if (e.checked) {
                      pendingDetails.forEach(d => toggleSampleSelection(d.detailId));
                    } else {
                      clearSelection();
                    }
                  }}
                >
                  <Checkbox.Control />
                </Checkbox.Root>
              </Table.ColumnHeader>
              <Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Test</Table.ColumnHeader>
              <Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Barcode</Table.ColumnHeader>
              <Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Status</Table.ColumnHeader>
              <Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Actions</Table.ColumnHeader>
            </Table.Row>
          </Table.Header>
          <Table.Body>
            {pendingDetails.length === 0 ? (
              <Table.Row>
                <Table.Cell colSpan={5}>
                  No samples pending collection
                </Table.Cell>
              </Table.Row>
            ) : (
              pendingDetails.map((detail) => (
                <Table.Row
                  key={detail.detailId}
                >
                  <Table.Cell>
                    <Checkbox.Root
                      checked={selectedSamples.has(detail.detailId)}
                      onCheckedChange={() => toggleSampleSelection(detail.detailId)}
                    >
                      <Checkbox.Control />
                    </Checkbox.Root>
                  </Table.Cell>
                  <Table.Cell>
                    <div className="text-sm font-medium text-gray-900">{detail.testName}</div>
                    <div className="text-xs text-gray-500 font-mono">{detail.testCode}</div>
                  </Table.Cell>
                  <Table.Cell>
                    {detail.barcode ? (
                      <span className="font-mono text-sm text-gray-900">{detail.barcode}</span>
                    ) : (
                      <BarcodeGenerator
                        orderId={orderId}
                        detailId={detail.detailId}
                        testName={detail.testName}
                      />
                    )}
                  </Table.Cell>
                  <Table.Cell>
                    <span className={`inline-flex items-center px-2 py-0.5 rounded text-xs font-medium ${
                      detail.status === 'COLLECTED'
                        ? 'bg-green-100 text-green-800'
                        : 'bg-yellow-100 text-yellow-800'
                    }`}>
                      {detail.status}
                    </span>
                  </Table.Cell>
                  <Table.Cell>
                    <Button
                      variant="secondary"
                      size="sm"
                      onClick={() => {
                        setActiveDetail(detail);
                        setShowRejectModal(true);
                      }}
                    >
                      Reject
                    </Button>
                  </Table.Cell>
                </Table.Row>
              ))
            )}
          </Table.Body>
        </Table.Root>
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
                <FormField label="Rejection Reason" required>
                  <Textarea
                    bg="white"
                    borderColor="gray.300"
                    value={rejectionReason}
                    onChange={(e) => setRejectionReason(e.target.value)}
                    rows={3}
                    placeholder="Enter reason for rejection..."
                  />
                </FormField>
              </div>
              <div className="flex gap-2">
                <Button
                  variant="primary"
                  size="sm"
                  onClick={handleReject}
                  loading={rejectSample.isPending}
                  loadingText="Rejecting..."
                  disabled={!rejectionReason || rejectSample.isPending}
                >
                  {rejectSample.isPending ? 'Rejecting...' : 'Confirm Rejection'}
                </Button>
                <Button
                  variant="secondary"
                  size="sm"
                  onClick={() => {
                    setShowRejectModal(false);
                    setRejectionReason('');
                    setActiveDetail(null);
                  }}
                >
                  Cancel
                </Button>
              </div>
            </div>
          </div>
        </div>
      )}
    </section>
  );
}