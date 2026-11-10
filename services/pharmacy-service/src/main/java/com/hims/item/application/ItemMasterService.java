package com.hims.item.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hims.item.application.exceptions.InvalidItemRequestException;
import com.hims.item.application.exceptions.ItemNotFoundException;
import com.hims.item.domain.model.Item;
import com.hims.item.domain.model.ItemBatch;
import com.hims.item.domain.model.ItemBase;
import com.hims.item.domain.model.Store;
import com.hims.item.domain.repository.ItemBatchRepository;
import com.hims.item.domain.repository.ItemRepository;
import com.hims.item.domain.repository.StoreRepository;
import com.hims.item.infrastructure.outbox.OutboxService;
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
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Item/Store/Batch master write path (MIG-012, first slice). Same contract as the
 * reference-service masters write path: server-owned ids (per-table sequence) and
 * audit fields, required-field validation, transactional {@code MasterDataChanged}
 * outbox events. Lookup masters (item_group etc.) follow in the next slice.
 */
@Service
public class ItemMasterService {

    private static final Logger log = LoggerFactory.getLogger(ItemMasterService.class);

    public static final long AUDIT_ACTOR_ID = 1L;   // MIG-004: principal id
    public static final String EVENT_TYPE_MASTER_CHANGED = "MasterDataChanged";

    private final ItemRepository itemRepository;
    private final StoreRepository storeRepository;
    private final ItemBatchRepository batchRepository;
    private final OutboxService outboxService;
    private final ObjectMapper objectMapper;

    @PersistenceContext
    private EntityManager em;

    public ItemMasterService(ItemRepository itemRepository,
                             StoreRepository storeRepository,
                             ItemBatchRepository batchRepository,
                             OutboxService outboxService,
                             ObjectMapper objectMapper) {
        this.itemRepository = itemRepository;
        this.storeRepository = storeRepository;
        this.batchRepository = batchRepository;
        this.outboxService = outboxService;
        this.objectMapper = objectMapper;
    }

    // ------------------------------------------------------------------ Item

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Item createItem(JsonNode payload, Long unitId) {
        Item item = bind(payload, Item.class);
        require(item, "itemName");
        item.setId(nextId("item_seq"));
        item.setStatus(item.getStatus() == null ? true : item.getStatus());
        stampCreate(item, unitId);
        Item saved = itemRepository.save(item);
        emitMaster("Item", saved.getId(), "create");
        return saved;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Item updateItem(Long id, JsonNode payload) {
        Item existing = itemRepository.findById(id)
                .orElseThrow(() -> new ItemNotFoundException("BEXA031", "Not Authorized for editing !"));
        Item patch = bind(payload, Item.class);
        applyUpdate(existing, patch);
        existing.setUpdatedBy(AUDIT_ACTOR_ID);
        existing.setUpdatedDatetime(Timestamp.from(Instant.now()));
        Item saved = itemRepository.save(existing);
        emitMaster("Item", saved.getId(), "update");
        return saved;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Item removeItem(Long id) {
        Item existing = itemRepository.findById(id)
                .orElseThrow(() -> new ItemNotFoundException("NOT_FOUND", "Record not found"));
        itemRepository.delete(existing);
        emitMaster("Item", id, "remove");
        return existing;
    }

    // ------------------------------------------------------------------ Store

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Store createStore(JsonNode payload, Long unitId) {
        Store store = bind(payload, Store.class);
        require(store, "code", "description");
        store.setId(nextId("store_seq"));
        store.setStatus(store.getStatus() == null ? true : store.getStatus());
        stampCreate(store, unitId);
        Store saved = storeRepository.save(store);
        emitMaster("Store", saved.getId(), "create");
        return saved;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Store updateStore(Long id, JsonNode payload) {
        Store existing = storeRepository.findById(id)
                .orElseThrow(() -> new ItemNotFoundException("BEXA031", "Not Authorized for editing !"));
        Store patch = bind(payload, Store.class);
        applyUpdate(existing, patch);
        existing.setUpdatedBy(AUDIT_ACTOR_ID);
        existing.setUpdatedDatetime(Timestamp.from(Instant.now()));
        Store saved = storeRepository.save(existing);
        emitMaster("Store", saved.getId(), "update");
        return saved;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Store removeStore(Long id) {
        Store existing = storeRepository.findById(id)
                .orElseThrow(() -> new ItemNotFoundException("NOT_FOUND", "Record not found"));
        storeRepository.delete(existing);
        emitMaster("Store", id, "remove");
        return existing;
    }

    // ------------------------------------------------------------------ Batch

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ItemBatch createBatch(JsonNode payload, Long unitId) {
        ItemBatch batch = bind(payload, ItemBatch.class);
        require(batch, "itemId", "batchCode");
        batch.setId(nextId("item_batch_seq"));
        batch.setStatus(batch.getStatus() == null ? true : batch.getStatus());
        stampCreate(batch, unitId);
        ItemBatch saved = batchRepository.save(batch);
        emitMaster("ItemBatch", saved.getId(), "create");
        return saved;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ItemBatch updateBatch(Long id, JsonNode payload) {
        ItemBatch existing = batchRepository.findById(id)
                .orElseThrow(() -> new ItemNotFoundException("BEXA031", "Not Authorized for editing !"));
        ItemBatch patch = bind(payload, ItemBatch.class);
        applyUpdate(existing, patch);
        existing.setUpdatedBy(AUDIT_ACTOR_ID);
        existing.setUpdatedDatetime(Timestamp.from(Instant.now()));
        ItemBatch saved = batchRepository.save(existing);
        emitMaster("ItemBatch", saved.getId(), "update");
        return saved;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ItemBatch removeBatch(Long id) {
        ItemBatch existing = batchRepository.findById(id)
                .orElseThrow(() -> new ItemNotFoundException("NOT_FOUND", "Record not found"));
        batchRepository.delete(existing);
        emitMaster("ItemBatch", id, "remove");
        return existing;
    }

    // ------------------------------------------------------------------ helpers

    private <T extends ItemBase> T bind(JsonNode payload, Class<T> type) {
        if (payload == null || payload.isNull() || payload.isMissingNode()) {
            throw new InvalidItemRequestException("BEXA003", "Invalid input data !");
        }
        try {
            return objectMapper.treeToValue(payload, type);
        } catch (Exception e) {
            throw new InvalidItemRequestException("BEXA003", "Invalid input data !");
        }
    }

    private void require(ItemBase entity, String... fields) {
        for (String f : fields) {
            Object v = fieldValue(entity, f);
            if (v == null || (v instanceof String s && s.isBlank())) {
                throw new InvalidItemRequestException("BEXA003", "Invalid input data !");
            }
        }
    }

    private Object fieldValue(ItemBase entity, String field) {
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
            log.warn("Cannot read field {} for validation", field);
        }
        return null;
    }

    private void stampCreate(ItemBase entity, Long unitId) {
        entity.setAddedBy(AUDIT_ACTOR_ID);
        entity.setAddedDatetime(Timestamp.from(Instant.now()));
        entity.setUpdatedBy(null);
        entity.setUpdatedDatetime(null);
        entity.setSynchronizedd(false);
        if (unitId != null) {
            entity.setUnitId(unitId);
        }
    }

    /** Server-owned field copy (identity + audit columns never client-writable). */
    private void applyUpdate(ItemBase existing, ItemBase patch) {
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

    private long nextId(String sequenceName) {
        return ((Number) em.createNativeQuery("SELECT nextval('ihms_item.\"" + sequenceName + "\"')")
                .getSingleResult()).longValue();
    }

    private void emitMaster(String aggregateType, long id, String op) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("masterType", aggregateType.toLowerCase());
        payload.put("masterId", id);
        payload.put("op", op);
        try {
            outboxService.append(EVENT_TYPE_MASTER_CHANGED, aggregateType, id,
                    String.valueOf(id), objectMapper.writeValueAsString(payload),
                    aggregateType.toLowerCase() + ":" + id + ":" + op + ":" + java.util.UUID.randomUUID());
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize outbox payload", e);
        }
    }
}
