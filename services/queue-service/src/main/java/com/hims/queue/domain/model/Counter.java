package com.hims.queue.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Clean target of T_OPDSTATIONMASTER: counter/station with doctor mapping. */
@Entity
@Table(name = "counter")
public class Counter extends QueueBase {

    @Id
    private Long id;

    @Column(name = "code", nullable = false)
    private String code;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "department_id")
    private Long departmentId;

    @Column(name = "doctor_id")
    private Long doctorId;

    @Override public Long getId() { return id; }
    @Override public void setId(Long id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Long getDepartmentId() { return departmentId; }
    public void setDepartmentId(Long departmentId) { this.departmentId = departmentId; }
    public Long getDoctorId() { return doctorId; }
    public void setDoctorId(Long doctorId) { this.doctorId = doctorId; }
}
