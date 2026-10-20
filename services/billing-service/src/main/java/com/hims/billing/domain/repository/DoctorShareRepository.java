package com.hims.billing.domain.repository;

import com.hims.billing.domain.model.DoctorShare;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DoctorShareRepository extends JpaRepository<DoctorShare, Long> {
    List<DoctorShare> findByBillIdOrderByShareIdAsc(Long billId);
    List<DoctorShare> findByDoctorIdOrderByShareIdAsc(Long doctorId);
}
