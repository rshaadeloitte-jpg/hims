package com.hims.aasmaan.application;
import com.hims.aasmaan.domain.model.AasmaanChecklist;
import com.hims.aasmaan.domain.repository.AasmaanChecklistRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
public class AasmaanChecklistServiceTest {
    @Autowired AasmaanChecklistRepository repo;
    @Test
    void save_checklist_service() {
        AasmaanChecklistService svc = new AasmaanChecklistService(repo);
        AasmaanChecklist ch = new AasmaanChecklist(); ch.setCaseId(1L); ch.setCategory("Vitals"); ch.setCompleted(true);
        AasmaanChecklist s = svc.save(ch);
        assertNotNull(s.getChecklistId());
    }
}
