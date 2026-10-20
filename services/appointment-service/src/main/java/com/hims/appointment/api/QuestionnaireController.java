package com.hims.appointment.api;

import com.hims.appointment.application.QuestionnaireService;
import com.hims.appointment.domain.model.Questionnaire;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Pre-consult questionnaire API (M02 gap, legacy T_QuestionAnswer parity).
 * Module: 'Patient Assessment' | 'Doctor Assessment' | 'Social Assessment'
 * Answers stored as JSONB: [{questionId, answerIds[], answerText, answerGrp}]
 */
@RestController
@RequestMapping("/api/v1/appointments")
@Tag(name = "Questionnaire (M02)",
        description = "Pre-consult symptom questionnaire. answers JSONB stores [{questionId, answerIds[], answerText, answerGrp}]. "
                + "module: Patient Assessment/Doctor Assessment/Social Assessment")
public class QuestionnaireController {

    private final QuestionnaireService questionnaireService;

    public QuestionnaireController(QuestionnaireService questionnaireService) {
        this.questionnaireService = questionnaireService;
    }

    @PostMapping("/{id}/questionnaire")
    @Operation(summary = "Submit or update questionnaire answers",
            description = "Submits questionnaire answers for an appointment. "
                    + "If a questionnaire already exists for the appointment+module, updates it.")
    public ResponseEntity<Questionnaire> submitAnswers(
            @PathVariable("id") long appointmentId,
            @RequestBody Map<String, Object> body) {
        Long encounterId = AppointmentController.longOf(body, "encounterId");
        Long patientId = AppointmentController.longOf(body, "patientId");
        Long doctorId = AppointmentController.longOf(body, "doctorId");
        String module = AppointmentController.strOf(body, "module");
        String moduleType = AppointmentController.strOf(body, "moduleType");
        String answersJson = AppointmentController.strOf(body, "answers");
        Boolean clinicianReviewFlag = body.containsKey("clinicianReviewFlag")
                ? Boolean.valueOf(String.valueOf(body.get("clinicianReviewFlag")))
                : null;

        Questionnaire saved = questionnaireService.submitAnswers(
                appointmentId, encounterId, patientId, doctorId,
                module, moduleType, answersJson, clinicianReviewFlag);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @GetMapping("/{id}/questionnaire")
    @Operation(summary = "Get questionnaire for an appointment",
            description = "Returns the questionnaire for the specified module (default: Patient Assessment)")
    public Questionnaire getQuestionnaire(
            @PathVariable("id") long appointmentId,
            @RequestParam(defaultValue = "Patient Assessment") String module) {
        return questionnaireService.getByAppointmentAndModule(appointmentId, module)
                .orElseThrow(() -> new com.hims.appointment.application.exceptions.InvalidAppointmentRequestException(
                        "NOT_FOUND", "No questionnaire found for appointment " + appointmentId + " module " + module));
    }

    @GetMapping("/encounters/{encounterId}/questionnaires")
    @Operation(summary = "List all questionnaires for an encounter")
    public List<Questionnaire> listByEncounter(@PathVariable long encounterId) {
        return questionnaireService.listByEncounter(encounterId);
    }

    @GetMapping("/questionnaires/pending-review")
    @Operation(summary = "List questionnaires pending clinician review")
    public List<Questionnaire> listPendingReview() {
        return questionnaireService.listPendingReview();
    }

    @PostMapping("/questionnaires/{id}/review")
    @Operation(summary = "Mark questionnaire as reviewed by clinician")
    public Questionnaire markReviewed(
            @PathVariable long id,
            @RequestParam Long reviewedBy) {
        return questionnaireService.markReviewed(id, reviewedBy);
    }
}
