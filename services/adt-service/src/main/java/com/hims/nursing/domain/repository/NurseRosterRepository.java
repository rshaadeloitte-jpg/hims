package com.hims.nursing.domain.repository;

import com.hims.nursing.domain.model.NurseRoster;
import org.springframework.data.jpa.repository.JpaRepository;

import java.sql.Date;
import java.util.List;

public interface NurseRosterRepository extends JpaRepository<NurseRoster, Long> {
    List<NurseRoster> findByRosterDateAndWardIdOrderByShiftAscIdAsc(Date rosterDate, Long wardId);
    List<NurseRoster> findByNurseIdAndRosterDateOrderByShiftAscIdAsc(Long nurseId, Date rosterDate);
    List<NurseRoster> findByRosterDateAndWardIdAndShift(Date rosterDate, Long wardId, String shift);
}
