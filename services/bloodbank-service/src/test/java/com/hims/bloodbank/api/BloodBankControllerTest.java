package com.hims.bloodbank.api;

import com.hims.bloodbank.application.BagService;
import com.hims.bloodbank.application.DonorService;
import com.hims.bloodbank.application.JobService;
import com.hims.bloodbank.application.RequisitionService;
import com.hims.bloodbank.application.exceptions.BagConflictException;
import com.hims.bloodbank.application.exceptions.BloodBankEntityNotFoundException;
import com.hims.bloodbank.application.exceptions.InvalidBloodBankRequestException;
import com.hims.bloodbank.configuration.SecurityConfig;
import com.hims.bloodbank.domain.model.BloodBag;
import com.hims.bloodbank.domain.model.BloodDonor;
import com.hims.bloodbank.domain.model.BloodRequisition;
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
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Contract tests for the Blood Bank API (default-deny enforced). Services mocked; L1 covers the rules. */
@WebMvcTest({DonorController.class, BagController.class, RequisitionController.class, JobController.class})
@Import(SecurityConfig.class)
@AutoConfigureMockMvc
class BloodBankControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DonorService donorService;

    @MockitoBean
    private BagService bagService;

    @MockitoBean
    private RequisitionService requisitionService;

    @MockitoBean
    private JobService jobService;

    private static BloodDonor donor(long id) {
        BloodDonor d = new BloodDonor();
        d.setDonorId(id);
        d.setName("R. Kumar");
        d.setBloodGroup("O+");
        d.setDonationStatus(BloodDonor.DNR_NEW);
        d.setLastActionAt(Timestamp.from(Instant.parse("2027-06-15T08:00:00Z")));
        return d;
    }

    private static BloodRequisition req(long id, int status) {
        BloodRequisition r = new BloodRequisition();
        r.setRequisitionId(id);
        r.setPatientId(501L);
        r.setBloodGroup("O+");
        r.setOrderedVolumeMl(700);
        r.setRequesteddate(Timestamp.from(Instant.parse("2027-06-15T09:00:00Z")));
        r.setStatus(status);
        return r;
    }

    private static BloodBag bag(long id) {
        BloodBag b = new BloodBag();
        b.setBagId(id);
        b.setDonorId(1L);
        b.setBloodGroup("O+");
        b.setVolumeMl(350);
        b.setExpirydate(java.time.LocalDate.parse("2027-07-20"));
        return b;
    }

    @Test
    void createDonor_201() throws Exception {
        when(donorService.create(eq("R. Kumar"), eq("O+"), isNull(), isNull()))
                .thenReturn(donor(1L));

        mockMvc.perform(post("/api/v1/donors").with(user("devops").roles("DEV"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"R. Kumar","bloodGroup":"O+"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.donorId").value(1))
                .andExpect(jsonPath("$.donationStatus").value(1));
    }

    @Test
    void touchDonor_200() throws Exception {
        when(donorService.touch(1L)).thenReturn(donor(1L));

        mockMvc.perform(post("/api/v1/donors/1/touch").with(user("devops").roles("DEV")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.donorId").value(1));
    }

    @Test
    void createRequisition_201() throws Exception {
        when(requisitionService.create(eq(501L), eq("O+"), eq(700), any(), eq(72), any()))
                .thenReturn(req(10L, 1));

        mockMvc.perform(post("/api/v1/requisitions").with(user("devops").roles("DEV"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"patientId":501,"bloodGroup":"O+","orderedVolumeMl":700,
                                 "orderValidationHours":72,
                                 "components":[{"component":"PRBC","volumeMl":700}]}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.requisitionId").value(10))
                .andExpect(jsonPath("$.status").value(1));
    }

    @Test
    void crossmatchConflict_409() throws Exception {
        when(requisitionService.crossmatch(10L, 5L))
                .thenThrow(new BagConflictException("BLD_BAG_CONFLICT", "Bag 5 already crossmatched"));

        mockMvc.perform(post("/api/v1/requisitions/10/crossmatch").with(user("devops").roles("DEV"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"bagId":5}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("BLD_BAG_CONFLICT"));
    }

    @Test
    void issueNonCrossmatched_400() throws Exception {
        when(requisitionService.issue(10L, 6L))
                .thenThrow(new InvalidBloodBankRequestException("BLD_STATE", "Bag 6 is not crossmatched against requisition 10"));

        mockMvc.perform(post("/api/v1/requisitions/10/issue").with(user("devops").roles("DEV"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"bagId":6}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("BLD_STATE"));
    }

    @Test
    void cancel_200() throws Exception {
        when(requisitionService.cancel(10L)).thenReturn(req(10L, 10));

        mockMvc.perform(post("/api/v1/requisitions/10/cancel").with(user("devops").roles("DEV")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(10));
    }

    @Test
    void jobEndpoints_returnSummaries() throws Exception {
        when(jobService.decrossExpired()).thenReturn(Map.of(
                "agedDecrossed", 0, "bagsExpired", 0, "expiredDecrossed", 0, "requisitionsRecalculated", 0));
        when(jobService.expireRequisitions(null)).thenReturn(Map.of("scanned", 3, "expired", 1));
        when(jobService.cancelStaleDonors()).thenReturn(Map.of("scanned", 5, "cancelled", 2));

        mockMvc.perform(post("/api/v1/jobs/decross-expired").with(user("devops").roles("DEV")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.agedDecrossed").value(0));
        mockMvc.perform(post("/api/v1/jobs/expire-requisitions").with(user("devops").roles("DEV")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.expired").value(1));
        mockMvc.perform(post("/api/v1/jobs/cancel-stale-donors").with(user("devops").roles("DEV")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cancelled").value(2));
    }

    @Test
    void unauthenticated_is401_defaultDeny() throws Exception {
        mockMvc.perform(get("/api/v1/donors"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unknownRequisition_404() throws Exception {
        when(requisitionService.detail(77L))
                .thenThrow(new BloodBankEntityNotFoundException("BLD_NOT_FOUND", "Requisition 77 not found"));

        mockMvc.perform(get("/api/v1/requisitions/77").with(user("devops").roles("DEV")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("BLD_NOT_FOUND"));
    }
}
