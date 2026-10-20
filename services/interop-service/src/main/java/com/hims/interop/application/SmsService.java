package com.hims.interop.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hims.interop.application.exceptions.InteropNotFoundException;
import com.hims.interop.application.exceptions.InvalidRequestException;
import com.hims.interop.domain.model.SmsNotificationQueue;
import com.hims.interop.domain.model.SmsNotificationStatus;
import com.hims.interop.domain.repository.SmsNotificationQueueRepository;
import com.hims.interop.domain.repository.SmsNotificationStatusRepository;
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
 * SMS notification queue (MIG-055, eSanchar gateway). Same forward-only status
 * machine as the WhatsApp queue (parity with 07 §3.1 #15):
 *
 *   0=QUEUED 1=SENT 2=DELIVERED                (strictly one step forward)
 *   3=DELIVERY_FAILED legal ONLY from 0 or 1
 *   4=EXPIRED legal from 0, 1, 2 (final delivery failure beyond retry budget)
 *   everything else (backward, skip-ahead, DELIVERY_FAILED from DELIVERED,
 *                    EXPIRED from DELIVERY_FAILED) -&gt; 400 INTEROP_ILLEGAL_TRANSITION
 *
 * Every ACCEPTED transition appends a t_sms_notification_status history row.
 * The provider send job (send.sms.url / send.sms.url.otp) is OUT OF SCOPE
 * (comment-only per register WP-T1); PUT .../status is the webhook emulation.
 */
@Service
public class SmsService {

    private static final Logger log = LoggerFactory.getLogger(SmsService.class);

    public static final long AUDIT_ACTOR_ID = 1L;

    private final SmsNotificationQueueRepository messageRepository;
    private final SmsNotificationStatusRepository statusRepository;
    private final ObjectMapper objectMapper;

    @PersistenceContext
    private EntityManager em;

    public SmsService(SmsNotificationQueueRepository messageRepository,
                      SmsNotificationStatusRepository statusRepository,
                      ObjectMapper objectMapper) {
        this.messageRepository = messageRepository;
        this.statusRepository = statusRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Enqueue a notification {template, recipient, payloadVars, gateway?}. Guards:
     * recipient required (&lt;= 20 chars); template optional (&lt;= 60 chars); gateway
     * defaults to "eSanchar". New rows start QUEUED (0). Emits NOTHING.
     */
    @Transactional
    public SmsNotificationQueue create(String template, String recipient, String gateway,
                                      Map<String, Object> payloadVars) {
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
        if (gateway == null || gateway.isBlank()) {
            gateway = SmsNotificationQueue.GATEWAY_ESANCHAR;
        }

        Timestamp nowTs = now();
        SmsNotificationQueue msg = new SmsNotificationQueue();
        msg.setId(nextId("t_sms_notification_queue_seq"));
        msg.setTemplate(template);
        msg.setRecipient(recipient);
        msg.setGateway(gateway);
        msg.setPayloadVars(toJson(payloadVars));
        msg.setStatus(SmsNotificationQueue.STATUS_QUEUED);
        msg.setAddedBy(AUDIT_ACTOR_ID);
        msg.setAddedDatetime(nowTs);
        messageRepository.save(msg);
        log.info("Queued SMS message {} to {} via {}", msg.getId(), recipient, gateway);
        return msg;
    }

    /**
     * Apply a provider status update {statusCode, providerMessageId?, detail?} —
     * webhook emulation. Same forward-only machine as the WhatsApp queue but with
     * an extra terminal EXPIRED (4) that can land from QUEUED/SENT/DELIVERED.
     * DELIVERY_FAILED (3) is the partial-failure path and is legal ONLY from
     * QUEUED or SENT. Emits NOTHING to the outbox.
     */
    @Transactional
    public SmsNotificationQueue applyStatus(long messageId, Short statusCode, String providerMessageId,
                                           String detail) {
        SmsNotificationQueue msg = messageRepository.findById(messageId)
                .orElseThrow(() -> new InteropNotFoundException("NOT_FOUND",
                        "SMS message not found: " + messageId));
        short target = parseTarget(statusCode);

        boolean legal;
        if (target == SmsNotificationQueue.STATUS_DELIVERY_FAILED) {
            legal = msg.canFail();
        } else if (target == SmsNotificationQueue.STATUS_EXPIRED) {
            // EXPIRED is the terminal outcome: legal from any non-terminal state
            // (0/1/2). NOT legal from DELIVERY_FAILED (3) — that is itself a
            // terminal state and the row must NOT rewind/forward from there.
            legal = msg.getStatus() == SmsNotificationQueue.STATUS_QUEUED
                    || msg.getStatus() == SmsNotificationQueue.STATUS_SENT
                    || msg.getStatus() == SmsNotificationQueue.STATUS_DELIVERED;
        } else {
            Short nextForward = SmsNotificationQueue.nextForward(msg.getStatus());
            legal = nextForward != null && target == nextForward;
        }
        if (!legal) {
            throw new InvalidRequestException("INTEROP_ILLEGAL_TRANSITION",
                    "Illegal SMS transition " + msg.getStatus() + " -> " + target);
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

        SmsNotificationStatus hist = new SmsNotificationStatus();
        hist.setId(nextId("t_sms_notification_status_seq"));
        hist.setMessageId(messageId);
        hist.setStatusCode(target);
        hist.setTs(nowTs);
        hist.setDetail(detail == null ? ("transition " + previous + " -> " + target) : detail);
        hist.setAddedBy(AUDIT_ACTOR_ID);
        hist.setAddedDatetime(nowTs);
        statusRepository.save(hist);

        log.info("SMS message {}: {} -> {}{}", messageId, previous, target,
                providerMessageId == null ? "" : (" (provider " + providerMessageId + ")"));
        return msg;
    }

    // ------------------------------------------------------------------ queries

    @Transactional(readOnly = true)
    public SmsNotificationQueue get(long messageId) {
        SmsNotificationQueue msg = load(messageId);
        msg.setHistory(statusRepository.findByMessageIdOrderByTsAsc(messageId));
        return msg;
    }

    @Transactional(readOnly = true)
    public java.util.List<SmsNotificationQueue> list(Short status) {
        return status == null
                ? messageRepository.findAllByOrderByIdDesc()
                : messageRepository.findByStatusOrderByIdDesc(status);
    }

    // ------------------------------------------------------------------ helpers

    /** Transition targets are 1..4 only (0=QUEUED is the initial state, never a target). */
    public static short parseTarget(Short raw) {
        if (raw == null || raw < SmsNotificationQueue.STATUS_SENT || raw > SmsNotificationQueue.STATUS_EXPIRED) {
            throw new InvalidRequestException("BEXA003",
                    "statusCode must be one of 1=SENT, 2=DELIVERED, 3=DELIVERY_FAILED, 4=EXPIRED");
        }
        return raw;
    }

    public static String statusName(short s) {
        return switch (s) {
            case SmsNotificationQueue.STATUS_QUEUED -> "QUEUED";
            case SmsNotificationQueue.STATUS_SENT -> "SENT";
            case SmsNotificationQueue.STATUS_DELIVERED -> "DELIVERED";
            case SmsNotificationQueue.STATUS_DELIVERY_FAILED -> "DELIVERY_FAILED";
            case SmsNotificationQueue.STATUS_EXPIRED -> "EXPIRED";
            default -> String.valueOf(s);
        };
    }

    private SmsNotificationQueue load(long messageId) {
        return messageRepository.findById(messageId)
                .orElseThrow(() -> new InteropNotFoundException("NOT_FOUND",
                        "SMS message not found: " + messageId));
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

    private static Timestamp now() { return Timestamp.from(Instant.now()); }
}
