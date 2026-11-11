package com.hims.item.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hims.item.application.exceptions.InvalidItemRequestException;
import com.hims.item.application.exceptions.ItemNotFoundException;
import com.hims.item.domain.model.Indent;
import com.hims.item.domain.model.IndentDetail;
import com.hims.item.domain.repository.IndentDetailRepository;
import com.hims.item.domain.repository.IndentRepository;
import com.hims.item.domain.repository.ItemRepository;
import com.hims.item.domain.repository.StoreRepository;
import com.hims.item.infrastructure.outbox.OutboxService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Indent create/approve/list (WP-2 procurement slice). Approval flips
 * doc_status PENDING -> APPROVED in one tx and appends an {@code IndentApproved}
 * event to the transactional outbox.
 */
@Service
public class IndentService {

    private static final Logger log = LoggerFactory.getLogger(IndentService.class);

    public static final String EVENT_TYPE_INDENT_APPROVED = "IndentApproved";

    /** Indent create request DTO (Jackson-bound). */
    public record IndentRequest(Long storeId, Long departmentId, String indentNo, String remarks,
                                List<IndentLine> lines) { }

    public record IndentLine(Long itemId, BigDecimal quantity, String remarks) { }

    private final IndentRepository indentRepository;
    private final IndentDetailRepository indentDetailRepository;
    private final ItemRepository itemRepository;
    private final StoreRepository storeRepository;
    private final OutboxService outboxService;
    private final ObjectMapper objectMapper;

    public IndentService(IndentRepository indentRepository,
                         IndentDetailRepository indentDetailRepository,
                         ItemRepository itemRepository,
                         StoreRepository storeRepository,
                         OutboxService outboxService,
                         ObjectMapper objectMapper) {
        this.indentRepository = indentRepository;
        this.indentDetailRepository = indentDetailRepository;
        this.itemRepository = itemRepository;
        this.storeRepository = storeRepository;
        this.outboxService = outboxService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public Indent create(JsonNode body) {
        IndentRequest req = bind(body);
        if (req.storeId() == null || !storeRepository.existsById(req.storeId())) {
            throw new ItemNotFoundException("NOT_FOUND", "Store not found");
        }
        if (req.lines() == null || req.lines().isEmpty()) {
            throw new InvalidItemRequestException("BEXA003", "At least one indent line is required");
        }

        long id = indentRepository.nextIndentId();
        Indent indent = new Indent();
        indent.setId(id);
        indent.setIndentNo(req.indentNo() == null || req.indentNo().isBlank() ? "IND-" + id : req.indentNo());
        indent.setStoreId(req.storeId());
        indent.setDepartmentId(req.departmentId());
        indent.setRemarks(req.remarks());
        indent.setDocStatus(Indent.STATUS_PENDING);
        indent.setAddedBy(StockService.AUDIT_ACTOR_ID);
        indent.setAddedDatetime(Timestamp.from(Instant.now()));
        indent.setSynchronizedd(false);
        indentRepository.save(indent);

        for (IndentLine line : req.lines()) {
            if (line.itemId() == null || !itemRepository.existsById(line.itemId())) {
                throw new ItemNotFoundException("NOT_FOUND", "Item not found");
            }
            if (line.quantity() == null || line.quantity().signum() <= 0) {
                throw new InvalidItemRequestException("BEXA003", "Invalid input data !");
            }
            IndentDetail detail = new IndentDetail();
            detail.setId(indentDetailRepository.nextIndentDetailId());
            detail.setIndentId(indent.getId());
            detail.setItemId(line.itemId());
            detail.setQuantity(line.quantity());
            detail.setRemarks(line.remarks());
            detail.setAddedBy(StockService.AUDIT_ACTOR_ID);
            detail.setAddedDatetime(Timestamp.from(Instant.now()));
            detail.setSynchronizedd(false);
            indentDetailRepository.save(detail);
        }

        log.info("Indent created id={} no={} store={} lines={}", indent.getId(), indent.getIndentNo(),
                indent.getStoreId(), req.lines().size());
        return indent;
    }

    /**
     * Approve a pending indent: doc_status 1 -> 2, audit stamped, single
     * {@code IndentApproved} outbox event appended in the SAME tx.
     */
    @Transactional
    public Indent approve(Long id) {
        Indent indent = indentRepository.findById(id)
                .orElseThrow(() -> new ItemNotFoundException("NOT_FOUND", "Record not found"));
        if (indent.getDocStatus() != Indent.STATUS_PENDING) {
            throw new InvalidItemRequestException("BEXA003", "Only a PENDING indent can be approved");
        }
        indent.setDocStatus(Indent.STATUS_APPROVED);
        indent.setApprovedBy(StockService.AUDIT_ACTOR_ID);
        indent.setApprovedDatetime(Timestamp.from(Instant.now()));
        indent.setUpdatedBy(StockService.AUDIT_ACTOR_ID);
        indent.setUpdatedDatetime(Timestamp.from(Instant.now()));
        Indent saved = indentRepository.save(indent);
        emitIndentApproved(saved);
        log.info("Indent approved id={} no={}", saved.getId(), saved.getIndentNo());
        return saved;
    }

    @Transactional(readOnly = true)
    public List<Indent> list(Short status) {
        if (status != null) {
            return indentRepository.findByDocStatus(status);
        }
        return indentRepository.findAllByOrderByIdDesc();
    }

    @Transactional(readOnly = true)
    public Indent get(Long id) {
        return indentRepository.findById(id)
                .orElseThrow(() -> new ItemNotFoundException("NOT_FOUND", "Record not found"));
    }

    @Transactional(readOnly = true)
    public List<IndentDetail> getDetails(Long indentId) {
        return indentDetailRepository.findByIndentId(indentId);
    }

    private void emitIndentApproved(Indent indent) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("indentId", indent.getId());
        payload.put("indentNo", indent.getIndentNo());
        payload.put("storeId", indent.getStoreId());
        payload.put("departmentId", indent.getDepartmentId());
        payload.put("docStatus", indent.getDocStatus());
        try {
            outboxService.append(EVENT_TYPE_INDENT_APPROVED, "Indent", indent.getId(),
                    String.valueOf(indent.getStoreId()), objectMapper.writeValueAsString(payload),
                    EVENT_TYPE_INDENT_APPROVED + ":" + indent.getId());
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize IndentApproved payload", e);
        }
    }

    private IndentRequest bind(JsonNode body) {
        if (body == null || body.isNull() || body.isMissingNode()) {
            throw new InvalidItemRequestException("BEXA003", "Invalid input data !");
        }
        try {
            return objectMapper.treeToValue(body, IndentRequest.class);
        } catch (Exception e) {
            throw new InvalidItemRequestException("BEXA003", "Invalid input data !");
        }
    }

}
