package com.hims.ordering.api;

import com.hims.ordering.application.OrderService;
import com.hims.ordering.application.exceptions.InvalidOrderException;
import com.hims.ordering.configuration.SecurityConfig;
import com.hims.ordering.domain.model.ClinicalOrder;
import com.hims.ordering.domain.model.OrderItem;
import com.hims.ordering.domain.model.SagaState;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * L1 — contract tests for the MIG-033 order saga API (default-deny enforced).
 * Service mocked; the BR-ORD rules themselves are covered at L0/L2.
 */
@WebMvcTest({OrderController.class, InternalOrderController.class})
@Import(SecurityConfig.class)
@AutoConfigureMockMvc
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderService orderService;

    private static final String BODY = """
            {
              "patientId": 501, "encounterId": 7001, "prescriberId": 11,
              "wardId": 2, "concessionPercent": 10,
              "items": [
                {"itemType": "LAB", "itemId": 10, "serviceName": "CBC", "rate": 500, "qty": 1, "doctorSharePercent": 10},
                {"itemType": "CONSULTATION", "itemId": 20, "serviceName": "OPD consult", "rate": 100, "qty": 2}
              ]
            }
            """;

    private ClinicalOrder sample() {
        ClinicalOrder o = new ClinicalOrder();
        o.setOrderId(42L);
        o.setPatientId(501L);
        o.setEncounterId(7001L);
        o.setPrescriberId(11L);
        o.setStatusCode(ClinicalOrder.STATUS_INITIATED);
        o.setGrossTotal(new BigDecimal("700.00"));
        o.setConcessionPercent(new BigDecimal("10.00"));
        o.setConcessionAmount(new BigDecimal("70.00"));
        o.setNetTotal(new BigDecimal("630.00"));

        OrderItem i = new OrderItem();
        i.setOrderId(42L);
        i.setLineNo(1L);
        i.setItemType("LAB");
        i.setItemId(10L);
        i.setServiceName("CBC");
        i.setRate(new BigDecimal("500.00"));
        i.setQty(1);
        i.setDoctorSharePercent(new BigDecimal("10.00"));
        o.setItems(List.of(i));

        SagaState s = new SagaState();
        s.setSagaId(500L);
        s.setOrderId(42L);
        s.setSagaStatus(SagaState.IN_PROGRESS);
        s.setBillingStep(SagaState.PENDING);
        s.setLabStep(SagaState.PENDING);
        o.setSaga(s);
        return o;
    }

    @Test
    void createReturns201WithItemsAndSaga() throws Exception {
        when(orderService.create(eq(501L), eq(7001L), isNull(), eq(11L), eq(2L),
                any(), isNull(), anyList())).thenReturn(sample());
        mockMvc.perform(post("/api/v1/orders").with(user("u").roles("USER")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderId").value(42))
                .andExpect(jsonPath("$.statusCode").value(1))
                .andExpect(jsonPath("$.grossTotal").value(700.00))
                .andExpect(jsonPath("$.netTotal").value(630.00))
                .andExpect(jsonPath("$.items[0].itemType").value("LAB"))
                .andExpect(jsonPath("$.items[0].doctorSharePercent").value(10.00))
                .andExpect(jsonPath("$.saga.sagaStatus").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.saga.labStep").value("PENDING"))
                .andExpect(jsonPath("$.saga.radiologyStep").doesNotExist());
    }

    @Test
    void defaultDenyRejectsUnauthenticatedCreate() throws Exception {
        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void validationErrorMapsToLegacyEnvelope() throws Exception {
        when(orderService.create(any(), any(), any(), any(), any(), any(), any(), anyList()))
                .thenThrow(new InvalidOrderException("BR-ORD-003",
                        "concessionPercent must be within [0,100]"));
        mockMvc.perform(post("/api/v1/orders").with(user("u")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("ERROR"))
                .andExpect(jsonPath("$.errorCode").value("BR-ORD-003"));
    }

    @Test
    void getByIdExposesSagaAndRejectionReason() throws Exception {
        ClinicalOrder failed = sample();
        failed.setStatusCode(ClinicalOrder.STATUS_FAILED);
        failed.setRejectionReason("lab rejected");
        failed.getSaga().setSagaStatus(SagaState.FAILED);
        failed.getSaga().setFailedStep(SagaState.LAB);
        when(orderService.get(42L)).thenReturn(failed);
        mockMvc.perform(get("/api/v1/orders/42").with(user("u")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(4))
                .andExpect(jsonPath("$.rejectionReason").value("lab rejected"))
                .andExpect(jsonPath("$.saga.failedStep").value("LAB"));
    }

    @Test
    void listFilterByStatusName() throws Exception {
        when(orderService.list(eq((short) 4), eq(501L))).thenReturn(List.of(sample()));
        mockMvc.perform(get("/api/v1/orders").with(user("u"))
                        .param("status", "FAILED").param("patientId", "501"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].orderId").value(42));
    }

    @Test
    void unknownStatusNameIs400() throws Exception {
        mockMvc.perform(get("/api/v1/orders").with(user("u")).param("status", "WARP"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("BEXA003"));
    }

    @Test
    void cancelEndpointDelegates() throws Exception {
        ClinicalOrder cancelled = sample();
        cancelled.setStatusCode(ClinicalOrder.STATUS_CANCELLED);
        cancelled.getSaga().setSagaStatus(SagaState.CANCELLED);
        when(orderService.cancel(42L)).thenReturn(cancelled);
        mockMvc.perform(post("/api/v1/orders/42/cancel").with(user("u")).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(3))
                .andExpect(jsonPath("$.saga.sagaStatus").value("CANCELLED"));
    }

    // ------------------------------------------------------------ internal callbacks

    @Test
    void internalConfirmReturnsUpdatedOrder() throws Exception {
        ClinicalOrder confirmed = sample();
        confirmed.setStatusCode(ClinicalOrder.STATUS_CONFIRMED);
        confirmed.getSaga().setSagaStatus(SagaState.COMPLETED);
        confirmed.getSaga().setBillingStep(SagaState.SUCCEEDED);
        when(orderService.confirmStep(42L, "BILLING")).thenReturn(confirmed);
        mockMvc.perform(post("/api/v1/internal/orders/42/steps/billing/confirm")
                        .with(user("svc").roles("SERVICE")).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(2))
                .andExpect(jsonPath("$.saga.sagaStatus").value("COMPLETED"));
    }

    @Test
    void internalFailReturnsFailedOrderWithCompensation() throws Exception {
        ClinicalOrder failed = sample();
        failed.setStatusCode(ClinicalOrder.STATUS_FAILED);
        failed.setRejectionReason("PACS unreachable");
        failed.getSaga().setSagaStatus(SagaState.FAILED);
        failed.getSaga().setFailedStep(SagaState.LAB);
        failed.getSaga().setLabStep(SagaState.COMPENSATED);
        failed.getSaga().setBillingStep(SagaState.COMPENSATED);
        when(orderService.failStep(42L, "LAB", "PACS unreachable")).thenReturn(failed);
        mockMvc.perform(post("/api/v1/internal/orders/42/steps/lab/fail")
                        .with(user("svc").roles("SERVICE")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"PACS unreachable\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(4))
                .andExpect(jsonPath("$.rejectionReason").value("PACS unreachable"))
                .andExpect(jsonPath("$.saga.billingStep").value("COMPENSATED"));
    }

    @Test
    void illegalTransitionMapsTo400Envelope() throws Exception {
        when(orderService.confirmStep(42L, "BILLING"))
                .thenThrow(new InvalidOrderException("ORD_ILLEGAL_TRANSITION",
                        "Cannot confirm — saga already FAILED"));
        mockMvc.perform(post("/api/v1/internal/orders/42/steps/billing/confirm")
                        .with(user("svc")).with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("ERROR"))
                .andExpect(jsonPath("$.errorCode").value("ORD_ILLEGAL_TRANSITION"));
    }
}
