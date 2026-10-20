package com.hims.aasmaan.domain.repository;
import com.hims.aasmaan.domain.model.AasmaanCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
public class AasmaanCaseRepositoryTest {
    @Autowired AasmaanCaseRepository repo;
    @Test
    void save_and_find() {
        AasmaanCase c = new AasmaanCase(); c.setPatientId(1L); c.setCaseType("ANC"); c.setStatus((short)1); c.setOpenedOn(LocalDate.now());
        repo.save(c);
        assertNotNull(c.getCaseId());
    }
}
