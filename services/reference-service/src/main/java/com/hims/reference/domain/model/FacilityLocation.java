package com.hims.reference.domain.model;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * Clean target of M_BUILDING/FLOOR/WING/ROOM/CABINMASTER (+NURSING_STATION) —
 * one hierarchy table (AGT-02 §4.3, V5). MIG-011.
 */
@Entity
@Table(name = "facility_location")
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
public class FacilityLocation extends MasterEntityBase {

    @Column(name = "location_type", nullable = false)
    private String locationType;

    @Column(name = "code")
    private String code;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "name_hindi")
    private String nameHindi;

    @Column(name = "parent_id")
    private Long parentId;

    @Column(name = "sequence_no")
    private Integer sequenceNo;

    @Column(name = "is_unique_sequence")
    private Boolean isUniqueSequence = false;

    @Column(name = "token_prefix")
    private String tokenPrefix;

    @Column(name = "is_room_amenities")
    private Boolean isRoomAmenities = false;

    public String getLocationType() { return locationType; }
    public void setLocationType(String locationType) { this.locationType = locationType; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getNameHindi() { return nameHindi; }
    public void setNameHindi(String nameHindi) { this.nameHindi = nameHindi; }
    public Long getParentId() { return parentId; }
    public void setParentId(Long parentId) { this.parentId = parentId; }
    public Integer getSequenceNo() { return sequenceNo; }
    public void setSequenceNo(Integer sequenceNo) { this.sequenceNo = sequenceNo; }
    public Boolean getIsUniqueSequence() { return isUniqueSequence; }
    public void setIsUniqueSequence(Boolean isUniqueSequence) { this.isUniqueSequence = isUniqueSequence; }
    public String getTokenPrefix() { return tokenPrefix; }
    public void setTokenPrefix(String tokenPrefix) { this.tokenPrefix = tokenPrefix; }
    public Boolean getIsRoomAmenities() { return isRoomAmenities; }
    public void setIsRoomAmenities(Boolean isRoomAmenities) { this.isRoomAmenities = isRoomAmenities; }
}
