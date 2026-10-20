package com.hims.kiosk.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hims.kiosk.application.exceptions.InvalidKioskRequestException;
import com.hims.kiosk.application.exceptions.KioskNotFoundException;
import com.hims.kiosk.domain.model.*;
import com.hims.kiosk.domain.repository.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

@Service
public class KioskService {
    private static final Logger log = LoggerFactory.getLogger(KioskService.class);
    public static final long AUDIT_ACTOR_ID = 1L;

    private final KioskRegistrationRepository regRepo;
    private final KioskAppointmentRepository apptRepo;
    private final KioskQueueRepository queueRepo;
    private final KioskLabResultRepository labRepo;
    private final ObjectMapper mapper = new ObjectMapper();

    @PersistenceContext private EntityManager em;

    public KioskService(KioskRegistrationRepository regRepo,
                        KioskAppointmentRepository apptRepo,
                        KioskQueueRepository queueRepo,
                        KioskLabResultRepository labRepo) {
        this.regRepo = regRepo;
        this.apptRepo = apptRepo;
        this.queueRepo = queueRepo;
        this.labRepo = labRepo;
    }

    // ---------------- Self-registration ----------------
    @Transactional
    public KioskRegistration register(JsonNode body, Long unitId) {
        KioskRegistration r = bind(body, KioskRegistration.class);
        if (r.getPatientName() == null || r.getPatientName().isBlank()
                || r.getContactPhone() == null || r.getContactPhone().isBlank()) {
            throw new InvalidKioskRequestException("BEXA003", "patientName and contactPhone required");
        }
        r.setId(nextId("kiosk_reg_seq"));
        r.setRegistrationDate(LocalDate.now());
        r.setRegistrationToken(UUID.randomUUID().toString().substring(0, 32));
        r.setStatus(true);
        r.setAddedBy(AUDIT_ACTOR_ID);
        r.setAddedDatetime(Timestamp.from(Instant.now()));
        if (unitId != null) r.setUnitId(unitId);
        return regRepo.save(r);
    }

    public Optional<KioskRegistration> findByToken(String token) {
        return regRepo.findByRegistrationToken(token);
    }

    // ---------------- Self-appointment ----------------
    @Transactional
    public KioskAppointment bookAppointment(JsonNode body, Long unitId) {
        KioskAppointment a = bind(body, KioskAppointment.class);
        if (a.getPatientId() == null || a.getAppointmentDate() == null) {
            throw new InvalidKioskRequestException("BEXA003", "patientId and appointmentDate required");
        }
        a.setId(nextId("kiosk_appt_seq"));
        a.setStatus(true);
        a.setStatusCode((short) 1); // BOOKED
        a.setVisitType(a.getVisitType() != null ? a.getVisitType() : "SELF_APPOINTMENT");
        a.setAddedBy(AUDIT_ACTOR_ID);
        a.setAddedDatetime(Timestamp.from(Instant.now()));
        if (unitId != null) a.setUnitId(unitId);
        return apptRepo.save(a);
    }

    public List<KioskAppointment> listAppointments(LocalDate date) {
        return apptRepo.findByAppointmentDateAndStatusTrue(date != null ? date : LocalDate.now());
    }

    // ---------------- Kiosk queue ----------------
    @Transactional
    public KioskQueue issueToken(Long patientId, Long deptId) {
        if (patientId == null || deptId == null) {
            throw new InvalidKioskRequestException("BEXA003", "patientId and departmentId required");
        }
        KioskQueue q = new KioskQueue();
        q.setId(nextId("kiosk_queue_seq"));
        q.setPatientId(patientId);
        q.setDepartmentId(deptId);
        q.setQueueDate(LocalDate.now());
        q.setQueueStatus((short) 1); // WAITING
        q.setPriority((short) 1); // NORMAL
        q.setTokenNo(nextDailyTokenNo());
        q.setStatus(true);
        q.setAddedBy(AUDIT_ACTOR_ID);
        q.setAddedDatetime(Timestamp.from(Instant.now()));
        return queueRepo.save(q);
    }

    public List<KioskQueue> listQueue(LocalDate date, Short status) {
        return queueRepo.findByQueueDateAndStatusTrueAndQueueStatus(
                date != null ? date : LocalDate.now(), status != null ? status : (short) 1);
    }

    // ---------------- Lab result upload ----------------
    @Transactional
    public KioskLabResult uploadResult(JsonNode body, Long unitId) {
        KioskLabResult lr = bind(body, KioskLabResult.class);
        if (lr.getPatientId() == null) {
            throw new InvalidKioskRequestException("BEXA003", "patientId required");
        }
        lr.setId(nextId("kiosk_lab_seq"));
        lr.setStatus(true);
        lr.setUploadedAt(Timestamp.from(Instant.now()));
        lr.setVerified(false);
        lr.setAddedBy(AUDIT_ACTOR_ID);
        lr.setAddedDatetime(Timestamp.from(Instant.now()));
        if (unitId != null) lr.setUnitId(unitId);
        return labRepo.save(lr);
    }

    public List<KioskLabResult> listLabResults(Long patientId) {
        return labRepo.findByPatientIdAndStatusTrue(patientId);
    }

    // ---------------- Dashboard ----------------
    public Map<String, Object> kioskDashboard() {
        LocalDate today = LocalDate.now();
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("date", today.toString());
        m.put("waiting", queueRepo.countByQueueDateAndStatusTrueAndQueueStatus(today, (short) 1));
        m.put("called", queueRepo.countByQueueDateAndStatusTrueAndQueueStatus(today, (short) 2));
        m.put("completed", queueRepo.countByQueueDateAndStatusTrueAndQueueStatus(today, (short) 3));
        return m;
    }

    // ---------------- Helpers ----------------
    @SuppressWarnings("unchecked")
    private <T> T bind(JsonNode payload, Class<T> type) {
        if (payload == null || payload.isNull() || payload.isMissingNode()) {
            throw new InvalidKioskRequestException("BEXA003", "Invalid input data !");
        }
        try {
            return mapper.treeToValue(payload, type);
        } catch (Exception e) {
            throw new InvalidKioskRequestException("BEXA003", "Invalid input data !");
        }
    }

    private long nextId(String seq) {
        return ((Number) em.createNativeQuery("SELECT nextval('ihms_kiosk.\"" + seq + "\"')").getSingleResult()).longValue();
    }

    private long nextDailyTokenNo() {
        // Simple daily counter: could be DB-based; using sequence for now
        return nextId("kiosk_queue_seq");
    }
}
