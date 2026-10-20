package com.hims.adt.domain.repository;

import com.hims.adt.domain.model.ClearanceChecklistItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ClearanceChecklistRepository extends JpaRepository<ClearanceChecklistItem, Long> {

    List<ClearanceChecklistItem> findByAdmissionIdOrderByClearanceId(Long admissionId);

    Optional<ClearanceChecklistItem> findByAdmissionIdAndDepartment(
            Long admissionId, com.hims.adt.domain.model.ClearanceDepartment department);

    long countByAdmissionIdAndClrStatus(Long admissionId, ClearanceChecklistItem.ClrStatus status);
}
