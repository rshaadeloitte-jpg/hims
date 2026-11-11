package com.hims.aasmaan.domain.repository;
import com.hims.aasmaan.domain.model.AasmaanChecklist;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
public class AasmaanChecklistRepositoryTest {
    @Autowired AasmaanChecklistRepository repo;
    @Test
    void save_checklist() {
        AasmaanChecklist ch = new AasmaanChecklist(); ch.setCaseId(1L); ch.setCategory("Vitals"); ch.setCompleted(true);
        repo.save(ch);
        assertNotNull(ch.getChecklistId());
    }
}
