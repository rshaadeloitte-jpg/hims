package com.hims.aasmaan.application;
import com.hims.aasmaan.domain.model.AasmaanMonitor;
import com.hims.aasmaan.domain.repository.AasmaanMonitorRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
public class AasmaanMonitorServiceTest {
    @Autowired AasmaanMonitorRepository repo;
    @Test
    void record_monitor() {
        AasmaanMonitorService svc = new AasmaanMonitorService(repo);
        AasmaanMonitor m = new AasmaanMonitor(); m.setCaseId(1L); m.setMetric("temp"); m.setValue(99.0);
        AasmaanMonitor s = svc.record(m);
        assertNotNull(s.getMonitorId());
    }
}
