package com.hims.appointment.api;

import com.hims.appointment.application.AppointmentService;
import com.hims.appointment.application.EncounterService;
import com.hims.appointment.application.exceptions.AppointmentNotFoundException;
import com.hims.appointment.application.exceptions.InvalidAppointmentRequestException;
import com.hims.appointment.application.exceptions.SlotAlreadyBookedException;
import com.hims.appointment.configuration.SecurityConfig;
import com.hims.appointment.domain.model.Appointment;
import com.hims.appointment.domain.model.AppointmentStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.introspection.OpaqueTokenIntrospector;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * L1 — controller contract tests (MIG-031): default-deny 401 without a Bearer
 * token, authenticated paths via JWT principal, legacy field parity of the
 * appointment envelope, BEXA003/NOT_FOUND/APPT_SLOT_TAKEN error envelopes and
 * the correlation-id echo.
 */
@WebMvcTest({AppointmentController.class, DoctorScheduleController.class, EncounterController.class})
@Import({SecurityConfig.class, AppointmentControllerTest.JwtDecoderStub.class})
@AutoConfigureMockMvc
class AppointmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AppointmentService service;

    @MockitoBean
    private EncounterService encounterService;

    /** Resource-server chain needs a JwtDecoder bean; the stub never fetches at startup. */
    @TestConfiguration
    static class JwtDecoderStub {
        @Bean
        NimbusJwtDecoder jwtDecoder() {
            return NimbusJwtDecoder.withJwkSetUri("http://localhost:0/dummy-jwks").build();
        }
    }

    private static Appointment appt(long id, int token) {
        Appointment a = new Appointment();
        a.setId(id);
        a.setTokenNo(token);
        a.setPatientId(501L);
        a.setDoctorId(11L);
        a.setScheduleId(3L);
        a.setApptDate(LocalDate.of(2027, 3, 1));
        a.setSlotStart(LocalTime.of(9, 30));
        a.setSlotEnd(LocalTime.of(10, 0));
        a.setApptStatus(AppointmentStatus.BOOKED.code());
        a.setStatus(true);
        return a;
    }

    // ------------------------------------------------------------------ security

    @Test
    void defaultDeny_bookWithoutBearer_401() throws Exception {
        mockMvc.perform(post("/api/v1/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void defaultDeny_allBusinessEndpointsWithoutBearer_401() throws Exception {
        mockMvc.perform(get("/api/v1/appointments").param("date", "2027-03-01"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/appointments/1")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/appointments/1/checkin")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/doctors/11/slots").param("date", "2027-03-01"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/encounters/1")).andExpect(status().isUnauthorized());
    }

    @Test
    void bearerToken_grantsAccess_andCorrelationIdIsEchoed() throws Exception {
        when(service.get(1L)).thenReturn(appt(1L, 1));
        mockMvc.perform(get("/api/v1/appointments/1")
                        .with(jwt())
                        .header("X-Request-ID", "corr-test-123"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Request-ID", "corr-test-123"));
    }

    @Test
    void missingCorrelationId_generatesEchoedUuid() throws Exception {
        when(service.get(1L)).thenReturn(appt(1L, 1));
        mockMvc.perform(get("/api/v1/appointments/1").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Request-ID"));
    }

    // ------------------------------------------------------------------ booking contract

    @Test
    void book_validBearer_returns201_withLegacyFieldParity() throws Exception {
        when(service.book(any())).thenReturn(appt(1L, 2));

        mockMvc.perform(post("/api/v1/appointments")
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"patientId":501,"doctorId":11,"scheduleId":3,
                                 "apptDate":"2027-03-01","slotStart":"09:30"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.tokenNo").value(2))
                .andExpect(jsonPath("$.apptStatus").value(1))       // BOOKED (locked dictionary)
                .andExpect(jsonPath("$.patientId").value(501))
                .andExpect(jsonPath("$.doctorId").value(11))
                .andExpect(jsonPath("$.scheduleId").value(3))
                .andExpect(jsonPath("$.apptDate").value("2027-03-01"))
                .andExpect(jsonPath("$.slotStart").value("09:30:00"))
                .andExpect(jsonPath("$.slotEnd").value("10:00:00"))
                .andExpect(jsonPath("$.status").value(true));
    }

    @Test
    void book_missingFields_returns400Bexa003Envelope() throws Exception {
        mockMvc.perform(post("/api/v1/appointments")
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"doctorId\":11}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("ERROR"))
                .andExpect(jsonPath("$.errorCode").value("BEXA003"))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void book_malformedBody_returns400Bexa003() throws Exception {
        mockMvc.perform(post("/api/v1/appointments")
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{not-json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("BEXA003"));
    }

    @Test
    void book_slotTaken_returns409ConflictEnvelope() throws Exception {
        when(service.book(any()))
                .thenThrow(new SlotAlreadyBookedException("APPT_SLOT_TAKEN", "Slot already booked"));

        mockMvc.perform(post("/api/v1/appointments")
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"patientId":501,"doctorId":11,"scheduleId":3,
                                 "apptDate":"2027-03-01","slotStart":"09:30"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("APPT_SLOT_TAKEN"))
                .andExpect(jsonPath("$.status").value("ERROR"));
    }

    @Test
    void get_unknownId_returns404NotFoundEnvelope() throws Exception {
        when(service.get(404L))
                .thenThrow(new AppointmentNotFoundException("NOT_FOUND", "Appointment not found: 404"));

        mockMvc.perform(get("/api/v1/appointments/404").with(jwt()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("NOT_FOUND"))
                .andExpect(jsonPath("$.status").value("ERROR"));
    }

    // ------------------------------------------------------------------ lifecycle contract

    @Test
    void checkin_returnsCheckedInWithEncounterLink() throws Exception {
        Appointment checkedIn = appt(1L, 2);
        checkedIn.transitionTo(AppointmentStatus.CHECKED_IN);
        checkedIn.setEncounterId(55L);
        when(encounterService.checkIn(1L)).thenReturn(null);
        when(service.get(1L)).thenReturn(checkedIn);

        mockMvc.perform(post("/api/v1/appointments/1/checkin").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.apptStatus").value(2))     // CHECKED_IN
                .andExpect(jsonPath("$.encounterId").value(55));
    }

    @Test
    void checkin_illegalTransition_returns400EnvelopeNot500() throws Exception {
        when(encounterService.checkIn(1L)).thenThrow(new InvalidAppointmentRequestException(
                "APPT_ILLEGAL_TRANSITION", "checkin requires status BOOKED, current=COMPLETED"));

        mockMvc.perform(post("/api/v1/appointments/1/checkin").with(jwt()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("APPT_ILLEGAL_TRANSITION"));
    }

    @Test
    void cancel_noshow_reschedule_contractShapes() throws Exception {
        Appointment cancelled = appt(1L, 2);
        cancelled.transitionTo(AppointmentStatus.CANCELLED);
        when(service.cancel(1L)).thenReturn(cancelled);
        mockMvc.perform(post("/api/v1/appointments/1/cancel").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.apptStatus").value(4));    // CANCELLED

        Appointment noShow = appt(1L, 2);
        noShow.transitionTo(AppointmentStatus.NO_SHOW);
        when(service.noshow(1L)).thenReturn(noShow);
        mockMvc.perform(post("/api/v1/appointments/1/noshow").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.apptStatus").value(6));    // NO_SHOW

        when(service.reschedule(eq(1L), any())).thenReturn(appt(9L, 7));
        mockMvc.perform(post("/api/v1/appointments/1/reschedule").with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"patientId":501,"doctorId":11,"scheduleId":3,
                                 "apptDate":"2027-03-08","slotStart":"09:30"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(9))
                .andExpect(jsonPath("$.tokenNo").value(7));
    }

    // ------------------------------------------------------------------ listing + schedules + slots

    @Test
    void list_byDateAndDoctor_legacyFieldParity() throws Exception {
        when(service.listByDateAndDoctor(LocalDate.of(2027, 3, 1), 11L))
                .thenReturn(List.of(appt(1L, 1)));

        mockMvc.perform(get("/api/v1/appointments")
                        .with(jwt())
                        .param("date", "2027-03-01")
                        .param("doctorId", "11"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].apptDate").value("2027-03-01"))
                .andExpect(jsonPath("$[0].slotStart").value("09:30:00"));
    }

    @Test
    void list_missingDateParam_returns400Bexa003() throws Exception {
        mockMvc.perform(get("/api/v1/appointments").with(jwt()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("BEXA003"));
    }

    @Test
    void slots_contractShape_includesBookedFlagAndCabin() throws Exception {
        when(service.findSlots(11L, LocalDate.of(2027, 3, 1))).thenReturn(List.of(
                new java.util.LinkedHashMap<>(java.util.Map.of(
                        "scheduleId", 3L, "doctorId", 11L, "date", "2027-03-01",
                        "slotStart", "09:30", "slotEnd", "10:00",
                        "booked", false, "capacity", 1, "bookedCount", 0))));
        when(service.listSchedules(isNull())).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/doctors/11/slots")
                        .with(jwt()).param("date", "2027-03-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].scheduleId").value(3))
                .andExpect(jsonPath("$[0].booked").value(false))
                .andExpect(jsonPath("$[0].slotEnd").value("10:00"));

        mockMvc.perform(post("/api/v1/doctors/11/schedules").with(jwt())
                        .param("unitId", "1").param("dayOfWeek", "1")
                        .param("startTime", "09:00").param("endTime", "12:00"))
                .andExpect(status().isCreated());
    }
}
