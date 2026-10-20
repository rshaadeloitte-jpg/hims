package com.hims.billing.application;

import com.hims.billing.application.exceptions.InvalidBillingRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Tests for BillingService.estimate (M04 gap).
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("BillingService Estimate (M04 gap)")
class BillingServiceEstimateTest {

    private BillingService service;

    @BeforeEach
    void wireService() {
        service = new BillingService(null, null, null, null, null, null);
    }

    private static BillingService.EstimateItem item(String rate, int qty) {
        return new BillingService.EstimateItem(900L, "Test Service", new BigDecimal(rate), qty,
                null, null, null, false, null, null, null, null, null, false);
    }

    private static BillingService.EstimateItem itemWithTax(String rate, int qty, String taxA) {
        return new BillingService.EstimateItem(900L, "Test Service", new BigDecimal(rate), qty,
                new BigDecimal(taxA), null, null, false, null, null, null, null, null, false);
    }

    // ---- Estimate edge cases ----

    @Nested
    @DisplayName("Estimate edge cases")
    class EstimateTests {

        @Test
        void estimate_singleItem_returnsCorrectTotal() {
            BillingService.EstimateResult result = service.estimate(505L, true, List.of(item("100.00", 1)));
            assertThat(result.totalAmount()).isEqualByComparingTo("100.00");
            assertThat(result.patientId()).isEqualTo(505L);
        }

        @Test
        void estimate_multipleItems_sumsTotals() {
            BillingService.EstimateResult result = service.estimate(505L, true, List.of(
                    item("100.00", 1),
                    item("200.00", 2),
                    item("50.00", 3)
            ));
            // 100 + 400 + 150 = 650
            assertThat(result.totalAmount()).isEqualByComparingTo("650.00");
        }

        @Test
        void estimate_withTax_includesTaxInSelfAmount() {
            BillingService.EstimateResult result = service.estimate(505L, true, List.of(
                    itemWithTax("100.00", 1, "18")
            ));
            // lineTotal = 100, tax = 18, selfAmount = 100 + 18 = 118
            assertThat(result.taxAmount()).isEqualByComparingTo("18.00");
        }

        @Test
        void estimate_nullPatientId_throwsException() {
            assertThatThrownBy(() -> service.estimate(null, true, List.of(item("100.00", 1))))
                    .isInstanceOf(InvalidBillingRequestException.class);
        }

        @Test
        void estimate_emptyItems_throwsException() {
            assertThatThrownBy(() -> service.estimate(505L, true, List.of()))
                    .isInstanceOf(InvalidBillingRequestException.class);
        }

        @Test
        void estimate_nullItems_throwsException() {
            assertThatThrownBy(() -> service.estimate(505L, true, null))
                    .isInstanceOf(InvalidBillingRequestException.class);
        }

        @Test
        void estimate_negativeRate_throwsException() {
            assertThatThrownBy(() -> service.estimate(505L, true, List.of(item("-100.00", 1))))
                    .isInstanceOf(InvalidBillingRequestException.class);
        }

        @Test
        void estimate_zeroQty_throwsException() {
            assertThatThrownBy(() -> service.estimate(505L, true, List.of(item("100.00", 0))))
                    .isInstanceOf(InvalidBillingRequestException.class);
        }

        @Test
        void estimate_nullIsSelfCompany_defaultsToSelf() {
            BillingService.EstimateResult result = service.estimate(505L, null, List.of(item("100.00", 1)));
            assertThat(result.isSelfCompany()).isTrue();
        }

        @Test
        void estimate_nonSelfCompany_appliesNonSelfPath() {
            BillingService.EstimateResult result = service.estimate(505L, false, List.of(item("100.00", 1)));
            assertThat(result.isSelfCompany()).isFalse();
        }

        @Test
        void estimate_returnsLineDetails() {
            BillingService.EstimateResult result = service.estimate(505L, true, List.of(
                    item("100.00", 1),
                    item("200.00", 1)
            ));
            assertThat(result.lines()).hasSize(2);
        }
    }
}
