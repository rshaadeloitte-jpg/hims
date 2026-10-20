package com.hims.bloodbank.infrastructure.persistence;

import com.hims.bloodbank.BloodBankServiceApplication;
import com.hims.bloodbank.application.BagService;
import com.hims.bloodbank.application.DonorService;
import com.hims.bloodbank.application.JobService;
import com.hims.bloodbank.application.RequisitionService;
import com.hims.bloodbank.domain.model.BloodBag;
import com.hims.bloodbank.domain.model.BloodDonor;
import com.hims.bloodbank.domain.model.BloodRequisition;
import com.hims.bloodbank.domain.repository.BloodBagRepository;
import com.hims.bloodbank.domain.repository.BloodDonorRepository;
import com.hims.bloodbank.domain.repository.BloodRequisitionDetailRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Timestamp;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * L2 — WP-9 Blood Bank service on real PG16: V1 schema applies (ihms_bloodbank),
 * donor registry + touch, bag registration, BR-BLD-001 machine end-to-end
 * (partial cross-match stays 1 -> full -> 2 -> issue -> 3, cancel -> 10),
 * BR-BLD-002 decross on real timestamps, BR-BLD-004 expiry scan,
 * BR-BLD-005 stale donors — and NO outbox table (BloodRequisitionCreated DEFERRED
 * per KAFKA-EVENT-CATALOG §3).
 * NOTE: requires Docker; skipped automatically when Docker is absent
 * (@Testcontainers(disabledWithoutDocker = true)) — do not remove.
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(classes = BloodBankServiceApplication.class)
@DirtiesContext
@Transactional
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class BloodBankServiceIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private DonorService donorService;

    @Autowired
    private BagService bagService;

    @Autowired
    private RequisitionService requisitionService;

    @Autowired
    private JobService jobService;

    @Autowired
    private BloodDonorRepository donorRepository;

    @Autowired
    private BloodBagRepository bagRepository;

    @Autowired
    private BloodRequisitionDetailRepository detailRepository;

    @PersistenceContext
    private EntityManager em;

    /** Backdate a column inside the test transaction so time-based rules fire. */
    private void backdate(String sql, Object id) {
        em.createNativeQuery(sql).setParameter(1, id).executeUpdate();
        em.flush();
        em.clear();
    }

    @Test
    @Order(1)
    void fullBloodBankFlow_brRulesOnRealPg() {
        // ---- schema sanity: flyway applied, explicit sequence live ------------------
        Number seq = (Number) em.createNativeQuery("SELECT nextval('ihms_bloodbank.t_blooddonor_seq')")
                .getSingleResult();
        assertThat(seq.longValue()).isPositive();

        // ---- donor registry + touch -------------------------------------------------
        BloodDonor donor = donorService.create("R. Kumar", "O+", null, 1L);
        assertThat(donor.getDonationStatus()).isEqualTo(BloodDonor.DNR_NEW);
        BloodDonor touched = donorService.touch(donor.getDonorId());
        assertThat(touched.getLastActionAt()).isNotNull();

        BloodDonor fitDonor = donorService.create("S. Devi", "B+", BloodDonor.DNR_FIT, 1L);

        // ---- bags -------------------------------------------------------------------
        BloodBag b1 = bagService.create(donor.getDonorId(), "O+", 350,
                LocalDate.now().plusDays(35), 1L);
        BloodBag b2 = bagService.create(fitDonor.getDonorId(), "O+", 350,
                LocalDate.now().plusDays(35), 1L);

        // ---- BR-BLD-001 machine end-to-end -------------------------------------------
        BloodRequisition r = requisitionService.create(501L, "O+", 700,
                new Timestamp(System.currentTimeMillis()), 72, null);
        assertThat(r.getStatus()).isEqualTo(BloodRequisition.STATUS_NEW);

        // partial cross-match: 350 < 700 -> stays 1
        requisitionService.crossmatch(r.getRequisitionId(), b1.getBagId());
        em.flush();
        r = requisitionService.get(r.getRequisitionId());
        assertThat(r.getStatus()).isEqualTo(BloodRequisition.STATUS_NEW);

        // full cross-match: cumulative 700 >= ordered 700 -> 2
        requisitionService.crossmatch(r.getRequisitionId(), b2.getBagId());
        em.flush();
        r = requisitionService.get(r.getRequisitionId());
        assertThat(r.getStatus()).isEqualTo(BloodRequisition.STATUS_CROSS_MATCHED);

        // partial issue: 350 < 700 -> stays 2
        requisitionService.issue(r.getRequisitionId(), b1.getBagId());
        em.flush();
        r = requisitionService.get(r.getRequisitionId());
        assertThat(r.getStatus()).isEqualTo(BloodRequisition.STATUS_CROSS_MATCHED);

        // full issue: 700 >= 700 -> 3
        requisitionService.issue(r.getRequisitionId(), b2.getBagId());
        em.flush();
        r = requisitionService.get(r.getRequisitionId());
        assertThat(r.getStatus()).isEqualTo(BloodRequisition.STATUS_ISSUED);

        // ---- cancel path on a separate requisition -> 10 -----------------------------
        BloodBag b3 = bagService.create(donor.getDonorId(), "O+", 450,
                LocalDate.now().plusDays(30), 1L);
        BloodRequisition r2 = requisitionService.create(602L, "O+", 450,
                new Timestamp(System.currentTimeMillis()), 72, null);
        final long cancelledReqId = requisitionService.crossmatch(r2.getRequisitionId(), b3.getBagId())
                .getRequisitionId();
        BloodRequisition cancelled = requisitionService.cancel(cancelledReqId);
        assertThat(cancelled.getStatus()).isEqualTo(BloodRequisition.STATUS_CANCELLED_EXPIRED);
        assertThat(bagRepository.findById(b3.getBagId()).orElseThrow().getCrossMatched()).isFalse();

        // cancelled requisition rejects further crossmatching (BLD_STATE -> 400)
        BloodBag b5 = bagService.create(fitDonor.getDonorId(), "O+", 200,
                LocalDate.now().plusDays(30), 1L);
        assertThatThrownBy(() -> requisitionService.crossmatch(cancelledReqId, b5.getBagId()))
                .isInstanceOf(com.hims.bloodbank.application.exceptions.InvalidBloodBankRequestException.class)
                .hasMessageContaining("cancelled/expired");

        // ---- NO events: outbox table absent (BloodRequisitionCreated DEFERRED §3) ----
        Number outboxTables = (Number) em.createNativeQuery(
                "SELECT count(*) FROM information_schema.tables "
                + "WHERE table_schema='ihms_bloodbank' AND table_name='outbox'").getSingleResult();
        assertThat(outboxTables.longValue()).isZero();

        // ---- persistence of the BR-BLD-001 column ------------------------------------
        Number statusRow = (Number) em.createNativeQuery(
                "SELECT requisition_status FROM ihms_bloodbank.t_bloodrequisition WHERE requisition_id=?")
                .setParameter(1, r.getRequisitionId()).getSingleResult();
        assertThat(statusRow.intValue()).isEqualTo(BloodRequisition.STATUS_ISSUED);

        // ---- detail mirror rows exist and track the header ----------------------------
        r2 = requisitionService.get(r2.getRequisitionId());
        assertThat(detailRepository.findByRequisitionId(r2.getRequisitionId())).isEmpty();   // created w/o components
    }

    @Test
    @Order(2)
    void decrossJob_brBld002_onRealTimestamps_idempotent() {
        // fresh (<48h) crossmatched bag untouched...
        BloodDonor donor = donorService.create("Fresh Donor", "A+", null, 1L);
        BloodBag edge = bagService.create(donor.getDonorId(), "A+", 400,
                LocalDate.now().plusDays(40), 1L);
        BloodRequisition rFresh = requisitionService.create(701L, "A+", 400,
                new Timestamp(System.currentTimeMillis()), 72, null);
        requisitionService.crossmatch(rFresh.getRequisitionId(), edge.getBagId());
        em.flush();
        assertThat(requisitionService.get(rFresh.getRequisitionId()).getStatus())
                .isEqualTo(BloodRequisition.STATUS_CROSS_MATCHED);   // 400 >= 400

        // ...then age the cross-match past the 48h boundary (real PG now())
        backdate("UPDATE ihms_bloodbank.t_bloodbag "
                + "SET cross_matched_test_date = now() - interval '49 hours' WHERE bag_id = ?",
                edge.getBagId());

        var firstRun = jobService.decrossExpired();
        assertThat(firstRun).containsEntry("agedDecrossed", 1);
        em.flush();   // persist the job's pending recalc before clearing the context
        em.clear();
        BloodBag decrossed = bagRepository.findById(edge.getBagId()).orElseThrow();
        assertThat(decrossed.getCrossMatched()).isFalse();
        assertThat(decrossed.getCrossMatchedTestDate()).isNull();
        assertThat(decrossed.getRequisitionId()).isNull();

        // requisition walked back toward 1 after the volume decrement
        assertThat(requisitionService.get(rFresh.getRequisitionId()).getStatus())
                .isEqualTo(BloodRequisition.STATUS_NEW);

        // idempotent second run: nothing left to do
        var secondRun = jobService.decrossExpired();
        assertThat(secondRun).containsEntry("agedDecrossed", 0)
                .containsEntry("bagsExpired", 0)
                .containsEntry("requisitionsRecalculated", 0);
    }

    @Test
    @Order(3)
    void bagExpiry_brBld003_marksExpiredAndDecrosses() {
        BloodDonor donor = donorService.create("Expiry Donor", "AB-", null, 1L);
        BloodBag todayExpiry = bagService.create(donor.getDonorId(), "AB-", 300,
                LocalDate.now(), 1L);                       // expirydate = today -> BR-BLD-003
        BloodBag futureExpiry = bagService.create(donor.getDonorId(), "AB-", 300,
                LocalDate.now().plusDays(10), 1L);          // untouched
        BloodRequisition r = requisitionService.create(901L, "AB-", 600,
                new Timestamp(System.currentTimeMillis()), 72, null);
        requisitionService.crossmatch(r.getRequisitionId(), todayExpiry.getBagId());
        requisitionService.crossmatch(r.getRequisitionId(), futureExpiry.getBagId());   // keeps req at >=1? 600>=600 -> 2
        em.flush();

        var summary = jobService.decrossExpired();

        assertThat(summary).containsEntry("bagsExpired", 1)
                .containsEntry("expiredDecrossed", 1);
        em.clear();
        BloodBag expired = bagRepository.findById(todayExpiry.getBagId()).orElseThrow();
        assertThat(expired.getIsbagexpired()).isTrue();
        assertThat(expired.getCrossMatched()).isFalse();       // BR-BLD-003 decross of expired crossmatch

        // issued bags are skipped entirely by the sweep
        BloodBag issuedBag = bagService.create(donor.getDonorId(), "AB-", 250,
                LocalDate.now(), 1L);
        issuedBag.setCrossMatched(true);
        issuedBag.setCrossMatchedTestDate(new Timestamp(System.currentTimeMillis()));
        issuedBag.setIssued(true);
        bagRepository.save(issuedBag);
        em.flush();
        var rerun = jobService.decrossExpired();
        assertThat(rerun).containsEntry("bagsExpired", 0);     // only unissued bags are candidates
    }

    @Test
    @Order(4)
    void expireRequisitions_brBld004_setsExpiredWithReason_sampleCollectedUntouched() {
        // 80h-old NEW requisition (default validation window 72h) -> expires
        BloodRequisition stale = requisitionService.create(801L, "A-", 300,
                new Timestamp(System.currentTimeMillis() - 80 * 3600_000L), 72, null);
        // fresh requisition stays
        BloodRequisition fresh = requisitionService.create(802L, "A-", 300,
                new Timestamp(System.currentTimeMillis()), 72, null);
        // old but sample collected stays
        BloodRequisition collected = requisitionService.create(803L, "A-", 300,
                new Timestamp(System.currentTimeMillis() - 200 * 3600_000L), 72, null);
        collected.setSampleCollected(true);

        var summary = jobService.expireRequisitions(null);

        assertThat((Integer) summary.get("expired")).isGreaterThanOrEqualTo(1);
        BloodRequisition expiredReq = requisitionService.get(stale.getRequisitionId());
        assertThat(expiredReq.getStatus()).isEqualTo(BloodRequisition.STATUS_CANCELLED_EXPIRED);
        assertThat(expiredReq.getOrderExpired()).isTrue();

        assertThat(requisitionService.get(fresh.getRequisitionId()).getStatus())
                .isEqualTo(BloodRequisition.STATUS_NEW);
        assertThat(requisitionService.get(collected.getRequisitionId()).getStatus())
                .isEqualTo(BloodRequisition.STATUS_NEW);
        assertThat(collected.getOrderExpired()).isFalse();

        // idempotent re-run: nothing more to expire
        var rerun = jobService.expireRequisitions(null);
        assertThat((Integer) rerun.get("expired")).isZero();
    }

    @Test
    @Order(5)
    void cancelStaleDonors_brBld005_72hRule_onRealPg() {
        BloodDonor staleNew = donorService.create("Stale New", "O-", null, 1L);
        BloodDonor recentFit = donorService.create("Recent Fit", "O-", BloodDonor.DNR_FIT, 1L);
        donorService.touch(recentFit.getDonorId());   // last_action_at = now -> survives

        // age one donor past 72h (inclusive boundary semantics verified in L1)
        backdate("UPDATE ihms_bloodbank.t_blooddonor "
                + "SET last_action_at = now() - interval '73 hours' WHERE donor_id = ?",
                staleNew.getDonorId());

        var summary = jobService.cancelStaleDonors();

        assertThat(summary).containsEntry("cancelled", 1);
        assertThat(donorRepository.findById(staleNew.getDonorId()).orElseThrow()
                .getDonationStatus()).isEqualTo(BloodDonor.DNR_CANCELLATION);
        assertThat(donorRepository.findById(recentFit.getDonorId()).orElseThrow()
                .getDonationStatus()).isEqualTo(BloodDonor.DNR_FIT);   // touched -> kept out

        // idempotent re-run
        var rerun = jobService.cancelStaleDonors();
        assertThat(rerun).containsEntry("cancelled", 0);
    }
}
