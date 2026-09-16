import { useState } from 'react';
import { Button } from '@chakra-ui/react';
import { useLabOrders, useNotifyCritical } from './queries';
import type { LabDetail, CriticalAlert } from './types';

export function CriticalAlerts() {
  const { data: orders = [], isLoading } = useLabOrders();
  const notifyCritical = useNotifyCritical();
  const [notifiedAlerts, setNotifiedAlerts] = useState<Set<number>>(new Set());

  const criticalAlerts: CriticalAlert[] = orders.flatMap(order =>
    (order.details ?? []).filter(d => d.isCritical && d.resultValue).map(detail => ({
      detailId: detail.detailId,
      orderId: order.orderId,
      patientId: order.patientId,
      patientName: order.patientName,
      testName: detail.testName,
      resultValue: detail.resultValue || '',
      referenceRange: detail.referenceRange || '',
      criticalLevel: determineCriticalLevel(detail),
      notifiedAt: detail.verifiedAt,
    }))
  );

  const handleNotify = async (orderId: number, detailId: number) => {
    try {
      await notifyCritical.mutateAsync({ id: orderId, detailId });
      setNotifiedAlerts(prev => new Set(prev).add(detailId));
    } catch (err) { console.error('Failed to notify critical value:', err); }
  };

  if (isLoading) return <div className="p-4 text-center text-gray-500">Loading critical alerts...</div>;

  return (
    <section className="space-y-4">
      <header className="flex items-center justify-between">
        <div><h2 className="text-xl font-semibold text-gray-900 flex items-center gap-2"><span className="text-2xl">⚠️</span>Critical Alerts</h2><p className="text-sm text-gray-500">{criticalAlerts.length} critical value{criticalAlerts.length !== 1 ? 's' : ''} requiring notification</p></div>
        <div className="flex items-center gap-2"><span className="px-3 py-1 bg-red-100 text-red-800 rounded-full text-sm font-medium">{criticalAlerts.filter(a => !notifiedAlerts.has(a.detailId)).length} Unnotified</span></div>
      </header>
      {criticalAlerts.length === 0 ? (<div className="p-8 border border-gray-200 rounded-lg bg-green-50 text-center"><p className="text-green-800 font-medium">No critical alerts</p><p className="text-sm text-green-600 mt-1">All results are within normal ranges</p></div>) : (
        <div className="space-y-3">
          {criticalAlerts.map((alert) => {
            const isNotified = notifiedAlerts.has(alert.detailId) || alert.notifiedAt;
            return (
              <div key={alert.detailId} className={`border-2 rounded-lg p-4 ${alert.criticalLevel === 'HIGH' ? 'border-red-500 bg-red-50' : 'border-orange-500 bg-orange-50'}`}>
                <div className="flex items-start justify-between mb-3">
                  <div className="flex items-center gap-3"><div className={`w-12 h-12 rounded-full flex items-center justify-center text-2xl ${alert.criticalLevel === 'HIGH' ? 'bg-red-200' : 'bg-orange-200'}`}>{alert.criticalLevel === 'HIGH' ? '🔺' : '🔻'}</div><div><h3 className="text-lg font-semibold text-gray-900">{alert.testName}</h3><p className="text-sm text-gray-600">Patient #{alert.patientId} {alert.patientName && `(${alert.patientName})`}</p></div></div>
                  <div className="text-right"><div className={`text-2xl font-bold font-mono ${alert.criticalLevel === 'HIGH' ? 'text-red-700' : 'text-orange-700'}`}>{alert.resultValue}</div><div className="text-xs text-gray-500 font-mono">Range: {alert.referenceRange}</div></div>
                </div>
                <div className="flex items-center justify-between pt-3 border-t border-gray-200">
                  <div className="text-sm text-gray-600"><span className="font-medium">Order:</span> #{alert.orderId}</div>
                  {isNotified ? (<div className="flex items-center gap-2"><span className="text-green-700 text-sm font-medium">✓ Notified</span>{alert.notifiedAt && <span className="text-xs text-gray-500">{new Date(alert.notifiedAt).toLocaleString()}</span>}</div>) : (<Button variant="primary" size="sm" onClick={() => handleNotify(alert.orderId, alert.detailId)} loading={notifyCritical.isPending} loadingText="Notifying...">{notifyCritical.isPending ? 'Notifying...' : 'Notify Doctor'}</Button>)}
                </div>
                <div className="mt-3 p-2 bg-white rounded border border-gray-200"><p className="text-xs text-gray-700"><strong>Action Required:</strong> Immediately contact the attending physician to report this critical value and document the notification.</p></div>
              </div>
            );
          })}
        </div>
      )}
    </section>
  );
}

function determineCriticalLevel(detail: LabDetail): 'HIGH' | 'LOW' {
  const value = parseFloat(detail.resultValue || '0');
  const range = detail.referenceRange || '';
  const match = range.match(/([\d.]+)\s*-\s*([\d.]+)/);
  if (match) {
    const low = parseFloat(match[1]);
    const high = parseFloat(match[2]);
    if (value > high) return 'HIGH';
    if (value < low) return 'LOW';
  }
  return 'HIGH';
}