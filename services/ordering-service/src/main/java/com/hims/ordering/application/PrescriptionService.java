package com.hims.ordering.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hims.ordering.application.exceptions.InvalidPrescriptionException;
import com.hims.ordering.application.exceptions.PrescriptionNotFoundException;
import com.hims.ordering.domain.model.Prescription;
import com.hims.ordering.domain.model.PrescriptionItem;
import com.hims.ordering.domain.repository.PrescriptionItemRepository;
import com.hims.ordering.domain.repository.PrescriptionRepository;
import com.hims.ordering.infrastructure.outbox.OutboxPublisher;
import com.hims.ordering.infrastructure.outbox.OutboxService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Ordering &amp; Prescription slice (GAP-REGISTER-4 WP-10, Catalog #4). NEW service
 * replacing the ordering fragment of the legacy mega-txn (AddServiceOrderServiceImpl);
 * the full order saga (Lab/Rad OrderCreated, billing choreography) is DEFERRED per
 * KAFKA-EVENT-CATALOG §3 — never silently invented here.
 *
 * Status machine (t_prescription.status_code SMALLINT):
 *   1=ACTIVE -> cancel -> 3=CANCELLED (guard ACTIVE only)
 *   2=COMPLETED reserved for the completion flow of a later slice.
 *
 * E-06 PrescriptionCreated is emitted via the transactional outbox on CREATE ONLY —
 * type string 'PrescriptionCreated', schema token prescription-created.v1,
 * topic ihms.ordering.events, partition key encounterId (documented fallback:
 * '&lt;admissionId&gt;' when encounterId is NULL; 'no-context:&lt;id&gt;' when both are null),
 * payload keys EXACTLY {prescriptionId, patientId, encounterId, admissionId,
 * lines[{itemId,qty,dose,frequency}], prescriberId, wardId} — the encounter/admission
 * slot that was not supplied travels as explicit null (sibling lab-service convention).
 * Cancel emits NOTHING.
 */
@Service
public class PrescriptionService {

    private static final Logger log = LoggerFactory.getLogger(PrescriptionService.class);

    public static final long AUDIT_ACTOR_ID = 1L;   // MIG-004: principal id

    private final PrescriptionRepository prescriptionRepository;
    private final PrescriptionItemRepository detailRepository;
    private final OutboxService outboxService;

    @PersistenceContext
    private EntityManager em;

    public PrescriptionService(PrescriptionRepository prescriptionRepository,
                               PrescriptionItemRepository detailRepository,
                               OutboxService outboxService) {
        this.prescriptionRepository = prescriptionRepository;
        this.detailRepository = detailRepository;
        this.outboxService = outboxService;
    }

    // ------------------------------------------------------------------ create

    /**
     * Create a prescription header + lines. Guards (all 400 BEXA003-style):
     * patientId required; prescriberId required; at least ONE line required;
     * encounterId XOR admissionId — at most one of the two may be supplied;
     * every line needs itemId, qty &gt; 0 AND dose + frequency (BR-ORD-005 clinical
     * safety gate). Emits exactly one E-06 in the same tx.
     */
    @Transactional
    public Prescription create(Long patientId, Long encounterId, Long admissionId,
                               Long prescriberId, Long wardId, List<LineInput> lines) {
        if (patientId == null) {
            throw new InvalidPrescriptionException("BEXA003", "patientId is required");
        }
        if (prescriberId == null) {
            throw new InvalidPrescriptionException("BEXA003", "prescriberId is required");
        }
        if (encounterId != null && admissionId != null) {
            throw new InvalidPrescriptionException("BEXA003",
                    "encounterId and admissionId are mutually exclusive — supply at most one");
        }
        if (lines == null || lines.isEmpty()) {
            throw new InvalidPrescriptionException("BEXA003",
                    "At least one prescription line {itemId,qty,dose,frequency} is required");
        }
        for (LineInput line : lines) {
            if (line == null || line.itemId() == null) {
                throw new InvalidPrescriptionException("BEXA003",
                        "Each line needs a non-null itemId");
            }
            if (line.qty() == null || line.qty().signum() <= 0) {
                throw new InvalidPrescriptionException("BEXA003",
                        "Line qty must be > 0 for itemId " + line.itemId());
            }
            if (line.dose() == null || line.dose().isBlank()) {
                throw new InvalidPrescriptionException("BR-ORD-005",
                        "dose is required for itemId " + line.itemId());
            }
            if (line.frequency() == null || line.frequency().isBlank()) {
                throw new InvalidPrescriptionException("BR-ORD-005",
                        "frequency is required for itemId " + line.itemId());
            }
        }

        Timestamp nowTs = now();
        Prescription prescription = new Prescription();
        prescription.setPrescriptionId(nextId("prescription_seq"));
        prescription.setPatientId(patientId);
        prescription.setEncounterId(encounterId);
        prescription.setAdmissionId(admissionId);
        prescription.setPrescriberId(prescriberId);
        prescription.setWardId(wardId);
        prescription.setStatusCode(Prescription.STATUS_ACTIVE);
        prescription.setStatus(true);
        prescription.setSynchronizedd(false);
        prescription.setAddedBy(AUDIT_ACTOR_ID);
        prescription.setAddedDatetime(nowTs);
        prescriptionRepository.save(prescription);

        List<PrescriptionItem> savedLines = new ArrayList<>();
        long lineNo = 1L;
        for (LineInput line : lines) {
            PrescriptionItem d = new PrescriptionItem();
            d.setPrescriptionId(prescription.getPrescriptionId());
            d.setLineNo(lineNo++);
            d.setItemId(line.itemId());
            d.setQty(line.qty());
            d.setDose(line.dose());
            d.setFrequency(line.frequency());
            d.setStatus(true);
            d.setSynchronizedd(false);
            d.setAddedBy(AUDIT_ACTOR_ID);
            d.setAddedDatetime(nowTs);
            detailRepository.save(d);
            savedLines.add(d);
        }

        emitPrescriptionCreated(prescription, savedLines);   // E-06, same tx, CREATE ONLY
        log.info("Created prescription {} for patient {} with {} line(s)",
                prescription.getPrescriptionId(), patientId, savedLines.size());
        return get(prescription.getPrescriptionId());
    }

    /**
     * E-06 PrescriptionCreated — type string EXACTLY 'PrescriptionCreated',
     * schema token prescription-created.v1 (OutboxPublisher.schemaOf),
     * partition key encounterId; documented fallback '&lt;admissionId&gt;' when
     * encounterId is NULL (same pattern as lab WP-7), 'no-context:&lt;id&gt;' when
     * both are null (partition_key column is NOT NULL).
     * Payload keys EXACTLY {prescriptionId, patientId, encounterId|admissionId,
     * lines[{itemId,qty,dose,frequency}], prescriberId, wardId}.
     */
    void emitPrescriptionCreated(Prescription prescription, List<PrescriptionItem> details) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("prescriptionId", String.valueOf(prescription.getPrescriptionId()));
        payload.put("patientId", String.valueOf(prescription.getPatientId()));
        payload.put("encounterId", prescription.getEncounterId() == null
                ? null : String.valueOf(prescription.getEncounterId()));
        payload.put("admissionId", prescription.getAdmissionId() == null
                ? null : String.valueOf(prescription.getAdmissionId()));
        List<Map<String, Object>> lines = new ArrayList<>();
        for (PrescriptionItem d : details) {
            Map<String, Object> line = new LinkedHashMap<>();
            line.put("itemId", String.valueOf(d.getItemId()));
            line.put("qty", d.getQty());                       // numeric per schema NUMERIC(12,2)
            line.put("dose", d.getDose());
            line.put("frequency", d.getFrequency());
            lines.add(line);
        }
        payload.put("lines", lines);
        payload.put("prescriberId", String.valueOf(prescription.getPrescriberId()));
        payload.put("wardId", prescription.getWardId() == null
                ? null : String.valueOf(prescription.getWardId()));

        String partitionKey;
        if (prescription.getEncounterId() != null) {
            partitionKey = String.valueOf(prescription.getEncounterId());
        } else if (prescription.getAdmissionId() != null) {
            partitionKey = String.valueOf(prescription.getAdmissionId());   // documented fallback '<admissionId>'
        } else {
            partitionKey = "no-context:" + prescription.getPrescriptionId(); // documented fallback, column NOT NULL
        }
        outboxService.append(OutboxPublisher.OrderingEvents.PRESCRIPTION_CREATED, "prescription",
                prescription.getPrescriptionId(), partitionKey, toJson(payload),
                "prescription-created:" + prescription.getPrescriptionId());
        log.info("Emitted {} for prescription {} (partition key {})",
                OutboxPublisher.OrderingEvents.PRESCRIPTION_CREATED,
                prescription.getPrescriptionId(), partitionKey);
    }

    // ------------------------------------------------------------------ cancel

    /** Guard ACTIVE only: 1 -> 3. Cancelled/Completed prescriptions reject with ORD_ILLEGAL_TRANSITION. */
    @Transactional
    public Prescription cancel(long prescriptionId) {
        Prescription prescription = load(prescriptionId);
        if (!prescription.isActive()) {
            throw new InvalidPrescriptionException("ORD_ILLEGAL_TRANSITION",
                    "Cannot cancel prescription " + prescriptionId + " from status "
                            + statusName(prescription.getStatusCode())
                            + " — only ACTIVE prescriptions can be cancelled");
        }
        prescription.setStatusCode(Prescription.STATUS_CANCELLED);
        prescription.setUpdatedBy(AUDIT_ACTOR_ID);
        prescription.setUpdatedDatetime(now());
        prescriptionRepository.save(prescription);
        log.info("Cancelled prescription {}", prescriptionId);
        return get(prescriptionId);   // NO event on cancel — E-06 is create-only per register
    }

    // ------------------------------------------------------------------ queries

    @Transactional(readOnly = true)
    public Prescription get(long prescriptionId) {
        Prescription prescription = load(prescriptionId);
        prescription.setDetails(detailsOf(prescriptionId));
        return prescription;
    }

    @Transactional(readOnly = true)
    public List<Prescription> list(Short status, Long patientId) {
        List<Prescription> prescriptions = prescriptionRepository.search(status, patientId);
        for (Prescription p : prescriptions) {
            p.setDetails(detailsOf(p.getPrescriptionId()));
        }
        return prescriptions;
    }

    // ------------------------------------------------------------------ helpers

    private Prescription load(long prescriptionId) {
        return prescriptionRepository.findById(prescriptionId)
                .orElseThrow(() -> new PrescriptionNotFoundException("NOT_FOUND",
                        "Prescription not found: " + prescriptionId));
    }

    private List<PrescriptionItem> detailsOf(long prescriptionId) {
        return detailRepository.findByPrescriptionIdOrderByLineNoAsc(prescriptionId);
    }

    static String statusName(short s) {
        return switch (s) {
            case 1 -> "ACTIVE";
            case 2 -> "COMPLETED";
            case 3 -> "CANCELLED";
            default -> String.valueOf(s);
        };
    }

    private String toJson(Map<String, Object> payload) {
        try {
            return new ObjectMapper().writeValueAsString(payload);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize outbox payload", e);
        }
    }

    private Long nextId(String sequenceName) {
        Number n = (Number) em.createNativeQuery("SELECT nextval('ihms_ordering." + sequenceName + "')")
                .getSingleResult();
        return n.longValue();
    }

    private static Timestamp now() {
        return Timestamp.from(Instant.now());
    }

    /** One requested prescription line at creation. */
    public record LineInput(Long itemId, BigDecimal qty, String dose, String frequency) {}
}
