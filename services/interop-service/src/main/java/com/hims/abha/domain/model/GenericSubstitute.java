package com.hims.abha.domain.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "generic_substitute", schema = "ihms_abha")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class GenericSubstitute extends AbhaBase {
    @Column(name = "brand_drug_code")
    private String brandDrugCode;

    @Column(name = "generic_drug_code")
    private String genericDrugCode;

    @Column(name = "substitution_ratio")
    private Double substitutionRatio;
}