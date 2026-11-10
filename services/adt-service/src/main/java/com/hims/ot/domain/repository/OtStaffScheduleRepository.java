package com.hims.ot.domain.repository;

import com.hims.ot.domain.model.OtStaffSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OtStaffScheduleRepository extends JpaRepository<OtStaffSchedule, Long> {
    List<OtStaffSchedule> findByScheduleId(Long scheduleId);
    List<OtStaffSchedule> findByStaffIdAndAssignedStartBetween(Long staffId, java.sql.Timestamp start, java.sql.Timestamp end);
}
