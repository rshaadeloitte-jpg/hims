package com.hims.pmrd.api;

import com.hims.pmrd.application.PMRDService;
import com.hims.patient.configuration.JwtConfig;
import com.hims.patient.configuration.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PMRDController.class)
@Import({SecurityConfig.class, JwtConfig.class})
@AutoConfigureMockMvc
class JsyFormsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PMRDService pMRDService;

    @Test
    void jsyForms_formTypeJ1_ok() throws Exception {
        when(pMRDService.getJsyForms("J1", null)).thenReturn(List.of());
        mockMvc.perform(get("/api/v1/pmrd/jsy-forms")
                        .param("formType", "J1")
                        .with(user("tester")))
                .andExpect(status().isOk());
    }
}
