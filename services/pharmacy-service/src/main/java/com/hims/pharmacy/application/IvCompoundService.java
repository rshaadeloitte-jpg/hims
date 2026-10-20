package com.hims.pharmacy.application;

import com.hims.pharmacy.application.exceptions.BcmaVerificationException;
import com.hims.pharmacy.application.exceptions.CompoundNotFoundException;
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

/**
 * IV compounding service. Status: 1=PENDING -> 2=COMPOUNDED -> 3=VERIFIED -> 4=ADMINISTERED (terminal)
 *                                                         -> 5=CANCELLED (terminal)
 * Emits NOTHING to the outbox.
 */
@Service
public class IvCompoundService {

    public static final long AUDIT_ACTOR_ID = 1L;

    private final IvCompoundRepository compoundRepository;
    private final IvCompoundLineRepository lineRepository;

    @PersistenceContext
    private EntityManager em;

    public IvCompoundService(IvCompoundRepository compoundRepository,
                              IvCompoundLineRepository lineRepository) {
        this.compoundRepository = compoundRepository;
        this.lineRepository = lineRepository;
    }

    /**
     * Create an IV compound with >= 1 line. Each line needs itemId, itemName, quantity, uom.
     */
    @Transactional
    public IvCompound create(Long patientId, Long encounterId, Long prescriptionId,
                             List<CompoundLineInput> lines, BigDecimal totalVolumeMl,
                             Timestamp beyondUseDate, String remarks, Long userId) {
        if (patientId == null) {
            throw new BcmaVerificationException("BEXA003", "patientId is required");
        }
        if (lines == null || lines.isEmpty()) {
            throw new BcmaVerificationException("BEXA003", "At least one compound line is required");
        }
        Timestamp now = Timestamp.from(Instant.now());

        IvCompound compound = new IvCompound();
        compound.setCompoundId(nextId("ihms_pharmacy.t_iv_compound_seq"));
        compound.setPatientId(patientId);
        compound.setEncounterId(encounterId);
        compound.setPrescriptionId(prescriptionId);
        compound.setOrderDate(now);
        compound.setStatusCode(IvCompound.STATUS_PENDING);
        compound.setTotalVolumeMl(totalVolumeMl);
        compound.setBeyondUseDate(beyondUseDate);
        compound.setRemarks(remarks);
        compound.setAddedBy(userId != null ? userId : AUDIT_ACTOR_ID);
        compound.setAddedDatetime(now);
        compoundRepository.save(compound);

        long lineNo = 1;
        List<IvCompoundLine> savedLines = new ArrayList<>();
        for (CompoundLineInput l : lines) {
            if (l.itemId() == null || l.itemName() == null || l.quantity() == null || l.uom() == null) {
                throw new BcmaVerificationException("BEXA003",
                        "Each compound line requires itemId, itemName, quantity, uom");
            }
            IvCompoundLine line = new IvCompoundLine();
            line.setLineId(nextId("ihms_pharmacy.t_iv_compound_line_seq"));
            line.setCompoundId(compound.getCompoundId());
            line.setItemId(l.itemId());
            line.setItemName(l.itemName());
            line.setQuantity(l.quantity());
            line.setUom(l.uom());
            line.setBatchId(l.batchId());
            line.setExpiryDate(l.expiryDate());
            line.setLineNo((int) lineNo++);
            line.setAddedBy(userId != null ? userId : AUDIT_ACTOR_ID);
            line.setAddedDatetime(now);
            lineRepository.save(line);
            savedLines.add(line);
        }
        compound.setLines(savedLines);
        return compound;
    }

    /** Mark as COMPOUNDED (preparation complete) */
    @Transactional
    public IvCompound compound(Long compoundId, Long userId) {
        IvCompound c = load(compoundId);
        if (c.getStatusCode() != IvCompound.STATUS_PENDING) {
            throw new BcmaVerificationException("PH_ILLEGAL_TRANSITION",
                    "Cannot compound from status " + c.getStatusCode());
        }
        Timestamp now = Timestamp.from(Instant.now());
        c.setStatusCode(IvCompound.STATUS_COMPOUNDED);
        c.setUpdatedBy(userId);
        c.setUpdatedDatetime(now);
        return compoundRepository.save(c);
    }

    /** Verify the compounded preparation (pharmacist sign-off) */
    @Transactional
    public IvCompound verify(Long compoundId, Long verifiedBy) {
        IvCompound c = load(compoundId);
        if (c.getStatusCode() != IvCompound.STATUS_COMPOUNDED) {
            throw new BcmaVerificationException("PH_ILLEGAL_TRANSITION",
                    "Cannot verify from status " + c.getStatusCode());
        }
        Timestamp now = Timestamp.from(Instant.now());
        c.setStatusCode(IvCompound.STATUS_VERIFIED);
        c.setVerifiedBy(verifiedBy);
        c.setVerifiedAt(now);
        c.setUpdatedBy(verifiedBy);
        c.setUpdatedDatetime(now);
        return compoundRepository.save(c);
    }

    /** Mark as ADMINISTERED (nurse administers to patient) */
    @Transactional
    public IvCompound administer(Long compoundId, Long administeredBy) {
        IvCompound c = load(compoundId);
        if (c.getStatusCode() != IvCompound.STATUS_VERIFIED) {
            throw new BcmaVerificationException("PH_ILLEGAL_TRANSITION",
                    "Cannot administer from status " + c.getStatusCode());
        }
        Timestamp now = Timestamp.from(Instant.now());
        c.setStatusCode(IvCompound.STATUS_ADMINISTERED);
        c.setAdministeredBy(administeredBy);
        c.setAdministeredAt(now);
        c.setUpdatedBy(administeredBy);
        c.setUpdatedDatetime(now);
        return compoundRepository.save(c);
    }

    /** Cancel (only possible from PENDING or COMPOUNDED) */
    @Transactional
    public IvCompound cancel(Long compoundId, Long userId) {
        IvCompound c = load(compoundId);
        if (c.getStatusCode() == IvCompound.STATUS_ADMINISTERED ||
            c.getStatusCode() == IvCompound.STATUS_CANCELLED) {
            throw new BcmaVerificationException("PH_ILLEGAL_TRANSITION",
                    "Cannot cancel from status " + c.getStatusCode());
        }
        Timestamp now = Timestamp.from(Instant.now());
        c.setStatusCode(IvCompound.STATUS_CANCELLED);
        c.setUpdatedBy(userId);
        c.setUpdatedDatetime(now);
        return compoundRepository.save(c);
    }

    @Transactional(readOnly = true)
    public IvCompound get(Long compoundId) {
        IvCompound c = load(compoundId);
        c.setLines(lineRepository.findByCompoundIdOrderByLineNo(compoundId));
        return c;
    }

    @Transactional(readOnly = true)
    public List<IvCompound> listForPatient(Long patientId) {
        List<IvCompound> compounds = compoundRepository.findByPatientId(patientId);
        for (IvCompound c : compounds) {
            c.setLines(lineRepository.findByCompoundIdOrderByLineNo(c.getCompoundId()));
        }
        return compounds;
    }

    @Transactional(readOnly = true)
    public List<IvCompound> listByStatus(Short status) {
        return compoundRepository.findByStatus(status);
    }

    private IvCompound load(Long compoundId) {
        return compoundRepository.findById(compoundId)
                .orElseThrow(() -> new CompoundNotFoundException("NOT_FOUND",
                        "IV compound not found: " + compoundId));
    }

    private Long nextId(String sequenceName) {
        Number n = (Number) em.createNativeQuery("SELECT nextval('" + sequenceName + "')").getSingleResult();
        return n.longValue();
    }

    public record CompoundLineInput(Long itemId, String itemName, BigDecimal quantity,
                                     String uom, String batchId, java.sql.Date expiryDate) {}
}
