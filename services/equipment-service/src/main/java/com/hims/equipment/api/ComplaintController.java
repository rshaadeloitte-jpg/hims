package com.hims.equipment.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.hims.equipment.application.ComplaintResolutionService;
import com.hims.equipment.application.ComplaintService;
import com.hims.equipment.domain.model.EquipmentComplaint;
import com.hims.equipment.domain.model.EquipmentComplaintDetails;
import com.hims.equipment.domain.model.EquipmentComplaintResolution;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Equipment complaint API (MIG-023 Phase 2) — raise, search, detail view,
 * per-asset complaint history and the resolution lifecycle. Complaint numbers
 * are server-generated {@code IHMS/EQ/{yyyy}/{%05d}}.
 */
@RestController
@RequestMapping("/api/v1/equipment")
@Tag(name = "Equipment Complaints (MIG-023)",
        description = "Raise complaints against assets, search them, walk the "
                + "raise -> resolve -> close lifecycle and keep resolution records")
public class ComplaintController {

    private final ComplaintService complaintService;
    private final ComplaintResolutionService resolutionService;

    public ComplaintController(ComplaintService complaintService,
                               ComplaintResolutionService resolutionService) {
        this.complaintService = complaintService;
        this.resolutionService = resolutionService;
    }

    @PostMapping("/complaints")
    @Operation(summary = "Raise a complaint",
            description = "Body is the complaint record (equipmentId required). Server assigns id from "
                    + "equipment_complaint_seq, generates complaintNo IHMS/EQ/{yyyy}/{%05d}, defaults "
                    + "complaintStatus to OPEN(1) (1 OPEN, 2 IN_PROGRESS, 3 RESOLVED, 4 CLOSED), stamps "
                    + "complaintDate/reportedDate to now unless given and snapshots accession/department/"
                    + "store from the asset. Rejected for unknown, inactive or CONDEMNED assets.")
    public ResponseEntity<EquipmentComplaint> raise(@RequestBody(required = false) JsonNode body,
                                                    @RequestParam(required = false) Long unitId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(complaintService.raise(body, unitId));
    }

    @GetMapping("/complaints")
    @Operation(summary = "List / search complaints",
            description = "Optional filters: unitId, equipmentId, complaintStatus (1 OPEN, 2 IN_PROGRESS, "
                    + "3 RESOLVED, 4 CLOSED), search (free text over complaintNo, description, accessionNo). "
                    + "Active complaints only, newest first.")
    public List<EquipmentComplaint> list(@RequestParam(required = false) Long unitId,
                                         @RequestParam(required = false) Long equipmentId,
                                         @RequestParam(required = false) Long complaintStatus,
                                         @RequestParam(required = false) String search) {
        return complaintService.search(unitId, equipmentId, complaintStatus, search);
    }

    @GetMapping("/{equipmentId}/complaints")
    @Operation(summary = "Complaint history of an asset", description = "Active complaints of one asset, newest first.")
    public List<EquipmentComplaint> history(@PathVariable long equipmentId) {
        return complaintService.history(equipmentId);
    }

    @GetMapping("/complaints/{id}")
    @Operation(summary = "Complaint details",
            description = "Header record plus its line items and its resolutions in chronological order.")
    public ComplaintDetailsResponse get(@PathVariable long id) {
        EquipmentComplaint complaint = complaintService.getComplaint(id);
        List<EquipmentComplaintDetails> details = complaintService.detailsOf(id);
        List<EquipmentComplaintResolution> resolutions = resolutionService.listResolutions(id);
        return new ComplaintDetailsResponse(complaint, details, resolutions);
    }

    @PostMapping("/complaints/{id}/resolutions")
    @Operation(summary = "Add a resolution",
            description = "Body is the resolution record; resolvedStatus defaults to RESOLVED(3) and must be a "
                    + "valid dictionary id (1..4); resolutionDate defaults to now. Optional details[] array saves "
                    + "replaced-part line items (batch/expiry traceability). The complaint moves to resolvedStatus "
                    + "and gets its resolvedDate atomically with the resolution row.")
    public ResponseEntity<EquipmentComplaintResolution> addResolution(@PathVariable long id,
                                                                      @RequestBody(required = false) JsonNode body) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(resolutionService.add(id, body));
    }

    @PutMapping("/complaints/{id}/resolutions/{resId}")
    @Operation(summary = "Update a resolution",
            description = "Null patch fields are ignored; id/complaintId/unit/audit columns cannot be changed; "
                    + "resolvedStatus is intentionally not patchable — status transitions belong to add-resolution. "
                    + "The resolution must belong to the given complaint.")
    public EquipmentComplaintResolution updateResolution(@PathVariable long id,
                                                         @PathVariable long resId,
                                                         @RequestBody(required = false) JsonNode body) {
        return resolutionService.update(id, resId, body);
    }

    @GetMapping("/complaints/{id}/resolutions")
    @Operation(summary = "List resolutions of a complaint", description = "Chronological (oldest first).")
    public List<EquipmentComplaintResolution> resolutions(@PathVariable long id) {
        return resolutionService.listResolutions(id);
    }
}
