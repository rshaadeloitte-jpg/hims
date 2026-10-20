package com.hims.appointment.application;

import com.hims.appointment.application.exceptions.InvalidAppointmentRequestException;
import com.hims.appointment.domain.model.TelemedicineRoom;
import com.hims.appointment.domain.repository.TelemedicineRoomRepository;
import com.hims.appointment.infrastructure.persistence.SequenceGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TelemedicineServiceTest {

    @Mock private TelemedicineRoomRepository telemedRepository;
    @Mock private SequenceGateway sequences;

    private TelemedicineService service;

    @BeforeEach
    void setUp() {
        service = new TelemedicineService(telemedRepository, sequences);
    }

    @Test
    void createRoom_validAppointment_createsRoom() {
        when(sequences.next(anyString())).thenReturn(200L);
        when(telemedRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        TelemedicineRoom result = service.createRoom(1L);

        assertThat(result.getId()).isEqualTo(200L);
        assertThat(result.getAppointmentId()).isEqualTo(1L);
        assertThat(result.getRoomId()).isNotBlank();
        assertThat(result.getVideoLink()).contains("stub");
        assertThat(result.getRoomStatus()).isEqualTo((short) 1);
    }

    @Test
    void getByAppointment_existingRoom_returnsRoom() {
        TelemedicineRoom room = new TelemedicineRoom();
        room.setId(300L);
        room.setAppointmentId(5L);
        room.setRoomId("abc-123");
        when(telemedRepository.findByAppointmentId(5L)).thenReturn(Optional.of(room));

        TelemedicineRoom result = service.getByAppointment(5L);

        assertThat(result.getId()).isEqualTo(300L);
        assertThat(result.getRoomId()).isEqualTo("abc-123");
    }

    @Test
    void startRoom_createdRoom_becomesActive() {
        TelemedicineRoom room = new TelemedicineRoom();
        room.setId(200L);
        room.setRoomStatus((short) 1);
        room.setStatus(true);
        when(telemedRepository.findById(200L)).thenReturn(Optional.of(room));
        when(telemedRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        TelemedicineRoom result = service.startRoom(200L);

        assertThat(result.getRoomStatus()).isEqualTo((short) 2);
        assertThat(result.getStartedAt()).isNotNull();
    }

    @Test
    void startRoom_alreadyActive_throws() {
        TelemedicineRoom room = new TelemedicineRoom();
        room.setId(200L);
        room.setRoomStatus((short) 2);
        room.setStatus(true);
        when(telemedRepository.findById(200L)).thenReturn(Optional.of(room));

        assertThatThrownBy(() -> service.startRoom(200L))
                .isInstanceOf(InvalidAppointmentRequestException.class)
                .hasMessageContaining("TELEMED_INVALID_STATUS");
    }

    @Test
    void endRoom_activeRoom_becomesEnded() {
        TelemedicineRoom room = new TelemedicineRoom();
        room.setId(200L);
        room.setRoomStatus((short) 2);
        room.setStatus(true);
        when(telemedRepository.findById(200L)).thenReturn(Optional.of(room));
        when(telemedRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        TelemedicineRoom result = service.endRoom(200L);

        assertThat(result.getRoomStatus()).isEqualTo((short) 3);
        assertThat(result.getEndedAt()).isNotNull();
    }
}
