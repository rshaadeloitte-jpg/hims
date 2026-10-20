package com.hims.ordering.domain.repository;

import com.hims.ordering.domain.model.PrescriptionItem;
import com.hims.ordering.domain.model.PrescriptionItemKey;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PrescriptionItemRepository extends JpaRepository<PrescriptionItem, PrescriptionItemKey> {
    List<PrescriptionItem> findByPrescriptionIdOrderByLineNoAsc(Long prescriptionId);
}
