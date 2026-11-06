package com.hims.equipment.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hims.equipment.application.exceptions.EquipmentNotFoundException;
import com.hims.equipment.application.exceptions.InvalidEquipmentRequestException;
import com.hims.equipment.domain.model.Equipment;
import com.hims.equipment.domain.model.EquipmentBase;
import com.hims.equipment.domain.model.EquipmentTransfer;
import com.hims.equipment.domain.model.EquipmentWorkingStatus;
import com.hims.equipment.domain.repository.EquipmentRepository;
import com.hims.equipment.domain.repository.EquipmentTransferRepository;
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
import java.util.List;

/**
 * Equipment asset-register lifecycle (MIG-023 sub-slice, S18). Clean target of
 * legacy t_equipmentdetails / M_EQUIPMENTDETAIL maintenance flows.
 *
 * <p>Preserved conventions:
 * <ul>
 *   <li>server-owned ids from explicit sequences (IMP-DEC-003) via native
 *       nextval — no auto-increment;</li>
 *   <li>accession number unique per unit (uq_equipment_unit_accession partial
 *       index); the service pre-validates case-insensitively so callers get a
 *       clean BEXA003 instead of a constraint violation;</li>
 *   <li>working_status holds an equipment_working_status_master id
 *       ({@link EquipmentWorkingStatus}, default WORKING).</li>
 * </ul>
 *
 * <p>Documented improvement: transfer is a guarded, atomic move — the asset
 * must be active and must not be CONDEMNED (terminal state), the transfer log
 * row and the department move commit together.
 */
@Service
public class EquipmentService {

    private static final Logger log = LoggerFactory.getLogger(EquipmentService.class);

    public static final long AUDIT_ACTOR_ID = 1L;   // MIG-004: principal id

    private final EquipmentRepository equipmentRepository;
    private final EquipmentTransferRepository transferRepository;
    private final ObjectMapper objectMapper;

    @PersistenceContext
    private EntityManager em;

    public EquipmentService(EquipmentRepository equipmentRepository,
                            EquipmentTransferRepository transferRepository,
                            ObjectMapper objectMapper) {
        this.equipmentRepository = equipmentRepository;
        this.transferRepository = transferRepository;
        this.objectMapper = objectMapper;
    }

    // ---------------------------------------------------------------- create

    /**
     * Register a new asset (legacy equipment save). Requires itemId;
     * workingStatus defaults to WORKING and must be a valid dictionary id;
     * accessionNo is validated unique per unit (case-insensitive).
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Equipment register(JsonNode payload, Long unitId) {
        if (payload == null || payload.isNull() || payload.isMissingNode()) {
            throw new InvalidEquipmentRequestException("BEXA003", "Invalid input data !");
        }
        Equipment equipment = bind(payload, Equipment.class);
        if (equipment.getItemId() == null) {
            throw new InvalidEquipmentRequestException("BEXA003", "itemId is required");
        }
        if (equipment.getWorkingStatus() == null) {
            equipment.setWorkingStatus(EquipmentWorkingStatus.WORKING);
        } else if (!EquipmentWorkingStatus.isValid(equipment.getWorkingStatus())) {
            throw new InvalidEquipmentRequestException("BEXA003",
                    "Invalid workingStatus: " + equipment.getWorkingStatus());
        }
        validateAccessionAvailable(equipment.getAccessionNo(), unitId, null);
        equipment.setId(nextId("equipment_seq"));
        equipment.setStatus(equipment.getStatus() == null ? true : equipment.getStatus());
        stampCreate(equipment, unitId);
        Equipment saved = equipmentRepository.save(equipment);
        log.info("Equipment registered id={} itemId={} accessionNo={}",
                saved.getId(), saved.getItemId(), saved.getAccessionNo());
        return saved;
    }

    // ---------------------------------------------------------------- update

    /**
     * Update asset details (legacy equipment edit). Null patch fields are
     * ignored; id/unit/audit columns are never taken from the payload. A
     * changed accessionNo is re-validated for per-unit uniqueness; a supplied
     * workingStatus must be a valid dictionary id.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Equipment update(Long id, JsonNode payload) {
        if (payload == null || payload.isNull() || payload.isMissingNode()) {
            throw new InvalidEquipmentRequestException("BEXA003", "Invalid input data !");
        }
        Equipment existing = getEquipment(id);
        Equipment patch = bind(payload, Equipment.class);
        String newAccession = patch.getAccessionNo();
        if (newAccession != null && !newAccession.equalsIgnoreCase(existing.getAccessionNo())) {
            validateAccessionAvailable(newAccession, existing.getUnitId(), id);
        }
        if (patch.getWorkingStatus() != null && !EquipmentWorkingStatus.isValid(patch.getWorkingStatus())) {
            throw new InvalidEquipmentRequestException("BEXA003",
                    "Invalid workingStatus: " + patch.getWorkingStatus());
        }
        applyUpdate(existing, patch);
        stampUpdate(existing);
        return equipmentRepository.save(existing);
    }

    // -------------------------------------------------------------- transfer

    /**
     * Transfer an asset to another department. Guarded: the asset must exist
     * and be active, must not already be CONDEMNED (terminal), and the target
     * department is required. Writes the equipment_transfer log row and moves
     * equipment.department_id in one transaction. Payload:
     * {@code {toDepartmentId (required), reason?, transferDate?, toLocation?, toStoreId?}}.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public EquipmentTransfer transfer(Long id, JsonNode payload) {
        if (payload == null || payload.isNull() || payload.isMissingNode()) {
            throw new InvalidEquipmentRequestException("BEXA003", "Invalid input data !");
        }
        Equipment equipment = getEquipment(id);
        if (!Boolean.TRUE.equals(equipment.getStatus())) {
            throw new InvalidEquipmentRequestException("BEXA003", "Cannot transfer an inactive asset");
        }
        if (equipment.getWorkingStatus() != null
                && equipment.getWorkingStatus() == EquipmentWorkingStatus.CONDEMNED) {
            throw new InvalidEquipmentRequestException("BEXA003", "Cannot transfer a condemned asset");
        }
        JsonNode toDeptNode = payload.path("toDepartmentId");
        if (!toDeptNode.isNumber()) {
            throw new InvalidEquipmentRequestException("BEXA003", "toDepartmentId is required");
        }
        long toDepartmentId = toDeptNode.asLong();

        EquipmentTransfer transfer = new EquipmentTransfer();
        transfer.setId(nextId("equipment_transfer_seq"));
        transfer.setEquipmentId(equipment.getId());
        transfer.setFromDepartmentId(equipment.getDepartmentId());
        transfer.setToDepartmentId(toDepartmentId);
        transfer.setTransferDate(payload.hasNonNull("transferDate")
                ? Timestamp.from(Instant.parse(payload.get("transferDate").asText()))
                : Timestamp.from(Instant.now()));
        transfer.setReason(payload.hasNonNull("reason") ? payload.get("reason").asText() : null);
        transfer.setStatus(true);
        stampCreate(transfer, equipment.getUnitId());
        EquipmentTransfer saved = transferRepository.save(transfer);

        equipment.setDepartmentId(toDepartmentId);
        if (payload.hasNonNull("toLocation")) {
            equipment.setLocation(payload.get("toLocation").asText());
        }
        if (payload.hasNonNull("toStoreId")) {
            equipment.setStoreId(payload.get("toStoreId").asLong());
        }
        stampUpdate(equipment);
        equipmentRepository.save(equipment);
        log.info("Equipment {} transferred {} -> {}", equipment.getId(),
                saved.getFromDepartmentId(), saved.getToDepartmentId());
        return saved;
    }

    // ----------------------------------------------------------------- reads

    @Transactional(readOnly = true)
    public Equipment getEquipment(Long id) {
        return equipmentRepository.findById(id)
                .orElseThrow(() -> new EquipmentNotFoundException("NOT_FOUND", "Equipment not found"));
    }

    /** Asset register listing/search (unit, working status, free text). */
    @Transactional(readOnly = true)
    public List<Equipment> search(Long unitId, Long workingStatus, String searchText) {
        if (workingStatus != null && !EquipmentWorkingStatus.isValid(workingStatus)) {
            throw new InvalidEquipmentRequestException("BEXA003",
                    "Invalid workingStatus: " + workingStatus);
        }
        String trimmed = (searchText == null || searchText.isBlank()) ? null : searchText.trim();
        // Sentinel values (0L / "") are bound instead of null: PostgreSQL cannot
        // type an untyped null parameter; the boolean gates ignore them when absent.
        return equipmentRepository.search(
                unitId == null ? 0L : unitId, unitId != null,
                workingStatus == null ? 0L : workingStatus, workingStatus != null,
                trimmed == null ? "" : trimmed, trimmed != null);
    }

    /** Accession lookup; unit scope optional (falls back to newest match). */
    @Transactional(readOnly = true)
    public Equipment findByAccession(String accessionNo, Long unitId) {
        if (accessionNo == null || accessionNo.isBlank()) {
            throw new InvalidEquipmentRequestException("BEXA003", "accessionNo is required");
        }
        return (unitId != null
                ? equipmentRepository.findByUnitIdAndAccessionNoIgnoreCase(unitId, accessionNo.trim())
                : equipmentRepository.findFirstByAccessionNoIgnoreCaseOrderByIdDesc(accessionNo.trim()))
                .orElseThrow(() -> new EquipmentNotFoundException("NOT_FOUND",
                        "Equipment not found for accession " + accessionNo));
    }

    @Transactional(readOnly = true)
    public List<Equipment> findByItem(Long itemId) {
        return equipmentRepository.findByItemIdAndStatusTrueOrderByIdDesc(itemId);
    }

    @Transactional(readOnly = true)
    public List<EquipmentTransfer> transferHistory(Long id) {
        getEquipment(id); // 404 for unknown asset
        return transferRepository.findByEquipmentIdOrderByIdAsc(id);
    }

    // --------------------------------------------------------------- helpers

    /** Accession numbers are unique per unit (uq_equipment_unit_accession). */
    private void validateAccessionAvailable(String accessionNo, Long unitId, Long selfId) {
        if (accessionNo == null || accessionNo.isBlank()) {
            return;
        }
        equipmentRepository.findByUnitIdAndAccessionNoIgnoreCase(unitId, accessionNo.trim())
                .ifPresentOrElse(existing -> {
                    if (!existing.getId().equals(selfId)) {
                        throw new InvalidEquipmentRequestException("BEXA003",
                                "Accession number already exists in this unit");
                    }
                }, () -> {
                    // No unit scope: fall back to a global check so an omitted
                    // unitId cannot smuggle a duplicate past validation.
                    if (unitId == null) {
                        equipmentRepository.findFirstByAccessionNoIgnoreCaseOrderByIdDesc(accessionNo.trim())
                                .filter(existing -> !existing.getId().equals(selfId))
                                .ifPresent(existing -> {
                                    throw new InvalidEquipmentRequestException("BEXA003",
                                            "Accession number already exists");
                                });
                    }
                });
    }

    private <T> T bind(JsonNode payload, Class<T> type) {
        try {
            return objectMapper.treeToValue(payload, type);
        } catch (Exception e) {
            throw new InvalidEquipmentRequestException("BEXA003", "Invalid input data !");
        }
    }

    /** Null-safe field copy from the patch onto the managed entity (waste-service
     *  pattern: walks into EquipmentBase so {@code status} is patchable, while the
     *  skip list keeps id/unit/audit columns server-owned). */
    private void applyUpdate(Equipment existing, Equipment patch) {
        for (Class<?> c = patch.getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
            for (Field field : c.getDeclaredFields()) {
                int mods = field.getModifiers();
                if (Modifier.isStatic(mods) || Modifier.isFinal(mods)
                        || field.getName().startsWith("_persistence_")
                        || field.getName().equals("id") || field.getName().equals("unitId")
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
