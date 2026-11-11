package com.hims.aasmaan.application;
import com.hims.aasmaan.domain.model.AasmaanMonitor;
import com.hims.aasmaan.domain.repository.AasmaanMonitorRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.default_schema=public",
        "spring.jpa.properties.hibernate.hbm2ddl.create_namespaces=true"
})
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
