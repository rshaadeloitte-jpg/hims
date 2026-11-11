package com.hims.abha.domain.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "eaushadhi_drug", schema = "ihms_abha")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EAushadhiDrug extends AbhaBase {
    private String drugCode;
    private String drugName;
    private String manufacturer;
    private String dosageForm;
    private String strength;
    @Builder.Default
    private Boolean isGeneric = false;
}