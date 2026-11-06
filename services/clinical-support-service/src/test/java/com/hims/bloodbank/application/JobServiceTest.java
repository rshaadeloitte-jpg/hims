package com.hims.bloodbank.application;

import com.hims.bloodbank.domain.model.BloodBag;
import com.hims.bloodbank.domain.model.BloodDonor;
import com.hims.bloodbank.domain.model.BloodRequisition;
import com.hims.bloodbank.domain.repository.BloodBagRepository;
import com.hims.bloodbank.domain.repository.BloodDonorRepository;
import com.hims.bloodbank.domain.repository.BloodRequisitionDetailRepository;
import com.hims.bloodbank.domain.repository.BloodRequisitionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * L1 — job rules, no Spring context.
 * BR-BLD-002 decross &lt;48h untouched vs >=48h (incl. exactly-2880-min boundary);
 * BR-BLD-003 bag expiry; BR-BLD-004 requisition expiry; BR-BLD-005 72h donor rule.
 */
class JobServiceTest {

    private BloodBagRepository bagRepository;
    private BloodRequisitionRepository requisitionRepository;
    private BloodDonorRepository donorRepository;
    private BloodRequisitionDetailRepository detailRepository;
    private RequisitionService requisitionService;
    private JobService jobs;

    // Fixed "now" so the 48h / 72h boundaries are exact.
    private static final Instant NOW = Instant.parse("2027-06-15T12:00:00Z");
    private static final ZoneId Z = ZoneId.of("UTC");

    @BeforeEach
    void setUp() {
        bagRepository = mock(BloodBagRepository.class);
        requisitionRepository = mock(BloodRequisitionRepository.class);
        detailRepository = mock(BloodRequisitionDetailRepository.class);
        donorRepository = mock(BloodDonorRepository.class);
        requisitionService = mock(RequisitionService.class);
        ClockHolder clocks = new ClockHolder(Clock.fixed(NOW, Z));
        when(requisitionService.crossmatchedVolume(anyLong())).thenReturn(0);
        when(requisitionService.issuedVolume(anyLong())).thenReturn(0);

        jobs = new JobService(bagRepository, requisitionRepository, detailRepository,
                donorRepository, requisitionService, clocks, 72);

        when(bagRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(donorRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(requisitionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private BloodBag crossmatchedBag(long id, long reqId, int volumeMl, Instant testDate) {
        BloodBag b = new BloodBag();
        b.setBagId(id);
        b.setDonorId(1L);
        b.setBloodGroup("A+");
        b.setVolumeMl(volumeMl);
        b.setCollectedAt(Timestamp.from(NOW.minusSeconds(10 * 86400)));
        b.setExpirydate(LocalDate.ofInstant(NOW, Z).plusDays(30));
        b.setCrossMatched(true);
        b.setCrossMatchedTestDate(Timestamp.from(testDate));
        b.setRequisitionId(reqId);
        return b;
    }

    private BloodRequisition req(long id, int ordered, int status) {
        BloodRequisition r = new BloodRequisition();
        r.setRequisitionId(id);
        r.setPatientId(1L);
        r.setBloodGroup("A+");
        r.setOrderedVolumeMl(ordered);
        r.setRequesteddate(Timestamp.from(NOW.minusSeconds(3600)));
        r.setStatus(status);
        return r;
    }

    // ------------------------------------------------------------ BR-BLD-002

    @Test
    void decross_under48h_untouched() {
        BloodBag fresh = crossmatchedBag(1L, 100L, 350, NOW.minusSeconds(47 * 3600L + 59 * 60L));
        when(bagRepository.findByCrossMatchedTrueAndIssuedFalseAndIsbagexpiredFalseAndCrossMatchedTestDateLessThanEqual(
                Timestamp.from(NOW.minusSeconds(48 * 3600L))))
                .thenReturn(List.of());   // 47:59 is NOT <= now-48h

        var summary = jobs.decrossExpired();

        assertThat(summary.get("agedDecrossed")).isEqualTo(0);
        assertThat(fresh.getCrossMatched()).isTrue();
    }

    @Test
    void decross_atExactly48h_boundary_decrossed() {
        // exactly 2880 min old -> inclusive boundary -> decross
        BloodBag edge = crossmatchedBag(2L, 101L, 350, NOW.minusSeconds(48 * 3600L));
        when(bagRepository.findByCrossMatchedTrueAndIssuedFalseAndIsbagexpiredFalseAndCrossMatchedTestDateLessThanEqual(
                any()))
                .thenReturn(List.of(edge));
        BloodRequisition r = req(101L, 700, BloodRequisition.STATUS_CROSS_MATCHED);
        when(requisitionRepository.findById(101L)).thenReturn(java.util.Optional.of(r));
        when(bagRepository.findByExpirydateEqualsAndIssuedFalseAndIsbagexpiredFalse(any())).thenReturn(List.of());

        var summary = jobs.decrossExpired();

        assertThat(summary.get("agedDecrossed")).isEqualTo(1);
        assertThat(edge.getCrossMatched()).isFalse();
        assertThat(edge.getCrossMatchedTestDate()).isNull();
        assertThat(edge.getRequisitionId()).isNull();
        verify(requisitionService).recomputeStatus(r);   // requisition walks back toward 1
    }

    @Test
    void decross_over48h_decrossed() {
        BloodBag stale = crossmatchedBag(3L, 102L, 450, NOW.minusSeconds(72 * 3600L));
        when(bagRepository.findByCrossMatchedTrueAndIssuedFalseAndIsbagexpiredFalseAndCrossMatchedTestDateLessThanEqual(
                any()))
                .thenReturn(List.of(stale));
        when(bagRepository.findByExpirydateEqualsAndIssuedFalseAndIsbagexpiredFalse(any())).thenReturn(List.of());
        BloodRequisition r = req(102L, 900, BloodRequisition.STATUS_CROSS_MATCHED);
        when(requisitionRepository.findById(102L)).thenReturn(java.util.Optional.of(r));

        var summary = jobs.decrossExpired();

        assertThat(summary.get("agedDecrossed")).isEqualTo(1);
        assertThat(stale.getCrossMatched()).isFalse();
        verify(requisitionService).recomputeStatus(r);
    }

    @Test
    void decross_idempotent_secondRunFindsNothing() {
        when(bagRepository.findByCrossMatchedTrueAndIssuedFalseAndIsbagexpiredFalseAndCrossMatchedTestDateLessThanEqual(
                any()))
                .thenReturn(List.of())
                .thenReturn(List.of());
        when(bagRepository.findByExpirydateEqualsAndIssuedFalseAndIsbagexpiredFalse(any())).thenReturn(List.of());

        var first = jobs.decrossExpired();
        var second = jobs.decrossExpired();

        assertThat(first.get("agedDecrossed")).isEqualTo(0);
        assertThat(second).containsEntry("agedDecrossed", 0)
                .containsEntry("bagsExpired", 0)
                .containsEntry("requisitionsRecalculated", 0);
    }

    // ------------------------------------------------------------ BR-BLD-003

    @Test
    void bagExpiry_expiringToday_markedExpired_andDecrossedIfCrossmatched() {
        LocalDate today = LocalDate.ofInstant(NOW, Z);
        BloodBag plain = new BloodBag();
        plain.setBagId(4L);
        plain.setDonorId(1L);
        plain.setBloodGroup("B+");
        plain.setVolumeMl(300);
        plain.setCollectedAt(Timestamp.from(NOW.minusSeconds(20 * 86400L)));
        plain.setExpirydate(today);
        plain.setCrossMatched(false);

        BloodBag xmatched = crossmatchedBag(5L, 103L, 400, NOW.minusSeconds(2 * 3600L));   // fresh crossmatch, expired today
        xmatched.setExpirydate(today);

        when(bagRepository.findByCrossMatchedTrueAndIssuedFalseAndIsbagexpiredFalseAndCrossMatchedTestDateLessThanEqual(
                any()))
                .thenReturn(List.of());
        when(bagRepository.findByExpirydateEqualsAndIssuedFalseAndIsbagexpiredFalse(today))
                .thenReturn(List.of(plain, xmatched));
        BloodRequisition r = req(103L, 800, BloodRequisition.STATUS_CROSS_MATCHED);
        when(requisitionRepository.findById(103L)).thenReturn(java.util.Optional.of(r));

        var summary = jobs.decrossExpired();

        assertThat(summary.get("bagsExpired")).isEqualTo(2);
        assertThat(summary.get("expiredDecrossed")).isEqualTo(1);
        assertThat(plain.getIsbagexpired()).isTrue();
        assertThat(xmatched.getIsbagexpired()).isTrue();
        assertThat(xmatched.getCrossMatched()).isFalse();   // BR-BLD-003 decross
        verify(requisitionService).recomputeStatus(r);
    }

    @Test
    void bagExpiry_issuedBagsSkipped() {
        // issued bags are not returned by findByExpirydateEqualsAndIssuedFalseAndIsbagexpiredFalse(today) — contract pinned:
        when(bagRepository.findByCrossMatchedTrueAndIssuedFalseAndIsbagexpiredFalseAndCrossMatchedTestDateLessThanEqual(
                any()))
                .thenReturn(List.of());
        when(bagRepository.findByExpirydateEqualsAndIssuedFalseAndIsbagexpiredFalse(LocalDate.ofInstant(NOW, Z)))
                .thenReturn(List.of());

        var summary = jobs.decrossExpired();

        assertThat(summary.get("bagsExpired")).isEqualTo(0);
    }

    // ------------------------------------------------------------ BR-BLD-004

    @Test
    void expireRequisitions_pastValidationPeriod_sets10WithReason() {
        BloodRequisition stale = req(201L, 600, BloodRequisition.STATUS_NEW);
        stale.setRequesteddate(Timestamp.from(NOW.minusSeconds(73 * 3600L)));   // 73h ago > 72h window
        var detail = new com.hims.bloodbank.domain.model.BloodRequisitionDetail();
        detail.setDetailId(1L);
        detail.setRequisitionId(201L);
        detail.setComponent("PRBC");
        detail.setVolumeMl(600);
        when(requisitionRepository.findByStatus(BloodRequisition.STATUS_NEW)).thenReturn(List.of(stale));
        when(detailRepository.findByRequisitionId(201L)).thenReturn(List.of(detail));

        var summary = jobs.expireRequisitions(null);   // per-requisition snapshot (default 72h)

        assertThat(summary.get("expired")).isEqualTo(1);
        assertThat(stale.getStatus()).isEqualTo(BloodRequisition.STATUS_CANCELLED_EXPIRED);
        assertThat(stale.getOrderExpired()).isTrue();
        assertThat(detail.getRequisitionStatus()).isEqualTo(BloodRequisition.STATUS_CANCELLED_EXPIRED);
        assertThat(detail.getIsorderexpired()).isTrue();
        assertThat(detail.getExpiryReason()).isEqualTo("Order is expired");   // legacy literal
    }

    @Test
    void expireRequisitions_withinWindowOrCollected_untouched() {
        BloodRequisition fresh = req(202L, 600, BloodRequisition.STATUS_NEW);
        fresh.setRequesteddate(Timestamp.from(NOW.minusSeconds(3600)));   // 1h ago
        BloodRequisition collected = req(203L, 600, BloodRequisition.STATUS_NEW);
        collected.setRequesteddate(Timestamp.from(NOW.minusSeconds(200 * 3600L)));
        collected.setSampleCollected(true);   // sample drawn -> never expires
        when(requisitionRepository.findByStatus(BloodRequisition.STATUS_NEW))
                .thenReturn(List.of(fresh, collected));

        var summary = jobs.expireRequisitions(null);

        assertThat(summary.get("expired")).isEqualTo(0);
        assertThat(fresh.getStatus()).isEqualTo(BloodRequisition.STATUS_NEW);
        assertThat(collected.getStatus()).isEqualTo(BloodRequisition.STATUS_NEW);
        assertThat(collected.getOrderExpired()).isFalse();
    }

    // ------------------------------------------------------------ BR-BLD-005

    @Test
    void cancelStaleDonors_beyond72h_cancelled() {
        BloodDonor staleNew = donor(301L, BloodDonor.DNR_NEW, Timestamp.from(NOW.minusSeconds(73 * 3600L)));
        BloodDonor staleFit = donor(302L, BloodDonor.DNR_FIT, Timestamp.from(NOW.minusSeconds(80 * 3600L)));
        when(donorRepository.findByDonationStatusInAndLastActionAtLessThanEqual(
                List.of(BloodDonor.DNR_NEW, BloodDonor.DNR_FIT), Timestamp.from(NOW.minusSeconds(72 * 3600L))))
                .thenReturn(List.of(staleNew, staleFit));

        var summary = jobs.cancelStaleDonors();

        assertThat(summary.get("cancelled")).isEqualTo(2);
        assertThat(staleNew.getDonationStatus()).isEqualTo(BloodDonor.DNR_CANCELLATION);
        assertThat(staleFit.getDonationStatus()).isEqualTo(BloodDonor.DNR_CANCELLATION);
    }

    @Test
    void cancelStaleDonors_atExactly72hBoundary_cancelled() {
        BloodDonor edge = donor(303L, BloodDonor.DNR_FIT, Timestamp.from(NOW.minusSeconds(72 * 3600L)));
        when(donorRepository.findByDonationStatusInAndLastActionAtLessThanEqual(
                List.of(BloodDonor.DNR_NEW, BloodDonor.DNR_FIT), Timestamp.from(NOW.minusSeconds(72 * 3600L))))
                .thenReturn(List.of(edge));   // <= cutoff inclusive

        var summary = jobs.cancelStaleDonors();

        assertThat(summary.get("cancelled")).isEqualTo(1);
        assertThat(edge.getDonationStatus()).isEqualTo(BloodDonor.DNR_CANCELLATION);
    }

    @Test
    void cancelStaleDonors_recentTouchOrPostExamStages_untouched() {
        // recently touched donors are excluded by the repository predicate itself;
        // post-med-exam statuses (3..6) are outside DNR_NEW/DNR_FIT — both pinned here
        when(donorRepository.findByDonationStatusInAndLastActionAtLessThanEqual(
                List.of(BloodDonor.DNR_NEW, BloodDonor.DNR_FIT), Timestamp.from(NOW.minusSeconds(72 * 3600L))))
                .thenReturn(List.of());

        var summary = jobs.cancelStaleDonors();

        assertThat(summary.get("cancelled")).isEqualTo(0);
    }

    @Test
    void touch_refreshesLastAction_keepsDonorOutOfScan() {
        // DonorService.touch semantics underpinning BR-BLD-005
        var donorRepository = mock(BloodDonorRepository.class);
        var em = mock(jakarta.persistence.EntityManager.class);
        DonorService donorService = new DonorService(donorRepository,
                new ClockHolder(Clock.fixed(NOW, Z)));
        org.springframework.test.util.ReflectionTestUtils.setField(donorService, "em", em);
        when(em.createNativeQuery(any(String.class))).thenReturn(mock(jakarta.persistence.Query.class));
        BloodDonor d = donor(304L, BloodDonor.DNR_FIT, Timestamp.from(NOW.minusSeconds(100 * 3600L)));
        when(donorRepository.findById(304L)).thenReturn(java.util.Optional.of(d));
        when(donorRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        BloodDonor touched = donorService.touch(304L);

        assertThat(touched.getLastActionAt()).isEqualTo(Timestamp.from(NOW));
    }

    private BloodDonor donor(long id, int status, Timestamp lastAction) {
        BloodDonor d = new BloodDonor();
        d.setDonorId(id);
        d.setName("Donor " + id);
        d.setBloodGroup("O-");
        d.setDonationStatus(status);
        d.setLastActionAt(lastAction);
        return d;
    }
}
