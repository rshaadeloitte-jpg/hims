package com.hims.reference.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.util.Date;

/**
 * Legacy {@code M_UNITMASTER} (Unitmaster.java). Field names preserved verbatim —
 * they ARE the JSON contract (legacy Jackson field-visibility serialization).
 */
@Entity
@Table(name = "M_UNITMASTER")
public class UnitMaster extends MasterEntityBase {

    @Column(name = "CODE")
    private String code;

    @Column(name = "NAME")
    private String name;

    @Column(name = "DESCRIPTION")
    private String description;

    @Column(name = "ADDRESSLINE1")
    private String addressline1;

    @Column(name = "ADDRESSLINE2")
    private String addressline2;

    @Column(name = "ADDRESSLINE3")
    private String addressline3;

    @Column(name = "COUNTRY")
    private String country;

    @Column(name = "STATE")
    private String state;

    @Column(name = "DISTRICT")
    private String district;

    @Column(name = "TALUKA")
    private String taluka;

    @Column(name = "AREA")
    private String area;

    @Column(name = "PINCODE")
    private String pincode;

    @Column(name = "SERVERNAME")
    private String servername;

    @Column(name = "DATABASENAME")
    private String databasename;

    @Column(name = "PHARMACYLICENSENO")
    private String pharmacylicenseno;

    @Column(name = "CLINICREGNO")
    private String clinicregno;

    @Column(name = "SHOPNO")
    private String shopno;

    @Column(name = "TRADENO")
    private String tradeno;

    @Column(name = "CITY")
    private String city;

    @Column(name = "CONTACTNO")
    private String contactno;

    @Column(name = "RESINOCOUNTRYCODE")
    private Integer resinocountrycode;

    @Column(name = "RESISTDCODE")
    private Integer resistdcode;

    @Column(name = "FAXNO")
    private String faxno;

    @Column(name = "EMAIL")
    private String email;

    @Column(name = "UNITPREFIXFORMRNO")
    private String unitprefixformrno;

    @Column(name = "LOGO")
    private byte[] logo;

    @Column(name = "VAT_TIN_No")
    private String vatTinNo;

    @Column(name = "ISMEDVERIFY")
    private Boolean isMedVerify;

    @Column(name = "DISTRICTID")
    private Long districtId;

    @Column(name = "TENANTID")
    private Long tenantId;

    @Column(name = "CATEGORY")
    private Long category;

    @Column(name = "TYPE")
    private Long type;

    @Column(name = "ISALLOWONLINE")
    private Boolean isAllowOnline = false;

    @Column(name = "LONGITUDE")
    private String longitude;

    @Column(name = "LATIUTDE")
    private String latiutde;

    @Column(name = "ISPARENT")
    private Boolean isParent = false;

    @Column(name = "PARENTID")
    private Long parentId;

    @Column(name = "ISVIRTUAL")
    private Boolean isVirtual = false;

    @Column(name = "VIRTUALID")
    private Long virtualId;

    @Column(name = "CONTRACTFROMDATE")
    private Date contractFromDate;

    @Column(name = "CONTRACTTODATE")
    private Date contractToDate;

    @Column(name = "ISSUSPENSION")
    private Boolean isSuspension = false;

    @Column(name = "ISTPU")
    private Boolean istpu = false;

    @Column(name = "ISTSU")
    private Boolean istsu = false;

    @Column(name = "DEFAULTGENDERID")
    private Long defaultgenderid;

    @Column(name = "NINCODE")
    private String ninCode;

    @Column(name = "ISCONFIRMATORY")
    private Boolean isConfirmatory;

    @Column(name = "ISADMINISTRATIVE")
    private Boolean isAdministrative;

    @Column(name = "ISOFFLINE")
    private Boolean isOffline;

    @Column(name = "ALLOWDIRECTONLINEAPPOINTMENT")
    private Boolean allowDirectOnlineAppointment = false;

    @Column(name = "ALLOWLABNORMALRESULTREVIEW")
    private Boolean allowLabNormalResultReview = true;

    @Column(name = "BILLNUMBERLISINTEGRATION")
    private Boolean billNumberLisIntegration = false;

    @Column(name = "CONSULTTYPE")
    private Integer consultType = 0;

    @Column(name = "ENABLEEHR")
    private Boolean enableEhr = false;

    @Column(name = "ENABLEOTPBASEDLOGIN")
    private Boolean enableOTPBasedLogin = false;

    @Column(name = "ENABLEWHATSAPP")
    private Boolean enableWhatsApp = false;

    @Column(name = "FACILITYLINKED")
    private Boolean facilityLinked = false;

    @Column(name = "ISEAUSHADHIDATASYNC")
    private Boolean isEaushadhiDataSync = false;

    @Column(name = "ISMULTIMAINSTORE")
    private Boolean isMultiMainStore = false;

    @Column(name = "ISOPLABELREQUIRED")
    private Boolean isOpLabelRequired = false;

    @Column(name = "ISOPERATIONAL")
    private Boolean isOperational = false;

    @Column(name = "ISPHARMACYUPLOADPRESC")
    private Boolean ispharmacyUploadPresc = false;

    @Column(name = "ISPHARMACYVISITREQUIRED")
    private Boolean isPharmacyVisitRequired = false;

    @Column(name = "PARENTCHILDUNITBASED")
    private Boolean parentChildUnitBased = false;

    @Column(name = "SHOWPATHOLOCATION")
    private Boolean showPathoLocation = false;

    @Column(name = "SHOWPATHOSUBSPECIALIZATION")
    private Boolean showPathoSubSpecialization = false;

    @Column(name = "HOSPITALTYPE")
    private Integer hospitalType;

    @Column(name = "ISAUTOINDENT")
    private Boolean isAutoIndent = false;

    @Column(name = "AUTOINDENTEXECUTIONTIME")
    private String autoIndentExecutionTime;

    @Column(name = "ISAUTOINDENTFOR")
    private Long isAutoIndentFor;

    @Column(name = "PATHIESTYPE_NEW")
    private String pathiesType;

    @Column(name = "APIKEY")
    private String apiKey;

    @Column(name = "EAUSHADHISTOREID")
    private String eAushadhiStoreId;

    @Column(name = "EAUSHADHIWAREHOUSEID")
    private String eAushadhiWarehouseId;

    @Column(name = "EAPARENTSTOREID")
    private Long eAParentStoreId;

    @Column(name = "EASTOREID")
    private Long eAStoreId;

    @Column(name = "apiToken")
    private String apiToken;

    @Column(name = "EAUSPCTS_STOREID")
    private Long EausPCTS_StoreId;

    @Column(name = "OUTREACHID")
    private Long outReachid;

    @Column(name = "ISCENTRALSTORE")
    private Boolean isCentralStore = false;

    @Column(name = "ISCENTRALLYOUTSOURCEDLAB")
    private Boolean isCentrallyOutsourcedLab = false;

    @Column(name = "ISCWFCOVERSHEET")
    private Short isCWFCoversheet;

    @Column(name = "CENTRALSTOREIDS")
    private String centralstoreids;

    @Column(name = "ERAKTKOSHUNITID")
    private String eRaktkoshUnitId;

    @Column(name = "ERAKTOSHKEY")
    private String eRaktkoshKey;

    @Column(name = "MERCHANTCODE")
    private String merchantCode;

    @Column(name = "ABDMID")
    private String abdmId;

    @Column(name = "HMISID")
    private String hmisId;

    @Column(name = "HMISCODE")
    private String hmisCode;

    @Column(name = "HISTONOCATEGORY")
    private String histoNoCategory;

    @Column(name = "ALLOWEDOPERATORMOBILENUMBERSJSON")
    private String allowedOperatorMobileNumbersJson;

    @Column(name = "BEDCAPACITY")
    private Long bedCapacity;

    @Column(name = "EHRCODE")
    private String ehrCode;

    @Column(name = "EAUSHADHISTOREDESC")
    private String eAushadhiStoreDesc;

    @Column(name = "ABDMHFRID")
    private String abdmHfrId;

    @Column(name = "ABDMFACILITYNAME")
    private String abdmFacilityName;

    @Column(name = "LOCALEDESCRIPTION")
    private String localeDescription;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
