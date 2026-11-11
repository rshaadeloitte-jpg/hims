import { describe, expect, it, vi, beforeEach, afterEach } from 'vitest';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { renderHook, waitFor } from '@testing-library/react';
import {
  useLabOrders,
  useLabOrder,
  useCreateLabOrder,
  getLabOrderStatusLabel,
  LAB_ORDER_STATUS_MAP,
} from '../queries';

function wrapper() {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return ({ children }: { children: React.ReactNode }) => (
    <QueryClientProvider client={qc}>{children}</QueryClientProvider>
  );
}

const fetchMock = vi.fn();

beforeEach(() => {
  fetchMock.mockReset();
  vi.stubGlobal('fetch', fetchMock);
});

afterEach(() => {
  vi.unstubAllGlobals();
});

describe('useLabOrders', () => {
  it('fetches lab orders from API', async () => {
    fetchMock.mockResolvedValueOnce({
      ok: true,
      status: 200,
      json: async () => [
        {
          orderId: 1,
          patientId: 100,
          status: 'ORDERED',
          isOutSourced: false,
          orderDate: '2024-01-01T00:00:00Z',
        },
      ],
    });

    const { result } = renderHook(() => useLabOrders(), { wrapper: wrapper() });
    
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    
    expect(fetchMock).toHaveBeenCalledWith(
      '/api/v1/lab/orders',
      expect.any(Object)
    );
    expect(result.current.data).toHaveLength(1);
    expect(result.current.data?.[0].orderId).toBe(1);
  });
});

describe('useLabOrder', () => {
  it('fetches single order with details', async () => {
    fetchMock.mockResolvedValueOnce({
      ok: true,
      status: 200,
      json: async () => ({
        orderId: 1,
        patientId: 100,
        status: 'ORDERED',
        isOutSourced: false,
        orderDate: '2024-01-01T00:00:00Z',
        details: [
          {
            detailId: 1,
            testName: 'Glucose',
            status: 'PENDING',
          },
        ],
      }),
    });

    const { result } = renderHook(() => useLabOrder(1), { wrapper: wrapper() });
    
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    
    expect(fetchMock).toHaveBeenCalledWith(
      '/api/v1/lab/orders/1',
      expect.any(Object)
    );
    expect(result.current.data?.orderId).toBe(1);
    expect(result.current.data?.details).toHaveLength(1);
  });

  it('is disabled when id is 0', () => {
    const { result } = renderHook(() => useLabOrder(0), { wrapper: wrapper() });
    
    expect(fetchMock).not.toHaveBeenCalled();
    expect(result.current.fetchStatus).toBe('idle');
  });
});

describe('useCreateLabOrder', () => {
  it('creates new lab order', async () => {
    fetchMock.mockResolvedValueOnce({
      ok: true,
      status: 201,
      json: async () => ({
        orderId: 100,
        patientId: 100,
        status: 'ORDERED',
        isOutSourced: false,
        orderDate: '2024-01-01T00:00:00Z',
      }),
    });

    const { result } = renderHook(() => useCreateLabOrder(), { wrapper: wrapper() });
    
    result.current.mutate({
      patientId: 100,
      tests: [{ testId: 1, testName: 'Glucose' }],
    });
    
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    
    expect(fetchMock).toHaveBeenCalledWith(
      '/api/v1/lab/orders',
      expect.objectContaining({
        method: 'POST',
        body: JSON.stringify({
          patientId: 100,
          tests: [{ testId: 1, testName: 'Glucose' }],
        }),
      })
    );
    expect(result.current.data?.orderId).toBe(100);
  });
});

describe('useLabOrders with PathOrderBooking shape', () => {
  it('fetches PathOrderBooking rows with numeric status and bookingId', async () => {
    fetchMock.mockResolvedValueOnce({
      ok: true,
      status: 200,
      json: async () => [
        {
          bookingId: 10,
          bookingNo: 'LAB-10',
          patientId: 505,
          encounterId: 7777,
          bookingStatus: 1,
          isOutSourced: false,
          addedDatetime: '2024-01-01T10:30:00Z',
          finalizedAt: null,
          isCritical: false,
          details: [
            {
              detailId: 21,
              bookingId: 10,
              testCode: 'CBC',
              testName: 'Complete Blood Count',
              detailStatus: 1,
            },
          ],
        },
      ],
    });

    const { result } = renderHook(() => useLabOrders(), { wrapper: wrapper() });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));

    expect(result.current.data).toHaveLength(1);
    const item = result.current.data?.[0];
    expect(item?.bookingId).toBe(10);
    expect(item?.bookingNo).toBe('LAB-10');
    expect(item?.patientId).toBe(505);
    expect(item?.bookingStatus).toBe(1);
    expect(item?.addedDatetime).toBe('2024-01-01T10:30:00Z');
  });
});

describe('getLabOrderStatusLabel', () => {
  it('maps numeric status correctly', () => {
    expect(LAB_ORDER_STATUS_MAP[1]).toBe('ORDERED');
    expect(getLabOrderStatusLabel(1)).toBe('ORDERED');
    expect(getLabOrderStatusLabel(2)).toBe('SAMPLE_COLLECTED');
    expect(getLabOrderStatusLabel(3)).toBe('IN_PROGRESS');
    expect(getLabOrderStatusLabel(4)).toBe('FINALIZED');
    expect(getLabOrderStatusLabel(5)).toBe('CANCELLED');
  });

  it('handles numeric strings', () => {
    expect(getLabOrderStatusLabel('1')).toBe('ORDERED');
    expect(getLabOrderStatusLabel('4')).toBe('FINALIZED');
  });

  it('preserves valid string status labels', () => {
    expect(getLabOrderStatusLabel('ORDERED')).toBe('ORDERED');
    expect(getLabOrderStatusLabel('SAMPLE_COLLECTED')).toBe('SAMPLE_COLLECTED');
  });

  it('handles null, undefined, and boolean values safely', () => {
    expect(getLabOrderStatusLabel(null)).toBe('—');
    expect(getLabOrderStatusLabel(undefined)).toBe('—');
    expect(getLabOrderStatusLabel(true)).toBe('—');
    expect(getLabOrderStatusLabel(false)).toBe('—');
  });
});
