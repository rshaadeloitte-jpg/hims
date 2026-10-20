package com.hims.appointment.application;

import com.hims.appointment.application.exceptions.AppointmentNotFoundException;
import com.hims.appointment.application.exceptions.InvalidAppointmentRequestException;
import com.hims.appointment.domain.model.TelemedicineRoom;
import com.hims.appointment.domain.repository.TelemedicineRoomRepository;
import com.hims.appointment.infrastructure.persistence.SequenceGateway;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

@Service
public class TelemedicineService {
    private final TelemedicineRoomRepository telemedicineRoomRepository;
    private final SequenceGateway sequences;

    public TelemedicineService(TelemedicineRoomRepository telemedicineRoomRepository, SequenceGateway sequences) {
        this.telemedicineRoomRepository = telemedicineRoomRepository;
        this.sequences = sequences;
    }

    @Transactional
    public TelemedicineRoom createRoom(Long appointmentId) {
        // Generate room ID and link
        String roomId = UUID.randomUUID().toString();
        String videoLink = "https://tele.example/stub/" + roomId; // Stub link

        TelemedicineRoom room = new TelemedicineRoom();
        room.setId(sequences.next("ihms_appointment.t_telemedicine_room_seq"));
        room.setAppointmentId(appointmentId);
        room.setRoomId(roomId);
        room.setVideoLink(videoLink);
        room.setRoomStatus((short) 1); // CREATED
        room.setExpiresAt(Timestamp.from(Instant.now().plusSeconds(3600)));
        room.setStatus(true);
        room.setSynchronizedd(false);
        room.setAddedBy(AppointmentService.AUDIT_ACTOR_ID);
        room.setAddedDatetime(Timestamp.from(Instant.now()));
        room.setUpdatedBy(AppointmentService.AUDIT_ACTOR_ID);
        room.setUpdatedDatetime(Timestamp.from(Instant.now()));

        return telemedicineRoomRepository.save(room);
    }

    @Transactional(readOnly = true)
    public TelemedicineRoom getByAppointment(Long appointmentId) {
        return telemedicineRoomRepository.findByAppointmentId(appointmentId)
                .orElseThrow(() -> new RuntimeException("Telemedicine room not found for appointment: " + appointmentId));
    }

    @Transactional
    public TelemedicineRoom startRoom(Long roomId) {
        TelemedicineRoom room = telemedicineRoomRepository.findById(roomId)
                .filter(r -> Boolean.TRUE.equals(r.getStatus()))
                .orElseThrow(() -> new InvalidAppointmentRequestException("NOT_FOUND", "Telemedicine room not found: " + roomId));

        if (room.getRoomStatus() == 2) {
            throw new InvalidAppointmentRequestException("TELEMED_INVALID_STATUS", "TELEMED_INVALID_STATUS: Telemedicine room already active: " + roomId);
        }

        room.setRoomStatus((short) 2); // ACTIVE
        room.setStartedAt(Timestamp.from(Instant.now()));
        room.setUpdatedBy(AppointmentService.AUDIT_ACTOR_ID);
        room.setUpdatedDatetime(Timestamp.from(Instant.now()));

        return telemedicineRoomRepository.save(room);
    }

    @Transactional
    public TelemedicineRoom endRoom(Long roomId) {
        TelemedicineRoom room = telemedicineRoomRepository.findById(roomId)
                .filter(r -> Boolean.TRUE.equals(r.getStatus()))
                .orElseThrow(() -> new InvalidAppointmentRequestException("NOT_FOUND", "Telemedicine room not found: " + roomId));

        if (room.getRoomStatus() != 2) {
            throw new InvalidAppointmentRequestException("TELEMED_INVALID_STATUS", "TELEMED_INVALID_STATUS: Cannot end room without active session: " + roomId);
        }

        room.setRoomStatus((short) 3); // ENDED
        room.setEndedAt(Timestamp.from(Instant.now()));
        room.setUpdatedBy(AppointmentService.AUDIT_ACTOR_ID);
        room.setUpdatedDatetime(Timestamp.from(Instant.now()));

        return telemedicineRoomRepository.save(room);
    }
}
