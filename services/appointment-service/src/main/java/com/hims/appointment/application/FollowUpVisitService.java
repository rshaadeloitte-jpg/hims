package com.hims.appointment.application;

import com.hims.appointment.application.exceptions.AppointmentNotFoundException;
import com.hims.appointment.application.exceptions.InvalidAppointmentRequestException;
import com.hims.appointment.domain.model.Appointment;
import com.hims.appointment.domain.model.FollowUpVisit;
import com.hims.appointment.domain.repository.AppointmentRepository;
import com.hims.appointment.domain.repository.FollowUpVisitRepository;
import com.hims.appointment.infrastructure.persistence.SequenceGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Follow-up visit scheduling (legacy PatientRegApptServiceImpl:3362 parity).
 */
@Service
public class FollowUpVisitService {

    private static final Logger log = LoggerFactory.getLogger(FollowUpVisitService.class);

    private final FollowUpVisitRepository followUpRepository;
    private final AppointmentRepository appointmentRepository;
    private final SequenceGateway sequences;

    public FollowUpVisitService(FollowUpVisitRepository followUpRepository,
                                AppointmentRepository appointmentRepository,
                                SequenceGateway sequences) {
        this.followUpRepository = followUpRepository;
        this.appointmentRepository = appointmentRepository;
        this.sequences = sequences;
    }

    /**
     * Schedule a follow-up visit linked to a parent appointment.
     */
    @Transactional
    public FollowUpVisit scheduleFollowUp(Long parentAppointmentId, Long patientId, Long doctorId,
                                          Long departmentId, Long unitId, LocalDate followUpDate,
                                          LocalTime slotStart, LocalTime slotEnd, Long scheduleId,
                                          String cabin, String recurrenceRule, LocalDate recurrenceEndDate) {
        if (parentAppointmentId == null || patientId == null || followUpDate == null) {
            throw new InvalidAppointmentRequestException("BEXA003", "BEXA003: parentAppointmentId, patientId, followUpDate are required");
        }
        // Validate parent appointment exists
        Appointment parent = appointmentRepository.findById(parentAppointmentId)
                .filter(a -> Boolean.TRUE.equals(a.getStatus()))
                .orElseThrow(() -> new AppointmentNotFoundException("NOT_FOUND",
                        "Parent appointment not found: " + parentAppointmentId));

        FollowUpVisit f = new FollowUpVisit();
        f.setId(sequences.next("ihms_appointment.t_follow_up_visit_seq"));
        f.setParentAppointmentId(parentAppointmentId);
        f.setPatientId(patientId);
        f.setDoctorId(doctorId != null ? doctorId : parent.getDoctorId());
        f.setDepartmentId(departmentId != null ? departmentId : parent.getDepartmentId());
        f.setClinicId(parent.getClinicId());
        f.setUnitId(unitId != null ? unitId : parent.getUnitId());
        f.setFollowUpDate(followUpDate);
        f.setSlotStart(slotStart);
        f.setSlotEnd(slotEnd);
        f.setScheduleId(scheduleId);
        f.setCabin(cabin);
        f.setRecurrenceRule(recurrenceRule);
        f.setRecurrenceEndDate(recurrenceEndDate);
        f.setFollowUpStatus((short) 1); // SCHEDULED
        f.setIsCancelled(false);
        f.setStatus(true);
        f.setSynchronizedd(false);
        f.setAddedBy(AppointmentService.AUDIT_ACTOR_ID);
        f.setAddedDatetime(Timestamp.from(Instant.now()));
        f.setUpdatedBy(AppointmentService.AUDIT_ACTOR_ID);
        f.setUpdatedDatetime(Timestamp.from(Instant.now()));

        FollowUpVisit saved = followUpRepository.save(f);
        log.info("Scheduled follow-up id={} parent={} patient={} date={}",
                saved.getId(), parentAppointmentId, patientId, followUpDate);
        return saved;
    }

    @Transactional(readOnly = true)
    public List<FollowUpVisit> listByParentAppointment(Long parentAppointmentId) {
        return followUpRepository.findByParentAppointmentId(parentAppointmentId);
    }

    @Transactional(readOnly = true)
    public List<FollowUpVisit> listByPatientAndDate(Long patientId, LocalDate date) {
        return followUpRepository.findByPatientIdAndFollowUpDateOrderByFollowUpDateAsc(patientId, date);
    }

    @Transactional(readOnly = true)
    public List<FollowUpVisit> listByDoctorAndDate(Long doctorId, LocalDate date) {
        return followUpRepository.findByDoctorIdAndFollowUpDateOrderByFollowUpDateAsc(doctorId, date);
    }

    /** Cancel a follow-up visit (SCHEDULED -> CANCELLED). */
    @Transactional
    public FollowUpVisit cancel(Long id) {
        FollowUpVisit f = followUpRepository.findById(id)
                .filter(x -> Boolean.TRUE.equals(x.getStatus()))
                .orElseThrow(() -> new InvalidAppointmentRequestException("NOT_FOUND",
                        "Follow-up visit not found: " + id));
        if (f.getFollowUpStatus() != 1) {
            throw new InvalidAppointmentRequestException("FOLLOWUP_ALREADY_PROCESSED", "FOLLOWUP_ALREADY_PROCESSED: Cannot cancel follow-up with status " + f.getFollowUpStatus());
        }
        f.setFollowUpStatus((short) 3); // CANCELLED
        f.setIsCancelled(true);
        f.setCancelledBy(AppointmentService.AUDIT_ACTOR_ID);
        f.setCancelledDate(Timestamp.from(Instant.now()));
        f.setUpdatedBy(AppointmentService.AUDIT_ACTOR_ID);
        f.setUpdatedDatetime(Timestamp.from(Instant.now()));
        return followUpRepository.save(f);
    }

    /** Mark follow-up as completed. */
    @Transactional
    public FollowUpVisit complete(Long id) {
        FollowUpVisit f = followUpRepository.findById(id)
                .filter(x -> Boolean.TRUE.equals(x.getStatus()))
                .orElseThrow(() -> new InvalidAppointmentRequestException("NOT_FOUND",
                        "Follow-up visit not found: " + id));
        if (f.getFollowUpStatus() != 1) {
            throw new InvalidAppointmentRequestException("FOLLOWUP_ALREADY_PROCESSED", "FOLLOWUP_ALREADY_PROCESSED: Cannot complete follow-up with status " + f.getFollowUpStatus());
        }
        f.setFollowUpStatus((short) 2); // COMPLETED
        f.setUpdatedBy(AppointmentService.AUDIT_ACTOR_ID);
        f.setUpdatedDatetime(Timestamp.from(Instant.now()));
        return followUpRepository.save(f);
    }
}
