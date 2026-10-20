package com.hims.aasmaan.domain.repository;
import com.hims.aasmaan.domain.model.AasmaanMonitor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
public class AasmaanMonitorRepositoryTest {
    @Autowired AasmaanMonitorRepository repo;
    @Test
    void save_monitor() {
        AasmaanMonitor m = new AasmaanMonitor(); m.setCaseId(1L); m.setMetric("temp"); m.setValue(98.6);
        repo.save(m);
        assertNotNull(m.getMonitorId());
    }
}
