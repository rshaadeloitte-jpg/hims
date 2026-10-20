package com.hims.equipment.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.sql.Timestamp;

/**
 * Equipment document metadata row (equipment_detail_docs, V1) — one row per
 * document attached to an asset (legacy T_EQUIPMENTDETAILDOCS).
 *
 * <p>Metadata-only: {@code filePath} points at wherever the file lives
 * (network share / DMS key); this service stores no bytes. {@code description}
 * maps the TEXT column. {@code docType} is a free dictionary value (MANUAL /
 * WARRANTY_CARD / CALIBRATION_CERT / CONDEMNATION_REPORT ...). Server-owned id
 * from ihms_equipment.equipment_detail_docs_seq (IMP-DEC-003). No Java-side
 * initializers — every column except equipment_id is optional and the service
 * owns validation/defaulting.
 */
@Entity
@Table(name = "equipment_detail_docs")
public class EquipmentDetailDocs extends EquipmentBase {

    @Id
    private Long id;

    @Column(name = "equipment_id", nullable = false)
    private Long equipmentId;

    /** MANUAL | WARRANTY_CARD | CALIBRATION_CERT | CONDEMNATION_REPORT ... */
    @Column(name = "doc_type")
    private String docType;

    @Column(name = "file_name")
    private String fileName;

    /** Where the file actually lives (share path / DMS key) — no bytes here. */
    @Column(name = "file_path")
    private String filePath;

    /** TEXT column — free-text description of the document. */
    @Column(name = "description")
    private String description;

    @Override
    public Long getId() { return id; }
    @Override
    public void setId(Long id) { this.id = id; }
    public Long getEquipmentId() { return equipmentId; }
    public void setEquipmentId(Long equipmentId) { this.equipmentId = equipmentId; }
    public String getDocType() { return docType; }
    public void setDocType(String docType) { this.docType = docType; }
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
