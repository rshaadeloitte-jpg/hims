package com.hims.aasmaan.application;
import com.hims.aasmaan.domain.model.AasmaanChecklist;
import com.hims.aasmaan.domain.repository.AasmaanChecklistRepository;
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
