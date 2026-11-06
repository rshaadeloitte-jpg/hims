package com.hims.reporting.api.reports; import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/reports/statistical")
public class StatisticalReportsController {
  @GetMapping("/disease-prevalence") public String d() { return "disease_prevalence"; }
  @GetMapping("/readmission-rate") public String r() { return "readmission_rate"; }
  @GetMapping("/mortality-rate") public String m() { return "mortality_rate"; }
  @GetMapping("/avg-length-of-stay") public String a() { return "avg_length_of_stay"; }
  @GetMapping("/satisfaction") public String s() { return "patient_satisfaction"; }
  @GetMapping("/revenue-service-line") public String rev() { return "revenue_by_service_line"; }
  @GetMapping("/cost-per-patient") public String c() { return "cost_per_patient"; }
  @GetMapping("/resource-efficiency") public String res() { return "resource_utilization"; }
  @GetMapping("/staff-productivity") public String st() { return "staff_productivity"; }
  @GetMapping("/equipment-utilization") public String eq() { return "equipment_utilization"; }
}
