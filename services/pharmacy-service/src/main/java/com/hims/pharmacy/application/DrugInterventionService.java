package com.hims.pharmacy.application;

import com.hims.pharmacy.application.exceptions.InvalidSaleRequestException;
import com.hims.pharmacy.domain.model.DrugIntervention;
import com.hims.pharmacy.domain.repository.DrugInterventionRepository;
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
 * Pharmacist clinical drug interventions (register WP-R2): record + per-patient
 * listing. Guards: patientId, interventionType and pharmacistId required (400
 * BEXA003). Emits NOTHING to the outbox (no catalog event for interventions).
 */
@Service
public class DrugInterventionService {

    private static final Logger log = LoggerFactory.getLogger(DrugInterventionService.class);

    public static final long AUDIT_ACTOR_ID = 1L;   // MIG-004: principal id

    private final DrugInterventionRepository repository;

    @PersistenceContext
    private EntityManager em;

    public DrugInterventionService(DrugInterventionRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public DrugIntervention create(Long patientId, Long saleId, String interventionType,
                                   String notes, Long pharmacistId) {
        if (patientId == null) {
            throw new InvalidSaleRequestException("BEXA003", "patientId is required");
        }
        if (interventionType == null || interventionType.isBlank()) {
            throw new InvalidSaleRequestException("BEXA003", "interventionType is required");
        }
        if (pharmacistId == null) {
            throw new InvalidSaleRequestException("BEXA003", "pharmacistId is required");
        }
        DrugIntervention intervention = new DrugIntervention();
        intervention.setInterventionId(nextId());
        intervention.setPatientId(patientId);
        intervention.setSaleId(saleId);
        intervention.setInterventionType(interventionType);
        intervention.setNotes(notes);
        intervention.setPharmacistId(pharmacistId);
        intervention.setAddedBy(AUDIT_ACTOR_ID);
        intervention.setAddedDatetime(Timestamp.from(Instant.now()));
        DrugIntervention saved = repository.save(intervention);
        log.info("Recorded drug intervention {} type {} for patient {}",
                saved.getInterventionId(), interventionType, patientId);
        return saved;
    }

    @Transactional(readOnly = true)
    public List<DrugIntervention> listByPatient(long patientId) {
        return repository.findByPatientIdOrderByInterventionIdDesc(patientId);
    }

    private Long nextId() {
        Number n = (Number) em.createNativeQuery(
                "SELECT nextval('ihms_pharmacy.t_drug_intervention_seq')").getSingleResult();
        return n.longValue();
    }
}
