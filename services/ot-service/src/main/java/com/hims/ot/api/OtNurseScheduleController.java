package com.hims.ot.api;

import com.hims.ot.application.OtNurseScheduleService;
import com.hims.ot.domain.model.OtNurseSchedule;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.sql.Date;
import java.util.List;

@RestController
@RequestMapping("/ot/nurse-schedule")
public class OtNurseScheduleController {

    private final OtNurseScheduleService service;

    public OtNurseScheduleController(OtNurseScheduleService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<OtNurseSchedule> create(@RequestParam Long staffId, @RequestParam Date dutyDate,
                                                  @RequestParam String shiftType, @RequestParam String dutyRoster,
                                                  @RequestParam(defaultValue = "false") Boolean emergencyCall) {
        return ResponseEntity.ok(service.createDuty(staffId, dutyDate, shiftType, dutyRoster, emergencyCall));
    }

    @GetMapping
    public ResponseEntity<List<OtNurseSchedule>> findByStaff(@RequestParam Long staffId) {
        return ResponseEntity.ok(service.findByStaff(staffId));
    }
}
