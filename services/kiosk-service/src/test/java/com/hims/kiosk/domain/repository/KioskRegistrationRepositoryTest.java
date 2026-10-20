package com.hims.kiosk.domain.repository;

import com.hims.kiosk.domain.model.KioskRegistration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.Optional;

@DataJpaTest
class KioskRegistrationRepositoryTest {
    @Autowired private KioskRegistrationRepository repo;

    @Test
    void saveAndFindByToken() {
        KioskRegistration r = new KioskRegistration();
        r.setId(99L); r.setPatientName("T"); r.setContactPhone("9"); r.setRegistrationToken("TK");
        r.setStatus(true);
        repo.save(r);
        Optional<KioskRegistration> found = repo.findByRegistrationToken("TK");
        assert found.isPresent();
    }
}
