package com.hims.item.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.hims.item.application.exceptions.InvalidItemRequestException;
import com.hims.item.application.exceptions.ItemNotFoundException;
import com.hims.item.domain.model.RateContract;
import com.hims.item.domain.repository.ItemRepository;
import com.hims.item.domain.repository.RateContractRepository;
import com.hims.item.domain.repository.SupplierRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * M14 rate contract (supplier × item × validity window).
 * Only one ACTIVE contract per (supplier, item) is allowed at a time.
 * Lookups can be performed at an arbitrary date; the contract whose window
 * contains that date is returned.
 */
@Service
public class RateContractService {
    private static final long ACTOR = 1L;
    private final RateContractRepository contractRepository;
    private final SupplierRepository supplierRepository;
    private final ItemRepository itemRepository;

    public RateContractService(RateContractRepository contractRepository,
                               SupplierRepository supplierRepository,
                               ItemRepository itemRepository) {
        this.contractRepository = contractRepository;
        this.supplierRepository = supplierRepository;
        this.itemRepository = itemRepository;
    }

    @Transactional
    public RateContract create(Long supplierId, Long itemId, BigDecimal contractPrice,
                               BigDecimal taxPercent, BigDecimal minOrderQty,
                               String validFrom, String validTo, String remarks) {
        if (supplierId == null || !supplierRepository.existsById(supplierId))
            throw new ItemNotFoundException("NOT_FOUND", "Supplier not found");
        if (itemId == null || !itemRepository.existsById(itemId))
            throw new ItemNotFoundException("NOT_FOUND", "Item not found");
        Date from = parseDate(validFrom);
        Date to = parseDate(validTo);
        if (from == null || to == null)
            throw new InvalidItemRequestException("BEXA003", "validFrom and validTo are required");
        if (to.before(from))
            throw new InvalidItemRequestException("BEXA003", "validTo must be on or after validFrom");
        if (contractPrice == null || contractPrice.signum() < 0)
            throw new InvalidItemRequestException("BEXA003", "contractPrice must be non-negative");
        // Active overlap check
        contractRepository.findBySupplierIdAndItemId(supplierId, itemId).forEach(existing -> {
            if (existing.getStatusFlag() == RateContract.STATUS_ACTIVE
                    && windowsOverlap(existing.getValidFrom(), existing.getValidTo(), from, to)) {
                throw new InvalidItemRequestException("BEXA003",
                        "Overlapping ACTIVE rate contract exists: " + existing.getContractNo());
            }
        });
        long id = contractRepository.nextId();
        RateContract rc = new RateContract();
        rc.setId(id);
        rc.setContractNo("RC-" + id);
        rc.setSupplierId(supplierId);
        rc.setItemId(itemId);
        rc.setContractPrice(contractPrice);
        rc.setTaxPercent(taxPercent == null ? BigDecimal.ZERO : taxPercent);
        rc.setMinOrderQty(minOrderQty);
        rc.setValidFrom(from);
        rc.setValidTo(to);
        rc.setStatusFlag(RateContract.STATUS_ACTIVE);
        rc.setRemarks(remarks);
        rc.setStatus(true);
        rc.setAddedBy(ACTOR);
        rc.setAddedDatetime(Timestamp.from(Instant.now()));
        rc.setSynchronizedd(false);
        return contractRepository.save(rc);
    }

    @Transactional
    public RateContract cancel(long id) {
        RateContract rc = get(id);
        rc.setStatusFlag(RateContract.STATUS_CANCELLED);
        stamp(rc);
        return contractRepository.save(rc);
    }

    @Transactional(readOnly = true)
    public RateContract get(long id) {
        return contractRepository.findById(id)
                .orElseThrow(() -> new ItemNotFoundException("NOT_FOUND", "Rate contract not found"));
    }

    @Transactional(readOnly = true)
    public List<RateContract> list(Long supplierId, Long itemId) {
        if (supplierId != null && itemId != null) return contractRepository.findBySupplierIdAndItemId(supplierId, itemId);
        return contractRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<RateContract> lookupActive(Long supplierId, Long itemId, String onDate) {
        if (supplierId == null || itemId == null) return Optional.empty();
        Date d = parseDate(onDate);
        if (d == null) d = new Date(System.currentTimeMillis());
        return contractRepository.findActiveOnDate(supplierId, itemId, RateContract.STATUS_ACTIVE, d);
    }

    private void stamp(RateContract rc) { rc.setUpdatedBy(ACTOR); rc.setUpdatedDatetime(Timestamp.from(Instant.now())); }
    private static Date parseDate(String s) {
        if (s == null || s.isBlank()) return null;
        return Date.valueOf(java.time.LocalDate.parse(s));
    }
    private static boolean windowsOverlap(Date aFrom, Date aTo, Date bFrom, Date bTo) {
        return !aTo.before(bFrom) && !bTo.before(aFrom);
    }
}
