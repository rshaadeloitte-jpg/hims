import { describe, expect, it } from 'vitest';
import {
  radOrderBookingSchema,
  radReportSchema,
  radRescheduleSchema,
  radRetestSchema,
  radTemplateSchema,
} from '../zodSchema';

describe('radOrderBookingSchema', () => {
  it('accepts valid order with one line', () => {
    const result = radOrderBookingSchema.safeParse({
      patientId: 100,
      encounterId: 200,
      modality: 'CT',
      priority: 'ROUTINE',
      lines: [{ itemId: 500, qty: 1 }],
    });
    expect(result.success).toBe(true);
  });

  it('requires patientId', () => {
    const result = radOrderBookingSchema.safeParse({
      encounterId: 200,
      modality: 'CT',
      priority: 'ROUTINE',
      lines: [{ itemId: 500, qty: 1 }],
    });
    expect(result.success).toBe(false);
    if (!result.success) {
      expect(result.error.issues[0].path).toContain('patientId');
    }
  });

  it('requires valid modality', () => {
    const result = radOrderBookingSchema.safeParse({
      patientId: 100,
      modality: 'INVALID',
      lines: [{ itemId: 500, qty: 1 }],
    });
    expect(result.success).toBe(false);
  });

  it('accepts all valid modalities', () => {
    for (const modality of ['XRAY', 'CT', 'MRI', 'US']) {
      const result = radOrderBookingSchema.safeParse({
        patientId: 100,
        modality,
        lines: [{ itemId: 500, qty: 1 }],
      });
      expect(result.success).toBe(true);
    }
  });

  it('requires at least one line', () => {
    const result = radOrderBookingSchema.safeParse({
      patientId: 100,
      modality: 'CT',
      lines: [],
    });
    expect(result.success).toBe(false);
  });

  it('coerces numeric strings', () => {
    const result = radOrderBookingSchema.safeParse({
      patientId: '100',
      encounterId: '200',
      modality: 'CT',
      priority: 'URGENT',
      lines: [{ itemId: '500', qty: '2' }],
    });
    expect(result.success).toBe(true);
    if (result.success) {
      expect(result.data.patientId).toBe(100);
      expect(result.data.lines[0].itemId).toBe(500);
      expect(result.data.lines[0].qty).toBe(2);
    }
  });

  it('defaults priority to ROUTINE', () => {
    const result = radOrderBookingSchema.safeParse({
      patientId: 100,
      modality: 'XRAY',
      lines: [{ itemId: 500, qty: 1 }],
    });
    expect(result.success).toBe(true);
    if (result.success) {
      expect(result.data.priority).toBe('ROUTINE');
    }
  });

  it('allows both encounterId and admissionId to be undefined', () => {
    const result = radOrderBookingSchema.safeParse({
      patientId: 100,
      modality: 'XRAY',
      lines: [{ itemId: 500, qty: 1 }],
    });
    expect(result.success).toBe(true);
  });
});

describe('radReportSchema', () => {
  it('requires reportText', () => {
    const result = radReportSchema.safeParse({
      reportText: '',
      reportedBy: 'Dr. Smith',
    });
    expect(result.success).toBe(false);
  });

  it('requires reportedBy', () => {
    const result = radReportSchema.safeParse({
      reportText: 'Normal',
      reportedBy: '',
    });
    expect(result.success).toBe(false);
  });

  it('accepts valid report', () => {
    const result = radReportSchema.safeParse({
      reportText: 'No acute abnormality detected.',
      reportedBy: 'Dr. Smith',
    });
    expect(result.success).toBe(true);
  });
});

describe('radRescheduleSchema', () => {
  it('requires rescheduledTo', () => {
    const result = radRescheduleSchema.safeParse({ rescheduledTo: '' });
    expect(result.success).toBe(false);
  });

  it('accepts valid datetime', () => {
    const result = radRescheduleSchema.safeParse({
      rescheduledTo: '2024-03-15T10:00:00',
    });
    expect(result.success).toBe(true);
  });
});

describe('radRetestSchema', () => {
  it('requires reason', () => {
    const result = radRetestSchema.safeParse({ reason: '' });
    expect(result.success).toBe(false);
  });

  it('accepts valid reason', () => {
    const result = radRetestSchema.safeParse({
      reason: 'Motion artifact',
    });
    expect(result.success).toBe(true);
  });
});

describe('radTemplateSchema', () => {
  it('requires templateCode', () => {
    const result = radTemplateSchema.safeParse({
      templateCode: '',
      modality: 'CT',
      bodyTemplate: 'Findings...',
    });
    expect(result.success).toBe(false);
  });

  it('requires valid modality', () => {
    const result = radTemplateSchema.safeParse({
      templateCode: 'CT-HEAD',
      modality: 'INVALID',
      bodyTemplate: 'Findings...',
    });
    expect(result.success).toBe(false);
  });

  it('requires bodyTemplate', () => {
    const result = radTemplateSchema.safeParse({
      templateCode: 'CT-HEAD',
      modality: 'CT',
      bodyTemplate: '',
    });
    expect(result.success).toBe(false);
  });

  it('accepts optional description', () => {
    const result = radTemplateSchema.safeParse({
      templateCode: 'CT-HEAD',
      modality: 'CT',
      bodyTemplate: 'Findings...',
      description: 'Standard CT head template',
    });
    expect(result.success).toBe(true);
  });

  it('accepts without description', () => {
    const result = radTemplateSchema.safeParse({
      templateCode: 'CT-HEAD',
      modality: 'CT',
      bodyTemplate: 'Findings...',
    });
    expect(result.success).toBe(true);
  });
});
