package com.hims.bcma.domain.model;

public record PatientDrugMatch(Long patientId, String drugIen, String drugName,
                               Boolean verified, String verificationStatus,
                               String message, Long scanId) {
    public boolean isMatch() {
        return "MATCH".equalsIgnoreCase(verificationStatus) || Boolean.TRUE.equals(verified);
    }
}
