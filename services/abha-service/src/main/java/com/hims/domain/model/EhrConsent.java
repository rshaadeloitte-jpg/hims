package com.hims.domain.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "ehr_consent")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EhrConsent extends AbhaBase {
    private String consentRequestId;
    private String abhaId;
    private String hipId;
    private String hiuId;
    private String status;
    private String purpose;
}