package com.hims.pharmacy.application;

import com.hims.pharmacy.application.exceptions.BcmaVerificationException;
import com.hims.pharmacy.application.exceptions.HomeMedicationNotFoundException;
import com.hims.pharmacy.domain.model.HomeMedication;
import com.hims.pharmacy.domain.repository.HomeMedicationRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

/**
 * Discharge home medication service.
 * Emits NOTHING to the outbox.
 */
@Service
public class HomeMedicationService {

    public static final long AUDIT_ACTOR_ID = 1L;

    private final HomeMedicationRepository repository;

    @PersistenceContext
    private EntityManager em;

    public HomeMedicationService(HomeMedicationRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public HomeMedication prescribe(Long patientId, Long encounterId, Long admissionId,
                                     Long itemId, String itemName, String dosage,
                                     String frequency, Integer durationDays,
                                     java.math.BigDecimal quantity, String uom,
                                     String instructions, Long prescribedBy) {
        if (patientId == null) {
            throw new BcmaVerificationException("BEXA003", "patientId is required");
        }
        if (encounterId == null) {
            throw new BcmaVerificationException("BEXA003", "encounterId is required");
        }
        if (itemId == null || itemName == null) {
            throw new BcmaVerificationException("BEXA003", "itemId and itemName are required");
        }
        Timestamp now = Timestamp.from(Instant.now());
        HomeMedication hm = new HomeMedication();
        hm.setHomeMedId(nextId("ihms_pharmacy.t_home_medication_seq"));
        hm.setPatientId(patientId);
        hm.setEncounterId(encounterId);
        hm.setAdmissionId(admissionId);
        hm.setItemId(itemId);
        hm.setItemName(itemName);
        hm.setDosage(dosage);
        hm.setFrequency(frequency);
        hm.setDurationDays(durationDays);
        hm.setQuantity(quantity);
        hm.setUom(uom);
        hm.setInstructions(instructions);
        hm.setPrescribedBy(prescribedBy);
        hm.setPrescribedDate(now);
        hm.setDispensed(false);
        hm.setAddedBy(prescribedBy != null ? prescribedBy : AUDIT_ACTOR_ID);
        hm.setAddedDatetime(now);
        return repository.save(hm);
    }

    @Transactional
    public HomeMedication markDispensed(Long homeMedId, Long userId) {
        HomeMedication hm = load(homeMedId);
        if (hm.getDispensed()) {
            throw new BcmaVerificationException("PH_ILLEGAL_TRANSITION",
                    "Home medication " + homeMedId + " already dispensed");
        }
        Timestamp now = Timestamp.from(Instant.now());
        hm.setDispensed(true);
        hm.setDispensedAt(now);
        hm.setUpdatedBy(userId);
        hm.setUpdatedDatetime(now);
        return repository.save(hm);
    }

    @Transactional(readOnly = true)
    public HomeMedication get(Long homeMedId) {
        return load(homeMedId);
    }

    @Transactional(readOnly = true)
    public List<HomeMedication> listForPatient(Long patientId) {
        return repository.findByPatientId(patientId);
    }

    @Transactional(readOnly = true)
    public List<HomeMedication> listForEncounter(Long encounterId) {
        return repository.findByEncounterId(encounterId);
    }

    @Transactional(readOnly = true)
    public List<HomeMedication> listPendingDispense() {
        return repository.findPendingDispense();
    }

    private HomeMedication load(Long homeMedId) {
        return repository.findById(homeMedId)
                .orElseThrow(() -> new HomeMedicationNotFoundException("NOT_FOUND",
                        "Home medication not found: " + homeMedId));
    }

    private Long nextId(String sequenceName) {
        Number n = (Number) em.createNativeQuery("SELECT nextval('" + sequenceName + "')").getSingleResult();
        return n.longValue();
    }
}
