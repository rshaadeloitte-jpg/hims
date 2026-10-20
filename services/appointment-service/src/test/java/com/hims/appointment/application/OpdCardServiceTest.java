package com.hims.appointment.application;

import com.hims.appointment.application.exceptions.AppointmentNotFoundException;
import com.hims.appointment.domain.model.Appointment;
import com.hims.appointment.domain.repository.AppointmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OpdCardServiceTest {

    @Mock private AppointmentRepository appointmentRepository;

    private OpdCardService service;

    @BeforeEach
    void setUp() {
        service = new OpdCardService(appointmentRepository);
    }

    @Test
    void generateOpdCard_validAppointment_returnsData() {
        Appointment a = new Appointment();
        a.setId(1L);
        a.setPatientId(501L);
        a.setDoctorId(11L);
        a.setDepartmentId(2L);
        a.setClinicId(3L);
        a.setUnitId(1L);
        a.setApptDate(LocalDate.of(2027, 3, 15));
        a.setSlotStart(LocalTime.of(9, 30));
        a.setSlotEnd(LocalTime.of(10, 0));
        a.setTokenNo(5);
        a.setCabin("C1");
        a.setVisitType("WALK_IN");
        a.setApptStatus((short) 2);
        a.setStatus(true);
        when(appointmentRepository.findById(1L)).thenReturn(Optional.of(a));

        Map<String, Object> card = service.generateOpdCard(1L);

        assertThat(card.get("appointmentId")).isEqualTo(1L);
        assertThat(card.get("patientId")).isEqualTo(501L);
        assertThat(card.get("apptDate")).isEqualTo("15-Mar-2027");
        assertThat(card.get("slotStart")).isEqualTo("09:30");
        assertThat(card.get("tokenNo")).isEqualTo(5);
        assertThat(card.get("cabin")).isEqualTo("C1");
    }

    @Test
    void generateOpdCard_notFound_throws() {
        when(appointmentRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.generateOpdCard(999L))
                .isInstanceOf(AppointmentNotFoundException.class);
    }

    @Test
    void generateOpdCardHtml_returnsValidHtml() {
        Appointment a = new Appointment();
        a.setId(1L);
        a.setPatientId(501L);
        a.setDoctorId(11L);
        a.setApptDate(LocalDate.of(2027, 3, 15));
        a.setSlotStart(LocalTime.of(9, 30));
        a.setTokenNo(5);
        a.setStatus(true);
        when(appointmentRepository.findById(1L)).thenReturn(Optional.of(a));

        String html = service.generateOpdCardHtml(1L);

        assertThat(html).contains("<!DOCTYPE html>");
        assertThat(html).contains("OPD Consultation Card");
        assertThat(html).contains("501");
        assertThat(html).contains("09:30");
    }
}
