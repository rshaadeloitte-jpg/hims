package com.hims.abha.domain.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "eaushadhi_drug", schema = "ihms_abha")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EAushadhiDrug extends AbhaBase {
    @Column(name = "drug_code")
    private String drugCode;

    @Column(name = "drug_name")
    private String drugName;

    @Column(name = "manufacturer")
    private String manufacturer;

    @Column(name = "dosage_form")
    private String dosageForm;

    @Column(name = "strength")
    private String strength;

    @Builder.Default
    @Column(name = "is_generic")
    private Boolean isGeneric = false;
}