package com.hims.equipment.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hims.equipment.application.exceptions.EquipmentNotFoundException;
import com.hims.equipment.application.exceptions.InvalidEquipmentRequestException;
import com.hims.equipment.domain.model.ComplaintStatus;
import com.hims.equipment.domain.model.EquipmentBase;
import com.hims.equipment.domain.model.EquipmentComplaint;
import com.hims.equipment.domain.model.EquipmentComplaintResolution;
import com.hims.equipment.domain.model.EquipmentComplaintResolutionDetails;
import com.hims.equipment.domain.repository.EquipmentComplaintRepository;
import com.hims.equipment.domain.repository.EquipmentComplaintResolutionDetailsRepository;
import com.hims.equipment.domain.repository.EquipmentComplaintResolutionRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Complaint resolution lifecycle (MIG-023 Phase 2, S18) — clean target of the
 * legacy t_eqpcomplaintresolution flows.
 *
 * <p>Preserved conventions:
 * <ul>
 *   <li>server-owned ids from explicit sequences (IMP-DEC-003) via native
 *       nextval — no auto-increment;</li>
 *   <li>a resolution moves its complaint to {@code resolvedStatus} (a
 *       complaint_status_master id, default RESOLVED(3)) and stamps the
 *       complaint's {@code resolved_date} — log row + status move commit in one
 *       transaction;</li>
 *   <li>{@code resolutionNotes} maps legacy REMARKS; structured replaced-part
 *       lines ride along as optional nested {@code details[]} rows.</li>
 * </ul>
 *
 * <p>Documented improvement: PUT only edits the resolution record — the status
 * transition is owned by add-resolution, so {@code resolvedStatus} is not
 * patchable after the fact (re-open/re-close flows go through new resolutions).
 */
@Service
public class ComplaintResolutionService {

    private static final Logger log = LoggerFactory.getLogger(ComplaintResolutionService.class);

    public static final long AUDIT_ACTOR_ID = 1L;   // MIG-004: principal id

    private final EquipmentComplaintRepository complaintRepository;
    private final EquipmentComplaintResolutionRepository resolutionRepository;
    private final EquipmentComplaintResolutionDetailsRepository resolutionDetailsRepository;
    private final ObjectMapper objectMapper;

    @PersistenceContext
    private EntityManager em;

    public ComplaintResolutionService(EquipmentComplaintRepository complaintRepository,
                                      EquipmentComplaintResolutionRepository resolutionRepository,
                                      EquipmentComplaintResolutionDetailsRepository resolutionDetailsRepository,
                                      ObjectMapper objectMapper) {
        this.complaintRepository = complaintRepository;
        this.resolutionRepository = resolutionRepository;
        this.resolutionDetailsRepository = resolutionDetailsRepository;
        this.objectMapper = objectMapper;
    }

    // ------------------------------------------------------------------- add

    /**
     * Add a resolution to a complaint and move the complaint to
     * {@code resolvedStatus} (default RESOLVED(3), must be a valid dictionary
     * id 1..4). resolutionDate defaults to now. Optional nested {@code details[]}
     * array saves replaced-part line items. Atomic: resolution row(s) + complaint
     * status move commit together.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public EquipmentComplaintResolution add(Long complaintId, JsonNode payload) {
        if (payload == null || payload.isNull() || payload.isMissingNode()) {
            throw new InvalidEquipmentRequestException("BEXA003", "Invalid input data !");
        }
        EquipmentComplaint complaint = getComplaint(complaintId);
        EquipmentComplaintResolution resolution = bind(payload, EquipmentComplaintResolution.class);
        if (resolution.getResolvedStatus() == null) {
            resolution.setResolvedStatus(ComplaintStatus.RESOLVED);
        } else if (!ComplaintStatus.isValid(resolution.getResolvedStatus())) {
            throw new InvalidEquipmentRequestException("BEXA003",
                    "Invalid resolvedStatus: " + resolution.getResolvedStatus());
        }
        Timestamp now = Timestamp.from(Instant.now());
        Timestamp resolutionDate = resolution.getResolutionDate() == null ? now : resolution.getResolutionDate();
        resolution.setResolutionDate(resolutionDate);

        resolution.setId(nextId("equipment_complaint_resolution_seq"));
        resolution.setComplaintId(complaint.getId());
        resolution.setStatus(resolution.getStatus() == null ? true : resolution.getStatus());
        stampCreate(resolution, complaint.getUnitId());
        // Parent row MUST be inserted before its detail lines: there is no JPA
        // relationship mapping between the entities, so Hibernate has no
        // dependency information and would otherwise flush the child INSERTs
        // first — failing the resolution_id FK at commit time.
        EquipmentComplaintResolution saved = resolutionRepository.save(resolution);
        List<EquipmentComplaintResolutionDetails> lines = bindDetailLines(payload);
        for (EquipmentComplaintResolutionDetails line : lines) {
            line.setId(nextId("equipment_complaint_resolution_details_seq"));
            line.setResolutionId(saved.getId());
            line.setStatus(line.getStatus() == null ? true : line.getStatus());
            stampCreate(line, complaint.getUnitId());
            resolutionDetailsRepository.save(line);
        }

        complaint.setComplaintStatus(resolution.getResolvedStatus());
        if (resolution.getResolvedStatus() >= ComplaintStatus.RESOLVED
                && complaint.getResolvedDate() == null) {
            complaint.setResolvedDate(resolutionDate);
        }
        stampUpdate(complaint);
        complaintRepository.save(complaint);
        log.info("Resolution {} added to complaint {} -> status {}", saved.getId(),
                complaint.getId(), resolution.getResolvedStatus());
        return saved;
    }

    // ---------------------------------------------------------------- update

    /**
     * Update a resolution record (null patch fields ignored; id/complaintId/
     * unit/audit columns server-owned; resolvedStatus intentionally not
     * patchable — see class doc). The resolution must belong to the given
     * complaint.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public EquipmentComplaintResolution update(Long complaintId, Long resId, JsonNode payload) {
        if (payload == null || payload.isNull() || payload.isMissingNode()) {
            throw new InvalidEquipmentRequestException("BEXA003", "Invalid input data !");
        }
        getComplaint(complaintId); // 404 for unknown complaint
        EquipmentComplaintResolution existing = resolutionRepository.findById(resId)
                .orElseThrow(() -> new EquipmentNotFoundException("NOT_FOUND", "Resolution not found"));
        if (!existing.getComplaintId().equals(complaintId)) {
            throw new InvalidEquipmentRequestException("BEXA003",
                    "Resolution " + resId + " does not belong to complaint " + complaintId);
        }
        EquipmentComplaintResolution patch = bind(payload, EquipmentComplaintResolution.class);
        applyUpdate(existing, patch);
        stampUpdate(existing);
        return resolutionRepository.save(existing);
    }

    // ----------------------------------------------------------------- reads

    /** Resolutions of one complaint — chronological (oldest first). */
    @Transactional(readOnly = true)
    public List<EquipmentComplaintResolution> listResolutions(Long complaintId) {
        getComplaint(complaintId); // 404 for unknown complaint
        return resolutionRepository.findByComplaintIdOrderByIdAsc(complaintId);
    }

    // --------------------------------------------------------------- helpers

    private EquipmentComplaint getComplaint(Long id) {
        return complaintRepository.findById(id)
                .orElseThrow(() -> new EquipmentNotFoundException("NOT_FOUND", "Complaint not found"));
    }

    /** Optional {@code details[]} replaced-part lines on an add/update payload. */
    private List<EquipmentComplaintResolutionDetails> bindDetailLines(JsonNode payload) {
        List<EquipmentComplaintResolutionDetails> lines = new ArrayList<>();
        JsonNode details = payload.path("details");
        if (details.isArray()) {
            for (JsonNode node : details) {
                try {
                    lines.add(objectMapper.treeToValue(node, EquipmentComplaintResolutionDetails.class));
                } catch (Exception e) {
                    throw new InvalidEquipmentRequestException("BEXA003", "Invalid input data !");
                }
            }
        }
        return lines;
    }

    private <T> T bind(JsonNode payload, Class<T> type) {
        try {
            return objectMapper.treeToValue(payload, type);
        } catch (Exception e) {
            throw new InvalidEquipmentRequestException("BEXA003", "Invalid input data !");
        }
    }

    /** Null-safe field copy from the patch onto the managed entity (waste-service
     *  pattern). Skip list keeps id/complaintId/unit/audit columns and
     *  resolvedStatus server-owned. */
    private void applyUpdate(EquipmentComplaintResolution existing, EquipmentComplaintResolution patch) {
        for (Class<?> c = patch.getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
            for (Field field : c.getDeclaredFields()) {
                int mods = field.getModifiers();
                if (Modifier.isStatic(mods) || Modifier.isFinal(mods)
                        || field.getName().startsWith("_persistence_")
                        || field.getName().equals("id") || field.getName().equals("unitId")
                        || field.getName().equals("complaintId") || field.getName().equals("resolvedStatus")
                        || field.getName().equals("addedBy") || field.getName().equals("addedDatetime")
                        || field.getName().equals("updatedBy") || field.getName().equals("updatedDatetime")
                        || field.getName().equals("createdUnitId") || field.getName().equals("updatedUnitId")
                        || field.getName().equals("synchronizedd")) {
                    continue;
                }
                try {
                    field.setAccessible(true);
                    Object value = field.get(patch);
                    if (value != null) {
                        field.set(existing, value);
                    }
                } catch (IllegalAccessException e) {
                    log.warn("Cannot copy field {} on update", field.getName());
                }
            }
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

    private void stampUpdate(EquipmentBase entity) {
        entity.setUpdatedBy(AUDIT_ACTOR_ID);
        entity.setUpdatedDatetime(Timestamp.from(Instant.now()));
    }

    private long nextId(String sequenceName) {
        return ((Number) em.createNativeQuery("SELECT nextval('ihms_equipment.\"" + sequenceName + "\"')")
                .getSingleResult()).longValue();
    }
}
