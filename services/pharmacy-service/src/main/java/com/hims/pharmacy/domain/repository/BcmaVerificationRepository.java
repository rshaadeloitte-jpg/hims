package com.hims.pharmacy.domain.repository;

import com.hims.pharmacy.domain.model.BcmaVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface BcmaVerificationRepository extends JpaRepository<BcmaVerification, Long> {

    @Query("SELECT b FROM BcmaVerification b WHERE b.patientId = :patientId ORDER BY b.scanTime DESC")
    List<BcmaVerification> findByPatientId(@Param("patientId") Long patientId);

    @Query("SELECT b FROM BcmaVerification b WHERE b.saleId = :saleId ORDER BY b.scanTime DESC")
    List<BcmaVerification> findBySaleId(@Param("saleId") Long saleId);

    @Query("SELECT b FROM BcmaVerification b WHERE b.administered = false AND b.allRightsPassed = false ORDER BY b.scanTime DESC")
    List<BcmaVerification> findPendingOverrides();
}
