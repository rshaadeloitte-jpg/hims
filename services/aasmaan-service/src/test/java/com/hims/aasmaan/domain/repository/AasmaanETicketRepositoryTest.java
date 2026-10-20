package com.hims.aasmaan.domain.repository;
import com.hims.aasmaan.domain.model.AasmaanETicket;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
public class AasmaanETicketRepositoryTest {
    @Autowired AasmaanETicketRepository repo;
    @Test
    void save_ticket() {
        AasmaanETicket t = new AasmaanETicket(); t.setCaseId(1L); t.setSubject("Issue"); t.setPriority("HIGH");
        repo.save(t);
        assertNotNull(t.getTicketId());
    }
}
