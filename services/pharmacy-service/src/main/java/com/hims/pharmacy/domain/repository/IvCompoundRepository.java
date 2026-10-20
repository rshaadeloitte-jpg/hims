package com.hims.pharmacy.domain.repository;

import com.hims.pharmacy.domain.model.IvCompound;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface IvCompoundRepository extends JpaRepository<IvCompound, Long> {

    @Query("SELECT c FROM IvCompound c WHERE c.patientId = :patientId ORDER BY c.orderDate DESC")
    List<IvCompound> findByPatientId(@Param("patientId") Long patientId);

    @Query("SELECT c FROM IvCompound c WHERE c.statusCode = :status ORDER BY c.orderDate DESC")
    List<IvCompound> findByStatus(@Param("status") Short status);
}
