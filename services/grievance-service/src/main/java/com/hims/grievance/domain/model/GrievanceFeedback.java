package com.hims.grievance.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Clean target of T_PATIENTGRIEVANCEFEEDBACK (5-question satisfaction + free text). */
@Entity
@Table(name = "grievance_feedback")
public class GrievanceFeedback extends GrievanceBase {

    @Id
    private Long id;

    @Column(name = "grievance_id", nullable = false)
    private Long grievanceId;

    @Column(name = "question1")
    private Short question1;

    @Column(name = "question2")
    private Short question2;

    @Column(name = "question3")
    private Short question3;

    @Column(name = "question4")
    private Short question4;

    @Column(name = "question5")
    private Short question5;

    @Column(name = "free_text_feedback")
    private String freeTextFeedback;

    @Column(name = "email")
    private String email;

    @Override public Long getId() { return id; }
    @Override public void setId(Long id) { this.id = id; }
    public Long getGrievanceId() { return grievanceId; }
    public void setGrievanceId(Long grievanceId) { this.grievanceId = grievanceId; }
    public Short getQuestion1() { return question1; }
    public void setQuestion1(Short question1) { this.question1 = question1; }
    public Short getQuestion2() { return question2; }
    public void setQuestion2(Short question2) { this.question2 = question2; }
    public Short getQuestion3() { return question3; }
    public void setQuestion3(Short question3) { this.question3 = question3; }
    public Short getQuestion4() { return question4; }
    public void setQuestion4(Short question4) { this.question4 = question4; }
    public Short getQuestion5() { return question5; }
    public void setQuestion5(Short question5) { this.question5 = question5; }
    public String getFreeTextFeedback() { return freeTextFeedback; }
    public void setFreeTextFeedback(String freeTextFeedback) { this.freeTextFeedback = freeTextFeedback; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}
