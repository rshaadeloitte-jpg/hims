package com.hims.abha.domain.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "hpr_verification", schema = "ihms_abha")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class HprVerification extends AbhaBase {
    @Column(name = "verification_type")
    private String verificationType;

    @Column(name = "verification_value")
    private String verificationValue;

    @Column(name = "status")
    private String status;

    @Column(name = "remarks")
    private String remarks;
}