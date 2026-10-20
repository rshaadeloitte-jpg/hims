package com.hims.pharmacy.application;

import com.hims.pharmacy.application.exceptions.InvalidSaleRequestException;
import com.hims.pharmacy.application.exceptions.SaleNotFoundException;
import com.hims.pharmacy.domain.model.WardPicklist;
import com.hims.pharmacy.domain.repository.ItemSaleRepository;
import com.hims.pharmacy.domain.repository.WardPicklistRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;

/**
 * Ward pick lists (register WP-R2): {wardId, saleId} -> picked_at=now.
 * Guards: wardId/saleId required (400 BEXA003); unknown sale -&gt; 404 NOT_FOUND.
 * Emits NOTHING to the outbox (no catalog event for picklists).
 */
@Service
public class WardPicklistService {

    private static final Logger log = LoggerFactory.getLogger(WardPicklistService.class);

    public static final long AUDIT_ACTOR_ID = 1L;   // MIG-004: principal id

    private final WardPicklistRepository repository;
    private final ItemSaleRepository saleRepository;

    @PersistenceContext
    private EntityManager em;

    public WardPicklistService(WardPicklistRepository repository,
                               ItemSaleRepository saleRepository) {
        this.repository = repository;
        this.saleRepository = saleRepository;
    }

    @Transactional
    public WardPicklist create(Long wardId, Long saleId) {
        if (wardId == null) {
            throw new InvalidSaleRequestException("BEXA003", "wardId is required");
        }
        if (saleId == null) {
            throw new InvalidSaleRequestException("BEXA003", "saleId is required");
        }
        if (!saleRepository.existsById(saleId)) {
            throw new SaleNotFoundException("NOT_FOUND", "Item sale not found: " + saleId);
        }
        WardPicklist picklist = new WardPicklist();
        picklist.setPicklistId(nextId());
        picklist.setWardId(wardId);
        picklist.setSaleId(saleId);
        picklist.setPickedAt(Timestamp.from(Instant.now()));
        picklist.setAddedBy(AUDIT_ACTOR_ID);
        picklist.setAddedDatetime(Timestamp.from(Instant.now()));
        WardPicklist saved = repository.save(picklist);
        log.info("Created ward picklist {} for ward {} on sale {}",
                saved.getPicklistId(), wardId, saleId);
        return saved;
    }

    private Long nextId() {
        Number n = (Number) em.createNativeQuery(
                "SELECT nextval('ihms_pharmacy.t_ward_picklist_seq')").getSingleResult();
        return n.longValue();
    }
}
