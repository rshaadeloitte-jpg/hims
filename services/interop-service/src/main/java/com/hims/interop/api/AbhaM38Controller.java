package com.hims.interop.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.hims.interop.application.AbhaService;
import com.hims.interop.domain.model.AbhaConsent;
import com.hims.interop.domain.model.AbhaLink;
import com.hims.interop.domain.model.PhrPush;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/interop/abha-m38")
public class AbhaM38Controller {
    private final AbhaService service;
    public AbhaM38Controller(AbhaService service) { this.service = service; }

    @PostMapping("/links")
    public ResponseEntity<AbhaLink> createLink(@RequestBody JsonNode body) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createLink(
                textOrThrow(body, "abhaAddress"),
                textOrNull(body, "healthIdNumber"),
                longOrNull(body, "patientId"),
                textOrThrow(body, "txnId")));
    }
    @PostMapping("/links/{id}/verify")
    public AbhaLink verifyLink(@PathVariable long id, @RequestBody(required = false) JsonNode body) {
        return service.verifyLink(id, body == null ? null : textOrNull(body, "healthIdNumber"));
    }
    @PostMapping("/links/{id}/delink") public AbhaLink delink(@PathVariable long id) { return service.delink(id); }
    @PostMapping("/links/{id}/revoke") public AbhaLink revoke(@PathVariable long id) { return service.revoke(id); }
    @GetMapping("/links/{id}") public AbhaLink getLink(@PathVariable long id) { return service.getLink(id); }
    @GetMapping("/links/by-patient/{patientId}")
    public List<AbhaLink> byPatient(@PathVariable long patientId) { return service.findByPatient(patientId); }
    @GetMapping("/links/by-status/{status}")
    public List<AbhaLink> byStatus(@PathVariable short status) { return service.findByStatus(status); }

    @PostMapping("/consents")
    public ResponseEntity<AbhaConsent> grantConsent(@RequestBody JsonNode body) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.grantConsent(
                longOrThrow(body, "abhaLinkId"),
                textOrThrow(body, "consentRequestId"),
                textOrThrow(body, "purpose"),
                textOrThrow(body, "hipId"),
                textOrThrow(body, "hiuId"),
                java.sql.Timestamp.valueOf(textOrThrow(body, "validFrom").replace("T", " ").replace("Z", "")),
                java.sql.Timestamp.valueOf(textOrThrow(body, "validTo").replace("T", " ").replace("Z", ""))));
    }
    @PostMapping("/consents/{id}/revoke") public AbhaConsent revokeConsent(@PathVariable long id) { return service.revokeConsent(id); }
    @PostMapping("/consents/{id}/expire") public AbhaConsent expireConsent(@PathVariable long id) { return service.expireConsent(id); }
    @GetMapping("/consents/by-link/{abhaLinkId}")
    public List<AbhaConsent> consentsFor(@PathVariable long abhaLinkId) { return service.consentsFor(abhaLinkId); }

    @PostMapping("/phr-push")
    public ResponseEntity<PhrPush> queuePush(@RequestBody JsonNode body) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.queuePush(
                longOrThrow(body, "abhaLinkId"),
                textOrThrow(body, "payloadType"),
                body.path("payload")));
    }
    @PostMapping("/phr-push/{id}/pushed") public PhrPush markPushed(@PathVariable long id) { return service.markPushed(id); }
    @PostMapping("/phr-push/{id}/failed") public PhrPush markFailed(@PathVariable long id, @RequestBody JsonNode body) {
        return service.markFailed(id, textOrNull(body, "reason"));
    }
    @GetMapping("/phr-push/pending/{abhaLinkId}")
    public List<PhrPush> pending(@PathVariable long abhaLinkId) { return service.pendingPushes(abhaLinkId); }

    private static String textOrNull(JsonNode n, String f) { JsonNode v = n.path(f); return v.isMissingNode()||v.isNull()?null:v.asText(); }
    private static String textOrThrow(JsonNode n, String f) { JsonNode v = n.path(f); if (v.isMissingNode()||v.isNull()) throw new com.hims.interop.application.exceptions.InvalidRequestException("BEXA003", f + " is required"); return v.asText(); }
    private static Long longOrNull(JsonNode n, String f) { JsonNode v = n.path(f); return v.isMissingNode()||v.isNull()||v.asLong(0)==0?null:v.asLong(); }
    private static long longOrThrow(JsonNode n, String f) { JsonNode v = n.path(f); if (v.isMissingNode()||v.isNull()||v.asLong(0)==0) throw new com.hims.interop.application.exceptions.InvalidRequestException("BEXA003", f + " is required"); return v.asLong(); }
}
