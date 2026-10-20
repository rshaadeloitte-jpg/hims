package com.hims.interop.application;

import com.hims.interop.application.exceptions.InvalidRequestException;
import com.hims.interop.domain.model.InboundHis;
import com.hims.interop.domain.repository.InboundHisRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Set;

/**
 * Inbound HL7 v2 ingress (register WP-T1). Validates messageType against the
 * allowed ADT/ORM set else 400 INTEROP_INVALID_MESSAGE_TYPE; rows land
 * 0=RECEIVED with processed_at NULL.
 *
 * OUT OF SCOPE (comment-only): MLLP listener transport, HL7 v2 segment parsing,
 * routing into clinical services and the PROCESSED/REJECTED dispatcher.
 */
@Service
public class InboundService {

    private static final Logger log = LoggerFactory.getLogger(InboundService.class);

    public static final long AUDIT_ACTOR_ID = 1L;   // MIG-004: principal id

    /**
     * Allowed inbound HL7 v2 message types. ADT^A01/A02/A03/A04/A08/A45 cover the
     * register examples ("ADT^A01/ADT^A45/ORM etc") plus standard admit/discharge/
     * update/cancel events; ORM^O01 (orders) and ORU^R01 (results) complete the
     * ADT/ORM set. Exact legacy type inventory is a migration-cutover concern
     * (disclosed in the WP report).
     */
    public static final Set<String> ALLOWED_MESSAGE_TYPES = Set.of(
            "ADT^A01", "ADT^A02", "ADT^A03", "ADT^A04", "ADT^A08", "ADT^A45",
            "ORM^O01", "ORU^R01");

    private final InboundHisRepository repository;

    @PersistenceContext
    private EntityManager em;

    public InboundService(InboundHisRepository repository) {
        this.repository = repository;
    }

    /**
     * Receive an inbound HL7 message {messageType, sender, rawPayload}. Guards:
     * messageType in ALLOWED_MESSAGE_TYPES else 400 INTEROP_INVALID_MESSAGE_TYPE;
     * sender and rawPayload required (BEXA003). Emits NOTHING to the outbox.
     */
    @Transactional
    public InboundHis receive(String messageType, String sender, String rawPayload) {
        if (messageType == null || !ALLOWED_MESSAGE_TYPES.contains(messageType)) {
            throw new InvalidRequestException("INTEROP_INVALID_MESSAGE_TYPE",
                    "Unsupported HL7 messageType: " + messageType + " — allowed: " + ALLOWED_MESSAGE_TYPES);
        }
        if (rawPayload == null || rawPayload.isBlank()) {
            throw new InvalidRequestException("BEXA003", "rawPayload is required");
        }
        if (sender != null && sender.length() > 40) {
            throw new InvalidRequestException("BEXA003", "sender must be at most 40 characters");
        }

        InboundHis row = new InboundHis();
        row.setId(nextId("t_inbound_his_seq"));
        row.setMessageType(messageType);
        row.setSender(sender);
        row.setRawPayload(rawPayload);
        row.setStatus(InboundHis.STATUS_RECEIVED);
        row.setAddedBy(AUDIT_ACTOR_ID);
        row.setAddedDatetime(now());
        repository.save(row);
        log.info("Received inbound HL7 {} from {} as row {}", messageType, sender, row.getId());
        return row;
    }

    @Transactional(readOnly = true)
    public java.util.List<InboundHis> list() {
        return repository.findAllByOrderByIdDesc();
    }

    private Long nextId(String sequenceName) {
        Number n = (Number) em.createNativeQuery("SELECT nextval('ihms_interop." + sequenceName + "')")
                .getSingleResult();
        return n.longValue();
    }

    private static Timestamp now() {
        return Timestamp.from(Instant.now());
    }
}
