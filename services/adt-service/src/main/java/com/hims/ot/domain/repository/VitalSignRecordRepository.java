package com.hims.ot.domain.repository;

import com.hims.ot.domain.model.VitalSignRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VitalSignRecordRepository extends JpaRepository<VitalSignRecord, Long> {
    List<VitalSignRecord> findByRecordId(Long recordId);
}
