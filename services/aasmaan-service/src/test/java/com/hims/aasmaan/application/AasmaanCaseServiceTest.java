package com.hims.aasmaan.application;
import com.hims.aasmaan.domain.model.AasmaanCase;
import com.hims.aasmaan.domain.repository.AasmaanCaseRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
public class AasmaanCaseServiceTest {
    @Autowired AasmaanCaseRepository repo;
    @Test
    void create_case_service() {
        AasmaanCaseService svc = new AasmaanCaseService(repo);
        AasmaanCase c = new AasmaanCase(); c.setPatientId(1L); c.setCaseType("ANC");
        AasmaanCase saved = svc.create(c);
        assertNotNull(saved.getCaseId());
        assertEquals((short)1, saved.getStatus());
    }
    @Test
    void create_missing_patient_throws() {
        AasmaanCaseService svc = new AasmaanCaseService(repo);
        AasmaanCase c = new AasmaanCase(); c.setCaseType("ANC");
        assertThrows(IllegalArgumentException.class, () -> svc.create(c));
    }
}
