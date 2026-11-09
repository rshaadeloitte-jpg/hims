package com.hims.kiosk.domain.repository;

import com.hims.kiosk.domain.model.KioskDepartment;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class KioskDepartmentRepositoryTest {

    @Autowired
    private KioskDepartmentRepository repo;

    @Test
    void findByActiveTrueOrderByNameAsc() {
        repo.save(new KioskDepartment(1L, "Pediatrics", true));
        repo.save(new KioskDepartment(2L, "Cardiology", true));
        repo.save(new KioskDepartment(3L, "Radiology", false)); // inactive
        repo.save(new KioskDepartment(4L, "Anesthesiology", true));

        List<KioskDepartment> active = repo.findByActiveTrueOrderByNameAsc();
        assertThat(active).hasSize(3);
        assertThat(active.get(0).getName()).isEqualTo("Anesthesiology");
        assertThat(active.get(1).getName()).isEqualTo("Cardiology");
        assertThat(active.get(2).getName()).isEqualTo("Pediatrics");
    }
}
