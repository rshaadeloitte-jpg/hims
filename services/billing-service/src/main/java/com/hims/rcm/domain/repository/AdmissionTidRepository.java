package com.hims.rcm.domain.repository;

import com.hims.rcm.domain.model.AdmissionTid;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdmissionTidRepository extends JpaRepository<AdmissionTid, Long> {

    java.util.Optional<AdmissionTid> findByAdmissionId(Long admissionId);

    boolean existsByAdmissionId(Long admissionId);
}
