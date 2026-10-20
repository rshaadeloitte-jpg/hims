package com.hims.ordering.api;

import com.hims.ordering.application.OrderService;
import com.hims.ordering.domain.model.ClinicalOrder;
import com.hims.ordering.domain.model.SagaState;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * INTERNAL saga step callbacks (MIG-033): called by downstream consumers
 * (Billing / Lab / Rad / Nursing / Pharmacy ingress) to confirm or fail their
 * step against an order's saga. Tagged internal; default-deny still applies
 * (no exemption in SecurityConfig until MIG-004 service-to-service auth lands).
 */
@RestController
@RequestMapping("/api/v1/internal/orders")
@Tag(name = "Orders internal saga callbacks (MIG-033)",
        description = "INTERNAL saga choreography endpoints: confirm or fail a downstream step (BILLING|LAB|RADIOLOGY|NURSING|PHARMACY). "
                + "Failure compensates succeeded steps, flips the order FAILED(4) and emits OrderFailed in the same tx; default-deny applies")
public class InternalOrderController {

    private final OrderService service;

    public InternalOrderController(OrderService service) {
        this.service = service;
    }

    @PostMapping("/{id}/steps/{step}/confirm")
    @Operation(summary = "[internal] Confirm a saga step",
            description = "Downstream SUCCESS callback: PENDING -> SUCCEEDED for an applicable step (BILLING|LAB|RADIOLOGY|NURSING|"
                    + "PHARMACY). Idempotent on consumer redelivery. When every applicable step is SUCCEEDED the order flips to "
                    + "CONFIRMED(2) and the saga COMPLETES. Errors: 400 ORD_SAGA_STEP_INVALID / ORD_ILLEGAL_TRANSITION.")
    public ClinicalOrder confirmStep(@PathVariable long id, @PathVariable String step) {
        return service.confirmStep(id, normalize(step));
    }

    @PostMapping("/{id}/steps/{step}/fail")
    @Operation(summary = "[internal] Fail a saga step (compensation trigger)",
            description = "Downstream FAILURE callback: the failing step -> FAILED, every SUCCEEDED step -> COMPENSATED, order -> "
                    + "FAILED(4) with reason persisted, and an OrderFailed compensation event written in the SAME tx so downstream "
                    + "reservations roll back without any lost-update window. Idempotent on replay.")
    public ClinicalOrder failStep(@PathVariable long id, @PathVariable String step,
                                  @RequestBody StepFailRequest body) {
        return service.failStep(id, normalize(step), body.reason());
    }

    static String normalize(String step) {
        return step == null ? null : step.trim().toUpperCase();
    }

    public record StepFailRequest(String reason) {}
}
