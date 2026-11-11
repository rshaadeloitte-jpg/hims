package com.hims.nursing.api;

import com.hims.nursing.application.PartographService;
import com.hims.nursing.domain.model.Partograph;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;

@RestController
@Tag(name = "Partograph (M09)", description = "Labour monitoring chart: cervix dilation, descent, vitals, membrane status, delivery outcome")
public class PartographController {

    private final PartographService service;

    public PartographController(PartographService service) {
        this.service = service;
    }

    @PostMapping("/api/v1/partographs")
    @Operation(summary = "Create a partograph for an admission (M09 gap)")
    public ResponseEntity<Partograph> create(@RequestBody Map<String, Object> body) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createPartograph(
                longOf(body, "patientId"), longOf(body, "admissionId"),
                longOf(body, "opdIpdId"), longOf(body, "opdIpd"),
                strOf(body, "parity"), strOf(body, "startOfLabourDate"),
                strOf(body, "membraneStatus"), timestampOf(body, "membraneRuptureDate"),
                strOf(body, "membraneVitalAbb"), decimalOf(body, "cervixDilation"),
                intOf(body, "descent"), intOf(body, "systolicBp"), intOf(body, "diastolicBp"),
                intOf(body, "heartRate"), decimalOf(body, "temperature"),
                intOf(body, "respirationRate"), decimalOf(body, "oxygenSaturation"),
                intOf(body, "contractionFreq"), intOf(body, "contractionDuration"),
                intOf(body, "foetalHeartRate"), intOf(body, "moulding"), strOf(body, "liquor"),
                strOf(body, "drugsAndFluid"), strOf(body, "oxytocin"),
                intOf(body, "urineOutput"), strOf(body, "urineAcetone"), strOf(body, "urineProtein"),
                strOf(body, "deliveryDateTime"), strOf(body, "deliveryOutcome"),
                decimalOf(body, "babyWeight"), intOf(body, "apgarScore1min"),
                intOf(body, "apgarScore5min"), intOf(body, "apgarScore10min"),
                intOf(body, "apgarScore15min"), intOf(body, "apgarScore20min"),
                strOf(body, "placentaType"), intOf(body, "bloodLossMl"), strOf(body, "remarks")));
    }

    @PutMapping("/api/v1/partographs/{id}/delivery")
    @Operation(summary = "Update delivery outcome on a partograph")
    public Partograph updateDelivery(@PathVariable long id, @RequestBody Map<String, Object> body) {
        return service.updatePartographDeliveryOutcome(id,
                strOf(body, "deliveryOutcome"), decimalOf(body, "babyWeight"),
                intOf(body, "apgarScore1min"), intOf(body, "apgarScore5min"),
                intOf(body, "apgarScore10min"), intOf(body, "apgarScore15min"),
                intOf(body, "apgarScore20min"), strOf(body, "placentaType"),
                intOf(body, "bloodLossMl"), strOf(body, "remarks"));
    }

    @GetMapping("/api/v1/partographs/{id}")
    @Operation(summary = "Get a partograph by id")
    public Partograph getById(@PathVariable long id) {
        return service.getById(id);
    }

    @GetMapping("/api/v1/partographs")
    @Operation(summary = "List partographs by patient or admission")
    public List<Partograph> list(@RequestParam(required = false) Long patientId,
                                 @RequestParam(required = false) Long admissionId) {
        if (patientId != null) return service.listByPatient(patientId);
        if (admissionId != null) return service.listByAdmission(admissionId);
        throw new com.hims.nursing.application.exceptions.InvalidNursingRequestException("BEXA003", "patientId or admissionId is required");
    }

    private static Long longOf(Map<String, Object> body, String key) {
        Object v = body.get(key);
        if (v == null) return null;
        if (v instanceof Number n) return n.longValue();
        try { return Long.valueOf(String.valueOf(v)); } catch (NumberFormatException e) {
            throw new com.hims.nursing.application.exceptions.InvalidNursingRequestException("BEXA003", key + " must be numeric");
        }
    }

    private static Integer intOf(Map<String, Object> body, String key) {
        Object v = body.get(key);
        if (v == null) return null;
        if (v instanceof Number n) return n.intValue();
        try { return Integer.valueOf(String.valueOf(v)); } catch (NumberFormatException e) {
            throw new com.hims.nursing.application.exceptions.InvalidNursingRequestException("BEXA003", key + " must be numeric");
        }
    }

    private static String strOf(Map<String, Object> body, String key) {
        Object v = body.get(key);
        return v == null ? null : String.valueOf(v);
    }

    private static BigDecimal decimalOf(Map<String, Object> body, String key) {
        Object v = body.get(key);
        if (v == null) return null;
        if (v instanceof Number n) return new BigDecimal(n.toString());
        try { return new BigDecimal(String.valueOf(v)); } catch (NumberFormatException e) {
            throw new com.hims.nursing.application.exceptions.InvalidNursingRequestException("BEXA003", key + " must be numeric");
        }
    }

    private static Timestamp timestampOf(Map<String, Object> body, String key) {
        String s = strOf(body, key);
        if (s == null || s.isBlank()) return null;
        try { return Timestamp.valueOf(LocalDateTime.parse(s)); } catch (DateTimeParseException e) {
            throw new com.hims.nursing.application.exceptions.InvalidNursingRequestException("BEXA003", key + " must be an ISO date-time");
        }
    }
}
