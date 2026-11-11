package com.hims.abha.domain.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "ehr_consent", schema = "ihms_abha")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EhrConsent extends AbhaBase {
    @Column(name = "consent_request_id")
    private String consentRequestId;

    @Column(name = "abha_id")
    private String abhaId;

    @Column(name = "hip_id")
    private String hipId;

    @Column(name = "hiu_id")
    private String hiuId;

    @Column(name = "status")
    private String status;

    @Column(name = "purpose")
    private String purpose;
}