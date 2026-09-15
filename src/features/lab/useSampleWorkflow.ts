import { useState, useCallback } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { labQueryKeys } from './queries';
import type { Sample, LabDetail } from './types';

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

/**
 * Hook for managing sample collection workflow
 */
export function useSampleWorkflow() {
  const [selectedSamples, setSelectedSamples] = useState<Set<number>>(new Set());
  const [scanningMode, setScanningMode] = useState(false);
  const qc = useQueryClient();

  const generateBarcode = useMutation({
    mutationFn: ({ orderId, detailId }: { orderId: number; detailId: number }) =>
      labJson<LabDetail>(`${LAB_BASE}/api/v1/lab/orders/${orderId}/details/${detailId}/barcode`, {
        method: 'POST',
      }),
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: labQueryKeys.orders() });
    },
  });

  const acceptSample = useMutation({
    mutationFn: ({ orderId, detailId }: { orderId: number; detailId: number }) =>
      labJson<LabDetail>(`${LAB_BASE}/api/v1/lab/orders/${orderId}/details/${detailId}/accept`, {
        method: 'POST',
      }),
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: labQueryKeys.orders() });
    },
  });

  const rejectSample = useMutation({
    mutationFn: ({ orderId, detailId, reason }: { orderId: number; detailId: number; reason: string }) =>
      labJson<LabDetail>(`${LAB_BASE}/api/v1/lab/orders/${orderId}/details/${detailId}/reject`, {
        method: 'POST',
        body: JSON.stringify({ reason }),
      }),
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: labQueryKeys.orders() });
    },
  });

  const toggleSampleSelection = useCallback((detailId: number) => {
    setSelectedSamples(prev => {
      const next = new Set(prev);
      if (next.has(detailId)) {
        next.delete(detailId);
      } else {
        next.add(detailId);
      }
      return next;
    });
  }, []);

  const clearSelection = useCallback(() => {
    setSelectedSamples(new Set());
  }, []);

  return {
    selectedSamples,
    scanningMode,
    setScanningMode,
    toggleSampleSelection,
    clearSelection,
    generateBarcode,
    acceptSample,
    rejectSample,
  };
}

/**
 * Hook for barcode scanning simulation
 */
export function useBarcodeScanner(onScan: (barcode: string) => void) {
  const [isScanning, setIsScanning] = useState(false);

  const startScanning = useCallback(() => {
    setIsScanning(true);
  }, []);

  const stopScanning = useCallback(() => {
    setIsScanning(false);
  }, []);

  const simulateScan = useCallback((barcode: string) => {
    onScan(barcode);
    setIsScanning(false);
  }, [onScan]);

  return {
    isScanning,
    startScanning,
    stopScanning,
    simulateScan,
  };
}