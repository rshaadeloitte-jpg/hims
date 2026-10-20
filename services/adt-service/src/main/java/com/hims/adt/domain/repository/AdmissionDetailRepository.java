package com.hims.adt.domain.repository;

import com.hims.adt.domain.model.AdmissionDetail;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AdmissionDetailRepository extends JpaRepository<AdmissionDetail, Long> {
    Optional<AdmissionDetail> findByAdmissionId(Long admissionId);
}
