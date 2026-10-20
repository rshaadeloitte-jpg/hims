package com.hims.appointment.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;

public class QuestionnaireRequest {
    @NotBlank(message = "Module is required")
    private String module;
    private String moduleType = "PRE_CONSULT";
    @NotNull(message = "Answers are required")
    private List<Map<String, Object>> answers;
    private Boolean clinicianReviewFlag = false;

    public String getModule() { return module; }
    public void setModule(String module) { this.module = module; }
    public String getModuleType() { return moduleType; }
    public void setModuleType(String moduleType) { this.moduleType = moduleType; }
    public List<Map<String, Object>> getAnswers() { return answers; }
    public void setAnswers(List<Map<String, Object>> answers) { this.answers = answers; }
    public Boolean getClinicianReviewFlag() { return clinicianReviewFlag; }
    public void setClinicianReviewFlag(Boolean clinicianReviewFlag) { this.clinicianReviewFlag = clinicianReviewFlag; }
}
