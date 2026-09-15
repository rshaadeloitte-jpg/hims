import { describe, expect, it } from 'vitest';
import { renderHook } from '@testing-library/react';
import { useResultValidation } from '../useResultValidation';
import type { LabTest } from '../types';

describe('useResultValidation', () => {
  const mockTest: LabTest = {
    testId: 1,
    testCode: 'GLU',
    testName: 'Glucose',
    department: 'Biochemistry',
    sampleType: 'Blood',
    containerType: 'Gray Top',
    turnaroundTime: 2,
    rate: 100,
    isOutsourced: false,
    unit: 'mg/dL',
    criticalLow: 40,
    criticalHigh: 500,
    normalLow: 70,
    normalHigh: 110,
  };

  it('returns valid for empty value', () => {
    const { result } = renderHook(() => useResultValidation(mockTest));
    const validation = result.current.validate('');
    
    expect(validation.isValid).toBe(true);
    expect(validation.isAbnormal).toBe(false);
    expect(validation.isCritical).toBe(false);
  });

  it('returns valid for non-numeric value', () => {
    const { result } = renderHook(() => useResultValidation(mockTest));
    const validation = result.current.validate('Positive');
    
    expect(validation.isValid).toBe(true);
    expect(validation.isAbnormal).toBe(false);
    expect(validation.isCritical).toBe(false);
    expect(validation.message).toBe('Non-numeric value');
  });

  it('detects critical low value', () => {
    const { result } = renderHook(() => useResultValidation(mockTest));
    const validation = result.current.validate('35');
    
    expect(validation.isValid).toBe(true);
    expect(validation.isAbnormal).toBe(true);
    expect(validation.isCritical).toBe(true);
    expect(validation.message).toContain('CRITICAL LOW');
  });

  it('detects critical high value', () => {
    const { result } = renderHook(() => useResultValidation(mockTest));
    const validation = result.current.validate('550');
    
    expect(validation.isValid).toBe(true);
    expect(validation.isAbnormal).toBe(true);
    expect(validation.isCritical).toBe(true);
    expect(validation.message).toContain('CRITICAL HIGH');
  });

  it('detects abnormal low value', () => {
    const { result } = renderHook(() => useResultValidation(mockTest));
    const validation = result.current.validate('60');
    
    expect(validation.isValid).toBe(true);
    expect(validation.isAbnormal).toBe(true);
    expect(validation.isCritical).toBe(false);
    expect(validation.message).toContain('LOW');
  });

  it('detects abnormal high value', () => {
    const { result } = renderHook(() => useResultValidation(mockTest));
    const validation = result.current.validate('120');
    
    expect(validation.isValid).toBe(true);
    expect(validation.isAbnormal).toBe(true);
    expect(validation.isCritical).toBe(false);
    expect(validation.message).toContain('HIGH');
  });

  it('returns normal for value within range', () => {
    const { result } = renderHook(() => useResultValidation(mockTest));
    const validation = result.current.validate('90');
    
    expect(validation.isValid).toBe(true);
    expect(validation.isAbnormal).toBe(false);
    expect(validation.isCritical).toBe(false);
    expect(validation.message).toBe('Normal');
  });

  it('handles test without ranges', () => {
    const testWithoutRanges: LabTest = {
      ...mockTest,
      criticalLow: undefined,
      criticalHigh: undefined,
      normalLow: undefined,
      normalHigh: undefined,
    };

    const { result } = renderHook(() => useResultValidation(testWithoutRanges));
    const validation = result.current.validate('100');
    
    expect(validation.isValid).toBe(true);
    expect(validation.isAbnormal).toBe(false);
    expect(validation.isCritical).toBe(false);
  });
});