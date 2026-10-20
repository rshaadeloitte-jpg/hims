package com.hims.reference.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.math.BigDecimal;

/**
 * Legacy {@code M_COMPANYMASTER} (Companymaster.java). Field names preserved verbatim.
 */
@Entity
@Table(name = "M_COMPANYMASTER")
public class CompanyMaster extends MasterEntityBase {

    @Column(name = "CODE")
    private String code;

    @Column(name = "DESCRIPTION")
    private String description;

    @Column(name = "COMPANYTYPEID")
    private Long companyTypeId;

    @Column(name = "TOTALLIMIT")
    private BigDecimal totallimit;

    @Column(name = "USEDAMOUNT")
    private BigDecimal usedamount;

    @Column(name = "CREDITLIMIT")
    private Long creditlimit;

    @Column(name = "SERVICECHARGE")
    private BigDecimal servicecharge;

    @Column(name = "ADDRESS")
    private String address;

    @Column(name = "COUNTRYID")
    private Long countryId;

    @Column(name = "STATEID")
    private Long stateId;

    @Column(name = "CITYID")
    private Long cityId;

    @Column(name = "AREA")
    private String area;

    @Column(name = "PINCODE")
    private String pincode;

    @Column(name = "CONTACTPERSON")
    private String contactperson;

    @Column(name = "MOBILECOUNTRYCODE")
    private Long mobilecountrycode;

    @Column(name = "MOBILENO")
    private Long mobileno;

    @Column(name = "COUNTRYCODE")
    private Long countrycode;

    @Column(name = "STDCODE")
    private Long stdcode;

    @Column(name = "PHONENO")
    private Long phoneno;

    @Column(name = "REPORTFORMAT")
    private byte[] reportformat;

    @Column(name = "REPORTPATH")
    private String reportpath;

    @Column(name = "MOUDISPER")
    private BigDecimal moudisper;

    @Column(name = "FLATPER")
    private BigDecimal flatper;

    @Column(name = "DESIGNATION")
    private String designation;

    @Column(name = "EMAIL")
    private String email;

    @Column(name = "CREDIT_ALLOWED")
    private Boolean creditAllowed;

    @Column(name = "COMPANYNAMEHINDI")
    private String companyNameHindi;

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
