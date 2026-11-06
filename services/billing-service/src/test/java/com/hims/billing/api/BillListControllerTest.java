package com.hims.billing.api;

import com.hims.billing.application.BillingService;
import com.hims.billing.configuration.JwtConfig;
import com.hims.billing.configuration.SecurityConfig;
import com.hims.billing.domain.model.Bill;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BillController.class)
@Import({SecurityConfig.class, JwtConfig.class})
@AutoConfigureMockMvc
class BillListControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BillingService billingService;

    @Test
    void listBills_ok() throws Exception {
        when(billingService.listBills(0, 20)).thenReturn(List.of(new Bill()));
        mockMvc.perform(get("/api/v1/bills")
                        .param("page", "0")
                        .param("size", "20")
                        .with(user("tester")))
                .andExpect(status().isOk());
    }

    @Test
    void methodNotAllowed_returns405() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1/bills/1")
                        .with(user("tester")))
                .andExpect(status().isMethodNotAllowed());
    }
}
