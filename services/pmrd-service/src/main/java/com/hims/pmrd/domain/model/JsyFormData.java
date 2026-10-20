package com.hims.pmrd.domain.model;

import jakarta.persistence.*;

@Entity
@Table(name = "jsy_form_data")
public class JsyFormData {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "form_type")
    private String formType; // 1 or 2

    @Column(name = "pcts_no")
    private String pctsNo;

    @Column(name = "caste_id")
    private Long casteId;

    @Column(name = "ration_card_number")
    private String rationCardNumber;

    @Column(name = "bpl_number")
    private String bplNumber;

    @Column(name = "res_city_id")
    private Long resCityId;

    @Column(name = "patient_id")
    private Long patientId;

    public JsyFormData() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getFormType() { return formType; }
    public void setFormType(String formType) { this.formType = formType; }
    public String getPctsNo() { return pctsNo; }
    public void setPctsNo(String pctsNo) { this.pctsNo = pctsNo; }
    public Long getCasteId() { return casteId; }
    public void setCasteId(Long casteId) { this.casteId = casteId; }
    public String getRationCardNumber() { return rationCardNumber; }
    public void setRationCardNumber(String rationCardNumber) { this.rationCardNumber = rationCardNumber; }
    public String getBplNumber() { return bplNumber; }
    public void setBplNumber(String bplNumber) { this.bplNumber = bplNumber; }
    public Long getResCityId() { return resCityId; }
    public void setResCityId(Long resCityId) { this.resCityId = resCityId; }
    public Long getPatientId() { return patientId; }
    public void setPatientId(Long patientId) { this.patientId = patientId; }
}
