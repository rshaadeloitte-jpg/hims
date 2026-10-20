package com.hims.appointment.application;

import com.hims.appointment.application.exceptions.AppointmentNotFoundException;
import com.hims.appointment.application.exceptions.InvalidAppointmentRequestException;
import com.hims.appointment.application.exceptions.SlotAlreadyBookedException;
import com.hims.appointment.domain.model.Appointment;
import com.hims.appointment.domain.model.AppointmentStatus;
import com.hims.appointment.domain.model.DoctorSchedule;
import com.hims.appointment.domain.model.DoctorSlot;
import com.hims.appointment.domain.repository.AppointmentRepository;
import com.hims.appointment.domain.repository.DoctorScheduleRepository;
import com.hims.appointment.domain.repository.DoctorSlotRepository;
import com.hims.appointment.infrastructure.outbox.OutboxService;
import com.hims.appointment.infrastructure.persistence.SequenceGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * L0 — booking validation matrix, slot availability/capacity semantics, state
 * machine moves and reschedule slot release. Repos/outbox are mocked; the
 * real database behaviour is covered by the L2 Testcontainers suite.
 */
class AppointmentServiceTest {

    private static final LocalDate MONDAY = LocalDate.of(2027, 3, 1);   // a Monday

    private DoctorScheduleRepository schedules;
    private DoctorSlotRepository slots;
    private AppointmentRepository appointments;
    private OutboxService outbox;
    private SequenceGateway sequences;
    private AppointmentService service;

    private final AtomicLong ids = new AtomicLong(100);

    @BeforeEach
    void setUp() {
        schedules = mock(DoctorScheduleRepository.class);
        slots = mock(DoctorSlotRepository.class);
        appointments = mock(AppointmentRepository.class);
        outbox = mock(OutboxService.class);
        sequences = mock(SequenceGateway.class);
        when(sequences.next(anyString())).thenAnswer(inv -> ids.incrementAndGet());
        service = new AppointmentService(schedules, slots, appointments, outbox, sequences);
    }

    private DoctorSchedule schedule(short dow) {
        DoctorSchedule s = new DoctorSchedule();
        s.setId(3L);
        s.setDoctorId(11L);
        s.setDayOfWeek(dow);
        s.setStartTime(LocalTime.of(9, 0));
        s.setEndTime(LocalTime.of(12, 0));
        s.setSlotMinutes(30);
        s.setDepartmentId(5L);
        s.setClinicId(9L);
        s.setUnitId(1L);
        s.setStatus(true);
        return s;
    }

    private DoctorSlot freeSlot() {
        DoctorSlot d = new DoctorSlot();
        d.setId(77L);
        d.setScheduleId(3L);
        d.setDoctorId(11L);
        d.setDepartmentId(5L);
        d.setClinicId(9L);
        d.setUnitId(1L);
        d.setSlotDate(MONDAY);
        d.setSlotStart(LocalTime.of(9, 30));
        d.setSlotEnd(LocalTime.of(10, 0));
        d.setCapacity((short) 1);
        d.setBookedCount((short) 0);
        d.setStatus(true);
        return d;
    }

    private void stubHappyPath(DoctorSlot slot) {
        when(schedules.findById(3L)).thenReturn(Optional.of(schedule((short) MONDAY.getDayOfWeek().getValue())));
        when(slots.materialize(anyLong(), any(), anyLong(), anyLong(), anyLong(), any(), any(),
                any(), any(), any(), anyInt(), any())).thenReturn(1);
        when(slots.findByScheduleIdAndSlotDateAndSlotStart(3L, MONDAY, LocalTime.of(9, 30)))
                .thenReturn(Optional.of(slot));
        when(appointments.countByScheduleIdAndApptDateAndSlotStartAndApptStatusInAndStatusTrue(
                eq(3L), eq(MONDAY), eq(LocalTime.of(9, 30)), anyCollection())).thenReturn(0L);
        when(slots.claimCapacity(77L)).thenReturn(1);
        when(appointments.maxToken(3L, MONDAY)).thenReturn(null);
        when(appointments.save(any(Appointment.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private AppointmentService.BookingSpec spec() {
        return new AppointmentService.BookingSpec(501L, 11L, 3L, 1L, 5L, MONDAY,
                LocalTime.of(9, 30), null);
    }

    // ------------------------------------------------------------------ validation

    @Test
    void book_missingRequiredFields_bexa003() {
        AppointmentService.BookingSpec bad = new AppointmentService.BookingSpec(
                null, 11L, 3L, 1L, null, MONDAY, LocalTime.of(9, 30), null);
        assertThatThrownBy(() -> service.book(bad))
                .isInstanceOf(InvalidAppointmentRequestException.class)
                .hasFieldOrPropertyWithValue("errorCode", "BEXA003");
    }

    @Test
    void book_unknownSchedule_notFound() {
        when(schedules.findById(3L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.book(spec()))
                .isInstanceOf(AppointmentNotFoundException.class)
                .hasFieldOrPropertyWithValue("errorCode", "NOT_FOUND");
    }

    @Test
    void book_doctorMismatch_rejected() {
        when(schedules.findById(3L)).thenReturn(Optional.of(schedule((short) 1)));
        assertThatThrownBy(() -> service.book(new AppointmentService.BookingSpec(
                501L, 99L, 3L, 1L, null, MONDAY, LocalTime.of(9, 30), null)))
                .isInstanceOf(InvalidAppointmentRequestException.class)
                .hasFieldOrPropertyWithValue("errorCode", "APPT_DOCTOR_MISMATCH");
    }

    @Test
    void book_dayMismatch_rejected() {
        // Tuesday schedule vs Monday date
        when(schedules.findById(3L)).thenReturn(Optional.of(schedule((short) 2)));
        assertThatThrownBy(() -> service.book(spec()))
                .isInstanceOf(InvalidAppointmentRequestException.class)
                .hasFieldOrPropertyWithValue("errorCode", "APPT_DAY_MISMATCH");
    }

    @Test
    void book_departmentMismatch_rejected() {
        when(schedules.findById(3L)).thenReturn(Optional.of(schedule((short) 1)));
        assertThatThrownBy(() -> service.book(new AppointmentService.BookingSpec(
                501L, 11L, 3L, 1L, 42L, MONDAY, LocalTime.of(9, 30), null)))
                .isInstanceOf(InvalidAppointmentRequestException.class)
                .hasFieldOrPropertyWithValue("errorCode", "APPT_DEPT_MISMATCH");
    }

    @Test
    void book_slotOffGridOrOutsideWindow_rejected() {
        when(schedules.findById(3L)).thenReturn(Optional.of(schedule((short) 1)));
        for (LocalTime bad : new LocalTime[] {LocalTime.of(9, 15),   // off the 30-min grid
                LocalTime.of(8, 30),                                  // before window
                LocalTime.of(11, 45)}) {                              // window would end after endTime
            AppointmentService.BookingSpec s = new AppointmentService.BookingSpec(
                    501L, 11L, 3L, 1L, null, MONDAY, bad, null);
            assertThatThrownBy(() -> service.book(s))
                    .as("slot %s must be rejected", bad)
                    .isInstanceOf(InvalidAppointmentRequestException.class)
                    .hasFieldOrPropertyWithValue("errorCode", "APPT_SLOT_NOT_IN_SCHEDULE");
        }
    }

    @Test
    void book_cabinMismatch_rejected() {
        DoctorSchedule withCabin = schedule((short) 1);
        withCabin.setCabin("CAB-1");
        when(schedules.findById(3L)).thenReturn(Optional.of(withCabin));
        assertThatThrownBy(() -> service.book(new AppointmentService.BookingSpec(
                501L, 11L, 3L, 1L, null, MONDAY, LocalTime.of(9, 30), "CAB-2")))
                .isInstanceOf(InvalidAppointmentRequestException.class)
                .hasFieldOrPropertyWithValue("errorCode", "APPT_CABIN_MISMATCH");
    }

    // ------------------------------------------------------------------ availability

    @Test
    void book_slotFull_returns409ConflictCarrier() {
        DoctorSlot full = freeSlot();
        full.setBookedCount((short) 1);   // capacity 1, already claimed
        stubHappyPath(full);
        assertThatThrownBy(() -> service.book(spec()))
                .isInstanceOf(SlotAlreadyBookedException.class)
                .hasFieldOrPropertyWithValue("errorCode", "APPT_SLOT_TAKEN");
        verify(slots, never()).claimCapacity(anyLong());
    }

    @Test
    void book_concurrentClaimLoss_returns409ConflictCarrier() {
        DoctorSlot slot = freeSlot();
        stubHappyPath(slot);
        when(slots.claimCapacity(77L)).thenReturn(0);   // another tx won the CAS race
        assertThatThrownBy(() -> service.book(spec()))
                .isInstanceOf(SlotAlreadyBookedException.class)
                .hasFieldOrPropertyWithValue("errorCode", "APPT_SLOT_TAKEN");
    }

    // ------------------------------------------------------------------ happy path

    @Test
    void book_assignsSequentialToken_statusBooked_outboxPatientKeyed() {
        DoctorSlot slot = freeSlot();
        stubHappyPath(slot);
        when(appointments.maxToken(3L, MONDAY)).thenReturn(4);

        Appointment saved = service.book(spec());

        assertThat(saved.getId()).isEqualTo(ids.get());
        assertThat(saved.getStatusEnum()).isEqualTo(AppointmentStatus.BOOKED);
        assertThat(saved.getTokenNo()).isEqualTo(5);
        assertThat(saved.getSlotEnd()).isEqualTo(LocalTime.of(10, 0));
        assertThat(saved.getDepartmentId()).isEqualTo(5L);

        ArgumentCaptor<String> key = ArgumentCaptor.forClass(String.class);
        verify(outbox).append(eq(AppointmentService.EVENT_APPOINTMENT_BOOKED), eq("appointment"),
                eq(saved.getId()), key.capture(), anyString(), anyString());
        assertThat(key.getValue()).isEqualTo("501");   // partition key = patientId
    }

    @Test
    void book_cabinDefaultsFromSchedule() {
        DoctorSlot slot = freeSlot();
        slot.setCabin("CAB-1");
        stubHappyPath(slot);
        DoctorSchedule withCabin = schedule((short) 1);
        withCabin.setCabin("CAB-1");
        when(schedules.findById(3L)).thenReturn(Optional.of(withCabin));

        Appointment saved = service.book(new AppointmentService.BookingSpec(
                501L, 11L, 3L, 1L, null, MONDAY, LocalTime.of(9, 30), null));
        assertThat(saved.getCabin()).isEqualTo("CAB-1");
    }

    // ------------------------------------------------------------------ lifecycle

    private Appointment existing(short statusCode) {
        Appointment a = new Appointment();
        a.setId(7L);
        a.setTokenNo(2);
        a.setPatientId(501L);
        a.setDoctorId(11L);
        a.setScheduleId(3L);
        a.setDepartmentId(5L);
        a.setApptDate(MONDAY);
        a.setSlotStart(LocalTime.of(9, 30));
        a.setSlotEnd(LocalTime.of(10, 0));
        a.setApptStatus(statusCode);
        a.setStatus(true);
        when(appointments.findById(7L)).thenReturn(Optional.of(a));
        when(appointments.save(any(Appointment.class))).thenAnswer(inv -> inv.getArgument(0));
        return a;
    }

    @Test
    void cancel_fromBooked_releasesSlot_andEmitsCancelledEvent() {
        Appointment a = existing(AppointmentStatus.BOOKED.code());
        when(slots.findByScheduleIdAndSlotDateAndSlotStart(3L, MONDAY, LocalTime.of(9, 30)))
                .thenReturn(Optional.of(freeSlot()));

        Appointment cancelled = service.cancel(7L);

        assertThat(cancelled.getStatusEnum()).isEqualTo(AppointmentStatus.CANCELLED);
        verify(slots).releaseCapacity(77L);
        ArgumentCaptor<String> key = ArgumentCaptor.forClass(String.class);
        verify(outbox).append(eq(AppointmentService.EVENT_APPOINTMENT_CANCELLED), eq("appointment"),
                eq(7L), key.capture(), anyString(), anyString());
        assertThat(key.getValue()).isEqualTo("501");
    }

    @Test
    void cancel_fromTerminal_illegal() {
        existing(AppointmentStatus.COMPLETED.code());
        assertThatThrownBy(() -> service.cancel(7L))
                .isInstanceOf(InvalidAppointmentRequestException.class)
                .hasFieldOrPropertyWithValue("errorCode", "APPT_ILLEGAL_TRANSITION");
    }

    @Test
    void noshow_onlyFromBooked_completeOnlyFromCheckedIn() {
        existing(AppointmentStatus.CHECKED_IN.code());
        assertThatThrownBy(() -> service.noshow(7L))
                .isInstanceOf(InvalidAppointmentRequestException.class)
                .hasFieldOrPropertyWithValue("errorCode", "APPT_ILLEGAL_TRANSITION");

        existing(AppointmentStatus.BOOKED.code());
        assertThatThrownBy(() -> service.complete(7L))
                .isInstanceOf(InvalidAppointmentRequestException.class)
                .hasFieldOrPropertyWithValue("errorCode", "APPT_ILLEGAL_TRANSITION");

        Appointment checkedIn = existing(AppointmentStatus.CHECKED_IN.code());
        assertThat(service.complete(7L).getStatusEnum()).isEqualTo(AppointmentStatus.COMPLETED);
        assertThat(checkedIn.getStatusEnum()).isEqualTo(AppointmentStatus.COMPLETED);
    }

    @Test
    void reschedule_movesOriginalToRescheduled_linksNewRow_releasesOldSlot() {
        Appointment original = existing(AppointmentStatus.BOOKED.code());
        when(schedules.findById(3L)).thenReturn(Optional.of(schedule((short) MONDAY.getDayOfWeek().getValue())));
        when(slots.findByScheduleIdAndSlotDateAndSlotStart(3L, MONDAY, LocalTime.of(9, 30)))
                .thenReturn(Optional.of(freeSlot()));          // old slot release + target materialization
        when(slots.claimCapacity(77L)).thenReturn(1);
        when(appointments.maxToken(3L, MONDAY)).thenReturn(1);

        Appointment moved = service.reschedule(7L, spec());

        assertThat(original.getStatusEnum()).isEqualTo(AppointmentStatus.RESCHEDULED);
        assertThat(moved.getStatusEnum()).isEqualTo(AppointmentStatus.BOOKED);
        assertThat(moved.getRescheduledFromId()).isEqualTo(7L);
        assertThat(moved.getId()).isNotEqualTo(original.getId());
        verify(slots).releaseCapacity(77L);
        verify(slots).claimCapacity(77L);
    }

    @Test
    void listByDate_missingDate_bexa003() {
        assertThatThrownBy(() -> service.listByDateAndDoctor(null, null))
                .isInstanceOf(InvalidAppointmentRequestException.class)
                .hasFieldOrPropertyWithValue("errorCode", "BEXA003");
    }
}
