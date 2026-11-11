package com.hims.ot.domain.repository;

import com.hims.ot.domain.model.OtNurseSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OtNurseScheduleRepository extends JpaRepository<OtNurseSchedule, Long> {
    List<OtNurseSchedule> findByStaffId(Long staffId);
    List<OtNurseSchedule> findByDutyDate(java.sql.Date dutyDate);
}
