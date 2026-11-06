package com.hims.reporting.api.reports; import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/reports/janta")
public class JantaClinicReportsController {
  @GetMapping("/rural-health-camp") public String r() { return "rural_health_camp"; }
  @GetMapping("/free-medicine") public String f() { return "free_medicine"; }
  @GetMapping("/vaccination-drive") public String v() { return "vaccination_drive"; }
  @GetMapping("/health-screening") public String h() { return "health_screening_camp"; }
  @GetMapping("/community-outreach") public String c() { return "community_outreach"; }
  @GetMapping("/preventive-care") public String p() { return "preventive_care"; }
}
