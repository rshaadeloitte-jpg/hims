package com.hims.billing.api;

import com.hims.billing.application.DoctorShareService;
import com.hims.billing.application.DoctorShareService.DoctorShareCalculation;
import com.hims.billing.domain.model.DoctorShare;
import com.hims.billing.domain.model.DoctorIncentive;
import com.hims.billing.domain.model.VisitCharge;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * M06 Doctor Share REST API.
 * Endpoints:
 *   POST   /api/v1/doctor-share-calculations       — calculate share (percentage + fixed)
 *   GET    /api/v1/doctor-share-calculations/{billId} — get calculated shares for a bill
 *   PUT    /api/v1/doctor-share-add                — add doctor incentive (fixed amount)
 *   DELETE /api/v1/doctor-share-deduct             — deduct doctor incentive (fixed amount)
 *   GET    /api/v1/doctor-share/bills              — get doctor shares by doctor ID
 *   GET    /api/v1/doctor-share/visit-charges      — get visit charges by doctor ID
 */
@RestController
@RequestMapping("/api/v1")
public class DoctorShareController {

    private final DoctorShareService doctorShareService;

    public DoctorShareController(DoctorShareService doctorShareService) {
        this.doctorShareService = doctorShareService;
    }

    // ---- POST /api/v1/doctor-share-calculations ----
    @PostMapping("/doctor-share-calculations")
    public ResponseEntity<DoctorShareCalculation> calculateShare(
            @RequestBody ShareCalculationRequest request) {
        DoctorShareCalculation result = doctorShareService.calculateShare(
                request.totalAmount(), request.sharePercentage(), request.fixedAmount());
        return ResponseEntity.ok(result);
    }

    // ---- GET /api/v1/doctor-share-calculations/{billId} ----
    @GetMapping("/doctor-share-calculations/{billId}")
    public ResponseEntity<List<DoctorShareCalculation>> getCalculatedShares(@PathVariable Long billId) {
        return ResponseEntity.ok(doctorShareService.calculateSharesForBill(billId));
    }

    // ---- PUT /api/v1/doctor-share-add ----
    @PutMapping("/doctor-share-add")
    public ResponseEntity<DoctorIncentive> addIncentive(@RequestBody DoctorIncentive incentive) {
        return ResponseEntity.status(HttpStatus.CREATED).body(doctorShareService.addIncentive(incentive));
    }

    // ---- DELETE /api/v1/doctor-share-deduct ----
    @DeleteMapping("/doctor-share-deduct")
    public ResponseEntity<DoctorIncentive> deductIncentive(@RequestBody DoctorIncentive incentive) {
        return ResponseEntity.ok(doctorShareService.deductIncentive(incentive));
    }

    // ---- GET /api/v1/doctor-share/bills ----
    @GetMapping("/doctor-share/bills")
    public ResponseEntity<List<DoctorShare>> getBillsByDoctorId(@RequestParam Long doctorId) {
        return ResponseEntity.ok(doctorShareService.getBillsByDoctorId(doctorId));
    }

    // ---- GET /api/v1/doctor-share/visit-charges ----
    @GetMapping("/doctor-share/visit-charges")
    public ResponseEntity<List<VisitCharge>> getVisitChargesByDoctorId(@RequestParam Long doctorId) {
        return ResponseEntity.ok(doctorShareService.getVisitChargesByDoctorId(doctorId));
    }

    // ---- Additional endpoints for completeness ----

    @GetMapping("/doctor-shares")
    public Page<DoctorShare> listShares(@RequestParam(defaultValue = "0") int page,
                                         @RequestParam(defaultValue = "20") int size) {
        return doctorShareService.listShares(page, size);
    }

    @GetMapping("/doctor-shares/{id}")
    public DoctorShare getShare(@PathVariable Long id) {
        return doctorShareService.getShare(id);
    }

    @PostMapping("/doctor-shares")
    public ResponseEntity<DoctorShare> createShare(@RequestBody DoctorShare share) {
        return ResponseEntity.status(HttpStatus.CREATED).body(doctorShareService.createShare(share));
    }

    @PatchMapping("/doctor-shares/{id}/payout")
    public DoctorShare markPaid(@PathVariable Long id) {
        return doctorShareService.markPaid(id);
    }

    @GetMapping("/doctor-shares/bill/{billId}")
    public List<DoctorShare> getSharesByBillId(@PathVariable Long billId) {
        return doctorShareService.getSharesByBillId(billId);
    }

    @GetMapping("/visit-charges")
    public List<VisitCharge> getVisitCharges(@RequestParam Long doctorId) {
        return doctorShareService.getVisitChargesByDoctorId(doctorId);
    }

    @GetMapping("/visit-charges/{id}")
    public VisitCharge getVisitCharge(@PathVariable Long id) {
        return doctorShareService.getVisitCharge(id);
    }

    @PostMapping("/visit-charges")
    public ResponseEntity<VisitCharge> createVisitCharge(@RequestBody VisitCharge visitCharge) {
        return ResponseEntity.status(HttpStatus.CREATED).body(doctorShareService.createVisitCharge(visitCharge));
    }

    @DeleteMapping("/visit-charges/{id}")
    public ResponseEntity<Void> deleteVisitCharge(@PathVariable Long id) {
        doctorShareService.deleteVisitCharge(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/doctor-incentives")
    public List<DoctorIncentive> getIncentives(@RequestParam Long doctorId) {
        return doctorShareService.getIncentivesByDoctorId(doctorId);
    }

    @GetMapping("/doctor-incentives/unbilled")
    public List<DoctorIncentive> getUnbilledIncentives(@RequestParam Long doctorId) {
        return doctorShareService.getUnbilledIncentivesByDoctorId(doctorId);
    }

    @PatchMapping("/doctor-incentives/{id}/bill")
    public DoctorIncentive markIncentiveBilled(@PathVariable Long id) {
        return doctorShareService.markIncentiveBilled(id);
    }

    // ---- Request DTO ----
    public record ShareCalculationRequest(
            java.math.BigDecimal totalAmount,
            java.math.BigDecimal sharePercentage,
            java.math.BigDecimal fixedAmount
    ) {}
}
