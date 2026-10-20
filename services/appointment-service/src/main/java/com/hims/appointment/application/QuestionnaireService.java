package com.hims.appointment.application;

import com.hims.appointment.domain.model.Questionnaire;
import com.hims.appointment.domain.repository.QuestionnaireRepository;
import com.hims.appointment.infrastructure.persistence.SequenceGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class QuestionnaireService {
    private static final Logger log = LoggerFactory.getLogger(QuestionnaireService.class);

    private final QuestionnaireRepository questionnaireRepository;
    private final SequenceGateway sequences;

    public QuestionnaireService(QuestionnaireRepository questionnaireRepository, 
                              SequenceGateway sequences) {
        this.questionnaireRepository = questionnaireRepository;
        this.sequences = sequences;
    }

    @Transactional
    public Questionnaire submitAnswers(Long appointmentId, Long encounterId, Long patientId, Long doctorId,
                                       String module, String moduleType, String answersJson, Boolean clinicianReviewFlag) {
        Questionnaire q = new Questionnaire();
        q.setId(sequences.next("ihms_appointment.t_questionnaire_seq"));
        q.setAppointmentId(appointmentId);
        q.setEncounterId(encounterId);
        q.setPatientId(patientId);
        q.setDoctorId(doctorId);
        q.setModule(module);
        q.setModuleType(moduleType != null ? moduleType : "PRE_CONSULT");
        q.setAnswers(answersJson);
        q.setClinicianReviewFlag(clinicianReviewFlag != null ? clinicianReviewFlag : false);
        q.setStatus(true);
        q.setSynchronizedd(false);
        q.setAddedBy(AppointmentService.AUDIT_ACTOR_ID);
        q.setAddedDatetime(Timestamp.from(Instant.now()));
        q.setUpdatedBy(AppointmentService.AUDIT_ACTOR_ID);
        q.setUpdatedDatetime(Timestamp.from(Instant.now()));

        Questionnaire saved = questionnaireRepository.save(q);
        log.info("Submitted questionnaire id={} appointment={} module={}", saved.getId(), appointmentId, module);
        return saved;
    }

    @Transactional(readOnly = true)
    public Optional<Questionnaire> getByAppointmentAndModule(Long appointmentId, String module) {
        return questionnaireRepository.findByAppointmentIdAndModule(appointmentId, module);
    }

    @Transactional(readOnly = true)
    public List<Questionnaire> listByEncounter(Long encounterId) {
        return questionnaireRepository.findByEncounterId(encounterId);
    }

    @Transactional(readOnly = true)
    public List<Questionnaire> listPendingReview() {
        return questionnaireRepository.findByClinicianReviewFlagTrueAndStatusTrue();
    }

    @Transactional
    public Questionnaire markReviewed(Long questionnaireId, Long reviewedBy) {
        Questionnaire q = questionnaireRepository.findById(questionnaireId)
                .orElseThrow(() -> new RuntimeException("Questionnaire not found: " + questionnaireId));

        q.setClinicianReviewFlag(true);
        q.setReviewedBy(reviewedBy);
        q.setReviewedAt(Timestamp.from(Instant.now()));
        q.setUpdatedBy(reviewedBy);
        q.setUpdatedDatetime(Timestamp.from(Instant.now()));

        return questionnaireRepository.save(q);
    }
}
