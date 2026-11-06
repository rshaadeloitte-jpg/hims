package com.hims.equipment.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hims.equipment.application.exceptions.EquipmentNotFoundException;
import com.hims.equipment.application.exceptions.InvalidEquipmentRequestException;
import com.hims.equipment.domain.model.EquipmentBase;
import com.hims.equipment.domain.model.EquipmentOpeningBalance;
import com.hims.equipment.domain.repository.EquipmentOpeningBalanceRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Opening-balance register (MIG-023 Phase 6, S18) — clean target of the legacy
 * T_EQUIPMENTOPENINGBALANCE stock take on the V1 equipment_opening_balance
 * table: record per-item store counts as of go-live (quantity + rate/amount),
 * read them back as a register and correct rows.
 *
 * <p>Preserved conventions:
 * <ul>
 *   <li>server-owned ids from explicit sequences (IMP-DEC-003) via native
 *       nextval — no auto-increment;</li>
 *   <li>{@code opening_balance} is NOT NULL DEFAULT 0 at the column level —
 *       the default is applied by this service when the payload omits the key
 *       (the entity deliberately has no initializer so a plain null check
 *       detects absence; Hibernate would otherwise send NULL into the NOT NULL
 *       column);</li>
 *   <li>gated-sentinel register query (EquipmentRepository.search convention),
 *       covered by ix_equipment_opening_balance_unit_item.</li>
 * </ul>
 *
 * <p>Documented improvements:
 * <ul>
 *   <li>{@code itemId} is required (NOT NULL column) and positive — clean
 *       BEXA003 instead of a raw constraint violation;</li>
 *   <li>negative quantities/values are rejected and money fields are
 *       normalized to the NUMERIC(19,2) column scale with an explicit range
 *       guard so oversized values fail as BEXA003 instead of an SQL error;</li>
 *   <li>bulk import rides the same POST endpoint: an array body imports many
 *       rows atomically (one REQUIRES_NEW transaction — one bad row rolls back
 *       the whole file) while an object body adds a single row;</li>
 *   <li>update is a null-safe patch that never re-points {@code itemId} or the
 *       audit/tenant block (same server-owned skip-list as warranty).</li>
 * </ul>
 */
@Service
public class OpeningBalanceService {

    private static final Logger log = LoggerFactory.getLogger(OpeningBalanceService.class);

    public static final long AUDIT_ACTOR_ID = 1L;   // MIG-004: principal id

    /** NUMERIC(19,2) column ceiling — friendlier guard than an SQL overflow error. */
    private static final BigDecimal MAX_MONEY = new BigDecimal("99999999999999999.99");

    private final EquipmentOpeningBalanceRepository balanceRepository;
    private final ObjectMapper objectMapper;

    @PersistenceContext
    private EntityManager em;

    public OpeningBalanceService(EquipmentOpeningBalanceRepository balanceRepository,
                                 ObjectMapper objectMapper) {
        this.balanceRepository = balanceRepository;
        this.objectMapper = objectMapper;
    }

    // ------------------------------------------------------------------- add

    /**
     * Add one opening-balance row. Server stamps id
     * (equipment_opening_balance_seq), defaults {@code openingBalance} to 0
     * when the key is absent and normalizes money to NUMERIC(19,2).
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public EquipmentOpeningBalance add(JsonNode payload, Long unitId) {
        EquipmentOpeningBalance saved = balanceRepository.save(createRow(payload, unitId));
        log.info("Opening balance {} added: item={} qty={} rate={} amount={} store={}",
                saved.getId(), saved.getItemId(), saved.getOpeningBalance(),
                saved.getRate(), saved.getAmount(), saved.getStoreId());
        return saved;
    }

    /**
     * Bulk import of an opening-balance file: every element is validated and
     * inserted inside ONE REQUIRES_NEW transaction, so a single bad row rolls
     * back the whole import (all-or-nothing). The node must be a non-empty
     * JSON array.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public List<EquipmentOpeningBalance> importBulk(JsonNode arrayPayload, Long unitId) {
        if (arrayPayload == null || !arrayPayload.isArray()) {
            throw new InvalidEquipmentRequestException("BEXA003",
                    "Bulk import requires a JSON array body");
        }
        if (arrayPayload.isEmpty()) {
            throw new InvalidEquipmentRequestException("BEXA003",
                    "Bulk import requires at least one row");
        }
        List<EquipmentOpeningBalance> imported = new ArrayList<>(arrayPayload.size());
        for (JsonNode row : arrayPayload) {
            imported.add(balanceRepository.save(createRow(row, unitId)));
        }
        log.info("Bulk import committed {} opening-balance rows for unit {}", imported.size(), unitId);
        return imported;
    }

    // ------------------------------------------------------------------ reads

    /**
     * Opening-balance register — active rows, optionally scoped by unit and/or
     * item, grouped per item (itemId ASC, then insertion order).
     */
    @Transactional(readOnly = true)
    public List<EquipmentOpeningBalance> list(Long unitId, Long itemId) {
        return balanceRepository.findRegisterRows(
                unitId, unitId != null,
                itemId, itemId != null);
    }

    // ----------------------------------------------------------------- update

    /**
     * Patch an opening-balance row. Null-safe field copy (warranty update
     * semantics): only present, non-null keys change anything; id, itemId
     * (the row's pairing key), tenant/audit columns and synchronizedd are
     * server-owned and never patchable.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public EquipmentOpeningBalance update(Long id, JsonNode payload) {
        if (payload == null || payload.isNull() || payload.isMissingNode()) {
            throw new InvalidEquipmentRequestException("BEXA003", "Invalid input data !");
        }
        EquipmentOpeningBalance existing = balanceRepository.findById(id)
                .orElseThrow(() -> new EquipmentNotFoundException("NOT_FOUND",
                        "Opening balance not found: " + id));
        EquipmentOpeningBalance patch = bind(payload);
        applyUpdate(existing, patch);
        stampUpdate(existing);
        EquipmentOpeningBalance saved = balanceRepository.save(existing);
        log.info("Opening balance {} updated (item={} qty={} rate={})",
                saved.getId(), saved.getItemId(), saved.getOpeningBalance(), saved.getRate());
        return saved;
    }

    // --------------------------------------------------------------- helpers

    /** Validate + bind one payload row (shared by add and bulk import). */
    private EquipmentOpeningBalance createRow(JsonNode payload, Long unitId) {
        if (payload == null || payload.isNull() || payload.isMissingNode()) {
            throw new InvalidEquipmentRequestException("BEXA003", "Invalid input data !");
        }
        EquipmentOpeningBalance balance = bind(payload);
        if (balance.getItemId() == null || balance.getItemId() <= 0) {
            throw new InvalidEquipmentRequestException("BEXA003",
                    "itemId is required and must be positive");
        }
        if (balance.getOpeningBalance() == null) {      // key absent -> column DEFAULT 0
            balance.setOpeningBalance(0L);
        }
        if (balance.getOpeningBalance() < 0) {
            throw new InvalidEquipmentRequestException("BEXA003",
                    "openingBalance must not be negative");
        }
        normalizeMoney(balance);
        balance.setAccessionNo(trimToNull(balance.getAccessionNo()));
        balance.setSerialNo(trimToNull(balance.getSerialNo()));
        balance.setModel(trimToNull(balance.getModel()));
        balance.setRemarks(trimToNull(balance.getRemarks()));
        balance.setId(nextId("equipment_opening_balance_seq"));
        balance.setStatus(balance.getStatus() == null ? true : balance.getStatus());
        stampCreate(balance, unitId);
        return balance;
    }

    /** NUMERIC(19,2) normalization: 2 dp HALF_UP, non-negative, range-guarded. */
    private void normalizeMoney(EquipmentOpeningBalance balance) {
        if (balance.getRate() != null) {
            balance.setRate(balance.getRate().setScale(2, RoundingMode.HALF_UP));
        }
        if (balance.getAmount() != null) {
            balance.setAmount(balance.getAmount().setScale(2, RoundingMode.HALF_UP));
        }
        if ((balance.getRate() != null && balance.getRate().signum() < 0)
                || (balance.getAmount() != null && balance.getAmount().signum() < 0)) {
            throw new InvalidEquipmentRequestException("BEXA003",
                    "rate/amount must not be negative");
        }
        if ((balance.getRate() != null && balance.getRate().compareTo(MAX_MONEY) > 0)
                || (balance.getAmount() != null && balance.getAmount().compareTo(MAX_MONEY) > 0)) {
            throw new InvalidEquipmentRequestException("BEXA003",
                    "rate/amount exceed the maximum supported value 99999999999999999.99");
        }
    }

    private void applyUpdate(EquipmentOpeningBalance existing, EquipmentOpeningBalance patch) {
        for (Class<?> c = patch.getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
            for (Field field : c.getDeclaredFields()) {
                int mods = field.getModifiers();
                if (Modifier.isStatic(mods) || Modifier.isFinal(mods)
                        || field.getName().startsWith("_persistence_")
                        || field.getName().equals("id") || field.getName().equals("unitId")
                        || field.getName().equals("itemId")
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
        normalizeMoney(existing);
        existing.setAccessionNo(trimToNull(existing.getAccessionNo()));
        existing.setSerialNo(trimToNull(existing.getSerialNo()));
        existing.setModel(trimToNull(existing.getModel()));
        existing.setRemarks(trimToNull(existing.getRemarks()));
    }

    private EquipmentOpeningBalance bind(JsonNode payload) {
        try {
            return objectMapper.treeToValue(payload, EquipmentOpeningBalance.class);
        } catch (Exception e) {
            throw new InvalidEquipmentRequestException("BEXA003", "Invalid input data !");
        }
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
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
