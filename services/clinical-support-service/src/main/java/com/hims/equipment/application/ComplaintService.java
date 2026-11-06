package com.hims.equipment.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hims.equipment.application.exceptions.EquipmentNotFoundException;
import com.hims.equipment.application.exceptions.InvalidEquipmentRequestException;
import com.hims.equipment.domain.model.ComplaintStatus;
import com.hims.equipment.domain.model.Equipment;
import com.hims.equipment.domain.model.EquipmentBase;
import com.hims.equipment.domain.model.EquipmentComplaint;
import com.hims.equipment.domain.model.EquipmentComplaintDetails;
import com.hims.equipment.domain.model.EquipmentWorkingStatus;
import com.hims.equipment.domain.repository.EquipmentComplaintDetailsRepository;
import com.hims.equipment.domain.repository.EquipmentComplaintRepository;
import com.hims.equipment.domain.repository.EquipmentRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

/**
 * Equipment complaint lifecycle (MIG-023 Phase 2, S18) — clean target of the
 * legacy T_EqpComplaint raise/search flows.
 *
 * <p>Preserved conventions:
 * <ul>
 *   <li>server-owned ids from explicit sequences (IMP-DEC-003) via native
 *       nextval — no auto-increment;</li>
 *   <li>complaint number rule {@code IHMS/EQ/{yyyy}/{%05d}} (grievance-service
 *       ticket-number pattern) driven by ihms_equipment.equipment_complaint_no_seq,
 *       unique per partial index uq_equipment_complaint_no;</li>
 *   <li>{@code complaintStatus} holds a complaint_status_master id
 *       ({@link ComplaintStatus}, default OPEN);</li>
 *   <li>raise-time snapshot of accessionNo / department / store from the asset —
 *       the asset may transfer later, the complaint keeps where it was reported.</li>
 * </ul>
 *
 * <p>Documented improvement: complaints can only be raised on an existing,
 * active, non-condemned asset (clean BEXA003/404 instead of raw constraint
 * violations).
 */
@Service
public class ComplaintService {

    private static final Logger log = LoggerFactory.getLogger(ComplaintService.class);

    public static final long AUDIT_ACTOR_ID = 1L;   // MIG-004: principal id
    public static final String COMPLAINT_NO_PREFIX = "IHMS/EQ/";

    private final EquipmentRepository equipmentRepository;
    private final EquipmentComplaintRepository complaintRepository;
    private final EquipmentComplaintDetailsRepository complaintDetailsRepository;
    private final ObjectMapper objectMapper;

    @PersistenceContext
    private EntityManager em;

    public ComplaintService(EquipmentRepository equipmentRepository,
                            EquipmentComplaintRepository complaintRepository,
                            EquipmentComplaintDetailsRepository complaintDetailsRepository,
                            ObjectMapper objectMapper) {
        this.equipmentRepository = equipmentRepository;
        this.complaintRepository = complaintRepository;
        this.complaintDetailsRepository = complaintDetailsRepository;
        this.objectMapper = objectMapper;
    }

    // ------------------------------------------------------------------ raise

    /**
     * Raise a complaint against an asset. Server stamps complaintNo
     * ({@code IHMS/EQ/{yyyy}/{%05d}}), complaintDate/reportedDate (now unless
     * given) and the OPEN(1) default status. Asset existence/activity are
     * validated first so callers never see constraint violations; accession /
     * department / store are snapshotted from the asset when the payload omits
     * them. Payload: {@code {equipmentId (required), categoryId?, complaintTypeId?,
     * severityId?, description?, complaintDate?, reportedDate?, reportedBy?, ...}}.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public EquipmentComplaint raise(JsonNode payload, Long unitId) {
        if (payload == null || payload.isNull() || payload.isMissingNode()) {
            throw new InvalidEquipmentRequestException("BEXA003", "Invalid input data !");
        }
        EquipmentComplaint complaint = bind(payload, EquipmentComplaint.class);
        if (complaint.getEquipmentId() == null) {
            throw new InvalidEquipmentRequestException("BEXA003", "equipmentId is required");
        }
        Equipment asset = equipmentRepository.findById(complaint.getEquipmentId())
                .orElseThrow(() -> new EquipmentNotFoundException("NOT_FOUND",
                        "Equipment not found: " + complaint.getEquipmentId()));
        if (!Boolean.TRUE.equals(asset.getStatus())) {
            throw new InvalidEquipmentRequestException("BEXA003",
                    "Cannot raise a complaint on an inactive asset");
        }
        if (asset.getWorkingStatus() != null
                && asset.getWorkingStatus() == EquipmentWorkingStatus.CONDEMNED) {
            throw new InvalidEquipmentRequestException("BEXA003",
                    "Cannot raise a complaint on a condemned asset");
        }
        if (complaint.getComplaintStatus() == null) {
            complaint.setComplaintStatus(ComplaintStatus.OPEN);
        } else if (!ComplaintStatus.isValid(complaint.getComplaintStatus())) {
            throw new InvalidEquipmentRequestException("BEXA003",
                    "Invalid complaintStatus: " + complaint.getComplaintStatus());
        }
        if (complaint.getAccessionNo() == null) {
            complaint.setAccessionNo(asset.getAccessionNo());
        }
        if (complaint.getDepartmentId() == null) {
            complaint.setDepartmentId(asset.getDepartmentId());
        }
        if (complaint.getStoreId() == null) {
            complaint.setStoreId(asset.getStoreId());
        }
        Timestamp now = Timestamp.from(Instant.now());
        if (complaint.getComplaintDate() == null) {
            complaint.setComplaintDate(now);
        }
        if (complaint.getReportedDate() == null) {
            complaint.setReportedDate(now);
        }
        complaint.setId(nextId("equipment_complaint_seq"));
        complaint.setComplaintNo(generateComplaintNo());
        complaint.setStatus(complaint.getStatus() == null ? true : complaint.getStatus());
        stampCreate(complaint, unitId);
        EquipmentComplaint saved = complaintRepository.save(complaint);
        log.info("Complaint raised id={} no={} equipment={}",
                saved.getId(), saved.getComplaintNo(), saved.getEquipmentId());
        return saved;
    }

    // ----------------------------------------------------------------- search

    /** Complaint listing/search (unit, asset, status dictionary id, free text). */
    @Transactional(readOnly = true)
    public List<EquipmentComplaint> search(Long unitId, Long equipmentId, Long complaintStatus,
                                           String searchText) {
        if (complaintStatus != null && !ComplaintStatus.isValid(complaintStatus)) {
            throw new InvalidEquipmentRequestException("BEXA003",
                    "Invalid complaintStatus: " + complaintStatus);
        }
        String trimmed = (searchText == null || searchText.isBlank()) ? null : searchText.trim();
        // Sentinel values (0L / "") are bound instead of null: PostgreSQL cannot
        // type an untyped null parameter; the boolean gates ignore them when absent.
        return complaintRepository.search(
                unitId == null ? 0L : unitId, unitId != null,
                equipmentId == null ? 0L : equipmentId, equipmentId != null,
                complaintStatus == null ? 0L : complaintStatus, complaintStatus != null,
                trimmed == null ? "" : trimmed, trimmed != null);
    }

    // ------------------------------------------------------------------ reads

    @Transactional(readOnly = true)
    public EquipmentComplaint getComplaint(Long id) {
        return complaintRepository.findById(id)
                .orElseThrow(() -> new EquipmentNotFoundException("NOT_FOUND", "Complaint not found"));
    }

    /** Line items of one complaint (entry order). */
    @Transactional(readOnly = true)
    public List<EquipmentComplaintDetails> detailsOf(Long complaintId) {
        getComplaint(complaintId); // 404 for unknown complaint
        return complaintDetailsRepository.findByComplaintIdOrderByIdAsc(complaintId);
    }

    /** Complaint history of one asset — newest first. */
    @Transactional(readOnly = true)
    public List<EquipmentComplaint> history(Long equipmentId) {
        equipmentRepository.findById(equipmentId)
                .orElseThrow(() -> new EquipmentNotFoundException("NOT_FOUND",
                        "Equipment not found: " + equipmentId));
        return complaintRepository.findByEquipmentIdAndStatusTrueOrderByIdDesc(equipmentId);
    }

    // --------------------------------------------------------------- helpers

    /** Complaint number rule: IHMS/EQ/{yyyy}/{%05d seq}. */
    private String generateComplaintNo() {
        long seq = nextId("equipment_complaint_no_seq");
        int year = LocalDate.now(ZoneId.systemDefault()).getYear();
        return COMPLAINT_NO_PREFIX + year + "/" + String.format("%05d", seq);
    }

    private <T> T bind(JsonNode payload, Class<T> type) {
        try {
            return objectMapper.treeToValue(payload, type);
        } catch (Exception e) {
            throw new InvalidEquipmentRequestException("BEXA003", "Invalid input data !");
        }
    }

    private void stampCreate(EquipmentBase entity, Long unitId) {
        entity.setAddedBy(AUDIT_ACTOR_ID);
        entity.setAddedDatetime(Timestamp.from(Instant.now()));
        entity.setUpdatedBy(null);
        entity.setUpdatedDatetime(null);
        entity.setSynchronizedd(false);
        entity.setCreatedUnitId(unitId);
        entity.setUpdatedUnitId(unitId);
        if (unitId != null) {
            entity.setUnitId(unitId);
        }
    }

    private long nextId(String sequenceName) {
        return ((Number) em.createNativeQuery("SELECT nextval('ihms_equipment.\"" + sequenceName + "\"')")
                .getSingleResult()).longValue();
    }
}
