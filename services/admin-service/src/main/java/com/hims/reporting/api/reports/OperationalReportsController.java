package com.hims.reporting.api.reports;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/reports/operational")
public class OperationalReportsController {
  @GetMapping("/daily-patient-census") public String dailyCensus() { return "daily_patient_census"; }
  @GetMapping("/opd-flow") public String opdFlow() { return "opd_patient_flow"; }
  @GetMapping("/ipd-admission") public String ipdAdm() { return "ipd_admission_discharge"; }
  @GetMapping("/ot-utilization") public String otUtil() { return "ot_utilization"; }
  @GetMapping("/emergency-wait") public String emerg() { return "emergency_wait_time"; }
  @GetMapping("/pharmacy-stock") public String pharm() { return "pharmacy_stock"; }
  @GetMapping("/lab-turnaround") public String lab() { return "lab_turnaround"; }
  @GetMapping("/billing-collection") public String bill() { return "billing_collection"; }
  @GetMapping("/insurance-claims") public String ins() { return "insurance_claim_status"; }
  @GetMapping("/consultation-volume") public String doc() { return "doctor_consultation_volume"; }
  @GetMapping("/nurse-patient-ratio") public String nurse() { return "nurse_patient_ratio"; }
  @GetMapping("/bed-occupancy") public String bed() { return "bed_occupancy"; }
  @GetMapping("/surgery-cancellation") public String surg() { return "surgery_cancellation"; }
  @GetMapping("/medication-admin") public String med() { return "medication_admin"; }
  @GetMapping("/infection-control") public String inf() { return "infection_control"; }
}
