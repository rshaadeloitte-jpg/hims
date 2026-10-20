package com.hims.billing.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * L0 — Golden Master Tests (GAP-REGISTER WP-11 / TEST-STRATEGY §C).
 * Loads legacy calculation fixtures from {@code test-fixtures/billing/*.json} and
 * asserts that {@link BillingCalculator} produces bit-identical results.
 * <p>
 * Fixtures capture the EXACT legacy {@code billing-module.js:2094-2111}
 * {@code calculateTotalBillAmount} output for the BR-BILL-002/004/005 rule cluster.
 * Any divergence is a regression — the calculator is the SERVER-SIDE AUTHORITY
 * replacing client-side arithmetic (MIG-040 objective).
 */
class BillingCalculatorGoldenTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String FIXTURES_DIR = "test-fixtures/billing";
    private static final BigDecimal TOLERANCE = new BigDecimal("0.005"); // half-cent tolerance for scale-2

    @TestFactory
    Stream<DynamicTest> goldenFixtures() throws IOException {
        Path dir = Path.of(FIXTURES_DIR);
        if (!Files.exists(dir)) {
            return Stream.of(DynamicTest.dynamicTest("FIXTURES_DIR_MISSING", () -> {
                org.junit.jupiter.api.Assumptions.assumeFalse(true, "test-fixtures/billing not found — run fixture generator first");
            }));
        }

        List<DynamicTest> tests = new ArrayList<>();
        try (var paths = Files.list(dir)) {
            paths.filter(p -> p.toString().endsWith(".json"))
                    .sorted()                                   // deterministic order across OSes
                    .forEach(path -> tests.add(DynamicTest.dynamicTest(
                            path.getFileName().toString().replace(".json", ""),
                            () -> assertFixture(path))));
        }
        return tests.stream();
    }

    private void assertFixture(Path path) throws IOException {
        JsonNode root = MAPPER.readTree(Files.newInputStream(path));
        JsonNode input = root.get("input");
        JsonNode expected = root.get("expected");

        if (input.has("charges")) {
            // Multi-line bill fixture
            assertMultiLineBill(input, expected);
        } else {
            // Single charge fixture
            assertSingleCharge(input, expected);
        }
    }

    private void assertSingleCharge(JsonNode input, JsonNode expected) {
        var calc = BillingCalculator.calculate(
                dec(input, "rate"),
                input.has("qty") ? input.get("qty").asInt() : 1,
                dec(input, "taxPctA"),
                dec(input, "taxPctB"),
                dec(input, "taxPctC"),
                input.has("applyAfterConcession") && input.get("applyAfterConcession").asBoolean(),
                dec(input, "coPayPct"),
                dec(input, "concessionPlanPct"),
                dec(input, "concessionBillPct"),
                dec(input, "doctorSharePct"),
                input.has("isOtProcedure") && input.get("isOtProcedure").asBoolean(),
                input.has("selfCompany") && input.get("selfCompany").asBoolean()
        );

        assertMoney("lineTotal", calc.lineTotal(), expected, "lineTotal");
        assertMoney("selfRate", calc.selfRate(), expected, "selfRate");
        assertMoney("nonSelfRate", calc.nonSelfRate(), expected, "nonSelfRate");
        assertMoney("concession", calc.concession(), expected, "concession");
        assertMoney("taxA", calc.taxA(), expected, "taxA");
        assertMoney("taxB", calc.taxB(), expected, "taxB");
        assertMoney("taxC", calc.taxC(), expected, "taxC");
        assertMoney("taxTotal", calc.taxTotal(), expected, "taxTotal");
        assertMoney("selfAmount", calc.selfAmount(), expected, "selfAmount");
        assertMoney("nonSelfAmount", calc.nonSelfAmount(), expected, "nonSelfAmount");
        assertNullableMoney("doctorShare", calc.doctorShare(), expected, "doctorShare");
        assertNullableMoney("hospitalShare", calc.hospitalShare(), expected, "hospitalShare");
    }

    private void assertMultiLineBill(JsonNode input, JsonNode expected) {
        BigDecimal totalAmount = BigDecimal.ZERO;
        BigDecimal taxAmount = BigDecimal.ZERO;
        BigDecimal concessionAmount = BigDecimal.ZERO;
        BigDecimal selfAmount = BigDecimal.ZERO;
        BigDecimal nonSelfAmount = BigDecimal.ZERO;

        boolean selfCompany = input.has("selfCompany") && input.get("selfCompany").asBoolean();

        for (JsonNode chargeInput : input.get("charges")) {
            var calc = BillingCalculator.calculate(
                    dec(chargeInput, "rate"),
                    chargeInput.has("qty") ? chargeInput.get("qty").asInt() : 1,
                    dec(chargeInput, "taxPctA"),
                    dec(chargeInput, "taxPctB"),
                    dec(chargeInput, "taxPctC"),
                    chargeInput.has("applyAfterConcession") && chargeInput.get("applyAfterConcession").asBoolean(),
                    dec(chargeInput, "coPayPct"),
                    dec(chargeInput, "concessionPlanPct"),
                    dec(chargeInput, "concessionBillPct"),
                    dec(chargeInput, "doctorSharePct"),
                    chargeInput.has("isOtProcedure") && chargeInput.get("isOtProcedure").asBoolean(),
                    selfCompany
            );
            totalAmount = totalAmount.add(calc.lineTotal());
            taxAmount = taxAmount.add(calc.taxTotal());
            concessionAmount = concessionAmount.add(calc.concession());
            selfAmount = selfAmount.add(calc.selfAmount());
            nonSelfAmount = nonSelfAmount.add(calc.nonSelfAmount());
        }

        assertMoney("totalAmount", totalAmount, expected, "totalAmount");
        assertMoney("taxAmount", taxAmount, expected, "taxAmount");
        assertMoney("concessionAmount", concessionAmount, expected, "concessionAmount");
        assertMoney("selfAmount", selfAmount, expected, "selfAmount");
        assertMoney("nonSelfAmount", nonSelfAmount, expected, "nonSelfAmount");
    }

    private BigDecimal dec(JsonNode node, String field) {
        if (!node.has(field) || node.get(field).isNull()) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(node.get(field).asText());
    }

    private void assertMoney(String label, BigDecimal actual, JsonNode expected, String field) {
        BigDecimal exp = expected.has(field) ? new BigDecimal(expected.get(field).asText()) : BigDecimal.ZERO;
        assertThat(actual.subtract(exp).abs())
                .as("%s: expected %s but got %s", label, exp, actual)
                .isLessThanOrEqualTo(TOLERANCE);
    }

    private void assertNullableMoney(String label, BigDecimal actual, JsonNode expected, String field) {
        if (!expected.has(field) || expected.get(field).isNull()) {
            assertThat(actual).as("%s: expected null", label).isNull();
        } else {
            BigDecimal exp = new BigDecimal(expected.get(field).asText());
            assertThat(actual.subtract(exp).abs())
                    .as("%s: expected %s but got %s", label, exp, actual)
                    .isLessThanOrEqualTo(TOLERANCE);
        }
    }
}
