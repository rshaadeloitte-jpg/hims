package com.hims.equipment.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hims.equipment.application.exceptions.EquipmentNotFoundException;
import com.hims.equipment.application.exceptions.InvalidEquipmentRequestException;
import com.hims.equipment.domain.model.Equipment;
import com.hims.equipment.domain.model.EquipmentBase;
import com.hims.equipment.domain.model.EquipmentDetailDocs;
import com.hims.equipment.domain.repository.EquipmentDetailDocsRepository;
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
import java.util.List;

/**
 * Equipment document metadata management (MIG-023 Phase 5, S18) — clean target
 * of the legacy T_EQUIPMENTDETAILDOCS flows on the V1 equipment_detail_docs
 * table.
 *
 * <p>Preserved conventions:
 * <ul>
 *   <li>server-owned ids from explicit sequences (IMP-DEC-003) via native
 *       nextval — no auto-increment;</li>
 *   <li>metadata-only — {@code filePath} names where the file lives, no bytes
 *       are stored or transferred by this service;</li>
 *   <li>{@code equipmentId} is server-owned from the path on upload;</li>
 *   <li>delete is a soft delete (status=false) so the audit trail and any
 *       historical references survive (waste-service deleteBioRequest
 *       convention); a deleted document reads as absent afterwards.</li>
 * </ul>
 *
 * <p>Documented improvements: {@code fileName} is required (an unnamed
 * metadata row is unidentifiable), and uploads are rejected for unknown (404)
 * and inactive (BEXA003) assets. Unlike complaints / PM schedules / warranties,
 * CONDEMNED assets still ACCEPT documents — the condemnation report, disposal
 * paperwork and final calibration certificates are exactly what gets attached
 * around condemnation time.
 */
@Service
public class EquipmentDocumentService {

    private static final Logger log = LoggerFactory.getLogger(EquipmentDocumentService.class);

    public static final long AUDIT_ACTOR_ID = 1L;   // MIG-004: principal id

    private final EquipmentRepository equipmentRepository;
    private final EquipmentDetailDocsRepository docsRepository;
    private final ObjectMapper objectMapper;

    @PersistenceContext
    private EntityManager em;

    public EquipmentDocumentService(EquipmentRepository equipmentRepository,
                                    EquipmentDetailDocsRepository docsRepository,
                                    ObjectMapper objectMapper) {
        this.equipmentRepository = equipmentRepository;
        this.docsRepository = docsRepository;
        this.objectMapper = objectMapper;
    }

    // ---------------------------------------------------------------- upload

    /**
     * Attach document metadata to an asset. Server stamps id
     * (equipment_detail_docs_seq) and pairs {@code equipmentId} from the path;
     * {@code fileName} is required, all other fields optional. Asset must
     * exist and be active (condemned assets still accept documents).
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public EquipmentDetailDocs upload(Long equipmentId, JsonNode payload, Long unitId) {
        if (payload == null || payload.isNull() || payload.isMissingNode()) {
            throw new InvalidEquipmentRequestException("BEXA003", "Invalid input data !");
        }
        Equipment asset = requireActiveAsset(equipmentId);
        EquipmentDetailDocs doc = bind(payload);
        if (doc.getFileName() == null || doc.getFileName().isBlank()) {
            throw new InvalidEquipmentRequestException("BEXA003",
                    "fileName is required for a document");
        }
        doc.setFileName(doc.getFileName().trim());
        if (doc.getDocType() != null && doc.getDocType().isBlank()) {
            doc.setDocType(null);                       // blank type == absent
        } else if (doc.getDocType() != null) {
            doc.setDocType(doc.getDocType().trim());
        }
        doc.setId(nextId("equipment_detail_docs_seq"));
        doc.setEquipmentId(asset.getId());              // server-owned pairing
        doc.setStatus(doc.getStatus() == null ? true : doc.getStatus());
        stampCreate(doc, unitId);
        EquipmentDetailDocs saved = docsRepository.save(doc);
        log.info("Document {} ({}) attached to equipment {} file={}",
                saved.getId(), saved.getDocType(), saved.getEquipmentId(), saved.getFileName());
        return saved;
    }

    // ----------------------------------------------------------------- reads

    /** Active documents of one asset — newest first. */
    @Transactional(readOnly = true)
    public List<EquipmentDetailDocs> listByEquipment(Long equipmentId) {
        requireAsset(equipmentId); // 404 for unknown asset even when it has no docs
        return docsRepository.findByEquipmentIdAndStatusTrueOrderByIdDesc(equipmentId);
    }

    // ---------------------------------------------------------------- delete

    /**
     * Soft-delete a document (status=false + update stamps). Unknown or already
     * deleted documents read as absent (404). Returns the tombstoned row.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public EquipmentDetailDocs delete(Long docId) {
        EquipmentDetailDocs doc = docsRepository.findByIdAndStatusTrue(docId)
                .orElseThrow(() -> new EquipmentNotFoundException("NOT_FOUND",
                        "Document not found: " + docId));
        doc.setStatus(false);
        stampUpdate(doc);
        EquipmentDetailDocs saved = docsRepository.save(doc);
        log.info("Document {} ({}) deleted from equipment {}",
                saved.getId(), saved.getFileName(), saved.getEquipmentId());
        return saved;
    }

    // --------------------------------------------------------------- helpers

    /** Existence only (list endpoint works for any real asset row). */
    private Equipment requireAsset(Long equipmentId) {
        return equipmentRepository.findById(equipmentId)
                .orElseThrow(() -> new EquipmentNotFoundException("NOT_FOUND",
                        "Equipment not found: " + equipmentId));
    }

    /** Existence + activity guard for writes (condemned assets allowed). */
    private Equipment requireActiveAsset(Long equipmentId) {
        Equipment asset = requireAsset(equipmentId);
        if (!Boolean.TRUE.equals(asset.getStatus())) {
            throw new InvalidEquipmentRequestException("BEXA003",
                    "Cannot attach a document to an inactive asset");
        }
        return asset;
    }

    private EquipmentDetailDocs bind(JsonNode payload) {
        try {
            return objectMapper.treeToValue(payload, EquipmentDetailDocs.class);
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

    private void stampUpdate(EquipmentBase entity) {
        entity.setUpdatedBy(AUDIT_ACTOR_ID);
        entity.setUpdatedDatetime(Timestamp.from(Instant.now()));
    }

    private long nextId(String sequenceName) {
        return ((Number) em.createNativeQuery("SELECT nextval('ihms_equipment.\"" + sequenceName + "\"')")
                .getSingleResult()).longValue();
    }
}
