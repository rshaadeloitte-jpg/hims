package com.hims.linen.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hims.linen.application.exceptions.InvalidLinenRequestException;
import com.hims.linen.application.exceptions.LinenNotFoundException;
import com.hims.linen.domain.model.LinenBase;
import com.hims.linen.domain.model.LinenRequest;
import com.hims.linen.domain.model.LinenRequestDetails;
import com.hims.linen.domain.model.LinenStatus;
import com.hims.linen.domain.repository.LinenRequestDetailsRepository;
import com.hims.linen.domain.repository.LinenRequestRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Linen &amp; laundry request lifecycle (MIG-023 sub-slice, S16). Clean target
 * of legacy LinenLaundryServiceImpl (T_LINENREQUEST + T_LINENREQUESTDETAILS).
 * REST-only: no linen events exist in KAFKA-EVENT-CATALOG — no outbox until a
 * second consumer appears.
 *
 * <p>Preserved legacy behavior:
 * <ul>
 *   <li>raise stamps requisitionNo {unitPrefix}/LR/{year-2000}/{%05d seq}
 *       (linen.no.template=%s/LR/%d/%s) + requestStatus=1(PENDING) and saves
 *       each detail line with the request id;</li>
 *   <li>7-state lifecycle PENDING(1) ACCEPTED(2) HANDOVERED(3) RECEIVED(4)
 *       WASHED(5) DISPATCHED(6) COMPLETED(7);</li>
 *   <li>receive-main-store stamps receivedQuantity (+ damagedQuantity/reason
 *       when damaged); dispatch stamps dispatchedQuantity and ACCUMULATES
 *       damagedQuantity (new + previous, legacy dispatchLinenRequest);</li>
 *   <li>complete stamps receivedBy + per-line finalQuantity;</li>
 *   <li>soft delete sets status=false + requestStatus=0;</li>
 *   <li>list/counts scope a store as source OR destination
 *       (storeId = store_id OR to_store_id), with optional category
 *       (request_status) filter and requisitionNo search.</li>
 * </ul>
 *
 * <p>Documented improvements (IMP-DEC-034): forward-only lifecycle guards
 * (legacy raw UPDATEs had none — a request could be moved backwards or
 * re-approved), and the detail-update path requires PENDING.
 *
 * <p>Deferred cross-domain integrations (dependency-neutral):
 * <ul>
 *   <li>getItemsForLinenRequest — item/batch availability read from ihms_item
 *       (legacy stored proc CIMS_GETITEMSBYSTOCKFORLINENREQUEST minus
 *       already-committed open-request quantities);</li>
 *   <li>updateItemStockForDamagedQty — stock subtraction for damaged linen on
 *       complete (legacy CIMS_ADDITEMSTOCK_UPDATE, TransactionType 26
 *       LINENREQUEST, OperationType 2);</li>
 *   <li>unitPrefix lookup from M_UNITMASTER (caller-supplied until the
 *       reference-service integration; default IHMS).</li>
 * </ul>
 */
@Service
public class LinenRequestService {

    private static final Logger log = LoggerFactory.getLogger(LinenRequestService.class);

    public static final long AUDIT_ACTOR_ID = 1L;   // MIG-004: principal id
    public static final String DEFAULT_UNIT_PREFIX = "IHMS";

    private final LinenRequestRepository requestRepository;
    private final LinenRequestDetailsRepository detailsRepository;
    private final ObjectMapper objectMapper;

    @PersistenceContext
    private EntityManager em;

    public LinenRequestService(LinenRequestRepository requestRepository,
                               LinenRequestDetailsRepository detailsRepository,
                               ObjectMapper objectMapper) {
        this.requestRepository = requestRepository;
        this.detailsRepository = detailsRepository;
        this.objectMapper = objectMapper;
    }

    // ----------------------------------------------------------------- raise

    /**
     * Raise a linen request (legacy addNewLinenRequest). Payload shape mirrors
     * legacy LinenRequestView: {@code {"request": {storeId, toStoreId, ...},
     * "details": [{itemId, batchId, quantity, remark}, ...]}}.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public LinenRequest raiseRequest(JsonNode payload, Long unitId, String unitPrefix) {
        if (payload == null || payload.isNull() || payload.isMissingNode()
                || payload.path("request").isMissingNode() || payload.path("request").isNull()) {
            throw new InvalidLinenRequestException("BEXA003", "Invalid input data !");
        }
        LinenRequest request = bind(payload.path("request"), LinenRequest.class);
        request.setId(nextId("linen_request_seq"));
        request.setRequisitionNo(generateRequisitionNo(unitPrefix));
        request.setRequestStatus(LinenStatus.PENDING);
        request.setRequestDate(Timestamp.from(Instant.now()));
        request.setStatus(request.getStatus() == null ? true : request.getStatus());
        stampCreate(request, unitId);
        LinenRequest saved = requestRepository.save(request);

        JsonNode details = payload.path("details");
        if (details.isArray()) {
            for (JsonNode node : details) {
                LinenRequestDetails line = bind(node, LinenRequestDetails.class);
                line.setId(nextId("linen_request_details_seq"));
                line.setLinenRequestId(saved.getId());
                line.setQuantity(line.getQuantity() == null ? 0L : line.getQuantity());
                line.setStatus(true);
                stampCreate(line, unitId);
                detailsRepository.save(line);
            }
        }
        log.info("Linen request raised id={} requisitionNo={}", saved.getId(), saved.getRequisitionNo());
        return saved;
    }

    // ------------------------------------------------------------ lifecycle

    /** Approve (legacy approveLinenRequest): PENDING -> ACCEPTED. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public LinenRequest approve(Long id) {
        LinenRequest request = getRequest(id);
        requireStatus(request, LinenStatus.PENDING, "approve");
        request.setRequestStatus(LinenStatus.ACCEPTED);
        stampUpdate(request);
        return requestRepository.save(request);
    }

    /** Hand over (legacy handOverLinenItems): ACCEPTED -> HANDOVERED, handedOverTo stamped. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public LinenRequest handover(Long id, String handedOverTo) {
        if (handedOverTo == null || handedOverTo.isBlank()) {
            throw new InvalidLinenRequestException("BEXA003", "handedOverTo is required");
        }
        LinenRequest request = getRequest(id);
        requireStatus(request, LinenStatus.ACCEPTED, "handover");
        request.setRequestStatus(LinenStatus.HANDOVERED);
        request.setHandedOverTo(handedOverTo);
        stampUpdate(request);
        return requestRepository.save(request);
    }

    /**
     * Receive at main store (legacy receiveLinenRequestMainStore): HANDOVERED ->
     * RECEIVED. Body {@code [{"id": detailId, "receivedQuantity": n,
     * "damagedQuantity": n, "reason": "..."}]}: lines with damagedQuantity stamp
     * receivedQuantity + damagedQuantity + reason; lines without stamp
     * receivedQuantity only.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public LinenRequest receiveMainStore(Long id, JsonNode details) {
        LinenRequest request = getRequest(id);
        requireStatus(request, LinenStatus.HANDOVERED, "receive at main store");
        request.setRequestStatus(LinenStatus.RECEIVED);
        stampUpdate(request);
        applyDetailStamps(id, details, (line, node) -> {
            if (node.hasNonNull("receivedQuantity")) {
                line.setReceivedQuantity(node.get("receivedQuantity").asLong());
            }
            if (node.hasNonNull("damagedQuantity")) {
                line.setDamagedQuantity(node.get("damagedQuantity").asLong());
                if (node.hasNonNull("reason")) {
                    line.setReason(node.get("reason").asText());
                }
            }
        });
        return requestRepository.save(request);
    }

    /** Process/wash (legacy processLinenRequest): RECEIVED -> WASHED. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public LinenRequest process(Long id) {
        LinenRequest request = getRequest(id);
        requireStatus(request, LinenStatus.RECEIVED, "process");
        request.setRequestStatus(LinenStatus.WASHED);
        stampUpdate(request);
        return requestRepository.save(request);
    }

    /**
     * Dispatch (legacy dispatchLinenRequest): WASHED -> DISPATCHED. Body
     * {@code [{"id": detailId, "dispatchedQuantity": n, "damagedQuantity": n,
     * "reason": "..."}]}: dispatchedQuantity stamped; damagedQuantity is
     * ACCUMULATED (new + previous — legacy adds to the receive-stage value).
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public LinenRequest dispatch(Long id, JsonNode details) {
        LinenRequest request = getRequest(id);
        requireStatus(request, LinenStatus.WASHED, "dispatch");
        request.setRequestStatus(LinenStatus.DISPATCHED);
        stampUpdate(request);
        applyDetailStamps(id, details, (line, node) -> {
            if (node.hasNonNull("dispatchedQuantity")) {
                line.setDispatchedQuantity(node.get("dispatchedQuantity").asLong());
            }
            if (node.hasNonNull("damagedQuantity")) {
                long incoming = node.get("damagedQuantity").asLong();
                long previous = line.getDamagedQuantity() == null ? 0L : line.getDamagedQuantity();
                line.setDamagedQuantity(incoming + previous);
                if (node.hasNonNull("reason")) {
                    line.setReason(node.get("reason").asText());
                }
            }
        });
        return requestRepository.save(request);
    }

    /**
     * Complete (legacy receiveLinenRequest): DISPATCHED -> COMPLETED, receivedBy
     * stamped on the request; body {@code [{"id": detailId, "finalQuantity": n}]}
     * stamps per-line finalQuantity. Legacy then subtracts damaged quantity from
     * item stock (updateItemStockForDamagedQty) — DEFERRED until the item-service
     * integration slice (cross-domain stock write).
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public LinenRequest complete(Long id, String receivedBy, JsonNode details) {
        LinenRequest request = getRequest(id);
        requireStatus(request, LinenStatus.DISPATCHED, "complete");
        request.setRequestStatus(LinenStatus.COMPLETED);
        request.setReceivedBy(receivedBy);
        stampUpdate(request);
        applyDetailStamps(id, details, (line, node) -> {
            if (node.hasNonNull("finalQuantity")) {
                line.setFinalQuantity(node.get("finalQuantity").asLong());
            }
        });
        return requestRepository.save(request);
    }

    /** Soft delete (legacy deleteLinenRequest): status=false + requestStatus=0. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public LinenRequest deleteRequest(Long id) {
        LinenRequest request = getRequest(id);
        request.setStatus(false);
        request.setRequestStatus(0L);
        stampUpdate(request);
        return requestRepository.save(request);
    }

    /**
     * Update detail quantity/remark before approval (legacy
     * updateItemsForLinenRequest). Detail ids must belong to the request;
     * request must be PENDING (documented improvement — legacy had no guard).
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public List<LinenRequestDetails> updateDetails(Long id, JsonNode details) {
        LinenRequest request = getRequest(id);
        requireStatus(request, LinenStatus.PENDING, "update details");
        if (details == null || !details.isArray()) {
            throw new InvalidLinenRequestException("BEXA003", "Invalid input data !");
        }
        List<LinenRequestDetails> updated = new ArrayList<>();
        for (JsonNode node : details) {
            LinenRequestDetails line = resolveDetail(id, node);
            if (node.hasNonNull("quantity")) {
                line.setQuantity(node.get("quantity").asLong());
            }
            if (node.hasNonNull("remark")) {
                line.setRemark(node.get("remark").asText());
            }
            stampUpdate(line);
            updated.add(detailsRepository.save(line));
        }
        return updated;
    }

    // ----------------------------------------------------------------- reads

    @Transactional(readOnly = true)
    public LinenRequest getRequest(Long id) {
        return requestRepository.findById(id)
                .orElseThrow(() -> new LinenNotFoundException("NOT_FOUND", "Linen request not found"));
    }

    @Transactional(readOnly = true)
    public List<LinenRequestDetails> getDetails(Long id) {
        getRequest(id); // 404 for unknown request
        return detailsRepository.findByLinenRequestIdOrderByIdAsc(id);
    }

    /**
     * List requests (legacy getCurrentLinenStatus): a store sees requests where
     * it is source OR destination; optional category (request_status) filter,
     * requisitionNo search, and request-date range. Newest first.
     */
    @Transactional(readOnly = true)
    public List<LinenRequest> listRequests(Long unitId, Long storeId, Long requestStatus,
                                           String search, LocalDate fromDate, LocalDate toDate) {
        String trimmed = (search == null || search.isBlank()) ? null : search.trim();
        return requestRepository.search(
                unitId == null ? 0L : unitId, unitId != null,
                storeId == null ? 0L : storeId, storeId != null,
                requestStatus == null ? 0L : requestStatus, requestStatus != null,
                trimmed == null ? "" : trimmed, trimmed != null,
                fromDate == null ? Timestamp.from(Instant.EPOCH) : Timestamp.valueOf(fromDate.atStartOfDay()),
                fromDate != null,
                toDate == null ? Timestamp.from(Instant.EPOCH) : Timestamp.valueOf(toDate.plusDays(1).atStartOfDay()),
                toDate != null);
    }

    /**
     * Dashboard counts (legacy getLinenRequestCount): TOTAL + one bucket per
     * lifecycle state, scoped by unit + store (source or destination) + optional
     * request-date range. Legacy map keys totalCount/pendingCount/.../
     * handOveredCount (sic) map to the clean uppercase contract keys.
     */
    @Transactional(readOnly = true)
    public Map<String, Long> getCounts(Long unitId, Long storeId, LocalDate fromDate, LocalDate toDate) {
        Timestamp from = fromDate == null ? Timestamp.from(Instant.EPOCH)
                : Timestamp.valueOf(fromDate.atStartOfDay());
        Timestamp toExclusive = toDate == null ? Timestamp.from(Instant.EPOCH)
                : Timestamp.valueOf(toDate.plusDays(1).atStartOfDay());
        Map<String, Long> counts = new LinkedHashMap<>();
        counts.put("TOTAL", countScoped(unitId, storeId, from, fromDate != null, toExclusive, toDate != null, null));
        for (Map.Entry<String, Long> state : Map.of(
                "PENDING", LinenStatus.PENDING,
                "ACCEPTED", LinenStatus.ACCEPTED,
                "HANDOVERED", LinenStatus.HANDOVERED,
                "RECEIVED", LinenStatus.RECEIVED,
                "WASHED", LinenStatus.WASHED,
                "DISPATCHED", LinenStatus.DISPATCHED,
                "COMPLETED", LinenStatus.COMPLETED).entrySet()) {
            counts.put(state.getKey(), countScoped(unitId, storeId, from, fromDate != null,
                    toExclusive, toDate != null, state.getValue()));
        }
        return counts;
    }

    // --------------------------------------------------------------- helpers

    private long countScoped(Long unitId, Long storeId, Timestamp from, boolean hasFrom,
                             Timestamp toExclusive, boolean hasTo, Long requestStatus) {
        return requestRepository.countScoped(
                unitId == null ? 0L : unitId, unitId != null,
                storeId == null ? 0L : storeId, storeId != null,
                from, hasFrom, toExclusive, hasTo,
                requestStatus == null ? 0L : requestStatus, requestStatus != null);
    }

    private interface DetailStamper {
        void stamp(LinenRequestDetails line, JsonNode node);
    }

    private void applyDetailStamps(Long requestId, JsonNode details, DetailStamper stamper) {
        if (details == null || !details.isArray()) {
            return; // legacy no-op when the detail list is empty
        }
        for (JsonNode node : details) {
            LinenRequestDetails line = resolveDetail(requestId, node);
            stamper.stamp(line, node);
            stampUpdate(line);
            detailsRepository.save(line);
        }
    }

    private LinenRequestDetails resolveDetail(Long requestId, JsonNode node) {
        long detailId = node.path("id").asLong(-1);
        LinenRequestDetails line = detailsRepository.findById(detailId)
                .orElseThrow(() -> new LinenNotFoundException("NOT_FOUND", "Linen request detail not found"));
        if (!requestId.equals(line.getLinenRequestId())) {
            throw new InvalidLinenRequestException("BEXA003", "Detail does not belong to this request");
        }
        return line;
    }

    /** Requisition number rule: {unitPrefix}/LR/{year-2000}/{%05d seq}. */
    private String generateRequisitionNo(String unitPrefix) {
        long seq = nextId("linen_request_req_no_seq");
        int yy = LocalDate.now().getYear() - 2000;
        String prefix = (unitPrefix == null || unitPrefix.isBlank()) ? DEFAULT_UNIT_PREFIX : unitPrefix.trim();
        return prefix + "/LR/" + yy + "/" + String.format("%05d", seq);
    }

    private void requireStatus(LinenRequest request, long expected, String action) {
        Long current = request.getRequestStatus();
        if (current == null || current != expected) {
            throw new InvalidLinenRequestException("BEXA003",
                    "Cannot " + action + ": request status is " + current + ", expected " + expected);
        }
    }

    private <T> T bind(JsonNode payload, Class<T> type) {
        if (payload == null || payload.isNull() || payload.isMissingNode()) {
            throw new InvalidLinenRequestException("BEXA003", "Invalid input data !");
        }
        try {
            return objectMapper.treeToValue(payload, type);
        } catch (Exception e) {
            throw new InvalidLinenRequestException("BEXA003", "Invalid input data !");
        }
    }

    private void stampCreate(LinenBase entity, Long unitId) {
        entity.setAddedBy(AUDIT_ACTOR_ID);
        entity.setAddedDatetime(Timestamp.from(Instant.now()));
        entity.setUpdatedBy(null);
        entity.setUpdatedDatetime(null);
        entity.setSynchronizedd(false);
        if (unitId != null) {
            entity.setUnitId(unitId);
        }
    }

    private void stampUpdate(LinenBase entity) {
        entity.setUpdatedBy(AUDIT_ACTOR_ID);
        entity.setUpdatedDatetime(Timestamp.from(Instant.now()));
    }

    private long nextId(String sequenceName) {
        return ((Number) em.createNativeQuery("SELECT nextval('ihms_linen.\"" + sequenceName + "\"')")
                .getSingleResult()).longValue();
    }

    /** M19 laundry report: counts of linen requests by status. */
    @Transactional(readOnly = true)
    public java.util.Map<String, Long> laundryReport() {
        java.util.List<LinenRequest> all = requestRepository.findAll();
        java.util.Map<String, Long> counts = new java.util.LinkedHashMap<>();
        for (Long status : LinenStatus.ALL) {
            counts.put(LinenStatus.label(status), all.stream().filter(r -> r.getRequestStatus() != null && r.getRequestStatus().equals(status)).count());
        }
        counts.put("TOTAL", (long) all.size());
        return counts;
    }
}
