package com.hims.lab.domain.model;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.sql.Timestamp;

/**
 * Parsed KDL/machine protocol result. Stores raw instrument payload before mapping
 * to patho parameter rows (Catalog #7 machine interface).
 * Ported from legacy LabKDLResultListDto, LabLoadListForKDLDto.
 */
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE,
        fieldVisibility = JsonAutoDetect.Visibility.ANY,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Entity
@Table(name = "t_kdl_result")
public class KdlResult extends LabBase {

    @Id
    @Column(name = "kdl_result_id")
    private Long kdlResultId;

    @Column(name = "machine_id")
    private Long machineId;

    @Column(name = "protocol", nullable = false, length = 50)
    private String protocol;

    @Column(name = "raw_payload", nullable = false, columnDefinition = "TEXT")
    private String rawPayload;

    @Column(name = "parsed_payload")
    private String parsedPayload;

    @Column(name = "lab_number", length = 50)
    private String labNumber;

    @Column(name = "mrn_no", length = 50)
    private String mrnNo;

    @Column(name = "patient_name", length = 200)
    private String patientName;

    @Column(name = "test_name", length = 200)
    private String testName;

    @Column(name = "sample_status", length = 50)
    private String sampleStatus;

    @Column(name = "is_mapped", nullable = false)
    private Boolean isMapped = false;

    @Column(name = "mapped_detail_id")
    private Long mappedDetailId;

    @Column(name = "received_at", nullable = false)
    private Timestamp receivedAt;

    public Long getKdlResultId() { return kdlResultId; }
    public void setKdlResultId(Long kdlResultId) { this.kdlResultId = kdlResultId; }
    public Long getId() { return kdlResultId; }
    public void setId(Long id) { this.kdlResultId = id; }
    public Long getMachineId() { return machineId; }
    public void setMachineId(Long machineId) { this.machineId = machineId; }
    public String getProtocol() { return protocol; }
    public void setProtocol(String protocol) { this.protocol = protocol; }
    public String getRawPayload() { return rawPayload; }
    public void setRawPayload(String rawPayload) { this.rawPayload = rawPayload; }
    public String getParsedPayload() { return parsedPayload; }
    public void setParsedPayload(String parsedPayload) { this.parsedPayload = parsedPayload; }
    public String getLabNumber() { return labNumber; }
    public void setLabNumber(String labNumber) { this.labNumber = labNumber; }
    public String getMrnNo() { return mrnNo; }
    public void setMrnNo(String mrnNo) { this.mrnNo = mrnNo; }
    public String getPatientName() { return patientName; }
    public void setPatientName(String patientName) { this.patientName = patientName; }
    public String getTestName() { return testName; }
    public void setTestName(String testName) { this.testName = testName; }
    public String getSampleStatus() { return sampleStatus; }
    public void setSampleStatus(String sampleStatus) { this.sampleStatus = sampleStatus; }
    public Boolean getIsMapped() { return isMapped; }
    public void setIsMapped(Boolean isMapped) { this.isMapped = isMapped; }
    public Long getMappedDetailId() { return mappedDetailId; }
    public void setMappedDetailId(Long mappedDetailId) { this.mappedDetailId = mappedDetailId; }
    public Timestamp getReceivedAt() { return receivedAt; }
    public void setReceivedAt(Timestamp receivedAt) { this.receivedAt = receivedAt; }
}
