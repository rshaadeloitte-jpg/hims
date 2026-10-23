package com.hims.mortuary.application;

import com.hims.mortuary.application.exceptions.InvalidMortuaryRequestException;
import com.hims.mortuary.application.exceptions.MortuaryConflictException;
import com.hims.mortuary.application.exceptions.MortuaryEntityNotFoundException;
import com.hims.mortuary.domain.model.AutopsyRequest;
import com.hims.mortuary.domain.model.DeadBody;
import com.hims.mortuary.domain.model.MortuaryBase;
import com.hims.mortuary.domain.model.ReleaseCertificate;
import com.hims.mortuary.domain.repository.AutopsyRequestRepository;
import com.hims.mortuary.domain.repository.DeadBodyRepository;
import com.hims.mortuary.domain.repository.ReleaseCertificateRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Mortuary &amp; Death service (M28) — business logic for dead body registry,
 * autopsy requests, and release certificates.
 *
 * BR-M28-001: body status machine (ADMITTED -> RELEASED/AUTOPSY_PENDING/etc.)
 * BR-M28-002: autopsy request lifecycle (PENDING -> APPROVED -> COMPLETED/REJECTED)
 * BR-M28-003: release certificate lifecycle (REQUESTED -> APPROVED -> RELEASED/REJECTED)
 *
 * Integration with adt-service: admission_id links to t_admission (death linkage).
 */
@Service
public class MortuaryService {

    public static final long AUDIT_ACTOR_ID = 1L;   // MIG-004: principal id

    private final DeadBodyRepository deadBodyRepository;
    private final AutopsyRequestRepository autopsyRequestRepository;
    private final ReleaseCertificateRepository releaseCertificateRepository;

    @PersistenceContext
    private EntityManager em;

    public MortuaryService(DeadBodyRepository deadBodyRepository,
                           AutopsyRequestRepository autopsyRequestRepository,
                           ReleaseCertificateRepository releaseCertificateRepository) {
        this.deadBodyRepository = deadBodyRepository;
        this.autopsyRequestRepository = autopsyRequestRepository;
        this.releaseCertificateRepository = releaseCertificateRepository;
    }

    // ============================================================ dead body CRUD

    @Transactional
    public DeadBody createDeadBody(Long admissionId, Long patientId, String patientName,
                                    Timestamp deathDate, String causeOfDeath, String deathLocation,
                                    String nextOfKin, String nextOfKinRelation, String nextOfKinPhone,
                                    String chamberNo, Boolean policeCase, String mlcNo,
                                    String remark, Long unitId) {
        if (admissionId == null) {
            throw new InvalidMortuaryRequestException("MOR_REQ_INVALID", "admissionId is required");
        }
        if (patientId == null) {
            throw new InvalidMortuaryRequestException("MOR_REQ_INVALID", "patientId is required");
        }
        if (patientName == null || patientName.isBlank()) {
            throw new InvalidMortuaryRequestException("MOR_REQ_INVALID", "patientName is required");
        }
        if (deathDate == null) {
            throw new InvalidMortuaryRequestException("MOR_REQ_INVALID", "deathDate is required");
        }

        DeadBody body = new DeadBody();
        body.setId(deadBodyRepository.nextId());
        body.setAdmissionId(admissionId);
        body.setPatientId(patientId);
        body.setPatientName(patientName);
        body.setDeathDate(deathDate);
        body.setCauseOfDeath(causeOfDeath);
        body.setDeathLocation(deathLocation);
        body.setNextOfKin(nextOfKin);
        body.setNextOfKinRelation(nextOfKinRelation);
        body.setNextOfKinPhone(nextOfKinPhone);
        body.setBodyStatus(DeadBody.STATUS_ADMITTED);
        body.setChamberNo(chamberNo);
        body.setPoliceCase(policeCase != null && policeCase);
        body.setMlcNo(mlcNo);
        body.setRemark(remark);
        body.setUnitId(unitId);
        stampAudit(body, true);
        return deadBodyRepository.save(body);
    }

    @Transactional(readOnly = true)
    public DeadBody getDeadBody(Long id) {
        return deadBodyRepository.findById(id)
                .orElseThrow(() -> new MortuaryEntityNotFoundException("MOR_NOT_FOUND",
                        "Dead body " + id + " not found"));
    }

    @Transactional(readOnly = true)
    public DeadBody getDeadBodyByAdmissionId(Long admissionId) {
        return deadBodyRepository.findByAdmissionId(admissionId)
                .orElseThrow(() -> new MortuaryEntityNotFoundException("MOR_NOT_FOUND",
                        "Dead body for admission " + admissionId + " not found"));
    }

    @Transactional(readOnly = true)
    public List<DeadBody> listDeadBodies(String bodyStatus, Long patientId) {
        if (bodyStatus != null) {
            return deadBodyRepository.findByBodyStatus(bodyStatus);
        }
        if (patientId != null) {
            return deadBodyRepository.findByPatientId(patientId);
        }
        return deadBodyRepository.findAll();
    }

    @Transactional
    public DeadBody updateDeadBody(Long id, String causeOfDeath, String deathLocation,
                                    String nextOfKin, String nextOfKinRelation, String nextOfKinPhone,
                                    String chamberNo, String deathCertNo, String remark) {
        DeadBody body = getDeadBody(id);
        if (body.isReleased()) {
            throw new MortuaryConflictException("MOR_STATE",
                    "Dead body " + id + " is already released/cannot be modified");
        }
        if (causeOfDeath != null) body.setCauseOfDeath(causeOfDeath);
        if (deathLocation != null) body.setDeathLocation(deathLocation);
        if (nextOfKin != null) body.setNextOfKin(nextOfKin);
        if (nextOfKinRelation != null) body.setNextOfKinRelation(nextOfKinRelation);
        if (nextOfKinPhone != null) body.setNextOfKinPhone(nextOfKinPhone);
        if (chamberNo != null) body.setChamberNo(chamberNo);
        if (deathCertNo != null) body.setDeathCertNo(deathCertNo);
        if (remark != null) body.setRemark(remark);
        stampAudit(body, false);
        return deadBodyRepository.save(body);
    }

    // ============================================================ release

    @Transactional
    public ReleaseCertificate releaseBody(Long id, String handoverTo, String handoverRelation,
                                           String handoverPhone, String remark) {
        DeadBody body = getDeadBody(id);
        if (body.isReleased()) {
            throw new MortuaryConflictException("MOR_STATE",
                    "Dead body " + id + " is already released");
        }
        if (handoverTo == null || handoverTo.isBlank()) {
            throw new InvalidMortuaryRequestException("MOR_REQ_INVALID", "handoverTo is required for release");
        }

        // Create release certificate
        ReleaseCertificate cert = new ReleaseCertificate();
        cert.setId(releaseCertificateRepository.nextId());
        cert.setReleaseNo("MOR-RC-" + cert.getId());
        cert.setDeadBodyId(id);
        cert.setRequestDate(Timestamp.from(Instant.now()));
        cert.setRequestedBy(AUDIT_ACTOR_ID);
        cert.setReleaseStatus(ReleaseCertificate.STATUS_RELEASED);
        cert.setHandoverTo(handoverTo);
        cert.setHandoverRelation(handoverRelation);
        cert.setHandoverPhone(handoverPhone);
        cert.setHandoverDate(Timestamp.from(Instant.now()));
        cert.setRemark(remark);
        stampAudit(cert, true);
        releaseCertificateRepository.save(cert);

        // Update body status
        body.setBodyStatus(DeadBody.STATUS_RELEASED);
        body.setNextOfKin(handoverTo);
        body.setNextOfKinRelation(handoverRelation);
        body.setNextOfKinPhone(handoverPhone);
        stampAudit(body, false);
        deadBodyRepository.save(body);

        return cert;
    }

    // ============================================================ autopsy

    @Transactional
    public AutopsyRequest requestAutopsy(Long id, Long requestedBy, String reason) {
        DeadBody body = getDeadBody(id);
        if (body.isReleased()) {
            throw new MortuaryConflictException("MOR_STATE",
                    "Cannot request autopsy for released body " + id);
        }
        if (requestedBy == null) {
            throw new InvalidMortuaryRequestException("MOR_REQ_INVALID", "requestedBy is required");
        }

        AutopsyRequest request = new AutopsyRequest();
        request.setId(autopsyRequestRepository.nextId());
        request.setDeadBodyId(id);
        request.setRequestDate(Timestamp.from(Instant.now()));
        request.setRequestedBy(requestedBy);
        request.setRequestStatus(AutopsyRequest.STATUS_PENDING);
        request.setReason(reason);
        stampAudit(request, true);
        autopsyRequestRepository.save(request);

        // Update body status
        body.setBodyStatus(DeadBody.STATUS_AUTOPSY_PENDING);
        stampAudit(body, false);
        deadBodyRepository.save(body);

        return request;
    }

    @Transactional
    public AutopsyRequest approveAutopsy(Long autopsyId, Long doctorId) {
        AutopsyRequest request = autopsyRequestRepository.findById(autopsyId)
                .orElseThrow(() -> new MortuaryEntityNotFoundException("MOR_NOT_FOUND",
                        "Autopsy request " + autopsyId + " not found"));
        if (request.getRequestStatus() != AutopsyRequest.STATUS_PENDING) {
            throw new InvalidMortuaryRequestException("MOR_STATE",
                    "Autopsy request is not PENDING (current=" + request.getRequestStatus() + ")");
        }
        request.setRequestStatus(AutopsyRequest.STATUS_APPROVED);
        request.setDoctorId(doctorId);
        stampAudit(request, false);
        return autopsyRequestRepository.save(request);
    }

    @Transactional
    public AutopsyRequest completeAutopsy(Long autopsyId, Timestamp autopsyDate, String findings) {
        AutopsyRequest request = autopsyRequestRepository.findById(autopsyId)
                .orElseThrow(() -> new MortuaryEntityNotFoundException("MOR_NOT_FOUND",
                        "Autopsy request " + autopsyId + " not found"));
        if (request.getRequestStatus() != AutopsyRequest.STATUS_APPROVED) {
            throw new InvalidMortuaryRequestException("MOR_STATE",
                    "Autopsy request must be APPROVED before completion (current=" + request.getRequestStatus() + ")");
        }
        request.setRequestStatus(AutopsyRequest.STATUS_COMPLETED);
        request.setAutopsyDate(autopsyDate != null ? autopsyDate : Timestamp.from(Instant.now()));
        request.setFindings(findings);
        stampAudit(request, false);
        autopsyRequestRepository.save(request);

        // Update body status
        DeadBody body = getDeadBody(request.getDeadBodyId());
        body.setBodyStatus(DeadBody.STATUS_AUTOPSY_COMPLETE);
        stampAudit(body, false);
        deadBodyRepository.save(body);

        return request;
    }

    @Transactional
    public AutopsyRequest rejectAutopsy(Long autopsyId, String rejectionReason) {
        AutopsyRequest request = autopsyRequestRepository.findById(autopsyId)
                .orElseThrow(() -> new MortuaryEntityNotFoundException("MOR_NOT_FOUND",
                        "Autopsy request " + autopsyId + " not found"));
        if (request.getRequestStatus() != AutopsyRequest.STATUS_PENDING) {
            throw new InvalidMortuaryRequestException("MOR_STATE",
                    "Only PENDING autopsy requests can be rejected (current=" + request.getRequestStatus() + ")");
        }
        request.setRequestStatus(AutopsyRequest.STATUS_REJECTED);
        request.setRejectionReason(rejectionReason);
        stampAudit(request, false);
        autopsyRequestRepository.save(request);

        // Revert body status
        DeadBody body = getDeadBody(request.getDeadBodyId());
        body.setBodyStatus(DeadBody.STATUS_ADMITTED);
        stampAudit(body, false);
        deadBodyRepository.save(body);

        return request;
    }

    // ============================================================ report

    @Transactional(readOnly = true)
    public Map<String, Object> getReport(String bodyStatus) {
        List<DeadBody> bodies = (bodyStatus != null)
                ? deadBodyRepository.findByBodyStatus(bodyStatus)
                : deadBodyRepository.findAll();

        long admitted = bodies.stream().filter(b -> DeadBody.STATUS_ADMITTED.equals(b.getBodyStatus())).count();
        long released = bodies.stream().filter(b -> DeadBody.STATUS_RELEASED.equals(b.getBodyStatus())).count();
        long autopsyPending = bodies.stream().filter(b -> DeadBody.STATUS_AUTOPSY_PENDING.equals(b.getBodyStatus())).count();
        long autopsyComplete = bodies.stream().filter(b -> DeadBody.STATUS_AUTOPSY_COMPLETE.equals(b.getBodyStatus())).count();
        long policeCase = bodies.stream().filter(b -> DeadBody.STATUS_POLICE_CASE.equals(b.getBodyStatus())).count();
        long handedOver = bodies.stream().filter(b -> DeadBody.STATUS_HANDED_OVER.equals(b.getBodyStatus())).count();

        return Map.of(
                "total", bodies.size(),
                "admitted", admitted,
                "released", released,
                "autopsyPending", autopsyPending,
                "autopsyComplete", autopsyComplete,
                "policeCase", policeCase,
                "handedOver", handedOver,
                "bodies", bodies
        );
    }

    // ============================================================ helpers

    private void stampAudit(MortuaryBase entity, boolean create) {
        Timestamp now = Timestamp.from(Instant.now());
        if (create) {
            entity.setAddedBy(AUDIT_ACTOR_ID);
            entity.setAddedDatetime(now);
        }
        entity.setUpdatedBy(AUDIT_ACTOR_ID);
        entity.setUpdatedDatetime(now);
    }
}
