package com.hims.reference.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.math.BigDecimal;

/**
 * Legacy {@code M_DEPARTMENTMASTER} (Departmentmaster.java). Field names preserved verbatim.
 */
@Entity
@Table(name = "M_DEPARTMENTMASTER")
public class DepartmentMaster extends MasterEntityBase {

    @Column(name = "CODE")
    private String code;

    @Column(name = "DESCRIPTION")
    private String description;

    @Column(name = "ISCLINICAL")
    private Boolean isclinical = false;

    @Column(name = "ISPAEDIATRICSDEPARTMENT")
    private Boolean ispaediatricsdepartment = false;

    @Column(name = "FEMALEDEPARTMENT")
    private Boolean femaledepartment = false;

    @Column(name = "TIMESLOT")
    private Integer timeslot;

    @Column(name = "NOOFPATIENT")
    private Integer noofpatient;

    @Column(name = "UPDATEDWINDOWSLOGINNAME")
    private String updatedwindowsloginname;

    @Column(name = "DEFAULTDOCTORID")
    private Long defaultdoctorid;

    @Column(name = "TOKENPREFIX")
    private String tokenprefix;

    @Column(name = "AGELIMIT")
    private Integer agelimit;

    @Column(name = "ISALLIEDHEALTH")
    private Boolean isalliedhealth = false;

    @Column(name = "ISASSESS")
    private Boolean isassess = false;

    @Column(name = "MAINTAINANCESTORE")
    private Boolean maintainancestore = false;

    @Column(name = "ONLINEAPPOINTMENTCOUNT")
    private Integer onlineAppointmentCount = 0;

    @Column(name = "APPOINTMENTSLOT")
    private BigDecimal appointmentslot;

    @Column(name = "ISIPD")
    private Boolean isIpd = false;

    @Column(name = "MAPPEDPHARMACY")
    private Long mappedPharmacy;

    @Column(name = "DEFAULTDEPARTMENT")
    private Boolean defaultdepartment = false;

    @Column(name = "ISFAMILYPLANNINGDEPARTMENT")
    private Boolean isFamilyPlanning = false;

    @Column(name = "EMAIL")
    private String email;

    @Column(name = "LOCALEDESCRIPTION")
    private String localeDescription;

    @Column(name = "DEFAULTROOMID")
    private Long defaultRoomId;

    @Column(name = "qmsNursingStationAvailable")
    private Boolean qmsNursingStationAvailable = false;

    @Column(name = "FROMAGE")
    private Integer fromage = 0;

    @Column(name = "TOAGE")
    private Integer toage = 100;

    @Column(name = "ISTELECONSULT")
    private Boolean isteleconsult = false;

    @Column(name = "ISEMERGENCY")
    private Boolean isEmergency = false;

    @Column(name = "QUEUEPAGESIZE")
    private Long initialQueueCount = 4L;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
