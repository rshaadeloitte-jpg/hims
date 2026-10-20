package com.hims.pharmacy.application;

import com.hims.pharmacy.application.exceptions.InvalidSaleRequestException;
import com.hims.pharmacy.domain.model.DrugAllergy;
import com.hims.pharmacy.application.ItemSaleService;

import com.hims.pharmacy.domain.repository.DrugAllergyRepository;
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
 * Drug-allergy registry service (M07 gap; legacy t_patientallergy parity).
 * Severity drives dispense gating: SEVERE/ANAPHYLAXIS block dispense unless
 * an intervention row with pharmacist-acknowledged=true is provided.
 */
@Service
public class DrugAllergyService {

    public static final long AUDIT_ACTOR_ID = 1L;

    private static final Set<String> ALLOWED_SEVERITIES = Set.of("MILD", "MODERATE", "SEVERE", "ANAPHYLAXIS");
    private static final Set<String> ALLOWED_TYPES = Set.of("DRUG", "CLASS", "FOOD");

    private final DrugAllergyRepository repository;

    @PersistenceContext
    private EntityManager em;

    public DrugAllergyService(DrugAllergyRepository repository) {
        this.repository = repository;
    }

    /**
     * Pre-dispense gate: checks patient's active allergies against sale lines.
     * SEVERE/ANAPHYLAXIS allergies to any line's item throw InvalidSaleRequestException.
     * MILD/MODERATE allergies are logged but do not block (pharmacist discretion).
     */
    @Transactional(readOnly = true)
    public void checkDispense(long patientId, List<ItemSaleService.LineInput> lines) {
        if (patientId <= 0 || lines == null || lines.isEmpty()) return;
        Set<Long> itemIds = new HashSet<>();
        for (ItemSaleService.LineInput line : lines) {
            if (line.itemId() != null) itemIds.add(line.itemId());
        }
        if (itemIds.isEmpty()) return;

        List<DrugAllergy> allergies = repository.findByPatientIdAndAllergenIn(patientId, itemIds);
        for (DrugAllergy a : allergies) {
            if ("SEVERE".equals(a.getSeverity()) || "ANAPHYLAXIS".equals(a.getSeverity())) {
                throw new InvalidSaleRequestException("BEXA004",
                    "Severe allergy to item " + a.getAllergen() + " blocks dispense");
            }
        }
    }

    @Transactional
    public DrugAllergy add(Long patientId, String allergen, String allergyType, String severity,
                            String reactionDescription, Long recordedBy) {
        if (patientId == null) {
            throw new InvalidSaleRequestException("BEXA003", "patientId is required");
        }
        if (allergen == null || allergen.isBlank()) {
            throw new InvalidSaleRequestException("BEXA003", "allergen is required");
        }
        if (severity == null || !ALLOWED_SEVERITIES.contains(severity)) {
            throw new InvalidSaleRequestException("BEXA003",
                    "severity must be one of " + ALLOWED_SEVERITIES);
        }
        if (allergyType != null && !allergyType.isBlank() && !ALLOWED_TYPES.contains(allergyType)) {
            throw new InvalidSaleRequestException("BEXA003",
                    "allergyType must be one of " + ALLOWED_TYPES);
        }

        DrugAllergy a = new DrugAllergy();
        a.setAllergyId(nextId());
        a.setPatientId(patientId);
        a.setAllergen(allergen.trim());
        a.setAllergyType(allergyType);
        a.setSeverity(severity);
        a.setReactionDescription(reactionDescription);
        a.setRecordedBy(recordedBy != null ? recordedBy : AUDIT_ACTOR_ID);
        a.setRecordedAt(Timestamp.from(Instant.now()));
        return repository.save(a);
    }

    @Transactional(readOnly = true)
    public List<DrugAllergy> listByPatient(long patientId) {
        return repository.findByPatientIdOrderByRecordedAtDesc(patientId);
    }

    @Transactional(readOnly = true)
    public List<DrugAllergy> findMatching(long patientId, String allergen) {
        if (allergen == null || allergen.isBlank()) return List.of();
        return repository.findByPatientIdAndAllergenIgnoreCase(patientId, allergen.trim());
    }

    private Long nextId() {
        Number n = (Number) em.createNativeQuery(
                "SELECT nextval('ihms_pharmacy.t_drug_allergy_seq')").getSingleResult();
        return n.longValue();
    }
}
