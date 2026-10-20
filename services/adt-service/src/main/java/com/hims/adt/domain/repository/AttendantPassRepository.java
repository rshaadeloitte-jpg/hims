package com.hims.adt.domain.repository;

import com.hims.adt.domain.model.AttendantPass;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AttendantPassRepository extends JpaRepository<AttendantPass, Long> {
    List<AttendantPass> findByAdmissionIdOrderByPassIdAsc(Long admissionId);
    List<AttendantPass> findByPatientIdOrderByPassIdDesc(Long patientId);
}
