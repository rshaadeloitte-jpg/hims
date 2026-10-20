package com.hims.payments.application;

import com.hims.payments.application.exceptions.InvalidPaymentRequestException;
import com.hims.payments.application.exceptions.PaymentEntityNotFoundException;
import com.hims.payments.domain.model.Advance;
import com.hims.payments.domain.repository.AdvanceRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

/**
 * Advances (Catalog #6 / GAP-REGISTER WP-13): t_advance with a GENERATED
 * balance column (amount - used STORED). Creation validates patientId and
 * amount > 0; listing is FIFO-ordered by advance_id for BR-BILL-007.
 */
@Service
public class AdvanceService {

    public static final long AUDIT_ACTOR_ID = 1L;   // MIG-004: principal id

    private final AdvanceRepository advanceRepository;

    @PersistenceContext
    private EntityManager em;

    public AdvanceService(AdvanceRepository advanceRepository) {
        this.advanceRepository = advanceRepository;
    }

    @Transactional
    public Advance create(Long patientId, BigDecimal amount) {
        if (patientId == null) {
            throw new InvalidPaymentRequestException("PAY_REQ_INVALID", "patientId is required");
        }
        if (amount == null || amount.signum() <= 0) {
            throw new InvalidPaymentRequestException("PAY_REQ_INVALID", "amount must be > 0");
        }
        Advance advance = new Advance();
        advance.setAdvanceId(nextId("t_advance_seq"));
        advance.setPatientId(patientId);
        advance.setAmount(amount);
        advance.setUsed(BigDecimal.ZERO);
        advance.setStatus(true);
        advance.setSynchronizedd(false);
        advance.setAddedBy(AUDIT_ACTOR_ID);
        advance.setAddedDatetime(now());
        advance = advanceRepository.save(advance);
        em.flush();            // push the INSERT down before re-reading the generated column
        em.refresh(advance);   // read the DB-generated balance column
        return advance;
    }

    @Transactional(readOnly = true)
    public List<Advance> byPatient(Long patientId) {
        if (patientId == null) {
            throw new InvalidPaymentRequestException("PAY_REQ_INVALID", "patientId query parameter is required");
        }
        return advanceRepository.findByPatientIdOrderByAdvanceIdAsc(patientId);
    }

    @Transactional(readOnly = true)
    public Advance get(long advanceId) {
        return advanceRepository.findById(advanceId)
                .orElseThrow(() -> new PaymentEntityNotFoundException("Advance not found: " + advanceId));
    }

    private Long nextId(String sequenceName) {
        Number n = (Number) em.createNativeQuery("SELECT nextval('ihms_payment." + sequenceName + "')")
                .getSingleResult();
        return n.longValue();
    }

    private static Timestamp now() {
        return Timestamp.from(Instant.now());
    }
}
