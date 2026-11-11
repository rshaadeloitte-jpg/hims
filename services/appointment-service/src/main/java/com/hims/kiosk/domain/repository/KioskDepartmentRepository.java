package com.hims.kiosk.domain.repository;

import com.hims.kiosk.domain.model.KioskDepartment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface KioskDepartmentRepository extends JpaRepository<KioskDepartment, Long> {
    List<KioskDepartment> findByActiveTrueOrderByNameAsc();
}
