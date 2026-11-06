package com.hims.waste.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hims.waste.application.exceptions.InvalidWasteRequestException;
import com.hims.waste.application.exceptions.WasteNotFoundException;
import com.hims.waste.domain.model.BioMedicalWaste;
import com.hims.waste.domain.model.BioMedicalWasteDetails;
import com.hims.waste.domain.model.WasteStatus;
import com.hims.waste.domain.repository.BioMedicalWasteDetailsRepository;
import com.hims.waste.domain.repository.BioMedicalWasteRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Bio-medical waste request lifecycle (MIG-023 sub-slice, S19). Clean target of
 * legacy BioMedicalWasteManagementServiceImpl (T_BIOMEDICALWASTE +
 * T_BIOMEDICALWASTEDETAILS). REST-only: BMWRecorded REJECTED phase 1
 * (KAFKA-EVENT-CATALOG line 206) — no outbox until a second consumer appears.
 *
 * <p>Preserved legacy behavior:
 * <ul>
 *   <li>raise expands each requested quantity into qty=1 bag rows, each with a
 *       unique 13-char barcode {yyyyMM}{%07d monthly counter};</li>
 *   <li>requisition number {unitPrefix}/BW/{year-2000}/{%05d seq}
 *       (biowaste.no.template=%s/BW/%d/%s);</li>
 *   <li>approve/handover/receive/process stamp request + all bag statuses;</li>
 *   <li>soft delete sets status=false + requestStatus=0;</li>
 *   <li>updateBagStatus increments per-bag status and re-derives the request
 *       status as the minimum bag status;</li>
 *   <li>numeric list search also matches bag barcodes.</li>
 * </ul>
 *
 * <p>Documented improvements (IMP-DEC-033): forward-only lifecycle guards
 * (legacy raw UPDATEs had none), and the barcode counter moved from Redis into
 * the DB transaction so allocation rolls back with the request.
 *
 * <p>Deferred cross-domain integrations: unitPrefix lookup from M_UNITMASTER
 * (caller-supplied until reference-service integration), handedOverToFirm
 * supplier lookup (M_SUPPLIER, MIG-023 equipment/supplier slice), barcode
 * label decoration from M_UNITMASTER name/pincode/code (getBioRequestDetails).
 */
@Service
public class BioMedicalWasteService {

    private static final Logger log = LoggerFactory.getLogger(BioMedicalWasteService.class);

    public static final long AUDIT_ACTOR_ID = 1L;   // MIG-004: principal id
    public static final String DEFAULT_UNIT_PREFIX = "IHMS";
    public static final int BARCODE_LENGTH = 13;

    private final BioMedicalWasteRepository requestRepository;
    private final BioMedicalWasteDetailsRepository detailsRepository;
    private final ObjectMapper objectMapper;

    @PersistenceContext
    private EntityManager em;

    public BioMedicalWasteService(BioMedicalWasteRepository requestRepository,
                                  BioMedicalWasteDetailsRepository detailsRepository,
                                  ObjectMapper objectMapper) {
        this.requestRepository = requestRepository;
        this.detailsRepository = detailsRepository;
        this.objectMapper = objectMapper;
    }

    // ----------------------------------------------------------------- raise

    /**
     * Raise a disposal request (legacy addNewBioRequest). Payload shape mirrors
     * legacy BioMedicalWasteView: {@code {"request": {...}, "details": [{bagId,
     * quantity, remark, weight}, ...]}}. Each detail with quantity &gt; 0 is
     * expanded into quantity individual bag rows (qty=1) with barcodes.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public BioMedicalWaste raiseRequest(JsonNode payload, Long unitId, String unitPrefix) {
        if (payload == null || payload.isNull() || payload.isMissingNode()
                || payload.path("request").isMissingNode() || payload.path("request").isNull()) {
            throw new InvalidWasteRequestException("BEXA003", "Invalid input data !");
        }
        BioMedicalWaste request = bind(payload.path("request"), BioMedicalWaste.class);
        request.setId(nextId("bio_medical_waste_seq"));
        request.setRequisitionNo(generateRequisitionNo(unitPrefix));
        request.setRequestStatus(WasteStatus.PENDING);
        request.setRequestDate(Timestamp.from(Instant.now()));
        request.setStatus(request.getStatus() == null ? true : request.getStatus());
        stampCreate(request, unitId);
        BioMedicalWaste saved = requestRepository.save(request);

        JsonNode details = payload.path("details");
        if (details.isArray()) {
            String monthKey = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM"));
            for (JsonNode node : details) {
                BioMedicalWasteDetails template = bind(node, BioMedicalWasteDetails.class);
                long qty = template.getQuantity() == null ? 0L : template.getQuantity();
                if (qty <= 0) {
                    continue;
                }
                for (long i = 0; i < qty; i++) {
                    BioMedicalWasteDetails bag = new BioMedicalWasteDetails();
                    bag.setId(nextId("bio_medical_waste_details_seq"));
                    bag.setBioMedicalWasteId(saved.getId());
                    bag.setBagId(template.getBagId());
                    bag.setQuantity(1L);
                    bag.setRemark(template.getRemark());
                    bag.setWeight(template.getWeight());
                    bag.setBagStatus(WasteStatus.PENDING);
                    bag.setBarcodeNo(monthKey + String.format("%07d", nextBarcodeCounter(monthKey, unitId)));
                    bag.setStatus(true);
                    stampCreate(bag, unitId);
                    detailsRepository.save(bag);
                }
            }
        }
        log.info("BMW request raised id={} requisitionNo={}", saved.getId(), saved.getRequisitionNo());
        return saved;
    }

    // ------------------------------------------------------------ lifecycle

    /** Approve (legacy approveBioMedicalRequest): PENDING -> ACCEPTED, request + all bags. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public BioMedicalWaste approve(Long id) {
        BioMedicalWaste request = getRequest(id);
        requireStatus(request, WasteStatus.PENDING, "approve");
        request.setRequestStatus(WasteStatus.ACCEPTED);
        stampUpdate(request);
        for (BioMedicalWasteDetails bag : detailsRepository.findByBioMedicalWasteIdOrderByIdAsc(id)) {
            bag.setBagStatus(WasteStatus.ACCEPTED);
            stampUpdate(bag);
            detailsRepository.save(bag);
        }
        return requestRepository.save(request);
    }

    /** Hand over (legacy handOverBags): ACCEPTED -> HANDOVERED, handedOverTo stamped on request + bags. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public BioMedicalWaste handover(Long id, String handedOverTo) {
        if (handedOverTo == null || handedOverTo.isBlank()) {
            throw new InvalidWasteRequestException("BEXA003", "handedOverTo is required");
        }
        BioMedicalWaste request = getRequest(id);
        requireStatus(request, WasteStatus.ACCEPTED, "handover");
        request.setRequestStatus(WasteStatus.HANDOVERED);
        request.setHandedOverTo(handedOverTo);
        stampUpdate(request);
        for (BioMedicalWasteDetails bag : detailsRepository.findByBioMedicalWasteIdOrderByIdAsc(id)) {
            bag.setBagStatus(WasteStatus.HANDOVERED);
            bag.setHandedOverTo(handedOverTo);
            stampUpdate(bag);
            detailsRepository.save(bag);
        }
        return requestRepository.save(request);
    }

    /**
     * Receive at main store (legacy receiveBioRequestMainStore): HANDOVERED ->
     * RECEIVED, receivedBy stamped; optional per-bag weight capture
     * ({@code [{"id": bagId, "weight": 1.5}, ...]}).
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public BioMedicalWaste receive(Long id, String receivedBy, JsonNode bagWeights) {
        BioMedicalWaste request = getRequest(id);
        requireStatus(request, WasteStatus.HANDOVERED, "receive");
        request.setRequestStatus(WasteStatus.RECEIVED);
        request.setReceivedBy(receivedBy);
        stampUpdate(request);
        if (bagWeights != null && bagWeights.isArray()) {
            for (JsonNode node : bagWeights) {
                long bagId = node.path("id").asLong(-1);
                BioMedicalWasteDetails bag = detailsRepository.findById(bagId)
                        .orElseThrow(() -> new WasteNotFoundException("NOT_FOUND", "Bag not found"));
                if (!id.equals(bag.getBioMedicalWasteId())) {
                    throw new InvalidWasteRequestException("BEXA003", "Bag does not belong to this request");
                }
                if (node.hasNonNull("weight")) {
                    bag.setWeight(node.get("weight").decimalValue());
                }
                bag.setBagStatus(WasteStatus.RECEIVED);
                stampUpdate(bag);
                detailsRepository.save(bag);
            }
        } else {
            for (BioMedicalWasteDetails bag : detailsRepository.findByBioMedicalWasteIdOrderByIdAsc(id)) {
                bag.setBagStatus(WasteStatus.RECEIVED);
                stampUpdate(bag);
                detailsRepository.save(bag);
            }
        }
        return requestRepository.save(request);
    }

    /**
     * Process/complete (legacy processBioRequest): RECEIVED -> COMPLETED,
     * firm details stamped on all bags.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public BioMedicalWaste process(Long id, String firmName, String contactPerson, Long contactNo) {
        BioMedicalWaste request = getRequest(id);
        requireStatus(request, WasteStatus.RECEIVED, "process");
        request.setRequestStatus(WasteStatus.COMPLETED);
        stampUpdate(request);
        for (BioMedicalWasteDetails bag : detailsRepository.findByBioMedicalWasteIdOrderByIdAsc(id)) {
            bag.setBagStatus(WasteStatus.COMPLETED);
            bag.setFirmName(firmName);
            bag.setContactPerson(contactPerson);
            bag.setContactNo(contactNo);
            stampUpdate(bag);
            detailsRepository.save(bag);
        }
        return requestRepository.save(request);
    }

    /** Soft delete (legacy deleteBioRequest): status=false + requestStatus=0. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public BioMedicalWaste deleteRequest(Long id) {
        BioMedicalWaste request = getRequest(id);
        request.setStatus(false);
        request.setRequestStatus(0L);
        stampUpdate(request);
        return requestRepository.save(request);
    }

    /**
     * Update bag quantity/remark/weight before approval (legacy updateBioRequest
     * detail update). Bag ids must belong to the request.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public List<BioMedicalWasteDetails> updateBags(Long id, JsonNode bags) {
        BioMedicalWaste request = getRequest(id);
        requireStatus(request, WasteStatus.PENDING, "update bags");
        if (bags == null || !bags.isArray()) {
            throw new InvalidWasteRequestException("BEXA003", "Invalid input data !");
        }
        List<BioMedicalWasteDetails> updated = new ArrayList<>();
        for (JsonNode node : bags) {
            long bagId = node.path("id").asLong(-1);
            BioMedicalWasteDetails bag = detailsRepository.findById(bagId)
                    .orElseThrow(() -> new WasteNotFoundException("NOT_FOUND", "Bag not found"));
            if (!id.equals(bag.getBioMedicalWasteId())) {
                throw new InvalidWasteRequestException("BEXA003", "Bag does not belong to this request");
            }
            if (node.hasNonNull("quantity")) {
                bag.setQuantity(node.get("quantity").asLong());
            }
            if (node.hasNonNull("remark")) {
                bag.setRemark(node.get("remark").asText());
            }
            if (node.hasNonNull("weight")) {
                bag.setWeight(node.get("weight").decimalValue());
            }
            stampUpdate(bag);
            updated.add(detailsRepository.save(bag));
        }
        return updated;
    }

    /**
     * Per-bag status advance (legacy updateBagStatus): each listed bag advances
     * bagStatus+1 (capped at COMPLETED — legacy had no cap); when the payload
     * entry carries bagStatus=ACCEPTED(2) the handedOverTo is stamped. The
     * request status is then re-derived as the minimum bag status, and a
     * supplied handedOverTo is appended to the request's handedOverTo.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public BioMedicalWaste updateBagStatus(Long requestId, JsonNode bags) {
        BioMedicalWaste request = getRequest(requestId);
        if (bags == null || !bags.isArray() || bags.isEmpty()) {
            throw new InvalidWasteRequestException("BEXA003", "Invalid input data !");
        }
        String handedOverTo = null;
        for (JsonNode node : bags) {
            long bagId = node.path("id").asLong(-1);
            BioMedicalWasteDetails bag = detailsRepository.findById(bagId)
                    .orElseThrow(() -> new WasteNotFoundException("NOT_FOUND", "Bag not found"));
            if (!requestId.equals(bag.getBioMedicalWasteId())) {
                throw new InvalidWasteRequestException("BEXA003", "Bag does not belong to this request");
            }
            long next = Math.min((bag.getBagStatus() == null ? WasteStatus.PENDING : bag.getBagStatus()) + 1,
                    WasteStatus.COMPLETED);
            bag.setBagStatus(next);
            if (node.hasNonNull("weight")) {
                bag.setWeight(node.get("weight").decimalValue());
            }
            if (node.hasNonNull("remark") && !node.get("remark").asText().isBlank()) {
                bag.setRemark(node.get("remark").asText());
            }
            if (node.path("bagStatus").asLong(-1) == WasteStatus.ACCEPTED && node.hasNonNull("handedOverTo")) {
                bag.setHandedOverTo(node.get("handedOverTo").asText());
                handedOverTo = node.get("handedOverTo").asText();
            }
            stampUpdate(bag);
            detailsRepository.save(bag);
        }
        Long minStatus = detailsRepository.minBagStatus(requestId).orElse(request.getRequestStatus());
        request.setRequestStatus(minStatus);
        if (handedOverTo != null && !handedOverTo.isBlank()) {
            request.setHandedOverTo(request.getHandedOverTo() == null
                    ? handedOverTo : request.getHandedOverTo() + "," + handedOverTo);
        }
        stampUpdate(request);
        return requestRepository.save(request);
    }

    // ----------------------------------------------------------------- reads

    @Transactional(readOnly = true)
    public BioMedicalWaste getRequest(Long id) {
        return requestRepository.findById(id)
                .orElseThrow(() -> new WasteNotFoundException("NOT_FOUND", "Bio-medical waste request not found"));
    }

    @Transactional(readOnly = true)
    public List<BioMedicalWaste> listRequests(Long unitId, Long requestStatus, String search) {
        String trimmed = (search == null || search.isBlank()) ? null : search.trim();
        boolean barcodeSearch = trimmed != null && trimmed.matches("\\d+");
        // Sentinel values (0L / "") are bound instead of null: PostgreSQL cannot
        // type an untyped null parameter; the boolean gates ignore them when absent.
        return requestRepository.search(
                unitId == null ? 0L : unitId, unitId != null,
                requestStatus == null ? 0L : requestStatus, requestStatus != null,
                trimmed == null ? "" : trimmed, trimmed != null,
                barcodeSearch);
    }

    @Transactional(readOnly = true)
    public List<BioMedicalWasteDetails> getDetails(Long id) {
        getRequest(id); // 404 for unknown request
        return detailsRepository.findByBioMedicalWasteIdOrderByIdAsc(id);
    }

    /** Dashboard counts (legacy getBioMedWasteCount). */
    @Transactional(readOnly = true)
    public Map<String, Long> getCounts() {
        Map<String, Long> counts = new LinkedHashMap<>();
        counts.put("TOTAL", requestRepository.countByStatusTrue());
        counts.put("PENDING", requestRepository.countByStatusTrueAndRequestStatus(WasteStatus.PENDING));
        counts.put("ACCEPTED", requestRepository.countByStatusTrueAndRequestStatus(WasteStatus.ACCEPTED));
        counts.put("HANDOVERED", requestRepository.countByStatusTrueAndRequestStatus(WasteStatus.HANDOVERED));
        counts.put("RECEIVED", requestRepository.countByStatusTrueAndRequestStatus(WasteStatus.RECEIVED));
        counts.put("COMPLETED", requestRepository.countByStatusTrueAndRequestStatus(WasteStatus.COMPLETED));
        return counts;
    }

    /** Barcode belongs to this request (legacy validateBarcode). */
    @Transactional(readOnly = true)
    public boolean validateBarcode(Long requestId, String barcode) {
        if (barcode == null || barcode.isBlank()) {
            throw new InvalidWasteRequestException("BEXA003", "barcode is required");
        }
        return detailsRepository.findByBioMedicalWasteIdAndBarcodeNo(requestId, barcode).isPresent();
    }

    /** Bag weight by barcode (legacy getBagWeightByBarcode). */
    @Transactional(readOnly = true)
    public BigDecimal getBagWeightByBarcode(Long requestId, String barcode) {
        if (barcode == null || barcode.isBlank()) {
            throw new InvalidWasteRequestException("BEXA003", "barcode is required");
        }
        return detailsRepository.findByBioMedicalWasteIdAndBarcodeNo(requestId, barcode)
                .map(BioMedicalWasteDetails::getWeight)
                .orElseThrow(() -> new WasteNotFoundException("NOT_FOUND", "Barcode not found for this request"));
    }

    // --------------------------------------------------------------- helpers

    /** Requisition number rule: {unitPrefix}/BW/{year-2000}/{%05d seq}. */
    private String generateRequisitionNo(String unitPrefix) {
        long seq = nextId("bio_medical_waste_req_no_seq");
        int yy = LocalDate.now().getYear() - 2000;
        String prefix = (unitPrefix == null || unitPrefix.isBlank()) ? DEFAULT_UNIT_PREFIX : unitPrefix.trim();
        return prefix + "/BW/" + yy + "/" + String.format("%05d", seq);
    }

    /**
     * Atomic monthly barcode counter (legacy Redis key bmw_{yyyy}_{MM}_{unitId}).
     * Moved into the DB transaction: allocation rolls back with the request.
     */
    private long nextBarcodeCounter(String monthKey, Long unitId) {
        String key = "bmw_" + monthKey + "_" + (unitId == null ? 0 : unitId);
        return ((Number) em.createNativeQuery("""
                INSERT INTO ihms_waste.barcode_counter (counter_key, counter_value)
                VALUES (:key, 1)
                ON CONFLICT (counter_key)
                DO UPDATE SET counter_value = ihms_waste.barcode_counter.counter_value + 1
                RETURNING counter_value
                """)
                .setParameter("key", key)
                .getSingleResult()).longValue();
    }

    private void requireStatus(BioMedicalWaste request, long expected, String action) {
        Long current = request.getRequestStatus();
        if (current == null || current != expected) {
            throw new InvalidWasteRequestException("BEXA003",
                    "Cannot " + action + ": request status is " + current + ", expected " + expected);
        }
    }

    private <T> T bind(JsonNode payload, Class<T> type) {
        if (payload == null || payload.isNull() || payload.isMissingNode()) {
            throw new InvalidWasteRequestException("BEXA003", "Invalid input data !");
        }
        try {
            return objectMapper.treeToValue(payload, type);
        } catch (Exception e) {
            throw new InvalidWasteRequestException("BEXA003", "Invalid input data !");
        }
    }

    private void stampCreate(com.hims.waste.domain.model.WasteBase entity, Long unitId) {
        entity.setAddedBy(AUDIT_ACTOR_ID);
        entity.setAddedDatetime(Timestamp.from(Instant.now()));
        entity.setUpdatedBy(null);
        entity.setUpdatedDatetime(null);
        entity.setSynchronizedd(false);
        if (unitId != null) {
            entity.setUnitId(unitId);
        }
    }

    private void stampUpdate(com.hims.waste.domain.model.WasteBase entity) {
        entity.setUpdatedBy(AUDIT_ACTOR_ID);
        entity.setUpdatedDatetime(Timestamp.from(Instant.now()));
    }

    private long nextId(String sequenceName) {
        return ((Number) em.createNativeQuery("SELECT nextval('ihms_waste.\"" + sequenceName + "\"')")
                .getSingleResult()).longValue();
    }
}
