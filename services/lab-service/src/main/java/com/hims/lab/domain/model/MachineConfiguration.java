package com.hims.lab.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Clean target of T_MACHINECONFIGURATION — analyser/machine interface config
 * (protocol e.g. HL7 / ASTM / file-drop). Machine-interface auth itself is
 * [UNKNOWN] in Catalog #7 security line — deferred.
 */
@Entity
@Table(name = "t_machineconfiguration")
public class MachineConfiguration extends LabBase {

    @Id
    @Column(name = "machine_id")
    private Long machineId;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "protocol", length = 50)
    private String protocol;

    @Column(name = "active", nullable = false)
    private Boolean active = true;

    public Long getMachineId() { return machineId; }
    public void setMachineId(Long machineId) { this.machineId = machineId; }
    public Long getId() { return machineId; }
    public void setId(Long id) { this.machineId = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getProtocol() { return protocol; }
    public void setProtocol(String protocol) { this.protocol = protocol; }
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
}
