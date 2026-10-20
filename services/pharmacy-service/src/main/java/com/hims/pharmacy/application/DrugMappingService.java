package com.hims.pharmacy.application;

import com.hims.pharmacy.application.exceptions.BcmaVerificationException;
import com.hims.pharmacy.domain.model.DrugMapping;
import com.hims.pharmacy.domain.repository.DrugMappingRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class DrugMappingService {
    public static final long AUDIT_ACTOR_ID = 1L;
    private final DrugMappingRepository repo;
    @PersistenceContext private EntityManager em;

    public DrugMappingService(DrugMappingRepository repo) { this.repo = repo; }

    @Transactional
    public DrugMapping create(Long itemId, String itemName, String genericName, String brandName,
                              String category, String strength, String uom, BigDecimal mrp,
                              BigDecimal taxPercent, Long userId) {
        if (itemId == null) throw new BcmaVerificationException("BEXA003", "itemId required");
        Timestamp now = Timestamp.from(Instant.now());
        DrugMapping m = new DrugMapping();
        m.setMappingId(nextId("ihms_pharmacy.t_drug_mapping_seq"));
        m.setItemId(itemId); m.setItemName(itemName); m.setGenericName(genericName);
        m.setBrandName(brandName); m.setCategory(category); m.setStrength(strength);
        m.setUom(uom); m.setMrp(mrp); m.setTaxPercent(taxPercent);
        m.setIsActive(true); m.setAddedBy(userId != null ? userId : AUDIT_ACTOR_ID); m.setAddedDatetime(now);
        return repo.save(m);
    }

    @Transactional
    public DrugMapping update(Long mappingId, String itemName, String genericName, BigDecimal mrp, Boolean isActive, Long userId) {
        DrugMapping m = repo.findById(mappingId).orElseThrow(() -> new BcmaVerificationException("NOT_FOUND", "Mapping not found"));
        Timestamp now = Timestamp.from(Instant.now());
        if (itemName != null) m.setItemName(itemName);
        if (genericName != null) m.setGenericName(genericName);
        if (mrp != null) m.setMrp(mrp);
        if (isActive != null) m.setIsActive(isActive);
        m.setUpdatedBy(userId); m.setUpdatedDatetime(now);
        return repo.save(m);
    }

    @Transactional(readOnly = true)
    public Optional<DrugMapping> getByItem(Long itemId) { return repo.findByItemId(itemId); }

    @Transactional(readOnly = true)
    public List<DrugMapping> listActive() { return repo.findByIsActiveTrue(); }

    @Transactional(readOnly = true)
    public List<DrugMapping> searchGeneric(String q) { return repo.findByGenericNameContainingIgnoreCase(q); }

    private Long nextId(String seq) {
        Number n = (Number) em.createNativeQuery("SELECT nextval('" + seq + "')").getSingleResult();
        return n.longValue();
    }
}
