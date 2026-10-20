package com.hims.mortuary.infrastructure.persistence;

import com.hims.mortuary.MortuaryServiceApplication;
import com.hims.mortuary.application.MortuaryService;
import com.hims.mortuary.domain.model.AutopsyRequest;
import com.hims.mortuary.domain.model.DeadBody;
import com.hims.mortuary.domain.model.ReleaseCertificate;
import com.hims.mortuary.domain.repository.AutopsyRequestRepository;
import com.hims.mortuary.domain.repository.DeadBodyRepository;
import com.hims.mortuary.domain.repository.ReleaseCertificateRepository;
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
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * L2 — M28 Mortuary service on real PG16: V1 schema applies (ihms_mortuary),
 * dead body registry, autopsy request lifecycle (BR-M28-002), release certificate
 * lifecycle (BR-M28-003), report — end-to-end.
 * NOTE: requires Docker; skipped automatically when Docker is absent
 * (@Testcontainers(disabledWithoutDocker = true)).
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(classes = MortuaryServiceApplication.class)
@DirtiesContext
@Transactional
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class MortuaryServiceIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private MortuaryService mortuaryService;

    @Autowired
    private DeadBodyRepository deadBodyRepository;

    @Autowired
    private AutopsyRequestRepository autopsyRequestRepository;

    @Autowired
    private ReleaseCertificateRepository releaseCertificateRepository;

    @PersistenceContext
    private EntityManager em;

    private static final Instant NOW = Instant.parse("2027-06-15T10:00:00Z");

    @Test
    @Order(1)
    void fullMortuaryFlow_brRulesOnRealPg() {
        // ---- schema sanity: flyway applied, explicit sequence live ------------------
        Number seq = (Number) em.createNativeQuery("SELECT nextval('ihms_mortuary.t_dead_body_seq')")
                .getSingleResult();
        assertThat(seq.longValue()).isPositive();

        // ---- create dead body -------------------------------------------------------
        DeadBody body = mortuaryService.createDeadBody(101L, 201L, "John Doe",
                Timestamp.from(NOW), "Cardiac arrest", "ICU", "Jane Doe", "Spouse",
                "9876543210", "CH-01", false, null, null, 1L);
        assertThat(body.getId()).isPositive();
        assertThat(body.getBodyStatus()).isEqualTo(DeadBody.STATUS_ADMITTED);
        assertThat(body.getAdmissionId()).isEqualTo(101L);

        // ---- get by id --------------------------------------------------------------
        DeadBody fetched = mortuaryService.getDeadBody(body.getId());
        assertThat(fetched.getPatientName()).isEqualTo("John Doe");

        // ---- get by admission id ----------------------------------------------------
        DeadBody byAdm = mortuaryService.getDeadBodyByAdmissionId(101L);
        assertThat(byAdm.getId()).isEqualTo(body.getId());

        // ---- update -----------------------------------------------------------------
        DeadBody updated = mortuaryService.updateDeadBody(body.getId(), "Stroke", "Ward B",
                "New Kin", "Son", "9999999999", "CH-02", "DC-001", "Updated remark");
        assertThat(updated.getCauseOfDeath()).isEqualTo("Stroke");
        assertThat(updated.getDeathCertNo()).isEqualTo("DC-001");

        // ---- request autopsy -------------------------------------------------------
        AutopsyRequest req = mortuaryService.requestAutopsy(body.getId(), 5L, "Unknown cause");
        assertThat(req.getRequestStatus()).isEqualTo(AutopsyRequest.STATUS_PENDING);

        em.flush();
        em.clear();
        DeadBody afterReq = mortuaryService.getDeadBody(body.getId());
        assertThat(afterReq.getBodyStatus()).isEqualTo(DeadBody.STATUS_AUTOPSY_PENDING);

        // ---- approve autopsy --------------------------------------------------------
        AutopsyRequest approved = mortuaryService.approveAutopsy(req.getId(), 7L);
        assertThat(approved.getRequestStatus()).isEqualTo(AutopsyRequest.STATUS_APPROVED);

        // ---- complete autopsy -------------------------------------------------------
        AutopsyRequest completed = mortuaryService.completeAutopsy(req.getId(), Timestamp.from(NOW), "Natural causes");
        assertThat(completed.getRequestStatus()).isEqualTo(AutopsyRequest.STATUS_COMPLETED);
        assertThat(completed.getFindings()).isEqualTo("Natural causes");

        em.flush();
        em.clear();
        DeadBody afterAutopsy = mortuaryService.getDeadBody(body.getId());
        assertThat(afterAutopsy.getBodyStatus()).isEqualTo(DeadBody.STATUS_AUTOPSY_COMPLETE);

        // ---- release body -----------------------------------------------------------
        ReleaseCertificate cert = mortuaryService.releaseBody(body.getId(), "Jane Doe", "Spouse", "9876543210", null);
        assertThat(cert.getReleaseNo()).startsWith("MOR-RC-");
        assertThat(cert.getReleaseStatus()).isEqualTo(ReleaseCertificate.STATUS_RELEASED);
        assertThat(cert.getHandoverTo()).isEqualTo("Jane Doe");

        em.flush();
        em.clear();
        DeadBody released = mortuaryService.getDeadBody(body.getId());
        assertThat(released.getBodyStatus()).isEqualTo(DeadBody.STATUS_RELEASED);
        assertThat(released.isReleased()).isTrue();

        // ---- report -----------------------------------------------------------------
        Map<String, Object> report = mortuaryService.getReport(null);
        assertThat(report.get("total")).isEqualTo(1L);
        assertThat(report.get("released")).isEqualTo(1L);

        // ---- list -------------------------------------------------------------------
        List<DeadBody> admitted = mortuaryService.listDeadBodies(DeadBody.STATUS_RELEASED, null);
        assertThat(admitted).hasSize(1);
    }

    @Test
    @Order(2)
    void create_missingAdmissionId_is400() {
        assertThatThrownBy(() -> mortuaryService.createDeadBody(null, 201L, "John",
                Timestamp.from(NOW), null, null, null, null, null, null, false, null, null, null))
                .isInstanceOf(com.hims.mortuary.application.exceptions.InvalidMortuaryRequestException.class);
    }

    @Test
    @Order(3)
    void get_unknown_is404() {
        assertThatThrownBy(() -> mortuaryService.getDeadBody(99999L))
                .isInstanceOf(com.hims.mortuary.application.exceptions.MortuaryEntityNotFoundException.class);
    }

    @Test
    @Order(4)
    void release_alreadyReleased_is409() {
        // Create a fresh body, release it, then attempt to release again
        DeadBody body = mortuaryService.createDeadBody(201L, 301L, "Second Patient",
                Timestamp.from(NOW), "Cause", "Location", "Kin", "Relation",
                "1234567890", null, false, null, null, 1L);
        mortuaryService.releaseBody(body.getId(), "Kin", "Relation", "1234567890", null);

        assertThatThrownBy(() -> mortuaryService.releaseBody(body.getId(), "Someone", null, null, null))
                .isInstanceOf(com.hims.mortuary.application.exceptions.MortuaryConflictException.class);
    }

    @Test
    @Order(5)
    void completeAutopsy_notApproved_is400() {
        DeadBody body = mortuaryService.createDeadBody(301L, 401L, "Third Patient",
                Timestamp.from(NOW), "Cause", "Location", "Kin", "Relation",
                "1234567890", null, false, null, null, 1L);
        AutopsyRequest req = mortuaryService.requestAutopsy(body.getId(), 5L, "reason");

        // Try to complete without approving first
        assertThatThrownBy(() -> mortuaryService.completeAutopsy(req.getId(), Timestamp.from(NOW), null))
                .isInstanceOf(com.hims.mortuary.application.exceptions.InvalidMortuaryRequestException.class);
    }

    @Test
    @Order(6)
    void rejectAutopsy_revertsBodyStatus() {
        DeadBody body = mortuaryService.createDeadBody(401L, 501L, "Fourth Patient",
                Timestamp.from(NOW), "Cause", "Location", "Kin", "Relation",
                "1234567890", null, false, null, null, 1L);
        AutopsyRequest req = mortuaryService.requestAutopsy(body.getId(), 5L, "reason");

        AutopsyRequest rejected = mortuaryService.rejectAutopsy(req.getId(), "Not needed");
        assertThat(rejected.getRequestStatus()).isEqualTo(AutopsyRequest.STATUS_REJECTED);

        DeadBody reverted = mortuaryService.getDeadBody(body.getId());
        assertThat(reverted.getBodyStatus()).isEqualTo(DeadBody.STATUS_ADMITTED);
    }

    @Test
    @Order(7)
    void listDeadBodies_byPatientId() {
        mortuaryService.createDeadBody(501L, 601L, "Fifth Patient",
                Timestamp.from(NOW), "Cause", "Location", "Kin", "Relation",
                "1234567890", null, false, null, null, 1L);
        mortuaryService.createDeadBody(502L, 601L, "Fifth Patient Duplicate",
                Timestamp.from(NOW), "Cause", "Location", "Kin", "Relation",
                "1234567890", null, false, null, null, 1L);

        List<DeadBody> result = mortuaryService.listDeadBodies(null, 601L);
        assertThat(result).hasSize(2);
    }

    @Test
    @Order(8)
    void updateDeadBody_releasedBody_is409() {
        DeadBody body = mortuaryService.createDeadBody(601L, 701L, "Sixth Patient",
                Timestamp.from(NOW), "Cause", "Location", "Kin", "Relation",
                "1234567890", null, false, null, null, 1L);
        mortuaryService.releaseBody(body.getId(), "Kin", "Relation", "1234567890", null);

        assertThatThrownBy(() -> mortuaryService.updateDeadBody(body.getId(), "New Cause", null, null, null, null, null, null, null))
                .isInstanceOf(com.hims.mortuary.application.exceptions.MortuaryConflictException.class);
    }

    @Test
    @Order(9)
    void getReport_filteredByStatus() {
        Map<String, Object> report = mortuaryService.getReport(DeadBody.STATUS_ADMITTED);
        assertThat(report).containsKey("total");
        assertThat(report).containsKey("admitted");
    }

    @Test
    @Order(10)
    void requestAutopsy_missingRequestedBy_is400() {
        DeadBody body = mortuaryService.createDeadBody(701L, 801L, "Seventh Patient",
                Timestamp.from(NOW), "Cause", "Location", "Kin", "Relation",
                "1234567890", null, false, null, null, 1L);

        assertThatThrownBy(() -> mortuaryService.requestAutopsy(body.getId(), null, "reason"))
                .isInstanceOf(com.hims.mortuary.application.exceptions.InvalidMortuaryRequestException.class);
    }
}
