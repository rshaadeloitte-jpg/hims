package com.hims.appointment.application;

import com.hims.appointment.domain.model.Appointment;
import com.hims.appointment.domain.repository.AppointmentRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@Service
public class OpdCardService {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd-MMM-yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    private final AppointmentRepository appointmentRepository;

    public OpdCardService(AppointmentRepository appointmentRepository) {
        this.appointmentRepository = appointmentRepository;
    }

    public Map<String, Object> generateOpdCard(Long appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new com.hims.appointment.application.exceptions.AppointmentNotFoundException("NOT_FOUND", "Appointment not found: " + appointmentId));

        Map<String, Object> card = new LinkedHashMap<>();
        card.put("appointmentId", appointment.getId());
        card.put("patientId", appointment.getPatientId());
        card.put("doctorId", appointment.getDoctorId());
        card.put("apptDate", appointment.getApptDate() != null ? appointment.getApptDate().format(DATE_FORMAT) : null);
        card.put("slotStart", appointment.getSlotStart() != null ? appointment.getSlotStart().format(TIME_FORMAT) : null);
        card.put("slotEnd", appointment.getSlotEnd() != null ? appointment.getSlotEnd().format(TIME_FORMAT) : null);
        card.put("tokenNo", appointment.getTokenNo());
        card.put("cabin", appointment.getCabin());
        card.put("visitType", appointment.getVisitType());
        card.put("apptStatus", appointment.getApptStatus());
        card.put("encounterId", appointment.getEncounterId());

        return card;
    }

    public String generateOpdCardHtml(Long appointmentId) {
        Map<String, Object> card = generateOpdCard(appointmentId);

        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html><head><title>OPD Consultation Card</title>");
        html.append("<style>table{border-collapse:collapse;width:100%;}th,td{border:1px solid #333;padding:8px;text-align:left;}</style>");
        html.append("</head><body>");
        html.append("<h1>OPD Consultation Card</h1>");
        html.append("<table>");
        html.append("<tr><th>Field</th><th>Value</th></tr>");
        html.append("<tr><th>Appointment ID</th><td>").append(card.get("appointmentId")).append("</td></tr>");
        html.append("<tr><th>Patient ID</th><td>").append(card.get("patientId")).append("</td></tr>");
        html.append("<tr><th>Doctor ID</th><td>").append(card.get("doctorId")).append("</td></tr>");
        html.append("<tr><th>Date</th><td>").append(card.get("apptDate")).append("</td></tr>");
        html.append("<tr><th>Slot</th><td>").append(card.get("slotStart")).append(" - ").append(card.get("slotEnd")).append("</td></tr>");
        html.append("<tr><th>Token No</th><td>").append(card.get("tokenNo")).append("</td></tr>");
        if (card.get("cabin") != null) {
            html.append("<tr><th>Cabin</th><td>").append(card.get("cabin")).append("</td></tr>");
        }
        if (card.get("visitType") != null) {
            html.append("<tr><th>Visit Type</th><td>").append(card.get("visitType")).append("</td></tr>");
        }
        html.append("<tr><th>Status</th><td>").append(card.get("apptStatus")).append("</td></tr>");
        if (card.get("encounterId") != null) {
            html.append("<tr><th>Encounter ID</th><td>").append(card.get("encounterId")).append("</td></tr>");
        }
        html.append("</table></body></html>");
        return html.toString();
    }
}
