package com.hims.abha.domain.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "generic_substitute", schema = "ihms_abha")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class GenericSubstitute extends AbhaBase {
    private String brandDrugCode;
    private String genericDrugCode;
    private Double substitutionRatio;
}