package com.hims.domain.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "fhir_bundle")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class FhirBundle extends AbhaBase {
    private String bundleType;
    @Lob
    private String content;
    private Integer totalEntries;
}