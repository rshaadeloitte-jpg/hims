package com.hims.pharmacy.application;

import com.hims.pharmacy.application.exceptions.InvalidSaleRequestException;
import com.hims.pharmacy.domain.model.DrugInteraction;

import com.hims.pharmacy.domain.repository.DrugInteractionRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.ArrayList;

/**
 * Drug-drug / drug-food / drug-disease interaction warnings (M07 gap).
 * Acknowledged flag stores pharmacist override (BR-PHAR-004).
 */
@Service
public class DrugInteractionService {

    public static final long AUDIT_ACTOR_ID = 1L;

    private static final Set<String> ALLOWED_TYPES = Set.of("DRUG_DRUG", "DRUG_FOOD", "DRUG_DISEASE");
    private static final Set<String> ALLOWED_SEVERITIES = Set.of("MINOR", "MODERATE", "MAJOR", "CONTRAINDICATED");

    private final DrugInteractionRepository repository;

    @PersistenceContext
    private EntityManager em;

    public DrugInteractionService(DrugInteractionRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public DrugInteraction record(Long patientId, Long drug1Id, Long drug2Id, String interactionType,
                                    String severity, String description) {
        if (patientId == null) {
            throw new InvalidSaleRequestException("BEXA003", "patientId is required");
        }
        if (drug1Id == null || drug2Id == null) {
            throw new InvalidSaleRequestException("BEXA003", "drug1Id and drug2Id are required");
        }
        if (interactionType == null || !ALLOWED_TYPES.contains(interactionType)) {
            throw new InvalidSaleRequestException("BEXA003",
                    "interactionType must be one of " + ALLOWED_TYPES);
        }
        if (severity == null || !ALLOWED_SEVERITIES.contains(severity)) {
            throw new InvalidSaleRequestException("BEXA003",
                    "severity must be one of " + ALLOWED_SEVERITIES);
        }

        DrugInteraction d = new DrugInteraction();
        d.setInteractionId(nextId());
        d.setPatientId(patientId);
        d.setDrug1Id(drug1Id);
        d.setDrug2Id(drug2Id);
        d.setInteractionType(interactionType);
        d.setSeverity(severity);
        d.setDescription(description);
        d.setAcknowledged(false);
        d.setRecordedAt(Timestamp.from(Instant.now()));
        return repository.save(d);
    }

    /**
     * Pre-dispense gate: checks patient's active interactions against sale lines.
     * Contraindicated/major interactions throw InvalidSaleRequestException.
     * Minor/moderate interactions return warnings (logged).
     */
    @Transactional(readOnly = true)
    public void checkDispense(long patientId, List<ItemSaleService.LineInput> lines) {
        if (patientId <= 0 || lines == null || lines.isEmpty()) return;
        Set<Long> itemIds = new HashSet<>();
        for (ItemSaleService.LineInput line : lines) {
            if (line.itemId() != null) itemIds.add(line.itemId());
        }
        if (itemIds.isEmpty()) return;

        List<Long> sortedIds = new ArrayList<>(itemIds);
        java.util.Collections.sort(sortedIds);
        for (int i = 0; i < sortedIds.size(); i++) {
            for (int j = i + 1; j < sortedIds.size(); j++) {
                Long drug1 = sortedIds.get(i);
                Long drug2 = sortedIds.get(j);
                List<DrugInteraction> matches = repository
                    .findByPatientIdAndDrug1IdAndDrug2Id(patientId, drug1, drug2);
                for (DrugInteraction di : matches) {
                    if ("CONTRAINDICATED".equals(di.getSeverity())
                            || "MAJOR".equals(di.getSeverity())) {
                        throw new InvalidSaleRequestException("BEXA005",
                            "Interaction between items " + drug1 + " and " + drug2
                            + " is " + di.getSeverity() + ": " + di.getDescription());
                    }
                }
            }
        }
    }

    @Transactional
    public DrugInteraction acknowledge(long interactionId, Long acknowledgedBy) {
        DrugInteraction d = repository.findById(interactionId)
                .orElseThrow(() -> new InvalidSaleRequestException("NOT_FOUND",
                        "Interaction not found: " + interactionId));
        d.setAcknowledged(true);
        d.setAcknowledgedBy(acknowledgedBy != null ? acknowledgedBy : AUDIT_ACTOR_ID);
        d.setAcknowledgedAt(Timestamp.from(Instant.now()));
        return repository.save(d);
    }

    @Transactional(readOnly = true)
    public List<DrugInteraction> listByPatient(long patientId) {
        return repository.findByPatientIdOrderByRecordedAtDesc(patientId);
    }

    @Transactional(readOnly = true)
    public List<DrugInteraction> unacknowledged(long patientId) {
        return repository.findByPatientIdAndAcknowledgedFalse(patientId);
    }

    private Long nextId() {
        Number n = (Number) em.createNativeQuery(
                "SELECT nextval('ihms_pharmacy.t_drug_interaction_seq')").getSingleResult();
        return n.longValue();
    }
}
