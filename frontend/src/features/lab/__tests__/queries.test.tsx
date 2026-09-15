import { describe, expect, it, vi, beforeEach, afterEach } from 'vitest';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { renderHook, waitFor } from '@testing-library/react';
import { useLabOrders, useLabOrder, useCreateLabOrder } from '../queries';

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
      '/api/api/v1/lab/orders',
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
      '/api/api/v1/lab/orders/1',
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
      '/api/api/v1/lab/orders',
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