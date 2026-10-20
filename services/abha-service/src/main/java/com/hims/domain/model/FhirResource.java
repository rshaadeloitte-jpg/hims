package com.hims.domain.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "fhir_resource")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class FhirResource extends AbhaBase {
    private String resourceType;
    private String resourceId;
    @Lob
    private String content;
    private String status;
}