package com.hims.item.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hims.item.application.exceptions.InvalidItemRequestException;
import com.hims.item.application.exceptions.ItemNotFoundException;
import com.hims.item.domain.model.Supplier;
import com.hims.item.domain.repository.SupplierRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

/**
 * Supplier master CRUD (WP-2 procurement slice). Maps onto the EXISTING
 * ihms_item.supplier table from V1 (ONE-TABLE-ONE-OWNER SS56 — no duplicate
 * t_supplier_master); server-owned ids from ihms_item.supplier_seq per the same
 * bind/validate/stamp conventions as ItemMasterService.
 */
@Service
public class SupplierService {

    public static final long AUDIT_ACTOR_ID = 1L;   // MIG-004: principal id

    private final SupplierRepository supplierRepository;
    private final ObjectMapper objectMapper;

    public SupplierService(SupplierRepository supplierRepository, ObjectMapper objectMapper) {
        this.supplierRepository = supplierRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<Supplier> list() {
        return supplierRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Supplier get(Long id) {
        return supplierRepository.findById(id)
                .orElseThrow(() -> new ItemNotFoundException("NOT_FOUND", "Record not found"));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Supplier create(JsonNode payload) {
        Supplier supplier = bind(payload);
        require(supplier, "code", "description");
        supplier.setId(supplierRepository.nextSupplierId());
        if (supplier.getStatus() == null) {
            supplier.setStatus(true);
        }
        stampCreate(supplier);
        return supplierRepository.save(supplier);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Supplier update(Long id, JsonNode payload) {
        Supplier existing = get(id);
        Supplier patch = bind(payload);
        applyUpdate(existing, patch);
        existing.setUpdatedBy(AUDIT_ACTOR_ID);
        existing.setUpdatedDatetime(Timestamp.from(Instant.now()));
        return supplierRepository.save(existing);
    }

    // ------------------------------------------------------------------ helpers (same conventions as ItemMasterService)

    private Supplier bind(JsonNode payload) {
        if (payload == null || payload.isNull() || payload.isMissingNode()) {
            throw new InvalidItemRequestException("BEXA003", "Invalid input data !");
        }
        try {
            return objectMapper.treeToValue(payload, Supplier.class);
        } catch (Exception e) {
            throw new InvalidItemRequestException("BEXA003", "Invalid input data !");
        }
    }

    private void require(Supplier entity, String... fields) {
        for (String f : fields) {
            Object v = fieldValue(entity, f);
            if (v == null || (v instanceof String s && s.isBlank())) {
                throw new InvalidItemRequestException("BEXA003", "Invalid input data !");
            }
        }
    }

    private Object fieldValue(Supplier entity, String field) {
        try {
            for (Class<?> c = entity.getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
                try {
                    Field f = c.getDeclaredField(field);
                    f.setAccessible(true);
                    return f.get(entity);
                } catch (NoSuchFieldException e) {
                    // continue up the hierarchy
                }
            }
        } catch (IllegalAccessException e) {
            throw new InvalidItemRequestException("BEXA003", "Invalid input data !");
        }
        return null;
    }

    private void stampCreate(Supplier entity) {
        entity.setAddedBy(AUDIT_ACTOR_ID);
        entity.setAddedDatetime(Timestamp.from(Instant.now()));
        entity.setUpdatedBy(null);
        entity.setSynchronizedd(false);
    }

    /** Server-owned field copy (identity + audit columns never client-writable). */
    private void applyUpdate(Supplier existing, Supplier patch) {
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
                    // skip non-copyable field
                }
            }
        }
    }

}
