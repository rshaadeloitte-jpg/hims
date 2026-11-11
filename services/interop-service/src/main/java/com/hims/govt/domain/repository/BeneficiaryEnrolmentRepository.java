package com.hims.govt.domain.repository;

import com.hims.govt.domain.model.BeneficiaryEnrolment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BeneficiaryEnrolmentRepository extends JpaRepository<BeneficiaryEnrolment, Long> {

    Optional<BeneficiaryEnrolment> findByEnrolmentNo(String enrolmentNo);

    boolean existsByEnrolmentNo(String enrolmentNo);
}
