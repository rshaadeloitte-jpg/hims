package com.hims.equipment.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hims.equipment.api.WarrantyCounts;
import com.hims.equipment.application.exceptions.EquipmentNotFoundException;
import com.hims.equipment.application.exceptions.InvalidEquipmentRequestException;
import com.hims.equipment.domain.model.Equipment;
import com.hims.equipment.domain.model.EquipmentBase;
import com.hims.equipment.domain.model.EquipmentWorkingStatus;
import com.hims.equipment.domain.model.EquipmentWarranty;
import com.hims.equipment.domain.repository.EquipmentRepository;
import com.hims.equipment.domain.repository.EquipmentWarrantyRepository;
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
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Warranty / AMC cover management (MIG-023 Phase 4, S18) — clean target of the
 * legacy m_eqpwarranty flows on the V1 equipment_warranty table.
 *
 * <p>Preserved conventions:
 * <ul>
 *   <li>server-owned ids from explicit sequences (IMP-DEC-003) via native
 *       nextval — no auto-increment;</li>
 *   <li>{@code warrantyType} is the legacy free dictionary (WARRANTY / AMC /
 *       CMC ...); the NOT NULL DEFAULT 'WARRANTY' column default is applied by
 *       the service when the payload omits it — no entity initializer, so a
 *       plain null check detects absence;</li>
 *   <li>{@code equipmentId} is server-owned: taken from the path on add and
 *       never patchable on update.</li>
 * </ul>
 *
 * <p>Documented improvements: covers can only be added to an existing, active,
 * non-condemned asset (clean BEXA003/404 instead of raw constraint violations,
 * matching raise-complaint / create-schedule); expiry alerting is an inclusive
 * window starting today so a cover lapsing later today still alerts, and
 * covers with no end_date are correctly treated as never-expiring.
 */
@Service
public class WarrantyService {

    private static final Logger log = LoggerFactory.getLogger(WarrantyService.class);

    public static final long AUDIT_ACTOR_ID = 1L;   // MIG-004: principal id
    public static final String DEFAULT_WARRANTY_TYPE = "WARRANTY";
    public static final long DEFAULT_EXPIRING_DAYS = 30L;

    private final EquipmentRepository equipmentRepository;
    private final EquipmentWarrantyRepository warrantyRepository;
    private final ObjectMapper objectMapper;

    @PersistenceContext
    private EntityManager em;

    public WarrantyService(EquipmentRepository equipmentRepository,
                           EquipmentWarrantyRepository warrantyRepository,
                           ObjectMapper objectMapper) {
        this.equipmentRepository = equipmentRepository;
        this.warrantyRepository = warrantyRepository;
        this.objectMapper = objectMapper;
    }

    // ------------------------------------------------------------------- add

    /**
     * Add a warranty / AMC cover to an asset. Server stamps id
     * (equipment_warranty_seq) and pairs {@code equipmentId} from the path;
     * {@code warrantyType} defaults to WARRANTY. Asset existence/activity are
     * validated first so callers never see constraint violations.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public EquipmentWarranty add(Long equipmentId, JsonNode payload, Long unitId) {
        if (payload == null || payload.isNull() || payload.isMissingNode()) {
            throw new InvalidEquipmentRequestException("BEXA003", "Invalid input data !");
        }
        Equipment asset = requireAsset(equipmentId);
        EquipmentWarranty warranty = bind(payload, EquipmentWarranty.class);
        if (warranty.getWarrantyType() == null || warranty.getWarrantyType().isBlank()) {
            warranty.setWarrantyType(DEFAULT_WARRANTY_TYPE);
        } else {
            warranty.setWarrantyType(warranty.getWarrantyType().trim());
        }
        warranty.setId(nextId("equipment_warranty_seq"));
        warranty.setEquipmentId(asset.getId());          // server-owned pairing
        warranty.setStatus(warranty.getStatus() == null ? true : warranty.getStatus());
        stampCreate(warranty, unitId);
        EquipmentWarranty saved = warrantyRepository.save(warranty);
        log.info("Warranty {} ({}) added to equipment {} end={}",
                saved.getId(), saved.getWarrantyType(), saved.getEquipmentId(), saved.getEndDate());
        return saved;
    }

    // ----------------------------------------------------------------- reads

    /** Active covers of one asset — newest first. */
    @Transactional(readOnly = true)
    public List<EquipmentWarranty> listByEquipment(Long equipmentId) {
        requireAsset(equipmentId); // 404 for unknown asset
        return warrantyRepository.findByEquipmentIdAndStatusTrueOrderByIdDesc(equipmentId);
    }

    /**
     * Expiry alerts: active covers whose end_date falls within {@code days}
     * days of today (window [today 00:00, end of today+days]; a cover lapsing
     * later today still alerts). days defaults to
     * {@link #DEFAULT_EXPIRING_DAYS} and must be positive. Covers without an
     * end_date never expire and are excluded. Optional unit / asset scoping.
     */
    @Transactional(readOnly = true)
    public List<EquipmentWarranty> expiring(Long days, Long unitId, Long equipmentId) {
        long windowDays = days == null ? DEFAULT_EXPIRING_DAYS : days;
        if (windowDays < 1) {
            throw new InvalidEquipmentRequestException("BEXA003",
                    "days must be a positive number of days");
        }
        LocalDate today = LocalDate.now(ZoneId.systemDefault());
        Timestamp from = Timestamp.from(today.atStartOfDay(ZoneId.systemDefault()).toInstant());
        Timestamp to = Timestamp.from(today.plusDays(windowDays + 1)
                .atStartOfDay(ZoneId.systemDefault()).toInstant());
        return warrantyRepository.findExpiring(
                from, to,
                unitId == null ? 0L : unitId, unitId != null,
                equipmentId == null ? 0L : equipmentId, equipmentId != null);
    }

    /**
     * AMC / warranty count summary over active covers: total, already expired,
     * expiring in the next 30 days and the per-type breakdown. Optional unit
     * scoping keeps multi-unit deployments meaningful.
     */
    @Transactional(readOnly = true)
    public WarrantyCounts counts(Long unitId) {
        boolean hasUnit = unitId != null;
        Timestamp startOfToday = Timestamp.from(LocalDate.now(ZoneId.systemDefault())
                .atStartOfDay(ZoneId.systemDefault()).toInstant());
        Timestamp in31Days = Timestamp.from(LocalDate.now(ZoneId.systemDefault()).plusDays(31)
                .atStartOfDay(ZoneId.systemDefault()).toInstant());

        long totalActive = warrantyRepository.countActive(hasUnit ? unitId : 0L, hasUnit);

        Map<String, Long> byType = new LinkedHashMap<>();
        for (Object[] row : warrantyRepository.countActiveGroupedByType(
                hasUnit ? unitId : 0L, hasUnit)) {
            byType.put((String) row[0], ((Number) row[1]).longValue());
        }
        long counted = byType.values().stream().mapToLong(Long::longValue).sum();
        if (counted != totalActive) {   // NULL-type rows would break the invariant
            byType.put("(unspecified)",
                    byType.getOrDefault("(unspecified)", 0L) + (totalActive - counted));
        }
        return new WarrantyCounts(totalActive,
                warrantyRepository.countExpiredBefore(startOfToday, hasUnit ? unitId : 0L, hasUnit),
                warrantyRepository.countExpiringBetween(startOfToday, in31Days,
                        hasUnit ? unitId : 0L, hasUnit),
                byType.entrySet().stream()
                        .map(e -> new WarrantyCounts.TypeCount(e.getKey(), e.getValue()))
                        .toList());
    }

    // ---------------------------------------------------------------- update

    /**
     * Update a cover record (null patch fields ignored; id/equipmentId/unit/
     * audit columns server-owned). A supplied warrantyType replaces the old
     * one trimmed.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public EquipmentWarranty update(Long warrantyId, JsonNode payload) {
        if (payload == null || payload.isNull() || payload.isMissingNode()) {
            throw new InvalidEquipmentRequestException("BEXA003", "Invalid input data !");
        }
        EquipmentWarranty existing = warrantyRepository.findById(warrantyId)
                .orElseThrow(() -> new EquipmentNotFoundException("NOT_FOUND", "Warranty not found"));
        EquipmentWarranty patch = bind(payload, EquipmentWarranty.class);
        applyUpdate(existing, patch);
        stampUpdate(existing);
        EquipmentWarranty saved = warrantyRepository.save(existing);
        log.info("Warranty {} updated (type={} end={})",
                saved.getId(), saved.getWarrantyType(), saved.getEndDate());
        return saved;
    }

    // --------------------------------------------------------------- helpers

    private Equipment requireAsset(Long equipmentId) {
        Equipment asset = equipmentRepository.findById(equipmentId)
                .orElseThrow(() -> new EquipmentNotFoundException("NOT_FOUND",
                        "Equipment not found: " + equipmentId));
        if (!Boolean.TRUE.equals(asset.getStatus())) {
            throw new InvalidEquipmentRequestException("BEXA003",
                    "Cannot attach a warranty to an inactive asset");
        }
        if (asset.getWorkingStatus() != null
                && asset.getWorkingStatus() == EquipmentWorkingStatus.CONDEMNED) {
            throw new InvalidEquipmentRequestException("BEXA003",
                    "Cannot attach a warranty to a condemned asset");
        }
        return asset;
    }

    private <T> T bind(JsonNode payload, Class<T> type) {
        try {
            return objectMapper.treeToValue(payload, type);
        } catch (Exception e) {
            throw new InvalidEquipmentRequestException("BEXA003", "Invalid input data !");
        }
    }

    /** Null-safe field copy from the patch onto the managed entity (waste-service
     *  pattern). Skip list keeps id/equipmentId/unit/audit columns server-owned. */
    private void applyUpdate(EquipmentWarranty existing, EquipmentWarranty patch) {
        for (Class<?> c = patch.getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
            for (Field field : c.getDeclaredFields()) {
                int mods = field.getModifiers();
                if (Modifier.isStatic(mods) || Modifier.isFinal(mods)
                        || field.getName().startsWith("_persistence_")
                        || field.getName().equals("id") || field.getName().equals("unitId")
                        || field.getName().equals("equipmentId")
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
        if (existing.getWarrantyType() != null) {
            existing.setWarrantyType(existing.getWarrantyType().trim());
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
