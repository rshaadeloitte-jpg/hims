package com.hims.adt.domain.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "mortuary_chambers")
public class MortuaryChamber {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "mortuary_chamber_seq")
    @SequenceGenerator(name = "mortuary_chamber_seq", sequenceName = "mortuary_chamber_seq", allocationSize = 1)
    private Long id;

    @Column(name = "chamber_no", nullable = false, unique = true)
    private String chamberNo;

    @Column(name = "body_name")
    private String bodyName;

    @Column(name = "status")
    private String status = "AVAILABLE";

    @Column(name = "admitted_at")
    private LocalDateTime admittedAt;

    @Column(name = "death_cert_no")
    private String deathCertNo;

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getChamberNo() { return chamberNo; }
    public void setChamberNo(String chamberNo) { this.chamberNo = chamberNo; }
    public String getBodyName() { return bodyName; }
    public void setBodyName(String bodyName) { this.bodyName = bodyName; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getAdmittedAt() { return admittedAt; }
    public void setAdmittedAt(LocalDateTime admittedAt) { this.admittedAt = admittedAt; }
    public String getDeathCertNo() { return deathCertNo; }
    public void setDeathCertNo(String deathCertNo) { this.deathCertNo = deathCertNo; }
}
