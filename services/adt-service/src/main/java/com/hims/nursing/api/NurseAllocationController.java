package com.hims.nursing.api;

import com.hims.nursing.application.NurseAllocationService;
import com.hims.nursing.application.exceptions.InvalidNursingRequestException;
import com.hims.nursing.domain.model.NurseAllocation;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/nursing/allocations")
public class NurseAllocationController {

    private final NurseAllocationService service;

    public NurseAllocationController(NurseAllocationService service) {
        this.service = service;
    }

    @GetMapping
    public List<NurseAllocation> list(@RequestParam(required = false) Long wardId,
                                      @RequestParam(required = false) String date) {
        Timestamp ts = (date == null || date.isBlank()) ? null : parse(date);
        return service.list(wardId, ts);
    }

    @PostMapping
    public ResponseEntity<NurseAllocation> create(@RequestBody Map<String, Object> body) {
        NurseAllocation a = new NurseAllocation();
        a.setWardId(longOf(body, "wardId"));
        a.setNurseId(longOf(body, "nurseId"));
        a.setAdmissionId(longOf(body, "admissionId"));
        a.setShift(strOf(body, "shift"));
        a.setUnitId(longOf(body, "unitId"));
        NurseAllocation saved = service.create(a);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PostMapping("/{id}/release")
    public NurseAllocation release(@PathVariable Long id) {
        return service.release(id);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    private static Long longOf(Map<String, Object> body, String key) {
        Object v = body.get(key);
        if (v == null) return null;
        if (v instanceof Number n) return n.longValue();
        try { return Long.valueOf(String.valueOf(v)); } catch (NumberFormatException e) {
            throw new InvalidNursingRequestException("BEXA003", key + " must be numeric");
        }
    }
    private static String strOf(Map<String, Object> body, String key) {
        Object v = body.get(key);
        return v == null ? null : String.valueOf(v);
    }
    private static Timestamp parse(String s) {
        try { return Timestamp.valueOf(LocalDateTime.parse(s)); }
        catch (DateTimeParseException e) { throw new InvalidNursingRequestException("BEXA003", "date must be ISO date-time"); }
    }
}