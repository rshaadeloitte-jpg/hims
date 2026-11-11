package com.hims.aasmaan.application;
import com.hims.aasmaan.domain.model.AasmaanETicket;
import com.hims.aasmaan.domain.repository.AasmaanETicketRepository;
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
public class AasmaanETicketServiceTest {
    @Autowired AasmaanETicketRepository repo;
    @Test
    void create_eticket_service() {
        AasmaanETicketService svc = new AasmaanETicketService(repo);
        AasmaanETicket t = new AasmaanETicket(); t.setCaseId(1L); t.setSubject("Bug");
        AasmaanETicket s = svc.create(t);
        assertEquals("OPEN", s.getStatus());
    }
}
