package com.hims.waste.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.hims.waste.application.BioMedicalWasteService;
import com.hims.waste.domain.model.BioMedicalWaste;
import com.hims.waste.domain.model.BioMedicalWasteDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Bio-medical waste request lifecycle API (MIG-023 sub-slice, S19) — clean
 * target of legacy BioMedicalWasteManagementServiceImpl (raise/approve/handover/
 * receive/process, per-bag barcode + status, counts, barcode validation).
 * REST-only: BMWRecorded REJECTED phase 1 (KAFKA-EVENT-CATALOG line 206).
 */
@RestController
@RequestMapping("/api/v1/biomedical-waste")
@Tag(name = "Bio-medical Waste (MIG-023)",
        description = "Disposal request lifecycle: raise (bag expansion + barcodes), approve, handover, "
                + "receive, process, per-bag status, counts, barcode validation")
public class BioMedicalWasteController {

    private final BioMedicalWasteService service;

    public BioMedicalWasteController(BioMedicalWasteService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "Raise a disposal request",
            description = "Payload {\"request\": {storeId, toStoreId, ...}, \"details\": [{bagId, quantity, remark, weight}]}. "
                    + "Server stamps requisitionNo ({unitPrefix}/BW/{yy}/{%05d}), requestStatus=1(PENDING), requestDate; "
                    + "each detail with quantity>0 expands into quantity bag rows (qty=1) with unique 13-char barcodes "
                    + "({yyyyMM}{%07d monthly counter}). unitPrefix optional (default IHMS until M_UNITMASTER integration).")
    public ResponseEntity<BioMedicalWaste> raise(@RequestBody(required = false) JsonNode body,
                                                 @RequestParam(required = false) Long unitId,
                                                 @RequestParam(required = false) String unitPrefix) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.raiseRequest(body, unitId, unitPrefix));
    }

    @GetMapping
    @Operation(summary = "List disposal requests",
            description = "Optional filters: unitId, requestStatus (1..5), search (requisitionNo; numeric search also "
                    + "matches bag barcodes — legacy getbioMedicalWasteStatus semantics). Newest first.")
    public List<BioMedicalWaste> list(@RequestParam(required = false) Long unitId,
                                      @RequestParam(required = false) Long requestStatus,
                                      @RequestParam(required = false) String search) {
        return service.listRequests(unitId, requestStatus, search);
    }

    @GetMapping("/counts")
    @Operation(summary = "Dashboard request counts",
            description = "TOTAL / PENDING / ACCEPTED / HANDOVERED / RECEIVED / COMPLETED (legacy getBioMedWasteCount).")
    public Map<String, Long> counts() {
        return service.getCounts();
    }

    @GetMapping("/{id}")
    public BioMedicalWaste get(@PathVariable long id) {
        return service.getRequest(id);
    }

    @GetMapping("/{id}/bags")
    @Operation(summary = "List bags of a request",
            description = "One row per physical bag (qty=1, unique barcodeNo, bagStatus).")
    public List<BioMedicalWasteDetails> bags(@PathVariable long id) {
        return service.getDetails(id);
    }

    @PutMapping("/{id}/bags")
    @Operation(summary = "Update bag quantity/remark/weight before approval",
            description = "Body [{id, quantity?, remark?, weight?}]; request must be PENDING (legacy updateBioRequest).")
    public List<BioMedicalWasteDetails> updateBags(@PathVariable long id,
                                                   @RequestBody(required = false) JsonNode body) {
        return service.updateBags(id, body);
    }

    @PostMapping("/{id}/approve")
    @Operation(summary = "Approve a request",
            description = "PENDING(1) -> ACCEPTED(2); stamps request + all bag statuses (legacy approveBioMedicalRequest).")
    public BioMedicalWaste approve(@PathVariable long id) {
        return service.approve(id);
    }

    @PostMapping("/{id}/handover")
    @Operation(summary = "Hand over bags to the collection firm",
            description = "ACCEPTED(2) -> HANDOVERED(3); handedOverTo stamped on request + bags (legacy handOverBags).")
    public BioMedicalWaste handover(@PathVariable long id, @RequestParam String handedOverTo) {
        return service.handover(id, handedOverTo);
    }

    @PostMapping("/{id}/receive")
    @Operation(summary = "Receive at main store",
            description = "HANDOVERED(3) -> RECEIVED(4); receivedBy stamped. Optional body [{id, weight}] captures "
                    + "per-bag weights (legacy receiveBioRequestMainStore).")
    public BioMedicalWaste receive(@PathVariable long id,
                                   @RequestParam(required = false) String receivedBy,
                                   @RequestBody(required = false) JsonNode body) {
        return service.receive(id, receivedBy, body);
    }

    @PostMapping("/{id}/process")
    @Operation(summary = "Process/complete the request",
            description = "RECEIVED(4) -> COMPLETED(5); firmName/contactPerson/contactNo stamped on all bags "
                    + "(legacy processBioRequest).")
    public BioMedicalWaste process(@PathVariable long id,
                                   @RequestParam(required = false) String firmName,
                                   @RequestParam(required = false) String contactPerson,
                                   @RequestParam(required = false) Long contactNo) {
        return service.process(id, firmName, contactPerson, contactNo);
    }

    @PostMapping("/{id}/bag-status")
    @Operation(summary = "Advance per-bag statuses",
            description = "Body [{id, bagStatus?, weight?, remark?, handedOverTo?}]: each listed bag advances "
                    + "bagStatus+1 (capped at COMPLETED); entries with bagStatus=2 stamp handedOverTo. Request status "
                    + "re-derived as min bag status (legacy updateBagStatus).")
    public BioMedicalWaste updateBagStatus(@PathVariable long id,
                                           @RequestBody(required = false) JsonNode body) {
        return service.updateBagStatus(id, body);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft-delete a request",
            description = "status=false + requestStatus=0 (legacy deleteBioRequest).")
    public BioMedicalWaste delete(@PathVariable long id) {
        return service.deleteRequest(id);
    }

    @GetMapping("/{id}/barcode/{barcode}/valid")
    @Operation(summary = "Validate a barcode against a request (legacy validateBarcode)")
    public Map<String, Object> validateBarcode(@PathVariable long id, @PathVariable String barcode) {
        return Map.of("barcode", barcode, "valid", service.validateBarcode(id, barcode));
    }

    @GetMapping("/{id}/barcode/{barcode}/weight")
    @Operation(summary = "Bag weight by barcode (legacy getBagWeightByBarcode)")
    public Map<String, Object> bagWeight(@PathVariable long id, @PathVariable String barcode) {
        BigDecimal weight = service.getBagWeightByBarcode(id, barcode);
        return Map.of("barcode", barcode, "weight", weight == null ? BigDecimal.ZERO : weight);
    }
}
