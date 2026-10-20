package com.hims.appointment.api;

import com.hims.appointment.application.FollowUpVisitService;
import com.hims.appointment.application.OpdCardService;
import com.hims.appointment.application.QuestionnaireService;
import com.hims.appointment.application.TelemedicineService;
import com.hims.appointment.application.exceptions.InvalidAppointmentRequestException;
import com.hims.appointment.configuration.SecurityConfig;
import com.hims.appointment.domain.model.FollowUpVisit;
import com.hims.appointment.domain.model.Questionnaire;
import com.hims.appointment.domain.model.TelemedicineRoom;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({FollowUpVisitController.class, TelemedicineController.class,
        QuestionnaireController.class, OpdCardController.class})
@Import({SecurityConfig.class, M02ControllerTest.JwtDecoderStub.class})
@AutoConfigureMockMvc
class M02ControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private FollowUpVisitService followUpService;
    @MockitoBean private TelemedicineService telemedicineService;
    @MockitoBean private QuestionnaireService questionnaireService;
    @MockitoBean private OpdCardService opdCardService;

    @TestConfiguration
    static class JwtDecoderStub {
        @Bean
        NimbusJwtDecoder jwtDecoder() {
            return NimbusJwtDecoder.withJwkSetUri("http://localhost:0/dummy-jwks").build();
        }
    }

    // ---- Follow-up ----

    @Test
    void scheduleFollowUp_validInput_returns201() throws Exception {
        FollowUpVisit f = new FollowUpVisit();
        f.setId(100L);
        f.setPatientId(501L);
        f.setDoctorId(11L);
        f.setDepartmentId(1L);
        f.setFollowUpDate(java.time.LocalDate.of(2027, 4, 1));
        f.setFollowUpStatus((short) 1);
        f.setStatus(true);
        f.setSynchronizedd(false);
        when(followUpService.scheduleFollowUp(anyLong(), any(), any(), any(), any(),
                any(), any(), any(), any(), any(), any(), any())).thenReturn(f);

        mockMvc.perform(post("/api/v1/appointments/1/follow-up")
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"patientId\":501,\"doctorId\":11,\"followUpDate\":\"2027-04-01\",\"departmentId\":1,\"unitId\":1}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.patientId").value(501))
                .andExpect(jsonPath("$.followUpStatus").value(1));
    }

    @Test
    void scheduleFollowUp_withoutBearer_401() throws Exception {
        mockMvc.perform(post("/api/v1/appointments/1/follow-up")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    // ---- Telemedicine ----

    @Test
    void createTelemedRoom_returns201() throws Exception {
        TelemedicineRoom room = new TelemedicineRoom();
        room.setId(200L);
        room.setRoomId("abc123");
        room.setVideoLink("https://tele.ihms.example/stub/abc123");
        room.setRoomStatus((short) 1);
        when(telemedicineService.createRoom(1L)).thenReturn(room);

        mockMvc.perform(post("/api/v1/appointments/1/telemedicine").with(jwt()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.roomId").value("abc123"))
                .andExpect(jsonPath("$.roomStatus").value(1));
    }

    @Test
    void getTelemedRoom_returnsRoom() throws Exception {
        TelemedicineRoom room = new TelemedicineRoom();
        room.setId(200L);
        room.setRoomId("abc123");
        when(telemedicineService.getByAppointment(1L)).thenReturn(room);

        mockMvc.perform(get("/api/v1/appointments/1/telemedicine").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roomId").value("abc123"));
    }

    // ---- Questionnaire ----

    @Test
    void submitQuestionnaire_returns201() throws Exception {
        Questionnaire q = new Questionnaire();
        q.setId(300L);
        q.setPatientId(501L);
        q.setModule("Patient Assessment");
        when(questionnaireService.submitAnswers(anyLong(), any(), any(), any(),
                any(), any(), any(), any())).thenReturn(q);

        mockMvc.perform(post("/api/v1/appointments/1/questionnaire")
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"patientId\":501,\"module\":\"Patient Assessment\",\"answers\":\"[]\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(300))
                .andExpect(jsonPath("$.module").value("Patient Assessment"));
    }

    @Test
    void getQuestionnaire_returnsQuestionnaire() throws Exception {
        Questionnaire q = new Questionnaire();
        q.setId(300L);
        q.setModule("Patient Assessment");
        when(questionnaireService.getByAppointmentAndModule(1L, "Patient Assessment"))
                .thenReturn(Optional.of(q));

        mockMvc.perform(get("/api/v1/appointments/1/questionnaire").with(jwt())
                        .param("module", "Patient Assessment"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(300));
    }

    @Test
    void getQuestionnaire_notFound_returns400() throws Exception {
        when(questionnaireService.getByAppointmentAndModule(1L, "Patient Assessment"))
                .thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/appointments/1/questionnaire").with(jwt())
                        .param("module", "Patient Assessment"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("NOT_FOUND"));
    }

    // ---- OPD Card ----

    @Test
    void getOpdCardJson_returnsData() throws Exception {
        Map<String, Object> card = new LinkedHashMap<>();
        card.put("appointmentId", 1L);
        card.put("patientId", 501L);
        card.put("tokenNo", 5);
        when(opdCardService.generateOpdCard(1L)).thenReturn(card);

        mockMvc.perform(get("/api/v1/appointments/1/opd-card").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.appointmentId").value(1))
                .andExpect(jsonPath("$.patientId").value(501))
                .andExpect(jsonPath("$.tokenNo").value(5));
    }

    @Test
    void getOpdCardHtml_returnsHtml() throws Exception {
        when(opdCardService.generateOpdCardHtml(1L)).thenReturn("<html>OPD Card</html>");

        mockMvc.perform(get("/api/v1/appointments/1/opd-card/html").with(jwt()))
                .andExpect(status().isOk());
    }
}
