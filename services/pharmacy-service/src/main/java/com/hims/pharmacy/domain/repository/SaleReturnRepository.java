package com.hims.pharmacy.domain.repository;

import com.hims.pharmacy.domain.model.SaleReturn;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SaleReturnRepository extends JpaRepository<SaleReturn, Long> {
    List<SaleReturn> findByPatientIdOrderByAddedDatetimeDesc(Long patientId);
    List<SaleReturn> findByOriginalSaleId(Long originalSaleId);
    List<SaleReturn> findByStatusCodeOrderByAddedDatetimeDesc(Short statusCode);
    List<SaleReturn> findByStatusCodeAndPatientIdOrderByAddedDatetimeDesc(Short statusCode, Long patientId);
}
