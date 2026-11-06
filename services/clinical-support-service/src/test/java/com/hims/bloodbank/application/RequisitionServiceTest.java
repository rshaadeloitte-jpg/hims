package com.hims.bloodbank.application;

import com.hims.bloodbank.application.exceptions.BagConflictException;
import com.hims.bloodbank.application.exceptions.BloodBankEntityNotFoundException;
import com.hims.bloodbank.application.exceptions.InvalidBloodBankRequestException;
import com.hims.bloodbank.domain.model.BloodBag;
import com.hims.bloodbank.domain.model.BloodRequisition;
import com.hims.bloodbank.domain.repository.BloodBagRepository;
import com.hims.bloodbank.domain.repository.BloodRequisitionDetailRepository;
import com.hims.bloodbank.domain.repository.BloodRequisitionRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * L1 — BR-BLD-001 status machine, no Spring context.
 * Covers the register's mandatory cases: partial cross-match stays 1, full -> 2,
 * issue -> 3, cancel -> 10, plus guard rails (409/400 paths).
 */
class RequisitionServiceTest {

    private BloodRequisitionRepository requisitionRepository;
    private BloodRequisitionDetailRepository detailRepository;
    private BloodBagRepository bagRepository;
    private RequisitionService service;

    private static final Instant NOW = Instant.parse("2027-06-15T10:00:00Z");

    @BeforeEach
    void setUp() {
        requisitionRepository = mock(BloodRequisitionRepository.class);
        detailRepository = mock(BloodRequisitionDetailRepository.class);
        bagRepository = mock(BloodBagRepository.class);
        ClockHolder clocks = new ClockHolder(Clock.fixed(NOW, ZoneId.of("UTC")));
        service = new RequisitionService(requisitionRepository, detailRepository,
                bagRepository, clocks);
        EntityManager em = mock(EntityManager.class);
        org.springframework.test.util.ReflectionTestUtils.setField(service, "em", em);

        when(requisitionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(bagRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(detailRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(detailRepository.findByRequisitionId(any())).thenReturn(List.of());
    }

    private BloodRequisition req(long id, int orderedVolumeMl, int status) {
        BloodRequisition r = new BloodRequisition();
        r.setRequisitionId(id);
        r.setPatientId(501L);
        r.setBloodGroup("O+");
        r.setOrderedVolumeMl(orderedVolumeMl);
        r.setRequesteddate(Timestamp.from(NOW.minusSeconds(3600)));
        r.setStatus(status);
        return r;
    }

    private BloodBag bag(long id, int volumeMl) {
        BloodBag b = new BloodBag();
        b.setBagId(id);
        b.setDonorId(1L);
        b.setBloodGroup("O+");
        b.setVolumeMl(volumeMl);
        b.setCollectedAt(Timestamp.from(NOW.minusSeconds(86400)));
        b.setExpirydate(NOW.atZone(ZoneId.of("UTC")).toLocalDate().plusDays(35));
        return b;
    }

    private void linkBags(BloodRequisition r, List<BloodBag> bags) {
        when(bagRepository.findByRequisitionId(r.getRequisitionId())).thenReturn(bags);
        for (BloodBag b : bags) {
            b.setRequisitionId(r.getRequisitionId());
        }
    }

    // ------------------------------------------------- partial cross-match stays 1

    @Test
    void crossmatch_partial_staysNew() {
        BloodRequisition r = req(1L, 1000, BloodRequisition.STATUS_NEW);   // ordered 1000ml
        when(requisitionRepository.findById(1L)).thenReturn(Optional.of(r));
        BloodBag b = bag(10L, 350);
        when(bagRepository.findById(10L)).thenReturn(Optional.of(b));
        linkBags(r, new java.util.ArrayList<>(List.of(b)));

        BloodBag result = service.crossmatch(1L, 10L);

        assertThat(result.getCrossMatched()).isTrue();
        assertThat(result.getCrossMatchedTestDate()).isEqualTo(Timestamp.from(NOW));
        assertThat(r.getStatus()).isEqualTo(BloodRequisition.STATUS_NEW);   // 350 < 1000 -> stays 1
    }

    @Test
    void crossmatch_full_promotesToCrossMatched() {
        BloodRequisition r = req(2L, 700, BloodRequisition.STATUS_NEW);
        when(requisitionRepository.findById(2L)).thenReturn(Optional.of(r));
        BloodBag b1 = bag(11L, 350);
        BloodBag b2 = bag(12L, 350);
        when(bagRepository.findById(11L)).thenReturn(Optional.of(b1));
        when(bagRepository.findById(12L)).thenReturn(Optional.of(b2));

        // first bag crossmatched earlier (already linked)
        b1.setCrossMatched(true);
        b1.setCrossMatchedTestDate(Timestamp.from(NOW.minusSeconds(600)));
        linkBags(r, new java.util.ArrayList<>(List.of(b1, b2)));

        service.crossmatch(2L, 12L);   // cumulative 700 >= ordered 700

        assertThat(r.getStatus()).isEqualTo(BloodRequisition.STATUS_CROSS_MATCHED);
    }

    // --------------------------------------------------------- issue semantics

    @Test
    void issue_full_promotesToIssued() {
        BloodRequisition r = req(3L, 700, BloodRequisition.STATUS_CROSS_MATCHED);
        when(requisitionRepository.findById(3L)).thenReturn(Optional.of(r));
        BloodBag b1 = bag(21L, 400);
        BloodBag b2 = bag(22L, 300);
        for (BloodBag b : List.of(b1, b2)) { b.setCrossMatched(true); }
        when(bagRepository.findById(21L)).thenReturn(Optional.of(b1));
        when(bagRepository.findById(22L)).thenReturn(Optional.of(b2));
        linkBags(r, new java.util.ArrayList<>(List.of(b1, b2)));

        // partial issue first: 400 < 700 -> stays 2; then full: 400+300 >= 700 -> 3
        service.issue(3L, 21L);
        assertThat(r.getStatus()).isEqualTo(BloodRequisition.STATUS_CROSS_MATCHED);
        BloodBag issued = service.issue(3L, 22L);   // cumulative issued 700 >= ordered 700

        assertThat(issued.getIssued()).isTrue();
        assertThat(issued.getIssuedRequisitionId()).isEqualTo(3L);
        assertThat(r.getStatus()).isEqualTo(BloodRequisition.STATUS_ISSUED);
    }

    @Test
    void issue_partial_keepsCurrentStatus() {
        BloodRequisition r = req(4L, 1000, BloodRequisition.STATUS_CROSS_MATCHED);
        when(requisitionRepository.findById(4L)).thenReturn(Optional.of(r));
        BloodBag b1 = bag(31L, 500);
        BloodBag b2 = bag(32L, 500);
        for (BloodBag b : List.of(b1, b2)) { b.setCrossMatched(true); }
        when(bagRepository.findById(32L)).thenReturn(Optional.of(b2));
        linkBags(r, new java.util.ArrayList<>(List.of(b1, b2)));

        service.issue(4L, 32L);   // only 500 of 1000 issued

        assertThat(r.getStatus()).isEqualTo(BloodRequisition.STATUS_CROSS_MATCHED);   // stays current
    }

    @Test
    void issue_nonCrossmatchedBag_is400() {
        BloodRequisition r = req(5L, 500, BloodRequisition.STATUS_NEW);
        when(requisitionRepository.findById(5L)).thenReturn(Optional.of(r));
        BloodBag b = bag(41L, 500);   // never crossmatched
        when(bagRepository.findById(41L)).thenReturn(Optional.of(b));
        linkBags(r, new java.util.ArrayList<>(List.of()));

        assertThatThrownBy(() -> service.issue(5L, 41L))
                .isInstanceOf(InvalidBloodBankRequestException.class)
                .hasMessageContaining("not crossmatched");
    }

    @Test
    void crossmatch_alreadyCrossmatchedBag_is409() {
        BloodRequisition r = req(6L, 500, BloodRequisition.STATUS_NEW);
        when(requisitionRepository.findById(6L)).thenReturn(Optional.of(r));
        BloodBag b = bag(42L, 500);
        b.setCrossMatched(true);
        when(bagRepository.findById(42L)).thenReturn(Optional.of(b));

        assertThatThrownBy(() -> service.crossmatch(6L, 42L))
                .isInstanceOf(BagConflictException.class)
                .hasMessageContaining("already crossmatched");
    }

    @Test
    void crossmatch_issuedOrExpiredBag_is409() {
        BloodRequisition r = req(7L, 500, BloodRequisition.STATUS_NEW);
        when(requisitionRepository.findById(7L)).thenReturn(Optional.of(r));
        BloodBag issued = bag(43L, 250);
        issued.setIssued(true);
        BloodBag expired = bag(44L, 250);
        expired.setIsbagexpired(true);
        when(bagRepository.findById(43L)).thenReturn(Optional.of(issued));
        when(bagRepository.findById(44L)).thenReturn(Optional.of(expired));

        assertThatThrownBy(() -> service.crossmatch(7L, 43L))
                .isInstanceOf(BagConflictException.class)
                .hasMessageContaining("already issued");
        assertThatThrownBy(() -> service.crossmatch(7L, 44L))
                .isInstanceOf(BagConflictException.class)
                .hasMessageContaining("expired");
    }

    // ---------------------------------------------------------------- cancel -> 10

    @Test
    void cancel_setsCancelledAndReleasesBags() {
        BloodRequisition r = req(8L, 700, BloodRequisition.STATUS_CROSS_MATCHED);
        when(requisitionRepository.findById(8L)).thenReturn(Optional.of(r));
        BloodBag b = bag(51L, 700);
        b.setCrossMatched(true);
        linkBags(r, new java.util.ArrayList<>(List.of(b)));

        BloodRequisition cancelled = service.cancel(8L);

        assertThat(cancelled.getStatus()).isEqualTo(BloodRequisition.STATUS_CANCELLED_EXPIRED);
        assertThat(b.getCrossMatched()).isFalse();
        assertThat(b.getRequisitionId()).isNull();
    }

    @Test
    void cancel_issuedOrAlreadyCancelled_is400() {
        BloodRequisition issued = req(9L, 700, BloodRequisition.STATUS_ISSUED);
        BloodRequisition cancelled = req(10L, 700, BloodRequisition.STATUS_CANCELLED_EXPIRED);
        when(requisitionRepository.findById(9L)).thenReturn(Optional.of(issued));
        when(requisitionRepository.findById(10L)).thenReturn(Optional.of(cancelled));

        assertThatThrownBy(() -> service.cancel(9L))
                .isInstanceOf(InvalidBloodBankRequestException.class)
                .hasMessageContaining("already fully issued");
        assertThatThrownBy(() -> service.cancel(10L))
                .isInstanceOf(InvalidBloodBankRequestException.class)
                .hasMessageContaining("already cancelled/expired");
    }

    // --------------------------------------------------------------- validation

    @Test
    void create_missingPatient_is400() {
        assertThatThrownBy(() -> service.create(null, "O+", 500, null, null, null))
                .isInstanceOf(InvalidBloodBankRequestException.class)
                .hasMessageContaining("patientId");
    }

    @Test
    void get_unknown_is404() {
        when(requisitionRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.get(99L))
                .isInstanceOf(BloodBankEntityNotFoundException.class);
    }

    @Test
    void decross_recalc_walksFullBackToPartial() {
        // status 2 (crossmatched), one of two bags gets decrossed by the job ->
        // recompute walks back to 1 (cumulative below ordered)
        BloodRequisition r = req(11L, 700, BloodRequisition.STATUS_CROSS_MATCHED);
        BloodBag b1 = bag(61L, 350);
        BloodBag b2 = bag(62L, 350);
        for (BloodBag b : List.of(b1, b2)) {
            b.setCrossMatched(true);
            b.setRequisitionId(11L);
        }
        linkBags(r, new java.util.ArrayList<>(List.of(b1, b2)));
        b2.setCrossMatched(false);
        b2.setRequisitionId(null);

        service.recomputeStatus(r);

        assertThat(r.getStatus()).isEqualTo(BloodRequisition.STATUS_NEW);
    }
}
