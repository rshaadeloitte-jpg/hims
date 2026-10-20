package com.hims.adt.api;

import com.hims.adt.application.AdmissionService;
import com.hims.adt.application.exceptions.InvalidAdtRequestException;
import com.hims.adt.domain.model.Admission;
import com.hims.adt.domain.model.BedTransferLog;
import com.hims.adt.domain.model.ClearanceChecklistItem;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;

/**
 * Admission / transfer / discharge API (GAP-REGISTER WP-5).
 * Admit enforces BR-IPD-001/002/003; discharge applies BR-DIS-003; the
 * transactional outbox emits PatientAdmitted (E-03), PatientDischarged (E-04),
 * BedTransferred and BedStatusChanged (E-11 semantics) to ihms.adt.lifecycle.
 */
@RestController
@RequestMapping("/api/v1/admissions")
@Tag(name = "Admissions (WP-5)",
        description = "IPD admission with CAS bed claim (BR-IPD-002), transfer and discharge "
                + "(BR-DIS-003); outbox events E-03/E-04/E-11")
public class AdmissionController {

    private final AdmissionService service;

    public AdmissionController(AdmissionService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "Admit a patient",
            description = "BR-IPD-001: admitting doctor required (BERA022), no duplicate active admission "
                    + "(BERB013). BR-IPD-002: auto-allocates a free bed via atomic CAS claim when bedId is "
                    + "omitted. BR-IPD-003: bed status isrelease gate (BERB005). ipdNo follows the "
                    + "BR-IPD-001 template A/<prefix>/<yy>/<%05d seq> (atomic per-unit sequential). "
                    + "Seeds the 4-department discharge clearance checklist; isMlc=true requires mlcNumber. "
                    + "Writes E-03 PatientAdmitted to the outbox in the same transaction.")
    public ResponseEntity<Admission> admit(@RequestBody Map<String, Object> body) {
        Admission admission = service.admit(
                WardController.longOf(body, "patientId"),
                WardController.longOf(body, "encounterId"),
                WardController.longOf(body, "doctorId"),
                WardController.strOf(body, "admissionType"),
                WardController.longOf(body, "unitId"),
                WardController.longOf(body, "bedId"),
                WardController.boolOf(body, "isMlc"),
                WardController.strOf(body, "mlcNumber"),
                WardController.strOf(body, "mlcRemarks"),
                WardController.longOf(body, "attendingDoctorId"),
                WardController.strOf(body, "remarks"));
        return ResponseEntity.status(HttpStatus.CREATED).body(admission);
    }

    @GetMapping(params = {"patientId"})
    @Operation(summary = "List admissions", description = "?patientId=&active=")
    public List<Admission> list(@RequestParam(required = false) Long patientId,
                                @RequestParam(required = false) Boolean active) {
        return service.search(patientId, active);
    }

    @GetMapping("/{id}")
    public Admission get(@PathVariable long id) {
        return service.get(id);
    }

    @PostMapping("/{id}/transfer")
    @Operation(summary = "Transfer the patient to another bed",
            description = "CAS-claims the target bed (409 BEXB001 on loss), releases the old bed to "
                    + "CLEANING (status 2) with a t_bed_transfer_log row, updates admission bed/ward and "
                    + "emits BedTransferred (key patientId) plus two BedStatusChanged events (key bedId).")
    public Admission transfer(@PathVariable long id, @RequestBody Map<String, Object> body) {
        return service.transfer(id, WardController.longOf(body, "toBedId"),
                WardController.strOf(body, "reason"));
    }

    @PostMapping("/{id}/discharge")
    @Operation(summary = "Discharge the patient",
            description = "Fills the discharge block on t_admission and releases the bed per BR-DIS-003 "
                    + "(occupied=false, bed_status_id=6). Emits E-04 PatientDischarged (partition key "
                    + "patientId) plus a BedStatusChanged for the released bed. 400 when already discharged. "
                    + "M03 death handling: isExpired=true records deathDate/deathCause/deathType.")
    public Admission discharge(@PathVariable long id, @RequestBody Map<String, Object> body) {
        return service.discharge(id,
                WardController.strOf(body, "dischargeType"),
                WardController.boolOf(body, "isExpired"),
                timestampOf(WardController.strOf(body, "deathDate")),
                timestampOf(WardController.strOf(body, "handoverDate")),
                WardController.strOf(body, "deathCause"),
                WardController.strOf(body, "deathType"));
    }

    // ------------------------------------------------------------------ M03

    @PostMapping("/interim")
    @Operation(summary = "Interim admission (M03)",
            description = "Unbedded admission: no bed claim, is_interim=true, bed_id NULL, swept to AUTO_ABSCONDED after cutoff (BR-IPD-005). MLC + TID parity.")
    public ResponseEntity<Admission> admitInterim(@RequestBody Map<String, Object> body) {
        Admission a = service.admitInterim(
                WardController.longOf(body, "patientId"),
                WardController.longOf(body, "encounterId"),
                WardController.longOf(body, "doctorId"),
                WardController.strOf(body, "admissionType"),
                WardController.longOf(body, "unitId"),
                WardController.boolOf(body, "isMlc"),
                WardController.strOf(body, "mlcNumber"),
                WardController.strOf(body, "mlcRemarks"),
                WardController.longOf(body, "attendingDoctorId"),
                WardController.strOf(body, "remarks"),
                WardController.strOf(body, "tid"));
        return ResponseEntity.status(HttpStatus.CREATED).body(a);
    }

    @PostMapping("/{id}/attendant-pass")
    @Operation(summary = "Issue attendant pass (M03)",
            description = "Ported from T_ATTENDANTPASSDETAILS: passNo ATT/yy/seq, ISSUED, printCount=1. Requires active admission.")
    public ResponseEntity<com.hims.adt.domain.model.AttendantPass> issueAttendantPass(
            @PathVariable long id, @RequestBody Map<String, Object> body) {
        var p = service.issueAttendantPass(id,
                WardController.strOf(body, "attendeeName"),
                WardController.longOf(body, "passPrice"));
        return ResponseEntity.status(HttpStatus.CREATED).body(p);
    }

    @GetMapping("/{id}/attendant-passes")
    @Operation(summary = "List attendant passes for an admission (M03)")
    public java.util.List<com.hims.adt.domain.model.AttendantPass> attendantPasses(@PathVariable long id) {
        return service.attendantPasses(id);
    }

    @PostMapping("/{id}/tid")
    @Operation(summary = "Set TID for an admission (M03 TID list)")
    public Admission setTid(@PathVariable long id, @RequestBody Map<String, Object> body) {
        return service.setTid(id, WardController.strOf(body, "tid"));
    }

    /** Manual abscond (BR-DIS-001 deviation): cancels the stay and stamps abscondDatetime. */
    @PostMapping("/{id}/abscond")
    @Operation(summary = "Mark the admission as absconded",
            description = "Cancels the stay with dischargeType=ABSCOND and stamps abscond_datetime "
                    + "(bypasses the clearance gate — no stay left to settle). Emits PatientDischarged.")
    public Admission abscond(@PathVariable long id) {
        return service.abscond(id, null);
    }

    /** BR-DIS-001: multi-department discharge clearance checklist. */
    @GetMapping("/{id}/clearance")
    @Operation(summary = "Discharge clearance checklist",
            description = "NURSING/PHARMACY/BILLING/LAB rows; all must be CLEARED before discharge.")
    public List<ClearanceChecklistItem> clearance(@PathVariable long id) {
        return service.clearances(id);
    }

    @PostMapping("/{id}/clearance/{department}")
    @Operation(summary = "Clear one department",
            description = "Marks PENDING -> CLEARED for NURSING|PHARMACY|BILLING|LAB (idempotent).")
    public ClearanceChecklistItem clearDepartment(@PathVariable long id,
                                                  @PathVariable String department,
                                                  @RequestBody(required = false) Map<String, Object> body) {
        return service.clearDepartment(id, department,
                body == null ? null : WardController.longOf(body, "clearedBy"),
                body == null ? null : WardController.strOf(body, "remarks"));
    }

    /** Bed movement audit for an admission. */
    @GetMapping("/{id}/transfers")
    @Operation(summary = "Bed transfer history")
    public List<BedTransferLog> transfers(@PathVariable long id) {
        return service.transferHistory(id);
    }

    private static Timestamp timestampOf(String iso) {
        if (iso == null || iso.isBlank()) return null;
        try {
            return Timestamp.from(Instant.parse(iso));
        } catch (DateTimeParseException e) {
            throw new InvalidAdtRequestException("ADT_REQ_INVALID",
                    "'" + iso + "' is not an ISO-8601 instant");
        }
    }
}
