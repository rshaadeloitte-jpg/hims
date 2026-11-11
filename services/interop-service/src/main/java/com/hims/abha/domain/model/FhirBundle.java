package com.hims.abha.domain.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "fhir_bundle", schema = "ihms_abha")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class FhirBundle extends AbhaBase {
    @Column(name = "bundle_type")
    private String bundleType;

    @Lob
    @Column(name = "content")
    private String content;

    @Column(name = "total_entries")
    private Integer totalEntries;
}