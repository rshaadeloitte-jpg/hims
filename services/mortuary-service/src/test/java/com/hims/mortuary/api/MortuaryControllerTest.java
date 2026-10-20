package com.hims.mortuary.api;

import com.hims.mortuary.application.MortuaryService;
import com.hims.mortuary.application.exceptions.InvalidMortuaryRequestException;
import com.hims.mortuary.application.exceptions.MortuaryConflictException;
import com.hims.mortuary.application.exceptions.MortuaryEntityNotFoundException;
import com.hims.mortuary.configuration.SecurityConfig;
import com.hims.mortuary.domain.model.AutopsyRequest;
import com.hims.mortuary.domain.model.DeadBody;
import com.hims.mortuary.domain.model.ReleaseCertificate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Contract tests for the Mortuary API (default-deny enforced). Services mocked.
 */
@WebMvcTest(MortuaryController.class)
@Import(SecurityConfig.class)
@AutoConfigureMockMvc
class MortuaryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MortuaryService mortuaryService;

    private static DeadBody body(Long id, String status) {
        DeadBody b = new DeadBody();
        b.setId(id);
        b.setAdmissionId(100L + id);
        b.setPatientId(200L + id);
        b.setPatientName("Patient " + id);
        b.setDeathDate(Timestamp.from(Instant.parse("2027-06-15T08:00:00Z")));
        b.setBodyStatus(status);
        b.setCauseOfDeath("Cardiac arrest");
        b.setDeathLocation("ICU");
        b.setNextOfKin("Relative " + id);
        return b;
    }

    private static AutopsyRequest autopsy(Long id, short status) {
        AutopsyRequest r = new AutopsyRequest();
        r.setId(id);
        r.setDeadBodyId(1L);
        r.setRequestStatus(status);
        r.setRequestedBy(5L);
        return r;
    }

    private static ReleaseCertificate cert(Long id, short status) {
        ReleaseCertificate c = new ReleaseCertificate();
        c.setId(id);
        c.setReleaseNo("MOR-RC-" + id);
        c.setDeadBodyId(1L);
        c.setReleaseStatus(status);
        c.setHandoverTo("Jane Doe");
        return c;
    }

    @Test
    void createDeadBody_201() throws Exception {
        DeadBody b = body(1L, DeadBody.STATUS_ADMITTED);
        when(mortuaryService.createDeadBody(eq(101L), eq(201L), eq("John Doe"),
                any(Timestamp.class), eq("Cardiac arrest"), eq("ICU"),
                eq("Jane Doe"), eq("Spouse"), eq("9876543210"),
                eq("CH-01"), eq(false), eq(null), eq(null), eq(1L)))
                .thenReturn(b);

        mockMvc.perform(post("/api/v1/mortuary/cases").with(user("devops").roles("DEV"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"admissionId":101,"patientId":201,"patientName":"John Doe",
                                 "deathDate":"2027-06-15T08:00:00Z","causeOfDeath":"Cardiac arrest",
                                 "deathLocation":"ICU","nextOfKin":"Jane Doe",
                                 "nextOfKinRelation":"Spouse","nextOfKinPhone":"9876543210",
                                 "chamberNo":"CH-01","unitId":1}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.bodyStatus").value("ADMITTED"))
                .andExpect(jsonPath("$.admissionId").value(101));
    }

    @Test
    void createDeadBody_missingAdmissionId_400() throws Exception {
        when(mortuaryService.createDeadBody(any(), any(), any(), any(), any(), any(),
                any(), any(), any(), any(), any(), any(), any(), any()))
                .thenThrow(new InvalidMortuaryRequestException("MOR_REQ_INVALID", "admissionId is required"));

        mockMvc.perform(post("/api/v1/mortuary/cases").with(user("devops").roles("DEV"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"patientId":201,"patientName":"John","deathDate":"2027-06-15T08:00:00Z"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("MOR_REQ_INVALID"));
    }

    @Test
    void getDeadBody_200() throws Exception {
        DeadBody b = body(1L, DeadBody.STATUS_ADMITTED);
        when(mortuaryService.getDeadBody(1L)).thenReturn(b);

        mockMvc.perform(get("/api/v1/mortuary/cases/1").with(user("devops").roles("DEV")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.patientName").value("Patient 1"));
    }

    @Test
    void getDeadBody_notFound_404() throws Exception {
        when(mortuaryService.getDeadBody(99L))
                .thenThrow(new MortuaryEntityNotFoundException("MOR_NOT_FOUND", "Dead body 99 not found"));

        mockMvc.perform(get("/api/v1/mortuary/cases/99").with(user("devops").roles("DEV")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("MOR_NOT_FOUND"));
    }

    @Test
    void listDeadBodies_200() throws Exception {
        when(mortuaryService.listDeadBodies(null, null)).thenReturn(List.of(body(1L, DeadBody.STATUS_ADMITTED)));

        mockMvc.perform(get("/api/v1/mortuary/cases").with(user("devops").roles("DEV")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    void updateDeadBody_200() throws Exception {
        DeadBody b = body(1L, DeadBody.STATUS_ADMITTED);
        b.setCauseOfDeath("Stroke");
        when(mortuaryService.updateDeadBody(eq(1L), eq("Stroke"), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(b);

        mockMvc.perform(put("/api/v1/mortuary/cases/1").with(user("devops").roles("DEV"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"causeOfDeath":"Stroke"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.causeOfDeath").value("Stroke"));
    }

    @Test
    void updateDeadBody_alreadyReleased_409() throws Exception {
        when(mortuaryService.updateDeadBody(eq(1L), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenThrow(new MortuaryConflictException("MOR_STATE", "Dead body 1 is already released"));

        mockMvc.perform(put("/api/v1/mortuary/cases/1").with(user("devops").roles("DEV"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"causeOfDeath":"Stroke"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("MOR_STATE"));
    }

    @Test
    void releaseBody_200() throws Exception {
        ReleaseCertificate c = cert(50L, ReleaseCertificate.STATUS_RELEASED);
        when(mortuaryService.releaseBody(eq(1L), eq("Jane Doe"), eq("Spouse"), eq("9876543210"), eq(null)))
                .thenReturn(c);

        mockMvc.perform(post("/api/v1/mortuary/cases/1/release").with(user("devops").roles("DEV"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"handoverTo":"Jane Doe","handoverRelation":"Spouse","handoverPhone":"9876543210"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.releaseNo").value("MOR-RC-50"))
                .andExpect(jsonPath("$.releaseStatus").value(3));
    }

    @Test
    void releaseBody_alreadyReleased_409() throws Exception {
        when(mortuaryService.releaseBody(eq(1L), any(), any(), any(), any()))
                .thenThrow(new MortuaryConflictException("MOR_STATE", "Dead body 1 is already released"));

        mockMvc.perform(post("/api/v1/mortuary/cases/1/release").with(user("devops").roles("DEV"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"handoverTo":"Someone"}
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void requestAutopsy_200() throws Exception {
        AutopsyRequest r = autopsy(10L, AutopsyRequest.STATUS_PENDING);
        when(mortuaryService.requestAutopsy(eq(1L), eq(5L), eq("Unknown cause")))
                .thenReturn(r);

        mockMvc.perform(post("/api/v1/mortuary/cases/1/autopsy-request").with(user("devops").roles("DEV"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"requestedBy":5,"reason":"Unknown cause"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestStatus").value(1))
                .andExpect(jsonPath("$.deadBodyId").value(1));
    }

    @Test
    void requestAutopsy_releasedBody_409() throws Exception {
        when(mortuaryService.requestAutopsy(eq(1L), any(), any()))
                .thenThrow(new MortuaryConflictException("MOR_STATE", "Cannot request autopsy for released body 1"));

        mockMvc.perform(post("/api/v1/mortuary/cases/1/autopsy-request").with(user("devops").roles("DEV"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"requestedBy":5,"reason":"Unknown cause"}
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void approveAutopsy_200() throws Exception {
        AutopsyRequest r = autopsy(10L, AutopsyRequest.STATUS_APPROVED);
        r.setDoctorId(7L);
        when(mortuaryService.approveAutopsy(eq(10L), eq(7L))).thenReturn(r);

        mockMvc.perform(post("/api/v1/mortuary/autopsy/10/approve").with(user("devops").roles("DEV"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"doctorId":7}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestStatus").value(2));
    }

    @Test
    void completeAutopsy_200() throws Exception {
        AutopsyRequest r = autopsy(10L, AutopsyRequest.STATUS_COMPLETED);
        when(mortuaryService.completeAutopsy(eq(10L), any(), eq("Findings")))
                .thenReturn(r);

        mockMvc.perform(post("/api/v1/mortuary/autopsy/10/complete").with(user("devops").roles("DEV"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"findings":"Findings"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestStatus").value(3));
    }

    @Test
    void rejectAutopsy_200() throws Exception {
        AutopsyRequest r = autopsy(10L, AutopsyRequest.STATUS_REJECTED);
        r.setRejectionReason("Not required");
        when(mortuaryService.rejectAutopsy(eq(10L), eq("Not required")))
                .thenReturn(r);

        mockMvc.perform(post("/api/v1/mortuary/autopsy/10/reject").with(user("devops").roles("DEV"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"rejectionReason":"Not required"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestStatus").value(4));
    }

    @Test
    void getReport_200() throws Exception {
        when(mortuaryService.getReport(null)).thenReturn(Map.of(
                "total", 5L, "admitted", 3L, "released", 1L,
                "autopsyPending", 1L, "autopsyComplete", 0L,
                "policeCase", 0L, "handedOver", 0L));

        mockMvc.perform(get("/api/v1/mortuary/report").with(user("devops").roles("DEV")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(5))
                .andExpect(jsonPath("$.admitted").value(3));
    }

    @Test
    void unauthenticated_is401_defaultDeny() throws Exception {
        mockMvc.perform(get("/api/v1/mortuary/cases"))
                .andExpect(status().isUnauthorized());
    }
}
