package com.hims.rcm.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.hims.rcm.application.RcmM32Service;
import com.hims.rcm.domain.model.ChiranjeeviCase;
import com.hims.rcm.domain.model.DischargeVerification;
import com.hims.rcm.domain.model.DmApproval;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/rcm")
public class RcmM32Controller {
    private final RcmM32Service service;
    public RcmM32Controller(RcmM32Service service) { this.service = service; }

    @PostMapping("/dm-approvals")
    public ResponseEntity<DmApproval> create(@RequestBody JsonNode body) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.requestDmApproval(
                longOrThrow(body, "patientId"), longOrNull(body, "admissionId"),
                textOrThrow(body, "scheme"), decimalOrThrow(body, "requestedAmount")));
    }
    @PostMapping("/dm-approvals/{id}/approve")
    public DmApproval approve(@PathVariable long id, @RequestBody JsonNode body) {
        return service.decide(id, true, decimalOrThrow(body, "approvedAmount"),
                longOrNull(body, "dmUserId"), textOrNull(body, "remarks"));
    }
    @PostMapping("/dm-approvals/{id}/reject")
    public DmApproval reject(@PathVariable long id, @RequestBody(required = false) JsonNode body) {
        return service.decide(id, false, null,
                body == null ? null : longOrNull(body, "dmUserId"),
                body == null ? null : textOrNull(body, "remarks"));
    }
    @GetMapping("/dm-approvals")
    public List<DmApproval> listDm(@RequestParam(required = false) Short status) { return service.listDm(status); }
    @GetMapping("/dm-approvals/{id}")
    public DmApproval getDm(@PathVariable long id) { return service.getDm(id); }

    @PostMapping("/discharge-verifications")
    public ResponseEntity<DischargeVerification> startDv(@RequestBody JsonNode body) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.startVerification(
                longOrThrow(body, "admissionId"), longOrThrow(body, "patientId")));
    }
    @PostMapping("/discharge-verifications/{id}/bill-settled")
    public DischargeVerification setBillSettled(@PathVariable long id, @RequestBody JsonNode body) {
        return service.setBillSettled(id, boolOrTrue(body, "settled"));
    }
    @PostMapping("/discharge-verifications/{id}/clearance-done")
    public DischargeVerification setClearanceDone(@PathVariable long id, @RequestBody JsonNode body) {
        return service.setClearanceDone(id, boolOrTrue(body, "done"));
    }
    @PostMapping("/discharge-verifications/{id}/complete")
    public DischargeVerification completeDv(@PathVariable long id, @RequestBody JsonNode body) {
        return service.complete(id, boolOrTrue(body, "verified"),
                longOrNull(body, "verifierUserId"), textOrNull(body, "remarks"));
    }
    @GetMapping("/discharge-verifications")
    public List<DischargeVerification> listDv(
            @RequestParam(required = false) Long admissionId,
            @RequestParam(required = false) Short status) { return service.listDv(admissionId, status); }
    @GetMapping("/discharge-verifications/{id}")
    public DischargeVerification getDv(@PathVariable long id) { return service.getDv(id); }

    @PostMapping("/chiranjeevi-cases")
    public ResponseEntity<ChiranjeeviCase> openCj(@RequestBody JsonNode body) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.openCase(
                longOrThrow(body, "patientId"), textOrThrow(body, "chiranjeeviId"),
                textOrThrow(body, "packageCode"), decimalOrThrow(body, "amount")));
    }
    @PostMapping("/chiranjeevi-cases/{id}/approve")
    public ChiranjeeviCase approveCj(@PathVariable long id, @RequestBody(required = false) JsonNode body) {
        return service.decideCase(id, true, body == null ? null : textOrNull(body, "remarks"));
    }
    @PostMapping("/chiranjeevi-cases/{id}/reject")
    public ChiranjeeviCase rejectCj(@PathVariable long id, @RequestBody(required = false) JsonNode body) {
        return service.decideCase(id, false, body == null ? null : textOrNull(body, "remarks"));
    }
    @PostMapping("/chiranjeevi-cases/{id}/close")
    public ChiranjeeviCase closeCj(@PathVariable long id, @RequestBody(required = false) JsonNode body) {
        return service.close(id, body == null ? null : textOrNull(body, "remarks"));
    }
    @GetMapping("/chiranjeevi-cases")
    public List<ChiranjeeviCase> listCj(@RequestParam(required = false) Short status) { return service.listCj(status); }
    @GetMapping("/chiranjeevi-cases/{id}")
    public ChiranjeeviCase getCj(@PathVariable long id) { return service.getCj(id); }

    private static String textOrNull(JsonNode n, String f) { JsonNode v = n.path(f); return v.isMissingNode()||v.isNull()?null:v.asText(); }
    private static String textOrThrow(JsonNode n, String f) { JsonNode v = n.path(f); if (v.isMissingNode()||v.isNull()) throw new com.hims.rcm.application.exceptions.InvalidRcmRequestException("BEXA003", f + " is required"); return v.asText(); }
    private static Long longOrNull(JsonNode n, String f) { JsonNode v = n.path(f); return v.isMissingNode()||v.isNull()||v.asLong(0)==0?null:v.asLong(); }
    private static long longOrThrow(JsonNode n, String f) { JsonNode v = n.path(f); if (v.isMissingNode()||v.isNull()||v.asLong(0)==0) throw new com.hims.rcm.application.exceptions.InvalidRcmRequestException("BEXA003", f + " is required"); return v.asLong(); }
    private static BigDecimal decimalOrThrow(JsonNode n, String f) { JsonNode v = n.path(f); if (v.isMissingNode()||v.isNull()) throw new com.hims.rcm.application.exceptions.InvalidRcmRequestException("BEXA003", f + " is required"); return v.decimalValue(); }
    private static boolean boolOrTrue(JsonNode n, String f) { JsonNode v = n.path(f); return v.isMissingNode()||v.isNull()?true:v.asBoolean(); }
}
