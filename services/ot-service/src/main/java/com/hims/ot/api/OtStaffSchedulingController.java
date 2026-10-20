package com.hims.ot.api;

import com.hims.ot.application.OtStaffSchedulingService;
import com.hims.ot.domain.model.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.sql.Timestamp;
import java.util.List;

@RestController
@RequestMapping("/ot/staff-schedule")
public class OtStaffSchedulingController {

    private final OtStaffSchedulingService service;

    public OtStaffSchedulingController(OtStaffSchedulingService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<OtStaffSchedule> assign(@RequestParam Long scheduleId, @RequestParam String role,
                                                  @RequestParam Long staffId, @RequestParam Timestamp start,
                                                  @RequestParam Timestamp end) {
        return ResponseEntity.ok(service.assignStaff(scheduleId, role, staffId, start, end));
    }

    @PostMapping("/rooms")
    public ResponseEntity<OtRoom> createRoom(@RequestParam String name, @RequestParam String type) {
        return ResponseEntity.ok(service.createRoom(name, type));
    }

    @GetMapping
    public ResponseEntity<List<OtStaffSchedule>> findBySchedule(@RequestParam Long scheduleId) {
        return ResponseEntity.ok(service.findBySchedule(scheduleId));
    }
}
