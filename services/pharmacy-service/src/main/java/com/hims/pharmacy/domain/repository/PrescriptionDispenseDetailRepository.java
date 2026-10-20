package com.hims.pharmacy.domain.repository;

import com.hims.pharmacy.domain.model.PrescriptionDispenseDetail;
import com.hims.pharmacy.domain.model.PrescriptionDispenseDetailKey;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PrescriptionDispenseDetailRepository
        extends JpaRepository<PrescriptionDispenseDetail, PrescriptionDispenseDetailKey> {

    List<PrescriptionDispenseDetail> findByDispenseIdOrderByLineNoAsc(Long dispenseId);
}
