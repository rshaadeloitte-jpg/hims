package com.hims.aasmaan.domain.repository;
import com.hims.aasmaan.domain.model.AasmaanETicket;
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
public class AasmaanETicketRepositoryTest {
    @Autowired AasmaanETicketRepository repo;
    @Test
    void save_ticket() {
        AasmaanETicket t = new AasmaanETicket(); t.setCaseId(1L); t.setSubject("Issue"); t.setPriority("HIGH");
        repo.save(t);
        assertNotNull(t.getTicketId());
    }
}
