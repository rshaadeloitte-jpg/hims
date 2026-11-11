package com.hims.ot.application;

import com.hims.ot.domain.model.OtNurseSchedule;
import com.hims.ot.domain.repository.OtNurseScheduleRepository;
import org.springframework.stereotype.Service;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

@Service
public class OtNurseScheduleService {

    private final OtNurseScheduleRepository nurseRepo;

    public OtNurseScheduleService(OtNurseScheduleRepository nurseRepo) {
        this.nurseRepo = nurseRepo;
    }

    public OtNurseSchedule createDuty(Long staffId, Date dutyDate, String shiftType, String roster, Boolean emergencyCall) {
        OtNurseSchedule ns = new OtNurseSchedule();
        ns.setStaffId(staffId);
        ns.setDutyDate(dutyDate);
        ns.setShiftType(shiftType);
        ns.setDutyRoster(roster);
        ns.setEmergencyCall(emergencyCall != null ? emergencyCall : false);
        ns.setAddedBy(1L);
        ns.setAddedDatetime(Timestamp.from(Instant.now()));
        return nurseRepo.save(ns);
    }

    public List<OtNurseSchedule> findByStaff(Long staffId) {
        return nurseRepo.findByStaffId(staffId);
    }

    public List<OtNurseSchedule> findByDate(Date date) {
        return nurseRepo.findByDutyDate(date);
    }
}
