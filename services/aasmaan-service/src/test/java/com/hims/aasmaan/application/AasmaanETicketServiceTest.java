package com.hims.aasmaan.application;
import com.hims.aasmaan.domain.model.AasmaanETicket;
import com.hims.aasmaan.domain.repository.AasmaanETicketRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
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
