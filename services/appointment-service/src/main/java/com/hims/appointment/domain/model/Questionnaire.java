package com.hims.appointment.domain.model;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.sql.Timestamp;

/**
 * Pre-consult symptom questionnaire (legacy T_QuestionAnswer parity).
 * answers JSONB stores array of: {questionId, answerIds[], answerText, answerGrp}
 * module: 'Patient Assessment' | 'Doctor Assessment' | 'Social Assessment'
 * moduleType: 'PRE_CONSULT' | 'CLINICIAN_REVIEW'
 */
@Entity
@Table(name = "t_questionnaire")
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE,
        fieldVisibility = JsonAutoDetect.Visibility.ANY,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Questionnaire extends AppointmentBase {

    @Id
    private Long id;

    @Column(name = "appointment_id")
    private Long appointmentId;

    @Column(name = "encounter_id")
    private Long encounterId;

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @Column(name = "doctor_id")
    private Long doctorId;

    @Column(name = "module", nullable = false, length = 50)
    private String module;

    @Column(name = "module_type", nullable = false, length = 20)
    private String moduleType = "PRE_CONSULT";

    @Column(name = "answers", columnDefinition = "jsonb", nullable = false)
    private String answers = "[]";

    @Column(name = "is_submitted", nullable = false)
    private Boolean isSubmitted = false;

    @Column(name = "submitted_at")
    private Timestamp submittedAt;

    @Column(name = "clinician_review_flag", nullable = false)
    private Boolean clinicianReviewFlag = false;

    @Column(name = "reviewed_by")
    private Long reviewedBy;

    @Column(name = "reviewed_at")
    private Timestamp reviewedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getAppointmentId() { return appointmentId; }
    public void setAppointmentId(Long appointmentId) { this.appointmentId = appointmentId; }
    public Long getEncounterId() { return encounterId; }
    public void setEncounterId(Long encounterId) { this.encounterId = encounterId; }
    public Long getPatientId() { return patientId; }
    public void setPatientId(Long patientId) { this.patientId = patientId; }
    public Long getDoctorId() { return doctorId; }
    public void setDoctorId(Long doctorId) { this.doctorId = doctorId; }
    public String getModule() { return module; }
    public void setModule(String module) { this.module = module; }
    public String getModuleType() { return moduleType; }
    public void setModuleType(String moduleType) { this.moduleType = moduleType; }
    public String getAnswers() { return answers; }
    public void setAnswers(String answers) { this.answers = answers; }
    public Boolean getIsSubmitted() { return isSubmitted; }
    public void setIsSubmitted(Boolean isSubmitted) { this.isSubmitted = isSubmitted; }
    public Timestamp getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(Timestamp submittedAt) { this.submittedAt = submittedAt; }
    public Boolean getClinicianReviewFlag() { return clinicianReviewFlag; }
    public void setClinicianReviewFlag(Boolean clinicianReviewFlag) { this.clinicianReviewFlag = clinicianReviewFlag; }
    public Long getReviewedBy() { return reviewedBy; }
    public void setReviewedBy(Long reviewedBy) { this.reviewedBy = reviewedBy; }
    public Timestamp getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(Timestamp reviewedAt) { this.reviewedAt = reviewedAt; }
}
