package com.hims.queue.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.hims.queue.application.QueueService;
import com.hims.queue.domain.model.Counter;
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
 * Counter + dashboard API (MIG-022 / S29): counter-to-doctor mappings,
 * call-next with emergency priority jump, kiosk/nursing-desk display projection.
 */
@RestController
@RequestMapping("/api/v1/queue")
@Tag(name = "Queue Counters & Dashboard (MIG-022)",
        description = "Counter master (counter-to-doctor mapping), call-next, dashboard projection")
public class QueueCounterController {

    private final QueueService service;

    public QueueCounterController(QueueService service) {
        this.service = service;
    }

    @PostMapping("/counters")
    @Operation(summary = "Create a counter",
            description = "code + name required; departmentId + doctorId form the counter-to-doctor mapping.")
    public ResponseEntity<Counter> createCounter(@RequestBody(required = false) JsonNode body,
                                                 @RequestParam(required = false) Long unitId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createCounter(body, unitId));
    }

    @GetMapping("/counters")
    @Operation(summary = "List counters", description = "activeOnly=true filters deactivated rows.")
    public List<Counter> listCounters(@RequestParam(required = false, defaultValue = "false") boolean activeOnly) {
        return service.listCounters(activeOnly);
    }

    @PostMapping("/counters/{id}/call-next")
    @Operation(summary = "Call the next waiting token for this counter",
            description = "Picks from the counter's department pool: EMERGENCY(2) tokens jump ahead of NORMAL(1), FIFO by daily token number. "
                    + "Stamps counterId + calledAt and moves the token to CALLED(2).")
    public QueueToken callNext(@PathVariable long id) {
        return service.callNext(id);
    }

    @GetMapping("/dashboard")
    @Operation(summary = "Kiosk/TV/nursing-desk display projection",
            description = "Counts per status, nowServing map (counterId -> tokenNo), and the ordered waiting list (max 20).")
    public Map<String, Object> dashboard(@RequestParam(required = false) Long unitId) {
        return service.dashboard(unitId);
    }

    @GetMapping("/department/{departmentId}/dashboard")
    @Operation(summary = "Department-specific dashboard (M27 rad/patho)",
            description = "Filtered counts/waitingList/nowServing for a department (e.g. 9001=RAD, 9002=PATHOLOGY) plus counters in dept (cabin board).")
    public Map<String, Object> departmentDashboard(@PathVariable long departmentId) {
        return service.dashboardByDepartment(departmentId);
    }

    @GetMapping("/department/{departmentId}/counters")
    @Operation(summary = "Counters in a department (M27)")
    public java.util.List<Counter> departmentCounters(@PathVariable long departmentId) {
        return service.listCountersByDepartment(departmentId);
    }

    @GetMapping("/cabin/{counterId}/board")
    @Operation(summary = "Cabin queue board (M27)",
            description = "Single cabin (counter) board: tokens at this cabin + department waiting.")
    public Map<String, Object> cabinBoard(@PathVariable long counterId) {
        return service.cabinBoard(counterId);
    }
}
