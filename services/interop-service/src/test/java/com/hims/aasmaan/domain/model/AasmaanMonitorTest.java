package com.hims.aasmaan.domain.model;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class AasmaanMonitorTest {
    @Test
    void monitor_set_metric() {
        AasmaanMonitor m = new AasmaanMonitor();
        m.setMetric("bp");
        assertEquals("bp", m.getMetric());
    }
}
