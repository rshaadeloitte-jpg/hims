import { useMemo } from 'react';
import type { LabTest, ResultEntry } from './types';

/**
 * Hook for validating lab results against reference ranges
 */
export function useResultValidation(test?: LabTest) {
  const validate = (value: string): { 
    isValid: boolean; 
    isAbnormal: boolean; 
    isCritical: boolean;
    message?: string;
  } => {
    if (!test || !value) {
      return { isValid: true, isAbnormal: false, isCritical: false };
    }

    const numericValue = parseFloat(value);
    if (isNaN(numericValue)) {
      return { 
        isValid: true, 
        isAbnormal: false, 
        isCritical: false,
        message: 'Non-numeric value'
      };
    }

    // Check critical ranges first
    if (test.criticalLow !== undefined && numericValue < test.criticalLow) {
      return {
        isValid: true,
        isAbnormal: true,
        isCritical: true,
        message: `CRITICAL LOW: Below ${test.criticalLow} ${test.unit ?? ''}`,
      };
    }

    if (test.criticalHigh !== undefined && numericValue > test.criticalHigh) {
      return {
        isValid: true,
        isAbnormal: true,
        isCritical: true,
        message: `CRITICAL HIGH: Above ${test.criticalHigh} ${test.unit ?? ''}`,
      };
    }

    // Check normal ranges
    if (test.normalLow !== undefined && numericValue < test.normalLow) {
      return {
        isValid: true,
        isAbnormal: true,
        isCritical: false,
        message: `LOW: Below normal range ${test.normalLow}-${test.normalHigh} ${test.unit ?? ''}`,
      };
    }

    if (test.normalHigh !== undefined && numericValue > test.normalHigh) {
      return {
        isValid: true,
        isAbnormal: true,
        isCritical: false,
        message: `HIGH: Above normal range ${test.normalLow}-${test.normalHigh} ${test.unit ?? ''}`,
      };
    }

    return {
      isValid: true,
      isAbnormal: false,
      isCritical: false,
      message: 'Normal',
    };
  };

  return { validate };
}

/**
 * Hook for batch validation of multiple results
 */
export function useBatchResultValidation(tests: Map<number, LabTest>) {
  const validateAll = (results: ResultEntry[]): {
    allValid: boolean;
    hasAbnormal: boolean;
    hasCritical: boolean;
    validations: Map<number, { isAbnormal: boolean; isCritical: boolean; message?: string }>;
  } => {
    const validations = new Map<number, { isAbnormal: boolean; isCritical: boolean; message?: string }>();
    let hasAbnormal = false;
    let hasCritical = false;

    for (const result of results) {
      const test = tests.get(result.detailId);
      if (!test) continue;

      const numericValue = parseFloat(result.value);
      if (isNaN(numericValue)) {
        validations.set(result.detailId, { isAbnormal: false, isCritical: false });
        continue;
      }

      let isAbnormal = false;
      let isCritical = false;
      let message: string | undefined;

      // Check critical ranges
      if (test.criticalLow !== undefined && numericValue < test.criticalLow) {
        isAbnormal = true;
        isCritical = true;
        message = `CRITICAL LOW`;
      } else if (test.criticalHigh !== undefined && numericValue > test.criticalHigh) {
        isAbnormal = true;
        isCritical = true;
        message = `CRITICAL HIGH`;
      }
      // Check normal ranges
      else if (test.normalLow !== undefined && numericValue < test.normalLow) {
        isAbnormal = true;
        message = `LOW`;
      } else if (test.normalHigh !== undefined && numericValue > test.normalHigh) {
        isAbnormal = true;
        message = `HIGH`;
      }

      if (isAbnormal) hasAbnormal = true;
      if (isCritical) hasCritical = true;

      validations.set(result.detailId, { isAbnormal, isCritical, message });
    }

    return {
      allValid: true,
      hasAbnormal,
      hasCritical,
      validations,
    };
  };

  return { validateAll };
}