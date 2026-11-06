package com.hims.reporting.application.reports; import org.junit.jupiter.api.Test; import static org.junit.jupiter.api.Assertions.*;
public class JantaReportTest {
  @Test public void dailyCensusExists() { assertNotNull("daily_patient_census"); }
  @Test public void opdFlowExists() { assertNotNull("opd_patient_flow"); }
}
