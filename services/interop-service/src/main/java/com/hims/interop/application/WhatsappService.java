package com.hims.interop.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hims.interop.application.exceptions.InteropNotFoundException;
import com.hims.interop.application.exceptions.InvalidRequestException;
import com.hims.interop.domain.model.WhatsappMessageQueue;
import com.hims.interop.domain.model.WhatsappMessageStatus;
import com.hims.interop.domain.repository.WhatsappMessageQueueRepository;
import com.hims.interop.domain.repository.WhatsappMessageStatusRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Map;

/**
 * WhatsApp notification queue (register WP-T1). FORWARD-ONLY status machine,
 * pinned by unit + integration tests:
 *
 *   0=QUEUED -&gt; 1=SENT -&gt; 2=DELIVERED -&gt; 3=READ   (strictly one step forward)
 *   4=FAILED allowed ONLY from 0 or 1
 *   everything else (backward, skip-ahead, FAILED after DELIVERED/READ) -&gt; 400
 *   INTEROP_ILLEGAL_TRANSITION.
 *
 * Every ACCEPTED transition appends a t_whatsapp_message_status history row.
 * OUT OF SCOPE (comment-only): the provider send job (legacy write-behind queue
 * drained every 15 min, window 14:00-07:00, max trigger count 5 — 07 §WhatsApp)
 * and webhook receiver; PUT .../status is the webhook emulation surface.
 */
@Service
public class WhatsappService {

    private static final Logger log = LoggerFactory.getLogger(WhatsappService.class);

    public static final long AUDIT_ACTOR_ID = 1L;   // MIG-004: principal id

    private final WhatsappMessageQueueRepository messageRepository;
    private final WhatsappMessageStatusRepository statusRepository;
    private final ObjectMapper objectMapper;

    @PersistenceContext
    private EntityManager em;

    public WhatsappService(WhatsappMessageQueueRepository messageRepository,
                           WhatsappMessageStatusRepository statusRepository,
                           ObjectMapper objectMapper) {
        this.messageRepository = messageRepository;
        this.statusRepository = statusRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Enqueue a notification {template, recipient, payloadVars}. Guards:
     * recipient required (&lt;= 20 chars); template optional (&lt;= 60 chars).
     * New rows start QUEUED (0). Emits NOTHING to the outbox.
     */
    @Transactional
    public WhatsappMessageQueue create(String template, String recipient, Map<String, Object> payloadVars) {
        if (recipient == null || recipient.isBlank()) {
            throw new InvalidRequestException("BEXA003", "recipient is required");
        }
        if (recipient.length() > 20) {
            throw new InvalidRequestException("BEXA003",
                    "recipient must be at most 20 characters");
        }
        if (template != null && template.length() > 60) {
            throw new InvalidRequestException("BEXA003",
                    "template must be at most 60 characters");
        }

        Timestamp nowTs = now();
        WhatsappMessageQueue msg = new WhatsappMessageQueue();
        msg.setId(nextId("t_whatsapp_message_queue_seq"));
        msg.setTemplate(template);
        msg.setRecipient(recipient);
        msg.setPayloadVars(toJson(payloadVars));
        msg.setStatus(WhatsappMessageQueue.STATUS_QUEUED);
        msg.setAddedBy(AUDIT_ACTOR_ID);
        msg.setAddedDatetime(nowTs);
        messageRepository.save(msg);
        log.info("Queued WhatsApp message {} to {}", msg.getId(), recipient);
        return msg;
    }

    /**
     * Apply a provider status update {statusCode, providerMessageId?} — webhook
     * emulation. Guards: unknown id -&gt; 404; statusCode must be 1..4 (0 is the
     * initial state, never a transition TARGET) else 400 BEXA003; machine rules
     * above else 400 INTEROP_ILLEGAL_TRANSITION. On success the row moves to the
     * new status, providerMessageId (if supplied) is stamped, and a history row
     * is appended. Emits NOTHING to the outbox.
     */
    @Transactional
    public WhatsappMessageQueue applyStatus(long messageId, Short statusCode, String providerMessageId,
                                            String detail) {
        WhatsappMessageQueue msg = messageRepository.findById(messageId)
                .orElseThrow(() -> new InteropNotFoundException("NOT_FOUND",
                        "WhatsApp message not found: " + messageId));
        short target = parseTarget(statusCode);

        Short nextForwardStep = WhatsappMessageQueue.nextForward(msg.getStatus());
        boolean legal = target == WhatsappMessageQueue.STATUS_FAILED
                ? msg.canFail()
                : (nextForwardStep != null && target == nextForwardStep);
        if (!legal) {
            throw new InvalidRequestException("INTEROP_ILLEGAL_TRANSITION",
                    "Illegal WhatsApp transition " + msg.getStatus() + " -> " + target
                            + " — only forward 0->1->2->3, and FAILED (4) only from 0/1");
        }

        Timestamp nowTs = now();
        short previous = msg.getStatus();
        msg.setStatus(target);
        if (providerMessageId != null && !providerMessageId.isBlank()) {
            msg.setProviderMessageId(providerMessageId);
        }
        msg.setUpdatedBy(AUDIT_ACTOR_ID);
        msg.setUpdatedDatetime(nowTs);
        messageRepository.save(msg);

        WhatsappMessageStatus hist = new WhatsappMessageStatus();
        hist.setId(nextId("t_whatsapp_message_status_seq"));
        hist.setMessageId(messageId);
        hist.setStatusCode(target);
        hist.setTs(nowTs);
        hist.setDetail(detail == null ? ("transition " + previous + " -> " + target) : detail);
        hist.setAddedBy(AUDIT_ACTOR_ID);
        hist.setAddedDatetime(nowTs);
        statusRepository.save(hist);

        log.info("WhatsApp message {}: {} -> {}{}", messageId, previous, target,
                providerMessageId == null ? "" : (" (provider " + providerMessageId + ")"));
        return msg;
    }

    // ------------------------------------------------------------------ queries

    @Transactional(readOnly = true)
    public WhatsappMessageQueue get(long messageId) {
        WhatsappMessageQueue msg = load(messageId);
        msg.setHistory(statusRepository.findByMessageIdOrderByTsAsc(messageId));
        return msg;
    }

    @Transactional(readOnly = true)
    public java.util.List<WhatsappMessageQueue> list(Short status) {
        return status == null
                ? messageRepository.findAllByOrderByIdDesc()
                : messageRepository.findByStatusOrderByIdDesc(status);
    }

    // ------------------------------------------------------------------ helpers

    /** Transition targets are 1..4 only — 0 (QUEUED) is never a transition target. */
    public static short parseTarget(Short raw) {
        if (raw == null || raw < WhatsappMessageQueue.STATUS_SENT || raw > WhatsappMessageQueue.STATUS_FAILED) {
            throw new InvalidRequestException("BEXA003",
                    "statusCode must be one of 1=SENT, 2=DELIVERED, 3=READ, 4=FAILED");
        }
        return raw;
    }

    public static String statusName(short s) {
        return switch (s) {
            case WhatsappMessageQueue.STATUS_QUEUED -> "QUEUED";
            case WhatsappMessageQueue.STATUS_SENT -> "SENT";
            case WhatsappMessageQueue.STATUS_DELIVERED -> "DELIVERED";
            case WhatsappMessageQueue.STATUS_READ -> "READ";
            case WhatsappMessageQueue.STATUS_FAILED -> "FAILED";
            default -> String.valueOf(s);
        };
    }

    private WhatsappMessageQueue load(long messageId) {
        return messageRepository.findById(messageId)
                .orElseThrow(() -> new InteropNotFoundException("NOT_FOUND",
                        "WhatsApp message not found: " + messageId));
    }

    private Long nextId(String sequenceName) {
        Number n = (Number) em.createNativeQuery("SELECT nextval('ihms_interop." + sequenceName + "')")
                .getSingleResult();
        return n.longValue();
    }

    private String toJson(Map<String, Object> payloadVars) {
        if (payloadVars == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(payloadVars);
        } catch (JsonProcessingException e) {
            throw new InvalidRequestException("BEXA003", "payloadVars must be a JSON object");
        }
    }

    private static Timestamp now() {
        return Timestamp.from(Instant.now());
    }
}
