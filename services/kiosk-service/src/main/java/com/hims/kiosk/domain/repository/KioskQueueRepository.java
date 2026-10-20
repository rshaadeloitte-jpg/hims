package com.hims.kiosk.domain.repository;

import com.hims.kiosk.domain.model.KioskQueue;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;

public interface KioskQueueRepository extends JpaRepository<KioskQueue, Long> {
    List<KioskQueue> findByQueueDateAndStatusTrueAndQueueStatus(LocalDate date, Short status);
    List<KioskQueue> findByPatientIdAndStatusTrue(Long patientId);
    long countByQueueDateAndStatusTrueAndQueueStatus(LocalDate date, Short queueStatus);
}
