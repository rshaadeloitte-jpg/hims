import { z } from 'zod';

export const slotSelectionSchema = z.object({
  date: z.string().regex(/^\d{4}-\d{2}-\d{2}$/, 'Invalid date format'),
  slotStart: z.string().regex(/^\d{2}:\d{2}$/, 'Invalid start time'),
  slotEnd: z.string().regex(/^\d{2}:\d{2}$/, 'Invalid end time'),
  doctorId: z.number().int().positive('Doctor ID required'),
});

export type SlotSelectionValues = z.infer<typeof slotSelectionSchema>;

export const appointmentSchema = z.object({
  followUpDate: z.string().regex(/^\d{4}-\d{2}-\d{2}$/, 'Date required'),
  doctorId: z.number().int().positive('Doctor required').optional(),
  departmentId: z.number().int().positive('Department required').optional(),
  slotStart: z.string().regex(/^\d{2}:\d{2}$/, 'Start time required'),
  slotEnd: z.string().regex(/^\d{2}:\d{2}$/, 'End time required'),
  cabin: z.string().max(20).optional(),
  recurrenceRule: z.string().optional(),
});

export type AppointmentValues = z.infer<typeof appointmentSchema>;