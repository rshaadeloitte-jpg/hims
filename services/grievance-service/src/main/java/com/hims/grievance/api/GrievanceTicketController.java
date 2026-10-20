package com.hims.grievance.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.hims.grievance.application.GrievanceTicketService;
import com.hims.grievance.domain.model.GrievanceComment;
import com.hims.grievance.domain.model.GrievanceFeedback;
import com.hims.grievance.domain.model.GrievanceTicket;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Grievance ticket lifecycle API (MIG-020) — clean target of the legacy
 * mdm grievance endpoints (raise/comment/status/severity/assign/reopen/feedback/counts).
 * REST-only: GrievanceRaised rejected phase 1 (KAFKA-EVENT-CATALOG line 206).
 */
@RestController
@RequestMapping("/api/v1/grievances")
@Tag(name = "Grievance Tickets (MIG-020)",
        description = "Ticket lifecycle: raise, comment, status, severity, assign, reopen, feedback, dashboard counts")
public class GrievanceTicketController {

    private final GrievanceTicketService service;

    public GrievanceTicketController(GrievanceTicketService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "Raise a grievance ticket",
            description = "Server stamps issueDate + ticketNo (IHMS/G/yyyy/%05d) + hospitalTicket=IHMSSUPPORT; "
                    + "issueStatus starts at 1=NEW. isCitizen=true marks citizen-raised tickets.")
    public ResponseEntity<GrievanceTicket> raise(@RequestBody(required = false) JsonNode body,
                                                 @RequestParam(required = false) Long unitId,
                                                 @RequestParam(required = false, defaultValue = "false") boolean isCitizen) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.raiseTicket(body, unitId, isCitizen));
    }

    @GetMapping
    @Operation(summary = "List grievance tickets",
            description = "Optional scope: mobileNo (citizen view) or unitId (hospital view); default all active tickets, newest first.")
    public List<GrievanceTicket> list(@RequestParam(required = false) Long unitId,
                                      @RequestParam(required = false) Long mobileNo) {
        return service.listTickets(unitId, mobileNo);
    }

    @GetMapping("/counts")
    @Operation(summary = "Dashboard ticket counts",
            description = "TOTAL / NEW / ASSIGNED / RESOLVED / RAISED-BY-CITIZEN / MY-LIST (issueStatus NOT IN 3,4 for employeeId) / TODAYS-TICKET.")
    public Map<String, Long> counts(@RequestParam(required = false) Long employeeId) {
        return service.getTicketCounts(employeeId);
    }

    @GetMapping("/{id}")
    public GrievanceTicket get(@PathVariable long id) {
        return service.getTicket(id);
    }

    @PostMapping("/{id}/comments")
    @Operation(summary = "Add a comment to a ticket")
    public ResponseEntity<GrievanceComment> addComment(@PathVariable long id,
                                                       @RequestBody(required = false) JsonNode body) {
        String comment = (body == null || body.path("comment").isMissingNode() || body.path("comment").isNull())
                ? null : body.path("comment").asText();
        return ResponseEntity.status(HttpStatus.CREATED).body(service.addComment(id, comment));
    }

    @GetMapping("/{id}/comments")
    public List<GrievanceComment> comments(@PathVariable long id) {
        return service.getComments(id);
    }

    @PostMapping("/{id}/status")
    @Operation(summary = "Change ticket status",
            description = "issueStatus: 1=NEW 2=ASSIGNED 3=RESOLVED 4=ASSIGN_TO_CREATOR. "
                    + "RESOLVED sets ticketCloseStatus=2(COMPLETED)+resolvedDate; assignToCreator=true sets "
                    + "ticketCloseStatus=3(CLOSED)+resolvedDate (legacy changeGrievanceTicketStatus).")
    public GrievanceTicket changeStatus(@PathVariable long id,
                                        @RequestParam Short issueStatus,
                                        @RequestParam(required = false, defaultValue = "false") boolean assignToCreator) {
        return service.changeStatus(id, issueStatus, assignToCreator);
    }

    @PostMapping("/{id}/severity")
    @Operation(summary = "Change ticket severity")
    public GrievanceTicket changeSeverity(@PathVariable long id, @RequestParam Long severityId) {
        return service.changeSeverity(id, severityId);
    }

    @PostMapping("/{id}/assign")
    @Operation(summary = "Assign ticket to a user",
            description = "Sets assignedUserId + expectedResolutionDate + issueType + hospitalTicket. "
                    + "Does NOT change issueStatus (legacy keeps status change separate).")
    public GrievanceTicket assign(@PathVariable long id,
                                  @RequestParam Long employeeId,
                                  @RequestParam(required = false)
                                  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate expectedDate,
                                  @RequestParam(required = false) String issueType,
                                  @RequestParam(required = false) String hospitalTicket) {
        Timestamp expected = expectedDate == null ? null
                : Timestamp.valueOf(expectedDate.atStartOfDay());
        return service.assignTicket(id, employeeId, expected, issueType, hospitalTicket);
    }

    @PostMapping("/{id}/reopen")
    @Operation(summary = "Reopen a resolved ticket",
            description = "isRaiseTicket=true -> issueStatus=2(ASSIGNED)+ticketCloseStatus=1(PENDING); "
                    + "false -> issueStatus=1(NEW). Stamps reopenedBy/reopenedDatetime and appends the comment.")
    public GrievanceTicket reopen(@PathVariable long id,
                                  @RequestBody(required = false) JsonNode body,
                                  @RequestParam(required = false, defaultValue = "false") boolean isRaiseTicket) {
        String comment = (body == null || body.path("comment").isMissingNode() || body.path("comment").isNull())
                ? null : body.path("comment").asText();
        return service.reopenTicket(id, comment, isRaiseTicket);
    }

    @PostMapping("/{id}/feedback")
    @Operation(summary = "Add satisfaction feedback and close the ticket",
            description = "Saves 5-question feedback + free text; sets ticket feedbackId and ticketCloseStatus=3(CLOSED).")
    public ResponseEntity<GrievanceFeedback> feedback(@PathVariable long id,
                                                      @RequestBody(required = false) JsonNode body) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.addFeedback(withGrievanceId(body, id)));
    }

    private static JsonNode withGrievanceId(JsonNode body, long id) {
        if (body != null && body.isObject()) {
            ((com.fasterxml.jackson.databind.node.ObjectNode) body).put("grievanceId", id);
        }
        return body;
    }
}
