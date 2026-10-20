package com.hims.pharmacy.application;

import com.hims.pharmacy.application.exceptions.SaleNotFoundException;
import com.hims.pharmacy.domain.model.PrescriptionDispense;
import com.hims.pharmacy.domain.model.PrescriptionDispenseDetail;
import com.hims.pharmacy.domain.repository.PrescriptionDispenseDetailRepository;
import com.hims.pharmacy.domain.repository.PrescriptionDispenseRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

/**
 * Pharmacy-LOCAL prescription dispense projection (GAP-REGISTER-5 WP-R2,
 * legacy T_PRESCRIPTIONDISPENSE / T_PRESCRIPTIONDISPENSEDETAILS semantics).
 *
 * Ordering-service remains the OWNER of prescription truth via the E-06 payload;
 * consuming PrescriptionCreated to seed this projection automatically is a
 * post-saga concern (comment-only per register). Until then the projection rows
 * are seeded from each sale that carries a prescriptionId, and BR-PHAR-002 runs
 * here:
 * <ul>
 *   <li>full-qty dispense of the linked sale line -&gt; detail IsClosed='1', PendingQty=0</li>
 *   <li>partial dispense -&gt; PendingQty -= qty</li>
 *   <li>master IsClosed='1' when ALL details are closed</li>
 * </ul>
 * The cross-service write-back of these flags into ihms_clinical is DEFERRED
 * post-saga (register: "comment + report") — never silently invented.
 */
@Service
public class PrescriptionDispenseService {

    private static final Logger log = LoggerFactory.getLogger(PrescriptionDispenseService.class);

    public static final long AUDIT_ACTOR_ID = 1L;   // MIG-004: principal id

    private final PrescriptionDispenseRepository masterRepository;
    private final PrescriptionDispenseDetailRepository detailRepository;

    @PersistenceContext
    private EntityManager em;

    public PrescriptionDispenseService(PrescriptionDispenseRepository masterRepository,
                                       PrescriptionDispenseDetailRepository detailRepository) {
        this.masterRepository = masterRepository;
        this.detailRepository = detailRepository;
    }

    /**
     * Ensure a projection master exists for {@code prescriptionId} and append one
     * detail row per supplied sale line (same line_no/itemId/qty as the sale).
     */
    @Transactional
    public void projectSaleLines(long prescriptionId,
                                 List<com.hims.pharmacy.domain.model.ItemSaleDetail> saleLines) {
        PrescriptionDispense master = masterRepository.findByPrescriptionId(prescriptionId)
                .orElseGet(() -> {
                    PrescriptionDispense created = new PrescriptionDispense();
                    created.setDispenseId(nextId("t_prescription_dispense_seq"));
                    created.setPrescriptionId(prescriptionId);
                    created.setClosed(false);
                    created.setAddedBy(AUDIT_ACTOR_ID);
                    created.setAddedDatetime(now());
                    return masterRepository.save(created);
                });

        List<PrescriptionDispenseDetail> existing =
                detailRepository.findByDispenseIdOrderByLineNoAsc(master.getDispenseId());
        long nextLineNo = existing.isEmpty() ? 1L
                : existing.get(existing.size() - 1).getLineNo() + 1;

        for (com.hims.pharmacy.domain.model.ItemSaleDetail saleLine : saleLines) {
            PrescriptionDispenseDetail d = new PrescriptionDispenseDetail();
            d.setDispenseId(master.getDispenseId());
            d.setLineNo(nextLineNo++);
            d.setItemId(saleLine.getItemId());
            d.setQty(saleLine.getQty());
            d.setPendingQty(saleLine.getQty());
            d.setClosed(false);
            d.setAddedBy(AUDIT_ACTOR_ID);
            d.setAddedDatetime(now());
            detailRepository.save(d);
        }
        log.info("Projected {} sale line(s) onto prescription {} dispensing master {}",
                saleLines.size(), prescriptionId, master.getDispenseId());
    }

    /**
     * BR-PHAR-002 outcome for one dispensed sale line: close on full-qty,
     * decrement pending otherwise, close the master when everything is closed.
     * {@code projectionLineNo} is the 1-based position of the projected line that
     * mirrors the sale line (seeded in the same order at creation time).
     */
    @Transactional
    public void applyDispenseOutcome(long prescriptionId, long projectionLineNo,
                                     boolean saleLineFullyDispensed, int dispensedQty) {
        PrescriptionDispense master = masterRepository.findByPrescriptionId(prescriptionId)
                .orElseThrow(() -> new SaleNotFoundException("NOT_FOUND",
                        "Prescription dispensing projection not found: " + prescriptionId));
        List<PrescriptionDispenseDetail> details =
                detailRepository.findByDispenseIdOrderByLineNoAsc(master.getDispenseId());

        PrescriptionDispenseDetail target = details.stream()
                .filter(d -> d.getLineNo().equals(projectionLineNo))
                .findFirst()
                .orElseThrow(() -> new SaleNotFoundException("NOT_FOUND",
                        "Prescription dispensing detail not found: prescription "
                                + prescriptionId + " line " + projectionLineNo));

        if (saleLineFullyDispensed) {
            target.setClosed(true);
            target.setPendingQty(0);                       // IsClosed='1' PendingQty=0
        } else {
            target.setPendingQty(target.getPendingQty() - dispensedQty);   // PendingQty -= qty
        }
        target.setUpdatedBy(AUDIT_ACTOR_ID);
        target.setUpdatedDatetime(now());
        detailRepository.save(target);

        boolean allClosed = details.stream().allMatch(PrescriptionDispenseDetail::getClosed);
        if (allClosed && !Boolean.TRUE.equals(master.getClosed())) {
            master.setClosed(true);                        // master IsClosed='1'
            master.setUpdatedBy(AUDIT_ACTOR_ID);
            master.setUpdatedDatetime(now());
            masterRepository.save(master);
            log.info("Prescription {} dispensing projection fully closed", prescriptionId);
        }
    }

    /** PUT /api/v1/prescriptions/{id}/emr-no — stamp the EMR number on the projection master. */
    @Transactional
    public PrescriptionDispense setEmrNo(long prescriptionId, String emrNo) {
        PrescriptionDispense master = masterRepository.findByPrescriptionId(prescriptionId)
                .orElseThrow(() -> new SaleNotFoundException("NOT_FOUND",
                        "No pharmacy dispensing record exists for prescription " + prescriptionId));
        master.setEmrNo(emrNo);
        master.setUpdatedBy(AUDIT_ACTOR_ID);
        master.setUpdatedDatetime(now());
        return masterRepository.save(master);
    }

    // ------------------------------------------------------------------ helpers

    private Long nextId(String sequenceName) {
        Number n = (Number) em.createNativeQuery("SELECT nextval('ihms_pharmacy." + sequenceName + "')")
                .getSingleResult();
        return n.longValue();
    }

    private static Timestamp now() {
        return Timestamp.from(Instant.now());
    }
}
