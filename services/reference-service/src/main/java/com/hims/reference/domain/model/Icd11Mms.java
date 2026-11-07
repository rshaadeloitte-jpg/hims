package com.hims.reference.domain.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "icd11_mms")
@AttributeOverrides({
        @AttributeOverride(name = "id", column = @Column(name = "id")),
        @AttributeOverride(name = "unitId", column = @Column(name = "unit_id")),
        @AttributeOverride(name = "status", column = @Column(name = "status")),
        @AttributeOverride(name = "createdunitid", column = @Column(name = "created_unit_id")),
        @AttributeOverride(name = "updatedunitid", column = @Column(name = "updated_unit_id")),
        @AttributeOverride(name = "addedby", column = @Column(name = "added_by")),
        @AttributeOverride(name = "addeddatetime", column = @Column(name = "added_datetime")),
        @AttributeOverride(name = "updatedby", column = @Column(name = "updated_by")),
        @AttributeOverride(name = "updateddatetime", column = @Column(name = "updated_datetime")),
        @AttributeOverride(name = "synchronizedd", column = @Column(name = "synchronized"))
})
public class Icd11Mms extends MasterEntityBase {

    public static final String ATTRIBUTION = "ICD-11 MMS, World Health Organization, licensed CC BY-ND 3.0 IGO";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "foundation_uri")
    private String foundationUri;

    @Column(name = "linearization_uri", nullable = false, unique = true)
    private String linearizationUri;

    @Column(name = "code")
    private String code;

    @Column(name = "block_id")
    private String blockId;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "class_kind", nullable = false)
    private String classKind;

    @Column(name = "depth_in_kind")
    private Integer depthInKind;

    @Column(name = "is_residual", nullable = false)
    private Boolean isResidual = false;

    @Column(name = "chapter_no")
    private String chapterNo;

    @Column(name = "is_leaf", nullable = false)
    private Boolean isLeaf = false;

    @Column(name = "parent_linearization_uri")
    private String parentLinearizationUri;

    @Column(name = "release_version", nullable = false)
    private String releaseVersion;

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public void setId(Long id) {
        this.id = id;
    }

    public String getFoundationUri() {
        return foundationUri;
    }

    public void setFoundationUri(String foundationUri) {
        this.foundationUri = foundationUri;
    }

    public String getLinearizationUri() {
        return linearizationUri;
    }

    public void setLinearizationUri(String linearizationUri) {
        this.linearizationUri = linearizationUri;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getBlockId() {
        return blockId;
    }

    public void setBlockId(String blockId) {
        this.blockId = blockId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getClassKind() {
        return classKind;
    }

    public void setClassKind(String classKind) {
        this.classKind = classKind;
    }

    public Integer getDepthInKind() {
        return depthInKind;
    }

    public void setDepthInKind(Integer depthInKind) {
        this.depthInKind = depthInKind;
    }

    public Boolean getIsResidual() {
        return isResidual;
    }

    public void setIsResidual(Boolean residual) {
        isResidual = residual;
    }

    public String getChapterNo() {
        return chapterNo;
    }

    public void setChapterNo(String chapterNo) {
        this.chapterNo = chapterNo;
    }

    public Boolean getIsLeaf() {
        return isLeaf;
    }

    public void setIsLeaf(Boolean leaf) {
        isLeaf = leaf;
    }

    public String getParentLinearizationUri() {
        return parentLinearizationUri;
    }

    public void setParentLinearizationUri(String parentLinearizationUri) {
        this.parentLinearizationUri = parentLinearizationUri;
    }

    public String getReleaseVersion() {
        return releaseVersion;
    }

    public void setReleaseVersion(String releaseVersion) {
        this.releaseVersion = releaseVersion;
    }

    @JsonProperty("attribution")
    public String getAttribution() {
        return ATTRIBUTION;
    }

    @JsonProperty("release_version")
    public String getRelease_version() {
        return releaseVersion;
    }
}
