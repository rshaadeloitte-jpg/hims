package com.hims.rcm.domain.repository;

import com.hims.rcm.domain.model.DischargeVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface DischargeVerificationRepository extends JpaRepository<DischargeVerification, Long> {
    List<DischargeVerification> findByAdmissionIdOrderByIdDesc(Long admissionId);
    List<DischargeVerification> findByDocStatusOrderByIdDesc(Short docStatus);
    @Query(value = "SELECT nextval('ihms_rcm.t_discharge_verification_seq')", nativeQuery = true)
    long nextId();
}
