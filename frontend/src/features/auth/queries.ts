import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import type { User, Role, Permission, NotificationTemplate, AuthTokens } from './types';

const API = '/api/v1';

async function api<T>(url: string, opts?: RequestInit): Promise<T> {
  const res = await fetch(url, { headers: { 'Content-Type': 'application/json' }, ...opts });
  if (!res.ok) throw new Error(`${res.status}: ${await res.text()}`);
  return res.json();
}

export function useLogin() {
  return useMutation({
    mutationFn: (data: { username: string; password: string }) =>
      api<AuthTokens>(`${API}/auth/login`, { method: 'POST', body: JSON.stringify(data) }),
  });
}

export function useGenerateOtp() {
  return useMutation({
    mutationFn: (data: { mobileNo: string }) =>
      api<{ devOtp?: string }>(`${API}/auth/otp/generate`, { method: 'POST', body: JSON.stringify(data) }),
  });
}

export function useVerifyOtp() {
  return useMutation({
    mutationFn: (data: { mobileNo: string; otp: string }) =>
      api<AuthTokens>(`${API}/auth/otp/verify`, { method: 'POST', body: JSON.stringify(data) }),
  });
}

export function useUsers() {
  return useQuery<User[]>({ queryKey: ['users'], queryFn: () => api(`${API}/users`) });
}

export function useCreateUser() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (payload: Record<string, unknown>) =>
      api<User>(`${API}/users`, { method: 'POST', body: JSON.stringify(payload) }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['users'] }),
  });
}

export function useRoles() {
  return useQuery<Role[]>({ queryKey: ['roles'], queryFn: () => api(`${API}/roles`) });
}

export function useCreateRole() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (payload: Record<string, unknown>) =>
      api<Role>(`${API}/roles`, { method: 'POST', body: JSON.stringify(payload) }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['roles'] }),
  });
}

export function usePermissions(roleId?: number) {
  return useQuery<Permission[]>({
    queryKey: ['permissions', roleId ?? 0],
    queryFn: () => (roleId ? api(`${API}/roles/${roleId}/permissions`) : api(`${API}/permissions`)),
  });
}

export function useCreatePermission() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (payload: Record<string, unknown>) =>
      api<Permission>(`${API}/permissions`, { method: 'POST', body: JSON.stringify(payload) }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['permissions'] }),
  });
}

export function useUpdatePermissions() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (data: { roleId: number; modulePermissions: { moduleId: number; permissions: string[] }[] }) =>
      api(`${API}/roles/${data.roleId}/permissions`, { method: 'PUT', body: JSON.stringify(data.modulePermissions) }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['permissions'] }),
  });
}

export function useNotificationTemplates() {
  return useQuery<NotificationTemplate[]>({ queryKey: ['notification-templates'], queryFn: () => api(`${API}/notification-templates`) });
}

export function useCreateTemplate() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (data: Partial<NotificationTemplate>) =>
      api<NotificationTemplate>(`${API}/notification-templates`, { method: 'POST', body: JSON.stringify(data) }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['notification-templates'] }),
  });
}

export function useCreateNotificationTemplate() {
  return useCreateTemplate();
}

export function useUpdateTemplate() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (data: NotificationTemplate) =>
      api<NotificationTemplate>(`${API}/notification-templates/${data.templateId}`, { method: 'PUT', body: JSON.stringify(data) }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['notification-templates'] }),
  });
}
