package com.hims.abha.domain.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "ehr_file", schema = "ihms_abha")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EhrFile extends AbhaBase {
    @Column(name = "abha_id")
    private String abhaId;

    @Column(name = "document_type")
    private String documentType;

    @Column(name = "document_reference")
    private String documentReference;

    @Column(name = "status")
    private String status;

    @Column(name = "file_path")
    private String filePath;
}