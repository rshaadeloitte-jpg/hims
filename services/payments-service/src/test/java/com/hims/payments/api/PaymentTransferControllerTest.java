package com.hims.payments.api;

import com.hims.payments.application.PaymentTransferService;
import com.hims.payments.domain.model.PaymentTransfer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PaymentTransferController.class)
@Import(com.hims.payments.configuration.SecurityConfig.class)
@AutoConfigureMockMvc
class PaymentTransferControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PaymentTransferService service;

    private PaymentTransfer sample() {
        PaymentTransfer t = new PaymentTransfer();
        t.setId(1L);
        t.setBeneficiaryName("John");
        t.setBeneficiaryAccountNo("123456");
        t.setBeneficiaryIfsc("SBIN0001");
        t.setAmount(new BigDecimal("100.00"));
        t.setScheme("PMJAY");
        t.setPurpose("DBT");
        t.setStatus("PENDING");
        t.setCreatedAt(LocalDateTime.now());
        t.setUpdatedAt(LocalDateTime.now());
        return t;
    }

    @Test
    void schemes_returnsList() throws Exception {
        when(service.schemes()).thenReturn(List.of("PMJAY", "JSY"));
        mockMvc.perform(get("/api/v1/payment-transfer/schemes").with(user("u").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value("PMJAY"));
    }

    @Test
    void initiate_createsTransfer() throws Exception {
        when(service.initiate(any(), any(), any(), any(), any(), any(), any())).thenReturn(sample());
        String body = "{\"beneficiaryName\":\"John\",\"beneficiaryAccountNo\":\"123456\",\"beneficiaryIfsc\":\"SBIN0001\",\"amount\":100.00,\"scheme\":\"PMJAY\",\"purpose\":\"DBT\"}";
        mockMvc.perform(post("/api/v1/payment-transfer").with(user("u").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.transferId").value(1))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void get_returnsTransfer() throws Exception {
        when(service.get(1L)).thenReturn(sample());
        mockMvc.perform(get("/api/v1/payment-transfer/1").with(user("u").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transferId").value(1));
    }
}