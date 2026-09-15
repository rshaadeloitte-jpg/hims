import { z } from 'zod';

export const loginSchema = z.object({
  username: z.string().min(1, 'Username required'),
  password: z.string().min(1, 'Password required'),
});

export const otpGenerateSchema = z.object({
  mobileNo: z.string().regex(/^[6-9]\d{9}$/, 'Invalid Indian mobile number'),
});

export const otpVerifySchema = z.object({
  mobileNo: z.string().regex(/^[6-9]\d{9}$/),
  otp: z.string().length(6, 'OTP must be 6 digits'),
});

export const permissionUpdateSchema = z.object({
  roleId: z.number().int().positive(),
  modulePermissions: z.array(z.object({
    moduleId: z.number().int().positive(),
    permissions: z.array(z.string()).min(1),
  })),
});

export const notificationTemplateSchema = z.object({
  templateCode: z.string().min(1, 'Code required').max(100),
  channel: z.enum(['SMS', 'EMAIL', 'WHATSAPP']),
  subject: z.string().optional(),
  bodyTemplate: z.string().min(10, 'Body must be at least 10 chars'),
  isActive: z.boolean().default(true),
});

export type LoginValues = z.infer<typeof loginSchema>;
export type OtpGenerateValues = z.infer<typeof otpGenerateSchema>;
export type OtpVerifyValues = z.infer<typeof otpVerifySchema>;
export type PermissionUpdateValues = z.infer<typeof permissionUpdateSchema>;
export type NotificationTemplateValues = z.infer<typeof notificationTemplateSchema>;

/** User creation form validation */
export const userSchema = z.object({
  username: z.string().min(1, "Username required"),
  fullName: z.string().min(1, "Full name required"),
  email: z.string().email("Invalid email"),
  mobileNo: z.string().regex(/^[6-9]\d{9}$/, "Invalid mobile").optional(),
  roleId: z.coerce.number().int().positive("Role required"),
  isActive: z.boolean().default(true),
});

export type UserValues = z.infer<typeof userSchema>;

/** Role creation form validation */
export const roleSchema = z.object({
  roleName: z.string().min(1, "Role name required"),
  description: z.string().optional(),
  isActive: z.boolean().default(true),
});

export type RoleValues = z.infer<typeof roleSchema>;

/** Permission assignment validation */
export const permissionSchema = z.object({
  moduleId: z.coerce.number().int().positive(),
  permissions: z.array(z.string()).min(1, "At least one permission required"),
});

export type PermissionValues = z.infer<typeof permissionSchema>;

/** Notification template form validation */
export const notifTemplateSchema = z.object({
  templateCode: z.string().min(1, "Code required"),
  channel: z.enum(["SMS", "EMAIL", "WHATSAPP"]),
  subject: z.string().optional(),
  bodyTemplate: z.string().min(10, "Body must be at least 10 chars"),
  isActive: z.boolean().default(true),
});

export type NotifTemplateValues = z.infer<typeof notifTemplateSchema>;
