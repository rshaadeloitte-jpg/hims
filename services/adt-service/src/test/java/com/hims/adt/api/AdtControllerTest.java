package com.hims.adt.api;

import com.hims.adt.application.AdmissionService;
import com.hims.adt.application.MasterDataService;
import com.hims.adt.application.exceptions.ActiveAdmissionExistsException;
import com.hims.adt.application.exceptions.BedUnavailableException;
import com.hims.adt.configuration.SecurityConfig;
import com.hims.adt.domain.model.Admission;
import com.hims.adt.domain.model.Bed;
import com.hims.adt.domain.model.BedReservation;
import com.hims.adt.domain.model.BedTransferLog;
import com.hims.adt.domain.model.ClearanceChecklistItem;
import com.hims.adt.domain.model.ClearanceDepartment;
import com.hims.adt.domain.model.Ward;
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
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Contract tests for the ADT API (default-deny enforced). Services mocked; L0 covers the rules. */
@WebMvcTest({WardController.class, BedController.class, AdmissionController.class, BedReservationController.class,
        AttendantPassController.class, AdtOpsController.class})
@Import(SecurityConfig.class)
@AutoConfigureMockMvc
class AdtControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MasterDataService masterService;

    @MockitoBean
    private AdmissionService admissionService;

    private static Ward ward(long id) {
        Ward w = new Ward();
        w.setWardId(id);
        w.setName("General");
        w.setUnitId(1L);
        w.setIscensus(true);
        return w;
    }

    private static Bed bed(long id, long wardId) {
        Bed b = new Bed();
        b.setBedId(id);
        b.setBedNo("G-" + id);
        b.setWardId(wardId);
        b.setBedStatusId((short) 1);
        b.setOccupied(false);
        return b;
    }

    private static Admission admission(long id, long patientId, long bedId) {
        Admission a = new Admission();
        a.setAdmissionId(id);
        a.setPatientId(patientId);
        a.setBedId(bedId);
        a.setWardId(1L);
        a.setDoctorId(9L);
        a.setIpdNo("A/IHMS/27/" + String.format("%05d", id));
        a.setAdmissionType("ROUTINE");
        a.setAdmittedAt(Timestamp.valueOf("2027-03-01 08:00:00"));
        a.setIsCancel(false);
        return a;
    }

    @Test
    void endpoints_requireAuth_defaultDeny() throws Exception {
        mockMvc.perform(get("/api/v1/admissions")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/beds")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/wards")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/bed-reservations")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"bedId\":1,\"patientId\":1,"
                                + "\"expiresAt\":\"2027-03-02T00:00:00Z\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void ward_crud_roundTrip() throws Exception {
        when(masterService.createWard(eq(1L), eq("General"), eq("ICU"), any()))
                .thenReturn(ward(3L));

        mockMvc.perform(post("/api/v1/wards")
                        .with(user("devops").roles("DEV"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"unitId\":1,\"name\":\"General\",\"wardType\":\"ICU\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.wardId").value(3));

        when(masterService.listWards(1L)).thenReturn(List.of(ward(3L)));
        mockMvc.perform(get("/api/v1/wards").with(user("devops").roles("DEV")).param("unitId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("General"));
    }

    @Test
    void beds_available_returnsFreeBeds_andCleaningCompleteIsExposed() throws Exception {
        when(masterService.availableBeds(2L)).thenReturn(List.of(bed(7L, 2L), bed(8L, 2L)));

        mockMvc.perform(get("/api/v1/beds/available")
                        .with(user("devops").roles("DEV")).param("wardId", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].bedId").value(7));

        Bed cleaned = bed(9L, 2L);
        when(admissionService.markCleaningComplete(9L)).thenReturn(cleaned);
        mockMvc.perform(post("/api/v1/beds/9/cleaning-complete").with(user("devops").roles("DEV")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bedId").value(9));
    }

    // ------------------------------------------------------------- admit

    @Test
    void admit_valid_returns201WithIpdNo() throws Exception {
        Admission saved = admission(10L, 501L, 7L);
        when(admissionService.admit(eq(501L), isNull(), eq(11L), eq("ROUTINE"), eq(1L), isNull(),
                isNull(), isNull(), isNull(), isNull(), isNull()))
                .thenReturn(saved);

        mockMvc.perform(post("/api/v1/admissions")
                        .with(user("devops").roles("DEV"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"patientId\":501,\"doctorId\":11,\"admissionType\":\"ROUTINE\",\"unitId\":1}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ipdNo").value("A/IHMS/27/00010"))
                .andExpect(jsonPath("$.bedId").value(7));
    }

    @Test
    void admit_mlcBody_isForwardedToTheService() throws Exception {
        Admission saved = admission(11L, 502L, 8L);
        when(admissionService.admit(eq(502L), isNull(), eq(11L), eq("EMERGENCY"), eq(1L), isNull(),
                eq(true), eq("MLC-77"), isNull(), eq(12L), any()))
                .thenReturn(saved);

        mockMvc.perform(post("/api/v1/admissions")
                        .with(user("devops").roles("DEV"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"patientId\":502,\"doctorId\":11,\"admissionType\":\"EMERGENCY\","
                                + "\"unitId\":1,\"isMlc\":true,\"mlcNumber\":\"MLC-77\","
                                + "\"attendingDoctorId\":12,\"remarks\":\"police case\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.admissionId").value(11));
    }

    @Test
    void admit_duplicateActive_returns409Berb013() throws Exception {
        when(admissionService.admit(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenThrow(new ActiveAdmissionExistsException("BERB013", "Patient already has an active admission"));

        mockMvc.perform(post("/api/v1/admissions")
                        .with(user("devops").roles("DEV"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"patientId\":501,\"doctorId\":11,\"admissionType\":\"ROUTINE\",\"unitId\":1}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("BERB013"))
                .andExpect(jsonPath("$.status").value("ERROR"));
    }

    @Test
    void admit_missingDoctor_returns400Bera022() throws Exception {
        when(admissionService.admit(any(), any(), isNull(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenThrow(new com.hims.adt.application.exceptions.InvalidAdtRequestException(
                        "BERA022", "Admitting doctor is required"));

        mockMvc.perform(post("/api/v1/admissions")
                        .with(user("devops").roles("DEV"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"patientId\":501,\"admissionType\":\"ROUTINE\",\"unitId\":1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("BERA022"));
    }

    // ------------------------------------------------------------- transfer

    @Test
    void transfer_casConflict_returns409Bexb001() throws Exception {
        when(admissionService.transfer(eq(10L), eq(12L), any()))
                .thenThrow(new BedUnavailableException("BEXB001", "Bed 12 could not be claimed"));

        mockMvc.perform(post("/api/v1/admissions/10/transfer")
                        .with(user("devops").roles("DEV"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"toBedId\":12}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("BEXB001"));
    }

    @Test
    void transfers_history_returnsTransferLog() throws Exception {
        BedTransferLog logRow = new BedTransferLog();
        logRow.setTransferId(5L);
        logRow.setAdmissionId(10L);
        logRow.setFromBedId(7L);
        logRow.setToBedId(12L);
        logRow.setFromWardId(1L);
        logRow.setToWardId(3L);
        when(admissionService.transferHistory(10L)).thenReturn(List.of(logRow));

        mockMvc.perform(get("/api/v1/admissions/10/transfers").with(user("devops").roles("DEV")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].fromBedId").value(7))
                .andExpect(jsonPath("$[0].toBedId").value(12));
    }

    // ------------------------------------------------------------- discharge

    @Test
    void discharge_returnsDischargedAdmission_withDeathFields() throws Exception {
        Admission d = admission(10L, 501L, 7L);
        d.setDischargeType("EXPIRED");
        d.setIsExpired(true);
        d.setDeathDate(Timestamp.from(Instant.parse("2027-03-03T06:30:00Z")));
        d.setDischargeDate(Timestamp.valueOf("2027-03-03 09:00:00"));
        d.setDischargeId(55L);
        when(admissionService.discharge(eq(10L), eq("EXPIRED"), eq(true), any(), isNull(), isNull(), isNull())).thenReturn(d);

        mockMvc.perform(post("/api/v1/admissions/10/discharge")
                        .with(user("devops").roles("DEV"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dischargeType\":\"EXPIRED\",\"isExpired\":true,"
                                + "\"deathDate\":\"2027-03-03T06:30:00Z\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dischargeId").value(55))
                .andExpect(jsonPath("$.deathDate").isNotEmpty());
    }

    // ------------------------------------------------------------- abscond

    @Test
    void abscond_endpoint_marksAbsconded() throws Exception {
        Admission a = admission(10L, 501L, 0L);
        a.setAbscondDatetime(Timestamp.valueOf("2027-03-02 23:00:00"));
        a.setDischargeType("ABSCOND");
        when(admissionService.abscond(eq(10L), isNull())).thenReturn(a);

        mockMvc.perform(post("/api/v1/admissions/10/abscond").with(user("devops").roles("DEV")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.abscondDatetime").isNotEmpty())
                .andExpect(jsonPath("$.dischargeType").value("ABSCOND"));
    }

    // ------------------------------------------------------------- clearance

    @Test
    void clearance_listAndClearDepartment() throws Exception {
        ClearanceChecklistItem pending = new ClearanceChecklistItem();
        pending.setClearanceId(1L);
        pending.setAdmissionId(10L);
        pending.setDepartment(ClearanceDepartment.PHARMACY);
        pending.setClrStatus(ClearanceChecklistItem.ClrStatus.PENDING);
        when(admissionService.clearances(10L)).thenReturn(List.of(pending));

        mockMvc.perform(get("/api/v1/admissions/10/clearance").with(user("devops").roles("DEV")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].department").value("PHARMACY"))
                .andExpect(jsonPath("$[0].clrStatus").value("PENDING"));

        ClearanceChecklistItem cleared = new ClearanceChecklistItem();
        cleared.setClearanceId(1L);
        cleared.setAdmissionId(10L);
        cleared.setDepartment(ClearanceDepartment.PHARMACY);
        cleared.setClrStatus(ClearanceChecklistItem.ClrStatus.CLEARED);
        cleared.setClearedBy(42L);
        cleared.setClearedAt(Timestamp.valueOf("2027-03-01 09:00:00"));
        when(admissionService.clearDepartment(eq(10L), eq("PHARMACY"), eq(42L), isNull()))
                .thenReturn(cleared);

        mockMvc.perform(post("/api/v1/admissions/10/clearance/PHARMACY")
                        .with(user("devops").roles("DEV"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clearedBy\":42}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clrStatus").value("CLEARED"))
                .andExpect(jsonPath("$.clearedBy").value(42));
    }

    // ------------------------------------------------------------- reservations

    @Test
    void reserve_createsReservation_201() throws Exception {
        BedReservation r = new BedReservation();
        r.setReservationId(9L);
        r.setBedId(15L);
        r.setPatientId(501L);
        r.setReservedAt(new Timestamp(System.currentTimeMillis()));
        r.setExpiresAt(Timestamp.from(Instant.parse("2027-03-02T18:00:00Z")));
        r.setResStatus(BedReservation.ResStatus.ACTIVE);
        when(admissionService.reserve(eq(15L), eq(501L),
                eq(Timestamp.from(Instant.parse("2027-03-02T18:00:00Z")))))
                .thenReturn(r);

        mockMvc.perform(post("/api/v1/bed-reservations")
                        .with(user("devops").roles("DEV"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bedId\":15,\"patientId\":501,\"expiresAt\":\"2027-03-02T18:00:00Z\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reservationId").value(9))
                .andExpect(jsonPath("$.resStatus").value("ACTIVE"));
    }

    @Test
    void cancelReservation_returns204() throws Exception {
        mockMvc.perform(delete("/api/v1/bed-reservations/9").with(user("devops").roles("DEV")))
                .andExpect(status().isNoContent());
        verify(admissionService).cancelReservation(9L);
    }

    // ------------------------------------------------------------- M03 interim / attendant / census / tid / death

    @Test
    void interim_returns201() throws Exception {
        Admission a = admission(20L, 501L, 0L);
        a.setBedId(null); a.setWardId(null); a.setIsInterim(true); a.setTid("TID123");
        when(admissionService.admitInterim(eq(501L), any(), eq(11L), any(), eq(1L), any(), any(), any(), any(), any(), eq("TID123")))
                .thenReturn(a);
        mockMvc.perform(post("/api/v1/admissions/interim").with(user("devops").roles("DEV"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"patientId\":501,\"doctorId\":11,\"unitId\":1,\"tid\":\"TID123\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.isInterim").value(true))
                .andExpect(jsonPath("$.tid").value("TID123"));
    }

    @Test
    void issueAttendantPass_returns201() throws Exception {
        var p = new com.hims.adt.domain.model.AttendantPass();
        p.setPassId(1L); p.setPassNo("ATT/26/00001"); p.setPassStatus("ISSUED"); p.setAttendeeName("Ram");
        when(admissionService.issueAttendantPass(eq(10L), eq("Ram"), any())).thenReturn(p);
        mockMvc.perform(post("/api/v1/admissions/10/attendant-pass").with(user("devops").roles("DEV"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"attendeeName\":\"Ram\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.passNo").value("ATT/26/00001"));
    }

    @Test
    void listAttendantPasses_returnsList() throws Exception {
        var p = new com.hims.adt.domain.model.AttendantPass();
        p.setPassId(1L); p.setPassNo("ATT/26/00001");
        when(admissionService.attendantPasses(10L)).thenReturn(List.of(p));
        mockMvc.perform(get("/api/v1/admissions/10/attendant-passes").with(user("devops").roles("DEV")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].passNo").value("ATT/26/00001"));
    }

    @Test
    void returnAttendantPass_returnsReturned() throws Exception {
        var p = new com.hims.adt.domain.model.AttendantPass();
        p.setPassId(5L); p.setPassStatus("RETURNED");
        when(admissionService.returnAttendantPass(5L)).thenReturn(p);
        mockMvc.perform(post("/api/v1/attendant-passes/5/return").with(user("devops").roles("DEV")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.passStatus").value("RETURNED"));
    }

    @Test
    void census_returnsMap() throws Exception {
        when(admissionService.census(any())).thenReturn(Map.of("wards", List.of(), "totalActiveAdmissions", 0));
        mockMvc.perform(get("/api/v1/adt/census").with(user("devops").roles("DEV")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.wards").isArray());
    }

    @Test
    void tidList_returnsAdmissions() throws Exception {
        Admission a = admission(10L, 501L, 7L); a.setTid("TID123");
        when(admissionService.tidList()).thenReturn(List.of(a));
        mockMvc.perform(get("/api/v1/adt/tids").with(user("devops").roles("DEV")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tid").value("TID123"));
    }

    @Test
    void deaths_returnsList() throws Exception {
        Admission a = admission(10L, 501L, 7L); a.setIsExpired(true); a.setDeathCause("Cardiac");
        when(admissionService.deathList()).thenReturn(List.of(a));
        mockMvc.perform(get("/api/v1/adt/deaths").with(user("devops").roles("DEV")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].isExpired").value(true));
    }

    @Test
    void setTid_returnsUpdated() throws Exception {
        Admission a = admission(10L, 501L, 7L); a.setTid("NEWTID");
        when(admissionService.setTid(eq(10L), eq("NEWTID"))).thenReturn(a);
        mockMvc.perform(post("/api/v1/admissions/10/tid").with(user("devops").roles("DEV"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tid\":\"NEWTID\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tid").value("NEWTID"));
    }
}
