import { describe, expect, it, vi, beforeEach, afterEach } from 'vitest';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { renderHook, waitFor } from '@testing-library/react';
import {
  useRadOrders,
  useRadOrder,
  useCreateRadOrder,
  useReportRadOrder,
  useRadTemplates,
  useCreateRadTemplate,
  useRescheduleLine,
  useRequestRetest,
  useRadOrderAction,
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

describe('useRadOrders', () => {
  it('fetches rad orders from API', async () => {
    fetchMock.mockResolvedValueOnce({
      ok: true,
      status: 200,
      json: async () => [
        {
          radOrderId: 1,
          patientId: 100,
          modality: 'CT',
          priority: 1,
          status: 1,
          imageCaptured: false,
        },
      ],
    });

    const { result } = renderHook(() => useRadOrders(), { wrapper: wrapper() });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));

    expect(fetchMock).toHaveBeenCalledWith(
      '/api/api/v1/rad-orders',
      expect.any(Object)
    );
    expect(result.current.data).toHaveLength(1);
    expect(result.current.data?.[0].radOrderId).toBe(1);
    expect(result.current.data?.[0].modality).toBe('CT');
  });

  it('applies filters as query params', async () => {
    fetchMock.mockResolvedValueOnce({
      ok: true,
      status: 200,
      json: async () => [],
    });

    const { result } = renderHook(
      () => useRadOrders({ status: '1', patientId: 100, modality: 'CT' }),
      { wrapper: wrapper() }
    );

    await waitFor(() => expect(result.current.isSuccess).toBe(true));

    expect(fetchMock).toHaveBeenCalledWith(
      '/api/api/v1/rad-orders?status=1&patientId=100&modality=CT',
      expect.any(Object)
    );
  });
});

describe('useRadOrder', () => {
  it('fetches single order with details', async () => {
    fetchMock.mockResolvedValueOnce({
      ok: true,
      status: 200,
      json: async () => ({
        radOrderId: 1,
        patientId: 100,
        modality: 'CT',
        priority: 1,
        status: 2,
        imageCaptured: true,
        details: [
          {
            radOrderId: 1,
            lineNo: 1,
            itemId: 500,
            qty: 1,
            retestRequested: false,
          },
        ],
      }),
    });

    const { result } = renderHook(() => useRadOrder(1), { wrapper: wrapper() });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));

    expect(fetchMock).toHaveBeenCalledWith(
      '/api/api/v1/rad-orders/1',
      expect.any(Object)
    );
    expect(result.current.data?.radOrderId).toBe(1);
    expect(result.current.data?.details).toHaveLength(1);
    expect(result.current.data?.details?.[0].itemId).toBe(500);
  });

  it('is disabled when id is 0', () => {
    const { result } = renderHook(() => useRadOrder(0), { wrapper: wrapper() });

    expect(fetchMock).not.toHaveBeenCalled();
    expect(result.current.fetchStatus).toBe('idle');
  });
});

describe('useCreateRadOrder', () => {
  it('creates new rad order', async () => {
    fetchMock.mockResolvedValueOnce({
      ok: true,
      status: 201,
      json: async () => ({
        radOrderId: 100,
        patientId: 100,
        modality: 'CT',
        priority: 1,
        status: 1,
        imageCaptured: false,
        details: [{ radOrderId: 100, lineNo: 1, itemId: 500, qty: 1, retestRequested: false }],
      }),
    });

    const { result } = renderHook(() => useCreateRadOrder(), { wrapper: wrapper() });

    result.current.mutate({
      patientId: 100,
      encounterId: 200,
      modality: 'CT',
      priority: 'ROUTINE',
      lines: [{ itemId: 500, qty: 1 }],
    });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));

    expect(fetchMock).toHaveBeenCalledWith(
      '/api/api/v1/rad-orders',
      expect.objectContaining({
        method: 'POST',
        body: JSON.stringify({
          patientId: 100,
          encounterId: 200,
          modality: 'CT',
          priority: 'ROUTINE',
          lines: [{ itemId: 500, qty: 1 }],
        }),
      })
    );
    expect(result.current.data?.radOrderId).toBe(100);
  });
});

describe('useReportRadOrder', () => {
  it('submits report', async () => {
    fetchMock.mockResolvedValueOnce({
      ok: true,
      status: 200,
      json: async () => ({
        radOrderId: 1,
        patientId: 100,
        modality: 'CT',
        status: 3,
        reportText: 'Normal study',
        reportedBy: 'Dr. Smith',
        imageCaptured: true,
      }),
    });

    const { result } = renderHook(() => useReportRadOrder(), { wrapper: wrapper() });

    result.current.mutate({
      id: 1,
      reportText: 'Normal study',
      reportedBy: 'Dr. Smith',
    });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));

    expect(fetchMock).toHaveBeenCalledWith(
      '/api/api/v1/rad-orders/1/report',
      expect.objectContaining({
        method: 'POST',
        body: JSON.stringify({ reportText: 'Normal study', reportedBy: 'Dr. Smith' }),
      })
    );
    expect(result.current.data?.status).toBe(3);
  });
});

describe('useRadOrderAction', () => {
  it('starts an order', async () => {
    fetchMock.mockResolvedValueOnce({
      ok: true,
      status: 200,
      json: async () => ({
        radOrderId: 1,
        patientId: 100,
        modality: 'CT',
        status: 2,
        imageCaptured: false,
      }),
    });

    const { result } = renderHook(() => useRadOrderAction('start'), { wrapper: wrapper() });

    result.current.mutate(1);

    await waitFor(() => expect(result.current.isSuccess).toBe(true));

    expect(fetchMock).toHaveBeenCalledWith(
      '/api/api/v1/rad-orders/1/start',
      expect.objectContaining({ method: 'POST' })
    );
    expect(result.current.data?.status).toBe(2);
  });

  it('captures image', async () => {
    fetchMock.mockResolvedValueOnce({
      ok: true,
      status: 200,
      json: async () => ({
        radOrderId: 1,
        patientId: 100,
        modality: 'CT',
        status: 2,
        imageCaptured: true,
      }),
    });

    const { result } = renderHook(() => useRadOrderAction('capture-image'), { wrapper: wrapper() });

    result.current.mutate(1);

    await waitFor(() => expect(result.current.isSuccess).toBe(true));

    expect(fetchMock).toHaveBeenCalledWith(
      '/api/api/v1/rad-orders/1/capture-image',
      expect.objectContaining({ method: 'POST' })
    );
    expect(result.current.data?.imageCaptured).toBe(true);
  });

  it('cancels an order', async () => {
    fetchMock.mockResolvedValueOnce({
      ok: true,
      status: 200,
      json: async () => ({
        radOrderId: 1,
        patientId: 100,
        modality: 'CT',
        status: 4,
        imageCaptured: false,
      }),
    });

    const { result } = renderHook(() => useRadOrderAction('cancel'), { wrapper: wrapper() });

    result.current.mutate(1);

    await waitFor(() => expect(result.current.isSuccess).toBe(true));

    expect(fetchMock).toHaveBeenCalledWith(
      '/api/api/v1/rad-orders/1/cancel',
      expect.objectContaining({ method: 'POST' })
    );
    expect(result.current.data?.status).toBe(4);
  });
});

describe('useRadTemplates', () => {
  it('fetches templates', async () => {
    fetchMock.mockResolvedValueOnce({
      ok: true,
      status: 200,
      json: async () => [
        {
          id: 1,
          templateCode: 'CT-HEAD',
          modality: 'CT',
          bodyTemplate: 'CT Head findings...',
          description: 'Standard CT head',
        },
      ],
    });

    const { result } = renderHook(() => useRadTemplates(), { wrapper: wrapper() });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));

    expect(fetchMock).toHaveBeenCalledWith(
      '/api/api/v1/radiology/templates',
      expect.any(Object)
    );
    expect(result.current.data).toHaveLength(1);
    expect(result.current.data?.[0].templateCode).toBe('CT-HEAD');
  });

  it('applies modality filter', async () => {
    fetchMock.mockResolvedValueOnce({
      ok: true,
      status: 200,
      json: async () => [],
    });

    const { result } = renderHook(() => useRadTemplates('CT'), { wrapper: wrapper() });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));

    expect(fetchMock).toHaveBeenCalledWith(
      '/api/api/v1/radiology/templates?modality=CT',
      expect.any(Object)
    );
  });
});

describe('useCreateRadTemplate', () => {
  it('creates new template', async () => {
    fetchMock.mockResolvedValueOnce({
      ok: true,
      status: 201,
      json: async () => ({
        id: 5,
        templateCode: 'MRI-SPINE',
        modality: 'MRI',
        bodyTemplate: 'MRI Spine...',
        description: '',
      }),
    });

    const { result } = renderHook(() => useCreateRadTemplate(), { wrapper: wrapper() });

    result.current.mutate({
      templateCode: 'MRI-SPINE',
      modality: 'MRI',
      bodyTemplate: 'MRI Spine...',
    });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));

    expect(fetchMock).toHaveBeenCalledWith(
      '/api/api/v1/radiology/templates',
      expect.objectContaining({
        method: 'POST',
        body: JSON.stringify({
          templateCode: 'MRI-SPINE',
          modality: 'MRI',
          bodyTemplate: 'MRI Spine...',
        }),
      })
    );
    expect(result.current.data?.id).toBe(5);
  });
});

describe('useRescheduleLine', () => {
  it('reschedules a line', async () => {
    fetchMock.mockResolvedValueOnce({
      ok: true,
      status: 200,
      json: async () => ({
        radOrderId: 1,
        lineNo: 1,
        itemId: 500,
        qty: 1,
        retestRequested: false,
        rescheduledTo: '2024-03-15T10:00:00',
      }),
    });

    const { result } = renderHook(() => useRescheduleLine(), { wrapper: wrapper() });

    result.current.mutate({
      orderId: 1,
      lineNo: 1,
      rescheduledTo: '2024-03-15T10:00:00',
    });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));

    expect(fetchMock).toHaveBeenCalledWith(
      '/api/api/v1/radiology/orders/1/lines/1/reschedule',
      expect.objectContaining({
        method: 'POST',
        body: JSON.stringify({ rescheduledTo: '2024-03-15T10:00:00' }),
      })
    );
    expect(result.current.data?.rescheduledTo).toBe('2024-03-15T10:00:00');
  });
});

describe('useRequestRetest', () => {
  it('requests retest for a line', async () => {
    fetchMock.mockResolvedValueOnce({
      ok: true,
      status: 200,
      json: async () => ({
        radOrderId: 1,
        lineNo: 1,
        itemId: 500,
        qty: 1,
        retestRequested: true,
        retestReason: 'Poor image quality',
      }),
    });

    const { result } = renderHook(() => useRequestRetest(), { wrapper: wrapper() });

    result.current.mutate({
      orderId: 1,
      lineNo: 1,
      reason: 'Poor image quality',
    });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));

    expect(fetchMock).toHaveBeenCalledWith(
      '/api/api/v1/radiology/orders/1/lines/1/retest',
      expect.objectContaining({
        method: 'POST',
        body: JSON.stringify({ reason: 'Poor image quality' }),
      })
    );
    expect(result.current.data?.retestRequested).toBe(true);
    expect(result.current.data?.retestReason).toBe('Poor image quality');
  });
});
