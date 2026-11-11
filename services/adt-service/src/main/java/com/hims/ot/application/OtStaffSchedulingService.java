package com.hims.ot.application;

import com.hims.ot.application.exceptions.OtRuleException;
import com.hims.ot.domain.model.*;
import com.hims.ot.domain.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

@Service
public class OtStaffSchedulingService {

    private final OtStaffScheduleRepository staffRepo;
    private final OtRoomRepository roomRepo;

    public OtStaffSchedulingService(OtStaffScheduleRepository staffRepo, OtRoomRepository roomRepo) {
        this.staffRepo = staffRepo;
        this.roomRepo = roomRepo;
    }

    public OtStaffSchedule assignStaff(Long scheduleId, String role, Long staffId, Timestamp start, Timestamp end) {
        // Conflict detection: same staff overlapping
        List<OtStaffSchedule> existing = staffRepo.findByStaffIdAndAssignedStartBetween(staffId, start, end);
        for (OtStaffSchedule s : existing) {
            if (s.getScheduleId() == null || !s.getScheduleId().equals(scheduleId)) {
                if (start.before(s.getAssignedEnd()) && end.after(s.getAssignedStart())) {
                    throw new OtRuleException("CONFLICT", "Staff double-booked");
                }
            }
        }
        OtStaffSchedule oss = new OtStaffSchedule();
        oss.setScheduleId(scheduleId);
        oss.setStaffRole(role);
        oss.setStaffId(staffId);
        oss.setAssignedStart(start);
        oss.setAssignedEnd(end);
        oss.setAddedBy(1L);
        oss.setAddedDatetime(Timestamp.from(Instant.now()));
        return staffRepo.save(oss);
    }

    public OtRoom createRoom(String name, String type) {
        OtRoom r = new OtRoom();
        r.setRoomName(name);
        r.setRoomType(type);
        r.setIsActive(true);
        r.setAddedBy(1L);
        r.setAddedDatetime(Timestamp.from(Instant.now()));
        return roomRepo.save(r);
    }

    public List<OtStaffSchedule> findBySchedule(Long scheduleId) {
        return staffRepo.findByScheduleId(scheduleId);
    }
}
