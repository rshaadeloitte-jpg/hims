package com.hims.kiosk.domain.repository;

import com.hims.kiosk.domain.model.KioskLabResult;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface KioskLabResultRepository extends JpaRepository<KioskLabResult, Long> {
    List<KioskLabResult> findByPatientIdAndStatusTrue(Long patientId);
}
