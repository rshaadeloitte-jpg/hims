package com.hims.adt.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Bed master (clean target of M_BEDMASTER). Occupancy state machine:
 * occupied + bed_status_id per STATUS-CODE-DICTIONARY §A5.
 *
 * BR-IPD-002 free-bed predicate: occupied=false AND status=true AND
 * isnoncensus/isot/ispreop/ispostop=false AND master isrelease=true.
 * Claims are atomic CAS: UPDATE m_bed SET occupied=true WHERE bed_id=? AND occupied=false.
 */
@Entity
@Table(name = "m_bed")
public class Bed extends AdtBase {

    @Id
    @Column(name = "bed_id")
    private Long bedId;

    @Column(name = "bed_no", nullable = false)
    private String bedNo;

    @Column(name = "ward_id", nullable = false)
    private Long wardId;

    @Column(name = "category")
    private String category;

    @Column(name = "building")
    private String building;

    @Column(name = "occupied", nullable = false)
    private Boolean occupied = false;

    @Column(name = "bed_status_id", nullable = false)
    private Short bedStatusId = 1;

    @Column(name = "isnoncensus", nullable = false)
    private Boolean isnoncensus = false;

    @Column(name = "isot", nullable = false)
    private Boolean isot = false;

    @Column(name = "ispreop", nullable = false)
    private Boolean ispreop = false;

    @Column(name = "ispostop", nullable = false)
    private Boolean ispostop = false;

    public Long getBedId() { return bedId; }
    public void setBedId(Long bedId) { this.bedId = bedId; }
    public Long getId() { return bedId; }
    public void setId(Long id) { this.bedId = id; }
    public String getBedNo() { return bedNo; }
    public void setBedNo(String bedNo) { this.bedNo = bedNo; }
    public Long getWardId() { return wardId; }
    public void setWardId(Long wardId) { this.wardId = wardId; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getBuilding() { return building; }
    public void setBuilding(String building) { this.building = building; }
    public Boolean getOccupied() { return occupied; }
    public void setOccupied(Boolean occupied) { this.occupied = occupied; }
    public Short getBedStatusId() { return bedStatusId; }
    public void setBedStatusId(Short bedStatusId) { this.bedStatusId = bedStatusId; }
    public Boolean getIsnoncensus() { return isnoncensus; }
    public void setIsnoncensus(Boolean isnoncensus) { this.isnoncensus = isnoncensus; }
    public Boolean getIsot() { return isot; }
    public void setIsot(Boolean isot) { this.isot = isot; }
    public Boolean getIspreop() { return ispreop; }
    public void setIspreop(Boolean ispreop) { this.ispreop = ispreop; }
    public Boolean getIspostop() { return ispostop; }
    public void setIspostop(Boolean ispostop) { this.ispostop = ispostop; }
}
