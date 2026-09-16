import { useQuery } from '@tanstack/react-query';
import type { LabTest } from './types';

const LAB_BASE: string = import.meta.env.VITE_LAB_API_BASE ?? '/api';

async function labJson<T>(url: string): Promise<T> {
  const res = await fetch(url, {
    headers: { 
      'Content-Type': 'application/json',
      'X-Request-ID': crypto.randomUUID()
    },
  });
  if (!res.ok) {
    const err = await res.json().catch(() => ({ message: res.statusText }));
    throw new Error(err.message ?? 'Request failed');
  }
  return res.json();
}

/**
 * Fetch available lab tests with optional filtering
 */
export function useLabTests(filters?: {
  department?: string;
  isOutsourced?: boolean;
  search?: string;
}) {
  return useQuery<LabTest[]>({
    queryKey: ['lab', 'tests', filters],
    queryFn: () => {
      const params = new URLSearchParams();
      if (filters?.department) params.set('department', filters.department);
      if (filters?.isOutsourced !== undefined) params.set('isOutsourced', String(filters.isOutsourced));
      if (filters?.search) params.set('search', filters.search);
      
      const queryString = params.toString();
      return labJson<LabTest[]>(
        `${LAB_BASE}/api/v1/lab/tests${queryString ? `?${queryString}` : ''}`
      );
    },
    staleTime: 5 * 60 * 1000, // 5 minutes
  });
}

/**
 * Fetch single lab test by ID
 */
export function useLabTest(testId: number) {
  return useQuery<LabTest>({
    queryKey: ['lab', 'tests', testId],
    queryFn: () => labJson<LabTest>(`${LAB_BASE}/api/v1/lab/tests/${testId}`),
    enabled: testId > 0,
  });
}