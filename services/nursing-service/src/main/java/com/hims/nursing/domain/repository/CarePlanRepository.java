package com.hims.nursing.domain.repository;

import com.hims.nursing.domain.model.CarePlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CarePlanRepository extends JpaRepository<CarePlan, Long> {
    List<CarePlan> findByAdmissionIdOrderByAddedDatetimeDesc(Long admissionId);
    List<CarePlan> findByAdmissionIdAndStatusCode(Long admissionId, Short statusCode);
}
