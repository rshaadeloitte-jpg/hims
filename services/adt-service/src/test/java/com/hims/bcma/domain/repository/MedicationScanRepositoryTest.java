package com.hims.bcma.domain.repository;

import com.hims.bcma.domain.model.MedicationScan;
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
class MedicationScanRepositoryTest {

    @Autowired private MedicationScanRepository repo;

    @Test
    void saveAndFindByScanId() {
        MedicationScan s = new MedicationScan();
        s.setScanId(100L); s.setPatientId(1L); s.setDrugIen("D1"); s.setScanTime(Timestamp.from(Instant.now()));
        repo.save(s);
        Optional<MedicationScan> found = repo.findByScanId(100L);
        assertTrue(found.isPresent());
        assertEquals(1L, found.get().getPatientId());
    }

    @Test
    void findByPatientIdAndDrugIen() {
        MedicationScan s = new MedicationScan(); s.setScanId(101L); s.setPatientId(2L); s.setDrugIen("D2"); s.setScanTime(Timestamp.from(Instant.now()));
        repo.save(s);
        assertEquals(1, repo.findByPatientIdAndDrugIen(2L, "D2").size());
    }

    @Test
    void findByPatientIdReturnsResults() {
        MedicationScan s = new MedicationScan(); s.setScanId(102L); s.setPatientId(3L); s.setDrugIen("D3"); s.setScanTime(Timestamp.from(Instant.now()));
        repo.save(s);
        assertFalse(repo.findByPatientId(3L).isEmpty());
    }

    @Test
    void findByPatientIdWithPageableOrdersNewestFirst() {
        Instant now = Instant.now();
        MedicationScan older = new MedicationScan();
        older.setScanId(201L); older.setPatientId(99L); older.setDrugIen("D1");
        older.setScanTime(Timestamp.from(now.minusSeconds(60)));
        repo.save(older);

        MedicationScan newer = new MedicationScan();
        newer.setScanId(202L); newer.setPatientId(99L); newer.setDrugIen("D2");
        newer.setScanTime(Timestamp.from(now));
        repo.save(newer);

        org.springframework.data.domain.Pageable p = org.springframework.data.domain.PageRequest.of(
                0, 10, org.springframework.data.domain.Sort.by(
                        org.springframework.data.domain.Sort.Order.desc("scanTime"),
                        org.springframework.data.domain.Sort.Order.desc("scanId")));
        java.util.List<MedicationScan> list = repo.findByPatientId(99L, p);
        assertEquals(2, list.size());
        assertEquals(202L, list.get(0).getScanId());
        assertEquals(201L, list.get(1).getScanId());
    }
}
