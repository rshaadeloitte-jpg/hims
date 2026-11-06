package com.hims.rmrs.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.hims.rmrs.application.RmrsM23Service;
import com.hims.rmrs.domain.model.BankStatement;
import com.hims.rmrs.domain.model.JournalVoucher;
import com.hims.rmrs.domain.model.JournalVoucherLeg;
import com.hims.rmrs.domain.model.LedgerMaster;
import com.hims.rmrs.domain.model.PoRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/v1/rmrs")
public class RmrsM23Controller {
    private final RmrsM23Service service;
    public RmrsM23Controller(RmrsM23Service service) { this.service = service; }

    // journal vouchers
    @PostMapping("/journal-vouchers")
    public ResponseEntity<JournalVoucher> createVoucher(@RequestBody JsonNode body) {
        List<RmrsM23Service.Leg> legs = new ArrayList<>();
        for (JsonNode l : body.path("legs")) legs.add(new RmrsM23Service.Leg(
                longOrNull(l, "ledgerId"), decimalOrNull(l, "debit"), decimalOrNull(l, "credit")));
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createVoucher(
                textOrNull(body, "narration"), textOrNull(body, "voucherDate"), legs));
    }
    @PostMapping("/journal-vouchers/{id}/post") public JournalVoucher post(@PathVariable long id) { return service.postVoucher(id, null); }
    @PostMapping("/journal-vouchers/{id}/reverse") public JournalVoucher reverse(@PathVariable long id) { return service.reverseVoucher(id, null); }
    @GetMapping("/journal-vouchers") public List<JournalVoucher> listVouchers(@RequestParam(required=false) Short status) { return service.listVouchers(status); }
    @GetMapping("/journal-vouchers/{id}") public JournalVoucher getVoucher(@PathVariable long id) { return service.getVoucher(id); }
    @GetMapping("/journal-vouchers/{id}/legs") public List<JournalVoucherLeg> legs(@PathVariable long id) { return service.voucherLegs(id); }

    // suspense
    @PostMapping("/ledgers/{id}/suspense") public LedgerMaster markSuspense(@PathVariable long id, @RequestBody JsonNode body) {
        return service.markSuspense(id, textOrNull(body, "reason"));
    }
    @PostMapping("/ledgers/{id}/suspense/clear") public LedgerMaster clearSuspense(@PathVariable long id) { return service.clearSuspense(id); }

    // bank statement
    @PostMapping("/bank-statements") public ResponseEntity<BankStatement> importBs(@RequestBody JsonNode body) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.importBankStatement(
                longOrThrow(body, "bankLedgerId"), textOrThrow(body, "txnDate"),
                decimalOrThrow(body, "amount"), textOrThrow(body, "direction"),
                textOrNull(body, "description"), textOrNull(body, "externalRef")));
    }
    @PostMapping("/bank-statements/{id}/reconcile") public BankStatement reconcile(@PathVariable long id, @RequestBody JsonNode body) {
        return service.reconcile(id, longOrNull(body, "matchedEntryId"), longOrNull(body, "matchedCollectionId"));
    }
    @PostMapping("/bank-statements/{id}/dispute") public BankStatement dispute(@PathVariable long id, @RequestBody JsonNode body) {
        return service.dispute(id, textOrNull(body, "reason"));
    }
    @GetMapping("/bank-statements") public List<BankStatement> listBs(@RequestParam(required=false) Short status) { return service.listBankStatements(status); }

    // PO request
    @PostMapping("/po-requests") public ResponseEntity<PoRequest> createPoq(@RequestBody JsonNode body) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createPoRequest(
                longOrNull(body, "requestingDeptId"), longOrThrow(body, "headId"), longOrNull(body, "ledgerId"),
                decimalOrThrow(body, "amount"), textOrNull(body, "remarks")));
    }
    @PostMapping("/po-requests/{id}/approve") public PoRequest approvePoq(@PathVariable long id) { return service.approvePoRequest(id, null); }
    @PostMapping("/po-requests/{id}/reject") public PoRequest rejectPoq(@PathVariable long id) { return service.rejectPoRequest(id); }
    @PostMapping("/po-requests/{id}/issue") public PoRequest issuePoq(@PathVariable long id, @RequestBody JsonNode body) {
        return service.markIssued(id, longOrNull(body, "issuedPoId"));
    }
    @GetMapping("/po-requests") public List<PoRequest> listPoq(@RequestParam(required=false) Short status) { return service.listPoRequests(status); }
    @GetMapping("/po-requests/{id}") public PoRequest getPoq(@PathVariable long id) { return service.getPoRequest(id); }

    private static String textOrNull(JsonNode n, String f) { JsonNode v = n.path(f); return v.isMissingNode()||v.isNull()?null:v.asText(); }
    private static String textOrThrow(JsonNode n, String f) { JsonNode v = n.path(f); if (v.isMissingNode()||v.isNull()) throw new com.hims.rmrs.application.exceptions.InvalidRmrsRequestException(f + " is required"); return v.asText(); }
    private static Long longOrNull(JsonNode n, String f) { JsonNode v = n.path(f); return v.isMissingNode()||v.isNull()||v.asLong(0)==0?null:v.asLong(); }
    private static long longOrThrow(JsonNode n, String f) { JsonNode v = n.path(f); if (v.isMissingNode()||v.isNull()||v.asLong(0)==0) throw new com.hims.rmrs.application.exceptions.InvalidRmrsRequestException(f + " is required"); return v.asLong(); }
    private static BigDecimal decimalOrNull(JsonNode n, String f) { JsonNode v = n.path(f); return v.isMissingNode()||v.isNull()?null:v.decimalValue(); }
    private static BigDecimal decimalOrThrow(JsonNode n, String f) { JsonNode v = n.path(f); if (v.isMissingNode()||v.isNull()) throw new com.hims.rmrs.application.exceptions.InvalidRmrsRequestException(f + " is required"); return v.decimalValue(); }
}
