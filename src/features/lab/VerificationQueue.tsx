import { useState } from 'react';
import { useLabOrders, useVerifyResult, useFinalizeResult } from './queries';
import type { LabOrder, LabDetail } from './types';

export function VerificationQueue() {
  const { data: orders = [], isLoading } = useLabOrders();
  const verifyResult = useVerifyResult();
  const finalizeResult = useFinalizeResult();
  const [selectedOrder, setSelectedOrder] = useState<LabOrder | null>(null);
  const [verifierId] = useState(1);

  const pendingVerification = orders.filter(order => 
    order.details?.some(d => d.status === 'COMPLETED' && !d.verified)
  );

  const handleVerify = async (orderId: number, detailId: number) => {
    try {
      await verifyResult.mutateAsync({ id: orderId, detailId });
      await finalizeResult.mutateAsync({ id: orderId, detailId });
    } catch (err) { console.error('Verification failed:', err); }
  };

  const handleVerifyAll = async (order: LabOrder) => {
    const unverifiedDetails = order.details?.filter(d => d.status === 'COMPLETED' && !d.verified) ?? [];
    for (const detail of unverifiedDetails) {
      await handleVerify(order.orderId, detail.detailId);
    }
  };

  if (isLoading) return <div className="p-4 text-center text-gray-500">Loading verification queue...</div>;

  return (
    <section className="space-y-4">
      <header className="flex items-center justify-between">
        <div><h2 className="text-xl font-semibold text-gray-900">Verification Queue</h2><p className="text-sm text-gray-500">{pendingVerification.length} order{pendingVerification.length !== 1 ? 's' : ''} pending verification</p></div>
        <div className="flex items-center gap-2"><span className="text-sm text-gray-600">Verifier:</span><span className="px-3 py-1 bg-blue-100 text-blue-800 rounded-full text-sm font-medium">User #{verifierId}</span></div>
      </header>
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-4">
        <div className="lg:col-span-1 border border-gray-200 rounded-lg overflow-hidden">
          <div className="bg-gray-50 px-4 py-3 border-b border-gray-200"><h3 className="text-sm font-medium text-gray-900">Orders</h3></div>
          <div className="divide-y divide-gray-200 max-h-[600px] overflow-y-auto">
            {pendingVerification.length === 0 ? (<div className="p-4 text-center text-gray-500 text-sm">No orders pending verification</div>) : pendingVerification.map((order) => {
              const unverifiedCount = order.details?.filter(d => d.status === 'COMPLETED' && !d.verified).length ?? 0;
              return (<div key={order.orderId} onClick={() => setSelectedOrder(order)} className={`p-4 cursor-pointer transition-colors ${selectedOrder?.orderId === order.orderId ? 'bg-blue-50 border-l-4 border-l-blue-600' : 'hover:bg-gray-50'}`}><div className="flex items-center justify-between mb-2"><span className="font-mono text-sm font-medium text-gray-900">#{order.orderId}</span><span className="px-2 py-0.5 bg-yellow-100 text-yellow-800 text-xs rounded-full">{unverifiedCount} pending</span></div><div className="text-xs text-gray-600">Patient #{order.patientId}</div></div>);
            })}
          </div>
        </div>
        <div className="lg:col-span-2 border border-gray-200 rounded-lg overflow-hidden">
          {selectedOrder ? (<>
            <div className="bg-gray-50 px-4 py-3 border-b border-gray-200 flex items-center justify-between"><div><h3 className="text-sm font-medium text-gray-900">Order #{selectedOrder.orderId}</h3><p className="text-xs text-gray-500">Patient #{selectedOrder.patientId}</p></div><button onClick={() => handleVerifyAll(selectedOrder)} disabled={verifyResult.isPending || finalizeResult.isPending} className="px-4 py-2 bg-green-600 text-white text-sm rounded-md hover:bg-green-700 disabled:opacity-50 transition-colors">{verifyResult.isPending || finalizeResult.isPending ? 'Verifying...' : 'Verify All'}</button></div>
            <div className="divide-y divide-gray-200 max-h-[600px] overflow-y-auto">
              {selectedOrder.details?.filter(d => d.status === 'COMPLETED' && !d.verified).map((detail) => (
                <div key={detail.detailId} className="p-4 hover:bg-gray-50">
                  <div className="flex items-start justify-between mb-3"><div><div className="text-sm font-medium text-gray-900">{detail.testName}</div><div className="text-xs text-gray-500 font-mono">{detail.testCode}</div></div><button onClick={() => handleVerify(selectedOrder.orderId, detail.detailId)} disabled={verifyResult.isPending} className="px-3 py-1 bg-blue-600 text-white text-xs rounded hover:bg-blue-700 disabled:opacity-50 transition-colors">Verify</button></div>
                  <div className="grid grid-cols-2 gap-4 text-sm"><div><label className="text-xs text-gray-500 uppercase tracking-wider">Result</label><div className={`font-mono text-lg mt-1 ${detail.isCritical ? 'text-red-600 font-bold' : 'text-gray-900'}`}>{detail.resultValue || 'N/A'}{detail.resultUnit && <span className="text-sm text-gray-500 ml-1">{detail.resultUnit}</span>}</div></div><div><label className="text-xs text-gray-500 uppercase tracking-wider">Reference Range</label><div className="font-mono text-lg text-gray-600 mt-1">{detail.referenceRange || 'N/A'}</div></div></div>
                  {detail.isCritical && <div className="mt-3 p-2 bg-red-50 border border-red-200 rounded"><p className="text-xs text-red-800 font-semibold">⚠️ CRITICAL VALUE - Immediate notification required</p></div>}
                  {detail.remarks && <div className="mt-3"><label className="text-xs text-gray-500 uppercase tracking-wider">Remarks</label><p className="text-sm text-gray-700 mt-1">{detail.remarks}</p></div>}
                </div>
              ))}
            </div>
          </>) : (<div className="p-8 text-center text-gray-500">Select an order to view verification details</div>)}
        </div>
      </div>
    </section>
  );
}