package com.hims.grievance.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hims.grievance.application.exceptions.GrievanceNotFoundException;
import com.hims.grievance.application.exceptions.InvalidGrievanceRequestException;
import com.hims.grievance.domain.model.GrievanceComment;
import com.hims.grievance.domain.model.GrievanceFeedback;
import com.hims.grievance.domain.model.GrievanceTicket;
import com.hims.grievance.domain.repository.GrievanceCommentRepository;
import com.hims.grievance.domain.repository.GrievanceFeedbackRepository;
import com.hims.grievance.domain.repository.GrievanceTicketRepository;
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
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Grievance ticket lifecycle (MIG-020) — clean target of the legacy grievance
 * cluster in MdmServiceImpl (addGrievenceTicket / addGrievenceComment /
 * changeGrievanceTicketStatus / changeGrievanceSeverity / assignedTicketToUser /
 * reopenGrievanceTicket / addGrievenceFeedback / getAllGrievanceTicketCount).
 *
 * Status dictionary (legacy grievanceTicketList.html + changeTicketStatus.html):
 *   issue_status: 1=NEW 2=ASSIGNED 3=RESOLVED 4=ASSIGN_TO_CREATOR
 *   ticket_close_status: 1=PENDING 2=COMPLETED 3=CLOSED
 *
 * Ticket number rule (legacy addGrievenceTicket): IHMS/G/{yyyy}/{%05d seq}.
 *
 * REST-only: GrievanceRaised is REJECTED phase 1 (KAFKA-EVENT-CATALOG line 206 —
 * domain-internal, keep REST until a second consumer appears), so no outbox here.
 *
 * Deferred to follow-up slices (cross-domain dependencies):
 *   - SERVICE REQUEST auto-assignment to helpdesk users (needs T_USERDETAILS / auth domain)
 *   - GrievanceSeverityTemplateDetails role->severity routing (needs auth domain)
 *   - SMS/email notification on raise (needs notification domain)
 *   - Document blob storage (MIG-021 MinIO)
 */
@Service
public class GrievanceTicketService {

    private static final Logger log = LoggerFactory.getLogger(GrievanceTicketService.class);

    public static final long AUDIT_ACTOR_ID = 1L;   // MIG-004: principal id
    public static final String TICKET_PREFIX = "IHMS/G/";
    public static final String DEFAULT_HOSPITAL_TICKET = "IHMSSUPPORT";

    public static final short STATUS_NEW = 1;
    public static final short STATUS_ASSIGNED = 2;
    public static final short STATUS_RESOLVED = 3;
    public static final short STATUS_ASSIGN_TO_CREATOR = 4;

    public static final short CLOSE_PENDING = 1;
    public static final short CLOSE_COMPLETED = 2;
    public static final short CLOSE_CLOSED = 3;

    private final GrievanceTicketRepository ticketRepository;
    private final GrievanceCommentRepository commentRepository;
    private final GrievanceFeedbackRepository feedbackRepository;
    private final ObjectMapper objectMapper;

    @PersistenceContext
    private EntityManager em;

    public GrievanceTicketService(GrievanceTicketRepository ticketRepository,
                                  GrievanceCommentRepository commentRepository,
                                  GrievanceFeedbackRepository feedbackRepository,
                                  ObjectMapper objectMapper) {
        this.ticketRepository = ticketRepository;
        this.commentRepository = commentRepository;
        this.feedbackRepository = feedbackRepository;
        this.objectMapper = objectMapper;
    }

    // ------------------------------------------------------------------ raise

    /**
     * Raise a grievance ticket (legacy addGrievenceTicket). Server stamps
     * issueDate + ticketNo (IHMS/G/yyyy/%05d) + hospitalTicket. issueStatus
     * starts at NEW (1). Auto-assignment for SERVICE REQUEST is deferred
     * (needs auth/user domain).
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public GrievanceTicket raiseTicket(JsonNode payload, Long unitId, boolean isCitizen) {
        GrievanceTicket t = bind(payload, GrievanceTicket.class);
        require(t, "name", "issue");
        t.setId(nextId("grievance_ticket_seq"));
        t.setIssueDate(Timestamp.from(Instant.now()));
        t.setTicketNo(generateTicketNo());
        t.setHospitalTicket(DEFAULT_HOSPITAL_TICKET);
        t.setIssueStatus(STATUS_NEW);
        t.setIsCitizen(isCitizen);
        t.setStatus(t.getStatus() == null ? true : t.getStatus());
        t.setSynchronizedd(false);
        t.setAddedBy(AUDIT_ACTOR_ID);
        t.setAddedDatetime(Timestamp.from(Instant.now()));
        if (unitId != null) {
            t.setUnitId(unitId);
        }
        GrievanceTicket saved = ticketRepository.save(t);
        log.info("Grievance ticket raised id={} ticketNo={} citizen={}", saved.getId(), saved.getTicketNo(), isCitizen);
        return saved;
    }

    // ---------------------------------------------------------------- comment

    /** Add a comment to a ticket (legacy addGrievenceComment). */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public GrievanceComment addComment(Long grievanceId, String comment) {
        if (grievanceId == null || comment == null || comment.isBlank()) {
            throw new InvalidGrievanceRequestException("BEXA003", "Invalid input data !");
        }
        GrievanceTicket ticket = ticketRepository.findById(grievanceId)
                .orElseThrow(() -> new GrievanceNotFoundException("NOT_FOUND", "Grievance ticket not found"));
        GrievanceComment c = new GrievanceComment();
        c.setId(nextId("grievance_comment_seq"));
        c.setGrievanceId(ticket.getId());
        c.setComment(comment);
        c.setStatus(true);
        c.setSynchronizedd(false);
        c.setAddedBy(AUDIT_ACTOR_ID);
        c.setAddedDatetime(Timestamp.from(Instant.now()));
        if (ticket.getUnitId() != null) {
            c.setUnitId(ticket.getUnitId());
        }
        return commentRepository.save(c);
    }

    // ----------------------------------------------------------------- status

    /**
     * Change ticket status (legacy changeGrievanceTicketStatus).
     * RESOLVED (3) -> ticketCloseStatus=COMPLETED(2), resolvedDate=now.
     * assignToCreator -> ticketCloseStatus=CLOSED(3), resolvedDate=now.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public GrievanceTicket changeStatus(Long grievanceId, Short issueStatus, boolean assignToCreator) {
        if (issueStatus == null || issueStatus < 1 || issueStatus > 4) {
            throw new InvalidGrievanceRequestException("BEXA003", "Invalid input data !");
        }
        GrievanceTicket t = ticketRepository.findById(grievanceId)
                .orElseThrow(() -> new GrievanceNotFoundException("NOT_FOUND", "Grievance ticket not found"));
        t.setIssueStatus(issueStatus);
        t.setUpdatedBy(AUDIT_ACTOR_ID);
        t.setUpdatedDatetime(Timestamp.from(Instant.now()));
        if (assignToCreator) {
            t.setTicketCloseStatus(CLOSE_CLOSED);
            t.setResolvedDate(Timestamp.from(Instant.now()));
        } else if (issueStatus == STATUS_RESOLVED) {
            t.setTicketCloseStatus(CLOSE_COMPLETED);
            t.setResolvedDate(Timestamp.from(Instant.now()));
        }
        return ticketRepository.save(t);
    }

    // --------------------------------------------------------------- severity

    /** Change ticket severity (legacy changeGrievanceSeverity). */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public GrievanceTicket changeSeverity(Long grievanceId, Long severityId) {
        if (severityId == null) {
            throw new InvalidGrievanceRequestException("BEXA003", "Invalid input data !");
        }
        GrievanceTicket t = ticketRepository.findById(grievanceId)
                .orElseThrow(() -> new GrievanceNotFoundException("NOT_FOUND", "Grievance ticket not found"));
        t.setSeverityId(severityId);
        t.setUpdatedBy(AUDIT_ACTOR_ID);
        t.setUpdatedDatetime(Timestamp.from(Instant.now()));
        return ticketRepository.save(t);
    }

    // ----------------------------------------------------------------- assign

    /**
     * Assign ticket to a user (legacy assignedTicketToUser). Sets assignedUserId,
     * expectedResolutionDate, issueType, hospitalTicket. Does NOT change issueStatus
     * (legacy keeps that a separate changeStatus call).
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public GrievanceTicket assignTicket(Long grievanceId, Long employeeId,
                                        Timestamp expectedResolutionDate,
                                        String issueType, String hospitalTicket) {
        if (employeeId == null) {
            throw new InvalidGrievanceRequestException("BEXA003", "Invalid input data !");
        }
        GrievanceTicket t = ticketRepository.findById(grievanceId)
                .orElseThrow(() -> new GrievanceNotFoundException("NOT_FOUND", "Grievance ticket not found"));
        t.setAssignedUserId(employeeId);
        if (expectedResolutionDate != null) {
            t.setExpectedResolutionDate(expectedResolutionDate);
        }
        if (issueType != null && !issueType.isBlank()) {
            t.setIssueType(issueType);
        }
        if (hospitalTicket != null && !hospitalTicket.isBlank()) {
            t.setHospitalTicket(hospitalTicket);
        }
        t.setUpdatedBy(AUDIT_ACTOR_ID);
        t.setUpdatedDatetime(Timestamp.from(Instant.now()));
        return ticketRepository.save(t);
    }

    // ----------------------------------------------------------------- reopen

    /**
     * Reopen a resolved ticket (legacy reopenGrievanceTicket).
     * isRaiseTicket=true  -> issueStatus=ASSIGNED(2), ticketCloseStatus=PENDING(1).
     * isRaiseTicket=false -> issueStatus=NEW(1).
     * Both stamp reopenedBy + reopenedDatetime and append the comment.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public GrievanceTicket reopenTicket(Long grievanceId, String comment, boolean isRaiseTicket) {
        GrievanceTicket t = ticketRepository.findById(grievanceId)
                .orElseThrow(() -> new GrievanceNotFoundException("NOT_FOUND", "Grievance ticket not found"));
        Timestamp now = Timestamp.from(Instant.now());
        if (isRaiseTicket) {
            t.setIssueStatus(STATUS_ASSIGNED);
            t.setTicketCloseStatus(CLOSE_PENDING);
        } else {
            t.setIssueStatus(STATUS_NEW);
        }
        t.setReopenedBy(AUDIT_ACTOR_ID);
        t.setReopenedDatetime(now);
        t.setUpdatedBy(AUDIT_ACTOR_ID);
        t.setUpdatedDatetime(now);
        GrievanceTicket saved = ticketRepository.save(t);
        if (comment != null && !comment.isBlank()) {
            addComment(grievanceId, comment);
        }
        return saved;
    }

    // --------------------------------------------------------------- feedback

    /**
     * Add satisfaction feedback (legacy addGrievenceFeedback). Saves the 5-question
     * feedback and closes the ticket: feedbackId set, ticketCloseStatus=CLOSED(3).
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public GrievanceFeedback addFeedback(JsonNode payload) {
        GrievanceFeedback f = bind(payload, GrievanceFeedback.class);
        if (f.getGrievanceId() == null) {
            throw new InvalidGrievanceRequestException("BEXA003", "Invalid input data !");
        }
        GrievanceTicket t = ticketRepository.findById(f.getGrievanceId())
                .orElseThrow(() -> new GrievanceNotFoundException("NOT_FOUND", "Grievance ticket not found"));
        f.setId(nextId("grievance_feedback_seq"));
        f.setStatus(true);
        f.setSynchronizedd(false);
        f.setAddedBy(AUDIT_ACTOR_ID);
        f.setAddedDatetime(Timestamp.from(Instant.now()));
        if (t.getUnitId() != null) {
            f.setUnitId(t.getUnitId());
        }
        GrievanceFeedback saved = feedbackRepository.save(f);
        // close the ticket (legacy: FEEDBACKID + TICKETCLOSESTATUS=3)
        t.setFeedbackId(saved.getId());
        t.setTicketCloseStatus(CLOSE_CLOSED);
        t.setUpdatedBy(AUDIT_ACTOR_ID);
        t.setUpdatedDatetime(Timestamp.from(Instant.now()));
        ticketRepository.save(t);
        return saved;
    }

    // ------------------------------------------------------------------ reads

    @Transactional(readOnly = true)
    public GrievanceTicket getTicket(Long id) {
        return ticketRepository.findById(id)
                .orElseThrow(() -> new GrievanceNotFoundException("NOT_FOUND", "Grievance ticket not found"));
    }

    @Transactional(readOnly = true)
    public List<GrievanceTicket> listTickets(Long unitId, Long mobileNo) {
        if (mobileNo != null) {
            return ticketRepository.findByMobileNoAndStatusTrueOrderByIdDesc(mobileNo);
        }
        if (unitId != null) {
            return ticketRepository.findByUnitIdAndStatusTrueOrderByIdDesc(unitId);
        }
        return ticketRepository.findByStatusTrueOrderByIdDesc();
    }

    @Transactional(readOnly = true)
    public List<GrievanceComment> getComments(Long grievanceId) {
        return commentRepository.findByGrievanceIdOrderByIdDesc(grievanceId);
    }

    /**
     * Dashboard ticket counts (legacy getAllGrievanceTicketCount):
     * TOTAL, NEW, ASSIGNED, RESOLVED, RAISED-BY-CITIZEN, MY-LIST, TODAYS-TICKET.
     */
    @Transactional(readOnly = true)
    public Map<String, Long> getTicketCounts(Long employeeId) {
        Map<String, Long> counts = new LinkedHashMap<>();
        counts.put("TOTAL", ticketRepository.countByStatusTrue());
        counts.put("NEW", ticketRepository.countByStatusTrueAndIssueStatus(STATUS_NEW));
        counts.put("ASSIGNED", ticketRepository.countByStatusTrueAndIssueStatus(STATUS_ASSIGNED));
        counts.put("RESOLVED", ticketRepository.countByStatusTrueAndIssueStatus(STATUS_RESOLVED));
        counts.put("RAISED-BY-CITIZEN", ticketRepository.countByStatusTrueAndIsCitizenTrue());
        counts.put("MY-LIST", employeeId == null ? 0L : ticketRepository.countMyList(employeeId));
        LocalDate today = LocalDate.now(ZoneId.systemDefault());
        Timestamp dayStart = Timestamp.from(today.atStartOfDay(ZoneId.systemDefault()).toInstant());
        Timestamp dayEnd = Timestamp.from(today.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant());
        counts.put("TODAYS-TICKET", ticketRepository.countTodays(dayStart, dayEnd));
        return counts;
    }

    // ---------------------------------------------------------------- helpers

    /** Ticket number rule (legacy addGrievenceTicket): IHMS/G/{yyyy}/{%05d seq}. */
    private String generateTicketNo() {
        long seq = nextId("grievance_ticket_no_seq");
        int year = LocalDate.now(ZoneId.systemDefault()).getYear();
        return TICKET_PREFIX + year + "/" + String.format("%05d", seq);
    }

    private <T> T bind(JsonNode payload, Class<T> type) {
        if (payload == null || payload.isNull() || payload.isMissingNode()) {
            throw new InvalidGrievanceRequestException("BEXA003", "Invalid input data !");
        }
        try {
            return objectMapper.treeToValue(payload, type);
        } catch (Exception e) {
            throw new InvalidGrievanceRequestException("BEXA003", "Invalid input data !");
        }
    }

    private void require(GrievanceTicket t, String... fields) {
        for (String f : fields) {
            Object v = fieldValue(t, f);
            if (v == null || (v instanceof String s && s.isBlank())) {
                throw new InvalidGrievanceRequestException("BEXA003", "Invalid input data !");
            }
        }
    }

    private Object fieldValue(GrievanceTicket t, String field) {
        try {
            for (Class<?> c = t.getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
                try {
                    java.lang.reflect.Field f = c.getDeclaredField(field);
                    f.setAccessible(true);
                    return f.get(t);
                } catch (NoSuchFieldException e) {
                    // continue up the hierarchy
                }
            }
        } catch (IllegalAccessException e) {
            log.warn("Cannot read field {} for validation", field);
        }
        return null;
    }

    private long nextId(String sequenceName) {
        return ((Number) em.createNativeQuery("SELECT nextval('ihms_grievance.\"" + sequenceName + "\"')")
                .getSingleResult()).longValue();
    }
}
