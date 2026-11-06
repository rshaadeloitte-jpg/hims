package com.hims.mortuary.application;

import com.hims.mortuary.application.exceptions.InvalidMortuaryRequestException;
import com.hims.mortuary.application.exceptions.MortuaryConflictException;
import com.hims.mortuary.application.exceptions.MortuaryEntityNotFoundException;
import com.hims.mortuary.domain.model.AutopsyRequest;
import com.hims.mortuary.domain.model.DeadBody;
import com.hims.mortuary.domain.model.ReleaseCertificate;
import com.hims.mortuary.domain.repository.AutopsyRequestRepository;
import com.hims.mortuary.domain.repository.DeadBodyRepository;
import com.hims.mortuary.domain.repository.ReleaseCertificateRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * L1 — MortuaryService business rules, no Spring context.
 * Covers BR-M28-001 (body status), BR-M28-002 (autopsy), BR-M28-003 (release).
 */
class MortuaryServiceTest {

    private DeadBodyRepository deadBodyRepository;
    private AutopsyRequestRepository autopsyRequestRepository;
    private ReleaseCertificateRepository releaseCertificateRepository;
    private MortuaryService service;

    private static final Instant NOW = Instant.parse("2027-06-15T10:00:00Z");

    @BeforeEach
    void setUp() {
        deadBodyRepository = mock(DeadBodyRepository.class);
        autopsyRequestRepository = mock(AutopsyRequestRepository.class);
        releaseCertificateRepository = mock(ReleaseCertificateRepository.class);
        service = new MortuaryService(deadBodyRepository, autopsyRequestRepository, releaseCertificateRepository);
        EntityManager em = mock(EntityManager.class);
        org.springframework.test.util.ReflectionTestUtils.setField(service, "em", em);

        when(deadBodyRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(autopsyRequestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(releaseCertificateRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private DeadBody body(Long id, String status) {
        DeadBody b = new DeadBody();
        b.setId(id);
        b.setAdmissionId(100L + id);
        b.setPatientId(200L + id);
        b.setPatientName("Patient " + id);
        b.setDeathDate(Timestamp.from(NOW.minusSeconds(3600)));
        b.setBodyStatus(status);
        b.setCauseOfDeath("Cardiac arrest");
        b.setDeathLocation("Ward A");
        b.setNextOfKin("Relative " + id);
        b.setNextOfKinRelation("Spouse");
        return b;
    }

    // --------------------------------------------------------------- create

    @Test
    void createDeadBody_admittedWithAllFields() {
        when(deadBodyRepository.nextId()).thenReturn(1L);
        DeadBody body = service.createDeadBody(101L, 201L, "John Doe",
                Timestamp.from(NOW), "Cardiac arrest", "ICU", "Jane Doe", "Spouse",
                "9876543210", "CH-01", false, null, null, 1L);

        assertThat(body.getId()).isEqualTo(1L);
        assertThat(body.getBodyStatus()).isEqualTo(DeadBody.STATUS_ADMITTED);
        assertThat(body.getAdmissionId()).isEqualTo(101L);
        assertThat(body.getPatientName()).isEqualTo("John Doe");
        assertThat(body.getNextOfKin()).isEqualTo("Jane Doe");
    }

    @Test
    void createDeadBody_missingAdmissionId_is400() {
        assertThatThrownBy(() -> service.createDeadBody(null, 201L, "John",
                Timestamp.from(NOW), null, null, null, null, null, null, false, null, null, null))
                .isInstanceOf(InvalidMortuaryRequestException.class)
                .hasMessageContaining("admissionId");
    }

    @Test
    void createDeadBody_missingPatientId_is400() {
        assertThatThrownBy(() -> service.createDeadBody(101L, null, "John",
                Timestamp.from(NOW), null, null, null, null, null, null, false, null, null, null))
                .isInstanceOf(InvalidMortuaryRequestException.class)
                .hasMessageContaining("patientId");
    }

    @Test
    void createDeadBody_missingPatientName_is400() {
        assertThatThrownBy(() -> service.createDeadBody(101L, 201L, "",
                Timestamp.from(NOW), null, null, null, null, null, null, false, null, null, null))
                .isInstanceOf(InvalidMortuaryRequestException.class)
                .hasMessageContaining("patientName");
    }

    @Test
    void createDeadBody_missingDeathDate_is400() {
        assertThatThrownBy(() -> service.createDeadBody(101L, 201L, "John",
                null, null, null, null, null, null, null, false, null, null, null))
                .isInstanceOf(InvalidMortuaryRequestException.class)
                .hasMessageContaining("deathDate");
    }

    // --------------------------------------------------------------- get

    @Test
    void getDeadBody_found() {
        DeadBody b = body(1L, DeadBody.STATUS_ADMITTED);
        when(deadBodyRepository.findById(1L)).thenReturn(Optional.of(b));

        DeadBody result = service.getDeadBody(1L);
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getPatientName()).isEqualTo("Patient 1");
    }

    @Test
    void getDeadBody_notFound_is404() {
        when(deadBodyRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getDeadBody(99L))
                .isInstanceOf(MortuaryEntityNotFoundException.class);
    }

    @Test
    void getDeadBodyByAdmissionId_found() {
        DeadBody b = body(1L, DeadBody.STATUS_ADMITTED);
        when(deadBodyRepository.findByAdmissionId(101L)).thenReturn(Optional.of(b));

        DeadBody result = service.getDeadBodyByAdmissionId(101L);
        assertThat(result.getAdmissionId()).isEqualTo(101L);
    }

    // --------------------------------------------------------------- update

    @Test
    void updateDeadBody_modifiesFields() {
        DeadBody b = body(1L, DeadBody.STATUS_ADMITTED);
        when(deadBodyRepository.findById(1L)).thenReturn(Optional.of(b));

        DeadBody updated = service.updateDeadBody(1L, "Stroke", "Ward B",
                "New Kin", "Son", "9999999999", "CH-02", "DC-001", "Updated");

        assertThat(updated.getCauseOfDeath()).isEqualTo("Stroke");
        assertThat(updated.getDeathLocation()).isEqualTo("Ward B");
        assertThat(updated.getNextOfKin()).isEqualTo("New Kin");
        assertThat(updated.getDeathCertNo()).isEqualTo("DC-001");
    }

    @Test
    void updateDeadBody_releasedBody_is409() {
        DeadBody b = body(1L, DeadBody.STATUS_RELEASED);
        when(deadBodyRepository.findById(1L)).thenReturn(Optional.of(b));

        assertThatThrownBy(() -> service.updateDeadBody(1L, "Stroke", null, null, null, null, null, null, null))
                .isInstanceOf(MortuaryConflictException.class)
                .hasMessageContaining("already released");
    }

    // --------------------------------------------------------------- release

    @Test
    void releaseBody_createsCertificateAndUpdatesStatus() {
        DeadBody b = body(1L, DeadBody.STATUS_ADMITTED);
        when(deadBodyRepository.findById(1L)).thenReturn(Optional.of(b));
        when(releaseCertificateRepository.nextId()).thenReturn(50L);

        ReleaseCertificate cert = service.releaseBody(1L, "Jane Doe", "Spouse", "9876543210", "Released");

        assertThat(cert.getReleaseNo()).isEqualTo("MOR-RC-50");
        assertThat(cert.getReleaseStatus()).isEqualTo(ReleaseCertificate.STATUS_RELEASED);
        assertThat(cert.getHandoverTo()).isEqualTo("Jane Doe");
        assertThat(b.getBodyStatus()).isEqualTo(DeadBody.STATUS_RELEASED);
    }

    @Test
    void releaseBody_alreadyReleased_is409() {
        DeadBody b = body(1L, DeadBody.STATUS_RELEASED);
        when(deadBodyRepository.findById(1L)).thenReturn(Optional.of(b));

        assertThatThrownBy(() -> service.releaseBody(1L, "Someone", null, null, null))
                .isInstanceOf(MortuaryConflictException.class)
                .hasMessageContaining("already released");
    }

    @Test
    void releaseBody_missingHandoverTo_is400() {
        DeadBody b = body(1L, DeadBody.STATUS_ADMITTED);
        when(deadBodyRepository.findById(1L)).thenReturn(Optional.of(b));

        assertThatThrownBy(() -> service.releaseBody(1L, "", null, null, null))
                .isInstanceOf(InvalidMortuaryRequestException.class)
                .hasMessageContaining("handoverTo");
    }

    // --------------------------------------------------------------- autopsy

    @Test
    void requestAutopsy_createsRequestAndUpdatesStatus() {
        DeadBody b = body(1L, DeadBody.STATUS_ADMITTED);
        when(deadBodyRepository.findById(1L)).thenReturn(Optional.of(b));
        when(autopsyRequestRepository.nextId()).thenReturn(10L);

        AutopsyRequest req = service.requestAutopsy(1L, 5L, "Unknown cause");

        assertThat(req.getRequestStatus()).isEqualTo(AutopsyRequest.STATUS_PENDING);
        assertThat(req.getDeadBodyId()).isEqualTo(1L);
        assertThat(req.getRequestedBy()).isEqualTo(5L);
        assertThat(b.getBodyStatus()).isEqualTo(DeadBody.STATUS_AUTOPSY_PENDING);
    }

    @Test
    void requestAutopsy_releasedBody_is409() {
        DeadBody b = body(1L, DeadBody.STATUS_RELEASED);
        when(deadBodyRepository.findById(1L)).thenReturn(Optional.of(b));

        assertThatThrownBy(() -> service.requestAutopsy(1L, 5L, "reason"))
                .isInstanceOf(MortuaryConflictException.class)
                .hasMessageContaining("released body");
    }

    @Test
    void requestAutopsy_missingRequestedBy_is400() {
        DeadBody b = body(1L, DeadBody.STATUS_ADMITTED);
        when(deadBodyRepository.findById(1L)).thenReturn(Optional.of(b));

        assertThatThrownBy(() -> service.requestAutopsy(1L, null, "reason"))
                .isInstanceOf(InvalidMortuaryRequestException.class)
                .hasMessageContaining("requestedBy");
    }

    @Test
    void approveAutopsy_pendingToApproved() {
        AutopsyRequest req = new AutopsyRequest();
        req.setId(10L);
        req.setDeadBodyId(1L);
        req.setRequestStatus(AutopsyRequest.STATUS_PENDING);
        when(autopsyRequestRepository.findById(10L)).thenReturn(Optional.of(req));

        AutopsyRequest approved = service.approveAutopsy(10L, 7L);
        assertThat(approved.getRequestStatus()).isEqualTo(AutopsyRequest.STATUS_APPROVED);
        assertThat(approved.getDoctorId()).isEqualTo(7L);
    }

    @Test
    void approveAutopsy_notPending_is400() {
        AutopsyRequest req = new AutopsyRequest();
        req.setId(10L);
        req.setRequestStatus(AutopsyRequest.STATUS_APPROVED);
        when(autopsyRequestRepository.findById(10L)).thenReturn(Optional.of(req));

        assertThatThrownBy(() -> service.approveAutopsy(10L, 7L))
                .isInstanceOf(InvalidMortuaryRequestException.class)
                .hasMessageContaining("not PENDING");
    }

    @Test
    void completeAutopsy_approvedToComplete() {
        AutopsyRequest req = new AutopsyRequest();
        req.setId(10L);
        req.setDeadBodyId(1L);
        req.setRequestStatus(AutopsyRequest.STATUS_APPROVED);
        when(autopsyRequestRepository.findById(10L)).thenReturn(Optional.of(req));

        DeadBody b = body(1L, DeadBody.STATUS_AUTOPSY_PENDING);
        when(deadBodyRepository.findById(1L)).thenReturn(Optional.of(b));

        AutopsyRequest completed = service.completeAutopsy(10L, Timestamp.from(NOW), "Findings here");
        assertThat(completed.getRequestStatus()).isEqualTo(AutopsyRequest.STATUS_COMPLETED);
        assertThat(completed.getFindings()).isEqualTo("Findings here");
        assertThat(b.getBodyStatus()).isEqualTo(DeadBody.STATUS_AUTOPSY_COMPLETE);
    }

    @Test
    void completeAutopsy_notApproved_is400() {
        AutopsyRequest req = new AutopsyRequest();
        req.setId(10L);
        req.setRequestStatus(AutopsyRequest.STATUS_PENDING);
        when(autopsyRequestRepository.findById(10L)).thenReturn(Optional.of(req));

        assertThatThrownBy(() -> service.completeAutopsy(10L, Timestamp.from(NOW), null))
                .isInstanceOf(InvalidMortuaryRequestException.class)
                .hasMessageContaining("APPROVED");
    }

    @Test
    void rejectAutopsy_pendingToRejected() {
        AutopsyRequest req = new AutopsyRequest();
        req.setId(10L);
        req.setDeadBodyId(1L);
        req.setRequestStatus(AutopsyRequest.STATUS_PENDING);
        when(autopsyRequestRepository.findById(10L)).thenReturn(Optional.of(req));

        DeadBody b = body(1L, DeadBody.STATUS_AUTOPSY_PENDING);
        when(deadBodyRepository.findById(1L)).thenReturn(Optional.of(b));

        AutopsyRequest rejected = service.rejectAutopsy(10L, "Not required");
        assertThat(rejected.getRequestStatus()).isEqualTo(AutopsyRequest.STATUS_REJECTED);
        assertThat(rejected.getRejectionReason()).isEqualTo("Not required");
        assertThat(b.getBodyStatus()).isEqualTo(DeadBody.STATUS_ADMITTED);
    }

    @Test
    void rejectAutopsy_notPending_is400() {
        AutopsyRequest req = new AutopsyRequest();
        req.setId(10L);
        req.setRequestStatus(AutopsyRequest.STATUS_APPROVED);
        when(autopsyRequestRepository.findById(10L)).thenReturn(Optional.of(req));

        assertThatThrownBy(() -> service.rejectAutopsy(10L, "reason"))
                .isInstanceOf(InvalidMortuaryRequestException.class)
                .hasMessageContaining("PENDING");
    }

    // --------------------------------------------------------------- report

    @Test
    void getReport_returnsCounts() {
        DeadBody b1 = body(1L, DeadBody.STATUS_ADMITTED);
        DeadBody b2 = body(2L, DeadBody.STATUS_RELEASED);
        DeadBody b3 = body(3L, DeadBody.STATUS_AUTOPSY_PENDING);
        when(deadBodyRepository.findAll()).thenReturn(List.of(b1, b2, b3));

        Map<String, Object> report = service.getReport(null);
        assertThat(report.get("total")).isEqualTo(3L);
        assertThat(report.get("admitted")).isEqualTo(1L);
        assertThat(report.get("released")).isEqualTo(1L);
        assertThat(report.get("autopsyPending")).isEqualTo(1L);
    }

    @Test
    void getReport_filteredByStatus() {
        DeadBody b1 = body(1L, DeadBody.STATUS_ADMITTED);
        when(deadBodyRepository.findByBodyStatus(DeadBody.STATUS_ADMITTED)).thenReturn(List.of(b1));

        Map<String, Object> report = service.getReport(DeadBody.STATUS_ADMITTED);
        assertThat(report.get("total")).isEqualTo(1L);
        assertThat(report.get("admitted")).isEqualTo(1L);
    }

    // --------------------------------------------------------------- list

    @Test
    void listDeadBodies_byStatus() {
        DeadBody b1 = body(1L, DeadBody.STATUS_ADMITTED);
        when(deadBodyRepository.findByBodyStatus(DeadBody.STATUS_ADMITTED)).thenReturn(List.of(b1));

        List<DeadBody> result = service.listDeadBodies(DeadBody.STATUS_ADMITTED, null);
        assertThat(result).hasSize(1);
    }

    @Test
    void listDeadBodies_byPatientId() {
        DeadBody b1 = body(1L, DeadBody.STATUS_ADMITTED);
        when(deadBodyRepository.findByPatientId(201L)).thenReturn(List.of(b1));

        List<DeadBody> result = service.listDeadBodies(null, 201L);
        assertThat(result).hasSize(1);
    }

    @Test
    void listDeadBodies_all() {
        DeadBody b1 = body(1L, DeadBody.STATUS_ADMITTED);
        DeadBody b2 = body(2L, DeadBody.STATUS_RELEASED);
        when(deadBodyRepository.findAll()).thenReturn(List.of(b1, b2));

        List<DeadBody> result = service.listDeadBodies(null, null);
        assertThat(result).hasSize(2);
    }
}
