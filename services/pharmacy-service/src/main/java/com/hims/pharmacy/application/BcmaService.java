package com.hims.pharmacy.application;

import com.hims.pharmacy.application.exceptions.BcmaVerificationException;
import com.hims.pharmacy.application.exceptions.SaleNotFoundException;
import com.hims.pharmacy.domain.model.BcmaVerification;
import com.hims.pharmacy.domain.model.ItemSale;
import com.hims.pharmacy.domain.model.ItemSaleDetail;
import com.hims.pharmacy.domain.repository.BcmaVerificationRepository;
import com.hims.pharmacy.domain.repository.ItemSaleDetailRepository;
import com.hims.pharmacy.domain.repository.ItemSaleRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

/**
 * BCMA five-rights verification service (BR-PHAR-BCMA).
 * Records scan results for right-patient/drug/dose/route/time,
 * supports override audit trail, and administration marking.
 * Emits NOTHING to the outbox.
 */
@Service
public class BcmaService {

    public static final long AUDIT_ACTOR_ID = 1L;

    private final BcmaVerificationRepository bcmaRepository;
    private final ItemSaleRepository saleRepository;
    private final ItemSaleDetailRepository detailRepository;

    @PersistenceContext
    private EntityManager em;

    public BcmaService(BcmaVerificationRepository bcmaRepository,
                       ItemSaleRepository saleRepository,
                       ItemSaleDetailRepository detailRepository) {
        this.bcmaRepository = bcmaRepository;
        this.saleRepository = saleRepository;
        this.detailRepository = detailRepository;
    }

    /**
     * Record a five-rights scan. If any right fails, the administration
     * must be explicitly overridden with a reason before the patient receives it.
     */
    @Transactional
    public BcmaVerification recordScan(Long patientId, Long saleId, Long detailId,
                                        Long encounterId,
                                        boolean rightPatient, boolean rightDrug,
                                        boolean rightDose, boolean rightRoute,
                                        boolean rightTime, Long userId) {
        if (patientId == null) {
            throw new BcmaVerificationException("BEXA003", "patientId is required");
        }
        if (saleId == null) {
            throw new BcmaVerificationException("BEXA003", "saleId is required");
        }
        if (detailId == null) {
            throw new BcmaVerificationException("BEXA003", "detailId is required");
        }

        // Verify sale and detail exist
        ItemSale sale = saleRepository.findById(saleId)
                .orElseThrow(() -> new SaleNotFoundException("NOT_FOUND", "Sale not found: " + saleId));
        if (!sale.getPatientId().equals(patientId)) {
            throw new BcmaVerificationException("BCMA_PATIENT_MISMATCH",
                    "Sale " + saleId + " belongs to patient " + sale.getPatientId() + ", not " + patientId);
        }
        ItemSaleDetail detail = detailRepository.findBySaleIdOrderByLineNoAsc(saleId).stream()
                .filter(d -> d.getLineNo().equals(detailId))
                .findFirst()
                .orElseThrow(() -> new BcmaVerificationException("BEXA003",
                        "Detail line " + detailId + " not found for sale " + saleId));

        Timestamp now = Timestamp.from(Instant.now());
        BcmaVerification bcma = new BcmaVerification();
        bcma.setBcmaId(nextId("ihms_pharmacy.t_bcma_verification_seq"));
        bcma.setPatientId(patientId);
        bcma.setSaleId(saleId);
        bcma.setDetailId(detail.getItemId());
        bcma.setEncounterId(encounterId);
        bcma.setScanTime(now);
        bcma.setRightPatient(rightPatient);
        bcma.setRightDrug(rightDrug);
        bcma.setRightDose(rightDose);
        bcma.setRightRoute(rightRoute);
        bcma.setRightTime(rightTime);
        bcma.setAllRightsPassed(
                rightPatient && rightDrug && rightDose && rightRoute && rightTime);
        bcma.setOverridden(false);
        bcma.setAdministered(false);
        bcma.setAddedBy(userId != null ? userId : AUDIT_ACTOR_ID);
        bcma.setAddedDatetime(now);
        return bcmaRepository.save(bcma);
    }

    /**
     * Override a failed-rights scan. Requires a non-empty reason.
     */
    @Transactional
    public BcmaVerification override(Long bcmaId, String reason, Long overrideBy) {
        if (reason == null || reason.trim().isEmpty()) {
            throw new BcmaVerificationException("BEXA003", "Override reason is required");
        }
        BcmaVerification bcma = bcmaRepository.findById(bcmaId)
                .orElseThrow(() -> new BcmaVerificationException("NOT_FOUND",
                        "BCMA verification not found: " + bcmaId));
        if (bcma.getAdministered()) {
            throw new BcmaVerificationException("BCMA_ALREADY_ADMINISTERED",
                    "Cannot override already-administered record " + bcmaId);
        }
        Timestamp now = Timestamp.from(Instant.now());
        bcma.setOverridden(true);
        bcma.setOverrideReason(reason.trim());
        bcma.setOverrideBy(overrideBy != null ? overrideBy : AUDIT_ACTOR_ID);
        bcma.setUpdatedBy(overrideBy);
        bcma.setUpdatedDatetime(now);
        return bcmaRepository.save(bcma);
    }

    /**
     * Mark administration complete. Allowed when all rights passed OR overridden.
     */
    @Transactional
    public BcmaVerification markAdministered(Long bcmaId, Long administeredBy) {
        BcmaVerification bcma = bcmaRepository.findById(bcmaId)
                .orElseThrow(() -> new BcmaVerificationException("NOT_FOUND",
                        "BCMA verification not found: " + bcmaId));
        if (bcma.getAdministered()) {
            throw new BcmaVerificationException("BCMA_ALREADY_ADMINISTERED",
                    "Already administered: " + bcmaId);
        }
        if (!bcma.getAllRightsPassed() && !bcma.getOverridden()) {
            throw new BcmaVerificationException("BCMA_RIGHTS_NOT_MET",
                    "Cannot administer: rights not met and not overridden");
        }
        Timestamp now = Timestamp.from(Instant.now());
        bcma.setAdministered(true);
        bcma.setAdministeredAt(now);
        bcma.setAdministeredBy(administeredBy != null ? administeredBy : AUDIT_ACTOR_ID);
        bcma.setUpdatedBy(administeredBy);
        bcma.setUpdatedDatetime(now);
        return bcmaRepository.save(bcma);
    }

    @Transactional(readOnly = true)
    public BcmaVerification get(Long bcmaId) {
        return bcmaRepository.findById(bcmaId)
                .orElseThrow(() -> new BcmaVerificationException("NOT_FOUND",
                        "BCMA verification not found: " + bcmaId));
    }

    @Transactional(readOnly = true)
    public List<BcmaVerification> listForPatient(Long patientId) {
        return bcmaRepository.findByPatientId(patientId);
    }

    @Transactional(readOnly = true)
    public List<BcmaVerification> listForSale(Long saleId) {
        return bcmaRepository.findBySaleId(saleId);
    }

    @Transactional(readOnly = true)
    public List<BcmaVerification> listPendingOverrides() {
        return bcmaRepository.findPendingOverrides();
    }

    private Long nextId(String sequenceName) {
        Number n = (Number) em.createNativeQuery("SELECT nextval('" + sequenceName + "')").getSingleResult();
        return n.longValue();
    }
}
