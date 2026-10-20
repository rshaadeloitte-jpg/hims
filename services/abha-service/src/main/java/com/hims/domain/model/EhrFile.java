package com.hims.domain.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "ehr_file")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EhrFile extends AbhaBase {
    private String abhaId;
    private String documentType;
    private String documentReference;
    private String status;
    private String filePath;
}