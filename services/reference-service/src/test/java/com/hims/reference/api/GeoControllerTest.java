package com.hims.reference.api;

import com.hims.reference.application.GeoService;
import com.hims.reference.configuration.JwtConfig;
import com.hims.reference.configuration.SecurityConfig;
import com.hims.reference.domain.model.GeoBlock;
import com.hims.reference.domain.model.GeoDistrict;
import com.hims.reference.domain.model.GeoSubDistrict;
import com.hims.reference.domain.model.GeoVillage;
import com.hims.reference.domain.model.LgdLocalBody;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(GeoController.class)
@Import({SecurityConfig.class, JwtConfig.class})
@AutoConfigureMockMvc
class GeoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GeoService service;

    @Test
    void districtsByState_returnsList() throws Exception {
        GeoDistrict d = new GeoDistrict();
        d.setId(1L);
        d.setStateId(2L);
        d.setCode("JAIPUR");
        d.setName("Jaipur");
        when(service.districtsByState(2L)).thenReturn(List.of(d));

        mockMvc.perform(get("/api/v1/geo/states/2/districts")
                        .with(user("test-user")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Jaipur"));
    }

    @Test
    void subDistrictsByDistrict_returnsList() throws Exception {
        GeoSubDistrict s = new GeoSubDistrict();
        s.setId(3L);
        s.setDistrictId(4L);
        s.setCode("SD1");
        s.setName("Sub One");
        when(service.subDistrictsByDistrict(4L)).thenReturn(List.of(s));

        mockMvc.perform(get("/api/v1/geo/districts/4/sub-districts")
                        .with(user("test-user")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Sub One"));
    }

    @Test
    void villages_qAndLimit_passedToService() throws Exception {
        GeoVillage v = new GeoVillage();
        v.setId(5L);
        v.setSubDistrictId(6L);
        v.setCode("V1");
        v.setName("Village One");
        when(service.villages(eq(6L), eq("vil"), eq(10))).thenReturn(List.of(v));

        mockMvc.perform(get("/api/v1/geo/sub-districts/6/villages")
                        .param("q", "vil")
                        .param("limit", "10")
                        .with(user("test-user")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Village One"));

        verify(service).villages(eq(6L), eq("vil"), eq(10));
    }

    @Test
    void blocksByDistrict_returnsList() throws Exception {
        GeoBlock b = new GeoBlock();
        b.setId(7L);
        b.setDistrictId(8L);
        b.setCode("B1");
        b.setName("Block One");
        when(service.blocksByDistrict(8L)).thenReturn(List.of(b));

        mockMvc.perform(get("/api/v1/geo/districts/8/blocks")
                        .with(user("test-user")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Block One"));
    }

    @Test
    void gramPanchayatsByVillage_returnsList() throws Exception {
        LgdLocalBody lb = new LgdLocalBody();
        lb.setId(9L);
        lb.setCode("GP1");
        lb.setName("GP One");
        lb.setBodyCategory("PRI");
        when(service.gramPanchayatsByVillage(10L)).thenReturn(List.of(lb));

        mockMvc.perform(get("/api/v1/geo/villages/10/gram-panchayats")
                        .with(user("test-user")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("GP One"));
    }

    @Test
    void unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/geo/states/2/districts"))
                .andExpect(status().isUnauthorized());
    }
}
