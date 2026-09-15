export interface User {
  userId: number;
  username: string;
  fullName: string;
  email?: string;
  mobileNo?: string;
  storeId?: number;
  wardId?: number;
  departmentId?: number;
  isActive: boolean;
}

export interface Role {
  roleId: number;
  roleName: string;
  description?: string;
  isActive: boolean;
}

export interface RolePermission {
  roleId: number;
  moduleId: number;
  moduleName: string;
  permissions: string[];
}

export interface Permission {
  permissionId: number;
  permissionName: string;
  module: string;
  description?: string;
}

export interface NotificationTemplate {
  templateId: number;
  templateCode: string;
  channel: 'SMS' | 'EMAIL' | 'WHATSAPP';
  subject?: string;
  bodyTemplate: string;
  isActive: boolean;
}

export interface AuthTokens {
  accessToken: string;
  refreshToken: string;
  expiresIn: number;
}
