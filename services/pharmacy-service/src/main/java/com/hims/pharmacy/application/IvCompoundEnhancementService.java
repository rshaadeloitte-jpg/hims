package com.hims.pharmacy.application;

import com.hims.pharmacy.application.exceptions.BcmaVerificationException;
import com.hims.pharmacy.domain.model.IvCompound;
import com.hims.pharmacy.domain.model.IvCompoundLine;
import com.hims.pharmacy.domain.repository.IvCompoundLineRepository;
import com.hims.pharmacy.domain.repository.IvCompoundRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class IvCompoundEnhancementService {
    public static final long AUDIT_ACTOR_ID = 1L;
    private final IvCompoundRepository compoundRepo;
    private final IvCompoundLineRepository lineRepo;
    @PersistenceContext private EntityManager em;

    public IvCompoundEnhancementService(IvCompoundRepository compoundRepo, IvCompoundLineRepository lineRepo) {
        this.compoundRepo = compoundRepo; this.lineRepo = lineRepo;
    }

    @Transactional
    public IvCompound createCompoundWithBilling(Long patientId, Long encounterId, Long prescriptionId,
                                                 List<IvCompoundService.CompoundLineInput> lines,
                                                 BigDecimal totalVolumeMl, Timestamp beyondUseDate,
                                                 String remarks, BigDecimal unitCost, Long userId) {
        if (lines == null || lines.isEmpty()) throw new BcmaVerificationException("BEXA003", "Lines required");
        Timestamp now = Timestamp.from(Instant.now());
        IvCompound c = new IvCompound();
        c.setCompoundId(nextId("ihms_pharmacy.t_iv_compound_seq"));
        c.setPatientId(patientId); c.setEncounterId(encounterId); c.setPrescriptionId(prescriptionId);
        c.setOrderDate(now); c.setStatusCode(IvCompound.STATUS_PENDING);
        c.setTotalVolumeMl(totalVolumeMl); c.setBeyondUseDate(beyondUseDate); c.setRemarks(remarks);
        c.setUnitCost(unitCost); c.setAddedBy(userId != null ? userId : AUDIT_ACTOR_ID); c.setAddedDatetime(now);
        compoundRepo.save(c);
        long ln = 1;
        List<IvCompoundLine> saved = new ArrayList<>();
        for (var l : lines) {
            IvCompoundLine line = new IvCompoundLine();
            line.setLineId(nextId("ihms_pharmacy.t_iv_compound_line_seq"));
            line.setCompoundId(c.getCompoundId()); line.setItemId(l.itemId());
            line.setItemName(l.itemName()); line.setQuantity(l.quantity()); line.setUom(l.uom());
            line.setBatchId(l.batchId()); line.setExpiryDate(l.expiryDate()); line.setLineNo((int) ln++);
            line.setAddedBy(userId != null ? userId : AUDIT_ACTOR_ID); line.setAddedDatetime(now);
            lineRepo.save(line); saved.add(line);
        }
        c.setLines(saved); return c;
    }

    @Transactional
    public IvCompound finalizeForBilling(Long compoundId, BigDecimal billedAmount, Long userId) {
        IvCompound c = compoundRepo.findById(compoundId)
            .orElseThrow(() -> new BcmaVerificationException("NOT_FOUND", "Compound not found"));
        Timestamp now = Timestamp.from(Instant.now());
        c.setStatusCode(IvCompound.STATUS_ADMINISTERED); // bill after verify
        c.setBilledAmount(billedAmount); c.setUpdatedBy(userId); c.setUpdatedDatetime(now);
        return compoundRepo.save(c);
    }

    private Long nextId(String seq) {
        Number n = (Number) em.createNativeQuery("SELECT nextval('" + seq + "')").getSingleResult();
        return n.longValue();
    }
}
