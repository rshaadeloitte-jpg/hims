package com.hims.aasmaan.application;
import com.hims.aasmaan.domain.model.AasmaanRscCallLog;
import com.hims.aasmaan.domain.repository.AasmaanRscCallLogRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
public class AasmaanRscCallLogServiceTest {
    @Autowired AasmaanRscCallLogRepository repo;
    @Test
    void log_rsc_service() {
        AasmaanRscCallLogService svc = new AasmaanRscCallLogService(repo);
        AasmaanRscCallLog r = new AasmaanRscCallLog(); r.setCaseId(1L); r.setNotes("Called RSC");
        AasmaanRscCallLog s = svc.log(r);
        assertNotNull(s.getLogId());
    }
}
