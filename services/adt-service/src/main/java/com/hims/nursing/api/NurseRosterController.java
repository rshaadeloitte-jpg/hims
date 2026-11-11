package com.hims.nursing.api;

import com.hims.nursing.application.NurseRosterService;
import com.hims.nursing.domain.model.NurseRoster;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.sql.Date;
import java.util.List;
import java.util.Map;

@RestController
@Tag(name = "Nurse Shift Roster (M09)", description = "Duty rostering by ward + shift per day")
public class NurseRosterController {

    private final NurseRosterService service;

    public NurseRosterController(NurseRosterService service) {
        this.service = service;
    }

    @PostMapping("/api/v1/nurse-rosters")
    @Operation(summary = "Assign a nurse to a ward+shift+day (M09 gap)")
    public ResponseEntity<NurseRoster> assign(@RequestBody Map<String, Object> body) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.assign(
                dateOf(body, "rosterDate"), longOf(body, "wardId"),
                longOf(body, "nurseId"), strOf(body, "shift"),
                longOf(body, "bedId"), boolOf(body, "isAllied")));
    }

    @DeleteMapping("/api/v1/nurse-rosters/{id}")
    @Operation(summary = "Remove a roster assignment (soft delete)")
    public NurseRoster remove(@PathVariable long id) {
        return service.removeAssignment(id);
    }

    @GetMapping("/api/v1/nurse-rosters")
    @Operation(summary = "List roster by ward+date or nurse+date")
    public List<NurseRoster> list(@RequestParam(required = false) String rosterDate,
                                  @RequestParam(required = false) Long wardId,
                                  @RequestParam(required = false) Long nurseId) {
        if (rosterDate == null) throw new com.hims.nursing.application.exceptions.InvalidNursingRequestException("BEXA003", "rosterDate is required");
        Date d = Date.valueOf(rosterDate);
        if (wardId != null) return service.listByWardAndDate(d, wardId);
        if (nurseId != null) return service.listByNurseAndDate(nurseId, d);
        throw new com.hims.nursing.application.exceptions.InvalidNursingRequestException("BEXA003", "wardId or nurseId is required");
    }

    private static Long longOf(Map<String, Object> body, String key) {
        Object v = body.get(key);
        if (v == null) return null;
        if (v instanceof Number n) return n.longValue();
        try { return Long.valueOf(String.valueOf(v)); } catch (NumberFormatException e) {
            throw new com.hims.nursing.application.exceptions.InvalidNursingRequestException("BEXA003", key + " must be numeric");
        }
    }

    private static String strOf(Map<String, Object> body, String key) {
        Object v = body.get(key);
        return v == null ? null : String.valueOf(v);
    }

    private static Boolean boolOf(Map<String, Object> body, String key) {
        Object v = body.get(key);
        if (v == null) return null;
        if (v instanceof Boolean b) return b;
        return Boolean.valueOf(String.valueOf(v));
    }

    private static Date dateOf(Map<String, Object> body, String key) {
        String s = strOf(body, key);
        if (s == null || s.isBlank()) return null;
        try { return Date.valueOf(s); } catch (IllegalArgumentException e) {
            throw new com.hims.nursing.application.exceptions.InvalidNursingRequestException("BEXA003", key + " must be a date (yyyy-MM-dd)");
        }
    }
}
