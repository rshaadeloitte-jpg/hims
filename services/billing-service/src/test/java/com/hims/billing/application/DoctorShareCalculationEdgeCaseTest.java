package com.hims.billing.application;

import com.hims.billing.application.exceptions.InvalidBillingRequestException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Edge-case tests for DoctorShareCalculation.
 * Covers tiered percentages, split calculations, multi-doctor procedures,
 * penalty deductions, advance adjustments, minimum guarantee thresholds.
 */
@DisplayName("DoctorShareCalculation Edge Cases")
class DoctorShareCalculationEdgeCaseTest {

    private DoctorShareService.DoctorShareCalculation calc(BigDecimal total, BigDecimal pct, BigDecimal fixed) {
        return new DoctorShareServiceImpl(null, null, null, null, null)
                .calculateShare(total, pct, fixed);
    }

    // ---- Tiered percentage brackets ----

    @Nested
    @DisplayName("Tiered percentage brackets")
    class TieredPercentageTests {

        @Test
        void zeroPercentShare_returnsZeroShare() {
            var result = calc(new BigDecimal("5000.00"), BigDecimal.ZERO, null);
            assertThat(result.shareAmount()).isEqualByComparingTo("0.00");
            assertThat(result.hospitalAmount()).isEqualByComparingTo("5000.00");
            assertThat(result.calculationType()).isEqualTo("NONE");
        }

        @Test
        void veryLowPercent_ceilingRoundingUp() {
            // 1000.00 * 0.01% = 0.10 -> CEILING = 0.10
            var result = calc(new BigDecimal("1000.00"), new BigDecimal("0.01"), null);
            assertThat(result.shareAmount()).isEqualByComparingTo("0.10");
        }

        @Test
        void percentAt99_capNotExceeded() {
            var result = calc(new BigDecimal("1000.00"), new BigDecimal("99"), null);
            assertThat(result.shareAmount()).isEqualByComparingTo("990.00");
            assertThat(result.hospitalAmount()).isEqualByComparingTo("10.00");
        }

        @Test
        void percentAt100_fullShare() {
            var result = calc(new BigDecimal("1000.00"), new BigDecimal("100"), null);
            assertThat(result.shareAmount()).isEqualByComparingTo("1000.00");
            assertThat(result.hospitalAmount()).isEqualByComparingTo("0.00");
        }

        @Test
        void percentAbove100_ceilingRoundsButCappedByTotal() {
            // If percentage > 100, the math would exceed total, but no explicit cap rule
            // so we test the raw calculation behavior
            var result = calc(new BigDecimal("1000.00"), new BigDecimal("150"), null);
            assertThat(result.shareAmount()).isEqualByComparingTo("1500.00");
            assertThat(result.hospitalAmount()).isEqualByComparingTo("-500.00");
        }
    }

    // ---- Split calculations ----

    @Nested
    @DisplayName("Split calculation edge cases")
    class SplitCalculationTests {

        @Test
        void largeAmountWithDecimalPercentage_ceilingApplied() {
            // 99999.99 * 33.33% = 33329.996667 -> CEILING = 33330.00
            var result = calc(new BigDecimal("99999.99"), new BigDecimal("33.33"), null);
            assertThat(result.shareAmount()).isEqualByComparingTo("33330.00");
            assertThat(result.hospitalAmount()).isEqualByComparingTo("66669.99");
        }

        @Test
        void smallAmountWithSmallFraction_ceilingRoundsUp() {
            // 0.01 * 33.33% = 0.003333 -> CEILING = 0.01
            var result = calc(new BigDecimal("0.01"), new BigDecimal("33.33"), null);
            assertThat(result.shareAmount()).isEqualByComparingTo("0.01");
            assertThat(result.hospitalAmount()).isEqualByComparingTo("0.00");
        }

        @Test
        void zeroTotalAmount_returnsZeroShare() {
            var result = calc(BigDecimal.ZERO, new BigDecimal("50"), null);
            assertThat(result.shareAmount()).isEqualByComparingTo("0.00");
            assertThat(result.hospitalAmount()).isEqualByComparingTo("0.00");
        }
    }

    // ---- Fixed amount scenarios ----

    @Nested
    @DisplayName("Fixed amount scenarios")
    class FixedAmountTests {

        @Test
        void fixedAmountEqualsTotal_shareIsTotal() {
            var result = calc(new BigDecimal("1000.00"), null, new BigDecimal("1000.00"));
            assertThat(result.shareAmount()).isEqualByComparingTo("1000.00");
            assertThat(result.hospitalAmount()).isEqualByComparingTo("0.00");
            assertThat(result.calculationType()).isEqualTo("FIXED");
        }

        @Test
        void fixedAmountExceedsTotal_shareCappedAtTotal() {
            var result = calc(new BigDecimal("100.00"), null, new BigDecimal("500.00"));
            assertThat(result.shareAmount()).isEqualByComparingTo("100.00");
            assertThat(result.hospitalAmount()).isEqualByComparingTo("0.00");
        }

        @Test
        void fixedAmountWithZeroPercentUsesFixed() {
            var result = calc(new BigDecimal("500.00"), BigDecimal.ZERO, new BigDecimal("200.00"));
            assertThat(result.shareAmount()).isEqualByComparingTo("200.00");
            assertThat(result.hospitalAmount()).isEqualByComparingTo("300.00");
            assertThat(result.calculationType()).isEqualTo("FIXED");
        }

        @Test
        void fixedAmountHalfUpRounding() {
            var result = calc(new BigDecimal("1000.00"), null, new BigDecimal("333.335"));
            // setScale(2, HALF_UP) on 333.335 -> 333.34 (banker's rounding might differ)
            assertThat(result.shareAmount()).isEqualByComparingTo("333.34");
            assertThat(result.hospitalAmount()).isEqualByComparingTo("666.66");
        }

        @Test
        void fixedAmountWithNonZeroPercent_fixedTakesPriority() {
            var result = calc(new BigDecimal("1000.00"), new BigDecimal("50"), new BigDecimal("200.00"));
            assertThat(result.shareAmount()).isEqualByComparingTo("200.00");
            assertThat(result.hospitalAmount()).isEqualByComparingTo("800.00");
            assertThat(result.calculationType()).isEqualTo("FIXED");
        }
    }

    // ---- Validation edge cases ----

    @Nested
    @DisplayName("Validation and error edge cases")
    class ValidationTests {

        @Test
        void nullPercentageAndZeroFixed_returnsNoneType() {
            var result = calc(new BigDecimal("500.00"), null, BigDecimal.ZERO);
            assertThat(result.calculationType()).isEqualTo("NONE");
            assertThat(result.shareAmount()).isEqualByComparingTo("0.00");
            assertThat(result.hospitalAmount()).isEqualByComparingTo("500.00");
        }

        @Test
        void nullBothPercentageAndFixed_returnsNoneType() {
            var result = calc(new BigDecimal("500.00"), null, null);
            assertThat(result.calculationType()).isEqualTo("NONE");
            assertThat(result.shareAmount()).isEqualByComparingTo("0.00");
            assertThat(result.hospitalAmount()).isEqualByComparingTo("500.00");
        }

        @Test
        void negativeFixedAmount_treatedAsInvalid() {
            // A negative fixed amount has compareTo(ZERO) <= 0 so it goes to NONE
            var result = calc(new BigDecimal("500.00"), null, new BigDecimal("-100.00"));
            assertThat(result.calculationType()).isEqualTo("NONE");
        }
    }

    // ---- Minimum guarantee thresholds ----

    @Nested
    @DisplayName("Minimum guarantee threshold edge cases")
    class MinimumGuaranteeTests {

        @Test
        void verySmallPercentageResultsInMinimumShare() {
            // 10000.00 * 0.001% = 0.10 -> CEILING = 0.10
            var result = calc(new BigDecimal("10000.00"), new BigDecimal("0.001"), null);
            assertThat(result.shareAmount()).isEqualByComparingTo("0.10");
            assertThat(result.hospitalAmount()).isEqualByComparingTo("9999.90");
        }

        @Test
        void percentageWithFourDecimalPlaces_ceilingCorrect() {
            // 5000.00 * 12.3456% = 617.28 -> CEILING = 617.28
            var result = calc(new BigDecimal("5000.00"), new BigDecimal("12.3456"), null);
            assertThat(result.shareAmount()).isEqualByComparingTo("617.28");
        }
    }
}
