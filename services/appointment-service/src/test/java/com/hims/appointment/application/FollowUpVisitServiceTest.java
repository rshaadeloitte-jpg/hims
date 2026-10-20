package com.hims.appointment.application;

import com.hims.appointment.application.exceptions.AppointmentNotFoundException;
import com.hims.appointment.application.exceptions.InvalidAppointmentRequestException;
import com.hims.appointment.domain.model.Appointment;
import com.hims.appointment.domain.model.FollowUpVisit;
import com.hims.appointment.domain.repository.AppointmentRepository;
import com.hims.appointment.domain.repository.FollowUpVisitRepository;
import com.hims.appointment.infrastructure.persistence.SequenceGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FollowUpVisitServiceTest {

    @Mock private FollowUpVisitRepository followUpRepository;
    @Mock private AppointmentRepository appointmentRepository;
    @Mock private SequenceGateway sequences;

    private FollowUpVisitService service;

    @BeforeEach
    void setUp() {
        service = new FollowUpVisitService(followUpRepository, appointmentRepository, sequences);
    }

    private Appointment parentAppt(long id) {
        Appointment a = new Appointment();
        a.setId(id);
        a.setPatientId(501L);
        a.setDoctorId(11L);
        a.setDepartmentId(2L);
        a.setClinicId(3L);
        a.setUnitId(1L);
        a.setStatus(true);
        return a;
    }

    @Test
    void scheduleFollowUp_validInput_createsFollowUp() {
        when(appointmentRepository.findById(1L)).thenReturn(Optional.of(parentAppt(1L)));
        when(sequences.next(anyString())).thenReturn(100L);
        when(followUpRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        FollowUpVisit result = service.scheduleFollowUp(
                1L, 501L, 11L, 2L, 1L,
                LocalDate.of(2027, 4, 1), LocalTime.of(10, 0), LocalTime.of(10, 15),
                null, null, null, null);

        assertThat(result.getId()).isEqualTo(100L);
        assertThat(result.getParentAppointmentId()).isEqualTo(1L);
        assertThat(result.getPatientId()).isEqualTo(501L);
        assertThat(result.getFollowUpStatus()).isEqualTo((short) 1);
        assertThat(result.getIsCancelled()).isFalse();
    }

    @Test
    void scheduleFollowUp_parentNotFound_throws() {
        when(appointmentRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.scheduleFollowUp(
                999L, 501L, 11L, 2L, 1L,
                LocalDate.of(2027, 4, 1), null, null, null, null, null, null))
                .isInstanceOf(AppointmentNotFoundException.class);
    }

    @Test
    void scheduleFollowUp_missingRequiredFields_throws() {
        assertThatThrownBy(() -> service.scheduleFollowUp(
                null, 501L, 11L, 2L, 1L,
                LocalDate.of(2027, 4, 1), null, null, null, null, null, null))
                .isInstanceOf(InvalidAppointmentRequestException.class);
    }

    @Test
    void cancel_scheduledFollowUp_setsCancelled() {
        FollowUpVisit f = new FollowUpVisit();
        f.setId(100L);
        f.setFollowUpStatus((short) 1);
        f.setStatus(true);
        when(followUpRepository.findById(100L)).thenReturn(Optional.of(f));
        when(followUpRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        FollowUpVisit result = service.cancel(100L);

        assertThat(result.getFollowUpStatus()).isEqualTo((short) 3);
        assertThat(result.getIsCancelled()).isTrue();
        assertThat(result.getCancelledBy()).isEqualTo(AppointmentService.AUDIT_ACTOR_ID);
    }

    @Test
    void cancel_alreadyCompleted_throws() {
        FollowUpVisit f = new FollowUpVisit();
        f.setId(100L);
        f.setFollowUpStatus((short) 2);
        f.setStatus(true);
        when(followUpRepository.findById(100L)).thenReturn(Optional.of(f));

        assertThatThrownBy(() -> service.cancel(100L))
                .isInstanceOf(InvalidAppointmentRequestException.class)
                .hasMessageContaining("FOLLOWUP_ALREADY_PROCESSED");
    }

    @Test
    void complete_scheduledFollowUp_setsCompleted() {
        FollowUpVisit f = new FollowUpVisit();
        f.setId(100L);
        f.setFollowUpStatus((short) 1);
        f.setStatus(true);
        when(followUpRepository.findById(100L)).thenReturn(Optional.of(f));
        when(followUpRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        FollowUpVisit result = service.complete(100L);

        assertThat(result.getFollowUpStatus()).isEqualTo((short) 2);
    }

    @Test
    void listByParentAppointment_delegatesToRepository() {
        when(followUpRepository.findByParentAppointmentId(1L)).thenReturn(List.of());
        List<FollowUpVisit> result = service.listByParentAppointment(1L);
        assertThat(result).isEmpty();
    }
}
