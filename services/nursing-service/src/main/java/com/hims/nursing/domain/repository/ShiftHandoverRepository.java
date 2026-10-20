package com.hims.nursing.domain.repository;

import com.hims.nursing.domain.model.ShiftHandover;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.sql.Timestamp;

public interface ShiftHandoverRepository extends JpaRepository<ShiftHandover, Long> {
    List<ShiftHandover> findByAdmissionIdOrderByAddedDatetimeDesc(Long admissionId);
    List<ShiftHandover> findByWardIdAndShiftOrderByAddedDatetimeDesc(Long wardId, String shift);
    List<ShiftHandover> findByWardIdAndShiftAndAddedDatetimeBetween(Long wardId, String shift, Timestamp from, Timestamp to);
}
