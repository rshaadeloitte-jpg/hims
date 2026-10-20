package com.hims.bcma.domain.repository;

import com.hims.bcma.domain.model.IVPEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@DataJpaTest
class IVPEventRepositoryTest {

    @Autowired private IVPEventRepository repo;

    @Test
    void saveAndFindByIvpId() {
        IVPEvent ev = new IVPEvent(); ev.setIvpId(200L); ev.setPatientId(1L); ev.setDrugName("D"); ev.setStartTime(Timestamp.from(Instant.now())); ev.setNurseId(5L);
        repo.save(ev);
        Optional<IVPEvent> found = repo.findByIvpId(200L);
        assertTrue(found.isPresent());
        assertEquals("D", found.get().getDrugName());
    }

    @Test
    void findByNurseId() {
        IVPEvent ev = new IVPEvent(); ev.setIvpId(201L); ev.setPatientId(2L); ev.setDrugName("D"); ev.setStartTime(Timestamp.from(Instant.now())); ev.setNurseId(7L);
        repo.save(ev);
        assertEquals(1, repo.findByNurseId(7L).size());
    }

    @Test
    void findByWardId() {
        IVPEvent ev = new IVPEvent(); ev.setIvpId(202L); ev.setPatientId(3L); ev.setDrugName("D"); ev.setStartTime(Timestamp.from(Instant.now())); ev.setNurseId(1L); ev.setWardId(9L);
        repo.save(ev);
        assertEquals(1, repo.findByWardId(9L).size());
    }
}
