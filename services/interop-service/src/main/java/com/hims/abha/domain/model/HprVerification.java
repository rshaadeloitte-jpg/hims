package com.hims.abha.domain.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "hpr_verification", schema = "ihms_abha")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class HprVerification extends AbhaBase {
    private String verificationType;
    private String verificationValue;
    private String status;
    private String remarks;
}