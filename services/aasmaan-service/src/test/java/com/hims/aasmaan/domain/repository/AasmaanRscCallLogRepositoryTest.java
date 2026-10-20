package com.hims.aasmaan.domain.repository;
import com.hims.aasmaan.domain.model.AasmaanRscCallLog;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
public class AasmaanRscCallLogRepositoryTest {
    @Autowired AasmaanRscCallLogRepository repo;
    @Test
    void save_rsc() {
        AasmaanRscCallLog r = new AasmaanRscCallLog(); r.setCaseId(1L); r.setCallRef("RSC-01");
        repo.save(r);
        assertNotNull(r.getLogId());
    }
}
