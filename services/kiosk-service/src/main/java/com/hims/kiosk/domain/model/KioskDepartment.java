package com.hims.kiosk.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "t_kiosk_department", schema = "ihms_kiosk")
public class KioskDepartment {
    @Id
    @Column(name = "id")
    private Long id;

    @Column(name = "name", length = 120, nullable = false)
    private String name;

    @Column(name = "active", nullable = false)
    private Boolean active = true;

    public KioskDepartment() {}

    public KioskDepartment(Long id, String name, Boolean active) {
        this.id = id;
        this.name = name;
        this.active = active;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
}
