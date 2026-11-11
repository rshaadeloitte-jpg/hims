package com.hims.queue.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.hims.queue.application.QueueService;
import com.hims.queue.domain.model.QueueToken;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Queue token lifecycle API (MIG-022 / S29) — clean target of legacy
 * QueueManagmentServiceImpl /queueManagment token ops.
 * REST-only: no queue-produced events in KAFKA-EVENT-CATALOG phase 1.
 */
@RestController
@RequestMapping("/api/v1/queue/tokens")
@Tag(name = "Queue Tokens (MIG-022)",
        description = "Token issuance (daily reset), call-next with emergency priority, transfer, no-show, complete, dashboard")
public class QueueTokenController {

    private final QueueService service;

    public QueueTokenController(QueueService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "Issue a queue token",
            description = "Server stamps queueDate + daily tokenNo (resets each day). "
                    + "priority: 1=NORMAL 2=EMERGENCY. Starts WAITING(1); counter assigned at call-next or transfer.")
    public ResponseEntity<QueueToken> issue(@RequestBody(required = false) JsonNode body,
                                            @RequestParam(required = false) Long unitId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.issue(body, unitId));
    }

    @GetMapping
    @Operation(summary = "List today's tokens", description = "Optional counterId filter; oldest first.")
    public List<QueueToken> list(@RequestParam(required = false) Long counterId,
                                 @RequestParam(required = false) Long unitId) {
        return service.listTokens(counterId);
    }

    @GetMapping("/department/{departmentId}")
    @Operation(summary = "List tokens by department (M27 rad/patho)",
            description = "Department-specific queue (e.g. RADIOLOGY/PATHOLOGY) — oldest first, today only.")
    public List<QueueToken> listByDepartment(@PathVariable long departmentId) {
        return service.listTokensByDepartment(departmentId);
    }

    @PostMapping("/{id}/transfer")
    @Operation(summary = "Transfer a token to another counter",
            description = "Allowed from WAITING/CALLED; the token returns to WAITING at the target counter and must be re-called. "
                    + "Terminal tokens cannot transfer.")
    public QueueToken transfer(@PathVariable long id, @RequestBody(required = false) JsonNode body) {
        return service.transfer(id, body);
    }

    @PostMapping("/{id}/no-show")
    @Operation(summary = "Mark a called token as no-show")
    public QueueToken noShow(@PathVariable long id) {
        return service.markNoShow(id);
    }

    @PostMapping("/{id}/complete")
    @Operation(summary = "Complete a called token (consultation done)")
    public QueueToken complete(@PathVariable long id) {
        return service.complete(id);
    }
}
